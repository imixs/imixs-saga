/*******************************************************************************
 * <pre>
 *  Imixs Workflow 
 *  Copyright (C) 2001-2020 Imixs Software Solutions GmbH,  
 *  http://www.imixs.com
 *  
 *  This program is free software; you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License 
 *  as published by the Free Software Foundation; either version 2 
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful, 
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of 
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 *  General Public License for more details.
 *  
 *  You can receive a copy of the GNU General Public
 *  License at http://www.gnu.org/licenses/gpl.html
 *  
 *  Project: 
 *      https://www.imixs.org
 *      https://github.com/imixs/imixs-workflow
 *  
 *  Contributors:  
 *      Imixs Software Solutions GmbH - initial API and implementation
 *      Ralph Soika - Software Developer
 * </pre>
 *******************************************************************************/

package org.imixs.microservice.security.auth;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKey;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.jwt.HMAC;
import org.imixs.jwt.JWTBuilder;
import org.imixs.jwt.JWTException;
import org.imixs.melman.AbstractClient;
import org.imixs.melman.BasicAuthenticator;
import org.imixs.melman.FormAuthenticator;
import org.imixs.melman.JWTAuthenticator;

/**
 * This is the default Imixs-Microservice Authenticator used by the Imixs-Microservice API for a
 * inter serivce communication. This Authenitcator is used by the imixs-registry to access an
 * Imixs-Microservice and also by the Imixs-Microserivce to access the Imixs-Registry.
 * <p>
 * The DefaultAuthenticator supports the following authentication methods:
 * <ul>
 * <li>BASIC</li>
 * <li>FORM</li>
 * <li>JWT</li>
 * <li>CUSTOM</li>
 * </ul>
 * Depending on the auth method (BASIC,FORM,JWT) the corresponding authenticator filter is chosen
 * per default. The properties secret and userid can be used to set password and userid.
 * <p>
 * <ul>
 * <li>IMIXS_REGISTRY_AUTH_SECRET</li>
 * <li>IMIXS_REGISTRY_AUTH_USERID</li>
 * </ul>
 * <p>
 * In case the property 'imixs.auth.propagation' is set to true and an authentication header is part
 * of the current request, then the authentication header is propagated to the following service
 * call.
 * <p>
 * In case the auth method is not defined or set to 'CUSTOM' no default filter is chosen. In this
 * case a custom implementation of an authenticator can observe the CDI Event
 * 'org.imixs.microservice.core.auth.AuthEvent' to register a custom filter.
 * 
 * @version 1.0
 * @author rsoika
 */
@RequestScoped
public class DefaultAuthenticator {

  private static final String QUERY_PARAM_SESSION = "jwt";

  @Inject
  @ConfigProperty(name = "imixs.auth.secret", defaultValue = "")
  String authSecret;

  @Inject
  @ConfigProperty(name = "imixs.auth.service", defaultValue = "")
  String authService;

  @Inject
  @ConfigProperty(name = "imixs.auth.userid", defaultValue = "")
  String authUserID;

  @Inject
  @ConfigProperty(name = "imixs.auth.method", defaultValue = "CUSTOM")
  String authMethod;

  @Inject
  @ConfigProperty(name = "imixs.auth.propagation", defaultValue = "false")
  boolean propagateAuthentication;

  private static Logger logger = Logger.getLogger(DefaultAuthenticator.class.getName());

  /**
   * This method registers the default JWT auth module.
   * <p>
   * The method creates a JWT based on a given secret with the userid='imixs-microservice'
   * 
   * @param authEvent - providing a melman rest client instance
   * @throws JWTException
   */
  public void registerRequestFilter(@Observes AuthEvent authEvent) throws AuthException {
    boolean debug = logger.isLoggable(Level.FINE);
    // Disabled?
    if ("CUSTOM".equalsIgnoreCase(authMethod) || authMethod.isEmpty()) {
      if (debug) {
        logger.finest("......Default Auth Module disabled!");
      }
      return;
    }

    // test if authentication propagation is true
    try {
      if (propagateAuthentication && authEvent.getRequest() != null) {
        String authorizationToken = null;
        // try to extract the authentication token....
        // 1st try bearer token...
        authorizationToken = authEvent.getRequest().getHeader("Authorization");
        if (authorizationToken != null && !authorizationToken.isEmpty()) {
          // fine, we can propagate the token
        } else {
          // 2nd try: check the header for a 'jwt' param
          String jwt = authEvent.getRequest().getHeader("jwt");
          if (jwt != null && !jwt.isEmpty()) {
            authorizationToken = "Bearer " + jwt;
          } else {
            // 3rd try quersting ?jwt=.....
            jwt = authEvent.getRequest().getQueryString();
            if (jwt != null && !jwt.isEmpty()) {
              int iPos = jwt.indexOf(QUERY_PARAM_SESSION + "=");
              if (iPos > -1) {
                if (debug) {
                  logger.fine("parsing query param " + QUERY_PARAM_SESSION + "....");
                }
                iPos = iPos + (QUERY_PARAM_SESSION + "=").length() + 0;
                jwt = jwt.substring(iPos);

                iPos = jwt.indexOf("&");
                if (iPos > -1) {
                  jwt = jwt.substring(0, iPos);
                }
                // url-decoding of token (issue #7)
                jwt = getURLDecodedToken(jwt);
                authorizationToken = "Bearer " + jwt;
              }
            }
          }
        }

        // in case we found a token, than we use the PropagationAuthenticator
        if (authorizationToken != null && !authorizationToken.isEmpty()) {
          registerPropagationAuthenticator(authEvent.getClient(), authorizationToken);
          return;
        }
      }
    } catch (Exception e) {
      logger.warning("Unable to resolve http request header - " + e.getMessage());
      // unable to resolve request param
      // no op!
    }

    if (authSecret.isEmpty()) {
      logger.warning("Default Auth Module: secret not set - check your configuration!");
    }

    if (authUserID.isEmpty()) {
      logger.warning("Default Auth Module: secret not set - check your configuration!");
    }

    // register default auth modules
    if ("JWT".equalsIgnoreCase(authMethod)) {
      registerJWTAuthenticator(authEvent.getClient());
    }
    if ("BASIC".equalsIgnoreCase(authMethod)) {
      registerBasicAuthenticator(authEvent.getClient());
    }
    if ("FORM".equalsIgnoreCase(authMethod)) {
      registerFormAuthenticator(authEvent.getClient());
    }

  }

  /**
   * Helper method to register a BasicAuthenticator
   * 
   * @param client
   * @throws AuthException
   */
  private void registerBasicAuthenticator(AbstractClient client) {
    // build default Basic Authenticator
    BasicAuthenticator basicAuthenticator = new BasicAuthenticator(authUserID, authSecret);
    client.registerClientRequestFilter(basicAuthenticator);
  }

  /**
   * Helper method to register a FormAuthenticator
   * 
   * @param client
   * @throws AuthException
   */
  private void registerFormAuthenticator(AbstractClient client) {
    // build default Basic Authenticator
    FormAuthenticator formAuthenticator =
        new FormAuthenticator(authService, authUserID, authSecret);
    client.registerClientRequestFilter(formAuthenticator);
  }

  /**
   * Helper method to register a JWTAuthenticator
   * 
   * @param client
   * @throws AuthException
   */
  private void registerJWTAuthenticator(AbstractClient client) throws AuthException {
    boolean debug = logger.isLoggable(Level.FINE);
    // build default jwt token
    SecretKey secretKey = HMAC.createKey("HmacSHA256", authSecret.getBytes());
    String payload = "{\"sub\":\"" + authUserID + "\",\"displayname\":\"" + authUserID
        + "\",\"groups\":[\"IMIXS-WORKFLOW-Manager\"]}";

    if (debug) {
      logger.finest("......Payload=" + payload);
    }
    JWTBuilder builder = new JWTBuilder().setKey(secretKey).setPayload(payload);
    try {
      // create JWT Auth Modul....
      JWTAuthenticator jwtAuth = new JWTAuthenticator(builder.getToken());
      client.registerClientRequestFilter(jwtAuth);

    } catch (JWTException e) {
      e.printStackTrace();
      throw new AuthException("JWT_ERROR", e.getMessage(), e);
    }

  }

  /**
   * Helper method to register a BasicAuthenticator
   * 
   * @param client
   * @throws AuthException
   */
  private void registerPropagationAuthenticator(AbstractClient client, String token) {
    // build default Basic Authenticator
    PropagationAuthenticator propagationAuthenticator = new PropagationAuthenticator(token);
    client.registerClientRequestFilter(propagationAuthenticator);
  }

  /**
   * This method decodes the token with the java.netURLDecoder. The method takes care about the '+'
   * character. The plus sign "+" is converted into a space character " " by the URLDecoder class.
   * This method replaces the " " again back into "+".
   * 
   * See also : https://docs.oracle.com/javase/6/docs/api/java/net/URLDecoder.html
   * 
   * @see issue #7
   * @param token
   * @return URL decoded token
   */
  String getURLDecodedToken(String token) {

    try {
      token = URLDecoder.decode(token, "UTF-8");
      // convert " " into "+"
      token = token.replaceAll(" ", "+");
    } catch (UnsupportedEncodingException e) {
      logger.severe("URL decoding of token failed " + e.getMessage());
      return null;
    }
    return token;
  }

}

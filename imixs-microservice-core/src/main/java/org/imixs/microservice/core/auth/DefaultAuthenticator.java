package org.imixs.microservice.core.auth;

import java.util.logging.Logger;

import javax.crypto.SecretKey;
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
 * This is the default Imixs-Registry Authenticator used by the
 * Imixs-Microservice API to register itself as an service at the
 * Imixs-Registry.
 * <p>
 * The DefaultAuthenticator supports the following authentication methods:
 * <ul>
 * <li>BASIC</li>
 * <li>FORM</li>
 * <li>JWT</li>
 * <li>CUSTOM</li>
 * </ul>
 * Depending on the auth method (BASIC,FORM,JWT) the corresponding authenticator
 * filter is chosen per default. The properties secret and userid can be used to
 * set password and userid.
 * <p>
 * <ul>
 * <li>IMIXS_REGISTRY_AUTH_SECRET</li>
 * <li>IMIXS_REGISTRY_AUTH_USERID</li>
 * </ul>
 * <p>
 * In case the auth method is not defined or set to 'CUSTOM' no default filter
 * is chosen. In this case a custom implementation of an authenticator can
 * observe the CDI Event 'org.imixs.microservice.core.auth.AuthEvent' to
 * register a custom filter.
 * 
 * @version 1.0
 * @author rsoika
 */
public class DefaultAuthenticator {

	@Inject
	@ConfigProperty(name = "imixs.registry.auth.secret", defaultValue = "")
	String authSecret;

	@Inject
	@ConfigProperty(name = "imixs.registry.auth.service", defaultValue = "")
	String authService;

	@Inject
	@ConfigProperty(name = "imixs.registry.auth.userid", defaultValue = "")
	String authUserID;

	@Inject
	@ConfigProperty(name = "imixs.registry.auth.method", defaultValue = "CUSTOM")
	String authMethod;

	private static Logger logger = Logger.getLogger(DefaultAuthenticator.class.getName());

	/**
	 * This method registers the default JWT auth module.
	 * <p>
	 * The method creates a JWT based on a given secret with the
	 * userid='imixs-microservice'
	 * 
	 * @param authEvent - providing a melman rest client instance
	 * @throws JWTException
	 */
	public void registerRequestFilter(@Observes AuthEvent authEvent) throws AuthException {

		// Disabled?
		if ("CUSTOM".equalsIgnoreCase(authMethod) || authMethod.isEmpty()) {
			logger.finest("......Default Auth Module disabled!");
			return;
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
		FormAuthenticator formAuthenticator = new FormAuthenticator(authService, authUserID, authSecret);
		client.registerClientRequestFilter(formAuthenticator);
	}

	/**
	 * Helper method to register a JWTAuthenticator
	 * 
	 * @param client
	 * @throws AuthException
	 */
	private void registerJWTAuthenticator(AbstractClient client) throws AuthException {
		// build default jwt token
		SecretKey secretKey = HMAC.createKey("HmacSHA256", authSecret.getBytes());
		String payload = "{\"sub\":\"" + authUserID + "\",\"displayname\":\"" + authUserID
				+ "\",\"groups\":[\"IMIXS-WORKFLOW-Manager\"]}";

		System.out.println("Payload=" + payload);
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

}

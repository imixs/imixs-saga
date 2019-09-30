package org.imixs.microservice.core.auth;

import java.util.logging.Logger;

import javax.crypto.SecretKey;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.jwt.HMAC;
import org.imixs.jwt.JWTBuilder;
import org.imixs.jwt.JWTException;
import org.imixs.melman.JWTAuthenticator;

/**
 * This is the default JWT Authenticator used by the Imixs-Microservice API to
 * register a service at the Imixs-Registry.
 * <p>
 * The DefaultAuth module can be replace by custom implementation. Therefore the
 * environment variable imixs.registry.auth.jwt.disable.default must be set to
 * 'false'
 * 
 * @author rsoika
 *
 */
public class DefaultJWTAuth {

	@Inject
	@ConfigProperty(name = "imixs.registry.auth.jwt.secret", defaultValue = "")
	String jwtSecret;

	@Inject
	@ConfigProperty(name = "imixs.registry.auth.jwt.disable.default", defaultValue = "false")
	boolean disabled;

	private static Logger logger = Logger.getLogger(DefaultJWTAuth.class.getName());

	/**
	 * This method registers the default JWT auth module.
	 * <p>
	 * The method creates a JWT based on a given secret with the
	 * userid='imixs-microservice'
	 * 
	 * @param authEvent - providing a melman rest client instance
	 * @throws JWTException
	 */
	public void registerRequestFilter(@Observes AuthEvent authEvent) throws JWTException {

		if (disabled) {
			logger.finest("......Default JWT Auth Module disabled!");
			return;
		}

		if (jwtSecret.isEmpty()) {
			logger.warning("Default JWT Auth Module - secret not set!");
			return;
		}

		// build default jwt token
		SecretKey secretKey = HMAC.createKey("HmacSHA256", jwtSecret.getBytes());
		String payload = "{\"sub\":\"imixs-microservice\",\"displayname\":\"Imixs-Microservice\",\"groups\":[\"IMIXS-WORKFLOW-Manager\"]}";

		System.out.println("Payload=" + payload);
		JWTBuilder builder = new JWTBuilder().setKey(secretKey).setPayload(payload);
		try {
			// create JWT Auth Modul....
			JWTAuthenticator jwtAuth = new JWTAuthenticator(builder.getToken());
			authEvent.getClient().registerClientRequestFilter(jwtAuth);

		} catch (JWTException e) {
			e.printStackTrace();
			throw new JWTException("JWT_ERROR", e.getMessage(), e);
		}

	}

}

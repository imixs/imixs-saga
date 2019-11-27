package org.imixs.microservice.security.auth;

import java.io.IOException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Client Request Filter for propagating an given authentication token.
 * 
 * @author rsoika
 */
public class PropagationAuthenticator implements ClientRequestFilter {

	private final String token;

	public PropagationAuthenticator(String authenticationToken) {
		this.token = authenticationToken;
	}

	public void filter(ClientRequestContext requestContext) throws IOException {
		MultivaluedMap<String, Object> headers = requestContext.getHeaders();
		headers.add("Authorization", token);
	}

}

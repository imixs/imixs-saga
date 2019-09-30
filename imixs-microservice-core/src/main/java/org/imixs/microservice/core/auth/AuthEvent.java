package org.imixs.microservice.core.auth;

import org.imixs.melman.AbstractClient;

/**
 * The AuthEvent provides a CDI observer pattern. The AuthEvent is fired by the
 * RegistryService EJB. An event Observer can react on this event to register a
 * authentication filter by calling the method registerClientRequestFilter.
 * <p>
 * See also the Imixs-Melman project
 * 
 * 
 * @author Ralph Soika
 * @version 1.0
 * @see org.imixs.microservice.core.AutoRegisterService
 */
public class AuthEvent {

	private AbstractClient client = null;

	public AuthEvent(AbstractClient _client) {
		super();
		setClient(_client);
	}

	public AbstractClient getClient() {
		return client;
	}

	public void setClient(AbstractClient _client) {
		this.client = _client;
	}

}

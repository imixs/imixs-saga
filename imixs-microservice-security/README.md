# Imixs-Microservice - Security

The Imixs-Microserivce security module contains HTTP authenticator classes for the inter service communictaion within the Imixs-Microservice architecture.
 
The security module can be bundled with a Imixs-Microserice or the Imixs-Registry by the following Maven dependency:

	<dependency>
		<groupId>org.imixs.workflow</groupId>
		<artifactId>imixs-microservice-security</artifactId>
		<version>${imixs.microservice.version}</version>
	</dependency> 



### Authentication

To register a Imixs-Microservice at the Imixs-Registry Service, the Imixs-Microservice need to authenticate itself with an appropriate auth method. 
The imixs-microservice-core module supports different Authencation Methods which can be chousn by configuration properties:

    IMIXS_REGISTRY_AUTH_METHOD:  auth method (BASIC|FORM|JWT|CUSTOM)
    IMIXS_REGISTRY_AUTH_SECRET:  user password or jwt secret
    IMIXS_REGISTRY_AUTH_USERID:  userId
    IMIXS_REGISTRY_AUTH_SERVICE: Service endpoint for Form based authentication or Custom implementations


Depending on the auth method (BASIC,FORM,JWT) the corresponding authenticator filter is chosen per default. The properties _imixs.registry.auth.secret_ and _imixs.registry.auth.userid_ can be used to set password and userid.

In case the auth method is not defined or set to 'CUSTOM' no default filter is chosen. In this case a custom implementation can observe the following CDI Event:
 

	org.imixs.microservice.core.auth.AuthEvent

The event can be used to register a custom RequestFilter:

	public void registerRequestFilter(@Observes AuthEvent authEvent) {
		// create a custom RequestFilter
		ClientRequestFilter filter=....
		// register auth filter
		authEvent.getClient().registerClientRequestFilter(filter);
	}

	
	
	
/*******************************************************************************
 *  Imixs Workflow 
 *  Copyright (C) 2001, 2011 Imixs Software Solutions GmbH,  
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
 *  	http://www.imixs.org
 *  	http://java.net/projects/imixs-workflow
 *  
 *  Contributors:  
 *  	Imixs Software Solutions GmbH - initial API and implementation
 *  	Ralph Soika - Software Developer
 *******************************************************************************/

package org.imixs.registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.xml.XMLDataCollectionAdapter;
import org.imixs.workflow.xml.XMLDocument;
import org.imixs.workflow.xml.XMLDocumentAdapter;

/**
 * This api endpoint provides methods to registry an Imixs-Microservice. The
 * endpoint provides a GET method to list all registered services and a POST
 * method to register a new Imixs-Microserivce. The Imixs-Microservice core api
 * provides the EJB 'RegistrySelfRegistrationService' which will automatically
 * register a Imixs-Microservice on startup if the property 'imixs.registry.api'
 * is set.
 * <p>
 * The client must have Manager access to be allowed to use this service.
 * 
 * @author rsoika
 *
 */
@Path("/services")
@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.TEXT_XML })
@Stateless
public class RegistryService {

	public static final String ITEM_API = "$api";

	@javax.ws.rs.core.Context
	private HttpServletRequest servletRequest;

	private static Logger logger = Logger.getLogger(RegistryService.class.getName());

	private Map<String, ItemCollection> serviceRegistry = new ConcurrentHashMap<String, ItemCollection>();

	
	/**
	 * Returns all registered service API endpoints
	 * @return
	 */
	public Set<String> getServices() {
		return serviceRegistry.keySet();
	}
	
	
	/**
	 * Returns all registered service definitions
	 * @return
	 */
	public Collection<ItemCollection> getServiceDefinitions() {
		return serviceRegistry.values();
	}


	/**
	 * Retuns a list of all registered services
	 * 
	 * @return
	 */
	@GET
	@Path("/")
	public Response listServices(@QueryParam("format") String format) {
		Collection<ItemCollection> serviceDefinitons = serviceRegistry.values();

		return convertResultList(serviceDefinitons, format);

	}

	/**
	 * This method registers a Imixs-Microservice provided in xml document
	 * description.
	 * <p>
	 * The document can contain items with a regex to define a matcher object.
	 * <p>
	 * The item '$api' is mandatory and must contain a valid service api endpoint
	 * accessible form the Imixs-Registry service.
	 * 
	 * @param xmlworkitem
	 *            - service description to be registered.
	 * @return status
	 */
	@POST
	@Path("/")
	@Produces(MediaType.APPLICATION_XML)
	@Consumes({ MediaType.APPLICATION_XML, "text/xml" })
	public Response registerService(XMLDocument xmlworkitem) {
		if (servletRequest.isUserInRole("org.imixs.ACCESSLEVEL.MANAGERACCESS") == false) {
			return Response.status(Response.Status.UNAUTHORIZED).build();
		}
		ItemCollection workitem;
		workitem = XMLDocumentAdapter.putDocument(xmlworkitem);

		if (workitem == null) {
			return Response.status(Response.Status.NOT_ACCEPTABLE).build();
		}

		String serviceEndpoint = workitem.getItemValueString(ITEM_API);
		if (serviceEndpoint.isEmpty()) {
			logger.severe("Invalid service registration. Service description must at least provide the item '"
					+ ITEM_API + "'");
			return Response.status(Response.Status.NOT_ACCEPTABLE).build();
		}

		serviceRegistry.put(serviceEndpoint, workitem);
		return Response.ok(XMLDataCollectionAdapter.getDataCollection(workitem), MediaType.APPLICATION_XML).build();
	}

	/**
	 * This method converts a ItemCollection List into a Jax-rs response object.
	 * <p>
	 * The method expects optional items and format string (json|xml)
	 * <p>
	 * In case the result set is null, than the method returns an empty collection.
	 * 
	 * @param result
	 *            list of ItemCollection
	 * @param items
	 *            - optional item list
	 * @param format
	 *            - optional format string (json|xml)
	 * @return jax-rs Response object.
	 */
	private Response convertResultList(Collection<ItemCollection> result, String format) {
		if (result == null) {
			result = new ArrayList<ItemCollection>();
		}
		if ("json".equals(format)) {
			return Response
					// Set the status and Put your entity here.
					.ok(XMLDataCollectionAdapter.getDataCollection(result))
					// Add the Content-Type header to tell Jersey which format it should marshall
					// the entity into.
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).build();
		} else if ("xml".equals(format)) {
			return Response
					// Set the status and Put your entity here.
					.ok(XMLDataCollectionAdapter.getDataCollection(result))
					// Add the Content-Type header to tell Jersey which format it should marshall
					// the entity into.
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML).build();
		} else {
			// default header param
			return Response
					// Set the status and Put your entity here.
					.ok(XMLDataCollectionAdapter.getDataCollection(result)).build();
		}
	}

}
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
import java.util.List;
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
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.xml.XMLDataCollectionAdapter;
import org.imixs.workflow.xml.XMLDocument;
import org.imixs.workflow.xml.XMLDocumentAdapter;

/**
 * This api endpoint provides method to registry a Imixs-Microservice. The
 * endpoint provides a GET method to list all registered services and a POST
 * method to register a new Imixs-Microserivce. The Imixs-Microservice core api
 * provicdes the EJB 'AutoRegisterService' which will automatically register a
 * Imixs-Microservice on startup if the property
 * 'imixs.registry.serviceendpoint' is set.
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

	public static final String ITEM_SERVICEENDPOINT = "$serviceendpoint";
	
	@javax.ws.rs.core.Context
	private HttpServletRequest servletRequest;

	private static Logger logger = Logger.getLogger(RegistryService.class.getName());

	private Set<String> serviceRegistry = ConcurrentHashMap.newKeySet();

	/**
	 * Retuns a list of all registered services
	 * 
	 * @return
	 */
	@GET
	@Path("/")
	public GenericEntity<List<String>> helloWorld() {
		List<String> list = new ArrayList<String>();
		for (String service : serviceRegistry) {
			// result=result+"service endpoint: " + service + " ";
			list.add(service);
		}
		return new GenericEntity<List<String>>(list) {
		};
	}

	/**
	 * This method registers a Imixs-Microservice provided in xml document
	 * description.
	 * <p>
	 * The document can contain any items with regex to define a matcher object.
	 * <p>
	 * The item '$serviceendpoint' is mandatory and must contain a valid service
	 * endpoint URI accessible form the Imixs-Registry service.
	 * 
	 * @param xmlworkitem - service description to be registered.
	 * @return the request document 
	 */
	@POST
	@Path("/")
	@Produces(MediaType.APPLICATION_XML)
	@Consumes({ MediaType.APPLICATION_XML, "text/xml" })
	public Response putJob(XMLDocument xmlworkitem) {
		if (servletRequest.isUserInRole("org.imixs.ACCESSLEVEL.MANAGERACCESS") == false) {
			return Response.status(Response.Status.UNAUTHORIZED).build();
		}
		ItemCollection workitem;
		workitem = XMLDocumentAdapter.putDocument(xmlworkitem);

		if (workitem == null) {
			return Response.status(Response.Status.NOT_ACCEPTABLE).build();
		}

		if (workitem.getItemValueString(ITEM_SERVICEENDPOINT).isEmpty()) {
			logger.severe("Invalid service registration. Service description must at least provide the item '"
					+ ITEM_SERVICEENDPOINT + "'");
			return Response.status(Response.Status.NOT_ACCEPTABLE).build();
		}

		serviceRegistry.add(workitem.getItemValueString(ITEM_SERVICEENDPOINT));
		return Response.ok(XMLDataCollectionAdapter.getDataCollection(workitem), MediaType.APPLICATION_XML)
				.build();
	}

}
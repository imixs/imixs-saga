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

package org.imixs.registry.api;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Singleton;
import javax.enterprise.event.Event;
import javax.inject.Inject;
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

import org.imixs.microservice.core.auth.AuthEvent;
import org.imixs.registry.DiscoveryService;
import org.imixs.registry.RegistryService;
import org.imixs.registry.index.SearchService;
import org.imixs.workflow.FileData;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.bpmn.BPMNModel;
import org.imixs.workflow.bpmn.BPMNParser;
import org.imixs.workflow.xml.XMLDataCollection;
import org.imixs.workflow.xml.XMLDataCollectionAdapter;

/**
 * This api endpoint provides methods to registry an Imixs-Microservice. The
 * endpoint provides a GET method to list all registered services and a POST
 * method to register a new Imixs-Microserivce. The Imixs-Microservice core api
 * provides the EJB 'RegistrySelfRegistrationService' which will automatically
 * register a Imixs-Microservice on startup if the property 'imixs.registry.api'
 * is set.
 * <p>
 * The service provides the API Resource /workflow to POST a BusinessEvent
 * (ItemCollection)
 * <p>
 * The client must have Manager access to be allowed to use this service.
 * <p>
 * Model Versions are ambiguous. It is not allowed to register a model version
 * with different api endpoints.
 * 
 * @author rsoika
 *
 */
@Path("/")
@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.TEXT_XML })
@Singleton
public class RegistryRestService {

	@javax.ws.rs.core.Context
	private HttpServletRequest servletRequest;

	private static Logger logger = Logger.getLogger(RegistryRestService.class.getName());

	@Inject
	protected RegistryService registrationService;

	@Inject
	protected DiscoveryService discoveryService;

	@Inject
	protected SearchService searchService;

	@Inject
	protected Event<AuthEvent> authEvents;

	/**
	 * Retuns a list of all registered service definitions in a XML format
	 * 
	 * @return
	 */
	@GET
	@Path("services/")
	public Response listServices(@QueryParam("format") String format) {

		List<ItemCollection> result = new ArrayList<ItemCollection>();
		Set<String> services = registrationService.getServices();
		for (String service : services) {
			List<BPMNModel> _modelList = registrationService.getModelByService(service);

			for (BPMNModel amodel : _modelList) {
				ItemCollection def = new ItemCollection();
				def.setItemValue(RegistryService.ITEM_API, service);

				def.model(amodel.getVersion());
				def.setItemValue("$workflowgroups", amodel.getGroups());
				result.add(def);
			}

		}
		return convertResultList(result, format);
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
	@Path("services/")
	@Produces(MediaType.APPLICATION_XML)
	@Consumes({ MediaType.APPLICATION_XML, "text/xml" })
	public Response registerService(XMLDataCollection xmlDataCollection) {
		long l = System.currentTimeMillis();
		boolean debug = logger.isLoggable(Level.FINE);

		if (servletRequest.isUserInRole("org.imixs.ACCESSLEVEL.MANAGERACCESS") == false) {
			return Response.status(Response.Status.UNAUTHORIZED).build();
		}

		if (xmlDataCollection == null) {
			return Response.status(Response.Status.NOT_ACCEPTABLE).build();
		}

		List<ItemCollection> modelDefinitions = XMLDataCollectionAdapter.putDataCollection(xmlDataCollection);

		if (debug) {
			logger.finest("...receifed " + modelDefinitions.size() + " model definitions.");
		}
		for (ItemCollection modelEntity : modelDefinitions) {

			String serviceEndpoint = modelEntity.getItemValueString(RegistryService.ITEM_API);
			if (serviceEndpoint.isEmpty()) {
				logger.severe("Invalid service registration. model description does not contain an api endpoint '"
						+ RegistryService.ITEM_API + "'");
				return Response.status(Response.Status.NOT_ACCEPTABLE).build();
			}

			BPMNModel model = getModelFromModelEntity(modelEntity);
			if (model != null) {
				// test if model version is ambiguous...
				String _service = registrationService.getServiceByModelVersion(model.getVersion());
				if (_service != null && !serviceEndpoint.equals(_service)) {
					logger.severe("Invalid service registration. ModelVersion is ambiguous for api endpoint '"
							+ _service + "'");
					return Response.status(Response.Status.NOT_ACCEPTABLE).build();
				}
				if (debug) {
					logger.fine("... add model '" + model.getVersion() + "' at service: " + serviceEndpoint);
				}
				registrationService.setModelByService(serviceEndpoint, model);
			}
		}
		if (debug) {
			logger.fine("......registered " + modelDefinitions.size() + " model definitions in "
					+ (System.currentTimeMillis() - l) + "ms....");
		}
		return Response.ok().build();
	}

	private BPMNModel getModelFromModelEntity(ItemCollection modelEntity) {
		List<FileData> files = modelEntity.getFileData();
		boolean debug = logger.isLoggable(Level.FINE);

		for (FileData file : files) {
			if (debug) {
				logger.finest("......loading file:" + file.getName());
			}
			byte[] rawData = file.getContent();
			InputStream bpmnInputStream = new ByteArrayInputStream(rawData);
			try {
				BPMNModel model = BPMNParser.parseModel(bpmnInputStream, "UTF-8");
				return model;

			} catch (Exception e) {
				logger.warning("Failed to load model '" + file.getName() + "' : " + e.getMessage());
			}
		}
		return null;
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
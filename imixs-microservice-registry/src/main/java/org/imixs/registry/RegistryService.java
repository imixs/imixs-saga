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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
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

import org.imixs.workflow.FileData;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Model;
import org.imixs.workflow.bpmn.BPMNModel;
import org.imixs.workflow.bpmn.BPMNParser;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
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
 * The client must have Manager access to be allowed to use this service.
 * <p>
 * Model Versions are ambiguous. It is not allowed to register a model version
 * with different api endpoints.
 * 
 * @author rsoika
 *
 */
@Path("/services")
@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.TEXT_XML })
@Singleton
public class RegistryService {

	public static final String ITEM_API = "$api";

	private Map<String, BPMNModel> modelStore = null;

	@javax.ws.rs.core.Context
	private HttpServletRequest servletRequest;

	private static Logger logger = Logger.getLogger(RegistryService.class.getName());

	private Map<String, BPMNModel> serviceRegistry = new ConcurrentHashMap<String, BPMNModel>();

	/**
	 * This method initializes a new modelStore
	 * 
	 * @return
	 */
	@PostConstruct
	void init() {
		modelStore = new TreeMap<String, BPMNModel>();
	}

	/**
	 * Returns all registered service API endpoints
	 * 
	 * @return
	 */
	public Set<String> getServices() {
		return serviceRegistry.keySet();
	}

	
	/**
	 * Returns all registered models
	 * 
	 * @return 
	 */
	public Collection<BPMNModel> getModels() {
		return serviceRegistry.values();
	} 

	
	
	/**
	 * Returns all registered service definitions as ItemCollections containing the
	 * API endpoint and the associated modelVersions
	 * 
	 * @return
	 */
	// public Collection<Model> getServiceDefinitions() {
	// return serviceRegistry.values();
	// }

	/**
	 * Retuns a list of all registered service definitions in a XML format
	 * 
	 * @return
	 */
	@GET
	@Path("/")
	public Response listServices(@QueryParam("format") String format) {

		List<ItemCollection> result = new ArrayList<ItemCollection>();
		Set<String> services = getServices();
		for (String service : services) {
			ItemCollection def = new ItemCollection();
			def.setItemValue(ITEM_API, service);
			Model model = serviceRegistry.get(service);
			def.model(model.getVersion());
			def.setItemValue("$workflowgroups", model.getGroups());
			result.add(def);
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
	@Path("/")
	@Produces(MediaType.APPLICATION_XML)
	@Consumes({ MediaType.APPLICATION_XML, "text/xml" })
	public Response registerService(XMLDataCollection xmlDataCollection) {
		long l = System.currentTimeMillis();
		if (servletRequest.isUserInRole("org.imixs.ACCESSLEVEL.MANAGERACCESS") == false) {
			return Response.status(Response.Status.UNAUTHORIZED).build();
		}

		if (xmlDataCollection == null) {
			return Response.status(Response.Status.NOT_ACCEPTABLE).build();
		}

		List<ItemCollection> modelDefinitions = XMLDataCollectionAdapter.putDataCollection(xmlDataCollection);
		for (ItemCollection modelEntity : modelDefinitions) {

			String serviceEndpoint = modelEntity.getItemValueString(ITEM_API);
			if (serviceEndpoint.isEmpty()) {
				logger.severe("Invalid service registration. model description does not contain an api endpoint '"
						+ ITEM_API + "'");
				return Response.status(Response.Status.NOT_ACCEPTABLE).build();
			}

			BPMNModel model = getModelFromModelEntity(modelEntity);
			if (model != null) {
				// test if model version is ambiguous...
				String _service = getServiceByModelVersion(model.getVersion());
				if (!serviceEndpoint.equals(_service)) {
					logger.severe("Invalid service registration. ModelVersion is ambiguous for api endpoint '"
							+ _service + "'");
					return Response.status(Response.Status.NOT_ACCEPTABLE).build();
				}
				serviceRegistry.put(serviceEndpoint, model);
			}
		}
		logger.finest("......parsed " + modelDefinitions.size() + " model entities in "
				+ (System.currentTimeMillis() - l) + "ms....");

		return Response.ok().build();
	}

	/**
	 * This method returns a service endpoint for a given model version or null if
	 * no service with this model version is registered.
	 * 
	 * @param version
	 * @return
	 */
	public String getServiceByModelVersion(String version) {
		if (version == null || version.isEmpty()) {
			return null;
		}

		for (Map.Entry<String, BPMNModel> entry : serviceRegistry.entrySet()) {
			String service = entry.getKey();
			Model model = entry.getValue();
			if (model.getVersion().contentEquals(version)) {
				return service;
			}
		}
		// no service found!
		return null;
	}

	/**
	 * This method removes a specific ModelVersion form the internal model store. If
	 * modelVersion is null the method will remove all models. The model will not be
	 * removed from the database. Use deleteModel to delete the model from the
	 * database.
	 * 
	 * @throws AccessDeniedException
	 */
	public void removeModel(String modelversion) {
		modelStore.remove(modelversion);
		logger.finest("......removed BPMNModel '" + modelversion + "'...");
	}

	/**
	 * Returns a Model by version. In case no matching model version exits, the
	 * method throws a ModelException.
	 **/
	public Model getModel(String version) throws ModelException {
		Model model = modelStore.get(version);
		if (model == null) {
			throw new ModelException(ModelException.UNDEFINED_MODEL_VERSION,
					"Modelversion '" + version + "' not found!");
		}
		return model;
	}

	/**
	 * This method returns a sorted list of model versions matching a given regex
	 * for a model version. The result is sorted in reverse order, so the highest
	 * version number is the first in the result list.
	 * 
	 * @param modelRegex
	 * @return
	 */
	public List<String> findVersionsByRegEx(String modelRegex) {
		List<String> result = new ArrayList<String>();
		logger.finest("......searching model versions for regex '" + modelRegex + "'...");
		// try to find matching model version by regex
		Collection<BPMNModel> models = modelStore.values();
		for (Model amodel : models) {
			if (Pattern.compile(modelRegex).matcher(amodel.getVersion()).find()) {
				result.add(amodel.getVersion());
			}
		}
		// sort result
		Collections.sort(result, Collections.reverseOrder());
		return result;
	}
	
	
	

	/**
	 * This method returns a sorted list of model versions matching a given workflow group.
	 * 
	 * @param group
	 * @return
	 */
	public List<String> findModelsByGroup(String group) {
		List<String> result = new ArrayList<String>();
		logger.finest("......searching model versions for group '" + group + "'...");
		// try to find matching model version by regex
		Collection<BPMNModel> models = modelStore.values();
		for (Model amodel : models) {
			
			List<String> groupList = amodel.getGroups();
			if (groupList.contains(group)) {
				result.add(amodel.getVersion());
			}
		}
		// sort result
		Collections.sort(result, Collections.reverseOrder());
		return result;
	}
	
	

	private BPMNModel getModelFromModelEntity(ItemCollection modelEntity) {
		List<FileData> files = modelEntity.getFileData();

		for (FileData file : files) {
			logger.finest("......loading file:" + file.getName());
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
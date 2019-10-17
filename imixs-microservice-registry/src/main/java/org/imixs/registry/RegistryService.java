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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.servlet.http.HttpServletRequest;

import org.imixs.workflow.Model;
import org.imixs.workflow.bpmn.BPMNModel;
import org.imixs.workflow.exceptions.ModelException;

/**
 * This RegistryService provides methods to manage the service registry and its models.
 *
 * @author rsoika
 * @version 1.0
 */
@Singleton
public class RegistryService {

	public static final String ITEM_API = "$api";

	//private Map<String, BPMNModel> modelStore = null;

	@javax.ws.rs.core.Context
	private HttpServletRequest servletRequest;

	private static Logger logger = Logger.getLogger(RegistryService.class.getName());

	private Map<String, BPMNModel> serviceRegistry = new ConcurrentHashMap<String, BPMNModel>();

	

	/**
	 * Returns all registered service API endpoints
	 * 
	 * @return
	 */
	public Set<String> getServices() {
		return serviceRegistry.keySet();
	}

	/**
	 * Returns the model by a service
	 * @param service
	 * @return
	 */
	public BPMNModel getModelByService(String service) {
		BPMNModel model = serviceRegistry.get(service);
		return model;
	}
	
	/**
	 * Puts a model into the registry
	 * @param serviceEndpoint
	 * @param model
	 */
	public void setModelByService(String serviceEndpoint, BPMNModel model) {
		serviceRegistry.put(serviceEndpoint, model);
	}
	
	/**
	 * Returns a sorted list of all registered models
	 * 
	 * @return 
	 */
	public List<BPMNModel> getModels() {
		List<BPMNModel> result=new ArrayList<BPMNModel>();
		result.addAll(serviceRegistry.values());
		
		
		// sort result by model version 
		
		
		
		
		Collections.sort(result, 
                (o1, o2) -> o1.getVersion().compareTo(o2.getVersion()));
		
		
		
		
		
		
		return result;
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
	 * Returns a Model by version. In case no matching model version exits, the
	 * method throws a ModelException.
	 **/
	public BPMNModel getModel(String version) throws ModelException {
		
		Collection<BPMNModel> models = serviceRegistry.values();
		for (BPMNModel _model: models) {
			if (_model.getVersion().equals(version)) {
				return _model;
			}
		}
		
			throw new ModelException(ModelException.UNDEFINED_MODEL_VERSION,
					"Modelversion '" + version + "' not found!");
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
		Collection<BPMNModel> models = serviceRegistry.values();
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
		Collection<BPMNModel> models = serviceRegistry.values();
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
	
	


	

}
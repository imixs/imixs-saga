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

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.imixs.workflow.BPMNRuleEngine;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Model;
import org.imixs.workflow.bpmn.BPMNModel;
import org.imixs.workflow.exceptions.ModelException;

/**
 * The DiscoveryService supports methods to start a new process instance based
 * on a given business event. The service discovery process automatically
 * selects a matching workflow service based on the data provided with the
 * business event and starts a new process instance.
 * <p>
 * The service supports the following discovery modes
 * <ul>
 * <li>Service Discover by Modelversion and Start Event</li>
 * <li>Service Discover by Workflow Group and Start Event</li>
 * <li>Service Discover by Workflow Group only</li>
 * <li>Service Discover by Business Rule</li>
 * </ul>
 * <p>
 * The methdod discoverService starts the process.
 * 
 * @author Ralph Soika
 * @version 1.0
 */

@DeclareRoles({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RolesAllowed({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Stateless
@LocalBean
public class DiscoveryService {

	@Inject
	protected RegistryService registryService;

	private static Logger logger = Logger.getLogger(DiscoveryService.class.getName());

	/**
	 * The method discovers a service based on the data of a businessEvent. If a
	 * service was found the item $api will contain the service endpoint.
	 * 
	 * @param businessEvent
	 */
	public void discoverService(ItemCollection businessEvent) {
		long l = System.currentTimeMillis();
		if (businessEvent == null) {
			return;
		}
		// remove $api
		businessEvent.removeItem(RegistryService.ITEM_API);

		try {
			// 1) $modelversion provided
			if (discoverServiceByModelVersion(businessEvent)) {
				logger.info("......service disvovery completed in " + (System.currentTimeMillis() - l) + "ms");
				return;
			}
			// 2) $workflowgroup provided
			if (discoverServiceByModelGroup(businessEvent)) {
				logger.info("......service disvovery completed in " + (System.currentTimeMillis() - l) + "ms");
				return;
			}

			// 3) disovery by rule (default)
			discoverServiceByRule(businessEvent);

		} catch (ModelException e) {
			logger.warning(e.getErrorCode() + " " + e.getMessage());
		}

		logger.info("......service disvovery completed in " + (System.currentTimeMillis() - l) + "ms");
	}

	/**
	 * This method discovers a service by a model version.
	 * <p>
	 * In case a service was found, the method set the item $api and returns true.
	 * <p>
	 * In case no model version was provided or no appropriate service matches the
	 * business event, the method returns false.
	 * 
	 * @param businessEvent
	 * @return
	 * @throws ModelException
	 */
	private boolean discoverServiceByModelVersion(ItemCollection businessEvent) throws ModelException {
		String service = null;
		String modelVersion = businessEvent.getModelVersion();

		if (modelVersion.isEmpty()) {
			return false;
		}

		service = registryService.getServiceByModelVersion(modelVersion);
		if (service != null && !service.isEmpty()) {
			businessEvent.setItemValue(RegistryService.ITEM_API, service);
			/*
			 * We assume that the $taskid and $eventid is correctly set and did not perform
			 * a validation here for performance reasons. In case the taskID or eventId is
			 * invalid the endpoint will throw an exception. This is a configuration issue.
			 */
			return true;
		}

		// We did not find a service. So we do search by regex....
		List<String> versions = registryService.findVersionsByRegEx(modelVersion);
		if (versions.size() == 1) {
			// exactly one model
			service = registryService.getServiceByModelVersion(versions.get(0));
			businessEvent.setItemValue(RegistryService.ITEM_API, service);
			/*
			 * We assume that the $taskid and $eventid is correctly set and did not perform
			 * a validation here for performance reasons. In case the taskID or eventId is
			 * invalid the endpoint will throw an exception. This is a configuration issue.
			 */
			return true;
		}

		if (versions.size() > 1) {
			// we found more than one maching model version. So we compare the TaskID and
			// EventID
			for (String version : versions) {
				Model model;

				model = registryService.getModel(version);
				// test if taskid and eventid matches....
				ItemCollection task = model.getTask(businessEvent.getTaskID());
				if (task != null) {
					ItemCollection event = model.getEvent(businessEvent.getTaskID(), businessEvent.getEventID());
					if (event != null) {
						// match!
						service = registryService.getServiceByModelVersion(version);
						businessEvent.setItemValue(RegistryService.ITEM_API, service);
						return true;
					}
				}
				// no match of task/event !
			}
			logger.warning(
					"Invalid model expression '" + modelVersion + "' did not match any registered model version!");

		}

		// no service found!
		return false;

	}

	/**
	 * This method discovers a service by a workflowGroup.
	 * <p>
	 * In case a service was found, the method set the item $api and returns true.
	 * <p>
	 * In case no model version was provided or no appropriate service matches the
	 * business event, the method returns false.
	 * 
	 * @param businessEvent
	 * @return
	 * @throws ModelException
	 */
	private boolean discoverServiceByModelGroup(ItemCollection businessEvent) throws ModelException {
		String service = null;
		String worfklowGroup = businessEvent.getWorkflowGroup();

		// We did not find a service. So we do search by regex....
		List<String> versions = registryService.findModelsByGroup(worfklowGroup);

		if (versions.size() == 0) {
			return false;
		}

		if (versions.size() == 1) {
			// exactly one model
			String modelVersion=versions.get(0);
			service = registryService.getServiceByModelVersion(versions.get(0));
			
			/*
			 * if no $taskid and $eventid is provided we compute it form the model
			 */
			if (businessEvent.getTaskID()==0) {
				
				BPMNModel model = registryService.getModel(modelVersion);
				List<ItemCollection> startTasks = model.getStartTasks();
				if (startTasks==null || startTasks.size()==0) {
					logger.warning(
							"Invalid model '" + model.getVersion() + "' no start task found!");
					return false;
				}
				ItemCollection task = startTasks.get(0);
				
				int taskID=task.getItemValueInteger("numprocessid");
				businessEvent.setTaskID(taskID);
				if (businessEvent.getEventID() == 0) {
					// evaluate start event....
					List<ItemCollection> events = model.getStartEvents(taskID);
					if (events != null && events.size() > 0) {
						// we take the first one!
						businessEvent.setEventID(events.get(0).getItemValueInteger("numactivityid"));
					} else {
						logger.warning(
								"Invalid model '" + model.getVersion() + "' no start event defined for task " + taskID);
						return false;
					}
				}
			}
			service = registryService.getServiceByModelVersion(modelVersion);
			businessEvent.setItemValue(RegistryService.ITEM_API, service);
			
			return true;
		}

		if (versions.size() > 1) {
			// we found more than one matching model version. So we compare the TaskID and
			// EventID
			for (String version : versions) {
				Model model;

				model = registryService.getModel(version);
				// test if taskid and eventid matches....
				ItemCollection task = model.getTask(businessEvent.getTaskID());
				if (task != null) {
					ItemCollection event = model.getEvent(businessEvent.getTaskID(), businessEvent.getEventID());
					if (event != null) {
						// match!
						service = registryService.getServiceByModelVersion(version);
						businessEvent.setItemValue(RegistryService.ITEM_API, service);
						return true;
					}
				}
				// no match of task/event !
			}
			logger.warning("ambiguous workflowGroup '" + worfklowGroup
					+ "' did not match any registered model version with provided $taskid!");

		}

		// no service found!
		return false;
	}

	/**
	 * This method discovers a service by a rule.
	 * <p>
	 * In case a service was found, the method set the item $api, $modelversion,
	 * $eventid and returns true.
	 * <p>
	 * In case no model version was provided or no appropriate service matches the
	 * business event, the method returns false.
	 * 
	 * @param businessEvent
	 * @return
	 * @throws ModelException
	 */
	private boolean discoverServiceByRule(ItemCollection businessEvent) throws ModelException {
		long l = System.currentTimeMillis();
		String service = null;
		BPMNRuleEngine bpmnRuleEngine = null;

		Collection<BPMNModel> models = registryService.getModels();
		for (BPMNModel model : models) {
			bpmnRuleEngine = new BPMNRuleEngine(model);

			int taskID = bpmnRuleEngine.eval(businessEvent);
			// test if this is an EndTask. If the model did not match!
			ItemCollection task = model.getTask(taskID);
			if (!task.getItemValueBoolean("endTask")) {
				businessEvent.setModelVersion(model.getVersion());
				businessEvent.setTaskID(taskID);
				if (businessEvent.getEventID() == 0) {
					// evaluate start event....
					List<ItemCollection> events = model.getStartEvents(taskID);
					if (events != null && events.size() > 0) {
						// we take the first one!
						businessEvent.setEventID(events.get(0).getItemValueInteger("numactivityid"));
					} else {
						logger.warning(
								"Invalid model '" + model.getVersion() + "' no start event defined for task " + taskID);
						continue;
					}

				}
				service = registryService.getServiceByModelVersion(model.getVersion());
				businessEvent.setItemValue(RegistryService.ITEM_API, service);
				logger.info("......discoverd Service by rule in " + (System.currentTimeMillis() - l) + "ms");
				return true;
			}
		}
		return false;
	}
}

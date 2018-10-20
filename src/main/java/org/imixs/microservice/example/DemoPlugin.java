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

package org.imixs.microservice.example;

import java.util.List;
import java.util.logging.Logger;

import javax.ejb.EJB;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.engine.ModelService;
import org.imixs.workflow.engine.plugins.AbstractPlugin;
import org.imixs.workflow.exceptions.PluginException;

/**
 * This Plugin demonstrates the CDI behavior of an imixs workflow pugin.
 * 
 * @author Ralph Soika
 * @version 1.0
 *
 */

public class DemoPlugin extends AbstractPlugin {

	// inject services...
	@EJB
	ModelService modelService;

	private static Logger logger = Logger.getLogger(DemoPlugin.class.getName());

	public void init(WorkflowContext actx) throws PluginException {
		// here we can initialize optional components 
	}

	public ItemCollection run(ItemCollection documentContext, ItemCollection adocumentActivity)
			throws PluginException {

		// test model service
		List<String> versions = modelService.getVersions();
		for (String aversion : versions) {
			logger.info("ModelVersion found: " + aversion);
		}
		
		// add a custom item
		documentContext.setItemValue("_some_item", "Hello World");
	
		return documentContext;
	}

	@Override
	public void close(boolean rollbackTransaction) throws PluginException {
		// here we can react on a rollback transaction
	}

}

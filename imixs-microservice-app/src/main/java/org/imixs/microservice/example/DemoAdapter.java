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
import org.imixs.workflow.SignalAdapter;
import org.imixs.workflow.engine.ModelService;
import org.imixs.workflow.exceptions.AdapterException;

/**
 * This adapter class demonstrates the CDI behavior of an imixs workflow adapter.
 * 
 * @author Ralph Soika
 * @version 1.0
 *
 */

public class DemoAdapter implements SignalAdapter {

	// inject services...
	@EJB
	ModelService modelService;

	private static Logger logger = Logger.getLogger(DemoAdapter.class.getName());


	

	@Override
	public ItemCollection execute(ItemCollection document, ItemCollection event) throws AdapterException {

		logger.info("...running demo adapter...");
		// test model service
		List<String> versions = modelService.getVersions();
		for (String aversion : versions) {
			logger.info("ModelVersion found: " + aversion);
		}

		return document;
	}

}

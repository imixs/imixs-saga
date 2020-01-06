/*******************************************************************************
 * <pre>
 *  Imixs Workflow 
 *  Copyright (C) 2001-2020 Imixs Software Solutions GmbH,  
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
 *      https://www.imixs.org
 *      https://github.com/imixs/imixs-workflow
 *  
 *  Contributors:  
 *      Imixs Software Solutions GmbH - initial API and implementation
 *      Ralph Soika - Software Developer
 * </pre>
 *******************************************************************************/

package org.imixs.microservice.batch;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.imixs.workflow.GenericAdapter;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.EventLogService;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.exceptions.AdapterException;
import org.imixs.workflow.exceptions.PluginException;

/**
 * This generic adapter class creates a batch event based on the current event workflow result.
 * 
 * Example:
 * 
 * <code>
			<item name="batch.event.id">[EVENT_ID]</item>
   </code>
 * 
 * The batch.event.id is the next bpmn event fired asynchrony by the BatchEventProcessor
 * 
 * @author rsoika
 *
 */
public class BatchEventAdapter implements GenericAdapter {

  public static final String BATCH_EVENT_ID = "batch.event.id";

  @Inject
  WorkflowService workflowService;

  @Inject
  EventLogService eventLogService;

  private static Logger logger = Logger.getLogger(BatchEventAdapter.class.getName());

  @Override
  public ItemCollection execute(ItemCollection document, ItemCollection event)
      throws AdapterException {
    try {
      boolean debug = logger.isLoggable(Level.FINE);
      // Check the txtActivityResult for a batch-event
      ItemCollection evalItemCollection = workflowService.evalWorkflowResult(event, document);
      if (evalItemCollection == null) {
        return document;
      }

      // test for a batch event ....
      if (evalItemCollection.hasItem(BATCH_EVENT_ID)) {
        int batchEventID = evalItemCollection.getItemValueInteger(BATCH_EVENT_ID);
        if (debug) {
          logger.finest("...create new batch event - eventId=" + batchEventID);
        }
        // ceate EventLogEntry....
        ItemCollection batchData = new ItemCollection().event(batchEventID);
        eventLogService.createEvent(BatchEventService.EVENTLOG_TOPIC_BATCH_EVENT,
            document.getUniqueID(), batchData);
      }

    } catch (PluginException e) {
      throw new AdapterException(e.getErrorContext(), e.getErrorCode(), e.getMessage(), e);
    }

    return document;
  }

}

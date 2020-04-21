/*  
 *  Imixs-Workflow 
 *  
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
 *      Imixs Software Solutions GmbH - Project Management
 *      Ralph Soika - Software Developer
 */

package org.imixs.microservice.batch;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJBException;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceContext;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.EventLogService;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.engine.jpa.EventLog;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.WorkflowException;

/**
 * The BatchEventService can be used to process workflow events in an
 * asynchronous batch process. The BatchEventService lookup eventLog entries of
 * the topic "batch.event". Those eventLog entries can be created by the
 * BatchEventAdatper by setting a corresponding workflow result:
 * <p>
 * {@code
			<item name="batch.event.id">[EVENT_ID]</item>
   }
 * <p>
 * The processor look up the workItem and starts a processing life cycle.
 * <p>
 * The BatchEventService is called onyl by the BatchEventScheduler which is
 * implementing a ManagedScheduledExecutorService.
 * <p>
 * To prevent concurrent processes to handle the same workitems the batch
 * process uses a Optimistic lock strategy. After fetching new event log entries
 * the processor updates the eventLog entry in a new transaction and set the
 * topic to 'batch.process.lock'. After that update we can be sure that no other
 * process is dealing with these entries. After completing the processing step
 * the eventlog entry will be removed.
 * <p>
 * To avoid ad deadlock the processor set an expiration time on the lock, so the
 * lock will be auto-released after 1 minute (batch.processor.deadlock).
 * 
 * @see BatchEventAdapter
 * @version 1.0
 * @author rsoika
 *
 */
@DeclareRoles({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
        "org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
        "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RolesAllowed({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
        "org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
        "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Stateless
@LocalBean
public class BatchEventService {

    public static final String EVENTLOG_TOPIC_BATCH_EVENT = "batch.event";
    public static final String EVENTLOG_TOPIC_BATCH_EVENT_LOCK = "batch.event.lock";

    public static final String ITEM_BATCH_EVENT_LOCK_DATE = "batch.event.lock.date";

    @PersistenceContext(unitName = "org.imixs.workflow.jpa")
    private EntityManager manager;

    private static Logger logger = Logger.getLogger(BatchEventService.class.getName());

    @Inject
    EventLogService eventLogService;

    @Inject
    private WorkflowService workflowService;

    /**
     * The method lookups for batch event log entries and processed workitems in a
     * batch process.
     * <p>
     * Each eventLogEntry is cached in the eventCache. The cache is cleared from all
     * eventLogEntries not part of the current collection. We can assume that the
     * event was succefully processed by the ArchiveHandler
     * 
     * @throws ArchiveException
     */
    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public void processEventLog() {
        long l = System.currentTimeMillis();
        boolean debug = logger.isLoggable(Level.FINE);

        // test for new event log entries...
        List<EventLog> events = eventLogService.findEventsByTopic(100, EVENTLOG_TOPIC_BATCH_EVENT);
        for (EventLog eventLogEntry : events) {

            try {
                // first try to lock the eventLog entry....
                eventLogService.lock(eventLogEntry);

                // now load the workitem
                ItemCollection workitem = workflowService.getWorkItem(eventLogEntry.getRef());
                if (workitem != null) {
                    // process workitem....
                    try {
                        // get the batch event id....
                        ItemCollection batchData = new ItemCollection(eventLogEntry.getData());
                        workitem.setEventID(batchData.getEventID());
                        workitem = workflowService.processWorkItemByNewTransaction(workitem);

                        // finally remove the event log entry...
                        eventLogService.removeEvent(eventLogEntry.getId());
                    } catch (WorkflowException | InvalidAccessException | EJBException e) {
                        // we also catch EJBExceptions here because we do not want to cancel the
                        // ManagedScheduledExecutorService
                        logger.severe("BatchEvent " + workitem.getUniqueID() + " processing failed: " + e.getMessage());
                        // now we need to remove the batch event
                        logger.warning("BatchEvent " + workitem.getUniqueID() + " will be removed!");
                        eventLogService.removeEvent(eventLogEntry.getId());
                    }
                }

            } catch (OptimisticLockException e) {
                // lock was not possible - continue....
                logger.info("...unable to lock batch event: " + e.getMessage());
            }

        }

        if (debug) {
            logger.fine("..." + events.size() + " batchEvents processed in " + (System.currentTimeMillis() - l) + "ms");
        }
    }

  
   

}

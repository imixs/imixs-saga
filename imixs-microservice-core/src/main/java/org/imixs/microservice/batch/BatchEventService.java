package org.imixs.microservice.batch;

import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RunAs;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceContext;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.EventLogService;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.engine.jpa.EventLog;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;

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
@DeclareRoles({ "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
@Stateless
@LocalBean
public class BatchEventService {

	
	public static final String EVENTLOG_TOPIC_BATCH_EVENT = "batch.event";
	public static final String EVENTLOG_TOPIC_BATCH_EVENT_LOCK = "batch.event.lock";

	// deadlock timeout interval in ms
	@Inject
	@ConfigProperty(name = BatchEventScheduler.BATCH_PROCESSOR_DEADLOCK, defaultValue = "60000")
	long deadLockInterval;

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
	public void processEventLog() {
		long l = System.currentTimeMillis();
		// test for new event log entries...
		List<EventLog> events = eventLogService.findEventsByTopic(100, EVENTLOG_TOPIC_BATCH_EVENT);
		for (EventLog eventLogEntry : events) {

			try {
				// first try to lock the eventLog entry....
				lock(eventLogEntry);

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
					} catch (AccessDeniedException | ProcessingErrorException | PluginException | ModelException e) {
						logger.warning(
								"BatchEvent " + workitem.getUniqueID() + " processing failed: " + e.getMessage());
						// here we need to unlock the eventLog entry...
						unlock(eventLogEntry);

					}
				}

			} catch (OptimisticLockException e) {
				// lock was not possible - continue....
			}

		}

		releaseDeadLocks();
		logger.finest(
				"......" + events.size() + " batchEvents processed in " + (System.currentTimeMillis() - l) + "ms");

	}

	/**
	 * This method unlocks eventlog entries which are older than 1 minute. We assume
	 * that these events are deadlocks.
	 */
	public void releaseDeadLocks() {

		// test if we have dead locks....
		List<EventLog> events = eventLogService.findEventsByTopic(100, EVENTLOG_TOPIC_BATCH_EVENT_LOCK);
		Date now = new Date();
		for (EventLog eventLogEntry : events) {

			// test if lock.date is older than 1 minute
			ItemCollection data = new ItemCollection(eventLogEntry.getData());
			Date lockDate = data.getItemValueDate("lock.date");
			long age = now.getTime() - lockDate.getTime();
			if (lockDate == null || age > deadLockInterval) {
				logger.warning("Deadlock detected! - batch.event.lock=" + eventLogEntry.getId()
						+ " will be unlocked! (deadlock since " + age + "ms)");
				unlock(eventLogEntry);
			}
		}
	}

	/**
	 * This method locks an eventLog entry for processing. The topic will be set to
	 * 'batch.process.lock'. If the lock is successful we can process the eventLog
	 * entry.
	 * <p>
	 * The method also adds a item 'lock.date' with a timestamp. This timestamp is
	 * used by the method 'autoUnlock' to release locked entries.
	 * 
	 * @param eventLogEntry
	 * @return
	 */
	@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
	public void lock(EventLog _eventLogEntry) {
		EventLog eventLog = manager.find(EventLog.class, _eventLogEntry.getId());
		if (eventLog != null) {
			eventLog.setTopic(EVENTLOG_TOPIC_BATCH_EVENT_LOCK);
			ItemCollection data = new ItemCollection(eventLog.getData());
			data.setItemValue("lock.date", new Date());
			manager.merge(eventLog);
		}
	}

	/**
	 * This method unlocks an eventLog entry. The topic will be set to
	 * 'batch.process'.
	 * 
	 * @param eventLogEntry
	 * @return
	 */
	public void unlock(EventLog _eventLogEntry) {
		EventLog eventLog = _eventLogEntry;
		if (eventLog != null && !manager.contains(eventLog)) {
			// entity is not atached - so lookup the entity....
			eventLog = manager.find(EventLog.class, eventLog.getId());
		}
		if (eventLog != null) {
			eventLog.setTopic(EVENTLOG_TOPIC_BATCH_EVENT);
			manager.merge(eventLog);
		}
	}

}

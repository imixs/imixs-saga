package org.imixs.microservice.batch;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
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
 * The BatchEventProcessor can be used to process workflow events in an
 * asynchronous batch process. The BatchEventProcessor lookup eventLog entries
 * of the topic "batch.event". Those eventLog entries can be created by the
 * BatchEventAdatper by setting a corresponding workflow result:
 * <p>
 * {@code
			<item name="batch.event.id">[EVENT_ID]</item>
   }
 * <p>
 * The processor look up the workItem and starts a processing life cycle.
 * <p>
 * The processor runs on a ManagedScheduledExecutorService with the interval
 * 'BATCH_PROCESSOR_INTERVAL' and an optional delay defined by
 * 'BATCH_PROCESSOR_INITIALDELAY'.
 * <p>
 * To prevent concurrent processes to handle the same workitems the batch
 * process uses a Optimistic lock strategy. After fetching new event log entries
 * the processor updates the eventLog entry in a new transaction and set the
 * topic to 'batch.process.lock'. After that update we can be sure that no other
 * process is dealing with these entries. After completing the processing step
 * the eventlog entry will be removed.
 * <p>
 * To avoid ad deadlock the processor set an expiration time on the lock, so the
 * lock will be auto-released after 1 minute.
 * 
 * @see BatchEventAdapter
 * @version 1.0
 * @author rsoika
 *
 */
@Startup
@Singleton
public class BatchEventProcessor {

	public static final String BATCH_PROCESSOR_INTERVAL = "batch.processor.interval";
	public static final String BATCH_PROCESSOR_INITIALDELAY = "batch.processor.initialdelay";

	public static final String EVENTLOG_TOPIC_BATCH_EVENT = "batch.event";
	public static final String EVENTLOG_TOPIC_BATCH_EVENT_LOCK = "batch.event.lock";

	@PersistenceContext(unitName = "org.imixs.workflow.jpa")
	private EntityManager manager;

	// timeout interval in ms
	@Inject
	@ConfigProperty(name = BATCH_PROCESSOR_INTERVAL, defaultValue = "1000")
	long batchProcessorInterval;

	// initial delay in ms
	@Inject
	@ConfigProperty(name = BATCH_PROCESSOR_INITIALDELAY, defaultValue = "0")
	long initialDelay;

	private static Logger logger = Logger.getLogger(BatchEventProcessor.class.getName());

	@Resource
	ManagedScheduledExecutorService scheduler;

	@Inject
	EventLogService eventLogService;

	@Inject
	private WorkflowService workflowService;

	@PostConstruct
	public void init() {
		this.scheduler.scheduleAtFixedRate(this::processEventLog, initialDelay, batchProcessorInterval,
				TimeUnit.MILLISECONDS);
	}

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

		logger.info("TODO auto release feature is missing!");
		logger.finest(
				"......" + events.size() + " batchEvents processed in " + (System.currentTimeMillis() - l) + "ms");

	}

	/**
	 * This method locks an eventLog entry for processing. The topic will be set to
	 * 'batch.process.lock'. If the lock is successful we can process the eventLog
	 * entry
	 * 
	 * @param eventLogEntry
	 * @return
	 */
	@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
	public void lock(EventLog _eventLogEntry) {

		EventLog eventLog = manager.find(EventLog.class, _eventLogEntry.getId());
		if (eventLog != null) {
			eventLog.setTopic(EVENTLOG_TOPIC_BATCH_EVENT_LOCK);
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

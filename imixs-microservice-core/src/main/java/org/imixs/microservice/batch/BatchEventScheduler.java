package org.imixs.microservice.batch;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * The BatchEventScheduler starts a ManagedScheduledExecutorService to process
 * batch events in an asynchronous by calling the BatchEventService.
 * <p>
 * The BatchEventScheduler runs on a ManagedScheduledExecutorService with the
 * interval 'BATCH_PROCESSOR_INTERVAL' and an optional delay defined by
 * 'BATCH_PROCESSOR_INITIALDELAY'. To enable the batchPorcessor
 * 'BATCH_PROCESSOR_ENABLED' must be set to true (default=false).
 * 'BATCH_PROCESSOR_DEADLOCK' deadlock timeout
 * <p>
 *
 * @see BatchEventService
 * @version 1.0
 * @author rsoika
 *
 */
@Startup
@Singleton
public class BatchEventScheduler {

	public static final String BATCH_PROCESSOR_ENABLED = "batch.processor.enabled";
	public static final String BATCH_PROCESSOR_INTERVAL = "batch.processor.interval";
	public static final String BATCH_PROCESSOR_INITIALDELAY = "batch.processor.initialdelay";
	public static final String BATCH_PROCESSOR_DEADLOCK = "batch.processor.deadlock";


	public static final String EVENTLOG_TOPIC_BATCH_EVENT = "batch.event";
	public static final String EVENTLOG_TOPIC_BATCH_EVENT_LOCK = "batch.event.lock";

	// enabled
	@Inject
	@ConfigProperty(name = BATCH_PROCESSOR_ENABLED, defaultValue = "false")
	boolean enabled;

	// timeout interval in ms
	@Inject
	@ConfigProperty(name = BATCH_PROCESSOR_INTERVAL, defaultValue = "1000")
	long interval;

	// initial delay in ms
	@Inject
	@ConfigProperty(name = BATCH_PROCESSOR_INITIALDELAY, defaultValue = "0")
	long initialDelay;

	private static Logger logger = Logger.getLogger(BatchEventScheduler.class.getName());

	@Resource
	ManagedScheduledExecutorService scheduler;

	@Inject
	BatchEventService batchEventProcessor;

	@PostConstruct
	public void init() {
		if (enabled) {
			logger.info(
					"Starting BatchEventScheduler - initalDelay=" + initialDelay + "  inverval=" + interval + " ....");
			this.scheduler.scheduleAtFixedRate(this::run, initialDelay, interval, TimeUnit.MILLISECONDS);
		}
	}

	/**
	 * The method delecates the event processing to the stateless ejb
	 * BatchEventProcessor.
	 * 
	 * 
	 */
	public void run() {
		batchEventProcessor.processEventLog();

	}

}
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

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RunAs;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * The BatchEventScheduler starts a ManagedScheduledExecutorService to process
 * batch events in an asynchronous way by calling the BatchEventService.
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
@DeclareRoles({ "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
@Startup
@Singleton
@LocalBean
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

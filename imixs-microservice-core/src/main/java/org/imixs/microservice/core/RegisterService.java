package org.imixs.microservice.core;

import java.io.Serializable;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.workflow.WorkflowKernel;

/**
 * 
 * 
 * @version 1.0
 * @author rsoika
 * 
 */
@Startup
@Singleton
// @ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class RegisterService implements Serializable {

	@Resource
	private TimerService timerService;

	
	@Inject
	@ConfigProperty(name = "imixs.registry.serviceendpoint", defaultValue = "")
	String registryServiceEndpoint;

	@Inject
	@ConfigProperty(name = "imixs.registry.intervall", defaultValue = "120000")
	int registryIntervall;

	
	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(RegisterService.class.getName());

	private String timerID = null;

	/**
	 * On startup we just initialize a new Timer running each 10 seconds to auto
	 * register on a given imixs-registry endpoint.
	 * <p>
	 * 
	 * @see https://stackoverflow.com/questions/55640112/how-to-implement-a-permanent-background-process-with-ejbs
	 * 
	 */
	@PostConstruct
	void init() {

		timerID = WorkflowKernel.generateUniqueID();
		// do we have a imixs-registry endpoint defined?
		if (!registryServiceEndpoint.isEmpty()) {
			registerMicroservice();
		}
	}

	@PreDestroy
	void close() {
		// destroy timer
		Timer timer = findTimer();
		if (timer!=null) {
			logger.info("...unregister Imixs-Registry endpoint:  " + registryServiceEndpoint);
			logger.finest("......cancel timer: "+ timerID);
			timer.cancel();
		}
	}

	/**
	 * On the timeout event we register the service at the experience registry.
	 * 
	 */
	@Timeout
	private synchronized void onTimer() {
		logger.info("...update Imixs-Registry endpoint: " + registryServiceEndpoint);
	}

	/**
	 * This method registers the microservice at a given imixs-registry endpoint
	 */
	private void registerMicroservice() {
		logger.info("...register Imixs-Registry endpoint:  " + registryServiceEndpoint);
		
		// start timer?
		if (findTimer()==null) {
			logger.finest("......create timer: "+ timerID + " - timer intervall="  + registryIntervall + "ms");
			TimerConfig config = new TimerConfig();
			// config.set
			config.setPersistent(false);
			timerService.createIntervalTimer(0, registryIntervall, config);
		}
	}
	
	
	/**
	 * This method returns a timer for a corresponding id if such a timer object
	 * exists.
	 * 
	 * @param id
	 * @return Timer
	 * @throws Exception
	 */
	public Timer findTimer() {
		for (Object obj : timerService.getTimers()) {
			Timer timer = (javax.ejb.Timer) obj;
			if (timerID.equals(timer.getInfo())) {
				return timer;
			}
		}
		return null;
	}


}
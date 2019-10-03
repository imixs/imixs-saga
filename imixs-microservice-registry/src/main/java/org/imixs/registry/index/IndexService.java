package org.imixs.registry.index;

import java.io.Serializable;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.registry.RegistryService;
import org.imixs.workflow.WorkflowKernel;

/**
 * The Index service is a singleton EJB which maintains a solr index based on
 * event log entries provided by a Imixs-Microservice. At startup the service
 * starts a timer service to update the index in the 'imixs.index.intervall'
 * (defined in ms). This feature will ensure that the Imixs-Registry holds an
 * updated solr index
 * <p>
 * The environment variable 'solr.api' defines the solr service. If no
 * endpoint is defined (default) no index will be written.
 * 
 * @version 1.0
 * @author rsoika
 * 
 */
@Startup
@Singleton
// @ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class IndexService implements Serializable {

	public static final String ITEM_API = "$api";

	@Resource
	private TimerService timerService;

	@Inject
	@ConfigProperty(name = "solr.api", defaultValue = "")
	String solrAPI;

	@Inject
	@ConfigProperty(name = "index.interval", defaultValue = "10000")
	int indexInterval;

	@Inject
	RegistryService registryService;

	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(IndexService.class.getName());

	private String timerID = null;

	/**
	 * On startup we just initialize a new Timer if the environment variable
	 * 'solr.api' is set. The timer is running in the 'index.interval' to refresh
	 * the solr index
	 * <p>
	 * 
	 * @see https://stackoverflow.com/questions/55640112/how-to-implement-a-permanent-background-process-with-ejbs
	 * 
	 */
	@PostConstruct
	void init() {
		logger.info("Starting IndexService...");
		timerID = WorkflowKernel.generateUniqueID();
		// do we have a imixs-registry endpoint defined?
		if (!solrAPI.isEmpty()) {
			logger.info("...index service endpoint: " + solrAPI);
			// start timer if no one is started yet....
			if (findTimer() == null) {
				logger.finest("......create new timer: " + timerID + " - timer intervall=" + indexInterval + "ms");
				TimerConfig config = new TimerConfig();
				// config.set
				config.setPersistent(false);
				timerService.createIntervalTimer(0, indexInterval, config);
			}

		}

	}

	/**
	 * On the timeout event we register the service at the experience registry.
	 * 
	 */
	@Timeout
	private synchronized void onTimer() {
		logger.info("...update Index: " + solrAPI);
		// iterate over all registeres Imixs-Microserives and read the eventLog entries

		Set<String> serviceEndpdoints = registryService.getServices();
		for (String serviceEndpoint : serviceEndpdoints) {
			logger.info("...fetch eventLog from " + serviceEndpoint);
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
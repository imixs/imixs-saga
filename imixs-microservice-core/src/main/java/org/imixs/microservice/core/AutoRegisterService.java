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
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.melman.DocumentClient;
import org.imixs.melman.RestAPIException;
import org.imixs.microservice.core.auth.AuthEvent;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.xml.XMLDocumentAdapter;

/**
 * This service is a singleton EJB which registers the Imixs-Microservice at
 * startup at a Imixs-Registry. The Auto-Registration is only performed if the
 * imixs.property 'imixs.registry.serviceendpoint' is set.
 * <p>
 * If the service was autoregistered, the service starts a timer service to
 * update the register in the 'imixs.registry.intervall' (defined in ms). This
 * feature will ensure that the Imixs-Registry is aware of unavailable services.
 * 
 * @version 1.0
 * @author rsoika
 * 
 */
@Startup
@Singleton
// @ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class AutoRegisterService implements Serializable {

	public static final String ITEM_SERVICEENDPOINT = "$serviceendpoint";

	@Resource
	private TimerService timerService;

	@Inject
	@ConfigProperty(name = "imixs.registry.serviceendpoint", defaultValue = "")
	String registryServiceEndpoint;

	@Inject
	@ConfigProperty(name = "imixs.registry.interval", defaultValue = "120000")
	int registryInterval;

	@Inject
	protected Event<AuthEvent> authEvents;

	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(AutoRegisterService.class.getName());

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

			// start timer if no one is started yet....
			if (findTimer() == null) {
				logger.finest("......create new timer: " + timerID + " - timer intervall=" + registryInterval + "ms");
				TimerConfig config = new TimerConfig();
				// config.set
				config.setPersistent(false);
				timerService.createIntervalTimer(0, registryInterval, config);
			}

		}

	}

	@PreDestroy
	void close() {
		// destroy timer
		Timer timer = findTimer();
		if (timer != null) {
			logger.info("...unregister Imixs-Registry endpoint:  " + registryServiceEndpoint);
			logger.finest("......cancel timer: " + timerID);
			timer.cancel();
		}
	}

	/**
	 * On the timeout event we register the service at the experience registry.
	 * 
	 */
	@Timeout
	private synchronized void onTimer() {
		logger.info("...ping Imixs-Registry endpoint: " + registryServiceEndpoint);
		registerMicroservice();
	}

	/**
	 * This method registers the microservice at a given imixs-registry endpoint
	 */
	private void registerMicroservice() {
		logger.info("...register Imixs-Registry endpoint:  " + registryServiceEndpoint);

		// create a new Instance of a DocumentClient to register the service at the
		// Imixs-Registry
		DocumentClient client = new DocumentClient(registryServiceEndpoint);
		// fire an AuthEvent to register a ClientRequestFilter
		if (authEvents != null) {
			AuthEvent authEvent = new AuthEvent(client);
			authEvents.fire(authEvent);
		} else {
			logger.warning("Missing CDI support for Event<AuthEvent> !");
		}

		try {
			ItemCollection matcherDocument = new ItemCollection();
			matcherDocument.setItemValue("type", "workitem");

			matcherDocument.setItemValue("$serviceendpoint", "http://app:8080/api");
			// post new service
			client.postXMLDocument("/services", XMLDocumentAdapter.getDocument(matcherDocument));
			logger.info("...registration successfull...");
		} catch (RestAPIException e) {
			logger.severe("Unable to register service at Imixs-Registry: " + registryServiceEndpoint + " - "
					+ e.getMessage());
			
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
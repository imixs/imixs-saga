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
 * This service is a singleton EJB which registers the Imixs-Microservice during
 * startup at a Imixs-Registry. The Self-Registration is only performed if the
 * imixs.property 'imixs.registry.service' is set.
 * <p>
 * If the service has successfully registered at a Imixs-Registry, the service
 * starts a timer to update the registry in an interval defined by the property
 * 'imixs.registry.intervall' in ms. This feature will ensure that the
 * Imixs-Registry is aware of unavailable services.
 * 
 * @version 1.0
 * @author rsoika
 * 
 */
@Startup
@Singleton
// @ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class RegistrySelfRegistrationService implements Serializable {

	public static final String ITEM_API = "$api";

	@Resource
	private TimerService timerService;

	@Inject
	@ConfigProperty(name = "imixs.registry.api", defaultValue = "")
	String registryAPI;

	@Inject
	@ConfigProperty(name = "imixs.registry.interval", defaultValue = "120000")
	int registryInterval;

	@Inject
	@ConfigProperty(name = "imixs.api", defaultValue = "http://localhost:8080/api")
	String imixsAPI;

	@Inject
	protected Event<AuthEvent> authEvents;

	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(RegistrySelfRegistrationService.class.getName());

	private String timerID = null;

	/**
	 * On startup we just initialize a new Timer running each 10 seconds to auto
	 * register on a given imixs-registry service api endpoint.
	 * <p>
	 * 
	 * @see https://stackoverflow.com/questions/55640112/how-to-implement-a-permanent-background-process-with-ejbs
	 * 
	 */
	@PostConstruct
	void init() {

		timerID = WorkflowKernel.generateUniqueID();
		// do we have a imixs-registry endpoint defined?
		if (!registryAPI.isEmpty()) {
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
			logger.info("...unregister Imixs-Registry (" + registryAPI + ")");
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
		logger.info("...ping Imixs-Registry (" + registryAPI + ")");
		registerMicroservice();
	}

	/**
	 * This method registers the microservice at a given imixs-registry endpoint
	 */
	private void registerMicroservice() {
		logger.info("...register Imixs-Registry (" + registryAPI + ")");

		// create a new Instance of a DocumentClient to register the service at the
		// Imixs-Registry
		DocumentClient client = new DocumentClient(registryAPI);
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

			matcherDocument.setItemValue(ITEM_API, imixsAPI);
			// post new service
			client.postXMLDocument("/services", XMLDocumentAdapter.getDocument(matcherDocument));
			logger.info("...registration successfull...");
		} catch (RestAPIException e) {
			logger.severe("Unable to register service at Imixs-Registry: " + registryAPI + " - " + e.getMessage());

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
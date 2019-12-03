package org.imixs.registry.index;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
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
import org.imixs.melman.EventLogClient;
import org.imixs.melman.RestAPIException;
import org.imixs.microservice.security.auth.AuthEvent;
import org.imixs.registry.RegistryService;
import org.imixs.registry.index.solr.SolrIndexService;
import org.imixs.registry.index.solr.SolrUpdateService;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;

/**
 * The UpdateService is a singleton EJB which maintains a solr index based on
 * event log entries provided by a registered Imixs-Microservice. At startup the
 * service starts a timer service to update the index in the
 * 'imixs.index.intervall' (defined in ms). This feature will ensure that the
 * Imixs-Registry holds an updated solr index
 * <p>
 * The environment variable 'solr.api' defines the solr service endpoint and
 * lucene core. If no endpoint is defined (default) no index will be written.
 * <p>
 * The service reads all enventLogEntires from all registered
 * Imixs-Microserivces and delegates the index update to the class
 * SolrUpdateervice.
 * 
 * @see SolrUpdateService
 * @version 1.0
 * @author rsoika
 */
@Startup
@Singleton
// @ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class UpdateService implements Serializable {

	public static final String EVENTLOG_TOPIC_INDEX_ADD = "imixs-registry.index.add";
	public static final String EVENTLOG_TOPIC_INDEX_REMOVE = "imixs-registry.index.remove";

	@Resource
	private TimerService timerService;

	@Inject
	@ConfigProperty(name = "solr.api", defaultValue = "http://solr:8983")
	private String api;

	@Inject
	@ConfigProperty(name = "imixs.registry.index.interval", defaultValue = "1000")
	int indexInterval; // default 1sec

	@Inject
	protected Event<AuthEvent> authEvents;

	@Inject
	RegistryService registryService;

	@Inject
	SolrUpdateService solrUpdateService;

	@Inject
	SolrIndexService solrIndexService;

	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(UpdateService.class.getName());

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
		// do we have a imixs-registry endpoint defined?
		if (!api.isEmpty()) {
			boolean debug = logger.isLoggable(Level.FINE);

			timerID = WorkflowKernel.generateUniqueID();
			logger.info("IndexService endpoint: " + api);
			// start timer if no one is started yet....
			if (findTimer() == null) {
				if (debug) {
					logger.finest("......create new timer: " + timerID + " - timer intervall=" + indexInterval + "ms");
				}
				TimerConfig config = new TimerConfig();
				// config.set
				config.setPersistent(false);
				timerService.createIntervalTimer(0, indexInterval, config);
			}

		}
	}

	/**
	 * This method flush the event log for all imixs-microservices registered at the
	 * experience registry.
	 */
	public synchronized void updateIndex() {
		if (!api.isEmpty()) {
			boolean debug = logger.isLoggable(Level.FINE);
			long l = System.currentTimeMillis();
			// iterate over all registered Imixs-Microserives and read the eventLog entries
			Set<String> services = registryService.getServices();
			if (services != null && services.size() > 0) {
				for (String service : services) {
					if (debug) {
						logger.finest("...update Index: " + service);
					}
					EventLogClient eventLogClient = createEventLogClient(service);
					List<ItemCollection> eventLog = loadEventLog(eventLogClient);
					updateIndex(eventLog, eventLogClient);
				}
				if (debug) {
					logger.fine("...full index update completed in " + (System.currentTimeMillis() - l) + "ms");
				}
			}
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
	private Timer findTimer() {
		for (Object obj : timerService.getTimers()) {
			Timer timer = (javax.ejb.Timer) obj;
			if (timerID.equals(timer.getInfo())) {
				return timer;
			}
		}
		return null;
	}

	/**
	 * On the timeout event we flush the event log for all imixs-microservices
	 * registered at the experience registry.
	 * 
	 */
	@Timeout
	private synchronized void onTimer() {
		updateIndex();
	}

	/**
	 * This helper method reads the imixs-registry.index topics for a given
	 * Imixs-Microservice instance.
	 * 
	 * @return
	 */
	private List<ItemCollection> loadEventLog(EventLogClient client) {
		boolean debug = logger.isLoggable(Level.FINE);
		List<ItemCollection> eventLogEntries = null;
		if (debug) {
			logger.finest("...loading event log : " + client.getBaseURI() + " ...");
		}
		try {
			// load eventLog entries.....
			eventLogEntries = client.searchEventLog(EVENTLOG_TOPIC_INDEX_ADD, EVENTLOG_TOPIC_INDEX_REMOVE);
		} catch (RestAPIException e) {
			logger.severe("Unable to read eventlog from: " + client.getBaseURI() + " - " + e.getMessage());
		}

		return eventLogEntries;
	}

	/**
	 * This method updates the solr index for a given collection of eventLogEntries.
	 * <p>
	 * The method creates two distinct collections containing the workitems to be
	 * added into the index and a collection containing. This is to reduce the rest
	 * api calls into solr. Only one update and one delete api call is processed
	 * 
	 * 
	 * <p>
	 * After the solr index was updated, the method deletes the eventLog entries
	 * from the Imixs-Microservice instance vie the imixs-microserivce Rest API.
	 * 
	 * @param eventLog
	 *            entries to be added or removed.
	 * @return true if the cache was totally flushed.
	 */
	@SuppressWarnings("unchecked")
	private boolean updateIndex(List<ItemCollection> eventLogEntries, EventLogClient eventLogClient) {
		Date lastEventDate = null;
		boolean cacheIsEmpty = true;
		boolean debug = logger.isLoggable(Level.FINE);

		Map<String, ItemCollection> distinctUpdates = new HashMap<String, ItemCollection>();
		Set<String> distinctDeletions = new HashSet<String>();

		long l = System.currentTimeMillis();
		if (debug) {
			logger.finest("......flush eventlog cache....");
		}
		if (eventLogEntries != null && eventLogEntries.size() > 0) {
			try {

				for (ItemCollection eventLogEntry : eventLogEntries) {
					String id = eventLogEntry.getItemValueString("id");
					String topic = eventLogEntry.getItemValueString("topic");
					String uniqueid = eventLogEntry.getItemValueString("ref");
					List<?> dataEntries = eventLogEntry.getItemValue("data");
					if (dataEntries == null || dataEntries.size() == 0) {
						logger.warning("wrong eventLogEntry '" + id
								+ "' - does not contain a data object - entry will be deleted...");
						eventLogClient.deleteEventLogEntry(id);
						continue;
					}
					Map<String, List<Object>> data = (Map<String, List<Object>>) dataEntries.get(0);
					ItemCollection workitem = new ItemCollection(data);

					// if the document was found we add/update the index. Otherwise we remove the
					// document form the index.
					if (EVENTLOG_TOPIC_INDEX_ADD.equals(topic)) {
						// do we have already an deletion entry then remove it..
						distinctDeletions.remove(uniqueid);
						distinctUpdates.put(uniqueid, workitem);

					}
					if (EVENTLOG_TOPIC_INDEX_REMOVE.equals(topic)) {
						// do we have already an update entry then remove it..
						distinctUpdates.remove(uniqueid);
						distinctDeletions.add(uniqueid);
					}

				}

				solrUpdateService.indexDocuments(distinctUpdates.values());
				solrUpdateService.removeDocuments(distinctDeletions);

				// remove the eventLogEntries
				for (ItemCollection eventLogEntry : eventLogEntries) {
					// TODO need to be optimized - see Issue #51
					eventLogClient.deleteEventLogEntry(eventLogEntry.getItemValueString("id"));
				}

			} catch (RestAPIException | org.imixs.workflow.services.rest.RestAPIException e) {
				logger.warning("...unable to flush lucene event log: " + e.getMessage());
				// We just log a warning here and close the flush mode to no longer block the
				// writer.
				// NOTE: maybe throwing a IndexException would be an alternative:
				//
				// throw new IndexException(IndexException.INVALID_INDEX, "Unable to update
				// lucene search index", luceneEx);
				return true;
			}
			if (debug) {
				logger.fine("...update index - " + eventLogEntries.size() + " events in "
						+ (System.currentTimeMillis() - l) + " ms - last log entry: " + lastEventDate);
			}
		} else {
			if (debug) {
				logger.fine("...no eventLog entries found. Index for " + eventLogClient.getBaseURI()
						+ " is already synchonised!");
			}
		}

		return cacheIsEmpty;

	}

	private EventLogClient createEventLogClient(String serviceAPI) {
		// create a new Instance of a DocumentClient...
		EventLogClient client = new EventLogClient(serviceAPI);
		// fire an AuthEvent to register a ClientRequestFilter
		if (authEvents != null) {
			AuthEvent authEvent = new AuthEvent(client, null);
			authEvents.fire(authEvent);
		} else {
			logger.warning("Missing CDI support for Event<AuthEvent> !");
		}
		return client;
	}

}
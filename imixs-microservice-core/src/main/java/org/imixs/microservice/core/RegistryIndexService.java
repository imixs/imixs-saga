package org.imixs.microservice.core;

import java.io.Serializable;
import java.util.logging.Logger;

import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.DocumentEvent;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.EventLogService;
import org.imixs.workflow.exceptions.AccessDeniedException;

/**
 * This service creates a eventLog entry each time a document is saved or
 * deleted.
 * <p>
 * The imixs-registry polls those eventLog entries to maintain a derived solr
 * index.
 * <p>
 * The seri
 * 
 * @version 1.0
 * @author rsoika
 * 
 */
@Stateless
public class RegistryIndexService implements Serializable {

	public static final String EVENTLOG_TOPIC_INDEX_ADD = "imixs-registry.index.add";
	public static final String EVENTLOG_TOPIC_INDEX_REMOVE = "imixs-registry.index.remove";

	@Inject
	@ConfigProperty(name = "imixs-registry.index", defaultValue = "false")
	boolean imixsRegistryIndex;

	private static final long serialVersionUID = 1L;

	private static Logger logger = Logger.getLogger(RegistryIndexService.class.getName());

	@Inject
	private EventLogService eventLogService;

	/**
	 * DocumentEvent listener to generate a new eventLog entry for a index request.
	 * <p>
	 * EventLog entries created only in case the property 'imixs-registry.index' is
	 * set to true.
	 * 
	 * @param documentEvent
	 * @throws AccessDeniedException
	 */
	public void onDocumentEvent(@Observes DocumentEvent documentEvent) throws AccessDeniedException {
		if (documentEvent == null) {
			return;
		}

		// is registry-index enabled?
		if (!imixsRegistryIndex) {
			return;
		}

		if (documentEvent.getEventType() == DocumentEvent.ON_DOCUMENT_SAVE) {
			// index...
			logger.info("...index document...");
			addDocumentToIndex(documentEvent.getDocument());
		}

		if (documentEvent.getEventType() == DocumentEvent.ON_DOCUMENT_DELETE) {
			// index...
			logger.info("...remove indexed document...");
			removeDocumentToIndex(documentEvent.getDocument());
		}
	}

	/**
	 * This method creates a new eventLog with the topic 'imixs-registry.index.add'.
	 * This event log entries are read by the Imixs-Registry index Service to add
	 * the document data to a derived solr index. The document will be indexed by
	 * the Imixs-Registry periodically.
	 * <p>
	 * Note: The index created by the Imixs-Registry has an asynchronous manner.
	 * 
	 * @param documentContext
	 */
	public void addDocumentToIndex(ItemCollection document) {
		// skip if the flag 'noindex' = true
		if (!document.getItemValueBoolean(DocumentService.NOINDEX)) {
			// write a new EventLog entry for each document....
			eventLogService.createEvent(EVENTLOG_TOPIC_INDEX_ADD, document.getUniqueID());
		}
	}

	/**
	 * This method creates a new eventLog with the topic
	 * 'imixs-registry.index.remove'. This event log entries are read by the
	 * Imixs-Registry index Service to remove delted document data from a derived
	 * solr index. The document will be removed from the inedex by the
	 * Imixs-Registry periodically.
	 * <p>
	 * Note: The index created by the Imixs-Registry has an asynchronous manner.
	 * 
	 * @param documentContext
	 */
	public void removeDocumentToIndex(ItemCollection document) {
		// remove document form index - @see issue #412
		if (!document.getItemValueBoolean(DocumentService.NOINDEX)) {
			// write a new EventLog entry for each document....
			eventLogService.createEvent(EVENTLOG_TOPIC_INDEX_REMOVE, document.getUniqueID());
		}
	}

}
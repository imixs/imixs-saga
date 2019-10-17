package org.imixs.microservice.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.DocumentEvent;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.EventLogService;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.engine.index.SchemaService;
import org.imixs.workflow.exceptions.AccessDeniedException;

/**
 * This service creates a eventLog entry each time a document is saved or
 * deleted.
 * <p>
 * The imixs-registry polls those eventLog entries to maintain a derived solr
 * index.
 * <p>
 * An index data object is added into the eventLog entry containing a subset of
 * items. The item list contains all items from the
 * SchemaService.DEFAULT_NOANALYZE_FIELD_LIST and optional items defined by the
 * imixs property 'imixs-registry.index.fields'.
 * <p>
 * The RegistryIndexService adds only workitems to the registry index.
 * 
 * @version 1.0
 * @author rsoika
 * 
 */
@DeclareRoles({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RolesAllowed({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Stateless
public class RegistryIndexService implements Serializable {

	public static final String EVENTLOG_TOPIC_INDEX_ADD = "imixs-registry.index.add";
	public static final String EVENTLOG_TOPIC_INDEX_REMOVE = "imixs-registry.index.remove";

	@Inject
	@ConfigProperty(name = "imixs.registry.api", defaultValue = "")
	String registryAPI;

	@Inject
	@ConfigProperty(name = "imixs-registry.index.enabled", defaultValue = "false")
	boolean imixsRegistryIndex;

	@Inject
	@ConfigProperty(name = "imixs-registry.index.fields", defaultValue = "")
	String imixsRegistryIndexFieldList;

	@Inject
	@ConfigProperty(name = "imixs-registry.index.typefilter", defaultValue = "(workitem|workitemarchive)")
	String imixsRegistryIndexTypeFilter;

	private static final long serialVersionUID = 1L;
	private List<String> fieldList = null;
	private static Logger logger = Logger.getLogger(RegistryIndexService.class.getName());

	/**
	 * The init method builds a static field list from the
	 * SchemaService.DEFAULT_NOANALYZE_FIELD_LIST and the optional item list
	 * 'imixs-registry.index.fields'.
	 * 
	 */
	@PostConstruct
	void init() {

		// compute search field list
		fieldList = new ArrayList<String>();
		fieldList.add(WorkflowKernel.UNIQUEID);
		fieldList.add(WorkflowService.READACCESS);
		fieldList.add(RegistrySelfRegistrationService.ITEM_API);

		// add all items form teh default field store list
		fieldList.addAll(SchemaService.DEFAULT_STORE_FIELD_LIST);
		if (imixsRegistryIndexFieldList != null && !imixsRegistryIndexFieldList.isEmpty()) {
			StringTokenizer st = new StringTokenizer(imixsRegistryIndexFieldList, ",");
			while (st.hasMoreElements()) {
				String sName = st.nextToken().toLowerCase().trim();
				// do not add internal fields
				if (!fieldList.contains(sName)) {
					fieldList.add(sName);
				}
			}
		}
	}

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
		if (!imixsRegistryIndex || registryAPI.isEmpty()) {
			return;
		}

		if (documentEvent.getDocument() == null) {
			// no data
			return;
		}

		if (documentEvent.getEventType() == DocumentEvent.ON_DOCUMENT_SAVE) {

			// Ignore NOINDEX
			if (documentEvent.getDocument().getItemValueBoolean(DocumentService.NOINDEX)) {
				return;
			}

			// does the document math the imixsRegistryIndexTypeFilter?
			if (!documentEvent.getDocument().getType().matches(imixsRegistryIndexTypeFilter)) {
				return;
			}
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
			ItemCollection indexDocument = buildIndexDocument(document);
			eventLogService.createEvent(EVENTLOG_TOPIC_INDEX_ADD, document.getUniqueID(), indexDocument);
		}
	}

	/**
	 * This helper method builds a indexDocument. This document contains a subset of
	 * items defined by the SchemaService.DEFAULT_NOANALYZE_FIELD_LIST and an
	 * optional item list defined by the Imixs property
	 * 'imixs-registry.index.fields'
	 * 
	 * @return
	 */
	private ItemCollection buildIndexDocument(ItemCollection document) {
		ItemCollection indexDocument = new ItemCollection();
		for (String itemName : fieldList) {
			indexDocument.replaceItemValue(itemName, document.getItemValue(itemName));
		}
		return indexDocument;
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
/*******************************************************************************
 * <pre>
 *  Imixs Workflow 
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
 *      Imixs Software Solutions GmbH - initial API and implementation
 *      Ralph Soika - Software Developer
 * </pre>
 *******************************************************************************/

package org.imixs.microservice.registry;

import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.registry.index.RegistrySchemaService;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.DocumentEvent;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.EventLogService;
import org.imixs.workflow.exceptions.AccessDeniedException;

/**
 * This service creates a eventLog entry each time a document is saved or deleted.
 * <p>
 * The imixs-registry polls those eventLog entries to maintain a derived solr index.
 * <p>
 * An index data object is added into the eventLog entry containing a subset of items. The item list
 * contains all items from the SchemaService.DEFAULT_NOANALYZE_FIELD_LIST and optional items defined
 * by the imixs property 'imixs.registry.index.fields'.
 * <p>
 * The RegistryIndexService adds only workitems to the registry index.
 * 
 * @version 1.0
 * @author rsoika
 * 
 */
@DeclareRoles({"org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
    "org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
    "org.imixs.ACCESSLEVEL.MANAGERACCESS"})
@RolesAllowed({"org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
    "org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
    "org.imixs.ACCESSLEVEL.MANAGERACCESS"})
@Stateless
public class RegistryIndexService implements Serializable {

  public static final String EVENTLOG_TOPIC_INDEX_ADD = "imixs-registry.index.add";
  public static final String EVENTLOG_TOPIC_INDEX_REMOVE = "imixs-registry.index.remove";

  @Inject
  @ConfigProperty(name = "imixs.registry.api", defaultValue = "")
  String registryAPI;

  @Inject
  @ConfigProperty(name = "imixs.registry.index.enabled", defaultValue = "false")
  boolean imixsRegistryIndex;


  @Inject
  @ConfigProperty(name = "imixs.registry.index.typefilter",
      defaultValue = "(workitem|workitemarchive)")
  String imixsRegistryIndexTypeFilter;

  @Inject
  RegistrySchemaService registrySchemaService;

  private static final long serialVersionUID = 1L;

  private static Logger logger = Logger.getLogger(RegistryIndexService.class.getName());

  /**
   * The init method builds a static field list from the SchemaService.DEFAULT_NOANALYZE_FIELD_LIST
   * and the optional item list 'imixs-registry.index.fields'.
   * 
   */
  @PostConstruct
  void init() {
    // is registry-index enabled?
    if (!imixsRegistryIndex || registryAPI.isEmpty()) {
      return;
    }
  }

  @Inject
  private EventLogService eventLogService;

  /**
   * DocumentEvent listener to generate a new eventLog entry for a index request.
   * <p>
   * EventLog entries created only in case the property 'imixs-registry.index' is set to true.
   * 
   * @param documentEvent
   * @throws AccessDeniedException
   */
  public void onDocumentEvent(@Observes DocumentEvent documentEvent) throws AccessDeniedException {
    boolean debug = logger.isLoggable(Level.FINE);
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

      // does the document match the imixsRegistryIndexTypeFilter?
      if (!documentEvent.getDocument().getType().matches(imixsRegistryIndexTypeFilter)) {
        return;
      }
      // index...
      if (debug) {
        logger.finest("...index document...");
      }
      addDocumentToIndex(documentEvent.getDocument());
    }

    if (documentEvent.getEventType() == DocumentEvent.ON_DOCUMENT_DELETE) {
      // index...
      if (debug) {
        logger.finest("...remove indexed document...");
      }
      removeDocumentToIndex(documentEvent.getDocument());
    }
  }

  /**
   * This method creates a new eventLog with the topic 'imixs-registry.index.add'. This event log
   * entries are read by the Imixs-Registry index Service to add the document data to a derived solr
   * index. The document will be indexed by the Imixs-Registry periodically.
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
   * This helper method builds a indexDocument. This document contains a subset of items defined by
   * the SchemaService.DEFAULT_NOANALYZE_FIELD_LIST and an optional item list defined by the Imixs
   * property 'imixs.registry.index.fields'
   * 
   * @return
   */
  private ItemCollection buildIndexDocument(ItemCollection document) {
    ItemCollection indexDocument = new ItemCollection();
    List<String> fieldList = registrySchemaService.getSchemaFieldList();
    for (String itemName : fieldList) {
      indexDocument.replaceItemValue(itemName, document.getItemValue(itemName));
    }
    return indexDocument;
  }

  /**
   * This method creates a new eventLog with the topic 'imixs-registry.index.remove'. This event log
   * entries are read by the Imixs-Registry index Service to remove delted document data from a
   * derived solr index. The document will be removed from the inedex by the Imixs-Registry
   * periodically.
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

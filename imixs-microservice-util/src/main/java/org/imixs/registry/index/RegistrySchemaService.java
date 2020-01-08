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

package org.imixs.registry.index;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.workflow.WorkflowKernel;

/**
 * The RegistrySchemaService provides schema informations used by the Imixs-Registry Solr Index.
 * 
 * @version 1.0
 * @author rsoika
 * 
 */
@Singleton
public class RegistrySchemaService implements Serializable {
  private static final long serialVersionUID = 1L;

  public static List<String> DEFAULT_STORE_FIELD_LIST = Arrays.asList(WorkflowKernel.UNIQUEID,
      "type", "$readaccess", "$taskid", "$writeaccess", "$workflowsummary", "$workflowabstract",
      "$workflowgroup", "$workflowstatus", "$modified", "$created", "$modelversion",
      "$lasteventdate", "$creator", "$editor", "$lasteditor", "$owner", "namowner", "$api");

  private List<String> schemaFieldList = null;

  @Inject
  @ConfigProperty(name = "imixs.registry.index.fields", defaultValue = "")
  String imixsIndexFieldList;

  /**
   * Create a solr rest client instance
   * 
   * @throws org.imixs.workflow.services.rest.RestAPIException
   */
  @PostConstruct
  public void init() {

    // crate unique index fields
    schemaFieldList = new ArrayList<String>();
    schemaFieldList.addAll(DEFAULT_STORE_FIELD_LIST);
    if (imixsIndexFieldList != null && !imixsIndexFieldList.isEmpty()) {
      StringTokenizer st = new StringTokenizer(imixsIndexFieldList, ",");
      while (st.hasMoreElements()) {
        String sName = st.nextToken().toLowerCase().trim();
        // do not add internal fields
        if (!schemaFieldList.contains(sName)) {
          schemaFieldList.add(sName);
        }
      }
    }


  }

  public List<String> getSchemaFieldList() {
    return schemaFieldList;
  }

  /**
   * This method adapts a search query for Imixs Item names and adapts these names with the
   * corresponding Solr field name (replace $ with _)
   * 
   * @return
   */
  public String adaptQueryFieldNames(String _query) {
    String result = _query;

    if (_query == null || !_query.contains("$")) {
      return result;
    }

    for (String imixsItemName : schemaFieldList) {
      if (imixsItemName.charAt(0) == '$') {
        // this item starts with $ and we need to parse the query for this item....
        while (result.contains(imixsItemName + ":")) {
          String solrField = "_" + imixsItemName.substring(1);
          result = result.replace(imixsItemName + ":", solrField + ":");
        }
      }
    }

    return result;
  }

  /**
   * This method adapts an Solr field name to the corresponding Imixs Item name. Because Solr does
   * not accept $ char at the beginning of an field we need to replace starting _ with $ if the item
   * is part of the Imixs Index Schema.
   * 
   * @param itemName
   * @return adapted Imixs item name
   */
  public String adaptSolrFieldName(String itemName) {
    if (itemName == null || itemName.isEmpty()) {
      return itemName;
    }


    if (itemName.charAt(0) == '_') {
      String adaptedName = "$" + itemName.substring(1);
      if (schemaFieldList.contains(adaptedName)) {
        return adaptedName;
      }
    }
    return itemName;
  }

  
  /**
   * This method adapts an Imixs item name to the corresponding Solr field name. Because Solr does
   * not accept $ char at the beginning of an field we need to replace starting $ with _ if the item
   * is part of the Imixs Index Schema.
   * 
   * @param itemName
   * @return adapted Solr field name
   */
  public String adaptImixsItemName(String itemName) {
    if (itemName == null || itemName.isEmpty() || schemaFieldList == null) {
      return itemName;
    }
    if (itemName.charAt(0) == '$') {
      if (schemaFieldList.contains(itemName)) {
        String adaptedName = "_" + itemName.substring(1);
        return adaptedName;
      }
    }
    return itemName;
  }

  

}

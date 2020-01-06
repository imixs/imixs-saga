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
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.LocalBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.microservice.security.auth.AuthEvent;
import org.imixs.registry.index.solr.SolrIndexService;
import org.imixs.registry.index.solr.SolrUpdateService;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.QueryException;

/**
 * The SearchService is a session EJB to search the solr index. The index is maintained by the
 * UpdateService EJB.
 * 
 * @see UpdateService
 * @version 1.0
 * @author rsoika
 */
@DeclareRoles({"org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
    "org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
    "org.imixs.ACCESSLEVEL.MANAGERACCESS"})
@RolesAllowed({"org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
    "org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
    "org.imixs.ACCESSLEVEL.MANAGERACCESS"})
@Stateless
@LocalBean
public class SearchService implements Serializable {

  public static final String ACCESSLEVEL_READERACCESS = "org.imixs.ACCESSLEVEL.READERACCESS";
  public static final String ACCESSLEVEL_AUTHORACCESS = "org.imixs.ACCESSLEVEL.AUTHORACCESS";
  public static final String ACCESSLEVEL_EDITORACCESS = "org.imixs.ACCESSLEVEL.EDITORACCESS";
  public static final String ACCESSLEVEL_MANAGERACCESS = "org.imixs.ACCESSLEVEL.MANAGERACCESS";

  public static final String ANONYMOUS = "ANONYMOUS";

  public static List<String> ACCESS_ROLES = Arrays.asList(ACCESSLEVEL_READERACCESS,
      ACCESSLEVEL_AUTHORACCESS, ACCESSLEVEL_EDITORACCESS, ACCESSLEVEL_MANAGERACCESS);

  @Inject
  @ConfigProperty(name = "solr.api", defaultValue = "http://solr:8983")
  private String api;

  @Resource
  SessionContext ctx;

  @Inject
  protected Event<AuthEvent> authEvents;

  @Inject
  SolrUpdateService solrUpdateService;

  @Inject
  SolrIndexService solrIndexService;

  @Inject
  RegistrySchemaService registrySchemaService;


  private static final long serialVersionUID = 1L;
  private static Logger logger = Logger.getLogger(SearchService.class.getName());

  /**
   * On startup we just initialize a new Timer if the environment variable 'solr.api' is set. The
   * timer is running in the 'index.interval' to refresh the solr index
   * <p>
   * 
   * @see https://stackoverflow.com/questions/55640112/how-to-implement-a-permanent-background-process-with-ejbs
   * 
   */
  @PostConstruct
  void init() {
    // do we have a imixs-registry endpoint defined?
    if (!api.isEmpty()) {
      logger.info("init SearchService...");

    }
  }

  /**
   * Returns a collection of documents matching the provided search term. The term will be extended
   * with the current users roles to test the read access level of each workitem matching the search
   * term.
   * <p>
   * The optional param 'searchOrder' can be set to force lucene to sort the search result by any
   * search order.
   * <p>
   * The optional param 'defaultOperator' can be set to Operator.AND
   * <p>
   * The optional param 'stubs' indicates if the full Imixs Document should be loaded or if only the
   * data fields stored in the lucedn index will be return. The later is the faster method but
   * returns only document stubs.
   * 
   * @param searchTerm
   * @param pageSize        - docs per page
   * @param pageIndex       - page number
   * @param sortOrder
   * @param defaultOperator - optional to change the default search operator
   * @param loadStubs       - optional indicates of only the lucene document should be returned.
   * @return collection of search result
   * 
   * @throws QueryException in case the searchtem is not understandable.
   */
  public List<ItemCollection> search(String _searchTerm, int pageSize, int pageIndex,
      SortOrder sortOrder, DefaultOperator defaultOperator) throws QueryException {
    boolean debug = logger.isLoggable(Level.FINE);
    long ltime = System.currentTimeMillis();
    List<ItemCollection> workitems = new ArrayList<ItemCollection>();

    if (pageSize <= 0) {
      pageSize = SolrUpdateService.DEFAULT_PAGE_SIZE;
    }

    if (pageIndex < 0) {
      pageIndex = 0;
    }

    logger.finest("......solr search: pageNumber=" + pageIndex + " pageSize=" + pageSize);

    String searchTerm = adaptSearchTerm(_searchTerm);
    // test if searchtem is provided
    if (searchTerm == null || "".equals(searchTerm)) {
      return workitems;
    }

    // post query....
    workitems = solrIndexService.query(searchTerm, pageSize, pageIndex, sortOrder, defaultOperator);

    if (debug) {
      logger.fine("...search result computed - " + workitems.size() + " workitems found in "
          + (System.currentTimeMillis() - ltime) + " ms");
    }
    return workitems;
  }

  /**
   * This method lookups a document by its $uniqueid in the search index.
   * 
   * @param uniqueid
   * @return document or null if no document was found!
   * @throws QueryException
   */
  public ItemCollection getDocument(String uniqueid) throws QueryException {
    String searchTerm = WorkflowKernel.UNIQUEID + ":\"" + uniqueid + "\"";
    List<ItemCollection> result = search(searchTerm, 1, 0, null, DefaultOperator.AND);
    if (result != null && result.size() > 0) {
      ItemCollection doc = result.get(0);
      return doc;
    }
    return null;
  }

  /**
   * This method addapts a given Solr search term. The method extend the search term by read access
   * query and also adapts the imixs item names to Solr field names.
   * 
   * @param _serachTerm
   * @return
   * @throws QueryException
   */
  private String adaptSearchTerm(String _serachTerm) throws QueryException {
    if (_serachTerm == null || "".equals(_serachTerm)) {
      return _serachTerm;
    }

    String searchTerm = getExtendedSearchTerm(_serachTerm);

    // Because Solr does not accept $ symbol in an item name we need to replace the
    // Imxis Item Names and adapt them into the corresponding Solr Field name
    searchTerm = registrySchemaService.adaptQueryFieldNames(searchTerm);

    return searchTerm;

  }

  /**
   * Returns the extended search term for a given query. The search term will be extended with a
   * users roles to test the read access level of each workitem matching the search term.
   * 
   * @param sSearchTerm
   * @return extended search term
   * @throws QueryException in case the searchtem is not understandable.
   */
  private String getExtendedSearchTerm(String sSearchTerm) throws QueryException {
    // test if searchtem is provided
    boolean debug = logger.isLoggable(Level.FINE);
    if (sSearchTerm == null || "".equals(sSearchTerm)) {
      logger.warning("No search term provided!");
      return "";
    }
    // extend the Search Term if user is not ACCESSLEVEL_MANAGERACCESS
    if (!isUserInRole(ACCESSLEVEL_MANAGERACCESS)) {
      // get user names list
      List<String> userNameList = getUserNameList();
      // create search term (always add ANONYMOUS)
      String sAccessTerm = "($readaccess:" + ANONYMOUS;
      for (String aRole : userNameList) {
        if (!"".equals(aRole))
          sAccessTerm += " OR $readaccess:\"" + aRole + "\"";
      }
      sAccessTerm += ") AND ";
      sSearchTerm = sAccessTerm + sSearchTerm;
    }
    if (debug) {
      logger.finest("......lucene final searchTerm=" + sSearchTerm);
    }
    return sSearchTerm;
  }

  /**
   * This method returns a list containing the current user name and roles the user is assigned to.
   * <p>
   * For a more fine granted access control search the imixs-micrsoervice instance.
   * <p>
   * 
   * @see UserGroupEvent
   * @return
   */
  private List<String> getUserNameList() {
    List<String> userNameList = new Vector<String>();

    // Begin with the username
    userNameList.add(ctx.getCallerPrincipal().getName().toString());
    // now construct role list
    // and add each role the user is in to the list
    for (String testRole : ACCESS_ROLES) {
      if (ctx.isCallerInRole(testRole)) {
        userNameList.add(testRole);
      }
    }
    return userNameList;
  }

  /**
   * Test if the caller has a given security role.
   * 
   * @param rolename
   * @return true if user is in role
   */
  private boolean isUserInRole(String rolename) {
    try {
      return ctx.isCallerInRole(rolename);
    } catch (Exception e) {
      // avoid a exception for a role request which is not defined
      return false;
    }
  }



}

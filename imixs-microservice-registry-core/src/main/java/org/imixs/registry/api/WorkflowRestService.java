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

package org.imixs.registry.api;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.melman.RestAPIException;
import org.imixs.melman.WorkflowClient;
import org.imixs.microservice.security.auth.AuthEvent;
import org.imixs.registry.DiscoveryService;
import org.imixs.registry.RegistryService;
import org.imixs.registry.index.DefaultOperator;
import org.imixs.registry.index.SearchService;
import org.imixs.registry.index.SortOrder;
import org.imixs.registry.index.UpdateService;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.ImixsExceptionHandler;
import org.imixs.workflow.exceptions.QueryException;
import org.imixs.workflow.util.JSONParser;
import org.imixs.workflow.xml.XMLDataCollectionAdapter;
import org.imixs.workflow.xml.XMLDocument;
import org.imixs.workflow.xml.XMLDocumentAdapter;

/**
 * The api endpoint '/workflow' provides methods to create or update a process instance based on a
 * BusinessEvent (ItemCollection)
 * <p>
 * A POST request for the resource '/workitem' discovers a service based on the data of a
 * businessEvent. In case the businessEvent contains a uid than a search request is delegated to the
 * Solr Index to lookup the existing instance and extract the $api item.
 * <p>
 * In case the businessEvent contains no uid the service will be discovered by the provided business
 * data (e.g. $modelversion, $workflowgroup or business rules). If a service was found the item $api
 * will contain the service endpoint.
 * <p>
 * The response code of the response object is set to 200 if case the processing was successful. In
 * case of an Exception a error message is generated and the status NOT_ACCEPTABLE is returned.
 * <p>
 * 
 * 
 * @author rsoika
 *
 */
@Path("/workflow")
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
@Stateless
public class WorkflowRestService {

  @javax.ws.rs.core.Context
  private HttpServletRequest servletRequest;

  @Resource
  private SessionContext ctx;

  private static Logger logger = Logger.getLogger(WorkflowRestService.class.getName());

  @Inject
  protected DiscoveryService discoveryService;

  @Inject
  protected SearchService searchService;

  @Inject
  protected UpdateService updateService;

  @Inject
  protected Event<AuthEvent> authEvents;

  @Inject
  @ConfigProperty(name = "index.defaultOperator", defaultValue = "AND")
  private String indexDefaultOperator;

  /**
   * returns a single workitem defined by $uniqueid
   * 
   * @param uniqueid
   * @return
   */
  @GET
  @Path("/workitem/{uniqueid : ([0-9a-f]{8}-.*|[0-9a-f]{11}-.*)}")
  public Response getWorkItem(@PathParam("uniqueid") String uniqueid,
      @QueryParam("items") String items, @QueryParam("format") String format) {

    ItemCollection workitem;
    try {
      workitem = searchService.getDocument(uniqueid);
      if (workitem == null) {
        // workitem not found
        return Response.status(Response.Status.NOT_FOUND).build();
      }

    } catch (QueryException e) {
      e.printStackTrace();
      workitem = null;
    }

    return convertResult(workitem, items, format);
  }

  @GET
  @Path("/tasklist/creator/{creator}")
  public Response getTaskListByCreator(@PathParam("creator") String creator,
      @QueryParam("type") String type, @DefaultValue("0") @QueryParam("pageIndex") int pageIndex,
      @DefaultValue("10") @QueryParam("pageSize") int pageSize,
      @DefaultValue("") @QueryParam("sortBy") String sortBy,
      @DefaultValue("false") @QueryParam("sortReverse") Boolean sortReverse,
      @QueryParam("items") String items, @QueryParam("format") String format) {
    List<ItemCollection> result = null;
    try {
      if ("null".equalsIgnoreCase(creator))
        creator = null;

      if (creator == null || "".equals(creator))
        creator = ctx.getCallerPrincipal().getName();

      String searchTerm = "(";
      if (type != null && !"".equals(type)) {
        searchTerm += " type:\"" + type + "\" AND ";
      }
      searchTerm += " $creator:\"" + creator + "\" )";
      result = searchIndex(searchTerm, pageSize, pageIndex, sortBy, sortReverse, items);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return convertResultList(result, items, format);
  }

  /**
   * Returns a resultset for a lucene Search Query
   * 
   * @param query
   * @param pageSize
   * @param pageIndex
   * @param items
   * @return
   */
  @GET
  @Path("/search/{query}")
  public Response findDocumentsByQuery(@PathParam("query") String query,
      @DefaultValue("-1") @QueryParam("pageSize") int pageSize,
      @DefaultValue("0") @QueryParam("pageIndex") int pageIndex,
      @QueryParam("sortBy") String sortBy, @QueryParam("sortReverse") boolean sortReverse,
      @QueryParam("items") String items, @QueryParam("format") String format) {
    List<ItemCollection> result = null;
    try {
      // decode query...
      String decodedQuery = URLDecoder.decode(query, "UTF-8");
      result = searchIndex(decodedQuery, pageSize, pageIndex, sortBy, sortReverse, items);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return convertResultList(result, items, format);
  }

  /**
   * Helper method to search the registry index
   * 
   * @param query
   * @param pageSize
   * @param pageIndex
   * @param sortBy
   * @param sortReverse
   * @param item
   * @return
   * @throws QueryException
   */
  private List<ItemCollection> searchIndex(String query, int pageSize, int pageIndex, String sortBy,
      boolean sortReverse, String items) throws QueryException {
    // create sort object
    SortOrder sortOrder = null;
    if (sortBy != null && !sortBy.isEmpty()) {
      // we do not support multi values here - see
      // LuceneUpdateService.addItemValues
      // it would be possible if we use a SortedSetSortField class here
      sortOrder = new SortOrder(sortBy, sortReverse);
    }

    // evaluate default index operator
    DefaultOperator defaultOperator = null;

    if (indexDefaultOperator != null && "OR".equals(indexDefaultOperator.toUpperCase())) {
      defaultOperator = DefaultOperator.OR;
    } else {
      defaultOperator = DefaultOperator.AND;
    }
    return searchService.search(query, pageSize, pageIndex, sortOrder, defaultOperator);

  }

  /**
   * Delegater
   * 
   * @param workitem
   * @return
   */
  @PUT
  @Path("/workitem")
  @Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.APPLICATION_JSON})
  public Response putWorkitem(XMLDocument xmlBusinessEvent) {
    boolean debug = logger.isLoggable(Level.FINE);
    if (debug) {
      logger.fine("putXMLWorkitem @PUT /workitem  delegate to POST....");
    }
    return postWorkitem(xmlBusinessEvent);
  }

  /**
   * The method discovers a service based on the data of a businessEvent. If a service was found the
   * item $api will contain the service endpoint.
   * 
   * @param businessEvent
   */
  @POST
  @Path("/workitem")
  @Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.APPLICATION_JSON})
  public Response postWorkitem(XMLDocument xmlBusinessEvent) {
    // test for null values
    if (xmlBusinessEvent == null) {
      return Response.status(Response.Status.NOT_ACCEPTABLE).build();
    }
    ItemCollection businessEvent = XMLDocumentAdapter.putDocument(xmlBusinessEvent);
    return processBusinessEvent(businessEvent, null);
  }

  @POST
  @Path("/workitem/{uniqueid : ([0-9a-f]{8}-.*|[0-9a-f]{11}-.*)}")
  @Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.APPLICATION_JSON})
  public Response postWorkitemByUniqueID(@PathParam("uniqueid") String uniqueid,
      XMLDocument xmlworkitem) {
    boolean debug = logger.isLoggable(Level.FINE);
    if (debug) {
      logger.fine(
          "postXMLWorkitemByUniqueID @POST /workitem/" + uniqueid + "  method:postWorkitemXML....");
    }
    ItemCollection workitem;
    workitem = XMLDocumentAdapter.putDocument(xmlworkitem);
    return processBusinessEvent(workitem, uniqueid);
  }

  /**
   * This method expects a form post and processes the WorkItem by the WorkflowService EJB.
   * 
   * The Method returns a JSON object with the new data. If a processException Occurs the method
   * returns a JSON object with the error code
   * 
   * The JSON result is computed by the service because JSON is not standardized and differs between
   * different jax-rs implementations. For that reason it can not be directly re-converted
   * XMLItemCollection
   * 
   * generated by this method Output format: <code>
   * ... value":{"@type":"xs:int","$":"10"}
   * </code>
   * 
   * 
   * @param requestBodyStream - form content
   * @return JSON object
   * @throws Exception
   */
  @POST
  @Path("/workitem/typed")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML})
  @Consumes(MediaType.APPLICATION_JSON)
  public Response postTypedWorkitemJSON(InputStream requestBodyStream,
      @QueryParam("error") String error, @QueryParam("encoding") String encoding) {
    boolean debug = logger.isLoggable(Level.FINE);
    if (debug) {
      logger.fine("postTypedWorkitemJSON @POST workitem....");
    }
    // determine encoding from servlet request ....
    if (encoding == null || encoding.isEmpty()) {
      encoding = servletRequest.getCharacterEncoding();
      if (debug) {
        logger.finest("postJSONWorkitem using request econding=" + encoding);
      }
    } else {
      if (debug) {
        logger.finest("postJSONWorkitem set econding=" + encoding);
      }
    }

    ItemCollection workitem = null;
    try {
      workitem = JSONParser.parseWorkitem(requestBodyStream, encoding);
    } catch (ParseException e) {
      logger.severe("postJSONWorkitem wrong json format!");
      e.printStackTrace();
      return Response.status(Response.Status.NOT_ACCEPTABLE).build();
    } catch (UnsupportedEncodingException e) {
      logger.severe("postJSONWorkitem wrong json format!");
      e.printStackTrace();
      return Response.status(Response.Status.NOT_ACCEPTABLE).build();
    }
    if (workitem == null) {
      return Response.status(Response.Status.NOT_ACCEPTABLE).build();
    }

    return processBusinessEvent(workitem, null);
  }

  /**
   * Delegater for PUT postJSONTypedWorkitem
   * 
   * @param workitem
   * @return
   */
  @PUT
  @Path("/workitem/typed")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response putTypedWorkitemJSON(InputStream requestBodyStream,
      @QueryParam("error") String error, @QueryParam("encoding") String encoding) {
    boolean debug = logger.isLoggable(Level.FINE);
    if (debug) {
      logger.finest("putTypedWorkitemJSON @PUT /workitem/{uniqueid}  delegate to POST....");
    }
    return postTypedWorkitemJSON(requestBodyStream, error, encoding);
  }

  @POST
  @Path("/workitem/{uniqueid : ([0-9a-f]{8}-.*|[0-9a-f]{11}-.*)}/typed")
  @Consumes({MediaType.APPLICATION_JSON})
  public Response postTypedWorkitemJSONByUniqueID(@PathParam("uniqueid") String uniqueid,
      InputStream requestBodyStream, @QueryParam("error") String error,
      @QueryParam("encoding") String encoding) {
    boolean debug = logger.isLoggable(Level.FINE);
    if (debug) {
      logger.finest("postJSONWorkitemByUniqueID @POST /workitem/" + uniqueid + "....");
    }
    // determine encoding from servlet request ....
    if (encoding == null || encoding.isEmpty()) {
      encoding = servletRequest.getCharacterEncoding();
      if (debug) {
        logger.finest("postJSONWorkitemByUniqueID using request econding=" + encoding);
      }
    } else {
      if (debug) {
        logger.finest("postJSONWorkitemByUniqueID set econding=" + encoding);
      }
    }

    ItemCollection workitem = null;
    try {
      workitem = JSONParser.parseWorkitem(requestBodyStream, encoding);
    } catch (ParseException e) {
      logger.severe("postJSONWorkitemByUniqueID wrong json format!");
      e.printStackTrace();
      return Response.status(Response.Status.NOT_ACCEPTABLE).build();
    } catch (UnsupportedEncodingException e) {
      logger.severe("postJSONWorkitemByUniqueID wrong json format!");
      e.printStackTrace();
      return Response.status(Response.Status.NOT_ACCEPTABLE).build();
    }
    return processBusinessEvent(workitem, uniqueid);
  }

  /**
   * Delegater for PUT postJSONWorkitemByUniqueID
   * 
   * @param workitem
   * @return
   */
  @PUT
  @Path("/workitem/{uniqueid : ([0-9a-f]{8}-.*|[0-9a-f]{11}-.*)}/typed")
  @Consumes({MediaType.APPLICATION_JSON})
  public Response putTypedWorkitemJSONByUniqueID(@PathParam("uniqueid") String uniqueid,
      InputStream requestBodyStream, @QueryParam("error") String error,
      @QueryParam("encoding") String encoding) {
    boolean debug = logger.isLoggable(Level.FINE);
    if (debug) {
      logger.finest("postJSONWorkitemByUniqueID @PUT /workitem/{uniqueid}  delegate to POST....");
    }
    return postTypedWorkitemJSONByUniqueID(uniqueid, requestBodyStream, error, encoding);
  }

  /**
   * creates a new Instance of a WorkflowClient.
   * <p>
   * In case the current request contains an authentication token we propagate this token to the
   * service.
   * 
   * @see DefaultAuthenicator
   * @return
   */
  private WorkflowClient createWorkflowClient(String serviceAPI) {

    WorkflowClient client = new WorkflowClient(serviceAPI);
    // fire an AuthEvent to register a ClientRequestFilter
    if (authEvents != null) {
      AuthEvent authEvent = new AuthEvent(client, servletRequest);
      authEvents.fire(authEvent);
    } else {
      logger.warning("Missing CDI support for Event<AuthEvent> !");
    }
    return client;
  }

  /**
   * This helper method processes a workitem.
   * <p>
   * In case the businessEvent contains a uid than a search request is delegated to the Solr Index
   * to lookup the existing instance and extract the $api item.
   * <p>
   * In case the businessEvent contains no uid the service will be discovered by the provided
   * business data (e.g. $modelversion, $workflowgroup or business rules). If a service was found
   * the item $api will contain the service endpoint.
   * <p>
   * The response code of the response object is set to 200 if case the processing was successful.
   * In case of an Exception a error message is generated and the status NOT_ACCEPTABLE is returned.
   * <p>
   * This method is called by the POST/PUT methods.
   * 
   * @param workitem
   * @param uid      - optional $uniqueid, will be validated.
   * @return
   */
  private Response processBusinessEvent(ItemCollection businessEvent, String uid) {
    long l = System.currentTimeMillis();
    boolean debug = logger.isLoggable(Level.FINE);
    if (debug) {
      logger.finest("...discover registry.....");
    }
    String serviceAPI = null;

    // validate optional uniqueId
    if (uid != null && !uid.isEmpty() && !businessEvent.getUniqueID().isEmpty()
        && !uid.equals(businessEvent.getUniqueID())) {
      logger.severe("@POST/@PUT workitem/" + uid
          + " : $UNIQUEID did not match, remove $uniqueid to create a new instance!");
      return Response.status(Response.Status.NOT_ACCEPTABLE).build();
    }

    if (uid != null && !uid.isEmpty()) {
      // set provided uniqueid
      businessEvent.replaceItemValue(WorkflowKernel.UNIQUEID, uid);
    }

    if (!businessEvent.getUniqueID().isEmpty()) {
      // lookup process instance by search index!
      try {
        ItemCollection _tmp = null;
        // 1. try a direct search
        _tmp = searchService.getDocument(businessEvent.getUniqueID());
        if (_tmp == null) {
          // 2. try to force an index update - we do not want to wait for the update
          // service...
          if (debug) {
            logger.finest("......workitem '" + businessEvent.getUniqueID()
                + "' not found in index -> force index update...");
          }
          updateService.updateIndex();
          _tmp = searchService.getDocument(businessEvent.getUniqueID());
        }
        // do we found the workitem?
        if (_tmp != null) {
          if (debug) {
            logger.finest("...existing business object found in index, updating $api...");
          }
          // update $api information
          businessEvent.setItemValue(RegistryService.ITEM_API,
              _tmp.getItemValueString(RegistryService.ITEM_API));
        } else {
          // error 404 !
          if (debug) {
            logger
                .finest("......workitem '" + businessEvent.getUniqueID() + "' not found in index.");
          }
          Response.status(Response.Status.NOT_FOUND).build();
        }
      } catch (QueryException e) {
        // no corresponding document found!
        logger.severe(uid + " not found or invalid read access!");
        Response.status(Response.Status.NOT_ACCEPTABLE).build();
      }

    } else {
      // create a new process instance based by calling the discovery service.
      discoveryService.discoverService(businessEvent);
    }

    // if we still have no $api endpoint, then we reject the request
    serviceAPI = businessEvent.getItemValueString(RegistryService.ITEM_API);
    if (serviceAPI.isEmpty()) {
      logger.severe("Invalid workitem - no service endpoint found!");
      return Response.status(Response.Status.NOT_ACCEPTABLE).build();
    }

    ItemCollection workitem = null;
    /*
     * Test the status of the discovered businessEvent object.
     * 
     * If we have a workItem with a $eventId - in this case we need to process the process instance.
     * 
     * In case we have a uniqueid but no $eventId we can return this instance without processing
     * 
     * In case we have no $uniqueId or a $eventID than we process the business event by the given
     * api endpoint.
     * 
     */
    if (!businessEvent.getUniqueID().isEmpty() && businessEvent.getEventID() == 0) {
      // no further processing required
      workitem = businessEvent;
      if (debug) {
        logger.fine("......return existing process instance - " + workitem.getUniqueID());
      }
    } else {
      // we need to process the workitem....
      try {

        if (debug) {
          if (businessEvent.getUniqueID().isEmpty()) {
            logger.fine("......creating a new process instance...");
          } else {
            logger.fine("......processing existing process instance - "
                + businessEvent.getUniqueID() + "...");
          }
        }

        // remote processing
        WorkflowClient workflowClient = createWorkflowClient(serviceAPI);
        workitem = workflowClient.processWorkitem(businessEvent);
        // update the api endpoint
        workitem.setItemValue(RegistryService.ITEM_API,
            serviceAPI + "/workflow/workitem/" + workitem.getUniqueID());
        if (debug) {
          logger.fine("......processed in " + (System.currentTimeMillis() - l) + "ms....");
        }
      } catch (RestAPIException e) {
        workitem = ImixsExceptionHandler.addErrorMessage(e, businessEvent);
        e.printStackTrace();
      }
    }

    // finally we return the result (workitem)
    try {
      if (workitem == null) {
        return Response.status(Response.Status.NOT_ACCEPTABLE).build();
      } else {
        if (workitem.hasItem("$error_code")) {
          logger.severe(workitem.getItemValueString("$error_code") + ": "
              + workitem.getItemValueString("$error_message"));
          return Response.ok(XMLDataCollectionAdapter.getDataCollection(workitem))
              .status(Response.Status.NOT_ACCEPTABLE).build();
        } else {
          // return workitem....
          return Response.ok(XMLDataCollectionAdapter.getDataCollection(workitem)).build();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.NOT_ACCEPTABLE).build();
    }
  }

  /**
   * This method converts a single ItemCollection into a Jax-rs response object.
   * <p>
   * The method expects optional items and format string (json|xml)
   * <p>
   * In case the result set is null, than the method returns an empty collection.
   * 
   * @param result list of ItemCollection
   * @param items  - optional item list
   * @param format - optional format string (json|xml)
   * @return jax-rs Response object.
   */
  private Response convertResult(ItemCollection workitem, String items, String format) {
    if (workitem == null) {
      workitem = new ItemCollection();
    }
    if ("json".equals(format)) {
      return Response
          // Set the status and Put your entity here.
          .ok(XMLDataCollectionAdapter.getDataCollection(workitem, getItemList(items)))
          // Add the Content-Type header to tell Jersey which format it should marshall
          // the entity into.
          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).build();
    } else if ("xml".equals(format)) {
      return Response
          // Set the status and Put your entity here.
          .ok(XMLDataCollectionAdapter.getDataCollection(workitem, getItemList(items)))
          // Add the Content-Type header to tell Jersey which format it should marshall
          // the entity into.
          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML).build();
    } else {
      // default header param
      return Response
          // Set the status and Put your entity here.
          .ok(XMLDataCollectionAdapter.getDataCollection(workitem, getItemList(items))).build();
    }
  }

  /**
   * This method converts a ItemCollection List into a Jax-rs response object.
   * <p>
   * The method expects optional items and format string (json|xml)
   * <p>
   * In case the result set is null, than the method returns an empty collection.
   * 
   * @param result list of ItemCollection
   * @param items  - optional item list
   * @param format - optional format string (json|xml)
   * @return jax-rs Response object.
   */
  private Response convertResultList(Collection<ItemCollection> result, String items,
      String format) {
    if (result == null) {
      result = new ArrayList<ItemCollection>();
    }
    if ("json".equals(format)) {
      return Response
          // Set the status and Put your entity here.
          .ok(XMLDataCollectionAdapter.getDataCollection(result, getItemList(items)))
          // Add the Content-Type header to tell Jersey which format it should marshall
          // the entity into.
          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).build();
    } else if ("xml".equals(format)) {
      return Response
          // Set the status and Put your entity here.
          .ok(XMLDataCollectionAdapter.getDataCollection(result, getItemList(items)))
          // Add the Content-Type header to tell Jersey which format it should marshall
          // the entity into.
          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML).build();
    } else {
      // default header param
      return Response
          // Set the status and Put your entity here.
          .ok(XMLDataCollectionAdapter.getDataCollection(result, getItemList(items))).build();
    }
  }

  /**
   * This method returns a List object from a given comma separated string. The method returns null
   * if no elements are found. The provided parameter looks typical like this: <code>
   *   txtWorkflowStatus,numProcessID,txtName
   * </code>
   * 
   * @param items
   * @return
   */
  protected static List<String> getItemList(String items) {
    if (items == null || "".equals(items))
      return null;
    Vector<String> v = new Vector<String>();
    StringTokenizer st = new StringTokenizer(items, ",");
    while (st.hasMoreTokens())
      v.add(st.nextToken());
    return v;
  }

}

/*******************************************************************************
 *  Imixs Workflow 
 *  Copyright (C) 2001, 2011 Imixs Software Solutions GmbH,  
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
 *  	http://www.imixs.org
 *  	http://java.net/projects/imixs-workflow
 *  
 *  Contributors:  
 *  	Imixs Software Solutions GmbH - initial API and implementation
 *  	Ralph Soika - Software Developer
 *******************************************************************************/

package org.imixs.registry.api;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.imixs.registry.index.DefaultOperator;
import org.imixs.registry.index.SearchService;
import org.imixs.registry.index.SortOrder;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.xml.XMLDataCollectionAdapter;

/**
 * This api endpoint index provides methods to search the derived index managed
 * by the registry
 * 
 * @author rsoika
 */
@Path("/")
@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.TEXT_XML })
@Singleton
public class IndexRestService {

	@SuppressWarnings("unused")
	private static Logger logger = Logger.getLogger(IndexRestService.class.getName());

	@Inject
	protected SearchService searchService;

	@Resource
	SessionContext ctx;

	/**
	 * returns a single document defined by $uniqueid
	 * 
	 * Regex for
	 * 
	 * UID - e.g: bcc776f9-4e5a-4272-a613-9f5ebf35354d
	 * 
	 * Snapshot: bcc776f9-4e5a-4272-a613-9f5ebf35354d-9b6655
	 * 
	 * deprecated format : 132d37bfd51-9a7868
	 * 
	 * @param uniqueid
	 * @return
	 */
	@GET
	@Path("/documents/{uniqueid : ([0-9a-f]{8}-.*|[0-9a-f]{11}-.*)}")
	public Response getDocument(@PathParam("uniqueid") String uniqueid, @QueryParam("items") String items,
			@QueryParam("format") String format) {
		try {
			// build search term by uniqueid....
			String searchTerm = WorkflowKernel.UNIQUEID + ":\"" + uniqueid + "\"";
			List<ItemCollection> result = searchService.search(searchTerm, 1, 0, null, DefaultOperator.AND);
			if (result != null && result.size() > 0) {
				ItemCollection doc = result.get(0);

				return convertResult(doc, items, format);
			}

			// not found
			return Response.status(Response.Status.NOT_FOUND).build();

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Just an alternative GET method for documents/unqiueid
	 * @param uniqueid
	 * @param items
	 * @param format
	 * @return
	 */
	@GET
	@Path("/workflow/workitem/{uniqueid : ([0-9a-f]{8}-.*|[0-9a-f]{11}-.*)}")
	public Response getWorkitem(@PathParam("uniqueid") String uniqueid, @QueryParam("items") String items,
			@QueryParam("format") String format) {
		return getDocument(uniqueid, items, format);
	}

	@GET
	@Path("/workflow/tasklist/creator/{creator}")
	public Response getTaskListByCreator(@PathParam("creator") String creator, @QueryParam("type") String type,
			@DefaultValue("0") @QueryParam("pageIndex") int pageIndex,
			@DefaultValue("10") @QueryParam("pageSize") int pageSize,
			@DefaultValue("") @QueryParam("sortBy") String sortBy,
			@DefaultValue("false") @QueryParam("sortReverse") Boolean sortReverse, @QueryParam("items") String items,
			@QueryParam("format") String format) {
		List<ItemCollection> result = null;
		try {
			if ("null".equalsIgnoreCase(creator))
				creator = null;

			// decode URL param
			if (creator != null)
				creator = URLDecoder.decode(creator, "UTF-8");

			if (creator == null || "".equals(creator))
				creator = ctx.getCallerPrincipal().getName();

			String searchTerm = "(";
			if (type != null && !"".equals(type)) {
				searchTerm += " type:\"" + type + "\" AND ";
			}
			searchTerm += " $creator:\"" + creator + "\" )";

			SortOrder sortOrder = null;
			if (sortBy!=null && !sortBy.isEmpty()) {
				sortOrder=new SortOrder(sortBy, sortReverse);
			}

			result = searchService.search(searchTerm, pageSize, pageIndex, sortOrder, DefaultOperator.AND);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return convertResultList(result, items, format);
	}

	/**
	 * This method converts a single ItemCollection into a Jax-rs response object.
	 * <p>
	 * The method expects optional items and format string (json|xml)
	 * <p>
	 * In case the result set is null, than the method returns an empty collection.
	 * 
	 * @param result
	 *            list of ItemCollection
	 * @param items
	 *            - optional item list
	 * @param format
	 *            - optional format string (json|xml)
	 * @return jax-rs Response object.
	 */
	public Response convertResult(ItemCollection workitem, String items, String format) {
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
	 * @param result
	 *            list of ItemCollection
	 * @param items
	 *            - optional item list
	 * @param format
	 *            - optional format string (json|xml)
	 * @return jax-rs Response object.
	 */
	private Response convertResultList(List<ItemCollection> result, String items, String format) {
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
	 * This method returns a List object from a given comma separated string. The
	 * method returns null if no elements are found. The provided parameter looks
	 * typical like this: <code>
	 *   txtWorkflowStatus,numProcessID,txtName
	 * </code>
	 * 
	 * @param items
	 * @return
	 */
	private static List<String> getItemList(String items) {
		if (items == null || "".equals(items))
			return null;
		Vector<String> v = new Vector<String>();
		StringTokenizer st = new StringTokenizer(items, ",");
		while (st.hasMoreTokens())
			v.add(st.nextToken());
		return v;
	}

}
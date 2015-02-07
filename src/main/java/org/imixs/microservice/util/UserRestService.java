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

package org.imixs.microservice.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.imixs.workflow.jee.ejb.WorkflowService;
import org.imixs.workflow.util.JSONParser;
import org.imixs.workflow.xml.EntityCollection;
import org.imixs.workflow.xml.XMLItem;
import org.imixs.workflow.xml.XMLItemCollection;
import org.imixs.workflow.xml.XMLItemCollectionAdapter;

/**
 * The WorkflowService Handler supports methods to process different kind of
 * request URIs
 * 
 * @author rsoika
 * 
 */
@Path("/user")
@Produces({ "text/html", "application/xml", "application/json" })
@Stateless
public class UserRestService {

	@EJB
	private WorkflowService workflowService;

	@javax.ws.rs.core.Context
	private static HttpServletRequest servletRequest;

	private static Logger logger = Logger.getLogger(UserRestService.class
			.getName());

	/**
	 * This method expects a form post and processes the WorkItem by the
	 * WorkflowService EJB.
	 * 
	 * The Method returns a JSON object with the new data. If a processException
	 * Occurs the method returns a JSON object with the error code
	 * 
	 * 
	 * @param requestBodyStream
	 *            - form content
	 * @return JSON object
	 * @throws Exception
	 */
	@POST
	@Path("/user.json")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response postWorkitemJSON(InputStream requestBodyStream,
		
			@QueryParam("encoding") String encoding) {

		logger.fine("[WorkflowRestService] @PUT /workitem  method:putWorkitemJSON....");

		
		// determine encoding from servlet request ....
		if (encoding==null || encoding.isEmpty()) {
			encoding=servletRequest.getCharacterEncoding();
			logger.fine("[WorkflowRestService] postWorkitemJSON using request econding=" + encoding);
		} else {
			logger.fine("[WorkflowRestService] postWorkitemJSON set econding=" + encoding);
		}
		// set defautl encoding UTF-8
		if (encoding==null || encoding.isEmpty()) {
			encoding="UTF-8";
			logger.fine("[WorkflowRestService] postWorkitemJSON no encoding defined, set default econding to" + encoding);
		}
		
		ItemCollection workitem = null;
		XMLItemCollection responseWorkitem = null;
		try {
			workitem = JSONParser.parseWorkitem(requestBodyStream,encoding);

		} catch (ParseException e) {
			logger.severe("postWorkitemJSON wrong json format!");
			e.printStackTrace();
			return Response.status(Response.Status.NOT_ACCEPTABLE).build();
		} catch (UnsupportedEncodingException e) {
			logger.severe("postWorkitemJSON wrong json format!");
			e.printStackTrace();
			return Response.status(Response.Status.NOT_ACCEPTABLE).build();
		}

		if (workitem != null) {
			// now test if an workItem with this $uniqueId still exits...
			String unid = workitem.getItemValueString("$uniqueID");
			if (!"".equals(unid)) {

				ItemCollection oldWorkitem = workflowService.getWorkItem(unid);
				if (oldWorkitem != null) {
					// an instance of this WorkItem still exists! so we need
					// to update the new values....
					oldWorkitem.getAllItems().putAll(workitem.getAllItems());
					workitem = oldWorkitem;
				}
			}
		}

		if (workitem == null) {
			return Response.status(Response.Status.NOT_ACCEPTABLE).build();
		}

		try {
			// now lets try to process the workitem...
			workitem = workflowService.processWorkItem(workitem);

			try {
				responseWorkitem = XMLItemCollectionAdapter
						.putItemCollection(workitem);
			} catch (Exception e1) {
				e1.printStackTrace();
				logger.warning("[WorkflowRestService] PostWorkitem failed: "
						+ e1.getMessage());
				return Response.status(Response.Status.NOT_ACCEPTABLE)
						.type(MediaType.APPLICATION_JSON).build();

			}
		} catch (AccessDeniedException e) {
			logger.warning("[WorkflowRestService] PostWorkitem failed: "
					+ e.getMessage());
			return Response.status(Response.Status.NOT_ACCEPTABLE)
					.type(MediaType.APPLICATION_JSON).entity(responseWorkitem)
					.build();
		} catch (ProcessingErrorException e) {
			logger.warning("[WorkflowRestService] PostWorkitem failed: "
					+ e.getMessage());
			return Response.status(Response.Status.NOT_ACCEPTABLE)
					.type(MediaType.APPLICATION_JSON).entity(responseWorkitem)
					.build();
		} catch (PluginException e) {
			// test for error code
			logger.warning("[WorkflowRestService] PostWorkitem failed: "
					+ e.getMessage());
			
				return Response.status(Response.Status.NOT_ACCEPTABLE)
						.type(MediaType.APPLICATION_JSON)
						.entity(responseWorkitem).build();
			
		}

		// success HTTP 200
		return Response.ok(responseWorkitem, MediaType.APPLICATION_JSON)
				.build();

	}

	/**
	 * This method expects a form post. The method parses the input stream to
	 * extract the provides field/value pairs. NOTE: The method did not(!)
	 * assume that the put/post request contains a complete workItem. For this
	 * reason the method loads the existing instance of the corresponding
	 * workItem (identified by the $unqiueid) and adds the values provided by
	 * the put/post request into the existing instance.
	 * 
	 * The following kind of lines which can be included in the InputStream will
	 * be skipped
	 * 
	 * <code>
	 * 	------------------------------1a26f3661ff7
		Content-Disposition: form-data; name="query"
		Connection: keep-alive
		Content-Type: multipart/form-data; boundary=---------------------------195571638125373
		Content-Length: 5680

		-----------------------------195571638125373
	 * </code>
	 * 
	 * @param requestBodyStream
	 * @return a workitem
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public final static ItemCollection parseWorkitem(
			InputStream requestBodyStream) {
		Vector<String> vMultiValueFieldNames = new Vector<String>();
		BufferedReader in = new BufferedReader(new InputStreamReader(
				requestBodyStream));
		String inputLine;
		ItemCollection workitem = new ItemCollection();

		logger.fine("[WorkflowRestService] parseWorkitem....");

		try {
			while ((inputLine = in.readLine()) != null) {
				// System.out.println(inputLine);

				// split params separated by &
				StringTokenizer st = new StringTokenizer(inputLine, "&", false);
				while (st.hasMoreTokens()) {
					String fieldValue = st.nextToken();
					logger.finest("[WorkflowRestService] parse line:"
							+ fieldValue + "");
					try {
						fieldValue = URLDecoder.decode(fieldValue, "UTF-8");

						if (!fieldValue.contains("=")) {
							logger.finest("[WorkflowRestService] line will be skipped");
							continue;
						}

						// get fieldname
						String fieldName = fieldValue.substring(0,
								fieldValue.indexOf('='));

						// if fieldName contains blank or : or --- we skipp the
						// line
						if (fieldName.contains(":") || fieldName.contains(" ")
								|| fieldName.contains(";")) {
							logger.finest("[WorkflowRestService] line will be skipped");
							continue;
						}

						// test for value...
						if (fieldValue.indexOf('=') == fieldValue.length()) {
							// no value
							workitem.replaceItemValue(fieldName, "");
							logger.fine("[WorkflowRestService] no value for '"
									+ fieldName + "'");
						} else {
							fieldValue = fieldValue.substring(fieldValue
									.indexOf('=') + 1);
							// test for a multiValue field - did we know
							// this
							// field....?
							fieldName = fieldName.toLowerCase();
							if (vMultiValueFieldNames.indexOf(fieldName) > -1) {

								List v = workitem.getItemValue(fieldName);
								v.add(fieldValue);
								logger.fine("[WorkflowRestService] multivalue for '"
										+ fieldName
										+ "' = '"
										+ fieldValue
										+ "'");
								workitem.replaceItemValue(fieldName, v);
							} else {
								// first single value....
								logger.fine("[WorkflowRestService] value for '"
										+ fieldName + "' = '" + fieldValue
										+ "'");
								workitem.replaceItemValue(fieldName, fieldValue);
								vMultiValueFieldNames.add(fieldName);
							}
						}

					} catch (NumberFormatException e) {
						e.printStackTrace();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			}
		} catch (IOException e1) {
			logger.severe("[WorkflowRestService] Unable to parse workitem data!");
			e1.printStackTrace();
			return null;
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		return workitem;
	}

	/**
	 * This method returns a List object from a given comma separated string.
	 * The method returns null if no elements are found. The provided parameter
	 * looks typical like this: <code>
	 *   txtWorkflowStatus,numProcessID,txtName
	 * </code>
	 * 
	 * @param items
	 * @return
	 */
	private List<String> getItemList(String items) {
		if (items == null || "".equals(items))
			return null;
		Vector<String> v = new Vector<String>();
		StringTokenizer st = new StringTokenizer(items, ",");
		while (st.hasMoreTokens())
			v.add(st.nextToken());
		return v;
	}
}

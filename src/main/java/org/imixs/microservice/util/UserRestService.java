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

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.imixs.marty.ejb.security.UserGroupService;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.util.JSONParser;
import org.imixs.workflow.xml.XMLItemCollection;
import org.imixs.workflow.xml.XMLItemCollectionAdapter;

/**
 * The WorkflowService Handler supports methods to manage user accounts to
 * access the Imixs-Microservice platform. This service is based on the Marty
 * UserGroupService.
 * 
 * @see org.imixs.marty.ejb.security.UserGroupService
 * @author rsoika
 * 
 */
@Path("/user")
@Produces({ "text/html", "application/xml", "application/json" })
@Stateless
public class UserRestService {

	@EJB
	UserGroupService userGroupService;
	
	@EJB
	DocumentService documentService;
	
	@javax.ws.rs.core.Context
	private static HttpServletRequest servletRequest;

	private static Logger logger = Logger.getLogger(UserRestService.class.getName());

	/**
	 * This method expects a JSON request object and creates or updates a corresponding
	 * user profile by calling the  the WorkItem by the userGroupService.
	 * 
	 * The Method returns a JSON object with the new data. If a processException
	 * Occurs the method returns a JSON object with the error code
	 * 
	 * <code>
	 * {"item":[    
	 * 		{"name":"type","value":{"@type":"xs:string","$":"profile"}},     
	 * 		{"name":"txtname","value":{"@type":"xs:string","$":"eddy"}},     
	 * 		{"name":"txtpassword","value":{"@type":"xs:string","$":"imixs"}},     
	 * 		{"name":"txtgroups","value":{"@type":"xs:string","$":"IMIXS-WORKFLOW-Author"}}
	 * ]}
	 * </code>
	 * 
	 * 
	 * @param requestBodyStream
	 *            - form content
	 * @return JSON object
	 * @throws Exception
	 */
	@POST
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response postWorkitemJSON(InputStream requestBodyStream, @QueryParam("encoding") String encoding) {

		logger.fine("[UserRestService] @PUT /workitem  method:putWorkitemJSON....");

		// determine encoding from servlet request ....
		if (encoding == null || encoding.isEmpty()) {
			encoding = servletRequest.getCharacterEncoding();
			logger.fine("[UserRestService] postWorkitemJSON using request econding=" + encoding);
		} else {
			logger.fine("[UserRestService] postWorkitemJSON set econding=" + encoding);
		}
		// set defautl encoding UTF-8
		if (encoding == null || encoding.isEmpty()) {
			encoding = "UTF-8";
			logger.fine("[UserRestService] postWorkitemJSON no encoding defined, set default econding to" + encoding);
		}

		ItemCollection workitem = null;
		XMLItemCollection responseWorkitem = null;
		try {
			workitem = JSONParser.parseWorkitem(requestBodyStream, encoding);
			responseWorkitem = XMLItemCollectionAdapter.putItemCollection(workitem);
		} catch (ParseException e) {
			logger.severe("postWorkitemJSON wrong json format!");
			e.printStackTrace();
			return Response.status(Response.Status.NOT_ACCEPTABLE).build();
		} catch (UnsupportedEncodingException e) {
			logger.severe("postWorkitemJSON wrong json format!");
			e.printStackTrace();
			return Response.status(Response.Status.NOT_ACCEPTABLE).build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(Response.Status.NOT_ACCEPTABLE).build();

		}

		if (workitem != null) {
			userGroupService.updateUser(workitem);
		} else {
			return Response.status(Response.Status.NOT_ACCEPTABLE).build();
		}

		// success HTTP 200
		return Response.ok(responseWorkitem, MediaType.APPLICATION_JSON).build();

	}
	

}

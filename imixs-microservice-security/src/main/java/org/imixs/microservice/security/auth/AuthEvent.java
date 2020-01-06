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

package org.imixs.microservice.security.auth;

import javax.servlet.http.HttpServletRequest;
import org.imixs.melman.AbstractClient;

/**
 * The AuthEvent provides a CDI observer pattern. The AuthEvent is fired by the RegistryService EJB.
 * An event Observer can react on this event to register a authentication filter by calling the
 * method registerClientRequestFilter.
 * <p>
 * See also the Imixs-Melman project
 * 
 * 
 * @author Ralph Soika
 * @version 1.0
 * @see org.imixs.microservice.core.RegistrySelfRegistrationService
 */
public class AuthEvent {

  private AbstractClient client = null;
  private HttpServletRequest request = null;

  public AuthEvent(AbstractClient _client, HttpServletRequest _request) {
    super();
    setClient(_client);
    setRequest(_request);
  }

  public AbstractClient getClient() {
    return client;
  }

  public void setClient(AbstractClient _client) {
    this.client = _client;
  }

  public HttpServletRequest getRequest() {
    return request;
  }

  public void setRequest(HttpServletRequest request) {
    this.request = request;
  }

}

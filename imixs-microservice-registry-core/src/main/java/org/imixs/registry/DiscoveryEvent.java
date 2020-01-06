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

package org.imixs.registry;

import org.imixs.workflow.ItemCollection;

/**
 * The DiscoveryEvent provides a CDI observer pattern. The DiscoveryEvent is fired by the
 * DiscoveryService EJB. An event Observer can react on the different phases during the discovery
 * process.
 * 
 * The DiscoveryEvent defines the following event types:
 * <ul>
 * <li>BEFORE_DISCOVERY - send immediately before the discovery process is started
 * <li>AFTER_DISCOVERY - send immediately after a discovery was successful finished
 * <li>ON_FAILURE - send in case the discovery process failed
 * </ul>
 * 
 * @author Ralph Soika
 * @version 1.0
 * @see org.imixs.workflow.engine.WorkflowService
 */
public class DiscoveryEvent {

  public static final int BEFORE_DISCOVERY = 1;
  public static final int AFTER_DISCOVERY = 2;
  public static final int ON_FAILURE = 3;

  private int eventType;
  private ItemCollection document;

  public DiscoveryEvent(ItemCollection document, int eventType) {
    this.eventType = eventType;
    this.document = document;
  }

  public int getEventType() {
    return eventType;
  }

  public ItemCollection getDocument() {
    return document;
  }

}

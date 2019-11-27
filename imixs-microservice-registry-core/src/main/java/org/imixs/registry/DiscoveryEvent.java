package org.imixs.registry;

import org.imixs.workflow.ItemCollection;

/**
 * The DiscoveryEvent provides a CDI observer pattern. The DiscoveryEvent is
 * fired by the DiscoveryService EJB. An event Observer can react on the
 * different phases during the discovery process.
 * 
 * The DiscoveryEvent defines the following event types:
 * <ul>
 * <li>BEFORE_DISCOVERY - send immediately before the discovery process is
 * started
 * <li>AFTER_DISCOVERY - send immediately after a discovery was successful
 * finished
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

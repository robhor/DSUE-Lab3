package analytics.impl;

import java.rmi.RemoteException;

import analytics.Subscriber;
import analytics.event.Event;

/**
 * Wrapper class for subscribers which swallows all events that do
 * not match a regex filter.
 */
public class FilteringSubscriber implements Subscriber {
	private final String filter;
	private final Subscriber next;
	
	/**
	 * Constructor.
	 * @param filter Filter regex pattern
	 * @param next Subscriber to forward to
	 */
	public FilteringSubscriber(String filter, Subscriber next) {
		this.filter = filter;
		this.next = next;
	}

	/**
	 * Process an event.
	 * @param event The event
	 * @throws RemoteException if a remoting error occurs
	 */
	@Override
	public void processEvent(Event event) throws RemoteException {
		if (event.getType().matches(filter)) {
			next.processEvent(event);
		}
	}

}

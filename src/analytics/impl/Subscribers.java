package analytics.impl;

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import analytics.AnalyticsException;
import analytics.Subscriber;
import analytics.event.Event;

/**
 * Maintains a list of connected subscribers to the analytics service.
 * All operations in this class are thread-safe.
 */
public class Subscribers {
	private final Map<UUID, Subscriber> subscribers;
	
	/**
	 * Constructor.
	 */
	public Subscribers() {
		subscribers = Collections.synchronizedMap(new HashMap<UUID, Subscriber> ());
	}
	
	/**
	 * Adds a subscriber to the list.
	 * @param subscriber The subscriber
	 * @return the new subscriber's unique identifier
	 */
	public UUID add(Subscriber subscriber) {
		UUID uuid = UUID.randomUUID();
		Subscriber prev = subscribers.put(uuid, subscriber);
		
		if (prev != null) {
			throw new RuntimeException("Duplicate UUID for subscriber.");
		}
		
		return uuid;
	}
	
	/**
	 * Removes a subscriber from the list.
	 * @param uuid Identifier of the subscription as returned by add()
	 * @throws AnalyticsException if the uuid could not be found
	 */
	public void remove(UUID uuid) throws AnalyticsException {
		Subscriber prev = subscribers.remove(uuid);
		
		if (null == prev) {
			throw new AnalyticsException(String.format("Unsubscribe failed: unknown ID \"%s\"."));
		}
	}
	
	/**
	 * Publishes an event to all subscribers of the service.
	 * As a side effect, all subscribers which encounter connection problems
	 * are automatically unsubscribed from the service.
	 * 
	 * Thoughts on performance: This method will block until all subscribers have confirmed
	 *   receiving the event by returning from processEvent(). This is currently a huge
	 *   potential risk and performance hazard which could be handled by assigning publishing
	 *   threads to all subscribers separately.
	 * 
	 * @param event The event
	 */
	public void publish(Event event) {
		synchronized(subscribers) {
			for(Iterator<Map.Entry<UUID, Subscriber>> it = subscribers.entrySet().iterator(); it.hasNext(); ) {
				Map.Entry<UUID, Subscriber> entry = it.next();
				try {
					entry.getValue().processEvent(event);
				} catch (RemoteException e) {
					try {
						remove(entry.getKey());
					} catch (AnalyticsException e1) {
						throw new RuntimeException(e1);
					}
				}
			}	
		}
	}
}

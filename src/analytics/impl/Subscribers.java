package analytics.impl;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import analytics.AnalyticsException;
import analytics.Subscriber;
import analytics.event.Event;

/**
 * Maintains a list of connected subscribers to the analytics service.
 * All operations in this class are thread-safe.
 */
public class Subscribers {
	private static final Logger logger = Logger.getLogger("Subscribers");
	
	private final Map<String, Subscriber> subscribers;
	private static int nextId = 1;
	private static Object idLock = new Object(); 
	
	/**
	 * Constructor.
	 */
	public Subscribers() {
		subscribers = Collections.synchronizedMap(new HashMap<String, Subscriber> ());
	}
	
	/**
	 * Adds a subscriber to the list.
	 * @param subscriber The subscriber
	 * @return the new subscriber's unique identifier
	 */
	public String add(Subscriber subscriber) {
		String id;
		synchronized(idLock) {
			id = Integer.toString(nextId++);
		}
		Subscriber prev = subscribers.put(id, subscriber);
		
		if (prev != null) {
			throw new RuntimeException("Duplicate ID for subscriber.");
		}
		
		return id;
	}
	
	/**
	 * Removes a subscriber from the list.
	 * @param id Identifier of the subscription as returned by add()
	 * @throws AnalyticsException if the id could not be found
	 */
	public void remove(String id) throws AnalyticsException {
		Subscriber prev = subscribers.remove(id);
		
		if (null == prev) {
			throw new AnalyticsException(String.format("Unsubscribe failed: unknown ID \"%s\"."));
		}
	}
	
	/**
	 * Publishes an event to all subscribers of the service.
	 * As a side effect, all subscribers which encounter connection problems
	 * are automatically unsubscribed from the service.
	 * 
	 * @param event The event
	 */
	public void publish(Event event) {
		synchronized(subscribers) {
			List<String> toRemove = new ArrayList<String> ();
			
			for(Iterator<Map.Entry<String, Subscriber>> it = subscribers.entrySet().iterator(); it.hasNext(); ) {
				Map.Entry<String, Subscriber> entry = it.next();
				try {
					entry.getValue().processEvent(event);
				} catch (RemoteException e) {
					logger.warning(String.format("Subscriber %s unreachable.", entry.getKey()));
					toRemove.add(entry.getKey());
				}
			}
			
			for(Iterator<String> it = toRemove.iterator(); it.hasNext(); ) {
				try {
					remove(it.next());
				} catch (AnalyticsException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}
}

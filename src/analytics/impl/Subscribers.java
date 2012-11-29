package analytics.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import analytics.AnalyticsException;
import analytics.Subscriber;
import analytics.event.Event;

/**
 * Maintains a list of connected subscribers to the analytics service.
 * All operations in this class are thread-safe.
 */
public class Subscribers {
	private final Map<String, Subscriber> subscribers;
	private final ExecutorService publishers;
	private static int nextId = 1;
	private static Object idLock = new Object(); 
	
	/**
	 * Constructor.
	 */
	public Subscribers() {
		subscribers = Collections.synchronizedMap(new HashMap<String, Subscriber> ());
		publishers = Executors.newCachedThreadPool();
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
			throw new AnalyticsException(String.format("Unsubscribe failed: unknown ID \"%s\".", id));
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
			for(Map.Entry<String, Subscriber> entry : subscribers.entrySet()) {
				PublishTask task = new PublishTask(this, entry.getKey(), entry.getValue(), event);
				publishers.execute(task);
			}
		}
	}
	
	public void shutdown() {
		publishers.shutdown();
	}
}

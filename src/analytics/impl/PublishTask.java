package analytics.impl;

import java.rmi.RemoteException;
import java.util.logging.Logger;

import analytics.AnalyticsException;
import analytics.Subscriber;
import analytics.event.Event;

/**
 * Sends an event to a subscriber.
 */
public class PublishTask implements Runnable {
	private static final Logger logger = Logger.getLogger("PublishTask");
	
	private Subscribers subscribers;
	private String subscriptionID;
	private Subscriber subscriber;
	private Event event;
	
	public PublishTask(Subscribers subscribers, String subscriptionID, Subscriber subscriber, Event event) {
		this.subscribers = subscribers;
		this.subscriptionID = subscriptionID;
		this.subscriber = subscriber;
		this.event = event;
	}
	
	@Override
	public void run() {
		try {
			subscriber.processEvent(event);
		} catch (RemoteException e) {
			logger.warning(String.format("Subscriber %s unreachable.", subscriptionID));
			try {
				subscribers.remove(subscriptionID);
			} catch (AnalyticsException e1) {
				// ignore
			}
		}
	}

}

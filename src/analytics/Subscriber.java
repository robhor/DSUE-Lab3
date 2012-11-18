package analytics;

import java.rmi.Remote;
import java.rmi.RemoteException;

import analytics.bean.Event;

/**
 * Subscribers to the AnalyticsServer must implement this
 * interface. Subscribers must be thread-safe.
 */
public interface Subscriber extends Remote {
	/**
	 * Process an event.
	 * @param event The event
	 * @throws RemoteException if a remoting error occurs
	 */
	void processEvent(Event event) throws RemoteException;
}

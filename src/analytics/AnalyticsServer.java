package analytics;

import java.rmi.Remote;
import java.rmi.RemoteException;

import analytics.bean.Event;

/**
 * The analytics server processes auction events and
 * forwards those events plus additional statistical
 * events to all subscribers.
 */
public interface AnalyticsServer extends Remote {
	/**
	 * Adds a subscriber to the list of subscribers.
	 * @param filter Regular expression filter for events
	 * @param subscriber Subscriber
	 * @return A unique subscription identifier string to be used for unsubscribing
	 * @throws RemoteException if a remoting error occurs
	 */
	String subscribe(String filter, Subscriber subscriber) throws RemoteException;
	
	/**
	 * Processes an event. Even though this method is thread-safe,
	 * some events which depend on each other must not be called
	 * out of sequence! A dependent event can only be processed
	 * after the processEvent() call for its dependency has
	 * returned.
	 * This affects:
	 * AUCTION_STARTED > [BID_PLACED] > AUCTION_ENDED
	 * BID_PLACED > [BID_OVERBID] > BID_WON
	 * USER_LOGIN > (USER_LOGOUT | USER_DISCONNECTED)
	 * 
	 * @param event The event
	 * @throws RemoteException if a remoting error occurs
	 */
	void processEvent(Event event) throws RemoteException, AnalyticsException;
	
	/**
	 * Removes a subscription.
	 * @param identifier The unique subscription identifier previously
	 *                    returned from subscribe() 
	 * @throws RemoteException if a remoting error occurs
	 */
	void unsubscribe(String identifier) throws RemoteException, AnalyticsException;
}

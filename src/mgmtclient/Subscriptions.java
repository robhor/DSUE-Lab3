package mgmtclient;

import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Set;

import analytics.AnalyticsException;
import analytics.AnalyticsServer;
import analytics.Subscriber;

public class Subscriptions {
	AnalyticsServer analyticsServer;
	private Subscriber subscriber;
	private Set<String> identifiers;
	
	public Subscriptions(AnalyticsServer analyticsServer, Subscriber subscriber) {
		this.analyticsServer = analyticsServer;
		this.subscriber = subscriber;
		identifiers = new HashSet<String> ();
	}
	
	public String add(String filter) throws RemoteException, ManagementException, AnalyticsException {
		if (null == analyticsServer) {
			throw new ManagementException("Analytics server is not available.");
		}
		
		String identifier = analyticsServer.subscribe(filter, subscriber);
		
		if (identifiers.contains(identifier)) {
			throw new ManagementException("Received duplicate identifiers from analytics server.");
		}
			
		identifiers.add(identifier);
		return identifier;
	}
	
	public void remove(String identifier) throws RemoteException, AnalyticsException, ManagementException {
		if (null == analyticsServer) {
			throw new ManagementException("Analytics server is not available.");
		}
		
		if (!identifiers.contains(identifier)) {
			String message = String.format("Failed to remove subscription with ID %s: ID not found.", identifier);
			throw new ManagementException(message);
		}
		
		analyticsServer.unsubscribe(identifier);
		identifiers.remove(identifier);
	}
	
	public void clear() throws RemoteException, ManagementException {
		if (null == analyticsServer) {
			return;
		}
		
		for (String identifier : identifiers) {
			try {
				analyticsServer.unsubscribe(identifier);	
			} catch (AnalyticsException e) {
				String message = String.format("Failed to unsubscribe from analytics with id %s.", identifier);
				throw new ManagementException(message, e);
			}
		}
		identifiers.clear();
	}
}

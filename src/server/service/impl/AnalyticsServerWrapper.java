package server.service.impl;

import java.rmi.RemoteException;
import java.util.logging.Logger;

import analytics.AnalyticsException;
import analytics.AnalyticsServer;
import analytics.event.Event;

/**
 * Calls an analytics server with exception handling.
 */
public class AnalyticsServerWrapper {
	private static Logger logger = Logger.getLogger("AnalyticsServerWrapper");
	
	private AnalyticsServer server;
	
	public AnalyticsServerWrapper(AnalyticsServer server) {
		this.server = server;
	}
	
	public void processEvent(Event event) {
		if (null == server) {
			return;
		}
		
		try {
			server.processEvent(event);
		} catch (RemoteException e) {
			logger.warning(String.format("Failed to call analytics server: %s", e.toString()));
		} catch (AnalyticsException e) {
			logger.warning(String.format("Failed to call analytics server: %s", e.toString()));
		}
	}
}

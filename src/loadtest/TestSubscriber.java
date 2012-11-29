package loadtest;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Properties;
import java.util.logging.Logger;

import util.PropertyReader;
import analytics.AnalyticsException;
import analytics.AnalyticsServer;
import mgmtclient.EventSink;

public class TestSubscriber implements Runnable {
	private static final Logger logger = Logger.getLogger("TestSubscriber");

	private EventSink eventSink;
	private AnalyticsServer analyticsServer;
	private String subscriptionId;
	
	public TestSubscriber(String analyticsBindingName) throws RemoteException, LoadTestException
	{
		subscriptionId = null;
		eventSink = new EventSink();

		try {
			Properties registryProps = PropertyReader.readProperties("registry.properties");
			if (null == registryProps ) {
				throw new LoadTestException("Could not read properties");
			}
			
			String host = registryProps.getProperty("registry.host");
			int port = Integer.parseInt(registryProps.getProperty("registry.port"));
			Registry reg = LocateRegistry.getRegistry(host, port);
			analyticsServer = (AnalyticsServer) reg.lookup(analyticsBindingName);
		} catch (NumberFormatException e) {
			throw new LoadTestException("Bad configuration: Invalid registry port", e);
		} catch (NotBoundException e) {
			throw new LoadTestException("Server not bound to registry", e);
		}
	}
	
	@Override
	public void run() {
		eventSink.auto();
		try {
			subscriptionId = analyticsServer.subscribe(".*", eventSink);
		} catch (RemoteException e) {
			logger.severe("ERROR: Failed to subscribe to analytics server: " + e.getMessage());
		} catch (AnalyticsException e) {
			throw new RuntimeException("Universal filter pattern was rejected.", e);
		}
	}
	
	public void shutdown() {
		try {
			analyticsServer.unsubscribe(subscriptionId);
			UnicastRemoteObject.unexportObject(eventSink, true);
		} catch (RemoteException e) {
			logger.warning("ERROR: Failed to unsubscribe from analytics server: " + e.getMessage());
		} catch (AnalyticsException e) {
			logger.warning("ERROR: Failed to unsubscribe from analytics server: " + e.getMessage());
		}
	}
}

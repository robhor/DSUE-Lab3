package mgmtclient;

import java.rmi.AccessException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Properties;
import java.util.logging.Logger;

import util.PropertyReader;
import billing.BillingServer;
import analytics.AnalyticsServer;

public class ManagementClient {
	private static final Logger logger = Logger.getLogger("ManagementClient");

	private static EventSink eventSink;
	private static Subscriptions subscriptions;
	private static Prompt prompt;
	
	private static BillingServer billingServer;
	private static AnalyticsServer analyticsServer;
	
	/**
	 * Main method.
	 * @param args Program arguments: analytics binding name, billing binding name
	 * @throws ManagementException if an error occurs during a management client operation
	 * @throws RemoteException if a remoting error occurs
	 */
	public static void main(String[] args) throws ManagementException, RemoteException {
		// Parse Arguments 
		if (args.length != 2) {
			System.out.println("USAGE: java ManagementClient analyticsBindingName billingBindingName");
			return;
		}
		
		String analyticsBindingName = args[0];
		String billingBindingName = args[1];
		
		// Get external servers from registry
		try
		{
			connectExternalServers(billingBindingName, analyticsBindingName);
		} catch (ManagementException e) {
			logger.warning("ERROR: " + e.getMessage());
		}
		
		// Create managers
		eventSink = new EventSink();
		subscriptions = new Subscriptions(analyticsServer, eventSink);
		prompt = new Prompt(billingServer, eventSink, subscriptions);

		System.out.println("Management Client ready.");
		
		prompt.run();

		System.out.println("Shutting down...");
		
		shutdown();
	}

	private static void connectExternalServers(String billingBindingName, String analyticsBindingName) throws ManagementException {
		Registry reg;
		
		try {
			Properties registryProps = PropertyReader.readProperties("registry.properties");
			if (null == registryProps ) {
				throw new ManagementException("Could not read properties");
			}
			
			String host = registryProps.getProperty("registry.host");
			int port = Integer.parseInt(registryProps.getProperty("registry.port"));
			reg = LocateRegistry.getRegistry(host, port);
		} catch (NumberFormatException e) {
			throw new ManagementException("Bad configuration: Invalid registry port", e);
		} catch (RemoteException e) {
			throw new ManagementException("Registry could not be accessed", e);
		} 
		
		try {
			billingServer = (BillingServer) reg.lookup(billingBindingName);
		} catch (NotBoundException e) {
			throw new ManagementException("Billing server not bound to registry", e);
		} catch (AccessException e) {
			throw new ManagementException("Registry could not be accessed", e);
		} catch (RemoteException e) {
			throw new ManagementException("Registry could not be accessed", e);
		} finally {
			try {
				analyticsServer = (AnalyticsServer) reg.lookup(analyticsBindingName);
			} catch (NotBoundException e) {
				throw new ManagementException("Analytics server not bound to registry", e);
			} catch (AccessException e) {
				throw new ManagementException("Registry could not be accessed", e);
			} catch (RemoteException e) {
				throw new ManagementException("Registry could not be accessed", e);
			}
		}
	}
	
	private static void shutdown() throws ManagementException {
		try {
			subscriptions.clear();
		} catch (RemoteException e) {
			logger.warning(String.format("Failed to unsubscribe all: %s", e.getMessage()));
		} catch (ManagementException e) {
			logger.warning(String.format("Failed to unsubscribe all: %s", e.getMessage()));
		}
		
		try {
			UnicastRemoteObject.unexportObject(eventSink, true);
		} catch (NoSuchObjectException e) {
			throw new RuntimeException("Failed to unexport subscriber object.", e);
		}
	}
}

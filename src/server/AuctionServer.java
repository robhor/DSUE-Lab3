package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Properties;

import analytics.AnalyticsServer;
import billing.BillingServer;
import billing.BillingServerSecure;

import server.bean.User;
import server.service.AuctionManager;
import server.service.ClientManager;
import server.service.UserManager;
import server.service.impl.AnalyticsServerWrapper;
import server.service.impl.AuctionManagerImpl;
import server.service.impl.ClientManagerImpl;
import server.service.impl.UserManagerImpl;
import util.PropertyReader;


public class AuctionServer {
	private static final String registryProperties = "registry.properties";
	private static final String billingServerUser = "server";
	private static final String billingServerPass = "secure password";
	
	private static ServerSocket socket;
	private static AuctionManager auManager;
	private static UserManager usManager;
	private static ClientManager clManager;
	
	private static BillingServerSecure billingServer;
	private static AnalyticsServer analyticsServer;
	
	public static void main(String[] args) {
		int tcpPort = 0;
		
		// Parse Arguments 
		if (args.length < 3) {
			System.out.println("USAGE: java AuctionServer tcpPort analyticsBindingName billingBindingName");
			System.exit(1);
		}
		try {
			tcpPort = Integer.valueOf(args[0]);
		} catch (NumberFormatException e) {
			System.out.println("Invalid tcp port!");
			System.exit(1);
		}
		
		String analyticsBindingName = args[1];
		String billingBindingName = args[2];
		
		// Get external servers from registry
		connectExternalServers(billingBindingName, analyticsBindingName);
		AnalyticsServerWrapper wrappedAnalytics = new AnalyticsServerWrapper(analyticsServer);
		
		// Create Managers
		clManager = new ClientManagerImpl();
		usManager = new UserManagerImpl(clManager, wrappedAnalytics);
		auManager = new AuctionManagerImpl(usManager, billingServer, wrappedAnalytics);
		
		// Create socket
		try {
			socket = new ServerSocket(tcpPort);
		} catch (IOException e) {
			System.out.println("Could not connect to Socket");
			System.exit(1);
		}
		
		// Accept connections
		ConnectionDispatcher dispatcher = 
				new ConnectionDispatcher(socket, clManager, usManager, auManager);
		Thread serverThread = new Thread(dispatcher);
		serverThread.start();
		
		// close when pressing enter
		try { System.in.read();
		} catch (IOException e) {}
		
		shutdown();
	}
	
	private static void connectExternalServers(String billingBindingName, String analyticsBindingName) {
		Properties registryProps = PropertyReader.readProperties(registryProperties);
		if (registryProps == null) {
			System.err.println("Could not read properties");
			System.exit(1);
		}
		
		String host = registryProps.getProperty("registry.host");
		
		try {
			Integer port = Integer.valueOf(registryProps.getProperty("registry.port"));
			Registry reg = LocateRegistry.getRegistry(host, port);
			BillingServer bills = (BillingServer) reg.lookup(billingBindingName);
			billingServer = bills.login(billingServerUser, billingServerPass);
			analyticsServer = (AnalyticsServer) reg.lookup(analyticsBindingName);
		} catch (NumberFormatException e) {
			System.err.println("Bad configuration: Invalid registry port");
		} catch (RemoteException e) {
			System.err.println("Registry could not be accessed");
		} catch (NotBoundException e) {
			System.err.println("Billing Server not bound to registry");
		}
	}
	
	private static void shutdown() {
		try { socket.close();
		} catch (IOException e) {}
		
		// logout all users
		for(User u : usManager.getUsers()) {
			usManager.logout(u);
		}
		clManager.disconnectAll();
		auManager.shutdown();
	}
}

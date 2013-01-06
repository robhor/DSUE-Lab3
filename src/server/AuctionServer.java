package server;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.PrivateKey;
import java.util.Properties;
import java.util.logging.Logger;

import server.bean.User;
import server.service.AuctionManager;
import server.service.ClientManager;
import server.service.UserManager;
import server.service.impl.AnalyticsServerWrapper;
import server.service.impl.AuctionManagerImpl;
import server.service.impl.ClientManagerImpl;
import server.service.impl.UserManagerImpl;
import util.PropertyReader;
import util.SecurityUtils;
import analytics.AnalyticsServer;
import billing.BillingServer;
import billing.BillingServerSecure;


public class AuctionServer {
	private static final Logger logger = Logger.getLogger("AuctionServer");
	
	private static final String registryProperties = "registry.properties";
	private static final String billingServerUser = "server";
	private static final String billingServerPass = "secure password";
	
	private static ServerSocket socket;
	private static AuctionManager auManager;
	private static UserManager usManager;
	private static ClientManager clManager;
	
	private static BillingServerSecure billingServer;
	private static AnalyticsServer analyticsServer;
	
	private static PrivateKey privateKey;
	
	public static void main(String[] args) {
		int tcpPort = 0;
		
		// Parse Arguments 
		if (args.length < 5) {
			System.out.println("USAGE: java AuctionServer tcpPort analyticsBindingName billingBindingName serverKey clientKeyDir");
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
		
		String serverKeyPath = args[3];
		String clientKeyDir  = args[4];
		
		getPrivateKey(serverKeyPath);
		
		// Get external servers from registry
		try {
			connectExternalServers(billingBindingName, analyticsBindingName);
		} catch(AuctionException e) {
			logger.warning("ERROR: " + e.getMessage());
		}
		AnalyticsServerWrapper wrappedAnalytics = new AnalyticsServerWrapper(analyticsServer);
		
		// Create Managers
		clManager = new ClientManagerImpl();
		usManager = new UserManagerImpl(clManager, wrappedAnalytics);
		auManager = new AuctionManagerImpl(usManager, billingServer, wrappedAnalytics);
		
		// Create socket
		try {
			socket = new ServerSocket(tcpPort);
		} catch (IOException e) {
			System.out.println("Could not create server socket");
			System.exit(1);
		}
		
		// Accept connections
		ConnectionDispatcher dispatcher = 
				new ConnectionDispatcher(socket, clManager, usManager, auManager, privateKey, clientKeyDir);
		Thread serverThread = new Thread(dispatcher);
		serverThread.start();
		
		System.out.println("Server ready.");
		
		// close when pressing enter
		try { System.in.read();
		} catch (IOException e) {}

		System.out.println("Shutting down...");
		
		shutdown();
	}
	
	private static void connectExternalServers(String billingBindingName, String analyticsBindingName) throws AuctionException {
		Properties registryProps = PropertyReader.readProperties(registryProperties);
		if (registryProps == null) {
			System.err.println("Could not read properties");
			System.exit(1);
		}
		
		String host = registryProps.getProperty("registry.host");
		Registry reg;
		
		try {
			Integer port = Integer.valueOf(registryProps.getProperty("registry.port"));
			reg = LocateRegistry.getRegistry(host, port);
		} catch (NumberFormatException e) {
			throw new AuctionException("Bad configuration: Invalid registry port");
		} catch (RemoteException e) {
			throw new AuctionException("Registry could not be accessed");
		}

		try {
			BillingServer bills = (BillingServer) reg.lookup(billingBindingName);
			billingServer = bills.login(billingServerUser, billingServerPass);
		} catch (NotBoundException e) {
			throw new AuctionException("Billing server not bound to registry", e);
		} catch (AccessException e) {
			throw new AuctionException("Registry could not be accessed", e);
		} catch (RemoteException e) {
			throw new AuctionException("Registry could not be accessed", e);
		} finally {
			try {
				analyticsServer = (AnalyticsServer) reg.lookup(analyticsBindingName);
			} catch (NotBoundException e) {
				throw new AuctionException("Analytics server not bound to registry", e);
			} catch (AccessException e) {
				throw new AuctionException("Registry could not be accessed", e);
			} catch (RemoteException e) {
				throw new AuctionException("Registry could not be accessed", e);
			}
		}
	}
	
	private static void getPrivateKey(String path) {
		try {
			// TODO: remove debugging code
			if (true) {
				privateKey = SecurityUtils.getPrivateKey(path, "23456");
				return;
			}
			
			System.out.println("Enter pass phrase:");
			String pass = new BufferedReader(new InputStreamReader(System.in)).readLine();
			privateKey = SecurityUtils.getPrivateKey(path, pass);
		} catch (FileNotFoundException e) {
			System.err.println("Private Key file not found!");
			System.exit(1);
		} catch (IOException e) {
			System.err.println("Could not read private key. Check your password.");
			System.exit(1);
		}
	}
	
	private static void shutdown() {
		try { socket.close();
		} catch (IOException e) {}
		
		// logout all users
		for(User u : usManager.getUsers()) {
			if (u.getClient() != null) usManager.logout(u);
		}
		clManager.disconnectAll();
		auManager.shutdown();
	}
}

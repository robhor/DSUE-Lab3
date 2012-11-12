package server;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Properties;

import server.bean.User;
import server.service.AuctionManager;
import server.service.ClientManager;
import server.service.UserManager;
import server.service.impl.AuctionManagerImpl;
import server.service.impl.ClientManagerImpl;
import server.service.impl.UserManagerImpl;


public class AuctionServer {
	private static int registryPort;
	
	public static void main(String[] args) {
		// Parse Arguments 
		if (args.length < 3) {
			System.out.println("USAGE: java AuctionServer tcpPort analyticsBindingName billingBindingName");
			System.exit(1);
		}
		Integer tcpPort = Integer.valueOf(args[0]);
		
		// Create Managers
		ClientManager clManager = new ClientManagerImpl();
		UserManager usManager = new UserManagerImpl(clManager);
		AuctionManager auManager = new AuctionManagerImpl(usManager);
		
		// Create RMI Registry
		if (readRegistryProperties()) {
			try {
				LocateRegistry.createRegistry(registryPort);
			} catch (RemoteException e) {
				System.err.println("Could not create registry");
			}
		}
		
		
		// Create socket
		ServerSocket socket = null;
		try {
			socket = new ServerSocket(tcpPort);
		} catch (IOException e) {
			System.out.println("ERROR");
			e.printStackTrace();
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
		
		try { socket.close();
		} catch (IOException e) {}
		
		// logout all users
		for(User u : usManager.getUsers()) {
			usManager.logout(u);
		}
		clManager.disconnectAll();
		auManager.shutdown();
	}
	
	private static boolean readRegistryProperties() {
		InputStream is = ClassLoader.getSystemResourceAsStream("registry.properties");
		if (is == null) return false;
		
		Properties props = new Properties();
		try {
			props.load(is);
			registryPort = Integer.parseInt(props.getProperty("registry.port"));
		} catch (NumberFormatException e) {
			return false;
		} catch (IOException e) {
			return false;
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				return false;
			}
		}
		
		return true;
	}
}

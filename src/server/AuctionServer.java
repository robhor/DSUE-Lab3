package server;

import java.io.IOException;
import java.net.ServerSocket;

import server.bean.User;
import server.service.AuctionManager;
import server.service.ClientManager;
import server.service.UserManager;
import server.service.impl.AuctionManagerImpl;
import server.service.impl.ClientManagerImpl;
import server.service.impl.UserManagerImpl;


public class AuctionServer {
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
}

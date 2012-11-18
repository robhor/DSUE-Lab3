package server;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

import server.bean.Client;
import server.service.AuctionManager;
import server.service.ClientManager;
import server.service.UserManager;


/**
 * Accepts connections on a socket
 * and handles incoming connections using a Thread Pool
 */
public class ConnectionDispatcher implements Runnable {
	private ServerSocket socket;
	private ClientManager  clManager;
	private UserManager    usManager;
	private AuctionManager auManager;
	
	public ConnectionDispatcher(ServerSocket socket, ClientManager clManager, UserManager usManager, AuctionManager auManager) {
		this.socket = socket;
		this.clManager = clManager;
		this.usManager = usManager;
		this.auManager = auManager;
	}
	
	@Override
	public void run() {
		ExecutorService executor = java.util.concurrent.Executors.newCachedThreadPool();
				//java.util.concurrent.Executors.newFixedThreadPool(9); // this severly limits load test capabilities ^^
		
		// Listen to Connections
		while (true) {
			try {
				Socket clientSocket = socket.accept();
				Client client = clManager.newClient(clientSocket);
				
				// create a Runnable for handling this client
				ConnectionHandler handler = new ConnectionHandler(client, clManager, usManager, auManager);
				
				// To the Thread Pool with it!
				executor.execute(handler);
			} catch (IOException e) {
				// Socket closed, break out of run loop
				break;
			}
		}
		
		executor.shutdownNow();
	}
	

}

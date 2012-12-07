package server;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PrivateKey;
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
	private PrivateKey     privateKey;
	private String         clientKeyDir;
	
	public ConnectionDispatcher(ServerSocket socket, ClientManager clManager, UserManager usManager,
								AuctionManager auManager, PrivateKey privateKey, String clientKeyDir) {
		this.socket = socket;
		this.clManager = clManager;
		this.usManager = usManager;
		this.auManager = auManager;
		this.privateKey = privateKey;
		this.clientKeyDir = clientKeyDir;
	}
	
	@Override
	public void run() {
		ExecutorService executor = java.util.concurrent.Executors.newCachedThreadPool();
		
		// Listen to Connections
		while (true) {
			try {
				Socket clientSocket = socket.accept();
				Client client = clManager.newClient(clientSocket);
				
				// create a Runnable for handling this client
				ConnectionHandler handler = new ConnectionHandler(client, clManager, usManager,
																  auManager, privateKey, clientKeyDir);
				
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

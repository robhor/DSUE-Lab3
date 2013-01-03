package client.timestamp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

public class TimestampServer extends Thread {
	private ServerSocket socket;
	ArrayList<Socket> connections;
	private PrivateKey key;
	
	public TimestampServer(int port) {
		try {
			socket = new ServerSocket(port);
			connections = new ArrayList<Socket>();
		} catch (IOException e) {
			System.err.println("TimestampServer failed to start");
		}
	}
	
	@Override
	public void run() {
		if (socket == null) return;
		
		ExecutorService executor = java.util.concurrent.Executors.newCachedThreadPool();
		
		while (true) {
			try {
				Socket clientSocket = socket.accept();
				connections.add(clientSocket);
				executor.execute(new TimestampHandler(this, clientSocket));
			} catch (IOException e) {
				break; // socket closed
			}
		}
		
		executor.shutdownNow();
		close();
	}
	
	public void close() {
		try {
			socket.close();
		} catch (IOException e) {
			System.err.println("TimestampServer Socket could not be closed");
		}
		
		for (Socket s : connections) {
			try { s.close(); }
			catch (IOException e) {}
		}
	}

	public void setSigningKey(PrivateKey key) {
		this.key = key;
	}
	
	public PrivateKey getSigningKey() {
		return key;
	}
	
	public int getPort() {
		return socket.getLocalPort();
	}
}

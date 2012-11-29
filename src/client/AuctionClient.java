package client;


import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

import server.bean.Client;
import server.service.ClientManager;
import server.service.impl.ClientManagerImpl;

public class AuctionClient {
	private static final boolean UDP_ENABLED = false; // disabled in lab2
	
	private static final String USAGE = "USAGE: host tcpPort udpPort";
	
	private ClientManager clManager;
	private Client server;
	
	private Socket socket;
	private DatagramSocket udpSocket;
	private TCPProtocol tcpProtocol;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String host;
		Integer tcpPort, udpPort;
		
		if (args.length != 3) {
			System.out.println(USAGE);
			return;
		}
		
		host = args[0];
		try {
			tcpPort = Integer.valueOf(args[1]);
			udpPort = Integer.valueOf(args[2]);
		} catch (NumberFormatException e) {
			System.err.println(USAGE);
			return;
		}
		
		if (!UDP_ENABLED) udpPort = 0;
		
		AuctionClient client = new AuctionClient(host, tcpPort, udpPort);

		System.out.println("Client ready.");
		
		client.prompt();
		
		System.out.println("Shutting down...");

		client.shutdown();
	}
	
	public AuctionClient(String host, int tcpPort, int udpPort) {
		// establish connection
		socket = null;
		clManager = new ClientManagerImpl();
		
		try {
			socket = new Socket(host, tcpPort);
		} catch (UnknownHostException e) {
			System.err.println("Unknown host");
			return;
		} catch (IOException e) {
			System.err.println("Could not connect");
			return;
		}
		
		server = clManager.newClient(socket);
		
		tcpProtocol = new TCPProtocol(clManager);
		tcpProtocol.setServer(server);
		tcpProtocol.setUdpPort(udpPort);
		
		// open UDP socket and listen in a separate thread
		if (UDP_ENABLED) setupUDP(udpPort);
	}
	
	private void setupUDP(int udpPort) {
		udpSocket = null;
		try {
			udpSocket = new DatagramSocket(udpPort);
		} catch (SocketException e) {
			System.err.println("UDP Socket could not be created");
			return;
		}
		
		UDPPrinter udpPrinter = new UDPPrinter(udpSocket); 
		(new Thread(udpPrinter)).start();
		
		
		tcpProtocol.setUdpProtocol(udpPrinter.getProtocol());
	}

	private void prompt() {
		Scanner scanner = new Scanner(System.in);
		System.out.print(promptPrefix());
		System.out.flush();
		
		while ( scanner.hasNextLine() ) {
			String input = scanner.nextLine();
			if(!tcpProtocol.processInput(input)) break;
			
			System.out.print(promptPrefix());
			System.out.flush();
		}
		
		System.out.println("Shutting down..");
		scanner.close();
	}
	
	private String promptPrefix() {
		String username = tcpProtocol.getUsername();
		return ((username == null) ? "" : username) + "> ";
	}
	
	/**
	 * @return the name of the currently logged in User
	 */
	public String getUser() {
		return tcpProtocol.getUsername();
	}
	
	public void shutdown() {
		clManager.disconnect(server);
		if (udpSocket != null) udpSocket.close();
	}
}

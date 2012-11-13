package server.service.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import server.bean.Client;
import server.service.ClientManager;

public class ClientManagerImpl implements ClientManager {
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private ArrayList<Client> clients;
	
	public ClientManagerImpl() {
		clients = new ArrayList<Client>();
	}
	
	@Override
	public Client newClient(Socket clientSocket) {
		Client client = new Client();
		client.setTcpSocket(clientSocket);
		
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
			
			client.setReader(in);
			client.setWriter(out);
		} catch (IOException e) {
			logger.log(Level.WARNING, "In/Output-Stream-Reader/Writer could not be created\n" + e);
			return null;
		}
		
		synchronized (clients) {
			clients.add(client);
		}
		return client;
	}

	@Override
	public void disconnect(Client client) {
		synchronized (clients) {
			clients.remove(client);
		}
		
		try {
			client.getTcpSocket().close();
			client.getReader().close();
			client.getWriter().close();
		} catch (IOException e) {
			logger.log(Level.INFO, "Client socket could not be closed");
		}
	}

	@Override
	public void sendMessage(Client client, String message) {
		synchronized (client) {
			client.getWriter().println(message);
		}
	}

	@Override
	public void postMessage(Client client, String message) {
		if (client == null)  return;
		if (message == null) return;
		
		if (client.getUdpPort() == 0) return;
		
		byte[] buf = message.getBytes();
		DatagramPacket p = new DatagramPacket(buf, buf.length, client.getTcpSocket().getInetAddress(), client.getUdpPort());
		
		DatagramSocket sock;
		try {
			sock = new DatagramSocket();
			sock.send(p);
		} catch (SocketException e) {
			logger.log(Level.WARNING, "Error sending datagram\n" + e);
		} catch (IOException e) {
			logger.log(Level.WARNING, "Error sending datagram\n" + e);
		}
	}

	@Override
	public String receiveMessage(Client client) {
		try {
			String line = client.getReader().readLine();
			return line;
		} catch (IOException e) {
			logger.log(Level.FINE, "Socket closed\n" + e);
			return null;
		}
	}

	@Override
	public void disconnectAll() {
		synchronized (clients) {
			while(clients.size() > 0) {
				disconnect(clients.get(0));
			}
		}
	}

}

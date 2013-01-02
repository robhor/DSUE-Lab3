package server.service;

import java.io.IOException;
import java.net.Socket;

import server.bean.Client;

public interface ClientManager {
	/**
	 * 
	 * @param clientSocket the socket, at which the client connected
	 * @return
	 */
	Client newClient(Socket clientSocket);
	
	/**
	 * Disconnects a client
	 * @param client
	 */
	void disconnect(Client client);
	
	/**
	 * Sends the client a message over TCP
	 * @param client
	 * @param message
	 */
	void sendMessage(Client client, String message);
	
	/**
	 * Sends the client a message over UDP
	 * @param client
	 * @param message
	 */
	void postMessage(Client client, String message);
	
	/**
	 * Listens for incoming messages
	 * @param client
	 * @return null if socket was closed while listening or received EOF
	 * @throws IOException 
	 */
	String receiveMessage(Client client) throws IOException;
	
	/**
	 * Upgrades this clients channel to a secure channel
	 * @param client
	 * @param secret
	 * @param iv
	 */
	void secureConnection(Client client, byte[] secret64, byte[] iv64);
	
	/**
	 * Downgrades this clients channel to an insecure channel
	 * @param client
	 */
	void unsecureConnection(Client client);
	
	/**
	 * Disconnects every client
	 */
	void disconnectAll();
}

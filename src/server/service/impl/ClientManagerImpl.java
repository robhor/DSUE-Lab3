package server.service.impl;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.util.encoders.Base64;

import server.bean.Client;
import server.service.ClientManager;
import channels.Base64Channel;
import channels.Channel;
import channels.CipherChannel;
import channels.TCPChannel;

public class ClientManagerImpl implements ClientManager {
	private static boolean DISABLE_UDP = true;
	
	private static final String CIPHER = "AES/CTR/NoPadding";
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private ArrayList<Client> clients;
	
	public ClientManagerImpl() {
		clients = new ArrayList<Client>();
	}
	
	@Override
	public Client newClient(Socket clientSocket) {
		Client client = new Client();
		client.setInetAddress(clientSocket.getInetAddress());
		
		try {
			Channel channel = new TCPChannel(clientSocket);
			channel = new Base64Channel(channel);
			CipherChannel cipherChannel = new CipherChannel(channel);
			
			client.setChannel(cipherChannel);
			client.setCipherChannel(cipherChannel);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Channel could not be created! " + e.getMessage());
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
			client.getChannel().close();
		} catch (IOException e) {
			logger.log(Level.INFO, "Client socket could not be closed");
		}
	}

	@Override
	public void sendMessage(Client client, String message) {
		if (client == null) throw new IllegalArgumentException("Cannot send a message to null!");
		
		synchronized (client) {
			client.getChannel().send(message.getBytes());
		}
	}

	@Override
	public void postMessage(Client client, String message) {
		if (DISABLE_UDP) return;
		if (client == null)  return;
		if (message == null) return;
		
		if (client.getUdpPort() == 0) return;
		
		byte[] buf = message.getBytes();
		DatagramPacket p = new DatagramPacket(buf, buf.length, client.getInetAddress(), client.getUdpPort());
		
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
	public String receiveMessage(Client client) throws IOException {
		byte[] line = client.getChannel().read();
		if (line == null) throw new SocketException("Socket closed");
		return new String(line);
	}

	@Override
	public void secureConnection(Client client, byte[] secret64, byte[] iv64) {
		CipherChannel channel = client.getCipherChannel();
		
		SecretKey secretKey = new SecretKeySpec(Base64.decode(iv64), CIPHER);
		byte[] iv = Base64.decode(iv64);
		
		try {
			Cipher encryptCipher = Cipher.getInstance(CIPHER);
			Cipher decryptCipher = Cipher.getInstance(CIPHER);
			
			IvParameterSpec ivp = new IvParameterSpec(iv);
			
			encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey, ivp);
			decryptCipher.init(Cipher.DECRYPT_MODE, secretKey, ivp);
			
			channel.setCipher(encryptCipher, decryptCipher);
		} catch (NoSuchAlgorithmException e) {
			logger.log(Level.SEVERE, "No such algorithm: " + e.getMessage());
		} catch (NoSuchPaddingException e) {
			logger.log(Level.SEVERE, "No such padding: " + e.getMessage());
		} catch (InvalidKeyException e) {
			logger.log(Level.SEVERE, "Invalid key: " + e.getMessage());
		} catch (InvalidAlgorithmParameterException e) {
			logger.log(Level.SEVERE, "Invalid algorithm parameter: " + e.getMessage());
		}
	}
	
	@Override
	public void unsecureConnection(Client client) {
		CipherChannel channel = client.getCipherChannel();
		channel.setCipher(null, null);
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

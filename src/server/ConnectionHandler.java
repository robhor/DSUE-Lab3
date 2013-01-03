package server;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.SecretKey;

import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Base64;

import server.bean.Auction;
import server.bean.Client;
import server.bean.User;
import server.service.AuctionManager;
import server.service.ClientManager;
import server.service.UserManager;
import util.SecurityUtils;
import client.TCPProtocol;


public class ConnectionHandler implements Runnable {
	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	private Client client;
	private User user;
	private ClientManager clManager;
	private UserManager usManager;
	private AuctionManager auManager;
	private PrivateKey privateKey;
	private String clientKeyDir;
	
	public ConnectionHandler(Client client, ClientManager clManager, UserManager usManager,
							 AuctionManager auManager, PrivateKey privateKey, String clientKeyDir) {
		this.client = client;
		this.clManager = clManager;
		this.usManager = usManager;
		this.auManager = auManager;
		this.privateKey = privateKey;
		this.clientKeyDir = clientKeyDir;
	}

	@Override
	public void run() {
		// listen for messages
		byte[] input;
		try {
			while ((input = client.getChannel().read()) != null) {
				processMessage(input);
			}
		} catch (IOException e) {
			logger.log(Level.FINE, "Client disconnected");
		} finally {	
			// close resources
			if (user != null) usManager.disconnect(user);
			clManager.disconnect(client);
		}
	}
	
	private void processMessage(byte[] message) {
		String msg = new String(message);
		String[] tokens = msg.split(" ");
		String cmd = tokens[0];
		
		if (cmd.equals(TCPProtocol.CMD_LOGOUT)) {
			logout(tokens);
		} else if (cmd.equals(TCPProtocol.CMD_CREATE)) {
			createAuction(tokens);
		} else if (cmd.equals(TCPProtocol.CMD_LIST)) {
			listAuctions(tokens);
		} else if (cmd.equals(TCPProtocol.CMD_BID)) {
			bid(tokens);
		} else if (cmd.equals(TCPProtocol.CMD_SIGNED_BID)) {
			signedBid(tokens);
		} else if (cmd.equals(TCPProtocol.CMD_UDP)) {
			setUdp(tokens);
		} else if (cmd.equals(TCPProtocol.CMD_ACTIVE_USERS)) {
			listActiveUsers();
		} else {
			// could be encrypted !login message
			msg = new String(SecurityUtils.decryptRSA(message, privateKey));
			
			tokens = msg.split(" ");
			cmd = tokens[0];
			
			if (cmd.equals(TCPProtocol.CMD_LOGIN)) {
				login(tokens);
			}
		}
	}
	
	private void login(String[] tokens) {
		// begin handshake
		/* **************************************************************************************
		 *              Step 1: receive !login <username> <tcpPort> <client-challenge>
		 * **************************************************************************************/
		if (tokens.length < 4) {
			clManager.sendMessage(client, TCPProtocol.RESPONSE_FAIL);
			return;
		}
		
		String name = tokens[1];
		Integer tcpPort = Integer.parseInt(tokens[2]); 
		String clientChallenge = tokens[3];
		
		// check if logged in already beforehand
		if (usManager.isLoggedIn(name)) {
			clManager.sendMessage(client, TCPProtocol.RESPONSE_FAIL);
			return;
		}
		
		
		/* **************************************************************************************
		 *   Step 2: send !ok <client-challenge> <server-challenge> <secret-key> <iv-parameter>
		 * **************************************************************************************/
		
		// generate server challenge
		SecureRandom secureRandom = new SecureRandom(); 
		final byte[] serverChallenge = new byte[32];
		secureRandom.nextBytes(serverChallenge);
		String serverChallenge64 = new String(Base64.encode(serverChallenge));
		
		
		// generate secret key
		SecretKey key = SecurityUtils.getSecretKey(TCPProtocol.KEYSIZE);
		String secretKey = new String(Base64.encode(key.getEncoded()));
		
		// generate iv
		final byte[] iv = new byte[16]; 
		secureRandom.nextBytes(iv);
		String iv64 = new String(Base64.encode(iv));
		
		
		String msg = String.format("%s %s %s %s %s", TCPProtocol.RESPONSE_SUCCESS,
								   clientChallenge, serverChallenge64, secretKey, iv64);
		
		// encrypt using client's public key
		PublicKey clientKey = null;
		try {
			clientKey = getPublicKey(name);
		} catch (IOException e) {
			logger.log(Level.INFO, "Public key of user " + name + " could not be read.");
		}
		
		client.getChannel().send(SecurityUtils.encryptRSA(msg.getBytes(), clientKey));
		
		/* **************************************************************************************
		 *                         Step 3: receive <server-challenge>
		 * **************************************************************************************/
		// secure channel should be created now!
		clManager.secureConnection(client, serverChallenge64.getBytes(), iv64.getBytes());
		
		try {
			byte[] response = client.getChannel().read();
			if (!Arrays.areEqual(Base64.decode(response), serverChallenge)) return;
		} catch (IOException e) {
			return;
		}
		
		// handshake successful
		if (user != null) usManager.logout(user);
		user = usManager.login(name, client);
		user.setTcpPort(tcpPort);
	}
	
	private void logout(String[] tokens) {
		usManager.logout(user);
		clManager.unsecureConnection(client);
		clManager.sendMessage(client, TCPProtocol.RESPONSE_SUCCESS);
		
		user = null;
	}
	
	private void createAuction(String[] tokens) {
		// !create <duration> <description>
		
		// must be logged in
		if (user == null) {
			clManager.sendMessage(client, TCPProtocol.RESPONSE_FAIL);
			return;
		}
		
		if (tokens.length < 3) {
			usManager.sendMessage(user, TCPProtocol.RESPONSE_FAIL);
			return;
		}
		
		int duration;
		String description;
		
		try {
			duration = Integer.valueOf(tokens[1]);
		} catch (NumberFormatException e) {
			logger.log(Level.INFO, "Invalid duration for new auction");
			usManager.sendMessage(user, TCPProtocol.RESPONSE_FAIL);
			return;
		}
		
		if (duration < 1) {
			usManager.sendMessage(user, TCPProtocol.RESPONSE_FAIL);
			return;
		}
		
		description = tokens[2];
		for (int i = 3; i < tokens.length; i++) {
			description += " " + tokens[i];
		}
		
		Auction au = auManager.createAuction(user, description, duration);
		
		String endDate = new SimpleDateFormat().format(au.getEndTime().getTime());
		String msg = String.format("%s %d %s", TCPProtocol.RESPONSE_SUCCESS, au.getId(), endDate);
		usManager.sendMessage(user, msg);
	}
	
	private void listAuctions(String[] tokens) {
		Collection<Auction> list = auManager.getAuctions();
		SimpleDateFormat sdf = new SimpleDateFormat();
		
	
		clManager.sendMessage(client, String.valueOf(list.size()));
		
		for (Auction a : list) {
			User bidder = a.getHighestBidder();
			String bidderName = (bidder == null) ? "none" : bidder.getName();
			String line = String.format("%d. '%s' by %s %s %.2f %s",
					a.getId(),
					a.getName(),
					a.getOwner().getName(),
					sdf.format(a.getEndTime().getTime()),
					a.getHighestBid(),
					bidderName);
			
			clManager.sendMessage(client, line);
		}
	}
	
	private void bid(String[] tokens) {
		// !bid #id #amount
		
		// need to be logged in
		if (user == null) {
			clManager.sendMessage(client, TCPProtocol.RESPONSE_FAIL);
			return;
		}
		
		int id;
		double amount;
		try {
			id = Integer.valueOf(tokens[1]);
			amount = Double.valueOf(tokens[2]);
		} catch(NumberFormatException e) {
			usManager.sendMessage(user, TCPProtocol.RESPONSE_FAIL);
			return;
		}
		
		if (amount < 0.01) {
			usManager.sendMessage(user, TCPProtocol.RESPONSE_FAIL);
			return;
		}
		
		Auction auction = auManager.getAuctionById(id);
		if (auction == null || auction.hasEnded()) {
			usManager.sendMessage(user, TCPProtocol.RESPONSE_NO_AUCTION);
			return;
		}
		
		boolean success = auManager.bid(user, auction, amount);
		String msg = (success) ? TCPProtocol.RESPONSE_SUCCESS : TCPProtocol.RESPONSE_FAIL;
		msg += String.format(" %.2f %s", auction.getHighestBid(), auction.getName());
		usManager.sendMessage(user, msg);
	}

	private void signedBid(String[] tokens) {
		int id;
		double bid;
		String sign1, sign2;
		try {
			id = Integer.valueOf(tokens[1]);
			bid = Double.valueOf(tokens[2]);
			sign1 = tokens[3];
			sign2 = tokens[4];
		} catch (NumberFormatException e) {
			return;
		}
		
		long time = validateSignatures(id, bid, sign1, sign2);
		
		boolean bidSuccessful = true;
		
		// validation unsuccessful
		if (time < 0) bidSuccessful = false;
		
		
		Auction auction = auManager.getAuctionById(id);
		if (bidSuccessful && auction != null && time < auction.getEndTime().getTimeInMillis())
			bidSuccessful = auManager.bid(user, auction, bid);
		else
			bidSuccessful = false;
		
		if (bidSuccessful) {
			String s = String.format("Signed bid: Auction \"%s\" now at %.2f (%s)",
									auction.getName(),
									auction.getHighestBid(),
									auction.getHighestBidder().getName());
			logger.log(Level.INFO, s);
		} else {
			String auctionName = (auction == null) ? "(Not existing)" : auction.getName();
			String s = String.format("Rejected signed bid: For auction \"%s\", %.2f (%s)",
					auctionName, bid, user.getName());
			logger.log(Level.INFO, s);
		}
	}
	
	private long validateSignatures(int id, double bid, String signature1, String signature2) {
		long time1 = validateSignature(id, bid, signature1);
		if (time1 < 0) return -1;
		
		long time2 = validateSignature(id, bid, signature2);
		if (time2 < 0) return -2;
		
		return (time1 + time2) / 2;
	}
	
	private long validateSignature(int id, double bid, String signature) {
		String[] tokens = signature.split(":");
		if (tokens.length != 3) return -1;
		
		long time;
		String user;
		byte[] sign;
		
		try {
			user = tokens[0];
			time = Long.parseLong(tokens[1]);
			sign = Base64.decode(tokens[2]);
		} catch (NumberFormatException e) {
			return -1;
		}
		
		// reconstruct original !timestamp command
		String timestamp = String.format("%s %d %f %d", TCPProtocol.RESPONSE_TIMESTAMP, id, bid, time);
		
		PublicKey key;
		try {
			key = getPublicKey(user);
		} catch (IOException e) {
			return -1;
		}
		
		if (!SecurityUtils.verify(timestamp.getBytes(), sign, key))
			return -1;
		
		return time;
	}

	private void setUdp(String[] tokens) {
		if (user == null) return;
		if (tokens.length < 2) return;
		
		Integer udpPort;
		try {
			udpPort = Integer.parseInt(tokens[1]);
		} catch (NumberFormatException e) {
			logger.log(Level.FINE, "Invalid udp-port received (" + tokens[1] + ")");
			return;
		}
		
		client.setUdpPort(udpPort);
	}

	private void listActiveUsers() {
		String users = "";
		
		for (User u : usManager.getUsers()) {
			if (u.isLoggedIn()) {
				String ip = u.getClient().getInetAddress().getHostAddress();
				int port = u.getTcpPort();
				
				String addr = String.format("%s:%d - %s", ip, port, u.getName());
				users += addr + "\n";
			}
		}
		
		if (users.equals("")) users = TCPProtocol.RESPONSE_FAIL;
		clManager.sendMessage(client, users);
	}
	
	private PublicKey getPublicKey(String username) throws IOException {
		return SecurityUtils.getPublicKey(clientKeyDir + username + ".pub.pem");	
	}
}

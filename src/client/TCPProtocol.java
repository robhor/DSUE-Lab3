package client;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Base64;

import server.bean.Client;
import server.service.ClientManager;
import util.SecurityUtils;
import client.timestamp.TimestampServer;
import client.timestamp.TimestampServerRecord;

/**
 * Protocol the client uses to communicate with the auction server
 */
public class TCPProtocol {
	public static final int    KEYSIZE             = 256; /** Keysize for AES-Cipher */
	public static final int    RECONNECT_INTERVAL  = 10; /** seconds */
	
    public static final String CMD_LOGIN        = "!login";
	public static final String CMD_LOGOUT       = "!logout";
	public static final String CMD_LIST         = "!list";
	public static final String CMD_CREATE       = "!create";
	public static final String CMD_BID          = "!bid";
	public static final String CMD_SIGNED_BID   = "!signedBid";
	public static final String CMD_GROUP_BID    = "!groupBid";
	public static final String CMD_CONFIRM      = "!confirm";
	public static final String CMD_EXIT         = "!end";
	public static final String CMD_UDP          = "!udp";
	public static final String CMD_ACTIVE_USERS = "!getClientList";
	
	public static final String CMD_GET_TIMESTAMP  = "!getTimestamp";
	public static final String RESPONSE_TIMESTAMP = "!timestamp";
	
	public static final String RESPONSE_FAIL       = "!fail";
	public static final String RESPONSE_SUCCESS    = "!ok";
	public static final String RESPONSE_NO_AUCTION = "!no-auction";
	public static final String RESPONSE_BLOCK      = "!block";
	public static final String RESPONSE_CONFIRMED  = "!confirmed";
	public static final String RESPONSE_REJECTED   = "!rejected";
	
	private Logger logger = Logger.getLogger(TCPProtocol.class.getSimpleName());
	
	private ClientManager clManager;
	private String user;
	private PrivateKey userKey;
	private int udpPort;
	
	private String serverHost;
	private int serverPort;
	private Client server;
	private Timer reconnectTimer;
	private TimerTask reconnectTask;
	private HashMap<String, String> signedBids; /** Key: Username, Value: 1 signedBid command per line */
	
	private UDPProtocol udpProtocol;
	private TimestampServer timestampServer;
	private ArrayList<TimestampServerRecord> activeUsers;
	private PublicKey serverKey;
	private String clientKeyDir;

	
	public TCPProtocol(ClientManager clManager, PublicKey serverKey, String clientKeyDir) {
		this.clManager = clManager;
		this.serverKey = serverKey;
		this.clientKeyDir = clientKeyDir;
		this.user = null;
		
		activeUsers = new ArrayList<TimestampServerRecord>();
		signedBids = new HashMap<String, String>();
		
		reconnectTimer = new Timer();
	}
	
	public void setUdpPort(Integer udpPort) {
		this.udpPort = udpPort;
	}
	
	public void setServer(String host, int port) throws UnknownHostException, IOException {
		this.serverHost = host;
		this.serverPort = port;
		
		Socket socket;
		socket = new Socket(host, port);
		
		server = clManager.newClient(socket);
	}

	/**
	 * Tries to reconnect to a previously connected server
	 * @return true, if successfully reconnected
	 */
	private boolean serverReconnect() {
		if (server != null) return true;
		try {
			setServer(serverHost, serverPort);
		} catch (UnknownHostException e) {
			logger.log(Level.WARNING, "Unknown Host");
			return false;
		} catch (IOException e) {
			logger.log(Level.INFO, "Could not connect to server");
			return false;
		}
		
		if (reconnectTask != null) {
			reconnectTask.cancel();
			reconnectTask = null;
		}
		
		try {
			if (user != null && userKey != null) {
				if (!handshake(user, userKey))
					logout();
				else
					sendSignedBids();
			}
		} catch (IOException e) {
			serverDisconnect();
			return false;
		}
		
		return true;
	}
	
	private void serverScheduleReconnect() {
		final String retryMessage = "Server unreachable. Will try to reconnect in " + RECONNECT_INTERVAL + " seconds";
		System.out.println(retryMessage);
		
		int period = RECONNECT_INTERVAL * 1000;
		if (reconnectTask == null) {
			reconnectTask = new TimerTask() {
				public void run() {
					if (serverReconnect()) {
						System.out.println("Successfully reconnected!");
						reconnectTask = null;
						cancel();
					} else {
						System.out.println(retryMessage);
					}
				}
			};
			
			
			reconnectTimer.scheduleAtFixedRate(reconnectTask, period, period);
		}
	}

	private void serverDisconnect() {
		server = null;
		serverScheduleReconnect();
	}

	public void setUdpProtocol(UDPProtocol udpProtocol) {
		this.udpProtocol = udpProtocol;
	}
	

	public void setTimestampServer(TimestampServer timestampServer) {
		this.timestampServer = timestampServer;
	}

	public boolean processInput(String input) {
		Scanner scanner = new Scanner(input);
		if (!scanner.hasNext()) return true;
		
		String token = scanner.next();
		
		if (token.equals(CMD_EXIT)) {
			shutdown();
			return false;
		}
		
		if (server == null && !serverReconnect()) {
			serverDisconnect();
			
			// Allow logging out while server unreachable
			// For safety.
			// And bidding by means of signedBids
			if (!token.equals(CMD_LOGOUT) && !token.equals(CMD_BID))
				return true;
		}
		
		// Commands for connected server
		try {
			if (token.equals(CMD_LOGIN)) {
				login(input);
			} else if (token.equals(CMD_LOGOUT)) {
				logout();
			} else if (token.equals(CMD_LIST)) {
				listAuctions();
			} else if (token.equals(CMD_CREATE)) {
				createAuction(input);
			} else if (token.equals(CMD_BID)) {
				bid(input);
			} else if (token.equals(CMD_GROUP_BID)) {
				groupBid(input);
			} else if (token.equals(CMD_CONFIRM)) {
				confirm(input);
			} else if (token.equals(CMD_ACTIVE_USERS)) {
				listActiveUsers();
			} else {
				System.out.println("unknown command");
			}
		} catch (IOException e) {
			serverDisconnect();
		}
		
		return true;
	}
	
	private void logout() throws IOException {
		if (isLoggedIn()) {
			System.out.println("Successfully logged out");
		} else {
			System.out.println("You have to log in first!");
			return;
		}
		
		try {
			if (server != null) {
				clManager.sendMessage(server, CMD_LOGOUT);
				clManager.receiveMessage(server);
			}
		} finally {
			user = null;
			userKey = null;
			if (server != null) clManager.unsecureConnection(server);
			if (udpProtocol != null) udpProtocol.setUser(null);
			if (timestampServer != null) timestampServer.setSigningKey(null);
		}
	}
	
	private void login(String input) throws IOException {
		// parse input
		String[] tokens = input.split(" ");
		if (tokens.length < 2) return;
		
		String username = tokens[1];
		if (udpProtocol != null) udpProtocol.setUser(username);
		
		if (loginUser(username)) {
			System.out.println("Successfully logged in as " + user + "!");
		} else {
			if (udpProtocol != null) udpProtocol.setUser(user);
			System.out.println("Login unsuccessful");
		}
	}
	
	/**
	 * Logs in the user with the given username
	 * @param username
	 * @return true if login was successful
	 */
	private boolean loginUser(String username) throws IOException {
		// check if user exists by checking if .pem exists
		String clientKeyPath = clientKeyDir + username + ".pem";
		File f = new File(clientKeyPath);
		if (!f.exists()) return false;
		
		String pass;
		try {
			System.out.println("Enter pass phrase:");
			pass = new BufferedReader(new InputStreamReader(System.in)).readLine();
			userKey = SecurityUtils.getPrivateKey(clientKeyPath, pass);
		} catch (IOException e) {
			logger.log(Level.INFO, "Private key could not be read");
			return false;
		}

		
		boolean handshakeSuccessful = false;
		try {
			handshakeSuccessful = handshake(username, userKey);
		} finally {
			if (handshakeSuccessful) {
				if (timestampServer != null) timestampServer.setSigningKey(userKey);
				user = username;
				sendSignedBids();
			}
		}
		
		updateActiveUsers();
		
		return handshakeSuccessful;
	}
	
	private boolean handshake(String username, PrivateKey privateKey) throws IOException {
		String message;
		String[] tokens;
		
		byte[] msg;
		byte[] clientChallenge;
		
		
		/* **************************************************************************************
		 *              Step 1: send !login <username> <tcpPort> <clientChallenge>
		 * **************************************************************************************/
		int tcpPort = timestampServer.getPort();
		
		clientChallenge = SecurityUtils.generateNumber(32);
		String clientChallenge64 = new String(Base64.encode(clientChallenge));
		
		message = String.format("%s %s %d %s", CMD_LOGIN, username, tcpPort, clientChallenge64);
		msg = SecurityUtils.encryptRSA(message.getBytes(), serverKey);
		
		server.getChannel().send(msg);
		
		/* **************************************************************************************
		 * Step 2: receive !ok <client-challenge> <server-challenge> <secret-key> <iv-parameter>
		 * **************************************************************************************/
		msg = server.getChannel().read();
		if (new String(msg).equals(RESPONSE_FAIL)) return false;
		msg = SecurityUtils.decryptRSA(msg, privateKey);
		
		message = new String(msg);
		tokens = message.split(" ");
		
		if (!tokens[0].equals(RESPONSE_SUCCESS)) return false;
		
		// check clientChallenge
		if (!Arrays.areEqual(clientChallenge, Base64.decode(tokens[1].getBytes()))) {
			logger.log(Level.INFO, "Server got the client challenge wrong");
			return false;
		}
		
		String serverChallenge = tokens[2];
		String secretKey64 = tokens[3];
		String iv64 = tokens[4];
		
		// establish encrypted channel
		clManager.secureConnection(server, secretKey64.getBytes(), iv64.getBytes());
		
		/* **************************************************************************************
		 *                              Step 3: send <server-challenge>
		 * **************************************************************************************/
		server.getChannel().send(serverChallenge.getBytes());
		
		return true;
	}

	private boolean listAuctions() throws IOException {
		return listAuctions(true);
	}
	
	private boolean listAuctions(boolean retry) throws IOException {
		clManager.sendMessage(server, CMD_LIST);
		
		String status = clManager.receiveMessage(server);
		if (status == null) return false;
		
		if (!status.startsWith(RESPONSE_SUCCESS)) {
			System.out.println("Listing failed. Check your shared key!");
			return true;
		}

		String header = clManager.receiveMessage(server);
		int auctions = Integer.valueOf(header);
		StringBuilder listBuilder = new StringBuilder(); // whole list output without either count or HMAC
		
		for (int i = 0; i < auctions; i++) {
			String line = clManager.receiveMessage(server);
			listBuilder.append(String.format("%s%n", line));
		}
		
		String wholeList = listBuilder.toString();
		String wholeMessage = String.format("%s%n%s%n%s", status, header, wholeList);
		
		if (isLoggedIn() && !verifyHmac(wholeMessage)) {
			if (retry) {
				System.out.println("Failed to verify the response from the server. Retry...");
				return listAuctions(false);
			} else {
				System.out.println("Verification failed again. Abort.");
				return true;
			}
		}
		
		System.out.print(wholeList);
		return true;
	}
	
	private boolean verifyHmac(String message) {
		try {
			String hmacKeyPath = clientKeyDir + user + ".key";
			Key hmacKey = SecurityUtils.getClientKey(hmacKeyPath);
			byte[] hmac = SecurityUtils.hmacSHA256(message.getBytes(), hmacKey);
			
			String hmac64 = clManager.receiveMessage(server);
			byte[] serverHmac = Base64.decode(hmac64);

			return Arrays.areEqual(hmac, serverHmac);
		} catch (IOException e) {
			logger.log(Level.INFO, "Shared HMAC key could not be read");
			return false;
		}
	}
	
	private void updateActiveUsers() throws IOException {
		clManager.sendMessage(server, CMD_ACTIVE_USERS);
		String msg = clManager.receiveMessage(server);
		
		activeUsers.clear();
		if (msg.equals(RESPONSE_FAIL)) return;
		
		String[] lines = msg.split("\n");
		for (String record : lines) {
			TimestampServerRecord rec = TimestampServerRecord.parse(record); 
			if (rec == null) continue;
			activeUsers.add(rec);
		}
	}
	
	private void listActiveUsers() throws IOException {
		updateActiveUsers();
		
		for (TimestampServerRecord rec : activeUsers) {
			System.out.println(rec);
		}
	}
	
	private boolean createAuction(String input) throws IOException {
		// !create <duration> <description>
		if (!isLoggedIn()) {
			System.err.println("You need to be logged in to do that");
			return true;
		}
		
		String[] tokens = input.split(" ");
		
		if (tokens.length < 3) {
			System.out.println("Syntax: !create <duration [seconds]> <description>");
			return true;
		}
		
		int duration;
		String name;
		
		try {
			duration = Integer.valueOf(tokens[1]);
		} catch (NumberFormatException e) {
			System.err.println("Duration not valid!");
			return true;
		}
		
		if (duration <= 0) {
			System.err.println("Duration must be positive!");
			return true;
		}
		
		name = tokens[2];
		for (int i = 3; i < tokens.length; i++)
			name += " " + tokens[i];
		
		clManager.sendMessage(server, input);
		
		String response = clManager.receiveMessage(server);
		if (response == null) return false;
		
		if (response.startsWith(RESPONSE_FAIL)) {
			System.out.println("Auction could not be created!");
		} else {
			// !ok <id> <end>
			tokens = response.split(" ");
			String id = tokens[1];
			String endDate = tokens[2];
			for (int i = 3; i < tokens.length; i++)
				endDate += " " + tokens[i];
			String msg = String.format("An auction '%s' with id %s has been created and will end on %s.",
					name, id, endDate);
			System.out.println(msg);
		}
		
		return true;
	}
	
	private void bid(String input) throws IOException {
		// parse
		String[] tokens = input.split(" ");
		if (tokens.length != 3) {
			System.err.println("Wrong number of parameters!");
			return;
		}
		
		int id;
		double bid;
		try {
			id =  Integer.valueOf(tokens[1]);
			bid = Double.valueOf(tokens[2]);
		} catch (NumberFormatException e) {
			System.err.println("Not a valid value!");
			return;
		}
		
		try {
			if (server != null) {
				onlineBid(id, bid);
				return;
			}
		} catch (IOException e) {
			// server offline, use signedBid
			serverDisconnect();
		}
		
		signedBid(id, bid);
	}
	
	private void onlineBid(int id, double bid) throws IOException {
		// send bid to server
		String message = String.format("%s %d %f", CMD_BID, id, bid);
		clManager.sendMessage(server, message);
		
		// receive response
		String response = clManager.receiveMessage(server);
		if (response == null) return;
		if (response.equals(RESPONSE_NO_AUCTION)) {
			System.out.println("No auction with that id exists");
			return;
		}
		
		String[] tokens = response.split(" ");
		if (response.startsWith(RESPONSE_FAIL) && tokens.length == 1) {
			System.out.println("Bidding on auction failed");
			return;
		}
		String amount = tokens[1];
		String auction = tokens[2];
		for (int i = 3; i < tokens.length; i++)
			auction += tokens[i];
		
		String msg = null;
		if (response.startsWith(RESPONSE_SUCCESS)) {
			msg = String.format("You successfully bid with %s on '%s'.", amount, auction);
		} else {
			msg = String.format("You unsuccessfully bid with %.2f on '%s'. Current highest bid is %s.", bid, auction, amount);
		}
		System.out.println(msg);
	}
	
	private void signedBid(int id, double bid) {
		Collections.shuffle(activeUsers);
		
		String sign1 = null;
		String sign2 = null;
		
		Iterator<TimestampServerRecord> it = activeUsers.iterator();
		while (it.hasNext()) {
			TimestampServerRecord rec = it.next();
			
			if (rec.getUser().equals(user)) {
				it.remove();
				continue;
			}
			
			String signature = null;
			try {
				signature = rec.stamp(id, bid);
			} catch (IOException e) {}
			
			if (signature == null) {
				it.remove();
				continue;
			}
			
			if (sign1 == null) {
				sign1 = signature;
				continue;
			} else {
				sign2 = signature;
				break;
			}
		}
		
		if (sign2 == null) {
			System.out.println("Bid failed: Not enough users available to sign");
		} else {
			String signedBid = CMD_SIGNED_BID + " " + id + " " + bid + " " + sign1 + " " + sign2;
			synchronized (signedBids) {
				String bids = signedBids.get(user);
				if (bids == null) bids = "";
				
				bids += signedBid + "\n";
				signedBids.put(user, bids);
			}
			System.out.println("Bid signed");	
		}
	}

	private void groupBid(String input) throws IOException {
		// parse
		String[] tokens = input.split(" ");
		if (tokens.length != 3) {
			System.err.println("Wrong number of parameters!");
			return;
		}
		
		int auctionId;
		double amount;
		try {
			auctionId =  Integer.valueOf(tokens[1]);
			amount = Double.valueOf(tokens[2]);
		} catch (NumberFormatException e) {
			System.err.println("Not a valid value!");
			return;
		}

		// send bid to server
		String message = String.format("%s %d %f", CMD_GROUP_BID, auctionId, amount);
		clManager.sendMessage(server, message);
		
		// receive response
		String response = clManager.receiveMessage(server);
		if (response == null) return;
		while (response.startsWith(RESPONSE_BLOCK)) {
			System.out.println("Waiting for group bid rights to become available...");
			response = clManager.receiveMessage(server);
		}
		tokens = response.split(" ");
		if (response.equals(RESPONSE_NO_AUCTION)) {
			System.out.println("No auction with that id exists");
			return;
		}
		
		if (response.startsWith(RESPONSE_FAIL) && tokens.length == 1) {
			System.out.println("Bidding on auction failed");
			return;
		}
		String responseAmount = tokens[1];
		String responseAuction = tokens[2];
		for (int i = 3; i < tokens.length; i++)
			responseAuction += tokens[i];
		
		String msg = null;
		if (response.startsWith(RESPONSE_SUCCESS)) {
			msg = String.format("You successfully bid with %s on '%s'.", responseAmount, responseAuction);
		} else {
			msg = String.format("You unsuccessfully bid with %.2f on '%s'. Current highest bid is %s.", amount, responseAuction, responseAmount);
		}
		System.out.println(msg);
	}

	/**
	 * The client communicates confirmation of a bid to the server.
	 */
	private void confirm(String input) throws IOException {
		// parse
		String[] tokens = input.split(" ");
		if (tokens.length != 4) {
			System.err.println("Wrong number of parameters!");
			return;
		}
		
		int auctionId;
		double amount;
		String initiator;
		try {
			auctionId =  Integer.valueOf(tokens[1]);
			amount = Double.valueOf(tokens[2]);
			initiator = tokens[3];
		} catch (NumberFormatException e) {
			System.err.println("Not a valid value!");
			return;
		}

		// send confirm to server
		String message = String.format("%s %d %f %s", CMD_CONFIRM, auctionId, amount, initiator);
		clManager.sendMessage(server, message);
		
		// receive response
		String response = clManager.receiveMessage(server);
		if (response == null) return;
		while (response.startsWith(RESPONSE_BLOCK)) {
			System.out.println("Please wait for the bid to be fully confirmed...");
			response = clManager.receiveMessage(server);
		}
		tokens = response.split(" ");
		if (response.equals(RESPONSE_REJECTED)) {
			System.out.format("Rejected: %s%n", response.substring(RESPONSE_REJECTED.length() + 1));
			return;
		}
		
		if (response.startsWith(RESPONSE_FAIL) && tokens.length == 1) {
			System.out.println("Bidding on auction failed");
			return;
		}

		if (response.startsWith(RESPONSE_CONFIRMED)) {
			System.out.println("Successfully confirmed.");
		} else {
			System.out.println("Bid unsuccessful.");
		}
	}
	
	private void sendSignedBids() {
		synchronized (signedBids) {
			String saved = signedBids.get(user);
			if (saved == null) return;
			
			String failed = "";
			
			String[] bids = saved.split("\n");
			for (String bid : bids) {
				clManager.sendMessage(server, bid);
			}
			
			if (failed.equals("")) failed = null;
			signedBids.put(user, failed);
		}
	}
	
	private void shutdown() {
		if (reconnectTask != null) reconnectTask.cancel();
		reconnectTask = null;
		
		reconnectTimer.cancel();
	}

	private boolean isLoggedIn() {
		return user != null;
	}

	public String getUsername() {
		return user;
	}
}

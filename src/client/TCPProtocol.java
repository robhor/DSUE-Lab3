package client;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Base64;

import server.bean.Client;
import server.service.ClientManager;
import util.SecurityUtils;

/**
 * Protocol the client uses to communicate with the auction server
 */
public class TCPProtocol {
	public static final int    KEYSIZE    = 256; /** Keysize for AES-Cipher */
	
    public static final String CMD_LOGIN        = "!login";
	public static final String CMD_LOGOUT       = "!logout";
	public static final String CMD_LIST         = "!list";
	public static final String CMD_CREATE       = "!create";
	public static final String CMD_BID          = "!bid";
	public static final String CMD_EXIT         = "!end";
	public static final String CMD_UDP          = "!udp";
	public static final String CMD_ACTIVE_USERS = "!getClientList";
	
	public static final String RESPONSE_FAIL       = "!fail";
	public static final String RESPONSE_SUCCESS    = "!ok";
	public static final String RESPONSE_NO_AUCTION = "!no-auction";
	
	private Logger logger = Logger.getLogger(TCPProtocol.class.getSimpleName());
	
	private ClientManager clManager;
	private String user;
	private Client server;
	private int udpPort;
	
	private UDPProtocol udpProtocol;
	private PublicKey serverKey;
	private String clientKeyDir;
	
	public TCPProtocol(ClientManager clManager, PublicKey serverKey, String clientKeyDir) {
		this.clManager = clManager;
		this.serverKey = serverKey;
		this.clientKeyDir = clientKeyDir;
		this.user = null;
	}
	
	public void setServer(Client server) {
		this.server = server;
	}
	
	public void setUdpPort(Integer udpPort) {
		this.udpPort = udpPort;
	}
	
	public void setUdpProtocol(UDPProtocol udpProtocol) {
		this.udpProtocol = udpProtocol;
	}
	

	public boolean processInput(String input) {
		Scanner scanner = new Scanner(input);
		if (!scanner.hasNext()) return true;
		
		String token = scanner.next();
		
		if (token.equals(CMD_LOGIN)) {
			if(!login(input)) return false;
		} else if (token.equals(CMD_LOGOUT)) {
			clManager.sendMessage(server, CMD_LOGOUT);
			
			if (isLoggedIn()) {
				System.out.println("Successfully logged out");
			} else {
				System.out.println("You have to log in first!");
			}
			
			user = null;
			clManager.unsecureConnection(server);
			if (udpProtocol != null) udpProtocol.setUser(null);
		} else if (token.equals(CMD_EXIT)) {
			return false;
		} else if (token.equals(CMD_LIST)) {
			if (!listAuctions()) return false;
		} else if (token.equals(CMD_CREATE)) {
			if (!createAuction(input)) return false;
		} else if (token.equals(CMD_BID)) {
			if (!bid(input)) return false;
		} else if (token.equals(CMD_ACTIVE_USERS)) {
			if (!listActiveUsers()) return false;
		} else {
			System.out.println("unknown command");
		}
		
		return true;
	}
	
	private boolean login(String input) {
		// parse input
		String[] tokens = input.split(" ");
		if (tokens.length < 2) return true;
		String username = tokens[1];
		if (udpProtocol != null) udpProtocol.setUser(username);
		
		if (loginUser(username)) {
			System.out.println("Successfully logged in as " + user + "!");
		} else {
			if (udpProtocol != null) udpProtocol.setUser(user);
			System.out.println("Login unsuccessful");
		}
		return true;
	}
	
	/**
	 * Logs in the user with the given username
	 * @param username
	 * @return true if login was successful
	 */
	private boolean loginUser(String username) {
		// check if user exists by checking if .pem exists
		String clientKeyPath = clientKeyDir + username + ".pem";
		File f = new File(clientKeyPath);
		if (!f.exists()) return false;
		
		String pass;
		PrivateKey privateKey;
		try {
			System.out.println("Enter pass phrase:");
			pass = new BufferedReader(new InputStreamReader(System.in)).readLine();
			privateKey = SecurityUtils.getPrivateKey(clientKeyPath, pass);
		} catch (IOException e) {
			logger.log(Level.INFO, "Private key could not be read");
			return false;
		}

		
		boolean handshakeSuccessful = false;
		try {
			handshakeSuccessful = handshake(username, privateKey);
		} catch (IOException e) {
			logger.log(Level.WARNING, "IOException during handshake: " + e.getMessage());
		}
		
		if (!handshakeSuccessful) return false;
		
		user = username;
		
		return true;
	}
	
	private boolean handshake(String username, PrivateKey privateKey) throws IOException {
		String message;
		String[] tokens;
		
		byte[] msg;
		byte[] clientChallenge;
		
		
		/* **************************************************************************************
		 *              Step 1: send !login <username> <tcpPort> <clientChallenge>
		 * **************************************************************************************/
		int tcpPort = 0; // TODO tcpPort is required for stage 4
		
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

	private boolean listAuctions() {
		clManager.sendMessage(server, CMD_LIST);
		String msg = clManager.receiveMessage(server);
		if (msg == null) return false;
		
		int auctions = Integer.valueOf(msg);
		for (int i = 0; i < auctions; i++) {
			msg = clManager.receiveMessage(server);
			System.out.println(msg);
		}
		
		return true;
	}
	
	private boolean listActiveUsers() {
		clManager.sendMessage(server, CMD_ACTIVE_USERS);
		String msg = clManager.receiveMessage(server);
		
		System.out.println(msg);
		
		return true;
	}

	private boolean createAuction(String input) {
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
	
	private boolean bid(String input) {
		// parse
		String[] tokens = input.split(" ");
		if (tokens.length < 3) return true;
		double bid;
		try {
			Integer.valueOf(tokens[1]);
			bid = Double.valueOf(tokens[2]);
		} catch (NumberFormatException e) {
			System.err.println("Not a valid value!");
			return true;
		}
		
		// send bid to server
		clManager.sendMessage(server, input);
		
		// receive response
		String response = clManager.receiveMessage(server);
		if (response == null) return false;
		if (response.equals(RESPONSE_NO_AUCTION)) {
			System.out.println("No auction with that id exists");
			return true;
		}
		
		tokens = response.split(" ");
		if (response.startsWith(RESPONSE_FAIL) && tokens.length == 1) {
			System.out.println("Bidding on auction failed");
			return true;
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
		
		return true;
	}
	
	private boolean isLoggedIn() {
		return user != null;
	}

	public String getUsername() {
		return user;
	}

}

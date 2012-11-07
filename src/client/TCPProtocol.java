package client;

import java.util.Scanner;

import server.bean.Client;
import server.service.ClientManager;

public class TCPProtocol {
	public static final String CMD_LOGIN  = "!login";
	public static final String CMD_LOGOUT = "!logout";
	public static final String CMD_LIST   = "!list";
	public static final String CMD_CREATE = "!create";
	public static final String CMD_BID    = "!bid";
	public static final String CMD_EXIT   = "!end";
	
	public static final String RESPONSE_FAIL       = "!fail";
	public static final String RESPONSE_SUCCESS    = "!ok";
	public static final String RESPONSE_NO_AUCTION = "!no-auction";
	
	private ClientManager clManager;
	private String user;
	private Client server;
	private int udpPort;
	private UDPProtocol udpProtocol;
	
	public TCPProtocol(ClientManager clManager) {
		this.clManager = clManager;
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
			server.getWriter().println(input);
			
			if (isLoggedIn()) {
				System.out.println("Successfully logged out");
			} else {
				System.out.println("You have to log in first!");
			}
			
			user = null;
			if (udpProtocol != null) udpProtocol.setUser(null);
		} else if (token.equals(CMD_EXIT)) {
			return false;
		} else if (token.equals(CMD_LIST)) {
			if (!listAuctions()) return false;
		} else if (token.equals(CMD_CREATE)) {
			if (!createAuction(input)) return false;
		} else if (token.equals(CMD_BID)) {
			if (!bid(input)) return false;
		} else {
			System.out.println("unknown command");
			clManager.sendMessage(server, input);
		}
		
		return true;
	}
	
	private boolean login(String input) {
		if (input == null) return true;
		String[] tokens = input.split(" ");
		if (tokens.length < 2) return true;
		
		String username = tokens[1];
		if (udpProtocol != null) udpProtocol.setUser(username);
		clManager.sendMessage(server, CMD_LOGIN + " " + username + " " + udpPort);
		
		String response = clManager.receiveMessage(server);
		if (response == null) return false;
		
		if (response.equals(RESPONSE_SUCCESS)) {
			user = username;
			
			System.out.println("Successfully logged in as " + user + "!");
		} else {
			if (udpProtocol != null) udpProtocol.setUser(user);
			System.out.println("Login unsuccessful");
		}
		
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

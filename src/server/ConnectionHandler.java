package server;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import server.bean.Auction;
import server.bean.Client;
import server.bean.User;
import server.service.AuctionManager;
import server.service.ClientManager;
import server.service.UserManager;
import client.TCPProtocol;


public class ConnectionHandler implements Runnable {
	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	private Client client;
	private User user;
	private ClientManager clManager;
	private UserManager usManager;
	private AuctionManager auManager;
	
	public ConnectionHandler(Client client, ClientManager clManager, UserManager usManager, AuctionManager auManager) {
		this.client = client;
		this.clManager = clManager;
		this.usManager = usManager;
		this.auManager = auManager;
	}

	@Override
	public void run() {
		// listen for messages
		String input;
		while ((input = clManager.receiveMessage(client)) != null) {
			processMessage(input);
		}
		
		// close resources
		if (user != null) usManager.disconnect(user);
		clManager.disconnect(client);
	}
	
	private void processMessage(String msg) {
		String[] tokens = msg.split(" ");
		String cmd = tokens[0];
		
		if (cmd.equals(TCPProtocol.CMD_LOGIN)) {
			login(tokens);
		} else if (cmd.equals(TCPProtocol.CMD_LOGOUT)) {
			logout(tokens);
		} else if (cmd.equals(TCPProtocol.CMD_CREATE)) {
			createAuction(tokens);
		} else if (cmd.equals(TCPProtocol.CMD_LIST)) {
			listAuctions(tokens);
		} else if (cmd.equals(TCPProtocol.CMD_BID)) {
			bid(tokens);
		}
	}
	
	private void login(String[] tokens) {
		if (tokens.length < 2) return;
		
		String name = tokens[1];
		
		User newUser = usManager.login(name, client);
		if (newUser == null) {
			clManager.sendMessage(client, TCPProtocol.RESPONSE_FAIL);
			return;
		} else {
			if (user != null) usManager.logout(user);
			user = newUser;
			clManager.sendMessage(client, TCPProtocol.RESPONSE_SUCCESS);
		}
		
		if (tokens.length >= 3) {
			int udpPort;
			try {
				udpPort = Integer.valueOf(tokens[2]);
				client.setUdpPort(udpPort);
			} catch (NumberFormatException e) {
				logger.log(Level.INFO, "Received invalid udpPort");
			}
		}
	}
	
	private void logout(String[] tokens) {
		usManager.logout(user);
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
		Map<Integer, Auction> list = auManager.getAuctions();
		SimpleDateFormat sdf = new SimpleDateFormat();
		
		String message = "";
		
		synchronized (list) {
			message += list.size();
			
			for (Auction a : list.values()) {
				User bidder = a.getHighestBidder();
				String bidderName = (bidder == null) ? "none" : bidder.getName();
				String line = String.format("%d. '%s' by %s %s %.2f %s",
						a.getId(),
						a.getName(),
						a.getOwner().getName(),
						sdf.format(a.getEndTime().getTime()),
						a.getHighestBid(),
						bidderName);
				
				message += "\n" + line;
			}
		}
		
		clManager.sendMessage(client, message);
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
		if (auction == null) {
			usManager.sendMessage(user, TCPProtocol.RESPONSE_NO_AUCTION);
			return;
		}
		
		boolean success = auManager.bid(user, auction, amount);
		String msg = (success) ? TCPProtocol.RESPONSE_SUCCESS : TCPProtocol.RESPONSE_FAIL;
		msg += String.format(" %.2f %s", auction.getHighestBid(), auction.getName());
		usManager.sendMessage(user, msg);
	}
}

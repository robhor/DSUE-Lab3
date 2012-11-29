package loadtest;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import server.bean.Client;
import server.service.impl.ClientManagerImpl;
import client.TCPProtocol;

/**
 * Test client that logs in, and then periodically
 * creates, updates, and bids on auctions.
 */
public class TestClient extends Thread {
	private static final boolean DEBUG = false;
	
	private String host;
	private int port;
	private int auctionsPerMin, auctionDuration, updateInterval, bidsPerMin, clientNr;
	private long startTime;
	private Client client;
	private int auctionCount = 0;
	private Random rand;
	
	private ArrayList<Integer> activeAuctions;
	private Timer createTimer, updateTimer, bidTimer;
	
	public TestClient(String host, int port,
					  int auctionsPerMin, int auctionDuration,
					  int updateIntervalSec, int bidsPerMin,
					  int clientNr, long startTime) {
		this.host = host;
		this.port = port;
		this.auctionsPerMin = auctionsPerMin;
		this.auctionDuration = auctionDuration;
		this.updateInterval = updateIntervalSec;
		this.bidsPerMin = bidsPerMin;
		this.clientNr = clientNr;
		this.startTime = startTime;
		
		rand = new Random(clientNr * startTime);
		activeAuctions = new ArrayList<Integer>();
	}
	
	public void run() {
		Socket sock;
		try {
			sock = new Socket(host, port);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		client = new ClientManagerImpl().newClient(sock);
		
		login();
		
		// starting timer
		createTimer = new Timer();
		createTimer.scheduleAtFixedRate(new TimerTask() {
			public void run() { createAuction(); }
		}, new Date(), freqToIntv(auctionsPerMin));
		
		updateTimer = new Timer();
		updateTimer.scheduleAtFixedRate(new TimerTask() {
			public void run() { updateActiveAuctions(); }
		}, new Date(), updateInterval * 1000);
		
		bidTimer = new Timer();
		bidTimer.scheduleAtFixedRate(new TimerTask() {
			public void run() { bid(); }
		}, new Date(), freqToIntv(bidsPerMin));
	}
	
	/**
	 * @param perMinute 
	 * @return interval in msec 
	 */
	private int freqToIntv(int perMinute) {
		return 60 * 1000 / perMinute;
	}
	
	
	
	/**
	 * @return true if login was successful
	 */
	public boolean login() {
		write("!login testClient" + clientNr + " 1234");
		String answer = read();
		if (answer.equals(TCPProtocol.RESPONSE_SUCCESS)) return true;
		
		return false;
	}
	
	
	
	
	
	/**
	 * Sends !list and looks at the answer.
	 */
	public void updateActiveAuctions() {
		if (DEBUG) System.out.println("\nUpdating Auctions list");
		
		synchronized (activeAuctions) {
			activeAuctions.clear();
		}
		synchronized (client) {
			write("!list");
			String line = read();
			Integer lines = Integer.parseInt(line);
			
			for (int i = 0; i < lines; i++) {
				line = read();
				synchronized (activeAuctions) {
					activeAuctions.add(parseAuctionId(line));
				}
			}
		}
	}
	
	/**
	 * Takes a line returned by !list
	 * and extracts the id of the auction
	 * @param line
	 */
	private int parseAuctionId(String line) {
		// Get id nr at start of line
		String idstr = line.substring(0, line.indexOf("."));
		return Integer.parseInt(idstr);
	}
	
	
	public boolean createAuction() {
		if (DEBUG) System.out.println("\nCreating auction..");
		String answer;
		synchronized (client) {
			write("!create " + auctionDuration + 
					" testClient" + clientNr + "'s great auction nr. " + auctionCount);
			answer = read();
		}
		if (!answer.equals(TCPProtocol.RESPONSE_SUCCESS)) return false;
		
		auctionCount++;
		return true;
	}
	
	public void bid() {
		if (DEBUG) System.out.println("\nBidding on something..");
		
		int auction;
		synchronized (activeAuctions) {
			if (activeAuctions.size() == 0) return;
			int index = rand.nextInt(activeAuctions.size());
			auction = activeAuctions.get(index);
		}
		
		bidOn(auction);
	}
	
	private void bidOn(int auctionId) {
		if (DEBUG) System.out.println("bidding on " + auctionId);
		long timeGone =  new Date().getTime() - startTime;
		float dollars =  timeGone / 10.0f;
		
		String bidStr = String.format("!bid %d %.2f", auctionId, dollars);
		
		synchronized (client) {
			write(bidStr);
			read();
		}
	}
	
	
	private void write(String str) {
		if (DEBUG) System.out.println("write: " + str);
		client.getWriter().println(str);
	}
	
	private String read() {
		String line;
		try {
			line = client.getReader().readLine();
			if (DEBUG) System.out.println("read: " + line);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		return line;
	}
	
	public void shutdown() {
		if(createTimer != null) createTimer.cancel();
		if(updateTimer != null) updateTimer.cancel();
		if(bidTimer != null)    bidTimer.cancel();
	}

}

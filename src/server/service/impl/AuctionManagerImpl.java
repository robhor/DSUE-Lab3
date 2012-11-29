package server.service.impl;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import server.bean.Auction;
import server.bean.User;
import server.service.AuctionManager;
import server.service.UserManager;
import analytics.event.AuctionEvent;
import analytics.event.BidEvent;
import analytics.event.Event;
import billing.BillingServerSecure;
import client.UDPProtocol;

public class AuctionManagerImpl implements AuctionManager {
	private Logger logger = Logger.getLogger(AuctionManagerImpl.class.getSimpleName());
	private AtomicInteger auctionID; /** Next free auction id */
	private TreeMap<Integer, Auction> auctions;
	
	private UserManager usManager;
	private Timer timer;

	private BillingServerSecure billingServer;
	private AnalyticsServerWrapper analyticsServer;
	
	private ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
	private Lock readLock = readWriteLock.readLock();
	private Lock writeLock = readWriteLock.writeLock();

	public AuctionManagerImpl(UserManager usManager, BillingServerSecure billingServer, AnalyticsServerWrapper analyticsServer) {
		this.usManager = usManager;
		this.billingServer = billingServer;
		this.analyticsServer = analyticsServer;
		
		auctionID = new AtomicInteger();
		auctions = new TreeMap<Integer, Auction>();
		
		timer = new Timer();
	}

	@Override
	public Auction createAuction(User owner, String name, int duration) {
		if (owner == null) throw new IllegalArgumentException("Owner can't be null");
		if (name == null) throw new IllegalArgumentException("Name can't be null");
		if (duration < 0) throw new IllegalArgumentException("Duration must be at least 0 seconds!");
		
		int id = auctionID.incrementAndGet();
		
		Auction auction = new Auction();
		auction.setId(id);
		auction.setOwner(owner);
		auction.setName(name);
		auction.setHighestBid(0);
		
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.SECOND, duration);
		
		auction.setEndTime(calendar);
		
		AuctionEndTask task = new AuctionEndTask(auction);
		timer.schedule(task, calendar.getTime());
		
		writeLock.lock();
		try {
			auctions.put(id, auction);
		} finally {
			writeLock.unlock();
		}

		// notify analytics
		Event event = new AuctionEvent("AUCTION_STARTED", System.currentTimeMillis(), auction.getId());
		analyticsServer.processEvent(event);
		
		return auction;
	}

	@Override
	public void closeAuction(Auction auction) {
		if (auction == null) return;
		
		writeLock.lock();
		try {
			auctions.remove(auction.getId());
		} finally {
			writeLock.unlock();
		}
		
		User winner;
		String msg = null;
		double highestBid;
		
		synchronized (auction) {
			winner = auction.getHighestBidder();
			highestBid = auction.getHighestBid();
		}

		
		// notify winner
		if (winner != null) {
			msg = String.format("%s %s %.2f %s", UDPProtocol.AUCTION_END, winner.getName(), highestBid, auction.getName());
			usManager.postMessage(winner, msg);
			
			Event event = new BidEvent("BID_WON", System.currentTimeMillis(), winner.getName(), auction.getId(), highestBid);
			analyticsServer.processEvent(event);
		}

		// notify analytics
		Event event = new AuctionEvent("AUCTION_ENDED", System.currentTimeMillis(), auction.getId());
		analyticsServer.processEvent(event);
		
		// notify owner
		if (winner != null && winner != auction.getOwner())
			usManager.postMessage(auction.getOwner(), msg);
		
		// bill owner
		try {
			if (billingServer != null)
				billingServer.billAuction(auction.getOwner().getName(), auction.getId(), highestBid);
		} catch (RemoteException e) {
			logger.log(Level.WARNING, "Could not bill auction: " + e.getMessage());
		}
	}

	@Override
	public Collection<Auction> getAuctions() {
		readLock.lock();
		try {
			return new ArrayList<Auction>(auctions.values());
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public boolean bid(User bidder, Auction auction, double amount) {
		if (bidder == null) throw new IllegalArgumentException("Bidder can't be null!");
		if (auction == null) throw new IllegalArgumentException("Auction can't be null!");
		if (amount <= 0) throw new IllegalArgumentException("Must bid at least 0.01 units of currency!");
		
		boolean overbid = false;
		
		synchronized (auction) {
			if (amount < auction.getHighestBid()) {
				return false;
			}
			
			if (auction.getHighestBidder() != null && auction.getHighestBidder() != bidder) {
				String msg = UDPProtocol.OVERBID + " " + auction.getName();
				usManager.postMessage(auction.getHighestBidder(), msg);
				overbid = true;
			}
			auction.setHighestBid(amount);
			auction.setHighestBidder(bidder);
		}

		// notify analytics
		String eventType = overbid ? "BID_OVERBID" : "BID_PLACED";
		Event event = new BidEvent(eventType, System.currentTimeMillis(), bidder.getName(), auction.getId(), amount);
		analyticsServer.processEvent(event);
		
		return true;
	}
	
	
	private class AuctionEndTask extends TimerTask {
		private Auction auction;
		
		public AuctionEndTask(Auction auction) {
			super();
			this.auction = auction;
		}
		
		@Override
		public void run() {
			closeAuction(auction);
		}
		
	}


	@Override
	public void shutdown() {
		timer.cancel();
	}

	@Override
	public Auction getAuctionById(int id) {
		readLock.lock();
		try {
			return auctions.get(id);
		} finally {
			readLock.unlock();
		}
	}
}

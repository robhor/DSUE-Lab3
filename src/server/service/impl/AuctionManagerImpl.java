package server.service.impl;

import java.util.Calendar;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import server.bean.Auction;
import server.bean.User;
import server.service.AuctionManager;
import server.service.UserManager;
import client.UDPProtocol;

public class AuctionManagerImpl implements AuctionManager {
	private int auctionID; /** Next free auction id */
	private TreeMap<Integer, Auction> auctions;
	
	private UserManager usManager;
	private Timer timer;
	
	public AuctionManagerImpl(UserManager usManager) {
		this.usManager = usManager;
		auctionID = 1;
		auctions = new TreeMap<Integer, Auction>();
		
		timer = new Timer();
	}

	@Override
	public Auction createAuction(User owner, String name, int duration) {
		if (owner == null) throw new IllegalArgumentException("Owner can't be null");
		if (name == null) throw new IllegalArgumentException("Name can't be null");
		if (duration < 0) throw new IllegalArgumentException("Duration must be at least 0 seconds!");
		
		Auction auction = new Auction();
		auction.setId(auctionID);
		auction.setOwner(owner);
		auction.setName(name);
		auction.setHighestBid(0);
		
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.SECOND, duration);
		
		auction.setEndTime(calendar);
		
		AuctionEndTask task = new AuctionEndTask(auction);
		timer.schedule(task, calendar.getTime());
		
		synchronized (this) {
			auctions.put(auctionID++, auction);
		}
		return auction;
	}

	@Override
	public void closeAuction(Auction auction) {
		if (auction == null) return;
		synchronized (this) {
			auctions.remove(auction.getId());
			
			// notify winner
			User winner = auction.getHighestBidder();
			String msg = null;
			if (winner != null) {
				msg = String.format("%s %s %.2f %s", UDPProtocol.AUCTION_END, winner.getName(), auction.getHighestBid(), auction.getName());
				usManager.postMessage(winner, msg);
			}
			
			// notify owner
			if (winner != null && winner != auction.getOwner())
				usManager.postMessage(auction.getOwner(), msg);
		}
	}

	@Override
	public Collection<Auction> getAuctions() {
		return auctions.values();
	}

	@Override
	public boolean bid(User bidder, Auction auction, double amount) {
		if (bidder == null) throw new IllegalArgumentException("Bidder can't be null!");
		if (auction == null) throw new IllegalArgumentException("Auction can't be null!");
		if (amount <= 0) throw new IllegalArgumentException("Must bid at least 0.01 units of currency!");
		
		boolean success = false;
		synchronized (auction) {
			if (amount > auction.getHighestBid()) {
				if (auction.getHighestBidder() != null && auction.getHighestBidder() != bidder) {
					String msg = UDPProtocol.OVERBID + " " + auction.getName();
					usManager.postMessage(auction.getHighestBidder(), msg);
				}
				auction.setHighestBid(amount);
				auction.setHighestBidder(bidder);
				
				success = true;
			}
		}
		
		return success;
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
		return auctions.get(id);
	}
}

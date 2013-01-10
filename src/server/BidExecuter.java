package server;

import server.bean.Auction;
import server.bean.GroupBid;
import server.bean.User;
import server.service.AuctionManager;
import server.service.UserManager;
import client.TCPProtocol;
import client.UDPProtocol;

/**
 * Implements confirmed groupBids
 */
public class BidExecuter implements Runnable {
	private final AuctionManager auManager;
	private final UserManager usManager;
	private GroupBid groupBid;
	private volatile String confirmerMessage;
	
	public BidExecuter(AuctionManager auManager, UserManager usManager) {
		this.auManager = auManager;
		this.usManager = usManager;
	}

	/**
	 * @return the confirmerMessage
	 */
	public String getConfirmerMessage() {
		return confirmerMessage;
	}

	@Override
	public void run() {
		Auction auction = auManager.getAuctionById(groupBid.getAuctionId());
		double amount = groupBid.getAmount();
		User bidder = groupBid.getUser();
		String bidderMessage;
		
		auManager.getTheGroup().removeGroupBid(groupBid);
		
		if (auManager.bid(bidder, auction, amount)) {
			bidderMessage = UDPProtocol.CONFIRMED + " " + auction.getName();
			confirmerMessage = TCPProtocol.RESPONSE_CONFIRMED;
		} else {
			bidderMessage = UDPProtocol.REJECTED + " " + auction.getName();
			confirmerMessage = String.format("%s Bid on '%s' failed.", TCPProtocol.RESPONSE_REJECTED, auction.getName());
		}
		
		usManager.postMessage(bidder, bidderMessage);
	}

	/**
	 * @param groupBid the groupBid to set
	 */
	public void setGroupBid(GroupBid groupBid) {
		this.groupBid = groupBid;
	}
}
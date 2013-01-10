package server.service;

import java.util.Collection;

import server.AuctionException;
import server.bean.Auction;
import server.bean.Group;
import server.bean.User;

/**
 * Handles auctions
 * and their creation, expiration, and bidding on auctions.
 */
public interface AuctionManager {
	/**
	 * Creates a new auction
	 * @param owner / creator
	 * @param name of the auction
	 * @param duration in seconds
	 * @return
	 */
	Auction createAuction(User owner, String name, int duration);
	
	/**
	 * Ends an auction, notifies the winner and the owner
	 * @param auction
	 */
	void closeAuction(Auction auction);
	
	/**
	 * @return a list of currently active auctions
	 */
	Collection<Auction> getAuctions();
	
	/**
	 * @return the group
	 */
	Group getTheGroup();
	
	/**
	 * @return the auction with the given id
	 *         or null if it does not exist
	 */
	Auction getAuctionById(int id);
	
	/**
	 * Bids on an auction
	 * @param bidder
	 * @param auction
	 * @param amount
	 * @return true, if bid was successful
	 */
	boolean bid(User bidder, Auction auction, double amount);
	
	/**
	 * Initiates a GroupBid, blocking if necessary.
	 * @return true, if bid was successful
	 */
	boolean groupBid(int auctionId, User bidder, double amount);
	
	/**
	 * Confirms a group bid.
	 * @return true, if bid was successful
	 * @throws AuctionException if the GroupBid was not found
	 */
	boolean confirmBid(User confirmer, Auction auction, double amount, String initiator) throws AuctionException;
	
	/**
	 * Prepares for system shutdown
	 */
	void shutdown();
}

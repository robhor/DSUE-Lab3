package server.service;

import java.util.Map;

import server.bean.Auction;
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
	Map<Integer, Auction> getAuctions();
	
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
	 * Prepares for system shutdown
	 */
	void shutdown();
}

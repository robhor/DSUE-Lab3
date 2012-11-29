package analytics.bean;

import java.util.HashMap;
import java.util.Map;

import analytics.AnalyticsException;

/**
 * Maintains a list of running auctions.
 */
public class Auctions {
	private final Map<Long, Auction> auctions;
	
	/**
	 * Constructor.
	 */
	public Auctions() {
		auctions = new HashMap<Long,Auction> ();
	}
	
	/**
	 * Adds an auction to the list.
	 */
	public void add(Auction auction) {
		auctions.put(auction.getID(), auction);
	}
	
	/**
	 * Marks an auction as having succeeded. An auction is considered
	 * successful if it receives at least one bid.
	 * @param auctionID The ID of the auction
	 * @throws AnalyticsException if the auction ID is invalid
	 */
	public void setSuccessful(long auctionID) throws AnalyticsException {
		Auction auction = auctions.get(auctionID);
		
		if (null == auction) {
			throw new AnalyticsException(String.format("Failed to set successful auction with invalid ID: \"%s\"", auctionID));
		}
		
		auction.setSuccess(true);
	}
	
	/**
	 * Removes an auction from the list.
	 * @param auctionID The ID of the auction
	 * @return the auction with that ID or null if the ID could not be found
	 */
	public Auction remove(long auctionID) {
		return auctions.remove(auctionID);
	}
}

package analytics.bean;

/**
 * Keeps track of some statistics about bids on auctions.
 */
public class BidStats {
	private final long startTime;
	private int countBids;
	private double maxBid;
	
	/**
	 * Constructor.
	 */
	public BidStats() {
		startTime = System.currentTimeMillis();
		countBids = 0;
		maxBid = Double.NEGATIVE_INFINITY;
	}
	
	/**
	 * Adds a bid to the stats.
	 * @param price Bid amount
	 */
	public void add(double price) {
		countBids++;
		if (price > maxBid) {
			maxBid = price;
		}
	}
	
	/**
	 * Get the maximum of all recorded bids. 
	 * @return the maximum recorded bid value
	 */
	public double max() {
		return maxBid;
	}
	
	/**
	 * Get the average number of bids received every minute. 
	 * @return the number of bids per minute
	 */
	public double bidsPerMin() {
		long timeMillis = System.currentTimeMillis() - startTime; 
		return 60000.0 * countBids / timeMillis;
	}
}

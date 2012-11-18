package analytics.bean;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps track about some general auction stats.
 */
public class AuctionStats {
	private final List<Long> times;
	private int countSuccesses;
	private int countAuctions;
	
	/**
	 * Constructor.
	 */
	public AuctionStats() {
		times = new ArrayList<Long> ();
		countSuccesses = 0;
		countAuctions = 0;
	}
	
	/**
	 * Adds a finished auction to the stats.
	 * @param time Duration of the auction
	 * @param success Indicates whether the auction was successful
	 */
	public void add(long time, boolean success) {
		times.add(time);
		countAuctions++;
		if (success) {
			countSuccesses++;
		}
	}
	
	/**
	 * Computes the average auction duration.
	 * @return the average auction duration
	 */
	public double timeAvg() {
		double avg = 0;
		for (int i = 0; i < times.size(); i++) {
			avg += times.get(i) / (double)countAuctions;
		}
		return avg;
	}
	
	/**
	 * Computes the ratio of successful auctions in all finished auctions.
	 * @return the auction success ratio
	 */
	public double successRatio() {
		return countSuccesses / (double)countAuctions;
	}
}

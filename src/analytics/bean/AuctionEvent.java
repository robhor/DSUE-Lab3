package analytics.bean;

/**
 * An auction-specific event.
 */
public class AuctionEvent extends Event {
	private static final long serialVersionUID = -481606403923121763L;

	private final long auctionID;

	/**
	 * Constructor.
	 * @param type Event type string
	 * @param timestamp Time of event occurence
	 * @param auctionID ID of the auction affected by the event
	 */
	public AuctionEvent(String type, long timestamp, long auctionID) {
		super(type, timestamp);
		this.auctionID = auctionID;
	}
	
	/**
	 * @return the auctionID
	 */
	public long getAuctionID() {
		return auctionID;
	}
}

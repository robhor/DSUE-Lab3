package analytics.event;


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
	
	/**
	 * Produces a string representation of the event.
	 * Used for printing in ManagementClient.
	 * @return The event string
	 */
	public String toString() {
		if ("AUCTION_STARTED".equals(getType())) {
			return String.format("%s - auction %s has started", super.toString(), getAuctionID());	
		} else
		if ("AUCTION_ENDED".equals(getType())) {
			return String.format("%s - auction %s has ended", super.toString(), getAuctionID());	
		}
		else {
			return super.toString(); // fallback for invalid type
		}
	}
}

package analytics.bean;

public class AuctionEvent extends Event {
	private static final long serialVersionUID = -481606403923121763L;

	private final long auctionID;

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

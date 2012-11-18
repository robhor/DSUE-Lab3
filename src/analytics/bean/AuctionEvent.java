package analytics.bean;

public class AuctionEvent extends Event {
	private static final long serialVersionUID = -481606403923121763L;

	private long auctionID;

	/**
	 * @return the auctionID
	 */
	public long getAuctionID() {
		return auctionID;
	}

	/**
	 * @param auctionID the auctionID to set
	 */
	public void setAuctionID(long auctionID) {
		this.auctionID = auctionID;
	}
}

package analytics.bean;

public class BidEvent extends Event {
	private static final long serialVersionUID = -7312684815832826724L;

	private final String userName;
	private final long auctionID;
	private final double price;

	public BidEvent(String type, long timestamp, String userName,
	                 long auctionID, double price) {
		super(type, timestamp);
		this.userName = userName;
		this.auctionID = auctionID;
		this.price = price;
	}
	
	/**
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * @return the auctionID
	 */
	public long getAuctionID() {
		return auctionID;
	}
	
	/**
	 * @return the price
	 */
	public double getPrice() {
		return price;
	}
}

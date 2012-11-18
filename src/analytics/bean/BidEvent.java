package analytics.bean;

public class BidEvent extends Event {
	private static final long serialVersionUID = -7312684815832826724L;

	private String userName;
	private long auctionID;
	private double price;

	/**
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * @param userName the userName to set
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}

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
	
	/**
	 * @return the price
	 */
	public double getPrice() {
		return price;
	}
	
	/**
	 * @param price the price to set
	 */
	public void setPrice(double price) {
		this.price = price;
	}
}

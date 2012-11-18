package analytics.bean;

/**
 * Contains all data about an auction that is relevant
 * for analytics.
 */
public class Auction {
	private long ID;
	private long startTime;
	private boolean success;
	
	/**
	 * @return the iD
	 */
	public long getID() {
		return ID;
	}
	
	/**
	 * @param iD the iD to set
	 */
	public void setID(long ID) {
		this.ID = ID;
	}
	
	/**
	 * @return the startTime
	 */
	public long getStartTime() {
		return startTime;
	}
	
	/**
	 * @param startTime the startTime to set
	 */
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	
	/**
	 * @return the success
	 */
	public boolean isSuccess() {
		return success;
	}
	
	/**
	 * @param success the success to set
	 */
	public void setSuccess(boolean success) {
		this.success = success;
	}
}

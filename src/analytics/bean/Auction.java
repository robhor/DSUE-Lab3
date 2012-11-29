package analytics.bean;

/**
 * Contains all data about an auction that is relevant
 * for analytics.
 */
public class Auction {
	private final long ID;
	private final long startTime;
	private boolean success;
	
	/**
	 * Constructor.
	 * @param ID auction ID
	 * @param startTime start time in millis since epoch
	 */
	public Auction(long ID, long startTime) {
		this.ID = ID;
		this.startTime = startTime;
		success = false;
	}
	
	/**
	 * @return the ID
	 */
	public long getID() {
		return ID;
	}
	
	/**
	 * @return the startTime
	 */
	public long getStartTime() {
		return startTime;
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

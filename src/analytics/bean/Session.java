package analytics.bean;

/**
 * Contains all data about a user session that is relevant
 * for analytics.
 */
public class Session {
	private String userName;
	private long startTime;
	
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
}

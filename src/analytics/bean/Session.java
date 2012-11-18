package analytics.bean;

/**
 * Contains all data about a user session that is relevant
 * for analytics.
 */
public class Session {
	private final String userName;
	private final long startTime;
	
	public Session(String userName, long startTime) {
		this.userName = userName;
		this.startTime = startTime;
	}
	
	/**
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}
	
	/**
	 * @return the startTime
	 */
	public long getStartTime() {
		return startTime;
	}
}

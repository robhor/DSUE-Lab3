package analytics.bean;

public class UserEvent extends Event {
	private static final long serialVersionUID = -8856986652864055911L;

	private final String userName;

	public UserEvent(String type, long timestamp, String userName) {
		super(type, timestamp);
		this.userName = userName;
	}
	
	/**
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}
}

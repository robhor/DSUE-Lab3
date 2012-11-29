package analytics.event;


/**
 * An event that concerns a user and their session.
 */
public class UserEvent extends Event {
	private static final long serialVersionUID = -8856986652864055911L;

	private final String userName;

	/**
	 * Constructor.
	 * @param type Event type string
	 * @param timestamp Time of event occurence
	 * @param userName Name of the user
	 */
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
	
	/**
	 * Produces a string representation of the event.
	 * Used for printing in ManagementClient.
	 * @return The event string
	 */
	public String toString() {
		if ("USER_LOGIN".equals(getType())) {
			return String.format("%s - user %s logged in", super.toString(), getUserName());	
		} else
		if ("USER_LOGOUT".equals(getType())) {
			return String.format("%s - user %s logged out", super.toString(), getUserName());	
		}
		else {
			return super.toString(); // fallback for invalid type
		}
	}
}

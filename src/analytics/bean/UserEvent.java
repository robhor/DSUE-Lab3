package analytics.bean;

public class UserEvent extends Event {
	private static final long serialVersionUID = -8856986652864055911L;

	private String userName;

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
}

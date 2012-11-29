package mgmtclient;

/**
 * This exception is thrown when an error occurs during
 * a management client operation.
 */
public class ManagementException extends Exception {
	private static final long serialVersionUID = 5954825082004361989L;

	/**
	 * Constructor.
	 * 
	 * @param message
	 *            Error message
	 */
	public ManagementException(String message) {
		super(message);
	}

	/**
	 * Constructor with cause.
	 * 
	 * @param message
	 *            Error message
	 * @param throwable
	 *            Error cause
	 */
	public ManagementException(String message, Throwable throwable) {
		super(message, throwable);
	}
}

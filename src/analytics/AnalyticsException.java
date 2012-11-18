package analytics;

/**
 * This exception is thrown when an error occurs during
 * analytics.
 */
public class AnalyticsException extends Exception {
	private static final long serialVersionUID = -2279412025840320442L;

	/**
	 * Constructor.
	 * 
	 * @param message
	 *            Error message
	 */
	public AnalyticsException(String message) {
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
	public AnalyticsException(String message, Throwable throwable) {
		super(message, throwable);
	}
}

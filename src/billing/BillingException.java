package billing;

public class BillingException extends Exception {
	private static final long serialVersionUID = -1328232757319203359L;

	public BillingException() {
		super();
	}

	public BillingException(String message) {
		super(message);
	}

	public BillingException(Throwable cause) {
		super(cause);
	}

	public BillingException(String message, Throwable cause) {
		super(message, cause);
	}
}

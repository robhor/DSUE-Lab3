package loadtest;

public class LoadTestException extends Exception {
	private static final long serialVersionUID = -6546506451159972336L;

	public LoadTestException() {
		super();
	}

	public LoadTestException(String message) {
		super(message);
	}

	public LoadTestException(Throwable cause) {
		super(cause);
	}

	public LoadTestException(String message, Throwable cause) {
		super(message, cause);
	}
}

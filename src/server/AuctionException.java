package server;

public class AuctionException extends Exception {
	private static final long serialVersionUID = -2125426902611518968L;

	public AuctionException() {
		super();
	}

	public AuctionException(String message) {
		super(message);
	}

	public AuctionException(Throwable cause) {
		super(cause);
	}

	public AuctionException(String message, Throwable cause) {
		super(message, cause);
	}
}

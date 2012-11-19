package analytics.event;


/**
 * An event that is produced from the accumulated information from
 * events from the auction server.
 */
public class StatisticsEvent extends Event {
	private static final long serialVersionUID = 7321892301916647329L;

	private final double value;

	/**
	 * Constructor.
	 * @param type Event type string
	 * @param timestamp Time of event occurence
	 * @param value Value of the computed statistic
	 */
	public StatisticsEvent(String type, long timestamp, double value) {
		super(type, timestamp);
		this.value = value;
	}
	
	/**
	 * @return the value
	 */
	public double getValue() {
		return value;
	}
	
	/**
	 * Produces a string representation of the event.
	 * Used for printing in ManagementClient.
	 * @return The event string
	 */
	public String toString() {
		if ("USER_SESSIONTIME_MIN".equals(getType())) {
			return String.format("%s - minimum session time is %s seconds", super.toString(), getValue() / 60000);	
		} else
		if ("USER_SESSIONTIME_MAX".equals(getType())) {
			return String.format("%s - maximum session time is %s seconds", super.toString(), getValue() / 60000);	
		} else
		if ("USER_SESSIONTIME_AVG".equals(getType())) {
			return String.format("%s - average session time is %s seconds", super.toString(), getValue() / 60000);	
		} else
		if ("BID_PRICE_MAX".equals(getType())) {
			return String.format("%s - maximum bid price seen so far is %s", super.toString(), getValue());	
		} else
		if ("BID_COUNT_PER_MINUTE".equals(getType())) {
			return String.format("%s - current bids per minute is %s", super.toString(), getValue());	
		} else
		if ("AUCTION_TIME_AVG".equals(getType())) {
			return String.format("%s - average auction time is %s seconds", super.toString(), getValue() / 60000);	
		} else
		if ("AUCTION_SUCCESS_RATIO".equals(getType())) {
			return String.format("%s - auction success ratio is %s", super.toString(), getValue());	
		}
		else {
			return super.toString(); // fallback for invalid type
		}
	}
}

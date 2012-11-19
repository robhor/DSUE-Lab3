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
	 * Used for logging.
	 * @return The event string
	 */
	public String toString() {
		return String.format("%s value=%s", super.toString(), getValue());
	}
}

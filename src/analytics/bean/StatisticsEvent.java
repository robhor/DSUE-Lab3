package analytics.bean;

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
}

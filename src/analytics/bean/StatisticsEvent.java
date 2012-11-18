package analytics.bean;

public class StatisticsEvent extends Event {
	private static final long serialVersionUID = 7321892301916647329L;

	private final double value;

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

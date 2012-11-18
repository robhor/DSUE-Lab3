package analytics.bean;

public class StatisticsEvent extends Event {
	private static final long serialVersionUID = 7321892301916647329L;

	private double value;

	/**
	 * @return the value
	 */
	public double getValue() {
		return value;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(double value) {
		this.value = value;
	}
}

package analytics.bean;

import java.util.ArrayList;
import java.util.List;

/**
 * Maintains a list of session times.
 */
public class SessionStats {
	private final List<Long> times;
	private int countSessions;
	private long minTime;
	private long maxTime;
	
	/**
	 * Constructor.
	 */
	public SessionStats() {
		times = new ArrayList<Long> ();
		countSessions = 0;
		minTime = Long.MAX_VALUE;
		maxTime = 0;
	}
	
	/**
	 * Adds a session duration value to the stats.
	 * @param time Duration of a recorded session in milliseconds
	 */
	public void add(long time) {
		times.add(time);
		countSessions++;
		
		if (time < minTime) {
			minTime = time;
		}
		
		if (time > maxTime) {
			maxTime = time;
		}
	}

	/**
	 * Get the minimum of all recorded session durations. 
	 * @return the minimum recorded session time in milliseconds
	 */
	public double min() {
		return minTime;
	}

	/**
	 * Get the maximum of all recorded session durations. 
	 * @return the maximum recorded session time in milliseconds
	 */
	public double max() {
		return maxTime;
	}

	/**
	 * Computes the average of all recorded session durations. 
	 * @return the average recorded session time in milliseconds
	 */
	public double avg() {
		double avg = 0;
		for (int i = 0; i < times.size(); i++) {
			avg += times.get(i) / (double)countSessions;
		}
		return avg;
	}
}

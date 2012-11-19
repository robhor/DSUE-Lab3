package analytics.event;

import java.io.Serializable;
import java.util.UUID;

/**
 * Any of the events that are either fired by the auction server
 * or calculated statistics by the analytics server.
 */
public class Event implements Serializable {
	private static final long serialVersionUID = 4632651216851811931L;
	
	private final String ID;
	private final String type;
	private final long timestamp;
	
	/**
	 * Constructor
	 * @param type Event type string
	 * @param timestamp Time of event occurence
	 */
	public Event(String type, long timestamp) {
		ID = UUID.randomUUID().toString();
		this.type = type;
		this.timestamp = timestamp;
	}
	
	/**
	 * @return the ID
	 */
	public String getID() {
		return ID;
	}
	
	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}
	
	/**
	 * @return the timestamp
	 */
	public long getTimestamp() {
		return timestamp;
	}
	
	/**
	 * Produces a string representation of the event.
	 * Used for logging.
	 * @return The event string
	 */
	public String toString() {
		return String.format("type=%s timestamp=%s", getType(), getTimestamp());
	}
}

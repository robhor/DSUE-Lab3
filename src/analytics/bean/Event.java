package analytics.bean;

import java.io.Serializable;
import java.util.UUID;

public class Event implements Serializable {
	private static final long serialVersionUID = 4632651216851811931L;
	
	private final String ID;
	private final String type;
	private final long timestamp;
	
	
	public Event(String type, long timestamp) {
		ID = UUID.randomUUID().toString();
		this.type = type;
		this.timestamp = timestamp;
	}
	
	/**
	 * @return the iD
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
}

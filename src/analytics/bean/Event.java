package analytics.bean;

import java.io.Serializable;

public class Event implements Serializable {
	private static final long serialVersionUID = 4632651216851811931L;
	
	private String ID;
	private String type;
	private long timestamp;
	
	/**
	 * @return the iD
	 */
	public String getID() {
		return ID;
	}
	
	/**
	 * @param iD the iD to set
	 */
	public void setID(String ID) {
		this.ID = ID;
	}
	
	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}
	
	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}
	
	/**
	 * @return the timestamp
	 */
	public long getTimestamp() {
		return timestamp;
	}
	
	/**
	 * @param timestamp the timestamp to set
	 */
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
}

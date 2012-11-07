package server.bean;

import java.util.Calendar;

public class Auction {
	private int id;
	private User owner;
	private String name;
	private Calendar endTime;
	private User highestBidder;
	private double highestBid;
	
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public User getOwner() {
		return owner;
	}
	public void setOwner(User owner) {
		this.owner = owner;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Calendar getEndTime() {
		return endTime;
	}
	public void setEndTime(Calendar endTime) {
		this.endTime = endTime;
	}
	public User getHighestBidder() {
		return highestBidder;
	}
	public void setHighestBidder(User highestBidder) {
		this.highestBidder = highestBidder;
	}
	public double getHighestBid() {
		return highestBid;
	}
	public void setHighestBid(double highestBid) {
		this.highestBid = highestBid;
	}
	
}

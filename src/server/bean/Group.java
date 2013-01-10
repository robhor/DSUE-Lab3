package server.bean;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Semaphore;

public class Group {
	private final List<User> members;
	private final List<GroupBid> groupBids;
	private final Semaphore bidBlock; // for groupBids
	
	public Group() {
		members = new ArrayList<User> ();
		groupBids = new ArrayList<GroupBid> ();
		bidBlock = new Semaphore(0, true);
	}
	
	public void addMember(User member) {
		members.add(member);
		giveBudget();
	}
	
	public void addBid(GroupBid groupBid) {
		groupBids.add(groupBid);
	}
	
	public void giveBudget() {
		bidBlock.release();
	}
	
	public void takeBudget() {
		try {
			bidBlock.acquire();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	
	public GroupBid findBid(int auctionId, double amount, String initiator) {
		for (GroupBid b : groupBids) {
			if ((b.getAuctionId() == auctionId) &&
				(b.getAmount() == amount) &&
				(b.getUser().getName().equals(initiator))) {
				
				return b;
			}
		}
		
		return null;
	}

	public void removeGroupBid(GroupBid groupBid) {
		if (!groupBids.remove(groupBid)) {
			throw new RuntimeException("Group bid not in group");
		}
	}
	
	public List<GroupBid> getGroupBids() {
		return groupBids;
	}
	
	public List<User> getMembers() {
		return members;
	}
}

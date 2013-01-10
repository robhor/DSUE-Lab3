package server.bean;

import java.util.ArrayList;
import java.util.List;

public class Group {
	List<User> members;
	List<GroupBid> groupBids;
	
	public Group() {
		members = new ArrayList<User> ();
		groupBids = new ArrayList<GroupBid> ();
	}
	
	public void addMember(User member) {
		members.add(member);
	}
	
	public void addBid(GroupBid groupBid) {
		groupBids.add(groupBid);
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

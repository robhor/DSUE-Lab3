package server.bean;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import server.AuctionException;
import server.BidExecuter;

public class GroupBid {
	private final long CONFIRM_TIMEOUT = 20;
	
	private final int auctionId;
	private final User user;
	private final double amount;
	private CyclicBarrier confirmBarrier;
	private BidExecuter bidExecuter;
	
	public GroupBid(int auctionId, User user, double amount, BidExecuter bidExecuter) {
		this.auctionId = auctionId;
		this.user = user;
		this.amount = amount;
		this.bidExecuter = bidExecuter;
		confirmBarrier = new CyclicBarrier(2, bidExecuter);
	}
	
	/**
	 * @return the auctionId
	 */
	public int getAuctionId() {
		return auctionId;
	}
	
	/**
	 * @return the user
	 */
	public User getUser() {
		return user;
	}
	
	/**
	 * @return the amount
	 */
	public double getAmount() {
		return amount;
	}

	/**
	 * Confirms the GroupBid on account of the given user.
	 * This method blocks until enough confirmations have been contributed or the time runs out.
	 * @param user the user who confirms the bid
	 * @return true if the confirmation completed before timeout, false otherwise
	 * @throws AuctionException if the group bid needs no more confirmations
	 */
	public boolean confirm(User user) throws AuctionException {
		try {
			int i = confirmBarrier.await(CONFIRM_TIMEOUT, TimeUnit.SECONDS);
			return true;
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (BrokenBarrierException e) {
			throw new AuctionException("This group bid has already been confirmed.", e);
		} catch (TimeoutException e) {
			return false;
		}
	}

	/**
	 * @return the bidExecuter
	 */
	public BidExecuter getBidExecuter() {
		return bidExecuter;
	}
	
	public int getConfirmsRemaining() {
		return confirmBarrier.getParties() - confirmBarrier.getNumberWaiting();
	}
}

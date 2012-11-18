package analytics.impl;

import java.rmi.RemoteException;

import analytics.AnalyticsException;
import analytics.AnalyticsServer;
import analytics.Subscriber;
import analytics.bean.AuctionEvent;
import analytics.bean.Auctions;
import analytics.bean.BidEvent;
import analytics.bean.Event;
import analytics.bean.Sessions;
import analytics.bean.SessionStats;
import analytics.bean.AuctionStats;
import analytics.bean.BidStats;
import analytics.bean.UserEvent;

public class AnalyticsServerImpl implements AnalyticsServer {
	private Auctions auctions;
	private Sessions sessions;
	private AuctionStats auctionStats;
	private SessionStats sessionStats;
	private BidStats bidStats;
	
	public AnalyticsServerImpl() {
		auctions = new Auctions();
		sessions = new Sessions();
		auctionStats = new AuctionStats();
		sessionStats = new SessionStats();
		bidStats = new BidStats();
	}

	@Override
	public String subscribe(String filter, Subscriber subscriber)
			throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void processEvent(Event event) throws RemoteException, AnalyticsException {
		if ("AUCTION_STARTED".equals(event.getType())) {
			processAuctionStarted((AuctionEvent)event);
		} else
		if ("AUCTION_ENDED".equals(event.getType())) {
			processAuctionEnded((AuctionEvent)event);
		} else
		if ("USER_LOGIN".equals(event.getType())) {
			processUserLogin((UserEvent)event);
		} else
		if ("USER_LOGOUT".equals(event.getType())) {
			processUserLogout((UserEvent)event);
		} else
		if ("USER_DISCONNECTED".equals(event.getType())) {
			processUserLogout((UserEvent)event);
		} else
		if ("BID_PLACED".equals(event.getType())) {
			processBidPlaced((BidEvent)event);
		} else
		if ("BID_OVERBID".equals(event.getType())) {
			processBidOverbid((BidEvent)event);
		} else
		if ("BID_WON".equals(event.getType())) {
			processBidWon((BidEvent)event);
		} else {
			throw new AnalyticsException("Unknown type of event.");
		}
	}

	@Override
	public void unsubscribe(String identifier) throws RemoteException {
		// TODO Auto-generated method stub
		
	}

	private void processAuctionStarted(AuctionEvent event) {
		// TODO Auto-generated method stub
		
	}

	private void processAuctionEnded(AuctionEvent event) {
		// TODO Auto-generated method stub
		
	}

	private void processUserLogin(UserEvent event) {
		// TODO Auto-generated method stub
		
	}

	private void processUserLogout(UserEvent event) {
		// TODO Auto-generated method stub
		
	}

	private void processBidPlaced(BidEvent event) {
		// TODO Auto-generated method stub
		
	}

	private void processBidOverbid(BidEvent event) {
		// TODO Auto-generated method stub
		
	}

	private void processBidWon(BidEvent event) {
		// TODO Auto-generated method stub
		
	}
}

package analytics.impl;

import java.rmi.RemoteException;

import analytics.AnalyticsException;
import analytics.AnalyticsServer;
import analytics.Subscriber;
import analytics.bean.Auction;
import analytics.bean.AuctionEvent;
import analytics.bean.Auctions;
import analytics.bean.BidEvent;
import analytics.bean.Event;
import analytics.bean.Session;
import analytics.bean.Sessions;
import analytics.bean.SessionStats;
import analytics.bean.AuctionStats;
import analytics.bean.BidStats;
import analytics.bean.StatisticsEvent;
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
	
	/**
	 * Sends an event to all subscribers.
	 * @param event The event
	 */
	private void publish(Event event) {
		
	}

	private void processAuctionStarted(AuctionEvent event) {
		Auction auction = new Auction(event.getAuctionID(), event.getTimestamp());
		
		synchronized(auctions) {
			auctions.add(auction);
		}
	}

	private void processAuctionEnded(AuctionEvent event) {
		Auction auction;
		
		synchronized(auctions) {
			auction = auctions.remove(event.getAuctionID());
		}
		
		long auctionTime = event.getTimestamp() - auction.getStartTime();
		double timeAvg;
		double successRatio;
		
		synchronized(auctionStats) {
			auctionStats.add(auctionTime, auction.isSuccess());
			timeAvg = auctionStats.timeAvg();
			successRatio = auctionStats.successRatio();
		}

		StatisticsEvent avgEvent = new StatisticsEvent("AUCTION_TIME_AVG", event.getTimestamp(), timeAvg);
		StatisticsEvent ratioEvent = new StatisticsEvent("AUCTION_SUCCESS_RATIO", event.getTimestamp(), successRatio);
		publish(avgEvent);
		publish(ratioEvent);
	}

	private void processUserLogin(UserEvent event) {
		Session session = new Session(event.getUserName(), event.getTimestamp());
		sessions.add(session); // thread-safe
	}

	private void processUserLogout(UserEvent event) {
		Session session = sessions.remove(event.getUserName()); // thread-safe
		long time = event.getTimestamp() - session.getStartTime();
		double prevMin;
		double prevMax;
		double timeAvg;
		
		synchronized(sessionStats) {
			prevMin = sessionStats.min();
			prevMax = sessionStats.max();
			sessionStats.add(time);
			timeAvg = sessionStats.avg();
		}

		if (time < prevMin) {
			StatisticsEvent minEvent = new StatisticsEvent("USER_SESSIONTIME_MIN", event.getTimestamp(), time);
			publish(minEvent);
		}
		
		if (time > prevMax) {
			StatisticsEvent maxEvent = new StatisticsEvent("USER_SESSIONTIME_MAX", event.getTimestamp(), time);
			publish(maxEvent);
		}
		
		StatisticsEvent avgEvent = new StatisticsEvent("USER_SESSIONTIME_AVG", event.getTimestamp(), timeAvg);
		publish(avgEvent);
	}

	private void processBidPlaced(BidEvent event) {
		synchronized(auctions) {
			auctions.setSuccessful(event.getAuctionID());
		}
		
		processBid(event.getPrice());
	}

	private void processBidOverbid(BidEvent event) {
		processBid(event.getPrice());
	}
	
	private void processBid(double price) {
		double prevMax;
		double bidsPerMin;
		
		synchronized(bidStats) {
			prevMax = bidStats.max();
			bidStats.add(price);
			bidsPerMin = bidStats.bidsPerMin();
		}
		
		if (price > prevMax) {
			StatisticsEvent maxEvent = new StatisticsEvent("BID_PRICE_MAX", event.getTimestamp(), price);
			publish(maxEvent);
		}

		StatisticsEvent bpmEvent = new StatisticsEvent("BID_COUNT_PER_MINUTE", event.getTimestamp(), bidsPerMin);
		publish(bpmEvent);
	}

	private void processBidWon(BidEvent event) {
		// do nothing with this type of event
	}
}

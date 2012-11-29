package analytics.impl;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Properties;
//import java.util.logging.Logger;

import util.PropertyReader;
import analytics.AnalyticsException;
import analytics.AnalyticsServer;
import analytics.Subscriber;
import analytics.bean.Auction;
import analytics.bean.AuctionStats;
import analytics.bean.Auctions;
import analytics.bean.BidStats;
import analytics.bean.Session;
import analytics.bean.SessionStats;
import analytics.bean.Sessions;
import analytics.event.AuctionEvent;
import analytics.event.BidEvent;
import analytics.event.Event;
import analytics.event.StatisticsEvent;
import analytics.event.UserEvent;

/**
 * This is the main business logic class for the analytics server.
 * It handles (un-)subscriptions from clients and events from
 * the auction server in a thread-safe manner.
 * 
 * Synchronization is grouped into the objects which are affected
 * by the event to be processed:
 * ------------------------------------------------------------
 * | Event type        | Synchronizes on
 * ------------------------------------------------------------
 * | AUCTION_STARTED   | auctions 
 * | AUCTION_ENDED     | auctions, auctionStats
 * | USER_LOGIN        | sessions
 * | USER_LOGOUT       | sessions, sessionStats
 * | USER_DISCONNECTED | sessions, sessionStats
 * | BID_PLACED        | auctions, bidStats
 * | BID_OVERBID       | bidStats
 * | BID_WON           | bidStats
 * ------------------------------------------------------------
 */
public class AnalyticsServerImpl extends UnicastRemoteObject implements AnalyticsServer {
	private static final long serialVersionUID = 6388407827917970751L;
	
	// private static final Logger logger = Logger.getLogger("AnalyticsServerImpl");
	
	private Subscribers subscribers;
	private Auctions auctions;
	private Sessions sessions;
	private AuctionStats auctionStats;
	private SessionStats sessionStats;
	private BidStats bidStats;
	
	/**
	 * Constructor.
	 * @throws RemoteException if the parent constructor failed
	 */
	public AnalyticsServerImpl() throws RemoteException {
		super();
		
		subscribers = new Subscribers();
		auctions = new Auctions();
		sessions = new Sessions();
		auctionStats = new AuctionStats();
		sessionStats = new SessionStats();
		bidStats = new BidStats();
	}

	/**
	 * Adds a subscriber to the list of subscribers.
	 * @param filter Regular expression filter for events
	 * @param subscriber Subscriber
	 * @return A unique subscription identifier string to be used for unsubscribing
	 * @throws AnalyticsException if the filter pattern is invalid
	 */
	@Override
	public String subscribe(String filter, Subscriber subscriber) throws AnalyticsException {
		Subscriber filteringSubscriber = new FilteringSubscriber(filter, subscriber);
		return subscribers.add(filteringSubscriber).toString();
	}

	/**
	 * Processes an event. Even though this method is thread-safe,
	 * some events which depend on each other must not be called
	 * out of sequence! A dependent event can only be processed
	 * after the processEvent() call for its dependency has
	 * returned.
	 * This affects:
	 * AUCTION_STARTED > [BID_PLACED] > AUCTION_ENDED
	 * BID_PLACED > [BID_OVERBID] > BID_WON
	 * USER_LOGIN > (USER_LOGOUT | USER_DISCONNECTED)
	 * 
	 * @param event The event
	 * @throws AnalyticsException if the passed event contains invalid input
	 */
	@Override
	public void processEvent(Event event) throws AnalyticsException {
		// logger.info(String.format("processEvent(%s)", event));
		
		publish(event); // forward
		
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

	/**
	 * Removes a subscription.
	 * @param identifier The unique subscription identifier previously
	 *                    returned from subscribe() 
	 * @throws AnalyticsException if the identifier could not be found among the subscriptions
	 */
	@Override
	public void unsubscribe(String identifier) throws AnalyticsException {
		subscribers.remove(identifier);
	}
	
	/**
	 * Sends an event to all subscribers.
	 * @param event The event
	 */
	private void publish(Event event) {
		// this info is a large performance hit
		// logger.info(String.format("publish(%s)", event));
		subscribers.publish(event);
	}

	/**
	 * Processes an AUCTION_STARTED event.
	 * @param event The event
	 */
	private void processAuctionStarted(AuctionEvent event) {
		Auction auction = new Auction(event.getAuctionID(), event.getTimestamp());
		
		synchronized(auctions) {
			auctions.add(auction);
		}
	}

	/**
	 * Processes an AUCTION_ENDED event.
	 * @param event The event
	 * @throws AnalyticsException if the auctionID in the event is invalid
	 */
	private void processAuctionEnded(AuctionEvent event) throws AnalyticsException {
		long auctionID = event.getAuctionID();
		Auction auction;
		
		synchronized(auctions) {
			auction = auctions.remove(auctionID);
		}
		
		if (null == auction) {
			throw new AnalyticsException(String.format("Failed to end auction with invalid ID: \"%s\"", auctionID));
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

	/**
	 * Processes a USER_LOGIN event.
	 * @param event The event
	 * @throws AnalyticsException if the user name is already associated with a running session
	 */
	private void processUserLogin(UserEvent event) throws AnalyticsException {
		Session session = new Session(event.getUserName(), event.getTimestamp());
		sessions.add(session); // thread-safe
	}

	/**
	 * Processes a USER_LOGOUT event.
	 * @param event The event
	 * @throws AnalyticsException if the user name could not be found among the running sessions
	 */
	private void processUserLogout(UserEvent event) throws AnalyticsException {
		String userName = event.getUserName();
		Session session = sessions.remove(userName); // thread-safe
		
		if (null == session) {
			throw new AnalyticsException(String.format("Failed to log out user \"%s\" who has no session.", userName));
		}
		
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

	/**
	 * Processes a BID_PLACED event.
	 * @param event The event
	 * @throws AnalyticsException if the auction ID is invalid
	 */
	private void processBidPlaced(BidEvent event) throws AnalyticsException {
		synchronized(auctions) {
			auctions.setSuccessful(event.getAuctionID());
		}
		
		processBid(event.getPrice(), event.getTimestamp());
	}

	/**
	 * Processes a BID_OVERBID event.
	 * @param event The event
	 */
	private void processBidOverbid(BidEvent event) {
		processBid(event.getPrice(), event.getTimestamp());
	}

	/**
	 * Processes a bid-related event which affects the bid stats.
	 * @param price Bidding amount
	 * @param timestamp Time of event occurence
	 */
	private void processBid(double price, long timestamp) {
		double prevMax;
		double bidsPerMin;
		
		synchronized(bidStats) {
			prevMax = bidStats.max();
			bidStats.add(price);
			bidsPerMin = bidStats.bidsPerMin();
		}
		
		if (price > prevMax) {
			StatisticsEvent maxEvent = new StatisticsEvent("BID_PRICE_MAX", timestamp, price);
			publish(maxEvent);
		}

		StatisticsEvent bpmEvent = new StatisticsEvent("BID_COUNT_PER_MINUTE", timestamp, bidsPerMin);
		publish(bpmEvent);
	}

	/**
	 * Processes a BID_WON event.
	 * @param event The event
	 */
	private void processBidWon(BidEvent event) {
		// do nothing with this type of event
	}
	
	private void printAllStats() {
		System.out.format("Auction success ratio:\t%s\n", auctionStats.successRatio());
		System.out.format("Auction average duration:\t%s\n", auctionStats.timeAvg());
		System.out.format("Session min time:\t%s\n", sessionStats.min());
		System.out.format("Session max time:\t%s\n", sessionStats.max());
		System.out.format("Bids per minute:\t%s\n", bidStats.bidsPerMin());
		System.out.format("Higest recorded bid:\t%s\n", bidStats.max());
	}
	
	private void shutdown() {
		subscribers.shutdown();
	}
	
	/**
	 * Main method.
	 * @param args The program argument (binding name)
	 * @throws IOException if there was an I/O error
	 */
	public static void main(String[] args) throws IOException {
		// arguments: bindingName
		if (args.length != 1) {
			System.out.println("USAGE: java ..AnalyticsServerImpl bindingName");
			return;
		}
		String bindingName = args[0];
		
		// registry port
		Properties props = PropertyReader.readProperties("registry.properties");
		String host  = props.getProperty("registry.host");
		int port  = Integer.valueOf(props.getProperty("registry.port"));
		
		Registry registry;
		try {
			registry = LocateRegistry.createRegistry(port);
		} catch (RemoteException e) {
			registry = LocateRegistry.getRegistry(host, port);
		}
		
		AnalyticsServerImpl analytics = new AnalyticsServerImpl();
		registry.rebind(bindingName, analytics);
		
		System.out.println("Analytics Server ready.");
		
		// shut down when pressing enter
		System.in.read();
		analytics.printAllStats();
		
		System.out.println("Shutting down...");
		analytics.shutdown();
		UnicastRemoteObject.unexportObject(analytics, true);
	}
}

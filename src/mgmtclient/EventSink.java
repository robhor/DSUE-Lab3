package mgmtclient;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import analytics.AnalyticsException;
import analytics.AnalyticsServer;
import analytics.Subscriber;
import analytics.event.Event;

/**
 * Handles all notifications from the analytics server.
 * Incoming events are printed to the console when necessary.
 */
public class EventSink implements Subscriber {
	private static final int DEDUP_BUFSIZE = 10;
	
	private AnalyticsServer analyticsServer;
	private List<Event> hideBuffer; // messages to be printed
	private Event[] dedupBuffer; // memory of events for deduplication
	private int dedupCursor; // current position in buffer
	private volatile boolean auto; // print messages as soon as they arrive

	public EventSink(AnalyticsServer analyticsServer) {
		this.analyticsServer = analyticsServer; 
		hideBuffer = new ArrayList<Event>();
		dedupBuffer = new Event[DEDUP_BUFSIZE];
		dedupCursor = 0;
		auto = false;
	}
	
	public String subscribe(String filter) throws RemoteException {
		return analyticsServer.subscribe(filter, this);
	}
	
	public void unsubscribe(String identifier) throws RemoteException, AnalyticsException {
		analyticsServer.unsubscribe(identifier);
	}
	
	public void auto() {
		auto = true;
		print();
	}
	
	public void hide() {
		auto = false;
	}
	
	public void print() {
		synchronized(hideBuffer) {
			for (int i = 0; i < hideBuffer.size(); i++) {
				Event event = hideBuffer.get(i);
				System.out.println(event);
			}
			
			hideBuffer.clear();
		}
	}
	
	@Override
	public void processEvent(Event event) {
		// check for duplicates
		synchronized(dedupBuffer) {
			for (int i = 0; i < DEDUP_BUFSIZE; i++) {
				if ((dedupBuffer[i] != null) && (dedupBuffer[i].getID() == event.getID())) {
					return; // discard event
				}
			}
			
			dedupBuffer[dedupCursor] = event;
			dedupCursor = (dedupCursor + 1) % DEDUP_BUFSIZE;
		}
		
		if (auto) {
			System.out.println(event);
		} else {
			synchronized(hideBuffer) {
				hideBuffer.add(event);
			}
		}
	}

}

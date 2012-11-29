package mgmtclient;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import analytics.Subscriber;
import analytics.event.Event;

/**
 * Handles all notifications from the analytics server.
 * Incoming events are printed to the console when necessary.
 */
public class EventSink extends UnicastRemoteObject implements Subscriber {
	private static final long serialVersionUID = 4677349532143227976L;

	private static final Logger logger = Logger.getLogger("EventSink");
	
	private static final int DEDUP_BUFSIZE = 10;
	
	private List<Event> hideBuffer; // messages to be printed
	private Event[] dedupBuffer; // memory of events for deduplication
	private int dedupCursor; // current position in buffer
	private volatile boolean auto; // print messages as soon as they arrive

	public EventSink() throws RemoteException { 
		hideBuffer = new ArrayList<Event>();
		dedupBuffer = new Event[DEDUP_BUFSIZE];
		dedupCursor = 0;
		auto = false;
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
			for (Event recent : dedupBuffer) {
				if ((recent != null) && (recent.getID().equals(event.getID()))) {
					logger.fine(String.format("Discard duplicate event %s.", event.toString()));
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

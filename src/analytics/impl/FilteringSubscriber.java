package analytics.impl;

import java.rmi.RemoteException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import analytics.AnalyticsException;
import analytics.Subscriber;
import analytics.event.Event;

/**
 * Wrapper class for subscribers which swallows all events that do
 * not match a regex filter.
 */
public class FilteringSubscriber implements Subscriber {	
	private final Pattern pattern;
	private final Subscriber next;
	
	/**
	 * Constructor.
	 * @param filter Filter regex pattern
	 * @param next Subscriber to forward to
	 * @throws AnalyticsException if the pattern is invalid
	 */
	public FilteringSubscriber(String filter, Subscriber next) throws AnalyticsException {
		try {
			pattern = Pattern.compile(filter);
		} catch (PatternSyntaxException e) {
			throw new AnalyticsException("Invalid pattern.", e);
		}
		this.next = next;
	}

	/**
	 * Process an event.
	 * @param event The event
	 * @throws RemoteException if a remoting error occurs
	 */
	@Override
	public void processEvent(Event event) throws RemoteException {
		Matcher matcher = pattern.matcher(event.getType());
		if (matcher.matches()) {
			next.processEvent(event);
		}
	}
}

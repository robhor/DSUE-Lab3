package analytics.bean;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import analytics.AnalyticsException;

/**
 * Maintains a list of running user sessions.
 * Note: Thread-safe implementation.
 */
public class Sessions {
	private final ConcurrentMap<String, Session> sessions;
	
	/**
	 * Constructor.
	 */
	public Sessions() {
		sessions = new ConcurrentHashMap<String, Session> ();
	}
	
	/**
	 * Adds a session to the list.
	 * @param session The session
	 * @throws AnalyticsException if the user name is already associated with a running session
	 */
	public void add(Session session) throws AnalyticsException {
		String userName = session.getUserName();
		Session s = sessions.putIfAbsent(userName, session);
		
		if (s != null) {
			throw new AnalyticsException(String.format("User session for \"%s\" started twice.", userName));
		}
	}
	
	/**
	 * Removes a session from the list.
	 * @param userName Name of the user who controls the session
	 * @return the removed session or null if the user name could not be found
	 */
	public Session remove(String userName) {
		return sessions.remove(userName);
	}
}

package server.service;

import java.util.Collection;

import server.bean.Client;
import server.bean.User;

public interface UserManager {
	/**
	 * Disconnect the given user.
	 * Closing open sockets.
	 * @param user
	 */
	void disconnect(User user);
	
	/**
	 * Logging out the given user.
	 * @param user
	 */
	void logout(User user);
	
	/**
	 * Log a user in.
	 * @param username The username of the user to log in.
	 * @param client The client, from which the user tries to log in.
	 * @return The User, if the login was successful.
	 *         If the login was unsuccessful, null is returned.
	 *         (Note: A User can't be logged in at two machines at once)
	 */
	User login(String username, Client client);
	
	/**
	 * Sends a message reliably to the given User (over TCP).
	 * @param user
	 * @param message
	 */
	void sendMessage(User user, String message);
	
	/**
	 * Sends a message to the given User (over UDP).
	 * @param user
	 * @param message
	 */
	void postMessage(User user, String message);
	
	/**
	 * @return A list of all registered Users.
	 */
	Collection<User> getUsers();
}

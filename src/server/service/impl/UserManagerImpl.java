package server.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import server.bean.Client;
import server.bean.User;
import server.service.ClientManager;
import server.service.UserManager;

public class UserManagerImpl implements UserManager {
	private ClientManager clManager;
	
	private ConcurrentHashMap<String, User> users;
	private HashMap<User, ArrayList<String>> pendingNotifications;
	
	public UserManagerImpl(ClientManager clManager) {
		this.clManager = clManager;
		users = new ConcurrentHashMap<String, User>();
		pendingNotifications = new HashMap<User, ArrayList<String>>();
	}

	@Override
	public void disconnect(User user) {
		Client client = user.getClient();
		if (client != null) clManager.disconnect(client);
		user.setClient(null);
	}

	@Override
	public void logout(User user) {
		if (user != null) user.setClient(null);
	}

	@Override
	public User login(String username, Client client) {
		if (username == null) throw new IllegalArgumentException("Username can't be null!");
		if (client   == null) throw new IllegalArgumentException("Client can't be null!");
		
		// check if already registered user
		User user = users.get(username);
		if (user != null) {
			if (user.getClient() != null) return null; // Can't log in on two machines
		} else {
			user = new User();
			user.setName(username);
		}
		
		user.setClient(client);
		users.put(username, user);
		
		sendQueuedNotifications(user);
		
		return user;
	}

	@Override
	public void postMessage(User user, String message) {
		Client client = user.getClient();
		if (client != null) {
			clManager.postMessage(user.getClient(), message);
		} else {
			addNotificationToQueue(user, message);
		}
	}
	
	private void addNotificationToQueue(User user, String message) {
		synchronized (pendingNotifications) {
			ArrayList<String> notifications = pendingNotifications.get(user);
			if (notifications == null) {
				notifications = new ArrayList<String>();
				pendingNotifications.put(user, notifications);
			}
			notifications.add(message);
		}
	}

	private void sendQueuedNotifications(User user) {
		synchronized (pendingNotifications) {
			ArrayList<String> notifications = pendingNotifications.get(user);
			if (notifications == null) return;
			
			for (String msg : notifications) {
				postMessage(user, msg);
			}
			notifications.clear();
		}
	}

	@Override
	public Collection<User> getUsers() {
		return users.values();
	}

	@Override
	public void sendMessage(User user, String message) {
		if (user == null) throw new IllegalArgumentException("User can't be null!");
		
		Client c = user.getClient();
		if (c == null) return;
		clManager.sendMessage(c, message);
	}

}

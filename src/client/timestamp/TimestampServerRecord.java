package client.timestamp;

import java.io.IOException;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import server.bean.Client;
import server.service.ClientManager;
import server.service.impl.ClientManagerImpl;

public class TimestampServerRecord {
	private String user;
	private String host;
	private int port;
	
	public static TimestampServerRecord parse(String msg) {
		// format: host:ip - user
		Matcher m = Pattern.compile("(.+?):(\\d+) - (.+)").matcher(msg);
		if(!m.find()) return null;
		
		String user = m.group(3);
		String host = m.group(1);
		int port;
		try {
			port = Integer.parseInt(m.group(2));
		} catch (NumberFormatException e) { return null; }
		
		return new TimestampServerRecord(user, host, port);
	}
	
	public TimestampServerRecord(String user, String host, int port) {
		this.user = user;
		this.host = host;
		this.port = port;
	}
	
	public String stamp(int id, double amount) throws IOException {
		Socket socket = new Socket(host, port);
		
		ClientManager clm = new ClientManagerImpl();
		Client client = clm.newClient(socket);
		
		String msg = String.format("!getTimestamp %d %f", id, amount);
		clm.sendMessage(client, msg);
		
		msg = clm.receiveMessage(client);
		String[] tokens = msg.split(" ");
		if (tokens.length != 5) return null;
		
		// msg ~= !timestamp <id> <amount> <time> <signature>
		String stamp = user + ":" + tokens[3] + ":" + tokens[4];
		
		return stamp;
	}
	
	public String toString() {
		return host + ":" + port + " - " + user;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result + port;
		result = prime * result + ((user == null) ? 0 : user.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TimestampServerRecord other = (TimestampServerRecord) obj;
		if (host == null) {
			if (other.host != null)
				return false;
		} else if (!host.equals(other.host))
			return false;
		if (port != other.port)
			return false;
		if (user == null) {
			if (other.user != null)
				return false;
		} else if (!user.equals(other.user))
			return false;
		return true;
	}

}

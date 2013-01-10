package client;

public class UDPProtocol {
	public static final String OVERBID = "!new-bid";
	public static final String CONFIRMED = "!confirmed";
	public static final String REJECTED = "!rejected";
	public static final String AUCTION_END = "!auction-ended";
	
	private String user;
	
	public String handleMessage(String message) {
		String[] tokens = message.split(" ");
		String output = null;
		
		if (tokens.length == 0) return null;
		
		if (tokens[0].equals(OVERBID)) {
			output = String.format("You have been overbid on '%s'", message.substring(OVERBID.length()+1));
		} else if (tokens[0].equals(CONFIRMED)) {
			output = String.format("Your bid on '%s' has been confirmed.", message.substring(CONFIRMED.length()+1));
		} else if (tokens[0].equals(REJECTED)) {
			output = String.format("Your bid on '%s' has been rejected.", message.substring(REJECTED.length()+1));
		} else if (tokens[0].equals(AUCTION_END)) {
			if(tokens.length < 4) return null;
			String auction = "";
			for (int i = 3; i < tokens.length; i++) {
				auction += " " + tokens[i];
			}
			
			auction = auction.trim();
			String username = tokens[1];
			username = (user != null && user.equals(username)) ? "You" : username;
			
			String amount = tokens[2];
			output = String.format("The auction '%s' has ended. %s won with %s.", auction, username, amount);
		}
		
		return output;
	}
	
	public void setUser(String username) {
		this.user = username;
	}
}
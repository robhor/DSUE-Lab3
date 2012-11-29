package billing.impl;

import java.io.IOException;
import java.math.BigInteger;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import util.PropertyReader;
import billing.BillingServer;
import billing.BillingServerSecure;

public class BillingServerImpl extends UnicastRemoteObject implements BillingServer {
	private static final long serialVersionUID = 1718767001064054327L;
	private static Logger logger = Logger.getLogger("BillingServerSecureImpl");
	
	private Properties users;
	private static BillingServerSecure secure;
	
	public static void main(String[] args) {
		// arguments: bindingName
		if (args.length < 1) {
			System.out.println("USAGE: java ..BillingServerImpl bindingName");
			return;
		}
		String bindingName = args[0];
		
		// registry port
		int port = 0;
		try {
			Properties props = PropertyReader.readProperties("registry.properties");
			port = Integer.valueOf(props.getProperty("registry.port"));
		} catch (NumberFormatException e) {
			System.err.println("Bad configuration: Registry port invalid");
		}
		
		Registry registry;
		BillingServerImpl bs;
		
		try {
			bs = new BillingServerImpl();
			registry = LocateRegistry.createRegistry(port);
			registry.rebind(bindingName, bs);
			
			secure = new BillingServerSecureImpl();
			UnicastRemoteObject.exportObject(secure, port);
		} catch (RemoteException e) {
			System.err.println("Could not bind to registry!");
			return;
		}
		
		// shut down when pressing enter
		try { System.in.read();
		} catch (IOException e) {}
		
		System.out.println("Shutting down...");
		
		// Unexport myself & registry.
		try {
			UnicastRemoteObject.unexportObject(registry, true);
			UnicastRemoteObject.unexportObject(bs, true);
			UnicastRemoteObject.unexportObject(secure, true);
		} catch (RemoteException e) {
			logger.log(Level.WARNING, "Unexporting registry failed");
			e.printStackTrace();
		}
	}
	
	protected BillingServerImpl() throws RemoteException {
		super();
		users = PropertyReader.readProperties("user.properties");
	}
	
	@Override
	public BillingServerSecure login(String username, String password) throws RemoteException {
		logger.log(Level.FINE, String.format("Logging in as %s with pwd %s", username, password));
				
		if (users == null) return null;
		String pwdhash = users.getProperty(username);
		
		if (pwdhash == null) {
			logger.log(Level.INFO, "User " + username + " does not exist.");
			return null;
		}
		
		if (!pwdhash.equals(md5(password))) {
			logger.log(Level.INFO, "Wrong password for User " + username + ".");
			return null;
		}
		
		// authorized
		logger.log(Level.INFO, "Login as " + username + " successful.");
		
		return secure;
	}
	
	
	private String md5(String str) {
		byte[] hashb;
		try {
			hashb = MessageDigest.getInstance("MD5").digest(str.getBytes());
			String hash = String.format("%1$032x", new BigInteger(1, hashb));
			return hash;
		} catch (NoSuchAlgorithmException e) {
			assert false;
		}
		
		return null;
	}
}

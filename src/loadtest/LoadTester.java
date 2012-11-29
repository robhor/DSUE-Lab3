package loadtest;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Logger;

import billing.impl.BillingServerImpl;

import mgmtclient.EventSink;

import util.PropertyReader;

public class LoadTester {
	private static final Logger logger = Logger.getLogger("LoadTester");
	
	private ArrayList<TestClient> threads;
	private TestSubscriber testSubscriber;
	
	public static void main(String[] args) {
		if (args.length < 3) {
			System.err.println("USAGE: java ..LoadTester host port analyticsBindingName");
			return;
		}
		
		String host = args[0];
		Integer port = Integer.parseInt(args[1]);
		String abn = args[2];
		
		LoadTester tester;
		
		try {
			tester = new LoadTester(host, port, abn);
		} catch (LoadTestException e) {
			logger.severe("Load test run failed: " + e.getMessage());
			return;
		} catch (RemoteException e) {
			logger.severe("Load test run failed: " + e.getMessage());
			return;
		}
		
		try { System.in.read();
		} catch (IOException e) {}
		
		tester.shutdown();
	}
	
	public LoadTester(String host, int port, String analyticsBindingName) throws LoadTestException, RemoteException {
		Properties props = PropertyReader.readProperties("loadtest.properties");
		
		Integer clients = Integer.parseInt(props.getProperty("clients"));
		Integer apm = Integer.parseInt(props.getProperty("auctionsPerMin"));
		Integer auctionDur = Integer.parseInt(props.getProperty("auctionDuration"));
		Integer updateIntv = Integer.parseInt(props.getProperty("updateIntervalSec"));
		Integer bpm = Integer.parseInt(props.getProperty("bidsPerMin"));
		
		testSubscriber = new TestSubscriber(analyticsBindingName);
		testSubscriber.run();
		
		threads = new ArrayList<TestClient>();
		
		long time = new Date().getTime();
		for (int clnr = 0; clnr < clients; clnr++) {
			TestClient t = new TestClient(host, port, apm, auctionDur, updateIntv, bpm, clnr, time);
			threads.add(t);
			t.start();
		}
	}
	
	public void shutdown() {
		testSubscriber.shutdown();
		
		for (TestClient t : threads) {
			t.shutdown();
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	

}

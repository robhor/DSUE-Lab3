package loadtest;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Logger;

import util.PropertyReader;

public class LoadTester {
	private static final Logger logger = Logger.getLogger("LoadTester");
	
	private ArrayList<TestClient> threads;
	private TestSubscriber testSubscriber;
	
	public LoadTester(String host, int port, String analyticsBindingName) throws LoadTestException, RemoteException {
		Properties props = PropertyReader.readProperties("loadtest.properties");

		int clients = Integer.parseInt(props.getProperty("clients"));
		int apm = Integer.parseInt(props.getProperty("auctionsPerMin"));
		int auctionDuration = Integer.parseInt(props.getProperty("auctionDuration"));
		int updateInterval = Integer.parseInt(props.getProperty("updateIntervalSec"));
		int bpm = Integer.parseInt(props.getProperty("bidsPerMin"));
		
		testSubscriber = new TestSubscriber(analyticsBindingName);

		long time = System.currentTimeMillis();
		threads = new ArrayList<TestClient>();
		for (int clnr = 0; clnr < clients; clnr++) {
			TestClient t = new TestClient(host, port, apm, auctionDuration, updateInterval, bpm, clnr, time);
			threads.add(t);
		}
	}
	
	public void run() {
		testSubscriber.run();
		
		for(Thread t : threads) {
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
		
		System.out.println("Load tester ready.");
		tester.run();
		
		try { System.in.read();
		} catch (IOException e) {}
		
		System.out.println("Shutting down...");
		
		tester.shutdown();
	}
}

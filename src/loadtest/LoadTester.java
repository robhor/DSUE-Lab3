package loadtest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import util.PropertyReader;

public class LoadTester {
	private ArrayList<TestClient> threads;
	
	public static void main(String[] args) {
		if (args.length < 3) return;
		
		String host = args[0];
		Integer port = Integer.parseInt(args[1]);
		String abn = args[2];
		
		LoadTester tester = new LoadTester(host, port, abn);
		
		try { System.in.read();
		} catch (IOException e) {}
		
		tester.shutdown();
	}
	
	public LoadTester(String host, int port, String analyticsBindingName) {
		Properties props = PropertyReader.readProperties("loadtest.properties");
		
		Integer clients = Integer.parseInt(props.getProperty("clients"));
		Integer apm = Integer.parseInt(props.getProperty("auctionsPerMin"));
		Integer auctionDur = Integer.parseInt(props.getProperty("auctionDuration"));
		Integer updateIntv = Integer.parseInt(props.getProperty("updateIntervalSec"));
		Integer bpm = Integer.parseInt(props.getProperty("bidsPerMin"));
		
		
		// TODO log in as management client, subscribe to ".*"
		/*
		 * Moreover, the test environment should instantiate a management client
		 * with an event subscription on any event type (filter ".*").
		 * During the tests, the management client should be in "auto" mode,
		 * i.e., print all the incoming events automatically to the command line.
		 */
		
		threads = new ArrayList<TestClient>();
		
		long time = new Date().getTime();
		for (int clnr = 0; clnr < clients; clnr++) {
			TestClient t = new TestClient(host, port, apm, auctionDur, updateIntv, bpm, clnr, time);
			threads.add(t);
			t.start();
		}
	}
	
	public void shutdown() {
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

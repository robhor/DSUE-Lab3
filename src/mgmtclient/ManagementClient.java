package mgmtclient;

import billing.BillingServer;
import billing.BillingServerSecure;
import analytics.AnalyticsServer;

public class ManagementClient {
	private AnalyticsServer analyticsServer;
	private BillingServer billingServer;
	private BillingServerSecure billingServerSecure;
	private EventSink eventSink;
	
	public ManagementClient(AnalyticsServer analyticsServer, BillingServer billingServer) {
		this.analyticsServer = analyticsServer;
		this.billingServer = billingServer;
		billingServerSecure = null;
		eventSink = new EventSink(analyticsServer);
	}
	
	
	/**
	 * Main method.
	 * @param args Program arguments: analytics binding name, billing binding name
	 */
	public static void main(String[] args) {
		

	}

}

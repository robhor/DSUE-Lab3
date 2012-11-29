package mgmtclient;

import java.rmi.RemoteException;
import java.util.NoSuchElementException;
import java.util.Scanner;

import analytics.AnalyticsException;
import billing.BillingException;
import billing.BillingServer;
import billing.BillingServerSecure;
import billing.bean.Bill;
import billing.bean.PriceSteps;

/**
 * Handles user interaction for the management client.
 */
public class Prompt {
	public static final String CMD_LOGIN       = "!login";
	public static final String CMD_STEPS       = "!steps";
	public static final String CMD_ADDSTEP     = "!addStep";
	public static final String CMD_RMSTEP      = "!removeStep";
	public static final String CMD_BILL        = "!bill";
	public static final String CMD_LOGOUT      = "!logout";
	public static final String CMD_SUBSCRIBE   = "!subscribe";
	public static final String CMD_UNSUBSCRIBE = "!unsubscribe";
	public static final String CMD_AUTO        = "!auto";
	public static final String CMD_HIDE        = "!hide";
	public static final String CMD_PRINT       = "!print";
	public static final String CMD_END         = "!end";
	
	private BillingServer billingServer;
	private EventSink eventSink;
	private Subscriptions subscriptions;
	private BillingServerSecure billingServerSecure;
	private Scanner stdinScanner;
	private boolean end;
	
	public Prompt(BillingServer billingServer, EventSink eventSink, Subscriptions subscriptions) {
		this.billingServer = billingServer;
		this.eventSink = eventSink;
		this.subscriptions = subscriptions;
		billingServerSecure = null;
		stdinScanner = new Scanner(System.in);
		end = false;
	}
	
	/**
	 * Prompts the user for input repeatedly until shutdown.
	 */
	public void run() {
		while (!end) {
			System.out.print("> ");
			handleInput(stdinScanner.nextLine());
		}
	}
	
	private void handleInput(String input) {
		try {
			Scanner scanner = new Scanner(input);
			if (!scanner.hasNext()) {
				return; // ignore empty line
			}			
			String command = scanner.next();
	
			if      (CMD_LOGIN.equals(command))        handleLogin(scanner);
			else if (CMD_STEPS.equals(command))        handleSteps(scanner);
			else if (CMD_ADDSTEP.equals(command))      handleAddStep(scanner);
			else if (CMD_RMSTEP.equals(command))       handleRmStep(scanner);
			else if (CMD_BILL.equals(command))         handleBill(scanner);
			else if (CMD_LOGOUT.equals(command))       handleLogout(scanner);
			else if (CMD_SUBSCRIBE.equals(command))    handleSubscribe(scanner);
			else if (CMD_UNSUBSCRIBE.equals(command))  handleUnsubscribe(scanner);
			else if (CMD_AUTO.equals(command))         handleAuto(scanner);
			else if (CMD_HIDE.equals(command))         handleHide(scanner);
			else if (CMD_PRINT.equals(command))        handlePrint(scanner);
			else if (CMD_END.equals(command))          handleEnd(scanner);
			else {
				System.out.println("unknown command");
			}
		}
		catch (AnalyticsException e) {
			System.err.println(e.toString());
		} catch (ManagementException e) {
			System.err.println(e.getMessage());
		} catch (BillingException e) {
			System.err.println(e.getMessage());
		} catch (NoSuchElementException e) {
			System.err.println("Invalid parameter.");
		} catch (RemoteException e) {
			System.err.format("Communication with the server failed: %s\n", e.getMessage());
		}
	}
	
	private void handleLogin(Scanner scanner) throws RemoteException, ManagementException
	{
		String username = scanner.next();
		String password = scanner.next();
		if (scanner.hasNext()) {
			throw new ManagementException("Too many parameters.");
		}
		billingServerSecure = billingServer.login(username, password);
		if (null == billingServerSecure) {
			throw new ManagementException("Invalid credentials!");
		}
		System.out.println("Ok.");
	}
	
	private void handleSteps(Scanner scanner) throws RemoteException, ManagementException
	{
		if (scanner.hasNext()) {
			throw new ManagementException("Too many parameters.");
		}
		if (null == billingServerSecure) {
			throw new ManagementException("You are not logged in.");
		}
		PriceSteps priceSteps = billingServerSecure.getPriceSteps();
		System.out.print(priceSteps);
	}
	
	private void handleAddStep(Scanner scanner) throws RemoteException, ManagementException
	{
		double startPrice = scanner.nextDouble();
		double endPrice = scanner.nextDouble();
		double fixedPrice = scanner.nextDouble();
		double variablePricePercent = scanner.nextDouble();
		if (scanner.hasNext()) {
			throw new ManagementException("Too many parameters.");
		}
		if (null == billingServerSecure) {
			throw new ManagementException("You are not logged in.");
		}
		billingServerSecure.createPriceStep(startPrice, endPrice, fixedPrice, variablePricePercent);
		System.out.println("Ok.");
	}
	
	private void handleRmStep(Scanner scanner) throws RemoteException, ManagementException
	{
		double startPrice = scanner.nextDouble();
		double endPrice = scanner.nextDouble();
		if (scanner.hasNext()) {
			throw new ManagementException("Too many parameters.");
		}
		if (null == billingServerSecure) {
			throw new ManagementException("You are not logged in.");
		}
		billingServerSecure.deletePriceStep(startPrice, endPrice);
		System.out.println("Ok.");
	}
	
	private void handleBill(Scanner scanner) throws RemoteException, ManagementException, BillingException
	{
		String user = scanner.next();
		if (scanner.hasNext()) {
			throw new ManagementException("Too many parameters.");
		}
		if (null == billingServerSecure) {
			throw new ManagementException("You are not logged in.");
		}
		Bill bill = billingServerSecure.getBill(user);
		System.out.print(bill);
	}
	
	private void handleLogout(Scanner scanner) throws ManagementException
	{
		if (scanner.hasNext()) {
			throw new ManagementException("Too many parameters.");
		}
		if (null == billingServerSecure) {
			throw new ManagementException("You are not logged in.");
		}
		billingServerSecure = null;
		System.out.println("Ok.");
	}
	
	private void handleSubscribe(Scanner scanner) throws RemoteException, ManagementException, AnalyticsException
	{
		String filter = unquote(scanner.next("('[^']*')|([^\\s]*)"));
		if (scanner.hasNext()) {
			throw new ManagementException("Too many parameters.");
		}
		String identifier = subscriptions.add(filter);
		System.out.format("Created subscription with ID %s for events using filter '%s'\n", identifier, filter);
	}
	
	private void handleUnsubscribe(Scanner scanner) throws RemoteException, AnalyticsException, ManagementException
	{
		String identifier = scanner.next();
		if (scanner.hasNext()) {
			throw new ManagementException("Too many parameters.");
		}
		subscriptions.remove(identifier);
		System.out.println("Ok.");
	}
	
	private void handleAuto(Scanner scanner) throws ManagementException
	{
		if (scanner.hasNext()) {
			throw new ManagementException("Too many parameters.");
		}
		eventSink.auto();
		System.out.println("Ok.");
	}
	
	private void handleHide(Scanner scanner) throws ManagementException
	{
		if (scanner.hasNext()) {
			throw new ManagementException("Too many parameters.");
		}
		eventSink.hide();
		System.out.println("Ok.");
	}
	
	private void handlePrint(Scanner scanner) throws ManagementException
	{
		if (scanner.hasNext()) {
			throw new ManagementException("Too many parameters.");
		}
		eventSink.print();
	}
	
	private void handleEnd(Scanner scanner) throws ManagementException
	{
		if (scanner.hasNext()) {
			throw new ManagementException("Too many parameters.");
		}
		end = true;
	}

	private String unquote(String next) {
		if ((next.charAt(0) == '\'') && (next.charAt(next.length()-1) == '\'')) {
			return next.substring(1,next.length()-1);
		} else {
			return next;
		}	
	}
}

package billing;

import java.rmi.Remote;
import java.rmi.RemoteException;

import billing.bean.Bill;
import billing.bean.PriceSteps;

public interface BillingServerSecure extends Remote {
	/**
	 * @return the current configuration of price steps.
	 * @throws RemoteException
	 */
	PriceSteps getPriceSteps() throws RemoteException;
	
	/**
	 * This method allows to create a price step for a given price interval.
	 * To represent an infinite value for the endPrice parameter (e.g., in the example price step "> 1000") you can use the value 0.
	 * @param startPrice
	 * @param endPrice
	 * @param fixedPrice
	 * @param variablePricePercent
	 * @throws RemoteException if any of the specified values is negative.
	 *                         Also thrown if the provided price interval collides (overlaps) with an existing price step
	 *                         (in this case the user would have to delete the other price step first).
	 */
	void createPriceStep(double startPrice, double endPrice, double fixedPrice, double variablePricePercent) throws RemoteException;
	
	/**
	 * This method allows to delete a price step for the pricing curve.
	 * @param startPrice
	 * @param endPrice
	 * @throws RemoteException if the specified interval does not match an existing price step interval
	 */
	void deletePriceStep(double startPrice, double endPrice) throws RemoteException;
	
	/**
	 * This method is called by the auction server as soon as an auction has ended.
	 * The billing server stores the auction result (data do not have to be persisted, storing in memory is sufficient)
	 * and later uses this information to calculate the bill for a user.
	 * @param user
	 * @param auctionID
	 * @param price
	 * @throws RemoteException 
	 */
	void billAuction(String user, long auctionID, double price) throws RemoteException;
	
	/**
	 * This method calculates and returns the bill for a given user,
	 * based on the price steps stored within the billing server.
	 * The bill shows the total history of all auctions created by the user
	 * @param user
	 * @return
	 * @throws RemoteException
	 * @throws BillingException 
	 */
	Bill getBill(String user) throws RemoteException, BillingException;
}

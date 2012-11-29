package billing.impl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import billing.BillingException;
import billing.BillingServerSecure;
import billing.bean.Bill;
import billing.bean.BillLine;
import billing.bean.PriceStep;
import billing.bean.PriceSteps;

public class BillingServerSecureImpl extends UnicastRemoteObject implements BillingServerSecure {
	private static final long serialVersionUID = 1L;
	private HashMap<String, ArrayList<BillLine>> bills;
	private ArrayList<PriceStep> priceSteps;
	
	public BillingServerSecureImpl() throws RemoteException {
		bills = new HashMap<String, ArrayList<BillLine>>();
		priceSteps = new ArrayList<PriceStep>();
	}

	@Override
	public PriceSteps getPriceSteps() throws RemoteException {
		return new PriceSteps(priceSteps);
	}

	@Override
	public void createPriceStep(double startPrice, double endPrice,
			double fixedPrice, double variablePricePercent)
			throws RemoteException {
		PriceStep step;
		
		try
		{
			step = new PriceStep(startPrice, endPrice, fixedPrice, variablePricePercent);
		}
		catch(IllegalArgumentException e)
		{
			// Assignment requirements: throw RemoteException
			throw new RemoteException("Invalid parameter for price step.", e);
		}
		
		synchronized (priceSteps) {
			// check for overlap
			for (PriceStep priceStep : priceSteps) {
				if (priceStep.overlap(step)) {
					throw new RemoteException("Price steps are overlapping!\n"+priceStep+"\n"+step);
				}
			}
			
			priceSteps.add(step);			
		}
	}

	@Override
	public void deletePriceStep(double startPrice, double endPrice)
			throws RemoteException {
		synchronized (priceSteps) {
			Iterator<PriceStep> it = priceSteps.iterator();
			while (it.hasNext()) {
				PriceStep step = it.next();
				if (step.getStartPrice() == startPrice && step.getEndPrice() == endPrice) {
					it.remove();
					return;
				}
			}
		}
		
		// nothing deleted
		throw new RemoteException(String.format("Price step [%s %s] does not exist", startPrice, endPrice));
	}

	@Override
	public void billAuction(String user, long auctionID, double price)
			throws RemoteException {
		
		ArrayList<BillLine> lines;
		synchronized (bills) {
			lines = bills.get(user);
			if (lines == null) {
				lines = new ArrayList<BillLine>();
				bills.put(user, lines);
			}
		}
		
		/*
		 * "For this assignment it is sufficient that you calculate the bill when the getBill
		 * command gets executed, it doesn't matter if the pricesteps may have changed during an auction. 
		 * If no pricesteps are present, you can assume, that there are no costs for auctions."
		 * https://www.infosys.tuwien.ac.at/teaching/courses/dslab/forum/viewtopic.php?id=92
		 */
		// only store auctionID & price, don't store fee yet
		BillLine line = new BillLine(auctionID, price, 0, 0);
		synchronized (lines) {
			lines.add(line);
		}
	}

	@Override
	public Bill getBill(String user) throws RemoteException, BillingException {
		ArrayList<BillLine> lines = bills.get(user);
		if (null == lines) {
			throw new BillingException("User not found.");
		}
		return new Bill(user, lines, getPriceSteps());
	}

}

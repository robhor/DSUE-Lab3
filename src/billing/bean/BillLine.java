package billing.bean;

import java.io.Serializable;


public class BillLine implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private long auctionID;
	private double strikePrice, fixedFee, variableFee;
	
	public BillLine(long auctionID, double strikePrice, double fixedFee, double variableFee) {
		this.auctionID = auctionID;
		this.strikePrice = strikePrice;
		this.fixedFee = fixedFee;
		this.variableFee = variableFee;
	}
	
	public long getAuctionID() {
		return auctionID;
	}

	public double getStrikePrice() {
		return strikePrice;
	}

	public double getFixedFee() {
		return fixedFee;
	}

	public double getVariableFee() {
		return variableFee;
	}
	
	@Override
	public String toString() {
		return String.format("%d\t%f\t%f\t%.1f\t%.1f", 
				auctionID, strikePrice, fixedFee, variableFee, fixedFee+variableFee);
	}
}

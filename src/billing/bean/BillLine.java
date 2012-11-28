package billing.bean;


public class BillLine {
	private long auctionID;
	private double strikePrice, fixedFee, variableFee;
	
	public BillLine(long auctionID, double strikePrice, double fixedFee, double variableFee) {
		this.auctionID = auctionID;
		this.strikePrice = strikePrice;
		this.fixedFee = fixedFee;
		this.variableFee = variableFee;
	}
	
	@Override
	public String toString() {
		return String.format("%d\t%.2f\t%.2f\t%.2f\t%.2f", 
				auctionID, strikePrice, fixedFee, variableFee, fixedFee+variableFee);
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

}

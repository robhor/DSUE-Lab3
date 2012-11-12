package billing.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

public class Bill implements Serializable {
	private static final long serialVersionUID = 48003604693640046L;
	
	private String user;
	private ArrayList<BillLine> lines;
	
	public Bill(String user, ArrayList<BillLine> lines) {
		this.user = user;
		this.lines = lines;
		
		if (lines == null)
			this.lines = new ArrayList<BillLine>();
	}
	
	public Bill(String user, ArrayList<BillLine> lines, PriceSteps priceSteps) {
		this(user, null);
		
		for (BillLine line : lines) {
			double price = line.getStrikePrice();
			double fixedFee = 0;
			double variableFee = 0;
			
			for (PriceStep step : priceSteps) {
				if (step.inStep(price)) {
					fixedFee = step.getFixedPrice();
					variableFee = step.getVariablePricePercent() * price / 100.0;
					break;
				}
			}
			
			this.lines.add(new BillLine(line.getAuctionID(), price, fixedFee, variableFee));
		}
	}

	public String getUser() {
		return user;
	}
	
	public Collection<BillLine> getBillLines() {
		return new ArrayList<BillLine>(lines);
	}
	
	@Override
	public String toString() {
		String str = "auction_ID\tstrike_price\tfee_fixed\tfee_variable\tfee_total\n";
		for (BillLine line : lines) {
			str += line.toString() + "\n";
		}
		return str;
	}
}

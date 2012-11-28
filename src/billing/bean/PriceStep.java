package billing.bean;


public class PriceStep implements Comparable<PriceStep> {
	private double startPrice, endPrice, fixedPrice, variablePricePercent;
	
	public PriceStep(double startPrice, double endPrice,
			double fixedPrice, double variablePricePercent) {
		this.startPrice = startPrice;
		this.endPrice = endPrice;
		this.fixedPrice = fixedPrice;
		this.variablePricePercent = variablePricePercent;
		
		if (startPrice > endPrice && endPrice != 0)
			throw new IllegalArgumentException("Start price cannot be higher than the end price.");
		
		if (startPrice < 0 || endPrice < 0 || fixedPrice < 0 || variablePricePercent < 0)
			throw new IllegalArgumentException("Prices cannot be negative.");
	}

	@Override
	public int compareTo(PriceStep step) {
		if (startPrice < step.startPrice) return -1;
		else if (startPrice == step.startPrice) return 0;
		else return 1;
	}
	
	/**
	 * Checks if this PriceStep overlaps with another PriceStep
	 * @param step
	 * @return true, if the price steps overlap
	 */
	public boolean overlap(PriceStep step) {
		if (endPrice != 0 && step.endPrice != 0) {
			if (startPrice >= step.endPrice) return false;
			if (endPrice <= step.startPrice) return false;
			
			return true;
		} else if (endPrice == 0 && step.endPrice == 0) {
			return true;
		} else if (endPrice == 0) {
			return startPrice < step.startPrice;
		} else {
			return step.startPrice < startPrice;
		}
	}
	
	/**
	 * Checks if the given price is within this price step.
	 * @param price
	 * @return true, if price is within this price step.
	 */
	public boolean inStep(double price) {
		if (price <= startPrice) return false;
		if (endPrice == 0)       return true;
		if (price > endPrice)    return false;
		
		return true;
	}

	public double getStartPrice() {
		return startPrice;
	}

	public double getEndPrice() {
		return endPrice;
	}

	public double getFixedPrice() {
		return fixedPrice;
	}

	public double getVariablePricePercent() {
		return variablePricePercent;
	}
	
	@Override
	public String toString() {
		return String.format("%.2f\t%.2f\t%.1f\t%.1f%%", 
				startPrice, endPrice, fixedPrice, variablePricePercent);
	}

}

package billing.bean;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

public class PriceSteps implements Remote, Iterable<PriceStep>, Serializable {
	private static final long serialVersionUID = 1L;
	
	private ArrayList<PriceStep> steps;
	
	public PriceSteps(Collection<PriceStep> priceSteps) throws RemoteException {
		steps = new ArrayList<PriceStep>();
		steps.addAll(priceSteps);
		Collections.sort(steps);
	}
	
	public int size() {
		return steps.size();
	}
	
	public PriceStep get(int index) {
		return steps.get(index);
	}

	@Override
	public Iterator<PriceStep> iterator() {
		return steps.iterator();
	}
	
	public String toString() {
		String str = "Min_Price\tMax_Price\tFee_Fixed\tFee_Variable\n";
		for(PriceStep step : steps){
			str += step.toString() + "\n";
		}
		return str;
	}
}

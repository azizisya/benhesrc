package uk.ac.gla.terrier.statistics;

public class StirlingFormula {
	// return approximation of the factorial number of k
	public static double stirling(double k){
		return Math.sqrt(2*Math.PI)*Math.pow(k, k+0.5)*Math.pow(Math.E, 1/(12*k+1)-k);
	}
}

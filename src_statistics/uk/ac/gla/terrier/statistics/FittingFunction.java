package uk.ac.gla.terrier.statistics;

public class FittingFunction {
	/**
	 * Implements the following quadratic function:
	 * y(x)=-a(c-x)^2+b
	 * @param a
	 * @param b
	 * @param c
	 * @return
	 */
	public static double quadratic(double a, double b, double c, double x){
		return -Math.pow(c-x, 2)*a+b;
	}
	
	public static double linear(double a, double b, double x){
		return a*x+b;
	}
}

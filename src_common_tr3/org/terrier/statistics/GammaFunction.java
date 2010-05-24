/*
 * Created on 20 Oct 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 * Original author: Vassilis Plachouras
 */
package org.terrier.statistics;

import org.terrier.statistics.GammaFunction;

public class GammaFunction {
	
	private static final double p0 = 1.000000000190015d;  
	private static final double p1 = 76.18009172947146d;
	private static final double p2 = -86.50532032941677d;
	private static final double p3 = 24.01409824083091d;
	private static final double p4 = -1.231739572450155d;
	private static final double p5 = 1.208650973866179e-3d;
	private static final double p6 = -5.395239384953e-6d;
	
	public double compute(double x) {		
		double result = (Math.sqrt(2*Math.PI) / x) * (p0  + p1/(x+1) + p2/(x+2) + p3/(x+3) + p4/(x+4) + p5/(x+5) + p6/(x+6))*(Math.pow(x+5.5,x+0.5))*(Math.pow(Math.E, -x-5.5));
		if (Double.isNaN(result)) 
			System.out.println("found NaN");
		
		if (Double.isInfinite(result)) 
			System.out.println("found Infinite");
		
		return(result);
	}
	
	public double compute_log(double x) {		
		double result = Math.log((Math.sqrt(2*Math.PI) / x)) + Math.log(p0  + p1/(x+1) + p2/(x+2) + p3/(x+3) + p4/(x+4) + p5/(x+5) + p6/(x+6)) + (x+0.5)*Math.log(x+5.5) + (-x-5.5)*Math.log(Math.E);
		if (Double.isNaN(result)) 
			System.out.println("found NaN");
		
		if (Double.isInfinite(result)) 
			System.out.println("found Infinite");
		
		return(result);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		GammaFunction gFunction = new GammaFunction();
		for (int i=1; i<100; i++) {
			System.out.println("gFunction("+i+") = " + gFunction.compute(i));
		}

		for (int i=1; i<100; i++) {
			System.out.println(i+"! = " + gFunction.compute(i+1));
		}
		
		System.out.println("3.5! = " + gFunction.compute(4.5));
	}

}

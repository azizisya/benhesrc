/*
 * Created on 12 Feb 2007
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.statistics;

public class SigmoidFunction {
	public static double sigmoidE(double x){
		return 1d/(1+Math.pow(Math.E, -x));
	}
	
	public static double sigmoid(double factor, double power){
		return 1d/(1+Math.pow(factor, power));
	}
	
	public static double inverseSigmoid(double factor, double power){
		return 1d+1d/Math.pow(factor, power);
	}
}

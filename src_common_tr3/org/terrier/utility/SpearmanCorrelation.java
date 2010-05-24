/*
 * Created on 2004-5-14
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.terrier.utility;

/**
 * @author ben
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class SpearmanCorrelation {
	protected int[] X;
	protected int[] Y;
	protected double correlation;
	
//	public SpearmanCorrelation(int[] X, int Y[]){
//		this.X = X;
//		this.Y = Y;
//		this.computeCorrelation();
//	}
	
	public SpearmanCorrelation(double[] x, double[] y){
		this.X = BubbleSort.sort(x);
		this.Y = BubbleSort.sort(y);
		this.computeCorrelation();
	}
	
	protected void computeCorrelation(){
		int n = X.length;
		double sigma = 0;
		for (int i = 0; i < n; i++)
			sigma += Math.pow(X[i]-Y[i], 2);
		this.correlation = 1 - (6*sigma / (Math.pow(n, 3)-n));
	}
	
	public double getCorrelation(){
		return this.correlation;
	}
}


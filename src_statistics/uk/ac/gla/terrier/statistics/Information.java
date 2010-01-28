/*
 * Created on 26 Nov 2007
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.statistics;

public class Information {
	/**
	 * 
	 * @param Px P(x)
	 * @param Py P(y)
	 * @param Py_x P(y|x)
	 * @param base Log base
	 * @return
	 */
	public static double mutualInformation(double Px, double Py, double Py_x, double base){
		return Px*Py_x*Math.log(Py_x/Py)/Math.log(base);
	}
	/**
	 * 
	 * @param Py P(y)
	 * @param Py_x P(y|x)
	 * @param base
	 * @return
	 */
	public static double pairwiseMutualInformation(double Py, double Py_x, double base){
		return Math.log(Py_x/Py)/Math.log(base);
	}
}

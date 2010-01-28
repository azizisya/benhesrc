/*
 * Created on 16-Jun-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package uk.ac.gla.terrier.optimisation;

/**
 * @author Vassilis Plachouras
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class NRUtils {

	public static double fmax(double a, double b) {
		return a>b ? a : b;
	}

	public static double sign(double a, double b) {
		if (b >= 0.0d)
			return Math.abs(a);
		return -Math.abs(a);
	}

}

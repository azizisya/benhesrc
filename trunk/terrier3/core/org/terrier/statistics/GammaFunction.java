package org.terrier.statistics;

import org.terrier.utility.ApplicationSetup;
/** Base class for implementations of the Gamma function.
 * Use getGammaFunction() to obtain an instance. The exact
 * instance can be controlled by property <tt>gamma.function</tt>
 * <p><b>Properties:</b></p>
 * <ul>
 * <li><tt>gamma.function</tt> - class name of the Gamma function implementation.
 * Defaults to use WikipediaLanczosGammaFunction 
 * @since 3.0
 * @author Craig Macdonald */
public abstract class GammaFunction {

	/** Get the value of the gamma function for the specified number.
	 * @param number for which is required
	 * @return (n-1)!
	 */
	public abstract double compute(double number);
	/** Get the value of the log of gamma function for the specified number.
	 * @param number for which is required
	 * @return log(n-1)!
	 */
	public abstract double compute_log(double number);
	
	/** Obtain an instance of GammaFunction */
	public static final GammaFunction getGammaFunction()
	{
		String className = ApplicationSetup.getProperty("gamma.function", WikipediaLanczosGammaFunction.class.getName());
		try{
			Class<? extends GammaFunction> clz = Class.forName(className).asSubclass(GammaFunction.class);
			return clz.newInstance();
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	/** Compute factorial of n, for 0 &lt; n &lt 21.
	 * @param n number to compute for
	 * @return factorial of n
	 */
	public static final long factorial(long n) {
        if      (n <  0) throw new RuntimeException("Underflow error in factorial");
        else if (n > 20) throw new RuntimeException("Overflow error in factorial");
        else if (n == 0) return 1;
        else             return n * factorial(n-1);
    }
	
	/** This implementation of the Lanczos approximation of the Gamma function
	 * is described on the Wikipedia page: 
	 * <a href="http://en.wikipedia.org/wiki/Lanczos_approximation">http://en.wikipedia.org/wiki/Lanczos_approximation</a>
	 * @author Transcribed from Python by Craig Macdonald
	 * @since 3.0
	 */
	static class WikipediaLanczosGammaFunction extends GammaFunction {		
		final static int g = 7;
		final static double p[] = new double[]{ 0.99999999999980993, 676.5203681218851, -1259.1392167224028,
			771.32342877765313, -176.61502916214059, 12.507343278686905,
			-0.13857109526572012, 9.9843695780195716e-6, 1.5056327351493116e-7};
		
		
		@Override
		public double compute(double z) {
			//Reflection formula
			if (z < 0.5d)
		        return Math.PI / (Math.sin(Math.PI*z)*compute(1.0d-z));
		    else
		    {
		        z -= 1.0d;
		        double x = p[0];
		        for(int i=1;i<g+2;i++)
		        	x += p[i]/(z+(double)i);
		        double t = z + (double)g + 0.5;
		        return Math.sqrt(2*Math.PI) * Math.pow(t,(z+0.5)) * Math.exp(-t) * x;
		    }
		}

		@Override
		public double compute_log(double number) {
			return Math.log(compute(number));
		}
	}
}

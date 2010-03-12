package org.terrier.statistics;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.terrier.statistics.GammaFunction.WikipediaLanczosGammaFunction;

/** Test cases for the Gamma Function. See 
 * <a href="http://en.wikipedia.org/wiki/Particular_values_of_the_Gamma_function">
 * http://en.wikipedia.org/wiki/Particular_values_of_the_Gamma_function</a>
 * @author Craig Macdonald
 * @since 3.0
 */
public abstract class TestGammaFunction
{
	final public static class TestWikipediaLanczosGammaFunction extends TestGammaFunction
	{
		public TestWikipediaLanczosGammaFunction() {
			super(new WikipediaLanczosGammaFunction());
		}
	}
	
	GammaFunction g;
	double wikipediatest_tolerance = 1e-10d;
	double wikipediatest_tolerance_log = 1e-10d;
	double[] small_tolerance = new double[]{
		0.01d, 0.01d, 0.01d, 0.01d, 0.01d, 0.01d, 0.01d, 0.01d,
		0.01d, 0.01d, 0.01d, 0.01d, 0.01d, 0.01d
	};
	double small_log_tolerance = 0.01d;
	
	protected TestGammaFunction(GammaFunction _g)
	{
		g = _g;
	}
	
	protected void test(double target, double number)
	{
		assertEquals(target, g.compute(number), wikipediatest_tolerance);
		assertEquals(Math.log(target), g.compute_log(number), wikipediatest_tolerance_log);
	}
	
	@Test public void testSmallIntegers()
	{
		for(int i=1;i<small_tolerance.length;i++)
		{
			double target = (double) GammaFunction.factorial(i-1) ;
			assertEquals("Compute mismatch for i="+ i, target, g.compute(i), small_tolerance[i-1]);
			assertEquals("Compute_log mismatch for i="+ i, Math.log(target), g.compute_log(i), small_log_tolerance);
		}
	}		
	
	@Test public void testWikipediaParticularValues()
	{
		//small integers
		test(1.0d, 1);
		test(1.0d, 2);
		test(2.0d, 3);
		test(6.0d, 4);
		test(24.0d, 5);
		//known half-integers
		test( 1.7724538509055160273d, 0.5d);
		test( 0.8862269254527580137d, 3d/2d);
		test( 1.3293403881791370205d, 5d/2d);
		test( 3.3233509704478425512d, 7d/2d);
		
		//other fractions
		test( 2.6789385347077476337d, 1d/3d);
		test( 3.6256099082219083119d, 1d/4d);
		test( 4.5908437119988030532, 1d/5d);
		test( 5.5663160017802352043, 1d/6d);
		test( 6.5480629402478244377, 1d/7d);
	}
	
	@Test public void testWikipediaNegativeValues()
	{
		//known negative half-integers
		test( -3.5449077018110320546d, -0.5d);
		test( 2.3632718012073547031d, -3d/2d);
		test( -0.9453087204829418812d, -5d/2d);
	}
}



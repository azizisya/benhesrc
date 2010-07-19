package uk.ac.gla.terrier.statistics;

public class WikipediaGammaFunction {
	final static int g = 7;
	final static double p[] = new double[]{ 0.99999999999980993, 676.5203681218851, -1259.1392167224028,
		771.32342877765313, -176.61502916214059, 12.507343278686905,
		-0.13857109526572012, 9.9843695780195716e-6, 1.5056327351493116e-7};
	
	public WikipediaGammaFunction(){
		
	}
	
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

	public double compute_log(double number) {
		return Math.log(compute(number));
	}
}

package org.terrier.matching.models.proximity;

import uk.ac.gla.terrier.statistics.StirlingFormula;
import org.terrier.utility.ApplicationSetup;

public class SurvHPP extends DistModel {
	protected double lambda = Double.parseDouble(ApplicationSetup.getProperty("proximity.lambda", "1d"));
	
	protected double getProbability(int dist){
		double sum = 0d;
		for (int k=0; k<=dist; k++){
			sum+= (k==0)?(1):(Math.pow(lambda, k)/StirlingFormula.stirling(k));
			
		}
		// System.out.println("sum: "+sum+", dist="+dist);
		if (Double.isNaN(sum)||sum>=1d)
			return 0d;
		return 1-Math.pow(Math.E, -lambda)*sum;	
	}
}

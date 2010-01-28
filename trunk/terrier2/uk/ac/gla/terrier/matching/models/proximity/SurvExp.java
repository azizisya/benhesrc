package uk.ac.gla.terrier.matching.models.proximity;

import uk.ac.gla.terrier.utility.ApplicationSetup;

public class SurvExp extends DistModel {
protected double lambda = Double.parseDouble(ApplicationSetup.getProperty("proximity.lambda", "1d"));
	
	protected double getProbability(double dist){
		return Math.pow(Math.E, -lambda*dist);	
	}
}

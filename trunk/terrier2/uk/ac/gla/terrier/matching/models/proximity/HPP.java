package uk.ac.gla.terrier.matching.models.proximity;

import uk.ac.gla.terrier.utility.ApplicationSetup;

public class HPP extends DistModel{
	
	protected double lambda = Double.parseDouble(ApplicationSetup.getProperty("proximity.lambda", "1d"));
	
	protected double getProbability(int dist){
		return Math.pow(Math.E, -lambda*dist)*lambda*dist;	
	}
}

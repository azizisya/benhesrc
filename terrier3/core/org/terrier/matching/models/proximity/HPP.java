package org.terrier.matching.models.proximity;

import org.terrier.utility.ApplicationSetup;

public class HPP extends DistModel{
	
	protected double lambda = Double.parseDouble(ApplicationSetup.getProperty("proximity.lambda", "1d"));
	
	protected double getProbability(int dist){
		return Math.pow(Math.E, -lambda*dist)*lambda*dist;	
	}
}

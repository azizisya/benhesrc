package uk.ac.gla.terrier.matching.models.proximity;

public class SurvEmp extends DistModel {
	
	protected double getProbability(int dist){
		double I = (dist==0d)?(1):((double)dist/(dist+1));
		return 1-I;	
	}
}

package uk.ac.gla.terrier.matching.models.proximity;

import java.io.Serializable;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.utility.ApplicationSetup;

public abstract class ProximityModel implements Serializable{
	
	/** The logger used */
	private static Logger logger = Logger.getRootLogger();
	
	public abstract double getNGramFrequency(final int[] blocksOfTerm1, int start1, int end1, 
			final int[] blocksOfTerm2, int start2, int end2, int wSize, int docLength);
	public abstract double getNGramFrequencyOrdered(final int[] blocksOfTerm1, int start1, int end1, 
			final int[] blocksOfTerm2, int start2, int end2, int wSize, int docLength);
	
	public static ProximityModel getProximityModel(String name){
		String prefix = "uk.ac.gla.terrier.matching.models.proximity.";
		if (name.indexOf('.')<0)
			name = prefix.concat(name);
		ProximityModel model = null;
		try{
			model = (ProximityModel)Class.forName(name).newInstance();
		}catch(Exception e){
			logger.warn("Error while initializing ProximityModel "+name);
			e.printStackTrace();
		}	
		return model;
	}
	
	public static ProximityModel getDefaultProximityModel(){
		String name = ApplicationSetup.getProperty("proximity.model.name", "FD");
		return getProximityModel(name);
	}
}

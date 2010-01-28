/*
 * Created on 2005-2-22
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.tuning;

import java.io.IOException;
import java.util.Vector;

import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.tfnormalisation.TermFrequencyNormalisation;

/**
 * @author ben
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public abstract class ParameterTuning {
	public TermFrequencyNormalisation method;
	
	public final String methodPackagePrefix = "uk.ac.gla.terrier.smooth.tfnormalisation.";
	
	public final String methodNamePrefix = "Normalisation";
	
	public Index index;
	
	public CollectionStatistics collSta;
	
	public ParameterTuning(String methodName, Index index){
		collSta = index.getCollectionStatistics();
		this.index = index;
		if (methodName.lastIndexOf('.')<0){
			if (methodName.trim().length() == 1)
				methodName = methodPackagePrefix.concat(methodNamePrefix).concat(methodName);
			else
				methodName = methodPackagePrefix.concat(methodName);
		}
		try {
			method = (TermFrequencyNormalisation) Class.forName(methodName).getConstructor(
					CollectionStatistics.class).newInstance(collSta);
			method.setAverageDocumentLength(collSta.getAverageDocumentLength());
		} 
		catch(Exception e) {
			System.err.println("Exception while loading the term frequency normalisation method:\n" + e);
			e.printStackTrace();
			System.err.println("Exiting...");
			System.exit(1);
		} 
	}
	
	abstract public double tuneSampling(int numberOfSamples);
	
	abstract public double tuneRealTRECQuery();
	
	abstract public double tune(Vector vecDocLength);
	
}

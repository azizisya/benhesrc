package org.terrier.tfnormalisation;

import org.terrier.matching.models.*;
import org.terrier.structures.CollectionStatistics;
import org.terrier.utility.*;

import uk.ac.gla.terrier.statistics.Statistics;
 /** This class implements the normalisation 2.
 *   Creation date: (15/06/2003 17:21:24)
 *   @author: Gianni Amati, Ben He
 */
public class NormalisationB extends TermFrequencyNormalisation {
	
	protected Idf idf;

	/** A default constructor.*/
	public NormalisationB(CollectionStatistics _collSta) {
		super(_collSta);
		idf = new Idf(collSta.getNumberOfDocuments());
	}
    
	/**
	 * Returns the name of the model.
	 * @return the name of the model
	 */
	public final String getInfo() {
		return "NormalisationB";
	}

    
	/**
	 * This method provides the contract for implementing term frequency normalisation
	 * methods.
	 * @param termFrequency double The within document frequency.
	 * @param documentLength double The document length.
	 * @return double The normalised term frequency.
	 */
	public final double getNormalisedTermFrequency(
		double termFrequency,
		double documentLength
		){
		return termFrequency / (1 - c + c * documentLength / averageDocumentLength);
	}
    
	/**
	 * This method provides the contract for implementing term frequency normalisation
	 * methods.
	 * @param termFrequency double The within document frequency.
	 * @param documentLength double The document length.
	 * @param averageDocumentLength double The average document length in the collection.
	 * @param beta double The beta parameter.
	 * @return double The normalised term frequency.
	 */
	public final double getNormalisedTermFrequency(
		double termFrequency,
		double documentLength,
		double averageDocumentLength,
		double b
	){
		return termFrequency / (1 - b + b * documentLength / averageDocumentLength);
	}
    
	/**
	 * This method provides the contract for implementing measuring normalisation
	 * effect.
	 * @param documentLength double[] The document length of the retrieved documents.
	 * @param averageDocumentLength double The average document length in the collection.
	 * @param beta double The beta parameter.
	 * @return double The normalisation effect.
	 */
	public final double getNED(
		double[] documentLength, double averageDocumentLength, double beta,
		int definition
	){
		double[] NE = new double[documentLength.length];
		for (int i = 0; i < documentLength.length; i++){
			NE[i] = this.getNormalisedTermFrequency(1, documentLength[i], 
				averageDocumentLength, beta);
		}
		double NED = 0;
		switch(definition){
			case 1: NED = Statistics.variance(NE) / Statistics.mean(NE); 
						break;// definition 1	
			case 2: NED = Statistics.variance(Statistics.normaliseMax(NE)); 
						break;// definition 2(max)
			case 3: NED = Statistics.variance(Statistics.normaliseMaxMin(NE)); 
						break;// definition 3(maxmin)
			}
		return NED;
	}
}

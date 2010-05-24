package org.terrier.tfnormalisation;

import java.io.IOException;

import org.terrier.structures.CollectionStatistics;



/**
 * This class should be extended by the classes used
 * for term frequency normalisation.
 * Creation date: (15/06/2003 16:23:04)
 * @author: Gianni Amati, Vassilis Plachouras
 */
public abstract class TermFrequencyNormalisation {
	/** The average document length in the collection. */
    protected double averageDocumentLength;

	/** The beta parameter.*/
	protected double c;
	
	protected double TF;
	
	public CollectionStatistics collSta;
	
	public TermFrequencyNormalisation(CollectionStatistics _collSta){
		collSta = _collSta;
		/*try{
			collSta = new CollectionStatistics();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}*/
	}

    /**
    * Returns the name of the normalisation method.
    * Creation date: (19/06/2003 12:09:55)
    * @return java.lang.String
    */
    public abstract String getInfo();
    
    /**
     * Set the average document length.
     * @param averageDocumentLength double The average document length.
     */
    public void setAverageDocumentLength(double averageDocumentLength){
        this.averageDocumentLength = averageDocumentLength;
    }
    
    /**
     * Set the c used for term frequency normalisation.
     * @param c double The c value for term frequency normalisation.
     */
    public void setParameter(double c){
        this.c = c;
    }
        
	/**
	 * This method provides the contract for implementing term frequency normalisation
     * methods.
     * @param termFrequency double The within document frequency.
     * @param documentLength double The document length.
	 * @return double The normalised term frequency.
	 */
	public abstract double getNormalisedTermFrequency(
        double termFrequency,
        double documentLength
        );
	/**
	 * This method provides the contract for implementing term frequency normalisation
     * methods.
     * @param termFrequency double The within document frequency.
     * @param documentLength double The document length.
     * @param averageDocumentLength double The average document length in the collection.
     * @param c double The c parameter.
	 * @return double The normalised term frequency.
	 */
	public abstract double getNormalisedTermFrequency(
        double termFrequency,
        double documentLength,
        double averageDocumentLength,
        double c
    );
	
    public void setTF(double TF){
    	this.TF = TF;
    }
    
    /**
	 * This method provides the contract for implementing measuring normalisation
     * effect.
     * @param documentLength double[] The document length of the retrieved documents.
     * @param averageDocumentLength double The average document length in the collection.
     * @param c double The c parameter.
	 * @return double The normalisation effect.
	 */
	public abstract double getNED(
        double[] documentLength,
        double averageDocumentLength,
        double c,
        int definition
        );
    
    public double getParameter(){
    	return this.c;
    }
}


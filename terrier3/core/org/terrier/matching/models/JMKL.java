package org.terrier.matching.models;

/**
 * This class implements the Dirichlet KL language model.
 * @author Ben He
 * @version $Revision: 1.1 $
 */
public class JMKL extends WeightingModel {
	private static final long serialVersionUID = 1L;

	/** 
	 * A default constructor. This must be followed by 
	 * specifying the c value.
	 */
	public JMKL() {
		super();
		this.ACCEPT_NEGATIVE_SCORE = true;
		this.ACCEPT_UNSEEN = true;
	}
	
	/** 
	 * Constructs an instance of this class with the 
	 * specified value for the parameter c.
	 * @param c the term frequency normalisation parameter value.
	 */
	public JMKL(double c) {
		this();
		this.c = c;
	}
	
	/**
	 * Returns the name of the model.
	 * @return the name of the model
	 */
	public final String getInfo() {
		return "JMc" + c;
	}
	/**
	* This method provides the contract for implementing weighting models.
	* @param tf The term frequency in the document
	* @param docLength the document's length
	* @return the score assigned to a document with the given tf and 
	*         docLength, and other preset parameters
	* 
	*/
	@Override
	public final double score(double tf, double docLength) {
		return keyFrequency * Idf.log( (1-c)*tf/docLength+c*termFrequency/numberOfTokens );
	}
	
	/**
	*This method provides the contract for implementing weighting models.
	* @param tf The term frequency in the document
	* @param docLength the document's length
	* @param documentFrequency The document frequency of the term
	* @param termFrequency the term frequency in the collection
	* @param keyFrequency the term frequency in the query
	* @return the score returned by the implemented weighting model.
	*/
	@Override
	public final double score(
		double tf,
		double docLength,
		double documentFrequency,
		double termFrequency,
		double keyFrequency) {
		return keyFrequency * Idf.log( (1-c)*tf/docLength+c*termFrequency/numberOfTokens );
	}
	@Override
	public final double scoreUnseen(double docLength) {
		return keyFrequency * Idf.log(c*termFrequency/numberOfTokens );
	}
	
	/* (non-Javadoc)
	 * @see org.terrier.matching.models.WeightingModel#score(double, double, double, double, double)
	 */
	@Override
	public final double scoreUnseen(double docLength, double nT, double F_t, double keyFrequency){
		return keyFrequency * Idf.log(c*termFrequency/numberOfTokens );
	}
}

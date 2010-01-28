/*
 * Contributor(s):
 *   Craig Macdonald
 */
package uk.ac.gla.terrier.matching.models.normalisation;

/**
 * This class implements a Normalisation method that forces all
 * term frequencies to the value of the parameter. If field retrieval
 * is enabled, then the parameter is multiplied by the field weight.
 * @author Craig Macdonald
 * @version $Revision: 1.1 $
 */
public class NormalisationStatic extends Normalisation{
	/** The name of the normalisation method .*/
	protected final String methodName = "Static";

	/**
	 * Get the name of the normalisation method.
	 * @return Return the name of the normalisation method.
	 */
	public String getInfo(){
		String info = this.methodName+"_"+parameter;
		if (this.isFieldRetrieval)
			info = info + "_w"+this.fieldWeight;
		return info;
	}

	/**
	 * Returns a static term frequency. 
	 * i.e. tf = (tf &gt; 0) ? parameter : 0
	 * @param tf The frequency of the query term in the document.
	 * @param docLength The number of tokens in the document.
	 * @param termFrequency The frequency of the query term in the collection.
	 * @return The normalised term frequency.
	 */
	public double normalise(double tf, double docLength, double termFrequency){
		if (docLength == 0)
			return tf;
		if (tf == 0)
			return 0;
		if (!this.isFieldRetrieval)
				return parameter;
		return this.fieldWeight * parameter;
	}
}

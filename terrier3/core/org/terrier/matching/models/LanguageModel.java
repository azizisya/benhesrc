/**
 * 
 */
package org.terrier.matching.models;

/**
 * @author ben
 *
 */
public abstract class LanguageModel extends WeightingModel {

	/**
	 * 
	 */
	public LanguageModel() {
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.terrier.matching.models.WeightingModel#getInfo()
	 */
	abstract public String getInfo();

	/* (non-Javadoc)
	 * @see org.terrier.matching.models.WeightingModel#score(double, double)
	 */
	@Override
	abstract public double score(double tf, double docLength);

	/* (non-Javadoc)
	 * @see org.terrier.matching.models.WeightingModel#score(double, double, double, double, double)
	 */
	abstract public double score(double tf, double docLength, double nT, double F_t,
			double keyFrequency);
	
	abstract public double scoreUnseen(double docLength);

	/* (non-Javadoc)
	 * @see org.terrier.matching.models.WeightingModel#score(double, double, double, double, double)
	 */
	abstract public double scoreUnseen(double docLength, double nT, double F_t, double keyFrequency);

}

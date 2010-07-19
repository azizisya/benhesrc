/**
 * 
 */
package org.terrier.matching.models;

/**
 * @author ben
 *
 */
public abstract class DFRModel extends WeightingModel {

	/**
	 * 
	 */
	public DFRModel() {
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.terrier.matching.models.WeightingModel#queryModel(double[])
	 */
	public void queryModel(double[] queryTermWeights) {
		double max = queryTermWeights[0];
		int n = queryTermWeights.length;
		for (int i=1; i<n; i++)
			max = Math.max(max, queryTermWeights[i]);
		for (int i=0; i<n; i++)
			queryTermWeights[i] /= max;
	}

}

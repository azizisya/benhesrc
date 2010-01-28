/*
 * Created on 10 Mar 2008
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.matching.models.sentence;

public class AvgInfo extends SentenceWeightingModel {

	@Override
	public String getInfo() {
		return "AvgInfo_"+qemodel.getInfo();
	}

	@Override
	public double score(double[] tf, double[] tfSent, double sentLength,
			double[] sentenceFrequency, double[] termFrequency,
			double[] documentFrequency, double[] keyFrequency) {
		double sum = 0d;
		for (int i=0; i<tf.length; i++)
			sum+=qemodel.score(tf[i], termFrequency[i]);
		return sum/sentLength;
	}

}

/*
 * Created on 19 Oct 2007
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.matching;

public class LanguageModellingMatching extends Matching {
	/**
	 * Assigns scores to documents for a particular term.
	 * @param scores double[] the scores of the documents for the query term.
	 * @param pointers int[][] the pointers read from the inverted file
	 *        for a particular query term.
	 */
	protected void assignScores(final double[] scores, final int[][] pointers) {
		final int[] pointers1 = pointers[0];
		final int[] pointers2 = pointers[1];
		final int numOfPointers = pointers1.length;
		//for each document that contains 
		//the query term, the score is computed.
		double score;

		//checking whether we have setup an upper threshold
        //for within document frequencies. If yes, we check 
        //whether we need to change the current term's frequency.
		if (FREQUENCY_UPPER_THRESHOLD > 0)
			for (int j = 0; j < numOfPointers; j++)
				 pointers2[j] =  pointers2[j] > FREQUENCY_UPPER_THRESHOLD ? FREQUENCY_UPPER_THRESHOLD : pointers2[j];
				 
		for (int j = 0; j < numOfPointers; j++) {
				// 
			if ((score = wmodel.score(pointers2[j], docIndex.getDocumentLength(pointers1[j]))) > 0)
				scores[j] = score;
			//else
			//	logger.debug("Wmodel gave -ve or 0 score for docid "+pointers1[j] + ", score was "+score);
		}
	}
}

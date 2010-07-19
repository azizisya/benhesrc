package uk.ac.gla.terrier.matching;

import uk.ac.gla.terrier.matching.models.WeightingModel;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntIntHashMap;
/**
 * Assign relevance score for retrieval units.
 * @author ben
 *
 */
public class UnitScorer {
	public static void score(TIntIntHashMap[] termidFreqMaps, int[] unitLength, 
			TIntIntHashMap termidTFMap, TIntIntHashMap termidNtMap, TIntDoubleHashMap termidQtwMap, 
			WeightingModel model, double[] scores
			){
		int n = scores.length;
		for (int i=0; i<n; i++)
			scores[i] = UnitScorer.score(termidFreqMaps[i], unitLength[i], termidTFMap, termidNtMap, termidQtwMap, model);
	}
	
	public static double score(TIntIntHashMap termidFreqMap,int unitLength, 
			TIntIntHashMap termidTFMap, TIntIntHashMap termidNtMap, TIntDoubleHashMap termidQtwMap, 
			WeightingModel model
			){
		int[] termids = termidQtwMap.keys();
		double score = 0d;
		for (int termid : termids){
			score += model.score(termidFreqMap.get(termid), unitLength, termidNtMap.get(termid), 
					termidTFMap.get(termid), termidQtwMap.get(termid));
		}
		return score;
	}
}

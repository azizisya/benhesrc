/**
 * 
 */
package org.terrier.matching.dsms;

import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntIntHashMap;

import java.util.Arrays;

import org.terrier.matching.MatchingQueryTerms;
import org.terrier.matching.ResultSet;
import org.terrier.matching.UnitScorer;
import org.terrier.matching.models.WeightingModel;
import org.terrier.structures.BlockIndexDocument;
import org.terrier.structures.Index;
import org.terrier.structures.Lexicon;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.TextWindow;
import org.terrier.utility.ApplicationSetup;

import uk.ac.gla.terrier.statistics.Statistics;

/**
 * @author ben
 *
 */
public class SeenPassageDSM extends PassageBasedDSM {

	/**
	 * 
	 */
	public SeenPassageDSM() {
		// TODO Auto-generated constructor stub
	}
	
	public Object clone() { return this; }
	
	public String getName() { return "SeenPassageDSM";}
	
	protected double score(TIntDoubleHashMap termidWeightMap, int[] termids, 
			TIntIntHashMap termidTFMap, TIntIntHashMap termidDFMap, 
			TextWindow[] passages, WeightingModel model){
		int[] psgLength = new int[passages.length];
		TIntIntHashMap[] termidFreqMaps = new TIntIntHashMap[passages.length];
		double[] scores = new double[passages.length];
		for (int i=0; i<passages.length; i++){
			termidFreqMaps[i] = passages[i].termidFreqMap;
			if (model.ACCEPT_UNSEEN){
				for (int termid : termids){
					if (!termidFreqMaps[i].containsKey(termid)){
						termidFreqMaps[i].put(termid, 0);
					}
				}
			}
		}
		UnitScorer.score(termidFreqMaps, psgLength, termidTFMap, termidDFMap, termidWeightMap, model, scores);
		if (model.ACCEPT_NEGATIVE_SCORE){
			for (int i=0; i<scores.length; i++)
				scores[i] = Math.pow(2, scores[i]);
		}
		double score = Statistics.sum(scores);
		int effPsgs = 0;
		// possible improvement: do not consider passages in which all query terms are unseen
		// discard passages in which all query terms are unseen
		for (int i=0; i<passages.length; i++){
			TextWindow passage = passages[i];
			boolean found = false;
			for (int termid : termids){
				if (passage.termidFreqMap.get(termid)>0){
					found = true;
					break;
				}
			}
			if (!found){
				score -= scores[i];
			}else
				effPsgs++;
		}
		psgLength = null; scores = null;
		return score / effPsgs;
	}

}

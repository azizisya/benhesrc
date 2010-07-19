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
 * Passage-based model 2 that represents a document by a sequence of 
 * passages with the highest query generation probabilities.
 * 
 * <h2>Properties</h2> 
 * 
 * <li><tt>psg.model.m</tt> - The top m passages to be considered. </li>
 * <li><tt>psg.model</tt> The name of the model used for scoring the passages. </li>
 * <li><tt>psg.model.c</tt> Parameter c of the passage model. </li>
 * <li><tt>text.window.size</tt> The size of each text window in tokens. </li>
 * 
 * @author Ben He (ben.he.09@gmail.com)
 *
 */
public class PBM2DSM extends PassageBasedDSM {

	/**
	 * 
	 */
	public PBM2DSM() {
		// TODO Auto-generated constructor stub
	}
	
	public Object clone() { return this; }
	
	public String getName() { return "PBM2DSM";}
	
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
	
	protected double scoreObselete(TIntDoubleHashMap termidWeightMap, int[] termids, 
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
			for (int i=0; i<scores.length; i++){
				scores[i] = Math.pow(2, scores[i]);
				passages[i].setWindowScore(scores[i]);
			}
			
		}
		// consider only the topX passages
		int m = Math.min(Integer.parseInt(ApplicationSetup.getProperty("psg.model.m", "0")), passages.length);

		double score = 0;
		int effPsgs = 0;
		Arrays.sort(passages);
		// possible improvement: do not consider passages in which all query terms are unseen
		// discard passages in which all query terms are unseen
		if (m==0)// consider all passages containing at least one of the query terms
			m = passages.length;
		for (int i=0; i<m; i++){
			TextWindow passage = passages[i];
			boolean found = false;
			for (int termid : termids){
				if (passage.termidFreqMap.get(termid)>0){
					found = true;
					break;
				}
			}
			if (!found){
				break;
			}else{
				effPsgs++;
				score+=passage.getWindowScore();
			}
		}
		psgLength = null; scores = null;
		return score / effPsgs;
	}

}

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
public class PassageBasedDSM implements DocumentScoreModifier {

	/**
	 * 
	 */
	public PassageBasedDSM() {
		// TODO Auto-generated constructor stub
	}
	
	public Object clone() { return this; }
	
	public String getName() { return "PassageBasedDSM";}
	
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
		// possible improvement: do not consider passages in which all query terms are unseen
		return Statistics.mean(scores);
	}
	
	public boolean modifyScores(final Index index, final MatchingQueryTerms queryTerms, final ResultSet resultSet){
		// get properties
		int wSize = Integer.parseInt(ApplicationSetup.getProperty("text.window.size", "300"));
		int topX = Math.min(Integer.parseInt(ApplicationSetup.getProperty("trec.output.format.length", "1000")),
				resultSet.getExactResultSize()
				);
		WeightingModel model = WeightingModel.getWeightingModel(ApplicationSetup.getProperty("psg.model", "DirichletKL"));
		model.setBackgroundStatistics(index.getCollectionStatistics());
		if (ApplicationSetup.getProperty("psg.model.c", null)!=null)
			model.setParameter(Double.parseDouble(ApplicationSetup.getProperty("psg.model.c", null)));
		String[] terms = queryTerms.getTerms();
		Lexicon lexicon = index.getLexicon();
		int[] termids = new int[terms.length];
		TIntDoubleHashMap termidWeightMap = new TIntDoubleHashMap();
		TIntIntHashMap termidTFMap = new TIntIntHashMap();
		TIntIntHashMap termidDFMap = new TIntIntHashMap();
		for (int i=0; i<terms.length; i++){
			LexiconEntry le = lexicon.getLexiconEntry(terms[i]); 
			termids[i] = le.getTermId();
			termidTFMap.put(termids[i], le.getFrequency());
			termidDFMap.put(termids[i], le.getDocumentFrequency());
			termidWeightMap.put(termids[i], queryTerms.getTermWeight(terms[i]));
		}
		// modify scores
		double[] scores = resultSet.getScores();
		int[] docids = resultSet.getDocids();
		Arrays.fill(scores, 0d);
		for (int i=0; i<topX; i++){
			TextWindow[] passages = BlockIndexDocument.segmentDocument(index, docids[i], wSize);
			scores[i] = this.score(termidWeightMap, termids, termidTFMap, termidDFMap, passages, model);
			passages = null;
		}
		
		// return
		return true;
	}

}

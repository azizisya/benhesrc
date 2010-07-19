/**
 * 
 */
package org.terrier.matching.dsms;

import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntIntHashMap;

import java.util.Arrays;

import org.terrier.matching.MatchingQueryTerms;
import org.terrier.matching.ResultSet;
import org.terrier.matching.models.WeightingModel;
import org.terrier.structures.BlockIndexDocument;
import org.terrier.structures.Index;
import org.terrier.structures.Lexicon;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.TextWindow;
import org.terrier.utility.ApplicationSetup;

import uk.ac.gla.terrier.statistics.ScoreNormaliser;

/**
 * @author ben
 *
 */
public class COMPBM2DSM extends PBM2DSM {

	/**
	 * 
	 */
	public COMPBM2DSM() {
		// TODO Auto-generated constructor stub
	}

	public boolean modifyScores(final Index index, final MatchingQueryTerms queryTerms, final ResultSet resultSet){
		// get properties
		int wSize = Integer.parseInt(ApplicationSetup.getProperty("text.window.size", "300"));
		int topX = Math.min(Integer.parseInt(ApplicationSetup.getProperty("trec.output.format.length", "1000")),
				resultSet.getExactResultSize()
				);
		WeightingModel model = WeightingModel.getWeightingModel(ApplicationSetup.getProperty("psg.model", "DirichletKL"));
		double alpha = Double.parseDouble(ApplicationSetup.getProperty("psg.model.alpha", "0.5d"));
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
		// Arrays.fill(scores, 0d);
		double[] pbmScores = new double[topX];
		for (int i=0; i<topX; i++){
			TextWindow[] passages = BlockIndexDocument.segmentDocument(index, docids[i], wSize);
			pbmScores[i] = this.score(termidWeightMap, termids, termidTFMap, termidDFMap, passages, model);
			passages = null;
		}
		
		ScoreNormaliser.normalizeScoresByMax(pbmScores);
		ScoreNormaliser.normalizeScoresByMax(scores);
		int N = scores.length;
		for (int i=0; i<topX; i++)
			scores[i] = (1-alpha)*scores[i]+alpha*pbmScores[i];
		for (int i=topX; i<N; i++)
			scores[i] = 0d;
		pbmScores = null; termids = null; termidWeightMap.clear(); termidWeightMap = null;
		termidTFMap.clear(); termidTFMap = null; termidDFMap.clear(); termidDFMap = null;
		// return
		return true;
	}
	
}

/**
 * 
 */
package org.terrier.matching.dsms;

import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntIntHashMap;

import java.io.IOException;

import org.terrier.matching.MatchingQueryTerms;
import org.terrier.matching.ResultSet;
import org.terrier.matching.models.WeightingModel;
import org.terrier.structures.BlockIndexDocument;
import org.terrier.structures.Index;
import org.terrier.structures.Lexicon;
import org.terrier.structures.LexiconEntry;
import org.terrier.utility.ApplicationSetup;

import uk.ac.gla.terrier.statistics.ScoreNormaliser;

/**
 * @author ben
 *
 */
public class COMCTX2DSM implements DocumentScoreModifier {

	/**
	 * 
	 */
	public COMCTX2DSM() {
		// TODO Auto-generated constructor stub
	}
	
public Object clone() { return this; }
	
	public String getName() { return "COMCTX2DSM";}
	
	protected void getLexiconEntries(Lexicon lexicon, int[] termids, TIntIntHashMap termidTFMap, TIntIntHashMap termidDFMap){
		for (int termid : termids){
			// System.err.println("Context term: "+lexicon.getLexiconEntry(termid).getKey());
			if (termidTFMap.contains(termid) && termidDFMap.contains(termid))
				continue;
			LexiconEntry le = (LexiconEntry)lexicon.getLexiconEntry(termid).getValue();
			termidTFMap.put(termid, le.getFrequency());
			termidDFMap.put(termid, le.getDocumentFrequency());
		}
	}
	
	protected double score(TIntDoubleHashMap termidWeightMap, int[] termids, 
			TIntIntHashMap termidTFMap, TIntIntHashMap termidDFMap, 
			Index index, int docid, WeightingModel model, int wSize){
		BlockIndexDocument doc = new BlockIndexDocument(index, docid);
		int[] proxTermids = doc.getProximityTerms(termids, wSize);
		this.getLexiconEntries(index.getLexicon(), proxTermids, termidTFMap, termidDFMap);
		TIntIntHashMap termidFreqMap = doc.getTermidFreqMap();
		double score = 0d;
		int docLength = 0;
		try{
			docLength = index.getDocumentIndex().getDocumentLength(docid);
		}catch(IOException ioe){}
		/*
		for (int termid : termids)
			score+=model.score(
					termidFreqMap.get(termid), 
					docLength, 
					termidDFMap.get(termid), 
					termidTFMap.get(termid), 
					termidWeightMap.get(termid)
					);*/
		for (int termid : proxTermids)
			score+=model.score(
					termidFreqMap.get(termid), 
					docLength, 
					termidDFMap.get(termid), 
					termidTFMap.get(termid), 
					1d// /proxTermids.length
					);
		if (model.ACCEPT_NEGATIVE_SCORE)
			score = Math.pow(2, score);
		return score;
	}

	public boolean modifyScores(final Index index, final MatchingQueryTerms queryTerms, final ResultSet resultSet){
		// get properties
		int wSize = Integer.parseInt(ApplicationSetup.getProperty("proximity.ngram.length", "5"));
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
			pbmScores[i] = this.score(termidWeightMap, termids, termidTFMap, termidDFMap, index, docids[i], model, wSize);
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

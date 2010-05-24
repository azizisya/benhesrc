package org.terrier.matching.dsms;

import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntIntHashMap;

import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.terrier.matching.MatchingQueryTerms;
import org.terrier.matching.ResultSet;
import org.terrier.matching.models.WeightingModel;
import org.terrier.statistics.GammaFunction;
import org.terrier.structures.CollectionStatistics;
import org.terrier.structures.DocumentIndex;
import org.terrier.structures.Index;
import org.terrier.structures.Lexicon;
import org.terrier.structures.LexiconEntry;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.Distance;



/**
 * Modifies the scores of the documents using n-grams, approximating
 * the dependence of terms between documents.
 * <p>
 * implement as a document score modifier, similarly to PhraseScoreModifier
 *
 * For each pair of query terms
 * do
 *	for each document
 *	do
 *		find number of times for which the terms co-occur in an n-gram and compute using the binomial and the laplace aftereffect
 * 		add the score to the corresponding document
 *	done
 * done
 *
 * <p>
 * <b>Properties</b>
 * <ul>
 * <li><tt>proximity.dependency.type</tt> - one of SD, FD for sequential dependence or full dependence</li>
 * <li><tt>proximity.ngram.length</tt> - proxmity windows for FD, in tokens</li>
 * <li><tt>proximity.ngram.SD.length</tt> - proximity windows for SD, in tokens</li>
 * <li><tt>proximity.w_t</tt> - weight of unigram in combination, defaults 1.0d</li>
 * <li><tt>proximity.w_o</tt> - weight of SD in combination, default 1.0d</li>
 * <li><tt>proximity.w_u</tt> - weight of FD in combination, default 1.0d</li>
 * <li><tt>proximity.high.weight.terms.only</tt> - only consider query terms with weight &gt;=1.0d. This maybe useful
 * for applications with QE or collection enrichment enabled. defaults to false.</li>
 * <li><tt>proximity.qtw.fnid</tt> - combination function to combine the qtws of two terms involved in a phrase. See below.</li>
 * </ul>
 * <p><b>QTW Combination Functions</b>
 * <ol>
 * <li><tt>1</tt>: phraseQTW = 0.5 * (qtw1 + qtw2)</li>
 * <li><tt>2</tt>: phraseQTW = qtw1 * qtw2</li>
 * <li><tt>3</tt>: phraseQTW = min(qtw1, qtw2)</li>
 * <li><tt>4</tt>: phraseQTW = max(qtw1, qtw2)</li>
 * </ol>
 * @author Vassilis Plachouras
 * @version $Revision: 1.1 $
 * @deprecated
 */
public class ProximityScoreModifier extends GeometricScoreModifier {
	/** The logger used */
	private static Logger logger = Logger.getRootLogger();
	
	public Object clone()
	{
		return new ProximityScoreModifier(phraseTerms);
	}

	public ProximityScoreModifier() { } 
	public ProximityScoreModifier(final String[] pTerms) {
		phraseTerms = pTerms;
	}

	public ProximityScoreModifier(final String[] pTerms, boolean r) {
		this(pTerms);
	}

	/**
	 * Returns the name of the modifier.
	 * 
	 * @return String the name of the modifier.
	 */
	public String getName() {
		return "AdhocGeometricScoreModifier";
	}
	
	@SuppressWarnings("unchecked")
	public void computeFDScore(String term1, String term2, double phraseTermWeight1, double phraseTermWeight2, double[] scores,
			double[] score_u, int[] docids, double ngramC, int ngramLength, boolean cache){
		double combinedPhraseQTWWeight = 1d;
		switch (phraseQTWfnid) {
			case 1: combinedPhraseQTWWeight = 0.5d * phraseTermWeight1 + 0.5d * phraseTermWeight2; 
			break;
			case 2: combinedPhraseQTWWeight = phraseTermWeight1 * phraseTermWeight2;
			break;
			case 3: combinedPhraseQTWWeight  = Math.min(phraseTermWeight1, phraseTermWeight2);
			break;
			case 4: combinedPhraseQTWWeight  = Math.max(phraseTermWeight1, phraseTermWeight2);
			break;
			default: combinedPhraseQTWWeight = 1.0d;
		}
		Lexicon lexicon = index.getLexicon();
		int termid1 = lexicon.getLexiconEntry(term1).getTermId();
	 	int termid2 = lexicon.getLexiconEntry(term2).getTermId();
	
		//System.err.println("term 1 and 2: " + term1 + " ("+termid1+") " + term2 + " ("+termid2+")");
	
		int[][] postings1 = getPostings(termid1, index, cache);
		int[][] postings2 = getPostings(termid2, index, cache);
/*		int[] postings14 = postings1[4];
		int[] postings24 = postings2[4];*/
		//find the documents that contain term1 and term2
		final int postings1Length = postings1[0].length;
		final int postings2Length = postings2[0].length;
	
		TIntDoubleHashMap ngramFrequencies = new TIntDoubleHashMap(docids.length);
		final int docidsLength = docids.length;
		int ngramDocumentFrequency = 0;
		int ngramCollectionFrequency = 0;
		// count ngram frequencies
		for (int docid : docids) {

			int index1 = Arrays.binarySearch(postings1[0], docid);
			int index2 = Arrays.binarySearch(postings2[0], docid);
			if (index1<0 || index2<0)
				continue;
	
			
			//find the places where the terms co-occur closely together
			int start1 = postings1[3][index1];
			int end1 = index1==postings1Length-1 ? postings1[4].length : postings1[3][index1+1];
		
			int start2 = postings2[3][index2];
   			int end2 = index2==postings2Length-1 ? postings2[4].length : postings2[3][index2+1];

	
			/**final int docLength = doi.getDocumentLength(docid);	
			final int matchingNGrams = Distance.noTimes(postings1[4], start1, end1,  postings2[4], start2, end2, ngramLength, docLength);*/
   			// logger.debug("Processing docid "+docid+", "+postings1[4][start1]+" "+postings1[4][end1-1]+" "+postings2[4][start2]+" "+postings2[4][end2-1]);
   			double ngramFrequency = Distance.bigramFrequency(postings1[4], start1, end1,  postings2[4], start2, end2, ngramLength);
   			
   			//if we found matching ngrams, we score them
   			if (ngramFrequency > 0) {
   				ngramDocumentFrequency++;
   				ngramCollectionFrequency+=ngramFrequency;
   			}
   			ngramFrequencies.put(docid, ngramFrequency);
		}
		// assign scores
		this.assignScores(scores, score_u, docids, ngramC, ngramFrequencies, ngramCollectionFrequency, ngramDocumentFrequency, combinedPhraseQTWWeight);
	}
	
	public void assignScores(double[] scores, double[] score_u, int[] docids, double ngramC, 
			TIntDoubleHashMap ngramFreqMap, int ngramCollFreq, int ngramDocFreq, double keyFrequency){
		int n = docids.length;
		model.setAverageDocumentLength(this.avgDocLen);
		model.setParameter(ngramC);
		model.setTermFrequency(ngramCollFreq);
		model.setDocumentFrequency(ngramDocFreq);
		model.setKeyFrequency(keyFrequency);
		int modifiedCounter = 0;
		for (int k=0; k<n; k++){
			int docLength = 0;
			try{
				docLength = doi.getDocumentLength(docids[k]);
			}catch(IOException ioe){
				ioe.printStackTrace();
			}
			final int numberOfNGrams = (docLength>0 && docLength < ngramLength)
			? 1
			: docLength - ngramLength + 1;
			final double nGf = ngramFreqMap.get(docids[k]);
						
			/*final double matchingNGramsNormalised = matchingNGrams * Math.log(1+ngramC*avgDocLen/numberOfNGrams)*REC_LOG_2; // /Math.log(2.0D);
	
			double p = 1.0D / avgDocLen numberOfNGrams;
			double q = 1.0d - p;
			double score = - gf.compute_log(numberOfNGrams avgDocLen+1.0d)*REC_LOG_2 // /Math.log(2.0d) 
					   + gf.compute_log(matchingNGramsNormalised+1.0d)*REC_LOG_2 // /Math.log(2.0d)
					   + gf.compute_log(numberOfNGrams avgDocLen - matchingNGramsNormalised + 1.0d)*REC_LOG_2// /Math.log(2.0d)
					   - matchingNGramsNormalised*Math.log(p)*REC_LOG_2 // /Math.log(2.0d)
					   - (numberOfNGrams avgDocLen - matchingNGramsNormalised)*Math.log(q)*REC_LOG_2;  // /Math.log(2.0d);
*/			
			
			if (nGf > 0){
				final double score = model.score(nGf, numberOfNGrams);
				if (Double.isInfinite(score) || Double.isNaN(score)) {
					logger.debug("docid: " + docids[k] + ", docLength:" + docLength + ", nGf: " + nGf + ", original score: " + scores[k] + "avgdoclen = "+ avgDocLen);
				} else
					score_u[k] += score;
				modifiedCounter++;
			}
		}
	}

}

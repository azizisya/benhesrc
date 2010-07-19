package org.terrier.matching.dsms;

import gnu.trove.THashSet;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntObjectHashMap;

import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.terrier.matching.MatchingQueryTerms;
import org.terrier.matching.ResultSet;
import org.terrier.matching.models.WeightingModel;
import org.terrier.matching.models.proximity.ProximityModel;
import org.terrier.statistics.GammaFunction;
import org.terrier.structures.BitIndexPointer;
import org.terrier.structures.CollectionStatistics;
import org.terrier.structures.DocumentIndex;
import org.terrier.structures.Index;
import org.terrier.structures.Lexicon;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.postings.BlockPosting;
import org.terrier.structures.postings.IterablePosting;
import org.terrier.utility.ApplicationSetup;



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
 * @author Ben He
 * @version $Revision: 1.1 $
 */
public class IterableProximityDSM extends ProximityDSM {
	/** The logger used */
	private static Logger logger = Logger.getRootLogger();
	
	public Object clone()
	{
		return new IterableProximityDSM(phraseTerms);
	}

	public IterableProximityDSM() { } 
	public IterableProximityDSM(final String[] pTerms) {
		phraseTerms = pTerms;
	}

	public IterableProximityDSM(final String[] pTerms, boolean r) {
		this(pTerms);
	}

	/**
	 * Returns the name of the modifier.
	 * 
	 * @return String the name of the modifier.
	 */
	public String getName() {
		return "IteratableProximityDSM";
	}
	
	public void computeProximityScore(String term1, String term2, double phraseTermWeight1, double phraseTermWeight2, double[] scores,
			double[] score_u, double[] score_o, int[] docids, double ngramC, int ngramLength, boolean cache) throws IOException{
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
	 	
		LexiconEntry le1 = lexicon.getLexiconEntry(term1);
		LexiconEntry le2 = lexicon.getLexiconEntry(term2);
		
		if (le1==null || le2==null)
			return;
	
		TIntDoubleHashMap ngramFrequencies = new TIntDoubleHashMap();
		TIntDoubleHashMap ngramFrequencies_o1 = new TIntDoubleHashMap();
		TIntDoubleHashMap ngramFrequencies_o2 = new TIntDoubleHashMap();
		
		int ngramDocumentFrequency = 0;
		int ngramCollectionFrequency = 0;
		int ngramDocumentFrequency_o1 = 0;
		int ngramCollectionFrequency_o1 = 0;
		int ngramDocumentFrequency_o2 = 0;
		int ngramCollectionFrequency_o2 = 0;
		
		IterablePosting postings1 = null;
		IterablePosting postings2 = null;
		
		try{
			postings1 = index.getInvertedIndex().getPostings((BitIndexPointer)le1);
			postings2 = index.getInvertedIndex().getPostings((BitIndexPointer)le2);
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
		
		postings1.next(); postings2.next();
		int counter = 0;
		// count ngram frequencies
		while (postings1.getId()!=IterablePosting.EOL && postings2.getId()!=IterablePosting.EOL){
			if (postings1.getId() < postings2.getId()){
				if (postings1.next()!=IterablePosting.EOL)
					continue;
				else
					break;
			}else if (postings1.getId() > postings2.getId()){
				if (postings2.next()!=IterablePosting.EOL)
					continue;
				else
					break;
			}
			
			int docid = postings1.getId();
			
			int docLength = postings1.getDocumentLength();
			int[] positions1 = ((BlockPosting)postings1).getPositions();
			int[] positions2 = ((BlockPosting)postings2).getPositions();
			
			if (UNORDERED){
				final double matchingNGrams = proxModel.getNGramFrequency(
						positions1, 0, positions1.length, 
						positions2, 0, positions2.length, 
						ngramLength, docLength);
	   			//if we found matching ngrams, we score them
	   			if (matchingNGrams > 0) {
	   				ngramDocumentFrequency++;
	   				ngramCollectionFrequency+=matchingNGrams;
	   			}
	   			ngramFrequencies.put(docid, matchingNGrams);
   			}
			if (ORDERED){
				double matchingNGrams = proxModel.getNGramFrequencyOrdered(
						positions1, 0, positions1.length, 
						positions2, 0, positions2.length, 
						ngramLength, docLength);
	   			//if we found matching ngrams, we score them
	   			if (matchingNGrams > 0) {
	   				ngramDocumentFrequency_o1++;
	   				ngramCollectionFrequency_o1+=matchingNGrams;
	   			}
	   			ngramFrequencies_o1.put(docid, matchingNGrams);
	   			matchingNGrams = proxModel.getNGramFrequencyOrdered(
	   					positions2, 0, positions2.length, 
						positions1, 0, positions1.length, 
						ngramLength, docLength);
	   			//if we found matching ngrams, we score them
	   			if (matchingNGrams > 0) {
	   				ngramDocumentFrequency_o2++;
	   				ngramCollectionFrequency_o2+=matchingNGrams;
	   			}
	   			ngramFrequencies_o2.put(docid, matchingNGrams);
   			}
			counter++;
			if (postings1.next()==IterablePosting.EOL || postings2.next()==IterablePosting.EOL)
				break;
			// System.err.println("doc "+docid+" has "+ngramFrequencies.get(docid)+" ngrams");
		}
		//System.err.println("counter="+counter+", ngramDocumentFrequency="+ngramDocumentFrequency+
				//", ngramCollectionFrequency="+ngramCollectionFrequency);
		// assign scores
		if (UNORDERED)
			assignScores(/*scores, */score_u, docids, ngramC, ngramFrequencies, ngramCollectionFrequency, ngramDocumentFrequency, combinedPhraseQTWWeight);
		if (ORDERED){
			assignScores(/*scores, */score_o, docids, ngramC, ngramFrequencies_o1, ngramCollectionFrequency_o1, 
					ngramDocumentFrequency_o1, combinedPhraseQTWWeight);
			assignScores(/*scores, */score_o, docids, ngramC, ngramFrequencies_o2, ngramCollectionFrequency_o2, 
					ngramDocumentFrequency_o2, combinedPhraseQTWWeight);
		}
		if (postings1!=null)
			postings1.close();
		if (postings2!=null)
			postings2.close();
		ngramFrequencies.clear(); ngramFrequencies = null;
		ngramFrequencies_o1.clear(); ngramFrequencies_o1 = null;
		ngramFrequencies_o2.clear(); ngramFrequencies_o2 = null;
		// System.gc();
	}
	
	protected int[] getPositions(IterablePosting postings, int docid) {
		int[] positions = null;
		try{
			while ((postings.getId())!=IterablePosting.EOL){
				if (postings.getId() < docid){
					postings.next();
					continue;
				}
				else if (postings.getId() > docid)
					return null;
				// docid found
				positions = ((BlockPosting)postings).getPositions();
				break;
			}
		}catch(IOException ioe){
			ioe.printStackTrace();
			return null;
		}
		return positions;
	}

}

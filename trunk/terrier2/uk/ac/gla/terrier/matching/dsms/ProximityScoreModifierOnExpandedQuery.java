/*
 * Created on 18 Aug 2008
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.matching.dsms;

import gnu.trove.TObjectDoubleHashMap;

import java.util.ArrayList;

import uk.ac.gla.terrier.matching.MatchingQueryTerms;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.LexiconEntry;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.FindTuples;

/**
 * @author ben
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ProximityScoreModifierOnExpandedQuery extends
		ProximityScoreModifierTREC2008 {
	protected FindTuples tupleFinder = new FindTuples(
			ApplicationSetup.getProperty("tuple.index.path", "/local/terrier/Indices/Wikipedia/2008/pfn/full/title/"),
			ApplicationSetup.getProperty("tuple.index.prefix", "data")
			);
	
	public Object clone()
	{
		return new ProximityScoreModifierRF2008(phraseTerms);
	}

	protected String[] phraseTerms;

	public ProximityScoreModifierOnExpandedQuery() {
		super();
	} 
	public ProximityScoreModifierOnExpandedQuery(final String[] pTerms) {
		super(pTerms);
		phraseTerms = pTerms;
	}

	public ProximityScoreModifierOnExpandedQuery(final String[] pTerms, boolean r) {
		this(pTerms);
	}

	/**
	 * Returns the name of the modifier.
	 * 
	 * @return String the name of the modifier.
	 */
	public String getName() {
		return "ProximityScoreModifierOnExpandedQuery";
	}
	
	/**
	 * Modifies the scores of documents, in which there exist, or there does not
	 * exist a given phrase.
	 * 
	 * @param index
	 *			Index the data structures to use.
	 * @param terms
	 *			MatchingQueryTerms the terms to be matched for the query. This
	 *			does not correspond to the phrase terms necessarily, but to
	 *			all the terms of the query.
	 * @param set
	 *			ResultSet the result set for the query.
	 * @return true if any scores have been altered
	 */
	public boolean modifyScores(Index index, MatchingQueryTerms terms, ResultSet set) {
		if (!ApplicationSetup.getProperty("querying.normalise.weights", "true").equals("false")){
			System.err.println("WARNING: Please set querying.normalise.weights to false to use this score modifier properly!");
		}

		if (phraseQTWfnid < 1 || phraseQTWfnid > 4)
		{
			System.err.println("ERROR: Wrong function id specified for ProximityScoreModifierRF2008");
		}
		if (this.index == null){
			this.index = index;
			doi = index.getDocumentIndex();
			final CollectionStatistics collStats = index.getCollectionStatistics();
			numTokens = collStats.getNumberOfTokens();
			numDocs = (long)(collStats.getNumberOfDocuments());
			avgDocLen =  ((double)(numTokens - numDocs *(ngramLength-1))) / (double)numDocs;
			avgDocLen_SD = ((double)(numTokens - numDocs*(ngramLength_SD - 1))) / (double)numDocs;
			lexicon = index.getLexicon();
		}
		
		w_t = Double.parseDouble(ApplicationSetup.getProperty("proximity.w_t","1.0d"));

		//get local references for the document ids and the
		//scores of documents from the result set.
		double[] scores = set.getScores();
		int[] docids = set.getDocids();
	
		//ordered dependence scores
		double[] score_o = new double[scores.length];
		//unordered dependence scores
		double[] score_u = new double[scores.length];
		//phraseterm - opinionTerm dependence scores
		double[] score_op = new double[scores.length];

		//the number of terms in the phrase
		//phraseTerms = terms.getTerms();
		String[] allTerms = terms.getTerms();
		
		TObjectDoubleHashMap<String> termHighWeightMap = new TObjectDoubleHashMap<String>();
		TObjectDoubleHashMap<String> termLowWeightMap = new TObjectDoubleHashMap<String>();
		
		for (String term : allTerms){
			LexiconEntry lexEntry = null;
			if ((lexEntry = lexicon.getLexiconEntry(term)) != null){
				/**
				 * Only terms that appear in no more than 20% of the documents are accepted.
				 * This is to reduce processing time.
				 */
				if (lexEntry.TF < numDocs/5){
					double weight = terms.getTermWeight(term);
					if (weight >=1d)
						termHighWeightMap.put(term, weight);
					else
						termLowWeightMap.put(term, weight);
				}
			}
		}
		
		for (String term : (String[])termHighWeightMap.keys(new String[termHighWeightMap.size()])){
			System.err.println("High weight phrase term: " + term);
		}
		if (termHighWeightMap.size() == 0)
			System.err.println("Warning: no high weight phrase term chosen.");
		
		for (String term : (String[])termLowWeightMap.keys(new String[termLowWeightMap.size()])){
			System.err.println("Low weight phrase term: " + term);
		}
		if (termLowWeightMap.size() == 0)
			System.err.println("Warning: no low weight phrase term chosen.");
		
		final double ngramC = Double.parseDouble(ApplicationSetup.getProperty("proximity.ngram.c","1.0d"));
		
		if (dependency.equals("FD")){
			w_u = Double.parseDouble(ApplicationSetup.getProperty("proximity.w_u","1.0d"));
			System.err.println("w_t: "+w_t+", w_u: "+w_u+", fnid: "+phraseQTWfnid+", ngramc: "+ngramC);
		}
		else if (dependency.equals("SD")){
			w_o = Double.parseDouble(ApplicationSetup.getProperty("proximity.w_o","1.0d"));
			System.err.println("w_t: "+w_t+", w_o: "+w_o+", fnid: "+phraseQTWfnid+", ngramc: "+ngramC);
		}

		if (dependency.equals("FD"))
		{
			for (String term1 : (String[])termHighWeightMap.keys(new String[termHighWeightMap.size()])){
				for (String term2 : (String[])termLowWeightMap.keys(new String[termLowWeightMap.size()])){
					if (tupleFinder.findTuple(term1, term2)){
						//System.out.println("Tuple ("+term1+", "+term2+") is valid");
						this.computeFDScore(term1, term2, scores, score_u, docids, 
							termHighWeightMap.get(term1), termLowWeightMap.get(term2), ngramC, ngramLength, index);
					}else{
						System.out.println("Tuple ("+term1+", "+term2+") ignored from querying");
					}
				}
			}
		
			for (int k=0; k<docids.length; k++) {		
				scores[k] =  w_t * scores[k] +  w_u * score_u[k] ;		
			}
			
	  		postingsCache.clear();
			 		   	  
	  	}
		else if (dependency.equals("SD")){
			for (String term1 : (String[])termHighWeightMap.keys(new String[termHighWeightMap.size()])){
				for (String term2 : (String[])termLowWeightMap.keys(new String[termLowWeightMap.size()])){
					if (tupleFinder.findTuple(term1, term2)){
						System.out.println("Tuple ("+term1+", "+term2+") is valid");
						this.computeFDScore(term1, term2, scores, score_u, docids, 
							termHighWeightMap.get(term1), termLowWeightMap.get(term2), ngramC, ngramLength, index);
					}else{
						System.out.println("Tuple ("+term1+", "+term2+") ignored from querying");
					}
				}
			}
			for (int k=0; k<docids.length; k++) {		
				scores[k] =  w_t * scores[k] +  w_o * score_u[k] ;		
			}		
	  		postingsCache.clear();
		}
		else
		{
			System.err.println("WARNING: proximity.dependency.type not set. Set it to either FD or SD");
			return false;
		}
		//returning true, assuming that we have modified the scores of documents		
		return true;		
	}
}

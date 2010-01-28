package uk.ac.gla.terrier.matching.dsms;

import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntIntHashMap;
import java.util.Arrays;
import java.util.ArrayList;
import uk.ac.gla.terrier.statistics.GammaFunction;
import uk.ac.gla.terrier.matching.MatchingQueryTerms;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Distance;
import uk.ac.gla.terrier.structures.DocumentIndex;


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
 */
public class ProximityScoreModifierTREC2008 implements DocumentScoreModifier {
	public Object clone()
	{
		return new ProximityScoreModifierTREC2008(phraseTerms);
	}

	protected final GammaFunction gf = new GammaFunction();
	protected static final double REC_LOG_2 = 1.0d / Math.log(2.0d);
	
	protected boolean CACHE_POSTINGS = Boolean.parseBoolean(ApplicationSetup.getProperty("cache.postings", "false"));
	
	protected Index index = null;
	protected DocumentIndex doi;
	protected long numTokens;
	protected long numDocs;
	protected double avgDocLen;
	protected double avgDocLen_SD;
	protected Lexicon lexicon;
	
	/** The size of the considered ngrams */
	protected int ngramLength = Integer.parseInt(ApplicationSetup.getProperty("proximity.ngram.length","2"));
	protected int ngramLength_SD = Integer.parseInt(ApplicationSetup.getProperty("proximity.ngram.SD.length","2"));
	/** type of proximity to use */	
	protected String dependency = ApplicationSetup.getProperty("proximity.dependency.type","");
	protected boolean onlyHighWeightQTerms = Boolean.parseBoolean(ApplicationSetup.getProperty("proximity.high.weight.terms.only", "false"));
	protected final int phraseQTWfnid = Integer.parseInt(ApplicationSetup.getProperty("proximity.qtw.fnid", "1"));

 	/** weight of unigram model */
	protected double w_t = Double.parseDouble(ApplicationSetup.getProperty("proximity.w_t","1.0d"));
	/** weight of ordered dependence model */
	protected double w_o = Double.parseDouble(ApplicationSetup.getProperty("proximity.w_o","1.0d"));
	/** weight of unordered dependence model */
	protected double w_u = Double.parseDouble(ApplicationSetup.getProperty("proximity.w_u","1.0d"));

	/** A list of the strings of the phrase terms. */
	protected String[] phraseTerms;

	public ProximityScoreModifierTREC2008() { } 
	public ProximityScoreModifierTREC2008(final String[] pTerms) {
		phraseTerms = pTerms;
	}

	public ProximityScoreModifierTREC2008(final String[] pTerms, boolean r) {
		this(pTerms);
	}

	/**
	 * Returns the name of the modifier.
	 * 
	 * @return String the name of the modifier.
	 */
	public String getName() {
		return "ProximityScoreModifierTREC2008";
	}

	TIntObjectHashMap postingsCache = new TIntObjectHashMap();
	
	public void clearCache(){
		this.postingsCache.clear();
		postingsCache = null;
		System.gc();
		postingsCache = new TIntObjectHashMap();
	}
	
	public void clearCache(int termid){
		this.postingsCache.remove(termid);
	}
	
	public void computeFDScore(String term1, String term2, double[] scores,
			double[] score_u, int[] docids, double phraseTermWeight1, 
			double phraseTermWeight2, double ngramC, int ngramLength, Index index){
		double combinedPhraseQTWWeight;
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
		lexicon.findTerm(term1);
		int termid1 = lexicon.getTermId();
		try{
			lexicon.findTerm(term2);
		}catch(StringIndexOutOfBoundsException se){
			System.err.println("term2: "+term2);
			se.printStackTrace();
			System.exit(1);
		}
	 	int termid2 = lexicon.getTermId();
	
		System.err.println("term 1 and 2: " + term1 + " ("+termid1+") " + term2 + " ("+termid2+")");
	
		int[][] postings1 = getPostings(termid1, index, this.CACHE_POSTINGS);
		int[][] postings2 = getPostings(termid2, index, this.CACHE_POSTINGS);
			int[] postings14 = postings1[4];
			int[] postings24 = postings2[4];
		//find the documents that contain term1 and term2
			final int postings1Length = postings1[0].length;
			final int postings2Length = postings2[0].length;
	
			TIntIntHashMap ngramFrequencies = new TIntIntHashMap(docids.length);
			int TF = 0;
			final int docidsLength = docids.length;
			for (int k=0; k<docidsLength; k++) {

				int index1 = Arrays.binarySearch(postings1[0], docids[k]);
				int index2 = Arrays.binarySearch(postings2[0], docids[k]);
				if (index1<0 || index2<0)
				continue;
	
			
			//find the places where the terms co-occur closely together
			int start1 = postings1[3][index1];
			int end1 = index1==postings1Length-1 ? postings1[4].length : postings1[3][index1+1];
		
			int start2 = postings2[3][index2];
   			int end2 = index2==postings2Length-1 ? postings2[4].length : postings2[3][index2+1];

	
			final int docLength = doi.getDocumentLength(docids[k]);	
			final int matchingNGrams = Distance.noTimes(postings1[4], start1, end1,  postings2[4], start2, end2, ngramLength, docLength);
   			//if we found matching ngrams, we score them
   			if (matchingNGrams > 0) {
			
				final int numberOfNGrams = (docLength>0 && docLength < ngramLength)
					? 1
					: docLength - ngramLength + 1;
   				final double matchingNGramsNormalised = matchingNGrams * Math.log(1+ngramC*avgDocLen/numberOfNGrams)*REC_LOG_2; // /Math.log(2.0D);

   				double p = 1.0D / avgDocLen /*numberOfNGrams*/;
   				double q = 1.0d - p;
   				double score = - gf.compute_log(/*numberOfNGrams*/ avgDocLen+1.0d)*REC_LOG_2 // /Math.log(2.0d) 
							   + gf.compute_log(matchingNGramsNormalised+1.0d)*REC_LOG_2 // /Math.log(2.0d)
							   + gf.compute_log(/*numberOfNGrams*/ avgDocLen - matchingNGramsNormalised + 1.0d)*REC_LOG_2// /Math.log(2.0d)
							   - matchingNGramsNormalised*Math.log(p)*REC_LOG_2 // /Math.log(2.0d)
							   - (/*numberOfNGrams*/ avgDocLen - matchingNGramsNormalised)*Math.log(q)*REC_LOG_2;  // /Math.log(2.0d);
			
				score = score / (1.0d + matchingNGramsNormalised);
			
   				if (Double.isInfinite(score) || Double.isNaN(score)) {
		   			System.err.println("docid: " + docids[k] + ", docLength:" + docLength + ", matchingNGrams: " + matchingNGrams + "matchingNGramsNormalised="+matchingNGramsNormalised+", original score: " + scores[k] + "avgdoclen = "+ avgDocLen);
				} else
					score_u[k] += combinedPhraseQTWWeight * score;		
			}
		}

	}
	
	public void computeFDScore(String term1, String term2, double[] scores,
			double[] score_u, int[] docids, double phraseTermWeight1, 
			double phraseTermWeight2, double ngramC, int ngramLength, Index index, boolean cache){
		double combinedPhraseQTWWeight;
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
		lexicon.findTerm(term1);
		int termid1 = lexicon.getTermId();
		try{
			lexicon.findTerm(term2);
		}catch(StringIndexOutOfBoundsException se){
			System.err.println("term2: "+term2);
			se.printStackTrace();
			System.exit(1);
		}
	 	int termid2 = lexicon.getTermId();
	
		//System.err.println("term 1 and 2: " + term1 + " ("+termid1+") " + term2 + " ("+termid2+")");
	
		int[][] postings1 = getPostings(termid1, index, cache);
		int[][] postings2 = getPostings(termid2, index, cache);
			int[] postings14 = postings1[4];
			int[] postings24 = postings2[4];
		//find the documents that contain term1 and term2
			final int postings1Length = postings1[0].length;
			final int postings2Length = postings2[0].length;
	
			TIntIntHashMap ngramFrequencies = new TIntIntHashMap(docids.length);
			int TF = 0;
			final int docidsLength = docids.length;
			for (int k=0; k<docidsLength; k++) {

				int index1 = Arrays.binarySearch(postings1[0], docids[k]);
				int index2 = Arrays.binarySearch(postings2[0], docids[k]);
				if (index1<0 || index2<0)
				continue;
	
			
			//find the places where the terms co-occur closely together
			int start1 = postings1[3][index1];
			int end1 = index1==postings1Length-1 ? postings1[4].length : postings1[3][index1+1];
		
			int start2 = postings2[3][index2];
   			int end2 = index2==postings2Length-1 ? postings2[4].length : postings2[3][index2+1];

	
			final int docLength = doi.getDocumentLength(docids[k]);	
			final int matchingNGrams = Distance.noTimes(postings1[4], start1, end1,  postings2[4], start2, end2, ngramLength, docLength);
   			//if we found matching ngrams, we score them
   			if (matchingNGrams > 0) {
			
				final int numberOfNGrams = (docLength>0 && docLength < ngramLength)
					? 1
					: docLength - ngramLength + 1;
   				final double matchingNGramsNormalised = matchingNGrams * Math.log(1+ngramC*avgDocLen/numberOfNGrams)*REC_LOG_2; // /Math.log(2.0D);

   				double p = 1.0D / avgDocLen /*numberOfNGrams*/;
   				double q = 1.0d - p;
   				double score = - gf.compute_log(/*numberOfNGrams*/ avgDocLen+1.0d)*REC_LOG_2 // /Math.log(2.0d) 
							   + gf.compute_log(matchingNGramsNormalised+1.0d)*REC_LOG_2 // /Math.log(2.0d)
							   + gf.compute_log(/*numberOfNGrams*/ avgDocLen - matchingNGramsNormalised + 1.0d)*REC_LOG_2// /Math.log(2.0d)
							   - matchingNGramsNormalised*Math.log(p)*REC_LOG_2 // /Math.log(2.0d)
							   - (/*numberOfNGrams*/ avgDocLen - matchingNGramsNormalised)*Math.log(q)*REC_LOG_2;  // /Math.log(2.0d);
			
				score = score / (1.0d + matchingNGramsNormalised);
			
   				if (Double.isInfinite(score) || Double.isNaN(score)) {
		   			System.err.println("docid: " + docids[k] + ", docLength:" + docLength + ", matchingNGrams: " + matchingNGrams + "matchingNGramsNormalised="+matchingNGramsNormalised+", original score: " + scores[k] + "avgdoclen = "+ avgDocLen);
				} else
					score_u[k] += combinedPhraseQTWWeight * score;		
			}
		}

	}
	
	public void computeSDScore(String term1, String term2, double[] scores,
			double[] score_o, int[] docids, double phraseTermWeight1, double ngramC, 
			double phraseTermWeight2, Index index){
		double combinedPhraseQTWWeight;
		switch (phraseQTWfnid) {
			case 1: combinedPhraseQTWWeight = 0.5 * phraseTermWeight1 + 0.5 * phraseTermWeight2;
			break;
			case 2: combinedPhraseQTWWeight = phraseTermWeight1 * phraseTermWeight2;
			break;
			case 3: combinedPhraseQTWWeight  = Math.min(phraseTermWeight1, phraseTermWeight2);
			break;
			case 4: combinedPhraseQTWWeight  = Math.max(phraseTermWeight1, phraseTermWeight2);
			break;
			default: combinedPhraseQTWWeight = 1.0d;
		}
		
		lexicon.findTerm(term1);
		int termid1 = lexicon.getTermId();
		lexicon.findTerm(term2);
	 	int termid2 = lexicon.getTermId();
		
 		lexicon.findTerm(term1);
 		termid1 = lexicon.getTermId();
 		lexicon.findTerm(term2);
 		termid2 = lexicon.getTermId();
			
		System.err.println("term 1 and 2: " + term1 + " ("+termid1+") " + term2 + " ("+termid2+")");
		
 		int[][] postings1 = getPostings(termid1, index, this.CACHE_POSTINGS);
		int[][] postings2 = getPostings(termid2, index, this.CACHE_POSTINGS);
 		int[] postings14 = postings1[4];
 		int[] postings24 = postings2[4];
 		//find the documents that contain term1 and term2
 		final int postings1Length = postings1[0].length;
 		final int postings2Length = postings2[0].length;
		
 		TIntIntHashMap ngramFrequencies = new TIntIntHashMap(docids.length);
		int TF = 0;
 		final int docidsLength = docids.length;
 		for (int k=0; k<docidsLength; k++) {
			int index1 = Arrays.binarySearch(postings1[0], docids[k]);
 			int index2 = Arrays.binarySearch(postings2[0], docids[k]);
 			if (index1<0 || index2<0)
				continue;
		
				
	  		//find the places where the terms co-occur closely together
	  		int start1 = postings1[3][index1];
	  		int end1 = index1==postings1Length-1 ? postings1[4].length : postings1[3][index1+1];
			
   			int start2 = postings2[3][index2];
	  		int end2 = index2==postings2Length-1 ? postings2[4].length : postings2[3][index2+1];
			final int docLength = doi.getDocumentLength(docids[k]);	
			final int matchingNGrams2 = Distance.noTimesSameOrder(postings1[4], start1, end1,  postings2[4], start2, end2, ngramLength, docLength);
			if (matchingNGrams2 > 0) {
				final int numberOfNGrams = (docLength>0 && docLength < ngramLength_SD)
					? 1
					: docLength - ngramLength_SD + 1;
				final double matchingNGramsNormalised = matchingNGrams2 * Math.log(1+ngramC*avgDocLen_SD/numberOfNGrams)*REC_LOG_2;// /Math.log(2.0D);
				double p = 1.0D / /*numberOfNGrams*/avgDocLen_SD;
				double q = 1.0d - p;
				double score = - gf.compute_log(/*numberOfNGrams*/avgDocLen_SD+1.0d)*REC_LOG_2// /Math.log(2.0d) 
							   + gf.compute_log(matchingNGramsNormalised+1.0d)*REC_LOG_2// /Math.log(2.0d)
							   + gf.compute_log(/*numberOfNGrams*/avgDocLen_SD - matchingNGramsNormalised + 1.0d)*REC_LOG_2// /Math.log(2.0d)
							   - matchingNGramsNormalised*Math.log(p)*REC_LOG_2// /Math.log(2.0d)
							   - (/*numberOfNGrams*/avgDocLen_SD - matchingNGramsNormalised)*Math.log(q)*REC_LOG_2; // /Math.log(2.0d);
				
				score = score / (1.0d + matchingNGramsNormalised);
				
				if (Double.isInfinite(score) || Double.isNaN(score)) {
					System.err.println("docid: " + docids[k] + ", docLength:" + docLength + ", matchingNGrams2: " + matchingNGrams2 + "matchingNGramsNormalised="+matchingNGramsNormalised+", original score: " + scores[k]);
				} else
					score_o[k] += combinedPhraseQTWWeight;
			
  			}
	
			}
 		//postings1 = null; postings2 = null;
		
	}
	
	public void setIndex(Index index){
		this.index = index;
		doi = index.getDocumentIndex();
		final CollectionStatistics collStats = index.getCollectionStatistics();
		numTokens = collStats.getNumberOfTokens();
		numDocs = (long)(collStats.getNumberOfDocuments());
		avgDocLen =  ((double)(numTokens - numDocs *(ngramLength-1))) / (double)numDocs;
		avgDocLen_SD = ((double)(numTokens - numDocs*(ngramLength_SD - 1))) / (double)numDocs;
		lexicon = index.getLexicon();
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

		if (phraseQTWfnid < 1 || phraseQTWfnid > 4)
		{
			System.err.println("ERROR: Wrong function id specified for ProximityScoreModifierTREC2008");
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
		phraseTerms = terms.getTerms();
		if (onlyHighWeightQTerms)
		{
			ArrayList<String> proxterms = new ArrayList<String>();
			for (String t : phraseTerms)
			{
				if (terms.getTermWeight(t) >= 1.0d)
					// A phrase term should appear in no more than 5% of documents in the collection.
					// This is to reduce memory cost.
					if (lexicon.getLexiconEntry(t).n_t < this.numDocs/20)
						proxterms.add(t);
			}
			phraseTerms = proxterms.toArray(new String[0]);
		}
		final int phraseLength = phraseTerms.length;
		final double[] phraseTermWeights = new double[phraseLength];
		for (int i=0; i<phraseLength; i++)
		{
			phraseTermWeights[i] = terms.getTermWeight(phraseTerms[i]);
			System.err.println("phrase term: " + phraseTerms[i]);
		}
		if (phraseLength == 0)
			System.err.println("Warning: no phrase term chosen.");
		String term1 = null;
		String term2 = null;
		
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
			for (int i=0; i<phraseLength-1; i++) {
				for (int j=i+1; j<phraseLength; j++) {
					term1 = phraseTerms[i];
					term2 = phraseTerms[j];
					this.computeFDScore(term1, term2, scores, score_u, docids, 
							phraseTermWeights[i], phraseTermWeights[j], ngramC, ngramLength, index);
	   			}
			}
		
			for (int k=0; k<docids.length; k++) {		
				scores[k] =  w_t * scores[k] +  w_u * score_u[k] ;		
			}
			
	  		postingsCache.clear();
			 		   	  
	  	}
		else if (dependency.equals("SD")){
			for (int i=0; i<phraseLength-1; i++) {
				for (int j=i+1; j<phraseLength; j++) {
					term1 = phraseTerms[i];
					term2 = phraseTerms[j];
					this.computeFDScore(term1, term2, scores, score_u, docids, 
							phraseTermWeights[i], phraseTermWeights[j], ngramC, ngramLength, index);
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

	
	/*protected int[][] getPostings(int termid, Index index) {
		
		int[][] postings = null;
		
		if (postingsCache.contains(termid)) {
			postings = (int[][])postingsCache.get(termid);
		} else {
			postings = index.getInvertedIndex().getDocuments(termid);
			
			//replace the block frequencies with the index of the blocks in the array
			final int docFrequency = postings[0].length;
			int blockFrequencySum = 0;
			int tmp;
			for (int i = 0; i<docFrequency; i++) {
				tmp = postings[3][i];
				postings[3][i] = blockFrequencySum;
				blockFrequencySum += tmp;
			}
			if (this.CACHE_POSTINGS)
				postingsCache.put(termid, postings);
		}
		return postings;
	}*/
	
protected int[][] getPostings(int termid, Index index, boolean cache) {
		
		int[][] postings = null;
		
		if (postingsCache.contains(termid)) {
			postings = (int[][])postingsCache.get(termid);
		} else {
			postings = index.getInvertedIndex().getDocuments(termid);
			
			//replace the block frequencies with the index of the blocks in the array
			final int docFrequency = postings[0].length;
			int blockFrequencySum = 0;
			int tmp;
			for (int i = 0; i<docFrequency; i++) {
				tmp = postings[3][i];
				postings[3][i] = blockFrequencySum;
				blockFrequencySum += tmp;
			}
			if (cache)
				postingsCache.put(termid, postings);
		}
		return postings;
	}

}

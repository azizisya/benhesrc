package uk.ac.gla.terrier.matching.dsms;

import gnu.trove.THashSet;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntIntHashMap;
import java.util.Arrays;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.statistics.GammaFunction;
import uk.ac.gla.terrier.statistics.Statistics;
import uk.ac.gla.terrier.matching.MatchingQueryTerms;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.matching.models.WeightingModel;
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
public class WindowNGramScoreModifier implements DocumentScoreModifier {
	/** The logger used */
	private static Logger logger = Logger.getRootLogger();
	
	public Object clone()
	{
		return new WindowNGramScoreModifier(phraseTerms);
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
	
	protected WeightingModel model;
	
	/** The size of the considered ngrams */
	protected int ngramLength = Integer.parseInt(ApplicationSetup.getProperty("proximity.ngram.length","2"));
	protected int ngramLength_SD = Integer.parseInt(ApplicationSetup.getProperty("proximity.ngram.SD.length","2"));
	/** type of proximity to use */	
	protected String dependency = ApplicationSetup.getProperty("proximity.dependency.type", "FD");
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

	public WindowNGramScoreModifier() { } 
	public WindowNGramScoreModifier(final String[] pTerms) {
		phraseTerms = pTerms;
	}

	public WindowNGramScoreModifier(final String[] pTerms, boolean r) {
		this(pTerms);
	}

	/**
	 * Returns the name of the modifier.
	 * 
	 * @return String the name of the modifier.
	 */
	public String getName() {
		return "WindowNGramScoreModifier";
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
		this.postingsCache = null;
		System.gc();
		this.postingsCache = new TIntObjectHashMap();
	}
	
	protected double getQTWn(double[] qtws){
		double qtwn = 1d;
		switch (phraseQTWfnid) {
			case 1: qtwn = Statistics.mean(qtws); 
			break;
			case 2: for (int i=0; i<qtws.length; i++) qtwn*=qtws[i];
			break;
			case 3: qtwn=Statistics.min(qtws);
			break;
			case 4: qtwn=Statistics.max(qtws);
			break;
			default: qtwn = 1.0d;
		}
		return qtwn;
	}
	
	public void computeFDScore(String[] terms, double[] phraseTermWeights, double[] scores,
			double[] score_u, int[] docids, double ngramC, int ngramLength, boolean cache){
		double combinedPhraseQTWWeight = this.getQTWn(phraseTermWeights);
		Lexicon lexicon = index.getLexicon();
		int numberOfTerms = terms.length;
		int[] termids = new int[numberOfTerms];
		
		for (int i=0; i<numberOfTerms; i++){
			termids[i] = lexicon.getLexiconEntry(terms[i]).termId;
		}
	
		//System.err.println("term 1 and 2: " + term1 + " ("+termid1+") " + term2 + " ("+termid2+")");
		int[][][] postings = new int[numberOfTerms][][];
		
		for (int i=0; i<numberOfTerms; i++)
			postings[i] = getPostings(termids[i], index, cache);
	 
		//find the documents that contain term1 and term2
		int[] postingsLength = new int[numberOfTerms];
		for (int i=0; i<numberOfTerms; i++)
			postingsLength[i] = postings[i][0].length;
	
		TIntIntHashMap ngramFrequencies = new TIntIntHashMap(docids.length);
		final int docidsLength = docids.length;
		int ngramDocumentFrequency = 0;
		int ngramCollectionFrequency = 0;
		// count ngram frequencies
		for (int docid : docids) {
			int[] index = new int[numberOfTerms];
			boolean notFound = false;
			for (int i=0; i<numberOfTerms; i++){
				index[i] = Arrays.binarySearch(postings[i][0], docid);
				if (index[i] < 0)
					notFound = true;
			}
			
			if (notFound)
				continue;
			int[] start = new int[numberOfTerms];
			int[] end = new int[numberOfTerms];
			
			//find the places where the terms co-occur closely together
			for (int i=0; i<numberOfTerms; i++){
				start[i] = postings[i][3][index[i]];
				end[i] = index[i]==postingsLength[i]-1 ? postings[i][4].length : postings[i][3][index[i]+1];
			}

	
			final int docLength = doi.getDocumentLength(docid);	
			int[][] blocksOfTerms = new int[numberOfTerms][];
			for (int i=0; i<numberOfTerms; i++)
				blocksOfTerms[i]=postings[i][4];
			final int matchingNGrams = Distance.noTimes(blocksOfTerms, start, end, ngramLength, docLength); 
				
   			//if we found matching ngrams, we score them
   			if (matchingNGrams > 0) {
   				ngramDocumentFrequency++;
   				ngramCollectionFrequency++;
   			}
   			ngramFrequencies.put(docid, matchingNGrams);
   			index = null;
   			start=null; end=null;
		}
		// assign scores
		this.assignScores(scores, score_u, docids, ngramC, ngramFrequencies, ngramCollectionFrequency, ngramDocumentFrequency, combinedPhraseQTWWeight);
		postings = null; postingsLength=null;
		ngramFrequencies.clear(); ngramFrequencies = null; termids = null;
		System.gc();
	}
	
	public void assignScores(double[] scores, double[] score_u, int[] docids, double ngramC, 
			TIntIntHashMap ngramFreqMap, int ngramCollFreq, int ngramDocFreq, double keyFrequency){
		int n = docids.length;
		model.setAverageDocumentLength(this.avgDocLen);
		model.setParameter(ngramC);
		model.setTermFrequency(ngramCollFreq);
		model.setDocumentFrequency(ngramCollFreq);
		model.setKeyFrequency(keyFrequency);
		int modifiedCounter = 0;
		for (int k=0; k<n; k++){
			final int docLength = doi.getDocumentLength(docids[k]);
			final int numberOfNGrams = (docLength>0 && docLength < ngramLength)
			? 1
			: docLength - ngramLength + 1;
			final int matchingNGrams = ngramFreqMap.get(docids[k]);	
			
			if (matchingNGrams != 0){
				final double score = model.score(matchingNGrams, docLength);
				if (Double.isInfinite(score) || Double.isNaN(score)) {
					logger.warn("docid: " + docids[k] + ", docLength:" + docLength + ", matchingNGrams: " + matchingNGrams + ", original score: " + scores[k] + "avgdoclen = "+ avgDocLen);
				} else
					score_u[k] += score;
				modifiedCounter++;
			}
		}
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
			logger.error("ERROR: Wrong function id specified for WindowNGramScoreModifier");
		}
		String modelName = ApplicationSetup.getProperty("geo.model", "BM25");
		this.model = WeightingModel.getWeightingModel(modelName);
		if (this.index == null){
			this.index = index;
			doi = index.getDocumentIndex();
			final CollectionStatistics collStats = index.getCollectionStatistics();
			numTokens = collStats.getNumberOfTokens();
			numDocs = (long)(collStats.getNumberOfDocuments());
			model.setNumberOfDocuments(numDocs);
			model.setNumberOfTokens((double)(numTokens - numDocs *(ngramLength-1)));
			avgDocLen =  ((double)(numTokens - numDocs *(ngramLength-1))) / (double)numDocs;
			avgDocLen_SD = ((double)(numTokens - numDocs*(ngramLength_SD - 1))) / (double)numDocs;
			lexicon = index.getLexicon();
		}
		
		w_t = Double.parseDouble(ApplicationSetup.getProperty("proximity.w_t","1.0d"));
		// if the geometric relevance score will be combined with the content-based relevance score.
		boolean combinedModel = Boolean.parseBoolean(ApplicationSetup.getProperty("geo.combined", "true"));

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
		
		// keep only terms appear in less than 10% of documents in the collection
		THashSet<String> phraseTermSet = new THashSet<String>();
		for (String term : phraseTerms){
			int df = lexicon.getLexiconEntry(term).n_t;
			if ((double)df/this.numDocs <= 0.1d)
				phraseTermSet.add(term);
			else
				logger.debug(term+" removed from phrase terms for high document frequency.");
		}
		phraseTerms = phraseTermSet.toArray(new String[phraseTermSet.size()]);
		
		final int phraseLength = phraseTerms.length;
		final double[] phraseTermWeights = new double[phraseLength];
		for (int i=0; i<phraseLength; i++)
		{
			phraseTermWeights[i] = terms.getTermWeight(phraseTerms[i]);
			logger.debug("phrase term: " + phraseTerms[i]);
		}
		if (phraseLength == 0)
			logger.warn("Warning: no phrase term chosen.");
		String term1 = null;
		String term2 = null;
		
		final double ngramC = Double.parseDouble(ApplicationSetup.getProperty("proximity.ngram.c","1.0d"));
		
		if (dependency.equals("FD")){
			w_u = Double.parseDouble(ApplicationSetup.getProperty("proximity.w_u","1.0d"));
			logger.debug("w_t: "+w_t+", w_u: "+w_u+", fnid: "+phraseQTWfnid+", ngramc: "+ngramC+", w_size: "+this.ngramLength);
		}
		else if (dependency.equals("SD")){
			w_o = Double.parseDouble(ApplicationSetup.getProperty("proximity.w_o","1.0d"));
			logger.debug("w_t: "+w_t+", w_o: "+w_o+", fnid: "+phraseQTWfnid+", ngramc: "+ngramC+", w_size: "+this.ngramLength);
		}
		int modifiedCounter = 0;
		if (dependency.equals("FD"))
		{
			for (int i=0; i<phraseLength-1; i++) {
				for (int j=i+1; j<phraseLength; j++) {
					term1 = phraseTerms[i];
					term2 = phraseTerms[j];
					/** TODO: parameterized ngram length */
					//this.computeFDScore(term1, term2, phraseTermWeights[i], phraseTermWeights[j], scores, score_u, docids, 
						//	ngramC, ngramLength, this.CACHE_POSTINGS);
	   			}
			}
			for (int k=0; k<docids.length; k++) {
				if (score_u[k] != 0d)
					modifiedCounter++;
				if (combinedModel)
					scores[k] =  w_t * scores[k] +  w_u * score_u[k] ;
				else
					scores[k] = score_u[k];
			}
			if (this.CACHE_POSTINGS)
				postingsCache.clear();
			logger.debug("Modifered scores for "+modifiedCounter+" documents.");
	  	}
		else if (dependency.equals("SD")){
			/**
			for (int i=0; i<phraseLength-1; i++) {
				for (int j=i+1; j<phraseLength; j++) {
					term1 = phraseTerms[i];
					term2 = phraseTerms[j];
					this.computeFDScore(term1, term2, phraseTermWeights[i], phraseTermWeights[j], scores, score_u, docids, 
							ngramC, ngramLength, this.CACHE_POSTINGS);
				}
			}
			for (int k=0; k<docids.length; k++) {		
				if (score_u[k] != 0d)
					modifiedCounter++;
				if (combinedModel)
					scores[k] =  w_t * scores[k] +  w_u * score_u[k] ;
				else
					scores[k] = score_u[k];		
			}
			if (this.CACHE_POSTINGS)
				postingsCache.clear();*/
		}
		else
		{
			logger.debug("WARNING: proximity.dependency.type not set. Set it to either FD or SD");
			return false;
		}
		//returning true, assuming that we have modified the scores of documents		
		// return true;
		return (modifiedCounter>0);
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

package uk.ac.gla.terrier.matching.dsms;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntObjectHashMap;

import java.util.Arrays;

import uk.ac.gla.terrier.matching.MatchingQueryTerms;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.statistics.GammaFunction;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.ExpansionTerm;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.InvertedIndex;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.structures.Tuple;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Distance;

public class TupleProximityScoreModifier implements FunctionScoreModifier {

	private int windowSize = Integer.parseInt(ApplicationSetup.getProperty("proximity.ngram.length", "2"));
	/** type of proximity to use */
	protected String dependency = ApplicationSetup.getProperty(
			"proximity.dependency.type", "");
	protected boolean onlyHighWeightQTerms = Boolean
			.parseBoolean(ApplicationSetup.getProperty(
					"proximity.high.weight.terms.only", "false"));
	/** weight of unigram model */
	protected double w_t = Double.parseDouble(ApplicationSetup.getProperty(
			"proximity.w_t", "1.0d"));
	/** weight of sequential (ordered) dependence model */
	protected double w_o = Double.parseDouble(ApplicationSetup.getProperty(
			"proximity.w_o", "1.0d"));
	/** weight of full (unordered) dependence model */
	protected double w_u = Double.parseDouble(ApplicationSetup.getProperty(
			"proximity.w_u", "1.0d"));
	
	private double ngramC = Double.parseDouble(ApplicationSetup.getProperty("proximity.ngram.c","1.0d"));
	private static final double REC_LOG_2 = 1.0d / Math.log(2.0d);
	
	private GammaFunction gf = new GammaFunction();
	
	private TIntObjectHashMap<int[][]> postingsCache = new TIntObjectHashMap<int[][]>();
	
	/** The array of tuples on which to evaluate proximity. */
	private Tuple[] tuples;
	
	/** The weight associated to this DSM. */
	private double weight;
	
	public TupleProximityScoreModifier(Tuple[] tuples, double weight) {
		//System.err.println("Constructing TupleProximityScoreModifier");
		this.tuples = tuples;
		this.weight = weight;
	}
	
	public Tuple[] getTuples() {
		return tuples;
	}
	
	public double getWeight() {
		return weight;
	}
	public String getName() {
		return "TupleProximityScoreModifier";
	}
	
	public TIntHashSet getCoOccurDocIdSet(int termid1, int termid2, InvertedIndex invIndex){
		TIntHashSet docidSet = new TIntHashSet();
		docidSet.addAll(invIndex.getDocuments(termid1)[0]);
		docidSet.retainAll(invIndex.getDocuments(termid2)[0]);
		return docidSet;
	}

	/**
	 * for each document in resultSet
	 *     for each tuple in tuples
		 *         compute tupleScore as the informativeness of the tuple in the
		 *           document based on the number of occurrences of the tuple in
		 *           windows of size windowSize in the document         
		 *         multiply the document tupleScore by the tuple weight
		 *         multiply the result by this DSM weight
		 *         combine the final result with the original document score
	 * 
	 * Modify the scores of the documents in the resultSet based on the
	 * frequency of each of the defined tuples in windows of size windowSize
	 * in each document of the set and on the defined tuple weight.
	 */
	// TODO 
	public boolean modifyScores(Index index, MatchingQueryTerms queryTerms,
			ResultSet resultSet) {
		//System.err.println("Start score modification");
//		System.out.println("  >> RUNNING TupleProximityScoreModifier");
//		for (Tuple tuple : tuples) {
//			System.out.println("   tuple: " + tuple);
//		}

		int[] docIds = resultSet.getDocids();
		double[] docScores = resultSet.getScores();
		
		int numDocs = docIds.length;
		//System.err.println("numDocs: "+numDocs);
		//System.err.println("resultSet.exactSize: "+resultSet.getExactResultSize());

		// return in case there are neither tuples nor documents to be boosted
		if (tuples.length == 0 || numDocs == 0) {
			return false;
		}
		
		boolean modified = false;
		
		double[] tupleScores = new double[numDocs];	
		Arrays.fill(tupleScores, 0d);
		
		Lexicon lexicon = index.getLexicon();
		
		for (Tuple tuple : tuples){
			System.err.println("Tuple: ("+tuple.getTerms()[0].getToken()+" "+tuple.getTerms()[1].getToken()+")^"+tuple.getWeight());
			TIntHashSet tupleDocidSet = getCoOccurDocIdSet(
					lexicon.getLexiconEntry(tuple.getTerms()[0].getToken()).termId, 
					lexicon.getLexiconEntry(tuple.getTerms()[1].getToken()).termId, 
					index.getInvertedIndex());
			for (int i = 0; i < numDocs; i++) {				
				// computes the document tuple score
				if (tupleDocidSet.contains(docIds[i]))
					tupleScores[i] += computeFDScore(tuple, docIds[i], index);
			}
		}
		int normIdx = 0;
		// updates the normalizing score index
		for (int i=1; i<numDocs; i++)
			if (tupleScores[i] > tupleScores[normIdx]) {
				normIdx = i;
			}
		
		// tuple score normalizer (i.e., s
		double normalizer = tupleScores[normIdx];
		
		// the actual size of the result set
		int size = 0;
		// for each document
		for (int i = 0; i < numDocs; i++) {
			// normalizes document tuple score
			tupleScores[i] /= normalizer;
			// further weigh the document tuple score with this DSM weight
			tupleScores[i] *= this.getWeight();
			
			if (tupleScores[i] > 0) {
				// updates the document score with its normalized tuple score
				docScores[i] = w_t * docScores[i] + w_u * tupleScores[i];	
				modified = true;
				
				// increments the number of non-zero scored documents in the result set
				if (docScores[i] > 0) {
					size++;
				}
			}
		}
		
		resultSet.setResultSize(size);
		//System.err.println("3");
		return modified;
	}
	
	/**
	 * Computes the score of a given tuple in the given document using the full
	 * full dependence model.
	 * 
	 * @param tuple
	 * @param resultSet
	 * @return tupleWeight * tupleScore
	 */
	private double computeFDScore(Tuple tuple, int docId, Index index) {

		CollectionStatistics collStats = index.getCollectionStatistics();
		DocumentIndex documentIndex = index.getDocumentIndex();
		InvertedIndex invertedIndex = index.getInvertedIndex();
		Lexicon lexicon = index.getLexicon();
		
		long numTokens = collStats .getNumberOfTokens();
		long numDocs = (long)(collStats.getNumberOfDocuments());
		double avgDocLen = ((double)(numTokens - numDocs * (windowSize - 1))) / (double)numDocs;		
		
		// tuple score 
		double score = 0;
		
		ExpansionTerm term1 = tuple.getTerms()[0];
		//System.err.println("1");
		// id of term1
		int term1Id = term1.getTermID();
		if (term1Id == -1)
			term1Id = lexicon.getLexiconEntry(term1.getToken()).termId;
		
		ExpansionTerm term2 = tuple.getTerms()[1];
		//System.err.println("2");
		// id of term2
		int term2Id = term2.getTermID();
		if (term2Id == -1)
			term2Id = lexicon.getLexiconEntry(term2.getToken()).termId;

		// list of postings for terms 1 and 2
		int[][] postings1 = getPostings(term1Id, invertedIndex);
		int[][] postings2 = getPostings(term2Id, invertedIndex);

		// number of postings for terms 1 and 2
		final int postings1Length = postings1[0].length;
		final int postings2Length = postings2[0].length;

		// looks for indices of doc k in postings of terms 1 and 2
		int index1 = Arrays.binarySearch(postings1[0], docId);
		int index2 = Arrays.binarySearch(postings2[0], docId);
		// skips doc k if it does not contain both terms 1 and 2
		if (index1 < 0 || index2 < 0){
			return 0;
		}

		// determines start and end boundaries for the list of blocks in
		// which term1 occurs in doc k
		int start1 = postings1[3][index1];
		int end1 = index1 == postings1Length - 1 ? postings1[4].length
				: postings1[3][index1 + 1];

		// determines start and end boundaries for the list of blocks in
		// which term2 occurs in doc k
		int start2 = postings2[3][index2];
		int end2 = index2 == postings2Length - 1 ? postings2[4].length
				: postings2[3][index2 + 1];

		// read number of tokens for doc k
		final int docLength = documentIndex.getDocumentLength(docId); 
		//System.err.println("4");
		// count number of co-occurences of terms with the given window size
		final int tupleFreq = Distance.noTimes(postings1[4], start1, end1, // blocks of term1
				                               postings2[4], start2, end2, // blocks of term2
				                               windowSize, // window size
				                               docLength);
		//System.err.println("5");
		// if we found matching ngrams, we score them
		boolean applyNorm2 = Boolean.parseBoolean(ApplicationSetup.getProperty("proximity.norm2.enabled", "false"));
		if (tupleFreq > 0) {

			final int numWindows = (docLength > 0 && docLength < windowSize) ? 1
					: docLength - windowSize + 1;
			final double background = applyNorm2 ? avgDocLen : numWindows;
			
			// apply Norm2 to pf
			final double matchingNGramsNormalised = applyNorm2
				? tupleFreq
					* Math.log(1 + ngramC * avgDocLen / numWindows)
					* REC_LOG_2
				: tupleFreq;

			double p = 1.0D / background;
			double q = 1.0d - p;
			double tmpScore = -gf
					.compute_log(background+ 1.0d)
					* REC_LOG_2 // /Math.log(2.0d)
					+ gf.compute_log(matchingNGramsNormalised + 1.0d)
					* REC_LOG_2 // /Math.log(2.0d)
					+ gf.compute_log(background
							- matchingNGramsNormalised + 1.0d)
					* REC_LOG_2// /Math.log(2.0d)
					- matchingNGramsNormalised
					* Math.log(p)
					* REC_LOG_2 // /Math.log(2.0d)
					- (background - matchingNGramsNormalised)
					* Math.log(q) * REC_LOG_2; // /Math.log(2.0d);

			tmpScore = tmpScore / (1.0d + matchingNGramsNormalised);

			if (Double.isInfinite(tmpScore) || Double.isNaN(tmpScore)) {
				System.err.println("docid: " + docId + ", docLength:"
						+ docLength + ", matchingNGrams: " + tupleFreq
						+ "matchingNGramsNormalised="
						+ matchingNGramsNormalised + ", avgdoclen = " + avgDocLen);
			}
			else {
				score += tuple.getWeight() * tmpScore;
			}
		}
		
		return score;
	}
			
	public int[][] getPostings(int termId, InvertedIndex invertedIndex) {
		int[][] postings = null;

		if (postingsCache.contains(termId)) {
			postings = (int[][])postingsCache.get(termId);
		}
		else {
			postings = invertedIndex.getDocuments(termId);

			//replace the block frequencies with the index of the blocks in the array
			final int docFrequency = postings[0].length;
			int blockFrequencySum = 0;
			int tmp;
			for (int i = 0; i<docFrequency; i++) {
				tmp = postings[3][i];
				postings[3][i] = blockFrequencySum;
				blockFrequencySum += tmp;
			}

			postingsCache.put(termId, postings);
		}
		return postings;
	}
	
	public Object clone() {
		return new TupleProximityScoreModifier(tuples, weight);
	}

}

package uk.ac.gla.terrier.matching.dsms;

import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectDoubleHashMap;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;

import uk.ac.gla.terrier.compression.BitFile;
import uk.ac.gla.terrier.matching.MatchingQueryTerms;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.statistics.GammaFunction;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Distance;
import uk.ac.gla.terrier.utility.Files;

/**
 * Modifies the scores of the documents using n-grams, approximating the
 * dependence of terms between documents.
 * <p>
 * implement as a document score modifier, similarly to PhraseScoreModifier
 * 
 * For each pair of query terms do for each document do find number of times for
 * which the terms co-occur in an n-gram and compute using the binomial and the
 * laplace aftereffect add the score to the corresponding document done done
 * 
 * <p>
 * <b>Properties</b>
 * <ul>
 * <li><tt>proximity.dependency.type</tt> - one of SD, FD for sequential
 * dependence or full dependence</li>
 * <li><tt>proximity.ngram.length</tt> - proxmity windows for FD, in tokens</li>
 * <li><tt>proximity.ngram.SD.length</tt> - proximity windows for SD, in
 * tokens</li>
 * <li><tt>proximity.w_t</tt> - weight of unigram in combination, defaults
 * 1.0d</li>
 * <li><tt>proximity.w_o</tt> - weight of SD in combination, default 1.0d</li>
 * <li><tt>proximity.w_u</tt> - weight of FD in combination, default 1.0d</li>
 * <li><tt>proximity.high.weight.terms.only</tt> - only consider query terms
 * with weight &gt;=1.0d. This maybe useful for applications with QE or
 * collection enrichment enabled. defaults to false.</li>
 * <li><tt>proximity.qtw.fnid</tt> - combination function to combine the qtws
 * of two terms involved in a phrase. See below.</li>
 * </ul>
 * <p>
 * <b>QTW Combination Functions</b>
 * <ol>
 * <li><tt>1</tt>: phraseQTW = 0.5 * (qtw1 + qtw2)</li>
 * <li><tt>2</tt>: phraseQTW = qtw1 * qtw2</li>
 * <li><tt>3</tt>: phraseQTW = min(qtw1, qtw2)</li>
 * <li><tt>4</tt>: phraseQTW = max(qtw1, qtw2)</li>
 * </ol>
 * 
 * @author Vassilis Plachouras
 * @version $Revision: 1.1 $
 */
public class ProximityScoreModifierMarkeredOFTREC2008 implements DocumentScoreModifier {
	public Object clone() {
		return new ProximityScoreModifierMarkeredOFTREC2008(phraseTerms);
	}

	/** Set of block delimiter terms */
	TObjectDoubleHashMap<String> subjDelimiterTermsWeights = new TObjectDoubleHashMap<String>();

	protected final GammaFunction gf = new GammaFunction();
	protected static final double REC_LOG_2 = 1.0d / Math.log(2.0d);
	
	protected boolean applyNorm2 = false;

	protected Index index = null;
	protected DocumentIndex doi;
	protected long numTokens;
	protected long numDocs;
	protected double avgDocLen;
	protected double avgDocLen_SD;
	protected Lexicon lexicon;

	/** The size of the considered ngrams (aka window size) */
	protected int ngramLength = Integer.parseInt(ApplicationSetup.getProperty(
			"proximity.ngram.length", "2"));
	protected int ngramLength_SD = Integer.parseInt(ApplicationSetup
			.getProperty("proximity.ngram.SD.length", "2"));
	/** type of proximity to use */
	protected String dependency = ApplicationSetup.getProperty(
			"proximity.dependency.type", "");
	protected boolean onlyHighWeightQTerms = Boolean
			.parseBoolean(ApplicationSetup.getProperty(
					"proximity.high.weight.terms.only", "false"));
	protected final int phraseQTWfnid = Integer.parseInt(ApplicationSetup
			.getProperty("proximity.qtw.fnid", "1"));

	/** weight of unigram model */
	protected double w_t = Double.parseDouble(ApplicationSetup.getProperty(
			"proximity.w_t", "1.0d"));
	/** weight of ordered dependence model */
	protected double w_o = Double.parseDouble(ApplicationSetup.getProperty(
			"proximity.w_o", "1.0d"));
	/** weight of unordered dependence model */
	protected double w_u = Double.parseDouble(ApplicationSetup.getProperty(
			"proximity.w_u", "1.0d"));

	/** A list of the strings of the phrase terms. */
	protected String[] phraseTerms;
	

	public ProximityScoreModifierMarkeredOFTREC2008() {

		for (String delim : ApplicationSetup
				.getProperty("block.delimiters", "").trim().toLowerCase()
				.split("\\s*,\\s*")) {
			
			final double weight = Double.parseDouble(ApplicationSetup.getProperty(
							"proximity.marker.terms." + delim + ".weight", "0.0"));
			//ignore marker terms with weight 0	
			if (weight > 0) {
				subjDelimiterTermsWeights.put(delim, weight);
			}
		}
		
		phraseTerms = subjDelimiterTermsWeights.keys(new String[0]);
		
	}
	
	protected int[] docLengthsInBlocks = null;
	protected double avgDocLengthInBlocks = 0;
	boolean initialised = false;
	protected void init(Index index)
	{
		if (initialised)
			return;
		initialised = true;
		
		try{
			final int numDocs = index.getCollectionStatistics().getNumberOfDocuments();
			docLengthsInBlocks = new int[numDocs];
			final String blockLengthsFile = ApplicationSetup.getProperty("proximity.block.lengths", null);
			final BufferedReader br = Files.openFileReader(blockLengthsFile);
			String line = null; int i=0;
			while((line = br.readLine())!= null)
			{
				docLengthsInBlocks[i] = Integer.parseInt(line);
				avgDocLengthInBlocks += (double)docLengthsInBlocks[i];
				i++;
			}
			avgDocLengthInBlocks = avgDocLengthInBlocks/ (double)numDocs;
		
		} catch (Exception e) {
			System.err.println("Error loading blockLengths file:"+e);
		}
	}

	public ProximityScoreModifierMarkeredOFTREC2008(final String[] pTerms) {
		phraseTerms = pTerms;
	}
	
//	public ProximityScoreModifierTREC2008(final String[] pTerms, boolean r) {
//		this(pTerms);
//	}

	/**
	 * Returns the name of the modifier.
	 * 
	 * @return String the name of the modifier.
	 */
	public String getName() {
		return "ProximityScoreModifierTREC2008";
	}

	TIntObjectHashMap postingsCache = new TIntObjectHashMap();

	/**
	 * Computes full dependency score between pair (term1, term2).
	 * 
	 * @param term1
	 * @param term2
	 * @param scores
	 * @param score_u
	 * @param docids
	 * @param phraseTermWeight1
	 * @param phraseTermWeight2
	 * @param ngramC
	 * @param ngramLength
	 * @param index
	 */
	protected void computeFDScore(String term1, String term2, double[] scores,
			double[] score_u, int[] docids, double phraseTermWeight1,
			double phraseTermWeight2, double ngramC, int ngramLength,
			Index index) {

		double combinedPhraseQTWWeight;

		switch (phraseQTWfnid) {
		case 1:
			combinedPhraseQTWWeight = 0.5d * phraseTermWeight1 + 0.5d
					* phraseTermWeight2;
			break;
		case 2:
			combinedPhraseQTWWeight = phraseTermWeight1 * phraseTermWeight2;
			break;
		case 3:
			combinedPhraseQTWWeight = Math.min(phraseTermWeight1,
					phraseTermWeight2);
			break;
		case 4:
			combinedPhraseQTWWeight = Math.max(phraseTermWeight1,
					phraseTermWeight2);
			break;
		default:
			combinedPhraseQTWWeight = 1.0d;
		}
		lexicon.findTerm(term1);
		int termid1 = lexicon.getTermId();
		lexicon.findTerm(term2);
		int termid2 = lexicon.getTermId();

		System.err.println("term 1 and 2: " + term1 + " (" + termid1 + ") "
				+ term2 + " (" + termid2 + ")");

		int[][] postings1 = getPostings(termid1, index);
		int[][] postings2 = getPostings(termid2, index);

		// number of postings for terms 1 and 2
		final int postings1Length = postings1[0].length;
		final int postings2Length = postings2[0].length;

		final int numDocs = docids.length;

		// for each document k in the collection
		for (int k = 0; k < numDocs; k++) {

			// looks for indices of doc k in postings of terms 1 and 2
			int index1 = Arrays.binarySearch(postings1[0], docids[k]);
			int index2 = Arrays.binarySearch(postings2[0], docids[k]);
			// skips doc k if it does not contain both terms 1 and 2
			if (index1 < 0 || index2 < 0)
				continue;

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

			// read number of blocks for doc k
			final int docLengthInBlocks = docLengthsInBlocks[docids[k]]; 
				
			// count number of co-occurences of terms with the given window size
			final int matchingNGrams = Distance.noTimes(postings1[4], start1,
					end1, // blocks of term1
					postings2[4], start2, end2, // blocks of term2
					ngramLength, // window size
					docLengthInBlocks);

			// if we found matching ngrams, we score them
			//final boolean applyNorm2 = false;
			if (matchingNGrams > 0) {

				final int numberOfNGrams = (docLengthInBlocks > 0 && docLengthInBlocks < ngramLength) ? 1
						: docLengthInBlocks - ngramLength + 1;
				final double background = applyNorm2 ? avgDocLengthInBlocks : numberOfNGrams;
				
				
				// apply Norm2 to pf
				final double matchingNGramsNormalised = applyNorm2
					? matchingNGrams
						* Math.log(1 + ngramC * avgDocLengthInBlocks / numberOfNGrams)
						* REC_LOG_2
					: matchingNGrams;

				double p = 1.0D / background;
				double q = 1.0d - p;
				double score = -gf
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

				score = score / (1.0d + matchingNGramsNormalised);

				if (Double.isInfinite(score) || Double.isNaN(score)) {
					System.err.println("docid: " + docids[k] + ", docLength:"
							+ docLengthInBlocks + ", matchingNGrams: " + matchingNGrams
							+ "matchingNGramsNormalised="
							+ matchingNGramsNormalised + ", original score: "
							+ scores[k] + "avgdoclen = " + avgDocLengthInBlocks);
				} else
					score_u[k] += combinedPhraseQTWWeight * score;
			}
		}

	}

	protected void computeSDScore(String term1, String term2, double[] scores,
			double[] score_o, int[] docids, double phraseTermWeight1,
			double ngramC, double phraseTermWeight2, Index index) {
		double combinedPhraseQTWWeight;
		switch (phraseQTWfnid) {
		case 1:
			combinedPhraseQTWWeight = 0.5 * phraseTermWeight1 + 0.5
					* phraseTermWeight2;
			break;
		case 2:
			combinedPhraseQTWWeight = phraseTermWeight1 * phraseTermWeight2;
			break;
		case 3:
			combinedPhraseQTWWeight = Math.min(phraseTermWeight1,
					phraseTermWeight2);
			break;
		case 4:
			combinedPhraseQTWWeight = Math.max(phraseTermWeight1,
					phraseTermWeight2);
			break;
		default:
			combinedPhraseQTWWeight = 1.0d;
		}

		lexicon.findTerm(term1);
		int termid1 = lexicon.getTermId();
		lexicon.findTerm(term2);
		int termid2 = lexicon.getTermId();

		lexicon.findTerm(term1);
		termid1 = lexicon.getTermId();
		lexicon.findTerm(term2);
		termid2 = lexicon.getTermId();

		System.err.println("term 1 and 2: " + term1 + " (" + termid1 + ") "
				+ term2 + " (" + termid2 + ")");

		int[][] postings1 = getPostings(termid1, index);
		int[][] postings2 = getPostings(termid2, index);
		int[] postings14 = postings1[4];
		int[] postings24 = postings2[4];
		// find the documents that contain term1 and term2
		final int postings1Length = postings1[0].length;
		final int postings2Length = postings2[0].length;

		TIntIntHashMap ngramFrequencies = new TIntIntHashMap(docids.length);
		int TF = 0;
		final int docidsLength = docids.length;
		for (int k = 0; k < docidsLength; k++) {
			int index1 = Arrays.binarySearch(postings1[0], docids[k]);
			int index2 = Arrays.binarySearch(postings2[0], docids[k]);
			if (index1 < 0 || index2 < 0)
				continue;

			// find the places where the terms co-occur closely together
			int start1 = postings1[3][index1];
			int end1 = index1 == postings1Length - 1 ? postings1[4].length
					: postings1[3][index1 + 1];

			int start2 = postings2[3][index2];
			int end2 = index2 == postings2Length - 1 ? postings2[4].length
					: postings2[3][index2 + 1];
			final int docLength = doi.getDocumentLength(docids[k]);
			final int matchingNGrams2 = Distance.noTimesSameOrder(postings1[4],
					start1, end1, postings2[4], start2, end2, ngramLength,
					docLength);
			if (matchingNGrams2 > 0) {
				final int numberOfNGrams = (docLength > 0 && docLength < ngramLength_SD) ? 1
						: docLength - ngramLength_SD + 1;
				final double matchingNGramsNormalised = matchingNGrams2
						* Math.log(1 + ngramC * avgDocLen_SD / numberOfNGrams)
						* REC_LOG_2;// /Math.log(2.0D);
				double p = 1.0D / /* numberOfNGrams */avgDocLen_SD;
				double q = 1.0d - p;
				double score = -gf
						.compute_log(/* numberOfNGrams */avgDocLen_SD + 1.0d)
						* REC_LOG_2// /Math.log(2.0d)
						+ gf.compute_log(matchingNGramsNormalised + 1.0d)
						* REC_LOG_2// /Math.log(2.0d)
						+ gf.compute_log(/* numberOfNGrams */avgDocLen_SD
								- matchingNGramsNormalised + 1.0d)
						* REC_LOG_2// /Math.log(2.0d)
						- matchingNGramsNormalised
						* Math.log(p)
						* REC_LOG_2// /Math.log(2.0d)
						- (/* numberOfNGrams */avgDocLen_SD - matchingNGramsNormalised)
						* Math.log(q) * REC_LOG_2; // /Math.log(2.0d);

				score = score / (1.0d + matchingNGramsNormalised);

				if (Double.isInfinite(score) || Double.isNaN(score)) {
					System.err.println("docid: " + docids[k] + ", docLength:"
							+ docLength + ", matchingNGrams2: "
							+ matchingNGrams2 + "matchingNGramsNormalised="
							+ matchingNGramsNormalised + ", original score: "
							+ scores[k]);
				} else
					score_o[k] += combinedPhraseQTWWeight;

			}

		}
		// postings1 = null; postings2 = null;

	}

	/**
	 * Modifies the scores of documents, in which there exist, or there does not
	 * exist a given phrase.
	 * 
	 * @param index
	 *            Index the data structures to use.
	 * @param terms
	 *            MatchingQueryTerms the terms to be matched for the query. This
	 *            does not correspond to the phrase terms necessarily, but to
	 *            all the terms of the query.
	 * @param set
	 *            ResultSet the result set for the query.
	 * @return true if any scores have been altered
	 */
	public boolean modifyScores(Index index, MatchingQueryTerms terms,
			ResultSet set) {
    // Reads the list of subjective markers from the app properties.
		for (String delim : ApplicationSetup
				.getProperty("block.delimiters", "").trim().toLowerCase()
				.split("\\s*,\\s*")) {
			
			final double weight = Double.parseDouble(ApplicationSetup.getProperty(
							"proximity.marker.terms." + delim + ".weight", "0.0"));
			//ignore marker terms with weight 0	
			if (weight > 0) {
				subjDelimiterTermsWeights.put(delim, weight);
			}
		}
		
		phraseTerms = subjDelimiterTermsWeights.keys(new String[0]);		
		/* NOTE: Selects query term weight (QTW) combination function */

		if (phraseQTWfnid < 1 || phraseQTWfnid > 4) {
			System.err
					.println("ERROR: Wrong function id specified for ProximityScoreModifierTREC2008");
		}
		if (this.index == null) {
			init(index);
			
			this.index = index;
			doi = index.getDocumentIndex();
			final CollectionStatistics collStats = index
					.getCollectionStatistics();
			numTokens = collStats.getNumberOfTokens();
			numDocs = (long) (collStats.getNumberOfDocuments());
			avgDocLen = ((double) (numTokens - numDocs * (ngramLength - 1)))
					/ (double) numDocs;
			avgDocLen_SD = ((double) (numTokens - numDocs
					* (ngramLength_SD - 1)))
					/ (double) numDocs;
			lexicon = index.getLexicon();
		}

		/* NOTE: Uni-gram model weight */

		w_t = Double.parseDouble(ApplicationSetup.getProperty("proximity.w_t",
				"1.0d"));

		// get local references for the document ids and the
		// scores of documents from the result set.
		double[] scores = set.getScores();
		int[] docids = set.getDocids();

		// ordered dependence scores
		double[] score_o = new double[scores.length];
		// unordered dependence scores
		double[] score_u = new double[scores.length];
		// phraseterm - opinionTerm dependence scores
		double[] score_op = new double[scores.length];

		// the number of terms in the phrase
		phraseTerms = terms.getTerms();

		/* NOTE: Considers proximity among highly weighted query terms only */

		if (onlyHighWeightQTerms) {
			ArrayList<String> proxterms = new ArrayList<String>();
			for (String t : phraseTerms) {
				if (terms.getTermWeight(t) >= 1.0d)
					proxterms.add(t);
			}
			phraseTerms = proxterms.toArray(new String[0]);
		}
		final int phraseLength = phraseTerms.length;
		final double[] phraseTermWeights = new double[phraseLength];
		for (int i = 0; i < phraseLength; i++) {
			phraseTermWeights[i] = terms.getTermWeight(phraseTerms[i]);
		}
		if (phraseLength == 0)
			System.err.println("Warning: no phrase term chosen.");
		String term1 = null;
		String term2 = null;
		
		final double ngramC = Double.parseDouble(ApplicationSetup.getProperty(
				"proximity.ngram.c", "1.0d"));
		applyNorm2 = Boolean.parseBoolean(ApplicationSetup.getProperty("proximity.marked.applynorm2", "false"));

		/* NOTE: Proximity model weights */
		/* Full, unordered dependency */
		if (dependency.equals("FD")) {
			w_u = Double.parseDouble(ApplicationSetup.getProperty(
					"proximity.w_u", "1.0d"));
			System.err.println("w_t: " + w_t + ", w_u: " + w_u + ", fnid: "
					+ phraseQTWfnid + ", ngramc: " + ngramC +", ngramLength: "+ngramLength);
		}
		/* Sequential, ordered dependency */
		else if (dependency.equals("SD")) {
			w_o = Double.parseDouble(ApplicationSetup.getProperty(
					"proximity.w_o", "1.0d"));
			System.err.println("w_t: " + w_t + ", w_o: " + w_o + ", fnid: "
					+ phraseQTWfnid + ", ngramc: " + ngramC +", ngramLength: "+ngramLength);
		}
		if (applyNorm2)
			System.err.println("Normalisation is applied.");
		else
			System.err.println("No normalisation applied.");

		/*
		 * NOTE: Computes the proximity model score for each document and
		 * updates the score of every document in the result set
		 */

		// query term vs marker term instead of pair of query terms
		if (dependency.equals("FD")) {
			for (int i = 0; i < phraseLength; i++) {
				for (String delim : subjDelimiterTermsWeights
						.keys(new String[0])) {
					term1 = phraseTerms[i];
					System.err.println("Prox between "+term1+ "("+phraseTermWeights[i]+") and "+delim+"("+subjDelimiterTermsWeights.get(delim)+")");
					this.computeFDScore(term1, delim, scores, score_u, docids,
							phraseTermWeights[i], subjDelimiterTermsWeights
									.get(delim), ngramC, ngramLength, index);
				}
			}

			for (int k = 0; k < docids.length; k++) {
				scores[k] = w_t * scores[k] + w_u * score_u[k];
			}

			postingsCache.clear();

		} else if (dependency.equals("SD")) {
			for (int i = 0; i < phraseLength - 1; i++) {
				for (int j = i + 1; j < phraseLength; j++) {
					term1 = phraseTerms[i];
					term2 = phraseTerms[j];
					this.computeFDScore(term1, term2, scores, score_u, docids,
							phraseTermWeights[i], phraseTermWeights[j], ngramC,
							ngramLength, index);
				}
			}

			for (int k = 0; k < docids.length; k++) {
				scores[k] = w_t * scores[k] + w_o * score_u[k];
			}

			postingsCache.clear();

		} else {
			System.err
					.println("WARNING: proximity.dependency.type not set. Set it to either FD or SD");
			return false;
		}
		// returning true, assuming that we have modified the scores of
		// documents
		return true;
	}

	/**
	 * @param termid
	 * @param index
	 * @return List of 0 &lt;= d &lt; n postings for the given termid in the
	 *         form: postings[0][d] : id of document d postings[1][d] :
	 *         frequency of term in document d postings[2][d] : field scores of
	 *         term in document d postings[3][d] : left offset x for list of
	 *         (y-x) blocks in document d [3][d+1] : left offset y for list of
	 *         blocks in document d+1 postings[4][x,y-1]: list of blocks in
	 *         which term occurs within document d
	 */
	protected int[][] getPostings(int termid, Index index) {

		int[][] postings = null;

		if (postingsCache.contains(termid)) {
			postings = (int[][]) postingsCache.get(termid);
		} else {
			postings = index.getInvertedIndex().getDocuments(termid);

			// replace the block frequencies with the index of the blocks in the
			// array
			final int docFrequency = postings[0].length;
			int blockFrequencySum = 0;
			int tmp;
			for (int i = 0; i < docFrequency; i++) {
				tmp = postings[3][i];
				postings[3][i] = blockFrequencySum;
				blockFrequencySum += tmp;
			}

			postingsCache.put(termid, postings);
		}
		return postings;
	}

}

package uk.ac.gla.terrier.matching.dsms;

import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntIntHashMap;
import java.util.Arrays;

import uk.ac.gla.terrier.statistics.GammaFunction;
import uk.ac.gla.terrier.matching.MatchingQueryTerms;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.utility.ApplicationSetup;

/**
 * Modifies the scores of the documents using n-grams, approximating
 * the dependence of terms between documents.
 * 
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
 * @author Vassilis Plachouras
 * @version $Revision: 1.1 $
 */
public class ProximityScoreModifier implements DocumentScoreModifier {

	protected GammaFunction gf = new GammaFunction();
	
	/** The size of the considered ngrams */
	protected int ngramLength;
	

  
  protected double l_t;
	
	protected	double l_u;

	/** A list of the strings of the phrase terms. */
	protected String[] phraseTerms;

	/**
	 * Indicates whether the phrase should appear in the retrieved documents, or
	 * not. The default value is true.
	 */
	protected boolean required = true;

	public ProximityScoreModifier() {
	}
	
	public ProximityScoreModifier(String[] pTerms) {
		phraseTerms = pTerms;
	}

	public ProximityScoreModifier(String[] pTerms, boolean r) {
		this(pTerms);
		required = r;
	}

	/**
	 * Returns the name of the modifier.
	 * 
	 * @return String the name of the modifier.
	 */
	public String getName() {
		return "ProximityScoreModifier";
	}

	TIntObjectHashMap postingsCache = new TIntObjectHashMap();
	
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
	public boolean modifyScores(Index index, MatchingQueryTerms terms, ResultSet set) {

		//get local references for the document ids and the
		//scores of documents from the result set.
		double[] scores = set.getScores();
		int[] docids = set.getDocids();
		ngramLength = Integer.parseInt(ApplicationSetup.getProperty("proximity.ngram.length","2"));
		l_t = Double.parseDouble(ApplicationSetup.getProperty("l_t","1.0d"));
		l_u = Double.parseDouble(ApplicationSetup.getProperty("l_u","1.0d"));

//////// begin score

    double[] score_o = new double[scores.length];
    double[] score_u = new double[scores.length];

//////// end score


		//the number of terms in the phrase
		//phraseTerms = terms.getTerms();
    	int topx = Integer.parseInt(ApplicationSetup.getProperty("top.phrase.terms", "0"));
    	phraseTerms = (topx>0)?(terms.getTermsWithHighestWeights(topx)):(terms.getTerms());
    	
		int phraseLength = phraseTerms.length;

		for (int i=0; i<phraseLength; i++) 
			System.err.println("phrase term: " + phraseTerms[i]);
		
		String term1 = null;
		String term2 = null;
		int termid1;
		int termid2;
		int[][] postings1 = null;
		int[][] postings2 = null;
		
		CollectionStatistics collStat = index.getCollectionStatistics();
		long numberOfTokens = collStat.getNumberOfTokens();
		int numberOfDocuments = collStat.getNumberOfDocuments();
		//long tokens = 126374125L; 
		//int docs = 370715;
		//double avgDocLen = (double)(tokens - docs*(ngramLength-1)) / docs;
		//double avgDocLen = (2751951739L - 3215171L*(ngramLength-1)) / 3215171L;
		//double avgDocLen = (2754094433L - 3215171L*(ngramLength-1)) / 3215171L;
		double avgDocLen = (double)(numberOfTokens-numberOfDocuments*(ngramLength-1))/numberOfDocuments;
		double ngramC = Double.parseDouble(ApplicationSetup.getProperty("proximity.ngram.c","1.0d"));
		
		System.out.println("average document length: "+avgDocLen);
		System.out.println("l_t: "+l_t+", l_u: "+l_u+", c: "+ngramC+", l:"+ngramLength);
		
		for (int i=0; i<phraseLength-1; i++) {
			for (int j=i+1; j<phraseLength; j++) {
				term1 = phraseTerms[i];
				term2 = phraseTerms[j];
								
				
				index.getLexicon().findTerm(term1);
				termid1 = index.getLexicon().getTermId();
				index.getLexicon().findTerm(term2);
				termid2 = index.getLexicon().getTermId();
				
				System.err.println("term 1 and 2: " + term1 + " ("+termid1+") " + term2 + " ("+termid2+")");
				
				postings1 = getPostings(termid1, index);
				postings2 = getPostings(termid2, index);
				int[] postings14 = postings1[4];
				int[] postings24 = postings2[4];
				//find the documents that contain term1 and term2
				final int postings1Length = postings1[0].length;
				final int postings2Length = postings2[0].length;
				
				TIntIntHashMap ngramFrequencies = new TIntIntHashMap(docids.length);
				int TF = 0;
				final int docidsLength = docids.length;
				int matchingNGrams;
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
					
					matchingNGrams = 0;
					for (int k1=start1; k1<end1; k1++) {
						for (int k2=start2; k2<end2; k2++) {
					
							if (Math.abs(postings14[k1]-postings24[k2])<ngramLength) {
								matchingNGrams++;
								
							}
						}
					}

					
					//if we found matching ngrams, we score them
					if (matchingNGrams > 0) {
						
						int docLength = index.getDocumentIndex().getDocumentLength(docids[k]);
						double matchingNGramsNormalised = matchingNGrams * Math.log(1+ngramC*avgDocLen/docLength)/Math.log(2.0D);

						int numberOfNGrams = docLength - ngramLength + 1;
						double p = 1.0D / numberOfNGrams;
						double q = 1.0d - p;
						double score = - gf.compute_log(numberOfNGrams+1.0d)/Math.log(2.0d) 
						               + gf.compute_log(matchingNGramsNormalised+1.0d)/Math.log(2.0d)
						               + gf.compute_log(numberOfNGrams - matchingNGramsNormalised + 1.0d)/Math.log(2.0d)
						               - matchingNGramsNormalised*Math.log(p)/Math.log(2.0d)
						               - (numberOfNGrams - matchingNGramsNormalised)*Math.log(q)/Math.log(2.0d);
						
						score = score / (1.0d + matchingNGramsNormalised);
						
						if (Double.isInfinite(score) || Double.isNaN(score)) {
							System.err.println("docid: " + docids[k] + ", docLength:" + docLength + ", matchingNGrams: " + matchingNGrams + ", original score: " + scores[k]);
						} else
						
              
              score_u[k] +=score;

		
					}
					
					
				
					
				
				
				}
				ngramFrequencies.clear();
			}
		}
		//System.err.println("docids.length="+docids.length);
		for (int k=0; k<docids.length; k++) {	
			//System.err.println("l_t: "+l_t+", "+scores[k]+", "+score_u[k]);
			scores[k] =  l_t * scores[k] +  l_u * score_u[k] ;		
		}
	
		
		postingsCache.clear();
		
		//returning true, assuming that we have modified the scores of documents
		
		return true;
	}
	
	protected int[][] getPostings(int termid, Index index) {
		
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
			
			postingsCache.put(termid, postings);
		}
		return postings;
	}

	public Object clone()
	        {
		                        return this;
					        }
}

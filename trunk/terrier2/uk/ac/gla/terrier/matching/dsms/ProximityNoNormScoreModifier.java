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
import uk.ac.gla.terrier.utility.Distance;
import uk.ac.gla.terrier.structures.DocumentIndex;


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
public class ProximityNoNormScoreModifier implements DocumentScoreModifier {
	public Object clone()
	{
		return new ProximityNoNormScoreModifier(phraseTerms);
	}

	protected final GammaFunction gf = new GammaFunction();
	protected static final double REC_LOG_2 = 1.0d / Math.log(2.0d);

  
  

	/** A list of the strings of the phrase terms. */
	protected String[] phraseTerms;

	/**
	 * Indicates whether the phrase should appear in the retrieved documents, or
	 * not. The default value is true.
	 */
	protected boolean required = true;

	public ProximityNoNormScoreModifier() {
		
	}
	
	public ProximityNoNormScoreModifier(String[] pTerms) {
		phraseTerms = pTerms;
	}

	public ProximityNoNormScoreModifier(String[] pTerms, boolean r) {
		this(pTerms);
		required = r;
	}

	/**
	 * Returns the name of the modifier.
	 * 
	 * @return String the name of the modifier.
	 */
	public String getName() {
		return "ProximityNoNormScoreModifier";
	}

	TIntObjectHashMap postingsCache = new TIntObjectHashMap();
	
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

		//get local references for the document ids and the
		//scores of documents from the result set.
		double[] scores = set.getScores();
		int[] docids = set.getDocids();
		
		/** The size of the considered ngrams */
		String dependency = ApplicationSetup.getProperty("dependency.type","FD");
		int ngramLength = Integer.parseInt(ApplicationSetup.getProperty("proximity.ngram.length","2"));
		int ngramLength_SD = Integer.parseInt(ApplicationSetup.getProperty("proximity.ngram.SD.length","2"));
		
		double w_t = Double.parseDouble(ApplicationSetup.getProperty("w_t","1.0d"));
		double w_o = Double.parseDouble(ApplicationSetup.getProperty("w_o","1.0d"));
		double w_u = Double.parseDouble(ApplicationSetup.getProperty("w_u","1.0d"));
		
		final double ngramC = Double.parseDouble(ApplicationSetup.getProperty("proximity.ngram.c","1.0d"));
		if (dependency.equals("FD"))
			System.out.println("type: "+dependency+", nl: "+ngramLength+", nc: "+ngramC);
		else if (dependency.equals("SD"))
			System.out.println("type: "+dependency+", nl: "+ngramLength_SD+", nc: "+ngramC);


//////// begin score

	double[] score_o = new double[scores.length];
	double[] score_u = new double[scores.length];

//////// end score


		//the number of terms in the phrase
		phraseTerms = terms.getTerms();
		int phraseLength = phraseTerms.length;

		for (int i=0; i<phraseLength; i++) 
			System.err.println("phrase term: " + phraseTerms[i]);
		
		String term1 = null;
		String term2 = null;
		int termid1;
		int termid2;
		int[][] postings1 = null;
		int[][] postings2 = null;
		
		final DocumentIndex doi = index.getDocumentIndex();
		final CollectionStatistics collStats = index.getCollectionStatistics();
		long numTokens = collStats.getNumberOfTokens();
		long numDocs = (long)(collStats.getNumberOfDocuments());
		final double avgDocLen =  (numTokens - numDocs *(ngramLength-1)) / numDocs;
		final double avgDocLen_SD = (numTokens - numDocs*(ngramLength_SD - 1)) / numDocs;		
		
		
		if (dependency.equals("FD")){
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

					//noTimes(final int[] blocksOfTerm1, int start1, int end1, final int[] blocksOfTerm2, int start2, int end2, final int windowSize, final int documentLengthInTokens)
				
					final int docLength = doi.getDocumentLength(docids[k]);	
					int matchingNGrams = Distance.noTimes(postings1[4], start1, end1,  postings2[4], start2, end2, ngramLength, docLength);
					/*
		  			int matchingNGrams = 0;
		  			for (int k1=start1; k1<end1; k1++) {
			   			for (int k2=start2; k2<end2; k2++) {
					
			   				if (Math.abs(postings14[k1]-postings24[k2])<ngramLength) {
					   			matchingNGrams++;
								
			   				}
			   			}
						}
			 		*/
			   		//if we found matching ngrams, we score them
			   		if (matchingNGrams > 0) {
						
						final int numberOfNGrams = (docLength>0 && docLength < ngramLength)
							? 1
							: docLength - ngramLength + 1;
			   			final double matchingNGramsNormalised = matchingNGrams; 
			   			//* Math.log(1+ngramC*avgDocLen/numberOfNGrams)*REC_LOG_2; // /Math.log(2.0D);

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
							score_u[k] +=score;		
						}
									
				  }
				  ngramFrequencies.clear();
	   		  }
			 }
		
				for (int k=0; k<docids.length; k++) {		
				scores[k] =  w_t * scores[k] +  w_u * score_u[k] ;		
				}
			
	  		   postingsCache.clear();
			 		   	  
	  		  }
		else if (dependency.equals("SD")){
		
		 	 	for (int i=0; i<phraseLength-1; i++) {

					int j=i+1;
	
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
						int matchingNGrams2 = Distance.noTimesSameOrder(postings1[4], start1, end1,  postings2[4], start2, end2, ngramLength, docLength);

			  			/*int matchingNGrams2 = 0;
			  			for (int k1=start1; k1<end1; k1++) {
			  				for (int k2=start2; k2<end2; k2++) {
			  					//System.err.println("positions: " + postings1[4][k1] + " " + postings2[4][k2]);
								if (((postings24[k2]-postings14[k1])<ngramLength_SD) & ((postings24[k2]-postings14[k1])>0)){
									matchingNGrams2++;
									//System.err.println("found matching ngram. Total number is " + matchingNGrams);
			  					}
							}
			  			}*/
					
					
						if (matchingNGrams2 > 0) {
						final int numberOfNGrams = (docLength>0 && docLength < ngramLength_SD)
							? 1
							: docLength - ngramLength_SD + 1;
						final double matchingNGramsNormalised = matchingNGrams2; 
						//* Math.log(1+ngramC*avgDocLen_SD/numberOfNGrams)*REC_LOG_2;// /Math.log(2.0D);
						
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
									
				   score_o[k] += score;
					
		  			}
			
	   			}
	   			ngramFrequencies.clear();
			}
		
			  for (int k=0; k<docids.length; k++) {		
				scores[k] =  w_t * scores[k] + w_o * score_o[k] ;		
			  }
		
		postingsCache.clear();		
		
	  }
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

}

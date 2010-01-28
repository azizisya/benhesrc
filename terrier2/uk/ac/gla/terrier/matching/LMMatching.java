/*
 * Terrier - Terabyte Retriever 
 * Webpage: http://ir.dcs.gla.ac.uk/terrier 
 * Contact: terrier{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * http://www.gla.ac.uk/
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is LMMatching.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk> (original author)
 */
package uk.ac.gla.terrier.matching;
import java.util.Arrays;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.matching.dsms.DocumentScoreModifier;
import uk.ac.gla.terrier.matching.models.languagemodel.LanguageModel;
import uk.ac.gla.terrier.matching.tsms.TermScoreModifier;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.indexing.DocumentInitialWeightIndex;
import uk.ac.gla.terrier.structures.indexing.TermEstimateIndex;
import uk.ac.gla.terrier.utility.HeapSort;
/**
 * Performs the matching of documents for language modelling with a query, by
 * assigning scores to documents for each query in the lexicon according to the
 * term's occurrences in the document and in the query.
 * @author Ben He
 * @version $Revision: 1.1 $ 
 */
public class LMMatching extends Matching {
	protected static Logger logger = Logger.getRootLogger();
	/** The language model for document weighting. */
	protected LanguageModel wmodel;
	
	/** The term fequency in each document of each query term. */
	protected double[][] tf;
	
	/** The initial document weights loaded from DocumentInitialWeightIndex. */
	protected double[] initialScores;
	/** The TermEstimateIndex for assigning scores. */
	protected TermEstimateIndex termEstimateIndex;
	
	/** 
	 * The term estimate for each query term. The length of the 
	 * array is equal to query length.
	 */
	protected double[] termEstimates;
	
	/** 
	 * Indicates whether a document contains 
	 * at least one query term or not.
	 */
	protected boolean[] retrieved;
	
	/** 
	 * The term frequency of each query term. The length of the
	 * array is equal to query length.
	 */
	protected double[] termFrequency;
	/**
	 * Default constructor.
	 * @param index Index for retrieval.
	 */
	public LMMatching(Index index) {
		super(index);
		int numberOfDocs = collectionStatistics.getNumberOfDocuments();
		initialScores = new double[numberOfDocs];
		if (! index.hasIndexStructure("documentinitialweight"))
		{
			logger.fatal("LMMatching will not work as the Index does not have a documentinitialweight structure. Did you run trec_terrier.sh -i -l ?"); 
		}
		DocumentInitialWeightIndex weightIndex = 
			(DocumentInitialWeightIndex)index.getIndexStructure("documentinitialweight");
		for (int i = 0; i < numberOfDocs; i++)
			initialScores[i] = weightIndex.getDocumentInitialWeight(i);
		if (! index.hasIndexStructure("termestimate"))
		{
			 logger.fatal("LMMatching will not work as the Index does not have a termestimate structure. Did you run trec_terrier.sh -i -l ?");
		}
		
		this.termEstimateIndex = (TermEstimateIndex)index.getIndexStructure("termestimate");
		this.retrieved = new boolean[numberOfDocs];
	}
	
	/** 
	 * Initialise before retrieval process for a query taking place.
	 * @param querylength int the number of unique terms in the query.
	 */
	protected void initialise(int querylength){
		this.initialise((double[])initialScores.clone());
		//resultSet.scores = (double[])initialScores.clone();
		tf = new double[collectionStatistics.getNumberOfDocuments()][querylength];
		for (int i = 0; i < querylength; i++)
			Arrays.fill(tf[i], 0d);
		Arrays.fill(this.retrieved, false);
		this.termFrequency = new double[querylength];
		this.termEstimates = new double[querylength];
	}
	
	/**
	 * Implements the matching of a query with the documents.
	 * @param queryNumber the identifier of the processed query.
	 * @param queryTerms the query terms to be processed.
	 */
	public void match(String queryNumber, MatchingQueryTerms queryTerms) {
		this.initialise(queryTerms.length());
		
		//load in the dsms
		DocumentScoreModifier[] dsms; int NumberOfQueryDSMs = 0;
		dsms = queryTerms.getDocumentScoreModifiers();
		if (dsms!=null)
			NumberOfQueryDSMs = dsms.length;
		
		//and prepare for the tsms
		TermScoreModifier[] tsms; //int NumberOfQueryTSMs = 0;
		String[] queryTermStrings = queryTerms.getTerms();
		//check whether we need to match an empty query.
		//if so, then return the existing result set.
		if (MATCH_EMPTY_QUERY && queryTermStrings.length == 0) {
			resultSet.setExactResultSize(collectionStatistics.getNumberOfDocuments());
			resultSet.setResultSize(collectionStatistics.getNumberOfDocuments());
			return;
		}		
		
		//in order to save the time from references to the
		//arrays, we create local references
		int[] docids = resultSet.getDocids();
		double[] scores = resultSet.getScores();
		short[] occurences = resultSet.getOccurrences();
		//the number of documents with non-zero score.
		numberOfRetrievedDocuments = 0;
		
		//the pointers read from the inverted file
		int[][] pointers;
		
		//the number of term score modifiers
		int numOfTermModifiers = termModifiers.size();
		
		//the number of document score modifiers
		int numOfDocModifiers = documentModifiers.size();
		
		//int numberOfModifiedDocumentScores =0;
        //inform the weighting model of the collection statistics       
        wmodel.setNumberOfTokens((double)collectionStatistics.getNumberOfTokens());
        wmodel.setNumberOfDocuments((double)collectionStatistics.getNumberOfDocuments());
        wmodel.setAverageDocumentLength((double)collectionStatistics.getAverageDocumentLength());
        wmodel.setNumberOfUniqueTerms((double)collectionStatistics.getNumberOfUniqueTerms());
		
		//for each query term in the query
		final int queryLength = queryTermStrings.length;
		for (int i = 0; i < queryLength; i++) {
			//we seek the query term in the lexicon
			boolean found = lexicon.findTerm(queryTermStrings[i]);
			//and if it is not found, we continue with the next term
			if (!found)
				continue;
			//because when the TreeNode is created, the term
			//code assigned is taken from
			//the TermCodes class, the assigned term code is
			//only valid during the indexing
			//process. Therefore, at this point, the term
			//code should be updated with the one
			//stored in the lexicon file.	
			queryTerms.setTermProperty(queryTermStrings[i], lexicon.getTermId());
			if(logger.isDebugEnabled()){
				logger.debug("" + (i + 1) + ": " + queryTermStrings[i].trim() + "(" + lexicon.getTermId() + ")");
			}
			//the weighting model is prepared for assigning scores to documents
			wmodel.setTermFrequency((double)lexicon.getTF());
			this.termFrequency[i] = (double)lexicon.getTF();
			this.termEstimates[i] = this.termEstimateIndex.getTermEstimateByTermid(lexicon.getTermId());
			if(logger.isDebugEnabled()){
				logger.debug(
					" with "
						+ lexicon.getNt()
						+ " documents (TF is "
						+ lexicon.getTF()
						+ ").");
			}
			//check if the IDF is very low.
			if(logger.isInfoEnabled()){
				if (IGNORE_LOW_IDF_TERMS==true && docIndex.getNumberOfDocuments() < lexicon.getTF()) {
					logger.info("query term " + queryTermStrings[i] + " has low idf - ignored from scoring.");
					continue;
				}
			}
			//the postings are beign read from the inverted file.
			pointers = invertedIndex.getDocuments(queryTerms.getTermCode(queryTermStrings[i]));
			
			init_tf(i, pointers);
			
			//the scores for the particular term
			double[] termScores = new double[pointers[0].length];
			
			//application dependent modification of scores
			//of documents for a term
			//numberOfModifiedDocumentScores = 0;
			for (int t = 0; t < numOfTermModifiers; t++)
				((TermScoreModifier)termModifiers.get(t)).modifyScores(termScores,pointers);
			//application dependent modification of scores
			//of documents for a term. These are predefined by the query
			tsms = queryTerms.getTermScoreModifiers(queryTermStrings[i]);
			if (tsms!=null) {
				for (int t=0; t<tsms.length; t++)
					if (tsms[t]!=null)
						tsms[t].modifyScores(termScores, pointers);
			}
			
			
			//finally setting the scores of documents for a term
			//a mask for setting the occurrences
			short mask = 0;
			if (i<16)
				mask = (short)(1 << i);
			
			int docid;
			final int[] pointers0 = pointers[0];
			final int numberOfPointers = pointers0.length;
			for (int k = 0; k < numberOfPointers; k++) {
				docid = pointers0[k];
				if (occurences[docid]!=0 && (termScores[k] < 0.0d)) {
					numberOfRetrievedDocuments--;
				}
				if (scores[docid]!=Double.NEGATIVE_INFINITY && termScores[k]==Double.NEGATIVE_INFINITY)
					scores[docid] = Double.NEGATIVE_INFINITY;
				occurences[docid] |= mask;
			}
		}
		//	assign scores to documents for query terms
		assignScores(scores);
		
		//sort in descending score order the top RETRIEVED_SET_SIZE documents
		//long sortingStart = System.currentTimeMillis();
		//we need to sort at most RETRIEVED_SET_SIZE, or if we have retrieved
		//less documents than RETRIEVED_SET_SIZE then we need to find the top 
		//numberOfRetrievedDocuments.
		int set_size = Math.min(RETRIEVED_SET_SIZE, numberOfRetrievedDocuments);
		if (set_size == 0) 
			set_size = numberOfRetrievedDocuments;
		
		//sets the effective size of the result set.
		resultSet.setExactResultSize(numberOfRetrievedDocuments);
		
		//sets the actual size of the result set.
		resultSet.setResultSize(set_size);
		
		HeapSort.descendingHeapSort(scores, docids, occurences, set_size);
		//long sortingEnd = System.currentTimeMillis();

		/*we apply the query dependent document score modifiers first and then
		we apply the application dependent ones. This is to ensure that the
		BooleanFallback modifier is applied last. If there are more than
		one application dependent dsms, then it's up to the application, ie YOU!
		to ensure that the BooleanFallback is applied last.*/

		/* dsms each require resorts of the result list. This is expensive, so should
		   be avoided if possible. Sorting is only done if the dsm actually altered any scores */

		/*query dependent modification of scores
		of documents for a query, defined by this query*/
		for (int t = NumberOfQueryDSMs-1; t >= 0; t--) {
			if (dsms[t].modifyScores(index, queryTerms, resultSet))
				HeapSort.descendingHeapSort(scores, docids, occurences, resultSet.getResultSize());
		}

		/*application dependent modification of scores
		of documents for a query, based on a static set by the client code
		sorting the result set after applying each DSM*/
		for (int t = 0; t < numOfDocModifiers; t++) {
			if (((DocumentScoreModifier)documentModifiers.get(t)).modifyScores(
					index,
					queryTerms,
					resultSet))
				HeapSort.descendingHeapSort(scores, docids, occurences, resultSet.getResultSize());
		}
		
		//output results
		if(logger.isInfoEnabled()){
			logger.info("number of retrieved documents: " + resultSet.getResultSize());
		}
	}

	/**
	 * Sets the language model used for retrieval.
	 * @param model the language model used for retrieval
	 */
	public void setModel(Model model) {
		wmodel = (LanguageModel)model;
	}
	
	/**
	 * Returns a descriptive string for the retrieval process performed.
	 * @return String the name of the matching.
	 */
	public String getInfo() {
		return wmodel.getInfo();
	}
	
	/** 
	 * Assigns scores to the documents in the collection.
	 * @param scores double[] the array containing the scores
	 *        of documents.
	 */
	protected void assignScores(double[] scores) {
		int querylength = tf[0].length;
		int numberOfDocs = tf.length;
		for (int i = 0; i < numberOfDocs; i++){
			double docLength = this.docIndex.getDocumentLength(i);
			for (int j = 0; j < querylength; j++){
				if (tf[i][j] == 0d) {
					scores[i] = scores[i] 
							  / wmodel.scoreUnseenNonQuery(termFrequency[j])
							  * wmodel.scoreUnseenQuery(termFrequency[j]);
				} else {
					scores[i] = scores[i] 
							  / wmodel.scoreSeenNonQuery(tf[i][j], docLength, termFrequency[j], termEstimates[j])
							  * wmodel.scoreSeenQuery(tf[i][j], docLength, termFrequency[j], termEstimates[j]);
				}
			}
		}
	}
	/** 
	 * Assigns scores to documents for a particular term and increases
	 * the number of retrieved documents accordingly.
	 * @param querytermIndex int the index of the query term that is
	 *        currently processed.
	 * @param pointers int[][] the pointers read from the inverted
	 *        file for a particular query term.
	 */
	private void init_tf(int querytermIndex, int[][] pointers) {
		int[] pointers1 = pointers[0];
		int[] pointers2 = pointers[1];
		final int numOfPointers = pointers1.length;
		//for each document that contains 
		//the query term, the score is computed.
		int frequency;
		for (int j = 0; j < numOfPointers; j++) {
			frequency = pointers2[j];			
			//checking whether we have setup an upper threshold
			//for within document frequencies. If yes, we check 
			//whether we need to change the current term's frequency.
			if (FREQUENCY_UPPER_THRESHOLD > 0 && frequency > FREQUENCY_UPPER_THRESHOLD) {
				//TODO check whether we need to update the document length as well
				//int diff = frequency - FREQUENCY_UPPER_THRESHOLD;
				//docLength -= diff;
				frequency = FREQUENCY_UPPER_THRESHOLD;	
			}
			
			//compute the score
			int docid = pointers1[j]; 
			tf[docid][querytermIndex] = frequency;
			if (!retrieved[docid])
				numberOfRetrievedDocuments++;
			retrieved[docid] = true;
		}
	}
}

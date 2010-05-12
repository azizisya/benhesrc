/**
 * 
 */
package org.terrier.matching;

import gnu.trove.TIntHashSet;

import java.io.IOException;
import java.util.Arrays;

import org.terrier.matching.dsms.DocumentScoreModifier;
import org.terrier.matching.models.WeightingModel;
import org.terrier.structures.BitIndexPointer;
import org.terrier.structures.DocumentIndex;
import org.terrier.structures.EntryStatistics;
import org.terrier.structures.Index;
import org.terrier.structures.IndexUtil;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.postings.IterablePosting;
import org.terrier.utility.HeapSort;

/**
 * @author ben
 *
 */
public class LMBasicMatching extends OldBasicMatching {
	
	protected DocumentIndex docIndex;

	/**
	 * 
	 */
	public LMBasicMatching() {
		super();
		this.docIndex = index.getDocumentIndex();
	}
	
	/**
	 * Initialises the arrays prior of retrieval. Only the first time it is called,
	 * it will allocate memory for the arrays.
	 */
	protected void initialise() {
		super.initialise();
	}

	/**
	 * @param index
	 */
	public LMBasicMatching(Index index) {
		super(index);
		// TODO Auto-generated constructor stub
		this.docIndex = index.getDocumentIndex();
	}
	
	/**
	 * Implements the matching of a query with the documents.
	 * @param queryNumber the identifier of the processed query.
	 * @param queryTerms the query terms to be processed.
	 * @return Returns the resultset expressed by this query.
	 */
	public ResultSet match(String queryNumber, MatchingQueryTerms queryTerms) throws IOException {
		//the first step is to initialise the arrays of scores and document ids.
		initialise();
		queryTerms.normalizeLMQueryModel();
		
		
		String[] queryTermStrings = queryTerms.getTerms();
		//check whether we need to match an empty query.
		//if so, then return the existing result set.
		if (MATCH_EMPTY_QUERY && queryTermStrings.length == 0) {
			resultSet.setExactResultSize(collectionStatistics.getNumberOfDocuments());
			resultSet.setResultSize(collectionStatistics.getNumberOfDocuments());
			return resultSet;
		}
		
		//load in the dsms
		DocumentScoreModifier[] dsms; int NumberOfQueryDSMs = 0;
		dsms = queryTerms.getDocumentScoreModifiers();
		if (dsms!=null)
			NumberOfQueryDSMs = dsms.length;
		//the number of document score modifiers
		int numOfDocModifiers = documentModifiers.size();
		
		//in order to save the time from references to the arrays, we create local references
		int[] docids = resultSet.getDocids();
		double[] scores = resultSet.getScores();
		short[] occurences = resultSet.getOccurrences();
		// int[] foundUnseen = new int[scores.length];
		// Arrays.fill(foundUnseen, 0);
		
		//the number of documents with non-zero score.
		numberOfRetrievedDocuments = 0;
		
		//the pointers read from the inverted file
		IterablePosting postings;		
		
		//inform the weighting model of the collection statistics
		

		//for each query term in the query
		final int queryLength = queryTermStrings.length;
		
		// get docids of all retrieved documents
		TIntHashSet retDocidSet = new TIntHashSet();
		for (int i = 0; i < queryLength; i++){
			LexiconEntry lEntry = lexicon.getLexiconEntry(queryTermStrings[i]);
			//and if it is not found, we continue with the next term
			if (lEntry==null)
			{
				// logger.info("Term Not Found: "+queryTermStrings[i]);
				continue;
			}
			postings = invertedIndex.getPostings((BitIndexPointer)lEntry);
			int docid = -1;
			while((docid = postings.next()) != IterablePosting.EOL){
				retDocidSet.add(docid);
			}
		}
		int[] retDocids = retDocidSet.toArray();
		retDocidSet.clear(); retDocidSet = null;
		Arrays.sort(retDocids);
		numberOfRetrievedDocuments = retDocids.length;
		
		for (int i = 0; i < queryLength; i++) {
			
			//get the entry statistics - perhaps this came from "far away"
			EntryStatistics entryStats = queryTerms.getStatistics(queryTermStrings[i]);
			//we seek the query term in the lexicon
			LexiconEntry lEntry = lexicon.getLexiconEntry(queryTermStrings[i]);
			if (entryStats == null)
				entryStats = lEntry;
			
			//and if it is not found, we continue with the next term
			if (lEntry==null)
			{
				logger.info("Term Not Found: "+queryTermStrings[i]);
				continue;
			}
			queryTerms.setTermProperty(queryTermStrings[i], lEntry);
			logger.debug((i + 1) + ": " + queryTermStrings[i].trim() + " with " + entryStats.getDocumentFrequency() 
					+ " documents (TF is " + entryStats.getFrequency() + ").");
			
			//check if the IDF is very low.
			if (IGNORE_LOW_IDF_TERMS && collectionStatistics.getNumberOfDocuments() < lEntry.getFrequency()) {
				logger.debug("query term " + queryTermStrings[i] + " has low idf - ignored from scoring.");
				continue;
			}
			
			//the weighting models are prepared for assigning scores to documents
			WeightingModel[] termWeightingModels = queryTerms.getTermWeightingModels(queryTermStrings[i]);
			
			if (termWeightingModels.length == 0)
			{
				logger.warn("No weighting models for term "+ queryTermStrings[i] +", skipping scoring");
				continue;
			}
			
			for (WeightingModel wmodel: termWeightingModels)
			{
				wmodel.setBackgroundStatistics(collectionStatistics);
				wmodel.setRequest(queryTerms.getRequest());
				wmodel.setKeyFrequency(queryTerms.getTermWeight(queryTermStrings[i]));
				wmodel.setEntryStatistics(entryStats);
				IndexUtil.configure(index, wmodel);
				//this requests any pre-calculations to be made
				// wmodel.prepare();
			} 
			
			//the postings are being read from the inverted file.
			postings = invertedIndex.getPostings((BitIndexPointer)lEntry);
			
			//assign scores to documents for a term
			//assignScores(termScores, pointers);
			assignScores(i, termWeightingModels, resultSet, retDocids, postings, lEntry, queryTerms.getTermWeight(queryTermStrings[i]));
		}
		logger.info("Number of docs with +ve score: "+numberOfRetrievedDocuments);
		//for (WeightingModel wmodel : queryTerms.)
		
		//sort in descending score order the top RETRIEVED_SET_SIZE documents
		//long sortingStart = System.currentTimeMillis();
		//we need to sort at most RETRIEVED_SET_SIZE, or if we have retrieved
		//less documents than RETRIEVED_SET_SIZE then we need to find the top 
		//numberOfRetrievedDocuments.
		int set_size = Math.min(RETRIEVED_SET_SIZE, numberOfRetrievedDocuments);
		if (set_size == 0) 
			set_size = numberOfRetrievedDocuments;
		/*
		for (int i=0; i<scores.length; i++)
			if (scores[i] > 0)
				scores[i] = 1d/scores[i];
		*/
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
			if (documentModifiers.get(t).modifyScores(
					index, 
					queryTerms, 
					resultSet))
				HeapSort.descendingHeapSort(scores, docids, occurences, resultSet.getResultSize());
		}
		logger.debug("number of retrieved documents: " + resultSet.getResultSize());
		retDocids = null;
		return resultSet;
	}
	/** Assign scores method 
	 * @param i which query term is this
	 * @param wModels weighting models to use for this term
	 * @param rs Resultset to alter
	 * @param retDocids An sorted array of retrieved docids 
	 * @param postings post list to process
	 * @param lEntry entry statistics 
	 * @param queryTermWeight weight of the query term
	 * */
	protected void assignScores(int i, final WeightingModel[] wModels, ResultSet rs, int[] retDocids, final IterablePosting postings, LexiconEntry lEntry, double queryTermWeight)
		throws IOException
	{
		
		
		//for each document that contains 
		//the query term, the score is computed.
		double score;
		double[] scores = rs.getScores();
		short[] occurences = rs.getOccurrences();
		
		//finally setting the scores of documents for a term
		//a mask for setting the occurrences
		short mask = 0;
		if (i<16)
			mask = (short)(1 << i);
		
		int docid;
		int retIndex = 0;
		while((docid = postings.next()) != IterablePosting.EOL)
		{
			while (retDocids[retIndex] < docid){
				// unseen term in document
				score = 0;
				double docLength = (double)docIndex.getDocumentLength(retDocids[retIndex]);
				for (WeightingModel wmodel: wModels)
				{
					score += wmodel.score(0d, docLength);
				}
				scores[retDocids[retIndex]] += score;
				occurences[retDocids[retIndex]] |= mask;
				retIndex++;
			}
			
			if (retDocids[retIndex] != docid)
				logger.warn("Docid mismatch in assigning language modeling scores.");
			
			score = 0;
			// seen term in document
			for (WeightingModel wmodel: wModels)
			{
				score += wmodel.score(postings);
			}
			/**
			if ((scores[docid] == 0.0d) && (score > 0.0d)) {
				numberOfRetrievedDocuments++;
			} else if ((scores[docid] > 0.0d) && (score < 0.0d)) {
				numberOfRetrievedDocuments--;
			}*/
			scores[docid] += score;
			occurences[docid] |= mask;
			retIndex++;
		}
	}

}

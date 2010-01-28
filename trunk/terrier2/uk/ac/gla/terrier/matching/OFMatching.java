/*
 * Created on 17 Jun 2008
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.matching;

import gnu.trove.THashMap;
import uk.ac.gla.terrier.evaluation.TRECResultsInMemory;
import uk.ac.gla.terrier.matching.dsms.DocumentScoreModifier;
import uk.ac.gla.terrier.matching.tsms.TermScoreModifier;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.LexiconEntry;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.HeapSort;

/**
 * @author ben
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class OFMatching extends Matching {
	
	protected String baselineFilename = ApplicationSetup.getProperty("of.baseline.filename", "");
	
	protected THashMap<String, int[]> queryidDocidMap = new THashMap<String, int[]>();
	
	protected THashMap<String, double[]> queryidScoreMap = new THashMap<String, double[]>();
	
	private void loadBaselineResults(String filename){
		if (logger.isDebugEnabled())
			logger.debug("Loading baseline results from "+filename);
		TRECResultsInMemory results = new TRECResultsInMemory(baselineFilename);
		String[] queryids = results.getQueryids();
		for (int i=0; i<queryids.length; i++){
			String[] docnos = results.getDocnoSet(queryids[i]);
			double[] scores = results.getScores(queryids[i]);
			int[] docids = new int[docnos.length];
			for (int j=0; j<docnos.length; j++)
				docids[j] = this.docIndex.getDocumentId(docnos[j]);
			docnos = null;
			queryidDocidMap.put(queryids[i], docids);
			queryidScoreMap.put(queryids[i], scores);
		}
		results = null;
		if (logger.isDebugEnabled())
			logger.debug("Done. Results of "+queryidDocidMap.size()+" topics are loaded.");
	}

	/**
	 * 
	 */
	public OFMatching() {
		this.loadBaselineResults(this.baselineFilename);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param index
	 */
	public OFMatching(Index index) {
		super(index);
		this.loadBaselineResults(this.baselineFilename);
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Implements the matching of a query with the documents.
	 * @param queryNumber the identifier of the processed query.
	 * @param queryTerms the query terms to be processed.
	 */
	public void match(String queryNumber, MatchingQueryTerms queryTerms) {
		//the first step is to initialise the arrays of scores and document ids.
		initialise();
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
		//in order to save the time from references to the arrays, we create local references
		int[] docids = resultSet.getDocids();
		double[] scores = resultSet.getScores();
		short[] occurences = resultSet.getOccurrences();
		
		
		
		//the number of documents with non-zero score.
		numberOfRetrievedDocuments = 0;
		
		//the pointers read from the inverted file
		//int[][] pointers;
		
		//the number of term score modifiers
		int numOfTermModifiers = termModifiers.size();
		
		//the number of document score modifiers
		int numOfDocModifiers = documentModifiers.size();
		
		//int numberOfModifiedDocumentScores =0;

		//inform the weighting model of the collection statistics		
		/*wmodel.setNumberOfTokens((double)collectionStatistics.getNumberOfTokens());
		wmodel.setNumberOfDocuments((double)collectionStatistics.getNumberOfDocuments());
		wmodel.setAverageDocumentLength((double)collectionStatistics.getAverageDocumentLength());
		wmodel.setNumberOfUniqueTerms((double)collectionStatistics.getNumberOfUniqueTerms());
		wmodel.setNumberOfPointers((double)collectionStatistics.getNumberOfPointers());*/

		//for each query term in the query
		/*final int queryLength = queryTermStrings.length;
		for (int i = 0; i < queryLength; i++) {
			//we seek the query term in the lexicon
			LexiconEntry lEntry = lexicon.getLexiconEntry(queryTermStrings[i]);
			//boolean found = lexicon.findTerm(queryTermStrings[i]);
			//and if it is not found, we continue with the next term
			if (lEntry==null)
			{
				logger.info("Term Not Found: "+queryTermStrings[i]);
				continue;
			}
			//because when the TreeNode is created, the term code assigned is taken from
			//the TermCodes class, the assigned term code is only valid during the indexing
			//process. Therefore, at this point, the term code should be updated with the one
			//stored in the lexicon file.	
			queryTerms.setTermProperty(queryTermStrings[i], lEntry.termId);
			//the weighting model is prepared for assigning scores to documents
			wmodel.setKeyFrequency(queryTerms.getTermWeight(queryTermStrings[i]));
			wmodel.setDocumentFrequency((double)lEntry.n_t);
			wmodel.setTermFrequency((double)lEntry.TF);
			
			logger.debug((i + 1) + ": " + queryTermStrings[i].trim() + " with " + lEntry.n_t + " documents (TF is " + lEntry.TF + ").");


			//check if the IDF is very low.
			if (IGNORE_LOW_IDF_TERMS && docIndex.getNumberOfDocuments() < lEntry.TF) {
				logger.debug("query term " + queryTermStrings[i] + " has low idf - ignored from scoring.");
				continue;
			}
			
			//the postings are being read from the inverted file.
			pointers = invertedIndex.getDocuments(lEntry);
			//TODO: some consideration here for the PositionInvertedIndex.
			//ie PositionInvertedIndex will returns positions by default,
			//eg, perhaps it should change and not this code

			final int numberOfPointers = pointers[0].length;
			//the scores for the particular term
			final double[] termScores = new double[numberOfPointers];
			
			//assign scores to documents for a term
			//assignScores(termScores, pointers);
			assignScores(termScores, pointers, lEntry, queryTerms.getTermWeight(queryTermStrings[i]));
			//application dependent modification of scores
			//of documents for a term
			//numberOfModifiedDocumentScores = 0;
			for (int t = 0; t < numOfTermModifiers; t++)
				termModifiers.get(t).modifyScores(termScores, pointers);
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
			//int[] pointers1 = pointers[1];
			for (int k = 0; k < numberOfPointers; k++) {
				docid = pointers0[k];
				if ((scores[docid] == 0.0d) && (termScores[k] > 0.0d)) {
					numberOfRetrievedDocuments++;
				} else if ((scores[docid] > 0.0d) && (termScores[k] < 0.0d)) {
					numberOfRetrievedDocuments--;
				}
				scores[docid] += termScores[k];
				occurences[docid] |= mask;
			}
		}*/
		// get scores from cached baseline results
		double[] baseline_scores = queryidScoreMap.get(queryTerms.queryId);
		int[] baseline_docids = queryidDocidMap.get(queryTerms.queryId);
		numberOfRetrievedDocuments = baseline_docids.length;
		for (int i=0; i<baseline_docids.length; i++){
			scores[baseline_docids[i]] = baseline_scores[i];
		}
		
		logger.debug("Number of docs with +ve score: "+numberOfRetrievedDocuments);
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
		resultSet = resultSet.getResultSet(0, set_size);
		
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

	}

}

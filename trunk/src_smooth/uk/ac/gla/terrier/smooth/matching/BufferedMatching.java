/*
 * Smooth - Smoothing term frequency normalisation
 * Webpage: http://ir.dcs.gla.ac.uk/smooth
 * Contact: ben{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * Information Retrieval Group
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
 * The Original Code is BufferedMatching.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.smooth.matching;

import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;

import uk.ac.gla.terrier.matching.Matching;
import uk.ac.gla.terrier.smooth.structures.BasicQuery;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.DirectIndex;
import uk.ac.gla.terrier.structures.ExpansionTerms;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.smooth.structures.trees.TermTreeNode;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.HeapSort;
/**
 * This class extends Matching by adding some supplemental functionalities.
 * @author Ben He <ben{a.}dcs.gla.ac.uk>
 * @version $Revision: 1.3 $
 */
public class BufferedMatching extends Matching {
	/** An aaray of the length of the retrieved documents. */
	protected double[] retrievedDocLength;
	/** An array of boolean variables showing if each document is retrieved. */
	protected boolean[] retrieved;
	/** Document frequencies of the query terms. */
	protected double[] documentFrequency;
	/** Indicate if perform matching for all the query terms, regardless the
	 * value of property <tt>ignore.low.idf.terms</tt>.
	 * */
	protected boolean MATCH_ALL_TERMS = false;
	/**
	 * An array containing the frequencies of the query terms in the collection. 
	 */
	protected double[] termFrequency;
	
	public Index index;
	
	/**
	 * Indicate if the matching is running in silent model (with less output).
	 */
	protected boolean silentMode = true;
	/** Indicate if it is running under debugging model. This correspondes to property
	 * <tt>debugging.mode</tt>. 
	 * */
	protected final boolean debugging = new Boolean(
			ApplicationSetup.getProperty("debugging.mode", "false")).booleanValue();
	/**
	 * The constructor.
	 * @param index The index used in the matching process.
	 */
	public BufferedMatching(Index index){
		super(index);
		this.index = index;
	}
	/**
	 * Initialise the class.
	 * @param queryLength The length of the query to be matched.
	 */
	protected void initialise(int queryLength){
		resultSet.initialise();
		this.collectionStatistics = index.getCollectionStatistics();
		
		RETRIEVED_SET_SIZE = (new Integer(ApplicationSetup.getProperty("matching.retrieved_set_size", "1000"))).intValue();
		FREQUENCY_UPPER_THRESHOLD = (new Integer(ApplicationSetup.getProperty("frequency.upper.threshold", "0"))).intValue();
		IGNORE_LOW_IDF_TERMS = (new Boolean(ApplicationSetup.getProperty("ignore.low.idf.terms","true"))).booleanValue();
		MATCH_EMPTY_QUERY = (new Boolean(ApplicationSetup.getProperty("match.empty.query","false"))).booleanValue();
		retrieved = new boolean[this.collectionStatistics.getNumberOfDocuments()];
		Arrays.fill(retrieved, false);
		this.documentFrequency = new double[queryLength];
		this.termFrequency = new double[queryLength];
	}
	/**
	 * This method performs a matching process for all the terms in the given
	 * BasicQuery, regardless the setting of <tt>ignore.low.idf.terms</tt>.
	 * @param query The BasicQuery to be matched.
	 */
	public void safeBasicMatch(BasicQuery query){
		this.MATCH_ALL_TERMS = true;
		this.basicMatch(query);
		this.MATCH_ALL_TERMS = false;
	}
	
	/**
	 * The method performs a matching process for a given array of terms. Statistics
	 * of the terms, including number of documents containing at least one of the
	 * terms, document frequency and term frequency of the terms in the collection,
	 * etc. are saved in a buffer. If property <tt>ignore.low.idf.terms</tt> is set 
	 * to true and MATCH_ALL_TERMS is set to false, terms with low idf will be ignored
	 * from matching.
	 * @param queryNumber The identifier of the processed query.
	 * @param queryTerms The query terms to be processed.
	 */
	public void matchWithoutScoring(String queryNumber, String[] queryTermStrings) {
		//the first step is to initialise the arrays of scores and document ids.
		initialise(queryTermStrings.length);
		Vector vectorRetrievedDocLength = new Vector();
		Vector vectorWithinDocFrequency = new Vector();

		//check whether we need to match an empty query.
		//if so, then return the existing result set.
		if (MATCH_EMPTY_QUERY && queryTermStrings.length == 0) {
			resultSet.setExactResultSize(this.collectionStatistics.getNumberOfDocuments());
			resultSet.setResultSize(this.collectionStatistics.getNumberOfDocuments());
			return;
		}

		//in order to save the time from references to the arrays, we create local references
		int[] docids = resultSet.getDocids();
		//the number of documents with non-zero score.
		numberOfRetrievedDocuments = 0;
		
		//the pointers read from the inverted file
		int[][] pointers;
		
		//for each query term in the query
		final int queryLength = queryTermStrings.length;
		for (int i = 0; i < queryLength; i++) {
			//we seek the query term in the lexicon
			boolean found = lexicon.findTerm(queryTermStrings[i]);

			//and if it is not found, we continue with the next term
			if (!found)
				continue;
			if (!this.silentMode){
				System.out.print("" + (i + 1) + ": " + queryTermStrings[i].trim() + 
						"(" + lexicon.getTermId() + ")");

				System.out.println(
						" with "
						+ lexicon.getNt()
						+ " documents (TF is "
						+ lexicon.getTF()
						+ ").");
			}
			documentFrequency[i] = lexicon.getNt();
			termFrequency[i] = lexicon.getTF();

			//check if the IDF is very low.
			if (IGNORE_LOW_IDF_TERMS==true && docIndex.getNumberOfDocuments() < lexicon.getTF()) {
				System.out.println("query term " + queryTermStrings[i] + " has low idf - ignored from scoring.");
				continue;
			}
			
			//the postings are beign read from the inverted file.
			pointers = invertedIndex.getDocuments(lexicon.getTermId());
			this.saveMatchingBuffer(pointers, vectorRetrievedDocLength, vectorWithinDocFrequency);
		}
		this.retrievedDocLength  = new double[vectorRetrievedDocLength.size()];
		for (int i = 0; i < retrievedDocLength.length; i++){
			retrievedDocLength[i] = ((Double)vectorRetrievedDocLength.get(i)).doubleValue();
		}
		if (!this.silentMode)
		System.out.println("number of retrieved documents: " + this.numberOfRetrievedDocuments);
	}
	/**
	 * This method performs a matching and scoring process for a BasicQuery.
	 * @param query The given BasicQuery.
	 */
	public void basicMatch(BasicQuery query){
		this.basicMatch(query.getQueryNumber(), query.getQueryTerms());
	}
	
	/**
	 * This method performs a matching and scoring process for an array 
	 * of term treenodes.
	 * @param queryNumber the identifier of the processed query.
	 * @param queryTerms the query terms to be processed.
	 */
	public void basicMatch(String queryNumber, TermTreeNode[] queryTerms) {
//		the first step is to initialise the arrays of scores and document ids.
		initialise(queryTerms.length);
		
		//check whether we need to match an empty query.
		//if so, then return the existing result set.
		if (MATCH_EMPTY_QUERY && queryTerms.length == 0) {
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
		int[][] pointers;
		
		//for each query term in the query
		final int queryLength = queryTerms.length;
		wmodel.setAverageDocumentLength(
				(double)collectionStatistics.getAverageDocumentLength());
		wmodel.setNumberOfDocuments((double)collectionStatistics.getNumberOfDocuments());
		wmodel.setNumberOfTokens((double)collectionStatistics.getNumberOfTokens());
		for (int i = 0; i < queryLength; i++) {
			if (queryTerms[i].normalisedFrequency <= 0d){
				System.err.println(queryTerms[i].term + " has zero qtf. ignore from" +
						" weighting.");
				continue;
			}
			//we seek the query term in the lexicon
			boolean found = lexicon.findTerm(queryTerms[i].term);
			//and if it is not found, we continue with the next term
			if (!found)
				continue;
			//because when the TreeNode is created, the term code assigned is taken from
			//the TermCodes class, the assigned term code is only valid during the indexing
			//process. Therefore, at this point, the term code should be updated with the one
			//stored in the lexicon file.	
			//queryTerms.setTermProperty(queryTerms[i], lexicon.getTermId());
			
			System.err.print("" + (i + 1) + ": " + queryTerms[i].term.trim());
			//the weighting model is prepared for assigning scores to documents
			wmodel.setKeyFrequency(queryTerms[i].normalisedFrequency);
			wmodel.setDocumentFrequency((double)lexicon.getNt());
			wmodel.setTermFrequency((double)lexicon.getTF());
			
			System.err.println(", model: " + wmodel.getInfo());
			System.err.println(
				" with "
					+ lexicon.getNt()
					+ " documents (TF is "
					+ lexicon.getTF()
					+ ").");
			//check if the IDF is very low.
			if (!MATCH_ALL_TERMS && IGNORE_LOW_IDF_TERMS==true && docIndex.getNumberOfDocuments() < lexicon.getTF()) {
				System.err.println("query term " + queryTerms[i].term + 
						" has low idf - ignored from scoring.");
				continue;
			}
			
			//the postings are beign read from the inverted file.
			pointers = invertedIndex.getDocuments(lexicon.getTermId());
			
			//the scores for the particular term
			double[] termScores = new double[pointers[0].length];
			//System.out.println("model: " + wmodel.getInfo());
			//assign scores to documents for a term
			assignScores(termScores, pointers);
			
			//finally setting the scores of documents for a term
			//a mask for setting the occurrences
			short mask = 0;
			if (i<16)
				mask = (short)(1 << i);
			
			int docid;
			int[] pointers0 = pointers[0];
			int[] pointers1 = pointers[1];
			final int numberOfPointers = pointers0.length;
			//System.out.println("numberOfPointers: " + numberOfPointers);
			
			for (int k = 0; k < numberOfPointers; k++) {
//				if (k < 10)
//					System.out.println("termScores["+ k + "]: " + termScores[k]);
				docid = pointers0[k];
				if ((scores[docid] <= 0.0d) && (termScores[k] > 0.0d)) {
					numberOfRetrievedDocuments++;
				} 
//				else if ((scores[docid] > 0.0d) && (termScores[k] < 0.0d)) {
//					numberOfRetrievedDocuments--;
//				}
				scores[docid] += termScores[k];
				occurences[docid] |= mask;
			}
		}
		//sort in descending score order the top RETRIEVED_SET_SIZE documents
		long sortingStart = System.currentTimeMillis();
		//we need to sort at most RETRIEVED_SET_SIZE, or if we have retrieved
		//less documents than RETRIEVED_SET_SIZE then we need to find the top 
		//numberOfRetrievedDocuments.
		//System.out.println("numberOfRetrievedDocuments: " + numberOfRetrievedDocuments);
		int set_size = Math.min(RETRIEVED_SET_SIZE, numberOfRetrievedDocuments);
		if (set_size == 0) 
			set_size = numberOfRetrievedDocuments;
		
		//sets the effective size of the result set.
		resultSet.setExactResultSize(numberOfRetrievedDocuments);
		
		//sets the actual size of the result set.
		resultSet.setResultSize(set_size);
		
		HeapSort.descendingHeapSort(scores, docids, occurences, set_size);
		long sortingEnd = System.currentTimeMillis();
		
		System.err.println("number of retrieved documents: " + resultSet.getResultSize());

	}
	/**
	 * This method retrieves the length of documents containing the given term, 
	 * and the corresponding within-document frequency.
	 * @param term The given term to be retrieved.
	 * @param docLength The length of documents containing the given term.
	 * @param tf The frequency of the given term in the documents in which it
	 * appears.
	 */
	public void accessInvIndex(String term, double[] docLength, double[] tf){
		if (lexicon.findTerm(term)){
			int[][] pointers = invertedIndex.getDocuments(lexicon.getTermId());
			int[] pointers1 = pointers[0];
			int[] pointers2 = pointers[1];

			final int numOfPointers = pointers1.length;

			//for each document that contains 
			//the query term, the score is computed.
			int frequency;
			int length;
			for (int j = 0; j < numOfPointers; j++) {
				frequency = pointers2[j];
				length = docIndex.getDocumentLength(pointers1[j]);
				docLength[j] = (double)length;
				tf[j] = (double)frequency;
			}
		}
		
	}
	/**
	 * This method saves the retrieved document length and the corresponding
	 * term frequency for a term in two temporiary vectors.
	 * @param pointers The pointers retrieved for a query term from the inverted
	 * index.
	 * @param vectorRetrievedDocLength The temporiary vector that saves the 
	 * retrieved document length.
	 * @param vectorWithinDocFrequency The temporiary vector that saves the within
	 * document frequency of the query term. 
	 */
	private void saveMatchingBuffer(int[][] pointers, 
			Vector vectorRetrievedDocLength,
			Vector vectorWithinDocFrequency) {
		int[] pointers1 = pointers[0];
		int[] pointers2 = pointers[1];
		if (!this.silentMode)
			System.out.println("pointers1.length: " + pointers1.length +
					", pointers2.length: " + pointers2.length);

		final int numOfPointers = pointers1.length;

		//for each document that contains 
		//the query term, the score is computed.
		int frequency;
		int docLength;
		for (int j = 0; j < numOfPointers; j++) {
			frequency = pointers2[j];
			docLength = docIndex.getDocumentLength(pointers1[j]);
			if (!retrieved[pointers1[j]]){
				vectorRetrievedDocLength.addElement(new Double(docLength));
				vectorWithinDocFrequency.addElement(new Double(frequency));
				retrieved[pointers1[j]] = true;
				numberOfRetrievedDocuments++;
			}
		}
	}
	/**
	 * This method accesses the direct index and inserts the terms in the
	 * given number of top-ranked documents into an instance of ExpansionTerms.  
	 * @param effDocuments The number of top-ranked documents from which the terms
	 * are extracted.
	 * @return A structure containing terms and corresponding frequency in the
	 * top-ranked documents.
	 */
	public ExpansionTerms accessDirectIndex(int effDocuments){
		DirectIndex directIndex = index.getDirectIndex();
		int[] docids = (int[])resultSet.getDocids().clone();
		double totalDocumentLength = 0d;
		for (int i = 0; i < effDocuments; i++)
			totalDocumentLength += this.docIndex.getDocumentLength(docids[i]);
		ExpansionTerms expansionTerms = new ExpansionTerms(this.collectionStatistics, totalDocumentLength, lexicon);
		for (int i = 0; i < effDocuments; i++){
			int[][] terms = directIndex.getTerms(docids[i]);
			for (int j = 0; j < terms[0].length; j++)
				expansionTerms.insertTerm(terms[0][j], (double)terms[1][j]);
		}
		return expansionTerms;
	}
	/**
	 * Get the frequency of the query terms in the collection.
	 * @return The frequency of the query terms in the collection.
	 */
	public double[] getTF(){
		return (double[])termFrequency.clone();
	}
	/**
	 * Get the document frequency of the terms in the given query.
	 * @return The document frequency of the terms in the given query.
	 */
	public double[] getNt(){
		return (double[])documentFrequency.clone();
	}
	/**
	 * Get the length of the retrieved documents.
	 * @return An array of the length of the retrieved documents.
	 */
	public double[] getRetrievedDocLength(){
		return (double[])retrievedDocLength.clone();
	}
	/** 
	 * Get the number of retrieved documents.
	 * @return The number of retrieved documents for the given query.
	 */
	public int getNumberOfRetrievedDocuments(){
		return this.numberOfRetrievedDocuments;
	}
}

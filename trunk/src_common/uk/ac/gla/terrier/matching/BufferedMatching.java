package uk.ac.gla.terrier.matching;

import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;

import uk.ac.gla.terrier.matching.Matching;
import uk.ac.gla.terrier.structures.BasicQuery;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.CorrelationIndex;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.trees.TermTreeNode;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.HeapSort;
/*
 * Created on 2004-12-27
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

/**
 * @author ben
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class BufferedMatching extends Matching {
	protected double[] retrievedDocLength;
	
	public CollectionStatistics collSta;
	
	protected boolean[] retrieved;
	
	protected double[] documentFrequency;
	
	protected double[] termFrequency;
	
	protected double[] withinDocFrequency;
	
	protected boolean silentMode = true;
	
	protected CorrelationIndex corrIndex;
	
	protected final boolean debugging = new Boolean(
			ApplicationSetup.getProperty("debugging.mode", "false")).booleanValue();
	
	public final boolean TERM_BASED_TUNING = 
		new Boolean(ApplicationSetup.getProperty("term.based.tuning", "false")).booleanValue();
	
	public BufferedMatching(Index index){
		super(index);
		if (debugging)
			System.out.println("Finished calling super constructor of BufferedMatching...");
		if (debugging)
			System.out.println("Finished calling super constructor of CorrelationIndex...");
	//	this.corrIndex = new CorrelationIndex("2", index);
		collSta = index.getCollectionStatistics();
	}
	
	public void setCorrelationIndex(CorrelationIndex corrIndex){
		this.corrIndex = corrIndex;
	}
	
	protected void initialise(int queryLength){
		resultSet.initialise();
		RETRIEVED_SET_SIZE = (new Integer(ApplicationSetup.getProperty("matching.retrieved_set_size", "1000"))).intValue();
		FREQUENCY_UPPER_THRESHOLD = (new Integer(ApplicationSetup.getProperty("frequency.upper.threshold", "0"))).intValue();
		IGNORE_LOW_IDF_TERMS = (new Boolean(ApplicationSetup.getProperty("ignore.low.idf.terms","true"))).booleanValue();
		MATCH_EMPTY_QUERY = (new Boolean(ApplicationSetup.getProperty("match.empty.query","false"))).booleanValue();
		retrieved = new boolean[collSta.getNumberOfDocuments()];
		Arrays.fill(retrieved, false);
		this.documentFrequency = new double[queryLength];
		this.termFrequency = new double[queryLength];
	}
	
	/**
	 * Implements the matching of a query with the documents.
	 * @param queryNumber the identifier of the processed query.
	 * @param queryTerms the query terms to be processed.
	 */
	public void matchWithoutScoring(String queryNumber, String[] queryTermStrings) {
		//the first step is to initialise the arrays of scores and document ids.
		initialise(queryTermStrings.length);
		Vector vectorRetrievedDocLength = new Vector();
		Vector vectorWithinDocFrequency = new Vector();

		//check whether we need to match an empty query.
		//if so, then return the existing result set.
		if (MATCH_EMPTY_QUERY && queryTermStrings.length == 0) {
			resultSet.setExactResultSize(collSta.getNumberOfDocuments());
			resultSet.setResultSize(collSta.getNumberOfDocuments());
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
//			if (IGNORE_LOW_IDF_TERMS==true && docIndex.getNumberOfDocuments() < lexicon.getTF()) {
//				System.out.println("query term " + queryTermStrings[i] + " has low idf - ignored from scoring.");
//				continue;
//			}
			
			//the postings are beign read from the inverted file.
			pointers = invertedIndex.getDocuments(lexicon.getTermId());
			this.saveMatchingBuffer(pointers, vectorRetrievedDocLength, vectorWithinDocFrequency);
		}
		this.retrievedDocLength  = new double[vectorRetrievedDocLength.size()];
		this.withinDocFrequency = new double[vectorWithinDocFrequency.size()];
		for (int i = 0; i < retrievedDocLength.length; i++){
			retrievedDocLength[i] = ((Double)vectorRetrievedDocLength.get(i)).doubleValue();
			withinDocFrequency[i] = ((Double)vectorWithinDocFrequency.get(i)).doubleValue();
		}
		if (!this.silentMode)
		System.out.println("number of retrieved documents: " + this.numberOfRetrievedDocuments);
	}
	
	public void basicMatch(BasicQuery query){
		this.basicMatch(query.getQueryNumber(), query.getQueryTerms());
	}
	
	/**
	 * Implements the matching of a query with the documents.
	 * @param queryNumber the identifier of the processed query.
	 * @param queryTerms the query terms to be processed.
	 */
	public void basicMatch(String queryNumber, TermTreeNode[] queryTerms) {
//		the first step is to initialise the arrays of scores and document ids.
		initialise(queryTerms.length);
		
		//check whether we need to match an empty query.
		//if so, then return the existing result set.
		if (MATCH_EMPTY_QUERY && queryTerms.length == 0) {
			resultSet.setExactResultSize(collSta.getNumberOfDocuments());
			resultSet.setResultSize(collSta.getNumberOfDocuments());
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
			wmodel.setAverageDocumentLength(
					(double)collSta.getAverageDocumentLength());
			if (this.TERM_BASED_TUNING){
				try{
					double parameter = corrIndex.getParameterValue(lexicon.getTermId());
					wmodel.setParameter(parameter);
				}
				catch(Exception e){
					e.printStackTrace();
					System.err.println("corrIndex: " + corrIndex);
					System.exit(1);
				}
			}
			System.err.println("model: " + wmodel.getInfo());
			System.err.println(
				" with "
					+ lexicon.getNt()
					+ " documents (TF is "
					+ lexicon.getTF()
					+ ").");
			//check if the IDF is very low.
			if (IGNORE_LOW_IDF_TERMS==true && docIndex.getNumberOfDocuments() < lexicon.getTF()) {
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
			//application dependent modification of scores
			//of documents for a term
//			numberOfModifiedDocumentScores = 0;
//			for (int t = 0; t < numOfTermModifiers; t++)
//				((TermScoreModifier)termModifiers.get(t)).modifyScores(termScores, pointers);
			//application dependent modification of scores
			//of documents for a term. These are predefined by the query
//			tsms = queryTerms.getTermScoreModifiers(queryTerms[i]);
//			if (tsms!=null) {
//				for (int t=0; t<tsms.length; t++)
//					if (tsms[t]!=null)
//						tsms[t].modifyScores(termScores, pointers);
//			}
			
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
		
		/*we apply the query dependent document score modifiers first and then 
		we apply the application dependent ones. This is to ensure that the 
		BooleanFallback modifier is applied last. If there are more than 
		one application dependent dsms, then it's up to the application, ie YOU!
		to ensure that the BooleanFallback is applied last.*/
		
		/* dsms each require resorts of the result list. This is expensive, so should
		   be avoided if possible. Sorting is only done if the dsm actually altered any scores */

		/*query dependent modification of scores
		of documents for a query, defined by this query*/
//		for (int t = NumberOfQueryDSMs-1; t >= 0; t--) {
//			if (dsms[t].modifyScores(index, queryTerms, resultSet))
//				HeapSort.descendingHeapSort(scores, docids, occurences, resultSet.getResultSize());
//		}
		
		/*application dependent modification of scores
		of documents for a query, based on a static set by the client code
		sorting the result set after applying each DSM*/
//		for (int t = 0; t < numOfDocModifiers; t++) {
//			if (((DocumentScoreModifier)documentModifiers.get(t)).modifyScores(
//					index, 
//					queryTerms, 
//					resultSet))
//				HeapSort.descendingHeapSort(scores, docids, occurences, resultSet.getResultSize());
//		}
		System.err.println("number of retrieved documents: " + resultSet.getResultSize());

	}
	
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
	
	public double[] getTF(){
		return (double[])termFrequency.clone();
	}
	
	public double[] getNt(){
		return (double[])documentFrequency.clone();
	}
	
	public double[] getWithinDocFrequency(){
		return (double[])withinDocFrequency.clone();
	}
	
	public double[] getRetrievedDocLength(){
		return (double[])retrievedDocLength.clone();
	}
	
	public int getNumberOfRetrievedDocuments(){
		return this.numberOfRetrievedDocuments;
	}
}

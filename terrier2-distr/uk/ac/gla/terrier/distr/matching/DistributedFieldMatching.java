/*
 * Terrier - Terabyte Retriever 
 * Webpage: http://ir.dcs.gla.ac.uk/terrier 
 * Contact: terrier{a.}dcs.gla.ac.uk
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
 * The Original Code is DistributedFieldMatching.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk> 
 */
package uk.ac.gla.terrier.distr.matching;

import gnu.trove.THashMap;
import gnu.trove.TIntHashSet;
import java.rmi.RemoteException;
import java.util.Arrays;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.distr.structures.DistributedExpansionTerms;
import uk.ac.gla.terrier.distr.structures.DistributedMatchingQueryTerms;
import uk.ac.gla.terrier.distr.structures.LocalMatchingQueryTerms;
import uk.ac.gla.terrier.matching.FieldMatching;
import uk.ac.gla.terrier.matching.QueryResultSet;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.matching.dsms.DocumentScoreModifier;
import uk.ac.gla.terrier.structures.BlockInvertedIndex;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.HeapSort;

/**
 * This class provides functionalities for running retrieval on three fields in
 * a distributed setting.
 * @author Ben He(ben@dcs.gla.ac.uk)
 * @version $Revision: 1.1 $
 */
public class DistributedFieldMatching extends FieldMatching {
	/** the logger for this class */
	private static Logger logger = Logger.getLogger("server");
	
	protected String id;
	
	protected boolean SIMPLE_NORMALISATION = false;
	
	/**
	 * The number of documents in each field in the whole collection.
	 */
	protected long[] globalNumberOfDocuments;
	
	protected THashMap cacheDistributedMatchingTerms = new THashMap();

	protected boolean CACHE_TERMS = (new Boolean(ApplicationSetup.getProperty(
			"cache.terms", "false"))).booleanValue();
	/**
	 * A hashmap from the address of global lexicons to their instances.
	 */
	protected THashMap addrLexMap = new THashMap();
	
	/**
	 * The number of tokens in each field in the whole collection.
	 */
	protected long[] globalNumberOfTokens;
	/**
	 * The average field length of the each field in the whole collection.
	 */
	protected double[] globalAverageDocumentLength;
	/**
	 * The total number of documents in the three fields in the whole collection.
	 * Fields in the same document count as one document.
	 */
	protected long globalTotalNumberOfDocuments;
	/**
	 * The sum of number of tokens in the three fields in the whole collection.
	 */
	protected long globalTotalNumberOfTokens;
	/**
	 * The average document length in the whole collection.
	 */
	protected double globalTotalAverageDocumentLength;
	
	public DistributedFieldMatching(Index[] indices){
		super(indices);
		this.resultSet = new QueryResultSet(this.getNumberOfDocuments()[0]);
	}
	
	
	
	public void setNormalisationStrategy(boolean value){
		this.SIMPLE_NORMALISATION = value;
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setQEWeights(double[] weights){
		this.QEWeights = weights;		
	}
	
	/**
	 * Get the sum of the length in the three fields in a given document.
	 * @param docno The docno of the given document.
	 * @return The document length.
	 */
	public int getFullDocumentLength(String docno){
		int docid = index.getDocumentIndex().getDocumentId(docno);
		int length = 0;
		
		if (docid<0)
			length = 0;
		else{
			for (int i=0; i<NumberOfFields; i++)
				length = this.docIndexes[i].getDocumentLength(docid);
		}
		return length;
	}
	/**
	 * Get the sum of the length in the three fields in a given document.
	 * @param docno The docno of the given document.
	 * @return The document length.
	 */
	public int getFullDocumentLength(int localDocid){
		int docid = localDocid;
		int length = 0;
		
		if (docid<0)
			length = 0;
		else{
			for (int i=0; i<NumberOfFields; i++)
				length = this.docIndexes[i].getDocumentLength(docid);
		}
		return length;
	}
	/**
	 * Get the sum of the frequency of a given term in the three fields of
	 * a given document.
	 * @param term The given term.
	 * @param docno The docno of the given document.
	 * @return The within-document frequency.
	 */
	public double getFullWithinDocFrequency(String term, String docno){
		double tf = 0;
		for (int i=0; i<NumberOfFields; i++){
			if (this.lexicons[i].findTerm(term)){
				int docid = this.docIndexes[i].getDocumentId(docno);
				int[][] terms = index.getDirectIndex().getTerms(docid);
				int termid = lexicons[i].getTermId();
				for (int j = 0; j < terms[0].length; j++)
					if (terms[0][j]==termid){
						tf+=terms[1][j];
						break;
					}
				}
		}
		return tf;
	}
	/**
	 * Set if to ignore terms with low idf values from matching.
	 * @param value True to ignore and false to keep.
	 */
	public void setIgnoreLowIdfTerms(boolean value){
		IGNORE_LOW_IDF_TERMS = value;
	}
	/**
	 * Get the in-subsection document frequency of a given term. 
	 * @param term The given term.
	 * @return The in-subsection document frequency. 
	 */
	public double getFullNt(String term){
		TIntHashSet allPointers = new TIntHashSet();
		for (int i=0; i<NumberOfFields; i++){
			if (lexicons[i].findTerm(term)) {
				int termid = lexicons[i].getTermId();
				int[][] pointers = invIndexes[i].getDocuments(termid);
				allPointers.addAll(pointers[0]);
			}
		}
		return allPointers.size();
	}
	/**
	 * Get the in-subcollection term frequency of a given term.
	 * @param term The given term.
	 * @return The in-subcollection term frequency.
	 */
	public double getFullTF(String term){
		return (double)getFullLocalTF(term);
	}
	
 	
 	private int getFullLocalTF(String term){
 		int TF = 0;
		for (int i=0; i<NumberOfFields; i++){
			if (lexicons[i].findTerm(term)){
				TF+=lexicons[i].getTF();
			}
		}
		return TF;
 	}
	
	/**
	 * Get the in-subcollection term frequency and the in-subcollection
	 * document frequency
	 * @param term The given term.
	 * @return A two-element double array. The first element contains the
	 * in-subcollection term frequency and the second element contains the 
	 * in-subcollection document frequency.
	 */
	public double[] getFullStats(String term){
		System.out.print("Retrieving statistics for term "+term+"...");
		double TF = 0;
		int Nt = 0;		
		TIntHashSet hashSet = new TIntHashSet();
		for (int i=0; i<NumberOfFields; i++){
			if (lexicons[i].findTerm(term) ){
				int termid = lexicons[i].getTermId();
				
				int[][] pointers = (invIndexes[i] instanceof BlockInvertedIndex)?
						((BlockInvertedIndex)invIndexes[i]).getDocumentsWithoutBlocks(termid):
							invIndexes[i].getDocuments(termid);
				
				TF += lexicons[i].getTF();
				if (pointers!=null)
					hashSet.addAll(pointers[0]);
			}
		}
			
		Nt += hashSet.size();
		
		double[] stats = {TF, Nt};
		if (logger.isDebugEnabled())
			logger.debug("TF: "+TF+", Nt: "+Nt);
		return stats;
	}
	/**
	 * Get terms in a set of given documents. 
	 * @param docnos The docnos of the given documents.
	 * @param distTerms A data structure for storing the terms in a set of documents.
	 * @return The terms in a set of given documents.
	 */
	public DistributedExpansionTerms getTerms(String[] docnos, 
			DistributedExpansionTerms distTerms,
			String globalLexiconAddress) throws RemoteException{
		if (logger.isDebugEnabled()){
			for (int i=0; i<NumberOfFields; i++)
				logger.debug("qeweight."+(i+1)+": "+QEWeights[i]);
		}
		int[][] terms = null;
		Arrays.sort(docnos);
		String docnoCat = "";
		for (int i=0; i<docnos.length; i++)
			docnoCat+=docnos[i].trim();
		if (CACHE_TERMS&&this.cacheDistributedMatchingTerms.containsKey(docnoCat)){
			if (logger.isInfoEnabled())
				logger.info("Terms have been cached.");
			return (DistributedExpansionTerms)this.cacheDistributedMatchingTerms.get(docnoCat);
		}
		
		Lexicon globalLexicon = null;
		if (this.addrLexMap.containsKey(globalLexiconAddress)){
			if (logger.isInfoEnabled())
				logger.info("Lexicon has been cached.");
			globalLexicon = (Lexicon)addrLexMap.get(globalLexiconAddress);
		}else{
			globalLexicon = new Lexicon(globalLexiconAddress);
			this.addrLexMap.put(globalLexiconAddress, globalLexicon);
		}
		
		for (int counter=0; counter<docnos.length; counter++){
			if (logger.isDebugEnabled())
				logger.debug("parsing document "+docnos[counter]);
			
			for (int i=0; i<NumberOfFields; i++){
				int docid = docIndexes[i].getDocumentId(docnos[counter]);
				if (docid >= 0){
					terms = indices[i].getDirectIndex().getTerms(docid);
					if (terms != null){
						for (int j=0; j<terms[0].length; j++){
							int id = terms[0][j];
							int frequency = terms[1][j];
							String term = null;
							
							lexicons[i].findTerm(id);
							term = lexicons[i].getTerm();
								
							distTerms.insertTerm(
									term, 
									(double)frequency*QEWeights[i], 
									docnos[counter],
									globalLexicon);
						}
					}
				}
			}
		}
		if (CACHE_TERMS)
			this.cacheDistributedMatchingTerms.put(docnoCat, distTerms);
		this.addrLexMap.put(globalLexiconAddress, globalLexicon);
		return distTerms;
	}
	
	/**
	 * Get the docnos of the first and the last documents in the
	 * subcollection.
	 * @return The docnos of the first and the last documents in the
	 * subcollection.
	 */
	public String[] getDocumentBoundaries(){
		String firstDocno = index.getDocumentIndex().getDocumentNumber(0);
		String lastDocno = index.getDocumentIndex().getDocumentNumber(
				index.getDocumentIndex().getNumberOfDocuments()-1);
		String[] docnos = {firstDocno, lastDocno};
		return docnos;
	}
	
	/**
	 * Updates the statistics of the query with 
	 * the local frequencies from the local inverted file.
	 * @param query a distributed query.
	 * @return DistributedQuery the updated query for the local inverted file
	 * @throws RemoteException if there is an error in the level of RMI
	 */
	public LocalMatchingQueryTerms updateQuery(LocalMatchingQueryTerms dmqt) throws RemoteException{
		if (logger.isInfoEnabled())
			logger.info("updating query " + dmqt.queryid);	
		int TF = 0;
		String[] queryTerms = dmqt.queryTerms;
		for (int i = 0; i < queryTerms.length; i++) {
			for (int j=0; j<NumberOfFields; j++){
				if (lexicons[j].findTerm(queryTerms[i])){
					dmqt.Nt[j][i] += (double)lexicons[j].getNt();
					dmqt.TF[j][i] += (double)lexicons[j].getTF();
					TF += lexicons[j].getTF();
				}
			}
			dmqt.totalTF[i] = TF;
		}
		if (logger.isInfoEnabled())
			logger.info("query statistics updated.");
		return dmqt;
	}
	/**
	 * Teh global statistics of the three fields and the documents in
	 * the whole collection.
	 * @param numberOfDocuments The number of documents in the indices of each field.
	 * @param numberOfTokens The number of tokens in the indices of each field.
	 * @param totalNumberOfDocuments The total number of documents in the whole collection.
	 * @param totalNumberOfTokens The total number of tokens in the whole collection,
	 * which is the sum of the numbers of tokens in the three fields.
	 */
	public void setGlobalStatistics(
			long[] numberOfDocuments,
			long[] numberOfTokens, 
			long totalNumberOfDocuments,
			long totalNumberOfTokens){
		this.globalNumberOfDocuments = numberOfDocuments;
		this.globalNumberOfTokens = numberOfTokens;
		this.globalAverageDocumentLength = new double[3];
		int numberOfFields = numberOfTokens.length;
		for (int i = 0; i < numberOfFields; i++){
			if (numberOfDocuments[i]!=0)
				this.globalAverageDocumentLength[i] =
					(double)numberOfTokens[i]/(double)numberOfDocuments[i];
			else
				this.globalAverageDocumentLength[i] = 0;
		}
		this.globalTotalNumberOfTokens = totalNumberOfTokens;
		this.globalTotalNumberOfDocuments = totalNumberOfDocuments;
		this.globalTotalAverageDocumentLength =
			(double)totalNumberOfTokens/(double)totalNumberOfDocuments;
	}
	
	public void setCacheTerms(boolean cache) throws RemoteException{
		this.CACHE_TERMS = cache;
	}
	
	/**
	 * Get the matching result set.
	 * @return The result set that contains the retrieved documents for
	 * the given query.
	 */
	public ResultSet getDistributedResultSet(){
		return resultSet;
	}
	
	/**
	 * This method does the matching for a given query on a subcollection.
	 * @param dmqt The 
	 */
	public void match(DistributedMatchingQueryTerms dmqt) {
		//	the first step is to initialise the arrays of scores and document ids.
		initialise();
		if (logger.isInfoEnabled()){
			logger.info("model: "+wmodel.getInfo());
			for (int i=0; i<NumberOfFields; i++)
				logger.info("Normalisation " + (i+1) + ": " + normalisation[i].getInfo());
		}		
		
		if (this.SIMPLE_NORMALISATION){
			normalisation[0].setAverageDocumentLength(this.globalTotalAverageDocumentLength);
			normalisation[0].setNumberOfTokens(this.globalTotalNumberOfTokens);
		}
		else{
			for (int i=0; i<NumberOfFields; i++){
				normalisation[i].setAverageDocumentLength(this.globalAverageDocumentLength[i]);
				normalisation[i].setNumberOfTokens(this.globalNumberOfTokens[i]);
			}
		}
		
		this.wmodel.setParameter(this.normalisation[0].getParameter());
		wmodel.setNumberOfDocuments(this.globalTotalNumberOfDocuments);
		wmodel.setAverageDocumentLength(this.globalTotalAverageDocumentLength);
		wmodel.setNumberOfTokens(this.globalTotalNumberOfTokens);
		if (logger.isDebugEnabled())
			logger.debug("N: "+this.globalTotalNumberOfDocuments);
		
		//load in the dsms
		DocumentScoreModifier[] dsms; int NumberOfQueryDSMs = 0;
		//Disabled temporially
		dsms = (DocumentScoreModifier[])dmqt.getDocumentScoreModifiers();
		
		if (dsms!=null)
			NumberOfQueryDSMs = dsms.length;
	
		
		//and prepare for the tsms
		//TermScoreModifier[] tsms; int NumberOfQueryTSMs = 0;


		String[] queryTermStrings = dmqt.getTerms();
		//check whether we need to match an empty query.
		//if so, then return the existing result set.
		if (MATCH_EMPTY_QUERY && queryTermStrings.length == 0) {
			resultSet.setExactResultSize((int)this.globalTotalNumberOfDocuments);
			resultSet.setResultSize((int)this.globalTotalNumberOfDocuments);
			return;
		}

		//in order to save the time from references to the arrays, we create local references
		int[] docids = resultSet.getDocids();
		double[] scores = resultSet.getScores();
		short[] occurences = resultSet.getOccurrences();
		
		
		//the number of documents with non-zero score.
		numberOfRetrievedDocuments = 0;
		
		//the number of term score modifiers
		//int numOfTermModifiers = termModifiers.size();
		
		//the number of document score modifiers
		//int numOfDocModifiers = documentModifiers.size();
		
		//int numberOfModifiedDocumentScores =0;
		
		//for each query term in the query
		final int queryLength = queryTermStrings.length;
		int effTermCounter = 0;
		//boolean ignoreTermWithZeroWeight = (new Boolean(ApplicationSetup.getProperty("ignore.term.with.zero.weight",
		//"true"))).booleanValue();
		for (int i = 0; i < queryLength; i++) {
			if (dmqt.getTermWeight(queryTermStrings[i])<=0){
				if (logger.isDebugEnabled())
					logger.debug("query term " + queryTermStrings[i] +
							" with qtw="+dmqt.getTermWeight(queryTermStrings[i]) +
							" ignored from matching.");
				continue;
			}
			
			//because when the TreeNode is created, the term code assigned is taken from
			//the TermCodes class, the assigned term code is only valid during the indexing
			//process. Therefore, at this point, the term code should be updated with the one
			//stored in the lexicon file.	
			//queryTerms.setTermProperty(queryTermStrings[i], lexicon.getTermId());
			if (logger.isDebugEnabled())
				logger.debug("" + (++effTermCounter) + ": " + queryTermStrings[i].trim());

			//the weighting model is prepared for assigning scores to documents
			wmodel.setKeyFrequency(dmqt.getTermWeight(queryTermStrings[i]));
				
			//wmodel.setAverageDocumentLength(this.globalTotalAverageDocumentLength);
			//System.err.println("avl: " + this.globalTotalAverageDocumentLength);
			wmodel.setDocumentFrequency(dmqt.totalNt[i]);
				
			wmodel.setTermFrequency(dmqt.totalTF[i]);
			if (logger.isDebugEnabled()){
				logger.debug("qtw: " + dmqt.getTermWeight(queryTermStrings[i]));
				logger.debug("Nt: " + dmqt.totalNt[i]);
				logger.debug("TF: " + dmqt.totalTF[i]);
			}

			//check if the IDF is very low.
			if (IGNORE_LOW_IDF_TERMS==true && 
					(
					//(double)globalTotalNumberOfDocuments < dmqt.totalNt[i]
					//(double)globalTotalNumberOfDocuments/10 < dmqt.totalNt[i] ||
					(int)globalTotalNumberOfDocuments < dmqt.totalTF[i]  
					)
					)
			{
				if (logger.isDebugEnabled())
					logger.debug("query term " + queryTermStrings[i] + " has low idf - ignored from scoring.");
				continue;
			}
			int[][][] pointers = new int[NumberOfFields][][];
			TIntHashSet allPointers = new TIntHashSet();
			boolean[] found = new boolean[NumberOfFields];
			
			for (int f=0; f<NumberOfFields; f++){
				if (lexicons[f].findTerm(queryTermStrings[i].trim())) {
					found[f] = true;
					if (logger.isDebugEnabled()){
						logger.debug("found in lexicon "+f);
						logger.debug("TF: "+lexicons[f].getTF());
						logger.debug("Nt: "+lexicons[f].getNt());
					}
					try{
						pointers[f] = invIndexes[f].getDocuments(lexicons[f].getTermId());
						allPointers.addAll(pointers[f][0]);
					}catch(Exception e){
						logger.error("Error occurs while processing term "+
								queryTermStrings[i]+". termid: "+lexicons[f].getTermId());
						e.printStackTrace();
					}
				}
				else {
					found[f] = false;
					pointers[f] = null;
					logger.debug("not found in lexicon "+f);
				}
			}

			int[] allPointersDocids = allPointers.toArray();
			if (logger.isDebugEnabled())
				logger.debug("Number of documents to be scored: "+allPointersDocids.length);
			double[] allPointersFreqs = new double[allPointersDocids.length];
			double[] docLength = new double[allPointersDocids.length];

			int tmpDocid;
			int[] tmpIndex = new int[NumberOfFields];
			Arrays.fill(tmpIndex, -1);
			
			double[] fieldFrequency = new double[NumberOfFields];
			double[] fieldLength = new double[NumberOfFields];
			
			for (int j=0; j<normalisation.length; j++)
				normalisation[j].setAverageDocumentLength(
						this.globalAverageDocumentLength[j]);
			
			for (int j=0; j<allPointersFreqs.length; j++) {
				tmpDocid = allPointersDocids[j];
				Arrays.fill(fieldLength, 0d);
				Arrays.fill(fieldFrequency, 0d);
				for (int f=0; f<NumberOfFields; f++){
					fieldLength[f] = docIndexes[f].getDocumentLength(tmpDocid);
					if (found[f] && (tmpIndex[f] = Arrays.binarySearch(pointers[f][0],tmpDocid)) >= 0)
						fieldFrequency[f] = pointers[f][1][tmpIndex[f]];
				}
				docLength[j] = sum(fieldLength);
				
				if (!this.SIMPLE_NORMALISATION){
				    for (int f=0; f<NumberOfFields; f++){
				    	if (normalisation[f]!=null&&fieldLength[f]!=0)
				    		fieldFrequency[f] = normalisation[f].normalise(
									fieldFrequency[f], 
									fieldLength[f], 
									dmqt.TF[f][i]);
				    }
					allPointersFreqs[j] = sum(fieldFrequency);				    
				}
				else{
					double totalFrequency = 0d;
					double TF = 0d;
					for (int f=0; f<NumberOfFields; f++){
						totalFrequency += fieldFrequency[f]*normalisation[f].getFieldWeight();
						TF+=dmqt.TF[f][i];
					}
					allPointersFreqs[j] = normalisation[0].normalise(
							totalFrequency, 
							sum(fieldLength), 
							TF);
				}
			}
	
//			if (optimiseK_1) {
//				double bodyTokens = bodyColStats.getNumberOfTokens();
//				double anchorTokens = anchorColStats.getNumberOfTokens();
//				double titleTokens = titleColStats.getNumberOfTokens();
//				double old_k = original_K_1;
//				double k = old_k * (bodyTokens * normalisation[0].getFieldWeight() 
//						+ anchorTokens * normalisation[1].getFieldWeight() 
//						+ titleTokens * normalisation[2].getFieldWeight()) 
//								 / (bodyTokens+anchorTokens+titleTokens);
//				ApplicationSetup.setProperty("k",Double.toString(Rounding.round(k,2)));
//				wmodel.getInfo(); //updates the value in the weighting model
//			}
//			System.err.println("K_1: " + ApplicationSetup.getProperty("k", "1.2d"));
			double[] termScores = new double[allPointersDocids.length];
			
			//assign scores to documents for a term
			assignScores(termScores, allPointersDocids, allPointersFreqs, docLength);
			//finally setting the scores of documents for a term
			//a mask for setting the occurrences
			short mask = 0;
			if (i<16)
				mask = (short)(1 << i);
			
			int docid;
			int[] pointers10 = allPointersDocids;
			//int[] pointers11 = pointers[1];
			final int numberOfPointers = pointers10.length;
			for (int k = 0; k < numberOfPointers; k++) {
				docid = pointers10[k];
				if ((scores[docid] == 0.0d) && (termScores[k] > 0.0d)) {
					numberOfRetrievedDocuments++;
				} else if ((scores[docid] > 0.0d) && (termScores[k] < 0.0d)) {
					numberOfRetrievedDocuments--;
				}
				scores[docid] += termScores[k];
				occurences[docid] |= mask;
			}
			allPointers.clear(); allPointers = null;
			allPointersDocids = null;
			allPointersFreqs = null; docLength = null;
		}
		
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
		//output results
		if (logger.isInfoEnabled())
			logger.info("number of retrieved documents: " + numberOfRetrievedDocuments);
		//System.err.println("time to sort: " + ((sortingEnd - sortingStart) / 1000.0D));

		
		//find potential homepages with all terms in anchor text
		/*if (computeBooleanAnchorHits) {
			URLServer urlServer = new URLServer();
			
			int start = 0;
			int end = numberOfRetrievedDocuments;
			int counter = 0;
			int anchorBoolAllHits = 0;
			for (int i=start; i<end; i++) {
							
				boolean isHome = false;
				try {
					String path = urlServer.getPath(docids[i]);
					if (path.endsWith("/") || 
					    path.endsWith("/index.html") ||
					    path.endsWith("/index.htm")  ||
					    path.endsWith("/default.html") ||
					    path.endsWith("/default.htm"))
					
						isHome = true;
				} catch(IOException ioe) {
					System.err.println(ioe);
				}
				
				if (!isHome)
					continue;
				counter++;//count a potential homepage
				if (anchorOccurrences[docids[i]] == queryLength) 
					anchorBoolAllHits++;
			}
			System.err.println("anchors: anchorBoolAllHits: " + anchorBoolAllHits + " potential_homepages: " + counter);
			try {
				urlServer.close();
			} catch(IOException ioe) {
				System.err.println("IOException while closing url server");
			}
			
			//finished computing hits
		}*/
		
		//modifyScores(query, docids, scores);
		//application dependent modification of scores
		//of documents for a query
		//sorting the result set after applying each DSM
		
		int numOfDocModifiers = documentModifiers.size();
		for (int t = 0; t < numOfDocModifiers; t++) {
			if (((DocumentScoreModifier)documentModifiers.get(t)).modifyScores(index, dmqt, resultSet));
				HeapSort.descendingHeapSort(scores, docids, occurences, resultSet.getResultSize());
		}
		

		//application dependent modification of scores
		//of documents for a query, defined by this query
		/** Disabled temrialy
		for (int t = NumberOfQueryDSMs-1; t >= 0; t--) {
			dsms[t].modifyScores(index, queryTerms, resultSet);
			HeapSort.descendingHeapSort(scores, docids, occurences, resultSet.getResultSize());
		}
		*/
		
	}	
	
	/**
	 * The method assigns relevance scores to the retrieved documents.
	 * @param scores An array containing the relevance scores of the retrieved documents.
	 * @param docids An array containing the docids of the retrieved documents.
	 * @param freqs An array containing the within-document frequencies of a query term.
	 * @param docLength The An array containing the document length. This is used
	 * when applying the hypergeometric models, e.g. DLH13.
	 */
	public void assignScores(double[] scores, 
			int[] docids, 
			double[] freqs,
			double[] docLength) {	

		final int numOfPointers = docids.length;

		//for each document that contains 
		//the query term, the score is computed.
		double score;
		for (int j = 0; j < numOfPointers; j++) {
			score = wmodel.score(freqs[j], docLength[j]);

			if (score > 0) {
				scores[j] = score;				
			}
		}
	}
	/**
	 * Get the docnos of an array of docids with a given starting point and
	 * an ending one. The ending point is taken into account.
	 * @param docids An array of docids.
	 * @param start The starting point of the docids in the array that will be
	 * converted into docnos.
	 * @param end The ending point of the docids in the array that will be
	 * converted into docnos.
	 * @return The docnos corresponding to the given docids.
	 */
	public String[] getDocNumbers(int[] docids, int start, int end){
		String[] docnos = new String[end-start+1];
		DocumentIndex docIndex = index.getDocumentIndex();
		for (int i = start; i <= end; i++){
			docnos[i]=docIndex.getDocumentNumber(docids[i]);
		}
		return docnos;
	}
}

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
 * The Original Code is DistributedThreeMatching.java.
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

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Arrays;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.distr.structures.DistributedExpansionTerms;
import uk.ac.gla.terrier.distr.structures.DistributedMatchingQueryTerms;
import uk.ac.gla.terrier.distr.structures.LocalMatchingQueryTerms;
import uk.ac.gla.terrier.links.URLServer;
import uk.ac.gla.terrier.matching.QueryResultSet;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.matching.ThreeMatching;
import uk.ac.gla.terrier.matching.dsms.DocumentScoreModifier;
import uk.ac.gla.terrier.structures.BlockInvertedIndex;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.InvertedIndex;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.HeapSort;
import uk.ac.gla.terrier.utility.Rounding;

/**
 * This class provides functionalities for running retrieval on three fields in
 * a distributed setting.
 * @author Ben He(ben@dcs.gla.ac.uk)
 * @version $Revision: 1.14 $
 */
public class DistributedThreeMatching extends ThreeMatching {
	/** the logger for this class */
	private static Logger logger = Logger.getLogger("field");
	
	protected String id;
	
	protected boolean SIMPLE_NORMALISATION = false;
	
	/**
	 * The number of documents in each field in the whole collection.
	 */
	protected long[] globalNumberOfDocuments;
	
	protected THashMap cacheDistributedMatchingTerms = new THashMap();

	protected boolean CACHE_TERMS = (new Boolean(ApplicationSetup.getProperty(
			"cache.terms", "false"))).booleanValue();
	/** The body querye expansion weight. */
	protected double wqb = 1;
	/** The anchor text query expansion weight. */
	protected double wqa = 0;
	/** The title query expansion weight. */
	protected double wqt = 1;
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
	/**
	 * The default constructor.
	 * @param pathBody The path of the body index.
	 * @param prefixBody The prefix of the body index.
	 * @param pathAtext The path of the anchor text index.
	 * @param prefixAtext The prefix of the anchor text index.
	 * @param pathTitle The path of the title index.
	 * @param prefixTitle The prefix of the title index.
	 */
	public DistributedThreeMatching(
			String pathBody, String prefixBody,
			String pathAtext, String prefixAtext,
			String pathTitle, String prefixTitle) {
		super(pathBody, prefixBody,
				pathAtext, prefixAtext,
				pathTitle, prefixTitle);	
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

	public void setQEWeights(double wqb, double wqa, double wqt){
		this.wqb = wqb;
		this.wqa = wqa;
		this.wqt = wqt;
		System.out.println("wqb: "+wqb+", wqa: "+wqa+", wqt: "+wqt);
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
			length = index.getDocumentIndex().getDocumentLength(docid);
			if (index2!=null)
				length+=index2.getDocumentIndex().getDocumentLength(docid);
			if (index3!=null)		
				length+=index3.getDocumentIndex().getDocumentLength(docid);
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
			length = index.getDocumentIndex().getDocumentLength(docid);
			if (index2!=null)
				length+=index2.getDocumentIndex().getDocumentLength(docid);
			if (index3!=null)		
				length+=index3.getDocumentIndex().getDocumentLength(docid);
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
		if (index.getLexicon().findTerm(term)){
			int docid = index.getDocumentIndex().getDocumentId(docno);
			int[][] terms = index.getDirectIndex().getTerms(docid);
			int termid = index.getLexicon().getTermId();
			for (int i = 0; i < terms[0].length; i++)
				if (terms[0][i]==termid){
					tf+=terms[1][i];
					break;
				}
		}
		if (index2 !=null)
			if (index2.getLexicon().findTerm(term)){
				int docid = index2.getDocumentIndex().getDocumentId(docno);
				int[][] terms = index2.getDirectIndex().getTerms(docid);
				int termid = index2.getLexicon().getTermId();
				for (int i = 0; i < terms.length; i++)
					if (terms[0][i]==termid){
						tf+=terms[1][i];
						break;
					}
			}
		if (index3 != null)
			if (index3.getLexicon().findTerm(term)){
				int docid = index3.getDocumentIndex().getDocumentId(docno);
				int[][] terms = index3.getDirectIndex().getTerms(docid);
				int termid = index3.getLexicon().getTermId();
				for (int i = 0; i < terms.length; i++)
					if (terms[0][i]==termid){
						tf+=terms[1][i];
						break;
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
		TIntHashSet allPointers = null;
		boolean found = false;
		if (index.getLexicon().findTerm(term)) {
			found = true;
			int termid = index.getLexicon().getTermId();
			int[][] pointers = index.getInvertedIndex().getDocuments(termid);
			allPointers = new TIntHashSet(pointers[0]);
		}
		if (index2!=null && index2.getLexicon().findTerm(term)) {
			int termid = index2.getLexicon().getTermId();
			int[][] pointers = index2.getInvertedIndex().getDocuments(termid);
			if (!found)
				allPointers = new TIntHashSet(pointers[0]);
			else
				allPointers.addAll(pointers[0]);
			found = true;
		}
		
		if (index3!=null&&index3.getLexicon().findTerm(term)) {
			int termid = index3.getLexicon().getTermId();
			int[][] pointers = index3.getInvertedIndex().getDocuments(termid);
			if (!found)
				allPointers = new TIntHashSet(pointers[0]);
			else
				allPointers.addAll(pointers[0]);
			found = true;
		}
		
		return allPointers.size();
	}
	/**
	 * Get the in-subcollection term frequency of a given term.
	 * @param term The given term.
	 * @return The in-subcollection term frequency.
	 */
	public double getFullTF(String term){
		double TF = 0;
		
		if (index.getLexicon().findTerm(term)){
			TF+=(double)index.getLexicon().getTF();
		}
		if (index2!=null&&index2.getLexicon().findTerm(term)){
			TF+=(double)index2.getLexicon().getTF();
		}
		if (index2!=null&&index3.getLexicon().findTerm(term)){
			TF+=(double)index3.getLexicon().getTF();
		}
		
		return TF;
	}
	
 	
 	private int getFullLocalTF(String term){
 		int TF=0;
 		Lexicon lexicon = index.getLexicon();
 		if (lexicon.findTerm(term)){
 			TF+=lexicon.getTF();
 			lexicon.close();
 		}
 		if (index2!=null){
 			lexicon = index2.getLexicon();
 			if (lexicon.findTerm(term)){
 				TF+=lexicon.getTF();
 	 			lexicon.close();
 			}
 		}
 		if (index3!=null){
 			lexicon = index3.getLexicon();
 			if (lexicon.findTerm(term)){
 				TF+=lexicon.getTF();
 	 			lexicon.close();
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
		if (logger.isDebugEnabled())
			logger.info("Retrieving statistics for term "+term+"...");
		double TF = 0;
		int Nt = 0;		
		InvertedIndex invIndex = index.getInvertedIndex();
		InvertedIndex invIndex2 = null;
		InvertedIndex invIndex3 = null;
		if (index2!=null)
			invIndex2 = index2.getInvertedIndex();
		if (index3!=null)
			invIndex3 = index3.getInvertedIndex();
		try{
			int[] pointers1 = null;
			int[] pointers2 = null;
			int[] pointers3 = null;
			TIntHashSet hashSet = new TIntHashSet();
			if (index.getLexicon().findTerm(term) ){
					//&& 
					//!term.equalsIgnoreCase("topiclautism")) {
				int termid = index.getLexicon().getTermId();
				
				int[][] pointers = pointers = (invIndex instanceof BlockInvertedIndex)?
						((BlockInvertedIndex)invIndex).getDocumentsWithoutBlocks(termid):
						invIndex.getDocuments(termid);
				
				
				
				pointers1 = pointers[0];
				TF += index.getLexicon().getTF();
				if (pointers1!=null)
					hashSet.addAll(pointers1);
			}
			if (index2!=null&&index2.getLexicon().findTerm(term)) {
				int termid = index2.getLexicon().getTermId();
				int[][] pointers = (invIndex2 instanceof BlockInvertedIndex)?
						((BlockInvertedIndex)invIndex2).getDocumentsWithoutBlocks(termid):
						invIndex2.getDocuments(termid);
				pointers2 = pointers[0];
				TF += index2.getLexicon().getTF();
				if (pointers2!=null)
					hashSet.addAll(pointers2);
			}
			if (index3!=null&&index3.getLexicon().findTerm(term)) {
				int termid = index3.getLexicon().getTermId();
				int[][] pointers = (invIndex3 instanceof BlockInvertedIndex)?
						((BlockInvertedIndex)invIndex3).getDocumentsWithoutBlocks(termid):
						invIndex3.getDocuments(termid);
				pointers3 = pointers[0];
				TF += index3.getLexicon().getTF();
				if (pointers3!=null)
					hashSet.addAll(pointers3);
			}
			
			Nt += hashSet.size();
		}	
		catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
		
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
		
		/*TIntHashSet bodyTermids = new TIntHashSet();
		TIntHashSet atextTermids = new TIntHashSet();
		TIntHashSet titleTermids = new TIntHashSet();
		TIntObjectHashMap bodyMapTermidTerm =  new TIntObjectHashMap();
		TIntObjectHashMap atextMapTermidTerm =  new TIntObjectHashMap();
		TIntObjectHashMap titleMapTermidTerm =  new TIntObjectHashMap();*/
		
		Lexicon globalLexicon = null;
		if (this.addrLexMap.containsKey(globalLexiconAddress)){
			if (logger.isInfoEnabled())
				logger.info("Lexicon has been cached.");
			globalLexicon = (Lexicon)addrLexMap.get(globalLexiconAddress);
		}else{
			globalLexicon = new Lexicon(globalLexiconAddress);
			this.addrLexMap.put(globalLexiconAddress, globalLexicon);
		}
			
			//new CachingLexicon(globalLexiconAddress);
		Lexicon bodyLex = null;
		Lexicon anchorLex = null;
		Lexicon titleLex = null;
		bodyLex = index.getLexicon();
		if (index2 != null) anchorLex = index2.getLexicon();
		if (index3 != null) titleLex = index3.getLexicon();
		
		for (int counter=0; counter<docnos.length; counter++){
			//THashSet blackList = new THashSet();
			if (logger.isInfoEnabled())
				logger.info("parsing document "+docnos[counter]);
			int docid = index.getDocumentIndex().getDocumentId(docnos[counter]);
			if (docid >= 0){
				terms = index.getDirectIndex().getTerms(docid);
				if (terms != null){
					for (int i=0; i<terms[0].length; i++){
						int id = terms[0][i];
						int frequency = terms[1][i];
						String term = null;
						
						bodyLex.findTerm(id);
						term = bodyLex.getTerm();
							
						

// 						distTerms.insertTerm(
// 								term, 
// 								(double)frequency*normalisation[0].getFieldWeight(), 
// 								docnos[counter],
// 								globalLexicon);
						distTerms.insertTerm(
								term, 
								(double)frequency*wqb, 
								docnos[counter],
								globalLexicon);
					}
				}
			}
			if (index2!=null)
				docid = index2.getDocumentIndex().getDocumentId(docnos[counter]);
			if (docid >= 0 && index2!=null){
				terms = index2.getDirectIndex().getTerms(docid);
				if (terms != null){
					for (int i=0; i<terms[0].length; i++){
						int id = terms[0][i];
						int frequency = terms[1][i];
						String term = null;
						anchorLex.findTerm(id);
						term = anchorLex.getTerm();
							
// 						distTerms.insertTerm(
// 								term, 
// 								(double)frequency*normalisation[1].getFieldWeight(), 
// 								docnos[counter],
// 								globalLexicon);
						distTerms.insertTerm(
								term, 
								(double)frequency*wqa, 
								docnos[counter],
								globalLexicon);
					}
				}
			}
			if (index3!=null)
				docid = index3.getDocumentIndex().getDocumentId(docnos[counter]);
			if (docid >= 0 && index3!=null){
				terms = index3.getDirectIndex().getTerms(docid);
				if (terms != null){
					for (int i=0; i<terms[0].length; i++){
						int id = terms[0][i];
						int frequency = terms[1][i];
						String term = null;
						titleLex.findTerm(id);
						term = titleLex.getTerm();
							
// 						distTerms.insertTerm(
// 								term, 
// 								(double)frequency*normalisation[2].getFieldWeight(), 
// 								docnos[counter],
// 								globalLexicon);
						distTerms.insertTerm(
								term, 
								(double)frequency*wqt, 
								docnos[counter],
								globalLexicon);
					}
				}
			}
		}
		/*bodyTermids.clear(); bodyTermids = null;
		atextTermids.clear(); atextTermids = null;
		titleTermids.clear(); titleTermids = null;
		bodyMapTermidTerm.clear(); bodyMapTermidTerm = null;
		atextMapTermidTerm.clear(); atextMapTermidTerm = null;
		titleMapTermidTerm.clear(); titleMapTermidTerm = null;*/
		//globalLexicon.close();
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
		//	for each query term in the query
		Lexicon lexBody = index.getLexicon(); 
		//InvertedIndex invIndex = index.getInvertedIndex();
		InvertedIndex invIndex2 = null;
		InvertedIndex invIndex3 = null;
		if (invIndex2 != null)
			invIndex2 = index2.getInvertedIndex();
		if (invIndex3 != null)
			invIndex3 = index3.getInvertedIndex();
		
		Lexicon lexAtext = null;
		if (index2!=null){
			lexAtext = index2.getLexicon();
		}
		Lexicon lexTitle = null;
		if (index3!=null){
			lexTitle = index3.getLexicon();
		}
		int TF = 0;
		//TIntHashSet hashSet = new TIntHashSet();
		String[] queryTerms = dmqt.queryTerms;
		for (int i = 0; i < queryTerms.length; i++) {
			boolean found = lexBody.findTerm(queryTerms[i]);
			if (found){
				dmqt.Nt[0][i] += (double)lexBody.getNt();
				dmqt.TF[0][i] += (double)lexBody.getTF();
				TF += lexBody.getTF();
			}
			found = false;
			if (index2!=null)
				found = lexAtext.findTerm(queryTerms[i]);
			if (found){
				dmqt.Nt[1][i] += (double)lexAtext.getNt();
				dmqt.TF[1][i] += (double)lexAtext.getTF();
				TF += lexAtext.getTF();
			}
			found = false;
			if (index3!=null)
				found = lexTitle.findTerm(queryTerms[i]);
			if (found){
				dmqt.Nt[2][i] += (double)lexTitle.getNt();
				dmqt.TF[2][i] += (double)lexTitle.getTF();
				TF += lexTitle.getTF();
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
		//the first step is to initialise the arrays of scores and document ids.
		initialise();
		if (logger.isDebugEnabled()){
			logger.debug("model: "+wmodel.getInfo());
			logger.debug("normalisation for body: " + normalisation[0].getInfo());
			logger.debug("normalisation for anchor: " + normalisation[1].getInfo());
			logger.debug("normalisation for title: " + normalisation[2].getInfo());
		}
		
		if (this.SIMPLE_NORMALISATION){
			normalisation[0].setAverageDocumentLength(this.globalTotalAverageDocumentLength);
			normalisation[0].setNumberOfTokens(this.globalTotalNumberOfTokens);
		}
		else{
			normalisation[0].setAverageDocumentLength(this.globalAverageDocumentLength[0]);
			normalisation[0].setNumberOfTokens(this.globalNumberOfTokens[0]);
			normalisation[1].setAverageDocumentLength(this.globalAverageDocumentLength[1]);
			normalisation[1].setNumberOfTokens(this.globalNumberOfTokens[1]);
			normalisation[2].setAverageDocumentLength(this.globalAverageDocumentLength[2]);
			normalisation[2].setNumberOfTokens(this.globalNumberOfTokens[2]);
		}
		
		this.wmodel.setParameter(this.normalisation[0].getParameter());
		wmodel.setNumberOfDocuments(this.globalTotalNumberOfDocuments);
		wmodel.setAverageDocumentLength(this.globalTotalAverageDocumentLength);
		//System.err.println("N: "+this.globalTotalNumberOfDocuments);
		
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
		
		//the pointers read from the inverted file
		int[][] pointers1 = null;
		int[][] pointers2 = null;
		int[][] pointers3 = null;
		
		//the number of term score modifiers
		//int numOfTermModifiers = termModifiers.size();
		
		//the number of document score modifiers
		//int numOfDocModifiers = documentModifiers.size();
		
		//int numberOfModifiedDocumentScores =0;
		Lexicon lexBody = index.getLexicon();
		Lexicon lexAtext = null;
		if (index2!=null)
			lexAtext = index2.getLexicon();
		Lexicon lexTitle = null;
		if (index3!=null)
			lexTitle = index3.getLexicon();
		
		//for each query term in the query
		final int queryLength = queryTermStrings.length;
		int effTermCounter = 0;
		//boolean ignoreTermWithZeroWeight = (new Boolean(ApplicationSetup.getProperty("ignore.term.with.zero.weight",
		//"true"))).booleanValue();
		for (int i = 0; i < queryLength; i++) {
			if (dmqt.getTermWeight(queryTermStrings[i])<=0){
				if (logger.isInfoEnabled())
					logger.info("query term " + queryTermStrings[i] +
						" with qtw="+dmqt.getTermWeight(queryTermStrings[i]) +
						" ignored from matching.");
				continue;
			}
			
			boolean foundBody = lexBody.findTerm(queryTermStrings[i]);
			boolean foundAtext = false;
			if (index2!=null)
				foundAtext = lexAtext.findTerm(queryTermStrings[i]);
			boolean foundTitle = false;
			if (index3!=null)
				foundTitle = lexTitle.findTerm(queryTermStrings[i]);
			
			//and if it is not found, we continue with the next term
			if (!(foundBody||foundAtext||foundTitle))
				continue;
			//because when the TreeNode is created, the term code assigned is taken from
			//the TermCodes class, the assigned term code is only valid during the indexing
			//process. Therefore, at this point, the term code should be updated with the one
			//stored in the lexicon file.	
			//queryTerms.setTermProperty(queryTermStrings[i], lexicon.getTermId());
			if (logger.isDebugEnabled())
				logger.debug("" + (++effTermCounter) + ": " + queryTermStrings[i].trim());
			if (foundBody) {
				if (logger.isDebugEnabled()){
					logger.debug("lexicon of body");
					//System.err.println("term : " + dmqt.getTerms()[i]);
					logger.debug("TF in body: " + dmqt.TF[0][i]);
					logger.debug("Nt in body: " + dmqt.Nt[0][i]);
				}
			} else {
				if (logger.isDebugEnabled())
					logger.debug("lexicon of body: not found");
			}
			if (foundAtext) {
				if (logger.isDebugEnabled()){
					logger.debug("lexicon of atext");
					//System.err.println("term : " + dmqt.getTerms()[i]);
					logger.debug("TF in atext: " + dmqt.TF[1][i]);
					logger.debug("Nt in atext: " + dmqt.Nt[1][i]);
				}	
			} else {
				if (logger.isDebugEnabled())
					logger.debug("lexicon of atext: not found");
			}
			if (foundTitle) {
				if (logger.isDebugEnabled()){
					logger.debug("lexicon of title");
					//System.err.println("term : " + dmqt.getTerms()[i]);
					logger.debug("TF in title: " + dmqt.TF[2][i]);
					logger.debug("Nt in title: " + dmqt.Nt[2][i]);
				}	
			} else {
				if (logger.isDebugEnabled())
					logger.debug("lexicon of title: not found");
			}

			//the weighting model is prepared for assigning scores to documents
			wmodel.setKeyFrequency(dmqt.getTermWeight(queryTermStrings[i]));
			
			//wmodel.setAverageDocumentLength(this.globalTotalAverageDocumentLength);
			//System.err.println("avl: " + this.globalTotalAverageDocumentLength);
			wmodel.setDocumentFrequency(dmqt.totalNt[i]);
			wmodel.setTermFrequency(dmqt.totalTF[i]);
			if (logger.isDebugEnabled()){
				logger.debug("Nt: " + dmqt.totalNt[i]);
				logger.debug("qtw: " + dmqt.getTermWeight(queryTermStrings[i]));
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
				if (logger.isInfoEnabled())
					logger.info("query term " + queryTermStrings[i] + " has low idf - ignored from scoring.");
				continue;
			}
			
	
			if (foundBody) {
				try{
					pointers1 = invertedIndex.getDocuments(lexBody.getTermId());
				}catch(Exception e){
					logger.error("Error occurs while processing term "+
							queryTermStrings[i]+". termid: "+lexBody.getTermId());
					e.printStackTrace();
				}
			}
			if (foundAtext) {
				pointers2 = index2.getInvertedIndex().getDocuments(lexAtext.getTermId());
			}
			//not adding the frequency from the title, because it 
			//is already counted in the body of the document.
			if (foundTitle) {
				pointers3 = index3.getInvertedIndex().getDocuments(lexTitle.getTermId());
			}
			
			TIntHashSet allPointers = null;
			if (foundBody) {
				allPointers = new TIntHashSet(pointers1[0]);
				if (foundAtext)
					allPointers.addAll(pointers2[0]);
				if (foundTitle)
					allPointers.addAll(pointers3[0]);
			} else if (foundAtext) {
				allPointers = new TIntHashSet(pointers2[0]);
				if (foundBody)
					allPointers.addAll(pointers1[0]);
				if (foundTitle)
					allPointers.addAll(pointers3[0]);
			} else if (foundTitle) {
				allPointers = new TIntHashSet(pointers3[0]);
				if (foundBody) 
					allPointers.addAll(pointers1[0]);
				if (foundAtext) 
					allPointers.addAll(pointers2[0]);
			}

			int[] allPointersDocids = allPointers.toArray();
			if (logger.isDebugEnabled())
				logger.debug("Number of documents to be scored: "+allPointersDocids.length);
			double[] allPointersFreqs = new double[allPointersDocids.length];
			double[] docLength = new double[allPointersDocids.length];

			int tmpDocid;
			int tmpIndex1 = -1;
			int tmpIndex2 = -1;
			int tmpIndex3 = -1;

			double titleFrequency = 0;
			double anchorFrequency = 0;
			double bodyFrequency = 0;
			
			double titleLength = 0;
			double anchorLength = 0;
			double bodyLength = 0;
			//double length = 0;
			
			for (int j=0; j<normalisation.length; j++)
				normalisation[j].setAverageDocumentLength(
						this.globalAverageDocumentLength[j]);
			
			for (int j=0; j<allPointersFreqs.length; j++) {
				tmpDocid = allPointersDocids[j];
				titleFrequency = anchorFrequency = bodyFrequency = 0;
				try{
					bodyLength = index.getDocumentIndex().getDocumentLength(tmpDocid);
				}
				catch(Exception e){
					logger.error("tmpDocid: "+tmpDocid);
					e.printStackTrace();
					System.exit(1);
				}
				if (index2!=null)
					anchorLength = index2.getDocumentIndex().getDocumentLength(tmpDocid);
				if (index3!=null)
					titleLength = index3.getDocumentIndex().getDocumentLength(tmpDocid);
				
				if (foundBody && (tmpIndex1 = Arrays.binarySearch(pointers1[0],tmpDocid)) >= 0) {
					bodyFrequency = pointers1[1][tmpIndex1];
				}

				if (foundAtext && (tmpIndex2 = Arrays.binarySearch(pointers2[0],tmpDocid)) >= 0) {
					//anchorOccurrences[tmpDocid]++;
					anchorFrequency = pointers2[1][tmpIndex2];
				}
				
				if (foundTitle && (tmpIndex3 = Arrays.binarySearch(pointers3[0], tmpDocid)) >= 0) {
					//titleOccurences[tmpDocid]++;
					titleFrequency = pointers3[1][tmpIndex3];
				}
				docLength[j] = bodyLength+anchorLength+titleLength;
				
				if (!this.SIMPLE_NORMALISATION){
				    //System.out.println("simple.normalisation=false");
					if (normalisation[0]!=null && bodyLength!=0){
						bodyFrequency = normalisation[0].normalise(
								bodyFrequency, 
								bodyLength, 
								dmqt.TF[0][i]);
					}
					if (normalisation[1]!=null && anchorLength!=0){
						anchorFrequency = normalisation[1].normalise(
								anchorFrequency, 
								anchorLength, 
								dmqt.TF[1][i]);
					}
					if (normalisation[2]!=null && titleLength!=0){
						titleFrequency = normalisation[2].normalise(
								titleFrequency, 
								titleLength, 
								dmqt.TF[2][i]);
					}
					allPointersFreqs[j] = bodyFrequency + titleFrequency + anchorFrequency;
				}
				else{
				    //System.out.println("simple.normalisation=true");
					double totalFrequency = bodyFrequency + 
						titleFrequency*normalisation[2].getFieldWeight() + 
						anchorFrequency*normalisation[1].getFieldWeight();
					allPointersFreqs[j] = normalisation[0].normalise(
							totalFrequency, 
							bodyLength+anchorLength+titleLength, 
							dmqt.TF[0][i]+dmqt.TF[1][i]+dmqt.TF[2][i]);
				}
			}
	
			if (optimiseK_1) {
				double bodyTokens = bodyColStats.getNumberOfTokens();
				double anchorTokens = anchorColStats.getNumberOfTokens();
				double titleTokens = titleColStats.getNumberOfTokens();
				double old_k = original_K_1;
				double k = old_k * (bodyTokens * normalisation[0].getFieldWeight() 
						+ anchorTokens * normalisation[1].getFieldWeight() 
						+ titleTokens * normalisation[2].getFieldWeight()) 
								 / (bodyTokens+anchorTokens+titleTokens);
				ApplicationSetup.setProperty("k",Double.toString(Rounding.round(k,2)));
				wmodel.getInfo(); //updates the value in the weighting model
			}
			//System.err.println("K_1: " + ApplicationSetup.getProperty("k", "1.2d"));
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
		if (computeBooleanAnchorHits) {
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
			//System.err.println("anchors: anchorBoolAllHits: " + anchorBoolAllHits + " potential_homepages: " + counter);
			try {
				urlServer.close();
			} catch(IOException ioe) {
				logger.error("IOException while closing url server");
			}
			
			//finished computing hits
		}
		
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

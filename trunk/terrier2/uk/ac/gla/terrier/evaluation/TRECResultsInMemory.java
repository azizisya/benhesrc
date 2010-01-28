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
 * The Original Code is TRECResultsInMemory.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 * Ben He <ben{a.}dcs.gla.ac.uk> 
 */
package uk.ac.gla.terrier.evaluation;
import gnu.trove.TDoubleArrayList;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TObjectIntHashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;
/**
 * Loads the result file in memory, for analysis on the result set.
 * @author Ben He
 * @version $Revision: 1.1 $
 */
public class TRECResultsInMemory{
	
	public class RetrievedDoc implements Comparable<RetrievedDoc>{
		public String docno;
		public int rank;
		public double score;
		
		public RetrievedDoc(String docno, double score) {
			super();
			this.docno = docno;
			this.score = score;
		}
		
		public RetrievedDoc clone(){
			RetrievedDoc obj = new RetrievedDoc(docno, score);
			return obj;
		}
		
		public int compareTo(RetrievedDoc o) {
			if (o.score > this.score) {
				return 1;
			}
			else if (o.score < this.score) {
				return -1;
			}
			else {
				return 0;
			}
		}
	}
	
	public class QueryResult{
		protected String qid;
		
		protected RetrievedDoc[] docs;
		
		protected THashSet<RetrievedDoc> docSet = new THashSet<RetrievedDoc>();
		
		protected TObjectIntHashMap<String> docRankMap = new TObjectIntHashMap<String>();
		
		public QueryResult clone(){
			QueryResult obj = new QueryResult(qid);
			for (RetrievedDoc doc : docs)
				obj.insertDoc(doc.docno, doc.score);
			obj.finalize();
			return obj;
		}
		
		public String getDocno(int rank){
			if (docs.length <= rank)
				return null;
			return docs[rank].docno;
		}
		
		public QueryResult(String qid){
			this.qid = qid;
		}
		
		public void insertDoc(String docno, double score){
			docSet.add(new RetrievedDoc(docno, score));
		}
		
		public void finalize(){
			docs = docSet.toArray(new RetrievedDoc[docSet.size()]);
			Arrays.sort(docs);
			docSet.clear();
			docSet = null;
			int rank = 0;
			for (RetrievedDoc doc : docs){
				doc.rank = rank++;
				docRankMap.put(doc.docno, doc.rank);
			}
		}
		
		public int getRank(String docno){
			return docs[docRankMap.get(docno)].rank;
		}
		
		public double[] getScores(){
			TDoubleArrayList scoreList = new TDoubleArrayList();
			for (RetrievedDoc doc : docs)
				scoreList.add(doc.score);
			return scoreList.toNativeArray();
		}
		
		public String[] getDocnos(){
			String[] docnos = new String[docs.length];
			for (int i=0; i<docs.length; i++)
				docnos[i] = docs[i].docno;
			return docnos;
		}
		
		public double getScore(String docno){
			if (!isRetrieved(docno))
				return 0d;
			return docs[docRankMap.get(docno)].score;
		}
		
		public int getNumberOfRetrieved(){
			return docs.length;
		}
		
		public boolean isRetrieved(String docno){
			return docRankMap.containsKey(docno);
		}
		
		public RetrievedDoc[] getDocs(){
			return docs.clone();
		}
	}
	
	/** 
	 * Each element in the array contains the result set retrieved for a given query.
	 */  
	public THashMap<String, QueryResult> resultsPerQuery;
	/**
	 * A hash set containing queryids in the result file.
	 */
	public THashSet<String> queryids;
	
	/**
	 * The result file.
	 */
	protected File fResults;
	
	protected int lowRank;
	
	protected int highRank;
	
	/** The total number of retrieved documents. */
	public int totalNumberOfRetrievedDocs;
	
	/** 
	 * A constructor that creates an instance of the class
	 * and loads in memory the result set from the 
	 * given file.
	 * @param resultFilename String The full path of the result file to load.
	 */
	public TRECResultsInMemory(String resultFilename){
		fResults = new File(resultFilename);
		this.lowRank = -1;
		this.highRank = 0;
		this.loadResultsFile();
	}
	
	/** 
	 * A constructor that creates an instance of the class
	 * and loads in memory the result set from the 
	 * given file.
	 * @param resultFilename String The full path of the result file to load.
	 */
	public TRECResultsInMemory(String resultFilename, int _lowRank, int _highRank){
		fResults = new File(resultFilename);
		this.lowRank = _lowRank;
		this.highRank = _highRank;
		this.loadResultsFile();
	}
	/**
	 * Get the identifiers of the queries existed in the result file.
	 * @return The query identifiers in the result file.
	 */
	public String[] getQueryids(){
		String[] ids = (String[])queryids.toArray(new String[queryids.size()]);
		java.util.Arrays.sort(ids);
		return ids;
	}
	/**
	 * Get the identifiers (docnos) of the retrieved documents for a given query. 
	 * @param queryid The identifier of the given query.
	 * @return The identifiers (docnos) of the retrieved documents for a given query.
	 */
	public String[] getRetrievedDocnos(String queryid){
		String[] docnos = ((QueryResult)resultsPerQuery.get(queryid)).getDocnos();
		return docnos;
	}
	
	public RetrievedDoc[] getRetrievedDocuments(String queryid){
		return resultsPerQuery.get(queryid).getDocs();
	}
	
	public RetrievedDoc[] getRetrievedDocs(String queryid){
		return resultsPerQuery.get(queryid).getDocs();
	}
	
	/**
	 * For a given query, get the identifiers (docnos) of the retrieved documents whose
	 * ranks are higher than or equal to the given minimum rank (the smaller the rank value is, 
	 * the higher the rank is). 
	 * @param queryid The identifier of the given query.
	 * @param minRank The lowest rank.
	 * @return The identifiers (docnos) of the retrieved documents for a given query.
	 */
	public String[] getRetrievedDocnos(String queryid, int minRank){
		String[] docnos = ((QueryResult)resultsPerQuery.get(queryid)).getDocnos();
		THashSet<String> docnoSet = new THashSet<String>();
		for (int i=0; i<docnos.length; i++){
			if (this.getRank(queryid, docnos[i])<=minRank)
				docnoSet.add(docnos[i]);
		}
		return (String[])docnoSet.toArray(new String[docnoSet.size()]);
	} 
	
	public String getDocno(String qid, int rank){
		if (resultsPerQuery.get(qid)==null)
			return null;
		return resultsPerQuery.get(qid).getDocno(rank);
	}
	
	/**
	 * Get the scores of the retrieved documents for a given query as ordered in the result file·
	 * @param queryid The identifier of the given query.
	 * @return The scores of the retrieved documents for the given query.
	 */
	public double[] getScores(String queryid){
		return ((QueryResult)resultsPerQuery.get(queryid)).getScores();
	}
	/**
	 * Get the identifiers of the retrieved documents for a given query in decreasing order of ranking.
	 * @param queryid The identifier of the given query.
	 * @return The documents identifiers in decreasing order of ranking retrieved for a given query.
	 */
	public String[] getDocnoSet(String queryid){
		return ((QueryResult)resultsPerQuery.get(queryid)).getDocnos();
	}
	/**
	 * Get the score of a given document retrieved for a given query.
	 * @param queryid The query identifier.
	 * @param docno The document identifier.
	 * @return The relevance socre of a given document retrieved for a given query.
	 */
	public double getScore(String queryid, String docno){
		return resultsPerQuery.get(queryid).getScore(docno);
	}
	
	/** 
	 * Returns the total number of queries contained in the
	 * loaded result file.
	 * @return int The number of unique queries in the loaded result file.
	 */
	public int getNumberOfQueries(){
		return queryids.size();
	}
	
	/**
	 * Returns the numbe of retrieved documents for a given query.
	 * @param queryid The identifier of a query.
	 * @return int The number of retrieved documents for the given query.
	 */
	public int getNumberOfRetrieved(String queryid){
		return resultsPerQuery.get(queryid).getNumberOfRetrieved();
	}
	/**
	 * Get the rank of a given document retrieved for a given query.
	 * @param queryid The query identifier.
	 * @param docno The document identifier.
	 * @return The rank of the given document retrieved for the given query.
	 */
	public int getRank(String queryid, String docno){
		return resultsPerQuery.get(queryid).getRank(docno);
	}
	
	public boolean isRetrieved(String queryid, String docno){
		return resultsPerQuery.get(queryid).isRetrieved(docno);
	}
	
	/**
	 * Load in memory the result file.
	 */
	protected void loadResultsFile(){
		queryids = new THashSet<String>();
		try{
			int qrelsCounter = 0;
			BufferedReader br = Files.openFileReader(fResults.getAbsolutePath());
			String preQueryid = "1st";
			this.resultsPerQuery = new THashMap<String, QueryResult>();
			QueryResult results = null;
			this.totalNumberOfRetrievedDocs = 0;
			
			String str = null;
			while ((str=br.readLine()) != null ){
				if (str.trim().length() == 0)
					continue;
				StringTokenizer stk = new StringTokenizer(str);
				String queryid = stk.nextToken();
				if (ApplicationSetup.getProperty("qrels.remove.prefix", "false").equals("true")){	
					// takes only the numeric chars at the end of an query id to
					// cope with ids like "WT04-065", which is interpretated as "65"
					String queryNoTmp = new String();
					boolean firstNumericChar = false;
					for (int i = queryid.length()-1; i >=0; i--){
						if (queryid.charAt(i) >= '0' && queryid.charAt(i) <= '9'){
							queryNoTmp = queryid.charAt(i)+queryNoTmp;
							firstNumericChar = true;
						}
						else if (firstNumericChar)
							break;
					}
					queryid = new String(""+Integer.parseInt(queryNoTmp));
				}
				stk.nextToken();
				String docno = stk.nextToken();
				if (!queryid.equals(preQueryid)){
					if (preQueryid.equals("1st")){
						results = new QueryResult(queryid);
						preQueryid = queryid;
					}
					else{
						queryids.add(preQueryid);
						results.finalize();
						resultsPerQuery.put(preQueryid, results.clone());
						this.totalNumberOfRetrievedDocs+=results.getNumberOfRetrieved();
						results = new QueryResult(queryid);
						preQueryid = queryid;
					}
				}
				try{
					int rank = Integer.parseInt(stk.nextToken());
					double score = Double.parseDouble(stk.nextToken());
					if (highRank >= lowRank && rank >= lowRank && rank <= highRank){
						//System.err.println("insert "+docno+", "+rank);
						results.insertDoc(docno, score);
					}else if (lowRank==-1)
						results.insertDoc(docno, score);
				}catch(NoSuchElementException e){
					System.err.println(str);
					e.printStackTrace();
					System.exit(1);
				}
			}
			queryids.add(preQueryid);
			results.finalize();
			resultsPerQuery.put(preQueryid, results.clone());
			this.totalNumberOfRetrievedDocs+=results.getNumberOfRetrieved();
			results = null;
			br.close();
		}
		catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	
	/**
	 * Checks whether there is a query with a given identifier
	 * in the result file. 
	 * @param queryid the identifier of a query.
	 * @return true if the given query exists in the result file, 
	 *         false otherwise.
	 */
	public boolean queryExistInResults(String queryid){
		return queryids.contains(queryid);
	}
	
	/**
	 * Models the set of retrieved documents for one query. 
	 *//*
	public class ResultsHashSet{
		*//** The identifier of the query.*//*
		public String queryid;
		
		*//** Mapping from rank to docno.*//*
		public TIntObjectHashMap<String> rankDocnoMap;
		*//** Mapping from rank to score. *//*
		public TIntDoubleHashMap rankScoreMap;
		*//** The ranks of the retrieved documents. *//*
		public TIntHashSet rankSet;
		*//** Mapping from docno to rank. *//*
		public TObjectIntHashMap<String> docnoRankMap;
		
		*//** 
		 * Creates the an instance of the class with a given 
		 * query identifier.
		 * @param queryid String the query identifier.
		 *//*
		public ResultsHashSet(String queryid){
			this.queryid = queryid;
			rankDocnoMap = new TIntObjectHashMap<String>();
			rankScoreMap = new TIntDoubleHashMap();
			rankSet = new TIntHashSet();
		}
		*//**
		 * Get the ranks of the retrieved documents in numerically increasing order.
		 * @return The ranks of the retrieved documents.
		 *//*
		public int[] getRanks(){
			int[] ranks = rankSet.toArray();
			Arrays.sort(ranks);
			return ranks;
		}
		*//**
		 * Get the identifiers of the retrieved documents.
		 * @return The identifiers of the retrieved documents in decreasing order of relevance ranking.
		 *//*
		public String[] getDocnos(){
			int[] ranks = getRanks();
			String[] docnos = new String[ranks.length];
			for (int i=0; i<ranks.length; i++)
				docnos[i] = rankDocnoMap.get(ranks[i]);
			return docnos;
		}
		*//**
		 * Get the relevance scores of the retrieved documents.
		 * @return The relevance scores of the retrieved documents in decreasing order of relevance ranking.
		 *//*
		public double[] getScores(){
			int[] ranks = getRanks();
			double[] scores = new double[ranks.length];
			for (int i=0; i<ranks.length; i++)
				scores[i] = rankScoreMap.get(ranks[i]);
			return scores;
		}
		*//**
		 * Get the rank of the given document.
		 * @param docno The document identifier.
		 * @return The rank of the given document.
		 *//*
		public int getRank(String docno){
			if (docnoRankMap == null)
				this.initInverseHash();
			if (docnoRankMap.containsKey(docno))
				return docnoRankMap.get(docno);
			return -1;
		}
		
		public boolean isRetrieved(String docno){
			return rankDocnoMap.containsValue(docno);
		}
		
		*//**
		 * Reverse the mapping between document identifiers and their rankings.
		 *//*
		private void initInverseHash(){
			int[] ranks = getRanks();
			docnoRankMap = new TObjectIntHashMap<String>();
			for (int i=0; i<ranks.length; i++)
				docnoRankMap.put(rankDocnoMap.get(ranks[i]), ranks[i]);
		}
		*//**
		 * Get the relevance score of a document document.
		 * @param docno The document identifier.
		 * @return The relevance score.
		 *//*
		public double getScore(String docno){
			int rank = getRank(docno);
			if (rankScoreMap.contains(rank))
				return rankScoreMap.get(rank);
			return -1;
		}
		
		*//** 
		 * Creates a clone of the current instance of the class.
		 * @return Object the clone of the current object.
		 *//*
		public Object clone(){
			ResultsHashSet dup = null;
			try{
				dup = (ResultsHashSet)super.clone();
			}catch(CloneNotSupportedException e){dup = new ResultsHashSet(queryid);}
			dup.rankSet = (TIntHashSet)rankSet.clone();
			dup.rankScoreMap = (TIntDoubleHashMap)rankScoreMap.clone();
			dup.rankDocnoMap = (TIntObjectHashMap<String>)rankDocnoMap.clone();
			return dup;
		}
		*//**
		 * Get the number of retrieved documents.
		 * @return The number of retrieved documents.
		 *//*
		public int getNumberOfRetrieved(){
			return rankSet.size();
		}
		
		*//**
		 * Adds a document identifier in the set of 
		 * retrieved documents for the query.
		 * @param docno String the identifier of a reretrieved document.
		 *//*
		public void insertTuple(String docno, double score, int rank){
			rankSet.add(rank);
			this.rankDocnoMap.put(rank, docno);
			rankScoreMap.put(rank, score);
		}
	}*/
}

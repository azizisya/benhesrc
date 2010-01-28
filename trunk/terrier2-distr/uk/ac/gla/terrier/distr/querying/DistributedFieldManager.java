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
  * The Original Code is DistributedThreeManager.java.
  *
  * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
  * All Rights Reserved.
  *
  * Contributor(s):
  *   Ben He <ben{a.}dcs.gla.ac.uk> (original author)
  *   Craig McDonald <craigm{a.}dcs.gla.ac.uk>
  *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
  */
package uk.ac.gla.terrier.distr.querying;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntObjectHashMap;

import java.io.File;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.distr.matching.DistributedMatchingServer;
import uk.ac.gla.terrier.distr.structures.DistributedExpansionTerms;
import uk.ac.gla.terrier.distr.structures.DistributedMatchingQueryTerms;
import uk.ac.gla.terrier.distr.structures.DistributedThread;
import uk.ac.gla.terrier.distr.structures.DistributedThreadGetFullStats;
import uk.ac.gla.terrier.distr.structures.DistributedThreadGetTerms;
import uk.ac.gla.terrier.distr.structures.DistributedThreadRetrieve;
import uk.ac.gla.terrier.distr.structures.DistributedThreadUpdateQuery;
import uk.ac.gla.terrier.distr.structures.LocalMatchingQueryTerms;
import uk.ac.gla.terrier.matching.QueryResultSet;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.matching.dsms.BooleanScoreModifier;
import uk.ac.gla.terrier.matching.models.Idf;
import uk.ac.gla.terrier.querying.FieldManager;
import uk.ac.gla.terrier.querying.Request;
import uk.ac.gla.terrier.querying.SearchRequest;
import uk.ac.gla.terrier.querying.parser.FieldQuery;
import uk.ac.gla.terrier.querying.parser.Query;
import uk.ac.gla.terrier.querying.parser.RequirementQuery;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.structures.LexiconInputStream;
import uk.ac.gla.terrier.structures.LexiconOutputStream;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.HeapSort;
import uk.ac.gla.terrier.utility.Rounding;
import uk.ac.gla.terrier.utility.TerrierTimer;

 /**
   * This class is responsible for handling/co-ordinating the main high-level
   * operations of a query. These are:
   * <li>Pre Processing (Term Pipeline, Control finding, term aggregration)</li>
   * <li>Matching</li>
   * <li>Post-processing @see uk.ac.gla.terrier.querying.PostProcess</li>
   * <li>Post-filtering @see uk.ac.gla.terrier.querying.PostFilter</li>
   * </ul>
   * Example usage:
   * <pre>
   * Manager m = new Manager(index);
   * SearchRequest srq = m.newSearchRequest();
   * srq.setQuery(query);
   * m.runPreProcessing(srq);
   * m.runMatching(srq);
   * m.runPostProcess(srq);
   * m.runPostFilters(srq);
   * </pre>
   * <p>
   * <b>Properties</b><ul>
   * <li><tt>querying.default.controls</tt> - sets the default controls for each query</li>
   * <li><tt>querying.allowed.controls</tt> - sets the controls which a users is allowed to set in a query</li>
   * <li><tt>querying.postprocesses.order</tt> - the order post processes should be run in</li>
   * <li><tt>querying.postprocesses.controls</tt> - mappings between controls and the post processes they should cause</li>
   * <li><tt>querying.postfilters.order</tt> - the order post filters should be run in </li>
   * <li><tt>querying.postfilters.controls</tt> - mappings between controls and the post filters they should cause</li>
   * </ul>
   * <p><b>Controls</b><ul>
   * <li><tt>start</tt> : The result number to start at - defaults to 0 (1st result)</li>
   * <li><tt>end</tt> : the result number to end at - defaults to 0 (display all results)</li>
   * <li><tt>c</tt> : the c parameter for the DFR models, or more generally, the parameters for weighting model scheme</li>
   * </ul>
   */

 public class DistributedFieldManager extends FieldManager
 {
	/** the logger for this class */
	private static Logger logger = Logger.getLogger("broker");
	 /**
	  * The total number of documents in the collection.
	  */
 	public long totalNumOfDocuments;
 	/**
 	 * The total number of pointers in the collection.
 	 */
 	public long totalNumOfPointers; 
 	/**
 	 * The total number of tokens in the collection.
 	 */
 	public long totalNumOfTokens;
 	
 	protected long SLEEPING_TIME = 200;
 	/**
 	 * The number of pointers in each field. This is a 3-element
 	 * array. Each element contains the number of pointers in body,
 	 * anchor text and title, respectively.
 	 */
 	public long[] totalNumOfPointersInFields;
 	/**
 	 * The number of tokens in each field. This is a 3-element
 	 * array. Each element contains the number of tokens in body,
 	 * anchor text and title, respectively.
 	 */
 	public long[] totalNumOfTokensInFields;
 	/**
 	 * The number of documents in each field. This is a 3-element
 	 * array. Each element contains the number of documents in body,
 	 * anchor text and title, respectively.
 	 */
 	public long[] totalNumOfDocumentsInFields;
 	/**
 	 * The average document length in the collection.
 	 */
 	public double avl;
 	
 	protected THashMap queryResultMap = new THashMap();
 	
 	protected boolean CACHE_TERMS = (new Boolean(ApplicationSetup.getProperty(
			"cache.terms", "false"))).booleanValue();
 	
 	/**
 	 * The global lexicon of the collection. It contains the global
 	 * in-collection term frequency and document frequency of each
 	 * term in vocabulary.
 	 */
 	public Lexicon globalLexicon;
 	
 	public String globalLexiconAddress;
 	
 	
 	
 	
 	/**
 	 * A boolean variable indicating if the collection statistics have been
 	 * updated. True for yes and false for no. 
 	 */
 	public boolean statisticsUpToDate = false;
 	/**
 	 * The references of the servers that mount the subcollections.
 	 */
 	public DistributedMatchingServer[] distMatch = null;
 	/**
 	 * The result sets returned from the servers.
 	 */
 	public ResultSet[] distResult = null;
 	/**
 	 * The query threads for the servers. These are used for implementing
 	 * the threaded retrieval applications in a distributed setting. 
 	 */
 	public DistributedThread[] distThread = null;
 	/**
 	 * The host names of the servers.
 	 */
 	public String[] serverNames = null;
 	/**
 	 * The number of top-ranked documents that are used for query expansion from
 	 * each server. This is used for analysis of the query expansion using external
 	 * resource.
 	 */
	public int[] numberOfTopRankedDocs;
 	
 	/**
 	 * The ids of the servers. These are assigned while starting the servers.
 	 */
 	public String[] ids = null;
 	/**
 	 * The final result set. It is merged from the result sets returned from
 	 * different servers.
 	 */
 	public ResultSet drs = null;
 	
 	/**
 	 * The size of the result set. It is set to 1000 by default. 
 	 */
 	public int numberOfRetrievedDocuments = Integer.parseInt(
 			ApplicationSetup.getProperty("matching.retrieved_set_size", "1000"));
 	/**
 	 * The number of unfinished query threads.
 	 */
 	public int waitForQuery = 0;
 	/**
 	 * The number of servers.
 	 */
 	public int numberOfServers;
 	
 	
 	/**
 	 * The default constructor. The arguments in the super() is null
 	 * because we don't want to load the indices at the broker.
 	 *
 	 */
 	public DistributedFieldManager(){
 		super(null);
 		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new RMISecurityManager());
		}
 		this.loadServers();
 		this.loadLexicon();
 	}
 	
 	/**
 	 * Load the global lexicon that used for speeding up the retrieval process.
 	 *
 	 */
 	protected void loadLexicon(){
 		globalLexiconAddress = ApplicationSetup.getProperty("global.lexicon.filename",
 				"Must be given");
 		if (logger.isDebugEnabled())
 			logger.debug(globalLexiconAddress + " is chosen as the global lexicon for retrieval.");
 		File f = new File(globalLexiconAddress);
 		if (!f.exists()){
 			logger.error("Global lexicon does not exist. Exit...");
 			System.exit(1);
 		}
 		globalLexicon = new Lexicon(globalLexiconAddress);
 	}
 	/**
 	 * Bind the servers. Retrieve the local statistics of the subcollections
 	 * and dispatch the merged global statistics to the servers.
 	 *
 	 */
 	protected void loadServers() {
 		
 		String property = ApplicationSetup.getProperty("server.names", "");
 		StringTokenizer stk = new StringTokenizer(property, ",");
 		
 		this.numberOfServers = stk.countTokens() / 2;
 		if (logger.isDebugEnabled())
 			logger.debug("Server names: "+property + ", number of servers: "+numberOfServers);
 		distMatch = new DistributedMatchingServer[numberOfServers];
 		distResult = new QueryResultSet[numberOfServers];
 		distThread = new DistributedThread[numberOfServers];
 		serverNames = new String[numberOfServers];
 		ids = new String[numberOfServers];
 		int counter = 0;
 		while (stk.hasMoreTokens()){
 			serverNames[counter] = stk.nextToken().trim();
 			ids[counter++] = stk.nextToken().trim();
 		}
 		if (logger.isInfoEnabled())
 			logger.info("Binding local servers...");
 		this.bindServers();
 		this.updateStatistics();
 		this.numberOfTopRankedDocs = new int[numberOfServers];
 		Arrays.fill(numberOfTopRankedDocs, 0);
 	}
 	
 	public int getServerIndex(String name, String id){
 		for (int i = 0; i < numberOfServers; i++){
 			if ((serverNames[i]+ids[i]).equals(name+id))
 				return i;
 		}
 		return -1;
 	}
 	
 	public boolean serverExists(String id){
 		for (int i = 0; i < numberOfServers; i++)
 			if (id.equals(ids[i]))
 				return true;
 		return false;
 				
 	}
 	
 	
 	/** Repeatedly try to bind the server name, pausing in-between attempts */
    protected DistributedMatchingServer bindOneServer(String name)
    {
        DistributedMatchingServer rtr = null;
        int Attempts = 3;
        long SleepTime = 1000;
        while(rtr == null)
        {
        	try{
                rtr = (DistributedMatchingServer) Naming.lookup(name);
                if (rtr == null)//null isn't an allowed answer
                        throw new Exception("Naming.lookup returned null");
            }
            catch (Exception e) {
                logger.warn("Exception while binding server ("+name+")");
                logger.error(e);
                if (Attempts > 0)
                {
                    logger.warn("Retrying again in "+ (SleepTime/1000) +"secs");
                    sleep(SleepTime);
                    Attempts--;
                }
                else
                {
                    logger.error("Gave up trying to contact it!");
                    e.printStackTrace();
                    System.exit(0);
                }
            }
        }
        if (logger.isDebugEnabled())
        	logger.debug("Contacted and Bound server " + name +"("+rtr+")");
        return rtr;
    }

    /** Sleep this thread of execution for the allotted time */
    protected void sleep(final long millis)
    {
        try{
            Thread.sleep(millis);
        }catch (Exception e) {/* dont care */}
    }


 	
 	/**
 	 * Bind the servers that mount the subcollections.
 	 *
 	 */
 	protected void bindServers() {
 		String name = null;
 		if (logger.isDebugEnabled())
 			logger.debug("binding servers...");
		final int rmi_port =
		Integer.parseInt(ApplicationSetup.getProperty("terrier.rmi.port", "1099"));
 		try {
 			for (int i=0; i<numberOfServers; i++) {
 				name = "//" + serverNames[i]+":"+rmi_port + "/DistMatch-"+ids[i];
 				distMatch[i] = this.bindOneServer(name);
 				distMatch[i].setId(ids[i]);
 				if (logger.isDebugEnabled())
 					logger.debug("Bound server " + name);
 			}
 		} catch(Exception e) {
 			logger.error("Exception while binding servers.");
 			logger.error(e.getMessage());
 			e.printStackTrace();			
 		}
 	}
 	/**
 	 * Get the length of a document that is the sum of the length in the three fields.
 	 * @param docno The docno of the document.
 	 * @return The document length.
 	 */
 	public int getFullDocumentLength(String docno, int serverIndex){
 		int length = 0;
// 		for (int i = 0; i < this.numberOfServers; i++){
// 			try{
// 				if (distMatch[i].isDocumentOnTheServer(docno)){
// 					length = distMatch[i].getFullDocumentLength(docno);
// 					break;
// 				}
// 			}
// 			catch(Exception e){
// 				e.printStackTrace();
// 				System.exit(1);
// 			}
// 		}
 		try{
 			length = distMatch[serverIndex].getFullDocumentLength(docno);
 		}
 		catch(Exception e){
 			e.printStackTrace();
			System.exit(1);
 		}
 		return length;
 	}
 	
 	
 	
 	/**
 	 * Get the within-document frequency of a given term in a given document. 
 	 * It is the sum of the term frequencies in the three fields.
 	 * @param term The given term.
 	 * @param docno The given document.
 	 * @return The within-document term frequency.
 	 */
 	public double getFullWithinDocFrequency(String term, String docno){
 		double tf = 0;
 		for (int i = 0; i < this.numberOfServers; i++){
 			try{
 				if (distMatch[i].isDocumentOnTheServer(docno)){
 					tf = distMatch[i].getFullWithinDocFrequency(term, docno);
 					break;
 				}
 			}
 			catch(Exception e){
 				e.printStackTrace();
 				System.exit(1);
 			}
 		}
 		return tf;
 	}
 	/**
 	 * Retrieve the terms in the given documents and stores the terms' 
 	 * statistics in a DistributedExpansionTerms. This is used for
 	 * query expansion.
 	 * @param docnos The docnos of the documents.
 	 * @param terms The terms and their statistics.
 	 * @return The terms in the top-ranked documents and their statistics.
 	 */
 	public DistributedExpansionTerms getTerms(String[] docnos,
 			DistributedExpansionTerms terms){
 		TIntHashSet serverIndex = new TIntHashSet();
 		TIntObjectHashMap mapIndexDocnos = new TIntObjectHashMap();
 		TIntHashSet foundDocnoIndex = new TIntHashSet(); 
 		int counterMatch = 0;
 		for (int i = 0; i < numberOfServers; i++){
 			try{
 				for (int j=0; j<docnos.length; j++){
 					if (foundDocnoIndex.contains(j))
 						continue;
 					if (distMatch[i].isDocumentOnTheServer(docnos[j])){
 						counterMatch++;
 						numberOfTopRankedDocs[i]++;
 						foundDocnoIndex.add(j);
 						if (!serverIndex.contains(i)){
 							serverIndex.add(i);
 							Vector _docnos = new Vector();
 							_docnos.addElement(docnos[j]);
 							mapIndexDocnos.put(i, _docnos);
 						}
 						else{
 							((Vector)mapIndexDocnos.get(i)).addElement(docnos[j]);
 						}
 						if (counterMatch == docnos.length)
 							break;
 					}
 					
 				}
 				if (counterMatch == docnos.length)
					break;
 			}
 			catch(Exception e){
 				e.printStackTrace();
 				System.exit(1);
 			}
 		}
 		int[] serverIndexes = serverIndex.toArray();
 		for (int i=0; i<serverIndexes.length; i++){
 			Vector _docnos = (Vector)mapIndexDocnos.get(serverIndexes[i]);
 			String[] tmpDocnos = (String[])_docnos.toArray(new String[_docnos.size()]);
 			try{
 				if (logger.isDebugEnabled())
 					logger.debug("Retrieving terms in "+tmpDocnos.length+ 
 						" documents from " + "server "+
 						serverNames[serverIndexes[i]]+ids[serverIndexes[i]]+"...");
 				TerrierTimer timer = new TerrierTimer();
 				timer.start();
 				terms = distMatch[serverIndexes[i]].getTerms(tmpDocnos, terms, globalLexiconAddress);
 				timer.setBreakPoint();
 				if (logger.isDebugEnabled())
 					logger.debug("parsing finished in "+timer.toStringMinutesSeconds());
 			}
 			catch(Exception e){
 				e.printStackTrace();
 				System.exit(1);
 			}
 		}
 		return terms;
 	}
 	/**
 	 * Retrieve the terms in the given documents and stores the terms' 
 	 * statistics in a DistributedExpansionTerms. This is used for
 	 * query expansion. The process is threaded.
 	 * @param docnos The docnos of the documents.
 	 * @param terms The terms and their statistics.
 	 * @return The terms in the top-ranked documents and their statistics.
 	 */
 	public DistributedExpansionTerms getTermsThreaded(String[] docnos, String[] serverIndex,
 			DistributedExpansionTerms terms){
// 		TIntHashSet serverIndexInt = new TIntHashSet();
// 		TIntObjectHashMap mapIndexDocnos = new TIntObjectHashMap();
// 		TIntHashSet foundDocnoIndex = new TIntHashSet(); 
// 		int counterMatch = 0;
// 		for (int i = 0; i < numberOfServers; i++){
// 			try{
// 				for (int j=0; j<docnos.length; j++){
// 					if (foundDocnoIndex.contains(j))
// 						continue;
// 					if (//distMatch[i].isDocumentOnTheServer(docnos[j]
// 						i==Integer.parseInt(serverIndex[j])
// 					){
// 						counterMatch++;
// 						numberOfTopRankedDocs[i]++;
// 						foundDocnoIndex.add(j);
// 						if (!serverIndexInt.contains(i)){
// 							serverIndexInt.add(i);
// 							Vector _docnos = new Vector();
// 							_docnos.addElement(docnos[j]);
// 							mapIndexDocnos.put(i, _docnos);
// 						}
// 						else{
// 							((Vector)mapIndexDocnos.get(i)).addElement(docnos[j]);
// 						}
// 						if (counterMatch == docnos.length)
// 							break;
// 					}
// 					
// 				}
// 				if (counterMatch == docnos.length)
//					break;
// 			}
// 			catch(Exception e){
// 				e.printStackTrace();
// 				System.exit(1);
// 			}
// 		}
// 		int[] serverIndices = serverIndexInt.toArray();
// 		DistributedQueryThread[] threads = new DistributedQueryThread[serverIndices.length];
// 		this.waitForQuery=serverIndices.length;
// 		for (int i=0; i<serverIndices.length; i++){
// 			Vector _docnos = (Vector)mapIndexDocnos.get(serverIndices[i]);
// 			String[] tmpDocnos = (String[])_docnos.toArray(new String[_docnos.size()]);
// 			try{
// 				threads[i] = new DistributedQueryThread(
// 						distMatch[serverIndices[i]],
//						this.serverNames[serverIndices[i]],
//						ids[serverIndices[i]],
//						this,
//						tmpDocnos,
//						new DistributedExpansionTerms((int)terms.totalDocumentLength,
//								(double)totalNumOfTokens, avl),
//						DistributedQueryThread.GET_TERMS);
// 				System.err.println("Retrieving terms in "+tmpDocnos.length+ 
// 						" documents from " + "server "+
// 						serverNames[serverIndices[i]]+"-"+ids[serverIndices[i]]+".");
// 			}
// 			catch(Exception e){
// 				e.printStackTrace();
// 				System.exit(1);
// 			}
// 		}
// 		int old = waitForQuery;
//			while(waitForQuery>0) {
//				if (old!=waitForQuery){
//					old=waitForQuery;
//					System.err.print("");
//				}
//					
//			}
// 		for (int i=0; i<threads.length; i++)
// 			terms.mergeExpansionTerms(threads[i].getTerms());		
 		
 		int numberOfDocuments = serverIndex.length;
 		THashSet[] docnosOnServer = new THashSet[numberOfServers];
 		TIntHashSet serverIndexHashSet = new TIntHashSet();
 		for (int i=0; i<numberOfServers; i++)
 			docnosOnServer[i] = new THashSet();
 		for (int i=0; i<numberOfDocuments; i++){
 			docnosOnServer[Integer.parseInt(serverIndex[i])].add(docnos[i]);
 			serverIndexHashSet.add(Integer.parseInt(serverIndex[i]));
 		}
 		
 		DistributedThreadGetTerms[] threads = new DistributedThreadGetTerms[serverIndexHashSet.size()];
 		this.waitForQuery=threads.length;
 		int counter = 0;
 		for (int i=0; i<numberOfServers; i++){
 			if (docnosOnServer[i].size() == 0)
 				continue;
 			else try{
 				distMatch[i].setCacheTerms(this.CACHE_TERMS);
 				threads[counter] = new DistributedThreadGetTerms(
 						distMatch[i],
						this.serverNames[i],
						ids[i],
						this,
						(String[])(docnosOnServer[i].toArray(new String[docnosOnServer[i].size()])),
						new DistributedExpansionTerms((int)terms.totalDocumentLength,
								(double)totalNumOfTokens, avl));
 				if (logger.isDebugEnabled())
 					logger.debug("Retrieving terms in "+docnosOnServer[i].size()+ 
 						" documents from " + "server "+
 						serverNames[i]+"-"+ids[i]+".");
 				this.numberOfTopRankedDocs[i] += docnosOnServer[i].size();
 				counter++;
 			}
 			catch(Exception e){
 				e.printStackTrace();
 				System.exit(1);
 			}
 		}
			while(waitForQuery>0) {
				this.sleep(this.SLEEPING_TIME);
			}
 		for (int i=0; i<threads.length; i++){
 			terms.mergeExpansionTerms(threads[i].getTerms());
 		}
 		
 		return terms;
 	}

 	/**
 	 * Stop the servers.
 	 *
 	 */
 	public void stopServers(){
 		for (int i=0; i<numberOfServers; i++)
 			try{
 				if (logger.isInfoEnabled())
 					logger.info("Stopping server " + serverNames[i]+ids[i] + "...");
 				distMatch[i].stopServer();
 			}
 			catch(Exception e){}
	}
 	
 	public long getCollectionModel(String term){
 		if (globalLexicon.findTerm(term))
 			return globalLexicon.getTF()/totalNumOfTokens;
 		else return 0;
 	}
 	
 	/**
 	 * Get the in-collection frequency and document frequency of a given term.
 	 * @param term The given term.
 	 * @return The in-collection frequency and document frequency of a given term.
 	 */
 	public double[] getFullStats(String term){
 		double[] stats = {0, 0};
 		if (this.globalLexicon.findTerm(term)){
 			stats[0] = this.globalLexicon.getTF(); 
 			stats[1] = this.globalLexicon.getNt();
 		}
 		return stats;
 	}
 	/**
 	 * Retrieve the local statistics of the subcollections
	 * and dispatch the merged global statistics to the servers.
	 */
 	protected void updateStatistics() {
 		totalNumOfDocuments = 0;
 		totalNumOfTokens = 0;
 		totalNumOfPointers = 0;
 		this.totalNumOfDocumentsInFields = new long[NumFields];
 		this.totalNumOfTokensInFields = new long[NumFields];
 		this.totalNumOfPointersInFields = new long[NumFields];
 		
		Arrays.fill(this.totalNumOfDocumentsInFields, 0);
		Arrays.fill(this.totalNumOfTokensInFields, 0);
		Arrays.fill(this.totalNumOfPointersInFields, 0);
 		try {
 			for (int i=0; i<numberOfServers; i++) {
 				long numOfDocuments = 0;
 				if (logger.isDebugEnabled())
 					logger.debug("fetching statistics of server " +
 						this.serverNames[i]+ids[i] + "...");
 				int[] docs = distMatch[i].getNumberOfDocuments();
 				long[] tokens = distMatch[i].getNumberOfTokens();
 				long[] pointers = distMatch[i].getNumberOfPointers();
 				
 				
 				for (int j = 0; j < docs.length; j++){
 					numOfDocuments = Math.max(docs[j], numOfDocuments);
 					this.totalNumOfPointersInFields[j] += pointers[j];
 					this.totalNumOfTokensInFields[j] += tokens[j];
 					this.totalNumOfPointers += pointers[j];
 					this.totalNumOfTokens += tokens[j];
 					this.totalNumOfDocumentsInFields[j] += docs[j];
 				}
 				totalNumOfDocuments += numOfDocuments;
 				for (int j = 0; j < docs.length; j++){
 					if (logger.isDebugEnabled()){
 						logger.debug("field " + (j+1) + ": ");
 						logger.debug("number of documents: " + docs[j]);
 						logger.debug("number of tokens: " + tokens[j]);
 					}
 				}
 				if (logger.isDebugEnabled()){
					logger.debug("----------------");
 				}	
 			}
 			if (logger.isDebugEnabled())
				logger.debug("global statistics");
 			for (int i = 0; i < this.totalNumOfDocumentsInFields.length; i++){
 				if (logger.isDebugEnabled()){
					logger.debug("field " + (i+1) + ": ");
					logger.debug("number of documents: " + totalNumOfDocumentsInFields[i]);
					logger.debug("number of tokens: " + totalNumOfTokensInFields[i]);
 				}
 			}
 			if (logger.isDebugEnabled()){
 				logger.debug("----------------");
 				logger.debug("in total: ");
 				logger.debug("number of documents: " + totalNumOfDocuments);
 				logger.debug("number of tokens: " + totalNumOfTokens);
				logger.debug("----------------");
 			}
 			long[] terms = new long[this.totalNumOfDocumentsInFields.length];
 			Arrays.fill(terms, 0);
 			for (int i=0; i<numberOfServers; i++) {
 				if (logger.isDebugEnabled())
 					logger.debug("setting global statistics for server #"+(i+1));
 				distMatch[i].setGlobalStatistics(
 						totalNumOfDocumentsInFields,
						totalNumOfTokensInFields,
 						totalNumOfDocuments,
						totalNumOfTokens);
 			}
 			
 		} catch (Exception e) {
 			logger.error("Exception while updating statistics.");
 			logger.error(e.getMessage());
 			e.printStackTrace();
 		}
 		if (logger.isDebugEnabled()){
 			logger.debug("totalNumOfDocuments: " + totalNumOfDocuments);
 			logger.debug("totalNumOfTokens: " + totalNumOfTokens);
 		}
 		this.statisticsUpToDate = true;
 		this.avl = (double)this.totalNumOfTokens / this.totalNumOfDocuments;
 	}
 	/**
 	 * Set the names of the servers.
 	 * @param serverNames The names of the servers.
 	 */
 	public void setServerNames(String[] serverNames) {
 		this.serverNames = serverNames;
 	}
 	/**
 	 * Set the ids of the servers.
 	 * @param ids The ids of the servers.
 	 */
 	public void setIDs(String[] ids) {
 		this.ids = ids;
 	}
 	/**
 	 * Merge the result sets returned from different servers.
 	 * @param resultSize The size of the final merged result set.
 	 * @return The merged result set.
 	 */
 	public QueryResultSet mergeResults(int resultSize) {
 		if (logger.isDebugEnabled())
 			logger.debug("merging results...");
 		
 		TIntArrayList listDocids = new TIntArrayList();
 		TIntObjectHashMap docnosMap = new TIntObjectHashMap();
 		TIntObjectHashMap serverIndexMap = new TIntObjectHashMap();
 		int counter = 0;
 		int fullSize = 0;
 		for (int i = 0; i < numberOfServers; i++){
 			fullSize+=distResult[i].getExactResultSize();
 		}
 		QueryResultSet rsFinal = new QueryResultSet(fullSize);
 		rsFinal.initialise();
 		double[] scores = rsFinal.getScores();
 		
 		for (int i = 0; i <numberOfServers; i++){
 			double[] localScores = distResult[i].getScores();
 			String[] docnos = distResult[i].getMetaItems("docnos");
 			String[] serverIndex = distResult[i].getMetaItems("serverIndex");
 			int size = distResult[i].getExactResultSize();
 			for (int j=0; j<size; j++){
 				listDocids.add(counter);
 				docnosMap.put(counter, docnos[j]);
 				serverIndexMap.put(counter, serverIndex[j]);
 				scores[counter]=localScores[j];
 				counter++;
 			}
 		}

 		int[] docids = listDocids.toNativeArray();
 		short[] occurences = new short[docids.length];
 		Arrays.fill(occurences, 0, docids.length, (short)0);
 		HeapSort.descendingHeapSort(scores, docids, occurences, resultSize);
 		
 		String[] docnos = new String[resultSize];
 		String[] serverIndex = new String[resultSize];
 		for (int i=0;i<resultSize;i++){
 			docnos[i]=(String)docnosMap.get(docids[i]);
 			serverIndex[i] = (String)serverIndexMap.get(docids[i]);
 		}
 		rsFinal = (QueryResultSet)rsFinal.getResultSet(0, resultSize);
 		
 		rsFinal.addMetaItems("docnos", docnos);
 		rsFinal.addMetaItems("serverIndex", serverIndex);
 		if (logger.isDebugEnabled())
 			logger.debug("Final result size: " + rsFinal.getExactResultSize());
 		return rsFinal;
 	}
 
 	/**
 	 * Retrieve for the given query that is stored in a DistributedMatchingQueryTerms.
 	 * @param dmqt The query.
 	 */
 	public void retrieve(DistributedMatchingQueryTerms dmqt) {
 		try {
 			for (int i=0; i<numberOfServers; i++) {
 				distResult[i] = distMatch[i].retrieve(dmqt, this.numberOfRetrievedDocuments);
 				distResult[i].addMetaItems("docnos", 
 						distMatch[i].getDocumentNumbers(
 								distResult[i].getDocids(), 0, distResult[i].getExactResultSize()));
 			}

 		}
 		catch (Exception e) {
 			logger.error("Exception while initialising the server array.");
 			logger.error(e.getMessage());
 			e.printStackTrace();
 		}		
 	}
 	/**
 	 * Update the query statistics from the servers.
 	 * @param dmqt The query.
 	 * @return The updated query.
 	 */
 	public DistributedMatchingQueryTerms updateQuery(DistributedMatchingQueryTerms dmqt){
 		dmqt.initialise();
 		if (logger.isDebugEnabled())
 			logger.debug("updating query...");
 		TerrierTimer updateTimer = new TerrierTimer();
 		updateTimer.start();
 		waitForQuery = numberOfServers;
		DistributedThreadUpdateQuery[] queryServers = new DistributedThreadUpdateQuery[numberOfServers];
		LocalMatchingQueryTerms[] lmqt = new LocalMatchingQueryTerms[numberOfServers];
		for (int i=0; i<numberOfServers; i++) {
			lmqt[i] = new LocalMatchingQueryTerms(dmqt.queryid, dmqt.getTerms(), 3);
			queryServers[i] = new DistributedThreadUpdateQuery(
					distMatch[i], 
					lmqt[i], 
					this,
					this.serverNames[i],
					ids[i]);
		}
		while(waitForQuery>0) {
			this.sleep(this.SLEEPING_TIME);
		}
		for (int i=0; i<numberOfServers; i++) {
			dmqt.addEntry(queryServers[i].getUpdatedQuery());
		}
 		String[] queryTerms = dmqt.getTerms();
 		int queryLength = queryTerms.length;
 		for (int i =0; i < queryLength; i++){

 			if (this.globalLexicon.findTerm(queryTerms[i])){
 				dmqt.totalTF[i] = this.globalLexicon.getTF();
 				dmqt.totalNt[i] = this.globalLexicon.getNt();			
 			}
 			else{
 				dmqt.totalTF[i] = 0;
 				dmqt.totalNt[i] = 0;
 			}
 		}
 		updateTimer.setBreakPoint();
 		if (logger.isDebugEnabled())
 			logger.debug("Query statistics updated in "+updateTimer.toStringMinutesSeconds());
 			
		return dmqt;
 	}
 	/**
 	 * Retrieve for a given query in threads.
 	 * @param dmqt The query.
 	 */
 	public void retrieveThreaded(DistributedMatchingQueryTerms dmqt) {
 		try {
 			waitForQuery = numberOfServers;
 			if (logger.isDebugEnabled())
 				logger.debug("retrieving from servers for query " +  dmqt.queryid + "...");
 			DistributedThreadRetrieve[] queryServers = new DistributedThreadRetrieve[numberOfServers];
 			for (int i=0; i<numberOfServers; i++) {
 				queryServers[i] = new DistributedThreadRetrieve(
 						distMatch[i], 
 						serverNames[i],
						ids[i],
						i,
						dmqt, 
						distResult[i], 
						this.numberOfRetrievedDocuments,
						this);
 			}
 			while(waitForQuery>0) {
 				this.sleep(this.SLEEPING_TIME);
 			}

 			for (int i=0; i<numberOfServers; i++) {
 				distResult[i] = (QueryResultSet)queryServers[i].getResultSet();
 			}
 			
 		}
 		catch (Exception e) {
 			logger.error("Exception while initialising the server array.");
 			logger.error(e.getMessage());
 			e.printStackTrace();
 		}	
 		
 	}
 	
 	
 	
 	/**
 	 * Decrease the number of awaited query threads. This is called by
 	 * the threads.
 	 *
 	 */
 	public synchronized void queryThreadFinished() {
 		waitForQuery--;
 	}
 	
 	/** Runs the weighting and matching stage - this the main entry
	  * into the rest of the Terrier framework.
	  * @param srq the current SearchRequest object.
	  */
	public void runMatching(SearchRequest srq)
	{
		if (logger.isDebugEnabled())
			logger.debug("querying on local collection.");
		Request rq = (Request)srq;
		if (logger.isDebugEnabled())
			logger.debug("weighting model: " + rq.getWeightingModel());
		DistributedMatchingQueryTerms mqt = (DistributedMatchingQueryTerms)rq.getMatchingQueryTerms();
		
		Query q = rq.getQuery();
		
		/* now propagate fields into requirements, and apply boolean matching
		   for the decorated terms. */
		ArrayList requirement_list = new ArrayList();
		ArrayList field_list = new ArrayList();
		
		q.getTermsOf(RequirementQuery.class, requirement_list, true);
		q.getTermsOf(FieldQuery.class, field_list, true);
		for (int j=0; j<field_list.size(); j++) 
			if (!requirement_list.contains(field_list.get(j)))
				requirement_list.add(field_list.get(j));
			
		if (requirement_list.size()>0) {
			mqt.addDocumentScoreModifier(new BooleanScoreModifier(requirement_list));
		}
		mqt.setQueryid(rq.getQueryID());
		mqt.setQuery(q);
		
		mqt = updateQuery(mqt);
		if (! rq.isEmpty())
		{
			boolean ResultCached = false;
			String key = "";
			QueryResultSet outRs = null;
			for (int i = 0; i < this.numberOfServers; i++){
				try{
					distMatch[i].setWeightingModel(getWeightingModel(rq.getWeightingModel()));
				}
				catch(RemoteException e){
					logger.error("remote exception occurs while " +
							"setting model " + rq.getWeightingModel());
					e.printStackTrace();
					System.exit(1);
				}			
			}			
						this.retrieveThreaded(mqt);
			//now crop the collectionresultset down to a query result set.
			int numberOfRetrievedDocuments = 
				Integer.parseInt(ApplicationSetup.getProperty("matching.retrieved_set_size", "1000"));
			outRs = mergeResults(numberOfRetrievedDocuments);
			rq.setResultSet(outRs);
		}
		else
		{
			if (logger.isDebugEnabled())
				logger.debug("Returning empty result set as query "+rq.getQueryID()+" is empty");
			rq.setResultSet(new QueryResultSet(0));
		}
	}
	/**
	 * Set the normalisation methods to the servers.
	 * @param normalisationNames The names of the normalisation methods. 
	 * @param parameters The parameter values.
	 * @param weights The weights of the three fields.
	 */
	protected void setNormalisation(String[] normalisationNames, double[] parameters,
			double[] weights){
		boolean normalisationStrategy = (new Boolean(ApplicationSetup.getProperty(
				"simple.normalisation", "false"))).booleanValue();
		double[] QEWeights = new double[NumFields];
		for (int i=1;i<=NumFields;i++){
			QEWeights[i-1] = Double.parseDouble(
					ApplicationSetup.getProperty("qeweight."+i,
							ApplicationSetup.getProperty("weight."+i, "1d")
							));
		}
		for (int i = 0; i < this.numberOfServers; i++){
			try{
				distMatch[i].setNormalisation(normalisationNames, parameters, weights);
				distMatch[i].setNormalisationStrategy(normalisationStrategy);
				distMatch[i].setQEWeights(QEWeights);
			}
			catch(RemoteException e){
				logger.error("remote exception occurs while " +
						"setting normalisation methods.");
				e.printStackTrace();
				System.exit(1);
			}			
		}
	}
 
 	//run methods
 	//These methods are called by the application in turn
 	//(or could just have one RUN method, and these are privates,
 	//but I would prefer the separate method)
 	/** runPreProcessing */
 	public void runPreProcessing(SearchRequest srq)
 	{
 		Request rq = (Request)srq;
 		Query query = rq.getQuery();
 		
 		//get the controls
 		boolean rtr = ! query.obtainControls(Allowed_Controls, rq.getControlHashtable());
 		//we check that there is stil something left in the query
 		if (! rtr)
 		{
 			rq.setEmpty(true);
 			return;
 		}
 		rtr = query.applyTermPipeline(this);
 		if (! rtr)
 		{
 			rq.setEmpty(true);
 			return;
 		}
 		if (ApplicationSetup.getProperty("querying.no.negative.requirement", "").equals("true")) {
 		    ArrayList terms = new ArrayList();
 		    query.getTermsOf(uk.ac.gla.terrier.querying.parser.SingleTermQuery.class, terms, true);
 		    final int size = terms.size();
 		    for(int x=0;x<size;x++)
 		    {
 		        uk.ac.gla.terrier.querying.parser.SingleTermQuery sqt = (uk.ac.gla.terrier.querying.parser.SingleTermQuery)terms.get(x);
 		        sqt.setRequired(0);
 		    }
 		}
 		
 		this.loadParameters();
 		
 		DistributedMatchingQueryTerms queryTerms = 
 				new DistributedMatchingQueryTerms(rq.getQueryID(), NumFields);
 		query.obtainQueryTerms(queryTerms);
 		/**
 		 * cacheResults has to be initialised here because we do not cache results for post retrieval.
 		 */
 		boolean cacheResults = (new Boolean(
 				ApplicationSetup.getProperty("cache.results", "false"))
 					).booleanValue();
 		if (cacheResults){
				 		
	 		boolean normalisationStrategy = (new Boolean(ApplicationSetup.getProperty(
					"simple.normalisation", "false"))).booleanValue();
	 		
	 		
	 		
	 		queryTerms.setNormStrategy(normalisationStrategy);
	 		queryTerms.setParameterValues(cValues);
	 		queryTerms.setFieldWeights(weightValues);
	 		for (int i = 0; i < this.numberOfServers; i++){
	 			try{
	 				distMatch[i].setProperty("cache.results", "true");
	 				if (ApplicationSetup.getProperty("matching.dsms", "").endsWith("ProximityScoreModifier")){
	 					distMatch[i].setProperty("proximity.ngram.length", ApplicationSetup.getProperty("proximity.ngram.length","2"));
	 					distMatch[i].setProperty("proximity.ngram.c", ApplicationSetup.getProperty("proximity.ngram.c","1"));
	 					distMatch[i].setProperty("l_t", ApplicationSetup.getProperty("l_t","1.0d"));
	 					distMatch[i].setProperty("l_u", ApplicationSetup.getProperty("l_u","1.0d"));
	 				}
	 			}catch(RemoteException e){
	 				e.printStackTrace();
	 				logger.error("RemoteException occurs while setting property cache.results.");
	 			}
	 		}
		}
 		rq.setMatchingQueryTerms(queryTerms);
 		for (int i = 0; i < this.numberOfServers; i++){
			try{
				try{
					//distMatch[i].setNormalisation(normalisationNames, cValues, weightValues);
					distMatch[i].setWeightingModel(getWeightingModel(rq.getWeightingModel()));
				}
				catch(Exception e){
					e.printStackTrace();
					logger.error("Error occurs while setting weighting model on server "+
							serverNames[i]+"-"+ids[i]);
				}
				if (ApplicationSetup.getProperty("querying.no.negative.requirement", "false").equals("true")) {
					distMatch[i].setIgnoreLowIdfTerms(false);
				}
				else
					distMatch[i].setIgnoreLowIdfTerms(true);
			}
			catch(RemoteException e){
				logger.error("remote exception occurs while " +
						"setting model " + rq.getWeightingModel());
				e.printStackTrace();
				System.exit(1);
			}			
		}
 		((DistributedMatchingQueryTerms)rq.getMatchingQueryTerms()).normaliseTermWeights();
 		this.setFirstPassRetrievalNormalisation();
 	}
 	/**
 	 * Set the normalisation methods to the servers for the first-pass
 	 * retrival.
 	 *
 	 */
 	public void setFirstPassRetrievalNormalisation(){
 		setNormalisation(normalisationNames, cValues, weightValues);
 	}
 	/**
 	 * Set the normalisation methods to the servers for the second-pass
 	 * retrieval.
 	 *
 	 */
 	public void setSecondPassRetrievalNormalisation(){
 		this.loadPostParameters();
		this.setNormalisation(normalisationNames, cPostValues, weightValues);
 	}
 	/**
 	 * This is a method for debugging. It finds the query terms that have
 	 * inconsistent term frequencies between that obtained from the global 
 	 * lexicon and that summed from the servers.
 	 *
 	 */
 	public void getTermsWithDifferentTF(){
 		int counter = 0;
 		HashSet terms = new HashSet();
 		LexiconInputStream lex = new LexiconInputStream(
					ApplicationSetup.getProperty("global.lexicon.filename", "Must be specified"));
 		int hasMore = 0;
 		StringBuffer buffer = new StringBuffer();
 		
 		while (counter < 1){
 			try{
 				hasMore = lex.readNextEntry();
 			}
 			catch(Exception e){
 				e.printStackTrace();
 				System.exit(1);
 			}
 			if (hasMore>=0){
 				String term = lex.getTerm();
 				if (logger.isDebugEnabled())
 					logger.debug("processing term "+term);
 				int TF = 0;
 				for (int i = 0; i < this.numberOfServers; i++){
 		 			try{
 		 				double[] localStats = distMatch[i].getFullStats(term);
 		 				TF+=localStats[0];
 		 			}
 		 			catch(Exception e){
 		 				e.printStackTrace();
 		 				System.exit(1);
 		 			}
 		 		}
 				try{
 					globalLexicon.findTerm(term);
	 				if (TF!=globalLexicon.getTF()){
	 					terms.add(term);
	 					if (logger.isDebugEnabled())
	 						logger.debug(term);
	 					buffer.append(term+", ");
	 					counter++;
	 				}
 				}
 				catch(Exception e){
 					logger.error("term: " + term);
 					e.printStackTrace();
 					System.exit(1);
 				}
 			}
 			else
 				break;
 		}
 	}
 	
 	/**
 	 * Rebuild the global lexicon. It computes the document frequency by
 	 * merging statistics from different servers.
 	 *
 	 */
 	public void rebuildLexicon(){
 		TerrierTimer timer = new TerrierTimer();
 		timer.start();
 		try{
 			String fn = ApplicationSetup.TREC_RESULTS+"/global.lex";
 			LexiconOutputStream lexOutStream = new LexiconOutputStream(fn);
 		       // globalLexicon.findTerm("reinscribirlo");	
 			LexiconInputStream lex = new LexiconInputStream(
 					ApplicationSetup.getProperty("global.lexicon.filename", "Must be specified")
					);
 			
 			
 			int hasMore = lex.readNextEntry();
 			int termid = 0;
 			long numberOfUniqueTerms = this.globalLexicon.getNumberOfLexiconEntries();
 			long counter = 0;
 			while (hasMore>=0){
 				String term = lex.getTerm().trim();
 				if (logger.isDebugEnabled())
 					logger.debug("processing "+term+"...");
// 				if (term.equals("22mar99")){
// 					hasMore = lex.readNextEntry();
// 					continue;
// 				}
 				int TF = 0;
 				int Nt = 0;
 				
 				/*if (lex.getNt()==1){
 				   TF= lex.getTF(); Nt = lex.getNt();
                }
 				else{*/
 				try{
 					waitForQuery = numberOfServers;
 					
 		 			DistributedThreadGetFullStats[] queryServers = new DistributedThreadGetFullStats[numberOfServers];
 		 			for (int i=0; i<numberOfServers; i++) {
 		 				queryServers[i] = new DistributedThreadGetFullStats(
 		 						distMatch[i], 
 		 						serverNames[i],
 								ids[i],
 								term,
								this);
 		 			}
 		 			while(waitForQuery>0) {
 		 				this.sleep(1);
 		 			}

 		 			for (int i=0; i<numberOfServers; i++) {
 		 				double[] stats = queryServers[i].getTermStats();
 		 				if (stats!=null){
 		 					TF += stats[0];
 		 					Nt += stats[1];
 		 				}
						stats = null;
 		 			}
					queryServers = null;
 				}
 				catch(Exception e){
 					e.printStackTrace();
 					System.exit(1);
 				}
 				//}
 				double percent = (double)(++counter)/numberOfUniqueTerms*100;
 				if (logger.isDebugEnabled())
 					logger.debug("TF: " + TF + ", Nt: "+Nt+", " + lex.getTF() + ", "+
 		 				lex.getNt() + ",  "+
 		 				Rounding.toString(percent, 4)+"% finished.");
 		 		if (TF>0)
 		 			lexOutStream.writeNextEntry(term, termid++, Nt, TF, 0L, (byte)0);
 		 		
 		 		hasMore = lex.readNextEntry();
 			}
 		}
 		catch(Exception e){
 			e.printStackTrace();
 			System.exit(1);
 		}
 		timer.setBreakPoint();
 		if (logger.isInfoEnabled())
 			logger.info("Rebuilding finished in " + timer.toStringMinutesSeconds());
 	}
 	
 	
 	/**
 	 * Get the Average Inverse Collection Term Frequency (AvICTF) in the collection
 	 * for a given query.
 	 * @param dmqt The query.
 	 * @return The AvICTF value.
 	 */
 	public double getAvICTF(DistributedMatchingQueryTerms dmqt){
 		String[] terms = dmqt.getTerms();
 		double AvICTF = 0;
 		Idf log = new Idf();
 		for (int i = 0; i < terms.length; i++){
 			if (globalLexicon.findTerm(terms[i])){
 				AvICTF += log.log((double)totalNumOfTokens 
 						/ globalLexicon.getTF());
 			}
 		}
 		return AvICTF/terms.length;
 	}
 	
 	/** Runs the PostProcessing modules in order added. PostProcess modules
 	  * alter the resultset. Examples might be query expansions which completelty replaces
 	  * the resultset.
 	  * @param srq the current SearchRequest object. */
 	public void runPostProcessing(SearchRequest srq)
 	{
 		
 		//TODO implement query expansion with field retrieval
 		Request rq = (Request)srq;
 		Hashtable controls = rq.getControlHashtable();
 		
 		for(int i=0; i<PostProcesses_Order.length; i++)
 		{
 			String PostProcesses_Name = PostProcesses_Order[i];
 			for(int j=0; j<PostProcesses_Controls[i].length; j++)
 			{
 				String ControlName = PostProcesses_Controls[i][j];
 				String value = (String)controls.get(ControlName);
 				this.setSecondPassRetrievalNormalisation();
 				//System.err.println(ControlName+ "("+PostProcesses_Name+") => "+value);
 				if (value == null)
 					continue;
 				value = value.toLowerCase();
 				if(! (value.equals("off") || value.equals("false")))
 				{
 					if (logger.isInfoEnabled())
 						logger.info("Processing: "+PostProcesses_Name);
 					getPostProcessModule(PostProcesses_Name).process(this, srq);
 					//we've now run this post process module, no need to check the rest of the controls for it.
 					break;
 				}
 			}
 		}
 	}
 	
 }

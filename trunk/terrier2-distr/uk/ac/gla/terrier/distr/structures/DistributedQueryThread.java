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
package uk.ac.gla.terrier.distr.structures;

import uk.ac.gla.terrier.distr.querying.DistributedFieldManager;
import uk.ac.gla.terrier.distr.querying.DistributedThreeManager;
import uk.ac.gla.terrier.distr.matching.DistributedMatchingServer;
import uk.ac.gla.terrier.matching.QueryResultSet;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.querying.Manager;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.TerrierTimer;

/**
 * This class is obsolete by DistributedThread and its extensions.
 * This class implements a thread for the distributed matching.
 * @author Ben He(ben@dcs.gla.ac.uk)
 * @version $Revision: 1.8 $
 */
public class DistributedQueryThread extends Thread {
	/** Specifies the action that the thread is going to perform.*/
	protected int task = PERFORM_RETRIEVAL;
	/** The default action. */
	public static final int DEFAULT = 0;
	/** Identifies that the thread performs retrieval. */
	public static final int PERFORM_RETRIEVAL = 1;
	/** Identifies that the thread performs query updating. */
	public static final int UPDATE_QUERY = 2;
	/** Identifies that the thread performs term extraction. */
	public static final int GET_TERMS = 3;
	/** Identifies that the thread retrieves term statistics. */
	public static final int GET_STATS = 4;
	/** The docnos of the top-ranked documents. This is used for
	 * query expansion. 
	 * */
	String[] docnos;
	public static final boolean PAR_QE = (new Boolean(
			ApplicationSetup.getProperty("partial.query.expansion", "false"))).booleanValue();
	/**
	 * The DistributedExpansionTerms used for query expansion. It convoys
	 * the terms in the top-ranked documents from a subcollection.
	 */
	DistributedExpansionTerms distTerms;
	/** The id of the query. */
	protected String queryid;
	/** The name of the server to which the server is associated. */
	protected String serverName;
	/** The id of the server to which the server is associated. */
	protected String id;
	/** The index of the server in the manager. */
	protected int serverIndex;
	/** The term for which the thread retrieves the statistics from
	 * the associated server. */
	protected String queryTerm;
	/**
	 * The statistics of the given term on the server.
	 */
	protected double[] termStats;

	/** The matching class to use.*/
	protected DistributedMatchingServer dmatch = null;
	/** The query. This is used for retrieval. */
	protected DistributedMatchingQueryTerms dq;

	/** The query to process.*/
	protected LocalMatchingQueryTerms lmqt = null; 

	/** The distributed result set containing the results.*/
	protected QueryResultSet drs = null;
	/** The maximum number of documents to be retrieved. */
	protected int numberOfRetrievedDocuments;
	/** The starting time of the retrieval process. */
	protected long startQueryingTime;
	/** 
	 * The retrieval class in which this instance is created. 
	 * This is needed in order to access the method queryThreadFinished().
	 */
	protected Manager dr = null;

	/**
	 * A constructor that creates a thread that does the retrieval for a given query.
	 * @param dmatch
	 * @param servername
	 * @param id
	 * @param serverIndex,
	 * @param dq
	 * @param drs
	 * @param numberOfRetrievedDocuments
	 * @param dr
	 * @param task
	 */
	public DistributedQueryThread(
			DistributedMatchingServer dmatch,
			String servername,
			String id,
			int serverIndex,
			DistributedMatchingQueryTerms dq, 
			ResultSet drs, 
			int numberOfRetrievedDocuments,
			Manager dr,
			int task) {
		super();
		this.serverName=servername;
		this.id=id;
		this.serverIndex = serverIndex;
		this.dmatch = dmatch;
		this.queryid = dq.queryid;
		this.drs = (QueryResultSet)drs;
		this.dq = dq;
		this.dr = dr;
		this.numberOfRetrievedDocuments = numberOfRetrievedDocuments;
		this.startQueryingTime = System.currentTimeMillis();
		this.task = task;
		this.start();

	}
	
	public DistributedQueryThread(
			DistributedMatchingServer dmatch,
			String servername,
			String id,
			String term,
			Manager dr,
			int task) {
		super();
		this.serverName=servername;
		this.id=id;
		this.dmatch = dmatch;
		this.queryTerm = term;
		this.task = task;
		this.dr = dr;
		this.start();
	}

	/** 
	 * A constructor for the DistributedQueryThread class. 
	 * @param dmatch the remote matching interface to use.
	 * @param dq the distributed query.
	 * @param drs the distributed result.
	 * @param dr the calling distributed retrieval class.
	 * @param numOfRetDocs the number of documents to retrieve.
	 */
	public DistributedQueryThread(DistributedMatchingServer dmatch,
			String queryid,
			DistributedMatchingQueryTerms dq,
			ResultSet drs,
			Manager dr,
			int numOfRetDocs,
			int t) {
		super();
		this.dmatch = dmatch;
		this.drs = (QueryResultSet)drs;
		this.dq = dq;
		this.dr = dr;
		this.numberOfRetrievedDocuments = numOfRetDocs;
		setTask(t);
		this.startQueryingTime = System.currentTimeMillis();
		this.start();

	}
	
	/** 
	 * A constructor for the DistributedQueryThread class. It creates a thread that does
	 * the query update.
	 * @param dmatch the remote matching interface to use.
	 * @param dq the distributed query.
	 * @param drs the distributed result.
	 * @param dr the calling distributed retrieval class.
	 * @param numOfRetDocs the number of documents to retrieve.
	 */
	public DistributedQueryThread(DistributedMatchingServer dmatch,
			LocalMatchingQueryTerms lmqt,
			Manager dr,
			String servername,
			String id,
			int t) {
		super();
		this.serverName = servername;
		this.id = id;
		this.dmatch = dmatch;
		this.lmqt = lmqt;
		this.dr = dr;
		setTask(t);
		this.start();

	}
	
	public DistributedQueryThread(DistributedMatchingServer dmatch,
			Manager dr,
			String servername,
			String id){
		super();
		this.serverName = servername;
		this.id = id;
		this.dmatch = dmatch;
		this.dr = dr;
	}
	
	public DistributedQueryThread(
			DistributedMatchingServer dmatch,
			String servername,
			String id,
			Manager dr,
			String[] docnos,
			DistributedExpansionTerms distTerms,
			int task
			){
		this.dmatch = dmatch;
		this.dr = dr;
		this.setGetTerms(docnos, distTerms);
		this.task = task;
		this.serverName = servername;
		this.id = id;
		this.start();
	}
	
	public void setServerid(String name, String id){
		this.serverName = name;
		this.id = id;
	}

	public void setTask(int t) {
		task = t;
	}
	
	public void setGetTerms(String[] docnos, DistributedExpansionTerms distTerms){
		this.docnos = docnos;
		this.distTerms = distTerms;
	}
	
	public double[] getTermStats(){
		return this.termStats;
	}
	
	public LocalMatchingQueryTerms getUpdatedQuery(){
		return this.lmqt;
	}

	/** 
	 * Overrides the default run() method of the thread. 
	 * This method actually calls the retrieve method of the
	 * matching interface.
	 */
	public void run() {
		try {
			switch(task) {
				case UPDATE_QUERY: 
					lmqt = dmatch.updateQuery(lmqt); 
					break;
				case PERFORM_RETRIEVAL: 
				case GET_TERMS:{
					TerrierTimer timer = new TerrierTimer();
					timer.start();
					/*if (PAR_QE)
						distTerms = dmatch.getBodyTerms(docnos, distTerms, dr.globalLexiconAddress);
					else*/
					if (dr instanceof DistributedThreeManager)
						distTerms = dmatch.getTerms(docnos, distTerms, ((DistributedThreeManager)dr).globalLexiconAddress);
					else if (dr instanceof DistributedFieldManager)
						distTerms = dmatch.getTerms(docnos, distTerms, ((DistributedFieldManager)dr).globalLexiconAddress);
					
					
					timer.setBreakPoint();
					System.err.println("Parsing on server "+serverName+"-"+id + " finished in "
							+ timer.toStringMinutesSeconds()+
							" with "+distTerms.terms.size()+ " extracted terms");
					break;
				}
				case GET_STATS:{
					termStats = dmatch.getFullStats(queryTerm);
					break;
				}
				default: {
					drs = (QueryResultSet)dmatch.retrieve(dq, this.numberOfRetrievedDocuments);
					if (drs==null){
						System.err.println("Warning: EMPTY RESULT SET.");
					}
					String[] serverIndexStrings = new String[drs.getExactResultSize()];
					java.util.Arrays.fill(serverIndexStrings, ""+serverIndex);
					drs.addMetaItems("serverIndex", serverIndexStrings);
					System.err.println("Querying on server "+this.serverName+"-"+id + " finished in "
						+(System.currentTimeMillis() - this.startQueryingTime)+
						"ms with "+drs.getExactResultSize()+ " retrieved documents");
					break;
				}
			}
			if (dr instanceof DistributedThreeManager)
				((DistributedThreeManager)dr).queryThreadFinished();
			else if (dr instanceof DistributedFieldManager)
				((DistributedFieldManager)dr).queryThreadFinished();
		} catch(Exception e) {
			System.err.println("Exception while retrieving from server"+serverName+"-"+id+ ".");
			System.err.println(e.getMessage());
			e.printStackTrace();			
		}
		
	}
	
	public DistributedExpansionTerms getTerms(){
		return this.distTerms;
	}
	
	/**
	 * Returns the result set that contains the retrieved documents.
	 * @return the distributed result set containing the retrieved documents.
	 */
	public ResultSet getResultSet() {
		return drs;
	}
}

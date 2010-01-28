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
 * The Original Code is DistributedThreeMatchingServer.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk> 
 */
package uk.ac.gla.terrier.distr.matching;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import gnu.trove.THashMap;
import gnu.trove.THashSet;

import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.distr.structures.DistributedExpansionTerms;
import uk.ac.gla.terrier.distr.structures.DistributedMatchingQueryTerms;
import uk.ac.gla.terrier.distr.structures.LocalMatchingQueryTerms;
import uk.ac.gla.terrier.matching.Model;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.utility.StringComparator;

/**
 * This class provides a contract for running a matching server in
 * a distributed setting over three indices, namely body, anchor text
 * and title.
 * @author Ben He(ben@dcs.gla.ac.uk)
 * @version $Revision: 1.14 $
 */
public class DistributedThreeMatchingServer extends UnicastRemoteObject
	implements DistributedMatchingServer{
	/** the logger for this class */
	private static Logger logger = Logger.getLogger("server");
	/**
	 * The matching class.
	 */
	protected DistributedThreeMatching matching;
	/** The docno of the first document on the server. */
	protected String firstDocno;
	/** The docno of the last document on the server. */
	protected String lastDocno;
	
	protected THashMap queryResultMap = new THashMap();
	
	private static final long serialVersionUID = 200603101232L;
	
	/** The default namespace for Weighting models to be loaded from. */
	public static final String NAMESPACE_WEIGHTING
		= "uk.ac.gla.terrier.matching.models.";

	/**
	 * Set a system property.
	 * @param key The name of the system property.
	 * @param value The value of the system property.
	 */
    public void setProperty(String key, String value) throws RemoteException
	{
		System.setProperty(key,value);
	}
    
    public void setId(String id) throws RemoteException{
    	matching.setId(id);
    }
    
    public void setQEWeights(double[] weights) throws RemoteException
    {
    	matching.setQEWeights(weights[0], weights[1], weights[2]);
    }
    
    public String getId() throws RemoteException{
    	return matching.getId();
    }
    
    public void setCacheTerms(boolean cache) throws RemoteException{
    	matching.setCacheTerms(cache);
    }

	/**
	 * The default constructor.
	 * @param pathBody The path of the body index.
	 * @param prefixBody The prefix of the body index.
	 * @param pathAtext The path of the anchor text index.
	 * @param prefixAtext The prefix of the anchor text index.
	 * @param pathTitle The path of the title index.
	 * @param prefixTitle The prefix of the title index.
	 * @throws RemoteException 
	 */
	public DistributedThreeMatchingServer(
			String pathBody, String prefixBody,
			String pathAtext, String prefixAtext,
			String pathTitle, String prefixTitle) throws RemoteException{
		matching = new DistributedThreeMatching(
				pathBody, prefixBody,
				pathAtext, prefixAtext,
				pathTitle, prefixTitle);
		String[] docnos = matching.getDocumentBoundaries();
		this.firstDocno = docnos[0];
		this.lastDocno = docnos[1];
	}
	
	/**
	 * Check if a document is on the server by a given docno.
	 * @param docno The docno of the given document.
	 * @return boolean True if the document is on the server and false
	 * if the other way around.
	 * @throws RemoteException 
	 */
	public boolean isDocumentOnTheServer(String docno) throws RemoteException{
		StringComparator comparator = new StringComparator();
		boolean yes = false;
		int first = comparator.compare(docno, firstDocno);
		int last = comparator.compare(docno, lastDocno);
		if (first >= 0 && last <=0)
			yes = true;
		return yes;
	}
	
	public void setNormalisationStrategy(boolean value) throws RemoteException{
		this.matching.setNormalisationStrategy(value);
	}
	
	/**
	 * Set if to ignore terms with low idf from matching.
	 * @param value True if to ignore and false if not to ignore.
	 * @throws RemoteException
	 */
	public void setIgnoreLowIdfTerms(boolean value) throws RemoteException{
		matching.setIgnoreLowIdfTerms(value);
	}
	
	/**
	 * Get terms in a given list of documents.
	 * @param docnos The docnos of the list of documents.
	 * @param distTerms A data structure that stores terms in a list of given documents.
	 * @return DistributedExpansionTerms The terms that are stored in a distributedExpansionTerms.
	 */
	public DistributedExpansionTerms getTerms(String[] docnos, 
			DistributedExpansionTerms distTerms,
			String globalLexiconAddress) throws RemoteException{
		long start = System.currentTimeMillis();
		DistributedExpansionTerms terms = matching.getTerms(docnos, distTerms, globalLexiconAddress);
		long time = System.currentTimeMillis() - start;
		System.gc();
		System.err.println("get terms finished in "+time+" ms.");
		return terms;
	}
	
	public int[] getFullDocumentLength(String[] docnos) throws RemoteException{
		int[] length = new int[docnos.length];
		int n = length.length;
		for (int i=0; i<n; i++)
			length[i]=matching.getFullDocumentLength(docnos[i]);
		return length;
	}
	
	public THashSet checkInvIndex(){
		return null;
		//matching.checkInvIndex();
	}
	
	/**
	 * Get the document length that is the sum of the length in the three 
	 * fields in a given document.
	 * @param docno The docno of the given document.
	 * @return The length of the given document.
	 * @throws RemoteException
	 */
	public int getFullDocumentLength(String docno) throws RemoteException{	
		return matching.getFullDocumentLength(docno);
	}
	
	/**
	 * Get the within-document frequency of a given term in a given document, which
	 * is the sum of the term frequencies in the three fields.
	 * @param term The given term.
	 * @param docno The docno of the given document.
	 * @return double The within-document term frequency.
	 * @throws RemoteException
	 */
	public double getFullWithinDocFrequency(String term, String docno) throws RemoteException{
		return matching.getFullWithinDocFrequency(term, docno);
	}
	/**
	 * Get the term frequency and the document frequency in the subcollection
	 * of a given term.
	 * @param term The given term.
	 * @return double[] A two-element array. The first element contains the term
	 * frequency in the subcollection and the second element contains the document
	 * frequency in the subcollection.
	 * @throws RemoteException
	 */
	public double[] getFullStats(String term) throws RemoteException{
		return matching.getFullStats(term);
	}
	
	/**
	 * Get number of documents in each field in the subcollection.
	 * @return int[] A 3-element array. Each element contains the number
	 * of documents in a field. The first element is for body, the 
	 * second for anchor text and the third for title.
	 * @throws RemoteException
	 */
	public int[] getNumberOfDocuments() throws RemoteException{
		return matching.getNumberOfDocuments();
	}
	/**
	 * Get number of tokens in each field.
	 * @return long[] A 3-element array. Each element contains the number
	 * of tokens in a field. The first element is for body, the 
	 * second for anchor text and the third for title.
	 * @throws RemoteException
	 */
	public long[] getNumberOfTokens() throws RemoteException{
		return matching.getNumberOfTokens();
	}
	/**
	 * Get number of unique terms in each field.
	 * @return long[] A 3-element array. Each element contains the number
	 * of unique terms in a field. The first element is for body, the 
	 * second for anchor text and the third for title.
	 * @throws RemoteException
	 */
	public long[] getNumberOfUniqueTerms() throws RemoteException{
		return matching.getNumberOfUniqueTerms();
	}
	/**
	 * Get number of pointers in each field.
	 * @return long[] A 3-element array. Each element contains the number
	 * of pointers in a field. The first element is for body, the 
	 * second for anchor text and the third for title.
	 * @throws RemoteException
	 */
	public long[] getNumberOfPointers() throws RemoteException{
		return matching.getNumberOfPointers();
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
	 * @throws RemoteException
	 */
	public String[] getDocumentNumbers(int[] docids, int start, int end) throws RemoteException{
		return matching.getDocNumbers(docids, start, end);
	}
	
	/**
	 * Updates the statistics of the query with 
	 * the local frequencies from the local inverted file.
	 * @param query a distributed query.
	 * @return DistributedQuery the updated query for the local inverted file
	 * @throws RemoteException
	 */
	public LocalMatchingQueryTerms updateQuery(LocalMatchingQueryTerms dmqt) throws RemoteException{
		return matching.updateQuery(dmqt);
	}
	/**
	 * Set the term frequency normalisation methods, parameters and the 
	 * weights of the three fields.
	 * @param normalisationNames The names of the normalisation methods.
	 * @param parameters The parameter values.
	 * @param weights The weights of the three fields.
	 * @throws RemoteException
	 */
	public void setNormalisation(
			String[] normalisationNames,
			double[] parameters,
			double[] weights) throws RemoteException{
		matching.setNormalisation(normalisationNames, parameters, weights);
	}
	
	/**
	 * Retrieves the documents that match the query
	 * @param query the distributed query to process
	 * @param numOfRetDocs the number of retrieved documents to return
	 * @return DistributedResultSet the top n retrieved documents
	 * @throws RemoteException
	 */
	public ResultSet retrieve(DistributedMatchingQueryTerms dmqt, int numberOfRetrievedDocuments) throws RemoteException{
		boolean CACHE_RESULTS = (new Boolean(ApplicationSetup.getProperty(
				"cache.results", "false"))).booleanValue();
		try{
			final long startTime = System.currentTimeMillis();
			if (CACHE_RESULTS){
				String key = dmqt.getKey();
				System.out.println("results cached for query "+dmqt.queryid);
				if (this.queryResultMap.contains(key))
					return (ResultSet)queryResultMap.get(key);
			}
			matching.match(dmqt);
		//	matching.plainMatch(dmqt);
			System.err.println("Retrieval time: "+ (System.currentTimeMillis() - startTime));
		}
		catch(Error e){
			e.printStackTrace();
			return null;
		}
		ResultSet drs = matching.getResultSet().getResultSet(0, numberOfRetrievedDocuments);
		drs.addMetaItems("docnos", 
					matching.getDocNumbers(
							drs.getDocids(), 0, drs.getExactResultSize()-1));
		System.gc();
		if (CACHE_RESULTS)
			queryResultMap.put(dmqt.getKey(), drs);
		return drs;
	}
	/**
	 * Get the retrieval result set.
	 * @return The retrieval result set.
	 * @throws RemoteException
	 */
	public ResultSet getResultSet() throws RemoteException{
		return matching.getResultSet();
	}
	
	/**
	 * Sets the weighting model
	 * @param wmodel The weighting model to be used for ranking
	 */
	public void setWeightingModel(Model wmodel) throws RemoteException{
		this.matching.setModel(wmodel);
	}
	/**
	 * Set the global statistics to the server.
	 * @param numberOfDocuments The number of documents in the three fields in
	 * the whole collection.
	 * @param numberOfTokens The number of tokens in the three fields in
	 * the whole collection.
	 * @param totalNumberOfDocuments The number of documents in the whole collection.
	 * @param totalNumberOfTokens The number of tokens in the whole collection.
	 * @throws RemoteException
	 */
	public void setGlobalStatistics(
			long[] numberOfDocuments,
			long[] numberOfTokens, 
			long totalNumberOfDocuments,
			long totalNumberOfTokens) throws RemoteException{
		matching.setGlobalStatistics(
				numberOfDocuments,
				numberOfTokens, 
				totalNumberOfDocuments,
				totalNumberOfTokens);
	}
	/**
	 * Stop the server.
	 * @throws RemoteException
	 */
	public void stopServer() throws RemoteException{
		System.gc();
		System.err.println("Stop server now.");
		System.exit(0);
	}
	/**
	 * The main method that runs the server.
	 * @param args A six-element string array. The elements are, from left to right,
	 * the path of the body index, the prefix of the body index,
	 * the path of the anchor text index, the prefix of the anchor text index,
	 * the path of the title index, and the prefix of the title index.
	 */
	public static void main(String[] args) {
		if (logger.isDebugEnabled())
			logger.debug("Starting server...");
		System.gc();
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new RMISecurityManager());
		}
		String hostName = null;
		try {
			hostName = InetAddress.getLocalHost().getHostName();
		} catch (java.net.UnknownHostException e) {
			if (logger.isDebugEnabled())
			logger.debug("Unable to access the local host's name.");
		}
		final int rmi_port = Integer.parseInt(ApplicationSetup.getProperty("terrier.rmi.port", "1099"));
		if (logger.isInfoEnabled())
			logger.info("Using port "+rmi_port);
		String matchName = "//"+hostName+":"+rmi_port+"/DistMatch-"+args[0];
		
		// initialise the server
		DistributedThreeMatchingServer match = null;
		
		try {
			
			match = new DistributedThreeMatchingServer(
					args[1], args[2],
					args[3], args[4],
					args[5], args[6]);
			Naming.rebind(matchName, (DistributedMatchingServer)match);
			System.err.println("DistributedMatchingServer bound");
		} catch (Exception e) {
			for (int i=1;i<=6;i++)
			   System.err.println(args[i]);
			System.err.println("args.length: "+args.length);
			System.err.println("DistributedMatchingServer exception: " + 
				   e.getMessage());
			e.printStackTrace();
		}
		System.gc();
	}
}

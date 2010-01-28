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
 * The Original Code is DistributedMatchingServer.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk> 
 */
package uk.ac.gla.terrier.distr.matching;

import java.rmi.Remote;
import java.rmi.RemoteException;

import uk.ac.gla.terrier.distr.structures.DistributedExpansionTerms;
import uk.ac.gla.terrier.distr.structures.DistributedMatchingQueryTerms;
import uk.ac.gla.terrier.distr.structures.LocalMatchingQueryTerms;
import uk.ac.gla.terrier.matching.Model;
import uk.ac.gla.terrier.matching.ResultSet;

/**
 * This class provides a contract for running a matching server in
 * a distributed setting.
 * @author Ben He(ben@dcs.gla.ac.uk)
 * @version $Revision: 1.9 $
 */
public interface DistributedMatchingServer extends Remote{
	/**
	 * Set the server id.
	 * @param id The server id.
	 * @throws RemoteException
	 */
	public abstract void setId(String id) throws RemoteException;
	/**
	 * Get the id of the server.
	 * @return The server id.
	 * @throws RemoteException
	 */
	public abstract String getId() throws RemoteException;
	
	/**
	 * Updates the statistics of the distributed query with 
	 * the local frequencies from the local inverted file.
	 * @param query a distributed query.
	 * @return DistributedQuery the updated query for the local inverted file
	 * @throws RemoteException if there is an error in the level of RMI
	 */
	public LocalMatchingQueryTerms updateQuery(
			LocalMatchingQueryTerms dmqt) throws RemoteException;
	/**
	 * Set global statistics of the fields
	 * @param numberOfTokens
	 * @param numberOfTerms
	 * @param numberOfPointers
	 * @throws RemoteException if there is an error in the level of RMI
	 */
	public void setGlobalStatistics(
			long[] numberOfDocuments,
			long[] numberOfTokens, 
			long totalNumberOfDocuments,
			long totalNumberOfTokens) throws RemoteException;
	/**
	 * Set if cache the results of the term reweighing during query expansion.
	 * @param cache A boolean variable.
	 * @throws RemoteException
	 */
	public void setCacheTerms(boolean cache) throws RemoteException;
	
	/**
	 * Check if a document exists on the subcollection that the server mounts.
	 * It does it by looking at the docnos of the first and last documents in
	 * the subcollection.
	 * @param docno 
	 * @return true if the document exists in the subsection and false if not.
	 * @throws RemoteException if there is an error in the level of RMI
	 */
	public boolean isDocumentOnTheServer(String docno) throws RemoteException;
	/**
	 * Set the query expansion weights.
	 * @param wqb
	 * @param wqa
	 * @param wqt
	 * @throws RemoteException
	 */
	public void setQEWeights(double[] weights) throws RemoteException;
	/**
	 * Set the normalisation methods for the three fields to the server.
	 * @param normalisationNames The names of the normalisation methods.
	 * @param parameters The parameter settings.
	 * @param weights The weights of the three fields.
	 * @throws RemoteException if there is an error in the level of RMI
	 */
	public void setNormalisation(
			String[] normalisationNames,
			double[] parameters,
			double[] weights) throws RemoteException;
	
	/**
	 * Set a system property. Note that this will not take effect if
	 * the property has already been given in the .properties file. 
	 * @param key The name of the property.
	 * @param value The property value.
	 * @throws RemoteException if there is an error in the level of RMI
	 */
	public void setProperty(String key, String value) throws RemoteException;
    /**
     * Get the result set after the retrieval.
     * @return The result set.
     * @throws RemoteException if there is an error in the level of RMI.
     */
	public ResultSet getResultSet() throws RemoteException;
	/**
	 * Get the length of a given document, which is the sum of the lengths
	 * in the three fields of the given document.
	 * @param docno The docno of the document.
	 * @return The document length.
	 * @throws RemoteException if there is an error in the level of RMI.
	 */
	public int getFullDocumentLength(String docno) throws RemoteException;
	
	public int[] getFullDocumentLength(String[] docnos) throws RemoteException;
	/**
	 * Get the within-document frequency of a term, which the sum of the frequency 
	 * in the three fields.
	 * @param term The given term.
	 * @param docno The docno of the given document.
	 * @return The within-document frequency.
	 * @throws RemoteException if there is an error in the level of RMI.
	 */
	public double getFullWithinDocFrequency(String term, String docno) throws RemoteException;
	
	/**
	 * Get the in-collection frequency and document frequency of a given term.
	 * @param term The given term.
	 * @return A two-element double array. The first element contains the in-collection
	 * frequency and the second one contains the document frequency.
	 * @throws RemoteException if there is an error in the level of RMI.
	 */
	public double[] getFullStats(String term) throws RemoteException;
	/**
	 * Get the terms in a set of documents. This is used for query expansion.
	 * @param docno The docnos of the documents.
	 * @param distTerms A data structure containing terms in a set of documents.
	 * @param globalLexiconAddress The filename of the global lexicon.
	 * @return The terms in the given documents.
	 * @throws RemoteException if there is an error in the level of RMI.
	 */
	public DistributedExpansionTerms getTerms(String docno[], 
			DistributedExpansionTerms distTerms,
			String globalLexiconAddress) throws RemoteException;
	
	/**
	 * Retrieves the documents that match the query
	 * @param query the distributed query to process
	 * @param numOfRetDocs the number of retrieved documents to return
	 * @return DistributedResultSet the top n retrieved documents
	 * @throws RemoteException if there is an error in the level of RMI.
	 */
	public ResultSet retrieve(
			DistributedMatchingQueryTerms dmqt,
			int numberOfRetrievedDocuments) throws RemoteException;
	/**
	 * Get number of documents in each field.
	 * @return An array with a size of 3. [body, atext, title]
	 */
	public int[] getNumberOfDocuments() throws RemoteException;
	/**
	 * Get number of tokens in each field.
	 * @return The number of tokens in each field.
	 */
	public long[] getNumberOfTokens() throws RemoteException;
	/**
	 * Set if ignore the terms with low idf from matching.
	 * @param value True to ignore and false to keep.
	 * @throws RemoteException if there is an error in the level of RMI.
	 */
	public void setIgnoreLowIdfTerms(boolean value) throws RemoteException;
	
	/**
	 * Set if simple normalisation is applied. False by default.
	 * @param value
	 * @throws RemoteException
	 */
	public void setNormalisationStrategy(boolean value) throws RemoteException;
	
	/**
	 * Get number of unique terms in each field.
	 * @return The number of unique terms in each field.
	 */
	public long[] getNumberOfUniqueTerms() throws RemoteException;
	
	/**
	 * Get the number of pointers in each field.
	 * @return The number of pointers in each field.
	 * @throws RemoteException if there is an error in the level of RMI.
	 */
	public long[] getNumberOfPointers() throws RemoteException;
	
	/**
	 * Convert the docids into docdos.
	 * @param docids The docids.
	 * @param start The starting point of the docids to convert.
	 * @param end The ending point of the docids to convert.
	 * @return The docnos that correspond to the docids.
	 * @throws RemoteException if there is an error in the level of RMI.
	 */
	public String[] getDocumentNumbers(int[] docids, int start, int end)throws RemoteException;
	
	/**
	 * Sets the weighting model.
	 * @param wmodel The weighting model to be used for ranking.
	 * @throws RemoteException if there is an error in the level of RMI.
	 */
	public void setWeightingModel(Model wmodel) throws RemoteException;
	
	/**
	 * Stop the server. This will not kill RMI registry process.
	 * @throws RemoteException if there is an error in the level of RMI.
	 */
	public void stopServer() throws RemoteException;
	
}

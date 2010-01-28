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
 * The Original Code is DistributedQueryExpansion.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk> 
 */
package uk.ac.gla.terrier.distr.querying;

import uk.ac.gla.terrier.distr.structures.DistributedExpansionTerms;
import uk.ac.gla.terrier.matching.MatchingQueryTerms;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.querying.Manager;
import uk.ac.gla.terrier.querying.QueryExpansion;
import uk.ac.gla.terrier.querying.Request;
import uk.ac.gla.terrier.querying.SearchRequest;
import uk.ac.gla.terrier.querying.parser.SingleTermQuery;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Rounding;
import uk.ac.gla.terrier.utility.TerrierTimer;

/**
 * This class does query expansion on a given resource in the form of 
 * DistributedThreeManager.
 * @author Ben He(ben@dcs.gla.ac.uk)
 * @version $Revision: 1.7 $
 */
public class DistributedQueryExpansion extends QueryExpansion{
	/**
 	* This method implements the functionality of expanding a query over
 	* three indices.
 	* @param query MatchingQueryTerms the query terms of 
 	*        the original query.
 	* @param resultSet CollectionResultSet the set of retrieved 
 	*        documents from the first pass retrieval.
 	*/
	public void expandQuery(MatchingQueryTerms query, ResultSet resultSet, Manager manager) {
        // the number of term to re-weight (i.e. to do relevance feedback) is
		// the maximum between the system setting and the actual query length.
		// if the query length is larger than the system setting, it does not
		// make sense to do relevance feedback for a portion of the query. Therefore, 
		// we re-weight the number of terms that equals the query length.
		int numberOfTermsToReweight = Math.max(ApplicationSetup.EXPANSION_TERMS, 
				query.length());

        // If no document retrieved, keep the original query.
        if (resultSet.getResultSize() == 0 || resultSet.getScores()[0] <= 0){ 
            System.out.println("Empty result set. Query expansion disabled.");       	
        	return;
        }
        // get the docnos of the retrieved documents from the result set.
		String[] docnos = resultSet.getMetaItems("docnos");
		
		// if the number of retrieved documents is lower than the parameter
		// EXPANSION_DOCUMENTS, reduce the number of documents for expansion
		// to the number of retrieved documents.
		int effDocuments = Math.min(resultSet.getExactResultSize(), 
				ApplicationSetup.EXPANSION_DOCUMENTS);
		// compute the number of tokens in the top-ranked documents.
		int totalDocumentLength = 0;
		for (int i = 0; i < effDocuments; i++){
			if (manager instanceof DistributedThreeManager)
			totalDocumentLength += ((DistributedThreeManager)manager).getFullDocumentLength(docnos[i],
					Integer.parseInt(resultSet.getMetaItem("serverIndex", resultSet.getDocids()[i])));
			else if (manager instanceof DistributedFieldManager)
				totalDocumentLength += ((DistributedFieldManager)manager).getFullDocumentLength(docnos[i],
						Integer.parseInt(resultSet.getMetaItem("serverIndex", resultSet.getDocids()[i])));
		}
		// instantiate a DistributedExpansionTerms that stores the statistics
		// of terms in the top-ranked documents for term-weighting.
		DistributedExpansionTerms expansionTerms = null;
		if (manager instanceof DistributedThreeManager){
			expansionTerms =
				new DistributedExpansionTerms(totalDocumentLength, 
						(double)((DistributedThreeManager)manager).totalNumOfTokens, 
						((DistributedThreeManager)manager).avl, 
						((DistributedThreeManager)manager).totalNumOfDocuments);
		}else if (manager instanceof DistributedFieldManager){
			expansionTerms =
				new DistributedExpansionTerms(totalDocumentLength, 
						(double)((DistributedFieldManager)manager).totalNumOfTokens, 
						((DistributedFieldManager)manager).avl, 
						((DistributedFieldManager)manager).totalNumOfDocuments);
		}

		System.err.println("number of top-ranked docs for qe: " + effDocuments);
		
		TerrierTimer timer = new TerrierTimer();
		timer.start();
		
		String[] qeDocnos = new String[effDocuments];
		String[] qeServerIndex = new String[effDocuments];
		for (int i = 0; i < effDocuments; i++) {
			qeDocnos[i] = docnos[i];
			qeServerIndex[i] = resultSet.getMetaItem("serverIndex", resultSet.getDocids()[i]);
		}
		// Get the terms' statistics from different servers. The process is threaded.
		if (manager instanceof DistributedThreeManager)
			expansionTerms = ((DistributedThreeManager)manager).getTermsThreaded(
					qeDocnos,
					qeServerIndex, 
					expansionTerms);
		else if (manager instanceof DistributedFieldManager)
			expansionTerms = ((DistributedFieldManager)manager).getTermsThreaded(
					qeDocnos,
					qeServerIndex, 
					expansionTerms);
		if (expansionTerms.terms.size()==0){
			System.out.println("No extracted terms. Skip the query expansion process.");
			return;
		}
		
		// Compute the term weights and get the expanded terms.
		if (ApplicationSetup.EXPANSION_TERMS == 0)
			expansionTerms.setOriginalQueryTerms(query.getTerms());
		SingleTermQuery[] expandedTerms =
			expansionTerms.getExpandedTerms(numberOfTermsToReweight, QEModel);
		timer.setBreakPoint();
		System.err.println("Query expansion finished in " + timer.toStringMinutesSeconds());
		
		System.err.println("number of terms in re-weighted query: " + expandedTerms.length);
		
		for (int i = 0; i < expandedTerms.length; i++){
			SingleTermQuery expandedTerm = expandedTerms[i];
			query.addTermPropertyWeight(expandedTerm.getTerm(), expandedTerm.getWeight());
		}
		int ql = query.length();
		String[] terms = query.getTerms();
		for (int i = 0; i < ql; i++){
			System.err.println("term " + terms[i]
				 	+ " appears in expanded query with normalised weight: "
					+ Rounding.toString(query.getTermWeight(terms[i]), 4));
		}
	}
	
	/**
	 * Runs the actual query expansion in a distributed setting.
	 * @see uk.ac.gla.terrier.querying.PostProcess#process(uk.ac.gla.terrier.querying.Manager,uk.ac.gla.terrier.querying.SearchRequest)
	 */
	public void process(Manager manager, SearchRequest q) {
	   	System.err.println("query expansion post-processing.");
		//get the query expansion model to use
		String qeModel = q.getControl("qemodel");
		if (qeModel == null || qeModel.length() ==0)
		{
			qeModel = ApplicationSetup.getProperty("default.qemodel", "Bo1");
			System.err.println("WARNING: qemodel control not set for QueryExpansion"+
					" post process. Using default model "+qeModel);
		}
	    setQueryExpansionModel(getQueryExpansionModel(qeModel));
	    System.err.println("query expansion model: " + QEModel.getInfo());
	    MatchingQueryTerms queryTerms = ((Request)q).getMatchingQueryTerms();
	    ResultSet resultSet = q.getResultSet();
	    // get the expanded query terms
	    expandQuery(queryTerms, resultSet, manager);
	    System.err.println("query length after expansion: " + queryTerms.length());
	   }
}

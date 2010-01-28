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
 * The Original Code is BasicQueryExpansion.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Gianni Amatti <gba{a.}fub.it> (original author)
 *   Ben He <ben{a.}dcs.gla.ac.uk>
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.querying;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.matching.models.WeightingModel;
import uk.ac.gla.terrier.matching.models.queryexpansion.QueryExpansionModel;
import uk.ac.gla.terrier.querying.QueryExpansion;
import uk.ac.gla.terrier.querying.parser.SingleTermQuery;
import uk.ac.gla.terrier.structures.BasicQuery;
import uk.ac.gla.terrier.structures.ExpansionTerms;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.trees.TermTreeNode;
import uk.ac.gla.terrier.utility.ApplicationSetup;
/**
 * Implements automatic query expansion as PostFilter that is applied to the resultset
 * after 1st-time matching.
 * <B>Controls</B>
 * <ul><li><tt>qemodel</tt> : The query expansion model used for Query Expansion. 
 * Defauls to Bo1.</li></ul>
 * <B>Properties</B>
 * <ul><li><tt>expansion.terms</tt> : The maximum terms for query expansion</li>
 * <li><tt>expansion.documents</tt> : The number of top documents from the 1st pass 
 * retrieval to use for QE</li>
 * </ul>
 * @version $Revision: 1.1 $
 * @author Gianni Amatti, Ben He, Vassilis Plachouras, Craig Macdonald
 */
public class BasicQueryExpansion extends QueryExpansion {
	
	public BasicQueryExpansion(Index index, WeightingModel qemodel){
		this.invertedIndex = index.getInvertedIndex();
		this.directIndex = index.getDirectIndex();
		this.documentIndex = index.getDocumentIndex();
		this.QEModel = qemodel;
		this.lexicon = index.getLexicon();
	}
	
	public BasicQueryExpansion(Index index){
		this.invertedIndex = index.getInvertedIndex();
		this.directIndex = index.getDirectIndex();
		this.documentIndex = index.getDocumentIndex();
		this.lexicon = index.getLexicon();
	}
	
	/**
 	* This method implements the functionality of expanding a query.
 	* @param query MatchingQueryTerms the query terms of 
 	*        the original query.
 	* @param resultSet CollectionResultSet the set of retrieved 
 	*        documents from the first pass retrieval.
 	*/
	public void expandQuery(BasicQuery query, ResultSet resultSet) {
		
		//		 If query length is larger than the default number of expanded terms, 
        //  we keep the original query.
        if (ApplicationSetup.EXPANSION_TERMS <= query.getQueryLength()){
        	System.err.println("Query length is larger than the number of " +
        			"expanded terms. Keep the original query.");
        	return;
        }

        // If no document retrieved, keep the original query.
        if (resultSet.getResultSize() == 0){        	
        	return;
        }

		int[] docIDs = resultSet.getDocids();
        double[] scores = resultSet.getScores();
		double totalDocumentLength = 0;
		
		// if the number of retrieved documents is lower than the parameter
		// EXPANSION_DOCUMENTS, reduce the number of documents for expansion
		// to the number of retrieved documents.
		int effDocuments = Math.min(docIDs.length, ApplicationSetup.EXPANSION_DOCUMENTS); 
		for (int i = 0; i < effDocuments; i++)
			totalDocumentLength += documentIndex.getDocumentLength(docIDs[i]);
		ExpansionTerms expansionTerms =
			new ExpansionTerms(collStats, totalDocumentLength, lexicon);
		for (int i = 0; i < effDocuments; i++) {
			int[][] terms = directIndex.getTerms(docIDs[i]);
			for (int j = 0; j < terms[0].length; j++)
				expansionTerms.insertTerm(terms[0][j], (double)terms[1][j]);
		}
		SingleTermQuery[] expandedTerms =
			expansionTerms.getExpandedTerms(ApplicationSetup.EXPANSION_TERMS, QEModel);

		for (int i = 0; i < expandedTerms.length; i++){
			SingleTermQuery expandedTerm = expandedTerms[i];
			TermTreeNode expandedTermTreeNode = new TermTreeNode(expandedTerm.getTerm());
			expandedTermTreeNode.normalisedFrequency = expandedTerm.getWeight();
			query.addTermTreeNode(expandedTermTreeNode);
			//query.addTermPropertyWeight(expandedTerm.getTerm(), expandedTerm.getWeight());
//			 System.err.println("term " + expandedTerms[i].getTerm()
//			 	+ " appears in expanded query with normalised weight: "
//				+ Rounding.toString(query.getTermWeight(expandedTerms[i].getTerm()), 4));
        	}

	}
}

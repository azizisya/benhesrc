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
 * The Original Code is QueryExpansion.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Gianni Amatti <gba{a.}fub.it> (original author)
 *   Ben He <ben{a.}dcs.gla.ac.uk>
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.querying;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.matching.MatchingQueryTerms;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.matching.models.queryexpansion.QueryExpansionModel;
import uk.ac.gla.terrier.querying.parser.SingleTermQuery;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.DirectIndex;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.ExpansionTerms;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.InvertedIndex;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Rounding;
/**
 * Implements automatic query expansion as PostFilter that is applied to the resultset
 * after 1st-time matching.
 * <B>Controls</B>
 * <ul><li><tt>qemodel</tt> : The query expansion model used for Query Expansion. 
 * Defauls to Bo1.</li></ul>
 * <B>Properties</B>
 * <ul><li><tt>expansion.terms</tt> : The maximum number of most weighted terms in the 
 * pseudo relevance set to be added to the original query. The system performs a conservative
 * query expansion if this property is set to 0. A conservation query expansion only reweighs
 * the original query terms without adding new terms to the query.</li>
 * <li><tt>expansion.documents</tt> : The number of top documents from the 1st pass 
 * retrieval to use for QE. The query is expanded from this set of docuemnts, also 
 * known as the pseudo relevance set.</li>
 * </ul>
 * @version $Revision: 1.1 $
 * @author Gianni Amatti, Ben He, Vassilis Plachouras, Craig Macdonald
 */
public class QueryExpansion implements PostProcess {
	protected static Logger logger = Logger.getRootLogger();
	/**
	 * The default namespace of query expansion model classes.
	 * The query expansion model names are prefixed with this
	 * namespace, if they are not fully qualified.
	 */
	public static final String NAMESPACE_QEMODEL = "uk.ac.gla.terrier.matching.models.queryexpansion.";
	/**
	 * Caching the query expansion models that have been
	 * created so far.
	 */
	protected Map<String, QueryExpansionModel> Cache_QueryExpansionModel = new HashMap<String, QueryExpansionModel>();
	/** The document index used for retrieval. */
	protected DocumentIndex documentIndex;
	/** The inverted index used for retrieval. */
	protected InvertedIndex invertedIndex;
	/** An instance of Lexicon class. */
	protected Lexicon lexicon;
	/** The direct index used for retrieval. */
	protected DirectIndex directIndex;
	/** The statistics of the index */
	protected CollectionStatistics collStats;
	/** The query expansion model used. */
	protected QueryExpansionModel QEModel;
	/**
	* The default constructor of QueryExpansion.
	*/
	public QueryExpansion() {}
	/** Set the used query expansion model.
	*  @param QEModel QueryExpansionModel The query expansion model to be used.
	*/
	public void setQueryExpansionModel(QueryExpansionModel QEModel){
		this.QEModel = QEModel;
	}
	/**
 	* This method implements the functionality of expanding a query.
 	* @param query MatchingQueryTerms the query terms of 
 	*		the original query.
 	* @param resultSet CollectionResultSet the set of retrieved 
 	*		documents from the first pass retrieval.
 	*/
	public void expandQuery(MatchingQueryTerms query, ResultSet resultSet) {
		// the number of term to re-weight (i.e. to do relevance feedback) is
		// the maximum between the system setting and the actual query length.
		// if the query length is larger than the system setting, it does not
		// make sense to do relevance feedback for a portion of the query. Therefore, 
		// we re-weight the number of query length of terms.
		int numberOfTermsToReweight = Math.max(ApplicationSetup.EXPANSION_TERMS, 
				query.length());
		if (ApplicationSetup.EXPANSION_TERMS == 0)
			numberOfTermsToReweight = 0;

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
		for (int i = 0; i < effDocuments; i++){
			totalDocumentLength += documentIndex.getDocumentLength(docIDs[i]);
			if(logger.isDebugEnabled()){
			logger.debug((i+1)+": " + documentIndex.getDocumentNumber(docIDs[i])+
					" ("+docIDs[i]+") with "+scores[i]);
			}
		}
		ExpansionTerms expansionTerms =
			new ExpansionTerms(collStats, totalDocumentLength, lexicon);
		for (int i = 0; i < effDocuments; i++) {
			int[][] terms = directIndex.getTerms(docIDs[i]);
			if (terms == null)
				logger.warn("document "+documentIndex.getDocumentLength(docIDs[i]) + "("+docIDs[i]+") not found");
			else
				for (int j = 0; j < terms[0].length; j++)
					expansionTerms.insertTerm(terms[0][j], (double)terms[1][j]);
		}
		expansionTerms.setOriginalQueryTerms(query);
		SingleTermQuery[] expandedTerms =
			expansionTerms.getExpandedTerms(numberOfTermsToReweight, QEModel);
		for (int i = 0; i < expandedTerms.length; i++){
			SingleTermQuery expandedTerm = expandedTerms[i];
			query.addTermPropertyWeight(expandedTerm.getTerm(), expandedTerm.getWeight());
			if(logger.isDebugEnabled()){
				logger.debug("term " + expandedTerms[i].getTerm()
				 	+ " appears in expanded query with normalised weight: "
					+ Rounding.toString(query.getTermWeight(expandedTerms[i].getTerm()), 4));
			}
			}
			

	}

	/** For easier sub-classing of which index the query expansion comes from */
	protected Index getIndex(Manager m)
	{
		return m.getIndex();
	}



	/**
	 * Runs the actual query expansion
	 * @see uk.ac.gla.terrier.querying.PostProcess#process(uk.ac.gla.terrier.querying.Manager,uk.ac.gla.terrier.querying.SearchRequest)
	 */
	public void process(Manager manager, SearchRequest q) {
	   	Index index = getIndex(manager);
		documentIndex = index.getDocumentIndex();
		invertedIndex = index.getInvertedIndex();
		lexicon = index.getLexicon();
		collStats = index.getCollectionStatistics(); 
		directIndex = index.getDirectIndex();
		if (directIndex == null)
		{
			logger.error("This index does not have a direct index. Query expansion disabled!!");
			return;
		}
		logger.info("Starting query expansion post-processing.");
		//get the query expansion model to use
		String qeModel = q.getControl("qemodel");
		if (qeModel == null || qeModel.length() ==0)
		{
			logger.warn("qemodel control not set for QueryExpansion"+
					" post process. Using default model Bo1");
			qeModel = "Bo1";
		}
		setQueryExpansionModel(getQueryExpansionModel(qeModel));
		if(logger.isInfoEnabled()){
			logger.info("query expansion model: " + QEModel.getInfo());
		}
		MatchingQueryTerms queryTerms = ((Request)q).getMatchingQueryTerms();
		if (queryTerms == null)
		{
			logger.warn("No query terms for this query. Skipping QE");
			return;
		}
		ResultSet resultSet = q.getResultSet();
		// get the expanded query terms
		expandQuery(queryTerms, resultSet);
		if(logger.isInfoEnabled()){
			logger.info("query length after expansion: " + queryTerms.length());
			logger.info("Expanded query: ");
		}
		final String[] newQueryTerms = queryTerms.getTerms();
		StringBuilder newQuery = new StringBuilder();
		for (int i = 0; i < newQueryTerms.length; i++){
			try{
				if(logger.isInfoEnabled()){
					logger.info((i + 1) + ": " + newQueryTerms[i] +
						", normalisedFrequency: " + Rounding.toString(queryTerms.getTermWeight(newQueryTerms[i]), 4));
				}
				newQuery.append(newQueryTerms[i]);
				newQuery.append('^');
				newQuery.append(Rounding.toString(queryTerms.getTermWeight(newQueryTerms[i]), 5));
				newQuery.append(' ');
			}
			catch(NullPointerException npe){
				logger.fatal("Nullpointer exception occured in Query Expansion dumping of new Query", npe);
			}
		}
		if(logger.isInfoEnabled()){
			logger.info("NEWQUERY "+q.getQueryID() +" "+newQuery.toString());
//			 run retrieval process again for the expanded query
			logger.info("Accessing inverted file for expanded query " + q.getQueryID());
		}
		
		
		manager.runMatching(q);
	}
	/** Obtain the query expansion model for QE to use.
	 *  This will be cached in a hashtable for the lifetime of the
	 *  application. If Name does not contain ".", then <tt>
	 *  NAMESPACE_QEMODEL will be prefixed to it before loading.
	 *  @param Name the naem of the query expansion model to load.
	 */
	public QueryExpansionModel getQueryExpansionModel(String Name)
	{
		QueryExpansionModel rtr = null;
		if (Name.indexOf(".") < 0 )
			Name = NAMESPACE_QEMODEL +Name;
		//check for acceptable matching models
		rtr = (QueryExpansionModel)Cache_QueryExpansionModel.get(Name);
		if (rtr == null)
		{
			try
			{
				rtr = (QueryExpansionModel) Class.forName(Name).newInstance();
			}
			catch(Exception e)
			{
				logger.error("Problem with postprocess named: "+Name+" : ",e);
				return null;
			}
			Cache_QueryExpansionModel.put(Name, rtr);
		}
		return rtr;
	}
	
	/**
	 * Returns the name of the used query expansion model.
	 * @return String the name of the used query expansion model.
	 */
	public String getInfo() {
		if (QEModel != null)
			return QEModel.getInfo();
		return "";
	}
}

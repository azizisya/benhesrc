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
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Gianni Amatti <gba{a.}fub.it> (original author)
 *   Ben He <ben{a.}dcs.gla.ac.uk>
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.querying;
import gnu.trove.TIntDoubleHashMap;

import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;

import java.util.Arrays;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.matching.MatchingQueryTerms;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.matching.models.WeightingModel;
import uk.ac.gla.terrier.querying.termselector.TermSelector;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.DirectIndex;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.ExpansionTerm;
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
	protected WeightingModel QEModel;
	
	protected Index index;
	
	/**
	* The default constructor of QueryExpansion.
	*/
	public QueryExpansion() {}
	/** Set the used query expansion model.
	*  @param QEModel QueryExpansionModel The query expansion model to be used.
	*/
	public void setQueryExpansionModel(WeightingModel QEModel){
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
		
		int effDocuments = Math.min(ApplicationSetup.EXPANSION_DOCUMENTS, docIDs.length);
		int[] feedbackDocIDs = Arrays.copyOf(docIDs, effDocuments);
		
		TermSelector selector = TermSelector.getDefaultTermSelector(index);
		selector.setResultSet(resultSet);
		
		ExpansionTerm[] expTerms = null;
		/*if (QEModel.PerDocQE)
			expTerms = this.expandPerDoc(feedbackDocIDs, query, numberOfTermsToReweight, index, QEModel, selector);
		else*/
			expTerms = this.expandFromDocuments(feedbackDocIDs, query, numberOfTermsToReweight, index, QEModel, selector);
		this.mergeWithExpandedTerms(expTerms, query);
	}
	
	protected void mergeWithExpandedTerms(ExpansionTerm[] expTerms, MatchingQueryTerms query){
		for (int i = 0; i < expTerms.length; i++){
			if (expTerms[i].getWeightExpansion()<=0)
				break;
			lexicon.findTerm(expTerms[i].getTermID());
			double finalWeight = (QEModel.PARAMETER_FREE&&QEModel.SUPPORT_PARAMETER_FREE_QE)?
				(QEModel.ROCCHIO_ALPHA*query.getTermWeight(lexicon.getTerm())+expTerms[i].getWeightExpansion()):
					(QEModel.ROCCHIO_ALPHA*query.getTermWeight(lexicon.getTerm())+QEModel.ROCCHIO_BETA*expTerms[i].getWeightExpansion());
			query.setTermProperty(lexicon.getTerm(), finalWeight);
			if(logger.isDebugEnabled()){
				logger.debug("term " + lexicon.getTerm()
				 	+ " appears in expanded query with normalised weight: "
					+ Rounding.toString(query.getTermWeight(lexicon.getTerm()), 4));
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
		Request rq = (Request)q;
		String c_set = rq.getControl("c_set");
		//String cString = (rq.getControl("c_set").equals("true"))?(rq.getControl("c")):(null);
		
		String cpostString = ApplicationSetup.getProperty("c.post", null);
		if (cpostString!=null){
			rq.setControl("c_set", "true");
			rq.setControl("c", cpostString);
		}
		
	   	index = getIndex(manager);
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
		setQueryExpansionModel(WeightingModel.getWeightingModel(qeModel));
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
		if (cpostString!=null){
			rq.setControl("c_set", c_set);
			rq.setControl("c", rq.getControl("c"));
		}
	}
	
	/**
	 * @param docIDs
	 * @param query
	 * @param numberOfTermsToReweight
	 * @param index
	 * @param QEModel
	 * @param selector
	 * @return
	 * @deprecated
	 */
	public static ExpansionTerm[] expandPerDoc(
			int[] docIDs, 
			MatchingQueryTerms query, 
			int numberOfTermsToReweight,
			Index index,
			WeightingModel QEModel,
			TermSelector selector){
		int effDocuments = docIDs.length;
		selector.setMetaInfo("normalize.weights", "false");
		ExpansionTerm[][] expTerms = new ExpansionTerm[effDocuments][];
		
		// for each of the top-ranked documents
		for (int i=0; i<effDocuments; i++){
			// obtain the weighted terms
			int[] oneDocid = {docIDs[i]};
			expTerms[i] = expandFromDocuments(oneDocid, query, index.getDocumentIndex().getDocumentLength(docIDs[i]), index, QEModel, selector);
		}
		// merge expansion terms: compute mean term weight for each term, sort again
		TIntDoubleHashMap termidWeightMap = new TIntDoubleHashMap();
		TIntIntHashMap termidDFMap = new TIntIntHashMap();
		for (int i=0; i<effDocuments; i++){
			for (int j=0; j<expTerms[i].length; j++){
				termidWeightMap.adjustOrPutValue(expTerms[i][j].getTermID(), expTerms[i][j].getWeightExpansion(), expTerms[i][j].getWeightExpansion());
				termidDFMap.adjustOrPutValue(expTerms[i][j].getTermID(), 1, 1);
			}
		}
		
		ExpansionTerm[] candidateTerms = new ExpansionTerm[termidWeightMap.size()];
		// expansion term should appear in at least half of the feedback documents
		//int minDF = (effDocuments%2==0)?(effDocuments/2):(effDocuments/2+1);
		int minDF = 2; // this creteria is dropped
		int counter = 0;
		for (int termid : termidWeightMap.keys()){
			candidateTerms[counter] = new ExpansionTerm(termid);
			if (docIDs.length>minDF&&termidDFMap.get(termid)<minDF)
				candidateTerms[counter].setWeightExpansion(0d);
			else
				candidateTerms[counter].setWeightExpansion(termidWeightMap.get(termid)/termidDFMap.get(termid));
			counter++;
		}
		Arrays.sort(candidateTerms);
		numberOfTermsToReweight = Math.min(numberOfTermsToReweight, candidateTerms.length);
		ExpansionTerm[] expandedTerms = new ExpansionTerm[numberOfTermsToReweight];
		// normalise the expansion weights by the maximum weight among the expansion terms
		double normaliser = candidateTerms[0].getWeightExpansion();
		for (int i=0; i<numberOfTermsToReweight; i++){
			expandedTerms[i] = candidateTerms[i];
			expandedTerms[i].setWeightExpansion(candidateTerms[i].getWeightExpansion()/normaliser);
		}
		return expandedTerms;
	}
	
	public static ExpansionTerm[] expandFromDocuments(
			int[] docIDs, 
			MatchingQueryTerms query, 
			int numberOfTermsToReweight,
			Index index,
			WeightingModel QEModel,
			TermSelector selector){
		 
		if (query!=null)
			selector.setOriginalQueryTerms(query.getTerms());
		selector.assignTermWeights(docIDs, QEModel, index.getLexicon());
		
		for (int i=0; i<docIDs.length; i++)
			logger.debug("doc "+(i+1)+": "+docIDs[i]);
		logger.debug("Number of unique terms in the feedback document set: "+selector.getNumberOfUniqueTerms());
		TIntObjectHashMap<ExpansionTerm> queryTerms = selector.getMostWeightedTermsInHashMap(numberOfTermsToReweight);
		
		ExpansionTerm[] expTerms = new ExpansionTerm[queryTerms.size()];
		int counter = 0;
		for (int i : queryTerms.keys())
			expTerms[counter++] = queryTerms.get(i);
		Arrays.sort(expTerms);
		return expTerms;
	}
	
	/**
	 * Returns the name of the used query expansion model.
	 * @return String the name of the used query expansion model.
	 */
	public String getInfo() {
		if (QEModel != null)
			return QEModel.getInfo()+QEModel.getQEInfo();
		return "";
	}
}

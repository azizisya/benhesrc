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
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Gianni Amatti <gba{a.}fub.it> (original author)
 *   Ben He <ben{a.}dcs.gla.ac.uk>
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
 */
package org.terrier.querying;
import gnu.trove.TIntObjectHashMap;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import org.terrier.matching.MatchingQueryTerms;
import org.terrier.matching.models.queryexpansion.QueryExpansionModel;
import org.terrier.structures.BitIndexPointer;
import org.terrier.structures.CollectionStatistics;
import org.terrier.structures.DocumentIndex;
import org.terrier.structures.Index;
import org.terrier.structures.InvertedIndex;
import org.terrier.structures.Lexicon;
import org.terrier.structures.MetaIndex;
import org.terrier.structures.PostingIndex;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.Rounding;

import org.terrier.querying.termselector.TermSelector;
import org.terrier.structures.ExpansionTerm;
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
 * <li><tt>qe.feedback.selector</tt> : The class to be used for selecting feedback documents.</li>
 * <li><tt>qe.expansion.terms.class</tt> : The class to be used for selecting expansion terms from feedback documents.</li>
 * </ul>
 * @version $Revision: 1.31 $
 * @author Gianni Amatti, Ben He, Vassilis Plachouras, Craig Macdonald
 */
public class QueryExpansion implements PostProcess {
	protected static final Logger logger = Logger.getRootLogger();
	/**
	 * The default namespace of query expansion model classes.
	 * The query expansion model names are prefixed with this
	 * namespace, if they are not fully qualified.
	 */
	public static final String NAMESPACE_QEMODEL = "org.terrier.matching.models.queryexpansion.";
	/**
	 * Caching the query expansion models that have been
	 * created so far.
	 */
	protected Map<String, QueryExpansionModel> Cache_QueryExpansionModel = new HashMap<String, QueryExpansionModel>();
	/** The document index used for retrieval. */
	protected DocumentIndex documentIndex;
	protected MetaIndex metaIndex;
	/** The inverted index used for retrieval. */
	protected InvertedIndex invertedIndex;
	/** An instance of Lexicon class. */
	protected Lexicon<String> lexicon;
	/** The direct index used for retrieval. */
	protected PostingIndex<BitIndexPointer> directIndex;
	
	
	/** The statistics of the index */
	protected CollectionStatistics collStats;
	/** The query expansion model used. */
	protected QueryExpansionModel QEModel;
	/** The process by which to select feedback documents */
	protected FeedbackSelector docSelector = null;
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
 	* @param rq the Request thus far, giving access to the query and the result set
 	*/
	public void expandQuery(MatchingQueryTerms query, Request rq) throws IOException 
	{
		// the number of term to re-weight (i.e. to do relevance feedback) is
		// the maximum between the system setting and the actual query length.
		// if the query length is larger than the system setting, it does not
		// make sense to do relevance feedback for a portion of the query. Therefore, 
		// we re-weight the number of query length of terms.
		int numberOfTermsToReweight = Math.max(ApplicationSetup.EXPANSION_TERMS, 
				query.length());
		if (ApplicationSetup.EXPANSION_TERMS == 0)
			numberOfTermsToReweight = 0;

		if (docSelector == null)
			docSelector = this.getFeedbackSelector(rq);
		if (docSelector == null)
			return;
		FeedbackDocument[] feedback = docSelector.getFeedbackDocuments(rq);
		if (feedback == null || feedback.length == 0)
			return;
	
		double totalDocumentLength = 0;
		int[] feedbackDocIDs = new int[feedback.length];
		for(int i=0; i<feedbackDocIDs.length; i++)
		{
			FeedbackDocument doc = feedback[i];
			feedbackDocIDs[i] = doc.docid;
			totalDocumentLength += documentIndex.getDocumentLength(doc.docid);

			if(logger.isDebugEnabled()){
				logger.debug(doc.rank +": " + metaIndex.getItem("docno", doc.docid)+
					" ("+doc.docid+") with "+doc.score);
			}
		}
		/**
		ExpansionTerms expansionTerms = getExpansionTerms();
		expansionTerms.setModel(QEModel);
		
		for(FeedbackDocument doc : feedback)
		{
			expansionTerms.insertDocument(doc);
		}
		logger.debug("Selecting "+numberOfTermsToReweight + " from " + expansionTerms.getNumberOfUniqueTerms());
		
		expansionTerms.setOriginalQueryTerms(query);
		SingleTermQuery[] expandedTerms = expansionTerms.getExpandedTerms(numberOfTermsToReweight);*/
		
		TermSelector selector = TermSelector.getDefaultTermSelector(lastIndex);
		selector.setResultSet(rq.getResultSet());
		this.expand(feedbackDocIDs, query, numberOfTermsToReweight, lastIndex, QEModel, selector);
		//ExpansionTerm[] expandedTerms = expandFromDocuments(feedbackDocIDs, query, numberOfTermsToReweight, lastIndex, QEModel, selector);
		/**
		for (int i = 0; i < expandedTerms.length; i++){
			// SingleTermQuery expandedTerm = expandedTerms[i];
			query.addTermPropertyWeight(expandedTerms[i].getTerm(), expandedTerm.getWeight());
			if(logger.isDebugEnabled()){
				logger.debug("term " + expandedTerms[i].getTerm()
				 	+ " appears in expanded query with normalised weight: "
					+ Rounding.toString(query.getTermWeight(expandedTerms[i].getTerm()), 4));
			}
		}*/
		// this.mergeWithExpandedTerms(expandedTerms, query);

	}
	/**
	protected void mergeWithExpandedTerms(ExpansionTerm[] expTerms, MatchingQueryTerms query){
		for (int i = 0; i < expTerms.length; i++){
			if (expTerms[i].getWeightExpansion()<=0)
				break;
			Entry<String, LexiconEntry> entry = lexicon.getLexiconEntry(expTerms[i].getTermID());
			double finalWeight = (QEModel.PARAMETER_FREE&&QEModel.SUPPORT_PARAMETER_FREE_QE)?
				(QEModel.ROCCHIO_ALPHA*query.getTermWeight(entry.getKey())+expTerms[i].getWeightExpansion()):
					(QEModel.ROCCHIO_ALPHA*query.getTermWeight(entry.getKey())+QEModel.ROCCHIO_BETA*expTerms[i].getWeightExpansion());
			query.setTermProperty(entry.getKey(), finalWeight);
			if(logger.isDebugEnabled()){
				logger.debug("term " + entry.getKey()
				 	+ " appears in expanded query with normalised weight: "
					+ Rounding.toString(query.getTermWeight(entry.getKey()), 4));
			}
		}
	}*/
	
	protected void expand(int[] docIDs, 
			MatchingQueryTerms query, 
			int numberOfTermsToReweight,
			Index index,
			QueryExpansionModel QEModel,
			TermSelector selector){
		if (query!=null)
			selector.setOriginalQueryTerms(query.getTerms());
		selector.assignTermWeights(docIDs, QEModel, index.getLexicon());
		
		for (int i=0; i<docIDs.length; i++)
			logger.debug("doc "+(i+1)+": "+docIDs[i]);
		logger.debug("Number of unique terms in the feedback document set: "+selector.getNumberOfUniqueTerms());
		
		selector.mergeWithQuery(QEModel, query, numberOfTermsToReweight);
	}
	
	public ExpansionTerm[] expandFromDocuments(
			int[] docIDs, 
			MatchingQueryTerms query, 
			int numberOfTermsToReweight,
			Index index,
			QueryExpansionModel QEModel,
			TermSelector selector){
		 
		if (query!=null)
			selector.setOriginalQueryTerms(query.getTerms());
		selector.assignTermWeights(docIDs, QEModel, index.getLexicon());
		
		for (int i=0; i<docIDs.length; i++)
			logger.debug("doc "+(i+1)+": "+docIDs[i]);
		logger.debug("Number of unique terms in the feedback document set: "+selector.getNumberOfUniqueTerms());
		
		//selector.mergeWithQuery(QEModel, query, numberOfTermsToReweight);
		
		TIntObjectHashMap<ExpansionTerm> queryTerms = selector.getMostWeightedTermsInHashMap(numberOfTermsToReweight);
		
		ExpansionTerm[] expTerms = new ExpansionTerm[queryTerms.size()];
		int counter = 0;
		for (int i : queryTerms.keys())
			expTerms[counter++] = queryTerms.get(i);
		Arrays.sort(expTerms);
		return expTerms;
	}

	/** For easier sub-classing of which index the query expansion comes from */
	protected Index getIndex(Manager m)
	{
		return m.getIndex();
	}
	
	/** load the expansion terms, as per the property <tt>qe.expansion.terms.class</tt>. Defaults to DFRBagExpansionTerms.
	 * @return an ExpansionTerms instance, which may or may not wrap other ExpansionTerms instances
	 */
	protected ExpansionTerms getExpansionTerms()
	{
		String expanderNames[] = ApplicationSetup.getProperty("qe.expansion.terms.class", "DFRBagExpansionTerms").split("\\s*,\\s*");
		ExpansionTerms rtr = null;
		
		//foreach name, starting from the last, finishing with the first
		
		for(int i=expanderNames.length -1;i>=0;i--)
		{
			String expanderName = expanderNames[i];
			ExpansionTerms next = null;
			if (! expanderName.contains("."))
				expanderName = "org.terrier.querying."+expanderName;
			else if (expanderName.startsWith("uk.ac.gla.terrier"))
				expanderName = expanderName.replaceAll("uk.ac.gla.terrier", "org.terrier");
			
			try{
				Class<? extends ExpansionTerms> clz = Class.forName(expanderName).asSubclass(ExpansionTerms.class);
				if (expanderNames.length -1 == i)
				{
					next = clz
						.getConstructor(CollectionStatistics.class, Lexicon.class, PostingIndex.class, DocumentIndex.class)
						.newInstance(collStats,lexicon, directIndex, documentIndex);
				}
				else
				{
					next = clz.getConstructor(ExpansionTerms.class).newInstance(rtr);
				}
				rtr = next;
			}
			catch (Exception e) {
				logger.error(e);
				return null;
			}
		}
		return rtr;
	}


	/** load the feedback selector, based on the property <tt>qe.feedback.selector</tt>  */
	protected FeedbackSelector getFeedbackSelector(Request rq)
	{
		String[] names = ApplicationSetup.getProperty("qe.feedback.selector", "PseudoRelevanceFeedbackSelector").split("\\s*,\\s*");
		FeedbackSelector rtr = null;
		for(int i=names.length -1;i>=0;i--)
		{
			String name = names[i];
			if (! name.contains("."))
				name = "org.terrier.querying."+name;
			else if (name.startsWith("uk.ac.gla.terrier"))
				name = name.replaceAll("uk.ac.gla.terrier", "org.terrier");
			
			FeedbackSelector next = null;
			try{
				Class<? extends FeedbackSelector> nextClass = Class.forName(name).asSubclass(FeedbackSelector.class);
				if (names.length -1 == i)
				{
					next = nextClass.newInstance();
				}
				else
				{
					next = nextClass.getConstructor(FeedbackSelector.class).newInstance(rtr);
				}
		
				rtr = next;
			} catch (Exception e) { 
				logger.error("Problem loading a FeedbackSelector called "+ name, e);
				return null;
			}
			rtr.setIndex(lastIndex);//TODO index should come from Request
		}
		return rtr;	
	}
	
	protected Index lastIndex = null; //TODO remove

	/**
	 * Runs the actual query expansion
	 * @see org.terrier.querying.PostProcess#process(org.terrier.querying.Manager,org.terrier.querying.SearchRequest)
	 */
	public void process(Manager manager, SearchRequest q) {
	   	Index index = getIndex(manager);
		lastIndex = index;
		documentIndex = index.getDocumentIndex();
		invertedIndex = index.getInvertedIndex();
		lexicon = index.getLexicon();
		collStats = index.getCollectionStatistics(); 
		directIndex = index.getDirectIndex();
		metaIndex = index.getMetaIndex();
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
		setQueryExpansionModel(QueryExpansionModel.getModel(qeModel));
		if(logger.isInfoEnabled()){
			logger.info("query expansion model: " + QEModel.getInfo());
		}
		MatchingQueryTerms queryTerms = ((Request)q).getMatchingQueryTerms();
		if (queryTerms == null)
		{
			logger.warn("No query terms for this query. Skipping QE");
			return;
		}
		// get the expanded query terms
		try{
			expandQuery(queryTerms, (Request)q);
		} catch (IOException ioe) {
			logger.error("IOException while expanding query, skipping QE", ioe);
			return;
		}
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
				newQuery.append(Rounding.toString(queryTerms.getTermWeight(newQueryTerms[i]), 9));
				newQuery.append(' ');
			}
			catch(NullPointerException npe){
				logger.fatal("Nullpointer exception occured in Query Expansion dumping of new Query", npe);
			}
		}
		
		logger.info("NEWQUERY "+q.getQueryID() +" "+newQuery.toString());
		final boolean no2ndPass = Boolean.parseBoolean(ApplicationSetup.getProperty("qe.no.2nd.matching", "false"));
		if (no2ndPass)
		{
			return;
		}
		
		//run retrieval process again for the expanded query
		logger.info("Accessing inverted file for expanded query " + q.getQueryID());
		manager.runMatching(q);
		
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

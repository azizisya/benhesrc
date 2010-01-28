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
 * The Original Code is FieldQueryExpansion.java.
 *
 * The Original Code is Copyright (C) 2004-2007 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.querying;
import gnu.trove.THashSet;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntHashSet;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectIntHashMap;

import java.util.Hashtable;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.matching.MatchingQueryTerms;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.matching.models.queryexpansion.QueryExpansionModel;
import uk.ac.gla.terrier.querying.parser.SingleTermQuery;
import uk.ac.gla.terrier.structures.DirectIndex;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.ExpansionTerms;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.InvertedIndex;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.structures.CollectionStatistics;
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
 * @author Ben He
 */
public class FieldQueryExpansion extends QueryExpansion {
	protected static Logger logger = Logger.getRootLogger();

	/** The document index used for retrieval. */
	protected DocumentIndex[] documentIndex;
	/** The inverted index used for retrieval. */
	protected InvertedIndex[] invertedIndex;
	/** An instance of Lexicon class. */
	protected Lexicon[] lexicon;
	/** The direct index used for retrieval. */
	protected DirectIndex[] directIndex;
	
	protected Lexicon globalLexicon;
	
	protected int numberOfFields;
	
	protected double[] weightValues;
	
	protected int numberOfDocuments;
	
	protected long numberOfTokens;
	
	protected double averageDocumentLength;
	
	/**
	* The default constructor of QueryExpansion.
	*/
	public FieldQueryExpansion() {
		super();
	}
	
	protected int getDocumentLength(int docid){
		int docLength = 0;
		for (int i=0; i<numberOfFields; i++){
			int fieldLength =  documentIndex[i].getDocumentLength(docid);
			if (fieldLength > 0)
				docLength += fieldLength; 
		}
		return docLength;
	}
	
	protected void getTerms(int docid,
			TIntHashSet termidSet, 
			TIntDoubleHashMap termidFrequencyMap){
		THashSet<String> termSet = new THashSet<String>();
		TObjectIntHashMap<String> termToIdMap = new TObjectIntHashMap<String>();
		for (int i=0; i<numberOfFields; i++){
			int[][] terms = directIndex[i].getTerms(docid);
			if (terms == null)
				continue;
			int termCount = terms[0].length;
			for (int j = 0; j < termCount; j++){
				int localTermid = terms[0][j];
				int frequency = terms[1][j];
				lexicon[i].findTerm(localTermid);
				String term = lexicon[i].getTerm();
				int termid = -1;
				if (!termSet.contains(term)){
					globalLexicon.findTerm(term);
					termid = globalLexicon.getTermId();
					termToIdMap.put(term, globalLexicon.getTermId());
					termSet.add(term);
					termidSet.add(termid);
				}else
					termid = termToIdMap.get(term);
				termidFrequencyMap.adjustOrPutValue(termid, weightValues[i]*terms[1][j], weightValues[i]*terms[1][j]);
			}
		}
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
		
		if (logger.isDebugEnabled()){
			for (int i=0; i<this.numberOfFields; i++){
				logger.debug("Field qeweight "+(i+1)+": "+weightValues[i]);
			}
		}
		
		// if the number of retrieved documents is lower than the parameter
		// EXPANSION_DOCUMENTS, reduce the number of documents for expansion
		// to the number of retrieved documents.
		int effDocuments = Math.min(docIDs.length, ApplicationSetup.EXPANSION_DOCUMENTS);
		for (int i = 0; i < effDocuments; i++){
			totalDocumentLength +=this.getDocumentLength(docIDs[i]);
		}
		ExpansionTerms expansionTerms = new ExpansionTerms(
				numberOfDocuments, 
				numberOfTokens, 
				averageDocumentLength, 
				totalDocumentLength, 
				globalLexicon);
		for (int i = 0; i < effDocuments; i++) {
			// GLOBAL termids of terms in the pseudo relevance set
			TIntHashSet termidSet = new TIntHashSet();
			TIntDoubleHashMap termidFrequencyMap = new TIntDoubleHashMap();
			if (logger.isDebugEnabled())
				logger.debug("docno: "+documentIndex[0].getDocumentNumber(docIDs[i]));
			
			this.getTerms(docIDs[i], termidSet, termidFrequencyMap);
			int[] termids = termidSet.toArray();
			int termCount = termids.length;
			for (int j=0; j<termCount; j++){
				expansionTerms.insertTerm(termids[j], termidFrequencyMap.get(termids[j]));
				//System.out.println("termid "+termids[j]+", tf "+termidFrequencyMap.get(termids[j]));
			}
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



	/**
	 * Runs the actual query expansion
	 * @see uk.ac.gla.terrier.querying.PostProcess#process(uk.ac.gla.terrier.querying.Manager,uk.ac.gla.terrier.querying.SearchRequest)
	 */
	public void process(Manager _manager, SearchRequest q) {
		if(logger.isInfoEnabled()){
			logger.info("Starting query expansion post-processing.");
		}
		FieldManager manager = (FieldManager)_manager;
	   	Index[] index = manager.fieldIndices;
	   	this.numberOfFields = manager.NumFields;
	   	// initialise index structures
		documentIndex = new DocumentIndex[numberOfFields];
		invertedIndex = new InvertedIndex[numberOfFields];
		lexicon = new Lexicon[numberOfFields]; 
		directIndex = new DirectIndex[numberOfFields];
		weightValues = manager.qeweightValues;
		CollectionStatistics[] collStats = new CollectionStatistics[numberOfFields];
		for (int i=0; i<numberOfFields; i++){
			documentIndex[i] = index[i].getDocumentIndex();
			invertedIndex[i] = index[i].getInvertedIndex();
			lexicon[i] = index[i].getLexicon();
			directIndex[i] = index[i].getDirectIndex();
			collStats[i]=index[i].getCollectionStatistics();
		}
		globalLexicon = new Lexicon(ApplicationSetup.getProperty("global.qelexicon.filename", 
				ApplicationSetup.getProperty("global.lexicon.filename", "")));
		// get collection statistics
		this.numberOfDocuments = 0;
		this.numberOfTokens = 0L;
		for (int i=0; i<numberOfFields; i++){
			numberOfDocuments = Math.max(numberOfDocuments, collStats[i].getNumberOfDocuments());
			numberOfTokens += collStats[i].getNumberOfTokens();
		}
		this.averageDocumentLength = (double)numberOfTokens/numberOfDocuments;
		
		
		//get the query expansion model to use
		String qeModel = q.getControl("qemodel");
		if (qeModel == null || qeModel.length() ==0)
		{
			logger.warn("WARNING: qemodel control not set for QueryExpansion"+
					" post process. Using default model Bo1");
			qeModel = "Bo1";
		}
		setQueryExpansionModel(getQueryExpansionModel(qeModel));
		if(logger.isInfoEnabled()){
			logger.info("query expansion model: " + QEModel.getInfo());
		}
		MatchingQueryTerms queryTerms = ((Request)q).getMatchingQueryTerms();
		ResultSet resultSet = q.getResultSet();
		// get the expanded query terms
		expandQuery(queryTerms, resultSet);
		if(logger.isInfoEnabled()){
			logger.info("query length after expansion: " + queryTerms.length());
			logger.info("Expanded query: ");
		}
		String[] newQueryTerms = queryTerms.getTerms();
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
				logger.fatal("Fatal Nullpointer exception occured", npe);
				logger.fatal("Exiting");
				System.exit(1);
			}
		}
		if(logger.isInfoEnabled()){
			logger.info("NEWQUERY "+q.getQueryID() +" "+newQuery.toString());
//			 run retrieval process again for the expanded query
			logger.info("Accessing inverted file for expanded query " + q.getQueryID());
		}
		
		
		manager.runMatching(q);
	}
}

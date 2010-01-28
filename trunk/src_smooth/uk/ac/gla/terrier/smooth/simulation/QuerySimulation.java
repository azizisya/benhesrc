/*
 * Smooth - Smoothing term frequency normalisation
 * Webpage: http://ir.dcs.gla.ac.uk/smooth
 * Contact: ben{a.}dcs.gla.ac.uk
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
 * The Original Code is QuerySimulation.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk>
 */

package uk.ac.gla.terrier.smooth.simulation;

import java.io.IOException;
import java.util.HashSet;

import uk.ac.gla.terrier.smooth.matching.BufferedMatching;
import uk.ac.gla.terrier.smooth.structures.BasicQuery;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.matching.models.WeightingModel;
import uk.ac.gla.terrier.matching.models.queryexpansion.QueryExpansionModel;
import uk.ac.gla.terrier.querying.parser.SingleTermQuery;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.DirectIndex;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.ExpansionTerms;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.InvertedIndex;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.smooth.structures.trees.TermTreeNode;
import uk.ac.gla.terrier.utility.ApplicationSetup;

/**
 * This class provides functionalities of query simulation.
 * @author Ben He <ben{a.}dcs.gla.ac.uk>
 * @version $Revision: 1.3 $
 */
public class QuerySimulation {
	/** The weighting model used for the simulation. */
	protected WeightingModel wmodel;
	/** The query expansion model used for the simulation. */
	protected QueryExpansionModel qemodel;
	/** The document index used for simulation. */
	protected DocumentIndex docIndex;
	/** The inverted index used for simulation. */
	protected InvertedIndex invIndex;
	/** The lexicon used for simulation. */
	protected Lexicon lexicon;
	/** The direct index used for simulation. */
	protected DirectIndex directIndex;
	/** The index of terms ranked by term frequency in the collection. */
	protected TFRanking tfRanking;
	/** The matching class. */
	protected BufferedMatching matching;
	/** The name of the default weight model of Smooth. */
	protected final String DEFAULT_WEIGHTING_MODEL =
		ApplicationSetup.getProperty("smooth.weighting.model.default", "PL2");
	/** The name of the default query expansion model of Smooth. */
	protected final String DEFAULT_EXPANSION_MODEL =
		ApplicationSetup.getProperty("smooth.expansion.model.default", "Bo1");
	
	protected CollectionStatistics collSta;
	/** 
	 * The default constructor.
	 * @param index The index used for simulation.
	 */
	public QuerySimulation(Index index){
		this.collSta = index.getCollectionStatistics();
		docIndex = index.getDocumentIndex();
		invIndex = index.getInvertedIndex();
		lexicon = index.getLexicon();
		directIndex = index.getDirectIndex();
		this.setModels(this.DEFAULT_WEIGHTING_MODEL, 
				this.DEFAULT_EXPANSION_MODEL, 7d);
		this.tfRanking = new TFRanking(lexicon, collSta);
		matching = new BufferedMatching(Index.createIndex());
		matching.setModel(this.wmodel);
	}
	/**
	 * Simulate a query using the 1-step simulation method.
	 * The length of the simulated query is a random integer 
	 * within [minLength, maxLength].
	 * @param minLength The minimal length of a simulated query.
	 * @param maxLength The maximal length of a simulated query.
	 * @return The simulated query.
	 */
	public BasicQuery oneStepSimulation(int minLength, int maxLength){
		int queryLength = minLength + (int)(Math.random()*(maxLength-minLength));
		return oneStepSimulation(queryLength);
	}
	/**
	 * Simulate a query using the 1-step simulation method with 
	 * the specified query length.
	 * @param queryLength The query length.
	 * @return The simulated query.
	 */
	public BasicQuery oneStepSimulation(int queryLength){
		TermTreeNode[] terms = this.extractInformativeTerms(queryLength);
		return new BasicQuery(terms, this.getRandomQueryId());
	}
	/**
	 * Simulated a query using the 2-step simulated method.
	 * The length of the simulated query is a random integer 
	 * within [minLength, maxLength].
	 * @param minLength The minimal length of a simulated query.
	 * @param maxLength The maximal length of a simulated query.
	 * @return The simulated query.
	 */
	public BasicQuery twoStepSimulation(int minLength, int maxLength){
		int queryLength = minLength + (int)(Math.random()*(maxLength-minLength+1));
		return twoStepSimulation(queryLength);
	}
	/**
	 * Simulate a query with the speficied length using the 2-step 
	 * simulated method.
	 * @param queryLength The query length.
	 * @return The simulated query.
	 */
	public BasicQuery twoStepSimulation(int queryLength){
		// extract terms using a random seed-term and then obtain the new seed-term
		String newSeedTerm = this.extractInformativeTerms(1)[0].term;
		// using the new seed-term to extract other composing terms of the
		// simulated query
		TermTreeNode[] extractedTerms = this.extractInformativeTerms(newSeedTerm,
				queryLength - 1);
		// the simulated query consists of the new seed-term and the extracted terms
		// in the second-time extraction
		TermTreeNode[] queryTerms = new TermTreeNode[queryLength];
		queryTerms[0] = new TermTreeNode(newSeedTerm);
		for (int i = 1; i < queryLength; i++){
			queryTerms[i] = new TermTreeNode(extractedTerms[i-1].term);
		}
		return new BasicQuery(queryTerms, this.getRandomQueryId());
	}
	/**
	 * Set the weighting model and query model used in the 2-step simulation.
	 * @param wModelName The name of the weighting model.
	 * @param qeModelName The name of the query expansion model.
	 * @param parameter The parameter setting of the weighting model.
	 */
	public void setModels(String wModelName, String qeModelName, double parameter){
		if (wModelName.lastIndexOf('.') < 0)
			wModelName = "uk.ac.gla.terrier.matching.models.".concat(wModelName);
		if (qeModelName.lastIndexOf('.') < 0)
			qeModelName = "uk.ac.gla.terrier.matching.models.queryexpansion.".concat(qeModelName);
		try{
			this.wmodel = (WeightingModel)Class.forName(wModelName).newInstance();
			this.wmodel.setParameter(parameter);
		}
		catch(Exception e){
			e.printStackTrace();
			System.err.println("Error occurs while creating the weighting model.");
			System.exit(1);
		}
		try{
			this.qemodel = (QueryExpansionModel)Class.forName(qeModelName).newInstance();
		}
		catch(Exception e){
			e.printStackTrace();
			System.err.println("Error occurs while creating the query expansion model.");
			System.exit(1);
		}
	}
	/**
	 * Extract the speficied number of the most informative terms from 
	 * the top-ranked documents with respect to a selected initial query 
	 * term from the lexicon.
	 * @param numberOfExtractedTerms The number of extracted terms.
	 * @return The extracted terms.
	 */
	public TermTreeNode[] extractInformativeTerms(int numberOfExtractedTerms){
		int numberOfTopDocs = ApplicationSetup.EXPANSION_DOCUMENTS;
		String queryid = "extraction";
		int documentFrequency = 0;
		int initialSeedTermid = 0;
		//System.out.println("start extracting...");
		while (documentFrequency < numberOfTopDocs){
			//System.out.println("selecting a random term...");
			initialSeedTermid = this.tfRanking.getValidRandomTermId();
			//System.out.println("initial seed termid: " + initialSeedTermid);
			lexicon.findTerm(initialSeedTermid);
			documentFrequency = lexicon.getNt();
		}
		String seedTerm = lexicon.getTerm();
		//System.out.println("seed-term: " + seedTerm);
		TermTreeNode[] singleTerm = {new TermTreeNode(seedTerm)};
		BasicQuery query = new BasicQuery(singleTerm, queryid);
		//System.out.println("matching for seed-term...");
		matching.basicMatch(query);
		ResultSet resultSet = matching.getResultSet();
		int[] docidsTmp = resultSet.getDocids();
		
		int[] docids = new int[numberOfTopDocs];
		for (int i = 0; i < numberOfTopDocs; i++)
			docids[i] = docidsTmp[i];
		HashSet excludedTerms = new HashSet();
		excludedTerms.add(seedTerm);
		return this.extractInformativeTerms(docids, numberOfExtractedTerms, excludedTerms);
	}
	/**
	 * Extract the most informative terms from top-returned documents ranked 
	 * according to a given term.
	 * @param seedTerm The given term according to which the documents are ranked.
	 * @param numberOfExtractedTerms The number of extracted terms.
	 * @return The extracted terms.
	 */
	public TermTreeNode[] extractInformativeTerms(String seedTerm, int numberOfExtractedTerms){
		String queryid = this.getRandomQueryId();
		TermTreeNode[] singleTerm = {new TermTreeNode(seedTerm)};
		BasicQuery query = new BasicQuery(singleTerm, queryid);
		matching.safeBasicMatch(query);
		ResultSet resultSet = matching.getResultSet();
		int[] docidsTmp = resultSet.getDocids();
		int numberOfTopDocs = Math.min(ApplicationSetup.EXPANSION_DOCUMENTS,
				resultSet.getExactResultSize());
		int []docids = new int[numberOfTopDocs];
		for (int i = 0; i < numberOfTopDocs; i++)
			docids[i] = docidsTmp[i];
		HashSet excludedTerms = new HashSet();
		excludedTerms.add(seedTerm);
		return this.extractInformativeTerms(docids, numberOfExtractedTerms
				,excludedTerms);
	}
	/**
	 * Extract the most informative terms, excluding a set of given terms,
	 * from the given set of documents.
	 * @param docids The ids of the given set of documents.
	 * @param numberOfExtractedTerms The number of extracted terms.
	 * @param excludedTerms The terms that are excluded from the extracted terms.
	 * @return The extracted terms.
	 */
	public TermTreeNode[] extractInformativeTerms(int[] docids
			, int numberOfExtractedTerms, HashSet excludedTerms){
		double totalDocumentLength = 0;
		int effDocuments = docids.length; 
		for (int i = 0; i < effDocuments; i++)
			totalDocumentLength += docIndex.getDocumentLength(docids[i]);
		ExpansionTerms expansionTerms =
			new ExpansionTerms(collSta, totalDocumentLength, lexicon);
		
		for (int i = 0; i < effDocuments; i++) {
			int[][] terms = directIndex.getTerms(docids[i]);
			for (int j = 0; j < terms[0].length; j++)
				expansionTerms.insertTerm(terms[0][j], (double)terms[1][j]);
		}
		SingleTermQuery[] expandedTerms =
			expansionTerms.getExpandedTerms(numberOfExtractedTerms+excludedTerms.size(), qemodel);
		TermTreeNode[] terms = new TermTreeNode[numberOfExtractedTerms];
		int counter = 0;
		for (int i = 0; i < expandedTerms.length; i++){
			if (excludedTerms.contains(expandedTerms[i].getTerm()))
				continue;
			else{
				terms[counter] = new TermTreeNode(expandedTerms[i].getTerm());
				terms[counter].normalisedFrequency = expandedTerms[i].getWeight();
				counter++;
				if (counter == numberOfExtractedTerms)
					break;
			}
		}
		return terms;
	}
	
	/**
	 * Get a queryid that is randomly chosen and unduplicatable.
	 * @return The query id.
	 */
	private String getRandomQueryId(){
		return new String(""+System.currentTimeMillis());
	}
	
}

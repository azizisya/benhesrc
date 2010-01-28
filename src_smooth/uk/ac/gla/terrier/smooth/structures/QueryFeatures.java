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
 * The Original Code is QueryFeatures.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.smooth.structures;

import java.io.IOException;

import uk.ac.gla.terrier.smooth.matching.BufferedMatching;
import uk.ac.gla.terrier.matching.models.Idf;
import uk.ac.gla.terrier.matching.models.WeightingModel;
import uk.ac.gla.terrier.matching.models.queryexpansion.QueryExpansionModel;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.ExpansionTerms;
import uk.ac.gla.terrier.smooth.structures.BasicQuery;
import uk.ac.gla.terrier.smooth.structures.trees.TermTreeNode;
import uk.ac.gla.terrier.statistics.Statistics;
import uk.ac.gla.terrier.utility.ApplicationSetup;
/**
 * This class implements a data structure for a set of 
 * query features that can be applied for query performance
 * prediction.
 * @author Ben He <ben{a.}dcs.gla.ac.uk>
 * @version $Revision: 1.3 $
 */

public class QueryFeatures {

    /** The number of documents containing (at least one of) the query terms. */
    public int Nq;
    /** The id of the query. It is an automatically generated unduplicable 
     * string by default. */
    protected String queryId = new String("" + System.currentTimeMillis());
    /** The document frequencies of the query terms. */
    protected double[] Nt;
    /** The frequencies of the query terms in the collection.*/
    protected double[] TF;

    /** The query length. */
    public int queryLength;
    /** The query term frequencies of the query terms. */
    public double[] qtf;
     
    /** standard deviation of idf. */
    public double stdIdf; 
    public CollectionStatistics collSta;
    
    /** Average inverse collection term frequency. */
    public double avICTF;
    
    /** Inverse collection term frequency./ */
    public double ICTF;
    /** Query scope.*/
    public double queryScope;
    /** The infoDFR of the query (query-difficulty).*/
    public double infoDFR;
    /** The name of the default weight model of Smooth. */
    protected final String DEFAULT_WEIGHTING_MODEL =
    	"uk.ac.gla.terrier.matching.models.".concat
		(ApplicationSetup.getProperty("smooth.weighting.model.default", "PL2"));
	/** The name of the default query expansion model of Smooth.*/
	protected final String DEFAULT_EXPANSION_MODEL =
		"uk.ac.gla.terrier.matching.models.queryexpansion.".concat
		(ApplicationSetup.getProperty("smooth.expansion.model.default", "KL"));
	/** The default parameter setting of the normalisation 2 in Smooth. */
	protected final double DEFAULT_PARAMETER = 
		Double.parseDouble(ApplicationSetup.getProperty("smooth.default.parameter.normalisation2", "7d"));
    /**
     * The constructor.
     * @param query The query.
     * @param matching The matching class.
     */
    public QueryFeatures(BasicQuery query, BufferedMatching matching){
    	this.initialise(query, matching);
    }
    /**
     * Initialise the query features.
     * @param query The given query.
     * @param matching The matching class used for retrieval.
     */
    protected void initialise(BasicQuery query, BufferedMatching matching){
    	this.qtf = new double[query.getQueryLength()];
    	TermTreeNode[] terms = query.getQueryTerms();
    	for (int i = 0; i < terms.length; i++)
    		qtf[i] = terms[i].normalisedFrequency / query.getQueryLength();
    	matching.matchWithoutScoring(query.getQueryNumber(), query.getQueryTermStrings());
    	this.Nq = matching.getNumberOfRetrievedDocuments();
    	Nt = matching.getNt();
    	TF = matching.getTF();
    	
    	collSta = matching.index.getCollectionStatistics();
    	
    	this.queryId = query.getQueryNumber();
    	QueryExpansionModel qemodel = null;
		WeightingModel wmodel = null;
		try{
			qemodel = (QueryExpansionModel)Class.forName(DEFAULT_EXPANSION_MODEL).newInstance();
			wmodel = (WeightingModel)Class.forName(DEFAULT_WEIGHTING_MODEL).newInstance();
			wmodel.setParameter(this.DEFAULT_PARAMETER);
		}
		catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
    	
    	// compute query features
    	
    	this.queryLength = this.Nt.length;

		// standard deviation of idf 
		double[] idfs = new double[this.Nt.length];
		Idf idf = new Idf();
		for (int i = 0; i < idfs.length; i++){
			idfs[i] = idf.idfNENQUIRY(this.Nt[i]);
		}
		this.stdIdf = Statistics.standardDeviation(idfs);
				
		/** AvICTF */
		ICTF = 0d;
		for (int i = 0; i < TF.length; i++){
			ICTF += Statistics.logx(2, collSta.getNumberOfTokens() / TF[i]);
		}
		this.avICTF = ICTF/queryLength;
		
		// query scope
		this.queryScope = -Statistics.logx(2, 
				(double)Nq/collSta.getNumberOfDocuments());
		
		// InfoDFR
		matching.setModel(wmodel);
		matching.basicMatch(query);
		this.infoDFR = 0d;
		int[] docids = matching.getResultSet().getDocids();
		int effDocuments = Math.min(docids.length, 
				ApplicationSetup.EXPANSION_DOCUMENTS);
		ExpansionTerms expansionTerms = matching.accessDirectIndex(effDocuments);
		
		expansionTerms.assignWeights(qemodel);
		TermTreeNode[] queryTerms = query.getQueryTerms();
		for (int i = 0; i < queryTerms.length; i++){
			if (expansionTerms.getFrequency(queryTerms[i].term) >= 2){
				this.infoDFR += expansionTerms.getExpansionWeight(queryTerms[i].term, qemodel);
			}
		}
    }
    /**
     * Get the query features.
     * @return A double array containing the query features.
     */
    public double[] getFeatureVector(){
    	double[] qf = new double[5];
    	qf[0] = this.stdIdf;
    	qf[1] = this.avICTF;
    	qf[2] = this.queryScope;
    	qf[3] = this.infoDFR;
    	qf[4] = this.queryLength;
    	return qf;
    }
	/**
	 * Speficy the query id.
	 * @param id Query id.
	 */
	public void setQueryId(String id){
		this.queryId = id;
	}
	/**
	 * Get the queryid.
	 * @return Query id.
	 */
	public String getQueryId(){
		return this.queryId;
	}
    
}

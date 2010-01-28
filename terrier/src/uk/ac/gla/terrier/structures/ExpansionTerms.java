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
 * The Original Code is ExpansionTerms.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Gianni Amati <gba{a.}fub.it> (original author)
 *   Ben He <ben{a.}dcs.gla.ac.uk> 
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.structures;
import gnu.trove.THashSet;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntObjectHashMap;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.matching.MatchingQueryTerms;
import uk.ac.gla.terrier.matching.models.queryexpansion.QueryExpansionModel;
import uk.ac.gla.terrier.querying.parser.SingleTermQuery;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Rounding;

/**
 * This class implements a data structure of terms 
 * in the top-retrieved documents.
 * <P><b>Properties</b>:<ul>
 * <li><tt>expansion.mindocuments</tt> - the minimum number of documents a term must exist in 
 * before it can be considered to be informative. Defaults to 2. For more information, see
 * 	Giambattista Amati: Information Theoretic Approach to Information Extraction. FQAS 2006: 519-529 <a href="http://dx.doi.org/10.1007/11766254_44">DOI 10.1007/11766254_44</a></li></ul>
 * @author Gianni Amati, Ben He, Vassilis Plachouras, Craig Macdonald
 * @version $Revision: 1.1 $
 */
public class ExpansionTerms {
	/** The logger used */
	private static Logger logger = Logger.getRootLogger();
	/** The terms in the top-retrieval documents. */
	protected TIntObjectHashMap<ExpansionTerm> terms;
	/** The lexicon used for retrieval. */
	protected Lexicon lexicon;
	/** The number of documents in the collection. */
	protected int numberOfDocuments;
	/** The number of tokens in the collection. */
	protected long numberOfTokens;
	/** The average document length in the collection. */
	protected double averageDocumentLength;
	/** The number of tokens in the X top ranked documents. */
	protected double totalDocumentLength;
	/** The original query terms. Used only for Conservative Query Expansion,
	  * where no terms are added to the query, only the existing ones are 
	  * reweighted. */
	protected THashSet<String> originalTerms = new THashSet<String>();
	/** The ids of the original query terms.*/
	protected TIntHashSet originalTermids = new TIntHashSet();
	/**
	 * The parameter-free term weight normaliser.
	 */
	public double normaliser = 1d;
	
	/** The minimum number of documents a term must occur in to be considered for expanded terms. This is not considered a parameter of query expansion, as the default value of 2 works extremely well. Set using the property <tt>expansion.mindocuments</tt> */
	protected static final int EXPANSION_MIN_DOCUMENTS = Integer.parseInt(ApplicationSetup.getProperty("expansion.mindocuments","2"));
	
	private static final SingleTermQuery[] emptySingleTermQuery_array = new SingleTermQuery[0];
	
	/** 
	 * This class implements a data structure 
	 * for a term in the top-retrieved documents. 
	 */
	public static class ExpansionTerm {
		
		/** The term ID. */
		protected int termID;
		/** The weight for query expansion. */
		protected double weightExpansion;
		/** The number of occurrences of the given term in the X top ranked documents. */
		protected double withinDocumentFrequency;
		
		/** The document frequency of the term in the X top ranked documents. */
		protected int documentFrequency;
		
		/** 
		 * The constructor of ExpansionTerm. Once the term is found in a top-
		 * retrieved documents, we create a record for this term.
		 * @param termID int the ID of the term
		 * @param withinDocumentFrequency double the frequency of the term in 
		 *		a top-retrieved document
		 */
		public ExpansionTerm(int termID, double withinDocumentFrequency){
			this.termID = termID;
			this.withinDocumentFrequency = withinDocumentFrequency;
			this.documentFrequency = 1;
			this.weightExpansion = 0;
		}
			
		/**
		 * Returns the ID of the term. 
		 * @return int the term ID.
		 */
		public int getTermID(){
			return this.termID;
		}
		/** 
		 * If the term is found in another top-retrieved document, we increase
		 * the frequency and the document frequency of the term.
		 * @param withinDocumentFrequency double the frequency of the term
		 *		in the corresponding top-retrieved document.
		 */
		public void insertRecord(double withinDocumentFrequency){
			this.withinDocumentFrequency += withinDocumentFrequency;
			this.documentFrequency++;
		}
		/** 
		 * Sets the expansion weight of the term.
		 * @param weightExpansion double the expansion weight of the term.
		 */
		public void setWeightExpansion(double weightExpansion){
			this.weightExpansion = weightExpansion;
		}
		
		/** 
		 * The method returns the document frequency of term in the top-retrieved
		 * documents.
		 * @return int The document frequency of term in the top-retrieved
		 *		 documents.
		 */
		public int getDocumentFrequency(){
			return this.documentFrequency;
		}
		/** 
		 * The method returns the expansion weight of the term.
		 * @return double The expansion weight of the term.
		 */
		public double getWeightExpansion(){
			return this.weightExpansion;
		}
		
		/** 
		 * The method returns the frequency of the term in the X top-retrieved
		 * documents.
		 * @return double The expansion weight of the term.
		 */
		public double getWithinDocumentFrequency(){
			return this.withinDocumentFrequency;
		}
	}
	
	/**
 	* Constructs an instance of ExpansionTerms.
	* @param totalLength The sum of the length of the top-retrieved documents.
	* @param lexicon Lexicon The lexicon used for retrieval.
 	*/
	public ExpansionTerms(CollectionStatistics collStats, double totalLength, Lexicon lexicon) {
		this(
				collStats.getNumberOfDocuments(),
				collStats.getNumberOfTokens(),
				collStats.getAverageDocumentLength(),
				totalLength,
				lexicon);
	}
	
	/**
 	* Constructs an instance of ExpansionTerms.
	* @param totalLength The sum of the length of the top-retrieved documents.
	* @param lexicon Lexicon The lexicon used for retrieval.
 	*/
	public ExpansionTerms(
			int numberOfDocuments,
			long numberOfTokens,
			double averageDocumentLength,
			double totalLength, 
			Lexicon lexicon) {
		this.numberOfDocuments = numberOfDocuments;
		this.numberOfTokens = numberOfTokens;
		this.averageDocumentLength = averageDocumentLength;
		this.terms = new TIntObjectHashMap<ExpansionTerm>();
		this.totalDocumentLength = totalLength;
		this.lexicon = lexicon;
	}

	/** Allows the totalDocumentLength to be set after the fact */
	public void setTotalDocumentLength(double totalLength)
	{
		 this.totalDocumentLength = totalLength;
	}

	/** Returns the termids of all terms found in the top-ranked documents */
	public int[] getTermIds()
	{
		return terms.keys();
	}

	/** Returns the unique number of terms found in all the top-ranked documents */
	public int getNumberOfUniqueTerms()
	{
		return terms.size();
	}

	/**
 	* This method implements the functionality of assigning expansion weights to
	* the terms in the top-retrieved documents, and returns the most informative
	* terms among them. Conservative Query Expansion (ConservativeQE) is used if
	* the number of expanded terms is set to 0. In this case, no new query terms
	* are added to the query, only the existing ones reweighted.
	* @param numberOfExpandedTerms int The number of terms to extract from the
	*		top-retrieved documents. ConservativeQE is set if this parameter is set to 0.
	* @param QEModel QueryExpansionModel the model used for query expansion
	* @return TermTreeNode[] The expanded terms.
 	*/
	public SingleTermQuery[] getExpandedTerms(int numberOfExpandedTerms, QueryExpansionModel QEModel) {
		// The number of terms to extract from the pseudo relevance set is the
		// minimum between tbe system setting and the number of unique terms in
		// the pseudo relevance set.
		numberOfExpandedTerms = Math.min(this.terms.size(), numberOfExpandedTerms);
		QEModel.setTotalDocumentLength(this.totalDocumentLength);
		QEModel.setCollectionLength(this.numberOfTokens);
		QEModel.setAverageDocumentLength(this.averageDocumentLength);
		QEModel.setNumberOfDocuments(this.numberOfDocuments);
		//System.out.println("totalDocumentLength: "+totalDocumentLength);

		final boolean ConservativeQE = (numberOfExpandedTerms == 0);
		
		// weight the terms
		int posMaxWeight = 0;
		Object[] arr = terms.getValues();
		ExpansionTerm[] allTerms = new ExpansionTerm[arr.length];
		final int len = allTerms.length;
		for(int i=0;i<len;i++)
			allTerms[i] = (ExpansionTerm)arr[i];

		for (int i=0; i<len; i++){
			try{
				//only consider terms which occur in 2 or more documents. Alter using the expansion.mindocuments property.
				if (allTerms[i].getDocumentFrequency() < EXPANSION_MIN_DOCUMENTS &&
						!originalTermids.contains(allTerms[i].getTermID())){
					allTerms[i].setWeightExpansion(0);
					continue;
				}
				
				double TF = 0;
				double Nt = 0;
				lexicon.findTerm(allTerms[i].getTermID());
				TF = lexicon.getTF();
				Nt = lexicon.getNt();
				allTerms[i].setWeightExpansion(QEModel.score(
					allTerms[i].getWithinDocumentFrequency(),
					TF
					)
				);
				if (allTerms[i].getWeightExpansion() > allTerms[posMaxWeight].getWeightExpansion())
					posMaxWeight = i;
				
			} catch(NullPointerException npe) {
				//TODO print something more explanatory here
				logger.fatal("A nullpointer exception occured while iterating over expansion terms at iteration number: "+"i = " + i,npe);
			}
		}
		
		// sort the terms by weight
		
		normaliser = allTerms[posMaxWeight].getWeightExpansion();
		if (QEModel.PARAMETER_FREE){
			QEModel.setMaxTermFrequency(allTerms[posMaxWeight].getWithinDocumentFrequency());
			normaliser = QEModel.parameterFreeNormaliser();
			if(logger.isInfoEnabled()){
				logger.info("parameter free query expansion.");
			}
		}
		lexicon.findTerm(allTerms[posMaxWeight].termID);
		if(logger.isDebugEnabled()){
		logger.debug("term with the maximum weight: " + lexicon.getTerm() +
				", normaliser: " + Rounding.toString(normaliser, 4));
		}
		THashSet<SingleTermQuery> expandedTerms = new THashSet<SingleTermQuery>();
		if (!ConservativeQE){
			
			for (int i = 0; i < numberOfExpandedTerms; i++){
				int position = i;
				for (int j = i; j < len; j++){
					if (allTerms[j].getWeightExpansion() > allTerms[position].getWeightExpansion())
						position = j;
				}
				if (position != i){
					ExpansionTerm temp = allTerms[position];
					allTerms[position] = allTerms[i];
					allTerms[i] = temp;
				}
				
				lexicon.findTerm(allTerms[i].getTermID());
				final SingleTermQuery expandedTerm = new SingleTermQuery(lexicon.getTerm());//new TermTreeNode(lexicon.getTerm());
				
				expandedTerm.setWeight(allTerms[i].getWeightExpansion()/normaliser);
				
				//expandedTerms[i].normalisedFrequency = 
				//terms[i].getWeightExpansion()/normaliser;
				if (!QEModel.PARAMETER_FREE)
					expandedTerm.setWeight(expandedTerm.getWeight()*QEModel.ROCCHIO_BETA);
					//normalisedFrequency *= QEModel.ROCCHIO_BETA;	
				expandedTerms.add(expandedTerm);
			}
		}
		else{
			int allTermsCount = allTerms.length;
			int weighedOriginalTermsCount=0;
			for (int i = 0; i < allTermsCount; i++){
				if (weighedOriginalTermsCount==originalTerms.size())
					break;
				
				lexicon.findTerm(allTerms[i].getTermID());
				if (!originalTerms.contains(lexicon.getTerm()))
					continue;
				weighedOriginalTermsCount++;
				final SingleTermQuery expandedTerm = new SingleTermQuery(lexicon.getTerm());//new TermTreeNode(lexicon.getTerm());
				expandedTerm.setWeight(allTerms[i].getWeightExpansion()/normaliser);
				//expandedTerms[i].normalisedFrequency = 
				//terms[i].getWeightExpansion()/normaliser;
				if (!QEModel.PARAMETER_FREE)
					expandedTerm.setWeight(expandedTerm.getWeight()*QEModel.ROCCHIO_BETA);
					//normalisedFrequency *= QEModel.ROCCHIO_BETA;		
				expandedTerms.add(expandedTerm);
			}		
		}
		return (SingleTermQuery[])expandedTerms.toArray(emptySingleTermQuery_array);
	}

	/**
	 * Set the original query terms.
	 * @param query The original query.
	 */
	public void setOriginalQueryTerms(MatchingQueryTerms query){
		String[] terms = query.getTerms();
		this.originalTermids.clear();
		this.originalTerms.clear();
		for (int i=0; i<terms.length; i++){
			this.originalTerms.add(terms[i]);
			this.originalTermids.add(query.getTermCode(terms[i]));
		}
	}

	/** Remove the records for a given term */
	public void deleteTerm(int termid)
	{
		terms.remove(termid);
	}

	/**
	 * Returns the weight of a given term, computed by the 
	 * specified query expansion model.
	 * @param term String the term to set the weight for.
	 * @param model QueryExpansionModel the used query expansion model.
	 * @return double the weight of the specified term.
	 */
	public double getExpansionWeight(String term, QueryExpansionModel model){
		lexicon.findTerm(term);
		return this.getExpansionWeight(lexicon.termId, model);
	}
	
	/**
	 * Returns the weight of a given term.
	 * @param term String the term to get the weight for.
	 * @return double the weight of the specified term.
	 */
	public double getExpansionWeight(String term){
		lexicon.findTerm(term);
		return this.getExpansionWeight(lexicon.termId);
	}
	/**
	 * Returns the un-normalised weight of a given term.
	 * @param term String the given term.
	 * @return The un-normalised term weight.
	 */
	public double getOriginalExpansionWeight(String term){
		return getExpansionWeight(term)*normaliser;
	}
	
	/**
	 * Returns the frequency of a given term in the top-ranked documents.
	 * @param term String the term to get the frequency for.
	 * @return double the frequency of the specified term in the top-ranked documents.
	 */
	public double getFrequency(String term){
		lexicon.findTerm(term);
		return this.getFrequency(lexicon.getTermId());
	}
	
	/**
	 * Returns the frequency of a given term in the top-ranked documents.
	 * @param termId int the id of the term to get the frequency for.
	 * @return double the frequency of the specified term in the top-ranked documents.
	 */
	public double getFrequency(int termId){
		Object o = terms.get(termId);
		if (o == null)
			return 0;
		return ((ExpansionTerm)o).getWithinDocumentFrequency();
	}

	/**
     * Returns the number of the top-ranked documents a given term occurs in.
     * @param termId int the id of the term to get the frequency for.
     * @return double the document frequency of the specified term in the top-ranked documents.
     */
    public double getDocumentFrequency(int termId){
        Object o = terms.get(termId);
        if (o == null)
            return 0;
        return ((ExpansionTerm)o).getDocumentFrequency();
    }
	
	/**
	 * Assign weight to terms that are stored in ExpansionTerm[] terms.
	 * @param QEModel QueryExpansionModel the used query expansion model.
	 */
	public void assignWeights(QueryExpansionModel QEModel){
		// Set required statistics to the query expansion model
		QEModel.setTotalDocumentLength(this.totalDocumentLength);
		QEModel.setCollectionLength(this.numberOfTokens);
		QEModel.setAverageDocumentLength(this.averageDocumentLength);
		QEModel.setNumberOfDocuments(this.numberOfDocuments);
		
		// weight the terms
		int posMaxWeight = 0;
		
		Object[] arr = terms.getValues();
		ExpansionTerm[] allTerms = new ExpansionTerm[arr.length];
		final int len = allTerms.length;
		for(int i=0;i<len;i++)
			allTerms[i] = (ExpansionTerm)arr[i];

		for (int i=0; i<len; i++){
			try{
				if (allTerms[i].getDocumentFrequency() <= EXPANSION_MIN_DOCUMENTS){
					allTerms[i].setWeightExpansion(0);
					continue;
				}
				
				double TF = 0;
				double Nt = 0;
				lexicon.findTerm(allTerms[i].getTermID());
				TF = lexicon.getTF();
				Nt = lexicon.getNt();
				allTerms[i].setWeightExpansion(QEModel.score(
					allTerms[i].getWithinDocumentFrequency(),
					TF
					)
				);
				if (allTerms[i].getWeightExpansion() > allTerms[posMaxWeight].getWeightExpansion())
					posMaxWeight = i;
				
			} catch(NullPointerException npe) {
				//TODO print something more explanatory here
				logger.fatal("A nullpointer exception occured while assigning weights on expansion terms at iteration: "+"i = " + i,npe);
			}
		}
		
		// sort the terms by weight
		normaliser = allTerms[posMaxWeight].getWeightExpansion();
		if (QEModel.PARAMETER_FREE){
			QEModel.setMaxTermFrequency(allTerms[posMaxWeight].getWithinDocumentFrequency());
			normaliser = QEModel.parameterFreeNormaliser();
			if(logger.isInfoEnabled()){
				logger.info("parameter free query expansion.");
			}
		}
		lexicon.findTerm(allTerms[posMaxWeight].termID);
		if(logger.isDebugEnabled()){
			logger.debug("term with the maximum weight: " + lexicon.getTerm() +
				", normaliser: " + Rounding.toString(normaliser, 4));
		}
		for (int i = 0; i < len; i++){
			allTerms[i].setWeightExpansion(allTerms[i].getWeightExpansion()/normaliser);
			//expandedTerms[i].normalisedFrequency = 
			//terms[i].getWeightExpansion()/normaliser;
			if (!QEModel.PARAMETER_FREE)
				allTerms[i].setWeightExpansion(allTerms[i].getWeightExpansion()*QEModel.ROCCHIO_BETA);
				//normalisedFrequency *= QEModel.ROCCHIO_BETA;		   
		}
	}
	
	/**
	 * Returns the weight of a term with the given
	 * term identifier, computed by the specified 
	 * query expansion model.
	 * @param termId int the term identifier to set the weight for.
	 * @param model QueryExpansionModel the used query expansion model.
	 * @return double the weight of the specified term.
	 */
	public double getExpansionWeight(int termId, QueryExpansionModel model){
		double score = 0;
		Object o = terms.get(termId);
		if (o != null)
		{
			double TF = 0;
			double Nt = 0;
			lexicon.findTerm(termId);
			TF = lexicon.getTF();
			Nt = lexicon.getNt();
			score = model.score(((ExpansionTerm)o).getWithinDocumentFrequency(),
					TF,
					this.totalDocumentLength,
					this.numberOfTokens,
					this.averageDocumentLength
					);
		}
		return score;
	}
	
	/**
	 * Returns the weight of a term with the given
	 * term identifier.
	 * @param termId int the term identifier to set the weight for.
	 * @return double the weight of the specified term.
	 */
	public double getExpansionWeight(int termId){
		Object o = terms.get(termId);
		if (o == null)
			return -1;
		return ((ExpansionTerm)o).getWeightExpansion();
	}

	/** Returns the probability of a given termid occurring
	  * in the expansion documents. Returns the quotient
	  * document frequency in the expansion documents, divided
	  * by the total length of all the expansion documents.
	  * @param termId int the term identifier to obtain the probability
	  * @return double the probability of the term */
	public double getExpansionProbability(int termId) {
		Object o = terms.get(termId);
		if (o == null)
			return -1;
		return ((ExpansionTerm)o).getDocumentFrequency() / totalDocumentLength;
	}
	
	/**
 	* Add a term in the X top-retrieved documents as a candidate of the 
	* expanded terms.
 	* @param termID int the integer identifier of a term
 	* @param withinDocumentFrequency double the within document 
 	*		frequency of a term
 	*/
	public void insertTerm(int termID, double withinDocumentFrequency) {
		final ExpansionTerm et = terms.get(termID);
		if (et == null)
			terms.put(termID, new ExpansionTerm(termID, withinDocumentFrequency));
		else
			et.insertRecord(withinDocumentFrequency);
	}
}

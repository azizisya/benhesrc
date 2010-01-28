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
 * The Original Code is DistributedExpansionTerms.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk> 
 */
package uk.ac.gla.terrier.distr.structures;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TIntHashSet;

import java.io.Serializable;

import uk.ac.gla.terrier.matching.models.queryexpansion.QueryExpansionModel;
import uk.ac.gla.terrier.querying.parser.SingleTermQuery;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Rounding;
/**
 * This class implements a data structure of terms 
 * in the top-retrieved documents.
 * @author Ben He(ben@dcs.gla.ac.uk)
 * @version $Revision: 1.9 $
 */
public class DistributedExpansionTerms implements Serializable{
    /** The terms in the top-retrieval documents. */
	public THashMap mapTermProperties;
    
	public THashSet terms;
	/** The number of tokens in the X top ranked documents. */
	public double totalDocumentLength;
	/** The number of tokens in the collection. */
	protected double numberOfTokens;
	
	protected long numberOfDocuments;
	
	private static final long serialVersionUID = 200603101233L;
	/**
	 * The average document length in the collection.
	 */
	protected double avl;

	/**
	 * A hashset containing the strings of the original query terms.
	 */
    protected THashSet originalQueryTermStrings;
	/**
	 * A version id for the serialisation.
	 */
	//private static final long serialVersionUID = (long)(System.currentTimeMillis() * Math.random());
	
    /** 
     * This class implements a data structure 
     * for a term in the top-retrieved documents. 
     */
	public class DistributedExpansionTerm implements Serializable{
		/** The docnos of the top-ranked documents containing the term. */
		protected THashSet docnos; 
		
		/** The weight for query expansion. */
		protected double weightExpansion;
		/** The number of occurrences of the given term in the X top ranked documents. */
		protected double withinDocumentFrequency;
		private static final long serialVersionUID = 200603101233L;
        /** The document frequency of the term in the X top ranked documents. */
        protected int documentFrequency;
        
        /** The term frequency of the term in the whole collection. */
        protected double TF;
        /** The document frequency in the whole collection. */
        protected double Nt;
        protected THashSet originalTerms;
        /** 
         * The constructor of ExpansionTerm. Once the term is found in a top-
         * retrieved documents, we create a record for this term.
         * @param termID int the ID of the term
         * @param withinDocumentFrequency double the frequency of the term in 
         *        a top-retrieved document
         */
		public DistributedExpansionTerm(
				double withinDocumentFrequency, double TF, double Nt){
			this.withinDocumentFrequency = withinDocumentFrequency;
            this.documentFrequency = 1;
			this.weightExpansion = 0;
			this.TF = TF;
			this.Nt = Nt;
			this.docnos = new THashSet();
		}
		/**
		 * Merge with a given new entry.
		 * @param entry The new entry.
		 */
		public void mergeEntry(DistributedExpansionTerm entry){
			this.withinDocumentFrequency+=entry.withinDocumentFrequency;
			this.documentFrequency+=entry.getDocumentFrequency();
			this.docnos.addAll(entry.docnos);
		}
			
		public void setOriginalQueryTerms(String[] terms){
			this.originalTerms = new THashSet();
			for (int i=0; i<terms.length; i++)
				originalTerms.add(terms[i]);
		}
		
        /** 
         * If the term is found in another top-retrieved document, we increase
         * the frequency and the document frequency of the term.
         * @param withinDocumentFrequency double the frequency of the term
         *        in the corresponding top-retrieved document.
         */
		public void insertRecord(double withinDocumentFrequency, String docno){
			this.withinDocumentFrequency += withinDocumentFrequency;
			if (!docnos.contains(docno)){
				this.documentFrequency++;
				docnos.add(docno);
			}
		}
        /** 
         * Sets the expansion weight of the term.
         * @param weightExpansion double the expansion weight of the term.
         */
		public void setWeightExpansion(double weightExpansion){
			this.weightExpansion = weightExpansion;
		}
		/**
		 * Set the term frequency in the collection.
		 * @param value The term frequency in the collection.
		 */
		public void setTF(double value){
			this.TF = value;
		}
		/**
		 * Set the document frequency in the collection.
		 * @param value The document frequency in the collection.
		 */
		public void setNt(double value){
			this.Nt = value;
		}
		/**
		 * Get the term frequency in the collection.
		 * @return The term frequency in the collection.
		 */
		public double getTF(){
			return TF;
		}
		/**
		 * Get the document frequency in the collection.
		 * @return The document frequency in the collection.
		 */
		public double getNt(){
			return Nt;
		}
        
        /** 
         * The method returns the document frequency of term in the top-retrieved
         * documents.
         * @return int The document frequency of term in the top-retrieved
         *         documents.
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
 	* Constructs an instance of DistributedExpansionTerms.
    * @param totalLength The sum of the length of the top-retrieved documents.
    * @param lexicon Lexicon The lexicon used for retrieval.
 	*/
	public DistributedExpansionTerms(
			int totalLength, 
			double numberOfTokens,
			double avl) {
		this.terms = new THashSet();
		this.mapTermProperties=new THashMap();
		this.totalDocumentLength = totalLength;	
		this.numberOfTokens = numberOfTokens;
		this.avl = avl;
	}
	
	/**
 	* Constructs an instance of DistributedExpansionTerms.
    * @param totalLength The sum of the length of the top-retrieved documents.
    * @param lexicon Lexicon The lexicon used for retrieval.
 	*/
	public DistributedExpansionTerms(
			int totalLength, 
			double numberOfTokens,
			double avl,
			long numberOfDocuments) {
		this.terms = new THashSet();
		this.mapTermProperties=new THashMap();
		this.totalDocumentLength = totalLength;	
		this.numberOfTokens = numberOfTokens;
		this.avl = avl;
		this.numberOfDocuments = numberOfDocuments;
	}
	
	/**
 	* This method implements the functionality of assigning expansion weights to
    * the terms in the top-retrieved documents, and returns the most informative
    * terms among them.
    * @param numberOfExpandedTerms int The number of terms to extract from the
    *        top-retrieved documents
    * @param QEModel QueryExpansionModel the model used for query expansion
    * @return TermTreeNode[] The expanded terms.
 	*/
	public SingleTermQuery[] getExpandedTerms(int numberOfExpandedTerms, QueryExpansionModel QEModel) {
		
		QEModel.setTotalDocumentLength(this.totalDocumentLength);
        QEModel.setCollectionLength(this.numberOfTokens);
        QEModel.setAverageDocumentLength(this.avl);
        QEModel.setNumberOfDocuments(this.numberOfDocuments);
        
        int originalNumberOfExpandedTerms = numberOfExpandedTerms;
        
		// weight the terms
		int posMaxWeight = 0;
		int index = terms.size();
		String[] termStrings = (String[])terms.toArray(new String[terms.size()]);
		
		int numberOfTermsWithNonZeroWeight = 0;
		
		for (int i=0; i<index; i++){
			DistributedExpansionTerm term = (DistributedExpansionTerm)(mapTermProperties.get(termStrings[i]));
			
            if (//term.getDocumentFrequency() <= 1 ||
            		termStrings[i].length() < 2){
                term.setWeightExpansion(0);
                continue;
            }
            
			double TF = term.getTF();
			if (TF<=0)
				term.setWeightExpansion(0d);
			else
				term.setWeightExpansion(QEModel.score(
						term.getWithinDocumentFrequency(),
						TF
                	)
            );
	    if (term.getWeightExpansion() > 0)
	        numberOfTermsWithNonZeroWeight++;
            if (term.getWeightExpansion() > 
            		((DistributedExpansionTerm)(mapTermProperties.get(termStrings[posMaxWeight]))).getWeightExpansion())
            	posMaxWeight = i;
		}
	
	
        double normaliser = 1;
        try{
        	normaliser = ((DistributedExpansionTerm)(mapTermProperties.get(termStrings[posMaxWeight]))).getWeightExpansion();
        }
        catch(Exception e){
        	e.printStackTrace();
        	System.exit(1);
        }
        if (QEModel.PARAMETER_FREE){
        	QEModel.setMaxTermFrequency(((DistributedExpansionTerm)(mapTermProperties.get(termStrings[posMaxWeight]))).getWithinDocumentFrequency());
        	normaliser = QEModel.parameterFreeNormaliser();
        	System.err.println("parameter free query expansion.");
        }
		System.err.println("term with the maximum weight: " + termStrings[posMaxWeight] +
				", normaliser: " + Rounding.toString(normaliser, 4));
		TIntHashSet positions = new TIntHashSet();

		
		// The number of terms to extract from the pseudo relevance set is the
		// minimum between tbe system setting and the number of unique terms in
		// the pseudo relevance set.

		numberOfExpandedTerms = Math.min(Math.min(terms.size(), numberOfTermsWithNonZeroWeight), numberOfExpandedTerms);
		//	Sort the terms by weight
		THashSet expandedTerms = new THashSet();
		
		boolean conservativeQE = (ApplicationSetup.EXPANSION_TERMS == 0);
		int repeat = numberOfExpandedTerms;
		if (conservativeQE){
			System.out.println("Conservative query expansion.");
			int numberOfOriginalQueryTerms = this.originalQueryTermStrings.size();
			String[] originalQueryTerms = (String[])originalQueryTermStrings.toArray(
							new String[originalQueryTermStrings.size()]);
			for (int i=0; i<numberOfOriginalQueryTerms; i++){
				SingleTermQuery expandedTerm = new SingleTermQuery(originalQueryTerms[i]);
				if (terms.contains(originalQueryTerms[i])){
					DistributedExpansionTerm term = (DistributedExpansionTerm)mapTermProperties.get(
							originalQueryTerms[i]);
				
					expandedTerm.setWeight(term.getWeightExpansion()/normaliser);
				}
				else{				
					expandedTerm.setWeight(0d);
				}
	            if (!QEModel.PARAMETER_FREE)
	            	expandedTerm.setWeight(expandedTerm.getWeight()*QEModel.ROCCHIO_BETA);
	            expandedTerms.add(expandedTerm);
			}
		}else for (int i = 0; i < repeat; i++){
				int position = 0;
				// if the term has already been expanded to the query, aggregate j until
				// an unexpanded term is found.
				if (positions.contains(position)){
					for (int j = 1; j < index; j++){
						if (!positions.contains(j)){
							position = j;
							break;
						}
					}
				}
				// compare two terms and set position to the one with a higher weight.
				for (int j = position+1; j < index; j++){
					if (positions.contains(j))
						continue;
					if (((DistributedExpansionTerm)(mapTermProperties.get(termStrings[j]))).getWeightExpansion() > 
							((DistributedExpansionTerm)(mapTermProperties.get(termStrings[position]))).getWeightExpansion())
						position = j;
				}
				// if the maximum weight is smaller than or equal to 0, quit the loop. 
				if (((DistributedExpansionTerm)(mapTermProperties.get(termStrings[position]))).getWeightExpansion() <=0 ){
					System.out.println("the maximum weight is smaller than or equal to 0: "+
							((DistributedExpansionTerm)(mapTermProperties.get(termStrings[position]))).getWeightExpansion());
					break;
				}
				// add the position of the term with the maximum weight to the hash set.
				positions.add(position);
				
	            DistributedExpansionTerm term = (DistributedExpansionTerm)mapTermProperties.get(termStrings[position]);
				SingleTermQuery expandedTerm = new SingleTermQuery(termStrings[position]);
				expandedTerm.setWeight(term.getWeightExpansion()/normaliser);
	            if (!QEModel.PARAMETER_FREE)
	            	expandedTerm.setWeight(expandedTerm.getWeight()*QEModel.ROCCHIO_BETA); 
	            expandedTerms.add(expandedTerm);
			}	
		// Switch off conservative query expansion automatically.
		this.originalQueryTermStrings = null;
		numberOfExpandedTerms = originalNumberOfExpandedTerms;
		int size = expandedTerms.size();
		return (SingleTermQuery[])expandedTerms.toArray(new SingleTermQuery[size]);
	}
	
	public void setOriginalQueryTerms(String[] termStrings){
		this.originalQueryTermStrings = new THashSet();
		for (int i=0; i<termStrings.length; i++)
			this.originalQueryTermStrings.add(termStrings[i]);
	}
	
	/**
	 * Insert an entry that is associated to a term. If there exists an entry
	 * that for the associated term, we merge the two entries. Otherwise, we 
	 * add the entry.
	 * @param term
	 * @param entry
	 */
	public void insertEntry(String term, DistributedExpansionTerm entry){
		if (terms.contains(term)){
			((DistributedExpansionTerm)mapTermProperties.get(term)).mergeEntry(entry);
		}
		else{
			terms.add(term);
			mapTermProperties.put(term, entry);
		}
	}
	/**
	 * Merge with a DistributedExpansionTerms.
	 * @param distTerms A DistributedExpansionTerms that contains entry of a 
	 * set of terms.
	 */
	public void mergeExpansionTerms(DistributedExpansionTerms distTerms){
		String[] _terms = (String[])distTerms.terms.toArray(new String[distTerms.terms.size()]);
		for (int i=0; i<_terms.length; i++)
			this.insertEntry(_terms[i], 
					(DistributedExpansionTerm)distTerms.mapTermProperties.get(_terms[i]));
	}
	/**
	 * Insert a term.
	 * @param term The given term.
	 * @param withinDocumentFrequency The term frequency in the given document.
	 * @param docno The docno of the given document.
	 * @param lex The lexicon for retrieving the term frequency and document frequency
	 * of the given term. It is normally a global lexicon. 
	 */
	public void insertTerm(
			String term,
			double withinDocumentFrequency,
			String docno,
			Lexicon lex){
		if (terms.contains(term)){
			((DistributedExpansionTerm)mapTermProperties.get(term)).insertRecord(withinDocumentFrequency, docno);
		}
		else{
			if (lex.findTerm(term)){
				terms.add(term);
				mapTermProperties.put(term, new DistributedExpansionTerm(withinDocumentFrequency, lex.getTF(), lex.getNt()));
			}
		}
	}
	
}

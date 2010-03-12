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
 * The Original Code is DFRee.java.
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Gianni Amati <gba{a.}fub.it> (Original author)
 */
package org.terrier.matching.models;
/**
 * This class implements the DFRee weighting model. DFRee stands for DFR free from parameters.
 */
public class DFRee extends WeightingModel {
	private static final long serialVersionUID = 1L;
	/** model name */
	private static final String name = "DFRee";


	/** 
	 * A default constructor to make this model.
	 */
	public DFRee() {
		super();
	}
	/**
	 * Returns the name of the model, in this case "DFRee"
	 * @return the name of the model
	 */
	public final String getInfo() {
		return name;
	}

	/**
	 * Uses DFRee to compute a weight for a term in a document.
	 * @param tf The term frequency of the term in the document
	 * @param docLength the document's length
	 * @return the score assigned to a document with the given 
	 *         tf and docLength, and other preset parameters
	 */
		public final double score(double tf, double docLength) {
			/**
			 * DFRee model with the log normalisation function.
			 */
                double prior = tf/docLength;
        		double posterior  = (tf +1d)/(docLength +1);
        		double InvPriorCollection = numberOfTokens/termFrequency;
        		//double alpha = 1d/docLength; //0 <= alpha <= posterior
        					
        					
        		double norm = tf*Idf.log(posterior/prior)  ; 
        		 
        		return keyFrequency * norm *(
        					     tf *( 
        			   - Idf.log (prior *InvPriorCollection) 
        					     )
        				      +
        					     (tf+1d) *  ( 
        			   + Idf.log ( posterior*InvPriorCollection) 
        					     )
        					     + 0.5*Idf.log(posterior/prior)
        			    );
 	}

	
	/**
	 * Uses DFRee to compute a weight for a term in a document.
	 * @param tf The term frequency of the term in the document
	 * @param docLength the document's length
	 * @param documentFrequency The document frequency of the term (ignored)
	 * @param termFrequency the term frequency in the collection (ignored)
	 * @param keyFrequency the term frequency in the query (ignored).
	 * @return the score assigned by the weighting model DFRee.
	 */
	public final double score(
		double tf,
		double docLength,
		double documentFrequency,
		double termFrequency,
		double keyFrequency) 
	{
		/**
		 * DFRee model with the log normalisation function.
		 */
            double prior = tf/docLength;
    		double posterior  = (tf +1d)/(docLength +1);
    		double InvPriorCollection = numberOfTokens/termFrequency;
    		//double alpha = 1d/docLength; //0 <= alpha <= posterior
    					
    					
    		double norm = tf*Idf.log(posterior/prior)  ; 
    		 
    		return keyFrequency * norm *(
    					     tf *( 
    			   - Idf.log (prior *InvPriorCollection) 
    					     )
    				      +
    					     (tf+1d) *  ( 
    			   + Idf.log ( posterior*InvPriorCollection) 
    					     )
    					     + 0.5*Idf.log(posterior/prior)
    			    );
	}
}

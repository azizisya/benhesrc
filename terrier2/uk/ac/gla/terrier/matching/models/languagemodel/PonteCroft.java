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
 * The Original Code is PonteCroft.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk> 
 */
package uk.ac.gla.terrier.matching.models.languagemodel;
/**
 * This class implements Ponte & Croft's language modelling approach.
 * @author Ben He
 * @version $Revision: 1.1 $
 */
public class PonteCroft extends LanguageModel {
	protected double numberOfTokens;	
	/**
	 * The default constructor.
	 */
	public PonteCroft() {
		super();
	}
	/**
	 * Returns the name of the model.
	 * @return java.lang.String The name of the model.
	 */
	public String getInfo() {
		return "PonteCroft";
	}
	/**
	 * The method assigns score for a seen query term.
	 * @param tf The within-document frequency.
	 * @param docLength The length of the weighted document.
	 * @param termFrequency The term frequency in the collection.
	 * @param termEstimate The term estimate of the query term.
	 * @return The score for a seen query term.
	 */
	public double scoreSeenQuery(double tf, double docLength, double termFrequency, double termEstimate){
		return Math.pow(tf/docLength, 1d-risk(tf, docLength, termFrequency)) * 
				Math.pow(termEstimate, risk(tf, docLength, termFrequency));
	}
	/**
	 * The method assigns score for a seen non-query term.
	 * @param tf The within-document frequency.
	 * @param docLength The length of the weighted document.
	 * @param termFrequency The term frequency in the collection.
	 * @param termEstimate The term estimate of the query term.
	 * @return The score for a seen non-query term.
	 */
	public double scoreSeenNonQuery(double tf, double docLength, double termFrequency, double termEstimate){
		return 1 - this.scoreSeenQuery(tf, docLength, termFrequency, termEstimate);
	}
	/**
	 * The method assigns score for a unseen query term.
	 * @param termFrequency The term frequency in the collection.
	 * @return The score for a unseen query term.
	 */
	public double scoreUnseenQuery(double termFrequency){
		return termFrequency/numberOfTokens;
	}
	/**
	 * The method assigns score for a unseen non-query term.
	 * @param termFrequency The term frequency in the collection.
	 * @return The score for a unseen non-query term.
	 */
	public double scoreUnseenNonQuery(double termFrequency){
		return 1-this.scoreUnseenQuery(termFrequency);
	}
	/**
	 * The method computes the risk of retrieving a seen query term.
	 * @param tf The within-document frequency.
	 * @param docLength The length of the weighted document.
	 * @param termEstimate The term estimate of the query term.
	 * @return The risk.
	 */
	public double risk(double tf, double docLength, double termEstimate){
		double ft = termEstimate * docLength;
		return 1d/(1d+ft) * Math.pow(ft/(1d+ft), tf);
	}
	/**
	 * The method computes the average term generation probability of a term in 
	 * vocabulary.
	 * @param tf An array of within-document frequency of a query term in all 
	 * documents where it occurs.
	 * @param docLength The length of all the documents where the term occurs.
	 * @return The average generation  probability.
	 */
	public double averageTermGenerationProbability(int[] tf, double[] docLength){
		double sumOfML = 0;
		for (int i = 0; i < tf.length; i++)
			sumOfML += (double)tf[i]/docLength[i];
		return sumOfML/tf.length;
	}

    public void setNumberOfTokens(double value){ this.numberOfTokens = value;}
    public void setAverageDocumentLength(double a){}
	public void setNumberOfUniqueTerms(double n){}
	public void setNumberOfPointers(double n) {}
}

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
 * The Original Code is LanguageModel.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk> 
 */
package uk.ac.gla.terrier.matching.models.languagemodel;
import uk.ac.gla.terrier.matching.Model;
import uk.ac.gla.terrier.matching.models.Idf;
/**
 * This class should be extended by the classes used
 * for weighting documents using language modelling.
 * @version $Revision: 1.1 $
 * @author Ben He
 */
public abstract class LanguageModel implements Model {
	/** The class used for computing the idf values.*/
	protected Idf i;
	/** The term frequency in the collection.*/
	protected double termFrequency;
	/** The number of documents in the collection.*/
	protected double numberOfDocuments;
	/**
	 * A default constructor that initialises the idf i attribute
	 */
	public LanguageModel() {
		i = new Idf();
	}
	/**
	 * Returns the name of the model.
	 * @return java.lang.String The name of the model.
	 */
	public abstract String getInfo();
	/**
	 * The method provides the contract for assigning score for a seen query term.
	 * @param tf The within-document frequency.
	 * @param docLength The length of the weighted document.
	 * @param termFrequency The term frequency in the collection.
	 * @param termEstimate The term estimate of the query term.
	 * @return The score for a seen query term.
	 */
	public abstract double scoreSeenQuery(double tf, double docLength, double termFrequency, double termEstimate);
	
	/**
	 * The method provides the contract for assgining score for a seen non-query term.
	 * @param tf The within-document frequency.
	 * @param docLength The length of the weighted document.
	 * @param termFrequency The term frequency in the collection.
	 * @param termEstimate The term estimate of the query term.
	 * @return The score for a seen non-query term.
	 */
	public abstract double scoreSeenNonQuery(double tf, double docLength, double termFrequency, double termEstimate);
	
	/**
	 * The method provides the contract for assigning score for a unseen query term.
	 * @param termFrequency The term frequency in the collection.
	 * @return The score for a unseen query term.
	 */
	public abstract double scoreUnseenQuery(double termFrequency);
	
	/**
	 * The method provides the contract for assigning score for a unseen non-query term.
	 * @param termFrequency The term frequency in the collection.
	 * @return The score for a unseen non-query term.
	 */
	public abstract double scoreUnseenNonQuery(double termFrequency);
	
	/**
	 * The method provides the contract for computing the risk of retrieving 
	 * a seen query term.
	 * @param tf The within-document frequency.
	 * @param docLength The length of the weighted document.
	 * @param termEstimate The term estimate of the query term.
	 * @return The risk.
	 */
	public abstract double risk(double tf, double docLength, double termEstimate);
	/**
	 * The method provides the contract for computing the average term generation
	 * probability of a term in vocabulary.
	 * @param tf An array of within-document frequency of a query term in all documents
	 * where it occurs.
	 * @param docLength The length of all the documents where the term occurs.
	 * @return The average generation  probability.
	 */
	public abstract double averageTermGenerationProbability(int[] tf, double[] docLength);
	/**
	 * Sets the number of documents in the collection.
	 * @param numOfDocs the number of documents in the collection.
	 */
	public void setNumberOfDocuments(double numOfDocs) {
		numberOfDocuments = numOfDocs;
	}
	/**
	 * Sets the term's frequency in the collection.
	 * @param termFreq the term's frequency in the collection.
	 */
	public void setTermFrequency(double termFreq) {
		termFrequency = termFreq;
	}
	
	
	/**
	 * This method is empty.
	 */
	public void setParameter(double param) {}
	public double getParameter() {return 0;}
}

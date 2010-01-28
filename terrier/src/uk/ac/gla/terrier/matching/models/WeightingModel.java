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
 * The Original Code is WeightingModel.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Gianni Amati <gba{a.}fub.it> (original author)
 *   Ben He <ben{a.}dcs.gla.ac.uk> 
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.matching.models;
import java.io.Serializable;

import uk.ac.gla.terrier.matching.Model;
/**
 * This class should be extended by the classes used
 * for weighting terms and documents.
 * @author Gianni Amati, Ben He, Vassilis Plachouras
 * @version $Revision: 1.1 $
 */
public abstract class WeightingModel implements Model, Serializable,Cloneable {
	/** The class used for computing the idf values.*/
	protected Idf i;
	/** The average length of documents in the collection.*/
	protected double averageDocumentLength;
	/** The term frequency in the query.*/
	protected double keyFrequency;
	/** The document frequency of the term in the collection.*/
	protected double documentFrequency;
	/** The term frequency in the collection.*/
	protected double termFrequency;
	/** The number of documents in the collection.*/
	protected double numberOfDocuments;
	/** The number of tokens in the collections. */
	protected double numberOfTokens;
	/** The parameter c. This defaults to 1.0, but should be set using in the constructor
	  * of each child weighting model to the sensible default for that weighting model. */
	protected double c = 1.0d;
	/** Number of unique terms in the collection */
	protected double numberOfUniqueTerms;	

	/** The number of distinct entries in the inverted file. This figure can be calculated
	  * as the sum of all Nt over all terms */
	protected double numberOfPointers;
	/**
	 * A default constructor that initialises the idf i attribute
	 */
	public WeightingModel() {
		i = new Idf();
	}

	/** Clone this weighting model */
	public Object clone() {
		try{
			WeightingModel newModel = (WeightingModel)super.clone();
			newModel.i = (Idf)this.i.clone();
			return newModel;
		} catch (CloneNotSupportedException e) {
			throw new InternalError(e.toString());
		}
	}

	/**
	 * Returns the name of the model.
	 * @return java.lang.String
	 */
	public abstract String getInfo();
	/**
	 * This method provides the contract for implementing weighting models.
	 * @param tf The term frequency in the document
	 * @param docLength the document's length
	 * @return the score assigned to a document with the given tf 
	 * and docLength, and other preset parameters
	 */
	public abstract double score(double tf, double docLength);
	/**
	 * This method provides the contract for implementing weighting models.
	 * @param tf The term frequency in the document
	 * @param docLength the document's length
	 * @param n_t The document frequency of the term
	 * @param F_t the term frequency in the collection
	 * @param keyFrequency the term frequency in the query
	 * @return the score returned by the implemented weighting model.
	 */
	public abstract double score(
		double tf,
		double docLength,
		double n_t,
		double F_t,
		double keyFrequency);
	/**
	 * Sets the average length of documents in the collection.
	 * @param avgDocLength The documents' average length.
	 */
	public void setAverageDocumentLength(double avgDocLength) {
		averageDocumentLength = avgDocLength;
	}
	/**
	 * Sets the c value
	 * @param c the term frequency normalisation parameter value.
	 */
	public void setParameter(double c) {
		this.c = c;
	}


	/**
	 * Returns the parameter as set by setParameter()
	 */
	public double getParameter() {
		return this.c;
	}

	/**
	 * Sets the document frequency of the term in the collection.
	 * @param docFreq the document frequency of the term in the collection.
	 */
	public void setDocumentFrequency(double docFreq) {
		documentFrequency = docFreq;
	}
	/**
	 * Sets the term's frequency in the query.
	 * @param keyFreq the term's frequency in the query.
	 */
	public void setKeyFrequency(double keyFreq) {
		keyFrequency = keyFreq;
	}
	/**
	 * Set the number of tokens in the collection.
	 * @param value The number of tokens in the collection.
	 */
	public void setNumberOfTokens(double value){
		this.numberOfTokens = value;
	}
	/**
	 * Sets the number of documents in the collection.
	 * @param numOfDocs the number of documents in the collection.
	 */
	public void setNumberOfDocuments(double numOfDocs) {
		numberOfDocuments = numOfDocs;
		i.setNumberOfDocuments(numOfDocs);
	}
	/**
	 * Sets the term's frequency in the collection.
	 * @param termFreq the term's frequency in the collection.
	 */
	public void setTermFrequency(double termFreq) {
		termFrequency = termFreq;
	}
	/**
	 * Set the number of unique terms in the collection.
	 */
	public void setNumberOfUniqueTerms(double number) {
		numberOfUniqueTerms = number;
	}
	/**
	 * Set the number of pointers in the collection.
	 */
	public void setNumberOfPointers(double number) {
		numberOfPointers = number;
	}
	/**
	* This method provides the contract for implementing the 
	* Stirling formula for the power series.
	* @param n The parameter of the Stirling formula.
	* @param m The parameter of the Stirling formula.
	* @return the approximation of the power series
	*/
	public double stirlingPower(double n, double m) {
		double dif = n - m;
		return (m + 0.5d) * Idf.log(n / m) + dif * Idf.log(n);
	}
}

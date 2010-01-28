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
 * The Original Code is Bo1.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Gianni Amati <gba{a.}fub.it> (original author)
 *   Ben He <ben{a.}dcs.gla.ac.uk> 
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.matching.models;
/**
 * This class implements the Bo1 weighting model. 
 * Bo1 stands for Bose-Einstein model for randomness.
 * @author Gianni Amati, Ben He, Vassilis Plachouras
 * @version $Revision: 1.1 $
 */
public class Bo1 extends WeightingModel {
	/** 
	 * A default constructor. This must be followed by 
	 * specifying the c value.
	 */
	public Bo1() {
		super();
		this.c = 0;
		this.SUPPORT_PARAMETER_FREE_QE = true;
	}
	
	/** 
	 * Constructs an instance of this class with the 
	 * specified value for the parameter c.
	 * @param c the term frequency normalisation parameter value.
	 */
	public Bo1(double c) {
		this();
		this.c = c;
		this.SUPPORT_PARAMETER_FREE_QE = true;
	}
	
	/**
	 * Returns the name of the model.
	 * @return the name of the model
	 */
	public final String getInfo() {
		return "Bo1";
	}
	
	/**
     * This method computes the normaliser of parameter-free query expansion.
     * @param maxTermFrequency The maximum of the term frequency of the query terms.
     * @param collectionLength The number of tokens in the collections.
     * @param totalDocumentLength The sum of the length of the top-ranked documents.
     * @return The normaliser.
     */
	public final double parameterFreeNormaliser(double maxTermFrequency, double collectionLength, double totalDocumentLength){
		double numberOfDocuments =
			collectionLength / averageDocumentLength;
		double f = maxTermFrequency/numberOfDocuments; 
		return (maxTermFrequency* Math.log( (1d +f)/ f) + Math.log(1d +f))/ Math.log( 2d);
	}
	
	/**
	* This method computes the term weight using Bo1.
	* @param tf The term frequency in the document
	* @param docLength the document's length
	* @return the score assigned to a document with the given tf and 
	*         docLength, and other preset parameters
	*/
	public final double weight(double tf, double docLength) {
		double f = termFrequency / numberOfDocuments;
		return tf * i.log((1d + f) / f)
			+ i.log(1d + f);
	}
	
	/**
	* This method computes the term weight using Bo1.
	* @param tf The term frequency in the document
	* @param docLength the document's length
	* @param documentFrequency The document frequency of the term
	* @param termFrequency the term frequency in the collection
	* @return the score returned by the implemented weighting model.
	*/
	public final double weight(
		double tf,
		double docLength,
		double documentFrequency,
		double termFrequency) {
		double f = termFrequency / numberOfDocuments;
		return tf * i.log((1d + f) / f) + i.log(1d + f);
	}
}

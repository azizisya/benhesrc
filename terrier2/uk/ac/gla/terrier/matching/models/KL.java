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
 * The Original Code is KL.java.
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
 * This class implements the KL weighting model.
 * KL stands for Kullback-Leibler divergence.
 * @author Gianni Amati, Ben He, Vassilis Plachouras
 * @version $Revision: 1.1 $
 */
public class KL extends WeightingModel {
	
	/** 
	 * A default constructor. This must be followed by 
	 * specifying the c value.
	 */
	public KL() {
		super();
		this.c = 0;
		this.SUPPORT_PARAMETER_FREE_QE = true;
	}
	
	/** 
	 * Constructs an instance of this class with the 
	 * specified value for the parameter c.
	 * @param c the term frequency normalisation parameter value.
	 */
	public KL(double c) {
		this();
		this.c = c;
		this.SUPPORT_PARAMETER_FREE_QE = true;
	}
	
	/**
	 * Returns the name of the model.
	 * @return the name of the model
	 */
	public final String getInfo() {
		return "KL";
	}
	
	/**
     * This method computes the normaliser of parameter-free query expansion.
     * @param maxTermFrequency The maximum of the term frequency of the query terms.
     * @param collectionLength The number of tokens in the collections.
     * @param totalDocumentLength The sum of the length of the top-ranked documents.
     * @return The normaliser.
     */
	public final double parameterFreeNormaliser(double maxTermFrequency, double collectionLength, double totalDocumentLength){
		return (maxTermFrequency) * Math.log(collectionLength/totalDocumentLength)/
		(Math.log(2d)*totalDocumentLength);
	}
	
	/**
	* This method computes term weighting using KL.
	* @param tf The term frequency in the document
	* @param docLength the document's length
	* @return the score assigned to a document with the given tf and 
	*         docLength, and other preset parameters
	*/
	public final double weight(double tf, double docLength) {
		if (tf / docLength < termFrequency / numberOfTokens)
            return 0;
        else
            return tf /docLength * 
                    i.log(tf / docLength, 
                        termFrequency / numberOfTokens);
	}
	
	/**
	* This method computes term weighting using KL.
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
		if (tf / docLength < termFrequency / numberOfTokens)
            return 0;
        else
            return tf /docLength * 
                    i.log(tf / docLength, 
                        termFrequency / numberOfTokens);
	}
}

/*
 * Terrier - Terabyte Retriever 
 * Webpage: http://ir.dcs.gla.ac.uk/terrier 
 * Contact: terrier@dcs.gla.ac.uk
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
 * The Original Code is BM25.java code.
 *
 * The Initial Developer of the Original Code is the University of Glasgow.
 * Portions created by the Initial Developer are Copyright (C) 2004
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Gianni Amati <gba@fub.it> (original author)
 *   Ben He <ben@dcs.gla.ac.uk> 
 *   Vassilis Plachouras <vassilis@dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.matching.models;

/**
 * This class implements the BM25 weighting model. The
 * used parameters are:<br>
 * k_1 = 1.2d<br>
 * k_3 = 1000d<br>
 * b = 0.75d<br>
 * @author Gianni Amati, Ben He, Vassilis Plachouras
 * @version $Revision: 1.1 $
 */
public class BM25EXP extends WeightingModel {
	/** The constant k_1.*/
	private double k_1 = 1.2d;
	
	/** The constant k_3.*/
	private double k_3 = 1000d;
	
	/** A default constructor.*/
	public BM25EXP() {
		super();
		this.c=0.75d;
	}
	
	public BM25EXP(double b) {
		this();
		this.c = b;
	}

	/**
	 * Returns the name of the model.
	 * @return the name of the model
	 */
	public final String getInfo() {
		return "BM25EXPb"+c;
	}
	
	/**
	 * Uses BM25 to compute a weight for a term in a document.
	 * @param tf The term frequency in the document
	 * @param docLength the document's length
	 * @return the score assigned to a document with the given 
	 *         tf and docLength, and other preset parameters
	 */
	public final double weight(double tf, double docLength) {
	    double K = k_1 * ((1 - c) + c * docLength / averageDocumentLength) + tf;
	    return (tf / K)* 
	    	i.log((numberOfDocuments - documentFrequency + 0.5d) / (documentFrequency + 0.5d));
	}
	/**
	 * Uses BM25 to compute a weight for a term in a document.
	 * @param tf The term frequency in the document
	 * @param docLength the document's length
	 * @param n_t The document frequency of the term
	 * @param F_t the term frequency in the collection
	 * @return the score assigned by the weighting model BM25.
	 */
	public final double weight(
		double tf,
		double docLength,
		double n_t,
		double F_t) {
		double K = k_1 * ((1 - c) + c * docLength / averageDocumentLength) + tf;
	    return (tf / K)* 
	    	i.log((numberOfDocuments - n_t + 0.5d) / (n_t + 0.5d));
	}
	
	/**
	 * Uses BM25 to compute a weight for a term in a document.
	 * @param tf The term frequency in the document
	 * @param docLength the document's length
	 * @return the score assigned to a document with the given 
	 *         tf and docLength, and other preset parameters
	 */
	public final double score(double tf, double docLength) {
	    return ((k_3 + 1d) * keyFrequency) / ((k_3 + keyFrequency)) * weight(tf, docLength);
	}
	/**
	 * Uses BM25 to compute a weight for a term in a document.
	 * @param tf The term frequency in the document
	 * @param docLength the document's length
	 * @param n_t The document frequency of the term
	 * @param F_t the term frequency in the collection
	 * @param keyFrequency the term frequency in the query
	 * @return the score assigned by the weighting model BM25.
	 */
	public final double score(
		double tf,
		double docLength,
		double n_t,
		double F_t,
		double keyFrequency) {
		return ((k_3 + 1d) * keyFrequency) / ((k_3 + keyFrequency)) * 
		 	weight(tf, docLength, n_t, F_t);
	}
	
}

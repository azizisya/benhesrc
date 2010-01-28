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
 * The Original Code is PB2.java.
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
 * This class implements the PB2 weighting model.
 * P stands for Poisson Approximation for randomness.
 * B stands for the ratio of two Bernoulli's processes for normalising term weight.
 * 2 stands for Normalisation 2 for normalising term frequency.
 * @author Gianni Amati, Ben He, Vassilis Plachouras
 * @version $Revision: 1.1 $
 */
public class PB2 extends WeightingModel {
	/** 
	 * A default constructor. This must be followed by 
	 * specifying the c value.
	 */
	public PB2() {
		super();
		this.c=1.0d;
	}
	/** 
	 * Constructs an instance of this class with the 
	 * specified value for the parameter c.
	 * @param c the term frequency normalisation parameter value. 
	 */
	public PB2(double c) {
		super();
		this.c = c;
	}
	/**
	 * Returns the name of the model.
	 * @return the name of the model
	 */
	public final String getInfo() {
		return "PB2c" + c;
	}
	/**
	* This method computes term weight using PB2.
	* @param tf The term frequency in the document
	* @param docLength the document's length
	* @return the score assigned to a document with the given tf 
	*         and docLength, and other preset parameters
	*/
	public final double weight(double tf, double docLength) {
		double TF =
			tf * Idf.log(1.0d + (c * averageDocumentLength) / docLength);
		double NORM = (termFrequency + 1d) / (documentFrequency * (TF + 1d));
		double f = termFrequency / numberOfDocuments;
		return NORM
			* (TF * i.log(1d / f)
				+ f * Idf.REC_LOG_2_OF_E
				+ 0.5d * Idf.log(2 * Math.PI * TF)
				+ TF * (Idf.log(TF) - Idf.REC_LOG_2_OF_E));
	}
	/**
	* This method computes term weight using PB2.
	* @param tf The term frequency in the document
	* @param docLength the document's length
	* @param documentFrequency The document frequency of the term
	* @param termFrequency the term frequency in the collection
	* @param keyFrequency the term frequency in the query
	* @return the score returned by the implemented weighting model.
	*/
	public final double weight(
		double tf,
		double docLength,
		double documentFrequency,
		double termFrequency) {
		double TF =
			tf * Idf.log(1.0d + (c * averageDocumentLength) / docLength);
		double NORM = (termFrequency + 1d) / (documentFrequency * (TF + 1d));
		double f = termFrequency / numberOfDocuments;
		return NORM
			* (TF * i.log(1d / f)
				+ f * Idf.REC_LOG_2_OF_E
				+ 0.5d * Idf.log(2 * Math.PI * TF)
				+ TF * (Idf.log(TF) - Idf.REC_LOG_2_OF_E));
	}
}

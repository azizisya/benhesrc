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
 * The Original Code is IFB3.java code.
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
 * This class implements the IFB3 weighting model.
 * IF stands for the inverse term frequency model for randomness.
 * B stands for the ratio of two Bernoulli's processes for normalising term weight.
 * 3 stands for Dirichlet Priors for normalising term frequency.
 * @author Gianni Amati, Ben He, Vassilis Plachouras
 * @version $Revision: 1.1 $
 */
public class IFB3 extends WeightingModel {
	/** 
	 * A default constructor. This must be followed 
	 * by specifying the c value.
	 */
	public IFB3() {
		super();
	}
	/** 
	 * Constructs an instance of this class with the 
	 * specified value for the parameter mu.
	 * @param c the term frequency normalisation parameter value.
	 */
	public IFB3(double c) {
		this();
		this.c = c;
	}
	/**
	 * Returns the name of the model.
	 * @return the name of the model
	 */
	public final String getInfo() {
		return "IFB3c" + c;
	}
	/**
	 * Uses IFB3 to compute a weight for a term in a document.
	 * @param tf The term frequency in the document
	 * @param docLength the document's length
	 * @return the score assigned to a document with the given 
	 *         tf and docLength, and other preset parameters
	 */
	public final double weight(double tf, double docLength) {
		double TF =
			c*(tf+c*termFrequency/numberOfTokens)/(docLength+c);
		double NORM = (termFrequency + 1d) / (documentFrequency * (TF + 1d));
		//double f = termFrequency / numberOfDocuments;
		return TF * i.idfDFR(termFrequency) * NORM;
	}
	/**
	 * Uses IFB3 to compute a weight for a term in a document.
	 * @param tf The term frequency in the document
	 * @param docLength the document's length
	 * @param n_t The document frequency of the term
	 * @param F_t the term frequency in the collection
	 * @param keyFrequency the term frequency in the query
	 * @return the score assigned by the weighting model IFB3.
	 */
	public final double weight(
		double tf,
		double docLength,
		double n_t,
		double F_t) {
		double TF =
			c*(tf+c*termFrequency/numberOfTokens)/(docLength+c);
		double NORM = (termFrequency + 1d) / (documentFrequency * (TF + 1d));
		//double f = termFrequency / numberOfDocuments;
		return TF * i.idfDFR(termFrequency) * NORM;
	}
}

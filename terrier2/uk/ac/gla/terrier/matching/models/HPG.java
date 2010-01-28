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
 * The Original Code is HPG.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk> 
 */
package uk.ac.gla.terrier.matching.models;
/**
 * This class implements the HPG weighting model.
 * @author Ben He
 * @version $Revision: 1.1 $
 */
public class HPG extends WeightingModel {
	/** A default constructor. */
	public HPG() {
		super();
	}
	/**
	 * Returns the name of the model.
	 * @return the name of the model
	 */
	public final String getInfo() {
		return "HPG";
	}
	/**
	 * Uses HPG to compute a weight for a term in a document.
	 * @param tf The term frequency in the document
	 * @param docLength the document's length
	 * @return the score assigned to a document with the given 
	 *         tf and docLength, and other preset parameters
	 */
	public final double weight(double tf, double docLength) {
		double NORM = 1.0D / (tf + 1d);
		double f = docLength / numberOfUniqueTerms;
		double p1 = tf * Idf.log(1d / f) + f * Idf.REC_LOG_2_OF_E
				+ 0.5d * Idf.log(2 * Math.PI * tf)
				+ tf * (Idf.log(tf) - Idf.REC_LOG_2_OF_E);
		double p2 = Idf.log((numberOfTokens-docLength)/(termFrequency-tf));
		double p3 = Idf.log(numberOfTokens/termFrequency);
		return NORM
			* (p1+p2-p3);
	}
	/**
	 * Uses HPG to compute a weight for a term in a document.
	 * @param tf The term frequency in the document
	 * @param docLength the document's length
	 * @param n_t The document frequency of the term
	 * @param F_t the term frequency in the collection
	 * @param keyFrequency the term frequency in the query
	 * @return the score assigned by the weighting model H.
	 */
	public final double weight(
		double tf,
		double docLength,
		double n_t,
		double F_t) {
		double NORM = 1.0D / (tf + 1d);
		double f = docLength / numberOfUniqueTerms;
		double p1 = tf * Idf.log(1d / f) + f * Idf.REC_LOG_2_OF_E
				+ 0.5d * Idf.log(2 * Math.PI * tf)
				+ tf * (Idf.log(tf) - Idf.REC_LOG_2_OF_E);
		double p2 = Idf.log((numberOfTokens-docLength)/(termFrequency-tf));
		double p3 = Idf.log(numberOfTokens/termFrequency);
		return NORM
			* (p1+p2-p3);
	}
}

/*
 * Terrier - Terabyte Retriever 
 * Webpage: http://ir.dcs.gla.ac.uk/terrier 
 * Contact: terrier{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * Information Retrieval Group
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
 * The Original Code is PL.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Gianni Amati <gba{a.}fub.it> (original author)
 *   Ben He <ben{a.}dcs.gla.ac.uk> 
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.matching.models;
/**
 * This class implements the PL weighting model.
 * @author Gianni Amati, Ben He, Vassilis Plachouras
 * @version $Revision: 1.2 $
 */
public class PL extends WeightingModel {
	/** 
	 * A default constructor. This must be followed 
	 * by specifying the c value.
	 */
	public PL() {
		super();
	}
	/** 
	 * Constructs an instance of this class with the 
	 * specified value for the parameter c.
	 * @param c the term frequency normalisation parameter value.
	 */
	public PL(double c) {
		this();
		this.c = c;
	}
	/**
	 * Returns the name of the model.
	 * @return the name of the model
	 */
	public final String getInfo() {
		return "PL";
	}
	/**
	 * Uses PL to compute a weight for a term in a document.
	 * @param tf The term frequency in the document
	 * @param docLength the document's length
	 * @return the score assigned to a document with the given 
	 *         tf and docLength, and other preset parameters
	 */
	public final double score(double tf, double docLength) {
		double TF = tf;
		//double TF =
		//	tf * i.log(1.0d + (c * averageDocumentLength) / docLength);
		double NORM = 1.0D / (TF + 1d);
		double f = (1.0D * termFrequency) / (1.0D * numberOfDocuments);
		return NORM
			* keyFrequency
			* (TF * i.log(1.0D / f)
				+ f * Idf.REC_LOG_2_OF_E
				+ 0.5d * i.log(2 * Math.PI * TF)
				+ TF * (i.log(TF) - Idf.REC_LOG_2_OF_E));
	}
	/**
	 * Uses PL to compute a weight for a term in a document.
	 * @param tf The term frequency in the document
	 * @param docLength the document's length
	 * @param n_t The document frequency of the term
	 * @param F_t the term frequency in the collection
	 * @param keyFrequency the term frequency in the query
	 * @return the score assigned by the weighting model PL.
	 */
	public final double score(
		double tf,
		double docLength,
		double n_t,
		double F_t,
		double keyFrequency) {
		double TF = tf;
		//double TF =
		//	tf * i.log(1.0d + (c * averageDocumentLength) / docLength);
		double NORM = 1.0D / (TF + 1d);
		double f = F_t / numberOfDocuments;
		return NORM
			* keyFrequency
			* (TF * i.log(1d / f)
				+ f * Idf.REC_LOG_2_OF_E
				+ 0.5d * i.log(2 * Math.PI * TF)
				+ TF * (i.log(TF) - Idf.REC_LOG_2_OF_E));
	}

	//TODO both these methods shouldn't be here
	public double normalise(double tf, double docLength, double avgDocLength) {
		return tf * i.log(1.0d + (c * avgDocLength / docLength));
	}

	public double normalise(double tf, double docLength, double avgDocLength, double cVal) {
		return tf * i.log(1.0d + (cVal * avgDocLength / docLength));
	}

}

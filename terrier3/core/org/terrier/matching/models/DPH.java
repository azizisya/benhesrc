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
 * The Original Code is DPH.java.
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Gianni Amati <gba{a.}fub.it> (original author)
 *   Ben He <ben{a.}dcs.gla.ac.uk> 
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
 */
package org.terrier.matching.models;
/**
 * This class implements the DPH hypergeometric weighting model. P
 * stands for Popper's normalization. This is a parameter-free
 * weighting model. Even if the user specifies a parameter value, it will <b>NOT</b>
 * affect the results. It is highly recomended to use the model with query expansion. 
 * @author Gianni Amati, Ben He, Vassilis Plachouras
 * @version $Revision: 1.8 $
 */
public class DPH extends WeightingModel {
	private static final long serialVersionUID = 1L;
	/** 
	 * A default constructor.
	 */
	public DPH() {
		super();
	}
	
	/**
	 * Returns the name of the model.
	 * @return the name of the model
	 */
	public final String getInfo() {
		return "DPH";
	}
	/**
	 * Uses DPH to compute a weight for a term in a document.
	 * @param tf The term frequency in the document
	 * @param docLength the document's length
	 * @return the score assigned to a document with the given 
	 *		 tf and docLength, and other preset parameters
	 */
	public final double score(double tf, double docLength) {
		if (tf==0d)
			return 0d;
		double f = tf/docLength;
 		double norm = (1d-f) * (1d -f)/(tf+1d);
 
 		return keyFrequency *norm
 			 * (tf*Idf.log ((tf*
			averageDocumentLength/docLength) *
			( numberOfDocuments/termFrequency) )
 			   + 0.5d* Idf.log(2d*Math.PI*tf*(1d-f))
 			 );
 	}

	/**
	 * Uses DPH to compute a weight for a term in a document.
	 * @param tf The term frequency in the document
	 * @param docLength the document's length
	 * @param n_t The document frequency of the term
	 * @param F_t the term frequency in the collection
	 * @param keyFrequency the term frequency in the query
	 * @return the score assigned by the weighting model DPH.
	 */
	public final double score(
		double tf,
		double docLength,
		double n_t,
		double F_t,
		double keyFrequency) {
        double f = tf/docLength;
 		double norm = (1d-f) * (1d -f)/(tf+1d);
 		if (tf==0d)
			return 0d;
 		return keyFrequency *norm
 			 * (tf*Idf.log ((tf*
			averageDocumentLength/docLength) *
			( numberOfDocuments/F_t) )
 			   + 0.5d* Idf.log(2d*Math.PI*tf*(1d-f))
 			   );
 	}

}

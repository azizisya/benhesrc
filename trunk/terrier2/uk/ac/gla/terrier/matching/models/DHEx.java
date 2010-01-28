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
 * The Original Code is DHEx.java.
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
 * This class implements the DHEx weighting model. This is a parameter-free
 * weighting model. Even if the user specifies a parameter value, it will <b>NOT</b>
 * affect the results. It is highly recomended to use the model with query expansion. 
 * @author Gianni Amati, Ben He
 * @version $Revision: 1.1 $
 */
public class DHEx extends WeightingModel {
	private double k = 0.5d;
	/** 
	 * A default constructor.
	 */
	public DHEx() {
		super();
	}
	
	/**
	 * Returns the name of the model.
	 * @return the name of the model
	 */
	public final String getInfo() {
		return "DHEx";
	}
	/**
	 * Uses DHEx to compute a weight for a term in a document.
	 * @param tf The term frequency in the document
	 * @param docLength the document's length
	 * @return the score assigned to a document with the given 
	 *         tf and docLength, and other preset parameters
	 */
	public final double weight(double tf, double docLength) {
		double f1  = tf/docLength;
 		double f2  = (tf+1D)/(docLength+1D);
 		double E1=(tf*i.log ((tf* averageDocumentLength/docLength) *
					( numberOfDocuments/termFrequency) )
			   + 0.5d* Idf.log(2d*Math.PI*tf*(1d-f1))
			   );
		double E2=((tf+1D)*i.log (((tf+1D)* averageDocumentLength/(docLength+1D)) *
					( numberOfDocuments/(termFrequency+1D)) )
			   + 0.5d* Idf.log(2d*Math.PI*(tf+1d)*(1d-f2))
			   );
		
		return (E2*f2 -E1*f1)/f2;
	}
	/**
	 * Uses DHEx to compute a weight for a term in a document.
	 * @param tf The term frequency in the document
	 * @param docLength the document's length
	 * @param n_t The document frequency of the term
	 * @param F_t the term frequency in the collection
	 * @param keyFrequency the term frequency in the query
	 * @return the score assigned by the weighting model DHEx.
	 */
	public final double weight(
		double tf,
		double docLength,
		double n_t,
		double F_t) {
			double prior = tf/docLength;
			double f  = prior ;
			double norm = (1d-prior) * (1 -f)/(tf+1d);
		 
		    return norm *
		    (tf*i.log ((tf* averageDocumentLength/docLength) *( numberOfDocuments/F_t) )
			       + 0.5d* Idf.log(2d*Math.PI*tf*(1d-f))
			       )/(docLength*(tf+1d)/(docLength -tf));	}
}

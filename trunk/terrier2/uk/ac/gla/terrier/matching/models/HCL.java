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
 * The Original Code is HCL.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk> 
 */
package uk.ac.gla.terrier.matching.models;
/**
 * This class implements the HCL weighting model.
 * @author Ben He
 * @version $Revision: 1.1 $
 */
public class HCL extends WeightingModel {
	
	/** A default constructor. */
	public HCL() {
		super();
	}
	/**
	 * Returns the name of the model.
	 * @return the name of the model
	 */
	public final String getInfo() {
		return "HCL";
	}
	public double logFactorial(double n){
		return Idf.log(Math.sqrt(2*Math.PI)) +
			(n+0.5)*Idf.log(n) - n //+ 1/(12*n+1)
			;
	}
	/**
	 * Uses HCL to compute a weight for a term in a document.
	 * @param tf The term frequency in the document
	 * @param docLength the document's length
	 * @return the score assigned to a document with the given 
	 *         tf and docLength, and other preset parameters
	 */
	public final double weight(double tf, double docLength) {
		
		double NORM = 1.0D / (tf + 1d);
		return NORM *
			(logFactorial(docLength-tf)+logFactorial(tf)+
			logFactorial(numberOfTokens-docLength-termFrequency+tf)+
			logFactorial(termFrequency-tf)+
			logFactorial(numberOfTokens)-
			logFactorial(numberOfTokens-termFrequency)-
			logFactorial(termFrequency)-
			logFactorial(docLength)-
			logFactorial(numberOfTokens-docLength));
	}
	/**
	 * Uses HCL to compute a weight for a term in a document.
	 * @param tf The term frequency in the document
	 * @param docLength the document's length
	 * @param n_t The document frequency of the term
	 * @param F_t the term frequency in the collection
	 * @param keyFrequency the term frequency in the query
	 * @return the score assigned by the weighting model HCL.
	 */
	public final double weight(
		double tf,
		double docLength,
		double n_t,
		double F_t) {
		double NORM = 1.0D / (tf + 1d);
		// combination
		return NORM *
			(logFactorial(docLength-tf)+logFactorial(tf)+
			logFactorial(numberOfTokens-docLength-termFrequency+tf)+
			logFactorial(termFrequency-tf)+
			logFactorial(numberOfTokens)-
			logFactorial(numberOfTokens-termFrequency)-
			logFactorial(termFrequency)-
			logFactorial(docLength)-
			logFactorial(numberOfTokens-docLength));
	}
}

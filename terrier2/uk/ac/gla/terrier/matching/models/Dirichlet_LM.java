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
 * The Original Code is Dirichlet_LM.java.
 *
 * The Original Code is Copyright (C) 2004-2007 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s): Ben He <ben{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.matching.models;

/**
 * This class implements the Dirichlet LM weighting model.
 * @author Ben He
 * @version $Revision: 1.1 $
 */
public class Dirichlet_LM extends WeightingModel {
	/** 
	 * A default constructor. Uses the default value of lambda=0.15.
	 */
	
	public Dirichlet_LM() {
		super();
		this.c = 0.15;
		
	}
	
	/** 
	 * Constructs an instance of this class with the 
	 * specified value for the parameter lambda.
	 * @param lambda the smoothing parameter.
	 */
	public Dirichlet_LM(double lambda) {
		this();
		this.c = lambda;
	}
	/**
	 * Returns the name of the model.
	 * @return the name of the model
	 */
	
	public final String getInfo(){
		return "Dirichlet_LM" + c;
	}
	/**
	 * Uses Dirichlet_LM to compute a weight for a term in a document.
	 * @param tf The term frequency in the document
	 * @param docLength the document's length
	 * @return the score assigned to a document with the given 
	 *         tf and docLength, and other preset parameters
	 */
	public final double weight(double tf, double docLength) {

		return i.log((1-c)*(tf*numberOfTokens)/(termFrequency*docLength)+c)+i.log(tf);	
		
	}
	/**
	 * Uses Dirichlet_LM to compute a weight for a term in a document.
	 * @param tf The term frequency in the document
	 * @param docLength the document's length
	 * @param n_t The document frequency of the term
	 * @param F_t the term frequency in the collection
	 * @param keyFrequency the term frequency in the query
	 * @return the score assigned by the weighting model Dirichlet_LM.
	 */
	public final double weight(
		double tf,
		double docLength,
		double n_t,
		double F_t) {


		return i.log((1-c)*(tf*numberOfTokens)/(termFrequency*docLength)+c)+i.log(tf);
		
	}
	
	/**
	 * Combine document model with query model.
	 * @param tf The term frequency in the document
	 * @param docLength the document's length
	 * @return the score assigned to a document with the given tf 
	 * and docLength, and other preset parameters
	 */
	public double score(double tf, double docLength){
		return weight(tf, docLength);
	}
	/**
	 * Combine document model with query model.
	 * @param tf The term frequency in the document
	 * @param docLength the document's length
	 * @param n_t The document frequency of the term
	 * @param F_t the term frequency in the collection
	 * @param keyFrequency the term frequency in the query
	 * @return the score returned by the implemented weighting model.
	 */
	public double score(
		double tf,
		double docLength,
		double n_t,
		double F_t,
		double keyFrequency){
		return weight(tf, docLength, n_t, F_t);
	}
}

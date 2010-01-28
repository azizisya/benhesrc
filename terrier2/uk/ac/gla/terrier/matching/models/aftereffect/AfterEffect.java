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
 * The Original Code is AfterEffect.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk> 
 */
package uk.ac.gla.terrier.matching.models.aftereffect;

import java.io.Serializable;

/**
 * This class provides a contract for implementing the first normalisation models by 
 * after effect for the DFR framework. This is referred to as the component (1-prob2) in the DFR framework.
 * Classes implementing this interface are used by the DFRWeightingModel.
 * @author Ben He
 * @version $Revision: 1.1 $
 * @see uk.ac.gla.terrier.matching.models.DFRWeightingModel
 */
public abstract class AfterEffect implements Serializable{
	/** The average document length in collection. */
	protected double avl;
	/** The term frequency normalisation parameter used for method L5 */
	protected double parameter;
	/**
	 * A default constructor
	 */
	public AfterEffect() {/* An empty constructor */
		
	}
	/**
	 * Set the average document length, which is used for computing the
	 * prior for the first normalisation.
	 * @param value The average document length.
	 */
	public void setAverageDocumentLength(double value){
		this.avl = value;	
	}
	/**
	 * @return the term frequency normalisation parameter
	 */
	public double getParameter() {
		return parameter;
	}
	/**
	 * @param parameter the term frequency normalisation parameter value to set
	 */
	public void setParameter(double parameter) {
		this.parameter = parameter;
	}
	/**
	 * Returns the name of the model.
	 * @return java.lang.String
	 */
	public abstract String getInfo();
	/**
	 * This method provides the contract for implementing first normalisation models
	 * by after effect.
	 * @param tf The term frequency in the document
	 * @param documentFrequency The document frequency of the given query term
	 * @param termFrequency The frequency of the given term in the whole collection.
	 * @return The gain of having one more occurrence of the query term.
	 */
	public abstract double gain(double tf, double documentFrequency, double termFrequency);

}

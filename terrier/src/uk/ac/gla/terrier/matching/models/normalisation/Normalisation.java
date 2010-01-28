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
 * The Original Code is Normalisation.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk> 
 */
package uk.ac.gla.terrier.matching.models.normalisation;

import java.io.Serializable;

import uk.ac.gla.terrier.matching.models.Idf;
import uk.ac.gla.terrier.utility.ApplicationSetup;

/**
 * This class provides a contract for implementing frequency normalisation methods.
 * <b>Properties</b><ul>
  * <li><tt>field.retrieval</tt> - A boolean variable that indicates if field-retrieval is enabled. This allows for further extension with retrieval over different fields.</li>
  * </ul>
 * @author Ben He
 * @version $Revision: 1.1 $
 */
public abstract class Normalisation implements Serializable{
	/** A boolean variable that indicates if the system is running field retrieval. */
	protected boolean isFieldRetrieval = Boolean.valueOf(
			ApplicationSetup.getProperty("field.retrieval", "false")).booleanValue();
	/** The average document length in the collection. */
	protected double averageDocumentLength;
	/** The class used for computing the logorithm values.*/ 
	protected Idf idf;
	/** The frequency of the query term in the whole collection. */
	protected double termFrequency;
	/** The free parameter of the normalisation method. */
	protected double parameter;
	/** The weight of the field. This takes effect only when fieldRetrieval
	 * is enabled. It is equal 1 by default. To change the setting, call
	 * method setFieldWeight.
	 * */
	protected double fieldWeight = 1d;
	/**
	 * The number of tokens in the whole collection.
	 */
	protected double numberOfTokens;
	/**
	 * The document frequency of the term.
	 */
	protected double Nt;
	/**
	 * The number of documents in the whole collection.
	 */
	protected double numberOfDocuments;
	/**
	 * The default constructor.
	 *
	 */
	public Normalisation(){
		idf = new Idf();
	}
	/**
	 * Set the average document length in the collection.
	 * @param value The average document length in the collection.
	 */
	public void setAverageDocumentLength(double value){
		this.averageDocumentLength = value;
	}
	/**
	 * Set the number of tokens in the collection.
	 * @param value The number of tokens in the collection.
	 */
	public void setNumberOfTokens(double value){
		this.numberOfTokens = value;
	}
	/**
	 * The constructor that specifies the parameter value.
	 * @param value The specified value of the free parameter of the implemented 
	 * frequency normalisation method.
	 */
	public Normalisation(double value){
		this();
		this.parameter = value;
	}
	/**
	 * This method sets the weight of a field. The final normalised frequency
	 * is the product of the normalised frequency with the weight.
	 * @param value The weight of the field.
	 */
	public void setFieldWeight(double value){
		this.fieldWeight = value;
	}
	public void enableFieldRetrieval(){
	        isFieldRetrieval=true;
	}
	public void disableFieldRetrieval(){
	        isFieldRetrieval=false;
	}
	/**
	 * Specify the free parameter of the implemented frequency normalisation method.
	 * @param value The specified value of the frequency normalisation parameter.
	 */
	public void setParameter(double value){
		this.parameter = value;
	}
	/**
	 * Returns the weight of the field for field retrieval.
	 * @return The weight of the field.
	 */
	public double getFieldWeight(){
		return this.fieldWeight;
	}
	/**
	 * Returns the hyper-parameter value.
	 * @return The hyper-parameter value.
	 */
	public double getParameter(){
		return this.parameter;
	}
	/**
	 * This method provides the contract for getting the name of the 
	 * implemented frequency normalisation method.
	 * @return The name of the implemented frequency normalisation method.
	 */
	public abstract String getInfo();
	/**
	 * This method provides the contract for implementing the frequency 
	 * normalisation formula.
	 * @param tf The frequency of the query term in the document.
	 * @param docLength The number of tokens in the document.
	 * @param termFrequency The frequency of the query term in the collection.
	 * @return The normalised term frequency.
	 */
	public abstract double normalise(double tf, double docLength, double termFrequency);
	/**
	 * @param nt the document frequency to set
	 */
	public void setDocumentFrequency(double nt) {
		Nt = nt;
	}
	/**
	 * @param numberOfDocuments the numberOfDocuments to set
	 */
	public void setNumberOfDocuments(double numberOfDocuments) {
		this.numberOfDocuments = numberOfDocuments;
	}
	
}

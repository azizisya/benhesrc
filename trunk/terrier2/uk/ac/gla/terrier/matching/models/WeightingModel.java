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
 * The Original Code is WeightingModel.java.
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
import org.apache.log4j.Logger;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import uk.ac.gla.terrier.matching.Model;
import uk.ac.gla.terrier.utility.ApplicationSetup;
/**
 * This class should be extended by the classes used
 * for weighting terms and documents.
 * @author Gianni Amati, Ben He, Vassilis Plachouras
 * @version $Revision: 1.1 $
 */
public abstract class WeightingModel implements Model, Serializable,Cloneable  {
	protected static Logger logger = Logger.getRootLogger();
	/**
	 * The default namespace of weighting model classes.
	 * The weighting model names are prefixed with this
	 * namespace, if they are not fully qualified.
	 */
	public static final String NAMESPACE_MODEL = "uk.ac.gla.terrier.matching.models.";
	/**
	 * Caching the weighting models that have been
	 * created so far.
	 */
	protected static Map<String, WeightingModel> Cache_Model = new HashMap<String, WeightingModel>();
	/** The class used for computing the idf values.*/
	protected Idf i;
	/** The average length of documents in the collection.*/
	protected double averageDocumentLength;
	/** The term frequency in the query.*/
	protected double keyFrequency;
	/** The document frequency of the term in the collection.*/
	protected double documentFrequency;
	/** The term frequency in the collection.*/
	protected double termFrequency;
	/** The number of documents in the collection.*/
	protected double numberOfDocuments;
	/** The number of tokens in the collections. */
	protected double numberOfTokens;
	/** The parameter c. This defaults to 1.0, but should be set using in the constructor
	  * of each child weighting model to the sensible default for that weighting model. */
	protected double c = 1.0d;
	/** Number of unique terms in the collection */
	protected double numberOfUniqueTerms;
	/** /** Rocchio's alpha for query expansion. Its default value is 1.*/
	public double ROCCHIO_ALPHA;
	/** Rocchio's beta for query expansion. Its default value is 0.4.*/
	public double ROCCHIO_BETA;
	/** Indicates whether the model supports parameter free query expansion. Defaults to false.*/
	public boolean SUPPORT_PARAMETER_FREE_QE = false;
	/** Boolean variable indicates whether to apply the parameter free query expansion. Defaults to true.*/
	public boolean PARAMETER_FREE = true;
	/** Indicates if query expansion  */
	public boolean PerDocQE = false;

	/** The number of distinct entries in the inverted file. This figure can be calculated
	  * as the sum of all Nt over all terms */
	protected double numberOfPointers;
	/**
	 * A default constructor that initialises the idf i attribute
	 */
	public WeightingModel() {
		i = new Idf();
		/* Accept both rocchio.beta and rocchio_beta as property name. rocchio_beta will deprecated in due course. */
		PerDocQE = Boolean.parseBoolean(ApplicationSetup.getProperty("expansion.per.doc", "false"));
		ROCCHIO_BETA = Double.parseDouble(ApplicationSetup.getProperty("rocchio.beta", ApplicationSetup.getProperty("rocchio_beta", "0.4d")));
		ROCCHIO_ALPHA = Double.parseDouble(ApplicationSetup.getProperty("rocchio.alpha", ApplicationSetup.getProperty("rocchio_alpha", "1d")));
		PARAMETER_FREE = (PerDocQE)?(false):(Boolean.parseBoolean(ApplicationSetup.getProperty("parameter.free.expansion", "true")));
	}

	/** Clone this weighting model */
	public Object clone() {
		try{
			WeightingModel newModel = (WeightingModel)super.clone();
			newModel.i = (Idf)this.i.clone();
			return newModel;
		} catch (CloneNotSupportedException e) {
			throw new InternalError(e.toString());
		}
	}

	/**
	 * Returns the name of the model.
	 * @return java.lang.String
	 */
	public abstract String getInfo();
	
	/**
	 * Returns query expansion-related information about the model.
	 * @return java.lang.String
	 */
	public String getQEInfo(){
		if (PARAMETER_FREE && SUPPORT_PARAMETER_FREE_QE)
			return "QEfree";
		return "QE"+ROCCHIO_BETA;
	}
	/**
	 * This method provides the contract for implementing weighting models.
	 * @param tf The term frequency in the document
	 * @param docLength the document's length
	 * @return The score returned by the implemented weighting model.
	 */
	public abstract double weight(double tf, double docLength);
	
	/**
	 * This method provides the contract for implementing weighting models.
	 * @param tf The term frequency in the document
	 * @param docLength the document's length
	 * @param n_t The document frequency of the term
	 * @param F_t the term frequency in the collection
	 * @param keyFrequency the term frequency in the query
	 * @return The score returned by the implemented weighting model.
	 */
	public abstract double weight(
		double tf,
		double docLength,
		double n_t,
		double F_t);
	
	/**
     * This method provides the contract for computing the normaliser of
     * parameter-free query expansion. It returns 1 by default. Override 
     * this method to implement parameter-free normaliser for each model.
     * @param maxTermFrequency The maximum of the in-collection term frequency of the terms in the pseudo relevance set.
     * @param collectionLength The number of tokens in the collections.
     * @param totalDocumentLength The sum of the length of the top-ranked documents.
     * @return The normaliser.
     */
    public double parameterFreeNormaliser(
    		double maxTermFrequency, 
    		double collectionLength, 
    		double totalDocumentLength){
    	if (!PARAMETER_FREE)
    		return 1;
    	else{
    		logger.warn("Parameter free normaliser not implemented.");
    		return 1;
    	}
    }
	
	/**
	 * This method provides the contract for combining query model with
	 * weighting model. The two models are combined by their product by
	 * default. Override this method if another combination is applied.
	 * @param tf The term frequency in the document
	 * @param docLength the document's length
	 * @return the score assigned to a document with the given tf 
	 * and docLength, and other preset parameters
	 */
	public double score(double tf, double docLength){
		return keyFrequency * weight(tf, docLength);
	}
	/**
	 * This method provides the contract for combining query model with
	 * weighting model. The two models are combined by their product by
	 * default. Override this method if other combination is applied.
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
		return keyFrequency * weight(tf, docLength, n_t, F_t);
	}
	/**
	 * Sets the average length of documents in the collection.
	 * @param avgDocLength The documents' average length.
	 */
	public void setAverageDocumentLength(double avgDocLength) {
		averageDocumentLength = avgDocLength;
	}
	/**
	 * Sets the c value
	 * @param c the term frequency normalisation parameter value.
	 */
	public void setParameter(double c) {
		this.c = c;
	}

	/** Obtain the weighting model to use.
	 *  This will be cached in a hashtable for the lifetime of the
	 *  application. If Name does not contain ".", then <tt>
	 *  NAMESPACE_QEMODEL will be prefixed to it before loading.
	 *  @param Name the name of the weighting model to load.
	 */
	public static WeightingModel getWeightingModel(String Name)
	{
		WeightingModel rtr = null;
		if (Name.indexOf(".") < 0 )
			Name = NAMESPACE_MODEL +Name;
		//check for acceptable matching models
		rtr = (WeightingModel)Cache_Model.get(Name);
		if (rtr == null)
		{
			try
			{
				if (Name.indexOf("(") > 0){
					String params = Name.substring( 
						Name.indexOf("(")+1, Name.indexOf(")"));
					String[] parameters = params.split("\\s*,\\s*");
					
					rtr = (WeightingModel) Class.forName(
									Name.substring(0,Name.indexOf("(")))
							.getConstructor(
									new Class[]{String[].class})
							.newInstance(
									new Object[]{parameters});
				}else{						
					rtr = (WeightingModel) Class.forName(Name).newInstance();
				}
			}
			catch(java.lang.reflect.InvocationTargetException ite)
			{
				logger.error("Recursive problem with weighting model named: "+Name,ite);
				return null;
			}
			catch(Exception e)
			{
				logger.error("Problem with weighting model named: "+Name,e);
				return null;
			}
			Cache_Model.put(Name, rtr);
		}
		return rtr;
	}

	/**
	 * Returns the term frequency normalisation parameter value.
	 */
	public double getParameter() {
		return this.c;
	}

	/**
	 * Set the document frequency of the term in the collection.
	 * @param docFreq the document frequency of the term in the collection.
	 */
	public void setDocumentFrequency(double docFreq) {
		documentFrequency = docFreq;
	}
	/**
	 * Set the term's frequency in the query.
	 * @param keyFreq the term's frequency in the query.
	 */
	public void setKeyFrequency(double keyFreq) {
		keyFrequency = keyFreq;
	}
	/**
	 * Set the number of tokens in the collection.
	 * @param value The number of tokens in the collection.
	 */
	public void setNumberOfTokens(double value){
		this.numberOfTokens = value;
	}
	/**
	 * Sets the number of documents in the collection.
	 * @param numOfDocs the number of documents in the collection.
	 */
	public void setNumberOfDocuments(double numOfDocs) {
		numberOfDocuments = numOfDocs;
		i.setNumberOfDocuments(numOfDocs);
	}
	/**
	 * Sets the term's frequency in the collection.
	 * @param termFreq the term's frequency in the collection.
	 */
	public void setTermFrequency(double termFreq) {
		termFrequency = termFreq;
	}
	/**
	 * Set the number of unique terms in the collection.
	 */
	public void setNumberOfUniqueTerms(double number) {
		numberOfUniqueTerms = number;
	}
	/**
	 * Set the number of pointers in the collection.
	 */
	public void setNumberOfPointers(double number) {
		numberOfPointers = number;
	}
	/**
	* This method implements the Stirling formula for the power series.
	* @param n The parameter of the Stirling formula.
	* @param m The parameter of the Stirling formula.
	* @return the approximation of the power series
	*/
	public double stirlingPower(double n, double m) {
		double dif = n - m;
		return (m + 0.5d) * Idf.log(n / m) + dif * Idf.log(n);
	}
}

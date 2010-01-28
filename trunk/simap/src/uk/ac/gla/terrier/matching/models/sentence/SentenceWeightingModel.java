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
package uk.ac.gla.terrier.matching.models.sentence;
import gnu.trove.THashMap;

import java.io.Serializable;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.matching.Model;
import uk.ac.gla.terrier.matching.models.Idf;
import uk.ac.gla.terrier.matching.models.WeightingModel;
import uk.ac.gla.terrier.matching.models.queryexpansion.QueryExpansionModel;
import uk.ac.gla.terrier.structures.CollectionStatistics;
/**
 * This class should be extended by the classes used
 * for weighting sentences.
 * @author Ben He
 * @version $Revision: 1.1 $
 */
public abstract class SentenceWeightingModel implements Model, Serializable  {
	protected static Logger logger = Logger.getRootLogger();
	
	/** The class used for computing the idf values.*/
	protected Idf i;
	/** The average length of documents in the collection.*/
	protected double averageDocumentLength;
	/** The term frequency in the query.*/
	protected double keyFrequency;
	/** The number of documents in the collection.*/
	protected double numberOfDocuments;
	/** The number of tokens in the collections. */
	protected double numberOfTokens;
	/** The parameter c. This defaults to 1.0, but should be set using in the constructor
	  * of each child weighting model to the sensible default for that weighting model. */
	protected double c = 1.0d;
	/** Number of unique terms in the collection */
	protected double numberOfUniqueTerms;
	
	/**
	 * The default namespace of query expansion model classes.
	 * The query expansion model names are prefixed with this
	 * namespace, if they are not fully qualified.
	 */
	public static final String NAMESPACE_QEMODEL = "uk.ac.gla.terrier.matching.models.";
	
	public WeightingModel qemodel;

	/** The number of distinct entries in the inverted file. This figure can be calculated
	  * as the sum of all Nt over all terms */
	protected double numberOfPointers;
	
	/** The meta data used for the weighting. */
	protected THashMap<String,Object> metaData;
	/**
	 * A default constructor that initialises the idf i attribute
	 */
	public SentenceWeightingModel() {
		i = new Idf();
		metaData = new THashMap<String,Object>();
	}
	
	public SentenceWeightingModel(String qemodelname){
		this();
		qemodel = getQueryExpansionModel(qemodelname);
	}
	
	public void setQueryExpansionModel(String qemodelname){
		this.qemodel=getQueryExpansionModel(qemodelname);
	}
	
	public void setCollectionStatistics(CollectionStatistics collSta){
		this.setNumberOfDocuments(collSta.getNumberOfDocuments());
		this.setNumberOfUniqueTerms(collSta.getNumberOfUniqueTerms());
		this.setNumberOfTokens(collSta.getNumberOfTokens());
		this.setNumberOfPointers(collSta.getNumberOfPointers());
		this.setAverageDocumentLength(collSta.getAverageDocumentLength());
	}
	
	public void setNumberOfDocuments(long numberOfDocuments){
		this.numberOfDocuments = numberOfDocuments;
		qemodel.setNumberOfDocuments(numberOfDocuments);
	}
	/**
	 * Sets the average length of documents in the collection.
	 * @param avgDocLength The documents' average length.
	 */
	public void setAverageDocumentLength(double averageDocumentLength){
		this.averageDocumentLength = averageDocumentLength;
		qemodel.setAverageDocumentLength(averageDocumentLength);
	}
	
	/**
	 * Returns the name of the model.
	 * @return java.lang.String
	 */
	public abstract String getInfo();
	
	/**
	 * This method provides the contract for implementing sentence weighting models.
	 * @param tf Frequency of terms in the document.
	 * @param tfSent Frequency of terms in the sentences.
	 * @param sentLength The number of tokens in the sentence.
	 * @param sentenceFrequency The number of sentences in the document that a term occurs in. 
	 * @param termFrequency The frequency of the term in the whole collection.
	 * @param documentFrequency The document frequency of the term in the whole collection.
	 * @param keyFrequency[]
	 * @return
	 */
	public abstract double score(
		double tf[],
		double tfSent[],
		double sentLength,
		double sentenceFrequency[],
		double termFrequency[],
		double documentFrequency[],
		double keyFrequency[]);
	/**
		double sum = 0d;
		for (int i=0; i<tf.length; i++)
			sum+=qemodel.score(tf[i], termFrequency[i]);
		return sum/sentLength;
		*/
	/**
	 * Sets the c value
	 * @param c the term frequency normalisation parameter value.
	 */
	public void setParameter(double c) {
		this.c = c;
	}
	/**
	 * Set a property to the weighting model.
	 * @param property The key of the property.
	 * @param value The value of the property.
	 */
	public void setProperty(String property, Object value){
		metaData.put(property, value);
	}
	/**
	 * Get the value of a property.
	 * @param property The key of the property.
	 * @return The value of the property.
	 */
	public Object getPropery(String property){
		return metaData.get(property);
	}

	/**
	 * Returns the parameter as set by setParameter()
	 */
	public double getParameter() {
		return this.c;
	}

	/**
	 * Sets the term's frequency in the query.
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
	
	/** Obtain the query expansion model for QE to use.
	 *  This will be cached in a hashtable for the lifetime of the
	 *  application. If Name does not contain ".", then <tt>
	 *  NAMESPACE_QEMODEL will be prefixed to it before loading.
	 *  @param Name the naem of the query expansion model to load.
	 */
	public WeightingModel getQueryExpansionModel(String Name)
	{
		WeightingModel rtr = null;
		if (Name.indexOf(".") < 0 )
			Name = NAMESPACE_QEMODEL +Name;
		//check for acceptable matching models
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
		}
		return rtr;
	}
}

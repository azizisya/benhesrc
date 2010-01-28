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
 * The Original Code is QueryExpansionModel.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Gianni Amati <gba{a.}fub.it> (original author)
 *   Ben He <ben{a.}dcs.gla.ac.uk> 
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.matching.models.queryexpansion;
import uk.ac.gla.terrier.matching.models.Idf;
import uk.ac.gla.terrier.utility.ApplicationSetup;
/**
 * This class should be extended by the classes used
 * for weighting terms and documents.
 * <p><b>Properties:</b><br><ul>
 * <li><tt>rocchio.beta</tt> - defaults to 0.4d</li>
 * <li><tt>parameter.free.expansion</tt> - defaults to true.</li>
 * </ul>
 * @author Gianni Amati, Ben He, Vassilis Plachouras
 * @version $Revision: 1.1 $
 * @deprecated
 */
public abstract class QueryExpansionModel {
	/** The average document length in the collection. */
    protected double averageDocumentLength;
    /** The total length of the X top-retrieved documents.
     *  X is given by system setting.
     */
    protected double totalDocumentLength;
    
    /** The number of tokens in the collection. */
    protected double collectionLength;
    
    /** The document frequency of a term. */
    protected double documentFrequency;
	/** An instance of Idf, in order to compute the logs.*/
	protected Idf idf;
	/** The maximum in-collection term frequencty of the terms in the pseudo relevance set.*/
	protected double maxTermFrequency;
	/** The number of documents in the collection. */
	protected long numberOfDocuments;
	/** The number of top-ranked documents in the pseudo relevance set. */	
	protected double EXPANSION_DOCUMENTS = 
		Integer.parseInt(ApplicationSetup.getProperty("expansion.documents", "3"));
	/** The number of the most weighted terms from the pseudo relevance set 
	 * to be added to the original query. There can be overlap between the 
	 * original query terms and the added terms from the pseudo relevance set.*/
	protected double EXPANSION_TERMS = 
		Integer.parseInt(ApplicationSetup.getProperty("expansion.terms", "10"));
	
	/** Rocchio's beta for query expansion. Its default value is 0.4.*/
	public double ROCCHIO_BETA;
	
	/** Boolean variable indicates whether to apply the parameter free query expansion. */
	public boolean PARAMETER_FREE;
	
	/**
	 * Initialises the Rocchio's beta for query expansion.
	 */
	public void initialise() {
		/* Accept both rocchio.beta and rocchio_beta as property name. rocchio_beta will deprecated in due course. */
		ROCCHIO_BETA = Double.parseDouble(ApplicationSetup.getProperty("rocchio.beta", ApplicationSetup.getProperty("rocchio_beta", "0.4d")));
		PARAMETER_FREE = Boolean.parseBoolean(ApplicationSetup.getProperty("parameter.free.expansion", "true"));
	}

	/**
	 * @param numberOfDocuments the numberOfDocuments to set
	 */
	public void setNumberOfDocuments(long numberOfDocuments) {
		this.numberOfDocuments = numberOfDocuments;
	}
	/**
	 *  A default constructor for the class that initialises the idf attribute.
	 */
	public QueryExpansionModel() {
		idf = new Idf();
		this.initialise();
	}
    /**
     * Returns the name of the model.
     * Creation date: (19/06/2003 12:09:55)
     * @return java.lang.String
     */
    public abstract String getInfo();
    
    /**
     * Set the average document length.
     * @param averageDocumentLength double The average document length.
     */
    public void setAverageDocumentLength(double averageDocumentLength){
        this.averageDocumentLength = averageDocumentLength;
    }
    
    /**
     * Set the collection length.
     * @param collectionLength double The number of tokens in the collection.
     */
    public void setCollectionLength(double collectionLength){
        this.collectionLength = collectionLength;
    }
    
    /**
     * Set the document frequency.
     * @param documentFrequency double The document frequency of a term.
     */
    public void setDocumentFrequency(double documentFrequency){
        this.documentFrequency = documentFrequency;
    }
    
    /**
     * Set the total document length.
     * @param totalDocumentLength double The total document length.
     */
    public void setTotalDocumentLength(double totalDocumentLength){
        this.totalDocumentLength = totalDocumentLength;
    }
    
    /** 
     * This method sets the maximum of the term frequency values of query terms.
     * @param maxTermFrequency
     */
    public void setMaxTermFrequency(double maxTermFrequency){
    	this.maxTermFrequency = maxTermFrequency;
    }
    
    /**
     * This method provides the contract for computing the normaliser of
     * parameter-free query expansion.
     * @return The normaliser.
     */
    public abstract double parameterFreeNormaliser();
    
    /**
     * This method provides the contract for computing the normaliser of
     * parameter-free query expansion.
     * @param maxTermFrequency The maximum of the in-collection term frequency of the terms in the pseudo relevance set.
     * @param collectionLength The number of tokens in the collections.
     * @param totalDocumentLength The sum of the length of the top-ranked documents.
     * @return The normaliser.
     */
    public abstract double parameterFreeNormaliser(double maxTermFrequency, double collectionLength, double totalDocumentLength);
    
	/**
	 * This method provides the contract for implementing query expansion models.
	 * @param withinDocumentFrequency double The term 
	 *        frequency in the X top-retrieved documents.
     * @param termFrequency double The term frequency in the collection.
	 * @return the score assigned to a document with the parameters, 
	 *         and other preset parameters
	 */
	public abstract double score(double withinDocumentFrequency, double termFrequency);
	
	/**
	 * This method provides the contract for implementing query expansion models.
     * For some models, we have to set the beta and the documentFrequency of a term.
	 * @param withinDocumentFrequency double The term frequency in the X top-retrieved documents.
     * @param termFrequency double The term frequency in the collection.
     * @param totalDocumentLength double The sum of length of the X top-retrieved documents.
     * @param collectionLength double The number of tokens in the whole collection.
     * @param averageDocumentLength double The average document length in the collection.
	 * @return double The score returned by the implemented model.
	 */
	public abstract double score(
        double withinDocumentFrequency, 
        double termFrequency,
        double totalDocumentLength, 
        double collectionLength, 
        double averageDocumentLength 
    );   
}

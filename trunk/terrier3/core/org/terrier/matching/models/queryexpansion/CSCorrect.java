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
 * The Original Code is CSCorrect.java.
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Gianni Amati <gba{a.}fub.it> (original author)
 *   Ben He <ben{a.}dcs.gla.ac.uk> 
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
 */
package org.terrier.matching.models.queryexpansion;

import org.terrier.matching.models.Idf;

/** 
 * This class implements the unsimplified chi-square divergence for query 
 * expansion.
 * @author Gianni Amati, Ben He, Vassilis Plachouras
 * @version $Revision: 1.14 $
 */
public class CSCorrect extends QueryExpansionModel {
    /** A default constructor.*/
    public CSCorrect() {
		super();
    }
    
    /**
     * Returns the name of the model.
     * @return the name of the model
     */
    public final String getInfo() {
        return "CSCorrect";
    }
	
	public final double parameterFreeNormaliser(int maxTermFrequency, int docLength){
		return 1d;
	}
    
    /** This method implements the query expansion model.
     *  @param withinDocumentFrequency double The term frequency in the X top-retrieved documents.
     *  @param termFrequency double The term frequency in the collection.
     *  @return double The query expansion weight using he complete 
     *  Kullback-Leibler divergence.
     */
    public final double score(double tf, double documentLength) {
        /**	    return 1- (Math.pow(2d, - this.totalDocumentLength*D(withinDocumentFrequency/this.totalDocumentLength,this.termFrequency/this.collectionLength))/Math.sqrt(2*Math.PI*this.totalDocumentLength*(1d- withinDocumentFrequency/this.totalDocumentLength))); */
        return documentLength * 
                //Poisson(withinDocumentFrequency / this.totalDocumentLength, termFrequency / this.collectionLength)
                (tf / documentLength) * 
                Idf.log(tf / documentLength, termFrequency / numberOfTokens)
                + 0.5d * Idf.log(2 * Math.PI * documentLength * 
                        (1d - tf / documentLength));
    } 
    
    /**
	 * This method implements the query expansion model.
	 * @param withinDocumentFrequency double The term frequency in the X top-retrieved documents.
     * @param termFrequency double The term frequency in the collection.
     * @param totalDocumentLength double The sum of length of the X top-retrieved documents.
     * @param collectionLength double The number of tokens in the whole collection.
     * @param averageDocumentLength double The average document length in the collection.
	 * @return double The score returned by the implemented model.
	 */
	public final double score(
        double tf, 
        double documentLength,
        double termFrequency, 
        double documentFrequency, 
        double keyFrequency
    ){
        /**	    return 1- (Math.pow(2d, - this.totalDocumentLength*D(withinDocumentFrequency/this.totalDocumentLength,this.termFrequency/this.collectionLength))/Math.sqrt(2*Math.PI*this.totalDocumentLength*(1d- withinDocumentFrequency/this.totalDocumentLength))); */
        return documentLength * 
                //Poisson(withinDocumentFrequency / this.totalDocumentLength, termFrequency / this.collectionLength)
                (tf / documentLength) * 
                Idf.log(tf / documentLength, termFrequency / numberOfTokens)
                + 0.5d * Idf.log(2 * Math.PI * documentLength * 
                        (1d - tf / documentLength));
    }
}
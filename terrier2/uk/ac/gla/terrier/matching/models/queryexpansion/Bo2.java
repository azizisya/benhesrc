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
 * The Original Code is Bo2.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Gianni Amati <gba{a.}fub.it> (original author)
 *   Ben He <ben{a.}dcs.gla.ac.uk> 
 */
package uk.ac.gla.terrier.matching.models.queryexpansion;

import uk.ac.gla.terrier.matching.models.Idf;

/** 
 * This class implements the Bo2 model for query expansion. 
 * See G. Amati's Phd Thesis.
 * @author Gianni Amati, Ben He
 * @version $Revision: 1.1 $
 */
public class Bo2 extends QueryExpansionModel {
	/** A default constructor.*/
	public Bo2() {
		super();
	}
	/**
	 * Returns the name of the model.
	 * @return the name of the model
	 */
	public final String getInfo() {
		if (PARAMETER_FREE)
			return "Bo2bfree";
		return "Bo2b"+ROCCHIO_BETA;
	}
	
	/**
     * This method computes the normaliser of parameter-free query expansion.
     * @return The normaliser.
     */
	public final double parameterFreeNormaliser(){
		double f =  (maxTermFrequency) * totalDocumentLength/collectionLength;
		return  ((maxTermFrequency)*  Idf.log((1d +f)/ f) +  Idf.log(1d +f));
	}
	/**
     * This method computes the normaliser of parameter-free query expansion.
     * @param maxTermFrequency The maximum of the term frequency of the query terms.
     * @param collectionLength The number of tokens in the collections.
     * @param totalDocumentLength The sum of the length of the top-ranked documents.
     * @return The normaliser.
     */
	public final double parameterFreeNormaliser(double maxTermFrequency, double collectionLength, double totalDocumentLength){
		double f =  (maxTermFrequency) * totalDocumentLength/collectionLength;
		return  ((maxTermFrequency)*  Idf.log((1d +f)/ f) +  Idf.log(1d +f));
	}
	/** This method implements the query expansion model.
	 *  @param withinDocumentFrequency double The term frequency 
	 *         in the X top-retrieved documents.
	 *  @param termFrequency double The term frequency in the collection.
	 *  @return double The query expansion weight using the Bose-Einstein statistics
	 *  where the mean is given by the Bernoulli process.
	 */
	public final double score(
		double withinDocumentFrequency,
		double termFrequency) {
		double f =
			withinDocumentFrequency
				* totalDocumentLength
				/ collectionLength;
		return withinDocumentFrequency * Idf.log((1d + f) / f)
			+ Idf.log(1d + f);
	}
	/**
	 * This method implements the query expansion model.
	 * @param withinDocumentFrequency double The term frequency 
	 *        in the X top-retrieved documents.
	 * @param termFrequency double The term frequency in the collection.
	 * @param totalDocumentLength double The sum of length of 
	 *        the X top-retrieved documents.
	 * @param collectionLength double The number of tokens in the whole collection.
	 * @param averageDocumentLength double The average document 
	 *        length in the collection.
	 * @return double The score returned by the implemented model.
	 */
	public final double score(
		double withinDocumentFrequency,
		double termFrequency,
		double totalDocumentLength,
		double collectionLength,
		double averageDocumentLength) {
		double f =
			withinDocumentFrequency
				* totalDocumentLength
				/ collectionLength;
		return withinDocumentFrequency * Idf.log((1d + f) / f)
			+ Idf.log(1d + f);
	}
}

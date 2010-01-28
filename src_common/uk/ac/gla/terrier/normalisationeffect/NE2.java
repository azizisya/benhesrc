/*
 * Smooth - Smoothing term frequency normalisation
 * Webpage: http://ir.dcs.gla.ac.uk/smooth
 * Contact: ben{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * Information Retrieval Group
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
 * The Original Code is NE2.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.normalisationeffect;

import uk.ac.gla.terrier.matching.models.normalisation.Normalisation2;
import uk.ac.gla.terrier.statistics.Statistics;
 /** This class implements the functionality of computing the normalisation 
 * effect for the normalisation 2.
 *   Creation date: (15/06/2003 17:21:24)
 *   @author: Ben He
 */
public class NE2 extends NormalisationEffect {
	/** The normalisation 2. */ 
	protected Normalisation2 method = new Normalisation2();
	/**
	 * Return the name of the normalisation method.
	 */
	public String getInfo(){
		return "2";
	}
    
	/**
	 * Set the average document length in the collection.
	 * @param value The average document length in the collection.
	 */
	public void setAverageDocumentLength(double value){
		this.method.setAverageDocumentLength(value);
	}
	/**
	 * Set the number of tokens in the collection.
	 * @param value The number of tokens in the collection.
	 */
	public void setNumberOfTokens(double value){
		this.method.setNumberOfTokens(value);
	}
	
    /**
	 * This method implements measuring the normalisation effect.
	 * @param termFrequency double The frequency of the query term in the collection.
     * @param documentLength double[] The document length of the retrieved documents.
     * @param c double The free parameter of the normalisation method.
     * @param definition int The definition of the normalisation effect. There are
     * three definitions in total. It is recommended to use definition 2.
	 * @return double The normalisation effect.
	 */
	public final double getNED(
			double[] documentLength, double c, int definition
    ){
		method.setParameter(c);
        double[] NE = new double[documentLength.length];
        for (int i = 0; i < documentLength.length; i++){
            NE[i] = method.normalise(1d, documentLength[i], 1d);
        }
        double NED = 0;
        switch(definition){
			case 1: NED = Statistics.variance(NE) / Statistics.mean(NE); 
						break;// definition 1	
			case 2: NED = Statistics.variance(Statistics.normaliseMax(NE)); 
						break;// definition 2(max), recommended
            case 3: NED = Statistics.variance(Statistics.normaliseMaxMin(NE)); 
						break;// definition 3(maxmin)
			}
        return NED;
    }
}

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
 * The Original Code is NormalisationEffect.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.normalisationeffect;

/**
 * An interface of implementing a normalisation effect class.
 * @author Ben He <ben{a.}dcs.gla.ac.uk>
 * @version $Revision: 1.1 $
 */
public abstract class NormalisationEffect{
	/**
	 * This method provides contract for implementing returning the name of
	 * the normalisation method.
	 * @return The name of the normalisation method.
	 */
	public abstract String getInfo();
	/**
	 * This method provides contract for implementing measuring the normalisation 
	 * effect.
	 * @param termFrequency double The frequency of the query term in the collection.
     * @param documentLength double[] The document length of the retrieved documents.
     * @param c double The free parameter of the normalisation method.
     * @param definition int The definition of the normalisation effect. There are
     * three definitions in total. It is recommended to use definition 2.
	 * @return double The normalisation effect.
	 */
	public abstract double getNED(
			double[] documentLength, double c, int definition
    );
	/**
	 * Set the average document length in the collection.
	 * @param value The average document length in the collection.
	 */
	public abstract void setAverageDocumentLength(double value);
	/**
	 * Set the number of tokens in the collection.
	 * @param value The number of tokens in the collection.
	 */
	public abstract void setNumberOfTokens(double value);
}

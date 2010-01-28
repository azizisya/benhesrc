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
 * The Original Code is NormalisationB.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk> 
 */
package uk.ac.gla.terrier.matching.models.normalisation;

/**
 * This class implements BM25's normalisation method.
 * @author Ben He
 * @version $Revision: 1.1 $
 */
public class NormalisationB extends Normalisation{
	/** The name of the normalisation method .*/
	protected final String methodName = "B";
	
	/**
	 * The default constructor. The hyper-parameter value is set to 0.75 by default.
	 */
	public NormalisationB()
	{
		super();
		parameter = 0.75d;
	}
	
	/**
	 * Get the name of the normalisation method.
	 * @return Return the name of the normalisation method.
	 */
	public String getInfo(){
		String info = this.methodName+"b"+parameter;
		if (this.isFieldRetrieval)
			info = info + "_w"+this.fieldWeight;
		return info;
	}
	/**
	 * This method gets the normalised term frequency.
	 * @param tf The frequency of the query term in the document.
	 * @param docLength The number of tokens in the document.
	 * @param termFrequency The frequency of the query term in the collection.
	 * @return The normalised term frequency.
	 */
	public double normalise(double tf, double docLength, double termFrequency){
		double tfn = tf / (1 - parameter + parameter * docLength / averageDocumentLength);
		if (!this.isFieldRetrieval)
			return tfn;
		else
			return this.fieldWeight * tfn;
	}
}

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
 * The Original Code is Normalisation2.java.
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk> 
 */
package org.terrier.matching.models.normalisation;

/**
 * This class implements an empty normalisation. It does no frequency normalisation but
 * returns the original frequency. In case of applying field retrieval, the returned 
 * value is the original frequency times the weight of the field.
 * @author Ben He
 * @version $Revision: 1.6 $
 */
public class Normalisation0 extends Normalisation{

	private static final long serialVersionUID = 1L;
	/** The name of the normalisation method .*/
	protected final String methodName = "0";
	/**
	 * Get the name of the normalisation method.
	 * @return Return the name of the normalisation method.
	 */
	public String getInfo(){
		return this.methodName+"c"+parameter;
	}
	/**
	 * This method returns the original term frequency. If field retrieval is
	 * applied, the returned value is the original frequency times the weight 
	 * of the field.
	 * @param tf The frequency of the query term in the document.
	 * @param docLength The number of tokens in the document.
	 * @param termFrequency The frequency of the query term in the collection.
	 * @return The normalised term frequency.
	 */
	public double normalise(double tf, double docLength, double termFrequency){
		if (docLength == 0)
			return tf;
		return tf;
	}
}

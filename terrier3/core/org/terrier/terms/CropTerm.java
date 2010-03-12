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
 * The Original Code is CropTerm.java.
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *  Craig Macdonald <craigm{a.}dcs.gla.ac.uk>   (original author)
 */

package org.terrier.terms;
import org.terrier.utility.ApplicationSetup;

/** Reduces the size of terms passing through the term pipeline to the maximum allowed
  * size for this indexing run. The crop term pipeline class has been provided as some 
  * term pipeline implementators (eg SnowballGermanStemmer) may actually lengthen words
  * that did previously fit in the indexing. 
  * @author Craig Macdonald
  * @version $Revision: 1.6 $
  */
  public final class CropTerm implements TermPipeline {
	/** Maximum length a term can be */ 
	protected static final int maxLen = ApplicationSetup.MAX_TERM_LENGTH;
	/** The next object in the term pipeline */	
	protected final TermPipeline next;

	
	/** Creates a new CropTerm pipeline object, which can be used in the 
	  * term pipeline 
	  * @param next The next termpipeline object to pass the term onto.
	  */
	public CropTerm(TermPipeline next)
	{
		this.next = next;
	}
	
	/**
	 * Reduces the term to the maximum allowed size for this indexing run
	 * @param t String the term to check the length of.
	 */
	public final void processTerm(String t)
	{
		if (t == null)
			return;
		if(t.length() > maxLen)
			t = t.substring(0,maxLen);	
		next.processTerm(t);
	}
}

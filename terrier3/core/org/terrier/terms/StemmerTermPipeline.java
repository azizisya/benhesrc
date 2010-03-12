/*
 * Terrier - Terabyte Retriever
 * Webpage: http://ir.dcs.gla.ac.uk/terrier
 * Contact: terrier{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * http://www.gla.uk
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
 * The Original Code is Stemmer.java.
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original author)
 *   
 */
package org.terrier.terms;

/** Abstract base class for Stemmers that are also TermPipeline instances
 * @since 3.0
 * @author Craig Macdonald
 */
public abstract class StemmerTermPipeline implements Stemmer, TermPipeline {

	protected TermPipeline next;
	
	protected StemmerTermPipeline()
	{
		this(null);
	}
	
	/** Make a new StemmerTermPipeline object, with _next being the next object 
	 * in this pipeline.
	 * @param _next Next pipeline object
	 */
	StemmerTermPipeline(TermPipeline _next) {
		this.next = _next;
	}
	
	/**
	 * Stems the given term and passes onto the next object in the term pipeline.
	 * @param t String the term to stem.
	 */
	public void processTerm(String t)
	{
		if (t == null)
			return;
		next.processTerm(stem(t));
	}
	
}

/*
 * Terrier - Terabyte Retriever 
 * Webpage: http://terrier.org/
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
 * The Original Code is EntryStatistics.java
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original contributor)
 */
package org.terrier.structures;

import java.io.Serializable;

/** An interface for basic statistics about a lexical entry (usually a term)
 * @since 3.0
 * @author Craig Macdonald
 */
public interface EntryStatistics extends Serializable
{
	/** The frequency (total number of occurrences) of the entry (term). */
	public int getFrequency(); //F
	/** The number of documents that the entry (term) occurred in */
	public int getDocumentFrequency(); //Nt
	/** The id of the term */
	public int getTermId();
	
	/** Increment the statistics of this object by that of another */
	public void add(EntryStatistics e);
	/** Decrement the statistics of this object by that of another */
    public void subtract(EntryStatistics e);
}

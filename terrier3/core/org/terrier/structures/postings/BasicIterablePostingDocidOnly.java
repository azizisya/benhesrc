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
 * The Original Code is BasicIterablePostingDocidOnly.java
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original contributor)
 */
package org.terrier.structures.postings;

import java.io.IOException;

import org.terrier.compression.BitIn;
import org.terrier.structures.DocumentIndex;

/** Posting iterator class that only returns identifiers, not frequencies. 
 * @since 3.0 
 */
public class BasicIterablePostingDocidOnly extends BasicIterablePosting {
	public BasicIterablePostingDocidOnly(){}
	
	public BasicIterablePostingDocidOnly(BitIn _bitFileReader, int _numEntries, DocumentIndex _doi) throws IOException {
		super(_bitFileReader, _numEntries, _doi);
	}
	
	@Override
	public int next() throws IOException {
		if (numEntries-- == 0)
			return EOL;
		id = bitFileReader.readGamma() + id;
		return id;
	}
	
	@Override
	public int getFrequency()
	{
		return 0;
	}
}

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
 * The Original Code is LexiconEntry.java
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original contributor)
 */
package org.terrier.structures;
import org.apache.hadoop.io.Writable;

public abstract class LexiconEntry implements EntryStatistics, Pointer, Writable
{
	private static final long serialVersionUID = 1L;
    public String toString()
	{
        return '('+getDocumentFrequency()+","+getFrequency()+')'
            + pointerToString();
	}

    public abstract void setTermId(int newTermId);
    public abstract void setStatistics(int n_t, int TF);
   
	@Override
	public boolean equals(Object obj) {
		if (! (obj instanceof LexiconEntry))
			return false;
		LexiconEntry o = (LexiconEntry)obj;
		return o.getTermId() == this.getTermId();
	}

	@Override
	public int hashCode() {
		return this.getTermId();
	}
}

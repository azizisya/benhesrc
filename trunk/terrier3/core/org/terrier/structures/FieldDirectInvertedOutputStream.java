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
 * The Original Code is FieldDirectInvertedOutputStream.java
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original contributor)
 */
package org.terrier.structures;

import java.io.IOException;

import org.terrier.compression.BitOut;
import org.terrier.structures.postings.FieldIterablePosting;
import org.terrier.structures.postings.FieldPosting;
import org.terrier.structures.postings.IterablePosting;
import org.terrier.structures.postings.Posting;
/** Bit out class for writing a posting list with fields */
public class FieldDirectInvertedOutputStream extends DirectInvertedOutputStream {

	public FieldDirectInvertedOutputStream(BitOut out) {
		super(out);
	}

	public FieldDirectInvertedOutputStream(String filename)
			throws IOException {
		super(filename);
	}
	
	@Override
	public Class<? extends IterablePosting> getPostingIteratorClass()
	{
		return FieldIterablePosting.class;
	}

	@Override
	protected void writePostingNotDocid(Posting _p) throws IOException {
		super.writePostingNotDocid(_p);
		final FieldPosting p = (FieldPosting)_p;
		for(int fieldFrequency : p.getFieldFrequencies())
		{
			super.output.writeUnary(fieldFrequency+1);
		}
		//System.err.println("FieldDirectInvertedOutputStream: " + ArrayUtils.join(p.getFieldFrequencies(), ","));
	}

}

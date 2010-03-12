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
 * The Original Code is BasicPostingImpl.java
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original contributor)
 */
package org.terrier.structures.postings;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.WritableUtils;

public class BasicPostingImpl implements WritablePosting {

	protected int id = -1;
	protected int tf;

	public BasicPostingImpl() {
		super();
	}

	public BasicPostingImpl(int docid, int frequency) {
		super();
		id = docid;
		tf = frequency;
	}

	public int getId() {
		return id;
	}

	public int getFrequency() {
		return tf;
	}

	/** Reads the a single posting (not an iterable posting - use BitPostingIndex for that) */
	public void readFields(DataInput in) throws IOException {
		id = WritableUtils.readVInt(in);
		tf = WritableUtils.readVInt(in);
	}

	/** Writes the current posting (not an iterable posting - use DirectInvertedOutputStream for that).
	 * Compression using this method is not expected to be comparable to bit-level compression. */
	public void write(DataOutput out) throws IOException {
		WritableUtils.writeVInt(out, id);
		WritableUtils.writeVInt(out, tf);
	}
	
	public void setId(int _id) {
		id = _id;
	}

	public int getDocumentLength() {
		return 0;
	}
	
	public WritablePosting asWritablePosting()
	{
		return new BasicPostingImpl(id, tf);
	}
	
	public String toString()
	{
		return "(" + id + "," + tf + ")";
	}

}

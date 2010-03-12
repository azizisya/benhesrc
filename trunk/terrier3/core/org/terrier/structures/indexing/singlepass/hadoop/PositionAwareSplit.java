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
 * The Original Code is PositionAwareSplit.java
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original contributor)
 *   Richard McCreadie <richardm{a.}dcs.gla.ac.uk>
 */
package org.terrier.structures.indexing.singlepass.hadoop;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.mapred.InputSplit;
import org.terrier.utility.io.WrappedIOException;

/** A split implementation that knows which it is in the scheme of things
 * @author Richard McCreadie
 */
public class PositionAwareSplit<T extends InputSplit> implements InputSplit{

	/** the wrapped split */
	protected T split;
	/** the index of this split */
	protected int splitnum;
	
	/** Make a new split, for use in Writable serialization */
	public PositionAwareSplit() {
		splitnum=-1;
	}
	
	/** Make a new split with the specified attributs */
	public PositionAwareSplit(T _split, int _splitnum) {
		split = _split;
		splitnum = _splitnum;
	}

	/**
	 * Get the index of this split
	 * @return the splitnum
	 */
	public int getSplitIndex() {
		return splitnum;
	}

	/**
	 * Set the index of this split
	 * @param splitnum the splitnum to set
	 */
	public void setSplitIndex(int splitnum) {
		this.splitnum = splitnum;
	}

	/**
	 * Get the wrapped split
	 * @return the split
	 */
	public T getSplit() {
		return split;
	}

	/**
	 * Set the wrapped split
	 * @param split the split to set
	 */
	public void setSplit(T split) {
		this.split = split;
	}

	/** {@inheritDoc} */
	public long getLength() throws IOException {
		return split.getLength();
	}

	/** {@inheritDoc} */
	public String[] getLocations() throws IOException {
		return split.getLocations();
	}

	/** {@inheritDoc} */
	@SuppressWarnings("unchecked")
	public void readFields(DataInput in) throws IOException {
		try {
			final String className = in.readUTF();
			Class<?> c = Class.forName(className, false, this.getClass().getClassLoader());
			split = (T)c.newInstance();
			split.readFields(in);
			splitnum = in.readInt();
		} catch (Exception e) {
			throw new WrappedIOException("Error during the reading of fields of a new PositionAwareSplit", e);
		} 
	}

	/** {@inheritDoc} */
	public void write(DataOutput out) throws IOException {
		out.writeUTF(split.getClass().getName());
		split.write(out);
		out.writeInt(splitnum);
	}
	
	
	
}

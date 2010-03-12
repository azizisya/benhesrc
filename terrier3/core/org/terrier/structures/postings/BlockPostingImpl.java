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
 * The Original Code is BlockPostingImpl.java
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

import org.terrier.utility.ArrayUtils;

public class BlockPostingImpl extends BasicPostingImpl implements BlockPosting {
	int[] positions;
	public BlockPostingImpl() {
	}

	public BlockPostingImpl(int docid, int frequency, int[] _positions) {
		super(docid, frequency);
		positions = _positions;
	}
	
	public int[] getPositions() {
		return positions;
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		super.readFields(in);
		final int blockCount = WritableUtils.readVInt(in);
		positions = new int[blockCount]; 
		for(int i=0;i<blockCount;i++)
			positions[i] = WritableUtils.readVInt(in);
	}

	@Override
	public void write(DataOutput out) throws IOException {
		super.write(out);
		WritableUtils.writeVInt(out, positions.length);
		for(int pos : positions)
			WritableUtils.writeVInt(out, pos);
	}

	@Override
	public WritablePosting asWritablePosting()
	{
		int[] newPos = new int[positions.length];
		System.arraycopy(positions, 0, newPos, 0, positions.length);
		return new BlockPostingImpl(id, tf, newPos);
	}
	
	public String toString()
	{
		return "(" + id + "," + tf + ",B[" + ArrayUtils.join(positions, ",") + "])";
	}
}

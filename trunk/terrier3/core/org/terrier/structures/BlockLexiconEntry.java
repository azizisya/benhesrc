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
 * The Original Code is BlockLexiconEntry.java
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original contributor)
 */
package org.terrier.structures;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class BlockLexiconEntry extends BasicLexiconEntry implements BlockEntryStatistics {
	private static final long serialVersionUID = 1L;
	int blockCount;
	
	public static class Factory extends BasicLexiconEntry.Factory
	{
		public int getSize() {
			return super.getSize() + 4;
		}
		public LexiconEntry newInstance() {
			return new BlockLexiconEntry();
		}
	}
	
	public BlockLexiconEntry() {
		super();
	}
	
	public BlockLexiconEntry(int tid, int n_t, int TF, byte fileId, BitFilePosition offset, int _blockCount) {
		super(tid, n_t, TF, fileId, offset);
		blockCount = _blockCount;
	}
	public BlockLexiconEntry(int tid, int n_t, int TF, byte fileId, long _startOffset, byte _startBitOffset, int _blockCount) {
		super(tid, n_t, TF, fileId, _startOffset, _startBitOffset);
		blockCount = _blockCount;
	}
	public BlockLexiconEntry(int tid, int n_t, int TF, int _blockCount) {
		super(tid, n_t, TF);
		blockCount = _blockCount;
	}
	
	/** {@inheritDoc} */
	public int getBlockCount()
	{
		return blockCount;
	}
	
	@Override
	public void readFields(DataInput in) throws IOException {
		super.readFields(in);
		blockCount = in.readInt();
		
	}
	@Override
	public void write(DataOutput out) throws IOException {
		super.write(out);
		out.writeInt(blockCount);
	}
	
	/** {@inheritDoc} */
	@Override
	public void add(EntryStatistics le)
	{
		super.add(le);
		if (le instanceof BlockEntryStatistics)
			blockCount += ((BlockEntryStatistics)le).getBlockCount();
	}
	
	/** {@inheritDoc} */
	@Override
	public void subtract(EntryStatistics le)
	{
		super.subtract(le);
		if (le instanceof BlockEntryStatistics)
			blockCount -= ((BlockEntryStatistics)le).getBlockCount();
	}
	
}

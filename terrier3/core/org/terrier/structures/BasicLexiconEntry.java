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
 * The Original Code is BlockDirectIndex.java.
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk> (original author)
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
 */
package org.terrier.structures;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.terrier.structures.seralization.FixedSizeWriteableFactory;

/** Contains all the information about one entry in the Lexicon. 
  * Created to make thread-safe lookups in the Lexicon easier. */
public class BasicLexiconEntry extends LexiconEntry implements BitIndexPointer {
	private static final long serialVersionUID = 1L;

	public static class Factory implements FixedSizeWriteableFactory<LexiconEntry>
	{	
		public Factory(String s) {}
		public Factory() {}
		
		public int getSize() {
			//System.err.println("Value size is"+((3*4) + 8 + 1));
			return (3*4) + 8 + 1;
		}
		public LexiconEntry newInstance() {
			return new BasicLexiconEntry();
		}

	}
	
	/** the termid of this entry */
	public int termId;
	/** the number of document that this entry occurs in */
	public int n_t;
	/** the total number of occurrences of the term in the index */
	public int TF;
	/** the start offset of the entry in the inverted index */
	public long startOffset;
	/** the start bit offset of the entry in the inverted index */
	public byte startBitOffset;

	/** Create an empty LexiconEntry */
	public BasicLexiconEntry(){}

	/** Create a lexicon entry with the following information.
	  * @param tid the term id
	  * @param n_t the number of documents the term occurs in (document frequency)
	  * @param TF the total count of therm t in the collection
	  */
	public BasicLexiconEntry(int tid, int n_t, int TF)
	{
		this.termId = tid;
		this.n_t = n_t;
		this.TF = TF;
	}
	
	public BasicLexiconEntry(int tid, int n_t, int TF, byte fileId, long _startOffset, byte _startBitOffset) {
		this.termId = tid;
		this.n_t = n_t;
		this.TF = TF;
		this.startOffset = _startOffset;
		this.startBitOffset = _startBitOffset;
	}
	
	public BasicLexiconEntry(int tid, int n_t, int TF, byte fileId, BitFilePosition offset) {
		this.termId = tid;
		this.n_t = n_t;
		this.TF = TF;
		this.startOffset = offset.getOffset();
		this.startBitOffset = offset.getOffsetBits();
		this.startBitOffset += (byte)(fileId << FILE_SHIFT);
	}
	
	public void setStatistics(int n_t, int TF)
	{
		this.n_t = n_t;
		this.TF = TF;
	}

	/** increment this lexicon entry by another */
	public void add(EntryStatistics le)
	{
		this.n_t += le.getDocumentFrequency();
		this.TF  += le.getFrequency();
	}

	/** alter this lexicon entry to subtract another lexicon entry */
	public void subtract(EntryStatistics le)
	{
		this.n_t -= le.getDocumentFrequency();
		this.TF  -= le.getFrequency();
	}

	
	/** returns a string representation of this lexicon entry */	
	public String toString() {
		return "term"+ termId + " Nt=" + n_t + " TF=" + TF 
			+ " @{"+ this.getFileNumber() +" " + startOffset + " " + this.getOffsetBits()+"}";
	}

	public int getDocumentFrequency() {
		return n_t;
	}

	public int getFrequency() {
		return TF;
	}

	public int getTermId() {
		return termId;
	}

	public int getNumberOfEntries() {
		return n_t;
	}

	public byte getOffsetBits() {
		return (byte)(startBitOffset & BIT_MASK);
	}

	public long getOffset() {
		return startOffset;
	}
	
	public byte getFileNumber() {
		return (byte)((0xFF & startBitOffset) >> FILE_SHIFT);
	}
	
	public void setFileNumber(byte fileId)
	{
		startBitOffset = getOffsetBits();
		startBitOffset += (fileId << FILE_SHIFT);
	}
	
	public void setTermId(int newTermId)
	{
		termId = newTermId;
	}
	
	public void setOffset(long bytes, byte bits)
	{
		startOffset = bytes;
		byte fileId = this.getFileNumber();
		startBitOffset = bits;
		startBitOffset += (fileId << FILE_SHIFT);
	}
	
	public void setBitIndexPointer(BitIndexPointer pointer) {
		n_t = pointer.getNumberOfEntries();
		setOffset(pointer);
		startBitOffset += (byte)(pointer.getFileNumber() << FILE_SHIFT);
	}

	public void setOffset(BitFilePosition pos) {
		startOffset = pos.getOffset();
		startBitOffset = pos.getOffsetBits();
	}

	public void readFields(DataInput in) throws IOException {
		termId = in.readInt();
		TF = in.readInt();
		n_t = in.readInt();
		startOffset = in.readLong();
		startBitOffset = in.readByte();
	}

	public void write(DataOutput out) throws IOException {
		out.writeInt(termId);
		out.writeInt(TF);
		out.writeInt(n_t);
		out.writeLong(startOffset);
		out.writeByte(startBitOffset);
	}

	public void setNumberOfEntries(int n) {
		n_t = n;
	}

	public String pointerToString() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setPointer(Pointer p) {
		n_t = p.getNumberOfEntries();
		startOffset = ((BitIndexPointer)p).getOffset();
		startBitOffset = ((BitIndexPointer)p).getOffsetBits();
	}
}

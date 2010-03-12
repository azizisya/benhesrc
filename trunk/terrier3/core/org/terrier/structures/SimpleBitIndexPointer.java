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
 * The Original Code is SimpleBitIndexPointer.java
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

import org.apache.hadoop.io.Writable;

import org.terrier.structures.seralization.FixedSizeWriteableFactory;

public class SimpleBitIndexPointer implements BitIndexPointer, Writable {
	
	/** byte offset in file */
	long bytes;
	/** bottom 3 bits: bit offset in file, high 4 bits: file number */
	byte bits;
	/** number of entries in list */
	int entries;
	
	public static class Factory implements FixedSizeWriteableFactory<SimpleBitIndexPointer>
	{

		public int getSize() {
			return 8+1+4;
		}

		public SimpleBitIndexPointer newInstance() {
			return new SimpleBitIndexPointer();
		}
		
	}
	
	public SimpleBitIndexPointer() {
		bytes = 0;
		bits = 0;
		entries = 0;		
	}
	
	public SimpleBitIndexPointer(byte fileId, long byteOffset, byte bitOffset, int numEntries)
	{
		bytes = byteOffset;
		bits = bitOffset;
		bits += fileId << FILE_SHIFT;
		entries = numEntries;
	}
	
	public SimpleBitIndexPointer(long byteOffset, byte bitOffset, int numEntries)
	{
		this((byte)0, byteOffset, bitOffset, numEntries);
	}
	
	public int getNumberOfEntries() {
		return entries;
	}

	public void setNumberOfEntries(int count) {
		entries = count;
	}

	public byte getOffsetBits() {
		return (byte)(bits & BIT_MASK);
	}
	
	public byte getFileNumber() {
		//System.err.println("Get Fileid=" + (byte)((bits & 0xFF)>> FILE_SHIFT));
		return (byte)((bits & 0xFF)>> FILE_SHIFT);
	}
	
	public void setFileNumber(byte fileId)
	{
		bits = getOffsetBits();
		//System.err.println("bits="+bits);
		bits += (fileId << FILE_SHIFT);
		//System.err.println("fileId="+fileId+", new bits var=" + bits);
	}

	public long getOffset() {
		return bytes;
	}

	public void setOffset(long _bytes, byte _bits) {
		bytes = _bytes;
		byte fileId = this.getFileNumber();
		bits = _bits;
		bits += fileId << FILE_SHIFT;
	}

	public void setBitIndexPointer(BitIndexPointer pointer) {
		entries = pointer.getNumberOfEntries();
		bits = 0;
		setOffset(pointer);
		bits += pointer.getFileNumber() << FILE_SHIFT;
	}

	public void setOffset(BitFilePosition pos) {
		bytes = pos.getOffset();
		bits = pos.getOffsetBits();
	}

	public void readFields(DataInput arg0) throws IOException {
		bytes = arg0.readLong();
		bits = arg0.readByte();
		entries = arg0.readInt();
	}

	public void write(DataOutput arg0) throws IOException {
		arg0.writeLong(bytes);
		arg0.writeByte(bits);
		arg0.writeInt(entries);
	}

	public String toString()
	{
		StringBuilder s = new StringBuilder();
		s.append(entries);
		s.append("@{");
		s.append(this.getFileNumber());
		s.append(',');
		s.append(bytes);
		s.append(',');
		s.append(this.getOffsetBits());
		s.append('}');
		return s.toString();
	}

	public String pointerToString() {
		return toString();
	}

	public void setPointer(Pointer p) {
		entries = p.getNumberOfEntries();
		bytes = ((BitIndexPointer)p).getOffset();
		bits = (byte)(BIT_MASK | ((BitIndexPointer)p).getOffsetBits());
	}
}

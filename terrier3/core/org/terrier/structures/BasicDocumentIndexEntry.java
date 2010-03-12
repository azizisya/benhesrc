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
 * The Original Code is BasicDocumentIndexEntry.java
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

import org.terrier.structures.seralization.FixedSizeWriteableFactory;

public class BasicDocumentIndexEntry extends DocumentIndexEntry
{
	public static class Factory implements FixedSizeWriteableFactory<DocumentIndexEntry>
	{
		public int getSize() {
			return 4 + 4 + 8 + 1;
		}
		public DocumentIndexEntry newInstance() {
			return new BasicDocumentIndexEntry();
		}
	}
	
	public BasicDocumentIndexEntry() {	}
	
	public BasicDocumentIndexEntry(DocumentIndexEntry in)
	{
		doclength = in.getDocumentLength();
		entries = in.getNumberOfEntries();
		bytes = in.getOffset();
		bits = in.getOffsetBits();
		bits += in.getFileNumber() << FILE_SHIFT;
	}
	
	public BasicDocumentIndexEntry(int length, BitIndexPointer pointer)
	{
		doclength = length;
		bytes = pointer.getOffset();
		bits = pointer.getOffsetBits();
		bits += pointer.getFileNumber() << FILE_SHIFT;
		entries = pointer.getNumberOfEntries();
	}
	
	public BasicDocumentIndexEntry(int length, byte fileId, long byteOffset, byte bitOffset, int numberOfTerms)
	{
		doclength = length;
		bytes = byteOffset;
		bits = bitOffset;
		bits += fileId << FILE_SHIFT;
		entries = numberOfTerms;
	}
	
	public void readFields(DataInput in) throws IOException {
		doclength = in.readInt();
		bytes = in.readLong();
		bits = in.readByte();
		entries = in.readInt();
	}

	public void write(DataOutput out) throws IOException {
		out.writeInt(doclength);
		out.writeLong(bytes);
		out.writeByte(bits);
		out.writeInt(entries);
	}

	public void setBitIndexPointer(BitIndexPointer pointer) {
		entries = pointer.getNumberOfEntries();
		setOffset(pointer);
	}

	public void setOffset(BitFilePosition pos) {
		bytes = pos.getOffset();
		bits = pos.getOffsetBits();
	}

	public void setNumberOfEntries(int n) {
		entries = n;
	}

	public String pointerToString() {
		return "@{"+bytes+ "," + bits + "}";
	}

	public void setPointer(Pointer p) {
		bytes = ((BitIndexPointer)p).getOffset();
		bits = ((BitIndexPointer)p).getOffsetBits();
	}
	
}

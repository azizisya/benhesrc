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
 * The Original Code is DocumentIndexEntry.java
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original contributor)
 */
package org.terrier.structures;

import org.apache.hadoop.io.Writable;

public abstract class DocumentIndexEntry implements BitIndexPointer, Writable
{
	int entries;
	int doclength;
	long bytes;
	byte bits;
	
	
	public int getDocumentLength()
	{
		return doclength;
	}
	
	public void setDocumentLength(int l)
	{
		doclength = l;
	}
	
	
	public int getNumberOfEntries() {
		return entries;
	}


	public byte getOffsetBits() {
		return (byte) (bits & BIT_MASK);
	}


	public long getOffset() {
		return bytes;
	}
	
	public byte getFileNumber() {
		return (byte) ( (0xFF & bits) >> FILE_SHIFT);
	}
	
	public void setFileNumber(byte fileId)
	{
		bits = getOffsetBits();
		bits += (fileId << FILE_SHIFT);
	}


	public void setOffset(long _bytes, byte _bits) {
		bytes = _bytes;
		byte fileId = this.getFileNumber();
		bits = _bits;
		bits += (fileId << FILE_SHIFT);
	}
	
	public String toString()
	{
		StringBuilder s = new StringBuilder();
		s.append(doclength);
		s.append(' ');
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
	
}

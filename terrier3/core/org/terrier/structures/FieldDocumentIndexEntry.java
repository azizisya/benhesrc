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
 * The Original Code is FieldDocumentIndexEntry.java
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

import org.terrier.utility.ArrayUtils;

/** A document index entry for use with fields */
public class FieldDocumentIndexEntry extends BasicDocumentIndexEntry {

	

	public static class Factory extends BasicDocumentIndexEntry.Factory
	{	
		protected int fieldCount;
		public Factory(int fieldCount)
		{
			super();
			this.fieldCount = fieldCount;
		}
		
		public Factory(String _fieldCount)
		{
			this(Integer.parseInt(_fieldCount));
		}

		@Override
		public int getSize() {
			return super.getSize() + fieldCount * 4;
		}

		@Override
		public DocumentIndexEntry newInstance() {
			return new FieldDocumentIndexEntry(fieldCount);
		}
		
	}
	
	protected int[] fieldLengths;
	
	//seldom used constructor
	public FieldDocumentIndexEntry()
	{
		fieldLengths = new int[0];
	}
	
	public FieldDocumentIndexEntry(DocumentIndexEntry die)
	{
		super(die);
		if (die instanceof FieldDocumentIndexEntry)
			this.fieldLengths = ((FieldDocumentIndexEntry)die).getFieldLengths();
	}
	
	public FieldDocumentIndexEntry(int fieldCount) 
	{
		this.fieldLengths = new int[fieldCount];
	}
	
	public int[] getFieldLengths()
	{
		return this.fieldLengths;
	}
	
	public void setFieldLengths(int[] f_lens)
	{
		this.fieldLengths = f_lens;
	}
	
	@Override
	public void readFields(DataInput in) throws IOException {
		super.readFields(in);
		final int l = fieldLengths.length;
		for(int i=0;i<l;i++)
			fieldLengths[i] = in.readInt();
	}

	@Override
	public void write(DataOutput out) throws IOException {
		super.write(out);
		for(int field_l : fieldLengths)
			out.writeInt(field_l);
	}
	
	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append(doclength);
		s.append(' ');
		s.append("F[");
		s.append(ArrayUtils.join(fieldLengths, ","));
		s.append("] ");
		s.append(entries);
		s.append("@{");
		s.append(bytes);
		s.append(',');
		s.append(bits);
		s.append('}');
		return s.toString();
	}

}

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
 * The Original Code is FieldIterablePosting.java
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

import org.terrier.compression.BitIn;
import org.terrier.structures.DocumentIndex;
import org.terrier.structures.FieldDocumentIndex;
import org.terrier.structures.FieldDocumentIndexEntry;
import org.terrier.utility.ArrayUtils;

/** A posting iterator for field postings */
public class FieldIterablePosting extends BasicIterablePosting implements FieldPosting {

	final int fieldCount;
	final int[] fieldFrequencies;
	final boolean doiIsFieldDocumentIndex;
	final FieldDocumentIndex fdoi;
	
	public FieldIterablePosting(int fieldCount) {
		super();
		this.fieldCount = fieldCount;
		this.fieldFrequencies = new int[fieldCount];
		this.doiIsFieldDocumentIndex = false;
		this.fdoi = null;
	}

	public FieldIterablePosting(BitIn fileReader, int entries, DocumentIndex _doi, int fieldCount) throws IOException {
		super(fileReader, entries, _doi);
		this.fieldCount = fieldCount;
		this.fieldFrequencies = new int[fieldCount];
		if (doiIsFieldDocumentIndex = _doi instanceof FieldDocumentIndex)
		{
			fdoi = (FieldDocumentIndex)super.doi;
		} else {
			fdoi = null;
		}
	}

	public int[] getFieldFrequencies() {
		return fieldFrequencies;
	}

	@Override
	public int next() throws IOException {
		if (numEntries-- == 0)
			return EOL;
		id = bitFileReader.readGamma() + id;
		tf = bitFileReader.readUnary();
		for(int i = 0;i<fieldCount;i++)
		{
			fieldFrequencies[i] = bitFileReader.readUnary()-1;
		}
		return id;
	}

	public int[] getFieldLengths() {
		if (doiIsFieldDocumentIndex)
		{
			try{
				return fdoi.getFieldLengths(id);
			} catch (IOException ioe) {
				System.err.println("Problem looking for doclength for document "+ id);
				ioe.printStackTrace();
				return new int[0];
			}
		}
		else
		{
			FieldDocumentIndexEntry fdie = null;
			try{
				fdie = ((FieldDocumentIndexEntry)doi.getDocumentEntry(id));
			} catch (IOException ioe) {
				//TODO log?
				System.err.println("Problem looking for doclength for document "+ id);
				ioe.printStackTrace();
				return new int[0];
			}
			return fdie.getFieldLengths();
		}
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		super.readFields(in);
		final int l = WritableUtils.readVInt(in);
		//fieldFrequencies = new int[l];
		for(int i=0;i<l;i++)
			fieldFrequencies[i] = WritableUtils.readVInt(in);
	}

	@Override
	public void write(DataOutput out) throws IOException {
		super.write(out);
		WritableUtils.writeVInt(out, fieldFrequencies.length);
		for(int field_f : fieldFrequencies)
			WritableUtils.writeVInt(out, field_f);
	}
	

	@Override
	public WritablePosting asWritablePosting()
	{	
		FieldPostingImpl fbp = new FieldPostingImpl(fieldCount);
		fbp.id = id;
		fbp.tf = tf;
		System.arraycopy(fieldFrequencies, 0, fbp.fieldFrequencies, 0, fieldCount);
		return fbp;
	}

	@Override
	public String toString()
	{
		return "(" + id + "," + tf + ",F[" + ArrayUtils.join(fieldFrequencies, ",") + "])";
	}
}

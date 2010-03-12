/*
 * Terrier - Terabyte Retriever
 * Webpage: http://ir.dcs.gla.ac.uk/terrier
 * Contact: terrier{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * http://www.gla.uk
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
 * The Original Code is WritableByteArray.java.
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Richard McCreadie <richardm{a.}dcs.gla.ac.uk> (original author)
 */
package org.terrier.structures.indexing.singlepass.hadoop;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

import org.apache.hadoop.io.Writable;

/**
 * Represents a Writable Posting List. Contains the
 * Posting List, DOcument Frequency and Term Frequency.
 * @author Richard McCreadie
 * @since 2.2
 * @version $Revision: 1.2 $
 *
 */
//TODO: this class should support return a BitIn instead of a byte array?
//TODO: this class should support copying the array to a temporary file instead of loading into memory
public class WritableByteArray implements Writable{
	
	/** Posting List */
	protected byte[] array;
	/** Document Frequency */
	protected int DocumentFreq;
	/** Term Frequency */
	protected int TermFreq;
	/** Size of the Posting List */
	protected int arraylength;
	
	/**
	 * Empty Constructor
	 */
	public WritableByteArray() {
		DocumentFreq = 0;
	}
	
	/**
	 * Constructor - with Document Frequency
	 * @param c - Document Frequency
	 */
	public WritableByteArray(int c) {
		DocumentFreq = c;
	}
	
	/**
	 * Constructor - with Document Frequency and Term Frequency
	 * @param c - Document Frequency
	 * @param c2 - Term Frequency
	 */
	public WritableByteArray(int c, int c2) {
		DocumentFreq = c;
		TermFreq = c2;
	}
	
	/**
	 * Factory Method
	 * @param b - Posting List
	 * @return a newly created object
	 * @throws IOException
	 */
	public static WritableByteArray create_Hadoop_WritableByteArray(byte[] b) throws IOException {
		WritableByteArray w = new WritableByteArray();
		w.setArray(b);
		return w;
	}
	
	/**
	 * Factory Method
	 * @param b - Posting List
	 * @param c - Document Frequency
	 * @return a newly created object
	 * @throws IOException
	 */
	public static WritableByteArray create_Hadoop_WritableByteArray(byte[] b, int c) throws IOException {
		WritableByteArray w = new WritableByteArray(c);
		w.setArray(b);
		return w;
	}
	
	/**
	 * Factory Method
	 * @param b - Posting List
	 * @param c - Document Frequency
	 * @param c2 - Term Frequency
	 * @return a newly created Indexing_WritableByteArray
	 * @throws IOException
	 */
	public static WritableByteArray create_Hadoop_WritableByteArray(byte[] b, int c, int c2) throws IOException {
		WritableByteArray w = new WritableByteArray(c, c2);
		w.setArray(b);
		return w;
	}

	/** Read this object from the input stream 'in' */
	public void readFields(DataInput arg0) throws IOException {
		arraylength = arg0.readInt();
		DocumentFreq = arg0.readInt();
		TermFreq = arg0.readInt();
		array = new byte[arraylength];
		arg0.readFully(array);
		
	}

	/** Write this object to the output stream 'out' */
	public void write(DataOutput arg0) throws IOException {
		arg0.write(array.length);
		arg0.writeInt(DocumentFreq);
		arg0.writeInt(TermFreq);
		for(byte b: array)
			arg0.writeByte(b);
	}
	
	public void setArray(byte[] b) {
		array = b;
	}
	
	public byte[] getArray() {
		return array;
	}
	
	public String toString() {
		return Arrays.toString(array);
	}
	
	public int getDocumentFreq() {
		return DocumentFreq;
	}

	public void setDocumentFreq(int DocumentFreq) {
		this.DocumentFreq = DocumentFreq;
	}
	

	public int getTermFreq() {
		return TermFreq;
	}

	public void setTermFrequency(int TermFreq) {
		this.TermFreq = TermFreq;
	}
	
	

	

}

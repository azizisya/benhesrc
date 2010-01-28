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
 * The Original Code is MapEmittedPostingList.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Richard McCreadie <richardm{a.}dcs.gla.ac.uk> (original author)
 *   
 */
package uk.ac.gla.terrier.structures.indexing.singlepass.hadoop;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;


/**
 * Sub-Class of WritableByteArray with additional
 * information about which Map and Flush it came from
 * @author Richard McCreadie
 * @version $Revision: 1.1 $
 * @since 2.2
 */
public class MapEmittedPostingList extends WritableByteArray{
	
	/** The Map Number */
	protected String Map;
	/** The Flush Number */
	protected int Run;
	
	/**
	 * Constructor
	 * @param map - Map task id
	 * @param run - Flush Number
	 * @param c - Document Frequency
	 * @param c2 - Term Frequency
	 */
	public MapEmittedPostingList (String map, int run, int c, int c2) {
		super(c,c2);
		Map = map;
		Run =run;
	}
	
	/**
	 * Super Constructor
	 * @param c - Document Frequency
	 * @param c2 - Term Frequency
	 */
	public MapEmittedPostingList (int c, int c2) {
		super(c,c2);
	}
	
	/**
	 * Empty Constructor
	 */
	public MapEmittedPostingList () {
		super();
	}
	
	/**
	 * Factory Method
	 * @param mapTaskID - Map Number
	 * @param flushNo - Flush Number
	 * @param postingList - Posting List
	 * @param DocumentFreq - Document Frequency
	 * @param TermFreq - Term Frequency
	 * @return a newly created Indexing_WritableRunPostingData
	 */
	public static MapEmittedPostingList create_Hadoop_WritableRunPostingData (String mapTaskID, int flushNo, byte[] postingList, int DocumentFreq, int TermFreq) {
		MapEmittedPostingList w = new MapEmittedPostingList(mapTaskID, flushNo, DocumentFreq, TermFreq);
		w.setArray(postingList);
		return w;
	}
	/**
	 * Super Factory Method
	 * @param postingList - Posting List
	 * @param DocumentFreq - Document Frequency
	 * @param TermFreq - Term Frequency
	 * @return a newly created Indexing_WritableRunPostingData
	 */
	public static MapEmittedPostingList create_Hadoop_WritableRunPostingData (byte[] postingList, int DocumentFreq, int TermFreq) {
		MapEmittedPostingList w = new MapEmittedPostingList(DocumentFreq, TermFreq);
		w.setArray(postingList);
		return w;
		
	}
	
	/**
	 * Returns the Map & Flush Number
	 */
	public String toString() {
		return "MapNo="+Map+ ",FlushNo="+Run;
	}

	public String getMap() {
		return Map;
	}

	public void setMap(String map) {
		Map = map;
	}
	
	public int getRun() {
		return Run;
	}

	public void setRun(int run) {
		Run = run;
	}

	/**
	 * Reads this object from the input stream 'in' 
	 */
	public void readFields(DataInput arg0) throws IOException {
		arraylength = arg0.readInt();
		Map = arg0.readUTF();
		Run = arg0.readInt();
		DocumentFreq = arg0.readInt();
		TermFreq = arg0.readInt();
		array = new byte[arraylength];
		arg0.readFully(array);
		//System.err.println("DEBUG: Finished Read, ArrayL:"+arraylength+" RunNo:"+Run+" DocF:"+DocumentFreq+" TermF:"+TermFreq+" Buffer:"+array.toString());
		
	}
	
	/**
	 * Reads this object from the input stream 'in' apart from the
	 * array. 
	 * @param arg0
	 * @throws IOException
	 */
	public void readFieldsMinusArray(DataInput arg0) throws IOException {
		arraylength = arg0.readInt();
		Map = arg0.readUTF();
		Run = arg0.readInt();
		DocumentFreq = arg0.readInt();
		TermFreq = arg0.readInt();
		array = new byte[1];
		//System.err.println("DEBUG: Finished Read, ArrayL:"+arraylength+" RunNo:"+Run+" DocF:"+DocumentFreq+" TermF:"+TermFreq+" Buffer:"+array.toString());
		
	}

	/** Write this object to the output stream 'out' */
	public void write(DataOutput arg0) throws IOException {
		arg0.writeInt(array.length);
		arg0.writeUTF(Map);
		arg0.writeInt(Run);
		arg0.writeInt(DocumentFreq);
		arg0.writeInt(TermFreq);
		arg0.write(array);
		//System.err.println("DEBUG: Finished Write, ArrayL:"+array.length+" RunNo:"+Run+" DocF:"+DocumentFreq+" TermF:"+TermFreq+" Buffer:"+array.toString());
	}
	
	public void printArray() {
		System.err.print("DEBUG: Posting Buffer Contents: ");
		for (int i = 0; i<array.length; i=i+1) {
			System.err.print(array[i]);
		}
		System.err.println();
	}

}

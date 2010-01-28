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
 * The Original Code is MapEmittedTerm.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Richard McCreadie <richardm{a.}dcs.gla.ac.uk> (original author)
 *   
 */
package uk.ac.gla.terrier.structures.indexing.singlepass.hadoop;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.RawComparator;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapred.TaskID;

/**
 * Represents a term emitted during indexing time. Each term also has a 
 * the map number and flush number that is was emitted from.
 * @author Richard McCreadie
 * @version $Revision: 1.1 $
 * @since 2.2
 */
public class MapEmittedTerm implements WritableComparable<MapEmittedTerm> {
	
	/**
	 * Comparator for MapEmittedTerm objects - order only by Term.
	 */
	public static class TermComparator implements RawComparator<MapEmittedTerm> {
		/**
		 * Compares Hadoop_TextPlus objects by comparison of the
		 * Text variables.
		 */
		public int compare(MapEmittedTerm a, MapEmittedTerm b)
		{
			return a.getText().compareTo(b.getText());
		}

		/**
		 * Raw comparison on text objects
		 * NOT USED
		 */
		public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2) 
		{
			DataInputStream b1S = new DataInputStream(new ByteArrayInputStream(b1, s1, l1));
			DataInputStream b2S = new DataInputStream(new ByteArrayInputStream(b2, s2, l2));
			try {
				MapEmittedTerm tempT = new MapEmittedTerm();
				MapEmittedTerm tempT2 = new MapEmittedTerm();
				tempT.readFields(b1S);
				tempT2.readFields(b2S);
				b1S.close();
				b2S.close();
				int value = tempT.getText().compareTo(tempT2.getText());
				return value;
			} catch (IOException e) {
				System.err.println("IO Exception during compare");
				return 0;
			}
		}

	}
	
	/**
	 * Raw Comparator class to compare MapEmittedTerm objects
	 * stage 1. (Order by term, then by map number, then by flush
	 * number)
	 * @author Richard McCreadie and Craig Macdonald
	 * @since 2.2
	 * @version $Revision: 1.1 $
	 */
	public static class TermMapFlushComparator implements RawComparator<MapEmittedTerm> {
		
		protected MapEmittedTerm tempT = new MapEmittedTerm();
		protected MapEmittedTerm tempT2 = new MapEmittedTerm();
		
		public int compare(MapEmittedTerm a, MapEmittedTerm b) {
			throw new Error("Unsupported method Indexing_CompareTextPlusKey.compare(Indexing_TextPlus,Indexing_TextPlus) was called");
			//richard's documentation say that this method is not used	
		}

		/**
		 * Compare by term (bit comparison on Text object) then by map number (int)
		 * then by flush number (int).
		 */
		public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2) {
			// Convert to streams so that the read methods can be used
			DataInputStream b1S = new DataInputStream(new ByteArrayInputStream(b1, s1, l1));
			DataInputStream b2S = new DataInputStream(new ByteArrayInputStream(b2, s2, l2));
			try {
				// Read in the TextPlus Objects
				tempT.readFields(b1S);
				tempT2.readFields(b2S);
				b1S.close();
				b2S.close();
				// Do Comparison Text
				int value = tempT.getText().compareTo(tempT2.getText());
				if (value != 0)
					return value;
				// If same do Comparison on map task id
				value = TaskID.forName( tempT.getMap() ).compareTo( TaskID.forName(tempT2.getMap()) );
				if (value != 0)
					return value;
				//lastly check the flush numbers
				return tempT.getFlush()-tempT2.getFlush();
			} catch (IOException e) {
				return 0;
			}
		}

	}
	
	/** The Map this Term was processed from */
	protected String mapTaskID;
	/** The Flush number this term was from */
	protected int flushNumber;
	/** The Term */
	protected Text text=null;
	
	/**
	 * Empty Constructor
	 */
	public MapEmittedTerm() {
		
	}
	
	/**
	 * Constructor
	 * @param s - Term
	 * @param _mapTaskID - Map Number
	 * @param _flushNumber - Flush Number
	 */
	public MapEmittedTerm(String s, String _mapTaskID, int _flushNumber) {
		mapTaskID = _mapTaskID;
		flushNumber = _flushNumber;
		text = new Text(s);
	}
	
	/**
	 * Factory Method
	 * @param s - Term
	 * @param a - Map Number
	 * @param b - Flush Number
	 * @return a newly created Indexing_TextPlus
	 */
	public static MapEmittedTerm create_TextPlus(String s, String a, int b) {
		MapEmittedTerm temp = new MapEmittedTerm();
		temp.setMap(a);
		temp.setFlush(b);
		temp.setText(new Text(s));
		return temp;
	}

	public String getMap() {
		return mapTaskID;
	}

	public void setMap(String id) {
		mapTaskID = id;
	}

	public int getFlush() {
		return flushNumber;
	}

	public void setFlush(int flush) {
		flushNumber = flush;
	}

	public Text getText() {
		return text;
	}

	public void setText(Text text) {
		this.text = text;
	}

	/**
	 * Reads in this object from the Input Stream 'in'
	 */
	public void readFields(DataInput in) throws IOException {
		mapTaskID = in.readUTF();
		flushNumber = in.readInt();
		text = new Text();
		text.readFields(in);
		
	}

	/**
	 * Writes this object to the Output Stream 'out'
	 */
	public void write(DataOutput out) throws IOException {
		out.writeUTF(mapTaskID);
		out.writeInt(flushNumber);
		text.write(out);
	}

	/**
	 * Text Comparator on the Term contained in this object
	 */
	public int compareTo(MapEmittedTerm o) {
		return this.getText().compareTo(o.getText());
	}

}

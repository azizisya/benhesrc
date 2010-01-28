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
 * The Original Code is MapData.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Richard McCreadie <richardm{a.}dcs.gla.ac.uk> (original author)
 *   
 */
package uk.ac.gla.terrier.structures.indexing.singlepass.hadoop;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.LinkedList;

/**
 * Storage class for information about each Map. 
 * Stores the number of the Map, the number of documents processed
 * by the map and the number of documents stored in each flush of the
 * map.
 * @author Richard McCreadie
 * @since 2.2
 * @version $Revision: 1.1 $
 */
public class MapData {
	
	/** TaskID of the Map */
	protected String mapTaskID;
	/** Number of Documents Processed by the Map */
	protected int numMapDocs;
	/** Number of Documents in each flush of the map */
	protected LinkedList<Integer> flushDocSizes = new LinkedList<Integer>();
	
	/**
	 * Constructor - Loads the Map Information from the DataInputStream Provided
	 * @param in - Stream of the Map data file
	 */
	public MapData(DataInputStream in) throws IOException{
		super();
		mapTaskID = in.readUTF();
		int flushSize;
		while ((flushSize = in.readInt()) != -1)
		{
			flushDocSizes.add(flushSize);
		}
		numMapDocs = in.readInt();
		System.err.printf("map %s had %d docs, with %d flushes\n", mapTaskID, numMapDocs, flushDocSizes.size());
	}

	public String getMap() {
		return mapTaskID;
	}
	public int getMapDocs() {
		return numMapDocs;
	}
	public void setMapDocs(int runDocs) {
		this.numMapDocs = runDocs;
	}
	/** Contains one element, for each run (aka flush) outputted by this map. 
	 * The element is the number of documents covered by all previous runs in that map.
	 */
	public LinkedList<Integer> getFlushDocSizes() {
		return flushDocSizes;//size of each run in documents
	}
}

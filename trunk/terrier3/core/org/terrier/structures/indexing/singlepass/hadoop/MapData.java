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
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Richard McCreadie <richardm{a.}dcs.gla.ac.uk> (original author)
 *   
 */
package org.terrier.structures.indexing.singlepass.hadoop;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.LinkedList;

import org.apache.hadoop.mapred.TaskID;
import org.apache.log4j.Logger;

/**
 * Storage class for information about each Map. 
 * Stores the number of the Map, the number of documents processed
 * by the map and the number of documents stored in each flush of the
 * map.
 * @author Richard McCreadie
 * @since 2.2
 * @version $Revision: 1.2 $
 */
public class MapData implements Comparable<MapData>{
	
	protected static Logger logger = Logger.getLogger(MapData.class);
	
	/** TaskID of the Map */
	protected String mapTaskID;
	/** Number of Documents Processed by the Map */
	protected int numMapDocs;
	/** Number of Documents in each flush of the map */
	protected LinkedList<Integer> flushDocSizes = new LinkedList<Integer>();
	/** The Split number **/
	protected int splitnum;
	protected int int_mapTaskId;
	
	/**
	 * Constructor - Loads the Map Information from the DataInputStream Provided
	 * @param in - Stream of the Map data file
	 */
	public MapData(DataInputStream in) throws IOException{
		super();
		mapTaskID = in.readUTF();
		int_mapTaskId = TaskID.forName(mapTaskID).getId();
		int flushSize;
		while ((flushSize = in.readInt()) != -1)
		{
			flushDocSizes.add(flushSize);
		}
		numMapDocs = in.readInt();
		splitnum = in.readInt();
		logger.info("map "+mapTaskID+" processed split "+splitnum+" which had "+numMapDocs+" docs, with "+flushDocSizes.size()+" flushes\n");
	}

	public String getMap() {
		return mapTaskID;
	}
	
	public int getMapId()
	{
		return int_mapTaskId;
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

	public int compareTo(MapData o) {
		return splitnum-o.splitnum;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof MapData))
			return false;
		return this.splitnum == ((MapData)obj).splitnum;
	}

	@Override
	public int hashCode() {
		return splitnum;
	}

	/**
	 * @return the splitnum
	 */
	public int getSplitnum() {
		return splitnum;
	}
	
	
}

/*
 * Smooth - Smoothing term frequency normalisation
 * Webpage: http://ir.dcs.gla.ac.uk/smooth
 * Contact: ben{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * Information Retrieval Group
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
 * The Original Code is OnelineTRECQuery.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
 */
package org.terrier.structures;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

import org.terrier.structures.TRECQuery;
import org.terrier.utility.ApplicationSetup;

/**
 * This class is used for reading the queries 
 * from TREC topic files.
 * @author Craig Macdonald
 * @version $Revision: 1.1 $
 */
public class OnelineTRECQuery extends TRECQuery{
	/** The queries in the topic files.*/
	protected String[] queries;
	
	/** The query identifiers in the topic files.*/
	protected String[] query_ids;
	/** The index of the queries.*/
	protected int index;
	/**
	 * Extracts and stores all the queries from a query file.
	 * @param queryfilename String the name of a file containing topics.
	 * @param vecStringQueries Vector a vector containing the 
	 *        queries as strings.
	 * @param vecStringIds Vector a vector containing the query 
	 *        identifiers as strings.
	 */
	public boolean extractQuery(String queryfilename, Vector vecStringQueries, Vector vecStringIds){
		boolean rtr = false;
		try {
			BufferedReader br;
			File queryFile = new File(queryfilename);
			if (!(queryFile.exists() && queryFile.canRead())) {
				System.err.println("The topics file " + queryfilename + " does not exist, or it cannot be read.");
				return false;
			} else {
				
				if (queryfilename.toLowerCase().endsWith("gz")) {
					br = new BufferedReader(new InputStreamReader(
							new GZIPInputStream(new FileInputStream(queryfilename))
					//,"UTF-8"
					));
				} else {
					br = new BufferedReader(new InputStreamReader(
							new FileInputStream(queryfilename)
					//,"UTF-8"
					));
				}
				String line = null;
				while((line = br.readLine()) != null){
				
					if(line.length() ==0)
						continue;

					//int FirstSpace = line.indexOf(' ');
					String queryID = ""+System.currentTimeMillis();
					//String query = line.substring(FirstSpace+1).replaceAll("\\.","");
					String query = line.replaceAll("\\.","");
					vecStringQueries.add(query);
					vecStringIds.add(queryID);
					rtr = true;
				}
				//after processing each query file, close the BufferedReader
				br.close();
			}
		}catch (IOException ioe) {
			System.err.println(
				"Input/Output exception while performing the matching. Stack trace follows.");
			ioe.printStackTrace();
			System.exit(1);
		}
		return rtr;
	}
	
	
	/** 
	 * Constructs an instance of TRECQuery,
	 * that reads and stores all the queries from
	 * the files specified in the trec.topics.list file. */
	public OnelineTRECQuery() {
		this.extractQuery();
		this.index = 0;
	}
	
	/** 
	 * Constructs an instance of TRECQuery that
	 * reads and stores all the queries from a 
	 * the specified query file.
	 * @param queryfile File the file containing the queries.
	 */
	public OnelineTRECQuery(File queryfile){
		this(queryfile.getName());
	}
	
	/** 
	 * Constructs an instance of TRECQuery that
	 * reads and stores all the queries from a 
	 * file with the specified filename.
	 * @param queryfilename String the name of the file containing 
	 *        all the queries.
	 */	
	public OnelineTRECQuery(String queryfilename){
		Vector vecStringQueries = new Vector();
		Vector vecStringQueryIDs = new Vector();
		this.extractQuery(queryfilename, vecStringQueries, vecStringQueryIDs);
		this.queries = (String[]) vecStringQueries.toArray(new String[0]);
		this.query_ids = (String[]) vecStringQueryIDs.toArray(new String[0]);	
		this.index = 0;
	}
	
	/** 
	 * Extracts and stores all the queries from 
	 * the topic files, specified in the file
	 * with default name <tt>trec.topics.list</tt>. 
	 */
	protected void extractQuery() {
		try {
			//open the query file
			File addressQuery =
				new File(ApplicationSetup.TREC_TOPICS_LIST);
			BufferedReader addressQueryFile =
				new BufferedReader(new FileReader(addressQuery));
			String queryFilename;
			Vector vecStringQueries = new Vector();
			Vector vecStringQueryIDs = new Vector();
			int fileCount = 0;
			while ((queryFilename = addressQueryFile.readLine()) != null) {
				if (queryFilename.startsWith("#") || queryFilename.equals(""))
                    continue;	
				System.err.println("Extracting queries from "+queryFilename);
				fileCount++;
				extractQuery(queryFilename, vecStringQueries, vecStringQueryIDs);				
			}
			if (fileCount ==0)
			{
				System.err.println("No topic files found in "+addressQuery.getName()+"  - please check");
			}
			this.queries = (String[]) vecStringQueries.toArray(new String[0]);
			this.query_ids = (String[]) vecStringQueryIDs.toArray(new String[0]);
			addressQueryFile.close();
		} catch (IOException ioe) {
			System.err.println(
				"Input/Output exception while performing the matching. Stack trace follows.");
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	/** 
	 * Returns the index of the last obtained query.
	 * @return int the index of the last obtained query. 
	 */
	public int getIndexOfCurrentQuery() {
		return index - 1;
	}
	
	/** 
	 * Returns the number of the queries read from the
	 * processed topic files. 
	 * @return int the number of topics contained in the 
	 *         processed topic files.
	 */
	public int getNumberOfQueries() {
		return queries.length;
	}
	
	/**
	* Return the query for the given query number.
	* @return String the string representing the query.
	* @param queryNo String The number of a query.
	*/
	public String getQuery(String queryNo) {
		for (int i = 0; i < query_ids.length; i++)
			if (query_ids[i].equals(queryNo))
				return queries[i];
		return null;
	}
	/** 
	 * Test if there are more queries to process.
	 * @return boolean true if there are more queries
	 *         to process, otherwise returns false.
	 */
	public boolean hasMoreQueries() {
		if (index == queries.length)
			return false;
		return true;
	}
	
	/** 
	 * Returns a query. 
	 * @return String the next query.
	 */
	public String nextQuery() {
		if (index == queries.length)
			return null;
		return queries[index++];
	}
	/** 
	 * Returns the query identifier of the last query
	 * fetched, or the first one, if none has been
	 * fetched yet.
	 * @return String the query number of a query.
	 */
	public String getQueryId() {
		return query_ids[index == 0 ? 0 : index-1];
	}
	
	/**
	* Returns the queries in an array of strings
	* @return String[] an array containing the strings that
	*         represent the queries.
	*/
	public String[] toArray() {
		return (String[]) queries.clone();
	}
	/**
	* Resets the query index.
	*/
	public void reset() {
		this.index = 0;
	}
}

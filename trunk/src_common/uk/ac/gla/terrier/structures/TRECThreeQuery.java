/*
 * Terrier - Terabyte Retriever 
 * Webpage: http://ir.dcs.gla.ac.uk/terrier 
 * Contact: terrier{a.}dcs.gla.ac.uk
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
 * The Original Code is TRECQuery.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk> (original author) 
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.structures;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Vector;
import uk.ac.gla.terrier.indexing.TRECFullTokenizer;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.TagSet;
/**
 * This class is used for reading the queries 
 * from TREC topic files.
 * @author Ben He &amp; Craig Macdonald
 * @version $Revision: 1.1 $
 */
public class TRECThreeQuery {
	/** The queries in the topic files.*/
	protected String[] queries;
	
	/** The query identifiers in the topic files.*/
	protected String[] query_ids;
	/** The index of the queries.*/
	protected int index;
	
	protected String titleField;
	
	protected String descField;
	
	protected String narrField;
	
	/**
	 * Extracts and stores all the queries from a query file.
	 * @param queryfilename String the name of a file containing topics.
	 * @param vecStringQueries Vector a vector containing the 
	 *        queries as strings.
	 * @param vecStringIds Vector a vector containing the query 
	 *        identifiers as strings.
	 */
	public void extractQuery(String queryfilename, Vector vecStringQueries, Vector vecStringIds){
		try {
			BufferedReader br;
			File queryFile = new File(queryfilename);
			if (!(queryFile.exists() && queryFile.canRead())) {
				System.err.println("The topics file " + queryfilename + " does not exist, or it cannot be read.");
			} else {
				
				if (queryfilename.toLowerCase().endsWith("gz")) {
					br = new BufferedReader(new InputStreamReader(
							new GZIPInputStream(new FileInputStream(queryfilename))));
				} else {
					br = new BufferedReader(new InputStreamReader(
							new FileInputStream(queryfilename)));
				}
				//BufferedReader queries = new BufferedReader(new FileReader(queryfile));
				//the query tokenizer
				TRECFullTokenizer queryTokenizer =
					new TRECFullTokenizer(
							new TagSet(TagSet.TREC_QUERY_TAGS),
							new TagSet(TagSet.EMPTY_TAGS),
							br);
				queryTokenizer.setIgnoreMissingClosingTags(true);
				while (!queryTokenizer.isEndOfFile()) {
					String docnoToken = null;
					StringBuffer query = new StringBuffer();
					while (!queryTokenizer.isEndOfDocument()) {
						String token = queryTokenizer.nextToken();
						if (token == null
								|| token.length() == 0
								|| queryTokenizer.inTagToSkip())
							continue;
						
						if (queryTokenizer.inDocnoTag()) {
							//The tokenizer is constructed from the trimmed version of the contents
							//of the query number tag, so that the last token extracted from it, is
							//always the query number, and not an empty string
							StringTokenizer docnoTokens =
								new StringTokenizer(token.trim(), " ");
							while (docnoTokens.hasMoreTokens())
								docnoToken = docnoTokens.nextToken().trim();
						} else if (queryTokenizer.inTagToProcess()) {
							// Removed the code that checks if "description" and 
							// "narrative" appear in "desc" and "narr", respective. 
							// THIS WILL HURT THE RETRIEVAL PERFORMANCE. Therefore, 
							// it is recommended to add these words in the stopword 
							// list.
							query.append(token);
							query.append(' ');
							
						}
					}
					if (query.length() == 0)
						continue;
					vecStringQueries.add(query.toString());
					vecStringIds.add(docnoToken);
					queryTokenizer.nextDocument();
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
	}
	
	
	/** 
	 * Constructs an instance of TRECQuery,
	 * that reads and stores all the queries from
	 * the files specified in the trec.topics.list file. */
	public TRECThreeQuery() {
		this.extractQuery();
		this.index = 0;
	}
	
	/** 
	 * Constructs an instance of TRECQuery that
	 * reads and stores all the queries from a 
	 * the specified query file.
	 * @param queryfile File the file containing the queries.
	 */
	public TRECThreeQuery(File queryfile){
		this(queryfile.getName());
	}
	
	/** 
	 * Constructs an instance of TRECQuery that
	 * reads and stores all the queries from a 
	 * file with the specified filename.
	 * @param queryfilename String the name of the file containing 
	 *        all the queries.
	 */	
	public TRECThreeQuery(String queryfilename){
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


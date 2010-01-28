/*
 * Terrier - Terabyte Retriever 
 * Webpage: http://ir.dcs.gla.ac.uk/terrier 
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
 * The Original Code is TRECQuery.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk> (original author) 
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.structures;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.indexing.TRECFullTokenizer;
import uk.ac.gla.terrier.indexing.TRECFullUTFTokenizer;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.TagSet;
/**
 * This class is used for reading the queries 
 * from TREC topic files.
 * <p><b>Properties:</b></p>
 * <ul>
 * <li><tt>trecquery.ignore.desc.narr.name.tokens</tt> - should the token DESCRIPTION and NARRATIVE in the desc and narr fields be ignored? Defaluts to true</li>
 * <li><tt>string.use_utf</tt> - set to true if query files contain non basic ASCII characters. Defaults to false.</li>
 * <li><tt>trec.encoding</tt> - use to set the encoding of TREC topic files. Defaults to the systems default encoding.</li>
 * </ul>
 * @author Ben He &amp; Craig Macdonald
 * @version $Revision: 1.1 $
 */
public class TRECQuery {
	/** The logger used for this class */
	protected static final Logger logger = Logger.getRootLogger();

	/** Value of <tt>trecquery.ignore.desc.narr.name.tokens</tt> - should the token DESCRIPTION and NARRATIVE in the desc and narr fields be ignored? Defaluts to true? */
	protected static final boolean IGNORE_DESC_NARR_NAME_TOKENS = 
		Boolean.parseBoolean(ApplicationSetup.getProperty("trecquery.ignore.desc.narr.name.tokens","true"));

	/** Value of <tt>string.use_utf</tt>. If set to true, TRECFullUTFTokenizer instead of TRECFullTokenizer is used to
	  * tokenize the topics file(s). */	
	protected final boolean UTF = Boolean.parseBoolean(ApplicationSetup.getProperty("string.use_utf", "false"));

	/** Encoding to be used to open all files. */
	protected static String desiredEncoding = ApplicationSetup.getProperty("trec.encoding", Charset.defaultCharset().name());

	/** The topic files used in this object */
	protected String[] topicFiles;

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
	 *		queries as strings.
	 * @param vecStringIds Vector a vector containing the query 
	 *		identifiers as strings.
	 * @return boolean true if some queries were successfully extracted.
	 */
	public boolean extractQuery(String queryfilename, Vector<String> vecStringQueries, Vector<String> vecStringIds){
		boolean gotSome = false;
		try {
			BufferedReader br;
			if (! Files.exists(queryfilename) || ! Files.canRead(queryfilename)) {
				logger.error("The topics file " + queryfilename + " does not exist, or it cannot be read.");
				return false;
			} else {
				br = Files.openFileReader(queryfilename,desiredEncoding);
				TRECFullTokenizer queryTokenizer = UTF	
					? new TRECFullUTFTokenizer(
							new TagSet(TagSet.TREC_QUERY_TAGS),
							new TagSet(TagSet.EMPTY_TAGS),
							br)
					: new TRECFullTokenizer(
							new TagSet(TagSet.TREC_QUERY_TAGS),
							new TagSet(TagSet.EMPTY_TAGS),
							br);
				queryTokenizer.setIgnoreMissingClosingTags(true);
				while (!queryTokenizer.isEndOfFile()) {
					String docnoToken = null;
					StringBuilder query = new StringBuilder();
					boolean seenDescriptionToken = ! IGNORE_DESC_NARR_NAME_TOKENS;
					boolean seenNarrativeToken = ! IGNORE_DESC_NARR_NAME_TOKENS;
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
							if (!seenDescriptionToken && queryTokenizer
							  .currentTag()
								.toUpperCase()
							   .equals("DESC")
							   && token.toUpperCase().equals("DESCRIPTION"))
							   continue;
							  if (!seenNarrativeToken && queryTokenizer
							   .currentTag()
							   .toUpperCase()
							   .equals("NARR")
							   && token.toUpperCase().equals("NARRATIVE"))
							   continue;	
							query.append(token);
							query.append(' ');
							
						}
					}
					if (query.length() == 0)
						continue;
					vecStringQueries.add(query.toString());
					vecStringIds.add(docnoToken);
					queryTokenizer.nextDocument();
					gotSome = true;
				}
				//after processing each query file, close the BufferedReader
				br.close();
			}
		}catch (IOException ioe) {
			logger.error("Input/Output exception while extracting queries from the topic file named "+queryfilename, ioe);
		}
		return gotSome;
	}
	
	
	/** 
	 * Constructs an instance of TRECQuery,
	 * that reads and stores all the queries from
	 * the files specified in the trec.topics.list file. */
	public TRECQuery() {
		this.extractQuery();
		this.index = 0;
	}
	
	/** 
	 * Constructs an instance of TRECQuery that
	 * reads and stores all the queries from a 
	 * the specified query file.
	 * @param queryfile File the file containing the queries.
	 */
	public TRECQuery(File queryfile){
		this(queryfile.getName());
	}
	
	/** 
	 * Constructs an instance of TRECQuery that
	 * reads and stores all the queries from a 
	 * file with the specified filename.
	 * @param queryfilename String the name of the file containing 
	 *		all the queries.
	 */	
	public TRECQuery(String queryfilename){
		Vector<String> vecStringQueries = new Vector<String>();
		Vector<String> vecStringQueryIDs = new Vector<String>();
		if (this.extractQuery(queryfilename, vecStringQueries, vecStringQueryIDs))
			this.topicFiles = new String[]{queryfilename};
		this.queries = vecStringQueries.toArray(new String[0]);
		this.query_ids = vecStringQueryIDs.toArray(new String[0]);	
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
			BufferedReader addressQueryFile = Files.openFileReader(ApplicationSetup.TREC_TOPICS_LIST);
			ArrayList<String> parsedTopicFiles = new ArrayList<String>(1);
			String queryFilename;
			Vector<String> vecStringQueries = new Vector<String>();
			Vector<String> vecStringQueryIDs = new Vector<String>();
			int fileCount = 0;
			while ((queryFilename = addressQueryFile.readLine()) != null) {
				if (queryFilename.startsWith("#") || queryFilename.equals(""))
					continue;	
				logger.info("Extracting queries from "+queryFilename);
				fileCount++;
				boolean rtr = extractQuery(queryFilename, vecStringQueries, vecStringQueryIDs);
				if (rtr)
					parsedTopicFiles.add(queryFilename);
			}
			if (fileCount ==0)
			{
				logger.error("No topic files found in "+ApplicationSetup.TREC_TOPICS_LIST  +"  - please check");
			}
			if (fileCount > 0 && parsedTopicFiles.size() == 0)
			{
				logger.error("Topic files were specified, but non could be parsed correctly to obtain any topics."
					+ "Check you have the correct topic files specified, and that TrecQueryTags properties are correct.");
			}
			this.queries = (String[]) vecStringQueries.toArray(new String[0]);
			this.query_ids = (String[]) vecStringQueryIDs.toArray(new String[0]);
			this.topicFiles = (String[]) parsedTopicFiles.toArray(new String[0]);
			//logger.info("found files ="+ this.topicFiles.length);
			addressQueryFile.close();
		} catch (IOException ioe) {
			logger.error("Input/Output exception while performing the matching.", ioe);
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
	 *		 processed topic files.
	 */
	public int getNumberOfQueries() {
		return queries.length;
	}
	
	/** Returns the filenames of the topic files from which the
	  * queries were extracted */
	public String[] getTopicFilenames() {
		return this.topicFiles;
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
	 *		 to process, otherwise returns false.
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

	/** Returns the query ids 
	  * @return String array containing the query ids.
	  * @since 2.2 */
	public String[] getQueryIds()
	{
		return query_ids;
	}	
	
	/**
	* Returns the queries in an array of strings
	* @return String[] an array containing the strings that
	*		 represent the queries.
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

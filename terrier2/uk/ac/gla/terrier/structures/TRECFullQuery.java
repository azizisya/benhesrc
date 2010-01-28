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
 * The Original Code is TRECFullQuery.java.
 *
 * The Original Code is Copyright (C) 2004-2007 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk> (original author) 

 */
package uk.ac.gla.terrier.structures;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TObjectIntHashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.indexing.TRECFullTokenizer;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.TagSet;
/**
 * This class is used for reading the queries 
 * from TREC topic files.
 * @author Ben He
 * @version $Revision: 1.1 $
 */
public class TRECFullQuery extends TRECQuery{
    /** The logger used for this class */
    protected static Logger logger = Logger.getRootLogger();

	protected static boolean IGNORE_DESC_NARR_NAME_TOKENS = 
		Boolean.parseBoolean(ApplicationSetup.getProperty("TRECFullQuery.ignore.desc.narr.name.tokens","true"));
	
	protected Vector<String> queryidSet;

	/** The topic files used in this object */
	protected String[] topicFiles;
	
	protected String[] queryTags;
	
	protected TObjectIntHashMap<String> queryTagMap;

	/** A hashmap from queryid to the strings in different accepted topic fields. */
	protected THashMap<String,Vector<String>> queryidFieldsMap;
	
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
	public boolean extractQuery(String queryfilename){
		boolean gotSome = false;
		try {
			BufferedReader br;
			File queryFile = new File(queryfilename);
			if (!(queryFile.exists() && queryFile.canRead())) {
				logger.error("The topics file " + queryfilename + " does not exist, or it cannot be read.");
				return false;
			} else {
				br = Files.openFileReader(queryfilename);	
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
					StringBuilder[] query = new StringBuilder[queryTags.length];
					for (int i=0; i<query.length; i++)
						query[i] = new StringBuilder();
					boolean emptyQuery = true;
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
							  // see in which tag the term occurs
							  /*if (!queryTagMap.contains(queryTokenizer.currentTag()))
								  continue;*/
							  int pos = queryTagMap.get(queryTokenizer.currentTag().toUpperCase());
							  if (pos>0){
								  query[pos-1].append(token);
								  query[pos-1].append(' ');
								  emptyQuery = false;
							  }
							
						}
					}
					if (emptyQuery)
						continue;
					Vector<String> queryVector = new Vector<String>();
					for (int i=0; i<queryTags.length; i++)
						queryVector.add(query[i].toString());
					queryidFieldsMap.put(docnoToken, queryVector);
					queryidSet.add(docnoToken);
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
	 * Constructs an instance of TRECFullQuery,
	 * that reads and stores all the queries from
	 * the files specified in the trec.topics.list file. */
	public TRECFullQuery() {
		this.initialise();
		this.extractQuery();
		this.index = 0;
	}
	
	protected void initialise(){
		TagSet tagset = new TagSet(TagSet.TREC_QUERY_TAGS);
		String[] tags = tagset.getTagsToProcess().replaceAll(",", " ").split(" ");
		queryTagMap = new TObjectIntHashMap<String>();
		
		queryTags = new String[tags.length-2];
		int counter = 0;
		for (int i=0; i<tags.length; i++)
			if (!tagset.isDocTag(tags[i])&&!tagset.isIdTag(tags[i])){
				queryTags[counter++] = tags[i].toUpperCase();
				queryTagMap.put(tags[i].toUpperCase(), counter);
			}
		queryidSet = new Vector<String>();
		queryidFieldsMap = new THashMap<String,Vector<String>>();
		tagset = null;
	}
	
	/** 
	 * Constructs an instance of TRECFullQuery that
	 * reads and stores all the queries from a 
	 * the specified query file.
	 * @param queryfile File the file containing the queries.
	 */
	public TRECFullQuery(File queryfile){
		this(queryfile.getName());
	}
	
	/** 
	 * Constructs an instance of TRECFullQuery that
	 * reads and stores all the queries from a 
	 * file with the specified filename.
	 * @param queryfilename String the name of the file containing 
	 *		all the queries.
	 */	
	public TRECFullQuery(String queryfilename){
		this.initialise();
		if (this.extractQuery(queryfilename))
			this.topicFiles = new String[]{queryfilename};
		this.query_ids = (String[])queryidSet.toArray(new String[queryidSet.size()]);	
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
			BufferedReader addressQueryFile = Files.openFileReader(addressQuery);
			ArrayList<String> parsedTopicFiles = new ArrayList<String>(1);
			String queryFilename;
			int fileCount = 0;
			while ((queryFilename = addressQueryFile.readLine()) != null) {
				if (queryFilename.startsWith("#") || queryFilename.equals(""))
					continue;	
				logger.info("Extracting queries from "+queryFilename);
				fileCount++;
				boolean rtr = extractQuery(queryFilename);
				if (rtr)
					parsedTopicFiles.add(queryFilename);
			}
			if (fileCount ==0)
			{
				logger.error("No topic files found in "+addressQuery.getName()+"  - please check");
			}
			if (fileCount > 0 && parsedTopicFiles.size() == 0)
			{
				logger.error("Topic files were specified, but non could be parsed correctly to obtain any topics."
					+ "Check you have the correct topic files specified, and that TRECQueryTags properties are correct.");
			}
			//this.queries = (String[]) vecStringQueries.toArray(new String[0]);
			this.query_ids = (String[])queryidSet.toArray(new String[queryidSet.size()]);	
			this.index = 0;
			this.topicFiles = (String[]) parsedTopicFiles.toArray(new String[0]);
			logger.info("found files ="+ this.topicFiles.length);
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
		return queryidSet.size();
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
		Vector<String> query = queryidFieldsMap.get(queryNo);
		if (query==null)
			return null;
		String queryString = "";
		for (int i=0; i<query.size(); i++)
			queryString = queryString+query.get(i)+" ";
		return queryString.trim();
	}
	/** 
	 * Test if there are more queries to process.
	 * @return boolean true if there are more queries
	 *		 to process, otherwise returns false.
	 */
	public boolean hasMoreQueries() {
		if (index == queryidSet.size())
			return false;
		return true;
	}
	
	/** 
	 * Returns a query. 
	 * @return String the next query.
	 */
	public String nextQuery() {
		if (index == queryidSet.size())
			return null;
		return getQuery(query_ids[index++]);
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
	
	public String[] getQueryids(){
		return (String[])query_ids.clone();
	}
	
	/**
	* Returns the queries in an array of strings
	* @return String[] an array containing the strings that
	*		 represent the queries.
	*/
	public String[] toArray() {
		String[] queries = new String[queryidSet.size()];
		for (int i=0; i<queryidSet.size(); i++)
			queries[i]=getQuery(query_ids[i]);
		return queries;
	}
	/**
	* Resets the query index.
	*/
	public void reset() {
		this.index = 0;
	}
}

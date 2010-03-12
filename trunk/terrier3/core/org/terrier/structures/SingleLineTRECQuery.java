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
 * The Original Code is SingleLineTRECQuery.java.
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original author)
 *   Ben He <ben{a.}dcs.gla.ac.uk>
 */
package org.terrier.structures;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Vector;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.Files;

/** This class can be used to extract batch queries from a simpler format than the regular SGML TREC format.
  * In particular, this class reads queries, one per line, verbatim from the specified file(s).
  * Empty lines and lines starting with # are ignored. Queries are not tokenised by this class, and in general
  * pased verbatim to the query parser.
  * Moreover, it assumes that the first token on each line is the query Id. This can be controlled
  * by the properties <tt>SingleLineTRECQuery.queryid.exists</tt> (default true). Trailing colons in the query Id
  * are removed (aka TREC single line format from the Million Query track).
  * Use this class by specifying <tt>trec.topics.parser=SingleLineTRECQuery</tt> and running
  * TRECQuerying or TrecTerrier as normal. 
  * <p><b>Properties:</b>
  * <ul>
  * <li><tt>SingleLineTRECQuery.queryid.exists</tt> - does the line start with a query Id? </li>
  * <li><tt>SingleLineTRECQuery.periods.allowed</tt> (defaults to false). By default, periods are removed from 
  * the query. Stop this by setting this property to true.</li>
  * </ul>
  */
public class SingleLineTRECQuery extends TRECQuery
{
	/** Constructor - default */
	public SingleLineTRECQuery() {
		super();
	}

	/** Reads queries from the specified file */
	public SingleLineTRECQuery(File queryfile){
		super(queryfile);
	}

	/** Reads queries from the specified filename */
	public SingleLineTRECQuery(String queryfilename){
		super(queryfilename);
	}

	/** Extracts queries from the specified filename, adding their contents to vecStringQueries and the
	  * corresponding query ids to vecStringIds. 
	  * @return true if some queries were successfully read */
	public boolean extractQuery(String queryfilename, Vector<String> vecStringQueries, Vector<String> vecStringIds)
	{
		boolean gotSome = false;
		final boolean QueryLineHasQueryID = Boolean.parseBoolean(ApplicationSetup.getProperty("SingleLineTRECQuery.queryid.exists","true"));
		final boolean QueryHasPeriods = Boolean.parseBoolean(ApplicationSetup.getProperty("SingleLineTRECQuery.periods.allowed", "false"));
		logger.info("Extracting queries from "+queryfilename + " - queryids "+QueryLineHasQueryID);
		try {
			BufferedReader br;
			if (! Files.exists(queryfilename))
			{
				logger.error("The topics file " + queryfilename + " does not exist, or it cannot be read.");
				return false;
			}
			br = Files.openFileReader(queryfilename, "UTF-8");	

			String line = null;
			int queryCount =0;
			while((line = br.readLine()) != null)
			{
				line = line.trim();
				if (line.startsWith("#"))
				{
					//comment encountered - skip line
					continue;
				}
				queryCount++;
				String queryID;
				String query;
				if (QueryLineHasQueryID)
				{
					final int queryIdEnd = minOver0(new int[]{ line.indexOf(' '), line.indexOf('\t'), line.indexOf(":")});
					if (queryIdEnd == -1)
					{
						//no query Id found
						continue;
					}
					queryID = line.substring(0,queryIdEnd);
					query = line.substring(queryIdEnd+1);
					/*if ()
					String parts[] = line.split("\\s+|:");
					queryID = parts[0];
					StringBuilder query_tmp = new StringBuilder();
					for(int i=1;i<parts.length;i++)
					{
						query_tmp.append(parts[i]);
						query_tmp.append(' ');
					}
					query = query_tmp.toString();*/
					if (! QueryHasPeriods)
						query = query.replaceAll("\\.", " ");
				}
				else
				{
					query = line;
					if (! QueryHasPeriods)
						query = query.replaceAll("\\.", " ");
					queryID = ""+queryCount;
				}
				vecStringQueries.add(query);
				vecStringIds.add(queryID);
				gotSome = true;
				logger.debug("Extracted queryID "+queryID+" "+query);
			}

		} catch (IOException ioe) {
			logger.error("IOException while extracting queries: ",ioe);	
			return gotSome;
		}
		logger.info("Extracted "+ vecStringQueries.size() + " queries");
		return gotSome;
	}
	
	static int minOver0(final int[] a)
	{
		int min = Integer.MAX_VALUE;
		for(int i : a)
			if (i != -1 && i < min)
				min = i;
		return min;
	}
}

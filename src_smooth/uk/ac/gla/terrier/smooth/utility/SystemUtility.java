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
 * The Original Code is SystemUtility.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.smooth.utility;

import uk.ac.gla.terrier.structures.*;
import uk.ac.gla.terrier.utility.*;
import uk.ac.gla.terrier.statistics.Statistics;
import java.io.*;
import java.util.*;

/**
 * This class implements some supplemental functionalities for the system.
 * @author Ben He <ben{a.}dcs.gla.ac.uk>
 * @version $Revision: 1.1 $
 */
public class SystemUtility {
	/**
	 * Write a string into a file. The content in the file will be replaced with
	 * the string. If the file does not exist, the method creates a new file
	 * and write the string into the file.
	 * @param fOutput The file to write the string.
	 * @param str The string to write in the file.
	 * @throws IOException
	 */
    public static void writeString(File fOutput, String str) throws IOException {
    	BufferedWriter bw = new BufferedWriter(new FileWriter(fOutput));
    	bw.write(str);
    	bw.close();
    }
    /**
     * Get the average query length in the trec topic file(s) specified in
     * the trec.topic.list.
     * @return The average query length.
     */
    public static double getAverageQueryLength(){
    	TRECQuery queries = new TRECQuery();
    	double[] avql = new double[queries.getNumberOfQueries()];
    	int counter = 0;
    	while (queries.hasMoreQueries()){
    		avql[counter++] = (new StringTokenizer(queries.nextQuery())).countTokens();
    	}
    	return Statistics.mean(avql);
    }  
	/**
	 * This method checks to which type the TREC queries belong.
	 * It returns an integer. Note that this method works only with
	 * English queries. To cope with other languages, modify the
	 * "narr", "desc" and "title" accordingly. For example, in a German
	 * query, if the tag of the title field is "DE-title", replace the
	 * "title" in the code with "DE-title", and do the same the "desc"
	 * and the "narr".
	 * @return The type that the TREC queries belong to. It returns
	 * an integer value. A returned value of 1 indicates that it
	 * is title-only, 2 indicates description-only, and 7 indicates
	 * title+description+narrative.  
	 */
	public static int queryType() {
		int[] type = new int[3];// 0: narr; 1: desc; 2: title
		Arrays.fill(type, 0);
		
		TagSet tags = new TagSet(TagSet.TREC_QUERY_TAGS);
		String tag = "narr";
		if (tags.isTagToProcess(tag.toUpperCase()))
			type[0] = 1;
			
		tag = "desc";
		if (tags.isTagToProcess(tag.toUpperCase()))
			type[1] = 1;
			
		tag = "title";
		if (tags.isTagToProcess(tag.toUpperCase()))
			type[2] = 1;

		return type[0] * 4 + type[1] * 2 + type[2] * 1;
	}
}

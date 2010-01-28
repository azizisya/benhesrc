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
 * The Original Code is AppPrediction.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.smooth.applications;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import uk.ac.gla.terrier.querying.Manager;
import uk.ac.gla.terrier.smooth.matching.BufferedMatching;
import uk.ac.gla.terrier.smooth.structures.BasicQuery;
import uk.ac.gla.terrier.smooth.structures.QueryFeatures;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.TRECQuery;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Rounding;

/**
 * This class provides functionalities for query performance prediction.
 * @author Ben He <ben{a.}dcs.gla.ac.uk>
 * @version $Revision: 1.1 $
 */
public class AppPrediction {
	/**
	 * Get performance predictors of the real queries. 
	 * @return An array of QueryFeatures containing the performance predictors of
	 * the real queries.
	 */
	public QueryFeatures[] getQf(){
		Index index = Index.createIndex();
		BufferedMatching matching = new BufferedMatching(index);
		TRECQuery queries = new TRECQuery();
		QueryFeatures[] qfs = new QueryFeatures[queries.getNumberOfQueries()];
		Manager pipe = new Manager(index);
		int counter = 0;
		while (queries.hasMoreQueries()){
			String queryString = queries.nextQuery();
			String queryid = queries.getQueryId();
			System.err.println("processing query " + queryid);
			BasicQuery query = new BasicQuery(queryString, queryid, pipe);
			qfs[counter] = new QueryFeatures(query, matching);
			counter++;
		}
		return qfs;
	}
	/**
	 * Save the performance predictors in a given file. Queries are
	 * those specified in the trec.topics file.
	 * @param fOut The output file of the performance predictors.
	 */
	public void printQfs(File fOut){
		QueryFeatures[] qfs = this.getQf();
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < qfs.length; i++){
			double[] qfVector = qfs[i].getFeatureVector();
			buffer.append(qfs[i].getQueryId());
			for (int j = 0; j < qfVector.length; j++)
				buffer.append(" " + Rounding.toString(qfVector[j], 6));
			buffer.append(ApplicationSetup.EOL);
		}
		try{
			BufferedWriter bw = new BufferedWriter(new FileWriter(fOut));
			bw.write(buffer.toString());
			bw.close();
		}
		catch(IOException ioe){
			ioe.printStackTrace();
		}
		System.err.println("Query performance predictors saved in file " +
				fOut.getPath());
	}
	/**
	 * The main method that starts the application.
	 * @param args The arguments.
	 */
	public static void main(String[] args) {
		AppPrediction app = new AppPrediction();
		File fOut = new File(ApplicationSetup.TREC_RESULTS, args[1]);
		app.printQfs(fOut);
	}
}

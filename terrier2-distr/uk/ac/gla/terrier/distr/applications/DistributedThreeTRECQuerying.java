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
 * The Original Code is DistributedThreeTRECQuerying.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk> 
 */
package uk.ac.gla.terrier.distr.applications;

import java.io.PrintWriter;
import java.rmi.RMISecurityManager;

import uk.ac.gla.terrier.applications.ThreeTRECQuerying;
import uk.ac.gla.terrier.distr.querying.DistributedThreeManager;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.querying.SearchRequest;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.TerrierTimer;

/**
 * This is the main class for running retrieval on three fields in
 * a distributed setting.
 * @author Ben He(ben@dcs.gla.ac.uk)
 * @version $Revision: 1.7 $
 */
public class DistributedThreeTRECQuerying extends ThreeTRECQuerying {
	/**
	 * The default constructor.
	 *
	 */
	public DistributedThreeTRECQuerying (){
	}
	
	protected void createManager(){
		queryingManager = new DistributedThreeManager();
	}
	
	/**
	 * Overide this method because we don't want to load the indices here.
	 */
	protected void loadIndex(){
	
	}
	

	
	/** 
	 * Prints the results for the given search request, 
	 * using the specified destination. 
	 * @param pw PrintWriter the destination where to save the results.
	 * @param q SearchRequest the object encapsulating the query and the results.
	 */
	public void printResults(PrintWriter pw, SearchRequest q) {
		ResultSet set = q.getResultSet();
		String[] docnos = set.getMetaItems("docnos");
		double[] scores = set.getScores();
		System.err.println("set size: " + set.getExactResultSize());
		int minimum = Integer.parseInt(ApplicationSetup.getProperty("matching.retrieved_set_size", "1000"));
		//if the minimum number of documents is more than the
		//number of documents in the results, aw.length, then
		//set minimum = aw.length
		if (minimum > set.getExactResultSize())
			minimum = set.getExactResultSize();
		System.err.println("minimum: " + minimum);
		String iteration = ITERATION + "0";
		
		String queryIdExpanded = q.getQueryID() + " " + iteration + " ";
		String methodExpanded = " " + method + "\n";
		StringBuffer sbuffer = new StringBuffer();
		//even though we have single-threaded usage
		//in mind, the synchronized makes code faster
		//since each sbuffer.append() call does not
		//try to obtain a lock.
		synchronized(sbuffer) {
			//the results are ordered in desceding order
			//with respect to the score. 
			int start = 0;
			int end = minimum;
			
			end = set.getExactResultSize();
			
			for (int i = start; i < end; i++) {
				if (scores[i]<=0){
					continue;
				}
				sbuffer.append(queryIdExpanded);
				sbuffer.append(docnos[i]);
				sbuffer.append(" ");
				sbuffer.append(i);
				sbuffer.append(" ");
				sbuffer.append(scores[i]);
				sbuffer.append(methodExpanded);
			}
			pw.write(sbuffer.toString());
		}
		pw.flush();
	}
	/**
	 * The main method of the class.
	 */
	public static void main(String[] args){
		TerrierTimer timer = new TerrierTimer();
		timer.start();
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new RMISecurityManager());
		}
		

		
		//sets up the system and updates collection statistics
		//accross the servers
		DistributedThreeTRECQuerying querying = new DistributedThreeTRECQuerying();
		if (args.length!=0){
			// create the global lexicon
			if (args[0].equalsIgnoreCase("-r"))
				((DistributedThreeManager)querying.queryingManager).rebuildLexicon();
		}
		else
			querying.processQueries();
		timer.setBreakPoint();
		System.out.println("Time elapsed: " + timer.toStringMinutesSeconds());
	}
}

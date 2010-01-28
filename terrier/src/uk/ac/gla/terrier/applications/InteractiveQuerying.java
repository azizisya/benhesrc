/*
 * Terrier - Terabyte Retriever
 * Webpage: http://ir.dcs.gla.ac.uk/terrier
 * Contact: terrier{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * http://www.ac.gla.uk
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
 * The Original Code is InteractiveQuerying.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Gianni Amati <gba{a.}fub.it> (original author)
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
 *   Ben He <ben{a.}dcs.gla.ac.uk>
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.applications;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.querying.Manager;
import uk.ac.gla.terrier.querying.SearchRequest;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.utility.ApplicationSetup;
/**
 * This class performs interactive querying at the command line. It asks
 * for a query on Standard Input, and then displays the document IDs that
 * match the given query.
 * <p><b>Properties:</b>
 * <ul><li><tt>interactive.model</tt> - which weighting model to use, defaults to PL2</li>
 * <li><tt>interactive.matching</tt> - which Matching class to use, defaults to Matching</li>
 * <li><tt>interactive.manager</tt> - which Manager class to use, defaults to Matching</li>
 * </ul>
 * @author Gianni Amati, Vassilis Plachouras, Ben He, Craig Macdonald
 * @version $Revision: 1.1 $
 */
public class InteractiveQuerying {
	/** The logger used */
	protected static Logger logger = Logger.getRootLogger();
	
	/** Change to lowercase? */
	protected final static boolean lowercase = Boolean.parseBoolean(ApplicationSetup.getProperty("lowercase", "true"));
	protected boolean verbose = true;
	/** the number of processed queries. */
	protected int matchingCount = 0;
	/** The file to store the output to.*/
	protected PrintWriter resultFile = new PrintWriter(System.out);
	/** The name of the manager object that handles the queries. Set by property <tt>trec.manager</tt>, defaults to Manager. */
	protected String managerName = ApplicationSetup.getProperty("interactive.manager", "Manager");
	/** The query manager.*/
	protected Manager queryingManager;
	/** The weighting model used. */
	protected String wModel = ApplicationSetup.getProperty("interactive.model", "PL2");
	/** The matching model used.*/
	protected String mModel = ApplicationSetup.getProperty("interactive.matching", "Matching");
	/** The data structures used.*/
	protected Index index;
	/** The maximum number of presented results. */
	protected static int RESULTS_LENGTH = 
		Integer.parseInt(ApplicationSetup.getProperty("interactive.output.format.length", "1000"));
	
	/** A default constructor initialises the index, and the Manager. */
	public InteractiveQuerying() {
		loadIndex();
		createManager();		
	}

	/**
	* Create a querying manager. This method should be overriden if
	* another matching model is required.
	*/
	protected void createManager(){
		try{
		if (managerName.indexOf('.') == -1)
			managerName = "uk.ac.gla.terrier.querying."+managerName;
		queryingManager = (Manager) (Class.forName(managerName)
			.getConstructor(new Class[]{Index.class})
			.newInstance(new Object[]{index}));
		} catch (Exception e) {
			logger.error("Problem loading Manager ("+managerName+"): ",e);	
		}
	}
	
	/**
	* Loads index(s) from disk.
	*
	*/
	protected void loadIndex(){
		long startLoading = System.currentTimeMillis();
		index = Index.createIndex();
		if(index == null)
		{
			logger.fatal("Failed to load index. Perhaps index files are missing");
		}
		long endLoading = System.currentTimeMillis();
		if (logger.isInfoEnabled())
			logger.info("time to intialise index : " + ((endLoading-startLoading)/1000.0D));
	}
	/**
	 * Closes the used structures.
	 */
	public void close() {
		index.close();
	}
	/**
	 * According to the given parameters, it sets up the correct matching class.
	 * @param queryId String the query identifier to use.
	 * @param query String the query to process.
	 * @param cParameter double the value of the parameter to use.
	 */
	public void processQuery(String queryId, String query, double cParameter) {
		SearchRequest srq = queryingManager.newSearchRequest(queryId, query);
		srq.setControl("c", Double.toString(cParameter));
		srq.addMatchingModel(mModel, wModel);
		matchingCount++;
		queryingManager.runPreProcessing(srq);
		queryingManager.runMatching(srq);
		queryingManager.runPostProcessing(srq);
		queryingManager.runPostFilters(srq);
		printResults(resultFile, srq);
	}
	/**
	 * Performs the matching using the specified weighting model 
	 * from the setup and possibly a combination of evidence mechanism.
	 * It parses the file with the queries (the name of the file is defined
	 * in the address_query file), creates the file of results, and for each
	 * query, gets the relevant documents, scores them, and outputs the results
	 * to the result file.
	 * @param cParameter the value of c
	 */
	public void processQueries(double cParameter) {
		try {
			//prepare console input
			InputStreamReader consoleReader = new InputStreamReader(System.in);
			BufferedReader consoleInput = new BufferedReader(consoleReader);
			String query; int qid=1;
			if (verbose)
				System.out.print("Please enter your query: ");
			while ((query = consoleInput.readLine()) != null) {
				if (query.length() == 0 || 
					query.toLowerCase().equals("quit") ||
					query.toLowerCase().equals("exit")
				)
				{
					return;
				}
				processQuery(""+(qid++), lowercase ? query.toLowerCase() : query, cParameter);
				if (verbose)
					System.out.print("Please enter your query: ");
			}
		} catch(IOException ioe) {
			logger.error("Input/Output exception while performing the matching. Stack trace follows.",ioe);
		}
	}
	/**
	 * Prints the results
	 * @param pw PrintWriter the file to write the results to.
	 * @param q SearchRequest the search request to get results from.
	 */
	public void printResults(PrintWriter pw, SearchRequest q) {
		ResultSet set = q.getResultSet();
		DocumentIndex docIndex = index.getDocumentIndex();
		int[] docids = set.getDocids();
		double[] scores = set.getScores();
		int minimum = RESULTS_LENGTH;
		//if the minimum number of documents is more than the
		//number of documents in the results, aw.length, then
		//set minimum = aw.length
		if (minimum > set.getResultSize())
			minimum = set.getResultSize();
		if (verbose)
			if(set.getResultSize()>0)
				pw.write("\n\tDisplaying 1-"+set.getResultSize()+ " results\n");
			else
				pw.write("\n\tNo results\n");
		StringBuilder sbuffer = new StringBuilder();
		//the results are ordered in asceding order
		//with respect to the score. For example, the
		//document with the highest score has score
		//score[scores.length-1] and its docid is
		//docid[docids.length-1].
		int start = 0;
		int end = minimum;
		for (int i = start; i < end; i++) {
			sbuffer.append(i);
			sbuffer.append(" ");
			//sbuffer.append(docids[i]);
			sbuffer.append(docIndex.getDocumentNumber(docids[i]));
			sbuffer.append(" ");
			sbuffer.append(docids[i]);
			sbuffer.append(" ");
			sbuffer.append(scores[i]);
			sbuffer.append('\n');
		}
		//System.out.println(sbuffer.toString());
		pw.write(sbuffer.toString());
		pw.flush();
		//pw.write("finished outputting\n");
	}
	/**
	 * Starts the interactive query application.
	 * @param args the command line arguments.
	 */
	public static void main(String[] args) {
		InteractiveQuerying iq = new InteractiveQuerying();
		if (args.length == 0)
		{
			iq.processQueries(1.0);
		}
		else if (args.length == 1 && args[0].equals("--noverbose"))
		{
			iq.verbose = false;
			iq.processQueries(1.0);
		}
		else
		{
			iq.verbose = false;
			StringBuilder s = new StringBuilder();
			for(int i=0; i<args.length;i++)
			{
				s.append(args[i]);
				s.append(" ");
			}
			iq.processQuery("CMDLINE", s.toString(), 1.0);
		}	
	}
}

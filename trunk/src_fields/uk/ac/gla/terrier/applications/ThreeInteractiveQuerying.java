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
 * The Original Code is ThreeInteractiveQuerying.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
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
import java.io.StringReader;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.querying.ThreeManager;
import uk.ac.gla.terrier.querying.SearchRequest;
import antlr.TokenStreamSelector;
import uk.ac.gla.terrier.querying.parser.TerrierLexer;
import uk.ac.gla.terrier.querying.parser.TerrierFloatLexer;
import uk.ac.gla.terrier.querying.parser.TerrierQueryParser;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.utility.ApplicationSetup;
/**
 * This class peforms interactive querying at the command line. It asks
 * for a query on Standard Input, and then displays the document IDs that
 * match the given query.
 * @author Gianni Amati, Vassilis Plachouras, Ben He, Craig Macdonald
 * @version $Revision: 1.4 $
 */
public class ThreeInteractiveQuerying {
	protected boolean verbose = true;
	/** the number of processed queries. */
	protected int matchingCount = 0;
	/** The file to store the output to.*/
	protected PrintWriter resultFile = new PrintWriter(System.out);
	/** The query manager.*/
	protected ThreeManager queryingManager;
	/** The weighting model used.*/
	protected String wModel;
	
	/** The matching model used.*/
	protected String mModel = "ThreeMatching";
	/** The data structures used.*/
	protected Index index;
	/** The maximum number of presented results. */
	protected static int RESULTS_LENGTH = 
		Integer.parseInt(ApplicationSetup.getProperty("interactive.output.format.length", "40"));
	/**
	 * A default constructor initialises the inverted index,
	 * the lexicon and the document index structures.
	 */
	public ThreeInteractiveQuerying() {
		long startLoading = System.currentTimeMillis();
		index = Index.createIndex();
		if (index==null) {
			System.err.println("ERROR: Failed to load index1. Perhaps index files are missing");
			System.exit(1);
		}
		
		Index index2 = Index.createIndex(
			ApplicationSetup.makeAbsolute(
				ApplicationSetup.getProperty("terrier.index.path2", ApplicationSetup.TERRIER_INDEX_PATH), 
				ApplicationSetup.TERRIER_VAR),
			ApplicationSetup.getProperty("terrier.index.prefix2", ApplicationSetup.TERRIER_INDEX_PREFIX));
		if (index2==null) {
			System.err.println("ERROR: Failed to load index2. Perhaps index files are missing");
			System.exit(1);
		}


		Index index3 = Index.createIndex(
			ApplicationSetup.makeAbsolute(
				ApplicationSetup.getProperty("terrier.index.path3", ApplicationSetup.TERRIER_INDEX_PATH), 
				ApplicationSetup.TERRIER_VAR), 
			ApplicationSetup.getProperty("terrier.index.prefix3", ApplicationSetup.TERRIER_INDEX_PREFIX));
		if (index3==null) {
			System.err.println("ERROR: Failed to load index3. Perhaps index files are missing");
			System.exit(1);
		}


		queryingManager = new ThreeManager(index, index2, index3);
		long endLoading = System.currentTimeMillis();
		mModel = ApplicationSetup.getProperty("matching.model", "uk.ac.gla.terrier.matching.ThreeMatching");
		
		System.err.println("time to intialise all 3 indices : " + ((endLoading-startLoading)/1000.0D));


		
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
		SearchRequest srq = queryingManager.newSearchRequest(queryId);
		
		//even though we have single-threaded usage
		//in mind, the synchronized makes code faster
		//since each sbuffer.append() call does not
		//try to obtain a lock.
		
		if (verbose)
			System.out.println(queryId +" : "+query);
		try{
			TerrierLexer lexer = new TerrierLexer(new StringReader(query));
			TerrierFloatLexer flexer = new TerrierFloatLexer(lexer.getInputState());

			TokenStreamSelector selector = new TokenStreamSelector();
			selector.addInputStream(lexer, "main");
			selector.addInputStream(flexer, "numbers");
			selector.select("main");
			TerrierQueryParser parser = new TerrierQueryParser(selector);
			parser.setSelector(selector);
			srq.setQuery(parser.query());
			//System.err.println("QUERY: "+srq.getQuery());
		} catch (Exception e) {
			System.out.println("Failed to process Q"+queryId+" : "+e);
			return;
		}
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
			wModel = "PL"; mModel = "ThreeMatching";			
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
				processQuery(""+(qid++), query, cParameter);
				if (verbose)
					System.out.print("Please enter your query: ");
			}
		} catch(IOException ioe) {
			System.err.println("Input/Output exception while performing the matching. Stack trace follows.");
			ioe.printStackTrace(System.err);
			System.exit(1);
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
		StringBuffer sbuffer = new StringBuffer();
		//even though we have single-threaded usage
		//in mind, the synchronized makes code faster
		//since each sbuffer.append() call does not
		//try to obtain a lock.
		synchronized(sbuffer) {
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
		}
		//pw.write("finished outputting\n");
	}
	/**
	 * Starts the interactive query application.
	 * @param args the command line arguments.
	 */
	public static void main(String[] args) {
		ThreeInteractiveQuerying iq = new ThreeInteractiveQuerying();
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
			StringBuffer s = new StringBuffer();
			for(int i=0; i<args.length;i++)
			{
				s.append(args[i]);
				s.append(" ");
			}
			iq.processQuery("CMDLINE", s.toString(), 1.0);
		}	
	}
}

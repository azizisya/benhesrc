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
 * the LiCense for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is TRECQuerying.java.
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Gianni Amati <gba{a.}fub.it> (original author)
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
 *   Ben He <ben{a.}dcs.gla.ac.uk>
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
 */
package org.terrier.applications;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;

import org.terrier.matching.ResultSet;
import org.terrier.querying.Manager;
import org.terrier.querying.Request;
import org.terrier.querying.SearchRequest;
import org.terrier.structures.Index;
import org.terrier.structures.MetaIndex;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.Files;

/**
 * This class performs a batch mode retrieval from a set of TREC queries. 
 * <h2>Configuring</h2> 
 * 
 * <h3>Topics</h3> 
 * Topics can be specified for this class in two different ways. Firstly, by 
 * placing the the name of the file(s) to be processed for topics in the file
 * <tt>etc/trec.topics.list</tt>. Secondly, by specifying the name of the file
 * to be processed for topics in the property <tt>trec.topics</tt>. If the 
 * <tt>trec.topics</tt> property exists, then only the specified file will be 
 * processsed, otherwise, the class will attempt to read a filename from the 
 * trec.topics.list file. The location of the <tt>trec.topics.list</tt> file 
 * can be altered from the default by altering
 * the property of the same name. 
 * 
 * 
 * <h3>Models</h3> 
 * 
 * If the <tt>trec.model</tt> property is specified, then all runs will be made 
 * using that weighting model.
 * Otherwise, one run is done for each weighting model specified in the file
 * <tt>etc/trec.models</tt>. The location of the <tt>trec.models</tt> file can
 * be altered from the default by altering the property of the same name. <h3>
 * Result Files</h3> The results from the system are output in a trec_eval
 * compatable format. The filename of the results file is specified as the
 * WEIGHTINGMODELNAME_cCVALUE.RUNNO.res, in the var/results folder. RUNNO is
 * (usually) a constantly increasing number, as specified by a file in the
 * results folder. The location of the results folder can be altered by the
 * <tt>trec.results</tt> property. If the property <tt>trec.querycounter.type</tt>
 * is not set to sequential, the RUNNO will be a string including the time and a 
 * randomly generated number. This is best to use when many instances of Terrier 
 * are writing to the same results folder, as the incrementing RUNNO method is 
 * not mult-process safe (eg one Terrier could delete it while another is reading it). 
 * 
 * 
 * <h2>Properties</h2> 
 * 
 * <li><tt>trec.topics.parser</tt> - the query parser that parses the topic file(s).
 * TRECQuery by default. Subclass the TRECQuery class and alter this property if
 * your topics come in a very different format to those of TREC. </li>
 * 
 * <li><tt>trec.topics</tt> - the name of the topic file.  </li>
 * 
 * <li><tt>trec.topics.list</tt> - the name of the file containing the name(s) of 
 * the topic file(s). </li>
 * 
 * <li><tt>trec.model</tt> the name of the weighting model used during retrieval.  </li>
 * 
 * <li><tt>trec.models</tt> - the name of the file containing the name(s) of the topic file(s).</li>
 * 
 * <li><tt>c</tt> - the term frequency normalisation parameter value. A value specified at runtime as an
 * API parameter (e.g. TrecTerrier -c) overrides this property. 
 * 
 * <li><tt>trec.matching</tt> the name of the matching model that is used for
 * retrieval. Defaults to Matching. </li>
 * 
 * <li><tt>trec.manager</tt> the name of the Manager that is used for retrieval. Defaults to Manager.</li> 
 * 
 * <li><tt>trec.results</tt> the location of the results folder for results.
 * Defaults to TERRIER_VAR/results/</li>
 * 
 * <li><tt>trec.results.file</tt> the exact result filename to be output.</li> 
 * 
 * <li><tt>trec.output.format.length</tt> - the very maximum number of results ever output per-query into the results file .
 * Default value 1000.</li> 
 * 
 * <li><tt>trec.querycounter.type</tt> - how the number (RUNNO) at the end of a run file should be generated. Defaults to sequential,
 * in which case RUNNO is a constantly increasing number. Otherwise it is a
 * string including the time and a randomly generated number.</li> 
 * 
 * <li><tt>trec.iteration</tt> - the contents of the Iteration column in the
 * trec_eval compatible results. Defaults to 0. </li>
 * 
 * <li><tt>trec.querying.dump.settings</tt> - controls whether the settings used to
 * generate a results file should be dumped to a .settings file in conjunction
 * with the .res file. Defaults to true. 
 * 
 * <li><tt>trec.querying.outputformat</tt>- controls class to write the results file. Defaults to
 * TRECQuerying$TRECDocnoOutputFormat.</li> 
 * 
 * <li><tt>trec.querying.resultscache</tt> - controls cache to use for query caching. 
 * Defaults to TRECQuerying$NullQueryResultCache</li> 
 * 
 * </ul>
 * 
 * @author Gianni Amati, Vassilis Plachouras, Ben He, Craig Macdonald
 * @version $Revision: 1.87 $
 */
public class TRECQuerying {

	/** interface for adjusting the output of TRECQuerying */
	public static interface OutputFormat {
		public void printResults(final PrintWriter pw, final SearchRequest q,
				String method, String iteration, int numberOfResults) throws IOException;
	}
	
	/** Interface for introducing caching strategies into TRECQuerying */
	public static interface QueryResultCache {
		public ResultSet checkCache(final SearchRequest q);
		public void add(final SearchRequest q);
		public void reset();
	}
	
	public static interface QuerySource extends Iterator<String> {
		/** 
		 * Returns the query identifier of the last query
		 * fetched, or the first one, if none has been
		 * fetched yet.
		 * @return String the query number of a query.
		 */
		public String getQueryId();
		
		/** Resets the query source back to the first query. */
		public void reset();
		
		/** Return information about the query source */
		public String[] getInfo();
	}
	
	/** Do nothing QueryResultCache */
	static class NullQueryResultCache implements QueryResultCache {
		public void reset(){}
		public void add(SearchRequest q) {}
		public ResultSet checkCache(SearchRequest q) {
			return null;
		}		
	}
	
	/** an astract results cache that puts stuff into an ever-growing Map */
	static abstract class GrowingMapQueryResultCache<K> implements QueryResultCache 
	{
		Map<K, ResultSet> cache = new HashMap<K, ResultSet>();		
		public void reset()
		{
			cache.clear();
		}
		protected abstract K hashQuery(SearchRequest q);
		
		public void add(SearchRequest q)
		{
			cache.put(hashQuery(q), q.getResultSet());
		}		
		
		public ResultSet checkCache(SearchRequest q)
		{
			return cache.get(hashQuery(q));
		}		
	}
	
	static class GrowingMapQueryStringResultCache extends GrowingMapQueryResultCache<String> {
		@Override
		protected String hashQuery(SearchRequest q) {
			return q.getOriginalQuery();
		}
	}
	

	/** The logger used */
	protected static final Logger logger = Logger.getRootLogger();

	protected static boolean removeQueryPeriods = false;

	/** random number generator */
	protected static final Random random = new Random();

	/** The number of matched queries. */
	protected int matchingCount = 0;

	/** The file to store the output to. */
	protected PrintWriter resultFile;

	/** The filename of the last file results were output to. */
	protected String resultsFilename;

	/**
	 * Dump the current settings along with the results. Controlled by property
	 * <tt>trec.querying.dump.settings</tt>, defaults to true.
	 */
	protected static boolean DUMP_SETTINGS = Boolean
			.parseBoolean(ApplicationSetup.getProperty(
					"trec.querying.dump.settings", "true"));

	/**
	 * The name of the manager object that handles the queries. Set by property
	 * <tt>trec.manager</tt>, defaults to Manager.
	 */
	protected String managerName = ApplicationSetup.getProperty("trec.manager",
			"Manager");
	/** The manager object that handles the queries. */
	protected Manager queryingManager;

	/**
	 * The name of the weighting model that is used for retrieval. Defaults to
	 * PL2
	 */
	protected String wModel = "PL2";

	/**
	 * The name of the matching model that is used for retrieval. Defaults to
	 * Matching
	 */
	protected String mModel = ApplicationSetup.getProperty("trec.matching",
			"Matching");

	/** The object that encapsulates the data structures used by Terrier. */
	protected Index index;

	/** The number of results to output. */
	static int RESULTS_LENGTH = Integer.parseInt(ApplicationSetup
			.getProperty("trec.output.format.length", "1000"));

	/** A TREC specific output field. */
	static String ITERATION = ApplicationSetup.getProperty(
			"trec.iteration", "Q");

	/**
	 * The method - ie the weighting model and parameters. Examples:
	 * <tt>TF_IDF</tt>, <tt>PL2c1.0</tt>
	 */
	protected String method = null;

	/**
	 * What parse to parse the batch topic files. Configured by property
	 * <tt>trec.topics.parser</tt>
	 */
	protected String topicsParser = ApplicationSetup.getProperty(
			"trec.topics.parser", "TRECQuery");

	protected QuerySource querySource;

	protected OutputFormat printer;
	
	protected QueryResultCache resultsCache;

	/**
	 * TRECQuerying default constructor initialises the inverted index, the
	 * lexicon and the document index structures.
	 */
	public TRECQuerying() {
		this.loadIndex();
		this.createManager();
		this.querySource = this.getQueryParser();
		this.printer = getOutputFormat();
		this.resultsCache = getResultsCache();
	}

	/**
	 * TRECQuerying constructor initialises the specified inverted index, the
	 * lexicon and the document index structures.
	 * 
	 * @param i
	 *            The specified index.
	 */
	public TRECQuerying(Index i) {
		this.setIndex(i);
		this.createManager();
		this.querySource = this.getQueryParser();
		this.printer = getOutputFormat();
		this.resultsCache = getResultsCache();
	}

	protected QueryResultCache getResultsCache() {
		QueryResultCache rtr = null;
		try {
			String className = ApplicationSetup.getProperty(
					"trec.querying.resultscache", NullQueryResultCache.class
							.getName());
			if (!className.contains("."))
				className = "org.terrier.applications.TRECQuerying$"
						+ className;
			else if (className.startsWith("uk.ac.gla.terrier"))
				className = className.replaceAll("uk.ac.gla.terrier", "org.terrier");
			rtr = Class.forName(className).asSubclass(QueryResultCache.class).newInstance();
		} catch (Exception e) {
			logger.error(e);
		}
		return rtr;
	}
	
	protected OutputFormat getOutputFormat() {
		OutputFormat rtr = null;
		try {
			String className = ApplicationSetup.getProperty(
					"trec.querying.outputformat", TRECDocnoOutputFormat.class.getName());
			if (!className.contains("."))
				className = "org.terrier.applications.TRECQuerying$"+ className;
			else if (className.startsWith("uk.ac.gla.terrier"))
				className = className.replaceAll("uk.ac.gla.terrier", "org.terrier");
			rtr = Class.forName(className).asSubclass(OutputFormat.class)
					.getConstructor(Index.class).newInstance(this.index);
		} catch (Exception e) {
			logger.error(e);
			throw new IllegalArgumentException("Could not load TREC OutputFormat class", e);
		}
		return rtr;
	}

	/**
	 * Create a querying manager. This method should be overriden if another
	 * matching model is required.
	 */
	protected void createManager() {
		try {
			if (managerName.indexOf('.') == -1)
				managerName = "org.terrier.querying." + managerName;
			else if (managerName.startsWith("uk.ac.gla.terrier"))
				managerName = managerName.replaceAll("uk.ac.gla.terrier", "org.terrier");
			queryingManager = (Manager) (Class.forName(managerName)
					.getConstructor(new Class[] { Index.class })
					.newInstance(new Object[] { index }));
		} catch (Exception e) {
			logger.error("Problem loading Manager (" + managerName + "): ", e);

		}
	}

	/**
	 * Loads index(s) from disk.
	 * 
	 */
	protected void loadIndex() {
		long startLoading = System.currentTimeMillis();
		index = Index.createIndex();
		if (index == null) {
			logger
					.fatal("Failed to load index. Perhaps index files are missing");
			logger.fatal(Index.getLastIndexLoadError());
		}
		long endLoading = System.currentTimeMillis();
		if (logger.isInfoEnabled())
			logger.info("time to intialise index : "
					+ ((endLoading - startLoading) / 1000.0D));
	}

	/**
	 * Get the index pointer.
	 * 
	 * @return The index pointer.
	 */
	public Index getIndex() {
		return index;
	}

	/**
	 * Set the index pointer.
	 * 
	 * @param i
	 *            The index pointer.
	 */
	public void setIndex(Index i) {
		this.index = i;
		if (index == null) {
			logger.fatal("Failed to load an index. Perhaps index files are missing");
		}
	}

	/**
	 * Get the querying manager.
	 * 
	 * @return The querying manager.
	 */
	public Manager getManager() {
		return queryingManager;
	}

	/**
	 * Closes the used structures.
	 */
	public void close() {
		if (index != null)
			try {
				index.close();
			} catch (IOException e) {
			}
	}

	/**
	 * Get the sequential number of the next result file in the results folder.
	 * 
	 * @param resultsFolder
	 *            The path of the results folder.
	 * @return The sequential number of the next result file in the results
	 *         folder.
	 */
	protected String getNextQueryCounter(String resultsFolder) {
		String type = ApplicationSetup.getProperty("trec.querycounter.type",
				"sequential").toLowerCase();
		if (type.equals("sequential"))
			return getSequentialQueryCounter(resultsFolder);
		// else if (type.equals("random"))
		// {
		return getRandomQueryCounter();
		// }
	}

	/**
	 * Get a random number between 0 and 1000.
	 * 
	 * @return A random number between 0 and 1000.
	 */
	protected String getRandomQueryCounter() {
		return ""
		/* seconds since epoch */
		+ (System.currentTimeMillis() / 1000) + "-"
		/* random number in range 0-1000 */
		+ random.nextInt(1000);
	}

	/**
	 * Get the sequential number of the current result file in the results
	 * folder.
	 * 
	 * @param resultsFolder
	 *            The path of the results folder.
	 * @return The sequential number of the current result file in the results
	 *         folder.
	 */
	protected String getSequentialQueryCounter(String resultsFolder) {
		/* TODO: NFS safe locking */
		File fx = new File(resultsFolder, "querycounter");
		int counter = 0;
		if (!fx.exists()) {
			try {
				BufferedWriter bufw = new BufferedWriter(new FileWriter(fx));
				bufw.write(counter + ApplicationSetup.EOL);
				bufw.close();
			} catch (IOException ioe) {
				logger.fatal("Input/Output exception while creating querycounter. Stack trace follows.", ioe);
			}
		} else {
			try {
				BufferedReader buf = new BufferedReader(new FileReader(fx));
				String s = buf.readLine();
				if (s != null)
					counter = Integer.parseInt(s);
				else
					counter = 0;
				counter++;
				buf.close();
				BufferedWriter bufw = new BufferedWriter(new FileWriter(fx));
				bufw.write(counter + ApplicationSetup.EOL);
				bufw.close();
			} catch (Exception e) {
				logger.fatal("Exception occurred when defining querycounter",e);
			}
		}
		return "" + counter;
	}

	/**
	 * Returns a PrintWriter used to store the results.
	 * 
	 * @param predefinedName
	 *            java.lang.String a non-standard prefix for the result file.
	 * @return a handle used as a destination for storing results.
	 */
	public PrintWriter getResultFile(String predefinedName) {
		final String PREDEFINED_RESULT_PREFIX = "prob";
		PrintWriter resultFile = null;
		File fx = new File(ApplicationSetup.TREC_RESULTS);
		if (!fx.exists()) {
			if (!fx.mkdir()) {
				logger.error("Could not create results directory ("
						+ ApplicationSetup.TREC_RESULTS
						+ ") - permissions problem?");
				return null;
			}
		}

		try {
			// write to a specific filename
			String theFilename = ApplicationSetup.getProperty(
					"trec.results.file", null);
			if (theFilename != null) {
				theFilename = ApplicationSetup.makeAbsolute(theFilename,
						ApplicationSetup.TREC_RESULTS);
				resultFile = new PrintWriter(Files.writeFileWriter(theFilename));
				resultsFilename = theFilename;
				if (logger.isInfoEnabled())
					logger.info("Writing results to " + resultsFilename);
				return resultFile;
			}

			// write to an automatically-generated filename
			String querycounter = getNextQueryCounter(ApplicationSetup.TREC_RESULTS);
			String prefix = null;
			if (predefinedName == null || predefinedName.equals(""))
				prefix = PREDEFINED_RESULT_PREFIX;
			else
				prefix = predefinedName;

			resultsFilename = ApplicationSetup.TREC_RESULTS + "/" + prefix
					+ "_" + querycounter + ApplicationSetup.TREC_RESULTS_SUFFIX;
			resultFile = new PrintWriter(new BufferedWriter(new FileWriter(
					new File(resultsFilename))));
			if (logger.isInfoEnabled())
				logger.info("Writing results to " + resultsFilename);
		} catch (IOException e) {
			logger
					.error(
							"Input/Output exception while creating the result file. Stack trace follows.",
							e);
		}
		return resultFile;
	}

	/**
	 * According to the given parameters, it sets up the correct matching class
	 * and performs retrieval for the given query.
	 * 
	 * @param queryId
	 *            the identifier of the query to process.
	 * @param query
	 *            the query to process.
	 */
	public SearchRequest processQuery(String queryId, String query) {
		return processQuery(queryId, query, 1.0, false);
	}

	/**
	 * According to the given parameters, it sets up the correct matching class
	 * and performs retrieval for the given query.
	 * 
	 * @param queryId
	 *            the identifier of the query to process.
	 * @param query
	 *            the query to process.
	 * @param cParameter
	 *            double the value of the parameter to use.
	 */
	public SearchRequest processQuery(String queryId, String query,
			double cParameter) {
		return processQuery(queryId, query, cParameter, true);
	}

	/**
	 * According to the given parameters, it sets up the correct matching class
	 * and performs retrieval for the given query.
	 * 
	 * @param queryId
	 *            the identifier of the query to process.
	 * @param query
	 *            the query to process.
	 * @param cParameter
	 *            double the value of the parameter to use.
	 * @param c_set
	 *            A boolean variable indicating if cParameter has been
	 *            specified.
	 */
	protected void processQueryAndWrite(String queryId, String query,
			double cParameter, boolean c_set) {
		SearchRequest srq = processQuery(queryId, query, cParameter, c_set);

		if (resultFile == null) {
			method = ApplicationSetup.getProperty("trec.runtag", queryingManager.getInfo(srq));
			resultFile = getResultFile(method);
		}
		final long t = System.currentTimeMillis();
		try {
			printer.printResults(resultFile, srq, method, ITERATION + "0", RESULTS_LENGTH);
		} catch (IOException ioe) {
			logger.error("Problem writing results file:", ioe);
		}
		logger.debug("Time to write results: "
				+ (System.currentTimeMillis() - t));
	}

	/**
	 * According to the given parameters, it sets up the correct matching class
	 * and performs retrieval for the given query.
	 * 
	 * @param queryId
	 *            the identifier of the query to process.
	 * @param query
	 *            the query to process.
	 * @param cParameter
	 *            double the value of the parameter to use.
	 * @param c_set
	 *            boolean specifies whether the parameter c is set.
	 */
	public SearchRequest processQuery(String queryId, String query,
			double cParameter, boolean c_set) {

		if (removeQueryPeriods && query.indexOf(".") > -1) {
			logger.warn("Removed . from query");
			query = query.replaceAll("\\.", " ");
		}

		if (logger.isInfoEnabled())
			logger.info(queryId + " : " + query);
		SearchRequest srq = queryingManager.newSearchRequest(queryId, query);
		initSearchRequestModification(queryId, srq);
		String c = null;
		if (c_set) {
			srq.setControl("c", Double.toString(cParameter));
		} else if ((c = ApplicationSetup.getProperty("trec.c", null)) != null) {
			srq.setControl("c", c);
		}
		c = null;
		if ((c = srq.getControl("c")).length() > 0) {
			c_set = true;
		}
		srq.setControl("c_set", "" + c_set);

		srq.addMatchingModel(mModel, wModel);
		preQueryingSearchRequestModification(queryId, srq);
		ResultSet rs = resultsCache.checkCache(srq);
		if (rs != null)
			((Request)rs).setResultSet(rs);
		
		
		if (logger.isInfoEnabled())
			logger.info("Processing query: " + queryId + ": '" + query + "'");
		matchingCount++;
		queryingManager.runPreProcessing(srq);
		queryingManager.runMatching(srq);
		queryingManager.runPostProcessing(srq);
		queryingManager.runPostFilters(srq);
		resultsCache.add(srq);
		return srq;
	}

	protected void preQueryingSearchRequestModification(String queryId,
			SearchRequest srq) {
	}

	protected void initSearchRequestModification(String queryId,
			SearchRequest srq) {
	}

	/**
	 * Performs the matching using the specified weighting model from the setup
	 * and possibly a combination of evidence mechanism. It parses the file with
	 * the queries (the name of the file is defined in the address_query file),
	 * creates the file of results, and for each query, gets the relevant
	 * documents, scores them, and outputs the results to the result file.
	 * 
	 * @return String the filename that the results have been written to
	 */
	public String processQueries() {
		return processQueries(1.0d, false);
	}

	/**
	 * Performs the matching using the specified weighting model from the setup
	 * and possibly a combination of evidence mechanism. It parses the file with
	 * the queries, creates the file of results, and for each query, gets the
	 * relevant documents, scores them, and outputs the results to the result
	 * file. It the term frequency normalisation parameter equal to the given
	 * value.
	 * 
	 * @param c
	 *            double the value of the term frequency parameter to use.
	 * @return String the filename that the results have been written to
	 */
	public String processQueries(double c) {
		return processQueries(c, true);
	}

	/**
	 * Get the query parser that is being used.
	 * 
	 * @return The query parser that is being used.
	 */
	protected QuerySource getQueryParser() {
		String topicsFile = null;
		QuerySource rtr = null;
		try {
			Class<? extends QuerySource> queryingClass = Class.forName(
					topicsParser.indexOf('.') > 0 ? topicsParser
							: "org.terrier.structures." + topicsParser)
					.asSubclass(QuerySource.class);

			if ((topicsFile = ApplicationSetup.getProperty("trec.topics", null)) != null) {
				Class<?>[] types = { String.class };
				Object[] params = { topicsFile };
				rtr = queryingClass.getConstructor(types)
						.newInstance(params);
			} else {
				rtr = queryingClass.newInstance();
			}
			// } catch (ClassNotFoundException cnfe) {

		} catch (Exception e) {
			logger.error("Error instantiating topic file tokeniser called "
					+ topicsParser, e);
		}
		return rtr;
	}

	/**
	 * Performs the matching using the specified weighting model from the setup
	 * and possibly a combination of evidence mechanism. It parses the file with
	 * the queries creates the file of results, and for each query, gets the
	 * relevant documents, scores them, and outputs the results to the result
	 * file.
	 * <p>
	 * <b>Queries</b><br />
	 * Queries are parse from a file. The filename can be expressed in the
	 * <tt>trec.topics</tt> property, or else the file named in the property
	 * <tt>trec.topics.list</tt> property is read, and the each file in that is
	 * used for queries.
	 * 
	 * @param c
	 *            the value of c.
	 * @param c_set
	 *            specifies whether a value for c has been specified.
	 * @return String the filename that the results have been written to
	 */
	public String processQueries(double c, boolean c_set) {
		
		matchingCount = 0;
		querySource.reset();
		this.startingBatchOfQueries();
		final long startTime = System.currentTimeMillis();
		boolean doneSomeMethods = false;
		boolean doneSomeTopics = false;
		try {
			String methodName = null;
			if ((methodName = ApplicationSetup.getProperty("trec.model", null)) != null) {
				wModel = methodName;
				// iterating through the queries
				while (querySource.hasNext()) {
					String query = querySource.next();
					String qid = querySource.getQueryId();
					// process the query
					long processingStart = System.currentTimeMillis();
					processQueryAndWrite(qid, query, c, c_set);
					long processingEnd = System.currentTimeMillis();
					if (logger.isInfoEnabled())
						logger
								.info("Time to process query: "
										+ ((processingEnd - processingStart) / 1000.0D));
					doneSomeTopics = true;
				}
				querySource.reset();
				this.finishedQueries();
				// after finishing with a batch of queries, close the result
				// file
				doneSomeMethods = true;
				if (DUMP_SETTINGS && doneSomeTopics)
					printSettings(queryingManager.newSearchRequest(""),
							querySource.getInfo(),
							"# run started at: " + startTime
									+ "\n# run finished at "
									+ System.currentTimeMillis() + "\n# c=" + c
									+ " c_set=" + c_set + "\n# model=" + wModel);
			} else {
				BufferedReader methodsFile = new BufferedReader(new FileReader(
						ApplicationSetup.TREC_MODELS));
				while ((methodName = methodsFile.readLine()) != null) {
					
					//trim whitespace
					methodName = methodName.trim();
					// ignore empty lines, or lines starting with # from the
					// methods file.
					if (methodName.startsWith("#") || methodName.equals(""))
						continue;
					wModel = methodName;
					// iterating through the queries
					while (querySource.hasNext()) {
						String query = querySource.next();
						String qid = querySource.getQueryId();
						// process the query
						long processingStart = System.currentTimeMillis();
						processQueryAndWrite(qid, query, c, c_set);
						long processingEnd = System.currentTimeMillis();
						if (logger.isInfoEnabled())
							logger
									.info("Time to process query: "
											+ ((processingEnd - processingStart) / 1000.0D));
						doneSomeTopics = true;
					}
					querySource.reset();
					this.finishedQueries();
					// after finishing with a batch of queries, close the result
					// file
					doneSomeMethods = true;
					if (DUMP_SETTINGS && doneSomeTopics)
						printSettings(
								queryingManager.newSearchRequest(""),
								querySource.getInfo(),
								String
										.format(
												"# run started at: %d\n# run finished at %d\n# c=%f c_set=%b\n# model=%s\n",
												startTime, System
														.currentTimeMillis(),
												c, c_set, wModel));
				}
				methodsFile.close();
			}

		} catch (IOException ioe) {
			logger
					.fatal(
							"Input/Output exception while performing the matching. Stack trace follows.",
							ioe);
		}
		if (!doneSomeTopics)
			logger.error("No queries were processed. Please check the file "
					+ ApplicationSetup.TREC_TOPICS_LIST);
		if (!doneSomeMethods)
			logger.error("No models were specified. Please check the file "
					+ ApplicationSetup.TREC_MODELS);
		if (doneSomeTopics && doneSomeMethods)
			logger.info("Finished topics, executed " + matchingCount
					+ " queries in "
					+ ((System.currentTimeMillis() - startTime) / 1000.0d)
					+ " seconds, results written to " + resultsFilename);
		return resultsFilename;
	}

	/**
	 * Before starting a batch of queries, this method is called by
	 * processQueries()
	 * 
	 * @since 2.2
	 */
	protected void startingBatchOfQueries() {

	}

	/**
	 * After finishing with a batch of queries, close the result file
	 * 
	 */
	protected void finishedQueries() {
		if (resultFile != null)
			resultFile.close();
		resultFile = null;
	}

	/**
	 * prints the current settings to a file with the same name as the current
	 * results file. this assists in tracing the settings used to generate a
	 * given run.
	 */
	public void printSettings(final SearchRequest default_q,
			final String[] topicsFiles, final String otherComments) {
		try {
			BufferedOutputStream bos = new BufferedOutputStream(
					new FileOutputStream(resultsFilename + ".settings"));
			ApplicationSetup.getUsedProperties().store(
					bos,
					" Settings of Terrier (TRECQuerying) generated for run "
							+ resultsFilename);
			PrintWriter pw = new PrintWriter(bos);
			if (topicsFiles != null)
				for (String f : topicsFiles)
					pw.println("# topicfile: " + f);
			java.util.Map<String, String> controls = ((org.terrier.querying.Request) default_q)
					.getControlHashtable();
			for (Map.Entry<String,String> kv : controls.entrySet())
			{
				pw.println(String.format("# control: %s=%s", kv.getKey(), kv.getValue()));
			}
			pw.println(otherComments);
			pw.close();
			logger.info("Settings of Terrier written to " + resultsFilename
					+ ".settings");
		} catch (IOException ioe) {
			logger
					.warn(
							"Couldn't write settings out to disk in TRECQuerying (.res.settings)",
							ioe);
		}
	}

	static class TRECDocnoOutputFormat implements OutputFormat {
		Index index;

		public TRECDocnoOutputFormat(Index _index) {
			this.index = _index;
		}
		
		/** method which extracts the docnos for the prescribed resultset */
		protected String[] obtainDocnos(final String metaIndexDocumentKey, final SearchRequest q, final ResultSet set) throws IOException
		{
			String[] docnos;
			if (set.hasMetaItems(metaIndexDocumentKey)) {
				docnos = set.getMetaItems(metaIndexDocumentKey);
			} else {
				final MetaIndex metaIndex = index.getMetaIndex();
				docnos = metaIndex.getItems(metaIndexDocumentKey, set.getDocids());
			}
			return docnos;
		}

		/**
		 * Prints the results for the given search request, using the specified
		 * destination.
		 * 
		 * @param pw
		 *            PrintWriter the destination where to save the results.
		 * @param q
		 *            SearchRequest the object encapsulating the query and the
		 *            results.
		 */
		public void printResults(final PrintWriter pw, final SearchRequest q,
				String method, String iteration, int RESULTS_LENGTH) throws IOException {
			final ResultSet set = q.getResultSet();
			final String metaIndexDocumentKey = "docno";
			final double[] scores = set.getScores();
			if (set.getResultSize() == 0) {
				logger.warn("No results retrieved for query " + q.getQueryID());
				return;
			}
			String[] docnos = obtainDocnos(metaIndexDocumentKey, q, set);
			
			final int maximum = RESULTS_LENGTH > set.getResultSize()
					|| RESULTS_LENGTH == 0 ? set.getResultSize()
					: RESULTS_LENGTH;
			// if the minimum number of documents is more than the
			// number of documents in the results, aw.length, then
			// set minimum = aw.length

			// if (minimum > set.getResultSize())
			// minimum = set.getResultSize();
			//final String iteration = ITERATION + "0";
			final String queryIdExpanded = q.getQueryID() + " " + iteration
					+ " ";
			final String methodExpanded = " " + method + ApplicationSetup.EOL;
			StringBuilder sbuffer = new StringBuilder();
			// the results are ordered in desceding order
			// with respect to the score.
			int limit = 10000;
			int counter = 0;
			for (int i = 0; i < maximum; i++) {
				if (scores[i] <= 0d)
					continue;
				sbuffer.append(queryIdExpanded);
				sbuffer.append(docnos[i]);
				sbuffer.append(" ");
				sbuffer.append(i);
				sbuffer.append(" ");
				sbuffer.append(scores[i]);
				sbuffer.append(methodExpanded);
				counter++;
				if (counter % limit == 0) {
					pw.write(sbuffer.toString());
					sbuffer = null;
					sbuffer = new StringBuilder();
					pw.flush();
				}
			}
			pw.write(sbuffer.toString());
			pw.flush();
		}
	}

	static class TRECDocidOutputFormat implements OutputFormat {
		
		public TRECDocidOutputFormat(Index index) {
		}

		/**
		 * Prints the results for the given search request, using the specified
		 * destination.
		 * 
		 * @param pw
		 *            PrintWriter the destination where to save the results.
		 * @param q
		 *            SearchRequest the object encapsulating the query and the
		 *            results.
		 */
		public void printResults(final PrintWriter pw, final SearchRequest q,
				String method, String iteration, int RESULTS_LENGTH) throws IOException {
			final ResultSet set = q.getResultSet();

			final int[] docids = set.getDocids();
			final double[] scores = set.getScores();
			if (set.getResultSize() == 0) {
				logger.warn("No results retrieved for query " + q.getQueryID());
				return;
			}

			final int maximum = RESULTS_LENGTH > set.getResultSize()
					|| RESULTS_LENGTH == 0 ? set.getResultSize()
					: RESULTS_LENGTH;
			// if the minimum number of documents is more than the
			// number of documents in the results, aw.length, then
			// set minimum = aw.length

			// if (minimum > set.getResultSize())
			// minimum = set.getResultSize();
			//final String iteration = ITERATION + "0";
			final String queryIdExpanded = q.getQueryID() + " " + iteration
					+ " ";
			final String methodExpanded = " " + method + ApplicationSetup.EOL;
			StringBuilder sbuffer = new StringBuilder();
			// the results are ordered in desceding order
			// with respect to the score.
			int limit = 10000;
			int counter = 0;
			for (int i = 0; i < maximum; i++) {
				if (scores[i] <= 0d)
					continue;
				sbuffer.append(queryIdExpanded);
				sbuffer.append(docids[i]);
				sbuffer.append(" ");
				sbuffer.append(i);
				sbuffer.append(" ");
				sbuffer.append(scores[i]);
				sbuffer.append(methodExpanded);
				counter++;
				if (counter % limit == 0) {
					pw.write(sbuffer.toString());
					sbuffer = null;
					sbuffer = new StringBuilder();
					pw.flush();
				}
			}
			pw.write(sbuffer.toString());
			pw.flush();
		}
	}

	public static class NullOutputFormat implements OutputFormat {

		public NullOutputFormat(Index i){} 
		
		public void printResults(PrintWriter pw, SearchRequest q, String method,
				String iteration, int numberOfResults) throws IOException {
			//does nothing
		}
	}
}

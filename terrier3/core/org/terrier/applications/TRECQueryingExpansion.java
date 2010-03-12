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
 * The Original Code is TRECQueryingExpansion.java.
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Gianni Amati <gba{a.}fub.it> (original author)
 *   Ben He <ben{a.}dcs.gla.ac.uk>
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
 */
package org.terrier.applications;
import java.io.BufferedReader;
import java.io.IOException;

import org.apache.log4j.Logger;

import org.terrier.querying.SearchRequest;
import org.terrier.structures.Index;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.Files;

/**
 * This class performs a batch mode retrieval for a set 
 * of TREC queries using query expansion.
 * <h2>Properties</h2>
 * <li><tt>c.post</tt> The term frequency normalisation parameter value used in the 
 * second-pass retrieval. </li>
 * <li><tt>trec.qe.model</tt> - name of the model to use for query expansion. If unset, then one will be read from etc/qemodels</li>
 * @author Gianni Amati, Ben He, Vassilis Plachouras
 * @version $Revision: 1.52 $
 */
public class TRECQueryingExpansion extends TRECQuerying {
   
	/** The logger used */
	private static Logger logger = Logger.getRootLogger();
	
	/** The name of the query expansion model used. */
	protected String qeModel;
	/**
	 * TRECQueryingExpansion default constructor. Calls super().
	 */
	public TRECQueryingExpansion() {
		super();
	}

	/** TRECQueryExpansion - Index constructor. Calls super(Index). */
	public TRECQueryingExpansion(Index i) {
		super(i);
	}
	
	/**
	 * According to the given parameters, it sets up the correct matching class.
	 * @param queryId the identifier of a query.
	 * @param query the query to process
	 * @param cParameter double the term frequency normalisation parameter value
	 * @param c_set specifies whether the given value for the parameter c should be used.
	 */
	public SearchRequest processQuery(String queryId, String query, double cParameter, boolean c_set) {
		SearchRequest srq = queryingManager.newSearchRequest(queryId, query);
		
		if (c_set)
			srq.setControl("c", Double.toString(cParameter));
		srq.addMatchingModel(mModel, wModel);
		srq.setControl("qemodel", qeModel);
		srq.setControl("qe", "on");
		srq.setControl("c_set", ""+c_set);

		if(logger.isInfoEnabled())
			logger.info("processing query " + queryId + " '"+ query + "'");
		matchingCount++;
		queryingManager.runPreProcessing(srq);
		queryingManager.runMatching(srq);
		final String cpost = ApplicationSetup.getProperty("c.post", null);
		if (cpost != null)
		{
			srq.setControl("c", cpost);
			srq.setControl("c_set", "true");
		}
		//else the c control is left as is. Ie if c_set is not true, then the weighting model's
		//default value is kept
		queryingManager.runPostProcessing(srq);
		queryingManager.runPostFilters(srq);
		return srq;
  	}
	
	/**
	 * According to the given parameters, it sets up the correct matching class
	 * and performs retrieval for the given query.
	 * @param queryId the identifier of the query to process.
	 * @param query org.terrier.structures.Query the query to process.
	 * @param cParameter double the value of the parameter to use.
	 * @param c_set A boolean variable indicating if cParameter has been specified.
	 */
	protected void processQueryAndWrite(String queryId, String query, double cParameter, boolean c_set) {
		SearchRequest srq = processQuery(queryId, query, cParameter, c_set);
		if (resultFile == null) {
			method = ApplicationSetup.getProperty("trec.runtag", queryingManager.getInfo(srq));
			String prefix = method +
				"_d_"+ApplicationSetup.getProperty("expansion.documents", "3")+
				"_t_"+ApplicationSetup.getProperty("expansion.terms", "10");
			resultFile = getResultFile(prefix);
		}
		try{
			printer.printResults(resultFile, srq, method, ITERATION + "0", RESULTS_LENGTH);
		} catch (IOException ioe) {
			logger.error("Problem writing results file:", ioe);
		}
	}

	/**
	 * Performs the matching using the specified weighting model 
	 * from the setup.
	 * It parses the file with the queries (the name of the file is defined
	 * in the address_query file), creates the file of results, and for each
	 * query, gets the relevant documents, scores them, and outputs the results
	 * to the result file.
	 * @param c the value of the document length normalisation parameter.
	 * @param c_set boolean specifies whether the value of the 
	 *		parameter c is specified.
	 * @return String the filename that the results have been written to
	 */
	public String processQueries(double c, boolean c_set) {
		final long startTime = System.currentTimeMillis();
		try {
			
	
			querySource.reset();
			boolean doneSomeMethods = false;
			boolean doneSomeTopics = false;
			boolean doneSomeQEMethods = false;
			boolean modelGiven = true;
			
			
			BufferedReader methodsFile = null;
			if ((wModel = ApplicationSetup.getProperty("trec.model", null)) == null){
				modelGiven = false;
				methodsFile = Files.openFileReader(ApplicationSetup.TREC_MODELS);
			}
			String wModelTmp = null;
			while (modelGiven||(wModelTmp = methodsFile.readLine()) != null) {
				if (!modelGiven){
					wModel = wModelTmp.trim();
					//ignore empty lines, or lines starting with # from the methods file.
					if (wModel.startsWith("#") || wModel.equals(""))
						continue;
				}
				
				String qeModelGiven = null;
				if ((qeModelGiven = ApplicationSetup.getProperty("trec.qe.model", null)) != null)
				{
					qeModel = qeModelGiven;
					while (querySource.hasNext()){
						
						String query = querySource.next();
						String qid = querySource.getQueryId();
						//process the query
						long processingStart = System.currentTimeMillis();
						processQueryAndWrite(qid, query, c, c_set);
						long processingEnd = System.currentTimeMillis();
						if(logger.isDebugEnabled())
							logger.debug("time to process query: " + ((processingEnd - processingStart)/1000.0D));
						doneSomeTopics = true;
					}
					if (DUMP_SETTINGS && doneSomeTopics)
						printSettings(queryingManager.newSearchRequest(""), querySource.getInfo(),
							String.format("# run started at: %d\n# run finished at %d\n# c=%f c_set=%b\n# model=%s\n",
							startTime, System.currentTimeMillis(),c,c_set, wModel));
					querySource.reset();
					this.finishedQueries();
					doneSomeQEMethods = true;
				}
				else
				{
				
					//System.err.println("Weighting Model: " + wModel);
					//System.err.println("c: " + c);
					BufferedReader qemethodsFile = Files.openFileReader(ApplicationSetup.EXPANSION_MODELS); 
		
					while ((qeModel = qemethodsFile.readLine()) != null) {
						qeModel = qeModel.trim();
						//ignore empty lines, or lines starting with # from the methods file.
						if (qeModel.startsWith("#") || qeModel.equals(""))
							continue;
						if(logger.isInfoEnabled()){
							logger.info("Weighting Model: " + wModel);
							logger.info("Query Expansion Model: " + qeModel);
						}
						while (querySource.hasNext()){
		
							String query = querySource.next();
							String qid = querySource.getQueryId();
								
							//process the query
							long processingStart = System.currentTimeMillis();
							processQueryAndWrite(qid, query, c, c_set);
							long processingEnd = System.currentTimeMillis();
							if(logger.isDebugEnabled())
								logger.debug("time to process query: " + ((processingEnd - processingStart)/1000.0D));
							doneSomeTopics = true;
						}
						if (DUMP_SETTINGS && doneSomeTopics)
							printSettings(queryingManager.newSearchRequest(""), querySource.getInfo(),
								String.format("# run started at: %d\n# run finished at %d\n# c=%f c_set=%b\n# model=%s\n",
								startTime, System.currentTimeMillis(),c,c_set, wModel));
						querySource.reset();
						this.finishedQueries();
						doneSomeQEMethods = true;
					}
					qemethodsFile.close();
				}
				doneSomeMethods = true;
				if (modelGiven)
					break;
			}
			if (! modelGiven)
				methodsFile.close();
			
			if (!doneSomeTopics) 
				logger.error("No queries were processed. Please check the file " + ApplicationSetup.TREC_TOPICS_LIST);
			if (!doneSomeMethods)
				logger.error("No models were specified. Please check the file " + ApplicationSetup.TREC_MODELS);
			if (!doneSomeQEMethods) 
				logger.error("No query expansion models were specified. Please check the file " + ApplicationSetup.EXPANSION_MODELS);
			if (doneSomeTopics && doneSomeMethods && doneSomeQEMethods)
				if(logger.isInfoEnabled())
					logger.info("Finished topics, executed "+matchingCount+" queries, results written to "+resultsFilename);
		} catch(IOException ioe) {
			logger.fatal("Input/Output exception while performing the matching. Stack trace follows.",ioe);
		}
		return resultsFilename;
	}
}

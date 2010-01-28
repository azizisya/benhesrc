/*
 * Created on 30 Jul 2007
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.applications;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.querying.SearchRequest;
import uk.ac.gla.terrier.querying.parser.TerrierFloatLexer;
import uk.ac.gla.terrier.querying.parser.TerrierLexer;
import uk.ac.gla.terrier.querying.parser.TerrierQueryParser;
import uk.ac.gla.terrier.structures.TRECQuery;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.TerrierTimer;
import antlr.TokenStreamSelector;

public class FieldTRECQueryingExpansion extends FieldTRECQuerying{
	/** The logger used */
	private static Logger logger = Logger.getRootLogger();
	
	/** The name of the query expansion model used. */
    protected String qeModel;
	/**
	 * TRECQueryingExpansion default constructor initialises the inverted index, the lexicon and the document index structures.
	 */
	public FieldTRECQueryingExpansion() {
		super();
	}
	
	/**
	 * According to the given parameters, it sets up the correct matching class.
	 * @param queryId the identifier of a query.
	 * @param query the query to process
	 * @param cParameter double the term frequency normalisation parameter value
	 * @param c_set specifies whether the given value for the parameter c should be used.
	 */
	public SearchRequest processQuery(String queryId, String query, double cParameter, boolean c_set) {
	    SearchRequest srq = queryingManager.newSearchRequest(queryId);
		
	    if(logger.isDebugEnabled())
	    	logger.debug("QueryId: "+queryId +" Query: "+query);
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
		} catch (Exception e) {
			logger.error("Failed to process Q"+queryId+" : ",e);
			return null;
		}
		
		if (c_set)
			srq.setControl("c", Double.toString(cParameter));
		srq.addMatchingModel(mModel, wModel);
		srq.setControl("qemodel", qeModel);
		srq.setControl("qe", "on");
		srq.setControl("c_set", ""+c_set);

		if(logger.isInfoEnabled())
			logger.info("processing query " + queryId);
		matchingCount++;
		queryingManager.runPreProcessing(srq);
		queryingManager.runMatching(srq);
        queryingManager.runPostProcessing(srq);
		queryingManager.runPostFilters(srq);
		return srq;
  	}
	
	/**
	 * According to the given parameters, it sets up the correct matching class
	 * and performs retrieval for the given query.
	 * @param queryId the identifier of the query to process.
	 * @param query uk.ac.gla.terrier.structures.Query the query to process.
	 * @param cParameter double the value of the parameter to use.
	 * @param c_set A boolean variable indicating if cParameter has been specified.
	 */
	protected void processQueryAndWrite(String queryId, String query, double cParameter, boolean c_set) {
		SearchRequest srq = processQuery(queryId, query, cParameter, c_set);
		if (resultFile == null) {
			method = queryingManager.getInfo(srq);
			String prefix = method +
				"_d_"+ApplicationSetup.getProperty("expansion.documents", "")+
				"_t_"+ApplicationSetup.getProperty("expansion.terms", "");
			resultFile = getResultFile(prefix);
        }
        printResults(resultFile, srq);
	}
	
	public static void main(String[] args) {
		TerrierTimer timer = new TerrierTimer();
		timer.start();
		FieldTRECQueryingExpansion trecQuerying = new FieldTRECQueryingExpansion();
		trecQuerying.processQueries();
		timer.setBreakPoint();
		if (logger.isInfoEnabled())
			logger.info("Time elapsed: " + timer.toStringMinutesSeconds());
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
	 *        parameter c is specified.
	 * @return String the filename that the results have been written to
	 */
	public String processQueries(double c, boolean c_set) {
		try {
			BufferedReader methodsFile = new BufferedReader(new FileReader(ApplicationSetup.TREC_MODELS));
	
	        TRECQuery querySource = getQueryParser();
	        boolean doneSomeMethods = false;
	        boolean doneSomeTopics = false;
	        boolean doneSomeQEMethods = false;
	        boolean modelGiven = true;
			if ((wModel = ApplicationSetup.getProperty("trec.model", null)) == null){
				modelGiven = false;
			}
			String wModelTmp = null;
			while (modelGiven||(wModelTmp = methodsFile.readLine()) != null) {
				if (!modelGiven){
					wModel = wModelTmp;
					//ignore empty lines, or lines starting with # from the methods file.
					if (wModel.startsWith("#") || wModel.equals(""))
						continue;
				}
				
				System.err.println("Weighting Model: " + wModel);
				System.err.println("c: " + c);
				BufferedReader qemethodsFile = new BufferedReader(new FileReader(ApplicationSetup.EXPANSION_MODELS));
	
			    while ((qeModel = qemethodsFile.readLine()) != null) {
		            //ignore empty lines, or lines starting with # from the methods file.
					if (qeModel.startsWith("#") || qeModel.equals(""))
		            	continue;
					if(logger.isInfoEnabled()){
				    	logger.info("Weighting Model: " + wModel);
		                logger.info("Query Expansion Model: " + qeModel);
					}
					while (querySource.hasMoreQueries()){
	
						String query = querySource.nextQuery();
						String qid = querySource.getQueryId();
							
	                    //process the query
	                    long processingStart = System.currentTimeMillis();
	                    processQueryAndWrite(qid, query, c, c_set);
	                    long processingEnd = System.currentTimeMillis();
	                    if(logger.isDebugEnabled())
	                    	logger.debug("time to process query: " + ((processingEnd - processingStart)/1000.0D));
	                    doneSomeTopics = true;
	                }
					querySource.reset();
					this.finishedQueries();
	                doneSomeQEMethods = true;
	            }
	            qemethodsFile.close();
	            doneSomeMethods = true;
	            if (modelGiven)
					break;
			}
			methodsFile.close();
			
			if (!doneSomeTopics) 
				logger.error("No queries were processed. Please check the file " + ApplicationSetup.TREC_TOPICS_LIST);
			if (!doneSomeMethods)
				logger.error("No models were specified. Please check the file " + ApplicationSetup.TREC_MODELS);
			if (!doneSomeQEMethods) 
				logger.error("No query expansion models were specified. Please check the file " + ApplicationSetup.EXPANSION_MODELS);
			if (doneSomeTopics && doneSomeMethods && doneSomeQEMethods);
				if(logger.isInfoEnabled())
				logger.info("Finished topics, executed "+matchingCount+" queries, results written to "+resultsFilename);
		} catch(IOException ioe) {
			logger.fatal("Input/Output exception while performing the matching. Stack trace follows.",ioe);
			logger.fatal("Exiting");
			System.exit(1);
		}
		return resultsFilename;
	}
}

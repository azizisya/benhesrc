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
 * The Original Code is DistributedFieldTRECQueryingExpansion.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk> 
 */
package uk.ac.gla.terrier.distr.applications;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.rmi.RMISecurityManager;

import uk.ac.gla.terrier.distr.querying.DistributedQEFieldManager;
import uk.ac.gla.terrier.querying.SearchRequest;
import uk.ac.gla.terrier.querying.parser.TerrierFloatLexer;
import uk.ac.gla.terrier.querying.parser.TerrierLexer;
import uk.ac.gla.terrier.querying.parser.TerrierQueryParser;
import uk.ac.gla.terrier.structures.TRECQuery;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.TerrierTimer;
import antlr.TokenStreamSelector;

/**
 * This is the main class for running retrieval on three fields in
 * a distributed setting.
 * @author Ben He(ben@dcs.gla.ac.uk)
 * @version $Revision: 1.1 $
 */
public class DistributedFieldTRECQueryingExpansion extends
		DistributedFieldTRECQuerying {
	/**
	 * The name of the query expansion model.
	 */
	protected String qeModel;
	/**
	 * The default constructor.
	 *
	 */
	public DistributedFieldTRECQueryingExpansion (){
		super();
		queryingManager = new DistributedQEFieldManager();
	}
	/**
	 * Override the method because we don't want to create duplicate manager.
	 */
	protected void createManager(){
		
	}
	
	/**
 	 * Returns a PrintWriter used to store the results.
 	 * @param predefinedName java.lang.String a non-standard prefix for the result file.
 	 * @return a handle used as a destination for storing results.
 	 */
	public PrintWriter getResultFile(String predefinedName) {
		final String PREDEFINED_RESULT_PREFIX = "prob";
		PrintWriter resultFile = null;
		File fx = new File(ApplicationSetup.TREC_RESULTS);
        if (!fx.exists())
            fx.mkdir();
        String querycounter = getNextQueryCounter(ApplicationSetup.TREC_RESULTS);
		try {
	        String prefix = null;
	        if (predefinedName==null || predefinedName.equals(""))
	        	prefix = PREDEFINED_RESULT_PREFIX;
	        else
	        	prefix = predefinedName;
	        	
	        //adding more info in the prefix
			//int NumFields = indices.length;
	     
			for(int i=1;i<=NumFields;i++)
			{
				String c = ApplicationSetup.getProperty("c."+i,"");
				String cpost = ApplicationSetup.getProperty("c."+i+".post", 
						ApplicationSetup.getProperty("c."+i, "")
							);
				prefix += "_c"+i+"_"+c;
				if (!cpost.equals(c))
					prefix += "_"+cpost;
				String weight = ApplicationSetup.getProperty("weight."+i,"");
				String qeweight = ApplicationSetup.getProperty("qeweight."+i, 
						ApplicationSetup.getProperty("weight."+i, "")
							);	
				prefix += "_w"+i+"_"+weight;
				if (!qeweight.equals(weight))
				prefix += "_"+qeweight;			
				
			}
			
	        /*
			prefix += "_cb"+ApplicationSetup.getProperty("c.body","");
	        prefix += "_ca"+ApplicationSetup.getProperty("c.anchor","");
	        prefix += "_ct"+ApplicationSetup.getProperty("c.title","");
	        prefix += "_aw"+ApplicationSetup.getProperty("anchor.weight","");
	        prefix += "_tw"+ApplicationSetup.getProperty("title.weight","");
	        */
	        if (!ApplicationSetup.getProperty("usm.id","").equals("")) {
	        	String usmId = ApplicationSetup.getProperty("usm.id","");
	        	prefix += "_usm"+usmId;
	        	prefix += "_omega"+ApplicationSetup.getProperty("usm."+usmId+".omega","");
	        	prefix += "_kappa"+ApplicationSetup.getProperty("usm."+usmId+".kappa","");
	        }
	        if (!ApplicationSetup.getProperty("lsm.id","").equals("")) {
	        	String lsmId = ApplicationSetup.getProperty("lsm.id","");
	        	prefix += "_lsm"+lsmId;
	        	prefix += "_omega"+ApplicationSetup.getProperty("lsm.omega","");
	        	prefix += "_kappa"+ApplicationSetup.getProperty("lsm.kappa","");
	        }
	       
			resultsFilename = ApplicationSetup.TREC_RESULTS + '/' + prefix + "_" + querycounter + ApplicationSetup.TREC_RESULTS_SUFFIX; 
            resultFile = new PrintWriter(new BufferedWriter(new FileWriter(resultsFilename)));
            if (logger.isDebugEnabled())
				logger.debug("Writing results to "+resultsFilename);
        } catch (IOException e) {
            logger.fatal("Input/Output exception while creating the result file. Stack trace follows.");
            e.printStackTrace();
            System.exit(1);
        }
        return resultFile;
    }
	
	/**
	 * According to the given parameters, it sets up the correct matching class.
	 * @param queryId the identifier of a query.
	 * @param query uk.ac.gla.terrier.structures.Query the query to process
	 * @param cParameter double the term frequency normalisation parameter value
	 * @param c_set specifies whether the given value for the parameter c should be used.
	 */
	public SearchRequest processQuery(String queryId, String query, double cParameter, boolean c_set) {
	    SearchRequest srq = queryingManager.newSearchRequest(queryId);
		
		System.err.println(queryId +" : "+query);
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
			System.err.println("Failed to process Q"+queryId+" : "+e);
			return null;
		}
		
		/*if (c_set)
			srq.setControl("c", Double.toString(cParameter));*/
		srq.addMatchingModel(mModel, wModel);
		srq.setControl("qemodel", qeModel);
		srq.setControl("qe", "on");

		System.err.println("processing query " + queryId);
		matchingCount++;
		queryingManager.runPreProcessing(srq);
		queryingManager.runMatching(srq);
        queryingManager.runPostProcessing(srq);
		queryingManager.runPostFilters(srq);
		if (resultFile == null) {
			method = queryingManager.getInfo(srq);
			String prefix = method +
				"_d_"+ApplicationSetup.getProperty("expansion.documents", "")+
			"_t_"+ApplicationSetup.getProperty("expansion.terms", "");
			resultFile = getResultFile(prefix);
		}
		printResults(resultFile, srq);
		return srq;
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
	 */
	public String processQueries(double c, boolean c_set) {
		try {
			BufferedReader methodsFile = methodsFile = new BufferedReader(new FileReader(ApplicationSetup.TREC_MODELS));;
			boolean modelGiven = true;
			if ((wModel = ApplicationSetup.getProperty("trec.model", null)) == null){
				modelGiven = false;
			}
	
	        	TRECQuery querySource = getQueryParser();
	        	boolean doneSomeMethods = false;
	        	boolean doneSomeTopics = false;
	        	boolean doneSomeQEMethods = false;
			String wModelTmp = null;	
			while ((wModelTmp = methodsFile.readLine()) != null||modelGiven) {
				if (!modelGiven){
					wModel = wModelTmp;
	            			//ignore empty lines, or lines starting with # from the methods file.
					if (wModel.startsWith("#") || wModel.trim().equals(""))
	            				continue;
				}
				
				//System.err.println("Weighting Model: " + wModel);
				//System.err.println("c: " + c);
				BufferedReader qemethodsFile = new BufferedReader(new FileReader(ApplicationSetup.EXPANSION_MODELS));
	
				boolean qemodelGiven = true;
				if ((qeModel = ApplicationSetup.getProperty("trec.qemodel", null)) == null){
					qemodelGiven = false;
				}
				String qeModelTmp = null;
			    	while ((qeModelTmp = qemethodsFile.readLine()) != null) {
					if (!qemodelGiven){
						qeModel = qeModelTmp;
						//ignore empty lines, or lines starting with # from the methods file.
						if (qeModel.startsWith("#") || qeModel.trim().equals(""))
							continue;
					}
			    		System.err.println("Weighting Model: " + wModel);
	                		System.err.println("Query Expansion Model: " + qeModel);
	
					while (querySource.hasMoreQueries()){
	
						String query = querySource.nextQuery();
						String qid = querySource.getQueryId();
							
	                    			//process the query
	                    			long processingStart = System.currentTimeMillis();
	                    			processQuery(qid, query, c, c_set);
	                    			long processingEnd = System.currentTimeMillis();
	                    			System.err.println("time to process query: " + ((processingEnd - processingStart)/1000.0D));
	                    			doneSomeTopics = true;
	                		}
					querySource.reset();
	                		//after finishing with a batch of queries, close the result file
					if (resultFile!=null) 
						resultFile.close();
	                		resultFile = null;
	                		doneSomeQEMethods = true;
	                		for (int i = 0; i < ((DistributedQEFieldManager)queryingManager).numberOfServers; i++)
	                		System.err.println(((DistributedQEFieldManager)queryingManager).numberOfTopRankedDocs[i] 
	                	        	+ " documents are extracted from server " +
	                	        	((DistributedQEFieldManager)queryingManager).serverNames[i]+"-"+
	                	        	((DistributedQEFieldManager)queryingManager).ids[i]);
	                		System.out.println("Time spent on query expansion: " + 
	                			((DistributedQEFieldManager)queryingManager).time_on_qe);
					if (qemodelGiven)
						break;
	            		}
	            		qemethodsFile.close();
	            		doneSomeMethods = true;
				if (modelGiven)
					break;
			}
			methodsFile.close();
			
			if (!doneSomeTopics) 
				System.err.println("No queries were processed. Please check the file " + ApplicationSetup.TREC_TOPICS_LIST);
			if (!doneSomeMethods)
				System.err.println("No models were specified. Please check the file " + ApplicationSetup.TREC_MODELS);
			if (!doneSomeQEMethods) 
				System.err.println("No query expansion models were specified. Please check the file " + ApplicationSetup.EXPANSION_MODELS);
			if (doneSomeTopics && doneSomeMethods && doneSomeQEMethods);
				System.err.println("Finished topics, executed "+matchingCount+" queries");
		} catch(IOException ioe) {
			System.err.println("Input/Output exception while performing the matching. Stack trace follows.");
			ioe.printStackTrace();
			System.exit(1);
		}
		return resultsFilename;
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
		DistributedFieldTRECQueryingExpansion querying = 
			new DistributedFieldTRECQueryingExpansion();

		querying.processQueries();
		timer.setBreakPoint();
		System.out.println("Time elapsed: " + timer.toStringMinutesSeconds());
	}
}

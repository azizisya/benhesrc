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
 * The Original Code is TRECLMQuerying.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.applications;
import org.apache.log4j.Logger;

import uk.ac.gla.terrier.structures.TRECQuery;
import uk.ac.gla.terrier.utility.ApplicationSetup;
/**
 * This class performs a batch mode retrieval 
 * from a set of TREC queries.
 * @author Gianni Amati, Vassilis Plachouras, Ben He, Craig Macdonald
 * @version $Revision: 1.1 $
 */
public class TRECLMQuerying extends TRECQuerying {
	
	/** The logger used */
	private static Logger logger = Logger.getRootLogger();
	
	/** The default prefix of a language model. */
	protected String DEFAULT_LM_NAMESPACE = "uk.ac.gla.terrier.matching.models.languagemodel.";
	/**
	 * TRECLMQuerying default constructor initialises the inverted index, 
	 * the lexicon and the document index structures.
	 */
	public TRECLMQuerying() {
		super();
		mModel = "LMMatching";
	}
	
	/**
	 * Performs the matching using the specified weighting model 
	 * from the setup and possibly a combination of evidence mechanism.
	 * It parses the file with the queries (the name of the file is defined
	 * in the address_query file), creates the file of results, and for each
	 * query, gets the relevant documents, scores them, and outputs the results
	 * to the result file. The parameter c is not used for language models.
	 * @param c this parameter is not used for language models.
	 * @return String the filename that the results have been written to
	 */
	public String processQueries(double c) {
		return processQueries();
	}

	/**
	 * Performs the matching using the specified weighting model 
	 * from the setup and possibly a combination of evidence mechanism.
	 * It parses the file with the queries (the name of the file is defined
	 * in the address_query file), creates the file of results, and for each
	 * query, gets the relevant documents, scores them, and outputs the results
	 * to the result file. The parameter c is not used for language models.
	 * @param c this parameter is not used for language models.
	 * @param c_set this parameter is not used for language models.
	 * @return String the filename that the results have been written to
	 */
	public String processQueries(double c, boolean c_set) {
		return processQueries();
	}
	
	/**
	 * Performs the matching using the specified weighting model 
	 * from the setup and possibly a combination of evidence mechanism.
	 * It parses the file with the queries (the name of the file is defined
	 * in the address_query file), creates the file of results, and for each
	 * query, gets the relevant documents, scores them, and outputs the results
	 * to the result file.
	 * @return String the filename that the results have been written to
	 */
	public String processQueries() {
		TRECQuery querySource = new TRECQuery();
		String methodName = ApplicationSetup.getProperty("language.model", "PonteCroft");
		
		if (methodName.lastIndexOf('.') < 0)
			methodName = this.DEFAULT_LM_NAMESPACE.concat(methodName);
		wModel = methodName;
			
		//the file to store the results to.
		//resultFile = getResultFile(shortModelName);
		//iterating through the queries
		boolean doneSomeTopics = false;
		while (querySource.hasMoreQueries())
		{
			String query = querySource.nextQuery();
			String qid = querySource.getQueryId();
			//process the query
			long processingStart = System.currentTimeMillis();
			processQueryAndWrite(qid, query, 1.0, false); //the paramemter value is not used
			long processingEnd = System.currentTimeMillis();
				if(logger.isInfoEnabled())
					logger.info("time to process query: " + ((processingEnd - processingStart)/1000.0D));
			doneSomeTopics = true;
		}
		this.finishedQueries();
		resultFile = null;
		querySource.reset();
		if (!doneSomeTopics) 
			logger.error("No queries were processed. Please check the file " + ApplicationSetup.TREC_TOPICS_LIST);
		else 
			if(logger.isInfoEnabled())
				logger.info("Finished topics, executed "+matchingCount+" queries");
		return resultsFilename;
	}
}

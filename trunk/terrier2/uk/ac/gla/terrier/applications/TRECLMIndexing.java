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
 * The Original Code is TRECLMIndexing.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.applications;
import org.apache.log4j.Logger;

import uk.ac.gla.terrier.indexing.CreateDocumentInitialWeightIndex;
import uk.ac.gla.terrier.indexing.CreateTermEstimateIndex;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.utility.ApplicationSetup;
/**
 * This class creates the indices for a language model. 
 */
public class TRECLMIndexing {
	/** The logger used */
	private static Logger logger = Logger.getRootLogger();

	protected Index index;	

	public TRECLMIndexing()
	{
		this(Index.createIndex());
	}

	public TRECLMIndexing(Index i)
	{
		this.index = i;
	}
	
	/** Creates additional structures for language modeling. */
	public void createLMIndex(){
		String modelName = ApplicationSetup.getProperty("language.model", "PonteCroft");
		if (index == null)
		{
			logger.error("No index found. You need to create an index before adding LM stuctures");
		}
		if (! index.hasIndexStructure("inverted"))
		{
			logger.error(index + " does not have an inverted file. LM indexing aborted.");
			return;
		}

		if (! index.hasIndexStructure("direct"))
		{
			logger.error(index + " does not have a direct file. LM indexing aborted.");
			return;
		}
	
		CreateTermEstimateIndex teIndex = new CreateTermEstimateIndex(index, modelName);
		teIndex.createTermEstimateIndex();
		
		CreateDocumentInitialWeightIndex docWIndex = new CreateDocumentInitialWeightIndex(index, modelName);
		docWIndex.createDocumentInitialWeightIndex();
	}
	
	/** 
	 * Used for testing purposes.
	 * @param args the command line arguments.
	 */
	public static void main(String[] args)
	{
		long startTime = System.currentTimeMillis();
		TRECLMIndexing indexing = new TRECLMIndexing();
		indexing.createLMIndex();
		long endTime = System.currentTimeMillis();
		if(logger.isInfoEnabled())
			logger.info("Elapsed time="+((endTime-startTime)/1000.0D));
	}
	
	
}

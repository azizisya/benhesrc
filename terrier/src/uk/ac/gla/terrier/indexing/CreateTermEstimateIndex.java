/*
 * Terrier - Terabyte Retriever 
 * Webpage: http://ir.dcs.gla.ac.uk/terrier 
 * Contact: terrier{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * http://www.gla.ac.uk/
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
 * The Original Code is CreateTermEstimateIndex.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk> (original author)
 */
package uk.ac.gla.terrier.indexing;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.matching.models.languagemodel.LanguageModel;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.InvertedIndex;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.structures.indexing.TermEstimateIndex;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.Rounding;
import uk.ac.gla.terrier.utility.TerrierTimer;
import uk.ac.gla.terrier.utility.ApplicationSetup;
/**
 * This class creates the term estimate index of all terms in vocabulary. This is
 * done for language modeling approach.
 * @author Ben He
 * @version $Revision: 1.1 $
 */

public class CreateTermEstimateIndex {
	protected static Logger logger = Logger.getRootLogger();
	/** The default prefix of a language model. */
	protected String DEFAULT_LM_NAMESPACE = "uk.ac.gla.terrier.matching.models.languagemodel.";
	
	/** The DocumentIndex for retrieval. */
	protected DocumentIndex docIndex;
	
	/** The InvertedIndex for retrieval. */
	protected InvertedIndex invIndex;
	
	/** The Lexicon for retrieval. */
	protected Lexicon lexicon;

	protected Index index;
	
	/** The language model computing the term estimate. */
	protected LanguageModel model;

	protected CollectionStatistics collectionStatistics;
	
	/** An array of term estimates of all terms in vocabulary. */
	protected double[] termEstimates;
	
	/** The data structure of the term esitmates. */

	protected String INDEX_FILENAME;
	
	/**
	 * The default constructor of CreateTermEstimateIndex.
	 * @param modelName The name of the language model.
	 */
	public CreateTermEstimateIndex(String modelName) {
		this(Index.createIndex(), modelName);
	}	

	public CreateTermEstimateIndex(Index i, String modelName) {
		super();
		this.index = i;	
		//load the index
		long startLoading = System.currentTimeMillis();
		docIndex = i.getDocumentIndex();
		lexicon = i.getLexicon();
		invIndex = i.getInvertedIndex();
		collectionStatistics = i.getCollectionStatistics();
		long endLoading = System.currentTimeMillis();
		if(logger.isInfoEnabled()){
		logger.info("time to load indices in memory: " + ((endLoading-startLoading)/1000.0D));
		}
		//load the appropriate lm model	
		if (modelName.lastIndexOf('.') < 0)
			modelName = this.DEFAULT_LM_NAMESPACE.concat(modelName);

		model = null;
		try {
			model = (LanguageModel) Class.forName(modelName).newInstance();
		} 
		catch(InstantiationException ie) {
			logger.fatal("Exception while loading the language model class:" + modelName, ie);
		} catch(IllegalAccessException iae) {
			logger.fatal("Exception while loading the language model class:" + modelName, iae);
		} catch(ClassNotFoundException cnfe) {
			logger.fatal("Exception while loading the language model class:" + modelName , cnfe);
		}	
		INDEX_FILENAME = ApplicationSetup.makeAbsolute(
            i.getPrefix()+"."+ApplicationSetup.getProperty("te.suffix", "te"),
            i.getPath() );
	}
	
	/**
	 * Create the TermEstimateIndex. It computes the average term generation probability for each term in the vocabulary of the collection.
	 *
	 */
	public void createTermEstimateIndex(){
		TerrierTimer timer = null;
		long numberOfUniqueTerms = collectionStatistics.getNumberOfUniqueTerms();
		if(logger.isInfoEnabled()){
		logger.info("number of unique terms: " + numberOfUniqueTerms);
		logger.info("Creating TermEstimateIndex...");
		}
		if(logger.isDebugEnabled()){
			timer = new TerrierTimer();
			timer.setTotalNumber((double)numberOfUniqueTerms);
			timer.start();
		}
		termEstimates = new double[(int)numberOfUniqueTerms];
		for (int i = 0; i < numberOfUniqueTerms; i++){
			lexicon.seekEntry(i);
			int[][] pointers = invIndex.getDocuments(i);
			int[] docids = pointers[0];
			int[] tf = pointers[1];
			double[] docLength = new double[tf.length];
			for (int j = 0; j < docids.length; j++){
				docLength[j] = docIndex.getDocumentLength(docids[j]);
			}
			//termids[i] = i;
			termEstimates[i] = model.averageTermGenerationProbability(tf, docLength);
			if(logger.isDebugEnabled()){
				if ((i+1) % 10000 == 0){
					timer.setRemainingTime((i+1));
					logger.debug("term: " + lexicon.getTerm() +
							", TF: " + lexicon.getTF() +", " +
							Rounding.toString((double)(i+1)/numberOfUniqueTerms*100, 2) +
							"% finished, time remaining: " + timer.toStringMinutesSeconds());
				}
			}
		}
		try{
			DataOutputStream output = new DataOutputStream(
					Files.writeFileStream(INDEX_FILENAME));
			for (int i = 0; i < termEstimates.length; i++)
				output.writeDouble(termEstimates[i]);
			output.close();
		}
		catch(IOException ioe){
			logger.fatal("IO Exception in CreateTermEstimateIndex", ioe);
		}
		if(logger.isDebugEnabled()){
			timer.setBreakPoint();
			logger.debug("Finished creating TermEstimateIndex. Time elapsed: " +
			timer.toStringMinutesSeconds());
		}
		index.addIndexStructure(
			"termestimate", 
			"uk.ac.gla.terrier.structures.indexing.TermEstimateIndex",
			"uk.ac.gla.terrier.structures.Index",
			"index");	
		index.flush();
	}
}

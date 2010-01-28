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
 * The Original Code is CreateDocumentInitialWeightIndex.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk> (original author)
 */
package uk.ac.gla.terrier.indexing;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.matching.models.languagemodel.LanguageModel;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.DirectIndex;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.InvertedIndex;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.structures.indexing.DocumentInitialWeightIndex;
import uk.ac.gla.terrier.structures.indexing.TermEstimateIndex;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.Rounding;
import uk.ac.gla.terrier.utility.TerrierTimer;
import uk.ac.gla.terrier.utility.ApplicationSetup;
/**
 * This class creates the initial weight index of all
 * documents in the collection. This is done for 
 * language modeling approach.
 * @author Ben He
 * @version $Revision: 1.1 $
 */
public class CreateDocumentInitialWeightIndex {
	protected static Logger logger = Logger.getRootLogger();
	/** The default prefix of a language model. */
	protected String DEFAULT_LM_NAMESPACE = "uk.ac.gla.terrier.matching.models.languagemodel.";
	
	/** The DocumentIndex for retrieval. */
	protected DocumentIndex docIndex;
	
	/** The InvertedIndex for retrieval. */
	protected InvertedIndex invIndex;
	
	/** The Lexicon for retrieval. */
	protected Lexicon lexicon;
	
	/** The DirectIndex for retrieval. */
	protected DirectIndex directIndex;
	
	/** The language model computing the term estimate. */
	protected LanguageModel model;

	protected CollectionStatistics collectionStatistics;

	protected Index i;
		
	/** The ids of documents. */
	protected int[] docids;
		
	/** The weight of documents. */
	protected double[] docWeights;
		
	/** The file in which the intial document weights are saved. */
	protected String INDEX_FILENAME;
	
	/** The data structure of the initial document weights. */
	protected DocumentInitialWeightIndex index;
	
	/** The data structure of the term esitmates. */
	protected TermEstimateIndex teIndex;
	/**
	 * The default constructor of CreateDocumentInitialWeightIndex.
	 * @param modelName The name of the applied language model.
	 */
	public CreateDocumentInitialWeightIndex(String modelName) 
	{
		this(Index.createIndex(), modelName);
	}

	public CreateDocumentInitialWeightIndex(Index i, String modelName) {
		long startLoading = System.currentTimeMillis();
		docIndex = i.getDocumentIndex();
		lexicon = i.getLexicon();
		directIndex = i.getDirectIndex();
		collectionStatistics = i.getCollectionStatistics();
		this.i = i;
		long endLoading = System.currentTimeMillis();

		INDEX_FILENAME = i.getPath()
            + ApplicationSetup.FILE_SEPARATOR
            + i.getPrefix()
            + '.'
            + ApplicationSetup.getProperty("dw.suffix", "dw");

		logger.info("time to load document index in memory: " + ((endLoading-startLoading)/1000.0D));
		model = null;
		if (modelName.lastIndexOf('.') < 0)
			modelName = this.DEFAULT_LM_NAMESPACE.concat(modelName);
		try {
			model = (LanguageModel) Class.forName(modelName).newInstance();
		} 
		catch(InstantiationException ie) {
			logger.fatal("Exception while loading the language model class:", ie);
		} catch(IllegalAccessException iae) {
			logger.fatal("Exception while loading the language model class:" , iae);
		} catch(ClassNotFoundException cnfe) {
			logger.fatal("Exception while loading the language model class:\n" , cnfe);
		}	
		this.index = new DocumentInitialWeightIndex(i);
		this.teIndex = new TermEstimateIndex(i);
	}
		
	/**
	 * Create the DocumentInitialWeightIndex. 
	 * All terms are considered as non-query terms at this stage. 
	 */
	public void createDocumentInitialWeightIndex(){
		long numberOfDocuments = collectionStatistics.getNumberOfDocuments();
		logger.debug("number of documents: " + numberOfDocuments);
		this.docWeights = new double[(int)numberOfDocuments];
		long numberOfUniqueTerms = collectionStatistics.getNumberOfUniqueTerms();
			
		//load all TF in memory from lexicon
		if(logger.isDebugEnabled()) {
		logger.debug("loading TF into memory...");
		}
		TerrierTimer timer1 = new TerrierTimer();
		timer1.start();
		double[] TF = new double[(int)numberOfUniqueTerms];
		for (int i = 0; i < numberOfUniqueTerms; i++){
			lexicon.findTerm(i);
			TF[i] = (double)lexicon.getTF();
		}
		timer1.setBreakPoint();
		if(logger.isDebugEnabled()) {
			logger.debug("time to load TF: " + timer1.toStringMinutesSeconds());
		}	
		//compute an initial weight. All terms are considered as unseen terms.
			
		double unseenGlobalWeight = 1d;
		long numberOfTokens = collectionStatistics.getNumberOfTokens();
		model.setNumberOfTokens(numberOfTokens);
		for (int i = 0; i < numberOfUniqueTerms; i++){
			unseenGlobalWeight *= this.model.scoreUnseenNonQuery(TF[i]);
		}
		Arrays.fill(this.docWeights, unseenGlobalWeight);
		if(logger.isDebugEnabled()) {	
			logger.debug("Creating DocumentInitialWeightIndex...");
		}
		TerrierTimer timer = new TerrierTimer();
		timer.start();
		timer.setTotalNumber((double)numberOfDocuments);
			
		for (int i = 0; i < numberOfDocuments; i++){
			int[][] terms = null;
			double docLength = docIndex.getDocumentLength(i);
			if (docLength != 0){
				terms = directIndex.getTerms(i);
				for (int j = 0; j < terms[0].length; j++){
					double unseenNonQuery = model.scoreUnseenNonQuery(TF[terms[0][j]]);
					double seenNonQuery = model.scoreSeenNonQuery((double)terms[1][j],
						  docLength, TF[terms[0][j]], teIndex.getTermEstimateByTermid(terms[0][j]));
					docWeights[i] = docWeights[i]/
						unseenNonQuery*
						seenNonQuery;
				}
			}
			if ((i+1) % 10000 == 0){
				timer.setBreakPoint();
				if(logger.isDebugEnabled()){
				logger.debug("docno: " + docIndex.getDocumentNumber(i) +
							//", length: " + (int)docLength +
							", " + Rounding.toString((double)(i+1)/numberOfDocuments*100, 2) +
							"% finished");// +
				}
							//", elapsed: " + timer.toStringMinutesSeconds());
				timer.setRemainingTime((i+1));
				if(logger.isDebugEnabled()){
				logger.debug(", time remaining: " + timer.toStringMinutesSeconds());
				}
			}
		}
		try{
			DataOutputStream output = new DataOutputStream(
					Files.writeFileStream(INDEX_FILENAME));
			for (int i = 0; i < docWeights.length; i++)
				output.writeDouble(docWeights[i]);
			output.close();
		}
		catch(IOException ioe){
			logger.fatal("An exception occured while trying to write the initial weight index",ioe);
		}
		timer.setBreakPoint();
		if(logger.isInfoEnabled()){
		logger.info("Finished creating DocumentInitialWeightIndex.");
		logger.info("Time elapsed: " + timer.toStringMinutesSeconds());
		}
		this.i.addIndexStructure(
			"documentinitialweight", 
			"uk.ac.gla.terrier.structures.indexing.DocumentInitialWeightIndex",
			"uk.ac.gla.terrier.structures.Index",
			"index");
		this.i.flush();
	}
}

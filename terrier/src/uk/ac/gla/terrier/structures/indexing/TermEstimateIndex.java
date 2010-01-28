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
 * The Original Code is TermEstimateIndex.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk> 
 */
package uk.ac.gla.terrier.structures.indexing;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.structures.LexiconInputStream;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;
/**
 * This class implements a data structure for the term estimate (P_{avg})
 * of language modelling approach to IR.
 */
public class TermEstimateIndex {
	private static Logger logger = Logger.getRootLogger();
	protected final Lexicon lex;
	protected final int numTerms;

	/** The array of term estimate for each term. It is sorted by termid. */
	protected double[] termEstimate;
	/** The filename of the term estimate index on disk. */
	protected String INDEX_FILENAME; 
	/**
	 * The default constructor.
	 */
	public TermEstimateIndex() {
		this( Index.createIndex() );
	}
	public TermEstimateIndex(Index index)
	{
		final String path  = index.getPath();
		final String prefix = index.getPrefix();
		INDEX_FILENAME = ApplicationSetup.makeAbsolute(
			prefix+"."+ApplicationSetup.getProperty("te.suffix", "te"),
			path );
		CollectionStatistics collectionStatistics = index.getCollectionStatistics();
		lex = index.getLexicon();
		numTerms = collectionStatistics.getNumberOfUniqueTerms();

		this.termEstimate = new double[numTerms];
		int[] termids = new int[numTerms];

		//always use a lexiconinputstream, as blocklexicons dont exist past invertedindex creation
		//but check if we're using UTF
		final LexiconInputStream lexin = (LexiconInputStream)index.getIndexStructureInputStream("lexicon");


		for (int i = 0; i < termids.length; i++){
			try{
				lexin.readNextEntry();
				termids[i] = lexin.getTermId();
			}
			catch(IOException ioe){
				logger.error("Problem reading lexicon input stream while loading TermEstimateIndex");
				
			}
		}
		lexin.close();
		
		if (Files.exists(INDEX_FILENAME)){
			try{
				DataInputStream in = new DataInputStream(
					Files.openFileStream(INDEX_FILENAME));
				for (int i = 0; i < collectionStatistics.getNumberOfUniqueTerms(); i++){
					this.termEstimate[termids[i]] = in.readDouble();
				}
				in.close();
			}
			catch(IOException ioe){
				logger.error("Problem reading TermEstimateIndex at "+INDEX_FILENAME, ioe);
			}
		}
	}
	/**
	 * This method prints all the entries in the term estimate index.
	 *
	 */
	public void dumpTermEstimateIndex(){
		try{
			DataInputStream in = new DataInputStream(
					Files.openFileStream(INDEX_FILENAME));
			for (int i = 0; i < numTerms; i++){
				double te = in.readDouble();
				lex.seekEntry(i);
				if(logger.isDebugEnabled()){
					logger.debug(lex.getTerm() + ": " + te);
				}
			}
			in.close();
		}
		catch(IOException ioe){
			logger.warn("Problem in dumpTermEstimateIndex", ioe);
		}
	}
	/**
	 * Get the term estimate of a term by termid.
	 * @param termid The id of term.
	 * @return The term estimate corresponding to the given termid.
	 */
	public double getTermEstimateByTermid(int termid){
		return this.termEstimate[termid];
	}
}

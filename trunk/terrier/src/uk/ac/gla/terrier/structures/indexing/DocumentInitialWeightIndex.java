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
 * The Original Code is DocumentInitialWeightIndex.java.
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

import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;
/**
 * This class implements a data structure for the term estimate (P_{avg})
 * of language modelling approach to IR.
 */
public class DocumentInitialWeightIndex {
	private static Logger logger = Logger.getRootLogger();
	/** The array of term estimate for each term. It is sorted by termid. */
	protected double[] docWeights;
	
	protected String INDEX_FILENAME; 
	
	/**
	 * 
	 */
	public DocumentInitialWeightIndex() {
		this(Index.createIndex());
	}

	public DocumentInitialWeightIndex(Index index) {
		INDEX_FILENAME = index.getPath()
			+ ApplicationSetup.FILE_SEPARATOR
			+ index.getPrefix()
			+ '.'
			+ ApplicationSetup.getProperty("dw.suffix", "dw");
		final int numDocs = index.getCollectionStatistics().getNumberOfDocuments();
		this.docWeights = new double[numDocs];
		if (Files.exists(INDEX_FILENAME)){
			try{
				DataInputStream in = new DataInputStream(Files.openFileStream(INDEX_FILENAME));
				for (int i = 0; i < numDocs; i++){
					this.docWeights[i] = in.readDouble();
				}
				in.close();
			}
			catch(IOException ioe){
				logger.error("Problem loading DocumentInitialWeightIndex", ioe);
			}
		}
	}
	
	public void dumpDocumentInitialWeightIndex(){
		for (int i = 0; i < this.docWeights.length; i++)
			System.out.println(i + ": " + this.docWeights[i]);
	}
	
	public double getDocumentInitialWeight(int docid){
		return this.docWeights[docid];
	}
}

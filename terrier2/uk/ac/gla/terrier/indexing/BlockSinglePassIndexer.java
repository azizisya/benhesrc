
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
 * The Original Code is BlockSinglePassIndexer.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *  Roi Blanco
 *  Craig Macdonald
 */

package uk.ac.gla.terrier.indexing;

import java.io.IOException;

import uk.ac.gla.terrier.structures.indexing.BlockDocumentPostingList;
import uk.ac.gla.terrier.structures.indexing.singlepass.BlockFieldMemoryPostings;
import uk.ac.gla.terrier.structures.indexing.singlepass.BlockFieldPostingInRun;
import uk.ac.gla.terrier.structures.indexing.singlepass.BlockMemoryPostings;
import uk.ac.gla.terrier.structures.indexing.singlepass.BlockPostingInRun;
import uk.ac.gla.terrier.structures.indexing.singlepass.FileRunIteratorFactory;
import uk.ac.gla.terrier.structures.indexing.singlepass.RunsMerger;
import uk.ac.gla.terrier.terms.TermPipeline;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.FieldScore;

/**
 * Indexes a document collection saving block information for the indexed terms.
 * It performs a single pass inversion (see {@link uk.ac.gla.terrier.indexing.BasicSinglePassIndexer}).
 * @author Roi Blanco
 *
 */
public class BlockSinglePassIndexer extends BasicSinglePassIndexer{
	
	/** This class implements an end of a TermPipeline that adds the
	 *  term to the DocumentTree. This TermProcessor does NOT have field
	 *  support.
	 */	 
	protected class BasicTermProcessor implements TermPipeline {
		public void processTerm(String t) {
			//	null means the term has been filtered out (eg stopwords)
			if (t != null) {
				//add term to thingy tree
				((BlockDocumentPostingList)termsInDocument).insert(t, blockId);
				numOfTokensInDocument++;
				if (++numOfTokensInBlock >= BLOCK_SIZE && blockId < MAX_BLOCKS) {
					numOfTokensInBlock = 0;
					blockId++;
				}
			}
		}
	}
	/** 
	 * This class implements an end of a TermPipeline that adds the
	 * term to the DocumentTree. This TermProcessor does have field
	 * support.
	 */
	protected class FieldTermProcessor implements TermPipeline {
		public void processTerm(String t) {
			//	null means the term has been filtered out (eg stopwords)
			if (t != null) {
				//add term to document posting list
				final int[] fieldIds = new int[numFields];
				int i=0;
				for (String fieldName: termFields)
				{
					fieldIds[i] = FieldNames.get(fieldName);
					i++;
				}
				((BlockDocumentPostingList)termsInDocument).insert(t,fieldIds, blockId);
				numOfTokensInDocument++;
				if (++numOfTokensInBlock >= BLOCK_SIZE && blockId < MAX_BLOCKS) {
					numOfTokensInBlock = 0;
					blockId++;
				}
			}
		}
	}

	/** The number of tokens in the current block of the current document. */
	protected int numOfTokensInBlock = 0;
	/** The block number in the current document. */
	protected int blockId;
		/** The maximum number of terms allowed in a block */
	protected int BLOCK_SIZE = ApplicationSetup.BLOCK_SIZE;
	/** 
	 * The maximum number allowed number of blocks in a document. 
	 * After this value, all the remaining terms are in the final block */
	protected int MAX_BLOCKS = ApplicationSetup.MAX_BLOCKS;
	/**
	 * Constructs an instance of this class, where the created data structures
	 * are stored in the given path.
	 * @param pathname String the path in which the created data structures 
	 *		will be saved.
	 */
	/** 
	 * Returns the object that is to be the end of the TermPipeline. 
	 * This method is used at construction time of the parent object. 
	 * @return TermPipeline the last component of the term pipeline.
	 */
	protected TermPipeline getEndOfPipeline() {
		if (FieldScore.USE_FIELD_INFORMATION)
			return new FieldTermProcessor();
		return new BasicTermProcessor();
	}
	
	public BlockSinglePassIndexer(String pathname, String prefix) {
		super(pathname, prefix);
		//delay the execution of init() if we are a parent class
        if (this.getClass() == BlockSinglePassIndexer.class) 
            init();
		invertedIndexClass = "uk.ac.gla.terrier.structures.BlockInvertedIndex";
		invertedIndexInputStreamClass =  "uk.ac.gla.terrier.structures.BlockInvertedIndexInputStream";
	}
	
	protected void createFieldRunMerger(String[][] files) throws IOException{
		merger = new RunsMerger(new FileRunIteratorFactory(files, BlockFieldPostingInRun.class));
	}
	
	protected void createRunMerger(String[][] files) throws Exception{
		merger = new RunsMerger(new FileRunIteratorFactory(files, BlockPostingInRun.class));
	}
	
	protected void createMemoryPostings(){
		if (useFieldInformation) 
			mp = new BlockFieldMemoryPostings();
		else 
			mp = new BlockMemoryPostings();
	}


	protected void createDocumentPostings(){
		termsInDocument = new BlockDocumentPostingList(FieldScore.FIELDS_COUNT);
		blockId = 0;
		numOfTokensInBlock = 0;
	}
	
}

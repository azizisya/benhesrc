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
 * The Original Code is BlockDirectIndexBuilder.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Douglas Johnson <johnsoda{a.}dcs.gla.ac.uk> (original author)
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> 
 */
package uk.ac.gla.terrier.structures.indexing;
import java.io.IOException;

import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.FilePosition;
import uk.ac.gla.terrier.structures.Index;
/**
 * Builds a direct index using block and possibly field information.
 * @author Douglas Johnson &amp; Vassilis Plachouras &amp; Craig Macdonald
 * @version $Revision: 1.1 $
 */
public class BlockDirectIndexBuilder extends DirectIndexBuilder {
	/**
	 * Constructs an instance of the class with 
	 * the given document index.
	 * @param docIndex The document index to be used
	 * @deprecated DocumentIndex is no longer required to use a DirectIndexBuilder
	 */
	public BlockDirectIndexBuilder(DocumentIndex docIndex) {
		super();
	}
	/**
	 * Constructs an instance of the class with
	 * the direct file being written to filename. 
	 * @param filename the non-default filename used for 
	 *		the underlying direct file.
	 */
	public BlockDirectIndexBuilder(String filename) {
		super(filename);
	}

	/** Constructs an instance of the class with the direct file
	  * being written in the index space defined */
	public BlockDirectIndexBuilder(String path, String prefix) {
		super(path, prefix);
	}
	
	public BlockDirectIndexBuilder(Index i)
	{
		super(i);
	}
	
	/**
	 * Adds a document to the direct index and returns the offset 
	 * in the direct index after adding the document. The document 
	 * is passed as an array of termids and frequencies.
	 * @param terms int[][] the array of the 
	 *		document's terms and tfs, blocks, etc.
	 * @return FilePosition the offset of the direct file after 
	 *		 adding the document.
	 * @since 1.1.0
	 */
	public FilePosition addDocument(int[][] terms) throws IOException
	{
		if (saveTagInformation) {
			addFieldDocument(terms);
		} else {
			addNoFieldDocument(terms);
		}
		/* find out where we are */
		FilePosition rtr = getLastEndOffset();
		
		/* flush to disk if necessary */
		if (DocumentsSinceFlush++ >= DocumentsPerFlush)
		{
			flushBuffer();
			resetBuffer();
			DocumentsSinceFlush = 0;
		}
		/* and then return where the position of the last 
		 * write to the DirectIndex */
		return rtr;
	}
	
	/**
	 * When the indexing has reached the end of all collections,
	 * this method writes the buffers on disk and closes the 
	 * corresponding files.
	 */
	public void finishedCollections()
	{
		flushBuffer();
		resetBuffer();
		DocumentsSinceFlush = 0;
		logger.info("flush direct index");
		try{
			close();
		} catch (IOException ioe) { logger.warn(ioe);} 
		if (index != null)
		{
			index.addIndexStructure(
				"direct", 
				"uk.ac.gla.terrier.structures.BlockDirectIndex", 
				"uk.ac.gla.terrier.structures.DocumentIndex,java.lang.String,java.lang.String", 
				"document,path,prefix");
			 index.addIndexStructureInputStream(
                    "direct",
                    "uk.ac.gla.terrier.structures.BlockDirectIndexInputStream",
                    "java.lang.String,java.lang.String,uk.ac.gla.terrier.structures.DocumentIndexInputStream",
                    "path,prefix,document-inputstream");
			index.setIndexProperty("num.direct.fields.bits", ""+fieldTags);
		}
	}
	
	/**
	 * Adds a document to the direct index with field information 
	 * and returns the offset in the direct index after adding 
	 * the document. The document is passed as an array 
	 * of term ids and frequencies
	 * @param postings int[][] the array of 
	 *		the document's terms with field information.
	 * @since 1.1.0
	 */
	protected void addFieldDocument(final int[][] postings) throws IOException {

        //local variables in order to reduce the number
        //of times we need to access a two-dimensional array
        final int[] postings0 = postings[0];
        final int[] postings1 = postings[1];
        final int[] postings2 = postings[2];
        final int[] postings3 = postings[3];
        final int[] postings4 = postings[4];
		int firstId = postings0[0];

        //write the first posting from the term's postings list
        file.writeGamma(firstId+1);                     //write term id
        file.writeUnary(postings1[0]);                //write frequency
        file.writeBinary(fieldTags, postings2[0]);   //write fields if binaryBits>0
        int blockIndex = 0;                             //the index of the current block id
        int blockFrequency = postings3[0];              //the number of block ids to write
        file.writeUnary(blockFrequency + 1);              //write block frequency
		if (blockFrequency > 0)
		{
        	file.writeGamma(postings4[blockIndex]+1); //write the first block id
        	blockIndex++;                                   //move to the next block id
        	for (int i=1; i<blockFrequency; i++) {          //write the next blockFrequency-1 ids
            	//write the gap between consequtive block ids
            	file.writeGamma(postings4[blockIndex]-postings4[blockIndex-1]);
            	blockIndex++;
        	}
		}

        //write the rest of the postings from the term's postings list
        final int length = postings[0].length;
        for (int k = 1; k < length; k++) {
            file.writeGamma(postings0[k] - postings0[k - 1]); //write gap of document ids
            file.writeUnary(postings1[k]);                    //write term frequency
            file.writeBinary(fieldTags, postings2[k]);       //write fields if binaryBits>0
            blockFrequency = postings3[k];                      //number of block ids to write
            file.writeUnary(blockFrequency + 1);                  //write block frequency
			if (blockFrequency > 0)
			{
            	file.writeGamma(postings4[blockIndex]+1);         //write the first block id
            	blockIndex++;                                       //move to the next block id
            	for (int i=1; i<blockFrequency; i++) {
                	//write the gap between consequtive block ids
                	file.writeGamma(postings4[blockIndex]-postings4[blockIndex-1]);
                	blockIndex++;
            	}
			}
        }
    }


	/**
	 * Adds a document to the direct index without field information 
	 * and returns the offset in the direct index after adding the 
	 * document. The document is passed as an array of termids and frequencies
	 * @param postings int[][] the array of the 
	 *		document's terms with field information.
	 * @since 1.1.0
	 */
    protected void addNoFieldDocument(int[][] postings) throws IOException {

        //local variables in order to reduce the number
        //of times we need to access a two-dimensional array
        final int[] postings0 = postings[0];
        final int[] postings1 = postings[1];
        final int[] postings3 = postings[3];
        final int[] postings4 = postings[4];
		int firstId = postings0[0];

        //write the first posting from the term's postings list
        file.writeGamma(firstId+1);                     //write term id
        file.writeUnary(postings1[0]);                //write frequency
        int blockIndex = 0;                             //the index of the current block id
        int blockFrequency = postings3[0];              //the number of block ids to write
		if (blockFrequency > 0)
		{
       		file.writeUnary(blockFrequency + 1);              //write block frequency
        	file.writeGamma(postings4[blockIndex]+1);     //write the first block id
        	blockIndex++;                                   //move to the next block id
        	for (int i=1; i<blockFrequency; i++) {          //write the next blockFrequency-1 ids
            	//write the gap between consequtive block ids
            	file.writeGamma(postings4[blockIndex]-postings4[blockIndex-1]);
            	blockIndex++;
        	}
		}

        //write the rest of the postings from the term's postings list
        final int length = postings0.length;
        for (int k = 1; k < length; k++) {
            file.writeGamma(postings0[k] - postings0[k - 1]); //write gap of document ids
            file.writeUnary(postings1[k]);                    //write term frequency
            blockFrequency = postings3[k];                          //number of block ids to write
            file.writeUnary(blockFrequency + 1);              //write block frequency
			if (blockFrequency > 0)
			{
           		file.writeGamma(postings4[blockIndex]+1);     //write the first block id
            	blockIndex++;                                           //move to the next block id
            	for (int i=1; i<blockFrequency; i++) {
                	//write the gap between consequtive block ids
                	file.writeGamma(postings4[blockIndex]-postings4[blockIndex-1]);
                	blockIndex++;
            	}
			}
        }
    }
}

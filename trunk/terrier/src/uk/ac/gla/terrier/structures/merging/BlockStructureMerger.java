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
 * The Original Code is BlockStructureMerger.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk> (original author) 
 */
package uk.ac.gla.terrier.structures.merging;
import java.io.IOException;
import java.util.Date;
import uk.ac.gla.terrier.compression.BitOut;
import uk.ac.gla.terrier.compression.BitOutputStream;
import uk.ac.gla.terrier.sorting.SortAscendingQuadrupleVectors;
import uk.ac.gla.terrier.sorting.SortAscendingQuintupleVectors;
import uk.ac.gla.terrier.structures.BlockDirectInvertedOutputStream;
import uk.ac.gla.terrier.structures.DirectIndex;
import uk.ac.gla.terrier.structures.DirectInvertedOutputStream;
import uk.ac.gla.terrier.structures.DocumentIndexInputStream;
import uk.ac.gla.terrier.structures.FilePosition;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.indexing.DocumentIndexBuilder;
import uk.ac.gla.terrier.utility.ApplicationSetup;


/**
 * This class merges two sets of data structures (ie direct index, 
 * inverted index, document index, lexicon and statistics), created by 
 * Terrier with position information (blocks) and possibly field 
 * information, into one set of data structures. 
 *
 * 
 * @author Vassilis Plachouras and Craig Macdonald
 * @version $Revision: 1.1 $
 * @see uk.ac.gla.terrier.structures.merging.StructureMerger
 */
public class BlockStructureMerger extends StructureMerger {
	
	/**
	 * A constructor that sets the filenames of the inverted
	 * files to merge
	 * @param _filename1 the first inverted file to merge
	 * @param _filename2 the second inverted file to merge
	 * @deprecated
	 */
	public BlockStructureMerger(String _filename1, String _filename2) {
		super(_filename1, _filename2);
		directFileOutputStreamClass = BlockDirectInvertedOutputStream.class;
		directFileInputClass = "uk.ac.gla.terrier.structures.BlockDirectIndex";
		directFileInputStreamClass = "uk.ac.gla.terrier.structures.BlockDirectIndexInputStream";
		invertedFileOutputStreamClass = BlockDirectInvertedOutputStream.class;
		invertedFileInputClass = "uk.ac.gla.terrier.structures.BlockInvertedIndex";
		invertedFileInputStreamClass = "uk.ac.gla.terrier.structures.BlockInvertedIndexInputStream";
	}
	
	public BlockStructureMerger(Index _srcIndex1, Index _srcIndex2, Index _destIndex)
	{
		super(_srcIndex1, _srcIndex2, _destIndex);
		directFileOutputStreamClass = BlockDirectInvertedOutputStream.class;
		directFileInputClass = "uk.ac.gla.terrier.structures.BlockDirectIndex";
		directFileInputStreamClass = "uk.ac.gla.terrier.structures.BlockDirectIndexInputStream";
		invertedFileOutputStreamClass = BlockDirectInvertedOutputStream.class;		
		invertedFileInputClass = "uk.ac.gla.terrier.structures.BlockInvertedIndex";
		invertedFileInputStreamClass = "uk.ac.gla.terrier.structures.BlockInvertedIndexInputStream";
	}

	/** write Block postings.
	  * @deprecated Use BlockDirectInvertedOutputStream instead */
	public static void writeBlockPostings(int[][] postings, int firstId, BitOutputStream output, int binaryBits)
            throws IOException {
        if (binaryBits>0)
            writeFieldPostings(postings, firstId, output, binaryBits);
        else
            writeNoFieldPostings(postings, firstId, output);
    }
	
	
	/**
	 * Merges the two direct files and the corresponding document id files.
	 */
	protected void mergeDirectFiles() {
		try {
		
			final DocumentIndexBuilder docidOutput = new DocumentIndexBuilder(destIndex);
			
			DirectInvertedOutputStream dfOutput = null;
			try{
				dfOutput = 
					(DirectInvertedOutputStream)invertedFileOutputStreamClass
					.getConstructor(String.class,Integer.TYPE)
					.newInstance(destIndex.getPath() + ApplicationSetup.FILE_SEPARATOR +  
								destIndex.getPrefix() + ApplicationSetup.DF_SUFFIX,
								binaryBits);
			} catch (Exception e) {
				logger.error("Couldn't create specified DirectInvertedOutputStream", e);
				return;
			}
				
			
			final DocumentIndexInputStream docidInput1 = (DocumentIndexInputStream)srcIndex1.getIndexStructureInputStream("document");
			final DirectIndex dfInput1 = srcIndex1.getDirectIndex();
	
			
			//traversing the first set of files, without any change
			while (docidInput1.readNextEntry() >= 0) {
				if (docidInput1.getDocumentLength() > 0)
				{
					final int[][] terms = dfInput1.getTerms(docidInput1.getDocumentId());
					dfOutput.writePostings(terms, terms[0][0]+1);
				}
				long endByte = dfOutput.getByteOffset();
				byte endBit = dfOutput.getBitOffset();
				endBit--;

				if (endBit < 0 && endByte > 0) {
					endBit = 7;
					endByte--;
				}
				
				docidOutput.addEntryToBuffer(docidInput1.getDocumentNumber(), 
									 docidInput1.getDocumentLength(),
									 new FilePosition(endByte, endBit));
			}
			
			//the output direct file
			final BitOut dfOutput2 = dfOutput.getBitOut();			
			final DocumentIndexInputStream docidInput2 = (DocumentIndexInputStream)srcIndex2.getIndexStructureInputStream("document");
			final DirectIndex dfInput2 = srcIndex2.getDirectIndex();
			
			while (docidInput2.readNextEntry() >= 0) {
				if (docidInput2.getDocumentLength() > 0)
				{
					int[][] terms = dfInput2.getTerms(docidInput2.getDocumentId());
					final int length = terms[0].length;

					//define an index array in order to access the block frequencies and
					//block ids, after sorting according to the new term identifiers
					int[] blockIdIndex = new int[length];

					//update the term identifiers
					terms[0][0] = termcodeHashmap.get(terms[0][0]);
					for (int j=1; j<length; j++) {
						terms[0][j] = termcodeHashmap.get(terms[0][j]);
						blockIdIndex[j] = blockIdIndex[j-1]+terms[3][j-1];
					}
										
					if (binaryBits>0) {
						SortAscendingQuintupleVectors.sort(terms[0], terms[1], terms[2], terms[3], blockIdIndex);
					} else {
						SortAscendingQuadrupleVectors.sort(terms[0], terms[1], terms[3], blockIdIndex);
					}
					
					//writePostings(terms, terms[0][0]+1, dfOutput, binaryBits);
					dfOutput2.writeGamma(terms[0][0]+1);
					dfOutput2.writeUnary(terms[1][0]);
					if (binaryBits>0) { 
						dfOutput2.writeBinary(binaryBits, terms[2][0]);
					}
					int blockFrequency = terms[3][0];
					dfOutput2.writeUnary(blockFrequency+1);
					int blockIndex = blockIdIndex[0];
					if (blockFrequency > 0)
					{
						dfOutput2.writeGamma(terms[4][blockIndex]+1);
						blockIndex++;
						for (int k=1; k<blockFrequency; k++) {
							dfOutput2.writeGamma(terms[4][blockIndex]-terms[4][blockIndex-1]);
							blockIndex++;
						}
					}
					
					for (int j = 1; j < length; j++) {
						dfOutput2.writeGamma(terms[0][j] - terms[0][j - 1]);
						dfOutput2.writeUnary(terms[1][j]);
						if (binaryBits > 0) {
							dfOutput2.writeBinary(binaryBits,terms[2][j]);
						}
						blockFrequency = terms[3][j];
						dfOutput2.writeUnary(blockFrequency +1);
						blockIndex = blockIdIndex[j];
						if (blockFrequency > 0)
						{
							dfOutput2.writeGamma(terms[4][blockIndex]+1);
							blockIndex++;
							for (int k=1; k<blockFrequency; k++) {
								dfOutput2.writeGamma(terms[4][blockIndex]-terms[4][blockIndex-1]);
								blockIndex++;
							}
						}
					}
				}
				long endByte = dfOutput.getByteOffset();
				byte endBit = dfOutput.getBitOffset();
				endBit--;

				if (endBit < 0 && endByte > 0) {
					endBit = 7;
					endByte--;
				}
				
				docidOutput.addEntryToBuffer(docidInput2.getDocumentNumber(), 
									 docidInput2.getDocumentLength(),
									 new FilePosition(endByte, endBit));
			
			}

			dfOutput.close();
			docidOutput.finishedCollections();
			docidOutput.close();
			docidInput2.close();
			docidInput1.close();
			dfInput1.close();
			dfInput2.close();
			destIndex.addIndexStructure(
					"direct", 
					directFileInputClass, 
					"uk.ac.gla.terrier.structures.DocumentIndex,java.lang.String,java.lang.String", 
					"document,path,prefix");
			destIndex.addIndexStructureInputStream(
					"direct", 
					directFileInputStreamClass, 
					"uk.ac.gla.terrier.structures.DocumentIndexInputStream,java.lang.String,java.lang.String", 
					"document-inputstream,path,prefix");
			destIndex.flush();
			
		} catch(IOException ioe) {
			logger.error("IOException while merging df and docid files.", ioe);
		}
	}
	


	/** usage: java uk.ac.gla.terrier.structures.merging.BlockStructureMerger [binary bits] [inverted file 1] [inverted file 2] [output inverted file]
     */
	public static void main(String[] args) {
		if (args.length != 7)
		{
			logger.fatal("usage: java uk.ac.gla.terrier.structures.merging.BlockStructureMerger [binary bits] srcPath1 srcPrefix1 srcPath2 srcPrefix2 destPath1 destPrefix1 ");
			logger.fatal("Exiting ...");
			System.exit(1);
		}
		
		int bits = Integer.parseInt(args[0]);
		
		Index indexSrc1 = Index.createIndex(args[1], args[2]);
		Index indexSrc2 = Index.createIndex(args[3], args[4]);
		Index indexDest = Index.createNewIndex(args[5], args[6]);
		
		StructureMerger sMerger = new BlockStructureMerger(indexSrc1, indexSrc2, indexDest);
		sMerger.setNumberOfBits(bits);
		long start = System.currentTimeMillis();
		logger.info("started at " + (new Date()));
		if (ApplicationSetup.getProperty("merger.onlylexicons","false").equals("true")) {
			sMerger.mergeLexicons();
		} else if (ApplicationSetup.getProperty("merger.onlydocids","false").equals("true")) {
			sMerger.mergeDocumentIndexFiles();
		} else {
			sMerger.mergeStructures();
		}
		
		logger.info("finished at " + (new Date()));
		long end = System.currentTimeMillis();
		logger.info("time elapsed: " + ((end-start)*1.0d/1000.0d) + " sec.");
	}
	
	
	
	
	
	
	
	
	
	
	
	
	/** write Block postings with fields.
	* @deprecated Use BlockDirectInvertedOutputStream instead */	
	public static void  writeFieldPostings(int[][] postings, int firstId, final BitOutputStream output, final int binaryBits)
	throws IOException {

		//local variables in order to reduce the number
		//of times we need to access a two-dimensional array
		final int[] postings0 = postings[0];
		final int[] postings1 = postings[1];
		final int[] postings2 = postings[2];
		final int[] postings3 = postings[3];
		final int[] postings4 = postings[4];
		
		//write the first posting from the term's postings list
		output.writeGamma(firstId);						//write document id 
		output.writeUnary(postings1[0]);    			//write frequency
		output.writeBinary(binaryBits, postings2[0]);	//write fields if binaryBits>0
		int blockIndex = 0;								//the index of the current block id
		int blockFrequency = postings3[0];				//the number of block ids to write
		output.writeUnary(blockFrequency);    			//write block frequency
		output.writeGamma(postings4[blockIndex]+1);	//write the first block id
		blockIndex++;									//move to the next block id
		for (int i=1; i<blockFrequency; i++) {			//write the next blockFrequency-1 ids
			//write the gap between consequtive block ids
			output.writeGamma(postings4[blockIndex]-postings4[blockIndex-1]);
			blockIndex++;
		}
		
		//write the rest of the postings from the term's postings list
		final int length = postings[0].length;
		for (int k = 1; k < length; k++) {
			output.writeGamma(postings0[k] - postings0[k - 1]);	//write gap of document ids
			output.writeUnary(postings1[k]);					//write term frequency
			output.writeBinary(binaryBits, postings2[k]);		//write fields if binaryBits>0
			blockFrequency = postings3[k];						//number of block ids to write
			output.writeUnary(blockFrequency);					//write block frequency
			output.writeGamma(postings4[blockIndex]+1);			//write the first block id
			blockIndex++;										//move to the next block id
			for (int i=1; i<blockFrequency; i++) {
				//write the gap between consequtive block ids
				output.writeGamma(postings4[blockIndex]-postings4[blockIndex-1]);
				blockIndex++;
			}
		}
	}

	/** write Block postings with fields.
	* @deprecated Use BlockDirectInvertedOutputStream instead */	
	public static void writeNoFieldPostings(int[][] postings, int firstId, final BitOutputStream output) 
		throws IOException {
		
		//local variables in order to reduce the number
		//of times we need to access a two-dimensional array
		final int[] postings0 = postings[0];
		final int[] postings1 = postings[1];
		final int[] postings3 = postings[3];
		final int[] postings4 = postings[4];
		
		//write the first posting from the term's postings list
		output.writeGamma(firstId);						//write document id 
		output.writeUnary(postings1[0]);    			//write frequency
		int blockIndex = 0;								//the index of the current block id
		int blockFrequency = postings3[0];				//the number of block ids to write
		output.writeUnary(blockFrequency);    			//write block frequency
		output.writeGamma(postings4[blockIndex]+1);		//write the first block id
		blockIndex++;									//move to the next block id
		for (int i=1; i<blockFrequency; i++) {			//write the next blockFrequency-1 ids
			//write the gap between consequtive block ids
			output.writeGamma(postings4[blockIndex]-postings4[blockIndex-1]);
			blockIndex++;
		}
		
		//write the rest of the postings from the term's postings list
		final int length = postings0.length;
		for (int k = 1; k < length; k++) {
			output.writeGamma(postings0[k] - postings0[k - 1]);	//write gap of document ids
			output.writeUnary(postings1[k]);					//write term frequency
			blockFrequency = postings3[k];							//number of block ids to write
			output.writeUnary(blockFrequency);				//write block frequency
			output.writeGamma(postings4[blockIndex]+1);		//write the first block id
			blockIndex++;											//move to the next block id
			for (int i=1; i<blockFrequency; i++) {
				//write the gap between consequtive block ids
				output.writeGamma(postings4[blockIndex]-postings4[blockIndex-1]);
				blockIndex++;
			}
		}		
	}
}


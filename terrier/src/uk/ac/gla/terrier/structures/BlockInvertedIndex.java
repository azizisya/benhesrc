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
 * The Original Code is BlockInvertedIndex.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *	 Douglas Johnson <johnsoda{a.}dcs.gla.ac.uk> (original author)
 *	 Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk> 
 */
package uk.ac.gla.terrier.structures;
import gnu.trove.TIntArrayList;

import java.io.IOException;

import uk.ac.gla.terrier.compression.BitIn;
import uk.ac.gla.terrier.utility.FieldScore;
/**
 * This class implements the block field inverted 
 * index for performing retrieval.
 * @author Douglas Johnson
 * @version $Revision: 1.1 $
 */
public class BlockInvertedIndex extends InvertedIndex implements IndexConfigurable {
	protected int DocumentBlockCountDelta = 1;
	protected BlockInvertedIndex() {}

	/**
	 * Creates an instance of the BlockInvertedIndex class 
	 * using the given lexicon.
	 * @param lexicon The lexicon used for retrieval
	 */
	public BlockInvertedIndex(Lexicon lexicon) {
		super(lexicon);
	}

	public BlockInvertedIndex(Lexicon lexicon, String path, String prefix) {
		super(lexicon, path, prefix);
	}
	
	/**
	 * Creates an instance of the BlockInvertedIndex class 
	 * using the given lexicon.
	 * @param lexicon The lexicon used for retrieval
	 * @param filename the name of the inverted file
	 */
	public BlockInvertedIndex(Lexicon lexicon, String filename) {
		super(lexicon, filename);
	}

	/** let it know which index to use */
	public void setIndex(Index i)
	{
		DocumentBlockCountDelta = i.getIntIndexProperty("blocks.invertedindex.countdelta", 1);
	}

	/**
	 * Prints out the block inverted index file.
	 */
	public void print() {
		for (int i = 0; i < lexicon.getNumberOfLexiconEntries(); i++) {
			lexicon.findTerm(i);
			System.out.print("Term ("+lexicon.getTerm()+","+i+") : ");
			int[][] documents = getDocuments(i);
			int blockindex = 0;
			for (int j = 0; j < documents[0].length; j++) {
				System.out.print(
					"("
						+ documents[0][j]
						+ ", "
						+ documents[1][j]
						+ ", ");
				if (FieldScore.USE_FIELD_INFORMATION)
				{
					System.out.print(documents[2][j]
					+ ", ");
				}
				System.out.print( documents[3][j]);
				
				for (int k = 0; k < documents[3][j]; k++) {
					System.out.print(", B" + documents[4][blockindex]);
					blockindex++;
				}
				System.out.print(")");
			}
			System.out.println();
		}
	}
	/**
	 * Returns a 2D array containing the document ids, 
	 * the term frequencies, the field scores the block frequencies and 
	 * the block ids for the given documents. 
	 * @return int[][] the five dimensional [5][] array containing 
	 *				 the document ids, frequencies, field scores and block 
	 *				 frequencies, while the last vector contains the 
	 *				 block identifiers and it has a different length from 
	 *				 the document identifiers.
	 * @param startOffset start byte of the postings in the inverted file
	 * @param startBitOffset start bit of the postings in the inverted file
	 * @param endOffset end byte of the postings in the inverted file
	 * @param endBitOffset end bit of the postings in the inverted file
	 * @param df the number of postings to expect 
	 */

	public int[][] getDocuments(final long startOffset, final byte startBitOffset, final long endOffset, final byte endBitOffset, final int df) {
		
		final int fieldCount = FieldScore.FIELDS_COUNT;
		final boolean loadTagInformation = FieldScore.USE_FIELD_INFORMATION;
		
		final int[][] documentTerms = new int[5][];
		documentTerms[0] = new int[df];
		documentTerms[1] = new int[df];
		documentTerms[2] = new int[df];
		documentTerms[3] = new int[df];
		final TIntArrayList blockids = new TIntArrayList(df); //ideally we'd have TF here

		try{
		
			final BitIn file = this.file.readReset(startOffset, startBitOffset, endOffset, endBitOffset);
	
			if (loadTagInformation) { //if there are tag information to process
				//documentTerms[2] = new int[df]; 
				documentTerms[0][0] = file.readGamma() - 1;
				documentTerms[1][0] = file.readUnary();
				documentTerms[2][0] = file.readBinary(fieldCount);
				int blockfreq = documentTerms[3][0] = file.readUnary() - DocumentBlockCountDelta;
				int tmpBlocks[] = new int[blockfreq];
				int previousBlockId = -1;
				for(int j=0;j<blockfreq;j++)
				{
					tmpBlocks[j] = previousBlockId = file.readGamma() + previousBlockId;
				}
				blockids.add(tmpBlocks);
				
				for (int i = 1; i < df; i++) {					
					documentTerms[0][i]  = file.readGamma() + documentTerms[0][i - 1];
					documentTerms[1][i]  = file.readUnary();
					documentTerms[2][i]  = file.readBinary(fieldCount);
					blockfreq = documentTerms[3][i] = file.readUnary() - DocumentBlockCountDelta;
					tmpBlocks = new int[blockfreq];
					previousBlockId = -1;
					for(int j=0;j<blockfreq;j++)
					{
						tmpBlocks[j] = previousBlockId = file.readGamma() + previousBlockId;
					}
					blockids.add(tmpBlocks);
				}
			} else { //no tag information to process					
				
				documentTerms[0][0] = file.readGamma() - 1;
				documentTerms[1][0] = file.readUnary();
				
				int blockfreq = documentTerms[3][0] = file.readUnary() - DocumentBlockCountDelta;
				int tmpBlocks[] = new int[blockfreq];
				int previousBlockId = -1;
				for(int j=0;j<blockfreq;j++)
				{
					tmpBlocks[j] = previousBlockId = file.readGamma() + previousBlockId;
				}
				blockids.add(tmpBlocks);
				
				for (int i = 1; i < df; i++) {					
					documentTerms[0][i]  = file.readGamma() + documentTerms[0][i - 1];
					documentTerms[1][i]  = file.readUnary();

					blockfreq = documentTerms[3][i] = file.readUnary() - DocumentBlockCountDelta;
					tmpBlocks = new int[blockfreq];
					previousBlockId = -1;
					for(int j=0;j<blockfreq;j++)
					{
						tmpBlocks[j] = previousBlockId = file.readGamma() + previousBlockId;
					}
					blockids.add(tmpBlocks);
				}
			}
			documentTerms[4] = blockids.toNativeArray();
			return documentTerms;
		} catch (IOException ioe) {
			logger.error("Problem reading block inverted index", ioe);
			return null;
		}
	}


	//* @param termid the id of the term whose documents we are looking for.
	//public int[][] getDocuments(int termid) {
	/*public int[][] getDocuments(final long startOffset, final byte startBitOffset, final long endOffset, final byte endBitOffset, int df) {

		//boolean found = lexicon.findTerm(termid);
		final byte startBitOffset = lexicon.getStartBitOffset();
		final long startOffset = lexicon.getStartOffset();
		final byte endBitOffset = lexicon.getEndBitOffset();
		final long endOffset = lexicon.getEndOffset();

		final int FIELDS_COUNT = FieldScore.FIELDS_COUNT;

		// TODO use heuristics here like we do in InvertedIndex.java
		 // for setting a good guess of the arraylist sizes. 
		TIntArrayList temporaryDocids = new TIntArrayList();
		TIntArrayList temporaryTFs = new TIntArrayList();
		TIntArrayList temporaryFields = new TIntArrayList();
		TIntArrayList temporaryBlockFreq = new TIntArrayList();
		TIntArrayList temporaryBlockIds = new TIntArrayList();
		int previousDocid = -1;
			
		//ArrayList temporaryTerms = new ArrayList();
		//ArrayList temporaryBlockids = new ArrayList();
		//int blockcount = 0;
		try{
			final BitIn file = this.file.readReset(startOffset, startBitOffset, endOffset, endBitOffset);
			//boolean hasMore = false;
			while (((file.getByteOffset() + startOffset) < endOffset)
				|| (((file.getByteOffset() + startOffset) == endOffset)
					&& (file.getBitOffset() < endBitOffset))) {
	
				temporaryDocids.add(previousDocid = file.readGamma() + previousDocid);
				temporaryTFs.add(file.readUnary());
				temporaryFields.add(file.readBinary(FIELDS_COUNT));
				
				/*int docId = file.readGamma();
				/int[] tmp = new int[4];
				tmp[0] = docId;
				tmp[1] = file.readUnary();
				tmp[2] = file.readBinary(FIELDS_COUNT);
				
				final int blockfreq = file.readUnary();
				temporaryBlockFreq.add(blockfreq);
				//tmp[3] = blockfreq;
				//System.out.print("docid="+previousDocid + "blockfreq="+blockfreq);
	
				int[] tmp2 = new int[blockfreq];
				int previousBlockId = -1;
				//System.out.print(" blocks=");
				for (int i = 0; i < blockfreq; i++) {
					tmp2[i] = previousBlockId = file.readGamma() + previousBlockId;
					 //System.out.print(previousBlockId + ",");
					//blockcount++;
				}
				// System.out.println("");
				//temporaryTerms.add(tmp);
				//temporaryBlockids.add(tmp2);
				temporaryBlockIds.add(tmp2);
			}
			int[][] documentTerms = new int[5][];
			documentTerms[0] = temporaryDocids.toNativeArray(); //new int[temporaryTerms.size()];
			documentTerms[1] = temporaryTFs.toNativeArray(); //new int[temporaryTerms.size()];
			documentTerms[2] = temporaryFields.toNativeArray(); //new int[temporaryTerms.size()];
			documentTerms[3] = temporaryBlockFreq.toNativeArray(); //new int[temporaryTerms.size()];
			documentTerms[4] =	temporaryBlockIds.toNativeArray(); //new int[blockcount];
			/*
			documentTerms[0][0] = ((int[]) temporaryTerms.get(0))[0] - 1;
			documentTerms[1][0] = ((int[]) temporaryTerms.get(0))[1];
			documentTerms[2][0] = ((int[]) temporaryTerms.get(0))[2];
			documentTerms[3][0] = ((int[]) temporaryTerms.get(0))[3];
			int[] blockids = ((int[]) temporaryBlockids.get(0));
			documentTerms[4][0] = blockids[0] - 1;
			for (int i = 1; i < blockids.length; i++) {
				documentTerms[4][i] = blockids[i] + documentTerms[4][i - 1];
			}
			int blockindex = blockids.length;
			if (documentTerms[0].length > 1) {
				for (int i = 1; i < documentTerms[0].length; i++) {
					int[] tmpMatrix = (int[]) temporaryTerms.get(i);
					documentTerms[0][i] = tmpMatrix[0] + documentTerms[0][i - 1];
					documentTerms[1][i] = tmpMatrix[1];
					documentTerms[2][i] = tmpMatrix[2];
					documentTerms[3][i] = tmpMatrix[3];
					blockids = ((int[]) temporaryBlockids.get(i));
					documentTerms[4][blockindex] = blockids[0] - 1;
					blockindex++;
					for (int j = 1; j < blockids.length; j++) {
						documentTerms[4][blockindex] =
							blockids[j] + documentTerms[4][blockindex - 1];
						blockindex++;
					}
				}
			}
			return documentTerms;
		}catch (IOException ioe) {
			logger.error("Problem reading direct index", ioe);
			return null;
		}
	}*/


	/*public int[][] getDocumentsWithoutBlocks(int termid, int startDocid, int endDocid) {
		if (! lexicon.findTerm(termid))
			return null;
	
		byte startBitOffset = lexicon.getStartBitOffset();
		long startOffset = lexicon.getStartOffset();
		byte endBitOffset = lexicon.getEndBitOffset();
		long endOffset = lexicon.getEndOffset();
		// TODO use heuristics here like we do in InvertedIndex.java
		// for setting a good guess of the arraylist sizes. 
		ArrayList<int[]> temporaryTerms = new ArrayList<int[]>();
		//int blockcount = 0;
		try{
			final BitIn file = this.file.readReset(startOffset, startBitOffset, endOffset, endBitOffset);
			//boolean hasMore = false;
			final int fieldCount = FieldScore.FIELDS_COUNT;
			while (((file.getByteOffset() + startOffset) < endOffset)
					|| (((file.getByteOffset() + startOffset) == endOffset)
					&& (file.getBitOffset() < endBitOffset))) {
				int docId = file.readGamma();
				int[] tmp = new int[3];
				tmp[0] = docId;
				tmp[1] = file.readUnary();
				tmp[2] = file.readBinary(fieldCount);
			 
				//read the blocks, but dont save them
				int blockfreq = file.readUnary();
				for (int i = 0; i < blockfreq; i++) {
					file.readGamma();
				 }
				if (docId >= startDocid && docId <=endDocid){
					temporaryTerms.add(tmp);		
				}
			}
			int[][] documentTerms = new int[3][];
			if (temporaryTerms.size()>0){
				documentTerms[0] = new int[temporaryTerms.size()];
				documentTerms[1] = new int[temporaryTerms.size()];
				documentTerms[2] = new int[temporaryTerms.size()];
	 
				documentTerms[0][0] = ((int[]) temporaryTerms.get(0))[0] - 1;
				documentTerms[1][0] = ((int[]) temporaryTerms.get(0))[1];
				documentTerms[2][0] = ((int[]) temporaryTerms.get(0))[2];
		 
				if (documentTerms[0].length > 1) {
					for (int i = 1; i < documentTerms[0].length; i++) {
						int[] tmpMatrix = (int[]) temporaryTerms.get(i);
						documentTerms[0][i] = tmpMatrix[0] + documentTerms[0][i - 1];
						documentTerms[1][i] = tmpMatrix[1];
						documentTerms[2][i] = tmpMatrix[2];
			 		}
				}
			}
			return documentTerms;
		} catch (IOException ioe) {
			logger.error("Problem reading direct index", ioe);
			return null;
		}
	}
	*/
	public int[][] getDocuments(int termid) {
		 LexiconEntry lEntry = lexicon.getLexiconEntry(termid);
		if (lEntry == null)
			return null;
		return getDocuments(lEntry.startOffset,
			lEntry.startBitOffset,
			lEntry.endOffset,
			lEntry.endBitOffset, lEntry.n_t);
	}
	public int[][] getDocumentsWithoutBlocks(int termid) {
		LexiconEntry lEntry = lexicon.getLexiconEntry(termid);
		if (lEntry == null)
			return null;
		return getDocumentsWithoutBlocks(lEntry.startOffset,
			lEntry.startBitOffset,
			lEntry.endOffset,
			lEntry.endBitOffset, lEntry.n_t);
	}

	public int[][] getDocumentsWithoutBlocks(LexiconEntry lEntry)
	{
		return getDocumentsWithoutBlocks(
			lEntry.startOffset,
			lEntry.startBitOffset,
			lEntry.endOffset,
			lEntry.endBitOffset, lEntry.n_t);
	}

	public int[][] getDocumentsWithoutBlocks(long startOffset,  byte startBitOffset, long endOffset, byte endBitOffset, int df)
	{	
		int[][] documentTerms = null;
		try{
			final BitIn file = this.file.readReset(startOffset, startBitOffset, endOffset, endBitOffset);
			final int fieldCount = FieldScore.FIELDS_COUNT;
			 final boolean loadTagInformation = FieldScore.USE_FIELD_INFORMATION;
 			if (loadTagInformation) { //if there are tag information to process		 
				documentTerms = new int[3][df];
				documentTerms[0][0] = file.readGamma() - 1;
				documentTerms[1][0] = file.readUnary();
				documentTerms[2][0] = file.readBinary(fieldCount);
				//read the blocks, but dont save them
				int blockfreq = file.readUnary() - DocumentBlockCountDelta;
				for (int j = 0; j < blockfreq; j++) {
					file.readGamma();
				 }
				for (int i = 1; i < df; i++) {
					documentTerms[0][i]  = file.readGamma() + documentTerms[0][i - 1];
					documentTerms[1][i]  = file.readUnary();
					documentTerms[2][i]  = file.readBinary(fieldCount);
					//read the blocks, but dont save them
			   		blockfreq = file.readUnary() - DocumentBlockCountDelta;
					for (int j = 0; j < blockfreq; j++) {
						file.readGamma();
				 	}
				}
			} else { //no tag information to process					
				documentTerms = new int[2][df];
				documentTerms[0][0] = file.readGamma() - 1;
				documentTerms[1][0] = file.readUnary();
				//read the blocks, but dont save them
				int blockfreq = file.readUnary() - DocumentBlockCountDelta;
				for (int j = 0; j < blockfreq; j++) {
					file.readGamma();
				 }
				for(int i = 1; i < df; i++){
					documentTerms[0][i] = file.readGamma() + documentTerms[0][i - 1];
					documentTerms[1][i] = file.readUnary();
					//read the blocks, but dont save them
					blockfreq = file.readUnary() - DocumentBlockCountDelta;
					for (int j = 0; j < blockfreq; j++) {
						file.readGamma();
				 	}
				}
			}
			return documentTerms;
		} catch (IOException ioe) {
			logger.error("Problem reading inverted index", ioe);
			return null;
		}
	}
}

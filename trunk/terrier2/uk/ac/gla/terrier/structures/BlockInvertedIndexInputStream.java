/*
 * Terrier - Terabyte Retriever
 * Webpage: http://ir.dcs.gla.ac.uk/terrier
 * Contact: terrier{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * http://www.gla.uk
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
 * The Original Code is BlockInvertedIndexInputStream.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original author)
 *   
 */
package uk.ac.gla.terrier.structures;

import java.io.IOException;
import uk.ac.gla.terrier.structures.LexiconInputStream;
import gnu.trove.TIntArrayList;
import uk.ac.gla.terrier.compression.BitIn;
import uk.ac.gla.terrier.utility.FieldScore;

/** Reads a BlockInvertedIndex as a stream
  * @author Craig Macdonald
  * @since 2.0
  * @version $Revision: 1.1 $
  */
public class BlockInvertedIndexInputStream extends InvertedIndexInputStream implements IndexConfigurable 
{
    protected int DocumentBlockCountDelta = 1;
	/** Make a new BlockInvertedIndexInputStream from the specified path/prefix combo. The LexiconInputStream
	  * is required to determine the offsets and the document frequency - ie number of postings for
 	  * each term. */
	public BlockInvertedIndexInputStream(String path, String prefix, LexiconInputStream lis) throws IOException
	{
		super(path, prefix, lis);
	}
	
	/** Make a new BlockInvertedIndexInputStream from the specified filename. The LexiconInputStream
	  * is required to determine the offsets and the document frequency - ie number of postings for
 	  * each term.
	  * @param filename Location of the inverted file to open */
	public BlockInvertedIndexInputStream(String filename, LexiconInputStream lis) throws IOException
	{
		super(filename, lis);
	}

	public BlockInvertedIndexInputStream(BitIn invFile, LexiconInputStream lis) throws IOException
	{
		super(invFile, lis);
	}

    /** let it know which index to use */
    public void setIndex(Index i)
    {
        DocumentBlockCountDelta = i.getIntIndexProperty("blocks.invertedindex.countdelta", 1);
    }

	protected int[][] getNextDocuments(int df, long endByteOffset, byte endBitOffset) throws IOException {
		final int fieldCount = FieldScore.FIELDS_COUNT;
		final boolean loadTagInformation = FieldScore.USE_FIELD_INFORMATION;
		
		final int[][] documentTerms = new int[5][];
		documentTerms[0] = new int[df];
		documentTerms[1] = new int[df];
		documentTerms[2] = new int[df];
		documentTerms[3] = new int[df];
		final TIntArrayList blockids = new TIntArrayList(df); //ideally we'd have TF here
	
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
	}

	public void print() {
		int documents[][] = null;
		int i =0;
		try{
		while((documents = getNextDocuments()) != null)
		{
			System.out.print("tid"+i);
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
		} catch (IOException ioe) {}
	}

}

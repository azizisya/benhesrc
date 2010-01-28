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
 * The Original Code is BlockInverted2DirectIndexBuilder.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald (craigm{at}dcs.gla.ac.uk)
 */
package uk.ac.gla.terrier.structures.indexing.singlepass;

import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.InvertedIndexInputStream;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import java.util.Arrays;
import java.io.IOException;


/** Create a block direct index from a BlockInvertedIndex.
  * <p><b>Properties:</b> 
  * <ol>
  * <li><tt>inverted2direct.processtokens</tt> - total number of tokens to attempt each iteration. Defaults to 50000000. Memory usage would more likely
  * be linked to the number of pointers and the number of blocks, however as the document index does not contain these statistics on a document basis.
  * these are impossible to estimate. Note that the default is less than Inverted2DirectIndexBuilder.</li>
  * </ol>
  * @author Craig Macdonald
  * @version $Revision: 1.1 $
  * @since 2.0 */
public class BlockInverted2DirectIndexBuilder extends Inverted2DirectIndexBuilder {

	public BlockInverted2DirectIndexBuilder(Index i)
	{
		super(i);
		directIndexClass = "uk.ac.gla.terrier.structures.BlockDirectIndex";
    	directIndexInputStreamClass = "uk.ac.gla.terrier.structures.BlockDirectIndexInputStream";
		processTokens = Long.parseLong(ApplicationSetup.getProperty("inverted2direct.processtokens", "10000000"));
	}

    /** get an array of posting object of the specified size. These will be used to hold
      * the postings for a range of documents */
    protected Posting[] getPostings(final int count)
    {
        Posting[] rtr = new Posting[count];
        if (saveTagInformation)
        {
            for(int i=0;i<count;i++)
                rtr[i] = new BlockFieldPosting();
        }
        else
        {
            for(int i=0;i<count;i++)
                rtr[i] = new BlockPosting();
        }
        return rtr;
    }

    /** returns the SPIR implementation that should be used for reading the postings
      * written earlier */
    protected PostingInRun getPostingReader()
    {
        if (saveTagInformation)
        {
            return new BlockFieldPostingInRun();
        }
        return new BlockPostingInRun();
    }
	
	/** traverse the inverted file, looking for all occurrences of documents in the given range */
    protected long traverseInvertedFile(final InvertedIndexInputStream iiis, int firstDocid, int lastDocid, final Posting[] directPostings)
        throws IOException
    {
        //foreach posting list in the inverted index
            //for each (in range) posting in list
                //add termid->tf tuple to the Posting array
		long tokens = 0;
        int[][] postings;
        int termId = -1;
        //array recording which of the current set of documents has had any postings written thus far
        boolean[] prevUse = new boolean[lastDocid - firstDocid + 1];
        Arrays.fill(prevUse, false);

        while((postings = iiis.getNextDocuments()) != null)
        {
            termId++;
            final int[] postings0 = postings[0];
            final int[] postings1 = postings[1];
            final int[] postings2 = saveTagInformation ? postings[2] : null;
			final int[] postings3 = postings[3];
            final int[] postings4 = postings[4];
            int startOffset = Arrays.binarySearch(postings0, firstDocid);
            int endOffset = Arrays.binarySearch(postings0, lastDocid+1);
            if (startOffset < 0)
                startOffset = -(startOffset+1);
            //no documents in range for this term
            if (startOffset == postings0.length)
                continue;
            if (endOffset < 0)
                endOffset = -(endOffset+1);
            if (endOffset == 0)
                continue;
			int blockIndex = 0;
            if (startOffset != 0)
                for(int i=0;i<startOffset;i++)
                    blockIndex += postings3[i];
            for(int offset = startOffset; offset<endOffset;offset++)
            {
                if (postings0[offset] >= firstDocid && postings0[offset] <= lastDocid)
                {
                    final int writerOffset = postings0[offset] - firstDocid;
					tokens +=  postings1[offset];
					final int[] blocks = new int[postings3[offset]];
					//System.err.println("Term has "+ postings4.length + " blocks, of which we are at offset "+ blockIndex + ", and this posting has "+ postings3[offset] + " blocks");
					/* TODO by adding offset/length parameters to the insert/writeFirstDoc methods of BlockPosting
					 * and BlockFieldPosting, this arraycopy could be prevented */
					System.arraycopy(postings4, blockIndex, blocks, 0, postings3[offset]);
                    if (prevUse[writerOffset])
                    {
                        if (saveTagInformation)
                            ((BlockFieldPosting)directPostings[writerOffset]).insert(termId, postings1[offset],  postings2[offset], blocks);
                        else
                            ((BlockPosting)directPostings[writerOffset]).insert(termId, postings1[offset], blocks);
                    }
                    else
                    {
                        prevUse[writerOffset] = true;
                        if (saveTagInformation)
                            ((BlockFieldPosting)directPostings[writerOffset]).writeFirstDoc(termId, postings1[offset],  postings2[offset], blocks);
                        else
                            ((BlockPosting)directPostings[writerOffset]).writeFirstDoc(termId, postings1[offset], blocks);
                    }
                }
				blockIndex+= postings3[offset];
            }
        }
		return tokens;
    }

	public static void main (String[] args)
	{
		Index i = Index.createIndex();
		if (i== null)
		{
			System.err.println("Sorry, no index could be found in default location");
			return;
		}
		new BlockInverted2DirectIndexBuilder(i).createDirectIndex();
		i.close();
	}

}

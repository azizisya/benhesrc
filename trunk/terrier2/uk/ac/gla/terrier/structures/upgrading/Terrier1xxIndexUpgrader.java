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
 * The Original Code is 1xxIndexUpgrader.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *  Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original author)
 */


package uk.ac.gla.terrier.structures.upgrading;

import uk.ac.gla.terrier.structures.*;
import uk.ac.gla.terrier.structures.indexing.LexiconBuilder;
import uk.ac.gla.terrier.structures.indexing.DocumentIndexBuilder;
import uk.ac.gla.terrier.utility.FieldScore;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import java.io.IOException;
/** Class to upgrade a Terrier 1.x index. Can be invoked from the command line using uk.ac.gla.terrier.structures.upgrading.UpgradeIndex 
  * @see uk.ac.gla.terrier.structures.upgrading.UpgradeIndex 
  * @since 2.0 
  * @author Craig Macdonald
  * @version $Revision: 1.1 $ */
public class Terrier1xxIndexUpgrader extends aIndexUpgrader
{
	/** Create a new IndexUpgrader from sourceIndex to destIndex.
	  * @param sourceIndex Index to use as the source of the upgrade 
	  * @param destIndex Fresh Index to use as the destination of the upgrade
	  */
	public Terrier1xxIndexUpgrader(Index sourceIndex, Index destIndex)
	{
		super(sourceIndex, destIndex);
	}

	 /** upgrade this index, source Index read, destination Index written */
    public void upgrade() throws Exception
	{
		//inverted index & lexicon
		if (sourceIndex.hasIndexStructure("inverted") && sourceIndex.hasIndexStructure("lexicon"))
		{
			try{
				upgradeInvertedIndex();
			} catch (Exception e) {
				throw new Exception("Unable to upgrade inverted index", e);
			}
		}

		//direct index & direct index
		if (sourceIndex.hasIndexStructure("direct"))
		{
			try{
				upgradeDirectIndex();
			} catch (Exception e) {
				throw new Exception("Unable to upgrade direct index", e);
			}
			upgradeDirectIndex();
		}//document index only
		else if (sourceIndex.hasIndexStructure("document"))
		{
			try{
				upgradeDocumentIndex();
			} catch (Exception e) {
				throw new Exception("Unable to upgrade document index", e);
			}

		}
	}

	/** Upgrade the inverted index, with lexicon */
	protected void upgradeInvertedIndex() throws IOException
	{
		final InvertedIndexInputStream iiis = (InvertedIndexInputStream)
			sourceIndex.getIndexStructureInputStream("inverted");
		final LexiconInputStream lis = (LexiconInputStream)sourceIndex.getIndexStructureInputStream("lexicon");
		final boolean UTFIndexing = lis instanceof UTFLexiconInputStream;
		final LexiconOutputStream los = UTFIndexing
			? new UTFLexiconOutputStream(destIndex.getPath(), destIndex.getPrefix())
			: new LexiconOutputStream(destIndex.getPath(), destIndex.getPrefix());
		
		final String OutDfFilename = destIndex.getPath() + 
				ApplicationSetup.FILE_SEPARATOR + 
				destIndex.getPrefix() + ApplicationSetup.IFSUFFIX;
		
		final DirectInvertedOutputStream ios = iiis instanceof BlockInvertedIndexInputStream
			? new BlockDirectInvertedOutputStream(OutDfFilename, FieldScore.FIELDS_COUNT)
			: new DirectInvertedOutputStream(OutDfFilename, FieldScore.FIELDS_COUNT);
		
		int[][] termPostings = null;
        LexiconEntry le =  null;
		//for each posting in the inverted index
        while((termPostings = iiis.getNextDocuments()) != null)
        {
            ios.writePostings(termPostings, termPostings[0][0]+1);
            //write new offsets of new lexicon (los), using information from lis.
            los.writeNextEntry(lis.getTerm(),lis.getTermId(),lis.getNt(),lis.getTF(),
				ios.getByteOffset(),ios.getBitOffset());
		}
		destIndex.addIndexStructure(
                "lexicon",
                UTFIndexing 
					? "uk.ac.gla.terrier.structures.UTFLexicon" 
					: "uk.ac.gla.terrier.structures.Lexicon" );
        destIndex.addIndexStructureInputStream(
                "lexicon",
                UTFIndexing 
					? "uk.ac.gla.terrier.structures.UTFLexiconInputStream" 
					: "uk.ac.gla.terrier.structures.LexiconInputStream");
		destIndex.setIndexProperty("num.Terms", ""+los.getNumberOfTermsWritten() );
        destIndex.setIndexProperty("num.Pointers", ""+los.getNumberOfPointersWritten());
        destIndex.setIndexProperty("num.Tokens", ""+los.getNumberOfTokensWritten());
		destIndex.setIndexProperty("num.Documents", ""+destIndex.getCollectionStatistics().getNumberOfDocuments());
		ios.close();
		los.close();
        LexiconBuilder.createLexiconIndex(destIndex);
		//TODO create lexicon hash if enabled?
		destIndex.flush();
	}

	/** Upgrade the direct index, with document index */
	protected void upgradeDirectIndex() throws IOException
	{

		final DocumentIndexBuilder docidOutput = new DocumentIndexBuilder(destIndex);
		
        final DirectIndex dfInput1 = sourceIndex.getDirectIndex();
        final boolean use_blocks = dfInput1 instanceof BlockDirectIndex;
		String OutDfFilename = destIndex.getPath() + 
				ApplicationSetup.FILE_SEPARATOR + 
				destIndex.getPrefix() + ApplicationSetup.DF_SUFFIX;
        final DirectInvertedOutputStream dfOutput = use_blocks
			? new uk.ac.gla.terrier.structures.BlockDirectInvertedOutputStream(OutDfFilename, FieldScore.FIELDS_COUNT)
			: new uk.ac.gla.terrier.structures.DirectInvertedOutputStream(OutDfFilename, FieldScore.FIELDS_COUNT);

        final DocumentIndexInputStream docidInput1 = (DocumentIndexInputStream)sourceIndex.getIndexStructureInputStream("document");

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
        dfInput1.close();
        docidInput1.close();
		docidOutput.finishedCollections();
        docidOutput.close();

		destIndex.addIndexStructure(
                    "direct",
					use_blocks
						? "uk.ac.gla.terrier.structures.BlockDirectIndex"
						: "uk.ac.gla.terrier.structures.DirectIndex",
                    "uk.ac.gla.terrier.structures.DocumentIndex,java.lang.String,java.lang.String",
                    "document,path,prefix");
        destIndex.addIndexStructureInputStream(
                    "direct",
					use_blocks
						? "uk.ac.gla.terrier.structures.BlockDirectIndexInputStream"
						: "uk.ac.gla.terrier.structures.DirectIndexInputStream",
                    "uk.ac.gla.terrier.structures.DocumentIndexInputStream,java.lang.String,java.lang.String",
                    "document-inputstream,path,prefix");
        destIndex.flush();
	}

	/** Upgrade the document index only */
	protected void upgradeDocumentIndex() throws IOException
	{
		final DocumentIndexBuilder docidOutput = new DocumentIndexBuilder(destIndex);

		//opening the first set of files.
        final DocumentIndexInputStream docidInput1 = (DocumentIndexInputStream)sourceIndex.getIndexStructureInputStream("document");

        //traversing the first set of files, without any change
        while (docidInput1.readNextEntry() >= 0) {

            docidOutput.addEntryToBuffer(docidInput1.getDocumentNumber(),
                                    docidInput1.getDocumentLength(),
                                    new FilePosition(0L, (byte)0));
        }
        docidInput1.close();
		docidOutput.finishedCollections();
        docidOutput.close();
		destIndex.flush();
	}
}

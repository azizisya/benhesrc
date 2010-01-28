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
 * The Original Code is BlockLexiconBuilder.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Douglas Johnson <johnsoda{a.}dcs.gla.ac.uk> (original author)
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> 
 */
package uk.ac.gla.terrier.structures.indexing;
import java.io.IOException;
import java.util.Arrays;
import java.util.PriorityQueue;

import uk.ac.gla.terrier.structures.BlockLexiconInputStream;
import uk.ac.gla.terrier.structures.BlockLexiconOutputStream;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.structures.LexiconInputStream;
import uk.ac.gla.terrier.structures.LexiconOutputStream;
import uk.ac.gla.terrier.utility.ApplicationSetup;
/**
 * Builds a block lexicon using block frequencies.
 * @author Douglas Johnson, Vassilis Plachouras &amp; Craig Macdonald
 * @version $Revision: 1.1 $
 */
public class BlockLexiconBuilder extends LexiconBuilder
{
	
	
	/**
	 * A default constructor of the class. The block lexicon is built in the 
	 * default path and file: ApplicationSetup.TERRIER_INDEX_PATH and 
	 * ApplicationSetup.TERRIER_INDEX_PREFIX respectively.
	 */
	public BlockLexiconBuilder()
	{
		this(ApplicationSetup.TERRIER_INDEX_PATH, ApplicationSetup.TERRIER_INDEX_PREFIX);
	}

	/**
	 * Creates an instance of the class, given the path
	 * to save the final and temporary lexicons.
	 * @param pathname String the path to save the temporary lexicons.
	 */
	public BlockLexiconBuilder(String pathname, String prefix) {
		super(pathname, prefix);
		LexiconMapClass = BlockLexiconMap.class;
		lexiconOutputStream = BlockLexiconOutputStream.class;
		lexiconInputStream = BlockLexiconInputStream.class;
		try{ TempLex = (LexiconMap) LexiconMapClass.newInstance(); } catch (Exception e) {logger.error(e);}
	}
	
	public BlockLexiconBuilder(Index i)
	{
		super(i);
		LexiconMapClass = BlockLexiconMap.class;
		lexiconOutputStream = BlockLexiconOutputStream.class;
		lexiconInputStream = BlockLexiconInputStream.class;
		try{ TempLex = (LexiconMap) LexiconMapClass.newInstance(); } catch (Exception e) {logger.error(e);}
	}

	/**
	 * The method that performs processing of the lexicon after the
	 * creation of the direct index has been completed. It flushes to 
	 * disk the current temporary lexicon, and it starts the merging
	 * of the temporary lexicons and the creation of the lexicon index. 
	 */
	public void finishedDirectIndexBuild()
	{
		logger.info("flushing block lexicon to disk after the direct index completed");
		 //only write a temporary lexicon if there are any items in it
		if (TempLex.getNumberOfNodes() > 0)
			writeTemporaryLexicon();
		TempLex = null;

		//merges the temporary lexicons
		if (tempLexFiles.size() > 0)
			try{
				merge(tempLexFiles);
	
				//creates the offsets file
				final String lexiconFilename = 
							indexPath + ApplicationSetup.FILE_SEPARATOR + 
							indexPrefix + ApplicationSetup.LEXICONSUFFIX;
				LexiconInputStream lis = getLexInputStream(lexiconFilename);
				createLexiconIndex(
						lis,
						lis.numberOfEntries(),
						/* after inverted index is built, the lexicon will be transformed into a
						 * normal lexicon, without block frequency */
						Lexicon.lexiconEntryLength 
						);
				TermCount = lis.numberOfEntries();
				if (index != null)
				{
					index.addIndexStructure("lexicon", "uk.ac.gla.terrier.structures.BlockLexicon");
					index.addIndexStructureInputStream("lexicon", "uk.ac.gla.terrier.structures.BlockLexiconInputStream");
					index.setIndexProperty("num.Terms", ""+lis.numberOfEntries());
					index.setIndexProperty("num.Pointers", ""+lis.getNumberOfPointersRead());
				}
			} catch(IOException ioe){
				logger.error("Indexing failed to merge temporary lexicons to disk. ",ioe);
			}
		else
			logger.warn("No temporary lexicons to merge, skipping");
	}

	/** Merge the two LexiconInputStreams into the given LexiconOutputStream
	  * @param lis1 First lexicon to be merged
	  * @param lis2 Second lexicon to be merged
	  * @param los Lexion to be merged to
	  */
	protected void mergeTwoLexicons(
			LexiconInputStream blis1,
			LexiconInputStream blis2,
			LexiconOutputStream blos) throws IOException
	{
		final BlockLexiconInputStream lis1 = (BlockLexiconInputStream)blis1;
		final BlockLexiconInputStream lis2 = (BlockLexiconInputStream)blis2;
		final BlockLexiconOutputStream los = (BlockLexiconOutputStream)blos;

		boolean hasMore1 = true;
		boolean hasMore2 = true;
		int termID1 = 0;
		int termID2 = 0;
		hasMore1 = (lis1.readNextEntry()!=-1);
		hasMore2 = (lis2.readNextEntry()!=-1);
		String sTerm1 = null;
		String sTerm2 = null;
		if (hasMore1) {
			termID1 = lis1.getTermId();
			sTerm1 = lis1.getTerm();
		}
		if (hasMore2) {
			termID2 = lis2.getTermId();
			sTerm2 = lis2.getTerm();
		}
		while (hasMore1 && hasMore2) {
			int compareString = 0;
			if (termID1 != termID2)
			{
				compareString = sTerm1.compareTo(sTerm2);
				if (compareString == 0)//, but termids don't match
				{
					logger.error("Term "+sTerm1+" had two termids ("+ termID1+","+termID2+")");
				}
			}
			
			if (compareString <0) {
				los.writeNextEntry(sTerm1, termID1, lis1.getNt(), lis1.getBlockFrequency(), lis1.getTF(), lis1.getEndOffset(), lis1.getEndBitOffset());
				hasMore1 = (lis1.readNextEntry()!=-1);
				if (hasMore1) {
					termID1 = lis1.getTermId();
					sTerm1 = lis1.getTerm();
				}
			} else if (compareString >0) {
				los.writeNextEntry(sTerm2, termID2, lis2.getNt(), lis2.getBlockFrequency(), lis2.getTF(), lis2.getEndOffset(), lis2.getEndBitOffset());
				hasMore2 = (lis2.readNextEntry()!=-1);
				if (hasMore2) {
					termID2 = lis2.getTermId();
					sTerm2 = lis2.getTerm();
				}
			} else /*if (compareString == 0)*/ {
				los.writeNextEntry(
					sTerm1, 
					termID1, 
					lis1.getNt() + lis2.getNt(),
					lis1.getBlockFrequency() + lis2.getBlockFrequency(),
					lis1.getTF() + lis2.getTF(),  							 
					0, //inverted index not built yet
					(byte)0 //inverted index not built yet
				);
		
				hasMore1 = (lis1.readNextEntry()!=-1);
				hasMore2 = (lis2.readNextEntry()!=-1);
				if (hasMore1) {
					termID1 = lis1.getTermId();
					sTerm1 = lis1.getTerm();
				}
				if (hasMore2) {
					termID2 = lis2.getTermId();
					sTerm2 = lis2.getTerm();
				}
			}
		}
		if (hasMore1) {
			lis2.close();

			while (hasMore1) {
				los.writeNextEntry(sTerm1, termID1, lis1.getNt(), lis1.getBlockFrequency(), lis1.getTF(), lis1.getEndOffset(), lis1.getEndBitOffset());
				hasMore1 = (lis1.readNextEntry()!=-1);
				if (hasMore1) {
					termID1 = lis1.getTermId();
					sTerm1 = lis1.getTerm();
				}
			}

			//close input file 1 stream
			lis1.close();
			
		} else if (hasMore2) {
			lis1.close();

			while (hasMore2) {
				los.writeNextEntry(sTerm2, termID2, lis2.getNt(), lis2.getBlockFrequency(), lis2.getTF(), lis2.getEndOffset(), lis2.getEndBitOffset());
				hasMore2 = (lis2.readNextEntry()!=-1);
				if (hasMore2) {
					termID2 = lis2.getTermId();
					sTerm2 = lis2.getTerm();
				}
			}
			//close input file 2 stream
			lis2.close();
		}
		//closing ouptut lexicon stream
		los.close();	
	}
	
	protected void mergeNLexicons(final LexiconInputStream[] _lis, final LexiconOutputStream _los) throws IOException
	{
		final int numLexicons = _lis.length;
		long totalTokens = 0;
		long totalPointers = 0;
		final int hasMore[] = new int[numLexicons];
		Arrays.fill(hasMore, -1);
		final PriorityQueue<String> terms = new PriorityQueue<String>(numLexicons);
		final BlockLexiconOutputStream los = (BlockLexiconOutputStream)_los;
		final BlockLexiconInputStream[] lis = new BlockLexiconInputStream[numLexicons];
		
		for(int i=0;i<numLexicons;i++)
		{
			lis[i] = (BlockLexiconInputStream) _lis[i];
			hasMore[i] = lis[i].readNextEntry();
			terms.add(lis[i].getTerm());	
		}
		int Tf = 0; int Nt = 0; int Bf = 0; String targetTerm= null;
		int targetTermId  = -1;
		while(terms.size() > 0)
		{
			//what term are we working on
			targetTerm = terms.poll();
			//logger.debug("Current term is "+targetTerm + "length="+targetTerm.length());
			//for each input lexicon
			for(int i=0;i<numLexicons;i++)
			{
				//does this lexicon contain the term
				//logger.debug("Checking lexicon "+i+" for "+targetTerm+"="+lis[i].getTerm());
				if(hasMore[i] != -1 && lis[i].getTerm().equals(targetTerm))
				{
					if (targetTermId == -1)
					{	//obtain the termid for this term from the first lexicon that has the term
						targetTermId = lis[i].getTermId();
					}
					else if (targetTermId != lis[i].getTermId())
					{	//check the termids match for this term
						logger.error("Term "+targetTerm+" had two termids ("+targetTermId+","+lis[i].getTermId()+")");
					}
					//logger.debug("Term "+targetTerm + " found in "+i + "termid="+ lis[i].getTermId());
					Tf += lis[i].getTF();
					Nt += lis[i].getNt();
					Bf += lis[i].getBlockFrequency();
					hasMore[i] = lis[i].readNextEntry();
					if (hasMore[i] != -1)
					{
						terms.add(lis[i].getTerm());
						//break;
					}
					break;
				}
			}
			if (terms.size()>0 && !terms.peek().equals(targetTerm))
			{
				if (targetTermId == -1)
				{
					logger.error("Term "+ targetTerm + " not found in any lexicons");
				}
				//end of this term, so we can write the lexicon entry
				totalTokens += Tf;
				totalPointers += Nt;
				los.writeNextEntry(targetTerm, targetTermId, Nt, Tf, Bf, 0, (byte)0);
				Bf = 0; Tf = Nt = 0; targetTermId = -1; targetTerm = null;
			}
		}
		totalTokens += Tf;
		totalPointers += Nt;
		if (targetTermId != -1)
			los.writeNextEntry(targetTerm, targetTermId, Nt, Tf, Bf, 0, (byte)0);
		los.close();
		for(int i=0;i<numLexicons;i++)
			lis[i].close();
	}


	public static void main(String args[]) {
		String path = args[0];
		String prefix = args[1];
		BlockLexiconBuilder blb = new BlockLexiconBuilder(path, prefix);
		
		String lexiconFilename = path + ApplicationSetup.FILE_SEPARATOR + prefix + ApplicationSetup.LEXICONSUFFIX;
		LexiconInputStream lexStream = new LexiconInputStream(lexiconFilename);
		blb.createLexiconHash(lexStream);
		
	}
	
}

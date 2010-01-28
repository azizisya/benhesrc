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
 * The Original Code is UTFBlockLexiconBuilder.java.
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

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.LexiconInputStream;
import uk.ac.gla.terrier.structures.UTFBlockLexiconInputStream;
import uk.ac.gla.terrier.structures.UTFBlockLexiconOutputStream;
import uk.ac.gla.terrier.structures.UTFLexicon;
import uk.ac.gla.terrier.utility.ApplicationSetup;
/**
 * Builds a block lexicon using block frequencies.
 * @author Douglas Johnsonm, Vassilis Plachouras &amp; Craig Macdonald
 * @version $Revision: 1.1 $
 */
public class UTFBlockLexiconBuilder extends BlockLexiconBuilder
{
	protected static Logger logger = Logger.getRootLogger();
	/**
	 * A default constructor of the class. The lexicon is built in the 
	 * default path and file: ApplicationSetup.TERRIER_INDEX_PATH and 
	 * ApplicationSetup.TERRIER_INDEX_PREFIX respectively.
	 */	
	public UTFBlockLexiconBuilder() {
		super();
		lexiconOutputStream = UTFBlockLexiconOutputStream.class;
		lexiconInputStream = UTFBlockLexiconInputStream.class;
		LexiconMapClass = BlockLexiconMap.class;
		try{ TempLex = (LexiconMap) LexiconMapClass.newInstance(); } catch (Exception e) {logger.error(e);}
	}
	
	public UTFBlockLexiconBuilder(Index i)
	{
		super(i);
		lexiconOutputStream = UTFBlockLexiconOutputStream.class;
		lexiconInputStream = UTFBlockLexiconInputStream.class;
		LexiconMapClass = BlockLexiconMap.class;
		try{ TempLex = (LexiconMap) LexiconMapClass.newInstance(); } catch (Exception e) {logger.error(e);}
	}
	
	/** 
	 * A default constructor which is given a pathname in which
	 * the temporary lexicons will be stored.
	 * @param pathname String the name of the path in which the temporary
	 * and final lexicons will be stored.
	 * @param prefix String the file component of the lexicons
	 */	
	public UTFBlockLexiconBuilder(String pathname, String prefix) {
		super(pathname, prefix);
		lexiconOutputStream = UTFBlockLexiconOutputStream.class;
		lexiconInputStream = UTFBlockLexiconInputStream.class;
		LexiconMapClass = BlockLexiconMap.class;
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
		if(logger.isInfoEnabled()){
			logger.info("flushing utf block lexicon to disk after the direct index completed");
		}
		//only write a temporary lexicon if there are any items in it
		if (TempLex.getNumberOfNodes() > 0)
			writeTemporaryLexicon();

		//merges the temporary lexicons
		if (tempLexFiles.size() > 0)
		{
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
					UTFLexicon.lexiconEntryLength
					); 
				TermCount = lis.numberOfEntries();
				if (index != null)
				{
					index.addIndexStructure("lexicon", "uk.ac.gla.terrier.structures.UTFBlockLexicon");
					index.addIndexStructureInputStream("lexicon", "uk.ac.gla.terrier.structures.UTFBlockLexiconInputStream");
					index.setIndexProperty("num.Terms", ""+lis.numberOfEntries());
					index.setIndexProperty("num.Pointers", ""+lis.getNumberOfPointersRead());
				}
			} catch(IOException ioe){
				logger.error("Indexing failed to write a lexicon index file to disk", ioe);
			}	
		}
		else
			logger.warn("No temporary lexicons to merge, skipping");
		
	}

	
}

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
 * The Original Code is UTFLexiconBuilder.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original author)
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.structures.indexing;

import java.io.IOException;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.LexiconInputStream;
import uk.ac.gla.terrier.structures.UTFLexicon;
import uk.ac.gla.terrier.structures.UTFLexiconInputStream;
import uk.ac.gla.terrier.structures.UTFLexiconOutputStream;
import uk.ac.gla.terrier.utility.ApplicationSetup;
/**
 * Builds temporary lexicons during indexing a collection and
 * merges them when the indexing of a collection has finished.
 * @author Craig Macdonald &amp; Vassilis Plachouras 
 * @version $Revision: 1.1 $
 */
public class UTFLexiconBuilder extends LexiconBuilder
{
	private static Logger logger = Logger.getRootLogger();
	/**
	 * A default constructor of the class. The lexicon is built in the 
	 * default path and file: ApplicationSetup.TERRIER_INDEX_PATH and 
	 * ApplicationSetup.TERRIER_INDEX_PREFIX respectively.
	 * @deprecated
	 */
	public UTFLexiconBuilder() 	{
		super();
		lexiconOutputStream = UTFLexiconOutputStream.class;
		lexiconInputStream = UTFLexiconInputStream.class;
	}

	/** 
	 * Creates an instance of the class, given the path
	 * to save the temporary lexicons.
	 * @param pathname String the path to save the temporary and final lexicons.
	 * @param prefix String the filename component of the lexicons
	 */
	public UTFLexiconBuilder(String pathname, String prefix) {
		super(pathname, prefix);
		lexiconOutputStream = UTFLexiconOutputStream.class;
		lexiconInputStream = UTFLexiconInputStream.class;
	}
	
	public UTFLexiconBuilder(Index i)
	{
		super(i);
		lexiconOutputStream = UTFLexiconOutputStream.class;
		lexiconInputStream = UTFLexiconInputStream.class;
	}

	
	/** 
	 * Processing the lexicon after finished creating the 
	 * direct and document indexes.
	 */
	public void finishedDirectIndexBuild()
	{
		if(logger.isInfoEnabled()){
			logger.info("flushing lexicon to disk after the direct index completed");
		}
		 //only write a temporary lexicon if there are any items in it
		if (TempLex.getNumberOfNodes() > 0)
			writeTemporaryLexicon();
		TempLex = null;

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
					UTFLexicon.lexiconEntryLength
					); 
				TermCount = lis.numberOfEntries();	
				if (index != null)
				{
					index.addIndexStructure("lexicon", "uk.ac.gla.terrier.structures.UTFLexicon");
					index.addIndexStructureInputStream("lexicon", "uk.ac.gla.terrier.structures.UTFLexiconInputStream");
					index.setIndexProperty("num.Terms", ""+lis.numberOfEntries());
		   			index.setIndexProperty("num.Pointers", ""+lis.getNumberOfPointersRead());
				}
		
			} catch (IOException ioe) {
				logger.error("Indexing failed to write a lexicon index file to disk", ioe);
			}
		}
		else
			logger.warn("No temporary lexicons to merge, skipping");
	}

	@Override
	public void finishedInvertedIndexBuild() {
		super.finishedInvertedIndexBuild();
		if (index != null)
		{
			index.addIndexStructure("lexicon", "uk.ac.gla.terrier.structures.UTFLexicon");
		}
	}
	
}

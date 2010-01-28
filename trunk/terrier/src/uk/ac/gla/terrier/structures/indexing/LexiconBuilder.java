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
 * The Original Code is LexiconBuilder.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original author)
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.structures.indexing;
import gnu.trove.TIntObjectHashMap;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Set;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.structures.LexiconInputStream;
import uk.ac.gla.terrier.structures.LexiconOutputStream;
import uk.ac.gla.terrier.structures.UTFLexiconInputStream;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;
/**
 * Builds temporary lexicons during indexing a collection and
 * merges them when the indexing of a collection has finished.
 * @author Craig Macdonald &amp; Vassilis Plachouras 
 * @version $Revision: 1.1 $
 */
public class LexiconBuilder
{
	/** class to be used as a lexiconinputstream. set by this and child classes */
	protected Class lexiconInputStream = null;
	/** class to be used as a lexiconoutpustream. set by this and child classes */
	protected Class lexiconOutputStream = null;

	protected Class LexiconMapClass = null;
	
	/** The logger used for this class */
	protected static Logger logger = Logger.getRootLogger();
	
	/** How many documents have been processed so far.*/
	protected int DocCount = 0;

	/** How many terms are in the final lexicon */
	protected int TermCount = 0;
	
	/** 
	 * The number of documents for which a temporary lexicon
	 * is created.
	 */
	protected static final int DocumentsPerLexicon = ApplicationSetup.BUNDLE_SIZE;
	/** The linkedlist in which the temporary lexicon filenames are stored.
	  * These are merged into a single Lexicon by the merge() method. 
	  * LinkedList is best List implementation for this, as all operations
	  * are either append element, or remove first element - making LinkedList
	  * ideal. */
	protected final LinkedList<String> tempLexFiles = new LinkedList<String>();
	
	/** The lexicontree to write the current term stream to */
	protected LexiconMap TempLex;
	
	/** The directory to write temporary lexicons to */
	protected String TemporaryLexiconDirectory = null;
	
	/** The directory to write the final lexicons to */
	protected String indexPath = null;
	/** The filename of the lexicons. */
	protected String indexPrefix = null;
	
	protected Index index = null;
	
	/** How many temporary lexicons have been generated so far */
	protected int TempLexCount = 0;
	
	/** How many temporary directories have been generated so far */
	protected int TempLexDirCount = 0;
	
	/** How many temporary lexicons per temporary directory. Set from the property <tt>lexicon.builder.templexperdir</tt>, default 100 */
	protected static final int TempLexPerDir = Integer.parseInt(ApplicationSetup.getProperty("lexicon.builder.templexperdir", "100"));

	/** Should we only merge lexicons in pairs (Terrier 1.0.x scheme)? Set by property <tt>lexicon.builder.merge.2lex.attime</tt> */
	protected static final boolean MERGE2LEXATTIME = Boolean.parseBoolean(ApplicationSetup.getProperty("lexicon.builder.merge.2lex.attime", "false"));

	/** Number of lexicons to merge at once. Set by property <tt>lexicon.builder.merge.lex.max</tt>, defaults to 16 */
	protected static final int MAXLEXMERGE = Integer.parseInt(ApplicationSetup.getProperty("lexicon.builder.merge.lex.max", "16"));

	/**
	 * A default constructor of the class. The lexicon is built in the 
	 * default path and file: ApplicationSetup.TERRIER_INDEX_PATH and 
	 * ApplicationSetup.TERRIER_INDEX_PREFIX respectively.
	 * @deprecated
	 */
	public LexiconBuilder()
	{
		this(ApplicationSetup.TERRIER_INDEX_PATH, ApplicationSetup.TERRIER_INDEX_PREFIX);
	}
	
	public LexiconBuilder(Index i) {
		this.index = i;
		this.indexPath = index.getPath();
		this.indexPrefix = index.getPrefix();
		TemporaryLexiconDirectory = indexPath + ApplicationSetup.FILE_SEPARATOR + indexPrefix + "_";
		LexiconMapClass = LexiconMap.class;	
		try{ TempLex = (LexiconMap) LexiconMapClass.newInstance(); } catch (Exception e) {logger.error(e);}
		lexiconInputStream = LexiconInputStream.class;
		lexiconOutputStream = LexiconOutputStream.class;
	}
	
	/** 
	 * Creates an instance of the class, given the path
	 * to save the temporary lexicons.
	 * @param pathname String the path to save the temporary lexicons.
	 */
	public LexiconBuilder(String pathname, String prefix) {
		indexPath = pathname;
		indexPrefix = prefix;
		TemporaryLexiconDirectory = pathname + ApplicationSetup.FILE_SEPARATOR + prefix + "_";
		LexiconMapClass = LexiconMap.class;	
		try{ TempLex = (LexiconMap) LexiconMapClass.newInstance(); } catch (Exception e) {logger.error(e);}
		lexiconInputStream = LexiconInputStream.class;
		lexiconOutputStream = LexiconOutputStream.class;
	}

	/** Returns the number of terms in the final lexicon. Only updated once finishDirectIndexBuild() has executed */
	public int getFinalNumberOfTerms()
	{
		return TermCount;
	}

	/** If the application code generated lexicons itself, use this method to add them to the merge list 
	  * Otherwise dont touch this method.
	  * @param filename Fully path to a lexicon to merge */
	public void addTemporaryLexicon(String filename) {
		filename = ApplicationSetup.makeAbsolute(filename, TemporaryLexiconDirectory);
	}

	/** Writes the current contents of TempLex temporary lexicon binary tree down to
	  * a temporary disk lexicon.
	  */
	protected void writeTemporaryLexicon()
	{
		try{
			TempLexDirCount = TempLexCount / TempLexPerDir;
			if (! Files.exists(TemporaryLexiconDirectory + TempLexDirCount)) {
				String tmpDir = TemporaryLexiconDirectory + TempLexDirCount;
				Files.mkdir(tmpDir);
				Files.deleteOnExit(tmpDir);//it's fine to mark the temporary *directory* for deletion
			}
			String tmpLexName = TemporaryLexiconDirectory + TempLexDirCount + ApplicationSetup.FILE_SEPARATOR + 
				(TempLexCount) + ApplicationSetup.LEXICONSUFFIX;
			LexiconOutputStream los = getLexOutputStream(tmpLexName);
			TempLex.storeToStream(los);
			los.close();
			/* An alternative but deprecated method to store the temporary lexicons is: 
			 * TempLex.storeToFile(tmpLexName); */
			tempLexFiles.addLast(tmpLexName);
		}catch(IOException ioe){
			logger.error("Indexing failed to write a lexicon to disk : ", ioe);
		}		
	}

	/** Add a single term to the lexicon being built 
	  * @param term The String term
	  * @param tf the frequency of the term */	
	public void addTerm(String term, int tf)
	{
		TempLex.insert(term,tf);
	}

	/** adds the terms of a document to the temporary lexicon in memory.
	  * @param terms DocumentPostingList the terms of the document to add to the temporary lexicon */
	public void addDocumentTerms(DocumentPostingList terms)
	{
		TempLex.insert(terms);
		DocCount++;
		if((DocCount % DocumentsPerLexicon) == 0)
		{
			if (logger.isDebugEnabled())
				logger.debug("flushing lexicon");
			writeTemporaryLexicon();
			TempLexCount++;
			try{ TempLex = (LexiconMap)LexiconMapClass.newInstance(); } catch (Exception e) {logger.error(e);}
		}
	}

	/** Force a temporary lexicon to be flushed */
	public void flush()
	{
		if (logger.isDebugEnabled())
			logger.debug("flushing lexicon");
		writeTemporaryLexicon();
		TempLexCount++;
		try{ TempLex = (LexiconMap)LexiconMapClass.newInstance(); } catch (Exception e) {logger.error(e);}
	}
	
	/**
	 * Processing the lexicon after finished creating the
	 * inverted index.
	 */
	public void finishedInvertedIndexBuild() {
		if (Boolean.parseBoolean(ApplicationSetup.getProperty("lexicon.use.hash","true"))) {
			String lexiconFilename = indexPath + ApplicationSetup.FILE_SEPARATOR + indexPrefix + ApplicationSetup.LEXICONSUFFIX;
			LexiconInputStream lexStream = getLexInputStream(lexiconFilename);
			this.createLexiconHash(lexStream);
		}
		if (index != null)
		{
			index.addIndexStructure("lexicon", "uk.ac.gla.terrier.structure.Lexicon");
		}
	}
	
	/** 
	 * Processing the lexicon after finished creating the 
	 * direct and document indexes.
	 */
	public void finishedDirectIndexBuild()
	{
		if (logger.isDebugEnabled())
			logger.debug("flushing lexicon to disk after the direct index completed");
		//only write a temporary lexicon if there are any items in it
		if (TempLex.getNumberOfNodes() > 0)
			writeTemporaryLexicon();
		TempLex = null;

		//merges the temporary lexicons
		if (tempLexFiles.size() > 0)
		{
			Set<String> tempDirectories = new HashSet<String>();
			for(String tmpLex : tempLexFiles)
			{
				tempDirectories.add(Files.getParent(tmpLex));
			}
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
						Lexicon.lexiconEntryLength
						); 
				TermCount = lis.numberOfEntries();
				if (index != null)
				{
					index.addIndexStructure("lexicon", "uk.ac.gla.terrier.structures.Lexicon");
					index.addIndexStructureInputStream("lexicon", "uk.ac.gla.terrier.structures.LexiconInputStream");
					index.setIndexProperty("num.Terms", ""+lis.numberOfEntries());
					index.setIndexProperty("num.Pointers", ""+lis.getNumberOfPointersRead());
				}
			} catch(IOException ioe){
				logger.error("Indexing failed to merge temporary lexicons to disk : ", ioe);
			}
			for (String tmpDir : tempDirectories)
			{
				Files.delete(tmpDir);
			}	
		}	
		else
			logger.warn("No temporary lexicons to merge, skipping");
	}
	
	/**
	 * Merges the intermediate lexicon files created during the indexing.
	 * @param filesToMerge java.util.LinkedList the list containing the 
	 *		filenames of the temporary files.
	 * @throws IOException an input/output exception is throws 
	 *		 if a problem is encountered.
	 */
	public void merge(LinkedList<String> filesToMerge) throws IOException {
		//now the merging of the files in the filesToMerge vector 
		//must take place. 
		//Several strategies exist here: 
		// a. number to merge is 0 - error condition?
		// b. number ito merge is 1 - none to merge, just rename it
		// c. merge 2 at a time in pairs (default to 1.0.2)
		// d. merge N at once (N is a constant)
		// e. merge all at once.


		final int mergeNMaxLexicon = MAXLEXMERGE;
		final int StartFileCount = filesToMerge.size();
		
		if (StartFileCount == 0)
		{
			logger.warn("Tried to merge 0 lexicons. That's funnny. Is everything ok?");
			return;
		}
		if (StartFileCount == 1)
		{
			Files.rename(filesToMerge.removeFirst(), indexPath + ApplicationSetup.FILE_SEPARATOR +indexPrefix + ApplicationSetup.LEXICONSUFFIX);
		}
		else if (MERGE2LEXATTIME)
		{
			//more than 1 lexicon to merge, but configured only to merge 2 at a time
			if (logger.isDebugEnabled())
				logger.debug("begin merging "+ StartFileCount +" temporary lexicons, in pairs...");
			long startTime = System.currentTimeMillis();
			int progressiveNumber = ApplicationSetup.MERGE_TEMP_NUMBER;
			String newMergedFile = null;
			while (filesToMerge.size() > 1) {
				String fileToMerge1 = (String) filesToMerge.removeFirst();
				String fileToMerge2 = (String) filesToMerge.removeFirst();
				
				//give the proper name to the final merged lexicon
				if (filesToMerge.size() == 0) 
					newMergedFile = indexPath + ApplicationSetup.FILE_SEPARATOR + 
						indexPrefix + ApplicationSetup.LEXICONSUFFIX;
				else 
					newMergedFile =
						Files.getParent(fileToMerge1) 
							+ ApplicationSetup.FILE_SEPARATOR
							+ ApplicationSetup.MERGE_PREFIX
							+ String.valueOf(progressiveNumber++)
							+ ApplicationSetup.LEXICONSUFFIX;
	
				//The opening of the files needs to break into more steps, so that
				//all the open streams are closed after the completion of the 
				//operation, and eventually the intermediate files are deleted.

				LexiconInputStream lis1 = getLexInputStream(fileToMerge1);
				LexiconInputStream lis2 = getLexInputStream(fileToMerge2);
				LexiconOutputStream los = getLexOutputStream(newMergedFile);
	
				if (logger.isDebugEnabled())
					logger.debug(
						"merging "
							+ fileToMerge1
							+ " with "
							+ fileToMerge2
							+ " to "
							+ newMergedFile);
				
				mergeTwoLexicons(lis1, lis2, los);
			
				//delete the two files just merged
				Files.delete(fileToMerge1);
				Files.delete(fileToMerge2);
				filesToMerge.addLast(newMergedFile);
			}
			long endTime = System.currentTimeMillis();
			if (logger.isDebugEnabled())
				logger.debug("end of merging...("+((endTime-startTime)/1000.0D)+" seconds)");
		}
		else if (mergeNMaxLexicon > 0 && StartFileCount > mergeNMaxLexicon)
		{
			if (logger.isDebugEnabled())
				logger.debug("begin merging "+ StartFileCount +" files in batches of upto "+mergeNMaxLexicon+"...");
			long startTime = System.currentTimeMillis();
			int progressiveNumber = ApplicationSetup.MERGE_TEMP_NUMBER;
	

			while (filesToMerge.size() > 1)
			{
				final int numLexicons = Math.min(filesToMerge.size(), mergeNMaxLexicon);
				if (logger.isDebugEnabled())
					 logger.debug("merging "+ numLexicons + " temporary lexicons");
				final String inputLexiconFileNames[] = new String[numLexicons];
				final LexiconInputStream[] lis = new LexiconInputStream[numLexicons];

				for(int i=0;i<numLexicons;i++)
				{
					inputLexiconFileNames[i] =  filesToMerge.removeFirst();
					lis[i] = getLexInputStream(inputLexiconFileNames[i]);
				}

				String newMergedFile = null;
				//give the proper name to the final merged lexicon
				if (filesToMerge.size() == 0)
					newMergedFile = indexPath + ApplicationSetup.FILE_SEPARATOR +
						indexPrefix + ApplicationSetup.LEXICONSUFFIX;
				else
					newMergedFile =
						Files.getParent(inputLexiconFileNames[0])
							+ ApplicationSetup.FILE_SEPARATOR
							+ ApplicationSetup.MERGE_PREFIX
							+ String.valueOf(progressiveNumber++)
							+ ApplicationSetup.LEXICONSUFFIX;

				final LexiconOutputStream  los = getLexOutputStream(newMergedFile);
				mergeNLexicons(lis, los);
				for(int i=0;i<numLexicons;i++)
				{
					Files.delete(inputLexiconFileNames[i]);
				}
				filesToMerge.addLast(newMergedFile);
			}
			long endTime = System.currentTimeMillis();
			if (logger.isDebugEnabled())
				logger.debug("end of merging...("+((endTime-startTime)/1000.0D)+" seconds)");
		} else {
			//merge all lexicons at once, regardless of how many exist
			 if (logger.isDebugEnabled())
				logger.debug("begin merging "+ StartFileCount +" temporary lexicons at once...");
			long startTime = System.currentTimeMillis();
			final String inputLexiconFileNames[] = new String[StartFileCount];
			final LexiconInputStream[] lis = new LexiconInputStream[StartFileCount];
			
			for(int i=0;i<StartFileCount;i++)
			{
				inputLexiconFileNames[i] = filesToMerge.removeFirst();
				lis[i] = getLexInputStream(inputLexiconFileNames[i]);
				//logger.debug(i+" "+inputLexiconFileNames[i]);
			}
			final LexiconOutputStream los = getLexOutputStream( indexPath + ApplicationSetup.FILE_SEPARATOR +
				indexPrefix + ApplicationSetup.LEXICONSUFFIX);
			mergeNLexicons(lis, los);
			for(int i=0;i<StartFileCount;i++)
			{
					Files.delete(inputLexiconFileNames[i]);
			}
			long endTime = System.currentTimeMillis();
			if (logger.isDebugEnabled())
				logger.debug("end of merging...("+((endTime-startTime)/1000.0D)+" seconds)");
		}
	}
	
	protected void mergeNLexicons(LexiconInputStream[] lis, LexiconOutputStream los) throws IOException
	{
		final int numLexicons = lis.length;
		long totalTokens = 0;
		long totalPointers = 0;
		int hasMore[] = new int[numLexicons];
		Arrays.fill(hasMore, -1);
		PriorityQueue<String> terms = new PriorityQueue<String>(numLexicons);
		for(int i=0;i<numLexicons;i++)
		{
			hasMore[i] = lis[i].readNextEntry();
			terms.add(lis[i].getTerm());	
		}
		int Tf = 0; int Nt = 0; String targetTerm= null;
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
				los.writeNextEntry(targetTerm, targetTermId, Nt, Tf, 0, (byte)0);
				Tf = Nt = 0; targetTermId = -1; targetTerm = null;
			}
		}
		totalTokens += Tf;
		totalPointers += Nt;
		if (targetTermId != -1)
			los.writeNextEntry(targetTerm, targetTermId, Nt, Tf, 0, (byte)0);
		los.close();
		for(int i=0;i<numLexicons;i++)
			lis[i].close();
	}
		

	/** Merge the two LexiconInputStreams into the given LexiconOutputStream
	  * @param lis1 First lexicon to be merged
	  * @param lis2 Second lexicon to be merged
	  * @param los Lexion to be merged to
	  */
	protected void mergeTwoLexicons(
			LexiconInputStream lis1,
			LexiconInputStream lis2,
			LexiconOutputStream los) throws IOException
	{

		//We always take the first two entries of
		//the vector, merge them, store the new lexicon in the directory
		//of the first of the two merged lexicons, and put the filename
		//of the new lexicon file at the back of the vector. The first
		//two entries that were merged are removed from the vector. The
		//use of the vector is similar to a FIFO queue in this case.

		boolean hasMore1 = true;
		boolean hasMore2 = true;
		int termID1 = 0;
		int termID2 = 0;

		long totalTokens = 0;
		long totalPointers = 0;
	

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
					logger.error("Term "+sTerm1+" had two termids ("+
						termID1+","+termID2+")");
				}
			}
			
			if (compareString <0) {
				totalTokens += lis1.getTF();
				totalPointers += lis1.getNt();
				los.writeNextEntry(sTerm1, termID1, lis1.getNt(), lis1.getTF(), lis1.getEndOffset(), lis1.getEndBitOffset());
				hasMore1 = (lis1.readNextEntry()!=-1);
				if (hasMore1) {
					termID1 = lis1.getTermId();
					sTerm1 = lis1.getTerm();
				}
			} else if (compareString >0) {
				totalTokens += lis2.getTF();
				totalPointers += lis2.getNt();
				los.writeNextEntry(sTerm2, termID2, lis2.getNt(), lis2.getTF(), lis2.getEndOffset(), lis2.getEndBitOffset());
				hasMore2 = (lis2.readNextEntry()!=-1);
				if (hasMore2) {
					termID2 = lis2.getTermId();
					sTerm2 = lis2.getTerm();
				}
			} else /*if (compareString == 0)*/ {
				totalTokens += lis1.getTF() + lis2.getTF();
				totalPointers += lis1.getNt() + lis2.getNt();
				los.writeNextEntry(
					sTerm1, 
					termID1, 
					lis1.getNt() + lis2.getNt(),
					lis1.getTF() + lis2.getTF(),  							 
					0, //inverted index not built yet, so no offsets
					(byte)0 //inverted index not built yet, so no offsets
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
				totalTokens += lis1.getTF();
				totalPointers += lis1.getNt();
				los.writeNextEntry(sTerm1, termID1, lis1.getNt(), lis1.getTF(), lis1.getEndOffset(), lis1.getEndBitOffset());
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
				totalTokens += lis2.getTF();
				totalPointers += lis2.getNt();	
				los.writeNextEntry(sTerm2, termID2, lis2.getNt(), lis2.getTF(), lis2.getEndOffset(), lis2.getEndBitOffset());
				hasMore2 = (lis2.readNextEntry()!=-1);
				if (hasMore2) {
					termID2 = lis2.getTermId();
					sTerm2 = lis2.getTerm();
				}
			}
			//close input file 2 stream
			lis2.close();
		}
		//close output file streams
		los.close();
	}
	
	/**
	 * Creates the lexicon index file that contains a mapping from the 
	 * given term id to the offset in the lexicon, in order to 
	 * be able to retrieve the term information according to the 
	 * term identifier. This is necessary, because the terms in the lexicon 
	 * file are saved in lexicographical order, and we also want to have 
	 * fast access based on their term identifier.
	 * @param lexicon The input stream of the lexicon that we are creating the lexid file for
	 * @param lexiconEntries The number of entries in this lexicon
	 * @param lexiconEntrySize The size of one entry in this lexicon
	 * @exception java.io.IOException Throws an Input/Output exception if 
	 *			there is an input/output error. 
	 */
	public void createLexiconIndex(final LexiconInputStream lexicon, 
			final int lexiconEntries, 
			final int lexiconEntrySize) throws IOException {
		createLexiconIndex(lexicon, lexiconEntries,lexiconEntrySize, indexPath, indexPrefix);
	}
	/**
	 * Creates the lexicon index file that contains a mapping from the
	 * given term id to the offset in the lexicon, in order to
	 * be able to retrieve the term information according to the
	 * term identifier. This is necessary, because the terms in the lexicon
	 * file are saved in lexicographical order, and we also want to have
	 * fast access based on their term identifier.
	 * @param lexicon The input stream of the lexicon that we are creating the lexid file for
	 * @param lexiconEntries The number of entries in this lexicon
	 * @param lexiconEntrySize The size of one entry in this lexicon
	 * @param path The path to the index containing the lexicon
	 * @param prefix The prefix of the index containing the lexicon
	 * @exception java.io.IOException Throws an Input/Output exception if
	 * 	there is an input/output error.
	 */

	public static void createLexiconIndex(final LexiconInputStream lexicon,
			final int lexiconEntries, final int lexiconEntrySize,
			final String path, final String prefix) throws IOException
	{
		//save the offsets to a file with the same name as
		//the lexicon and extension .lexid
		String lexid = path +
					ApplicationSetup.FILE_SEPARATOR +
					prefix +
					ApplicationSetup.LEXICON_INDEX_SUFFIX;
		DataOutputStream dosLexid = new DataOutputStream(Files.writeFileStream(lexid));
		createLexiconIndex(lexicon, lexiconEntries, lexiconEntrySize, dosLexid);
	}
	
	public static void createLexiconIndex(final LexiconInputStream lexicon,
				final int lexiconEntries, final int lexiconEntrySize,
				final DataOutputStream dosLexid) throws IOException
		{

		/*
		 * This method reads from the lexicon the term ids and stores the
		 * corresponding offsets in an array. Then this array is written out 
		 * in order according to the term id.
		 */
		long totalPointers = 0;
		long totalTokens = 0;


		//the i-th element of offsets contains the offset in the
		//lexicon file of the term with term identifier equal to i.
		long[] offsets = new long[lexiconEntries];
		int termid = -1;
		int i=0;
		try{
			while (lexicon.readNextEntry()!=-1) {
		 		termid = lexicon.getTermId();
				totalPointers += lexicon.getNt();
				totalTokens += lexicon.getTF();
				//Debugging: if an exception occurs here, then this infers that the number of entries in the lexicon
				//has been calculated incorrectly, or that termId > lexiconEntries. termid > lexiconEntries could be
				//a sign that the lexicon is being decoded incorrecty - eg you're using LexiconInputStream instead of
				//UTFLexiconInputStream
				offsets[termid] = (long)i * (long)lexiconEntrySize;
				i++;
			}
		} catch (ArrayIndexOutOfBoundsException aioob) {
			logger.error("Termid overflow while creating lexid file: NumEntries="+lexiconEntries+ " entrySize="
				+lexiconEntrySize+ " termid="+termid, aioob);
		}
		lexicon.close();
		//write out the offsets
		for (i = 0; i < lexiconEntries; i++) {
			dosLexid.writeLong(offsets[i]);
		}
		dosLexid.close();
	}
	
	/** Creates a lexicon index for the specified index
	  * @param index Index to make the lexicon index for
	  */	
	public static void createLexiconIndex(Index index) throws IOException
	{
		final LexiconInputStream lis = (LexiconInputStream)index.getIndexStructureInputStream("lexicon");
		LexiconBuilder.createLexiconIndex(
			lis,
			index.getCollectionStatistics().getNumberOfUniqueTerms(), 
			lis.getEntrySize(), 
			index.getPath(),
			index.getPrefix());
	}

	/** Create a lexicon hash for the current index
	  * @param lexStream lexiconinputstream to process
	  */
	public void createLexiconHash(final LexiconInputStream lexStream) {
		LexiconBuilder.createLexiconHash(lexStream, indexPath, indexPrefix);
	}

	
	/** Creates a lexicon hash for the specified index
	 * @param index Index to make the LexiconHash for
	 */
	public static void createLexiconHash(final Index index) throws IOException
	{
		LexiconBuilder.createLexiconHash((LexiconInputStream)index.getIndexStructureInputStream("lexicon"),
			index.getPath(),index.getPrefix());
	}
	
	/**
	 * Creates a Lexicon hash. This method reads the lexicon and finds the entries which 
	 * start with a different letter. The offset of these entries
	 * is used to speed up the binary search performed during retrieval.
	 * These offsets are saved to a lex hash file beside the Lexicon in the Index.
	 * @param lexStream LexiconInputStream to process
	 * @param path Path to the index containing the lexicon
	 * @param prefix Prefix of the index containing the lexicon
	 */
	public static void createLexiconHash(final LexiconInputStream lexStream, final String path, final String prefix) {
		String filename = path + ApplicationSetup.FILE_SEPARATOR + prefix + ApplicationSetup.LEXICON_HASH_SUFFIX;
		try{
			createLexiconHash(lexStream, Files.writeFileStream(filename));
		} catch(IOException ioe) {
			logger.error("IOException while creating hash file in LexiconBuilder.createLexiconHash: " + ioe);
		}
	}
	
	public static void createLexiconHash(final LexiconInputStream lexStream, OutputStream out)
	{
		TIntObjectHashMap map = new TIntObjectHashMap();
		int previousFirstChar = -1;
		int firstChar = 0;
		int counter = -1;

		try {
			//read all the terms in the lexicon and 
			//mark the offset of the ones that start
			//with a different character from the 
			//previous entry.
			while (lexStream.readNextEntry()!=-1) {
				firstChar = lexStream.getTerm().charAt(0);
				if (firstChar!=previousFirstChar) {
					int[] boundaries = new int[] {counter, 0};
					map.put(firstChar, boundaries);
					previousFirstChar = firstChar;
				}
				counter++;
			}
			lexStream.close();

	
			//NB: map should not be too large, say 26+10, more if UTF characters			
			
			// after reading all the entries, update the upper 
			// boundary, which is zero from the previous step.
			int[] mapKeys = map.keys();
			Arrays.sort(mapKeys);
			final int mapKeysSize = mapKeys.length;
			for (int i=0; i<mapKeysSize-1; i++) {
				int nextLowerBoundary = ((int[])map.get(mapKeys[i+1]))[0];
				int[] currentBoundaries = (int[])map.get(mapKeys[i]);
				currentBoundaries[1] = nextLowerBoundary;
				map.put(mapKeys[i], currentBoundaries);
			}
			//do something about the last entry
			int nextLowerBoundary = counter;
			int[] currentBoundaries = (int[])map.get(mapKeys[mapKeysSize-1]);
			currentBoundaries[1] = nextLowerBoundary;
			map.put(mapKeys[mapKeysSize-1], currentBoundaries);
			ObjectOutputStream oos = new ObjectOutputStream(out);
			oos.writeObject(map);
			oos.close();
			//logger.debug("Wrote lexicon hash to "+ filename);	
		} catch(IOException ioe) {
			logger.error("IOException while reading the lexicon in LexiconBuilder.createLexiconHash: " + ioe);
		}
	}

	public static void main(String args[])
	{
		boolean USE_UTF = Boolean.parseBoolean(ApplicationSetup.getProperty("string.use_utf", "false"));
		
		try{
			if ((args.length == 3||args.length ==4  )&& args[0].equals("--createlexiconindex"))
			{
				if (USE_UTF)
					createLexiconIndex(
							new UTFLexiconInputStream(args[1], args[2]),
							args.length == 4
								? Integer.parseInt(args[3])
								: Lexicon.numberOfEntries(args[1] + ApplicationSetup.FILE_SEPARATOR + args[2] + ApplicationSetup.LEXICONSUFFIX),
							Lexicon.lexiconEntryLength,
							args[1], args[2]);
				else	
					createLexiconIndex(
							new LexiconInputStream(args[1], args[2]),
							args.length == 4
							? Integer.parseInt(args[3])
									: Lexicon.numberOfEntries(args[1] + ApplicationSetup.FILE_SEPARATOR + args[2] + ApplicationSetup.LEXICONSUFFIX),
									Lexicon.lexiconEntryLength,
									args[1], args[2]);
			}
			else if (args.length == 3 && args[0].equals("--createlexiconhash"))
			{
				if (USE_UTF)
					createLexiconHash( new UTFLexiconInputStream(args[1], args[2]), args[1], args[2]);
				else
					createLexiconHash( new LexiconInputStream(args[1], args[2]), args[1], args[2]);
			}
			else
			{
				logger.fatal("Usage: uk.ac.gla.terrier.indexing.structures.LexiconBuilder {--createlexiconindex|--createlexiconhash} /path/to/index fileprefix [numEntries]");
				logger.fatal("Exiting ...");
				System.exit(0);
			}
		} catch (IOException ioe) {
			logger.error("IOException while building lexicon index : ",ioe);
			
		}
	}


	/** return the lexicon input stream for the current index at the specified filename */	
	protected LexiconInputStream getLexInputStream(String filename)
	{
		LexiconInputStream li = null;
		try{
			li = (LexiconInputStream) lexiconInputStream.getConstructor(String.class).newInstance(filename);
		} catch (Exception e) {
			logger.error("Problem loading a LexiconInputStream", e);
		}
		return li;
	}

	/** return the lexicon outputstream or the current index at the specified filename */
	protected LexiconOutputStream getLexOutputStream(String filename)
	{
		LexiconOutputStream lo = null;
		try{
			lo = (LexiconOutputStream) lexiconOutputStream.getConstructor(String.class).newInstance(filename);
		} catch (Exception e) {
			logger.error("Problem loading a LexiconOutputStream", e);
		}
		return lo;
	}

}

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
 * The Original Code is BlockDirectIndex.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk> (original author)
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
 */

package uk.ac.gla.terrier.structures.merging;

import java.io.IOException;
import java.util.Date;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.LexiconInputStream;
import uk.ac.gla.terrier.structures.LexiconOutputStream;
import uk.ac.gla.terrier.structures.UTFLexiconOutputStream;
import uk.ac.gla.terrier.structures.indexing.LexiconBuilder;
import uk.ac.gla.terrier.utility.ApplicationSetup;

/**
 * Merges many lexicons, termids and offsets are not kept.
 * @author vassilis
 */
public class LexiconMerger {
	protected final boolean UTFIndexing = Boolean.parseBoolean(ApplicationSetup.getProperty("string.use_utf", "false"));
	protected boolean USE_HASH = Boolean.parseBoolean(ApplicationSetup.getProperty("lexicon.use.hash","true"));

	/** The logger used */
	private static Logger logger = Logger.getRootLogger();

	protected Index srcIndex1;
	protected Index srcIndex2;
	protected Index destIndex;

	
	/**
	 * A constructor that sets the filenames of the lexicon
	 * files to merge
	 * @param src1 Source index 1
	 * @param src2 Source index 2
	 * @param dest Destination index
	 */
	public LexiconMerger(Index src1, Index src2, Index dest)
	{
		srcIndex1 = src1;
		srcIndex2 = src2;
		destIndex = dest;
	}
	
	/**
	 * Merges the two lexicons into one. After this stage, the offsets in the
	 * lexicon are not correct. They will be updated only after creating the 
	 * inverted file.
	 */
	public void mergeLexicons() {
		try {
			
			//setting the input streams
			final LexiconInputStream lexInStream1 = (LexiconInputStream)srcIndex1.getIndexStructureInputStream("lexicon");
			final LexiconInputStream lexInStream2 = (LexiconInputStream)srcIndex2.getIndexStructureInputStream("lexicon");
			
		
			//setting the output stream
			LexiconOutputStream lexOutStream = UTFIndexing
				? new UTFLexiconOutputStream(destIndex.getPath(), destIndex.getPrefix())
				: new LexiconOutputStream(destIndex.getPath(), destIndex.getPrefix());
			
			int hasMore1 = -1;
			int hasMore2 = -1;
			String term1;
			String term2;

			int termId = 0;
		
			hasMore1 = lexInStream1.readNextEntry();
			hasMore2 = lexInStream2.readNextEntry();
			while (hasMore1 >=0 && hasMore2 >= 0) {
				term1 = lexInStream1.getTerm();
				term2 = lexInStream2.getTerm();
				//System.out.println("term1 : " + term1 + "with id " + lexInStream1.getTermId());
				//System.out.println("term2 : " + term2 + "with id " + lexInStream2.getTermId());
				int lexicographicalCompare = term1.compareTo(term2);
				if (lexicographicalCompare < 0) {
					
					lexOutStream.writeNextEntry(term1,
									   termId,
									   lexInStream1.getNt(),
									   lexInStream1.getTF(),
									   0L,
									   (byte)0);
					termId++;
					hasMore1 = lexInStream1.readNextEntry();
				
				} else if (lexicographicalCompare > 0) {
					
					lexOutStream.writeNextEntry(term2,
									   			termId,
									   			lexInStream2.getNt(),
									   			lexInStream2.getTF(),
									   			0L,
									   			(byte)0);
					termId++;
					hasMore2 = lexInStream2.readNextEntry();
				} else {
					lexOutStream.writeNextEntry(term1,
												termId,
												(lexInStream1.getNt() + lexInStream2.getNt()),
												(lexInStream1.getTF() + lexInStream2.getTF()),
												0L,
												(byte)0);
					hasMore1 = lexInStream1.readNextEntry();
					hasMore2 = lexInStream2.readNextEntry();
					termId++;
				}
			}
			
			if (hasMore1 >= 0) {
				while (hasMore1 >= 0) {
					lexOutStream.writeNextEntry(lexInStream1.getTerm(),
									   			termId,
									   			lexInStream1.getNt(),
									   			lexInStream1.getTF(),
									   			0L,
												(byte)0);
					hasMore1 = lexInStream1.readNextEntry();
					termId++;
				}
			} else if (hasMore2 >= 0) {
				while (hasMore2 >= 0) {
					lexOutStream.writeNextEntry(lexInStream2.getTerm(),
												termId,
												lexInStream2.getNt(),
												lexInStream2.getTF(),
												0L,
												(byte)0);
					hasMore2 = lexInStream2.readNextEntry();
					termId++;
				}		
			}
			
			lexInStream1.close();
			lexInStream2.close();
			destIndex.setIndexProperty("num.Pointers", ""+lexOutStream.getNumberOfPointersWritten());
			destIndex.setIndexProperty("num.Terms", ""+lexOutStream.getNumberOfTermsWritten());
			destIndex.setIndexProperty("num.Tokens", ""+lexOutStream.getNumberOfTokensWritten());
			destIndex.addIndexStructure("lexicon", UTFIndexing
					? "uk.ac.gla.terrier.structures.UTFLexicon"
					: "uk.ac.gla.terrier.structures.Lexicon");
			destIndex.addIndexStructureInputStream("lexicon", UTFIndexing
					? "uk.ac.gla.terrier.structures.UTFLexiconInputStream"
					: "uk.ac.gla.terrier.structures.LexiconInputStream");
			lexOutStream.close();
			destIndex.flush();
		} catch(IOException ioe) {
			logger.error("IOException while merging lexicons.", ioe);
		}
		// create an empty lexid file
		//try{
		//	BufferedWriter bw = new BufferedWriter(Files.writeFileWriter(
		//			this.lexiconFileOutput+"id"));
		//	bw.write(" ");
		//	bw.close();
		//}
		//catch(IOException e){
		//	e.printStackTrace();
		//}
		try{
			LexiconBuilder.createLexiconIndex(destIndex);
			if (USE_HASH)
				LexiconBuilder.createLexiconHash(destIndex);
		} catch (IOException ioe) {
			logger.warn("Problems writing lexicon lexid or lexicon hash", ioe);
		}
	}
	public static void main(String[] args) {

		if (args.length != 6)
		{
			logger.fatal("usage: java uk.ac.gla.terrier.structures.merging.LexiconMerger srcPath1 srcPrefix1 srcPath2 srcPrefix2 destPath1 destPrefix1 ");
			logger.fatal("Exiting ...");
			System.exit(1);
		}

		Index indexSrc1 = Index.createIndex(args[0], args[1]);
		Index indexSrc2 = Index.createIndex(args[2], args[3]);
		Index indexDest = Index.createNewIndex(args[4], args[5]);

		LexiconMerger lMerger = new LexiconMerger(indexSrc1, indexSrc2, indexDest);
		long start = System.currentTimeMillis();
		if(logger.isInfoEnabled()){
			logger.info("started at " + (new Date()));
		}
		lMerger.mergeLexicons();
		indexSrc1.close();
		indexSrc2.close();
		indexDest.close();

		if(logger.isInfoEnabled()){
			logger.info("finished at " + (new Date()));
			long end = System.currentTimeMillis();
			logger.info("time elapsed: " + ((end-start)*1.0d/1000.0d) + " sec.");
		}
	}

	
}

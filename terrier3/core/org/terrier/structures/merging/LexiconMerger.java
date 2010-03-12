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
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk> (original author)
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
 */

package org.terrier.structures.merging;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.apache.hadoop.io.Text;
import org.apache.log4j.Logger;

import org.terrier.structures.FSOMapFileLexiconOutputStream;
import org.terrier.structures.Index;
import org.terrier.structures.IndexUtil;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.LexiconOutputStream;
import org.terrier.structures.Pointer;
import org.terrier.structures.SimpleBitIndexPointer;
import org.terrier.structures.indexing.LexiconBuilder;
import org.terrier.structures.seralization.FixedSizeWriteableFactory;
import org.terrier.utility.ApplicationSetup;

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
	@SuppressWarnings("unchecked")
	public void mergeLexicons() {
		try {
			
			//setting the input streams
			Iterator<Map.Entry<String,LexiconEntry>> lexInStream1 = 
				(Iterator<Map.Entry<String,LexiconEntry>>)srcIndex1.getIndexStructureInputStream("lexicon");
			Iterator<Map.Entry<String,LexiconEntry>> lexInStream2 = 
				(Iterator<Map.Entry<String,LexiconEntry>>)srcIndex2.getIndexStructureInputStream("lexicon");
			
			
			destIndex.setIndexProperty("lexicon-keyfactory", srcIndex1.getIndexProperty("lexicon-keyfactory", null));
			destIndex.setIndexProperty("lexicon-valuefactory", srcIndex1.getIndexProperty("lexicon-valuefactory", null));
			
		
			//setting the output stream
			LexiconOutputStream<String> lexOutStream = new FSOMapFileLexiconOutputStream(
					destIndex.getPath(), destIndex.getPrefix(), 
					"lexicon", 
					(FixedSizeWriteableFactory<Text>)destIndex.getIndexStructure("lexicon-keyfactory"));
			
			boolean hasMore1 = false;
			boolean hasMore2 = false;
			String term1;
			String term2;

			int termId = 0;
			
			Pointer p = new SimpleBitIndexPointer();
		
			hasMore1 = lexInStream1.hasNext();
			hasMore2 = lexInStream2.hasNext();
			Map.Entry<String,LexiconEntry> lee1 = null;
			Map.Entry<String,LexiconEntry> lee2 = null;
			while (hasMore1 && hasMore2) {
				lee1 = lexInStream1.next();
				lee2 = lexInStream2.next();
				
				term1 = lee1.getKey();
				term2 = lee2.getKey();
				int lexicographicalCompare = term1.compareTo(term2);
				if (lexicographicalCompare < 0) {
					lee1.getValue().setTermId(termId);
					lee1.getValue().setPointer(p);
					lexOutStream.writeNextEntry(term1, lee1.getValue());
					termId++;
					hasMore1 = lexInStream1.hasNext();
				
				} else if (lexicographicalCompare > 0) {
					lee2.getValue().setTermId(termId);
					lee2.getValue().setPointer(p);
					lexOutStream.writeNextEntry(term2, lee2.getValue());
					termId++;
					hasMore2 = lexInStream2.hasNext();
				} else {
					lee1.getValue().setTermId(termId);
					lee1.getValue().setPointer(p);
					lee1.getValue().add(lee2.getValue());
					lexOutStream.writeNextEntry(term1, lee1.getValue());
					hasMore1 = lexInStream1.hasNext();
					hasMore2 = lexInStream2.hasNext();
					termId++;
				}
			}
			
			if (hasMore1) {
				while (hasMore1) {
					lee1.getValue().setTermId(termId);
					lee1.getValue().setPointer(p);
					lexOutStream.writeNextEntry(lee1.getKey(), lee1.getValue());
					hasMore1 = lexInStream1.hasNext();
					termId++;
				}
			} else if (hasMore2) {
				while (hasMore2) {
					lee1.getValue().setTermId(termId);
					lee1.getValue().setPointer(p);
					lexOutStream.writeNextEntry(lee2.getKey(), lee2.getValue());
					hasMore2 = lexInStream2.hasNext();
					termId++;
				}		
			}
			IndexUtil.close(lexInStream1);
			IndexUtil.close(lexInStream2);
			lexOutStream.close();
			LexiconBuilder.optimise(destIndex, "lexicon");
			destIndex.flush();
		} catch(IOException ioe) {
			logger.error("IOException while merging lexicons.", ioe);
		}
	}
	public static void main(String[] args) throws Exception {

		if (args.length != 6)
		{
			logger.fatal("usage: java org.terrier.structures.merging.LexiconMerger srcPath1 srcPrefix1 srcPath2 srcPrefix2 destPath1 destPrefix1 ");
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

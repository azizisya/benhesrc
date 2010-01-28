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
 * The Original Code is InvertedIndex.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Douglas Johnson <johnsoda{a.}dcs.gla.ac.uk> (original author)
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk> 
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.structures;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.log4j.Logger;

import uk.ac.gla.terrier.compression.BitFile;
import uk.ac.gla.terrier.compression.BitIn;
import uk.ac.gla.terrier.compression.BitInSeekable;
import uk.ac.gla.terrier.compression.OldBitFile;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.FieldScore;
/**
 * This class implements the inverted index 
 * for performing retrieval, with field information
 * optionally.
 * @author Douglas Johnson, Vassilis Plachouras, Craig Macdonald
 * @version $Revision: 1.1 $
 */
public class InvertedIndex implements LegacyBitFileStructure, Closeable {
	/** The logger used for the Lexicon */
	protected static Logger logger = Logger.getRootLogger();
	
	/** This is used during retrieval for a rough guess sizing of the temporaryTerms
	  * arraylist in getDocuments(). The higher this value, the less chance that the
	  * arraylist will have to be grown (growing is expensive), however more memory
	  * may be used unnecessarily. */
	public static final double NORMAL_LOAD_FACTOR = 1.0;
	/** This is used during retrieval for a rough guess sizing of the temporaryTerms
	  * arraylist in getDocuments() - retrieval with Fields. The higher this value, 
	  * the less chance that the arraylist will have to be grown (growing is expensive), 
	  * however more memory may be used unnecessarily. */
	public static final double FIELD_LOAD_FACTOR = 1.0;
	/** Indicates whether field information is used.*/
	final boolean useFieldInformation = FieldScore.USE_FIELD_INFORMATION;
	
	/**
	 * The underlying bit file.
	 */
	protected BitInSeekable file;
	/** Filename of the open file */
	protected String filename;
	
	/**
	 * The lexicon used for retrieving documents.
	 */
	protected Lexicon lexicon;

	/** A constructor for child classes that doesnt open the file */
	protected InvertedIndex(long a, long b, long c) { }

	/** A default constructor, only for use by child classes */
	protected InvertedIndex()
	{
	
	}

	public InvertedIndex(Lexicon lexicon, String path, String prefix)
	{
		this(lexicon, path + ApplicationSetup.FILE_SEPARATOR + prefix + ApplicationSetup.IFSUFFIX);
	}
	
	/**
	 * Creates an instance of the HtmlInvertedIndex class using the lexicon.
	 * @param lexicon The lexicon used for retrieval
	 */
	public InvertedIndex(Lexicon lexicon) {
		this(lexicon, ApplicationSetup.TERRIER_INDEX_PATH, ApplicationSetup.TERRIER_INDEX_PREFIX);
		//file = new BitFile(ApplicationSetup.INVERTED_FILENAME, "r");
		//this.lexicon = lexicon;
	}
	/**
	 * Creates an instance of the HtmlInvertedIndex class using the given
	 * lexicon.
	 * @param lexicon The lexicon used for retrieval
	 * @param filename The name of the inverted file
	 */
	public InvertedIndex(Lexicon lexicon, String filename) {
		file = new BitFile(this.filename = filename, "r");
		this.lexicon = lexicon;
	}
	/** forces the data structure to reopen the underlying bitfile
	 *  using the legacy implementation of BitFile (OldBitFile)
	 * @throws IOException
	 */
	public void reOpenLegacyBitFile() throws IOException
	{
		try{file.close();} catch (IOException ioe) {/* dont care */}
		file = new OldBitFile(filename, "r");
	}
	
	/**
	 * Prints out the inverted index file.
	 */
	public void print() {
		for (int i = 0; i < lexicon.getNumberOfLexiconEntries(); i++) {
			int[][] documents = getDocuments(i);
			System.out.print("tid"+i);
			if (useFieldInformation) {
				for (int j = 0; j < documents[0].length; j++) {
					System.out.print("(" + documents[0][j] + ", " + documents[1][j]
							+ ", F" + documents[2][j] + ") ");
				}
				System.out.println();				
			} else {
				for (int j = 0; j < documents[0].length; j++) {
					System.out.print("(" + documents[0][j] + ", " 
										 + documents[1][j] + ") ");
				}
				System.out.println();
			}
		}
	}

	public int[][] getDocuments(LexiconEntry lEntry) {
		if (lEntry==null)
			return null;
		return getDocuments(lEntry.startOffset, 
			lEntry.startBitOffset, 
			lEntry.endOffset, 
			lEntry.endBitOffset, lEntry.n_t);
	}
	/**
	 * Returns a two dimensional array containing the document ids, term
	 * frequencies and field scores for the given documents. 	  
	 * @return int[][] the two dimensional [3][n] array containing the n 
	 *		 document identifiers, frequencies and field scores. If fields is not enabled, then size is [2][n].
	 * @param termid the identifier of the term whose documents we are looking for.
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
	
/**
	 * Returns a two dimensional array containing the document ids, term
	 * frequencies and field scores for the given documents. 	  
	 * @return int[][] the two dimensional [3][n] array containing the n 
	 *		 document identifiers, frequencies and field scores. If fields is not enabled, then size is [2][n].
	 * @param sOffset start byte of the postings in the inverted file
	 * @param sBitOffset start bit of the postings in the inverted file
	 * @param eOffset end byte of the postings in the inverted file
	 * @param eBitOffset end bit of the postings in the inverted file
	 */
	
	public int[][] getDocuments(long sOffset, byte sBitOffset, long eOffset, byte eBitOffset, int df) {
		
		final byte startBitOffset = sBitOffset;
		final long startOffset = sOffset;
		final byte endBitOffset = eBitOffset;
		final long endOffset = eOffset;
		final int fieldCount = FieldScore.FIELDS_COUNT;
		final boolean loadTagInformation = FieldScore.USE_FIELD_INFORMATION;
		//int df = lexicon.getNt();
		try{
			int[][] documentTerms = null;
			final BitIn file = this.file.readReset(startOffset, startBitOffset, endOffset, endBitOffset);		
			if (loadTagInformation) { //if there are tag information to process			
				documentTerms = new int[3][df];
				documentTerms[0][0] = file.readGamma() - 1;
				documentTerms[1][0] = file.readUnary();
				documentTerms[2][0] = file.readBinary(fieldCount);
				for (int i = 1; i < df; i++) {					
					documentTerms[0][i]  = file.readGamma() + documentTerms[0][i - 1];
					documentTerms[1][i]  = file.readUnary();
					documentTerms[2][i]  = file.readBinary(fieldCount);
				}				
			} else { //no tag information to process					
				documentTerms = new int[2][df];
				//new		
				documentTerms[0][0] = file.readGamma() - 1;
				documentTerms[1][0] = file.readUnary();
				for(int i = 1; i < df; i++){							 
					documentTerms[0][i] = file.readGamma() + documentTerms[0][i - 1];
					documentTerms[1][i] = file.readUnary();
				}
			}
			return documentTerms;
		} catch (IOException ioe) {
			logger.error("Problem reading inverted index", ioe);
			return null;
		}
	}
	
	
//	public int[][] getDocuments(long sOffset, byte sBitOffset, long eOffset, byte eBitOffset) {
//	
//		final byte startBitOffset = sBitOffset;
//		final long startOffset = sOffset;
//		final byte endBitOffset = eBitOffset;
//		final long endOffset = eOffset;
//		final int fieldCount = FieldScore.FIELDS_COUNT;
//		final boolean loadTagInformation = FieldScore.USE_FIELD_INFORMATION;
//	
//		/* Coding is done separately for with Fields and without Fields, to keep
//		 * if's out of loops. */	
//		
//		ArrayList temporaryTerms = null; //instantiate when we know roughly how big it should be
//		int[][] documentTerms = null;
//		file.readReset(startOffset, startBitOffset, endOffset, endBitOffset);
//		//boolean hasMore = false;
//		if (loadTagInformation) { //if there are tag information to process
//			/* FIELD_LOAD_FACTOR provides a heuristical rough size need for the arraylist. */
//			/* could probably do a better optimisation by considering the number of fields.*/
//			//temporaryTerms = new ArrayList((int)((endOffset-startOffset)*FIELD_LOAD_FACTOR));
//			TIntArrayList temporaryDocids = new TIntArrayList((int)((endOffset-startOffset)*NORMAL_LOAD_FACTOR));
//			TIntArrayList temporaryTFs = new TIntArrayList((int)((endOffset-startOffset)*NORMAL_LOAD_FACTOR));
//			TIntArrayList temporaryFields = new TIntArrayList((int)((endOffset-startOffset)*NORMAL_LOAD_FACTOR));
//			int previousDocid = -1;
//			
//			while (((file.getByteOffset() + startOffset) < endOffset)
//					|| (((file.getByteOffset() + startOffset) == endOffset) && (file
//							.getBitOffset() < endBitOffset))) {
//				//read document ID
//				temporaryDocids.add(previousDocid = file.readGamma() + previousDocid);
//				//read document frequency
//				temporaryTFs.add(file.readUnary());
//				//read fields bitset (fieldCount bits long)
//				temporaryFields.add(file.readBinary(fieldCount));
//		
//				/*int[] tmp = new int[3];
//				//read documnent ID
//				tmp[0] = file.readGamma();
//				//read document frequency
//				tmp[1] = file.readUnary();
//				//read fields bitset (fieldCount bits long) 
//				tmp[2] = file.readBinary(fieldCount);
//				temporaryTerms.add(tmp);*/
//			}
//			final int postingsListSize = temporaryDocids.size();
//			documentTerms = new int[3][postingsListSize];
//			temporaryDocids.toNativeArray(documentTerms[0], 0, postingsListSize);
//			temporaryTFs.toNativeArray(documentTerms[1], 0, postingsListSize);
//			temporaryFields.toNativeArray(documentTerms[2], 0, postingsListSize);	
//			/*
//			documentTerms = new int[3][temporaryTerms.size()];
//			int[] tmpDocumentTerms0 = documentTerms[0];
//			int[] tmpDocumentTerms1 = documentTerms[1];
//			int[] tmpDocumentTerms2 = documentTerms[2];
//			tmpDocumentTerms0[0] = ((int[]) temporaryTerms.get(0))[0] - 1;
//			tmpDocumentTerms1[0] = ((int[]) temporaryTerms.get(0))[1];
//			tmpDocumentTerms2[0] = ((int[]) temporaryTerms.get(0))[2];
//			if (documentTerms[0].length > 1) {
//				for (int i = 1; i < documentTerms[0].length; i++) {
//					int[] tmpMatrix = (int[]) temporaryTerms.get(i);
//					tmpDocumentTerms0[i] = tmpMatrix[0] + documentTerms[0][i - 1];
//					tmpDocumentTerms1[i] = tmpMatrix[1];
//					tmpDocumentTerms2[i] = tmpMatrix[2];
//				}
//			}
//			*/		
//		} else { //no tag information to process
//			
//			/* NORMAL_LOAD_FACTOR provides a heuristical rough size need for the arraylist */
//			TIntArrayList temporaryDocids = new TIntArrayList((int)((endOffset-startOffset)*NORMAL_LOAD_FACTOR));
//			TIntArrayList temporaryTFs = new TIntArrayList((int)((endOffset-startOffset)*NORMAL_LOAD_FACTOR));
//			//temporaryTerms = new ArrayList((int)((endOffset-startOffset)*NORMAL_LOAD_FACTOR));
//
//			int previousDocid = -1;
//			while (((file.getByteOffset() + startOffset) < endOffset)
//					|| (((file.getByteOffset() + startOffset) == endOffset) && (file
//							.getBitOffset() < endBitOffset))) {
//				//read document ID
//				temporaryDocids.add(previousDocid = file.readGamma() + previousDocid);
//				//read document frequency
//				temporaryTFs.add(file.readUnary());
//				//int[] tmp = new int[2];
//				//read document ID
//				//tmp[0] = file.readGamma();
//				//read document frequency
//				//tmp[1] = file.readUnary();
//				//temporaryTerms.add(tmp);
//			}
//
//			final int postingsListSize = temporaryDocids.size(); /*temporaryTerms.size()*/
//			documentTerms = new int[2][postingsListSize];
//			temporaryDocids.toNativeArray(documentTerms[0], 0, postingsListSize);
//			temporaryTFs.toNativeArray(documentTerms[1], 0, postingsListSize);
//			//int last = -1;
//			//int[] tmpDocumentTerms0 = documentTerms[0];
//			//for(int i=0;i<postingsListSize;i++)
//			//{
//			//	last = tmpDocumentTerms0[i] = tmpDocumentTerms0[i] + last;
//			//}
//
//			//int[] tmpDocumentTerms0 = documentTerms[0];
//			//int[] tmpDocumentTerms1 = documentTerms[1];
//			//tmpDocumentTerms0[0] = temporaryDocids.get(0);//((int[]) temporaryTerms.get(0))[0] - 1;
//			//tmpDocumentTerms1[0] = temporaryTFs.get(0); //((int[]) temporaryTerms.get(0))[1];
//			//if (documentTerms[0].length > 1) {
//			//	for (int i = 1; i < documentTerms[0].length; i++) {
//			//		last = tmpDocumentTerms0[i] = temporaryDocids.get(i) + last;
//			//		tmpDocumentTerms1[i] = temporaryTFs.get(i);
//					//int[] tmpMatrix = (int[]) temporaryTerms.get(i);
//					//tmpDocumentTerms0[i] = tmpMatrix[0] + documentTerms[0][i - 1];
//					//tmpDocumentTerms1[i] = tmpMatrix[1];
//			//	}
//			//}
//		}
//		//System.out.println((endOffset-startOffset)+" , "+temporaryTerms.size());
//		return documentTerms;
//	}
	/* *
	 * Returns a five dimensional array containing the document ids, 
	 * the term frequencies, the field scores the block frequencies and 
	 * the block ids for the given documents. The returned postings are
	 * for the documents within a specified range of docids.
	 * @return int[][] the five dimensional [5][] array containing 
	 *		 the document ids, frequencies, field scores and block 
	 *		 frequencies, while the last vector contains the 
	 *		 block identifiers and it has a different length from 
	 *		 the document identifiers.
	 * @param termid the id of the term whose documents we are looking for.
	 * @param startDocid The starting docid that will be returned.
	 * @param endDocid The last possible docid that will be returned.
	 */
	/*public int[][] getDocuments(int termid, int startDocid, int endDocid) {
		// Coding is done separately for with Fields and without Fields, to keep
		  if's out of loops. 
		boolean found = lexicon.findTerm(termid);
		if (!found) 
			return null;
		
		byte startBitOffset = lexicon.getStartBitOffset();
		long startOffset = lexicon.getStartOffset();
		byte endBitOffset = lexicon.getEndBitOffset();
		long endOffset = lexicon.getEndOffset();
		final int fieldCount = FieldScore.FIELDS_COUNT;
		final boolean loadTagInformation = FieldScore.USE_FIELD_INFORMATION;
		
		ArrayList<int[]> temporaryTerms = null; //instantiate when we know roughly how big it should be
		int[][] documentTerms = null;
		try{
			final BitIn file = this.file.readReset(startOffset, startBitOffset, endOffset, endBitOffset);
			//boolean hasMore = false;
			if (loadTagInformation) { //if there are tag information to process
				// FIELD_LOAD_FACTOR provides a heuristical rough size need for the arraylist. 
				// could probably do a better optimisation by considering the number of fields.
				temporaryTerms = new ArrayList<int[]>((int)((endOffset-startOffset)*FIELD_LOAD_FACTOR));
				while (((file.getByteOffset() + startOffset) < endOffset)
						|| (((file.getByteOffset() + startOffset) == endOffset) && (file
								.getBitOffset() < endBitOffset))) {
					int[] tmp = new int[3];
					//read documnent ID
					tmp[0] = file.readGamma();
					//read document frequency
					tmp[1] = file.readUnary();
					//read fields bitset (fieldCount bits long) 
					tmp[2] = file.readBinary(fieldCount);
					if (tmp[0]>=startDocid && tmp[0]<=endDocid)
						temporaryTerms.add(tmp);
				}
				documentTerms = new int[3][temporaryTerms.size()];
				int[] tmpDocumentTerms0 = documentTerms[0];
				int[] tmpDocumentTerms1 = documentTerms[1];
				int[] tmpDocumentTerms2 = documentTerms[2];
				tmpDocumentTerms0[0] = ((int[]) temporaryTerms.get(0))[0] - 1;
				tmpDocumentTerms1[0] = ((int[]) temporaryTerms.get(0))[1];
				tmpDocumentTerms2[0] = ((int[]) temporaryTerms.get(0))[2];
				if (documentTerms[0].length > 1) {
					for (int i = 1; i < documentTerms[0].length; i++) {
						int[] tmpMatrix = (int[]) temporaryTerms.get(i);
						tmpDocumentTerms0[i] = tmpMatrix[0] + documentTerms[0][i - 1];
						tmpDocumentTerms1[i] = tmpMatrix[1];
						tmpDocumentTerms2[i] = tmpMatrix[2];
					}
				}			
			} else { //no tag information to process
				
				//NORMAL_LOAD_FACTOR provides a heuristical rough size need for the arraylist 
				temporaryTerms = new ArrayList<int[]>((int)((endOffset-startOffset)*NORMAL_LOAD_FACTOR));
				while (((file.getByteOffset() + startOffset) < endOffset)
						|| (((file.getByteOffset() + startOffset) == endOffset) && (file
								.getBitOffset() < endBitOffset))) {
					int[] tmp = new int[2];
					//read document ID
					tmp[0] = file.readGamma();
					//read document frequency
					tmp[1] = file.readUnary();
					temporaryTerms.add(tmp);
				}
				documentTerms = new int[2][temporaryTerms.size()];
				int[] tmpDocumentTerms0 = documentTerms[0];
				int[] tmpDocumentTerms1 = documentTerms[1];
				tmpDocumentTerms0[0] = ((int[]) temporaryTerms.get(0))[0] - 1;
				tmpDocumentTerms1[0] = ((int[]) temporaryTerms.get(0))[1];
				if (documentTerms[0].length > 1) {
					for (int i = 1; i < documentTerms[0].length; i++) {
						int[] tmpMatrix = (int[]) temporaryTerms.get(i);
						tmpDocumentTerms0[i] = tmpMatrix[0] + documentTerms[0][i - 1];
						tmpDocumentTerms1[i] = tmpMatrix[1];
					}
				}			
			}
		}
		catch (IOException ioe) {
			logger.error("Problem reading inverted index", ioe);
			return null;
		}
		
		return documentTerms;
	}*/
	
	/**
	 * Returns the information for a posting list in string format 
	 */
	public String getInfo(int term) {
			StringBuilder info = new StringBuilder();					
			int[][] documents = getDocuments(term);			
			if (useFieldInformation) {
				for (int j = 0; j < documents[0].length; j++) {
					info.append("(");
					info.append(documents[0][j]);
					info.append(","); 
					info.append(documents[1][j]);
					info.append(",");
					info.append(documents[2][j]);
					info.append(")");
				}							
			} else {
				for (int j = 0; j < documents[0].length; j++) {
					info.append("(");
					info.append(documents[0][j]);
					info.append(",");
					info.append(documents[1][j]);
					info.append(")");
				}				
			}
			return info.toString();
		}
	
	
	/**
	 * Closes the underlying bit file.
	 */
	public void close() {
		try{
			file.close();
		}catch (IOException ioe) {}
	}

	/**
	 * Returns the underlying bit file, in order to make more
	 * efficient use of the bit file during assigning scores
	 * to the retrieved documents.
	 * @return file the underlying bit file
	 */
	public BitInSeekable getBitFile() {
		return file;
	}

	
}

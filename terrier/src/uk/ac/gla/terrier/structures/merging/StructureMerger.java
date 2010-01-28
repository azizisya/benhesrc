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
 * The Original Code is StructureMerger.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk> (original author) 
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.structures.merging;

import gnu.trove.TIntIntHashMap;

import java.io.IOException;
import java.util.Date;

import uk.ac.gla.terrier.compression.BitOut;
import uk.ac.gla.terrier.sorting.SortAscendingPairedVectors;
import uk.ac.gla.terrier.sorting.SortAscendingTripleVectors;
import uk.ac.gla.terrier.structures.DirectIndex;
import uk.ac.gla.terrier.structures.DirectInvertedOutputStream;
import uk.ac.gla.terrier.structures.DocumentIndexInputStream;
import uk.ac.gla.terrier.structures.FilePosition;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.InvertedIndex;
import uk.ac.gla.terrier.structures.LexiconInputStream;
import uk.ac.gla.terrier.structures.LexiconOutputStream;
import uk.ac.gla.terrier.structures.UTFLexiconOutputStream;
import uk.ac.gla.terrier.structures.indexing.DocumentIndexBuilder;
import uk.ac.gla.terrier.structures.indexing.LexiconBuilder;
import uk.ac.gla.terrier.utility.ApplicationSetup;

import org.apache.log4j.Logger;

/**
 * This class merges the structures created by Terrier, so that
 * we use fewer and larger inverted and direct files.
 * <p>
 * <b>Properties:</b><ul>
 * <li><tt>string.use_utf</tt> - use UTF support in index. Set to <tt>false</tt> by default.</li>
 * <li><tt>lexicon.use.hash</tt> - build a lexicon hash file for new index. Set to <tt>true</tt> by default.</li>
 * <li><tt>merge.direct</tt> - merge the direct indices if both indices have them. Set to <tt>true by default.</li>
 * @author Vassilis Plachouras and Craig Macdonald
 * @version $Revision: 1.1 $
 */
public class StructureMerger {
	/** use UTF supporting lexicon */
	protected final boolean UTFIndexing = Boolean.parseBoolean(ApplicationSetup.getProperty("string.use_utf", "false"));
	/** build a lexicon hash */
	protected boolean USE_HASH = Boolean.parseBoolean(ApplicationSetup.getProperty("lexicon.use.hash","true"));
	
	/** the logger used */
	protected static Logger logger = Logger.getRootLogger();
	
	/** the number of bits to write for binary encoded numbers */
	protected int binaryBits;
	
	/** 
	 * A hashmap for converting the codes of terms appearing only in the 
	 * vocabulary of the second set of data structures into a new set of 
	 * term codes for the merged set of data structures.
	 */
	protected TIntIntHashMap termcodeHashmap = null;
	protected boolean keepTermCodeMap = false;
	
	/** The filename of the first inverted file */
	protected String invertedFile1 = null;
	
	/** The filename of the second inverted file */
	protected String invertedFile2 = null;
	
	/** The filename of the output merged inverted file */
	protected String invertedFileOutput = null;
	
	/** The number of documents in the merged structures. */
	protected int numberOfDocuments;
	
	/** The number of pointers in the merged structures. */
	protected long numberOfPointers;
	
	/** The number of terms in the collection. */
	protected int numberOfTerms;

	/** source index 1 */	
	protected Index srcIndex1; 
	/** source index 2 */
	protected Index srcIndex2; 
	/** destination index */
	protected Index destIndex;

	/** class to use to write direct file */	
	protected Class<? extends DirectInvertedOutputStream> directFileOutputStreamClass = DirectInvertedOutputStream.class;
	/** class to use to write inverted file */
	protected Class<? extends DirectInvertedOutputStream> invertedFileOutputStreamClass = DirectInvertedOutputStream.class;
	/** class to use to read the direct file */
	protected String directFileInputClass = "uk.ac.gla.terrier.structures.DirectIndex";
	/** class to use to read the direct file as a stream */
	protected String directFileInputStreamClass = "uk.ac.gla.terrier.structures.DirectIndexInputStream";
	/** class to use to read the inverted file */
	protected String invertedFileInputClass = "uk.ac.gla.terrier.structures.InvertedIndex";
	/** class to use to read the inverted file as a stream */
	protected String invertedFileInputStreamClass = "uk.ac.gla.terrier.structures.InvertedIndexInputStream";
	
	public StructureMerger(Index _srcIndex1, Index _srcIndex2, Index _destIndex)
	{
		this.srcIndex1 = _srcIndex1;
		this.srcIndex2 = _srcIndex2;
		this.destIndex = _destIndex;
		numberOfDocuments = 0;
		numberOfPointers = 0;
		numberOfTerms = 0;
	}
	
	protected static String[] getIndexPathPrefix(String _IFfilename)
	{
		
		String parts[] = _IFfilename.split(ApplicationSetup.FILE_SEPARATOR);
		String path = _IFfilename.replaceFirst(parts[parts.length -1]+"$", ""); 
		String prefix = parts[parts.length -1].replaceAll(ApplicationSetup.IFSUFFIX+"$", "");
		return new String[]{path,prefix};
	}
	
	/**
	 * A constructor that sets the filenames of the inverted
	 * files to merge
	 * @param _srcfilename1 the first inverted file to merge
	 * @param _srcfilename2 the second inverted file to merge
	 * @deprecated
	 */
	public StructureMerger(String _srcfilename1, String _srcfilename2) {
		String[] p1 = getIndexPathPrefix(_srcfilename1);
		String[] p2 = getIndexPathPrefix(_srcfilename2);
		srcIndex1 = Index.createIndex(p1[0], p1[1]);
		srcIndex2 = Index.createIndex(p2[0], p2[1]);
		
		//invertedFile1 = _filename1;
		//invertedFile2 = _filename2;
		numberOfDocuments = 0;
		numberOfPointers = 0;
		numberOfTerms = 0;
	}
	
	/**
	 * Sets the number of bits to write or read for binary encoded numbers
	 * @param bits the number of bits to write or read
	 */
	public void setNumberOfBits(int bits) {
		binaryBits = bits;
	}
	
	/**
	 * Sets the output filename of the merged inverted file
	 * @param _outputName the filename of the merged inverted file
	 * @deprecated
	 */
	public void setOutputFilename(String _outputName) {
		//invertedFileOutput = _outputName;
		String[] p = getIndexPathPrefix(_outputName);
		destIndex = Index.createNewIndex(p[0], p[1]);
	}
	
	/**
	 * Sets the output index. This index should have no documents
	 * @param _outputIndex the index to be merged to
	 */
	public void setOutputIndex(Index _outputIndex) {
		this.destIndex = _outputIndex;
		//invertedFileOutput = _outputName;
	}
	

	/**
	 * Merges the two lexicons into one. After this stage, the offsets in the
	 * lexicon are ot correct. They will be updated only after creating the 
	 * inverted file.
	 */
	protected void mergeInvertedFiles() {
		try {
			//getting the number of entries in the first document index, 
			//in order to assign the correct docids to the documents 
			//of the second inverted file.
			
			int numberOfDocs1 = srcIndex1.getCollectionStatistics().getNumberOfDocuments();
			int numberOfDocs2 = srcIndex2.getCollectionStatistics().getNumberOfDocuments();
						
			numberOfDocuments = numberOfDocs1 + numberOfDocs2;
			
			
			//creating a new map between new and old term codes
			if (keepTermCodeMap)
				termcodeHashmap = new TIntIntHashMap();

			//setting the input streams
			LexiconInputStream lexInStream1 = (LexiconInputStream)srcIndex1.getIndexStructureInputStream("lexicon");
			LexiconInputStream lexInStream2 = (LexiconInputStream)srcIndex2.getIndexStructureInputStream("lexicon");
			
			LexiconOutputStream lexOutStream = UTFIndexing
				? new UTFLexiconOutputStream(destIndex.getPath(), destIndex.getPrefix())
				: new LexiconOutputStream(destIndex.getPath(), destIndex.getPrefix());
				

			int newCodes = (int)srcIndex1.getCollectionStatistics().getNumberOfUniqueTerms(); 
			
			InvertedIndex inverted1 = srcIndex1.getInvertedIndex();
			InvertedIndex inverted2 = srcIndex2.getInvertedIndex();
			
			DirectInvertedOutputStream invOS =null;
			try{
				invOS = 
					(DirectInvertedOutputStream)invertedFileOutputStreamClass
					.getConstructor(String.class,Integer.TYPE)
					.newInstance(destIndex.getPath() + ApplicationSetup.FILE_SEPARATOR +  
								destIndex.getPrefix() + ApplicationSetup.IFSUFFIX,
								binaryBits);
			} catch (Exception e) {
				logger.error("Couldn't create specified DirectInvertedOutputStream", e);
				return;
			}
			
			//BitOut invertedOutput = new BitOutputStream(
			//	);

			int hasMore1 = -1;
			int hasMore2 = -1;
			String term1;
			String term2;
		
			hasMore1 = lexInStream1.readNextEntry();
			hasMore2 = lexInStream2.readNextEntry();
			while (hasMore1 >=0 && hasMore2 >= 0) {
				term1 = lexInStream1.getTerm();
				term2 = lexInStream2.getTerm();

				int lexicographicalCompare = term1.compareTo(term2);
				//System.err.println("Comparing "+lexInStream1.getTermId() +":"+ term1 + " with "+lexInStream2.getTermId()+ ":"+ term2 + " results="+lexicographicalCompare);
				if (lexicographicalCompare < 0) {
					
					//write to inverted file as well.
					int[][] docs = inverted1.getDocuments(lexInStream1.getTermId());
					invOS.writePostings(docs, docs[0][0]+1);
					//writePostings(docs, docs[0][0]+1, invertedOutput, binaryBits);
					numberOfPointers+=docs[0].length;
					long endOffset = invOS.getByteOffset();
					byte endBitOffset = invOS.getBitOffset();
					endBitOffset--;
					if (endBitOffset < 0 && endOffset > 0) {
						endBitOffset = 7;
						endOffset--;
					}
					
					lexOutStream.writeNextEntry(term1,
									   lexInStream1.getTermId(),
									   lexInStream1.getNt(),
									   lexInStream1.getTF(),
									   endOffset,
									   endBitOffset);
					hasMore1 = lexInStream1.readNextEntry();
				
				} else if (lexicographicalCompare > 0) {
					//write to inverted file as well.
					int[][] docs = inverted2.getDocuments(lexInStream2.getTermId());
					invOS.writePostings(docs, docs[0][0]+numberOfDocs1+1);
					//writePostings(docs, docs[0][0]+numberOfDocs1+1, invertedOutput, binaryBits);
					numberOfPointers+=docs[0].length;
					long endOffset = invOS.getByteOffset();
					byte endBitOffset = invOS.getBitOffset();
					
					endBitOffset--;
					if (endBitOffset < 0 && endOffset > 0) {
						endBitOffset = 7;
						endOffset--;
					}
					
					int newCode = newCodes++;
					if (keepTermCodeMap)
						termcodeHashmap.put(lexInStream2.getTermId(), newCode);
					
					lexOutStream.writeNextEntry(term2,
									   			newCode,
									   			lexInStream2.getNt(),
									   			lexInStream2.getTF(),
									   			endOffset,
									   			endBitOffset);
					hasMore2 = lexInStream2.readNextEntry();
				} else {
					//write to inverted file as well.
					int[][] docs1 = inverted1.getDocuments(lexInStream1.getTermId());
					int[][] docs2 = inverted2.getDocuments(lexInStream2.getTermId());
					invOS.writePostings(docs1, docs1[0][0]+1);
					//writePostings(docs1, docs1[0][0]+1, invertedOutput, binaryBits);
					numberOfPointers+=docs1[0].length;
					invOS.writePostings(docs2, docs2[0][0] + numberOfDocs1 - docs1[0][docs1[0].length-1]);
					//writePostings(docs2, docs2[0][0] + numberOfDocs1 - docs1[0][docs1[0].length-1], 
					//					invertedOutput, binaryBits);
					numberOfPointers+=docs2[0].length;
					long endOffset = invOS.getByteOffset();
					byte endBitOffset = invOS.getBitOffset();
					endBitOffset--;
					if (endBitOffset < 0 && endOffset > 0) {
						endBitOffset = 7;
						endOffset--;
					}
					
					int newCode = lexInStream1.getTermId();
					if (keepTermCodeMap)
						termcodeHashmap.put(lexInStream2.getTermId(), newCode);
					
					lexOutStream.writeNextEntry(term1,
												newCode,
												(lexInStream1.getNt() + lexInStream2.getNt()),
												(lexInStream1.getTF() + lexInStream2.getTF()),
												endOffset,
												endBitOffset);
					hasMore1 = lexInStream1.readNextEntry();
					hasMore2 = lexInStream2.readNextEntry();
				}
			}
			
			if (hasMore1 >= 0) {
				while (hasMore1 >= 0) {
					
					//write to inverted file as well.
					int[][] docs = inverted1.getDocuments(lexInStream1.getTermId());
					invOS.writePostings(docs, docs[0][0]+1);
					//writePostings(docs, docs[0][0]+1, invertedOutput, binaryBits);
					numberOfPointers+=docs[0].length;
					long endOffset = invOS.getByteOffset();
					byte endBitOffset = invOS.getBitOffset();
					//long endOffset = invertedOutput.getByteOffset();
					//byte endBitOffset = invertedOutput.getBitOffset();
					endBitOffset--;
					if (endBitOffset < 0 && endOffset > 0) {
						endBitOffset = 7;
						endOffset--;
					}
					
					lexOutStream.writeNextEntry(lexInStream1.getTerm(),
									   			lexInStream1.getTermId(),
									   			lexInStream1.getNt(),
									   			lexInStream1.getTF(),
									   			endOffset,
												endBitOffset);
					hasMore1 = lexInStream1.readNextEntry();
				}
			} else if (hasMore2 >= 0) {
				while (hasMore2 >= 0) {
					//write to inverted file as well.
					int[][] docs = inverted2.getDocuments(lexInStream2.getTermId());
					invOS.writePostings(docs, docs[0][0]+numberOfDocs1+1);
					//writePostings(docs, docs[0][0]+numberOfDocs1+1, invertedOutput, binaryBits);
					numberOfPointers+=docs[0].length;
					long endOffset = invOS.getByteOffset();
					byte endBitOffset = invOS.getBitOffset();
					
					//long endOffset = invertedOutput.getByteOffset();
					//byte endBitOffset = invertedOutput.getBitOffset();
					endBitOffset--;
					if (endBitOffset < 0 && endOffset > 0) {
						endBitOffset = 7;
						endOffset--;
					}
					
					int newCode = newCodes++;
					if (keepTermCodeMap)
						termcodeHashmap.put(lexInStream2.getTermId(), newCode);
						
					lexOutStream.writeNextEntry(lexInStream2.getTerm(),
												newCode,
												lexInStream2.getNt(),
												lexInStream2.getTF(),
												endOffset,
												endBitOffset);
					hasMore2 = lexInStream2.readNextEntry();		
				}		
			}
			
			lexInStream1.close();
			lexInStream2.close();
			

			inverted1.close();
			inverted2.close();
			invOS.close();
			
			destIndex.setIndexProperty("num.Documents", ""+numberOfDocuments);
			destIndex.setIndexProperty("num.Pointers", ""+lexOutStream.getNumberOfPointersWritten());
			destIndex.setIndexProperty("num.Terms", ""+lexOutStream.getNumberOfTermsWritten());
			destIndex.setIndexProperty("num.Tokens", ""+lexOutStream.getNumberOfTokensWritten());
			destIndex.addIndexStructure("lexicon", UTFIndexing 
					? "uk.ac.gla.terrier.structures.UTFLexicon" 
					: "uk.ac.gla.terrier.structures.Lexicon");
			destIndex.addIndexStructureInputStream("lexicon", UTFIndexing 
					? "uk.ac.gla.terrier.structures.UTFLexiconInputStream" 
					: "uk.ac.gla.terrier.structures.LexiconInputStream");
			destIndex.addIndexStructure(
					"inverted", 
					invertedFileInputClass, 
					"uk.ac.gla.terrier.structures.Lexicon,java.lang.String,java.lang.String", 
					"lexicon,path,prefix");
			destIndex.addIndexStructureInputStream(
                    "inverted",
                    invertedFileInputStreamClass,
                    "java.lang.String,java.lang.String,uk.ac.gla.terrier.structures.LexiconInputStream",
                    "path,prefix,lexicon-inputstream");
			lexOutStream.close();
			destIndex.flush();
								
		} catch(IOException ioe) {
			logger.error("IOException while merging lexicons and inverted files.", ioe);
		}
	}

		/**
	 * Merges the two lexicons into one. After this stage, the offsets in the
	 * lexicon are not correct. 
	 */
	protected void mergeLexicons() {
		try {
			//getting the number of entries in the first document index, 
			//in order to assign the correct docids to the documents 
			//of the second inverted file.			
			
			//creating a new map between new and old term codes
			if (keepTermCodeMap)
				termcodeHashmap = new TIntIntHashMap();
			
			//setting the input streams
			final LexiconInputStream lexInStream1 = (LexiconInputStream)srcIndex1.getIndexStructureInputStream("lexicon");
			final LexiconInputStream lexInStream2 = (LexiconInputStream)srcIndex2.getIndexStructureInputStream("lexicon");
			
			final LexiconOutputStream lexOutStream = UTFIndexing
				? new UTFLexiconOutputStream(destIndex.getPath(), destIndex.getPrefix())
				: new LexiconOutputStream(destIndex.getPath(), destIndex.getPrefix());
				

			int newCodes = (int)srcIndex1.getCollectionStatistics().getNumberOfUniqueTerms(); 

			int hasMore1 = -1;
			int hasMore2 = -1;
			String term1;
			String term2;
		
			hasMore1 = lexInStream1.readNextEntry();
			hasMore2 = lexInStream2.readNextEntry();
			while (hasMore1 >=0 && hasMore2 >= 0) {
				term1 = lexInStream1.getTerm();
				term2 = lexInStream2.getTerm();

				int lexicographicalCompare = term1.compareTo(term2);
				if (lexicographicalCompare < 0) {
					
					lexOutStream.writeNextEntry(term1,
									   lexInStream1.getTermId(),
									   lexInStream1.getNt(),
									   lexInStream1.getTF(),
									   0L,
									   (byte)0);
					hasMore1 = lexInStream1.readNextEntry();
				
				} else if (lexicographicalCompare > 0) {
					int newCode = newCodes++;
					if (keepTermCodeMap)
						termcodeHashmap.put(lexInStream2.getTermId(), newCode);
					
					lexOutStream.writeNextEntry(term2,
									   			newCode,
									   			lexInStream2.getNt(),
									   			lexInStream2.getTF(),
									   			0L,
									   			(byte)0);
					hasMore2 = lexInStream2.readNextEntry();
				} else {
					int newCode = lexInStream1.getTermId();
					if (keepTermCodeMap)
						termcodeHashmap.put(lexInStream2.getTermId(), newCode);
					
					lexOutStream.writeNextEntry(term1,
												newCode,
												(lexInStream1.getNt() + lexInStream2.getNt()),
												(lexInStream1.getTF() + lexInStream2.getTF()),
												0L,
												(byte)0);
					hasMore1 = lexInStream1.readNextEntry();
					hasMore2 = lexInStream2.readNextEntry();
				}
			}
			
			if (hasMore1 >= 0) {
				while (hasMore1 >= 0) {
									
					lexOutStream.writeNextEntry(lexInStream1.getTerm(),
									   			lexInStream1.getTermId(),
									   			lexInStream1.getNt(),
									   			lexInStream1.getTF(),
									   			0L,
												(byte)0);
					hasMore1 = lexInStream1.readNextEntry();
				}
			} else if (hasMore2 >= 0) {
				while (hasMore2 >= 0) {
					int newCode = newCodes++;
					if  (keepTermCodeMap)
						termcodeHashmap.put(lexInStream2.getTermId(), newCode);
						
					lexOutStream.writeNextEntry(lexInStream2.getTerm(),
												newCode,
												lexInStream2.getNt(),
												lexInStream2.getTF(),
												0L,
												(byte)0);
					hasMore2 = lexInStream2.readNextEntry();		
				}		
			}
			
			lexInStream1.close();
			lexInStream2.close();
			
			
			destIndex.setIndexProperty("num.Documents", ""+numberOfDocuments);
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
	}

	/**
	 * Merges the two direct files and the corresponding document id files.
	 */
	protected void mergeDirectFiles() {
		try {
			final DocumentIndexBuilder docidOutput = new DocumentIndexBuilder(destIndex);
			DirectInvertedOutputStream dfOutput = null;
			try{
				dfOutput = 
					(DirectInvertedOutputStream)invertedFileOutputStreamClass
					.getConstructor(String.class,Integer.TYPE)
					.newInstance(destIndex.getPath() + ApplicationSetup.FILE_SEPARATOR +  
								destIndex.getPrefix() + ApplicationSetup.DF_SUFFIX,
								binaryBits);
			} catch (Exception e) {
				logger.error("Couldn't create specified DirectInvertedOutputStream", e);
				return;
			}
				
			
			final DocumentIndexInputStream docidInput1 = (DocumentIndexInputStream)srcIndex1.getIndexStructureInputStream("document");
			final DirectIndex dfInput1 = srcIndex1.getDirectIndex();
	
			
			//traversing the first set of files, without any change
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
			
			
			final DocumentIndexInputStream docidInput2 = (DocumentIndexInputStream)srcIndex2.getIndexStructureInputStream("document");
			final DirectIndex dfInput2 = srcIndex2.getDirectIndex();
			while (docidInput2.readNextEntry() >= 0) {
				if (docidInput2.getDocumentLength() > 0)
				{
					
					int[][] terms = dfInput2.getTerms(docidInput2.getDocumentId());
					final int length = terms[0].length;
					for (int j=0; j<length; j++) {
						terms[0][j] = termcodeHashmap.get(terms[0][j]);
					}
					terms = sortVectors(terms);
					dfOutput.writePostings(terms, terms[0][0]+1);
				}
				long endByte = dfOutput.getByteOffset();
				byte endBit = dfOutput.getBitOffset();
				endBit--;

				if (endBit < 0 && endByte > 0) {
					endBit = 7;
					endByte--;
				}
				
				docidOutput.addEntryToBuffer(docidInput2.getDocumentNumber(), 
									 docidInput2.getDocumentLength(),
									 new FilePosition(endByte, endBit));
			}
			
			
			dfOutput.close();
			docidOutput.finishedCollections();
			docidOutput.close();
			docidInput2.close();
			docidInput1.close();
			dfInput1.close();
			dfInput2.close();
			destIndex.addIndexStructure(
					"direct", 
					"uk.ac.gla.terrier.structures.DirectIndex", 
					"uk.ac.gla.terrier.structures.DocumentIndex,java.lang.String,java.lang.String", 
					"document,path,prefix");
			destIndex.addIndexStructureInputStream(
					"direct", 
					"uk.ac.gla.terrier.structures.DirectIndexInputStream", 
					"uk.ac.gla.terrier.structures.DocumentIndexInputStream,java.lang.String,java.lang.String", 
					"document-inputstream,path,prefix");
			destIndex.flush();
			
		} catch(IOException ioe) {
			logger.error("IOException while merging df and docid files.", ioe);
		}
	}

	protected int[][] sortVectors(int[][] terms)
	{
		if (binaryBits>0) {
			SortAscendingTripleVectors.sort(terms[0], terms[1], terms[2]);
		} else {
			SortAscendingPairedVectors.sort(terms[0], terms[1]);
		}
		return terms;
	}
	
	/**
	 * Merges the two direct files and the corresponding document id files.
	 */
	protected void mergeDocumentIndexFiles() {
		try {
			//the output docid file
			final DocumentIndexBuilder docidOutput = new DocumentIndexBuilder(destIndex);
		
		
			//opening the first set of files.
			final DocumentIndexInputStream docidInput1 = (DocumentIndexInputStream)srcIndex1.getIndexStructureInputStream("document");
			
			//traversing the first set of files, without any change
			while (docidInput1.readNextEntry() >= 0) {
				
				docidOutput.addEntryToBuffer(docidInput1.getDocumentNumber(), 
									 docidInput1.getDocumentLength(),
									 new FilePosition(0L, (byte)0));
			}
			
			//processing the second file
			final DocumentIndexInputStream docidInput2 = (DocumentIndexInputStream)srcIndex2.getIndexStructureInputStream("document");
			
			
			while (docidInput2.readNextEntry() >= 0) {
				
				docidOutput.addEntryToBuffer(docidInput2.getDocumentNumber(), 
									 docidInput2.getDocumentLength(),
									 new FilePosition(0L, (byte)0));
			}

			docidOutput.finishedCollections();
			docidOutput.close();
			docidInput2.close();
			docidInput1.close();
			destIndex.flush();
			
		} catch(IOException ioe) {
			logger.error("IOException while merging docid files.", ioe);
		}
	}



	/** 
	 * creates the final term code to offset file, and the lexicon hash if enabled.
	 */
	protected void createLexidFile() {
		try {
			LexiconBuilder.createLexiconIndex(destIndex);
		} catch(IOException ioe) {
			logger.error("IOException while creating lexid file.", ioe);
		}
		if (USE_HASH)
			try{
				LexiconBuilder.createLexiconHash(destIndex);
			} catch (IOException ioe) {
				logger.error("IOException while creating lexicon hash file", ioe);
			}
	}
	
	/**
	 * Merges the structures created by terrier.
	 */
	public void mergeStructures() {
		final boolean bothInverted = srcIndex1.hasIndexStructure("inverted") && srcIndex2.hasIndexStructure("inverted");
		final boolean bothDirect = srcIndex1.hasIndexStructure("direct") && srcIndex2.hasIndexStructure("direct");
		final boolean bothLexicon = srcIndex1.hasIndexStructure("lexicon") && srcIndex2.hasIndexStructure("lexicon");
		final long t1 = System.currentTimeMillis();
		keepTermCodeMap = bothDirect;
		long t2 = 0;
		long t3 = 0;
		long t4 = 0;
		if (bothInverted)
		{
			mergeInvertedFiles();
			t2 = System.currentTimeMillis();
	        logger.info("merged inverted files in " + ((t2-t1)/1000.0d));
		}
		else if (bothLexicon)
		{
			mergeLexicons();
			t2 = System.currentTimeMillis();
    	    logger.info("merged lexicons in " + ((t2-t1)/1000.0d));
		}
		else
		{
			logger.warn("No inverted or lexicon - no merging of lexicons took place");
			t2 = System.currentTimeMillis();
		}
		
		if (bothInverted || bothLexicon)
		{
			createLexidFile();
			t3 = System.currentTimeMillis();
			logger.debug("created lexid file and lex hash in " + ((t3-t2)/1000.0d));
		}
		t3 = System.currentTimeMillis();

		if (! bothDirect || ApplicationSetup.getProperty("merge.direct","true").equals("false"))
		{	
			mergeDocumentIndexFiles();
			t4 = System.currentTimeMillis();
			logger.info("merged documentindex files in " + ((t4-t3)/1000.0d));
		} 
		else 
		{
			mergeDirectFiles();	
			t4 = System.currentTimeMillis();
			logger.info("merged direct files in " + ((t4-t3)/1000.0d));
		}
	
		if (keepTermCodeMap)
		{
			//save up some memory
			termcodeHashmap.clear();
			termcodeHashmap = null;
		}
	}

	/** Usage: java uk.ac.gla.terrier.structures.merging.StructureMerger [binary bits] [inverted file 1] [inverted file 2] [output inverted file] <p>
      * Binary bits concerns the number of fields in use in the index. */
	public static void main(String[] args) {
		
		if (args.length != 7)
		{
			logger.fatal("usage: java uk.ac.gla.terrier.structures.merging.StructureMerger [binary bits] srcPath1 srcPrefix1 srcPath2 srcPrefix2 destPath1 destPrefix1 ");
			logger.fatal("Exiting ...");
			return;
		}
		
		int bits = Integer.parseInt(args[0]);
		
		Index indexSrc1 = Index.createIndex(args[1], args[2]);
		Index indexSrc2 = Index.createIndex(args[3], args[4]);
		Index indexDest = Index.createNewIndex(args[5], args[6]);
		
		StructureMerger sMerger = new StructureMerger(indexSrc1, indexSrc2, indexDest);
		sMerger.setNumberOfBits(bits);
		long start = System.currentTimeMillis();
		logger.info("started at " + (new Date()));
		if (ApplicationSetup.getProperty("merger.onlylexicons","false").equals("true")) {
			sMerger.mergeLexicons();
		} else if (ApplicationSetup.getProperty("merger.onlydocids","false").equals("true")) {
			sMerger.mergeDocumentIndexFiles();
		} else {
			sMerger.mergeStructures();
		}
		indexSrc1.close();
		indexSrc2.close();
		indexDest.close();
		
		logger.info("finished at " + (new Date()));
		long end = System.currentTimeMillis();
		logger.info("time elapsed: " + ((end-start)*1.0d/1000.0d) + " sec.");
	}

	/**
	 * Writes the given postings to a bit file. Depending on 
	 * the value of the field binaryBits, this method will call the 
	 * appropriate method writeToInvertedFileFields, or
	 * writeToInvertedFileNoFields.
	 * @param postings the postings list to write.
	 * @param firstId the first identifier to write. This can be 
	 *        an id plus one, or the gap of the current id and the previous one.
	 * @param output the output bit file.
	 * @deprecated Please use DirectInvertedOutputStream instead
	 */
	public static void writePostings(int[][] postings, int firstId, BitOut output, int binaryBits)
			throws IOException {
		if (binaryBits>0) 
			writeFieldPostings(postings, firstId, output, binaryBits);
		else 
			writeNoFieldPostings(postings, firstId, output);
	}
	
	/**
	 * Writes the given postings to a bit file. This method assumes that
	 * field information is available as well.
	 * @param postings the postings list to write.
	 * @param firstId the first identifier to write. This can be 
	 *        an id plus one, or the gap of the current id and the previous one.
	 * @param output the output bit file.
	 * @deprecated use DirectInvertedIndexOutputStream
	 */
	public static void  writeFieldPostings(int[][] postings, int firstId, BitOut output, int binaryBits) 
			throws IOException {
		
		//local variables in order to reduce the number
		//of times we need to access a two-dimensional array
		final int[] postings0 = postings[0];
		final int[] postings1 = postings[1];
		final int[] postings2 = postings[2];
		
		//write the first entry
		output.writeGamma(firstId);
		output.writeUnary(postings1[0]);
		output.writeBinary(binaryBits, postings2[0]);
	
		final int length = postings0.length;
		for (int k = 1; k < length; k++) {
			output.writeGamma(postings0[k] - postings0[k - 1]);
			output.writeUnary(postings1[k]);
			output.writeBinary(binaryBits, postings2[k]);
		}
	}
	
	/**
	 * Writes the given postings to a bit file. This method assumes that
	 * field information is not available.
	 * @param postings the postings list to write.
	 * @param firstId the first identifier to write. This can be 
	 *        an id plus one, or the gap of the current id and the previous one.
	 * @param output the output bit file.
	 * @throws IOException if an error occurs during writing to a file.
	 * @deprecated use DirectInvertedIndexOutputStream
	 */
	public static void writeNoFieldPostings(int[][] postings, int firstId, BitOut output) 
			throws IOException {

		//local variables in order to reduce the number
		//of times we need to access a two-dimensional array
		final int[] postings0 = postings[0];
		final int[] postings1 = postings[1];
		
		//write the first entry
		output.writeGamma(firstId);
		output.writeUnary(postings1[0]);
	
		final int length = postings[0].length;
		for (int k = 1; k < length; k++) {
			output.writeGamma(postings0[k] - postings0[k - 1]);
			output.writeUnary(postings1[k]);
		}
	}


}


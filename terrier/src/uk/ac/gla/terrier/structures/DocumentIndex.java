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
 * The Original Code is DocumentIndex.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Gianni Amati <gba{a.}fub.it> (original author)
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk> 
 */
package uk.ac.gla.terrier.structures;
import java.io.File;
import java.io.IOException;
import org.apache.log4j.Logger;

import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.StringComparator;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.io.RandomDataInput;
/**
 * This class provides an interface for accessing
 * the document index file. Each entry in the document
 * index consists of a document id, the document number,
 * and the length of the document, that is the number
 * of terms that make up the document.
 * @author Gianni Amati, Vassilis Plachouras
 * @version $Revision: 1.1 $
 */
public class DocumentIndex implements Closeable, IndexConfigurable
{
	/** The logger used */
	private static Logger logger = Logger.getRootLogger();

	/** length of the docno field in bytes. Set by application property docno.byte.length,
	  * although this can be overwritten by the index property docno.byte.length. */	
	protected int DOCNO_BYTE_LENGTH = ApplicationSetup.DOCNO_BYTE_LENGTH;

	/** A string comparator for comparing document numbers of different lengths.*/
	protected final static StringComparator sComp = new StringComparator();
	
	/** A static buffer for reading strings from file.*/
	protected byte[] buffer = new byte[DOCNO_BYTE_LENGTH];
	/** A static buffer for writing zero values to the files.*/
	protected byte[] zeroBuffer = new byte[DOCNO_BYTE_LENGTH];
	/** The random access file representing the document index.*/
	protected RandomDataInput docIndex;
	/** 
	 * The length in bytes of an entry in the file.
	 * It is 2*sizeof(int) plus the length of the stored document number
	 * plus sizeof(long) plus sizeof(byte).
	 */
	public int entryLength =
		(8 + DOCNO_BYTE_LENGTH + 9);
	/**
	 * The number of entries in the document index.
	 */
	protected int numberOfDocumentIndexEntries;
	/**
	 * The last read document id. Initialised with -1, in order to 
	 * avoid problems with the document indexed as zero (0).
	 */
	protected int docid = -1;
	/**
	 * The last read document length
	 */
	protected int docLength;
	/**
	 * The last read document number
	 */
	protected String docno;
	/**
	 * The start byte offset in the direct file
	 */
	protected long startOffset;
	/**
	 * The start bit offset in the direct file.
	 */
	protected byte startBitOffset;
	/**
	 * The end byte offset in the direct file
	 */
	protected long endOffset;
	/**
	 * The end bit offset in the direct file.
	 */
	protected byte endBitOffset;

	protected String filename;

	/** A constructor for child classes that doesnt open the file */
	protected DocumentIndex(long a, long b, long c) { }
	
	/** A default constructor for the class.*/
	public DocumentIndex() {
		this(ApplicationSetup.DOCUMENT_INDEX_FILENAME);
	}
	/** 
	 * A constructor of a document index from a given filename. <br>
	 * For the document pointers file we replace the extension of the 
	 * document index file with the right default extension. 
	 * <br>
	 * The given name should have an extension.
	 * @param filename String the filename of the document index, with an extension
	 */
	public DocumentIndex(String filename) {
		docno = "";
		try {
			docIndex = Files.openFileRandom(this.filename = filename);
			numberOfDocumentIndexEntries = (int) docIndex.length() / entryLength;
		} catch (IOException ioe) {
			logger.fatal(
				"Input/Output exception during opening the document index file. Stack trace follows.",ioe);
		}
	}

	public DocumentIndex(String path, String prefix)
	{
		this(path+ ApplicationSetup.FILE_SEPARATOR + prefix + ApplicationSetup.DOC_INDEX_SUFFIX);
	}

	/** This structure can be configured by the Index object. In particular, the length docno.byte.length
	  * can be picked up automatically from the index for non-default installations. */
	public void setIndex(Index i)
	{
		String v = i.getIndexProperty("docno.byte.length", null);
		if (v != null)
		{
			int l = Integer.parseInt(v);
			if (DOCNO_BYTE_LENGTH != l)
				this.setDocnoEntryLength(l);
		}
	}

	/** Set the length of docnos in the index file */
	public void setDocnoEntryLength(int l)
	{
		this.DOCNO_BYTE_LENGTH = l;
		this.entryLength = (8 + DOCNO_BYTE_LENGTH + 9);
		this.buffer = new byte[DOCNO_BYTE_LENGTH];
		this.zeroBuffer = new byte[DOCNO_BYTE_LENGTH];
		try{
			numberOfDocumentIndexEntries = (int) docIndex.length() / entryLength;
		} catch (IOException ioe) { }
	}
	
	/**
	 * Closes the random access file.
	 */
	public void close() {
		try {
			docIndex.close();
		} catch (IOException ioe) {
			logger.error(
				"Input/Output exception while closing docIndex file. Stack trace follows",ioe);
		}
	}
	/**
	 * Prints out to the standard error stream the contents of 
	 * the document index file.
	 */
	public void print() {
		try {
			for (int i = 0; i < numberOfDocumentIndexEntries; i++) {
				docIndex.seek(i * entryLength);
				docid = docIndex.readInt();
				docLength = docIndex.readInt();
				docIndex.readFully(buffer, 0, DOCNO_BYTE_LENGTH);
				docno = new String(buffer);
				endOffset = docIndex.readLong();
				endBitOffset = docIndex.readByte();
				System.out.println(
					""
						+ (i * entryLength)
						+ ", "
						+ docid
						+ ", "
						+ docLength
						+ ", "
						+ docno.trim()
						+ ", "
						+ endOffset
						+ ", "
						+ endBitOffset);
			}
		} catch (IOException ioe) {
			logger.fatal(
				"Input/Output exception while reading the " +
				"document index file in the print() method. " +
				"Stack trace follows.",ioe);
		}
	}
	/**
	 * Returns the document's id for the given docno.
	 * @return int The document's id, or -1 if docno was not found in the index.
	 * @param docno java.lang.String The document's number
	 */
	public int getDocumentId(String docno) {
		if (!this.docno.equals(docno)) {
			try {
				if (seek(docno) == false)
					return -1;
			} catch (IOException ioe) {
				logger.fatal(
					"Input/Output exception during reading the document index file. Stack trace follows.",ioe);
			}
		}
		return docid;
	}
	/**
	 * Reading the length for the i-th document.
	 * @param i the index of the document.
	 * @return the length of the i-th document, or -1 if the docid i wasn't found in the index.
	 */
	public int getDocumentLength(int i) {
		if (i != docid) {
			try {
				if (seek(i) == false)
					return -1;
			} catch (IOException ioe) {
				logger.fatal(
					"Input/Output exception during reading the document index file. Stack trace follows.",ioe);
			}
		}
		return docLength;
	}
	/**
	 * Return the length of the document with the given docno.
	 * Creation date: (29/05/2003 10:56:49)
	 * @return int The document's length, or -1 if the docno wasn't found in the index.
	 * @param docno java.lang.String The document's number
	 */
	public int getDocumentLength(String docno) {
		if (!this.docno.equals(docno)) {
			try {
				if (seek(docno) == false)
					return -1;
			} catch (IOException ioe) {
				logger.fatal(
					"Input/Output exception during reading the doucment index file. Stack trace follows.",ioe);
			}
		}
		return docLength;
	}
	/**
	 * Reading the docno for the i-th document.
	 * @param i the index of the document.
	 * @return the document number of the i-th document, or null if the docid i wasn't found in the index.
	 */
	public String getDocumentNumber(int i) {
		if (i != docid) {
			try {
				if (seek(i) == false)
					return null;
			} catch (IOException ioe) {
				logger.fatal(
					"Input/Output exception during reading the doucment index file. Stack trace follows.",ioe);
			}
		}
		return docno;
	}
	/** 
	 * Returns the ending offset of the document's entry
	 * in the direct index.
	 * @return FilePosition an offset in the direct index.
	 */
	public FilePosition getDirectIndexEndOffset() {
		return new FilePosition(endOffset, endBitOffset);
	}
	/**
	 * Returns the number of documents in the collection.
	 * @return the number of documents in the collection.
	 */
	public int getNumberOfDocuments() {
		return numberOfDocumentIndexEntries;
	}
	/**
	 * Returns the starting offset of the document's entry
	 * in the direct index.
	 * @return FilePosition an offset in the direct index.
	 */
	public FilePosition getDirectIndexStartOffset() {
		return new FilePosition(startOffset, startBitOffset);
	}
	/**
	 * Seeks from the document index the i-th entry.
	 * @param i the document id.
	 * @return boolean true if it was found, otherwise
	 *		 it returns false.
	 */
	public boolean seek(int i) throws IOException {
		if (i >= numberOfDocumentIndexEntries)
			return false;
		if (i == 0) {
			startOffset = 0;
			startBitOffset = 0;
			docIndex.seek(0);
			docid = docIndex.readInt();
			docLength = docIndex.readInt();
			docIndex.readFully(buffer, 0, DOCNO_BYTE_LENGTH);
			docno = (new String(buffer)).trim();
			endOffset = docIndex.readLong();
			endBitOffset = docIndex.readByte();
		} else {
			docIndex.seek(
				(i - 1) * entryLength
					+ 8
					+ DOCNO_BYTE_LENGTH);
			startOffset = docIndex.readLong();
			startBitOffset = docIndex.readByte();
			startBitOffset++;
			if (startBitOffset == 8) {
				startBitOffset = 0;
				startOffset++;
			}
			docid = docIndex.readInt();
			docLength = docIndex.readInt();
			docIndex.readFully(buffer, 0, DOCNO_BYTE_LENGTH);
			docno = (new String(buffer)).trim();
			endOffset = docIndex.readLong();
			endBitOffset = docIndex.readByte();
		}
		return true;
	}
	/**
	 * Seeks the document index entry for the given document number.
	 * @return true if the document was found, false otherwise.
	 * @param docno java.lang.String the document's number
	 * @throws IOException an input/output exception when it can 
	 *		 not read from the file
	 */
	public boolean seek(String docno) throws IOException {
		//byte[] bt = docno.getBytes(); //the document we are searching for
		int readDocid = 0;
		long low = -1;
		long high = numberOfDocumentIndexEntries;
		long i;
		final int stringByteLength = DOCNO_BYTE_LENGTH;
		while (high-low>1) {
			i = (long)(high+low)/2;
			try {
				docIndex.seek((i * entryLength));
				readDocid = docIndex.readInt();
				docIndex.skipBytes(4);
				docIndex.readFully(buffer,0,stringByteLength);
			} catch (IOException ioe) {
				logger.fatal(
					"Input/Output exception while reading from document index file. Stack trace follows.",ioe);
			}
			int compareResult = 0;
			compareResult = sComp.compare(docno, new String(buffer).trim());
			if (compareResult < 1)
				high = i;
			else
				low = i;			
		}
		
		if (high == numberOfDocumentIndexEntries)
			return false;
		try {
			docIndex.seek((high * entryLength));
			readDocid = docIndex.readInt();
			docIndex.skipBytes(4);
			docIndex.readFully(buffer,0,stringByteLength);
		} catch (IOException ioe) {
			logger.fatal(
				"Input/Output exception while reading from document index file. Stack trace follows.",ioe);
		}	
		if (sComp.compare(docno, new String(buffer).trim()) == 0) {
			seek(readDocid);
			return true;
		} else {
			return false;
		}
	}

	public static void main(java.lang.String[] args) {
		DocumentIndex docIndex = new DocumentIndex(args[0]);
		if (args[1].equals("--print"))
			docIndex.print();
		else if (args[1].equals("--docid"))
			System.out.println(docIndex.getDocumentId(args[2]));
		else if (args[1].equals("--docno"))
			System.out.println(docIndex.getDocumentNumber(Integer.parseInt(args[2])));
	}

}

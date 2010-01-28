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
 * The Original Code is DocumentIndexInputStream.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk> (original author) 
 */
package uk.ac.gla.terrier.structures;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.ApplicationSetup;
/**
 * This class provides access to the document index 
 * file sequentially, as a stream. 
 * Each entry in the document index consists of a 
 * document id, the document number,  and the length 
 * of the document, that is the number of terms that 
 * make up the document.
 * @author Vassilis Plachouras
 * @version $Revision: 1.1 $
 */
public class DocumentIndexInputStream implements IndexConfigurable {
	/** The logger used */
	private static Logger logger = Logger.getRootLogger();
	/** Keep a local copy of the maximum docno term length */
	protected int DOCNO_BYTE_LENGTH = ApplicationSetup.DOCNO_BYTE_LENGTH;
	/** A byte array used as buffer.*/
	private byte[] buffer = new byte[DOCNO_BYTE_LENGTH];
	/** The buffer from which the file is document index file is read.*/
	protected DataInputStream dis = null;
	/** The last read document id */
	protected int docid;
	/** The last read document length */
	protected int docLength;
	/** The last read document number */
	protected String docno;
	/** The start byte offset in the direct file */
	protected long startOffset;
	/** The start bit offset in the direct file. */
	protected byte startBitOffset;
	/** The end byte offset in the direct file */
	protected long endOffset;
	/** The end bit offset in the direct file. */
	protected byte endBitOffset;
	/** 
	 * A constructor for the class.
	 * @param is java.io.InputStream The underlying input stream
	 */
	public DocumentIndexInputStream(InputStream is) {
		dis = new DataInputStream(is);
	}
	/** 
	 * A constructor of a document index, from a given filename.
	 * @param filename java.lang.String The name of the document index file.
	 */
	public DocumentIndexInputStream(String filename) {
		try {
			dis = new DataInputStream(Files.openFileStream(filename));
		} catch (IOException ioe) {
			logger.fatal(
				"Input/Output exception during opening the document index file. Stack trace follows.", ioe);
		}
	}
	/** 
	 * A default constructor of a document index, from a given filename.
	 */
	public DocumentIndexInputStream() {
		try {
			dis = new DataInputStream( Files.openFileStream(ApplicationSetup.DOCUMENT_INDEX_FILENAME));
		} catch (IOException ioe) {
			logger.fatal(
				"Input/Output exception during opening the document index file. Stack trace follows.",ioe);
		}
	}
	/** 
	 * A constructor of a document index, from a given filename.
	 * @param file java.io.File The document index file.
	 */
	public DocumentIndexInputStream(File file) {
		try {
			dis = new DataInputStream(Files.openFileStream(file));
		} catch (IOException ioe) {
			logger.fatal(
				"Input/Output exception during opening the document index file. Stack trace follows.",ioe);
		}
	}

	/** A constructor of a document index input stream from an index path and prefix.
	  * @param path String path to the index
	  * @param prefix String prefix of the filenames of the index
	  */
	public DocumentIndexInputStream(final String path, String prefix) {
		this(path + ApplicationSetup.FILE_SEPARATOR + prefix + ApplicationSetup.DOC_INDEX_SUFFIX) ;
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
		this.buffer = new byte[DOCNO_BYTE_LENGTH];
	}


	/**
	 * Closes the stream.
	 */
	public void close() {
		try {
			dis.close();
		} catch (IOException ioe) {
			logger.fatal(
				"Input/Output exception while closing docIndex file. Stack trace follows",ioe);
		}
	}
	/** 
	 * Reads the next entry from the stream.
	 * @return the number of bytes read from the stream, or 
	 * 		   -1 if EOF has been reached.
	 * @throws java.io.IOException if an I/O error occurs.
	 */
	public int readNextEntry() throws IOException {
		try {
			docid = dis.readInt();
			docLength = dis.readInt();
			dis.readFully(buffer, 0, DOCNO_BYTE_LENGTH);
			docno = (new String(buffer)).trim();
			endOffset = dis.readLong();
			endBitOffset = dis.readByte();
			return 17 + DOCNO_BYTE_LENGTH;
		} catch (EOFException eofe) {
			return -1;
		}
	}
	/**
	 * Prints out to the standard error stream 
	 * the contents of the document index file.
	 */
	public void print() {
		int i = 0; //a counter
		try {
			while (readNextEntry() != -1) {
				System.out.println(
					""
						//+ (i * DocumentIndex.entryLength)
						//+ ", "
						+ docid
						+ ", "
						+ docLength
						+ ", "
						+ docno
						+ ", "
						+ endOffset
						+ ", "
						+ endBitOffset);
				i++;
			}
		} catch (IOException ioe) {
			logger.error(
				"Input/Output exception while reading the document " +
				"index input stream. Stack trace follows.", ioe);
		}
	}
	/**
	 * Returns the document's id for the given docno.
	 * @return int The document's id
	 */
	public int getDocumentId() {
		return docid;
	}
	/**
	 * Return the length of the document with the given docno.
	 * @return int The document's length
	 */
	public int getDocumentLength() {
		return docLength;
	}
	/**
	 * Reading the docno for the i-th document.
	 * @return the document number of the i-th document.
	 */
	public String getDocumentNumber() {
		return docno;
	}
	/**
	 * Returns the bit offset in the ending byte in the direct 
	 * file's entry for this document
	 * @return byte the bit offset in the ending byte in 
	 * 				the direct file's entry for this document
	 */
	public byte getEndBitOffset() {
		return endBitOffset;
	}
	/**
	 * Returns the offset of the ending byte in the 
	 * direct file for this document
	 * @return long the offset of the ending byte in the 
	 * 				direct file for this document
	 */
	public long getEndOffset() {
		return endOffset;
	}
	/**
	 * Return the bit offset in the starting byte in the entry in 
	 * the direct file for this document.
	 * @return byte the bit offset in the starting byte in the entry in 
	 * 		   		the direct file.
	 */
	public byte getStartBitOffset() {
		return startBitOffset;
	}
	/**
	 * Return the starting byte in the direct file for this document.
	 * @return long the offset of the starting byte in the direct file
	 */
	public long getStartOffset() {
		return startOffset;
	}
}

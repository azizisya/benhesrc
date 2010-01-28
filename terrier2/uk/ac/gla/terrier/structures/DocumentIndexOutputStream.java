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
 * The Original Code is DocumentIndexBuilder.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk> (original author)
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.structures;
import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.structures.FilePosition;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;
/**
 * A DocumentIndexOutputStream. Based on code from structures.indexing.DocumentIndexBuilder
 * @author Vassilis Plachouras, Craig Macdonald
 * @version $Revision: 1.1 $
 */
public class DocumentIndexOutputStream {
	/** The logger used */
	private static Logger logger = Logger.getRootLogger();
	/** The stream to which we write the data. */
	protected DataOutputStream dos;
	/** The total number of entries in the document index.*/
	protected int numberOfDocumentIndexEntries;
	/** Maximum length of a docno */
	protected final static int DOCNO_BYTE_LENGTH = ApplicationSetup.DOCNO_BYTE_LENGTH;
	/** A static buffer for writing zero values to the files. */
	protected final static byte[] zeroBuffer = new byte[DOCNO_BYTE_LENGTH];
	
	/** A default constructor for this document index output stream. The default document index is created
	  * from ApplicationSetup.TERRIER_INDEX_PATH and ApplicationSetup.TERRIER_INDEX_PREFIX .*/
	public DocumentIndexOutputStream() {
		try {
			numberOfDocumentIndexEntries = 0;
			dos = new DataOutputStream(Files.writeFileStream(ApplicationSetup.DOCUMENT_INDEX_FILENAME));
		} catch (IOException ioe) {
			logger.error(
				"Input/Output exception during opening the document index file. Stack trace follows.",ioe);
		}	
	}
	/** 
	 * A constructor of a document index output stream from a given filename.
	 * @param filename String the filename of the document index, 
	 *		with an extension
	 */
	public DocumentIndexOutputStream(final String filename) {
		try {
			numberOfDocumentIndexEntries = 0;
			dos = new DataOutputStream(Files.writeFileStream( filename));
		} catch (IOException ioe) {
			logger.error(
				"Input/Output exception during opening the document index file. Stack trace follows.",ioe);
		}
	}

	/** A constructor of a document index output stream from an index path and prefix. 
	  * @param path String path to the index
	  * @param prefix String prefix of the filenames of the index
	  */
	public DocumentIndexOutputStream(final String path, String prefix) {
		this(path + ApplicationSetup.FILE_SEPARATOR + prefix + ApplicationSetup.DOC_INDEX_SUFFIX) ;
	}

	/**
	 * Adds to the index a new entry, giving to it the next 
	 * available document id. The entry is writen first
	 * to the buffer, which afterwards has to be flushed to 
	 * the file on disk.
	 * @param docno String the document number.
	 * @param docLength int the number of indexed tokens in the document.
	 * @param directIndexOffset FilePosition the ending position of the 
	 *		document's entry in the direct index.
	 * @exception java.io.IOException Throws an exception in the 
	 *			case of an IO error.
	 */
	public void addEntry(
		final String docno,
		final int docLength,
		final FilePosition directIndexOffset)
		throws java.io.IOException
	{
		addEntry(docno, docLength, directIndexOffset.Bytes, directIndexOffset.Bits);
	}	

	/**
	 * Adds to the index a new entry, giving to it the next
	 * available document id. The entry is writen first
	 * to the buffer, which afterwards has to be flushed to
	 * the file on disk.
	 * @param docno String the document number.
	 * @param docLength int the number of indexed tokens in the document.
	 * @param directIndexOffsetBytes long the ending bytes position of the
	 *		document's entry in the direct index.
	 * @param directIndexOffsetBits byte the ending bits position of the
	 *		document's entry in the direct index.
	 * @exception java.io.IOException Throws an exception in the
	 *			case of an IO error.
	 */
	public void addEntry(
		final String docno,
		final int docLength,
		final long directIndexOffsetBytes,
		final byte directIndexOffsetBits)
		throws java.io.IOException
	{
		//writes the docid, length and the docno
		dos.writeInt(numberOfDocumentIndexEntries);
		dos.writeInt(docLength);
	
		try{
			dos.writeBytes(docno);
			dos.write(
				zeroBuffer,
				0,
				DOCNO_BYTE_LENGTH - docno.length()
				);
		} catch (ArrayIndexOutOfBoundsException auioobe) {
			logger.error("Problem writing document "+ docno + " to the DocumentIndex. Your docno is too long. Max length="
				+DOCNO_BYTE_LENGTH + ", alter docno.byte.length property to change");
		}
		dos.writeLong(directIndexOffsetBytes);
		dos.writeByte(directIndexOffsetBits);
		numberOfDocumentIndexEntries++;
	}

	/** As above, but for use when the docid is already known. Do not MIX this and other addEntry() calls */
	public void addEntry(
        final String docno,
		final int docid,
        final int docLength,
        final long directIndexOffsetBytes,
        final byte directIndexOffsetBits)
        throws java.io.IOException
    {
        //writes the docid, length and the docno
        dos.writeInt(docid);
        dos.writeInt(docLength);
        dos.writeBytes(docno);
		try{
	        dos.write(
    	        zeroBuffer,
            	0,
        	    DOCNO_BYTE_LENGTH - docno.length()
	            );
		} catch (ArrayIndexOutOfBoundsException auioobe) {
			logger.error("Problem writing document "+ docno + " to the DocumentIndex. Your docno is too long. Max length="
				+DOCNO_BYTE_LENGTH + ", alter docno.byte.length property to change");
		}
        dos.writeLong(directIndexOffsetBytes);
        dos.writeByte(directIndexOffsetBits);
		numberOfDocumentIndexEntries++;
    }
	

	/**
	 * Closes the random access file.
	 */
	public void close() {
		try {
			dos.close();
		} catch (IOException ioe) {
			logger.warn("Input/Output exception while closing docIndex file.",ioe);	
		}
	}
}

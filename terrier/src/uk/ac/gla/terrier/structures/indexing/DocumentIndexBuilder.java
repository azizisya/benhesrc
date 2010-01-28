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
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk> (original author)
 */
package uk.ac.gla.terrier.structures.indexing;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.structures.FilePosition;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.ApplicationSetup;
/**
 * A builder for the document index. 
 * @author Vassilis Plachouras
 * @version $Revision: 1.1 $
 */
public class DocumentIndexBuilder {
	/** The logger used */
	protected static final Logger logger = Logger.getRootLogger();
	
	/** The stream to which we write the data. */
	protected DataOutputStream dos;
	/** The total number of entries in the document index.*/
	protected int numberOfDocumentIndexEntries;
	/** index object of the index currently being created */
	protected Index index;
	
	/** A default constructor for the class. Uses the default index path and prefix
	 * @deprecated */
	public DocumentIndexBuilder() {
		this(ApplicationSetup.TERRIER_INDEX_PATH, ApplicationSetup.TERRIER_INDEX_PREFIX);
	}

	/** Construct a DocumentIndex associated with the specified index
	  * @param i Index being constructed
	  */	
	public DocumentIndexBuilder(Index i)
	{
		this(i.getPath(), i.getPrefix());
		this.index = i;		
	}
	
	/** Constructor using index path and prefix.
	  * @param path path to the index
	  * @param prefix filename prefix of the index
	  */
	public DocumentIndexBuilder(String path, String prefix)
	{
		this(path + ApplicationSetup.FILE_SEPARATOR + prefix + ApplicationSetup.DOC_INDEX_SUFFIX);
	}

	/** 
	 * A constructor of a document index from a given filename.
	 * @param filename String the filename of the document index, 
	 *		with an extension
	 */
	public DocumentIndexBuilder(String filename) {
		try {
			numberOfDocumentIndexEntries = 0;
			dos = new DataOutputStream(Files.writeFileStream(filename));
		} catch (IOException fnfe) {
			logger.error("Input/Output exception during opening the document index file. Stack trace follows.", fnfe);
		}
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
	 * @exception ArrayOutOfBoundsException The docno being written is too long
	 */
	public void addEntryToBuffer(
		String docno,
		int docLength,
		FilePosition directIndexOffset)
		throws java.io.IOException {
	
		//writes the docid, length and the docno
		dos.writeInt(numberOfDocumentIndexEntries);
		dos.writeInt(docLength);
		dos.writeBytes(docno);
		 /* if an exception occurs here, then the docnos are longer than
		  * ApplicationSetup.DOCNO_BYTE_LENGTH. Increase the property docno.byte.length */
		try{
			dos.write(
				zeroBuffer,
				0,
				ApplicationSetup.DOCNO_BYTE_LENGTH - docno.length());
		} catch (ArrayIndexOutOfBoundsException auioobe) {
			logger.error("Problem writing document "+ docno + " to the DocumentIndex. Your docno is too long. Max length="
				+ApplicationSetup.DOCNO_BYTE_LENGTH + ", alter docno.byte.length property to change");
		}
		dos.writeLong(directIndexOffset.Bytes);
		dos.writeByte(directIndexOffset.Bits);
		numberOfDocumentIndexEntries++;
	}
	
	
	/**
	 * Adds to the index a new entry, giving to it the next 
	 * available document id. The entry is writen first
	 * to the buffer, which afterwards has to be flushed to 
	 * the file on disk.
	 * @param docno String the document number.
	 * @param docLength int the number of indexed tokens in the document.
	 * @exception java.io.IOException Throws an exception in the 
	 *            case of an IO error.
	 */	
	public void addEntryToBuffer(String docno, int docLength) throws java.io.IOException {
		dos.writeInt(numberOfDocumentIndexEntries);
		dos.writeInt(docLength);
		dos.writeBytes(docno);
		try{
			dos.write(
				zeroBuffer,
				0,
				ApplicationSetup.DOCNO_BYTE_LENGTH - docno.length());
			dos.writeLong(0);
			dos.writeByte(0);
		} catch (ArrayIndexOutOfBoundsException auioobe) {
			logger.error("Problem writing document "+ docno + " to the DocumentIndex. Your docno is too long. Max length="
				+ApplicationSetup.DOCNO_BYTE_LENGTH + ", alter docno.byte.length property to change");
		}
		numberOfDocumentIndexEntries++;	
	}
	/**
	 * Closes the random access file.
	 */
	public void close() {
		try {
			dos.close();
		} catch (IOException ioe) {
			logger.error("Input/Output exception while closing docIndex file. Stack trace follows", ioe);
		}
	}
	/**
	 * Closes the underlying file after finished processing the collections.
	 */
	public void finishedCollections() {
		final int maxDocsEncodedDocid = Integer.parseInt(ApplicationSetup.getProperty("indexing.max.encoded.documentindex.docs","5000000"));
		if (index != null)
		{
			index.setIndexProperty("num.Documents", ""+numberOfDocumentIndexEntries);
			index.setIndexProperty("docno.byte.length", ""+ ApplicationSetup.DOCNO_BYTE_LENGTH);
			index.addIndexStructure("document", 
				numberOfDocumentIndexEntries > maxDocsEncodedDocid 
					? "uk.ac.gla.terrier.structures.DocumentIndex"
					: "uk.ac.gla.terrier.structures.DocumentIndexEncoded" );
			index.addIndexStructureInputStream("document", "uk.ac.gla.terrier.structures.DocumentIndexInputStream");
			if (numberOfDocumentIndexEntries > maxDocsEncodedDocid)
			{
				logger.warn("Index is very large, so memory efficient DocumentIndex will be used for retrieval. To ensure fast retrieval speed, set index.document.class to uk.ac.gla.terrier.structures.DocumentIndexEncoded, should enough memory be available");
			}
		}
		close();
	}
	
	/**
	 * A static buffer for writing zero values to the files.
	 */
	protected byte[] zeroBuffer = new byte[ApplicationSetup.DOCNO_BYTE_LENGTH];
}

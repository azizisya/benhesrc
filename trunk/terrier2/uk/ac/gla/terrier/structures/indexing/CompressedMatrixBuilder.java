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
 * The Original Code is CompressedMatrixBuilder.java.
 *
 * The Original Code is Copyright (C) 2004-2007 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *  
 */
package uk.ac.gla.terrier.structures.indexing;
import java.io.IOException;
import org.apache.log4j.Logger;
import uk.ac.gla.terrier.compression.BitOutputStream;
import uk.ac.gla.terrier.structures.FilePosition;
import uk.ac.gla.terrier.utility.ApplicationSetup;
/**
 * Builds a direct index, using field information optionally.
 * @author Vassilis Plachouras &amp; Craig Macdonald
 * @version $Revision: 1.1 $
 */
public class CompressedMatrixBuilder {
	/** The logger used */
	private static Logger logger = Logger.getRootLogger();
	/** The gamma compressed file containing the terms. */
	protected BitOutputStream file;

	/** The number of documents to be indexed before flushing the data to disk.*/
	protected static int DocumentsPerFlush = ApplicationSetup.BUNDLE_SIZE;

	/** The number of different fields that are used for indexing field information.*/
	protected static final int fieldTags = 1;

	/** Indicates whether field information is used. */
	protected static final boolean saveTagInformation = true;

	/** The number of documents indexed since the last flush to disk.*/
	protected int DocumentsSinceFlush = 0;

	/** Constructs an instance of the direct index
	  * using the given index path/prefix 
	  */
	public CompressedMatrixBuilder(String path, String prefix)
	{
		this(path + ApplicationSetup.FILE_SEPARATOR + prefix + ".mx");
	}

	/**
	 * Constructs an instance of the direct index
	 * in the default index location for the direct file.
	 */
	public CompressedMatrixBuilder() {
		this(ApplicationSetup.TERRIER_INDEX_PATH, ApplicationSetup.TERRIER_INDEX_PREFIX);
	}
	/**
	 * Constructs an instance of the direct index
	 * with a non-default name for the underlying direct file.
	 * @param filename the non-default filename used
	 *	for the underlying direct file.
	 */
	public CompressedMatrixBuilder(String filename) {
		try{	
			file = new BitOutputStream(filename);
		} catch (IOException ioe) {
			logger.error(ioe);
		}
		//resetBuffer();
	}

	
	/**
	 * terms: 
	 */
	public FilePosition addDocument(int[][] terms) throws IOException
	{
		addFieldDocument(terms);
		
		/* find out where we are */
		FilePosition rtr = getLastEndOffset();
		
		/* flush to disk if necessary */
		if (DocumentsSinceFlush++ >= DocumentsPerFlush)
		{
			flushBuffer();
			resetBuffer();
			DocumentsSinceFlush = 0;
		}
		/* and then return where the position of the last 
		 * write to the DirectIndex */
		return rtr;
	}

	protected void addFieldDocument(int[][] terms) throws IOException{
		final int TermsCount = terms[0].length;
		final int[] termCodes = terms[0];
		final int[] termFreqs = terms[1];
		final int[] termFields = terms[2];
		
		file.writeGamma(termCodes[0] + 1);
		file.writeUnary(termFreqs[0]);
		file.writeBinary(fieldTags, termFields[0]);
		int prevTermCode = termCodes[0];
		if (TermsCount > 1) {
			for (int termNo = 1; termNo < TermsCount; termNo++) {
				file.writeGamma(termCodes[termNo] - prevTermCode);
				file.writeUnary(termFreqs[termNo]);
				file.writeBinary(fieldTags, termFields[termNo]);
				prevTermCode = termCodes[termNo];
			}
		}
	}

	/**
	 * When the indexing has reached the end of all collections,
	 * this method writes the buffers on disk and closes the 
	 * corresponding files.
	 */
	public void finishedCollections()
	{
		flushBuffer();
		resetBuffer();
		DocumentsSinceFlush = 0;
		logger.info("flush direct index");
		try{
			close();
		} catch (IOException ioe) { logger.warn(ioe);} 
	}
	/** 
	 * Flushes the data to disk.
	 */
	public void flushBuffer() {
		file.flush();
	}
	/** 
	 * Returns the current offset in the direct index.
	 * @return FilePosition the offset in the direct index.
	 */
	public FilePosition getLastEndOffset()
	{
		/* where the current position of the DirectIndex, minus 1 bit */
		long endByte = file.getByteOffset();
		byte endBit = file.getBitOffset();
		endBit--;
	
		if (endBit < 0 && endByte > 0) {
			endBit = 7;
			endByte--;
		}
	
		return new FilePosition(endByte, endBit);
	}
	/**
	 * Resets the internal buffer for writing data. This method should
	 * be called before adding any documents to the direct index.
	 */
	public void resetBuffer() {
		//file.writeReset();
	}
	/**
	 * Closes the underlying gamma compressed file.
	 */
	public void close() throws IOException{
		file.close();
	}
}

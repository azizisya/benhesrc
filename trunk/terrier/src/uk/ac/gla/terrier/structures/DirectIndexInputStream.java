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
 * The Original Code is DirectIndexInputStream.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Douglas Johnson <johnsoda{a.}dcs.gla.ac.uk> (original author)
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk> 
 */
package uk.ac.gla.terrier.structures;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import uk.ac.gla.terrier.compression.BitIn;
import uk.ac.gla.terrier.compression.BitInputStream;
import uk.ac.gla.terrier.compression.OldBitInputStream;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.FieldScore;
/**
 * This class reads the direct index structure, sequentially,
 * as an input stream.
 * @author Douglas Johnson, Vassilis Plachouras, Craig Macdonald
 * @version $Revision: 1.1 $
 * @see uk.ac.gla.terrier.structures.DirectIndex
 */
public class DirectIndexInputStream implements Closeable,LegacyBitFileStructure {
	/** The logger used */
	private static Logger logger = Logger.getRootLogger();
	/**
	 * A document index stream.
	 */
	protected DocumentIndexInputStream documentIndexStream;
	/**
	 * The number of zero-length documents that were skipped during a call of the getNextTerms()
	 */
	protected int documentsSkipped = 0;
	/**
	 * The gamma compressed file containing the terms.
	 */
	protected BitIn gammaInputStream; 

	/** filename of underlying file */
	protected String filename;

	public DirectIndexInputStream() {
		this(ApplicationSetup.TERRIER_INDEX_PATH,
			ApplicationSetup.TERRIER_INDEX_PREFIX);
	}

	public DirectIndexInputStream(String path, String prefix, DocumentIndexInputStream doiis)
	{
		try {
            openDirectIndex(
                path + ApplicationSetup.FILE_SEPARATOR +
                    prefix + ApplicationSetup.DF_SUFFIX);
			documentIndexStream = doiis;		
		} catch (IOException ioe) {
            logger.error(
                "I/O Exception occured while opening the direct file for stream reading. Stack trace follows.",ioe);
        }
	}

	public DirectIndexInputStream(String path, String prefix) {
		try {
			openDirectIndex(
				path + ApplicationSetup.FILE_SEPARATOR + 
					prefix + ApplicationSetup.DF_SUFFIX);
			openDocumentIndex(	
				path + ApplicationSetup.FILE_SEPARATOR + 
					prefix + ApplicationSetup.DOC_INDEX_SUFFIX);
		} catch (IOException ioe) {
			 logger.error(
				"I/O Exception occured while opening the direct file for stream reading. Stack trace follows.",ioe);
		}
			
	}

	/** forces the data structure to reopen the underlying bitfile
	 *  using the legacy implementation of BitFile (OldBitFile)
	 *  @throws IOException
 	**/
    public void reOpenLegacyBitFile() throws IOException
    {
        gammaInputStream.close();
        gammaInputStream = new OldBitInputStream(filename);
    }
	
	protected void openDirectIndex(String filename) throws IOException
	{
		gammaInputStream =
			new BitInputStream(
					this.filename = filename
				);
	}

	protected void openDocumentIndex(String filename) throws IOException
	{
		documentIndexStream =
			new DocumentIndexInputStream(filename);
	}

	/**
	 * One call returns all the data for one document - [0][] is term ids, [1][] is frequency, [2][] is fields.
	 * The size of [0,1,2][] is how many unique terms occur in each document.
	 * Between calls, use getDocumentsSkipped() to keep track of what docid you're currently processing.
	 * @return int[][] the two dimensional array containing the term ids, fields 
	 * @throws java.io.IOException if an I/O error occurs
	 */
	public int[][] getNextTerms() throws IOException {
		documentsSkipped = 0;
		int n = documentIndexStream.readNextEntry();
		while (n != -1 && documentIndexStream.getDocumentLength() == 0) { 
			n = documentIndexStream.readNextEntry();
			documentsSkipped++;
		}
		if (n == -1) { //if the end of file has been reached then return null
			return null;
		}
		//long endByteOffset = documentIndexStream.getEndOffset();
		//byte endBitOffset = documentIndexStream.getEndBitOffset();
		return getNextTerms(documentIndexStream.getEndOffset(), documentIndexStream.getEndBitOffset());
	}

	public int[][] getNextTerms(long endByteOffset, byte endBitOffset) throws IOException
	{
		final int htmlTags = FieldScore.FIELDS_COUNT;
		final boolean loadTagInformation = FieldScore.USE_FIELD_INFORMATION;
		ArrayList<int[]> temporaryTerms = new ArrayList<int[]>();
		int[][] documentTerms = null;
		//boolean hasMore = false;
		if (loadTagInformation) { //if there is tag information to process		
			while ((endByteOffset > gammaInputStream.getByteOffset())
					|| (endByteOffset == gammaInputStream.getByteOffset()
						&& endBitOffset > gammaInputStream.getBitOffset())) {
				int[] tmp = new int[3];
				tmp[0] = gammaInputStream.readGamma();
				tmp[1] = gammaInputStream.readUnary();
				tmp[2] = gammaInputStream.readBinary(htmlTags);
				temporaryTerms.add(tmp);
			}
			documentTerms = new int[3][temporaryTerms.size()];
			int[] documentTerms0 = documentTerms[0];
			int[] documentTerms1 = documentTerms[1];
			int[] documentTerms2 = documentTerms[2];
			int[] tmpMatrix = (int[]) temporaryTerms.get(0);
			documentTerms0[0] = tmpMatrix[0] - 1;
			documentTerms1[0] = tmpMatrix[1];
			documentTerms2[0] = tmpMatrix[2];
			if (documentTerms[0].length > 1) {
				final int documentTerms0Length = documentTerms[0].length;
				for (int i = 1; i < documentTerms0Length; i++) {
					tmpMatrix = (int[]) temporaryTerms.get(i);
					documentTerms0[i] = tmpMatrix[0] + documentTerms[0][i - 1];
					documentTerms1[i] = tmpMatrix[1];
					documentTerms2[i] = tmpMatrix[2];
				}
			}		
		} else {
			while ((endByteOffset > gammaInputStream.getByteOffset())
				|| (endByteOffset == gammaInputStream.getByteOffset()
					&& endBitOffset > gammaInputStream.getBitOffset())) {
				int[] tmp = new int[2];
				tmp[0] = gammaInputStream.readGamma();
				tmp[1] = gammaInputStream.readUnary();
				temporaryTerms.add(tmp);
			}
			documentTerms = new int[2][temporaryTerms.size()];
			int[] documentTerms0 = documentTerms[0];
			int[] documentTerms1 = documentTerms[1];
			documentTerms0[0] = ((int[]) temporaryTerms.get(0))[0] - 1;
			documentTerms1[0] = ((int[]) temporaryTerms.get(0))[1];
			if (documentTerms[0].length > 1) {
				for (int i = 1; i < documentTerms[0].length; i++) {
					int[] tmpMatrix = (int[]) temporaryTerms.get(i);
					documentTerms0[i] = tmpMatrix[0] + documentTerms[0][i - 1];
					documentTerms1[i] = tmpMatrix[1];
				}
			}			
		}
		return documentTerms;
	}
	
	/**
	 * Prints out the html direct index file.
	 */
	public void print() {
		int[][] terms = null;
		try {
			int counter = 0;
			while ((terms = getNextTerms()) != null) {
				final int termColumns = terms.length;
				for (int j = 0; j < terms[0].length; j++) {
					System.out.print("(");
					for (int k = 0; k < termColumns-1; k++) {
						System.out.print(terms[k][j] + ", ");
					}
					System.out.print(terms[termColumns-1][j] + ") ");
				}
				counter++;
				System.out.println();
			}
		} catch (IOException ioe) {
			logger.fatal(
				"IOException occured while reading the direct file.",ioe);
		}
	}
	
	/**
	 * Closes the underlying gamma compressed file.
	 */
	public void close() {
		try {
			gammaInputStream.close();
			documentIndexStream.close();
		} catch(IOException ioe) {
			logger.fatal("Input/Output exception while closing the direct index input stream.",ioe);
		}
	}
	/**
	 * Returns the value of the documents that were skipped during
	 * a call to the method getNextTerms()
	 * @return int the number of documents skipped.
	 */
	public int getDocumentsSkipped() {
		return documentsSkipped;
	}
}

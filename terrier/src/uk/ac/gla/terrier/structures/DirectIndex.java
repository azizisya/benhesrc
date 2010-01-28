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
 * The Original Code is DirectIndex.java.
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
 * A class that implements the direct index and saves 
 * information about whether a term appears in 
 * one of the specified fields.
 * @author Douglas Johnson, Vassilis Plachouras, Craig Macdonald
 * @version $Revision: 1.1 $
 */
public class DirectIndex implements Closeable,LegacyBitFileStructure{
	/** The logger used */
	private static Logger logger = Logger.getRootLogger();
	/** Indicates whether field information is indexed. */
	protected static final boolean saveTagInformation = 
		FieldScore.USE_FIELD_INFORMATION;
	
	/** The gamma compressed file containing the terms.*/
	protected BitInSeekable file;
	/** Filename of the open file */
	protected String filename;
	/** The document index employed for retrieving the document offsets.*/
	protected DocumentIndex docIndex;

	/** A constructor for child classes that doesnt open the file */
    protected DirectIndex(long a, long b, long c) { }
	
	/** A default constructor, only for use by child classes */
	protected DirectIndex() { }
	
	/**
	 * Constructs an instance of the direct index 
	 * with the given document index, and a default 
	 * name for the underlying direct file.
	 * @param docIndex The document index to be used.
	 */
	public DirectIndex(DocumentIndex docIndex) {
		this(docIndex, ApplicationSetup.TERRIER_INDEX_PATH, ApplicationSetup.TERRIER_INDEX_PREFIX);
		//file = new BitFile(ApplicationSetup.DIRECT_FILENAME, "r");
		//this.docIndex = docIndex;
	}

	public DirectIndex(DocumentIndex docIndex, String path, String prefix)
	{
		this(
			docIndex, 
			path + ApplicationSetup.FILE_SEPARATOR + prefix + ApplicationSetup.DF_SUFFIX
			);			
	}

	/**
	 * Constructs an instance of the direct index
	 * with the given document index and a non-default
	 * name for the underlying direct file.
	 * @param docIndex The document index to be used
	 * @param filename the non-default filename used 
	 * 		  for the underlying direct file.
	 */
	public DirectIndex(DocumentIndex docIndex, String filename) {
		file = new BitFile(this.filename = filename, "r");
		this.docIndex = docIndex;
	}
	
	/** forces the data structure to reopen the underlying bitfile
	 *  using the legacy implementation of BitFile (OldBitFile)
	 * @throws IOException
	 */
	public void reOpenLegacyBitFile() throws IOException
	{
		try{ file.close(); } catch (IOException ioe) {}
		file = new OldBitFile(filename, "r");
	}
	
	
	/**
	 * Returns a two dimensional array containing the 
	 * term ids and the term frequencies for 
	 * the given document. 
	 * @return int[][] the two dimensional [n][3] array 
	 * 		   containing the term ids, frequencies and field scores. If
	 *         the given document identifier is not found in the document
	 *         index, then the method returns null. If fields are not used, 
	 *         then the dimension of the returned array are [n][2].
	 * @param docid the document identifier of the document which terms 
	 * 		  we retrieve.
	 */
	public int[][] getTerms(int docid) {
		try {
			boolean found = docIndex.seek(docid);
			if (!found)
				return null;
			if (docIndex.getDocumentLength(docid) == 0)
				return null;
			final FilePosition startOffset = docIndex.getDirectIndexStartOffset();
			final FilePosition endOffset = docIndex.getDirectIndexEndOffset();
			final boolean loadTagInformation = FieldScore.USE_FIELD_INFORMATION;
			final int fieldTags = FieldScore.FIELDS_COUNT;
			ArrayList<int[]> temporaryTerms = new ArrayList<int[]>();
			int[][] documentTerms = null;
			final BitIn file = this.file.readReset(startOffset.Bytes, startOffset.Bits, endOffset.Bytes, endOffset.Bits);
			//boolean hasMore = false;
			if (loadTagInformation) { //if there is tag information to process
				while (((file.getByteOffset() + startOffset.Bytes) < endOffset.Bytes)
					|| (((file.getByteOffset() + startOffset.Bytes) == endOffset.Bytes)
						&& (file.getBitOffset() < endOffset.Bits))) {
					
					int[] tmp = new int[3];
					tmp[0] = file.readGamma();
					tmp[1] = file.readUnary();
					tmp[2] = file.readBinary(fieldTags);
					temporaryTerms.add(tmp);
				}
				documentTerms = new int[3][temporaryTerms.size()];
				int[] documentTerms0 = documentTerms[0];
				int[] documentTerms1 = documentTerms[1];
				int[] documentTerms2 = documentTerms[2];
				documentTerms0[0] = ((int[]) temporaryTerms.get(0))[0] - 1;
				documentTerms1[0] = ((int[]) temporaryTerms.get(0))[1];
				documentTerms2[0] = ((int[]) temporaryTerms.get(0))[2];
				if (documentTerms[0].length > 1) {
					final int documentTerms0Length = documentTerms[0].length;
					for (int i=1; i<documentTerms0Length; i++) {
						int[] tmpMatrix = (int[]) temporaryTerms.get(i);
						documentTerms0[i] = tmpMatrix[0] + documentTerms[0][i - 1];
						documentTerms1[i] = tmpMatrix[1];
						documentTerms2[i] = tmpMatrix[2];
					}
					
				}
			} else { //else if there is no tag information to process
				while (((file.getByteOffset() + startOffset.Bytes) < endOffset.Bytes)
						|| (((file.getByteOffset() + startOffset.Bytes) == endOffset.Bytes)
							&& (file.getBitOffset() < endOffset.Bits))) {
					int[] tmp = new int[2];
					tmp[0] = file.readGamma();
					tmp[1] = file.readUnary();
					temporaryTerms.add(tmp);
				}
				documentTerms = new int[2][temporaryTerms.size()];
				int[] documentTerms0 = documentTerms[0];
				int[] documentTerms1 = documentTerms[1];
				documentTerms0[0] = ((int[]) temporaryTerms.get(0))[0] - 1;
				documentTerms1[0] = ((int[]) temporaryTerms.get(0))[1];
				if (documentTerms0.length > 1) {
					final int documentTerms0Length = documentTerms0.length;
					for (int i=1; i<documentTerms0Length; i++) {
						int[] tmpMatrix = (int[]) temporaryTerms.get(i);
						documentTerms0[i] = tmpMatrix[0] + documentTerms[0][i - 1];
						documentTerms1[i] = tmpMatrix[1];
					}
					
				}
			}
			return documentTerms;
		} catch (IOException ioe) {
			logger.error(
				"Input/Output exception while fetching the term ids for " +
				"a given document.",ioe);
			
		}
		return null;
	}
	/**
	 * Prints out the direct index file.
	 */
	public void print() {
		for (int i = 0; i < docIndex.getNumberOfDocuments(); i++) {
			int[][] terms = getTerms(i);
			final int termColumns = terms.length;
			for (int j = 0; j < terms[0].length; j++) {
				System.out.print("(");
				for (int k = 0; k < termColumns-1; k++) {
					System.out.print(terms[k][j] + ", ");
				}
				System.out.print(terms[termColumns-1][j] + ") ");
			}
			System.out.println();
		}
	}
	
	/**
	 * Closes the underlying bitfile compressed file.
	 */
	public void close() {
		try{
			file.close();
		} catch (IOException ioe) {/* dont care */}
	}
}

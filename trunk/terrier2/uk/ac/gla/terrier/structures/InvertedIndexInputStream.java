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
 * The Original Code is InvertedIndexInputStream.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *  Craig Macdonald <craigm{a.}dcs.gla.ac.uk (original author)
 */

package uk.ac.gla.terrier.structures;

import java.io.IOException;

import uk.ac.gla.terrier.compression.BitIn;
import uk.ac.gla.terrier.compression.BitInputStream;
import uk.ac.gla.terrier.compression.OldBitInputStream;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.FieldScore;


/** Reads an InvertedIndex as a stream
  * @author Craig Macdonald
  * @since 2.0
  * @version $Revision: 1.1 $
  */
public class InvertedIndexInputStream implements Closeable,LegacyBitFileStructure
{
	/** the lexicon input stream providing the offsets */
	protected final LexiconInputStream lis;
	/** The gamma compressed file containing the terms. */
	protected BitIn file; 
	/** filename of the underlying bitfile */
	protected String filename = null;

	/** Indicates whether field information is used.*/
	final boolean useFieldInformation = FieldScore.USE_FIELD_INFORMATION;
	
	public InvertedIndexInputStream(String path, String prefix, LexiconInputStream lis) throws IOException
	{
		this(path + ApplicationSetup.FILE_SEPARATOR + prefix + ApplicationSetup.IFSUFFIX, lis);
	}
	
	public InvertedIndexInputStream(String filename, LexiconInputStream lis) throws IOException
	{
		file = new BitInputStream(this.filename = filename);
		this.lis = lis;
	}

	public InvertedIndexInputStream(BitIn invFile, LexiconInputStream lis) throws IOException
	{
		file = invFile;
		this.lis = lis;
	}

	/** forces the data structure to reopen the underlying bitfile
	 *  using the legacy implementation of BitFile (OldBitFile)
	 * @throws IOException
	 */
	public void reOpenLegacyBitFile() throws IOException
	{
		file.close();
		file = new OldBitInputStream(filename);
	}
	
	public int[][] getNextDocuments() throws IOException {
		int rtrLis = lis.readNextEntry();
		if (rtrLis < 0)
			return null;
		return getNextDocuments(lis.getNt(), lis.getEndOffset(), lis.getEndBitOffset());
	}
	
	protected int[][] getNextDocuments(int df, long endByteOffset, byte endBitOffset) throws IOException {
		int[][] documentTerms = null;
		final int fieldCount = FieldScore.FIELDS_COUNT;
		if (useFieldInformation) { //if there are tag information to process			
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
	}

	public void print() {
		int documents[][] = null;
		int i =0;
		try{
		while((documents = getNextDocuments()) != null)
		{
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
		} catch (IOException ioe) { 	
		}
	}
	
	public void close()
	{
		try{ file.close(); } catch (IOException ioe) {}
		lis.close();
	}
}

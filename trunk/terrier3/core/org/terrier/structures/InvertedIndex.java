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
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Douglas Johnson <johnsoda{a.}dcs.gla.ac.uk> (original author)
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk> 
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
 */
package org.terrier.structures;
import java.io.IOException;

import org.apache.log4j.Logger;

import org.terrier.compression.BitIn;
import org.terrier.compression.BitInSeekable;
import org.terrier.structures.postings.BasicIterablePosting;
import org.terrier.structures.postings.IterablePosting;
import org.terrier.utility.FieldScore;
import org.terrier.utility.io.WrappedIOException;
/**
 * This class implements the inverted index 
 * for performing retrieval, with field information
 * optionally.
 * @author Douglas Johnson, Vassilis Plachouras, Craig Macdonald
 * @version $Revision: 1.40 $
 */
public class InvertedIndex extends BitPostingIndex {
	/** The logger used for the Lexicon */
	protected static final Logger logger = Logger.getLogger(InvertedIndex.class);
	
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
	
	protected DocumentIndex doi;
	
	public BitInSeekable[] getBitFiles() {
		return super.file;
	}
	
	public InvertedIndex(Index index, String structureName) throws IOException
	{
		this(index, structureName, index.getDocumentIndex());
	}
	
	public InvertedIndex(Index index, String structureName, DocumentIndex _doi) throws IOException
	{
		super(index, structureName, BasicIterablePosting.class);
		doi = _doi;
	}
	
	public InvertedIndex(Index index, String structureName, DocumentIndex _doi, Class<? extends IterablePosting> postingClass) throws IOException
	{
		super(index, structureName, postingClass);
		doi = _doi;
	}

	
	public void print()
	{
		throw new UnsupportedOperationException("InvIndex.print() is missing. Use IndexUtil instead.");
	}
	
	@Override
	public IterablePosting getPostings(BitIndexPointer pointer) throws IOException {
		final BitIn file = this.file[pointer.getFileNumber()].readReset(pointer.getOffset(), pointer.getOffsetBits());
		IterablePosting rtr = null;
		try{
			if (fieldCount > 0)
				rtr = postingImplementation
					.getConstructor(BitIn.class, Integer.TYPE, DocumentIndex.class, Integer.TYPE)
					.newInstance(file, pointer.getNumberOfEntries(), doi, fieldCount);
			else
				rtr = postingImplementation
					.getConstructor(BitIn.class, Integer.TYPE, DocumentIndex.class)
					.newInstance(file, pointer.getNumberOfEntries(), doi);
		} catch (Exception e) {
			throw new WrappedIOException(e);
		}
		return rtr;
	}
	
	public int[][] getDocuments(LexiconEntry le) {
		return getDocuments((BitIndexPointer)le);
	}
	
	public int[][] getDocuments(BitIndexPointer pointer) {
		if (pointer==null)
			return null;
		final boolean loadTagInformation = FieldScore.USE_FIELD_INFORMATION;
		final int count = pointer.getNumberOfEntries();
		try{
			final BitIn file = this.file[pointer.getFileNumber()].readReset(pointer.getOffset(), pointer.getOffsetBits());
			int[][] documentTerms = null;
			if (loadTagInformation) { //if there are tag information to process			
				documentTerms = new int[3][count];
				documentTerms[0][0] = file.readGamma() - 1;
				documentTerms[1][0] = file.readUnary();
				documentTerms[2][0] = file.readBinary(fieldCount);
				for (int i = 1; i < count; i++) {					
					documentTerms[0][i]  = file.readGamma() + documentTerms[0][i - 1];
					documentTerms[1][i]  = file.readUnary();
					documentTerms[2][i]  = file.readBinary(fieldCount);
				}				
			} else { //no tag information to process					
				documentTerms = new int[2][count];
				//new		
				documentTerms[0][0] = file.readGamma() - 1;
				documentTerms[1][0] = file.readUnary();
				for(int i = 1; i < count; i++){							 
					documentTerms[0][i] = file.readGamma() + documentTerms[0][i - 1];
					documentTerms[1][i] = file.readUnary();
				}
			}
			file.close();
			return documentTerms;
		} catch (IOException ioe) {
			logger.error("Problem reading inverted index", ioe);
			return null;
		}
	}

}

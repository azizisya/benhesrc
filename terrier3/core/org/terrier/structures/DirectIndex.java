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
import java.util.ArrayList;

import org.terrier.compression.BitIn;
import org.terrier.structures.postings.BasicIterablePosting;
import org.terrier.structures.postings.IterablePosting;
import org.terrier.utility.FieldScore;
/**
 * A class that implements the direct index and saves 
 * information about whether a term appears in 
 * one of the specified fields.
 * @author Douglas Johnson, Vassilis Plachouras, Craig Macdonald
 * @version $Revision: 1.37 $
 */
public class DirectIndex extends BitPostingIndex {
	
	/** Indicates whether field information is indexed. */
	//protected static final boolean saveTagInformation = 
	//	FieldScore.USE_FIELD_INFORMATION;
	
	
	/** The document index employed for retrieving the document offsets.*/
	protected DocumentIndex docIndex;
	
	/**
	 * Constructs an instance of the class with 
	 * the given index, using the specified structure name.
	 * @param index The index to be used
	 * @param structureName the name of this direct index
	 * @throws IOException 
	 */
	public DirectIndex(Index index, String structureName) throws IOException {
		super(index, structureName, BasicIterablePosting.class);
		docIndex = index.getDocumentIndex();
	}

	public DirectIndex(Index index, String structureName,
			Class<? extends IterablePosting> postingClass) throws IOException 
	{
		super(index, structureName, postingClass);
		docIndex = index.getDocumentIndex();
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
	public int[][] getTerms(int docid) throws IOException
	{
		DocumentIndexEntry de = docIndex.getDocumentEntry(docid);
		if (de == null)
			return null;
		if (de.getNumberOfEntries() == 0)
			return null;
		return getTerms(de);
	}
			
	public int[][] getTerms(BitIndexPointer de) throws IOException
	{
			//final FilePosition startOffset = docIndex.getDirectIndexStartOffset();
			//final FilePosition endOffset = docIndex.getDirectIndexEndOffset();
			final boolean loadTagInformation = FieldScore.USE_FIELD_INFORMATION;
			final int fieldTags = FieldScore.FIELDS_COUNT;
			ArrayList<int[]> temporaryTerms = new ArrayList<int[]>();
			int[][] documentTerms = null;
			final BitIn file = this.file[de.getFileNumber()].readReset(de.getOffset(), de.getOffsetBits());
			//boolean hasMore = false;
			if (loadTagInformation) { //if there is tag information to process
				for(int i=0; i<de.getNumberOfEntries(); i++ )
				{
				//while (((file.getByteOffset() + startOffset.Bytes) < endOffset.Bytes)
				//	|| (((file.getByteOffset() + startOffset.Bytes) == endOffset.Bytes)
				//		&& (file.getBitOffset() < endOffset.Bits))) {
					
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
				//while (((file.getByteOffset() + startOffset.Bytes) < endOffset.Bytes)
				//		|| (((file.getByteOffset() + startOffset.Bytes) == endOffset.Bytes)
				//			&& (file.getBitOffset() < endOffset.Bits))) {
				for(int i=0; i<de.getNumberOfEntries(); i++ )
				{
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
		/*} catch (IOException ioe) {
			logger.error(
				"Input/Output exception while fetching the term ids for " +
				"a given document.",ioe);
			
		}
		return null;*/
	}
	
	public static void main (String args[]) throws Exception
	{
		Index index = Index.createIndex();
		if (index == null)
		{
			System.err.println("Couldn't load index: " + Index.lastLoadError);
			return;
		}
		DirectIndex direct = index.getDirectIndex();
		DocumentIndex doc = index.getDocumentIndex();
		DocumentIndexEntry die = doc.getDocumentEntry(Integer.parseInt(args[0]));
		System.err.println("docid" + args[0] + " pointer = "+ die.toString());
		IterablePosting pi = direct.getPostings(die);
		System.out.print(args[0] + " ");
		while(pi.next() != IterablePosting.EOL)
		{
			System.out.print("(" + pi.getId() + ", " + pi.getFrequency() + ") ");
		}
		System.out.println();
	}

}

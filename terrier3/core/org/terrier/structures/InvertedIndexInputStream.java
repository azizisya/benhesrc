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
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *  Craig Macdonald <craigm{a.}dcs.gla.ac.uk (original author)
 */

package org.terrier.structures;

import java.io.IOException;
import java.util.Iterator;

import org.terrier.structures.postings.BasicIterablePosting;
import org.terrier.structures.postings.IterablePosting;


/** Reads an InvertedIndex as a stream
  * @author Craig Macdonald
  * @since 2.0
  * @version $Revision: 1.3 $
  */
public class InvertedIndexInputStream extends BitPostingIndexInputStream
{
	public  InvertedIndexInputStream(Index _index, String structureName) throws IOException
	{
		this(_index, structureName, BasicIterablePosting.class);
	}
	
	@SuppressWarnings("unchecked")
	public InvertedIndexInputStream(Index _index, String structureName, Iterator<? extends Pointer> lexInputStream) throws IOException
	{
		super(_index, structureName, (Iterator<BitIndexPointer>)lexInputStream, BasicIterablePosting.class);
	}
	
	@SuppressWarnings("unchecked")
	public InvertedIndexInputStream(Index _index, String structureName, Iterator<? extends Pointer> lexInputStream, Class<? extends IterablePosting> _postingIteratorClass) throws IOException
	{
		super(_index, structureName, (Iterator<BitIndexPointer>)lexInputStream, _postingIteratorClass);
	}
	
	
	@SuppressWarnings("unchecked")
	public InvertedIndexInputStream(Index _index, String structureName, Class<? extends IterablePosting> _postingIteratorClass) throws IOException
	{
		super(_index, 
				structureName,
				(Iterator<BitIndexPointer>)_index.getIndexStructureInputStream("lexicon-entry"),
				_postingIteratorClass);
	}
	
	public int[][] getNextDocuments() throws IOException {
		BitIndexPointer pointer = _next();
		if (pointer== null)
			return null;
		return getNextDocuments(pointer);
	}
	
	protected int[][] getNextDocuments(BitIndexPointer pointer) throws IOException {
		//	System.err.println("pointer="+pointer.toString() + " actual=@{"+file.getByteOffset() + ","+ file.getBitOffset()+ "}");
		if (file.getByteOffset() != pointer.getOffset())
		{
			//System.err.println("skipping " + (pointer.getOffset() - file.getByteOffset()) + " bytes");
			file.skipBytes(pointer.getOffset() - file.getByteOffset());
		}
		if (file.getBitOffset() != pointer.getOffsetBits())
		{
			//System.err.println("skipping "+ (pointer.getOffsetBits() - file.getBitOffset()) + "bits");
			file.skipBits(pointer.getOffsetBits() - file.getBitOffset());
		}
		
		int[][] documentTerms = null;
		final int fieldCount = super.fieldCount;
		final int df = pointer.getNumberOfEntries();
		if (fieldCount > 0) { //if there are tag information to process			
			documentTerms = new int[2+fieldCount][df];
			documentTerms[0][0] = file.readGamma() - 1;
			documentTerms[1][0] = file.readUnary();
			for(int fi=0;fi < fieldCount;fi++)
				documentTerms[2+fi][0] = file.readUnary() -1;
			for (int i = 1; i < df; i++) {					
				documentTerms[0][i]  = file.readGamma() + documentTerms[0][i - 1];
				documentTerms[1][i]  = file.readUnary();
				for(int fi=0;fi < fieldCount;fi++)
					documentTerms[2+fi][i] = file.readUnary() -1;
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
	
}

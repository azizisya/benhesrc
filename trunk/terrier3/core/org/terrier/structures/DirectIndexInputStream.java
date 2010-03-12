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
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Douglas Johnson <johnsoda{a.}dcs.gla.ac.uk> (original author)
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk> 
 */
package org.terrier.structures;
import java.io.IOException;
import java.util.Iterator;

import org.terrier.structures.postings.BasicIterablePosting;
import org.terrier.structures.postings.IterablePosting;
/**
 * This class reads the direct index structure, sequentially,
 * as an input stream.
 * @author Douglas Johnson, Vassilis Plachouras, Craig Macdonald
 * @version $Revision: 1.29 $
 * @see org.terrier.structures.DirectIndex
 */
public class DirectIndexInputStream extends BitPostingIndexInputStream {
	
	protected DirectIndexInputStream(Index _index, String structureName, 
			Iterator<? extends BitIndexPointer> _pointerList,
			Class<? extends IterablePosting> _postingIteratorClass)
		throws IOException
	{
		super(_index, structureName, _pointerList, _postingIteratorClass);
	}
	
	
	@SuppressWarnings("unchecked")
	public DirectIndexInputStream(Index index, String structureName, Class<? extends IterablePosting> postingIterator) throws IOException
	{
		super(index, structureName, (Iterator<DocumentIndexEntry>)index.getIndexStructureInputStream("document"), postingIterator);
	}
	
	@SuppressWarnings("unchecked")
	public DirectIndexInputStream(Index index, String structureName) throws IOException
	{
		super(index, structureName, (Iterator<DocumentIndexEntry>)index.getIndexStructureInputStream("document"), BasicIterablePosting.class);
	}
	
	/**
	 * One call returns all the data for one document - [0][] is term ids, [1][] is frequency, [2][] is fields.
	 * The size of [0,1,2][] is how many unique terms occur in each document.
	 * Between calls, use getDocumentsSkipped() to keep track of what docid you're currently processing.
	 * @return int[][] the two dimensional array containing the term ids, fields 
	 * @throws java.io.IOException if an I/O error occurs
	 */
	public int[][] getNextTerms() throws IOException {
		entriesSkipped = 0;
		
		if (! pointerList.hasNext())
			return null;
		BitIndexPointer de = (BitIndexPointer)pointerList.next();
		while (de != null && de.getNumberOfEntries() == 0 && pointerList.hasNext())
		{ 
			de = (BitIndexPointer)pointerList.next();
			entriesSkipped++;
		}
		if (de == null) { //if the end of file has been reached then return null
			return null;
		}
		if (de.getNumberOfEntries() == 0)
			return null;
		return getNextTerms(de);
	}

	public int[][] getNextTerms(BitIndexPointer pointer) throws IOException
	{
		final boolean loadTagInformation = fieldCount > 0;
		final int df = pointer.getNumberOfEntries();
		int[][] documentTerms = new int[2+ fieldCount][df];
		
		if (loadTagInformation) { //if there is tag information to process		
			for(int i=0;i<df;i++)
			{
				documentTerms[0][i] = file.readGamma();
				documentTerms[1][i] = file.readUnary();
				for(int fi = 0; fi < fieldCount; fi++)
					documentTerms[fi+2][i] = file.readUnary()-1;
				//System.err.println("id+="+ documentTerms[0][i] + " tf=" + documentTerms[1][i]);
			}
		} else {
			for(int i=0;i<df;i++)
			{
				documentTerms[0][i] = file.readGamma();
				documentTerms[1][i] = file.readUnary();
				//System.err.println("id+="+ documentTerms[0][i] + " tf=" + documentTerms[1][i]);	
			}
		}
		final int[] documentTerms0 = documentTerms[0];
		documentTerms0[0]--;
		for(int i=1;i<df;i++)
		{
			documentTerms0[i] += documentTerms0[i-1];
		}
		return documentTerms;
	}
}

/*
 * Terrier - Terabyte Retriever 
 * Webpage: http://terrier.org/
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
 * The Original Code is BasicIterablePosting.java
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original contributor)
 */

package org.terrier.structures.postings;

import java.io.IOException;


import org.terrier.compression.BitIn;
import org.terrier.structures.DocumentIndex;
import org.terrier.utility.ApplicationSetup;

/** Basic inverted and direct index format: [gamma(first docid +1) unary (frequency)], [gamma(delta docid) unary(frequency)]
 * @since 3.0 
 */
public class BasicIterablePosting extends BasicPostingImpl implements IterablePosting
{
	protected int numEntries;
	protected BitIn bitFileReader;
	protected DocumentIndex doi;
	
	public final int CACHE_SIZE = Integer.parseInt(ApplicationSetup.getProperty("posting.cache.size", "100000"));
	
	protected int[] idCache;
	
	protected int[] tfCache;
	
	protected int cacheIndex = 0;
	
	protected int exactCacheSize = 0;
	
	/** Create a new posting iterator */
	protected BasicIterablePosting(){
		idCache = new int[CACHE_SIZE];
		tfCache = new int[CACHE_SIZE];
	}
	
	/** Create a new posting iterator
	 * @param _bitFileReader BitIn to read the postings from
	 * @param _numEntries number of postings in the list
	 * @param _doi document index to use to satisfy getDocumentLength()
	 * @throws IOException thrown in an IO exception occurs
	 */
	public BasicIterablePosting(BitIn _bitFileReader, int _numEntries, DocumentIndex _doi) throws IOException {
		bitFileReader = _bitFileReader;
		numEntries = _numEntries;
		doi = _doi;
		idCache = new int[CACHE_SIZE];
		tfCache = new int[CACHE_SIZE];
	}
	
	protected void loadToCache() throws IOException{
		this.cacheIndex = 0;
		int localNumEntries = numEntries+1;
		this.exactCacheSize = 0;
		for (int i=0; i<CACHE_SIZE; i++){
			if (localNumEntries-- <= 0)
				break;
			idCache[i] = bitFileReader.readGamma() + id;
			tfCache[i] = bitFileReader.readUnary();
			id = idCache[i];
			this.exactCacheSize++;
		}
	}

	/** {@inheritDoc} */
	public int next() throws IOException {
		if (numEntries-- <= 0)
			return EOL;
		
		if (exactCacheSize == 0 || cacheIndex == exactCacheSize)
			loadToCache();
		
		id = idCache[cacheIndex];
		tf = tfCache[cacheIndex];
		cacheIndex++;
		/**
		id = bitFileReader.readGamma() + id;
		tf = bitFileReader.readUnary();
		*/
		return id;
	}
	
	/** {@inheritDoc}
	 * This implementation of next(int) which uses next() */
	public int next(int target) throws IOException {
		do
		{
			this.next();
		} while(this.getId() < target);
		return this.getId();
	}
	
	/** {@inheritDoc} */
	public boolean endOfPostings() {
		return numEntries <= 0;
	}

	/** {@inheritDoc} */
	public int getDocumentLength()
	{
		try{
			return doi.getDocumentLength(id);
		} catch (Exception e) {
			//TODO log?
			System.err.println("Problem looking for doclength for document "+ id);
			e.printStackTrace();
			return -1;
		}
	}

	/** {@inheritDoc} */
	public void close() throws IOException {
		//does not close the underlying file, just the read buffer
		bitFileReader.close();
	}
	
	/** {@inheritDoc} */
	public WritablePosting asWritablePosting() {
		BasicPostingImpl bp = new BasicPostingImpl();
		bp.id = id;
		bp.tf = tf;
		return bp;
	}

	
}

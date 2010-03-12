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
 * The Original Code is FSADocumentIndex.java
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original contributor)
 */
package org.terrier.structures;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Iterator;

import org.apache.log4j.Logger;

import org.terrier.structures.collections.FSArrayFile;
import org.terrier.structures.seralization.FixedSizeWriteableFactory;

public class FSADocumentIndex extends FSArrayFile<DocumentIndexEntry> implements DocumentIndex {
	protected static Logger logger = Logger.getLogger(FSADocumentIndex.class);
	
	protected int lastDocid = -1;
	protected DocumentIndexEntry lastEntry = null;
	protected int[] docLengths;
		
	public FSADocumentIndex(Index index, String structureName) throws IOException
	{
		this(index, structureName, true);		
	}
	
	@SuppressWarnings("unchecked")
	protected FSADocumentIndex(Index index, String structureName, boolean initialise) throws IOException
	{
		super(
				index.getPath() + "/" + index.getPrefix() + "."+ structureName + FSArrayFile.USUAL_EXTENSION,
				false,
				(FixedSizeWriteableFactory<DocumentIndexEntry>) index.getIndexStructure(structureName+"-factory")
				);
		if (initialise)
			initialise(index, structureName);
	}
	
	protected void initialise(Index index, String structureName) throws IOException
	{
		logger.info("Loading document lengths for " + structureName + " structure into memory");
		docLengths = new int[this.size()];
		int i=0;
		Iterator<DocumentIndexEntry> iter = new FSADocumentIndexIterator(index, structureName);
		while(iter.hasNext())
		{
			docLengths[i++] = iter.next().getDocumentLength();
		}
		IndexUtil.close(iter);
	}

	public final int getDocumentLength(int docid) throws IOException
	{
		return docLengths[docid];
	}

	public final DocumentIndexEntry getDocumentEntry(int docid) throws IOException 
	{
		if (docid == lastDocid)
		{
			return lastEntry;
		}
		try{
			lastEntry = null;
			return lastEntry = get(lastDocid = docid);
		} catch (NoSuchElementException nsee) {
			return null;
		}
	}
	
	public static class FSADocumentIndexIterator extends FSArrayFile.ArrayFileIterator<DocumentIndexEntry> implements Iterator<DocumentIndexEntry>
	{
		@SuppressWarnings("unchecked")
		public FSADocumentIndexIterator(Index index, String structureName) throws IOException
		{
			super(
					index.getPath() + "/" + index.getPrefix() + "."+ structureName + FSArrayFile.USUAL_EXTENSION, 
					(FixedSizeWriteableFactory<DocumentIndexEntry>) index.getIndexStructure("document-factory")
					);
		}		
	}

	public int getNumberOfDocuments() {
		return super.size();
	}

}

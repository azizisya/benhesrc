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
 * The Original Code is BitPostingIndexInputStream.java
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original contributor)
 */
package org.terrier.structures;

import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;

import org.terrier.compression.BitIn;
import org.terrier.compression.BitInputStream;
import org.terrier.structures.postings.IterablePosting;
import org.terrier.utility.io.WrappedIOException;

public class BitPostingIndexInputStream implements PostingIndexInputStream, Skipable {

	protected static Logger logger = Logger.getLogger(BitPostingIndexInputStream.class);
	
	/** the lexicon input stream providing the offsets */
	protected final Iterator<? extends BitIndexPointer> pointerList;
	/** The gamma compressed file containing the terms. */
	protected BitIn file; 

	protected Class<? extends IterablePosting> postingIteratorClass;
	protected int currentEntryCount;
	protected BitIndexPointer currentPointer;
	protected int fieldCount;
	protected int entriesSkipped = 0;
	protected byte fileCount;
	protected byte currentFile = 0;
	protected Index index;
	protected String structureName;
	
	public static String getFilename(String path, String prefix, String structureName, byte fileCount, byte fileId)
	{
		return path + "/" + prefix +"."+ structureName + BitIn.USUAL_EXTENSION + 
			(fileCount > 1 ? String.valueOf(fileId) : "");
	}
	
	public static String getFilename(Index _index, String structureName, byte fileCount, byte fileId)
	{
		return _index.getPath() + "/" + _index.getPrefix() +"."+ structureName + BitIn.USUAL_EXTENSION + 
			(fileCount > 1 ? String.valueOf(fileId) : "");
	}
	
	public BitPostingIndexInputStream(
			Index _index, String _structureName, 
			Iterator<? extends BitIndexPointer> _pointerList,
			Class<? extends IterablePosting> _postingIteratorClass) throws IOException
	{
		this.index = _index;
		this.structureName = _structureName;
		fileCount = Byte.parseByte(_index.getIndexProperty("index."+structureName+".data-files", "1"));
		file = new BitInputStream(getFilename(_index, structureName, fileCount, (byte)0));
		pointerList = _pointerList;
		postingIteratorClass = _postingIteratorClass;
		fieldCount = _index.getIntIndexProperty("index."+structureName+".fields.count", currentFile = 0);
	}
	
	public BitFilePosition getPos()
	{
		return new FilePosition(file.getByteOffset(), file.getBitOffset());
	}
	
	public void skip(int numEntries) throws IOException
	{
		((Skipable)pointerList).skip(numEntries);
	}
	
	public int getNumberOfCurrentPostings()
	{
		return currentEntryCount;
	}
	
	/** {@inheritDoc} */
	public IterablePosting getNextPostings() throws IOException {
		if (! this.hasNext())
			return null;
		return loadPostingIterator(pointerList.next());
	}
	
	/** {@inheritDoc} */
	public boolean hasNext() {
		return pointerList.hasNext();
	}

	protected BitIndexPointer _next()
	{
		if (! pointerList.hasNext())
			return null;
		entriesSkipped = 0;
		BitIndexPointer pointer = (BitIndexPointer)pointerList.next();
		while(pointer.getNumberOfEntries() == 0)
		{
			entriesSkipped++;
			if (pointerList.hasNext())
			{	
				pointer = (BitIndexPointer)pointerList.next();
			}
			else
			{
				return null;
			}
		}
		return pointer;
	}
	
	/** {@inheritDoc} */
	public IterablePosting next()
	{
		BitIndexPointer pointer = _next();
		if (pointer == null)//trailing empty document
			return null;
		try{
			return loadPostingIterator(pointer);
		} catch (IOException ioe) {
			logger.info("Couldn't load posting iterator", ioe);
			return null;
		}
	}
	
	public int getEntriesSkipped()
	{
		return entriesSkipped;
	}
	
	protected IterablePosting loadPostingIterator(BitIndexPointer pointer) throws IOException
	{
		//System.err.println("pointer="+pointer.toString() + " file="+currentFile+" actual=@{"+file.getByteOffset() + ","+ file.getBitOffset()+ "}");
		
		//check to see if file id has changed
		if (pointer.getFileNumber() > currentFile)
		{
			//file id changed: close current file, open specified file
			file.close();
			file = new BitInputStream(getFilename(index, structureName, fileCount, currentFile = pointer.getFileNumber()));
		}
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
		currentPointer = pointer;
		currentEntryCount = pointer.getNumberOfEntries();
		IterablePosting rtr = null;
		try{
			if (fieldCount > 0)
				rtr = postingIteratorClass
					.getConstructor(BitIn.class, Integer.TYPE, DocumentIndex.class, Integer.TYPE)
					.newInstance(file, pointer.getNumberOfEntries(), null, fieldCount);
			else
				rtr = postingIteratorClass
					.getConstructor(BitIn.class, Integer.TYPE, DocumentIndex.class)
					.newInstance(file, pointer.getNumberOfEntries(), null);
		} catch (Exception e) {
			throw new WrappedIOException("Problem creating IterablePosting", e);
		}
		return rtr;
	}
	
	public void print()
	{	
		try{
			while(this.hasNext())
			{
				IterablePosting ip = this.next();
				while(ip.next() != IterablePosting.EOL)
				{
					System.out.print(ip.toString());
					System.out.print(" ");
				}
				System.out.println();
			}
		} catch (Exception e) {
			logger.error(e);
		}
	}

	/** {@inheritDoc} */
	public void close() throws IOException
	{
		file.close();
		IndexUtil.close(pointerList);
	}

	/** Not supported */
	public void remove() {
		throw new UnsupportedOperationException();
	}

	public Pointer getCurrentPointer() {
		return currentPointer;
	}

}

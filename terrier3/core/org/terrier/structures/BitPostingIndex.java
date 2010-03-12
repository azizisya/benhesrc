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
 * The Original Code is BitPostingIndex.java
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original contributor)
 */
package org.terrier.structures;

import java.io.IOException;

import org.terrier.compression.BitFileBuffered;
import org.terrier.compression.BitFileInMemoryLarge;
import org.terrier.compression.BitIn;
import org.terrier.compression.BitInSeekable;
import org.terrier.structures.postings.IterablePosting;
import org.terrier.utility.io.WrappedIOException;
/** Class for various bit compressed index implementations, including parents to current DirectIndex and InvertedIndex implementations. 
 * <b>Index properties</b>:
 * <ul>
 * <li><tt>index.STRUCTURENAME.data-files</tt> - how many files represent this structure.</li>
 * <li><tt>index.STRUCTURENAME.data-source</tt> - one of {file,fileinmem} or a class implements BitInSeekable.</li>
 * <li><tt>index.STRUCTURENAME.fields.count</tt> - how many fields are in use by this structures.</li>
 * </ul>
 * @since 3.0
 */
public class BitPostingIndex implements PostingIndex<BitIndexPointer>
{
	protected BitInSeekable[] file;
	protected Class<? extends IterablePosting> postingImplementation;
	protected Index index = null;
	protected int fieldCount = 0;
	
	protected BitPostingIndex(
			String filename, byte fileCount,
			Class<? extends IterablePosting> _postingImplementation,
			String _dataSource)
		throws IOException
	{
		file = new BitInSeekable[fileCount];
		for(int i=0;i<fileCount;i++)
		{
			String dataFilename = fileCount == 1 ? filename : filename + String.valueOf(i);
			if (_dataSource.equals("fileinmem"))
			{
				System.err.println("BitPostingIndex loading " + dataFilename + " to memory");
				this.file[i] = new BitFileInMemoryLarge(dataFilename);
			}
			else if (_dataSource.equals("file"))
			{
				this.file[i] = new BitFileBuffered(dataFilename);
			}
			else
			{
				if (_dataSource.startsWith("uk.ac.gla.terrier"))
					_dataSource = _dataSource.replaceAll("uk.ac.gla.terrier", "org.terrier");				
				try{
					this.file[i] = Class.forName(_dataSource).asSubclass(BitInSeekable.class).getConstructor(String.class).newInstance(dataFilename);
				} catch (Exception e) {
					throw new WrappedIOException(e);
				}
			}
		}
		postingImplementation = _postingImplementation;
	}
	
	public BitPostingIndex(
			Index _index, 
			String _structureName, 
			Class<? extends IterablePosting> _postingImplementation)
		throws IOException
	{
		this(
				_index.getPath() + "/" + _index.getPrefix() + "." + _structureName + BitIn.USUAL_EXTENSION, 
				Byte.parseByte(_index.getIndexProperty("index."+_structureName+".data-files", "1")),
				_postingImplementation,
				_index.getIndexProperty("index."+_structureName+".data-source", "file"));
		index = _index;
		fieldCount = index.getIntIndexProperty("index."+_structureName+".fields.count", 0);
	}
	
	public IterablePosting getPostings(BitIndexPointer pointer) throws IOException
	{
		final BitIn file = this.file[pointer.getFileNumber()].readReset(pointer.getOffset(), pointer.getOffsetBits());
		IterablePosting rtr = null;
		try{
			if (fieldCount > 0)
				rtr = postingImplementation
					.getConstructor(BitIn.class, Integer.TYPE, DocumentIndex.class, Integer.TYPE)
					.newInstance(file, pointer.getNumberOfEntries(), null, fieldCount);
			else
				rtr = postingImplementation
					.getConstructor(BitIn.class, Integer.TYPE, DocumentIndex.class)
					.newInstance(file, pointer.getNumberOfEntries(), null);
		} catch (Exception e) {
			throw new WrappedIOException(e);
		}
		return rtr;
	}

	public void close() {
		try{
			for(java.io.Closeable c : file)
				c.close();
		} catch (IOException ioe) {}
	}

}

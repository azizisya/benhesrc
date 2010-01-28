/*
 * Terrier - Terabyte Retriever 
 * Webpage: http://ir.dcs.gla.ac.uk/terrier 
 * Contact: terrier{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * Information Retrieval Group
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
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.structures;
import gnu.trove.TIntHashSet;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;
import java.io.*;
/**
 * This class implements a disk cache of an DirectIndex.
 * Properties: <ul>
 * <li><tt>caching.directindex.compression</tt></li>
 * <li><tt>caching.directindex.cachedir</tt></li>
 * </ul>
 * @author Craig Macdonald
 * @version $Revision: 1.5 $
 */
public class DiskCachingDirectIndex extends DirectIndex {
	
	protected final boolean UseGzipCompression = Boolean.parseBoolean(ApplicationSetup.getProperty("caching.directindex.compression", "true"));
	protected final DirectIndex dirIndex;
	protected final TIntHashSet cachelist = new TIntHashSet();
	protected final String tmpPath = ApplicationSetup.getProperty("caching.directindex.cachedir",
			System.getProperty("java.io.tmpdir"));

	public DiskCachingDirectIndex(DirectIndex i) {
		super(3,3,3);
		dirIndex = i;
	}

	public DiskCachingDirectIndex()
	{
		this(new DirectIndex());
	}

	public DiskCachingDirectIndex(DocumentIndex lex, String path, String prefix)
	{
		this(new DirectIndex(lex, path, prefix));
	}

	public DiskCachingDirectIndex(DocumentIndex lex, String filename)
	{
		this(new DirectIndex(lex, filename));
	}

	public DiskCachingDirectIndex(DocumentIndex lex, DirectIndex dir)
	{
		this(dir);
	}

	public void reOpenLegacyBitFile() throws IOException
	{
		dirIndex.reOpenLegacyBitFile();
	}

	public int[][] getTerms(int docid)
	{
		int[][] rtr = null;
		if (cachelist.contains(docid))
		{
			try{
				rtr = readCache(docid);
			} catch (Exception ioe) {
				rtr = null;
			}
			if (rtr != null)
			{
				return rtr;
			}
		}
		rtr = dirIndex.getTerms(docid);
		if (cachePut(docid, rtr))
		{
			cachelist.add(docid);	
		}
		return rtr;	
	}

	protected File getCacheFile(final int docid)
	{
		return new File(tmpPath+ "/"+ docid + ".docpostings" +
                (UseGzipCompression ?  ".gz" : ""));
	}

	protected boolean cachePut(final int docid, final int[][] postings)
	{
		try{
			final File cacheFile = getCacheFile(docid);
			if (cacheFile.exists())
			{
				cacheFile.delete();
			}
			final ObjectOutputStream oos = new ObjectOutputStream(Files.writeFileStream(cacheFile));
			oos.writeObject(postings);
			oos.close();
			cacheFile.deleteOnExit();
			return true;
		} catch (Exception e) {
			return false;
		}		
	}

	protected int[][] readCache(int docid) throws Exception
	{
		int[][] rtr = null;
		final File cacheFile = getCacheFile(docid);
		final ObjectInputStream ois =  new ObjectInputStream(Files.openFileStream(cacheFile));
		rtr = (int[][]) ois.readObject();
		ois.close();	
		return rtr;
	}

	public void close() {
		//delete all the temporary files. They should delete on exit anyway
		for(int docid : cachelist.toArray())
		{
			getCacheFile(docid).delete();
		}
		cachelist.clear();
		dirIndex.close();
	}
	
}

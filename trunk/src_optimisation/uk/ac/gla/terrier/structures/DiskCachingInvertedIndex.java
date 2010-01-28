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
 * The Original Code is InvertedIndex.java.
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
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.io.*;
/**
 * This class implements a disk cache of an InvertedIndex
 * Properties: <ul>
 * <li><tt>caching.invertedindex.compression</tt></li>
 * <li><tt>caching.invertedindex.cachedir</tt></li>
 * </ul>
 * @author Craig Macdonald
 * @version $Revision: 1.5 $
 */
public class DiskCachingInvertedIndex extends InvertedIndex {
	
	protected final boolean UseGzipCompression = new Boolean(ApplicationSetup.getProperty("caching.invertedindex.compression", "true")).booleanValue();
	protected final InvertedIndex invIndex;
	protected final TIntHashSet cachelist = new TIntHashSet();
	protected final String tmpPath = ApplicationSetup.getProperty("caching.invertedindex.cachedir",
			System.getProperty("java.io.tmpdir"));

	public DiskCachingInvertedIndex(InvertedIndex i) {
		invIndex = i;
	}

	public DiskCachingInvertedIndex()
	{
		this(new InvertedIndex());
	}

    public DiskCachingInvertedIndex(Lexicon lex, String path, String prefix)
    {
        this(new InvertedIndex(lex, path, prefix));
    }


	public DiskCachingInvertedIndex(Lexicon lex, String filename)
	{
		this(new InvertedIndex(lex, filename));
	}

	public DiskCachingInvertedIndex(Lexicon lex, InvertedIndex inv)
	{
		this(inv);
	}

    public void reOpenLegacyBitFile() throws IOException
    {
        invIndex.reOpenLegacyBitFile();
    }


	public int[][] getDocuments(LexiconEntry lEntry) {
        if (lEntry==null)
            return null;
		return getDocuments(lEntry.termId);
	}


	public int[][] getDocuments(int termid)
	{
		int[][] rtr = null;
		if (cachelist.contains(termid))
		{
			try{
				rtr = readCache(termid);
			} catch (Exception ioe) {
				rtr = null;
			}
			if (rtr != null)
			{
				return rtr;
			}
		}
		rtr = invIndex.getDocuments(termid);
		if (cachePut(termid, rtr))
		{
			cachelist.add(termid);	
		}
		return rtr;	
	}

	protected boolean cachePut(final int termid, final int[][] postings)
	{
		try{
			final File cacheFile = new File(tmpPath+ "/"+ termid + ".postings");
			if (cacheFile.exists())
			{
				cacheFile.delete();
			}
			ObjectOutputStream oos = UseGzipCompression
				?  new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(cacheFile)))
				:  new ObjectOutputStream(new FileOutputStream(cacheFile));
			oos.writeObject(postings);
			oos.close();
			cacheFile.deleteOnExit();
			return true;
		} catch (Exception e) {
			return false;
		}		
	}

	protected int[][] readCache(int termid) throws Exception
	{
		int[][] rtr = null;
		final File cacheFile = new File(tmpPath+ "/"+ termid + ".postings");

		ObjectInputStream ois = UseGzipCompression
			?	new ObjectInputStream(new GZIPInputStream(new FileInputStream(cacheFile)))
			:	new ObjectInputStream(new FileInputStream(cacheFile));
		rtr = (int[][]) ois.readObject();
		ois.close();	
		return rtr;
	}

	public void close() {
		//TODO we could delete all the temporary files. They should delete on exit anyway
		cachelist.clear();
		invIndex.close();
	}
	
}

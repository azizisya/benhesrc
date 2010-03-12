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
 * The Original Code is TestCompressingMetaIndex.java
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original contributor)
 */
package org.terrier.structures;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.junit.Test;

import org.terrier.structures.CompressingMetaIndex.CompressingMetaIndexInputFormat;
import org.terrier.structures.indexing.CompressingMetaIndexBuilder;
import org.terrier.structures.indexing.MetaIndexBuilder;
import org.terrier.utility.Wrapper;
import org.terrier.utility.io.HadoopPlugin;
import org.terrier.utility.io.HadoopUtility;

/** Unit test for CompressingMetaIndex */
public class TestCompressingMetaIndex extends TestCase {

	String[] docnos_in_order = new String[]{
		"doc1",
		"doc2",
		"doc3",
		"doc4"
	};
	
	@Test public void testNormal() throws Exception
	{
		Index index = Index.createNewIndex("/tmp", "test");
		assertNotNull("Index should not be null", index);
		MetaIndexBuilder b = new CompressingMetaIndexBuilder(index, 
				new String[]{"docno"}, new int[]{20}, new String[]{"docno"});
		assertNotNull(b);
		
		for(String docno : docnos_in_order)
		{
			b.writeDocumentEntry(new String[]{docno});
		}
		b.close();
		b = null;
		finishedCreatingMeta(index);
		
		checkRandom(index, docnos_in_order, "docno", 0);
		checkStream(index, docnos_in_order, 0);
		
		checkMRInputFormat(index, docnos_in_order, -1);// 1 split
		checkMRInputFormat(index, docnos_in_order, 20);// 2 splits
		checkMRInputFormat(index, docnos_in_order, 10);// 3 splits  
		
		index.close();
		IndexUtil.deleteIndex("/tmp", "test");
	}
	
	protected void finishedCreatingMeta(Index index) throws Exception
	{
		
	}
	
	protected void checkMRInputFormat(Index index, String[] docnos, long blocksize) throws Exception
	{
		JobConf jc = HadoopPlugin.getJobFactory(this.getClass().getName()).newJob();
		HadoopUtility.toHConfiguration(index, jc);
		CompressingMetaIndexInputFormat.setStructure(jc, "meta");
		CompressingMetaIndexInputFormat information = new CompressingMetaIndexInputFormat();
		information.validateInput(jc);
		information.overrideDataFileBlockSize(blocksize);
		InputSplit[] splits = information.getSplits(jc, 2);
		Set<String> unseenDocnos = new HashSet<String>(Arrays.asList(docnos));
		int seenDocuments = 0;
		for(InputSplit split : splits)
		{
			RecordReader<IntWritable,Wrapper<String[]>> rr = information.getRecordReader(split, jc, null);
			IntWritable key = rr.createKey();
			Wrapper<String[]> value = rr.createValue();
			while(rr.next(key, value))
			{
				seenDocuments++;
				String docno = value.getObject()[0];
				unseenDocnos.remove(docno);
				assertEquals(docnos[key.get()], docno);
			}
			rr.close();
		}
		assertEquals("Not correct number of document seen", docnos.length, seenDocuments);
		assertEquals("Some documents unseen", 0, unseenDocnos.size());
	}
	
	
	@SuppressWarnings("unchecked")
	protected void checkStream(Index index, String[] docnos, int ith) throws Exception
	{
		Iterator<String[]> metaIn = (Iterator<String[]>) index.getIndexStructureInputStream("meta");
		assertNotNull(metaIn);
		int i = 0;
		while(metaIn.hasNext())
		{
			String[] data = metaIn.next();
			assertEquals(docnos[i], data[ith]);
			i++;
		}
		assertEquals(docnos.length, i);
		IndexUtil.close(metaIn);
	}
	
	protected void checkRandom(Index index, String[] docnos, String key, int offset) throws Exception
	{
		MetaIndex mi = index.getMetaIndex();
		assertNotNull(mi);
		for(int i=0;i < docnos.length; i++)
		{
			assertEquals(docnos[i], mi.getAllItems(i)[offset]);
			assertEquals(docnos[i], mi.getItem(key, i));
			assertEquals(docnos[i], mi.getItems(key, new int[]{i})[0]);
			assertEquals(docnos[i], mi.getItems(new String[]{key}, i)[0]);
			assertEquals(docnos[i], mi.getItems(new String[]{key},  new int[]{i})[0][0]);
			
			assertEquals(i, mi.getDocument(key, docnos[i]));
		}
		
		assertEquals(-1, mi.getDocument(key, "doc"));
		assertEquals(-1, mi.getDocument(key, "doc0"));
		assertEquals(-1, mi.getDocument(key, "doc10"));
		
		final int[] docids = new int[docnos.length];
		for(int i=0;i<docids.length;i++)
			docids[i] = i;
		
		final String[] retr_docnos = mi.getItems(key, docids);
		assertEquals(docids.length, retr_docnos.length);
		assertTrue(Arrays.equals(docnos, retr_docnos));
	
		final String[][] retr_docnos2 = mi.getItems(new String[]{key}, docids);
		assertEquals(docids.length, retr_docnos2.length);
		assertEquals(1, retr_docnos2[0].length);
		assertTrue(Arrays.equals(docnos, retr_docnos));
	}
	
}

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
 * The Original Code is TestCompressingMetaIndexBuilderPartitioner.java
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original contributor)
 */
package org.terrier.structures.indexing;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapred.JobConf;
import org.junit.Test;

import org.terrier.structures.indexing.CompressingMetaIndexBuilder.KeyValueTuple;
import org.terrier.structures.indexing.CompressingMetaIndexBuilder.KeyedPartitioner;
import junit.framework.TestCase;

/** Test that the meta index partitioner works as expected */
public class TestCompressingMetaIndexBuilderPartitioner extends TestCase {
	
	@Test public void testOnePartition()
	{
		JobConf jc = new JobConf();
		jc.set("CompressingMetaIndexBuilder.reverse.keyCount", "1");
		jc.set("CompressingMetaIndexBuilder.reverse.keys", "docno");
		
		KeyedPartitioner p = new KeyedPartitioner();
		p.configure(jc);
		KeyValueTuple kv = new KeyValueTuple("docno", "doc1");
		IntWritable docid = new IntWritable(0);
		assertEquals(0, p.getPartition(kv, docid, 1));
	}
	
	@Test public void testTwoKeyOneReducerPartition()
	{
		JobConf jc = new JobConf();
		jc.set("CompressingMetaIndexBuilder.reverse.keyCount", "2");
		jc.set("CompressingMetaIndexBuilder.reverse.keys", "docno,url");
		jc.setNumReduceTasks(1);
		KeyedPartitioner p = new KeyedPartitioner();
		p.configure(jc);
		KeyValueTuple kv1 = new KeyValueTuple("docno", "doc1");
		IntWritable docid = new IntWritable(0);
		assertEquals(0, p.getPartition(kv1, docid, 1));
		KeyValueTuple kv2 = new KeyValueTuple("url", "url1");
		assertEquals(0, p.getPartition(kv2, docid, 1));				
	}
	
	@Test public void testTwoKeyTwoReducerPartition()
	{
		JobConf jc = new JobConf();
		jc.set("CompressingMetaIndexBuilder.reverse.keyCount", "2");
		jc.set("CompressingMetaIndexBuilder.reverse.keys", "docno,url");
		jc.setNumReduceTasks(2);
		KeyedPartitioner p = new KeyedPartitioner();
		p.configure(jc);
		KeyValueTuple kv1 = new KeyValueTuple("docno", "doc1");
		IntWritable docid = new IntWritable(0);
		assertEquals(0, p.getPartition(kv1, docid, 2));
		KeyValueTuple kv2 = new KeyValueTuple("url", "url1");
		assertEquals(1, p.getPartition(kv2, docid, 2));				
	}
	
	@Test public void testThreeKeyTwoReducerPartition()
	{
		JobConf jc = new JobConf();
		jc.set("CompressingMetaIndexBuilder.reverse.keyCount", "2");
		jc.set("CompressingMetaIndexBuilder.reverse.keys", "docno,url,domain");
		jc.setNumReduceTasks(2);
		KeyedPartitioner p = new KeyedPartitioner();
		p.configure(jc);
		KeyValueTuple kv1 = new KeyValueTuple("docno", "doc1");
		IntWritable docid = new IntWritable(0);
		assertEquals(0, p.getPartition(kv1, docid, 2));
		KeyValueTuple kv2 = new KeyValueTuple("url", "url1");
		assertEquals(1, p.getPartition(kv2, docid, 2));
		KeyValueTuple kv3 = new KeyValueTuple("domain", "dom1");
		assertEquals(0, p.getPartition(kv3, docid, 2));
	}
	
	@Test public void testThreeKeyThreeReducerPartition()
	{
		JobConf jc = new JobConf();
		jc.set("CompressingMetaIndexBuilder.reverse.keyCount", "2");
		jc.set("CompressingMetaIndexBuilder.reverse.keys", "docno,url,domain");
		jc.setNumReduceTasks(3);
		KeyedPartitioner p = new KeyedPartitioner();
		p.configure(jc);
		KeyValueTuple kv1 = new KeyValueTuple("docno", "doc1");
		IntWritable docid = new IntWritable(0);
		assertEquals(0, p.getPartition(kv1, docid, 3));
		KeyValueTuple kv2 = new KeyValueTuple("url", "url1");
		assertEquals(1, p.getPartition(kv2, docid, 3));
		KeyValueTuple kv3 = new KeyValueTuple("domain", "dom1");
		assertEquals(2, p.getPartition(kv3, docid, 3));
	}
}

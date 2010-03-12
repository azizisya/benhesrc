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
 * The Original Code is TestCompressedBitFiles.java
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original contributor)
 */
package org.terrier.compression;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import org.terrier.structures.FilePosition;
import org.terrier.utility.io.RandomDataInputMemory;


@RunWith(Suite.class)
@SuiteClasses({
	TestCompressedBitFiles.TestCompressedBitFiles_Streams.class,
	TestCompressedBitFiles.TestCompressedBitFiles_BitByteStreams.class,
	TestCompressedBitFiles.TestCompressedBitFiles_BitFile.class,
	TestCompressedBitFiles.TestCompressedBitFiles_BitFileBuffered.class,
	TestCompressedBitFiles.TestCompressedBitFiles_BitFileInMemory.class,
	TestCompressedBitFiles.TestCompressedBitFiles_BitFile_RandomDataInputMemory.class
})

/** Ensures that Bit implementations perform as expected */
public class TestCompressedBitFiles  {

	public static abstract class TestCompressedBitFiles_Basic extends TestCase
	{
		final int[] testNumbers = new int[]{1, 20, 2, 40, 30, 2, 9, 1, 100, 200, 40};
		
		public TestCompressedBitFiles_Basic(){}
		
		protected abstract BitOut getBitOut() throws Exception;
		protected abstract BitIn getBitIn() throws Exception;
		
		@Test public void testBit() throws Exception
		{
			BitOut out = getBitOut();
			
			FilePosition [] startOffsets = new FilePosition[testNumbers.length];
			
			for(int i=0;i<testNumbers.length;i++)
			{
				startOffsets[i] = new FilePosition(out.getByteOffset(), out.getBitOffset());
				if (i %2 == 0)
					out.writeGamma(testNumbers[i]);
				else
					out.writeUnary(testNumbers[i]);			 
			}
			out.close();
			
			BitIn in = getBitIn();
			for(int i=0;i<testNumbers.length;i++)
			{
				System.err.println("i="+i + " start offset is " + in.getByteOffset() + "," + in.getBitOffset());
				assertEquals(startOffsets[i].getOffset(), in.getByteOffset());
				assertEquals(startOffsets[i].getOffsetBits(), in.getBitOffset());
				int number = (i %2 == 0) ? in.readGamma() : in.readUnary();
				assertEquals(testNumbers[i], number);
				System.err.println("i="+ i + " end offset is " + in.getByteOffset() + "," + in.getBitOffset());
			}
			in.close();
			//System.err.println("compressed form has size " + baos.size() + " bytes");
			in = getBitIn();
			
			in.skipBytes(startOffsets[2].getOffset());
			in.skipBits(startOffsets[2].getOffsetBits());
			assertEquals(startOffsets[2].getOffset(), in.getByteOffset());
			assertEquals(startOffsets[2].getOffsetBits(), in.getBitOffset());
			for(int i=2;i<testNumbers.length;i++)
			{
				//System.err.println("i="+i + " start offset is " + in.getByteOffset() + "," + in.getBitOffset());
				assertEquals(startOffsets[i].getOffset(), in.getByteOffset());
				assertEquals(startOffsets[i].getOffsetBits(), in.getBitOffset());
				int number = (i %2 == 0) ? in.readGamma() : in.readUnary();
				assertEquals(testNumbers[i], number);
				//System.err.println("i="+ i + " end offset is " + in.getByteOffset() + "," + in.getBitOffset());
			}
			in.close();
		}
	}
	
	public static class TestCompressedBitFiles_Streams extends TestCompressedBitFiles_Basic
	{
		public TestCompressedBitFiles_Streams(){}
		ByteArrayOutputStream baos;
		protected BitOut getBitOut() throws Exception
		{
			baos = new ByteArrayOutputStream();
			return new BitOutputStream(baos);
		}
		
		protected BitIn getBitIn() throws Exception
		{
			return new BitInputStream(new ByteArrayInputStream(baos.toByteArray()));
		}
	}
	
	public static class TestCompressedBitFiles_BitByteStreams extends TestCompressedBitFiles_Basic
	{
		public TestCompressedBitFiles_BitByteStreams(){}
		ByteArrayOutputStream baos;
		protected BitOut getBitOut() throws Exception
		{
			baos = new ByteArrayOutputStream();
			return new BitByteOutputStream(baos);
		}
		
		protected BitIn getBitIn() throws Exception
		{
			return new BitInputStream(new ByteArrayInputStream(baos.toByteArray()));
		}
	}
	
	public static abstract class TestCompressedBitFiles_OnFile extends TestCompressedBitFiles_Basic
	{
		@Rule
	    public TemporaryFolder tmpfolder = new TemporaryFolder();

		String filename = "/tmp/test.bf";
		public TestCompressedBitFiles_OnFile() {}
		
		protected BitOut getBitOut() throws Exception
		{
			return new BitOutputStream(tmpfolder.newFile("test.bf").toString());
		}
		
	}
	
	public static class TestCompressedBitFiles_BitFile extends TestCompressedBitFiles_OnFile
	{
		public TestCompressedBitFiles_BitFile(){}
		
		protected BitOut getBitOut() throws Exception
		{
			return new BitFile(filename, "w");
		}
		
		protected BitIn getBitIn() throws Exception
		{
			return new BitFile(filename, "r").readReset((long)0, (byte)0, new File(filename).length()-1, (byte)7);
		}				
	}
	
	public static class TestCompressedBitFiles_BitFileBuffered extends TestCompressedBitFiles_OnFile
	{
		public TestCompressedBitFiles_BitFileBuffered(){}
		
		protected BitOut getBitOut() throws Exception
		{
			return new BitOutputStream(filename);
		}
		
		protected BitIn getBitIn() throws Exception
		{
			return new BitFileBuffered(filename).readReset((long)0, (byte)0, new File(filename).length()-1, (byte)7);
		}

	}
	
	public static class TestCompressedBitFiles_BitFileInMemory extends TestCompressedBitFiles_OnFile
	{
		public TestCompressedBitFiles_BitFileInMemory(){}
				
		protected BitIn getBitIn() throws Exception
		{
			return new BitFileInMemory(filename).readReset((long)0, (byte)0, new File(filename).length()-1, (byte)7);
		}
	}
	
	public static class TestCompressedBitFiles_BitFile_RandomDataInputMemory extends TestCompressedBitFiles_OnFile
	{
		public TestCompressedBitFiles_BitFile_RandomDataInputMemory(){}
				
		protected BitIn getBitIn() throws Exception
		{
			return new BitFile(new RandomDataInputMemory(filename)).readReset((long)0, (byte)0, new File(filename).length()-1, (byte)7);
		}
	}
	
	public static junit.framework.Test suite()
	{
		 TestSuite suite = new TestSuite();
		 suite.setName(TestCompressedBitFiles.class.getName());
		 suite.addTestSuite(TestCompressedBitFiles_Streams.class);
		 suite.addTestSuite(TestCompressedBitFiles_BitByteStreams.class);
		 suite.addTestSuite(TestCompressedBitFiles_BitFile.class);
		 suite.addTestSuite(TestCompressedBitFiles_BitFileBuffered.class);
		 suite.addTestSuite(TestCompressedBitFiles_BitFileInMemory.class);
		 suite.addTestSuite(TestCompressedBitFiles_BitFile_RandomDataInputMemory.class);
		 return suite;
	}
	
}

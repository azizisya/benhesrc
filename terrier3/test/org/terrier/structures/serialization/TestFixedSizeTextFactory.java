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
 * The Original Code is TestFixedSizeTextFactory.java
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original contributor)
 */
package org.terrier.structures.serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import junit.framework.TestCase;

import org.apache.hadoop.io.Text;
import org.junit.Test;

import org.terrier.structures.seralization.FixedSizeTextFactory;
import org.terrier.structures.seralization.FixedSizeWriteableFactory;

/** Check that FixedSizeText behaves as expected */
public class TestFixedSizeTextFactory extends TestCase {
	static String makeStringOfLength(char c, int length)
	{
		StringBuilder s = new StringBuilder();
		for(int i = 0; i<length;i++)
			s.append(c);
		return s.toString();
	}
	
	@Test public void testVariousStrings() throws Exception
	{
		int length = 20;
		FixedSizeWriteableFactory<Text> factory = new FixedSizeTextFactory(length);
		int bytes = factory.getSize();
		
		String[] testStrings = {
				"", "a", "abat", 
				"1234567890", "123456789001234567890",
				"\u0290\u0290", 
				makeStringOfLength('\u0290', length),
				makeStringOfLength('\u0690', length)
				};
		for (String s : testStrings)
		{
			byte[] b = getBytes(factory, s);
			assertEquals(b.length, bytes);
			assertEquals(s, getString(factory, b));
		}
	}
	
	static String getString(FixedSizeWriteableFactory<Text> factory, byte[] b) throws Exception
	{
		ByteArrayInputStream buffer = new ByteArrayInputStream(b);
		DataInputStream dis = new DataInputStream(buffer);
		Text t = factory.newInstance();
		t.readFields(dis);
		return t.toString();
	}
	
	static byte[] getBytes(FixedSizeWriteableFactory<Text> factory, String s) throws Exception
	{
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(buffer);
		Text t = factory.newInstance();
		t.set(s);
		t.write(dos);
		return buffer.toByteArray();
	}
}

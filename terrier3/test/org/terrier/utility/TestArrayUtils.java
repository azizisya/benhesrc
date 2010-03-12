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
 * The Original Code is TestArrayUtils.java
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original contributor)
 */
package org.terrier.utility;

import java.util.Arrays;

import junit.framework.TestCase;

import org.junit.Test;
/** Test ArrayUtils methods */
public class TestArrayUtils extends TestCase {

	@Test public void testParseCommaDelimitedInt() throws Exception
	{
		assertTrue(Arrays.equals(new int[0], ArrayUtils.parseCommaDelimitedInts("")));
		assertTrue(Arrays.equals(new int[0], ArrayUtils.parseCommaDelimitedInts(" ")));
		assertTrue(Arrays.equals(new int[]{1}, ArrayUtils.parseCommaDelimitedInts("1")));
		assertTrue(Arrays.equals(new int[]{1}, ArrayUtils.parseCommaDelimitedInts(" 1")));
		assertTrue(Arrays.equals(new int[]{1}, ArrayUtils.parseCommaDelimitedInts("1 ")));
		assertTrue(Arrays.equals(new int[]{1}, ArrayUtils.parseCommaDelimitedInts(" 1 ")));
		assertTrue(Arrays.equals(new int[]{1,2}, ArrayUtils.parseCommaDelimitedInts("1,2")));
		assertTrue(Arrays.equals(new int[]{1,2}, ArrayUtils.parseCommaDelimitedInts("1 ,2")));
		assertTrue(Arrays.equals(new int[]{1,2}, ArrayUtils.parseCommaDelimitedInts("1, 2")));
		assertTrue(Arrays.equals(new int[]{1,2}, ArrayUtils.parseCommaDelimitedInts("1 , 2")));
		assertTrue(Arrays.equals(new int[]{12,256}, ArrayUtils.parseCommaDelimitedInts("12,256")));
	}
	
	@Test public void testParseCommaDelimitedStrings() throws Exception
	{
		assertTrue(Arrays.equals(new String[0], ArrayUtils.parseCommaDelimitedString("")));
		assertTrue(Arrays.equals(new String[0], ArrayUtils.parseCommaDelimitedString(" ")));
		assertTrue(Arrays.equals(new String[]{"1"}, ArrayUtils.parseCommaDelimitedString("1")));
		assertTrue(Arrays.equals(new String[]{"1"}, ArrayUtils.parseCommaDelimitedString(" 1")));
		assertTrue(Arrays.equals(new String[]{"1"}, ArrayUtils.parseCommaDelimitedString("1 ")));
		assertTrue(Arrays.equals(new String[]{"1"}, ArrayUtils.parseCommaDelimitedString(" 1 ")));
		assertTrue(Arrays.equals(new String[]{"1","2"}, ArrayUtils.parseCommaDelimitedString("1,2")));
		assertTrue(Arrays.equals(new String[]{"1","2"}, ArrayUtils.parseCommaDelimitedString("1 ,2")));
		assertTrue(Arrays.equals(new String[]{"1","2"}, ArrayUtils.parseCommaDelimitedString("1, 2")));
		assertTrue(Arrays.equals(new String[]{"1","2"}, ArrayUtils.parseCommaDelimitedString("1 , 2")));
		assertTrue(Arrays.equals(new String[]{"12","256"}, ArrayUtils.parseCommaDelimitedString("12,256")));
	}
	
}

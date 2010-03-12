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
 * The Original Code is TestStaTools.java
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original contributor)
 */
package org.terrier.utility;

import org.junit.Test;
import static org.junit.Assert.*;

/** Test some of the statistics behaves as expected */
public class TestStaTools {

	@Test public void testMean()
	{
		assertEquals(0.5d, StaTools.mean(new double[]{0.0d, 1.0d}), 0.0d);
		assertEquals(0.5d, StaTools.mean(new double[]{0.0d, 0.5d, 1.0d}), 0.0d);
		assertEquals(0.0d, StaTools.mean(new double[10]), 0.0d);
		assertEquals(0.0d, StaTools.mean(new double[26]), 0.0d);
	}
	
	@Test public void testMeanNonZero()
	{
		assertEquals(1.0d, StaTools.meanNonZero(new double[]{0.0d, 1.0d}), 0.0d);
		assertEquals(0.75d, StaTools.meanNonZero(new double[]{0.0d, 0.5d, 1.0d}), 0.0d);
	}
}

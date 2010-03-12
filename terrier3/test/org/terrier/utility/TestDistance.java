/*
 * Terrier - Terabyte Retriever
 * Webpage: http://ir.dcs.gla.ac.uk/terrier 
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
 * The Original Code is TestDistance.java.
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original author)
 */
package org.terrier.utility;

import static org.junit.Assert.*;
import org.junit.Test;

/** Test that Distance works as expected.
 * @since 3.0
 * @author Craig Macdonald
 */
public class TestDistance {
	@Test public void testNoTimesSameOrder_2terms()
	{
		int[] x = new int[]{8,10,14,15};
		int[] y = new int[]{1,4,6,12,17};
		assertEquals(0, Distance.noTimesSameOrder(new int[][]{x,y}, 20));
		
		y = new int[]{1,4,9,10,12,17};
		assertEquals(1, Distance.noTimesSameOrder(new int[][]{x,y}, 20));
		
		x = new int[]{0};
		y = new int[]{1,4,9,10,12,17};
		assertEquals(1, Distance.noTimesSameOrder(new int[][]{x,y}, 20));
		
		x = new int[]{0};
		y = new int[]{1};
		assertEquals(1, Distance.noTimesSameOrder(new int[][]{x,y}, 20));
		
		x = new int[]{0,5};
		y = new int[]{1,6};
		assertEquals(2, Distance.noTimesSameOrder(new int[][]{x,y}, 20));
		
		x = new int[]{10,15};
		y = new int[]{1,6};
		assertEquals(0, Distance.noTimesSameOrder(new int[][]{x,y}, 20));
	}
	
	@Test public void testFindSmallest()
	{
		int[] x = new int[]{8,14,10,15};
		int[] y = new int[]{4,6,10,12,17,1};
		assertEquals(0, Distance.findSmallest(x, y));
	}
}

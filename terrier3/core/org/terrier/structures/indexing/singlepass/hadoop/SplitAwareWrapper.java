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
 * The Original Code is SplitAwareWrapper.java
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original contributor)
 *   Richard McCreadie <richardm{a.}dcs.gla.ac.uk>
 */
package org.terrier.structures.indexing.singlepass.hadoop;

import org.terrier.utility.Wrapper;

public class SplitAwareWrapper<T> extends Wrapper<T> {
	protected int splitIndex;
	
	public SplitAwareWrapper(int index)
	{
		super();
		splitIndex = index;
	}
	
	public int getSplitIndex(){
		return splitIndex;
	}
	
	public void setSplitIndex(int index)
	{
		splitIndex = index;
	}
}

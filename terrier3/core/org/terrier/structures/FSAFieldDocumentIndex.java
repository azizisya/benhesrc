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
 * The Original Code is FSAFieldDocumentIndex.java
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original contributor)
 */
package org.terrier.structures;

import java.io.IOException;
import java.util.Iterator;

public class FSAFieldDocumentIndex
	extends FSADocumentIndex
	implements FieldDocumentIndex
{
	int[][] fieldLengths;
	
	public FSAFieldDocumentIndex(Index index, String structureName) throws IOException
	{
		super(index, structureName, false);
		initialise(index, structureName);
	}	
	
	@Override
	protected void initialise(Index index, String structureName)
			throws IOException 
	{
		logger.info("Loading document + field lengths for " + structureName + " structure into memory");
		docLengths = new int[this.size()];
		fieldLengths = new int[this.size()][];
		int i=0;
		Iterator<DocumentIndexEntry> iter = new FSADocumentIndexIterator(index, structureName);
		while(iter.hasNext())
		{
			FieldDocumentIndexEntry fdie = (FieldDocumentIndexEntry)iter.next();
			docLengths[i] = fdie.getDocumentLength();
			fieldLengths[i] = fdie.getFieldLengths();
			i++;
		}
		IndexUtil.close(iter);
	}

	public int[] getFieldLengths(int docid) throws IOException {
		return fieldLengths[docid];
	}

}

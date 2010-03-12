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
 * The Original Code is MetaIndex.java
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original contributor)
 */
package org.terrier.structures;

import java.io.IOException;

/** Interface for document metadata. Metadata is stored compressed on disk.
 * Document offsets are stored in memory. Use getItem() methods to get
 * metadata for a given document. Use getDocument() method to determine
 * docid for a given metadata value.
 * @author Craig Macdonald
 * @since 3.0
 */
public interface MetaIndex extends java.io.Closeable {

	/** Obtain metadata of specified type for specified document. */
	public String getItem(String Key, int docid)
		throws IOException;
	
	/** Obtain all metadata for specified document. */
	public String[] getAllItems(int docid) throws IOException;

	/** Obtain metadata of specified type for specified documents. */
	public String[] getItems(String Key, int[] docids)
	    throws IOException;
	
	/** Obtain metadata of specified types for specified document. */
	public String[] getItems(String[] keys, int docid) 
		throws IOException;
	
	/** Obtain metadata of specified types for specified documents. */
	public String[][] getItems(String Key[], int[] docids)
	    throws IOException;
	
	/** Obtain docid where document has specified metadata value in the specified type. 
	 * Returns -1 if the value cannot be found for the specified key type. */
	public int getDocument(String key, String value)
		throws IOException;
	
	/** Returns the keys of this meta index */
	public String[] getKeys();
}

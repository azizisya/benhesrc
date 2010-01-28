/*
 * Terrier - Terabyte Retriever 
 * Webpage: http://ir.dcs.gla.ac.uk/terrier 
 * Contact: terrier{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * Information Retrieval Group
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
 * The Original Code is DocumentIndexInMemory.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk> (original author) 
 */
package uk.ac.gla.terrier.structures;
import uk.ac.gla.terrier.utility.*;
import java.io.*;
import java.util.*;
/**
 * This class extends DocumentIndex, but instead of 
 * accessing the disk file each time, the data are 
 * loaded into memory, in order to decrease access time.a
 * In this implementation, docnos are not saved
 * @author Vassilis Plachouras, Craig Macdonald
 * @version $Revision: 1.1 $
 */
public class DocumentIndexInMemoryNoDocnos extends DocumentIndexInMemory {
	
	public DocumentIndexInMemoryNoDocnos()
	{
		super();
	}

	public DocumentIndexInMemoryNoDocnos(String filename) {
		super(filename);
	}
	
	public int getDocumentId(String docno) {
		return -1;
	}
	public int getDocumentLength(String docno) {
		return -1;
	}
	public String getDocumentNumber(int docid) {
		return null;
	}

	/**
	 * This method loads the data into memory. It does not save the 
	 * docnos
	 * @param dis java.io.DataInputStream The input stream from which 
	 * the data are read,
	 * @param numOfEntries int The number of entries to read
	 * @exception java.io.IOException An input/output exception 
	 * 			  is thrown if there is any error while reading from disk.
	 */
	public void loadIntoMemory(DataInputStream dis, int numOfEntries)
		throws java.io.IOException {
		bitOffset = new byte[numOfEntries];
		byteOffset = new long[numOfEntries];
		doclen = new int[numOfEntries];
		docnos = new String[0];
		final int termLength = ApplicationSetup.DOCNO_BYTE_LENGTH;
		byte[] buffer = new byte[termLength];
		for (int i = 0; i < numOfEntries; i++) {
			dis.readInt();
			doclen[i] = dis.readInt(); //read the document's length
			int bytesRead = dis.read(buffer, 0, termLength);
			//docnos[i] = (new String(buffer)).trim();
			byteOffset[i] = dis.readLong();
			bitOffset[i] = dis.readByte();
		}
	}
}

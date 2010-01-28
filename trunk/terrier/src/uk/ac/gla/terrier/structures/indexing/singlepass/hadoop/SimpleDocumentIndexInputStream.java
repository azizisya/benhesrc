/*
 * Terrier - Terabyte Retriever
 * Webpage: http://ir.dcs.gla.ac.uk/terrier
 * Contact: terrier{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * http://www.gla.uk
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
 * The Original Code is SimpleDocumentIndexInputStream.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Richard McCreadie <richardm{a.}dcs.gla.ac.uk> (original author)
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> 
 */
package uk.ac.gla.terrier.structures.indexing.singlepass.hadoop;

import java.io.EOFException;
import java.io.IOException;

import uk.ac.gla.terrier.structures.DocumentIndexInputStream;

/** InputStream for a simple DocumentIndex. Only contains Docno and length of document 
 * @author Richard McCreadie and Craig Macdonald
 * @version $Revision: 1.1 $
 * @since 2.2 */ 
public class SimpleDocumentIndexInputStream extends DocumentIndexInputStream {

	/** A constructor of a document index input stream from an index path and prefix.
	  * @param path String path to the index
	  * @param prefix String prefix of the filenames of the index
	  */
	public SimpleDocumentIndexInputStream(final String path, final String prefix) {
		super(path, prefix);
		docid = -1;
	}

	@Override
	public int readNextEntry() throws IOException {
		try{
			docid++;
			super.docno = dis.readUTF();
			super.docLength = super.dis.readInt();
			return 4+super.DOCNO_BYTE_LENGTH;
		} catch (EOFException ee) {
			return -1;
		}
	}

}

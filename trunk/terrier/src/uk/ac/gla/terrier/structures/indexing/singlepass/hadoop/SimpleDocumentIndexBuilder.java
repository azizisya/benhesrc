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
 * The Original Code is SimpleDocumentIndexBuilder.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Richard McCreadie <richardm{a.}dcs.gla.ac.uk> (original author)
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> 
 */
package uk.ac.gla.terrier.structures.indexing.singlepass.hadoop;

import java.io.IOException;

import uk.ac.gla.terrier.structures.FilePosition;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.indexing.DocumentIndexBuilder;
import uk.ac.gla.terrier.utility.ApplicationSetup;

/** A simple DocumentIndexBuilder. Only records Docno and length of document 
 * @author Richard McCreadie and Craig Macdonald
 * @version $Revision: 1.1 $
 * @since 2.2 */ 
public class SimpleDocumentIndexBuilder extends DocumentIndexBuilder {
	public SimpleDocumentIndexBuilder(Index i){
		super(i);
	}

	@Override
	public void addEntryToBuffer(String docno, int docLength, FilePosition directIndexOffset) throws IOException {
		addEntryToBuffer(docno, docLength);
		numberOfDocumentIndexEntries++;
	}

	@Override
	public void addEntryToBuffer(String docno, int docLength) throws IOException {
		super.dos.writeUTF(docno);
		super.dos.writeInt(docLength);
		numberOfDocumentIndexEntries++;
	}

	@Override
	public void finishedCollections() {
		//final int maxDocsEncodedDocid = Integer.parseInt(ApplicationSetup.getProperty("indexing.max.encoded.documentindex.docs","5000000"));
		if (index != null)
		{
			index.setIndexProperty("num.Documents", ""+numberOfDocumentIndexEntries);
			index.setIndexProperty("docno.byte.length", ""+ ApplicationSetup.DOCNO_BYTE_LENGTH);
			//index.addIndexStructure("document", 
				/*numberOfDocumentIndexEntries > maxDocsEncodedDocid 
					?*/ //"uk.ac.gla.terrier.structures.indexing.singlepass.hadoop.SimpleDocumentIndex"
					/*: "uk.ac.gla.terrier.structures.DocumentIndexEncoded"*/ //);
			index.addIndexStructureInputStream("document", "uk.ac.gla.terrier.structures.indexing.singlepass.hadoop.SimpleDocumentIndexInputStream");
			//if (numberOfDocumentIndexEntries > maxDocsEncodedDocid)
			//{
			//	logger.warn("Index is very large, so memory efficient DocumentIndex will be used for retrieval. To ensure fast retrieval speed, set index.document.class to uk.ac.gla.terrier.structures.DocumentIndexEncoded, should enough memory be available");
			//}
		}
		close();
	}
}

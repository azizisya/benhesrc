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
 * The Original Code is DocIDIndexer.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original author)
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.indexing;
import java.io.IOException;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.FilePosition;
import uk.ac.gla.terrier.structures.indexing.DocumentIndexBuilder;
import uk.ac.gla.terrier.structures.indexing.DocumentPostingList;
import uk.ac.gla.terrier.terms.TermPipeline;
import uk.ac.gla.terrier.utility.ApplicationSetup;
/** 
 * @version $Revision: 1.1 $
 * @author Craig Macdonald 
 */
public class DocIDIndexer extends Indexer
{
	protected static boolean eatTerms = Boolean.parseBoolean(ApplicationSetup.getProperty("docid.indexer.eat.terms", "true"));
	/** 
	 * Constructs an instance of a DocIDIndexer, using the given path name
	 * for storing the data structures.
	 * @param pathname String the path where the datastructures will be created.
	 * @param prefix String the prefix where the datastructures will be created.
	 */
	public DocIDIndexer(String path, String prefix){super(path,prefix);}
	/** 
	 * Creates the document index only
	 * Loops through each document in each of the collections, 
	 * extracting terms and pushing these through the Term Pipeline 
	 * (eg stemming, stopping, lowercase).
	 * @param collections Collection[] the collections to be indexed.
	 */
	public void createDirectIndex(Collection[] collections)
	{
		currentIndex = Index.createNewIndex(path, prefix);
		docIndexBuilder = new DocumentIndexBuilder(currentIndex);
		int numberOfDocuments = 0; 
		final long startBunchOfDocuments = System.currentTimeMillis();
		final int collections_length = collections.length;
		final DocumentPostingList bla = null;
		for(int collectionNo = 0; collectionNo < collections_length; collectionNo++)
		{
			final Collection collection = collections[collectionNo];
			long startCollection = System.currentTimeMillis();
			while(collection.nextDocument())
			{
				numberOfDocuments++;
				if (eatTerms)
				{
					final Document doc = collection.getDocument();
					while (!doc.endOfDocument())
	                    doc.getNextTerm();
				}
				indexDocument(collection.getDocid(), 0, bla);
			}
			long endCollection = System.currentTimeMillis();
			System.err.println("Collection #"+collectionNo+ " took "+((endCollection-startCollection)/1000)	+"seconds to build the document index\n");
			collection.close();
		}
		docIndexBuilder.finishedCollections();
		final int maxDocsEncodedDocid = Integer.parseInt(ApplicationSetup.getProperty("indexing.max.encoded.documentindex.docs","5000000"));
		currentIndex.flush();
	}

    protected TermPipeline getEndOfPipeline()
	{
		return null;
	}


	private final static FilePosition dummyPosition = new FilePosition((long)0,(byte)0);

    protected void indexDocument(String docid, int numOfTokensInDocument, DocumentPostingList termsInDocument)
    {
        try{
            /* add doc to documentindex */
            docIndexBuilder.addEntryToBuffer(docid, 0, dummyPosition);
        }
        catch (IOException ioe)
        {
            logger.error("Failed to index "+docid,ioe);
        }
    }

	
	/**
	 * Does nothing 
	 */
	public void createInvertedIndex() {	}
}

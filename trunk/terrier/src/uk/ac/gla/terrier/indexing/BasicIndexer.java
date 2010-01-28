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
 * The Original Code is BasicIndexer.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original author)
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.indexing;
import java.io.IOException;
import java.util.Set;

import uk.ac.gla.terrier.structures.FilePosition;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.indexing.DirectIndexBuilder;
import uk.ac.gla.terrier.structures.indexing.DocumentIndexBuilder;
import uk.ac.gla.terrier.structures.indexing.DocumentPostingList;
import uk.ac.gla.terrier.structures.indexing.InvertedIndexBuilder;
import uk.ac.gla.terrier.structures.indexing.LexiconBuilder;
import uk.ac.gla.terrier.structures.indexing.UTFInvertedIndexBuilder;
import uk.ac.gla.terrier.structures.indexing.UTFLexiconBuilder;
import uk.ac.gla.terrier.terms.TermPipeline;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.FieldScore;
import uk.ac.gla.terrier.utility.TermCodes;
/** 
 * BasicIndexer is the default indexer for Terrier. It takes 
 * terms from each Document object provided by the collection, and 
 * adds terms to temporary Lexicons, and into the DirectFile. 
 * The documentIndex is updated to give the pointers into the Direct
 * file. The temporary lexicons are then merged into the main lexicon.
 * Inverted Index construction takes place as a second step.
 * <br>
 * This class replaces much of the createDirectIndex and 
 * createInvertedIndex methods that used to be in DirectIndex.java 
 * in 1.0beta. This class was originally authored by Gianni Amatti 
 * and Vassilis Plachouras. It has been based on code removed from 
 * the class DirectIndex.<br>
 * <b>Properties:</b>
 * <ul>
 * <li><tt>indexing.max.encoded.documentindex.docs</tt> - how many docs before the DocumentIndexEncoded is dropped in favour of the DocumentIndex (on disk implementation).
 * <li><tt>string.use_utf</tt> - use the UTF index structures?
 * <li><i>See Also: Properties in </i><a href="Indexer.html">uk.ac.gla.terrier.indexing.Indexer</a> <i>and</i> <a href="BlockIndexer.html">uk.ac.gla.terrier.indexing.BlockIndexer</a></li>
 * </ul>
 * @version $Revision: 1.1 $
 * @author Craig Macdonald &amp; Vassilis Plachouras
 * @see uk.ac.gla.terrier.indexing.Indexer
 * @see uk.ac.gla.terrier.indexing.BlockIndexer
 */
public class BasicIndexer extends Indexer
{
	
	/** 
	 * This class implements an end of a TermPipeline that adds the
	 * term to the DocumentTree. This TermProcessor does NOT have field
	 * support.
	 */
	protected class BasicTermProcessor implements TermPipeline
	{
		//term pipeline implementation
		public void processTerm(String term)
		{
			/* null means the term has been filtered out (eg stopwords) */
			if (term != null)
			{
				//add term to thingy tree
				termsInDocument.insert(term);
				numOfTokensInDocument++;
			}
		}
	}
	/** This class implements an end of a TermPipeline that adds the
	 *  term to the DocumentTree. This TermProcessor does have field
	 *  support.
	 */
	protected class FieldTermProcessor implements TermPipeline
	{
		public void processTerm(String term)
		{
			/* null means the term has been filtered out (eg stopwords) */
			if (term != null)
			{
				/* add term to Document tree */
				final int[] fieldIds = new int[numFields];
				int i=0;
				for (String fieldName: termFields)
				{
					fieldIds[i] = FieldNames.get(fieldName);
					i++;
				}
				termsInDocument.insert(term,fieldIds);
				numOfTokensInDocument++;
			}
		}
	}

	/** 
	 * A private variable for storing the fields a term appears into.
	 */
	protected Set<String> termFields;
	
	/** 
	 * The structure that holds the terms found in a document.
	 */
	protected DocumentPostingList termsInDocument;
	
	/** 
	 * The number of tokens found in the current document so far/
	 */
	protected int numOfTokensInDocument = 0;
	
	/** Protected do-nothing constructor for use by child classes. Classes which
	  * use this method must call init() */
	protected BasicIndexer(long a, long b, long c) {
		super(a,b,c);
	}

	/** 
	 * Constructs an instance of a BasicIndexer, using the given path name
	 * for storing the data structures.
	 * @param path String the path where the datastructures will be created.
	 * @param prefix String the filename component of the data structures
	 */
	public BasicIndexer(String path, String prefix) {
		super(path, prefix);
		//delay the execution of init() if we are a parent class
		if (this.getClass() == BasicIndexer.class) 
			init();
	}


	/** 
	 * Returns the end of the term pipeline, which corresponds to 
	 * an instance of either BasicIndexer.BasicTermProcessor, or 
	 * BasicIndexer.FieldTermProcessor, depending on whether 
	 * field information is stored.
	 * @return TermPipeline the end of the term pipeline.
	 */
	protected TermPipeline getEndOfPipeline()
	{
		if(FieldScore.USE_FIELD_INFORMATION)
			return new FieldTermProcessor();
		return new BasicTermProcessor();
	}
	/** 
	 * Creates the direct index, the document index and the lexicon.
	 * Loops through each document in each of the collections, 
	 * extracting terms and pushing these through the Term Pipeline 
	 * (eg stemming, stopping, lowercase).
	 * @param collections Collection[] the collections to be indexed.
	 */
	
	public void createDirectIndex(Collection[] collections)
	{
		currentIndex = Index.createNewIndex(path, prefix);
		if (UTFIndexing)
		{
			lexiconBuilder = new UTFLexiconBuilder(currentIndex);
		}
		else
		{
			lexiconBuilder = new LexiconBuilder(currentIndex);
		}
		
		directIndexBuilder = new DirectIndexBuilder(currentIndex);
		docIndexBuilder = new DocumentIndexBuilder(currentIndex);
				
		//int LexiconCount = 0;
		int numberOfDocuments = 0; int numberOfTokens = 0;
		//final long startBunchOfDocuments = System.currentTimeMillis();
		final int collections_length = collections.length;
		final boolean boundaryDocsEnabled = BUILDER_BOUNDARY_DOCUMENTS.size() > 0;
		boolean stopIndexing = false;
		for(int collectionNo = 0; ! stopIndexing && collectionNo < collections_length; collectionNo++)
		{
			final Collection collection = collections[collectionNo];
			long startCollection = System.currentTimeMillis();
			boolean notLastDoc = false;
			while((notLastDoc = collection.nextDocument()))
			{
				/* get the next document from the collection */
				String docid = collection.getDocid();
				//System.out.println("Indexing ("+numberOfDocuments+") "+docid);
				Document doc = collection.getDocument();
				
				if (doc == null)
					continue;
				
				numberOfDocuments++; 
				/* setup for parsing */
				createDocumentPostings();
				String term; //term we're currently processing
				numOfTokensInDocument = 0;
	
				//get each term in the document
				while (!doc.endOfDocument()) {
					if ((term = doc.getNextTerm())!=null && !term.equals("")) {
						termFields = doc.getFields();
						/* pass term into TermPipeline (stop, stem etc) */
						pipeline_first.processTerm(term);
						/* the term pipeline will eventually add the term to this object. */
					}
					if (MAX_TOKENS_IN_DOCUMENT > 0 && 
							numOfTokensInDocument > MAX_TOKENS_IN_DOCUMENT)
							break;
				}
				//if we didn't index all tokens from document,
				//we need to get to the end of the document.
				while (!doc.endOfDocument()) 
					doc.getNextTerm();
				/* we now have all terms in the DocumentTree, so we save the document tree */
				if (termsInDocument.getDocumentLength() == 0)
				{	/* this document is empty, add the minimum to the document index */
					indexEmpty(docid);
				}
				else
				{	/* index this docuent */
					numberOfTokens += numOfTokensInDocument;
					indexDocument(docid, numOfTokensInDocument, termsInDocument);
				}	
				
				if (MAX_DOCS_PER_BUILDER>0 && numberOfDocuments >= MAX_DOCS_PER_BUILDER)
				{
					stopIndexing = true;
					break;
				}

				if (boundaryDocsEnabled && BUILDER_BOUNDARY_DOCUMENTS.contains(docid))
				{
					logger.warn("Document "+docid+" is a builder boundary document. Boundary forced.");
					stopIndexing = true;
					break;
				}
			}


			if (! notLastDoc)
			{
				collection.close();
			}

			long endCollection = System.currentTimeMillis();
			long secs = ((endCollection-startCollection)/1000);
			logger.info("Collection #"+collectionNo+ " took "+secs+"seconds to index "
				+"("+numberOfDocuments+" documents)");
			if (secs > 3600)
				 logger.info("Rate: "+((double)numberOfDocuments/((double)secs/3600.0d))+" docs/hour"); 
		}
		finishedDirectIndexBuild();
		/*end of all the collections has been reached */
		/* flush the index buffers */
		directIndexBuilder.finishedCollections();
		docIndexBuilder.finishedCollections();
		/* and then merge all the temporary lexicons */
		lexiconBuilder.finishedDirectIndexBuild();
		currentIndex.setIndexProperty("num.Tokens", ""+numberOfTokens);
		/* reset the in-memory mapping of terms to term codes.*/
		TermCodes.reset();
		/* and clear them out of memory */
		System.gc();
		/* record the fact that these data structures are complete */
		currentIndex.flush();
	}
	protected void indexNoFieldDocument(String docid, int numOfTokensInDocument, DocumentPostingList termsInDocument)
	{
		//final Posting[] termBuffer = termsInDocument.getPostings();
		//final int TermsCount = termBuffer.length;
		final int[][] postings = termsInDocument.getPostings();
		try{
			/* add words to lexicontree */
			lexiconBuilder.addDocumentTerms(termsInDocument);
			//Arrays.sort(termBuffer);
			/* add doc postings to the direct index */
			FilePosition dirIndexPost = directIndexBuilder.addDocument(postings);
			/* add doc to documentindex */
			docIndexBuilder.addEntryToBuffer(docid, numOfTokensInDocument, dirIndexPost);
		}
		catch (IOException ioe)
		{
			logger.error("Failed to index "+docid,ioe);
		}
	}
	protected void indexFieldDocument(String docid, int numOfTokensInDocument, DocumentPostingList termsInDocument)
	{
		//final Posting[] termBuffer = termsInDocument.getPostings();
		//final int TermsCount = termBuffer.length;
		final int[][] postings = termsInDocument.getPostings();
		try{
			/* add words to lexicontree */
			//lexiconBuilder.addDocumentTerms(termBuffer);
			lexiconBuilder.addDocumentTerms(termsInDocument);
			//Arrays.sort(termBuffer);
			/* add doc postings to the direct index */
			FilePosition dirIndexPost = directIndexBuilder.addDocument(postings);
			/* add doc to documentindex */
			docIndexBuilder.addEntryToBuffer(docid, numOfTokensInDocument, dirIndexPost);
		}
		catch (IOException ioe)
		{
			logger.error("Failed to index "+docid,ioe);
		}
	}

	/** 
	 * This adds a document to the direct and document indexes, as well 
	 * as it's terms to the lexicon. Handled internally by the methods 
	 * indexFieldDocument and indexNoFieldDocument.
	 * @param docid String the document identifier.
	 * @param termsInDocument DocumentPostingList the terms in the document.
	 * @param numOfTokensInDocument int the number of indexed tokens in the document.
	 */
	protected void indexDocument(String docid, int numOfTokensInDocument, DocumentPostingList termsInDocument) 
	{
		try{
			/* add words to lexicontree */
			lexiconBuilder.addDocumentTerms(termsInDocument);
			/* add doc postings to the direct index */
			FilePosition dirIndexPost = directIndexBuilder.addDocument(termsInDocument.getPostings());
			/* add doc to documentindex */
			docIndexBuilder.addEntryToBuffer(docid, numOfTokensInDocument, dirIndexPost);
		}
		catch (IOException ioe)
		{
			logger.error("Failed to index "+docid,ioe);
		}
	}
	
	/**
	 * Creates the inverted index after having created the 
	 * direct index, document index and lexicon.
	 */
	public void createInvertedIndex() {
		if (currentIndex == null)
		{
			currentIndex = Index.createIndex(path,prefix);
			if (currentIndex == null)
			{
				logger.error("No index at ("+path+","+prefix+") to build an inverted index for ");
			}
		}
		final long beginTimestamp = System.currentTimeMillis();
		logger.info("Started building the inverted index...");

		if (currentIndex.getCollectionStatistics().getNumberOfUniqueTerms() == 0)
        {
            logger.error("Index has no terms. Inverted index creation aborted.");
			return;
        }
		if (currentIndex.getCollectionStatistics().getNumberOfDocuments() == 0)
		{
			logger.error("Index has no documents. Inverted index creation aborted.");
			return;
		}


		//generate the inverted index
		if (UTFIndexing)
		{
			logger.info("Started building the UTF inverted index...");
			invertedIndexBuilder = new UTFInvertedIndexBuilder(currentIndex);
		}
		else
		{
			logger.info("Started building the inverted index...");
			invertedIndexBuilder = new InvertedIndexBuilder(currentIndex);
		}
		
		invertedIndexBuilder.createInvertedIndex();
		finishedInvertedIndexBuild();
		

//		//and finally, the collection statistics
//		CollectionStatistics.createCollectionStatistics(
//			path,prefix,
//			invertedIndexBuilder.numberOfDocuments, invertedIndexBuilder.numberOfTokens,
//			(int)invertedIndexBuilder.numberOfUniqueTerms, invertedIndexBuilder.numberOfPointers,
//			new String[] {
//				( UTFindexing 
//					? "uk.ac.gla.terrier.structures.UTFLexicon"
//					: "uk.ac.gla.terrier.structures.Lexicon"),
//				(invertedIndexBuilder.numberOfDocuments > maxDocsEncodedDocid
//					? "uk.ac.gla.terrier.structures.DocumentIndex"
//					: "uk.ac.gla.terrier.structures.DocumentIndexEncoded"),
//				"uk.ac.gla.terrier.structures.DirectIndex",
//				"uk.ac.gla.terrier.structures.InvertedIndex"}
//			);
		//invertedIndexBuilder.close();
		long endTimestamp = System.currentTimeMillis();
		logger.info("Finished building the inverted index...");
		long seconds = (endTimestamp - beginTimestamp) / 1000;
		//long minutes = seconds / 60;
		logger.info("Time elapsed for inverted file: " + seconds);
		currentIndex.flush();
	}
	
	/**
	 * Hook method that creates the right type of DocumentTree class.
	 */
	protected void createDocumentPostings(){
		termsInDocument = new DocumentPostingList(FieldScore.FIELDS_COUNT);
	}

	/** Hook method, called when the inverted index is finished - ie the lexicon is finished */
	protected void finishedInvertedIndexBuild()
	{
		if (Boolean.parseBoolean(ApplicationSetup.getProperty("lexicon.use.hash","true"))) {
			logger.debug("Building lexicon hash");
			try{
				LexiconBuilder.createLexiconHash(currentIndex);
			} catch (IOException ioe) {
				logger.warn("Problem creating (optional) Lexicon Hash", ioe);
			}
		}
	}
}

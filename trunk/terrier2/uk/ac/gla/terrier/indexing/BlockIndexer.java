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
 * The Original Code is BlockIndexer.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 * Craig Macdonald <craigm{a.}dcs.gla.ac.uk> 
 * Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.indexing;
import java.io.IOException;
import java.util.Set;

import uk.ac.gla.terrier.structures.FilePosition;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.indexing.BlockDirectIndexBuilder;
import uk.ac.gla.terrier.structures.indexing.BlockDocumentPostingList;
import uk.ac.gla.terrier.structures.indexing.BlockInvertedIndexBuilder;
import uk.ac.gla.terrier.structures.indexing.BlockLexiconBuilder;
import uk.ac.gla.terrier.structures.indexing.DocumentIndexBuilder;
import uk.ac.gla.terrier.structures.indexing.DocumentPostingList;
import uk.ac.gla.terrier.structures.indexing.LexiconBuilder;
import uk.ac.gla.terrier.structures.indexing.UTFBlockInvertedIndexBuilder;
import uk.ac.gla.terrier.structures.indexing.UTFBlockLexiconBuilder;
import uk.ac.gla.terrier.terms.TermPipeline;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.FieldScore;
import uk.ac.gla.terrier.utility.TermCodes;
/**
 * An indexer that saves block information for the indexed terms.
 * <B>Properties:</b>
 * <ul> 
 * <li><tt>block.size</tt> - How many terms should be in one block. If you want to use phrasal search, this need to be 1 (default).</li>
 * <li><tt>max.blocks</tt> - Maximum number of blocks in a document. After this number of blocks, all subsequent terms will be in the same block. Default 100,000</li>
 * <li><tt>block.indexing</tt> - This class should only be used if the <tt>block.indexing</tt> property is set.</li>
 * <li>indexing.max.encoded.documentindex.docs - how many docs before the DocumentIndexEncoded is dropped in favour of the DocumentIndex (on disk implementation).</li>
 * <li>string.use_utf - use the UTF index structures?</li>
 * <li><i>See Also: Properties in </i>uk.ac.gla.terrier.indexing.Indexer <i>and</i> uk.ac.gla.terrier.indexing.BasicIndexer</li>
 * </ul>
 * @author Craig Macdonald &amp; Vassilis Plachouras
 * @version $Revision: 1.1 $
 */
public class BlockIndexer extends Indexer {
	
	/** This class implements an end of a TermPipeline that adds the
	 *  term to the DocumentTree. This TermProcessor does NOT have field
	 *  support.
	 */	 
	protected class BasicTermProcessor implements TermPipeline {
		public void processTerm(String t) {
			//	null means the term has been filtered out (eg stopwords)
			if (t != null) {
				//add term to thingy tree
				termsInDocument.insert(t, blockId);
				numOfTokensInDocument++;
				if (++numOfTokensInBlock >= BLOCK_SIZE && blockId < MAX_BLOCKS) {
					numOfTokensInBlock = 0;
					blockId++;
				}
			}
		}
	}
	/** 
	 * This class implements an end of a TermPipeline that adds the
	 * term to the DocumentTree. This TermProcessor does have field
	 * support.
	 */
	protected class FieldTermProcessor implements TermPipeline {
		public void processTerm(String t) {
			//	null means the term has been filtered out (eg stopwords)
			if (t != null) {
				//add term to document posting list
				final int[] fieldIds = new int[numFields];
				int i=0;
				for (String fieldName: termFields)
				{
					fieldIds[i] = FieldNames.get(fieldName);
					i++;
				}
				termsInDocument.insert(t,fieldIds, blockId);
				numOfTokensInDocument++;
				if (++numOfTokensInBlock >= BLOCK_SIZE && blockId < MAX_BLOCKS) {
					numOfTokensInBlock = 0;
					blockId++;
				}
			}
		}
	}

	/** The number of tokens in the current document so far. */
	protected int numOfTokensInDocument = 0;
	/** The number of tokens in the current block of the current document. */
	protected int numOfTokensInBlock = 0;
	/** The block number of the current document. */
	protected int blockId;
	/** The fields that are set for the current term. */
	protected Set<String> termFields = null;
	/** The list of terms in this document, and for each, the block occurrences. */
	protected BlockDocumentPostingList termsInDocument = null;
	/** The maximum number of terms allowed in a block. See Property <tt>blocks.size</tt> */
	protected int BLOCK_SIZE;
	/** 
	 * The maximum number allowed number of blocks in a document. 
	 * After this value, all the remaining terms are in the final block. See Property <tt>max.blocks</tt>. */
	protected int MAX_BLOCKS;

	/** Constructs an instance of this class, where the created data structures
	  * are stored in the given path, with the given prefix on the filenames.
	  * @param pathname String the path in which the created data structures will be saved
	  * @param prefix String the prefix on the filenames of the created data structures
	  */
	public BlockIndexer(String pathname, String prefix) {
		super(pathname, prefix);
		if (this.getClass() == BlockIndexer.class)
            init();
	}

	/** 
	 * Returns the object that is to be the end of the TermPipeline. 
	 * This method is used at construction time of the parent object. 
	 * @return TermPipeline the last component of the term pipeline.
	 */
	protected TermPipeline getEndOfPipeline() {
		if (FieldScore.USE_FIELD_INFORMATION)
			return new FieldTermProcessor();
		return new BasicTermProcessor();
	}
	
	

	/**
	 * For the given collection, it iterates through the documents and
	 * creates the direct index, document index and lexicon, using 
	 * information about blocks and possibly fields.
	 * @param collections Collection[] the collection to index.
	 * @see uk.ac.gla.terrier.indexing.Indexer#createDirectIndex(uk.ac.gla.terrier.indexing.Collection[])
	 */
	//TODO if this class extends BasicIndexer, then this method could be inherited
	public void createDirectIndex(Collection[] collections) {
		logger.info("BlockIndexer creating direct index");
		currentIndex = Index.createNewIndex(path, prefix);
		if (UTFIndexing)
		{
			lexiconBuilder = new UTFBlockLexiconBuilder(currentIndex);
		}
		else
		{
			lexiconBuilder = new BlockLexiconBuilder(currentIndex);
		}
		directIndexBuilder = new BlockDirectIndexBuilder(currentIndex);
		docIndexBuilder = new DocumentIndexBuilder(currentIndex);
		//int LexiconCount = 0;
		int numberOfDocuments = 0;
		int numberOfTokens = 0;
		//long startBunchOfDocuments = System.currentTimeMillis();
		final boolean boundaryDocsEnabled = BUILDER_BOUNDARY_DOCUMENTS.size() > 0;
		boolean stopIndexing = false;
		for(int collectionNo = 0; !stopIndexing && collectionNo < collections.length; collectionNo++)
		{
			Collection collection = collections[collectionNo];
			long startCollection = System.currentTimeMillis();
			boolean notLastDoc = false;
			while ((notLastDoc = collection.nextDocument())) {
				//get the next document from the collection
				String docid = collection.getDocid();
				Document doc = collection.getDocument();
				
				if (doc == null)
					continue;
				
				numberOfDocuments++;
				//setup for parsing
				createDocumentPostings();
				String term;
				numOfTokensInDocument = 0;
				numOfTokensInBlock = 0;
				blockId = 0;
				//get each term in the document
				while (!doc.endOfDocument()) {
					if ((term = doc.getNextTerm()) != null && 
						!term.equals("")) {
						termFields = doc.getFields();
						//pass term into TermPipeline (stop, stem etc)
						pipeline_first.processTerm(term);
						//the term pipeline will eventually add the term to this
						// object.
					}
					if (MAX_TOKENS_IN_DOCUMENT > 0 && 
						numOfTokensInDocument > MAX_TOKENS_IN_DOCUMENT)
						break;
				}
				//if we didn't index all tokens from document,
				//we need to get to the end of the document.
				while (!doc.endOfDocument()) 
					doc.getNextTerm();
				//we now have all terms in the DocumentTree
	
				//process DocumentTree (tree of terms)
				if (termsInDocument.getDocumentLength() == 0) { 
					//this document is empty, add the
					// minimum to the document index
					indexEmpty(docid);
				} else { /* index this docuent */
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
					stopIndexing = true;
					break;
				}
			}
			long endCollection = System.currentTimeMillis();
			long secs = ((endCollection-startCollection)/1000);
			logger.info("Collection #"+collectionNo+ " took "+secs+"seconds to index "
				+"("+numberOfDocuments+" documents)\n");
			if (secs > 3600)
				 logger.info("Rate: "+((double)numberOfDocuments/((double)secs/3600.0d))+" docs/hour");

			if (! notLastDoc)
			{
				collection.close();
			}
		}

		/* end of the collection has been reached */
		finishedDirectIndexBuild();
		/* flush the index buffers */
		directIndexBuilder.finishedCollections();
		docIndexBuilder.finishedCollections();
		/* and then merge all the temporary lexicons */
		lexiconBuilder.finishedDirectIndexBuild();
		/* reset the in-memory mapping of terms to term codes.*/
		TermCodes.reset();
		System.gc();
		currentIndex.flush();
	}

	/**
	 * Creates the inverted index from the already created direct index,
	 * document index and lexicon. It saves block information and possibly
	 * field information as well.
	 * @see uk.ac.gla.terrier.indexing.Indexer#createInvertedIndex()
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
		long beginTimestamp = System.currentTimeMillis();

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

		if (UTFIndexing)
		{
			logger.info("Started building the utf block inverted index...");
			invertedIndexBuilder = new UTFBlockInvertedIndexBuilder(currentIndex);
		}
		else
		{
			logger.info("Started building the block inverted index...");
			invertedIndexBuilder = new BlockInvertedIndexBuilder(currentIndex);
		}
		invertedIndexBuilder.createInvertedIndex();
		this.finishedInvertedIndexBuild();
		currentIndex.flush();

//		final int maxDocsEncodedDocid = Integer.parseInt(ApplicationSetup.getProperty("indexing.max.encoded.documentindex.docs","5000000"));
//
//		//and finally, the collection statistics
//		CollectionStatistics.createCollectionStatistics(
//			path, prefix,
//			invertedIndexBuilder.numberOfDocuments, invertedIndexBuilder.numberOfTokens,
//			(int)invertedIndexBuilder.numberOfUniqueTerms, invertedIndexBuilder.numberOfPointers,
//			new String[] {
//				(UTFIndexing 
//					? "uk.ac.gla.terrier.structures.UTFLexicon"
//					: "uk.ac.gla.terrier.structures.Lexicon"),
//				(invertedIndexBuilder.numberOfDocuments > maxDocsEncodedDocid
//					? "uk.ac.gla.terrier.structures.DocumentIndex"
//					: "uk.ac.gla.terrier.structures.DocumentIndexEncoded"),
//				 "uk.ac.gla.terrier.structures.BlockDirectIndex",
//				 "uk.ac.gla.terrier.structures.BlockInvertedIndex"}
//			);

		//invertedIndexBuilder.close();
		long endTimestamp = System.currentTimeMillis();
		logger.info("Finished building the block inverted index...");
		long seconds = (endTimestamp - beginTimestamp) / 1000;
		logger.info("Time elapsed for inverted file: " + seconds);
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

	
	protected void createDocumentPostings(){
		termsInDocument = new BlockDocumentPostingList(FieldScore.FIELDS_COUNT);		
		blockId = 0;
		numOfTokensInBlock = 0;	
	}

	@Override
	protected void load_indexer_properties() {
		super.load_indexer_properties();
		BLOCK_SIZE = ApplicationSetup.BLOCK_SIZE;
		MAX_BLOCKS = ApplicationSetup.MAX_BLOCKS;
	}
}

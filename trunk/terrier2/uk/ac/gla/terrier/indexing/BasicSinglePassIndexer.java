
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
 * The Original Code is BasicSinglePassIndexer.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *  Roi Blanco
 *  Craig Macdonald
 */

package uk.ac.gla.terrier.indexing;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.LexiconInputStream;
import uk.ac.gla.terrier.structures.LexiconOutputStream;
import uk.ac.gla.terrier.structures.UTFLexiconInputStream;
import uk.ac.gla.terrier.structures.UTFLexiconOutputStream;
import uk.ac.gla.terrier.structures.indexing.DocumentIndexBuilder;
import uk.ac.gla.terrier.structures.indexing.DocumentPostingList;
import uk.ac.gla.terrier.structures.indexing.LexiconBuilder;
import uk.ac.gla.terrier.structures.indexing.singlepass.FieldPostingInRun;
import uk.ac.gla.terrier.structures.indexing.singlepass.FieldsMemoryPostings;
import uk.ac.gla.terrier.structures.indexing.singlepass.FileRunIteratorFactory;
import uk.ac.gla.terrier.structures.indexing.singlepass.MemoryPostings;
import uk.ac.gla.terrier.structures.indexing.singlepass.RunsMerger;
import uk.ac.gla.terrier.structures.indexing.singlepass.SimplePostingInRun;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.FieldScore;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.MemoryChecker;
import uk.ac.gla.terrier.utility.RuntimeMemoryChecker;
/**
 * This class indexes a document collection (skipping the direct file construction). It implements a single-pass algorithm,
 * that operates in two phases:<br>
 * First, it traverses the document collection, passes the terms through the TermPipeline and builds an in-memory
 * representation of the posting lists. When it has exhausted the main memory, it flushes the sorted posting to disk, along
 * with the lexicon, and continues traversing the collection.<br>
 * The second phase, merges the sorted runs (with their partial lexicons) in disk to create the final inverted file.
 * This class follows the template pattern, so the main bulk of the code is reused for block (and fields) indexing. There are a few hook methods,
 * that chooses the right classes to instanciate, depending on the indexing options defined.
 * <p>
 * <b>Properties:</b>
 * <ul>
 * <li><tt>memory.reserved</tt> - amount of free memory threshold before a run is committed.</li>
 * <li><tt>memory.heap.usage</tt> - amount of max heap allocated to JVM before a run is committed.</li>
 * <li><tt>docs.check</tt> - how often to check the amount of free memory.</li>
 * </ul> 
 * @author Roi Blanco
 * @version $Revision: 1.1 $
 */

public class BasicSinglePassIndexer extends BasicIndexer{

	/** Current document Id */
	protected int currentId = 0;
	
	
	/** Memory Checker - provides the method for checking to see if
	 * the system is running low on memory */
	protected MemoryChecker memoryCheck = null;
	
	/** Number of documents read per memory check */
	protected long docsPerCheck;
	/** Runtime system JVM running this instance of Terrier */
	protected static final Runtime runtime = Runtime.getRuntime();

	/** Number of documents read since the memory consumption was last checked */
	protected int numberOfDocsSinceCheck = 0;
	/** Number of documents read since the memory runs were last flushed to disk */
	protected int numberOfDocsSinceFlush = 0;
	/** Memory status after flush */
	protected long memoryAfterFlush = -1;
	/** Queue with the file names for the runs in disk */
	protected Queue<String[]> fileNames = new LinkedList<String[]>();
	/** Number of the current Run to be written in disk */
	protected int currentFile = 0;
	/** Structure that keeps the posting lists in memory */
	protected MemoryPostings mp;
	/** Structure for merging the run */
	protected RunsMerger merger;

	/** Number of documents indexed */
	protected int numberOfDocuments = 0;
	/** Number of tokens indexed */
	protected long numberOfTokens = 0;
	/** Number of unique terms indexed */
	protected int numberOfUniqueTerms = 0;
	/** Number of pointers indexed */
	protected long numberOfPointers = 0;
	/** what class should be used to read the generated inverted index? */
	protected String invertedIndexClass = "uk.ac.gla.terrier.structures.InvertedIndex";
	/** what class should be used to read the inverted index as a stream? */
	protected String invertedIndexInputStreamClass = "uk.ac.gla.terrier.structures.InvertedIndexInputStream";
	/**
	 * Constructs an instance of a BasicSinglePassIndexer, using the given path name
	 * for storing the data structures.
	 * @param pathname String the path where the datastructures will be created.
	 */
	public BasicSinglePassIndexer(String pathname, String prefix) {
		super(pathname, prefix);
		//delay the execution of init() if we are a parent class
        if (this.getClass() == BasicSinglePassIndexer.class) 
            init();
	}

	/** Protected do-nothing constructor for use by child classes */
	protected BasicSinglePassIndexer(long a, long b, long c) {
		super(a,b,c);
	}


	@Override
	public void createDirectIndex(Collection[] collections) {
		createInvertedIndex(collections);
	}
	@Override
	public void createInvertedIndex(){}




	/**
	 *  Builds the inverted file and lexicon file for the given collections
	 * Loops through each document in each of the collections,
	 * extracting terms and pushing these through the Term Pipeline
	 * (eg stemming, stopping, lowercase).
	 *  @param collections Collection[] the collections to be indexed.
	 */
	public void createInvertedIndex(Collection[] collections) {
		logger.info("Creating IF (no direct file)..");
		long startCollection, endCollection;
		fileNames = new LinkedList<String[]>();	
		numberOfDocuments = currentId = numberOfDocsSinceCheck = numberOfDocsSinceFlush = numberOfUniqueTerms = 0;
		numberOfTokens = numberOfPointers = 0;
		createMemoryPostings();
		currentIndex = Index.createNewIndex(path, prefix);
		docIndexBuilder = new DocumentIndexBuilder(currentIndex);
		MAX_DOCS_PER_BUILDER = Integer.parseInt(ApplicationSetup.getProperty("indexing.max.docs.per.builder", "0"));
		final boolean boundaryDocsEnabled = BUILDER_BOUNDARY_DOCUMENTS.size() > 0;
		final int collections_length = collections.length;
		boolean stopIndexing = false;
		System.gc();
		memoryAfterFlush = runtime.freeMemory();
		logger.debug("Starting free memory: "+memoryAfterFlush/1000000+"M");

		for(int collectionNo = 0; ! stopIndexing && collectionNo < collections_length; collectionNo++)
		{
			Collection collection = collections[collectionNo];
			startCollection = System.currentTimeMillis();
			while(collection.nextDocument())
			{
				/* get the next document from the collection */
				final String docno = collection.getDocid();
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
					// Nothing in the ifile
					indexEmpty(docno);
				}
				else
				{	/* index this document */
					numberOfTokens += numOfTokensInDocument;
					indexDocument(docno, numOfTokensInDocument, termsInDocument);
				}

				if (MAX_DOCS_PER_BUILDER>0 && numberOfDocuments >= MAX_DOCS_PER_BUILDER)
				{
					stopIndexing = true;
					break;
				}

				if (boundaryDocsEnabled && BUILDER_BOUNDARY_DOCUMENTS.contains(docno))
				{
					logger.warn("Document "+docno+" is a builder boundary document. Boundary forced.");
					stopIndexing = true;
					break;
				}
				termsInDocument.clear();
			}
			try{
				mp.finish(finishMemoryPosting());
			}catch(Exception e){
				e.printStackTrace();
			}
			endCollection = System.currentTimeMillis();
			long partialTime = (endCollection-startCollection)/1000;
			logger.info("Collection #"+collectionNo+ " took "+partialTime+ " seconds to build the runs for "+numberOfDocuments+" documents\n");
			logger.info("Merging "+fileNames.size()+" runs...");
			startCollection = System.currentTimeMillis();
			performMultiWayMerge();
			docIndexBuilder.finishedCollections();
			endCollection = System.currentTimeMillis();
			logger.info("Collection #"+collectionNo+" took "+((endCollection-startCollection)/1000)+" seconds to merge\n ");
			logger.info("Collection #"+collectionNo+" total time "+( (endCollection-startCollection)/1000+partialTime));
			long secs = ((endCollection-startCollection)/1000);
			if (secs > 3600)
                 logger.info("Rate: "+((double)numberOfDocuments/((double)secs/3600.0d))+" docs/hour");
		}
		//createStatistics();
		currentIndex.flush();
		finishedInvertedIndexBuild();
	}

	/**
	 * Adds the terms and possibly the field information of a document in
	 * the current lexicon and in the direct index. It also updates the document index.
	 * @param docno String the identifier of the document to index.
	 * @param numOfTokensInDocument int the number of indexed tokens in the document.
	 * @param termsInDocument DocumentPostingList the terms of the document to add to
	 *		the inverted file.
	 */
	protected void indexDocument(String docno, int numOfTokensInDocument, DocumentPostingList termsInDocument)
	{
		if (termsInDocument.getDocumentLength() > 0) {
			numberOfDocsSinceCheck++; numberOfDocsSinceFlush++;
			if(docsPerCheck == numberOfDocsSinceCheck){
				if (memoryCheck.checkMemory())
				{
					try {
						mp.finish(finishMemoryPosting());
					} catch (IOException ioe) {
						logger.error("Failed writing run at doc "+docno, ioe);
					} catch (Error e) {
						logger.error("Error writing run out at doc "+docno, e);
					}
					System.gc();
					createMemoryPostings();
					memoryCheck.reset();
					numberOfDocsSinceFlush = 0;
				}
				numberOfDocsSinceCheck = 0;
			}
			try{
				mp.addTerms(termsInDocument, currentId);
				docIndexBuilder.addEntryToBuffer(docno, numOfTokensInDocument);
			}catch(IOException ioe){
				logger.error("Failed to index "+docno, ioe);
			}
			currentId++;
		}
	}

	/**
	 * Adds the name of the current run + partial lexicon to be flushed in disk.
	 * @return the two dimensional String[] array with the names of the run and partial lexicon to write.
	 */
	protected String[] finishMemoryPosting(){
		String[] names = new String[2];
		names[0] = fileNameNoExtension + "Run."+(currentFile);
		names[1] = fileNameNoExtension + "Run."+(currentFile++)+".str";
		fileNames.add(names);
		return names;
	}

	/**
	 * Uses the merger class to perform a k multiway merge
	 * in a set of previously written runs.
	 * The file names and the number of runs are given by the private queue
	 */
	public void performMultiWayMerge(){
		String[][] fileNames = getFileNames();
		LexiconOutputStream lexStream = createLexiconOutputStream(path, prefix);
		try{
			if (useFieldInformation)
				createFieldRunMerger(fileNames);
			else
				createRunMerger(fileNames);
			merger.beginMerge(fileNames.length, path + ApplicationSetup.FILE_SEPARATOR + prefix +  ApplicationSetup.IFSUFFIX);
			while(!merger.isDone()){
				merger.mergeOne(lexStream);
			}
			merger.endMerge(lexStream);
			lexStream.close();
			numberOfUniqueTerms = merger.getNumberOfTerms();
			numberOfPointers = merger.getNumberOfPointers();
			// Delete the runs files
			for(int i = 0; i < fileNames.length; i++)
			{
				Files.delete(fileNames[i][0]);
				Files.delete(fileNames[i][1]);
			}
			currentIndex.setIndexProperty("num.Terms", ""+numberOfUniqueTerms);
			currentIndex.setIndexProperty("num.Pointers", ""+numberOfPointers);
			currentIndex.setIndexProperty("num.Tokens", ""+numberOfTokens);
			createLexicon(numberOfUniqueTerms);
			currentIndex.addIndexStructure(
					"inverted",
					invertedIndexClass,
					"uk.ac.gla.terrier.structures.Lexicon,java.lang.String,java.lang.String",
					"lexicon,path,prefix");
			currentIndex.addIndexStructureInputStream(
                    "inverted",
                    invertedIndexInputStreamClass,
                    "java.lang.String,java.lang.String,uk.ac.gla.terrier.structures.LexiconInputStream",
                    "path,prefix,lexicon-inputstream");
			currentIndex.setIndexProperty("num.inverted.fields.bits", ""+FieldScore.FIELDS_COUNT );
		}catch(Exception e){
			logger.error("Problem in performMultiWayMerge", e);
		}
	}

	/**
	 * @return the String[][] structure with the name of the runs files and partial lexicons.
	 */
	protected String[][] getFileNames(){
		String[][] files =  new String[fileNames.size()][2];
		int i = 0;
		while(!fileNames.isEmpty()){
			files[i++] = fileNames.poll();
		}
		return files;
	}

	/**
	 * Hook method that creates the right LexiconBuilder instance
	 * @throws IOException
	 */
	protected void createLexicon(int numberOfEntries) throws IOException{
		final LexiconInputStream lis = createLexiconInputStream(path, prefix);
		LexiconBuilder.createLexiconIndex(lis, numberOfEntries, lis.getEntrySize(), path, prefix );
		currentIndex.addIndexStructure(
				"lexicon",
				UTFIndexing ? "uk.ac.gla.terrier.structures.UTFLexicon" :"uk.ac.gla.terrier.structures.Lexicon" );
		currentIndex.addIndexStructureInputStream(
				"lexicon",
				UTFIndexing ? "uk.ac.gla.terrier.structures.UTFLexiconInputStream" :"uk.ac.gla.terrier.structures.LexiconInputStream");
	}

	/**
	 * Hook method that creates the rigth LexiconOutputStream instance.
 	 * @param name filename for the lexicon file.
	 */
	protected LexiconOutputStream createLexiconOutputStream(String path, String prefix){
		return UTFIndexing ? new UTFLexiconOutputStream(path, prefix) : new LexiconOutputStream(path, prefix);
	}

	/**
	 * Hook method that creates the rigth LexiconOutputStream instance.
 	 * @param name filename for the lexicon file.
	 */
	protected LexiconInputStream createLexiconInputStream(String path, String prefix){
		return UTFIndexing ? new UTFLexiconInputStream(path, prefix) : new LexiconInputStream(path, prefix);
	}

	/**
	 * Hook method that creates a FieldRunMerger instance
	 * @throws IOException if an I/O error occurs.
	 */
	protected void createFieldRunMerger(String[][] files) throws Exception{
		merger = new RunsMerger(new FileRunIteratorFactory(files, FieldPostingInRun.class));
	}


	/**
	 * Hook method that creates a RunsMerger instance
	 * @throws IOException if an I/O error occurs.
	 */
	protected void createRunMerger(String[][] files) throws Exception{
		merger = new RunsMerger(new FileRunIteratorFactory(files, SimplePostingInRun.class));
	}

	/**
	 * Hook method that creates the right type of MemoryPostings class.
	 */
	protected void createMemoryPostings(){
		if (useFieldInformation)
			mp = new FieldsMemoryPostings();
		else
			mp = new MemoryPostings();
	}

	/** Adds an entry to document index for empty document with the 
	  * specified docno, only if IndexEmptyDocuments is set to true.
	  * @param docno Document number of document to index
	  * @see uk.ac.gla.terrier.indexing.Indexer#indexEmpty(String) indexEmpty in Indexer
	  */
	protected void indexEmpty(final String docno)
	{
		/* add doc to documentindex, even though it's empty */
		if(IndexEmptyDocuments)
			try
			{
				logger.warn("Adding empty document "+docno);
				docIndexBuilder.addEntryToBuffer(docno, 0);
				currentId++;
			}
			catch (IOException ioe)
			{
				logger.error("Failed to index empty document "+docno, ioe);
			}
	}

	@Override
	protected void load_indexer_properties() {
		// TODO Auto-generated method stub
		super.load_indexer_properties();
		docsPerCheck = ApplicationSetup.DOCS_CHECK_SINGLEPASS;
		memoryCheck = new RuntimeMemoryChecker();
	}


}

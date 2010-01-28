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
 * The Original Code is SortAscendingTripleVectors.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *  Craig Macdonald <craigm{a.}dcs.gla.ac.uk
 *  Richard McCreadie <richardm{a.}dcs.gla.ac.uk
 */

package uk.ac.gla.terrier.indexing.hadoop;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ArrayList;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.TaskAttemptID;
import org.apache.hadoop.mapred.TaskID;

import uk.ac.gla.terrier.compression.BitOutputStream;
import uk.ac.gla.terrier.indexing.BasicSinglePassIndexer;
import uk.ac.gla.terrier.indexing.Document;
import uk.ac.gla.terrier.structures.DocumentIndexInputStream;
import uk.ac.gla.terrier.structures.FilePosition;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.LexiconOutputStream;
import uk.ac.gla.terrier.structures.indexing.DocumentIndexBuilder;
import uk.ac.gla.terrier.structures.indexing.DocumentPostingList;
import uk.ac.gla.terrier.structures.indexing.singlepass.FieldPostingInRun;
import uk.ac.gla.terrier.structures.indexing.singlepass.RunsMerger;
import uk.ac.gla.terrier.structures.indexing.singlepass.SimplePostingInRun;
import uk.ac.gla.terrier.structures.indexing.singlepass.hadoop.HadoopRunIteratorFactory;
import uk.ac.gla.terrier.structures.indexing.singlepass.hadoop.HadoopRunWriter;
import uk.ac.gla.terrier.structures.indexing.singlepass.hadoop.HadoopRunsMerger;
import uk.ac.gla.terrier.structures.indexing.singlepass.hadoop.MapData;
import uk.ac.gla.terrier.structures.indexing.singlepass.hadoop.MapEmittedTerm;
import uk.ac.gla.terrier.structures.indexing.singlepass.hadoop.MapEmittedPostingList;
import uk.ac.gla.terrier.structures.indexing.singlepass.hadoop.SimpleDocumentIndexBuilder;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.FieldScore;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.Wrapper;
import uk.ac.gla.terrier.utility.io.HadoopUtility;
import uk.ac.gla.terrier.utility.io.WrappedIOException;

/**
 * Single Pass Map-Reduce indexer. 
 * <p><h3>Map phase processing</h3>
 * Indexes as a Map task, taking in a series of documents, emitting posting lists for terms as
 * memory becomes exhausted. Two side-files are created for each map task: the first (run files) takes note of how many documents were indexed
 * for each flush and for each map; the second contains the statistics for each document in a minature document index
 * </p>
 * <p><h3>Reduce phase processing</h3>
 * All posting lists for each term are read in, one term at a time. Using the run files, the posting lists are output into the final inverted
 * file, with all document ids corrected. Lastly, when all terms have been processed, the document indexes are merged into the final document
 * index, and the lexicon hash and lexid created.
 * </p>
 * <p><h3>Partitioned Reduce processing</h3>
 * Normally, the map reduce indexer is used with a single reducer. However, if the partitioner is used, multiple reduces can run concurrently,
 * building several final indices. In doing so, a large collection can be indexed into several output indices, which may be useful for distributed
 * retrieval.
 * </p>
 * @author Richard McCreadie and Craig Macdonald
 * @since 2.2
 * @version $Revision: 1.1 $
 */
public class Hadoop_BasicSinglePassIndexer 
	extends BasicSinglePassIndexer 
	implements Mapper<Text, Wrapper<Document>, MapEmittedTerm, MapEmittedPostingList>,
	Reducer<MapEmittedTerm, MapEmittedPostingList, Object, Object>
{

	/** detect if a job is a Map job or not */
	protected static boolean isMap(JobConf jc) {
		return TaskAttemptID.forName(jc.get("mapred.task.id")).isMap();
	}

	/** JobConf of the current running job */	
	protected JobConf jc;

	/**
	 * Empty constructor. 
	 */
	public Hadoop_BasicSinglePassIndexer() {
		super(0,0,0);
		numberOfDocuments = currentId = numberOfDocsSinceCheck = numberOfDocsSinceFlush = numberOfUniqueTerms = 0;
		numberOfTokens = numberOfPointers = 0;
		flushNo=0;
		flushList = new LinkedList<Integer>();
	}
	
	/** Configure this indexer. Firstly, loads ApplicationSetup appropriately. 
	 * Actual configuration of indexer is then handled by configureMap() or configureReduce()
	 * depending on whether a Map or Reduce task is being configured.
	 * @param jc The configuration for the job
	 */
	public void configure(JobConf jc) 
	{
		this.jc = jc;
		
		//1. configure application
		try{ 
			HadoopUtility.loadTerrierJob(jc);
		} catch (Exception e) {
			throw new Error("Cannot load ApplicationSetup", e);
		}
		
		//2. configurure indexer
		try{
			if (isMap(jc))
			{
				configureMap();
			} else {
				configureReduce();
			}
		} catch (Exception e) { 
			throw new Error("Cannot configure indexer", e);
		}
	}
	
	/** Called when the Map or Reduce task ends, to finish up the indexer. Actual cleanup is
	 * handled by closeMap() or closeReduce() depending on whether this is a Map or Reduce task.
	 */
	public void close() throws IOException
	{
		if (isMap(jc))
		{
			closeMap();
		} else {
			closeReduce();
		}
	}
	
	@Override
	/** Hadoop indexer does not have the consideration of boundary documents. */
	protected void load_builder_boundary_documents() { }
	

	/* ==============================================================
	 * Map implementation from here down
	 * ==============================================================
	 */
	
	/** output collector for the current map indexing process */
	protected OutputCollector<MapEmittedTerm, MapEmittedPostingList> outputPostingListCollector;
	
	/** Current map number */
	protected String mapTaskID;
	/** How many flushes have we made */
	protected int flushNo;

	/** OutputStream for the the data on the runs (runNo, flushes etc) */
	protected DataOutputStream RunData;
	/** List of how many documents are in each flush we have made */
	protected LinkedList<Integer> flushList;
	
	protected void configureMap() throws Exception
	{	
		super.init();
		Path indexDestination = FileOutputFormat.getWorkOutputPath(jc);
		mapTaskID = TaskAttemptID.forName(jc.get("mapred.task.id")).getTaskID().toString();
		currentIndex = Index.createNewIndex(indexDestination.toString(), mapTaskID);
		RunData = new DataOutputStream(
				Files.writeFileStream(
						new Path(indexDestination, mapTaskID+".runs").toString())
				);
		RunData.writeUTF(mapTaskID);
		createMemoryPostings();
		super.docIndexBuilder = new SimpleDocumentIndexBuilder(currentIndex);
	}
	
	
	/** causes the posting lists built up in memory to be flushed out */
	protected void forceFlush() throws IOException
	{
		logger.info("Map "+mapTaskID+", flush requested, containing "+numberOfDocsSinceFlush+" documents, flush "+flushNo);
		numberOfDocsSinceFlush= 0;
		if (mp == null)
			throw new IOException("Map flushed before any documents were indexed");
		mp.finish(new HadoopRunWriter(outputPostingListCollector, mapTaskID, flushNo));
		RunData.writeInt(currentId);
	}	
	
	/**
	 * Map processes a single document. Stores the terms in the document along with the posting list
	 * until memory is full or all documents in this map have been processed then writes then to
	 * the output collector.  
	 * @param key - Wrapper for Document Number
	 * @param value - Wrapper for Document Object
	 * @param _outputPostingListCollector Collector for emitting terms and postings lists
	 * @throws IOException
	 */
	public void map(
			Text key, Wrapper<Document> value, 
			OutputCollector<MapEmittedTerm, MapEmittedPostingList> _outputPostingListCollector, 
			Reporter reporter) 
		throws IOException 
	{
		final String docno = key.toString();
		reporter.setStatus("Currently indexing "+docno);
		final Document doc = value.getObject();
		this.outputPostingListCollector = _outputPostingListCollector;
		
		/* setup for parsing */
		createDocumentPostings();
		String term;//term we're currently processing
		numOfTokensInDocument = 0;
		numberOfDocuments++;
		//get each term in the document
		while (!doc.endOfDocument()) {
			reporter.progress();
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
		//we need tocurrentId get to the end of the document.
		while (!doc.endOfDocument()){
			doc.getNextTerm();
		}
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
			reporter.progress();
		}
		termsInDocument.clear();
		
		// check to see if we should flush
		numberOfDocsSinceCheck++; numberOfDocsSinceFlush++;
		if(docsPerCheck == numberOfDocsSinceCheck)
		{
			if (memoryCheck.checkMemory())
			{
				logger.info("Memory running low, flush requested");
				forceFlush();
				// clear memory
				createMemoryPostings();
				currentId = 0;
				numberOfDocsSinceFlush = 0;
				flushNo++;
				System.gc();
				memoryCheck.reset();
			}
			numberOfDocsSinceCheck = 0;
		}

	}
	
	/**
	 * Adds the terms in the document to memory storage and updates the document index
	 */
	protected void indexDocument(String docno, int numOfTokensInDocument, DocumentPostingList termsInDocument)
	{
		assert(termsInDocument.getDocumentLength() > 0);
		
		try{
			// Adds the terms in the document to memory
			mp.addTerms(termsInDocument, currentId);
			// record the Document info in the Document Index
			docIndexBuilder.addEntryToBuffer(docno, numOfTokensInDocument);
		}catch(IOException ioe){
			logger.error("Failed to index "+docno, ioe);
		}
		currentId++; 
		numberOfDocuments++;
	}
	
	/**
	 * Write the empty document to the inverted index
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
				numberOfDocuments++;
			}
			catch (IOException ioe)
			{
				logger.error("Failed to index empty document "+docno, ioe);
			}
	}
	
	/** Finsh up the map processing. Forces a flush, then writes out the final run data */
	protected void closeMap() throws IOException{
		forceFlush();
		docIndexBuilder.finishedCollections();
		currentIndex.flush();
		currentIndex.close();
		RunData.writeInt(-1);
		RunData.writeInt(numberOfDocuments);
		RunData.close();
		logger.info("Map "+mapTaskID+ " finishing, indexed "+numberOfDocuments+ " in "+flushNo+" flushes");
	}

	/* ==============================================================
	 * Reduce implementation from here down
	 * ==============================================================
	 */
	
	/** OutputStream for the Lexicon*/ 
	protected LexiconOutputStream lexstream;
	/** runIterator factory being used to generate RunIterators */
	protected HadoopRunIteratorFactory runIteratorF = null;
	/** records whether the reduce() has been called for the first time */
	protected boolean reduceStarted = false;
	
	protected String[] MapIndexPrefixes = null;

	protected void configureReduce() throws Exception
	{	
		super.init();
		//load in the current index
		final Path indexDestination = FileOutputFormat.getWorkOutputPath(jc);
		currentIndex = Index.createNewIndex(path = indexDestination.toString(), prefix = "data");//TODO get the reduce number, instead of data
		
		super.merger = createtheRunMerger();
		reduceStarted = false;	
	}
	
	protected LinkedList<MapData> loadRunData() throws IOException 
	{
		// Load in Run Data
		ArrayList<String> mapTaskIDs = new ArrayList<String>();
		final LinkedList<MapData> runData = new LinkedList<MapData>();
		DataInputStream runDataIn;
	
		final String jobId = TaskAttemptID.forName(jc.get("mapred.task.id")).getJobID().toString().replaceAll("job", "task");
		final FileStatus[] files = FileSystem.get(jc).listStatus(
			FileOutputFormat.getOutputPath(jc), 
			new org.apache.hadoop.fs.PathFilter()
			{ 
				public boolean accept(Path path)
				{ 
					final String name = path.getName();
					return name.startsWith( jobId )  && name.endsWith(".runs"); 
				}
			}
		);

		if (files == null || files.length == 0)
		{
			throw new IOException("No run status files found in "+FileOutputFormat.getOutputPath(jc));
		}
		
		TaskID previousMapTaskID = null;
		MapData tempHRD;
		for (FileStatus file : files) 
		{
			if (file.isDir()) {
				continue;
			}
			logger.info("Run data file "+ file.getPath().toString()+" has length "+Files.length(file.getPath().toString()));
			runDataIn = new DataInputStream(Files.openFileStream(file.getPath().toString()));
			tempHRD = new MapData(runDataIn);
			// Sanity Check the file ordering
			
			mapTaskIDs.add(tempHRD.getMap());
			TaskID thisMapTaskID = TaskID.forName(tempHRD.getMap());
			if (previousMapTaskID != null && previousMapTaskID.compareTo(thisMapTaskID) > 0) {
				throw new Error("Run Data Files are out of order.");
			}
			previousMapTaskID = thisMapTaskID;
			runData.add(tempHRD);
			runDataIn.close();
		}
		MapIndexPrefixes = mapTaskIDs.toArray(new String[0]);
		return runData;
	}
	
	/**
	 * Merge the postings for the current term, converts the document ID's in the
	 * postings to be relative to one another using the run number, number of documents
	 * covered in each run, the flush number for that run and the number of documents
	 * flushed.
	 * @param mapData - info about the runs(maps) and the flushes
	 */
	public void startReduce(LinkedList<MapData> mapData)
	{
		((HadoopRunsMerger)(super.merger)).beginMerge(mapData);
		lexstream = createLexiconOutputStream(currentIndex.getPath(), currentIndex.getPrefix());
	}
	
	/** Main reduce algorithm step. Called for every term in the merged index, together with accessors
	 * to the posting list information that has been written.
	 * This reduce has no output.
	 * @param Term indexing term which we are reducing the posting lists into
	 * @param postingIterator Iterator over the temporary posting lists we have for this term
	 * @param output Unused output collector
	 * @param reporter Used to report progress
	 */
	public void reduce(
			MapEmittedTerm Term, 
			Iterator<MapEmittedPostingList> postingIterator, 
			OutputCollector<Object, Object> output, 
			Reporter reporter)
		throws IOException
	{
		if (logger.isDebugEnabled()) logger.debug("Reduce for term "+Term.getText());
		if (! reduceStarted)
		{
			final LinkedList<MapData> runData = loadRunData();
        	startReduce(runData);
			reduceStarted = true;
		}
			
		if (Term.getText().toString().trim().length() == 0)
			return;
		runIteratorF.setRunPostingIterator(postingIterator);
		runIteratorF.setTerm(Term.getText().toString());
		try{
			merger.mergeOne(lexstream);
		} catch (Exception e) {
			throw new WrappedIOException(e);
		}
		reporter.progress();
	}

	/** Merges the simple document indexes made for each map, instead creating the final document index */	
	protected void mergeDocumentIndex(Index[] src) throws IOException
	{
		final DocumentIndexBuilder docidOutput = new DocumentIndexBuilder(currentIndex);
		int i_index = 0;
		int docCount =-1;
		for (Index srcIndex: src)
		{
		    final DocumentIndexInputStream docidInput =
		        (DocumentIndexInputStream)srcIndex.getIndexStructureInputStream("document");
		    while (docidInput.readNextEntry() >= 0)
			{
				docCount++;
				System.err.println("docid="+docCount + " docno="+ docidInput.getDocumentNumber() + " doclength="+ docidInput.getDocumentLength());
		        docidOutput.addEntryToBuffer(
		            docidInput.getDocumentNumber(),
		            docidInput.getDocumentLength(),
		            new FilePosition(0L,(byte) 0));
			}
		    docidInput.close();
		    i_index++;
		}
		docidOutput.finishedCollections();
		docCount++;
		logger.info("Finished merging document indices from "+src.length+" map tasks: "+docCount +" indices found");
	}

	/** finishes the reduce step, by closing the lexicon and inverted file output,
 	  * building the lexicon hash and index, and merging the document indices created
	  * by the map tasks. All temporary map-phase files are deleted and the output index finalised */
	protected void closeReduce() throws IOException {
		
		if (! reduceStarted)
		{
			logger.warn("No terms were input, skipping reduce close");
			return;
		}
		//generate final index structures
		//1. any remaining lexicon terms
		merger.endMerge(lexstream);
		//2. the end of the inverted file
		merger.getBos().close();
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
		
		//3. document index
		Index[] sourceIndices = new Index[MapIndexPrefixes.length];
	 	for (int i= 0; i<MapIndexPrefixes.length;i++)
		{
			sourceIndices[i] = Index.createIndex(FileOutputFormat.getOutputPath(jc).toString(), MapIndexPrefixes[i]);
			if (sourceIndices[i] == null)
				throw new IOException("Could not load index from ("
					+FileOutputFormat.getOutputPath(jc).toString()+","+ MapIndexPrefixes[i] +") because "
					+Index.getLastIndexLoadError());
		}
		this.mergeDocumentIndex(sourceIndices);
		//4. close the map phase indices, and delete them
		for(Index i : sourceIndices)
		{
			String path = i.getPath();
			String prefix = i.getPrefix();
			i.close();
			Files.delete(path + ApplicationSetup.FILE_SEPARATOR + prefix + ".runs");
			Files.delete(path + ApplicationSetup.FILE_SEPARATOR + prefix + ApplicationSetup.DOC_INDEX_SUFFIX);
			Files.delete(path + ApplicationSetup.FILE_SEPARATOR + prefix + ".properties");
		}
		//5. finalise the lexicon
		int numTerms;
		currentIndex.setIndexProperty("num.Terms",""+ (numTerms = lexstream.getNumberOfTermsWritten()) );
		currentIndex.setIndexProperty("num.Tokens",""+lexstream.getNumberOfTokensWritten() );
		currentIndex.setIndexProperty("num.Pointers",""+lexstream.getNumberOfPointersWritten() );
		lexstream.close();
		this.createLexicon(numTerms);
		this.finishedInvertedIndexBuild();
		currentIndex.flush();
	}

	/** Creates the RunsMerger and the RunIteratorFactory */
	protected RunsMerger createtheRunMerger() {
		logger.info("creating run merged with fields="+useFieldInformation);
		runIteratorF = 
			new HadoopRunIteratorFactory(null, 
				useFieldInformation 
					? FieldPostingInRun.class
					: SimplePostingInRun.class);
		HadoopRunsMerger tempRM = new HadoopRunsMerger(runIteratorF);
		try{
			tempRM.setBos(new BitOutputStream(
					currentIndex.getPath() + ApplicationSetup.FILE_SEPARATOR 
					+ currentIndex.getPrefix() + ApplicationSetup.IFSUFFIX ));
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return (RunsMerger)tempRM;
	}

}

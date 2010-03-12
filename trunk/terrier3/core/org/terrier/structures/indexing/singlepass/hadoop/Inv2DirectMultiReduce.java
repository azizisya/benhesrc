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
 * The Original Code is Inv2DirectMultiReduce.java
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original contributor)
 */
package org.terrier.structures.indexing.singlepass.hadoop;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.VIntWritable;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.JobID;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Partitioner;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.RunningJob;
import org.apache.hadoop.mapred.TaskAttemptID;
import org.apache.hadoop.mapred.lib.NullOutputFormat;
import org.apache.log4j.Logger;
import org.terrier.compression.BitIn;
import org.terrier.structures.BasicDocumentIndexEntry;
import org.terrier.structures.BitIndexPointer;
import org.terrier.structures.BlockDirectIndex;
import org.terrier.structures.BlockDirectIndexInputStream;
import org.terrier.structures.BlockDirectInvertedOutputStream;
import org.terrier.structures.BlockFieldDirectInvertedOutputStream;
import org.terrier.structures.DirectIndex;
import org.terrier.structures.DirectIndexInputStream;
import org.terrier.structures.DirectInvertedOutputStream;
import org.terrier.structures.DocumentIndexEntry;
import org.terrier.structures.FieldDirectInvertedOutputStream;
import org.terrier.structures.FieldDocumentIndexEntry;
import org.terrier.structures.Index;
import org.terrier.structures.IndexUtil;
import org.terrier.structures.SimpleBitIndexPointer;
import org.terrier.structures.indexing.DocumentIndexBuilder;
import org.terrier.structures.postings.BasicIterablePosting;
import org.terrier.structures.postings.BasicPostingImpl;
import org.terrier.structures.postings.BlockFieldIterablePosting;
import org.terrier.structures.postings.BlockFieldPostingImpl;
import org.terrier.structures.postings.BlockIterablePosting;
import org.terrier.structures.postings.BlockPostingImpl;
import org.terrier.structures.postings.FieldIterablePosting;
import org.terrier.structures.postings.FieldPostingImpl;
import org.terrier.structures.postings.IterablePosting;
import org.terrier.structures.postings.Posting;
import org.terrier.structures.postings.PostingIdComparator;
import org.terrier.structures.postings.WritablePosting;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.Files;
import org.terrier.utility.Wrapper;
import org.terrier.utility.io.HadoopPlugin;
import org.terrier.utility.io.HadoopUtility;
import org.terrier.utility.io.WrappedIOException;
import org.terrier.utility.io.HadoopPlugin.JobFactory;
/** This class inverts an inverted index into a direct index, making use of a single MapReduce job.
 * On completion of the MapReduce job, the counters can be used as validation of the correct
 * running of the job. For instance "Map input records" should equal the number of terms in the index
 * and "Map output records" should equal the number of pointers.
 * @author Craig Macdonald
 * @since 3.0
 */
public class Inv2DirectMultiReduce extends HadoopUtility.MapReduceBase<IntWritable, Wrapper<IterablePosting>, VIntWritable, Posting, Object, Object>
{
	final static Logger logger = Logger.getLogger(Inv2DirectMultiReduce.class);
	
	/** Partitioner partitioning by docid */
	static class ByDocidPartitioner implements Partitioner<VIntWritable, Posting>
	{
		int numberOfDocuments = -1;
		public int getPartition(VIntWritable docid, Posting posting, int numberOfReducers) {
			final int partitionSize = (int)Math.ceil( (double)numberOfDocuments / (double)numberOfReducers);
			return docid.get() / partitionSize;
		}
		public void configure(JobConf job) {
			numberOfDocuments = job.getInt("Inv2Direct.numDocuments", -1);
		}
	}
	
	/** This class performs contains setup for the MR job. It allows reuse of the
	 * general MR job by other similar situations.
	 * @since 3.0
	 */
	public static class Inv2DirectMultiReduceJob
	{
		Index index;
		JobFactory jf;
		Class<?> mapOutputClass;
		Class<? extends DirectInvertedOutputStream> bitOutputClass;
		int numberOfReduceTasks;
		int numberOfTargetEntries;
		
		String sourceStructureName = "inverted";
		String sourceLookupStructureName = "lexicon";
		String targetStructureName = "direct";
		String targetLookupStructureName = "document-df";
		
		public Inv2DirectMultiReduceJob(Index _index, JobFactory _jf)
		{
			this.index = _index;
			this.jf = _jf;
			this.numberOfTargetEntries = this.index.getCollectionStatistics().getNumberOfDocuments();
			this.numberOfReduceTasks = 1;
		}
		
		
		/** Runs the MapReduce job described by the current state of this class */
		public void runJob() throws Exception
		{
			JobConf jc = jf.newJob();
			jc.setJobName(Inv2DirectMultiReduce.class.getSimpleName());
			jc.setMapperClass(Inv2DirectMultiReduce.class);
			jc.setMapOutputKeyClass(VIntWritable.class);
			jc.setMapOutputValueClass(mapOutputClass);
			//a local jobtracker only runs a single reducer
			if ((jc.get("mapred.job.tracker").equals("local")))
			{
				this.setNumberOfReduceTasks(1);
				jc.setNumReduceTasks(1);
				jc.setCompressMapOutput(false);
			} else {
				jc.setNumReduceTasks(numberOfReduceTasks);
				jc.setCompressMapOutput(true);
				jc.setMapOutputCompressorClass(GzipCodec.class);
			}
			jc.setReduceSpeculativeExecution(false);// prevent filename collisions
			jc.setReducerClass(Inv2DirectMultiReduce.class);
			jc.setPartitionerClass(ByDocidPartitioner.class);
			jc.set("Inv2Direct.TargetStructure", targetStructureName);
			jc.set("Inv2Direct.DirectInvertedOutputStream", bitOutputClass.getName());
			jc.setInt("Inv2Direct.numDocuments", numberOfTargetEntries);
			jc.setInputFormat(BitPostingIndexInputFormat.class);
			jc.setOutputFormat(NullOutputFormat.class);
			BitPostingIndexInputFormat.setStructures(jc, sourceStructureName, sourceLookupStructureName);
			HadoopUtility.toHConfiguration(index, jc);
			
			RunningJob rj = JobClient.runJob(jc);
			JobID jobId = rj.getID();
			if (! rj.isSuccessful())
			{
				throw new Exception("Could not complete job");
			}
			logger.info("Inv2DirectMultiReduce MR job "+ jobId.toString() + " is completed, now finishing");
		}

		public Class<? extends DirectInvertedOutputStream> getBitOutputClass() {
			return bitOutputClass;
		}

		public void setBitOutputClass(
				Class<? extends DirectInvertedOutputStream> bitOutputClass) {
			this.bitOutputClass = bitOutputClass;
		}
		
		/** How many entries should there be in the final structure */
		public int getNumberOfTargetEntries() {
			return numberOfTargetEntries;
		}

		public void setNumberOfTargetEntries(int numberOfTargetEntries) {
			this.numberOfTargetEntries = numberOfTargetEntries;
		}

		/** What job factory to use? */
		public JobFactory getJf() {
			return jf;
		}

		public void setJf(JobFactory jf) {
			this.jf = jf;
		}

		/** Type of the posting class */
		public Class<?> getMapOutputClass() {
			return mapOutputClass;
		}

		public void setMapOutputClass(Class<?> mapOutputClass) {
			this.mapOutputClass = mapOutputClass;
		}

		/** Number of reduce task to use */
		public int getNumberOfReduceTasks() {
			return numberOfReduceTasks;
		}

		public void setNumberOfReduceTasks(int numberOfReduceTasks) {
			this.numberOfReduceTasks = numberOfReduceTasks;
		}

		/** Name of the lookup structure for the source structure.
		 * I.e. "lexiconi" for "inverted"
		 */
		public String getSourceLookupStructureName() {
			return sourceLookupStructureName;
		}

		public void setSourceLookupStructureName(String sourceLookupStructureName) {
			this.sourceLookupStructureName = sourceLookupStructureName;
		}

		/** Name of the source posting structure.
		 * E.g. "inverted"
		 */
		public String getSourceStructureName() {
			return sourceStructureName;
		}

		public void setSourceStructureName(String sourceStructureName) {
			this.sourceStructureName = sourceStructureName;
		}

		/** Name of the lookup structure for the target. E.g. "document-new" for "direct" */
		public String getTargetLookupStructureName() {
			return targetLookupStructureName;
		}

		public void setTargetLookupStructureName(String targetLookupStructureName) {
			this.targetLookupStructureName = targetLookupStructureName;
		}

		/** Name of the target structure. E.g. "direct" */
		public String getTargetStructureName() {
			return targetStructureName;
		}

		public void setTargetStructureName(String targetStructureName) {
			this.targetStructureName = targetStructureName;
		}
	}
	
	public static void main(String[] args) throws Exception
	{
		if (args.length != 1 && args.length != 2)
		{
			System.err.println("Usage: "+Inv2DirectMultiReduce.class.getName()+" <numReduceTasks> [--finish]");
			return;
		}
		Index.setIndexLoadingProfileAsRetrieval(false);
		Index index = Index.createIndex();
		if (index == null)
		{
			System.err.println(Index.getLastIndexLoadError());
			return;
		}
		if (args.length > 1 && args[1].equals("--finish"))
			finish(
					index, 
					HadoopPlugin.getGlobalConfiguration(), 
					"inverted", "direct", hasBlocksFields(index, "inverted", null), 
					Integer.parseInt(args[0]));
		else
			invertStructure(index, HadoopPlugin.getJobFactory("inv2direct"), Integer.parseInt(args[0]));
	}
	
	static boolean[] hasBlocksFields(Index index, String sourceStructureName, Inv2DirectMultiReduceJob invJob)
	{
		boolean blocks = false;
		boolean fields = false;
		final String tmp = index.getIndexProperty("index."+sourceStructureName+".parameter_values", null);
		if (tmp.contains("BlockFieldIterablePosting")) {
			if (invJob != null)
			{
				invJob.setMapOutputClass(BlockFieldPostingImpl.class);
				invJob.setBitOutputClass(BlockFieldDirectInvertedOutputStream.class);
			}
			blocks = fields = true;
		} else if (tmp.contains("BlockIterablePosting")) {
			if (invJob != null)
			{
				invJob.setMapOutputClass(BlockPostingImpl.class);
				invJob.setBitOutputClass(BlockDirectInvertedOutputStream.class);
			}
			blocks = true; fields = false;
		} else if (tmp.contains("FieldIterablePosting")) {
			if (invJob != null)
			{
				invJob.setMapOutputClass(FieldPostingImpl.class);
				invJob.setBitOutputClass(FieldDirectInvertedOutputStream.class);
			}
			blocks = false; fields = true;
		} else if (tmp.contains("BasicIterablePosting")) {
			if (invJob != null)
			{
				invJob.setMapOutputClass(BasicPostingImpl.class);
				invJob.setBitOutputClass(DirectInvertedOutputStream.class);
			}
			blocks = fields = false;
		}
		return new boolean[]{blocks, fields};
	}
	
	
	/** Performs the inversion, from "inverted" structure to "direct" structure.
	 * @param index - the index to perform the inversion on 
	 * @param jf - MapReduce job factory
	 * @param numberOfReduceTasks - as it says. More is better.
	 */
	public static void invertStructure(Index index, JobFactory jf, int numberOfReduceTasks) throws Exception
	{
		String sourceStructureName = "inverted";
		String targetStructureName = "direct";
		final long start = System.currentTimeMillis();
		Inv2DirectMultiReduceJob invJob = new Inv2DirectMultiReduceJob(index, jf);
		invJob.setNumberOfReduceTasks(numberOfReduceTasks);
		
		boolean[] blocksfields = hasBlocksFields(index, sourceStructureName, invJob);
		
		invJob.runJob();
		final int numberOfReducers = invJob.getNumberOfReduceTasks();		
		finish(index, jf.newJob(), sourceStructureName, targetStructureName, blocksfields, numberOfReducers);
		
		final long end = System.currentTimeMillis();
		logger.info("Finished reinverting inverted to direct structure in "+ ((end - start)/1000) + " seconds");
	}

	@SuppressWarnings("unchecked")
	private static void finish(Index index, Configuration conf,
			String sourceStructureName, String targetStructureName,
			boolean[] blocksfields, final int numberOfReducers)
			throws IOException, Exception 
	{
		Iterator<DocumentIndexEntry> diis = (Iterator<DocumentIndexEntry>)index.getIndexStructureInputStream("document");
		DocumentIndexBuilder dios = new DocumentIndexBuilder(index, "document-df");
		BitIndexPointer pointer = new SimpleBitIndexPointer();
		
		final boolean blocks = blocksfields[0];
		final boolean fields = blocksfields[1];
		
		if (numberOfReducers == 1)
		{
			String outputPrefix = "-0";
			DataInputStream currentStream = new DataInputStream(Files.openFileStream(index.getPath() + ApplicationSetup.FILE_SEPARATOR + index.getPrefix() + "." + targetStructureName +outputPrefix+ ".pointers"));
			logger.info("Adding pointers to the document index");
			while(diis.hasNext())
			{
				DocumentIndexEntry die =  diis.next();
				pointer.readFields(currentStream);
				DocumentIndexEntry newDIentry = fields
					? new FieldDocumentIndexEntry(die)
					: new BasicDocumentIndexEntry(die);
				newDIentry.setOffset(pointer);
				newDIentry.setNumberOfEntries(pointer.getNumberOfEntries());
				dios.addEntryToBuffer(newDIentry);
			}
			logger.info("Renaming reducer output as direct file");
			Files.delete(index.getPath() + ApplicationSetup.FILE_SEPARATOR + index.getPrefix() + "." + targetStructureName+ BitIn.USUAL_EXTENSION);
			Files.rename(
					index.getPath() + ApplicationSetup.FILE_SEPARATOR + index.getPrefix() + "." + targetStructureName+outputPrefix + BitIn.USUAL_EXTENSION, 
					index.getPath() + ApplicationSetup.FILE_SEPARATOR + index.getPrefix() + "." + targetStructureName+ BitIn.USUAL_EXTENSION);
			currentStream.close();
			Files.delete(index.getPath() + ApplicationSetup.FILE_SEPARATOR + index.getPrefix() + "." + targetStructureName +outputPrefix+ ".pointers");
		}
		else
		{
			logger.info("Merging direct index output from "+ numberOfReducers + " reducers");
			
			final int partitionSize = (int)Math.ceil( (double)(index.getCollectionStatistics().getNumberOfDocuments()) / (double)numberOfReducers);
			final OutputStream DFout = Files.writeFileStream(index.getPath() + ApplicationSetup.FILE_SEPARATOR + index.getPrefix() + "." + targetStructureName+ BitIn.USUAL_EXTENSION);
			long finalFileOffset = 0;
			
			for(int reduce = 0; reduce < numberOfReducers; reduce++)
			{
				logger.info("Copying document index part for reduce task " + reduce);
				String outputPrefix = "-" + reduce;
				DataInputStream currentStream = new DataInputStream(Files.openFileStream(index.getPath() + ApplicationSetup.FILE_SEPARATOR + index.getPrefix() + "." + targetStructureName +outputPrefix+ ".pointers"));
				for(int docOffset = 0; docOffset < partitionSize && diis.hasNext(); docOffset++)
				{
					DocumentIndexEntry die =  diis.next();
					pointer.readFields(currentStream);
					DocumentIndexEntry newDIentry = fields
						? new FieldDocumentIndexEntry(die)
						: new BasicDocumentIndexEntry(die);
					newDIentry.setOffset(finalFileOffset + pointer.getOffset(), pointer.getOffsetBits());
					newDIentry.setNumberOfEntries(pointer.getNumberOfEntries());
					dios.addEntryToBuffer(newDIentry);
				}
				currentStream.close();
				Files.delete(index.getPath() + ApplicationSetup.FILE_SEPARATOR + index.getPrefix() + "." + targetStructureName +outputPrefix+ ".pointers");
				logger.info("Copying direct file part for reduce task " + reduce);
				String partDFfilename = index.getPath() + ApplicationSetup.FILE_SEPARATOR + index.getPrefix() + "." + targetStructureName+outputPrefix + BitIn.USUAL_EXTENSION;
				InputStream partDF = Files.openFileStream(partDFfilename);
				finalFileOffset += Files.length(partDFfilename);
				IOUtils.copyBytes(partDF, DFout, conf, false);
				partDF.close();
				Files.delete(partDFfilename);
			}
			IndexUtil.close(diis);
			DFout.close();
			
		}
		dios.close();
		Files.copyFile(index.getPath() + ApplicationSetup.FILE_SEPARATOR + index.getPrefix() + "." + "document.fsarrayfile", index.getPath() + ApplicationSetup.FILE_SEPARATOR + index.getPrefix() + "." + "document-backup.fsarrayfile");
		IndexUtil.renameIndexStructure(index, "document-df", "document");
		if (fields)
		{
			index.addIndexStructure("document-factory", FieldDocumentIndexEntry.Factory.class.getName(), "java.lang.String", "${index.direct.fields.count}");
		}
		else
		{
			index.addIndexStructure("document-factory", BasicDocumentIndexEntry.Factory.class.getName(), "", "");
		}
		
		String directIndexClass =  blocks ? BlockDirectIndex.class.getName() : DirectIndex.class.getName();
		String directIndexInputStreamClass = blocks ? BlockDirectIndexInputStream.class.getName() : DirectIndexInputStream.class.getName();
		String postingIterator; 
		if (blocks)
		{
			postingIterator = fields ? BlockFieldIterablePosting.class.getName() : BlockIterablePosting.class.getName();
		}			
		else
		{
			postingIterator = fields ? FieldIterablePosting.class.getName() : BasicIterablePosting.class.getName();
		}
		if (fields)
		{
			index.setIndexProperty("index."+targetStructureName+".fields.count", index.getIndexProperty("index."+sourceStructureName+".fields.count", "0"));
			index.setIndexProperty("index."+targetStructureName+".fields.names", index.getIndexProperty("index."+sourceStructureName+".fields.names", ""));
		}
		
		index.addIndexStructure(
				targetStructureName, 
				directIndexClass,
				"org.terrier.structures.Index,java.lang.String,java.lang.Class", 
				"index,structureName,"+ postingIterator);
		index.addIndexStructureInputStream(
				targetStructureName, 
				directIndexInputStreamClass,
				"org.terrier.structures.Index,java.lang.String,java.lang.Class",
				"index,structureName,"+ postingIterator);
		index.flush();
	}
	
	@Override
	protected void configureMap() throws IOException {}
	
	/** Take an iterator of postings. Each posting is inverted, and the a new posting generated */
	public void map(IntWritable termId, Wrapper<IterablePosting> postingWrapper,
			OutputCollector<VIntWritable, Posting> collector, Reporter reporter)
			throws IOException 
	{
		final IterablePosting postingIterator = postingWrapper.getObject();
		reporter.setStatus("Mapping for id " + termId);
		while(postingIterator.next() != IterablePosting.EOL)
		{
			WritablePosting wp = postingIterator.asWritablePosting();
			int docid = postingIterator.getId();
			wp.setId(termId.get());
			reporter.progress();
			collector.collect(new VIntWritable(docid), wp);
		}
	}

	@Override
	protected void closeMap() throws IOException {}

	
	/** stream to write postings to */
	DirectInvertedOutputStream postingOutputStream;
	/** index being processed */
	Index currentIndex;
	/** stream to write pointers to */
	DataOutputStream pointerOutputStream;
	/** this is the docid of the next target document we are expecting in the reduce call */
	int actualDocid=-1;
	/** this is the docid of the last document for this reduce task */
	int lastDocidInPartion = -1;
	
	@SuppressWarnings("unchecked")
	@Override
	protected void configureReduce() throws IOException {
		Index.setIndexLoadingProfileAsRetrieval(false);
		currentIndex = HadoopUtility.fromHConfiguration(jc);
		final int reduceId = TaskAttemptID.forName(jc.get("mapred.task.id")).getTaskID().getId();
		final String outputPrefix = "-" + reduceId;
		try{
			Class<DirectInvertedOutputStream> c = (Class<DirectInvertedOutputStream>)jc.getClass("Inv2Direct.DirectInvertedOutputStream", DirectInvertedOutputStream.class);
			postingOutputStream = c.getConstructor(String.class).newInstance(currentIndex.getPath() + ApplicationSetup.FILE_SEPARATOR + currentIndex.getPrefix() + "." + jc.get("Inv2Direct.TargetStructure")+outputPrefix + BitIn.USUAL_EXTENSION);
		} catch (Exception e) {
			throw new WrappedIOException(e);
		}
		
		logger.info("Writing pointers to" + currentIndex.getPath() + ApplicationSetup.FILE_SEPARATOR + currentIndex.getPrefix() + "." + jc.get("Inv2Direct.TargetStructure") +outputPrefix+ ".pointers");
		pointerOutputStream = new DataOutputStream(Files.writeFileStream(currentIndex.getPath() + ApplicationSetup.FILE_SEPARATOR + currentIndex.getPrefix() + "." + jc.get("Inv2Direct.TargetStructure") +outputPrefix+ ".pointers"));
		final int numberOfDocuments = jc.getInt("Inv2Direct.numDocuments", -1);
		final int numberOfReducers = jc.getNumReduceTasks();
		final int partitionSize = (int)Math.ceil( (double)numberOfDocuments / (double)numberOfReducers);
		actualDocid = 0;
		for(int iReduceId = 0;iReduceId<reduceId;iReduceId++)
			actualDocid += partitionSize;
		lastDocidInPartion = -1 + Math.min(numberOfDocuments, actualDocid + partitionSize);
		logger.info("First docid for this partition predicted to be "+actualDocid);
		logger.info("Last docid for this partition predicted to be "+ lastDocidInPartion);
	}

	public void reduce(VIntWritable _targetDocid, Iterator<Posting> documentPostings,
			OutputCollector<Object, Object> collector, Reporter reporter)
			throws IOException
	{
		final int targetDocid = _targetDocid.get();
		reporter.setStatus("Reducing for doc "+ targetDocid);
		if (actualDocid > targetDocid)
		{
			logger.error("Received posting list for target doc " + targetDocid + " which is greater than actualDoc " + actualDocid + ". This target doc's posting will be ignored.");
			return;
		}
		while(actualDocid < targetDocid)
		{	
			//if (logger.isDebugEnabled())
			//	logger.debug("moving forward: target="+targetDocid + " actual="+actualDocid );
			SimpleBitIndexPointer p = new SimpleBitIndexPointer();
			p.setOffset(postingOutputStream.getOffset());
			p.setNumberOfEntries(0);
			p.write(pointerOutputStream);
			//System.err.println("actualDocid="+ actualDocid + " writing empty pointer");
			actualDocid++;
			reporter.progress();
		}
		
		/* this implementation loads all postings for a given document into memory, then sorts them by
		 * term id. This is acceptable, as documents are assumed to have sufficiently small postings that
		 * they can fit in memory */
		
		List<Posting> postingList = new ArrayList<Posting>();
		int doclen = 0;
		while(documentPostings.hasNext())
		{
			final Posting p = documentPostings.next().asWritablePosting();
			postingList.add(p);
			doclen += p.getFrequency();
			reporter.progress();
		}
				
		Collections.sort(postingList, new PostingIdComparator());
		BitIndexPointer pointer = postingOutputStream.writePostings(postingList.iterator());
		pointer.write(pointerOutputStream);
		actualDocid++;
	}

	@Override
	protected void closeReduce() throws IOException
	{
		//add trailing entries to the pointers file
		while(actualDocid <= lastDocidInPartion)
		{
			SimpleBitIndexPointer p = new SimpleBitIndexPointer();
			p.setOffset(postingOutputStream.getOffset());
			p.setNumberOfEntries(0);
			p.write(pointerOutputStream);
			actualDocid++;
		}
		postingOutputStream.close();
		pointerOutputStream.close();
	}
}

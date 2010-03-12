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
 * The Original Code is HadoopIndexing.java.
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Richard McCreadie <richardm{a.}dcs.gla.ac.uk> (original author)
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> 
 */

package org.terrier.applications;


import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.JobID;
import org.apache.hadoop.mapred.RunningJob;
import org.apache.hadoop.mapred.TaskID;
import org.apache.hadoop.mapred.lib.HashPartitioner;
import org.apache.hadoop.mapred.lib.NullOutputFormat;
import org.apache.log4j.Logger;

import org.terrier.indexing.hadoop.Hadoop_BasicSinglePassIndexer;
import org.terrier.indexing.hadoop.Hadoop_BlockSinglePassIndexer;
import org.terrier.structures.BitIndexPointer;
import org.terrier.structures.BitPostingIndexInputStream;
import org.terrier.structures.FSOMapFileLexiconOutputStream;
import org.terrier.structures.FieldLexiconEntry;
import org.terrier.structures.Index;
import org.terrier.structures.IndexUtil;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.LexiconOutputStream;
import org.terrier.structures.indexing.LexiconBuilder;
import org.terrier.structures.indexing.singlepass.hadoop.MapEmittedPostingList;
import org.terrier.structures.indexing.singlepass.hadoop.MultiFileCollectionInputFormat;
import org.terrier.structures.indexing.singlepass.hadoop.SplitEmittedTerm;
import org.terrier.structures.seralization.FixedSizeWriteableFactory;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.FieldScore;
import org.terrier.utility.Files;
import org.terrier.utility.io.HadoopPlugin;

/**
 * Main run class for the map reduce indexing system.
 * Provides facilities to preform indexing over multiple
 * machines in a map reduce cluster.
 * <p><h3>Input</h3>
 * The collection is assumed to be a list of files, as specified in the collection.spec. For more advanced collections,
 * this class will be need to be changed. The files listed in collection.spec are assumed to be on the Hadoop shared default
 * filesystem - usually HDFS (else Hadoop will throw an error).
 * </p>
 * <p><h3>Output</h3>
 * This class creates indices for the indexed collection, in the directory specified by <tt>terrier.index.path</tt>. If this
 * folder is NOT on the Hadoop shared default (e.g. HDFS), then Hadoop will throw an error.
 * </p>
 * <p>
 * <h3>Reducers</h3>
 * Two reduce modes are supported: <i>term-partitioning</i> creates
 * a single index with multiple files making up the inverted structure; <i>document-partitioning</i>
 * creates mulitiple indices, partitioned by docid. More reduce tasks results in higher indexing
 * speed due to greater concurrency. 
 * <p>
 * Term-partitioning is the default scenario. In this scenario, the maximum reducers allowed is
 * 32. To select document-partitioning, specify the -p flag to main();
 * <p>
 * <b>Properties:</b>
 * <ul>
 * <li><tt>terrier.hadoop.indexing.reducers</tt> - number of reduce tasks, defaults to 26.</li>
 * <li>If <tt>block.indexing</tt> is set, then a block index will be created.</li>
 * </ul>
 * 
 * @author Richard McCreadie and Craig Macdonald
 * @since 2.2
 * @version $Revision: 1.5 $
*/
public class HadoopIndexing
{
	protected static final Logger logger = Logger.getLogger(HadoopIndexing.class);
	
	private static final String usage()
	{
		return "Usage: HadoopIndexing [-p]";
	}
	
	/** Starts the Map reduce indexing.
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		long time =System.currentTimeMillis();
		final HadoopPlugin.JobFactory jf = HadoopPlugin.getJobFactory("HOD-TerrierIndexing");
		if (jf == null)
			throw new Exception("Could not get JobFactory from HadoopPlugin");
		final JobConf conf = jf.newJob();
		conf.setJobName("terrierIndexing");
		if (Files.exists(ApplicationSetup.TERRIER_INDEX_PATH) && Index.existsIndex(ApplicationSetup.TERRIER_INDEX_PATH, ApplicationSetup.TERRIER_INDEX_PREFIX))
		{
			logger.fatal("Cannot index while index exists at "
				+ApplicationSetup.TERRIER_INDEX_PATH+"," + ApplicationSetup.TERRIER_INDEX_PREFIX);
			return;
		}
		
		boolean docPartitioned = false;
		int numberOfReducers = Integer.parseInt(ApplicationSetup.getProperty("terrier.hadoop.indexing.reducers", "26"));
		if (args.length==1 && args[0].equals("-p"))
		{
			logger.info("Document-partitioned Mode, "+numberOfReducers+" output indices.");
			numberOfReducers = Integer.parseInt(args[1]);
			docPartitioned = true;
		}
		else if (args.length == 0)
		{
			logger.info("Term-partitioned Mode, "+numberOfReducers+" reducers creating one inverted index.");
			docPartitioned = false;
			if (numberOfReducers > 26)
			{
				logger.warn("Excessive reduce tasks ("+numberOfReducers+") in use "
					+"- SplitEmittedTerm.SETPartitionerLowercaseAlphaTerm can use 26 at most");
			}
		} else
		{
			logger.fatal(usage());
			return;
		}
		
		boolean blockIndexing = ApplicationSetup.BLOCK_INDEXING;
		if (blockIndexing)
		{
			conf.setMapperClass(Hadoop_BlockSinglePassIndexer.class);
			conf.setReducerClass(Hadoop_BlockSinglePassIndexer.class);
		}
		else
		{
			conf.setMapperClass(Hadoop_BasicSinglePassIndexer.class);
			conf.setReducerClass(Hadoop_BasicSinglePassIndexer.class);
		}
		FileOutputFormat.setOutputPath(conf, new Path(ApplicationSetup.TERRIER_INDEX_PATH));
		conf.setMapOutputKeyClass(SplitEmittedTerm.class);
		conf.setMapOutputValueClass(MapEmittedPostingList.class);
		conf.setBoolean("indexing.hadoop.multiple.indices", docPartitioned);
		
		if (! conf.get("mapred.job.tracker").equals("local"))
		{
			conf.setMapOutputCompressorClass(GzipCodec.class);
			conf.setCompressMapOutput(true);
		}
		else
		{
			conf.setCompressMapOutput(false);
		}
		
		conf.setInputFormat(MultiFileCollectionInputFormat.class);
		conf.setOutputFormat(NullOutputFormat.class);
		conf.setOutputKeyComparatorClass(SplitEmittedTerm.SETRawComparatorTermSplitFlush.class);
		conf.setOutputValueGroupingComparator(SplitEmittedTerm.SETRawComparatorTerm.class);
		conf.setReduceSpeculativeExecution(false);
		//parse the collection.spec
		BufferedReader specBR = Files.openFileReader(ApplicationSetup.COLLECTION_SPEC);
		String line = null;
		List<Path> paths = new ArrayList<Path>();
		while((line = specBR.readLine()) != null)
		{
			if (line.startsWith("#"))
				continue;
			paths.add(new Path(line));
		}
		specBR.close();
		FileInputFormat.setInputPaths(conf,paths.toArray(new Path[paths.size()]));
		conf.setNumReduceTasks(numberOfReducers);
		if (numberOfReducers> 1)
		{
			if (docPartitioned)
				conf.setPartitionerClass(SplitEmittedTerm.SETPartitioner.class);
			else
				conf.setPartitionerClass(SplitEmittedTerm.SETPartitionerLowercaseAlphaTerm.class);
		}
		else
		{
			//for JUnit tests, we seem to need to restore the original partitioner class
			conf.setPartitionerClass(HashPartitioner.class);
		}
		
		JobID jobId = null;
		boolean ranOK = true;
		try{
			RunningJob rj = JobClient.runJob(conf);
			jobId = rj.getID();
		} catch (Exception e) { 
			logger.error("Problem running job", e);
			ranOK = false;
		}
		if (jobId != null)
		{
			deleteTaskFiles(ApplicationSetup.TERRIER_INDEX_PATH, jobId);
		}
		if (ranOK)
		{
			if (! docPartitioned)
			{
				if (numberOfReducers > 1)
					mergeLexiconInvertedFiles(ApplicationSetup.TERRIER_INDEX_PATH, numberOfReducers);
			}
			
			Hadoop_BasicSinglePassIndexer.finish(
					ApplicationSetup.TERRIER_INDEX_PATH, 
					docPartitioned ? numberOfReducers : 1, 
					jf);
		}
		System.out.println("Time Taken = "+((System.currentTimeMillis()-time)/1000)+" seconds");
		jf.close();
	}

	@SuppressWarnings("unchecked")
	protected static void mergeLexiconInvertedFiles(String index_path, int numberOfReducers) throws IOException {
		final String lexiconStructure = "lexicon";
		final String tmpLexiconStructure = "newlex";
		final String invertedStructure = "inverted";

		//we're handling indices as streams, so dont need to load it. but remember previous status
		//moreover, our indices dont have document objects, so errors may occur in preloading
		final boolean indexProfile = Index.getIndexLoadingProfileAsRetrieval();
		Index.setIndexLoadingProfileAsRetrieval(false);
		
		
		//1. load in the input indices
		final Index[] srcIndices = new Index[numberOfReducers];
		final boolean[] existsIndices = new boolean[numberOfReducers];
		Arrays.fill(existsIndices, true);
		for(int i=0;i<numberOfReducers;i++)
		{
			srcIndices[i] = Index.createIndex(index_path, "data-"+i);
			if (srcIndices[i] == null)
			{
				//remove any empty inverted file for this segment
				Files.delete(BitPostingIndexInputStream.getFilename(index_path, "data-"+i, invertedStructure, (byte)1, (byte)1));
				
				//remember that this index doesnt exist
				existsIndices[i] = false;
				logger.warn("No reduce "+i+" output : no output index ["+index_path+","+("data-"+i)+ "]");
			}
		}
		//2. the target index is the first source index
		Index dest = srcIndices[0];
		
		//3. create the new lexicon
		LexiconOutputStream<String> lexOut = new FSOMapFileLexiconOutputStream(
				dest, tmpLexiconStructure, 
				(FixedSizeWriteableFactory<Text>) dest.getIndexStructure(lexiconStructure + "-keyfactory"),
				(Class<? extends FixedSizeWriteableFactory<LexiconEntry>>) dest.getIndexStructure(lexiconStructure + "-valuefactory").getClass());
		
		//4. append each source lexicon on to the new lexicon, amending the filenumber as we go
		int termId = 0;
		for(int i=0;i<numberOfReducers;i++)
		{
			//the partition did not have any stuff
			if (! existsIndices[i])
			{
				//touch an empty inverted index file for this segment, as BitPostingIndex requires that all of the files exist
				Files.writeFileStream(BitPostingIndexInputStream.getFilename(
						dest, invertedStructure, (byte)numberOfReducers, (byte)i)).close();
				continue;
			}
			//else, append the lexicon
			Iterator<Map.Entry<String,LexiconEntry>> lexIn = (Iterator<Map.Entry<String, LexiconEntry>>) srcIndices[i].getIndexStructureInputStream("lexicon");
			while(lexIn.hasNext())
			{
				Map.Entry<String,LexiconEntry> e = lexIn.next();
				e.getValue().setTermId(termId);
				((BitIndexPointer)e.getValue()).setFileNumber((byte)i);
				lexOut.writeNextEntry(e.getKey(), e.getValue());
				termId++;
			}
			IndexUtil.close(lexIn);
			//rename the inverted file to be part of the destination index
			Files.rename(
					BitPostingIndexInputStream.getFilename(srcIndices[i], invertedStructure, (byte)1, (byte)1), 
					BitPostingIndexInputStream.getFilename(dest, invertedStructure, (byte)numberOfReducers, (byte)i));
		}
		lexOut.close();
		
		//5. change over lexicon structures
		final String[] structureSuffices = new String[]{"", "-entry-inputstream"};
		//remove old lexicon structures
		for (String suffix : structureSuffices)
		{
			if (! IndexUtil.deleteStructure(dest, lexiconStructure + suffix))
				logger.warn("Structure " + lexiconStructure + suffix + " not found when removing");
		}
		//rename new lexicon structures
		for (String suffix : structureSuffices)
		{
			if (! IndexUtil.renameIndexStructure(dest, tmpLexiconStructure + suffix, lexiconStructure + suffix))
				logger.warn("Structure " + tmpLexiconStructure + suffix + " not found when renaming");
		}
			
		//6. update destimation index
		
		if (FieldScore.FIELDS_COUNT > 0)
			dest.addIndexStructure("lexicon-valuefactory", FieldLexiconEntry.Factory.class.getName(), "java.lang.String", "${index.inverted.fields.count}");
		dest.setIndexProperty("index."+invertedStructure+".data-files", ""+numberOfReducers);
		LexiconBuilder.optimise(dest, lexiconStructure);
		dest.flush();
		
		//7. close source and dest indices
		for(Index src: srcIndices) //dest is also closed
		{
			if (src != null)
				src.close();
		}
		
		//8. rearrange indices into desired layout
		
		//rename target index
		IndexUtil.renameIndex(index_path, "data-0", index_path, "data");
		//delete other source indices
		for(int i=1;i<numberOfReducers;i++)
		{
			if (existsIndices[i])
				IndexUtil.deleteIndex(index_path, "data-" + i);
		}
		
		//restore loading profile
		Index.setIndexLoadingProfileAsRetrieval(indexProfile);
	}

	public static void deleteTaskFiles(String path, JobID job)
	{
		String[] fileNames = Files.list(path);
		if (fileNames == null)
			return;
		for(String filename : fileNames)
		{
			String periodParts[] = filename.split("\\.");
			try{
				TaskID tid = TaskID.forName(periodParts[0]);
				if (tid.getJobID().compareTo(job) == 0)
				{
					if (! Files.delete(path + "/" + filename))
						logger.warn("Could not delete temporary map side-effect file "+ path + "/" + filename);
				}
			} catch (Exception e) {}
		}   
	 }
}

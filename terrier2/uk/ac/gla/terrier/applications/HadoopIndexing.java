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
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Richard McCreadie <richardm{a.}dcs.gla.ac.uk> (original author)
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> 
 */

package uk.ac.gla.terrier.applications;


import java.io.BufferedReader;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.lib.NullOutputFormat;
import org.apache.log4j.Logger;

import uk.ac.gla.terrier.indexing.hadoop.Hadoop_BasicSinglePassIndexer;
import uk.ac.gla.terrier.indexing.hadoop.Hadoop_BlockSinglePassIndexer;
import uk.ac.gla.terrier.structures.indexing.singlepass.hadoop.ByMapPartitioner;
import uk.ac.gla.terrier.structures.indexing.singlepass.hadoop.MapEmittedPostingList;
import uk.ac.gla.terrier.structures.indexing.singlepass.hadoop.MapEmittedTerm;
import uk.ac.gla.terrier.structures.indexing.singlepass.hadoop.MultiFileCollectionInputFormat;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.io.HadoopPlugin;

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
 * If <tt>block.indexing</tt> is set, then a block index will be created.
 * If the -p flag is set, then more than one index will be created, where the -p value specifies the number of indices (and hence
 * the number of reducers).
 * </p>
 * 
 * @author Richard McCreadie and Craig Macdonald
 * @since 2.2
 * @version $Revision: 1.1 $
*/
public class HadoopIndexing
{
	protected static Logger logger = Logger.getLogger(HadoopIndexing.class);
	
	/** Starts the Map reduce indexing. Optionally, the -p flag can specify how many indices should
	 * be created. More indices results in higher reduce speed, as more reducers can run concurrently
	 * on less data.
	 * INPUT args: [-p numIndices] 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		long time =System.currentTimeMillis();
		final HadoopPlugin.JobFactory jf = HadoopPlugin.getJobFactory("HOD-TerrierIndexing");
		if (jf == null)
			throw new Exception("Could not get JobFactory from HadoopPlugin");
		final JobConf conf = jf.newJob();
		
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
		conf.setMapOutputKeyClass(MapEmittedTerm.class);
		conf.setMapOutputValueClass(MapEmittedPostingList.class);
		conf.setInputFormat(MultiFileCollectionInputFormat.class);
		conf.setOutputFormat(NullOutputFormat.class);
		conf.setOutputKeyComparatorClass(MapEmittedTerm.TermMapFlushComparator.class);
        conf.setOutputValueGroupingComparator(MapEmittedTerm.TermComparator.class);

		//parse the collection.spec
		BufferedReader specBR = Files.openFileReader(ApplicationSetup.COLLECTION_SPEC);
		String line = null;
		while((line = specBR.readLine()) != null)
		{
			FileInputFormat.addInputPath(conf, new Path(line));
		}
		specBR.close();
		
		if (args.length==2 && args[0].equals("-p")) 
		{
			logger.info("Partitioned Mode, "+Integer.parseInt(args[1])+" output indicies.");
			conf.setPartitionerClass(ByMapPartitioner.class);
			conf.setNumReduceTasks(Integer.parseInt(args[1]));
		} 
		else 
		{
			conf.setNumReduceTasks(1);
		}

		try{
			JobClient.runJob(conf);
		} catch (Exception e) { 
			logger.error("Problem running job", e);
		}
		System.out.println("Time Taken = "+((System.currentTimeMillis()-time)/1000)+" seconds");
		jf.close();
	}

}

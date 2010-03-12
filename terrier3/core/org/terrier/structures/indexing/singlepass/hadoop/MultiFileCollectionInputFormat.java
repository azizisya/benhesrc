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
 * The Original Code is MultiFileCollectionInputFormat.java.
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Richard McCreadie <richardm{a.}dcs.gla.ac.uk> (original author)
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> 
 */
package org.terrier.structures.indexing.singlepass.hadoop;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MultiFileInputFormat;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;
import org.apache.log4j.Logger;
import org.terrier.indexing.Document;

/**
 * Input Format Class for Hadoop Indexing. Splits the input collection into
 * sets of files where each Map task gets about the same number of files.
 * Files are assumed to be un-splittable and are not split. Splits are of
 * adjacent files - i.e. split 0 always has the first file, and the last
 * split always has the last file. Any given split will have adjacent files.
 * @author Richard McCreadie and Craig Macdonald
 * @since 2.2
 * @version $Revision: 1.2 $
 */
public class MultiFileCollectionInputFormat extends MultiFileInputFormat<Text, SplitAwareWrapper<Document>>
{

	/** logger for this class */
	protected static final Logger logger = Logger.getLogger(MultiFileCollectionInputFormat.class);
	
	@SuppressWarnings("unchecked")
	@Override
	/**
	 * Instantiates a FileCollectionRecordReader using the specified spit (which is
	 * assumed to be a MultiFileSplit.
	 * @param genericSplit contains files to be processed, assumed to be a MultiFileSplit
	 * @param job JobConf of this job
	 * @param reported To report progress
	 */
	public RecordReader<Text, SplitAwareWrapper<Document>> getRecordReader(
			InputSplit genericSplit, 
			JobConf job,
            Reporter reporter) 
		throws IOException 
	{
		reporter.setStatus(genericSplit.toString());
	    return new FileCollectionRecordReader(job, (PositionAwareSplit<MultiFileSplit>) genericSplit);
	}
	
	@Override
	/**
	 * Splits the input collection into
	 * sets of files where each Map task 
	 * gets about the same number of files
	 */
	public InputSplit[] getSplits(JobConf job, int numSplits) 
		throws IOException 
	{

		Path[] paths = FileInputFormat.getInputPaths(job);
		// HADOOP-1818: Manage splits only if there are paths
		if (paths.length == 0) 
		{
            return new InputSplit[0];
        }

		

		if (numSplits>paths.length) 
		{
			numSplits = paths.length;
		} 
		else if (numSplits<1) 
		{
			numSplits = 1;
		}
		logger.info("Allocating "+paths.length+ " files across "+numSplits +" map tasks");
		List<PositionAwareSplit<MultiFileSplit>> splits = new ArrayList<PositionAwareSplit<MultiFileSplit>>(numSplits);
		final int numPaths = paths.length;
		long[] lengths = new long[numPaths];
		long totLength = 0;
		final FileSystem fs = FileSystem.get(job);	
		for(int i=0; i<paths.length; i++) 
		{
			lengths[i] = fs.getFileStatus(paths[i]).getLen();
			totLength += lengths[i];
		}
		
		//we need to over-estimate using ceil, to ensure that the last split is not /too/ big
		final int numberOfFilesPerSplit = (int)Math.ceil((double)paths.length / (double)numSplits);
		
		int pathsUsed = 0;
		int splitnum = 0;
		MultiFileSplit mfs;
		// for each split except the last one (which may be smaller than numberOfFilesPerSplit)
		while(pathsUsed < numPaths)
		{
			/* caclulate split size for this task - usually numberOfFilesPerSplit, but
			 * less than this for the last split */
			final int splitSizeForThisSplit = numberOfFilesPerSplit + pathsUsed > numPaths
				? numPaths - pathsUsed
				: numberOfFilesPerSplit;
			//arrays of information for split
			Path[] splitPaths = new Path[splitSizeForThisSplit];
			long[] splitLengths = new long[splitSizeForThisSplit];
			
			//copy information for this split
			System.arraycopy(lengths, pathsUsed, splitLengths, 0, splitSizeForThisSplit);
			System.arraycopy(paths, pathsUsed, splitPaths, 0, splitSizeForThisSplit);
			//count the number of paths consumed
			pathsUsed += splitSizeForThisSplit;
			
			//make the actual split object
			//logger.info("New split of size " + splitSizeForThisSplit);
			mfs = new MultiFileSplit(job, splitPaths, splitLengths);
			splits.add(new PositionAwareSplit<MultiFileSplit>(mfs, splitnum));
			splitnum++;
		}

		if (!(pathsUsed==paths.length)) {
			throw new IOException("Number of used paths does not equal total available paths!");
		}
		return splits.toArray(new PositionAwareSplit[splits.size()]);    
	}

}

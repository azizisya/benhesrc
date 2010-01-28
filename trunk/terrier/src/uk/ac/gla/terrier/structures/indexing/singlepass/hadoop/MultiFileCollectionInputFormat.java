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
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Richard McCreadie <richardm{a.}dcs.gla.ac.uk> (original author)
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> 
 */
package uk.ac.gla.terrier.structures.indexing.singlepass.hadoop;

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
import org.apache.hadoop.mapred.MultiFileSplit;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;
import org.apache.log4j.Logger;

import uk.ac.gla.terrier.indexing.Document;
import uk.ac.gla.terrier.utility.Wrapper;

/**
 * Input Format Class for Hadoop Indexing. Splits the input collection into
 * sets of files where each Map task gets about the same number of files.
 * Files are assumed to be un-splittable and are not split.
 * @author Richard McCreadie and Craig Macdonald
 * @since 2.2
 * @version $Revision: 1.1 $
 */
public class MultiFileCollectionInputFormat extends MultiFileInputFormat<Text, Wrapper<Document>>
{

	/** logger for this class */
	protected static final Logger logger = Logger.getLogger(MultiFileCollectionInputFormat.class);
	
	@Override
	/**
	 * Instantiates a FileCollectionRecordReader using the specified spit (which is
	 * assumed to be a MultiFileSplit.
	 * @param genericSplit contains files to be processed, assumed to be a MultiFileSplit
	 * @param job JobConf of this job
	 * @param reported To report progress
	 */
	public RecordReader<Text, Wrapper<Document>> getRecordReader(
			InputSplit genericSplit, 
			JobConf job,
            Reporter reporter) 
		throws IOException 
	{
		reporter.setStatus(genericSplit.toString());
	    return new FileCollectionRecordReader(job, (MultiFileSplit) genericSplit);
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

		int pathsUsed = 0;

		if (numSplits>paths.length) 
		{
			numSplits = paths.length;
		} 
		else if (numSplits<1) 
		{
			numSplits = 1;
		}
		logger.info("Allocating "+paths.length+ " files across "+numSplits +" map tasks");
		List<MultiFileSplit> splits = new ArrayList<MultiFileSplit>(numSplits);
		long[] lengths = new long[paths.length];
		long totLength = 0;
		final FileSystem fs = FileSystem.get(job);	
		for(int i=0; i<paths.length; i++) 
		{
			//FileSystem fs = paths[i].getFileSystem(job); //done do we need to find the fs for each path
			lengths[i] = fs.getFileStatus(paths[i]).getLen();
			totLength += lengths[i];
		}
		final int numberOfFilesPerSplit = (int)(Math.floor(paths.length / numSplits));

		int index = 0;
		// for each split except the last one (which may be larger than numberOfFilesPerSplit)
		for(int i=0; i<numSplits-1; i++) 
		{
			Path[] splitPaths = new Path[numberOfFilesPerSplit];
			long[] splitLengths = new long[numberOfFilesPerSplit];
			for (int j=0; j<numberOfFilesPerSplit;j++) {
				splitPaths[j] = paths[index];
				splitLengths[j] = lengths[index];
				index++;
				pathsUsed++;
			}
			splits.add(new MultiFileSplit(job, splitPaths, splitLengths));
		}

		// Now do the last one containing remaining files
		int remainingFiles = paths.length-index;
		Path[] splitPaths = new Path[remainingFiles];
		long[] splitLengths = new long[remainingFiles];
		for (int j=0; j<remainingFiles;j++) {
			splitPaths[j] = paths[index];
			splitLengths[j] = lengths[index];
			index++;
			pathsUsed++;
		}
		splits.add(new MultiFileSplit(job, splitPaths, splitLengths)); 

		if (!(pathsUsed==paths.length)) {
			throw new IOException("Number of used paths does not equal total available paths!");
		}
		return splits.toArray(new MultiFileSplit[splits.size()]);    
	}

}

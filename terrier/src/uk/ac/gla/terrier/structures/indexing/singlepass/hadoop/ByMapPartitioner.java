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
 * The Original Code is ByMapPartitioner.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Richard McCreadie <richardm{a.}dcs.gla.ac.uk> (original author)
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> 
 */

package uk.ac.gla.terrier.structures.indexing.singlepass.hadoop;


import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.JobConfigurable;
import org.apache.hadoop.mapred.Partitioner;
import org.apache.hadoop.mapred.TaskID;

/**
 * Partitions the term postings lists from the map function,
 * such that the created indexes is partitioned evenly across
 * the reducers. This partitioner partitions by an even number of
 * maps, assuming that Map sizes are approximately equal.
 * @author Richard McCreadie and Craig Macdonald
 * @version $Revision: 1.1 $
 * @since 2.2
 */
public class ByMapPartitioner implements JobConfigurable, Partitioner<MapEmittedTerm, MapEmittedPostingList>{

	protected int numMapTasks = -1;
	public void configure(JobConf job)
	{
		numMapTasks = job.getNumMapTasks();
	}
	
	/**
	 * Forces each Map output to get its own reduce step
	 */
	public int getPartition(MapEmittedTerm key, MapEmittedPostingList value, int numPartitions) {
		final int mapNumber = getTaskID(value.getMap());
		final int partitionSize = (int) (Math.ceil( ((double)numMapTasks)/(double)numPartitions));
		return mapNumber / partitionSize;
	}

	protected static int getTaskID(String id)
	{
		return TaskID.forName(id).getId();
	}

}

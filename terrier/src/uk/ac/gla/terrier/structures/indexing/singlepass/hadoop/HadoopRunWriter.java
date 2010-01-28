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
 * The Original Code is HadoopRunWriter.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Richard McCreadie <richardm{a.}dcs.gla.ac.uk> (original author)
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
 *   
 */
package uk.ac.gla.terrier.structures.indexing.singlepass.hadoop;

import java.io.IOException;

import org.apache.hadoop.mapred.OutputCollector;

import uk.ac.gla.terrier.compression.MemorySBOS;
import uk.ac.gla.terrier.structures.indexing.singlepass.Posting;
import uk.ac.gla.terrier.structures.indexing.singlepass.RunWriter;

/** RunWriter for the MapReduce indexer 
 * @author Richard McCreadie and Craig Macdonald 
 * @version $Revision: 1.1 $ */
public class HadoopRunWriter extends RunWriter {
	/** output collector of Map task */
	protected OutputCollector<MapEmittedTerm, MapEmittedPostingList> outputCollector = null;
	/** map task id that is being flushed */
	protected String mapId;
	/** flushNo is the number of times this map task is being flushed */
	protected int flushNo;
	
	/** Create a new HadoopRunWriter, specifying the output collector of the map task
	 * the run number and the flush number.
	 * @param _outputCollector where to emit the posting lists to
	 * @param _mapId the task id of the map currently being processed
	 * @param _flushNo the number of times that this map task has flushed
	 */
	public HadoopRunWriter(OutputCollector<MapEmittedTerm, MapEmittedPostingList> _outputCollector,
			String _mapId, int _flushNo)
	{
		this.outputCollector = _outputCollector;
		this.mapId = _mapId;
		this.flushNo = _flushNo;
	}
	
	@Override
	public void beginWrite(int maxSize, int size) throws IOException
	{}
	
	/** Write the posting to the output collector
	 */
	@Override
	public void writeTerm(final String term, final Posting post) throws IOException
	{	
		final MemorySBOS Docs = post.getDocs();
		Docs.pad();
		//get the posting array buffer
		byte[] buffer = new byte[Docs.getMOS().getPos()+1];
		System.arraycopy(Docs.getMOS().getBuffer(), 0, 
				buffer, 0, 
				Math.min(Docs.getMOS().getBuffer().length, Docs.getMOS().getPos()+1));
		
		//emit the term and its posting list
		outputCollector.collect(
				MapEmittedTerm.create_TextPlus(term, mapId, flushNo), 
				MapEmittedPostingList.create_Hadoop_WritableRunPostingData(
						mapId,
						flushNo, 
						buffer,
						post.getDocF(), post.getTF()));
	}
	
	@Override
	public void finishWrite() throws IOException
	{}
	
	/** This RunWriter does not require that the output be sorted.
	  */
	@Override
	public boolean writeSorted()
	{
		return false;
	}
}

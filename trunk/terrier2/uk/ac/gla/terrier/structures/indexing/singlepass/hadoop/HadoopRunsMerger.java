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
 * The Original Code is HadoopRunsMerger.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Richard McCreadie <richardm{a.}dcs.gla.ac.uk> (original author)
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
 *   
 */
package uk.ac.gla.terrier.structures.indexing.singlepass.hadoop;

import java.io.IOException;
import java.util.LinkedList;
import java.util.ListIterator;

import uk.ac.gla.terrier.structures.LexiconOutputStream;
import uk.ac.gla.terrier.structures.indexing.singlepass.PostingInRun;
import uk.ac.gla.terrier.structures.indexing.singlepass.RunIterator;
import uk.ac.gla.terrier.structures.indexing.singlepass.RunIteratorFactory;
import uk.ac.gla.terrier.structures.indexing.singlepass.RunsMerger;

public class HadoopRunsMerger extends RunsMerger {

	protected LinkedList<MapData> mapData = null;
	
	public HadoopRunsMerger(RunIteratorFactory _runsSource) {
		super(_runsSource);
	}

	/**
	 * Alternate Merge operation for merging a linked list of runs of the form
	 * Hadoop_MapData. This routine merges the multiple runs
	 * created during the map process of hadoop indexing as such it corrects for
	 * Document id 'shift' caused by random splitting of runs due to flushing and
	 * map splitting.
	 * @param _mapData - information about the number of documents per map and run. One element for every map.
	 * @throws IOException
	 */
	public void beginMerge(LinkedList<MapData> _mapData)
	{
		mapData = _mapData;
	}

	public void endMerge(LexiconOutputStream lexStream) {}
	
	public void mergeOne(LexiconOutputStream lexStream) throws Exception
	{	
		int maxDF = 0;
		RunIterator run = runsSource.createRunIterator(-1);
		HadoopRunPostingIterator _run = (HadoopRunPostingIterator)run;
		
		boolean firstMap = true;
		lastTermWritten = null;
		lastFreq = 0;
		lastDocFreq= 0;
		// for each run in the list 
		int counter = 0;
		//for one term: for each set of postings for that term
		while (run.hasNext()) {
			
			PostingInRun posting = run.next();
			lastTermWritten = posting.getTerm();
			
			//
			if (posting.getDf() > maxDF) 
				maxDF = posting.getDf();
			
			int NumPreDocs = 0;
			MapData correctHRD = null;			
			for (MapData tempHRD : mapData)
			{
			//for (int i = 0; i<_run.getMapNo(); i++) { 
				if (tempHRD.getMap().equals(_run.getMapNo()))
				{
					correctHRD = tempHRD;
					break;
				}
				NumPreDocs += tempHRD.getMapDocs();
			}
			if (correctHRD == null)
				throw new IOException("Did not find map data for "+ _run.getMapNo());
			
			// Add the FlushShift
			int currentFlushDocs=0;
			
			ListIterator<Integer> LI = correctHRD.getFlushDocSizes().listIterator(0);
			int currentFlush =0;
			while (currentFlush<run.getRunNo()) {
				currentFlushDocs += LI.next(); 
				currentFlush++;
			}
	
			if (firstMap) {
				lastDocument = posting.append(bos, -1, NumPreDocs+currentFlushDocs);
				firstMap = false;
			}
			else {
				lastDocument = posting.append(bos, lastDocument, NumPreDocs+currentFlushDocs);
			}
			lastFreq += posting.getTF();
			lastDocFreq += posting.getDf();
			counter++;
		}
		lexStream.writeNextEntry(lastTermWritten, currentTerm++, lastDocFreq, lastFreq, this.getByteOffset(), (byte)this.getBitOffset());
		numberOfPointers += lastDocFreq;
	}

}

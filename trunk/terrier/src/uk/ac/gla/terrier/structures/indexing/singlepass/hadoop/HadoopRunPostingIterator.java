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
 * The Original Code is HadoopRunPostingIterator.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original author)
 *   
 */
package uk.ac.gla.terrier.structures.indexing.singlepass.hadoop;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

import uk.ac.gla.terrier.compression.BitInputStream;
import uk.ac.gla.terrier.structures.indexing.singlepass.PostingInRun;
import uk.ac.gla.terrier.structures.indexing.singlepass.RunIterator;

public class HadoopRunPostingIterator extends RunIterator {

	/** Runs To Be Merged */
	protected Iterator<MapEmittedPostingList> postingIterator; 
	
	/** Map number */
	protected String mapNo;
	/** Term that we're processing */
	protected String term;

	/** Constructs a new RunPostingIterator.
	  * @param postingClass is the name of the class to use to read the postings
	  * @param runNo is the number of the run
	  * @param _postingiterator is the iterator of reduce input data that we are going to loop through
	  * @param _term is the term that this iterator is operating on
	  */
	public HadoopRunPostingIterator(
			Class<? extends PostingInRun> postingClass,
			int runNo,
			Iterator<MapEmittedPostingList> _postingiterator,
			String _term) 
	throws Exception 
	{
		super(postingClass, runNo);
		postingIterator = _postingiterator;
		term = _term;
		createPosting();
	}

	/** Move to the next posting */	
	@Override
	public boolean hasNext() {
		return postingIterator.hasNext();
	}

	/** Return the next PostingInRun */
	@Override
	public PostingInRun next() {
		try{
			/** Current Posting List */
			final MapEmittedPostingList post = postingIterator.next();
			posting.setTerm(term);
			posting.setPostingSource(new BitInputStream(new ByteArrayInputStream(post.getArray())));
			posting.setDf(post.getDocumentFreq());
			posting.setTF(post.getTermFreq());
			mapNo = post.getMap();
			runNo = post.getRun();
		} catch (IOException ioe) {
			throw new Error(ioe);
		}
		return posting;
	}

	/** Returns the map that the current posting came from */
	public String getMapNo() { return mapNo; }
}

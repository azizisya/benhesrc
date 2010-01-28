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
 * The Original Code is MemoryPostings.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Roi Blanco (rblanc{at}@udc.es)
 *   Craig Macdonald (craigm{at}dcs.gla.ac.uk)
 */
package uk.ac.gla.terrier.structures.indexing.singlepass;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.structures.indexing.DocumentPostingList;

/**
 * Class for handling Simple posting lists in memory while indexing.
 * @author Roi Blanco
 */
public class MemoryPostings {
	/** logger to use in this class */
	protected static Logger logger = Logger.getRootLogger(); 
	
	/** Hashmap indexed by the term, containing the posting lists*/
	protected Map<String, Posting> postings = new HashMap<String, Posting>();
	/** The number of documents for any term in this run */
	protected int maxSize = 1;
	/** Number of pointers (<term,document> tuples in memory in this run. */
	protected long numPointers = 0;
	
	/**
	 * Add the terms in a DocumentPostingList to the postings in memory.
	 * @param docPostings DocumentPostingList containing the term information for the denoted document.
	 * @param docid Current document Identifier. 
	 * @throws IOException if an I/O error occurs.
	 */
	public void addTerms(DocumentPostingList docPostings, int docid) throws IOException {
		for (String term : docPostings.termSet())
			add(term, docid, docPostings.getFrequency(term));
	}
	
	/**
	 * Adds an occurrence of a term in a document to the posting in memory.
	 * @param term String representing the term.
	 * @param doc int containing the document identifier.
	 * @param frequency int containing the frequency of the term in the document.
	 * @throws IOException if an I/O error occurs.
	 */
	public void add(String term, int doc, int frequency) throws IOException{
		Posting post;
		numPointers++;
		if((post = postings.get(term)) != null) {	
			post.insert(doc, frequency);
			
			final int df = post.getDocF();
			if(df > maxSize) maxSize = df; 
		}
		else{
			post = new Posting();
			post.writeFirstDoc(doc, frequency);			
			postings.put(term, post);
		}
	}
	
	/**
	 * Triggers the writing of the postings in memory to disk. 
	 * Uses the default RunWriter, writing to the specified files.
	 * @param file name of the file to write the postings.
	 * @throws IOException if an I/O error occurs.
	 */
	public void finish(String[] file) throws IOException{	
		finish(new RunWriter(file[0], file[1]));
	}
	
	/** Triggers the writing of the postings in memory to the specified 
	 * RunWriter. If the RunWriter requires that terms are written in order,
	 * then this will happen.
	 * @param runWriter
	 * @throws IOException
	 */
	public void finish(RunWriter runWriter) throws IOException {
		logger.debug("Writing run "+runWriter.toString());
		//only sort the postings if required by the RunWriter
		writeToWriter(runWriter, runWriter.writeSorted() 
				? new TreeMap<String, Posting>(postings)
				: postings);
		logger.debug(" done");
	}
	
	/** Returns the number of terms in this posting list. 
	 * @return the number of posting lists in memory.
	 */
	public int getSize(){
		return postings.size();
	}
	
	/** Returns the number of pointers in this posting list. Pointers
	 * are unique (term,docid) tuples.
	 * @return the number of pointers in memory.
	 */
	public long getPointers()
	{
		return numPointers;
	}
	
	/**
	 * Writes the contents of the postings in memory to disk.
	 * @param writer The RunWriter to write the postings to.
	 * @param postings the Map<String,Posting> containing the posting lists in memory.
	 * @throws IOException if an I/O error occurs.
	 */
	private void writeToWriter(RunWriter writer, Map<String, Posting> postings) throws IOException{
		if (postings.size() != 0){
			writer.beginWrite(maxSize, postings.size());
			for( Entry<String,Posting> entry : postings.entrySet())
			{
				writer.writeTerm(entry.getKey(), entry.getValue());					
			}
		}
		writer.finishWrite();
	}
}


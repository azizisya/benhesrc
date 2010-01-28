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
 * The Original Code is FieldsMemoryPostings.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Roi Blanco (rblanc{at}@udc.es)
 *   Craig Macdonald (craigm{at}dcs.gla.ac.uk)
 */
package uk.ac.gla.terrier.structures.indexing.singlepass;

import java.io.IOException;

import uk.ac.gla.terrier.structures.indexing.DocumentPostingList;
/**
 * Class for handling posting lists containing field information in memory while indexing.
 * @author Roi Blanco
 *
 */
public class FieldsMemoryPostings extends MemoryPostings{
	
	public void addTerms(DocumentPostingList docPostings, int docid) throws IOException{
		for (String term : docPostings.termSet())
			add(term, docid, docPostings.getFrequency(term), docPostings.getFields(term));
	}
	
	/**
	 * Adds an occurrence of a term in a document to the posting in memory.
	 * @param term String representing the term.
	 * @param doc int containing the document identifier.
	 * @param frequency int containing the frequency of the term in the document.
	 * @param fieldScore int containing the field score for the term in the document.
	 * @throws IOException if an I/O error occurs.
	 */
	public void add(String term, int doc, int frequency, int fieldScore) throws IOException{
		FieldPosting post;
		if((post = (FieldPosting) postings.get(term)) != null) {						
			post.insert(doc, frequency, fieldScore);
			int tf = post.getTF();
			// Update the max size
			if(maxSize < tf) maxSize = tf; 
		}
		else{
			post = new FieldPosting();
			post.writeFirstDoc(doc, frequency, fieldScore);			
			postings.put(term,post);
		}
		numPointers++;
	}	
}

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
 * The Original Code is BlockFieldMemoryPostings.java.
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

import uk.ac.gla.terrier.structures.indexing.BlockDocumentPostingList;
import uk.ac.gla.terrier.structures.indexing.DocumentPostingList;
/**
 * Class for handling posting lists containing block and field information in memory while indexing.
 * @author Roi Blanco
 *
 */
public class BlockFieldMemoryPostings extends BlockMemoryPostings{
	
	public void addTerms(DocumentPostingList _docPostings, int docid) throws IOException {
		BlockDocumentPostingList docPostings = (BlockDocumentPostingList)  _docPostings;
		for (String term : docPostings.termSet())
			add(term, docid, docPostings.getFrequency(term) , docPostings.getFields(term), docPostings.getBlocks(term));
	}
	
	
	public void add(String term, int doc, int frequency, int fieldScore, int[] blockids)  throws IOException{
		BlockFieldPosting post;	
		if((post =(BlockFieldPosting) postings.get(term)) != null) {						
			post.insert(doc, frequency, fieldScore, blockids);
			int tf = post.getTF();			
			if(maxSize < tf) maxSize = tf; 
		}
		else{
			post = new BlockFieldPosting();
			post.writeFirstDoc(doc, frequency, fieldScore, blockids);			
			postings.put(term,post);
		}
		numPointers++;
	}
}

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
 * The Original Code is Posting.java.
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

import uk.ac.gla.terrier.compression.MemorySBOS;

/**
 * Class representing a simple posting list in memory.
 * It keeps the information for <code>TF, Nt</code>, and the sequence <code>[doc, tf]</code>
 * @author Roi Blanco
 *
 */
public class Posting {
	
	/** The term frequency */
	protected int TF;
	/** The document frequency */
	protected int Nt;
	/** The compressed in-memory object holding the sequence doc_id, idf*/	
	protected MemorySBOS docIds;
	/** Last document inserted in the posting */
	protected int lastInt = 0;
		
	/**
	 * Writes the first document in the posting list.
	 * @param docId the document identifier.
	 * @param freq the frequency of the term in the document.
	 * @throws IOException if an I/O error ocurrs.
	 */	
	public void writeFirstDoc(int docId, int freq) throws IOException{		
		docIds = new MemorySBOS();
		TF = freq;			
		Nt = 1;
		docIds.writeGamma(docId + 1);
		docIds.writeGamma(freq);
		lastInt = docId;
	}
	
	/**
	 * Inserts a new document in the posting list. Document insertions must be done
	 * in order.  
	 * @param doc the document identifier.
	 * @param freq the frequency of the term in the document.
	 * @return the updated term frequency.
	 * @throws IOException if and I/O error occurs.
	 */
	public int insert(int doc, int freq) throws IOException{		
		Nt++;
		TF += freq;
		docIds.writeGamma(doc - lastInt);
		docIds.writeGamma(freq);
		lastInt = doc;					
		return TF;
	}

	/**
	 * @return the term frequency of the term in the run
	 */
	public int getTF(){
		return TF;
	}
	
	/**
	 * @return the document data compressed object.
	 */
	public MemorySBOS getDocs(){
	    return docIds;
	}	
	
	/**
	 * Sets the term frequency in the run.
	 * @param tf the term frequency.
	 */
	public void setTF(int tf){
		this.TF = tf;
	}
	
	/**
	 * Sets the document data compressed object.
	 * @param docs
	 */
	public void setDocs(MemorySBOS docs){
		this.docIds = docs;
	}

	/**
	 * @return the document frequency - the number of documents this term occurs in
	 */
	public int getDocF() {
		return Nt;
	}

	/**
	 * Set the document frequency the number of documents this term occurs in.
	 * @param docF the document frequency.
	 */
	public void setDocF(int docF) {
		this.Nt = docF;
	}	
}

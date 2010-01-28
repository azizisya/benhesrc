
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
 * The Original Code is BlockDirectIndex.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk> (original author)
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.structures;

/** Contains all the information about one entry in the Lexicon. 
  * Created to make thread-safe lookups in the Lexicon easier. */
public class LexiconEntry {

	/** Create an empty LexiconEntry */
	public LexiconEntry(){}

	/** Create a lexicon entry with the following information.
	  * @param t the term 
	  * @param tid the term id
	  * @param n_t the number of documents the term occurs in (document frequency)
	  * @param TF the total count of therm t in the collection
	  */
	public LexiconEntry(String t, int tid, int n_t, int TF)
	{
		this.term =t;
		this.termId = tid;
		this.n_t = n_t;
		this.TF = TF;
	}

	/** increment this lexicon entry by another */
	public void add(LexiconEntry le)
	{
		this.n_t += le.n_t;
		this.TF  += le.TF;
	}

	/** alter this lexicon entry to subtract another lexicon entry */
	public void subtract(LexiconEntry le)
	{
		this.n_t -= le.n_t;
		this.TF  -= le.TF;
	}

	/** the term of this entry */	
	public String term;
	/** the termid of this entry */
	public int termId;
	/** the number of document that this entry occurs in */
	public int n_t;
	/** the total number of occurrences of the term in the index */
	public int TF;
	/** the start offset of the entry in the inverted index */
	public long startOffset;
	/** the start bit offset of the entry in the inverted index */
	public byte startBitOffset;
	/** the end offset of the entry in the inverted index */
	public long endOffset;
	/** the end bit offset of the entry in the inverted index */
	public byte endBitOffset;

	/** returns a string representation of this lexicon entry */	
	public String toString() {
		return term + " " + termId + " " + n_t + " " + TF + " " + startOffset + " " + startBitOffset + " " + endOffset + " " + endBitOffset;
	}
}

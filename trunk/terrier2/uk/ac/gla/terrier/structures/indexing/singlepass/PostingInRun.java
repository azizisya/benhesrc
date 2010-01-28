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
 * The Original Code is PostingInRun.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original author)
 *   
 */
package uk.ac.gla.terrier.structures.indexing.singlepass;

import java.io.IOException;

import uk.ac.gla.terrier.compression.BitIn;
import uk.ac.gla.terrier.compression.BitOut;

/** Base class for PostingInRun classes */
public abstract class PostingInRun {

	/** source for postings to be read from */
	protected BitIn postingSource;
	/** tf for the current posting */
	protected int termTF;
	/** Current term */
	protected String term;
	/** Document frequency */
	protected int termDf;

	public PostingInRun() {
		super();
	}

	/**
	 * @return the document frequency for the term.
	 */
	public int getDf() {
		return termDf;
	}

	/**
	 * Setter for the document frequency.
	 * @param df int with the new document frequency.
	 */
	public void setDf(int df) {
		this.termDf = df;
	}

	/**
	 * @return The term String in this posting list.
	 */
	public String getTerm() {
		return term;
	}

	/**
	 * Setter for the term.
	 * @param term String containing the term for this posting list.
	 */
	public void setTerm(String term) {
		this.term = term;
	}

	/**
	 * @return the term frequency.
	 */
	public int getTF() {
		return termTF;
	}

	/**
	 * Setter for the term frequency.
	 * @param tf the new term frequency.
	 */
	public void setTF(int tf) {
		this.termTF = tf;
	}

	/** Set where the postings should be read from */
	public void setPostingSource(BitIn source) {
		postingSource = source;
	}

	/**
	 * Writes the document data of this posting to a {@link uk.ac.gla.terrier.compression.BitOut} 
	 * It encodes the data with the right compression methods.
	 * The stream is written as <code>d1, idf(d1) , d2 - d1, idf(d2)</code> etc.
	 * @param bos BitOut to be written.
	 * @param last int representing the last document written in timport uk.ac.gla.terrier.structures.indexing.singlepass.RunReader;his posting.
	 * @return The last posting written.
	 */
	public abstract int append(BitOut bos, int last, int runShift) throws IOException;

	/**
	 * Writes the document data of this posting to a {@link uk.ac.gla.terrier.compression.BitOut} 
	 * It encodes the data with the right compression methods.
	 * The stream is written as <code>d1, idf(d1) , d2 - d1, idf(d2)</code> etc.
	 * @param bos BitOut to be written.
	 * @param last int representing the last document written in this posting.
	 * @return The last posting written.
	 */
	public int append(BitOut bos, int last) throws IOException {
		return append(bos, last, 0);
	}


}
/*
 * Terrier - Terabyte Retriever 
 * Webpage: http://terrier.org/
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
 * The Original Code is BasicTermStatsLexiconEntry.java
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original contributor)
 */
package org.terrier.structures;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class BasicTermStatsLexiconEntry extends LexiconEntry {
	private static final long serialVersionUID = 1L;
	protected int n_t;
	protected int TF;
	protected int termId;
	
	public BasicTermStatsLexiconEntry() {}
	
	public BasicTermStatsLexiconEntry(int _TF, int _n_t, int _termId)
	{
		TF = _TF;
		n_t = _n_t;
		termId = _termId;
	}
	
	public int getDocumentFrequency() {
		return n_t;
	}
	
	public void setDocumentFrequency(int _n_t) {
		n_t = _n_t;
	}

	public int getFrequency() {
		return TF;
	}
	
	public void setFrequency(int _TF) {
		TF = _TF;
	}

	public int getTermId() {
		return termId;
	}
	
	public void setTermId(int _termId) {
		termId = _termId;
	}
	
	public void setAll(int _TF, int _n_t, int _termId) {
		TF = _TF;
		n_t = _n_t;
		termId = _termId;
	}

	public int getNumberOfEntries() {
		return n_t;
	}

	public byte getOffsetBits() {
		return 0;
	}

	public long getOffset() {
		return 0;
	}
	
	public void setOffset(long bytes, byte bits)
	{
	}
	
	public void setBitIndexPointer(BitIndexPointer pointer) {
		
	}

	public void setOffset(BitFilePosition pos) {
		
	}

	public void readFields(DataInput in) throws IOException {
		TF = in.readInt();
		n_t = in.readInt();
		termId = in.readInt();
	}

	public void write(DataOutput out) throws IOException {
		out.writeInt(TF);
		out.writeInt(n_t);
		out.writeInt(termId);
	}

	public void add(EntryStatistics le) {
		TF += le.getFrequency();
		n_t += le.getDocumentFrequency();
	}

	public void subtract(EntryStatistics le) {
		this.n_t -= le.getDocumentFrequency();
		this.TF  -= le.getFrequency();
	}

	public void setNumberOfEntries(int n) {
	}

	public String pointerToString() {
		return "";
	}

	public void setPointer(Pointer p) {
		return;
	}

	@Override
	public void setStatistics(int n_t, int TF) {
		this.n_t = n_t;
		this.TF = TF;
	}

}

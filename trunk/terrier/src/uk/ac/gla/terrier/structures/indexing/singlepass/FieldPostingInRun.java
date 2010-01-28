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
 * The Original Code is FieldPostingInRun.java.
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

import uk.ac.gla.terrier.compression.BitOut;
import uk.ac.gla.terrier.utility.FieldScore;
/** Class holding the information for a posting list read
 * from a previously written run at disk. Used in the merging phase of the Single pass inversion method.
 * This class knows how to append itself to a {@link uk.ac.gla.terrier.compression.BitOut} and it
 * represents a posting with field information <code>(tf, df, [docid, idf, fieldScore])</code>
 * @author Roi Blanco
 *
 */
public class FieldPostingInRun extends SimplePostingInRun {
	/** The number of different fields that are used for indexing field information.*/		
	protected static final int fieldTags = FieldScore.FIELDS_COUNT;
	/**
	 * Constructor for the class.
	 */
	public FieldPostingInRun() {
		super();
	}
	/**
	 * Writes the document data of this posting to a {@link uk.ac.gla.terrier.compression.BitOut} 
	 * It encodes the data with the right compression methods.
	 * The stream is written as d1, idf(d1), fieldScore(d1) , d2 - d1, idf(d2), fieldScore(d2) etc.
	 * @param bos BitOut to be written.
	 * @param last int representing the last document written in this posting.
	 * @param runShift amount of delta to apply to the first posting read.
	 * @return The docid of the last posting written.
	 */
	public int append(BitOut bos, int last, int runShift)  throws IOException{
		int current = runShift - 1;
		for(int i = 0; i < termDf; i++){
			int docid = postingSource.readGamma() + current;
			bos.writeGamma(docid - last);
			bos.writeUnary(postingSource.readGamma());
			bos.writeBinary(fieldTags, postingSource.readBinary(fieldTags));
			current = last = docid;		
		}
		try{
			postingSource.align();
		}catch(Exception e){
			// last posting
		}
		return last;
	}
	
}

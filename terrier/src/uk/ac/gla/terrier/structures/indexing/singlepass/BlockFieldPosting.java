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
 * The Original Code is BlockFieldPosting.java.
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

import uk.ac.gla.terrier.utility.FieldScore;
/**
 * Class representing a posting list in memory containing fields and block iformation.
 * It keeps the information for <code>tf, df, field</code> and the sequence <code>[doc, idf, bockNo [blockId]]</code>
 * @author Roi Blanco
 *
 */
public class BlockFieldPosting extends BlockPosting{
	/** The number of different fields that are used for indexing field information.*/	
	protected static final int fieldTags = FieldScore.FIELDS_COUNT;
	
	public void  writeFirstDoc(final int doc, final int frequency, int fieldScore, int[] blockids) throws IOException{
		super.writeFirstDoc(doc, frequency);
		if (fieldTags> 0)
			docIds.writeBinary(fieldTags, fieldScore);
		final int blockCount = blockids.length;
		
		docIds.writeUnary(blockCount+1);
		if (blockCount > 0)
		{
			docIds.writeGamma(blockids[0]+1);
			for (int i=1; i<blockCount; i++) {
				docIds.writeGamma(blockids[i] - blockids[i-1]);
			}
		}
	}
	
	public int insert(final int doc, final int freq, final int fieldScore, final int[] blockids) throws IOException{
		int c = insert(doc, freq);
		if (fieldTags> 0)
			docIds.writeBinary(fieldTags, fieldScore);
		final int blockCount = blockids.length;
		
		docIds.writeUnary(blockCount+1);
		if (blockCount > 0)
		{
			docIds.writeGamma(blockids[0]+1);
			for (int i=1; i<blockCount; i++) {
				docIds.writeGamma(blockids[i] - blockids[i-1]);
			}
		}
		return c;
	}
}


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
 * The Original Code is BlockLexiconMap.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original author)
 *   
 */
package uk.ac.gla.terrier.structures.indexing;

import uk.ac.gla.terrier.structures.LexiconOutputStream;
import uk.ac.gla.terrier.structures.BlockLexiconOutputStream;
import gnu.trove.*;
import java.util.Arrays;
import java.io.IOException;
import uk.ac.gla.terrier.utility.TermCodes;

/** LexiconMap implementation that also keeps track of the number of blocks that a term occurrs in.
  * This is useful for sizing the block inverted index */
public class BlockLexiconMap extends LexiconMap
{
	/** Total number of blocks in this index */
	protected long numberOfBlocks = 0;
	/** Mapping term to blocks */
	protected final TObjectIntHashMap<String> blockFreqs = new TObjectIntHashMap<String>();
	/**
	 * Inserts a new term in the lexicon map.
	 * @param term The term to be inserted.
	 * @param tf The id of the term.
	 */
	public void insert(final String term, final int tf, final int blockfreq)
	{
		tfs.adjustOrPutValue(term, tf, tf);
		nts.adjustOrPutValue(term, 1 , 1);
		blockFreqs.put(term, blockfreq);
		numberOfPointers++;
		numberOfBlocks+=blockfreq;
	}

	/** Clear the lexicon map */
	public void clear()
	{
		super.clear();
		blockFreqs.clear();
	}

	/** Inserts all the terms from a document posting
	  * into the lexicon map
	  * @param _doc The postinglist for that document - must be a instance of BlockDocumentPostingList.
	  */
	public void insert(final DocumentPostingList _doc)
	{
		final BlockDocumentPostingList doc = (BlockDocumentPostingList)_doc;
		doc.occurrences.forEachEntry( new TObjectIntProcedure<String>() {
				public boolean execute(final String t, final int tf)
				{
					tfs.adjustOrPutValue(t, tf, tf);
					nts.adjustOrPutValue(t, 1 , 1);
					numberOfPointers++;
					final int bf = doc.term_blocks.get(t).size();
					blockFreqs.adjustOrPutValue(t, bf, bf);
					numberOfBlocks+=bf;
					return true;
				}
			});
	}

	/** Stores the lexicon map to a lexicon stream as a sequence of entries.
	  * @param _lexiconStream The lexicon output stream to store to. */
	public void storeToStream(final LexiconOutputStream _lexiconStream) throws IOException {
		final BlockLexiconOutputStream lexiconStream = (BlockLexiconOutputStream)_lexiconStream;
		final byte zerob = (byte)0;
		final long zerol = (long)0;
		final String[] terms = tfs.keys(new String[0]);
		Arrays.sort(terms);
		for (String t : terms)
		{
			lexiconStream.writeNextEntry(t, TermCodes.getCode(t), nts.get(t), tfs.get(t), blockFreqs.get(t), zerol, zerob);
		}
	}

}

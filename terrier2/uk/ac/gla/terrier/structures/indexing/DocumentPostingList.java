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
 * The Original Code is DocumentPostingList.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original author)
 *   
 */

package uk.ac.gla.terrier.structures.indexing;

import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntProcedure;
import uk.ac.gla.terrier.sorting.HeapSortInt;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.TermCodes;
/** Represents the postings of one document. Uses HashMaps internally.
  * <p>
  * <b>Properties:</b><br>
  * <ul><li><tt>indexing.avg.unique.terms.per.doc</tt> - number of unique terms per doc on average, used to tune the initial 
  * size of the haashmaps used in this class.</li></ul>
  */
public class DocumentPostingList {
	/** number of unique terms per doc on average, used to tune the initial size of the haashmaps used in this class. */
	protected static final int AVG_DOCUMENT_UNIQUE_TERMS =
		Integer.parseInt(ApplicationSetup.getProperty("indexing.avg.unique.terms.per.doc", "120"));

	/** length of the document so far. Sum of the term frequencies inserted so far. */
	protected int documentLength = 0;

	/** mapping term to tf mapping */	
	protected final TObjectIntHashMap<String> occurrences = new TObjectIntHashMap<String>(AVG_DOCUMENT_UNIQUE_TERMS);
	/** mapping term to field bitset */
	protected final TObjectIntHashMap<String> term_fields = new TObjectIntHashMap<String>(AVG_DOCUMENT_UNIQUE_TERMS);
	/** number of fields in this index */
	protected final int fieldCount;

	/** Make a new postings list for a document. No fields */	
	public DocumentPostingList()
	{
		this.fieldCount = 0;
	}
	
	/** Make a new postings list for a document, with the specified number of fields
	  * @param fieldCount number of fields marked in this index */
	public DocumentPostingList(int fieldCount)
	{
		this.fieldCount = fieldCount;
	}
	
	public String[] termSet()
	{
		return occurrences.keys(new String[0]);
	}
	
	public int getFrequency(String term)
	{
		return occurrences.get(term);
	}
	
	public int getFields(String term)
	{
		return term_fields.get(term);
	}

	/** Removes all postings from this document */
	public void clear()
	{
		occurrences.clear();
		term_fields.clear();
	}

	/** Returns the total number of tokens in this document */	
	public int getDocumentLength()
	{
		return documentLength;
	}

	/** Returns the number of unique terms in this document. */
	public int getNumberOfPointers()
	{
		return occurrences.size();
	}
	/** Insert a term into the posting list of this document 
	  * @param term the Term being inserted */
	public void insert(final String term)
	{
		occurrences.adjustOrPutValue(term,1,1);
		documentLength++;
	}
	
	/** Insert a term into the posting list of this document
	  * @param tf frequency
      * @param term the Term being inserted */
    public void insert(final int tf, final String term)
    {
        occurrences.adjustOrPutValue(term,tf,tf);
        documentLength++;
    }

	/** Insert a term into the posting list of this document, in the given field, with the given frequency
	  * @param tf frequency of the term in this document
	  * @param term String form of term
	  * @param fieldNum fieldNumber it occurrs in */
	public void insert(final int tf, final String term, final int fieldNum)
	{
		occurrences.adjustOrPutValue(term,tf,tf);
		if (fieldNum > 0)
		{
			final int InScore = 1<<(fieldCount - fieldNum);
			final int ExScore = term_fields.get(term);
			term_fields.put(term, InScore|ExScore);
		}
		documentLength+=tf;
	}
	/**  Insert a term into the posting list of this document, in the given field
	  * @param term the Term being inserted
	  * @param fieldNum the id of the field that the term was found in */
	public void insert(final String term, final int fieldNum)
	{
		occurrences.adjustOrPutValue(term,1,1);
		if (fieldNum > 0)
		{
			final int InScore = 1<<(fieldCount - fieldNum);
			final int ExScore = term_fields.get(term);
			term_fields.put(term, InScore|ExScore);
		}
		documentLength++;
	}

	/**  Insert a term into the posting list of this document, in the given field
	  * @param term the Term being inserted
	  * @param fieldNums the ids of the fields that the term was found in */
	public void insert(final String term, final int[] fieldNums)
	{
		occurrences.adjustOrPutValue(term,1,1);
		final int l = fieldNums.length; 
		int InScore = term_fields.get(term);
		for(int i=0;i<l;i++)
		{
			if (fieldNums[i] > 0)
				InScore |= 1<<(fieldCount - fieldNums[i]);
		}
		term_fields.put(term, InScore);
		documentLength++;
	}

	/**  Insert a term into the posting list of this document, in the given field
	  * @param tf the frequency of the term
	  * @param term the Term being inserted
	  * @param fieldNums the ids of the fields that the term was found in */
	public void insert(final int tf, final String term, final int[] fieldNums)
	{
		occurrences.adjustOrPutValue(term,tf,tf);
		final int l = fieldNums.length;
		int InScore = term_fields.get(term);
		for(int i=0;i<l;i++)
		{
			if (fieldNums[i] > 0)
				InScore |= 1<<(fieldCount - fieldNums[i]);
		}
		term_fields.put(term, InScore);
		documentLength+=tf;
	}

	/** returns the postings suitable to be written into the direct index */
	public int[][] getPostings()
	{
		final int termCount = occurrences.size();
		final int[] termids = new int[termCount];
		final int[] tfs = new int[termCount];
		final int[] fields = new int[termCount];
		if (fieldCount > 0)
		{
			occurrences.forEachEntry( new TObjectIntProcedure<String>() {
				int i=0;
				public boolean execute(final String a, final int b)
				{
					termids[i] = TermCodes.getCode(a);
					tfs[i] = b;
					fields[i++] = term_fields.get(a);
					return true;
				}
			});	
			HeapSortInt.ascendingHeapSort(termids, tfs, fields);
			return new int[][]{termids, tfs, fields};
		}
		else
		{
			occurrences.forEachEntry( new TObjectIntProcedure<String>() { 
				int i=0;
				public boolean execute(final String a, final int b)
				{
					termids[i] = TermCodes.getCode(a);
					tfs[i++] = b;
					return true;
				}
			});
			HeapSortInt.ascendingHeapSort(termids, tfs);
			return new int[][]{termids, tfs};	
		}
	}
	
}

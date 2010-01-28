/*
 * Terrier - Terabyte Retriever 
 * Webpage: http://ir.dcs.gla.ac.uk/terrier 
 * Contact: terrier{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * Information Retrieval Group
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
 * The Original Code is InvertedIndex.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Douglas Johnson <johnsoda{a.}dcs.gla.ac.uk> (original author)
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk> 
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.structures;
import gnu.trove.TIntObjectHashMap;
/**
 * This class implements a non-lossy cache of an InvertedIndex
 * @author Craig Macdonald
 * @version $Revision: 1.3 $
 */
public class CachingInvertedIndex extends InvertedIndex {
	
	protected final InvertedIndex invIndex;
	protected final TIntObjectHashMap cache = new TIntObjectHashMap();

	public CachingInvertedIndex(InvertedIndex i) {
		super(3,3,3);
		invIndex = i;
	}

	public CachingInvertedIndex()
	{
		this(new InvertedIndex());
	}

	public CachingInvertedIndex(Lexicon lex, String filename)
	{
		this(new InvertedIndex(lex, filename));
	}

	public int[][] getDocuments(LexiconEntry le)
	{
		return getDocuments(le.termId);
	}

	public int[][] getDocuments(int termid)
	{
		Object rtr = null;
		rtr = cache.get(termid+1);
		if (rtr != null)
		{
			return (int[][])rtr;
		}
		final int[][] rtrReal = invIndex.getDocuments(termid);
		cache.put(termid+1, rtrReal);
		return rtrReal;	
	}

	public void close() {
		cache.clear();
		invIndex.close();
	}
	
}

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
 * The Original Code is UTFInvertedIndexBuilder.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original author)
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.structures.indexing;

import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.UTFLexiconInputStream;
import uk.ac.gla.terrier.structures.UTFLexiconOutputStream;

/**
 * Builds a UTF inverted index, using field information optionally.
 * @author Craig Macdonald &amp; Vassilis Plachouras
 * @version $Revision: 1.1 $
 */
public class UTFInvertedIndexBuilder extends InvertedIndexBuilder {

	public UTFInvertedIndexBuilder(Index i)
	{
		super(i);
		lexiconInputStream = UTFLexiconInputStream.class;
		lexiconOutputStream = UTFLexiconOutputStream.class;
	}
	
	/**
	 * A default constructor of the class InvertedIndex.
	 * @deprecated
	 */
	public UTFInvertedIndexBuilder() {
		super();
		lexiconInputStream = UTFLexiconInputStream.class;
		lexiconOutputStream = UTFLexiconOutputStream.class;
	}

	/** @deprecated */
	public UTFInvertedIndexBuilder(String path, String prefix)
	{
		super(path, prefix);
		lexiconInputStream = UTFLexiconInputStream.class;
		lexiconOutputStream = UTFLexiconOutputStream.class;
	}

	/**
	 * Creates an instance of the InvertedIndex
	 * class using the given filename.
	 * @param filename The name of the inverted file
	 * @deprecated
	 */
	public UTFInvertedIndexBuilder(String filename) {
		super(filename);
		lexiconInputStream = UTFLexiconInputStream.class;
		lexiconOutputStream = UTFLexiconOutputStream.class;
	}
	
}

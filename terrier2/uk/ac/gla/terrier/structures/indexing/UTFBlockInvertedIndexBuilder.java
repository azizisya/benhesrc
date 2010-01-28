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
 * The Original Code is UTFBlockInvertedIndexBuilder.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Douglas Johnson <johnsoda{a.}dcs.gla.ac.uk> (original author)
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk> 
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.structures.indexing;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.UTFBlockLexiconInputStream;
import uk.ac.gla.terrier.structures.UTFLexiconOutputStream;
/**
 * Builds an inverted index using block information, where indexing lexicon is a UTFBlock lexicon. It is optional to 
 * save field information as well. 
 * @author Douglas Johnson &amp; Vassilis Plachouras &amp; Craig Macdonald
 * @version $Revision: 1.1 $
 */
public class UTFBlockInvertedIndexBuilder extends BlockInvertedIndexBuilder {

	public UTFBlockInvertedIndexBuilder(Index i)
	{
		super(i);
		lexiconInputStream = UTFBlockLexiconInputStream.class;
		lexiconOutputStream = UTFLexiconOutputStream.class;
		finalLexiconClass = "uk.ac.gla.terrier.structures.UTFLexicon";
		finalLexiconInputStreamClass = "uk.ac.gla.terrier.structures.UTFLexiconInputStream";
	}
	
	/**
	 * Creates an instance of the BlockInvertedIndex class.
	 * @deprecated
	 */
	public UTFBlockInvertedIndexBuilder() {
		super();
		lexiconInputStream = UTFBlockLexiconInputStream.class;
		lexiconOutputStream = UTFLexiconOutputStream.class;
		finalLexiconClass = "uk.ac.gla.terrier.structures.UTFLexicon";
		finalLexiconInputStreamClass = "uk.ac.gla.terrier.structures.UTFLexiconInputStream";
	}
	/**
	 * Creates an instance of the BlockInvertedIndex class 
	 * using the given filename.
	 * @param filename the name of the inverted file
	 * @deprecated
	 */
	public UTFBlockInvertedIndexBuilder(String filename) {
		super(filename);
		lexiconInputStream = UTFBlockLexiconInputStream.class;
		lexiconOutputStream = UTFLexiconOutputStream.class;
		finalLexiconClass = "uk.ac.gla.terrier.structures.UTFLexicon";
		finalLexiconInputStreamClass = "uk.ac.gla.terrier.structures.UTFLexiconInputStream";
	}
	
	/**
	@deprecated */
	public UTFBlockInvertedIndexBuilder(String path, String prefix) {
		super(path, prefix);
		lexiconInputStream = UTFBlockLexiconInputStream.class;
		lexiconOutputStream = UTFLexiconOutputStream.class;
		finalLexiconClass = "uk.ac.gla.terrier.structures.UTFLexicon";
		finalLexiconInputStreamClass = "uk.ac.gla.terrier.structures.UTFLexiconInputStream";
	}
}

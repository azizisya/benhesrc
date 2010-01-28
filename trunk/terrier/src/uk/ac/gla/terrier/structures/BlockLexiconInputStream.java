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
 * The Original Code is BlockLexiconInputStream.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Douglas Johnson <johnsoda{a.}dcs.gla.ac.uk> (original author)
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk> 
 */
package uk.ac.gla.terrier.structures;
import java.io.*;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.utility.ApplicationSetup;
/**
 * An input stream for accessing sequentially the entries
 * of a block lexicon.
 * @author Douglas Johnson, Vassilis Plachouras
 * @version $Revision: 1.1 $
 */
public class BlockLexiconInputStream extends LexiconInputStream {
	/** The logger used */
	private static Logger logger = Logger.getRootLogger();
	
	/** 
	 * The total number of different blocks a term appears in.
	 */
	protected int blockFrequency;
	/**
	 * A default constructor.
	 */
	public BlockLexiconInputStream() {
		super();
		entrySize = BlockLexicon.lexiconEntryLength;
	}
	/**
	 * A constructor given the filename.
	 * @param filename java.lang.String the name of the lexicon file.
	 */
	public BlockLexiconInputStream(String filename) {
		super(filename);
		entrySize = BlockLexicon.lexiconEntryLength;
	}

	public BlockLexiconInputStream(String path, String prefix) {
		super(path, prefix);
		 entrySize = BlockLexicon.lexiconEntryLength;
	}
	/**
	 * A constructor given the filename.
	 * @param file java.io.File the name of the lexicon file.
	 */
	public BlockLexiconInputStream(File file) {
		super(file);
		entrySize = BlockLexicon.lexiconEntryLength;
	}
	
	/** Read a lexicon from the specified input stream */
	public BlockLexiconInputStream(DataInput in) {
		super(in);
		entrySize = BlockLexicon.lexiconEntryLength;
	}
	/**
	 * Read the next lexicon entry.
	 * @return the number of bytes read if there is no error, 
	 *		 otherwise returns -1 in case of EOF
	 * @throws java.io.IOException if an I/O error occurs
	 */
	public int readNextEntry() throws IOException {
		try {
			startBitOffset = (byte) (endBitOffset + 1);
			startOffset = endOffset;
			if (startBitOffset == 8) {
				startOffset = endOffset + 1;
				startBitOffset = 0;
			}
			lexiconStream.readFully(
				termCharacters,
				0,
				ApplicationSetup.STRING_BYTE_LENGTH);
			
			termId = lexiconStream.readInt();
			documentFrequency = lexiconStream.readInt();
			blockFrequency = lexiconStream.readInt();
			termFrequency = lexiconStream.readInt();
			endOffset = lexiconStream.readLong();
			endBitOffset = lexiconStream.readByte();
			numPointersRead += documentFrequency;
			numTokensRead += termFrequency;
			numTermsRead++;
			return Lexicon.lexiconEntryLength;
		} catch (EOFException eofe) {
			return -1;
		}
	}
	
	/**
	   * Returns the number of entries in the lexicon file.
	   */
		public int numberOfEntries(){
				return (int)(lexiconFilelength / BlockLexicon.lexiconEntryLength);
		}
	
	/**
	 * Prints out the contents of the lexicon file to check.
	 */
	public void print() {
		int i = 0; //counter
		int entryLength = Lexicon.lexiconEntryLength;
		try {
			while (readNextEntry() != -1) {
				System.out.println(
					""
						+ (long)i * (long)entryLength
						+ ", "
						+ term.trim()
						+ ", "
						+ termId
						+ ", "
						+ documentFrequency
						+ ", "
						+ blockFrequency
						+ ", "
						+ termFrequency
						+ ", "
						+ endBitOffset);
				i++;
			}
		} catch (IOException ioe) {
			logger.error("Input/Output exception while reading the lexicon index input stream. ", ioe);
		}
	}

	/**
	 * Returns the block frequency for the currently processed term.
	 * @return int The block frequency for the currently processed term
	 */
	public int getBlockFrequency() {
		return blockFrequency;
	}
}

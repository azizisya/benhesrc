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
 * The Original Code is LexiconInputStream.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk> (original author) 
 *   Craig Macdonald <craigm{a.}.dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.structures;
import java.io.DataInput;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.StringTools;
/**
 * This class implements an input stream for the lexicon structure.
 * @author Vassilis Plachouras, Craig Macdonald
 * @version $Revision: 1.1 $
 */
public class UTFLexiconInputStream extends LexiconInputStream {
	/** A zero buffer for writing to the file.*/
	protected byte[] junkBuffer = new byte[ApplicationSetup.STRING_BYTE_LENGTH+2];
	
	/**
	 * A default constructor.
	 */
	public UTFLexiconInputStream() {
		super();
		entrySize = UTFLexicon.lexiconEntryLength;
		termCharacters = new byte[ApplicationSetup.STRING_BYTE_LENGTH +2];
	}
	/**
	 * A constructor given the filename.
	 * @param filename java.lang.String the name of the lexicon file.
	 */
	public UTFLexiconInputStream(String filename) {
		super(filename);
		entrySize = UTFLexicon.lexiconEntryLength;
		termCharacters = new byte[ApplicationSetup.STRING_BYTE_LENGTH +2];
	}
	/**
	 * A constructor given the filename.
	 * @param file java.io.File the name of the lexicon file.
	 */
	public UTFLexiconInputStream(File file) {
		super(file);
		entrySize = UTFLexicon.lexiconEntryLength;
		termCharacters = new byte[ApplicationSetup.STRING_BYTE_LENGTH +2];
	}
	
	public UTFLexiconInputStream(String path, String prefix) {
		this(path + ApplicationSetup.FILE_SEPARATOR + prefix + ApplicationSetup.LEXICONSUFFIX);
	}
	
	/** Read a lexicon from the specified input stream */
	public UTFLexiconInputStream(DataInput in) {
		super(in);
		entrySize = UTFLexicon.lexiconEntryLength;
	}

	/**
	 * Read the next lexicon entry, where the term is parsed as a string.
	 * This method does NOT work with getTermCharacters() - use readNextEntryBytes()
	 * iterator for that.
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
			
			term = lexiconStream.readUTF();
			lexiconStream.readFully(junkBuffer, 0, ApplicationSetup.STRING_BYTE_LENGTH - StringTools.utf8_length(term));

			termId = lexiconStream.readInt();
			documentFrequency = lexiconStream.readInt();
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
	 * Read the next lexicon entry, where the term is saved as a byte array. No attempt is
	 * made to parse the byte array and the padding bytes into a String. Use this method when
	 * you want to get the bytes of the string using getTermCharacters(). This method does
	 * NOT work with getTerm()
	 * @return the number of bytes read if there is no error, 
	 *		 otherwise returns -1 in case of EOF
	 * @throws java.io.IOException if an I/O error occurs
	 */
	public int readNextEntryBytes() throws IOException {
		try {
			startBitOffset = (byte) (endBitOffset + 1);
			startOffset = endOffset;
			if (startBitOffset == 8) {
				startOffset = endOffset + 1;
				startBitOffset = 0;
			}

			Arrays.fill(termCharacters, (byte)0);
			lexiconStream.readFully(termCharacters, 0, ApplicationSetup.STRING_BYTE_LENGTH +2);

			termId = lexiconStream.readInt();
			documentFrequency = lexiconStream.readInt();
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
			return (int)(lexiconFilelength / UTFLexicon.lexiconEntryLength);
	}

	/**
	 * Returns the string representation of the term.
	 * @return the string representation of the already found term.
	 */
	public String getTerm() {
		return term;
	}
	
	/** 
	 * Returns the bytes of the String. Only valid is readNextEntryByte was used.
	 * @return the byte array holding the term's byte representation
	 */
	public byte[] getTermCharacters() {
		return termCharacters;
	}
	
}

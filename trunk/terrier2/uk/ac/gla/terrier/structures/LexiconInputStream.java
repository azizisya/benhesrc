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
 */
package uk.ac.gla.terrier.structures;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;
/**
 * This class implements an input stream for the lexicon structure.
 * @author Vassilis Plachouras
 * @version $Revision: 1.1 $
 */
public class LexiconInputStream implements Iterable<String>, Closeable{
	/** The logger used for the Lexicon */
	protected Logger logger = Logger.getRootLogger();

	/** The term represented as an array of bytes.*/
	protected byte[] termCharacters =
		new byte[ApplicationSetup.STRING_BYTE_LENGTH];
	/** The term represented as a string.*/
	protected String term;
	/** An integer representing the id of the term.*/
	protected int termId;
	/** The document frequency of the term.*/
	protected int documentFrequency;
	/** The term frequency of the term.*/
	protected int termFrequency;
	/** The offset in bytes in the inverted file of the term.*/
	protected long endOffset;
	/** The starting offset in bytes in the inverted file of the term.*/
	protected long startOffset;
	/** The starting bit offset in the inverted file of the term.*/
	protected byte startBitOffset;
	/** 
	 * The offset in bits in the starting byte in the inverted file.
	 * Its initial value is -1 so that when we do startBitOffset = 
	 * endBitOffset +1, the first startBitOffset is 0
	 */
	protected byte endBitOffset = -1;
	/** A data input stream to read from the bufferInput.*/
	protected DataInput lexiconStream = null;
	/** The length of the lexicon file. */
	protected long lexiconFilelength;
	/** size of one entry of the lexicon */	
	protected int entrySize = 0;

	/** number of pointers read so far */
	protected long numPointersRead = 0;
	/** number of tokens read so far */
    protected long numTokensRead = 0;
	/** number of terms read so far */
	protected int numTermsRead = 0;

	 /** A constructor for child classes that doesnt open the file */
	protected LexiconInputStream(long a, long b, long c) { }

	/**
	 * A default constructor. Opens the default lexicon.
	 */
	public LexiconInputStream() {
		this(ApplicationSetup.LEXICON_FILENAME);
	}
	/**
	 * A constructor given the filename.
	 * @param filename java.lang.String the name of the lexicon file.
	 */
	public LexiconInputStream(String filename) {
		try {
			lexiconStream = new DataInputStream(Files.openFileStream(filename));
			this.lexiconFilelength = Files.length(filename);
		} catch (IOException ioe) {
			logger.fatal(
				"I/O Exception occured while opening the lexicon file. Stack trace follows.",ioe);
		}
		entrySize = Lexicon.lexiconEntryLength;
	}
	
	public LexiconInputStream(String path, String prefix) {
		this(path + ApplicationSetup.FILE_SEPARATOR + prefix + ApplicationSetup.LEXICONSUFFIX);
	}
	/**
	 * A constructor given the filename.
	 * @param file java.io.File the name of the lexicon file.
	 */
	public LexiconInputStream(File file) {
		try {
			lexiconStream = new DataInputStream(Files.openFileStream(file));
			 this.lexiconFilelength = Files.length(file);
		} catch (IOException ioe) {
			logger.fatal(
				"I/O Exception occured while opening the lexicon file. Stack trace follows.",ioe);
		}
		entrySize = Lexicon.lexiconEntryLength;
	}
	
	/** Read a lexicon from the specified input stream */
	public LexiconInputStream(DataInput in) {
		lexiconStream = in;
		this.lexiconFilelength = 0;
		entrySize = Lexicon.lexiconEntryLength;
	}
	
	/**
	 * Closes the lexicon stream.
	 * @throws IOException if an I/O error occurs
	 */
	public void close() {
		try{
			if (lexiconStream instanceof java.io.Closeable)
				((java.io.Closeable)lexiconStream).close();
		} catch (IOException ioe){}
	}
	
	public int getEntrySize()
	{
		return entrySize;
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
	
	/** This is an alias to readNextEntry(), except for implementations that 
	  * cannot parse the string from the byte array. */
	public int readNextEntryBytes() throws IOException {
		return readNextEntry();
	}
	/**
	* Returns the number of entries in the lexicon file.
	*/
	public int numberOfEntries(){
		return (int)(lexiconFilelength / Lexicon.lexiconEntryLength);
	}
	
	/**
	 * Prints out the contents of the lexicon file to check.
	 */
	public void print() {
		int i = 0; //counter
		int entryLength = getEntrySize();
		System.err.println("LexOffset, Term, Termid, DF, TF, OffsetBy, OffsetBit");
		try {
			while (readNextEntry() != -1) {
				System.out.println(
					""
						+ ((long)i * (long)entryLength)
						+ ", "
						+ getTerm()
						+ ", "
						+ termId
						+ ", "
						+ documentFrequency
						+ ", "
						+ termFrequency
						+ ", "
						+ endOffset
						+ ", "
						+ endBitOffset);
				i++;
			}
		} catch (IOException ioe) {
			logger.fatal(
				"Input/Output exception while reading the document index " +
				"input stream. Stack trace follows.",ioe);
		}
	}

	/** Returns the number of pointers there would be in an inverted index built using this lexicon (thus far).
	  * This is equal to the sum of the Nts written to this lexicon output stream. */
	public long getNumberOfPointersRead()
	{
		return numPointersRead;
	}

	/** Returns the number of tokens there are in the entire collection represented by this lexicon (thus far).
	  * This is equal to the sum of the TFs written to this lexicon output stream. */
	public long getNumberOfTokensRead()
	{
		return numTokensRead;
	}

	/** Returns the number of terms written so far by this LexiconInputStream */
	public int getNumberOfTermsRead()
	{
		return numTermsRead;
	}


	/**
	 * Returns the bit offset in the last byte of 
	 * the term's entry in the inverted file.
	 * @return byte the bit offset in the last byte of 
	 *		 the term's entry in the inverted file
	 */
	public byte getEndBitOffset() {
		return endBitOffset;
	}
	/**
	 * Returns the ending offset of the term's 
	 * entry in the inverted file.
	 * @return long The ending byte of the term's 
	 *			  entry in the inverted file.
	 */
	public long getEndOffset() {
		return endOffset;
	}
	/**
	 * Returns the bit offset in the first byte 
	 * of the term's entry in the inverted file.
	 * @return byte the bit offset in the first byte 
	 *		 of the term's entry in the inverted file
	 */
	public byte getStartBitOffset() {
		return startBitOffset;
	}
	/**
	 * Returns the starting offset of the term's 
	 * entry in the inverted file.
	 * @return long The starting byte of the term's entry 
	 * 				in the inverted file.
	 */
	public long getStartOffset() {
		return startOffset;
	}
	/**
	 * Return the document frequency for the given term.
	 * @return int The document frequency for the given term
	 */
	public int getNt() {
		return documentFrequency;
	}
	/**
	 * Returns the string representation of the term.
	 * @return the string representation of the already found term.
	 */
	public String getTerm() {
		return (new String(termCharacters)).trim();
	}
	/**
	 * Returns the term's id.
	 * @return the term's id.
	 */
	public int getTermId() {
		return termId;
	}
	/**
	 * Returns the term frequency for the already seeked term.
	 * @return the term frequency in the collection.
	 */
	public int getTF() {
		return termFrequency;
	}
	/** 
	 * Returns the bytes of the String.
	 * @return the byte array holding the term's byte representation
	 */
	public byte[] getTermCharacters() {
		return termCharacters;
	}

	/** Returns an Interator of Strings of each term in this lexicon */
	public Iterator<String> iterator()
	{
		return new Iterator<String>(){
			public boolean hasNext(){
				try{
					return readNextEntry() != -1;
				} catch (IOException ioe) {
					logger.error(ioe);
					return false;
				}
			}
			public String next()
			{
				return getTerm();
			}
			public void remove() { throw new UnsupportedOperationException();}

		};
	}
}

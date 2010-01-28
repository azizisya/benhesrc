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
 * The Original Code is Lexicon.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Gianni Amati <gba{a.}fub.it> (original author)
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk> 
 */
package uk.ac.gla.terrier.structures;
import gnu.trove.TIntObjectHashMap;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Iterator;

import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.io.RandomDataInput;
import uk.ac.gla.terrier.utility.io.RandomDataOutput;

import org.apache.log4j.Logger;
/**
 * The class that implements the lexicon structure. Apart from the lexicon file,
 * which contains the actual data about the terms, and takes its name from
 * ApplicationSetup.LEXICON_FILENAME, another file is created and
 * used, containing a mapping from the term's code to the offset of the term 
 * in the lexicon. The name of this file is given by 
 * ApplicationSetup.LEXICON_INDEX_FILENAME.
 * 
 * @see ApplicationSetup#LEXICON_FILENAME
 * @see ApplicationSetup#LEXICON_INDEX_FILENAME
 * @author Gianni Amati, Vassilis Plachouras
 * @version $Revision: 1.1 $
 */
public class Lexicon implements Iterable<String>, Closeable{
	/** The logger used for the Lexicon */
	protected Logger logger = Logger.getRootLogger();
	
	/** The term represented as an array of bytes.*/
	protected byte[] termCharacters;
	
	/** The term represented as a string.*/
	protected String term;
	
	/** An integer representing the id of the term.*/
	protected int termId;
	
	/** The document frequency of the term.*/
	protected int documentFrequency;
	
	/** The term frequency of the term.*/
	protected int termFrequency;
	
	/** The offset in bytes in the inverted file of the term.*/
	protected long startOffset;
	
	/** The offset in bits in the starting byte in the inverted file.*/
	protected byte startBitOffset;
	
	/** The offset in bytes in the inverted file of the term.*/
	protected long endOffset;
	
	/** The offset in bits in the ending byte in the inverted file.*/
	protected byte endBitOffset;
	
	/** 
	 * The size in bytes of an entry in the lexicon file.
	 * An entry corresponds to a string, an int (termCode), 
	 * an int (docf), an int (tf), a long (the offset of the end 
	 * of the term's entry in bytes in the inverted file) and
	 * a byte (the offset in bits of the last byte of the term's entry 
	 * in the inverted file.
	 */
	public static final int lexiconEntryLength =
		ApplicationSetup.STRING_BYTE_LENGTH //the string representation
		+12 //the three integers
		+8 //the long
		+1; //the byte
	
	/** The file containing the mapping from the codes to the offset in the lexicon file.*/
	protected RandomDataInput idToOffsetFile;
	
	/** The actual lexicon file.*/
	protected RandomDataInput lexiconFile;

	/** Filename of the of lexicon file opened */
	protected String lexiconFileName;
	
	/** The number of entries in the lexicon file.*/
	protected int numberOfLexiconEntries;
	
	/** A buffer for reading from the lexicon file.*/
	protected byte[] buffer = new byte[512];
	
	/** A second buffer for finding terms.*/
	protected byte[] bt = new byte[ApplicationSetup.STRING_BYTE_LENGTH];
	
	/** A byte input stream to read from the buffer.*/
	protected ByteArrayInputStream bufferInput = new ByteArrayInputStream(buffer);
	
	/** A data input stream to read from the bufferInput.*/
	protected DataInputStream dataInput = new DataInputStream(bufferInput);
	
	/** 
	 * A hashmap that is used in order to reduce the number 
	 * of random accesses on disk during the binary search
	 */
	protected TIntObjectHashMap map = null;

	/** Controls whether to use the hash for speeding up 
	 * lexicon entry lookups or not. The corresponding
	 * property is <tt>lexicon.use.hash</tt>.
	 */
	protected boolean USE_HASH = Boolean.parseBoolean(ApplicationSetup.getProperty("lexicon.use.hash","true"));

	protected Class inputStreamClass = LexiconInputStream.class;
	
	/** Contructor for child classes which dont want to open a file */
	protected Lexicon(long a, long b, long c) {}
	
	/** 
	 * A default constructor.
	 */
	public Lexicon() {
		this(ApplicationSetup.LEXICON_FILENAME);
	}

	public Lexicon(String path, String prefix)
	{
		this(path + ApplicationSetup.FILE_SEPARATOR + prefix + ApplicationSetup.LEXICONSUFFIX);
	}
	
	/**
	 * Constructs an instace of Lexicon and opens
	 * the corresponding file.
	 * 
	 * @param lexiconName the name of the lexicon file.
	 */
	public Lexicon(String lexiconName) {
		boolean updateable = false;
		try {
			lexiconFile = updateable
				? Files.writeFileRandom(this.lexiconFileName = lexiconName)
				: Files.openFileRandom(this.lexiconFileName = lexiconName);
			idToOffsetFile = Files.openFileRandom(lexiconName.substring(0,lexiconName.lastIndexOf(".")).concat(ApplicationSetup.LEXICON_INDEX_SUFFIX));
			numberOfLexiconEntries = (int) (lexiconFile.length() / (long)lexiconEntryLength);
			
			if (USE_HASH) {
				try{
					String hashFilename = lexiconName.substring(0,lexiconName.lastIndexOf(".")).concat(ApplicationSetup.LEXICON_HASH_SUFFIX);
					ObjectInputStream ois = new ObjectInputStream(Files.openFileStream(hashFilename));
					map = (TIntObjectHashMap)ois.readObject();
					ois.close();
				}
				catch (IOException ioe) {
					logger.warn("Input/output exception while reading the hashmap used for the lexicon. Hash will not be used." + ioe);
					USE_HASH = false;
				} catch (ClassNotFoundException cnfe) {
					logger.warn("ClassNotFoundException while reading the hashmap used for the lexicon. Hash will not be used." + cnfe);
					USE_HASH = false;
				}
			}//USE_HASH
		} catch (IOException ioe) {
			logger.error("Input/output exception while opening for reading the lexicon file: " + ioe);
		}

	}
	
	/**
	* Closes the lexicon and lexicon index files.
	*/
	public void close() {
		try {
			idToOffsetFile.close();
			lexiconFile.close();
		} catch (IOException ioe) {
			logger.error("Input/output exception while closing the lexicon file: " + ioe);
		}
	}
	
	/** 
	 * Prints out the contents of the lexicon file. 
	 * Streams are used to read the lexicon file.
	 */
	public void print() {
		LexiconInputStream tmp=null;
		try{
			tmp = (LexiconInputStream)inputStreamClass.getConstructor(String.class).newInstance(this.lexiconFileName);
		} catch (Exception e) {logger.error(e); return;}
		final LexiconInputStream _lis=tmp;
		_lis.print();
	}

	/**
	 * Finds the term given its term code.
	 *
	 * @return true if the term is found, else return false
	 * @param _termId the term's identifier
	 */
	public boolean findTerm(int _termId) {
		try {
			idToOffsetFile.seek((long)_termId * 8L);
			return seekEntry((int) (idToOffsetFile.readLong()/(long)lexiconEntryLength));
		} catch(IOException ioe) {
			logger.error("Input/Output exception while reading the lexicon index file for termid "+_termId+": ", ioe);
		}
		return false;
		
	}
	/** 
	 * Performs a binary search in the lexicon
	 * in order to locate the given term.
	 * If the term is located, the properties
	 * termCharacters, documentFrequency,
	 * termFrequency, startOffset, startBitOffset,
	 * endOffset and endBitOffset contain the
	 * values related to the term.
	 * @param _term The term to search for.
	 * @return true if the term is found, and false otherwise.
	 */
	public boolean findTerm(String _term) {
		int low = -1;
		int high = numberOfLexiconEntries;
		int i;
		int compareStrings;

		if (USE_HASH) {
			int firstChar = _term.charAt(0);
			int[] boundaries = (int[])map.get(firstChar);
			low = boundaries[0];
			high = boundaries[1];

		}

		//if (logger.isDebugEnabled()) 
		//	logger.debug("lexicon hash low high for term " + _term + " are: " + low + " " + high);
		
		try {
			while (high-low>1) {
				
				i = (high + low)/2;
				
				lexiconFile.seek((long)i * (long)lexiconEntryLength);
				lexiconFile.readFully(buffer, 0, lexiconEntryLength);
				term = new String(buffer,0,ApplicationSetup.STRING_BYTE_LENGTH).trim();
							
				if ((compareStrings = _term.compareTo(term))< 0)
					high = i;
				else if (compareStrings > 0)
					low = i;
				else { 
					seekEntry(i);
					return true;
				}
					
			
			}
		} catch(IOException ioe) {
			logger.fatal("IOException while binary searching the lexicon: " + ioe);
		}
		
		if (high == numberOfLexiconEntries)
			return false;
		
		seekEntry(high);
		if (_term.compareTo(term) == 0) 
			return true; 
		return false;
	}

	/**
	 * Returns the bit offset in the last byte of 
	 * the term's entry in the inverted file.
	 * @deprecated
	 * @return byte the bit offset in the last byte of 
	 *		 the term's entry in the inverted file
	 */
	public byte getEndBitOffset() {
		return endBitOffset;
	}
	/**
	 * Returns the ending offset of the term's entry in the inverted file.
	 * @deprecated
	 * @return long The ending byte of the term's entry in the inverted file.
	 */
	public long getEndOffset() {
		return endOffset;
	}
	/**
	 * Return the document frequency for the given term.
	 * @deprecated
	 * @return int The document frequency for the given term
	 */
	public int getNt() {
		return documentFrequency;
	}
	/**
	 * Returns the number of entries in the lexicon.
	 * @return the number of entries in the lexicon.
	 * @deprecated
	 */
	public long getNumberOfLexiconEntries() {
		return numberOfLexiconEntries;
	}
	/**
	 * The bit offset in the starting byte of 
	 * the entry in the inverted file.
	 * @deprecated
	 * @return byte The number of bits in the first 
	 *		 byte of the entry in the inverted file
	 */
	public byte getStartBitOffset() {
		return startBitOffset;
	}
	/**
	 * Returns the beginning of the term's entry in the inverted file.
	 * @deprecated
	 * @return long the start offset (in bytes) in the inverted file
	 */
	public long getStartOffset() {
		return startOffset;
	}
	/**
	 * Insert the method's description here.
	 * @deprecated
	 * @return java.lang.String The string representation of the seeked term.
	 */
	public String getTerm() {
		return this.term.trim();
	}
	/**
	 * Returns the term's id.
	 * @deprecated
	 * @return int the term's id.
	 */
	public int getTermId() {
		return termId;
	}
	/**
	 * Returns the term frequency for the already seeked term.
	 *
	 * @return int The term frequency in the collection.
	 * @deprecated
	 */
	public int getTF() {
		return termFrequency;
	}
	/**
	 * Seeks the i-th entry of the lexicon.
	 * TODO read a byte array from the file and decode it, 
	 * 		instead of reading the different pieces of 
	 *	  information separately.
	 * @param i The index of the entry we are looking for.
	 * @return true if the entry was found, false otherwise.
	 */
	public boolean seekEntry(int i) {
		try {
			if (i >= numberOfLexiconEntries || i < 0)
				return false;
			else {
				if (i == 0) {
					lexiconFile.seek(0);
					startOffset = 0;
					startBitOffset = 0;
					lexiconFile.readFully(buffer, 0, lexiconEntryLength);
					dataInput.reset();
					term = new String(buffer,0,ApplicationSetup.STRING_BYTE_LENGTH).trim();
				} else {
					lexiconFile.seek((i-1) * (long)lexiconEntryLength + (long)(ApplicationSetup.STRING_BYTE_LENGTH + 12));
					lexiconFile.readFully(buffer, 0, lexiconEntryLength + 9);
					dataInput.reset();
					startOffset = dataInput.readLong();
					startBitOffset = dataInput.readByte();
					if (++startBitOffset == 8) {
						startBitOffset = 0;
						startOffset++;
					}
					term = new String(buffer, 9, ApplicationSetup.STRING_BYTE_LENGTH).trim();					
				}
				dataInput.skipBytes(ApplicationSetup.STRING_BYTE_LENGTH);
				termId = dataInput.readInt();
				documentFrequency = dataInput.readInt();
				termFrequency = dataInput.readInt();
				endOffset = dataInput.readLong();
				endBitOffset = dataInput.readByte();
				return true;
			}
		} catch (IOException ioe) {
			logger.error("Input/Output exception while reading the idToOffset file. ", ioe);
		}
		return false;
	}

	
	/**
	 * In an already stored entry in the lexicon
	 * file, the information about the term frequency,
	 * the endOffset in bytes, and the endBitOffset in the last
	 * byte, is updated. The term is specified by the index of the entry.
	 *
	 * @return true if the information is updated properly, 
	 *		 otherwise return false
	 * @param i the i-th entry
	 * @param frequency the term's Frequency
	 * @param endOffset the offset of the ending byte in the inverted file
	 * @param endBitOffset the offset in bits in the ending byte 
	 *		in the term's entry in inverted file
	 * @deprecated The Lexicon class is only used for reading the
	 *			 lexicon file, and not for writing any information.
	 */
	public boolean updateEntry(
		int i,
		int frequency,
		long endOffset,
		byte endBitOffset) {
		
		if (! (lexiconFile instanceof RandomDataOutput))
			return false;
		RandomDataOutput _lexiconFile = (RandomDataOutput)lexiconFile;
		try {
			long lexiconOffset = (long)i * (long)lexiconEntryLength;
			//we seek the offset where the frequency should be writen
			_lexiconFile.seek(
				lexiconOffset + ApplicationSetup.STRING_BYTE_LENGTH + 8);
			_lexiconFile.writeInt(frequency);
			_lexiconFile.writeLong(endOffset);
			_lexiconFile.writeByte(endBitOffset);
		} catch (IOException ioe) {
			logger.error("Input/Output exception while writing to the lexicon file. ", ioe);
		}
		return false;
	}


	/** Returns the number of entries in the lexicon file specified by f.
	  * @param f The file to find the number of entries in
	  */
	public static int numberOfEntries(File f) {
		return (int) ( f.length()/(long)lexiconEntryLength );
	}

	/** Returns the number of entries in the lexicon file specified by filename.
	  * @param filename
	  */
	public static int numberOfEntries(String filename) {
		return numberOfEntries(new File(filename));
	}

	
	/** Returns a LexiconEntry describing all the information in the lexicon about the ith term 
	 * in the lexicon.
	 * @param termNumber The ith term in the lexicon. i is 0-based, and runs to getNumberOfLexiconEntries()-1
	 * @return LexiconEntry all information about the term's entry in the lexicon. null if termid not found
	 */
	public LexiconEntry getIthLexiconEntry(int termNumber) {
		if (! seekEntry(termNumber))
			return null;
		LexiconEntry le = new LexiconEntry();
		le.termId = this.termId;
		le.term = this.term.trim();
		le.TF = this.termFrequency;
		le.n_t = this.documentFrequency;
		le.startOffset = this.startOffset;
		le.startBitOffset = this.startBitOffset;
		le.endOffset = this.endOffset;
		le.endBitOffset = this.endBitOffset;
		return le;
	}
	
	/** Returns a LexiconEntry describing all the information in the lexicon about the term
	  * denoted by termid
	  * @param termid the termid of the term of interest
	  * @return LexiconEntry all information about the term's entry in the lexicon. null if termid not found */
	public LexiconEntry getLexiconEntry(int termid) {
		/* TODO: improve this to the effectiveness level of getLexiconEntry() */
		if (! findTerm(termid))
			return null;
		LexiconEntry le = new LexiconEntry();
		le.termId = this.termId;
		le.term = this.term.trim();
		le.TF = this.termFrequency;
		le.n_t = this.documentFrequency;
		le.startOffset = this.startOffset;
		le.startBitOffset = this.startBitOffset;
		le.endOffset = this.endOffset;
		le.endBitOffset = this.endBitOffset;
		return le;
	}
	
	/** Returns a LexiconEntry describing all the information in the lexicon about the term
	  * denoted by _term
	  * @param _term the String term that is of interest
	  * @return LexiconEntry all information about the term's entry in the lexicon. null if termid not found */
	public LexiconEntry getLexiconEntry(String _term) {
		int low = -1;
		int high = numberOfLexiconEntries;
		int i;
		int compareStrings;
		String term;
		byte[] buffer = new byte[lexiconEntryLength+9]; //to get the start offsets as well
		
		if (USE_HASH) {
			int firstChar = _term.charAt(0);
			int[] boundaries = (int[])map.get(firstChar);
			if (boundaries != null)
			{
				low = boundaries[0];
				high = boundaries[1];
			}
			//System.out.println("lexicon use hash: " + low + " " + high);
		}
		
		try {
			while (high-low>1) {
				
				i = (high + low)/2;
				if (i==0) {
					lexiconFile.seek(0);
					lexiconFile.readFully(buffer, 0, lexiconEntryLength);
					term = new String(buffer,0,ApplicationSetup.STRING_BYTE_LENGTH).trim();
				} else {
					lexiconFile.seek((long)i * (long)(lexiconEntryLength)-9L);
					lexiconFile.readFully(buffer, 0, lexiconEntryLength+9);
					term = new String(buffer,9,ApplicationSetup.STRING_BYTE_LENGTH).trim();
				}
							
				if ((compareStrings = _term.compareTo(term))< 0)
					high = i;
				else if (compareStrings > 0)
					low = i;
				else { //read the rest and return the data
					return getLexiconEntryFromBuffer(buffer, term, i);
				}
			}
		
			if (high == numberOfLexiconEntries)
				return null;
			
			if (high == 0) {
				lexiconFile.seek(0);
				lexiconFile.readFully(buffer, 0, lexiconEntryLength);
				term = new String(buffer,0,ApplicationSetup.STRING_BYTE_LENGTH).trim();
			} else {
				lexiconFile.seek((long)high * (long)(lexiconEntryLength)-9L);
				lexiconFile.readFully(buffer, 0, lexiconEntryLength+9);
				term = new String(buffer,9,ApplicationSetup.STRING_BYTE_LENGTH).trim();				
			}
			
			if (_term.compareTo(term) == 0) {
				return getLexiconEntryFromBuffer(buffer, term, high);
			}	
		} catch(IOException ioe) {
			logger.fatal("IOException while binary searching the lexicon: " + ioe);
		}
		return null;
	}

	protected LexiconEntry getLexiconEntryFromBuffer(byte[] buffer, String term, int index) {
		int offset;
		LexiconEntry lEntry = new LexiconEntry();
		lEntry.term = term;
		if (index==0) {
			lEntry.startOffset = 0;
			lEntry.startBitOffset = 0;
			offset = ApplicationSetup.STRING_BYTE_LENGTH;						
		} else {
			offset = 0;
//			lEntry.startOffset =
//				(((((((buffer[offset++]&0xff) << 8 | buffer[offset++]&0xff) << 8 | buffer[offset++]&0xff) << 8 | buffer[offset++]&0xff) << 8 |
//					   buffer[offset++]&0xff) << 8 | buffer[offset++]&0xff) << 8 | buffer[offset++]&0xff) << 8 | buffer[offset++]&0xff;

			long startOffset = (buffer[offset++] & 0xff);
			for (int j=0; j<7; j++)
				startOffset = startOffset<<8 | (buffer[offset++] & 0xff);
			lEntry.startOffset = startOffset;

			
			lEntry.startBitOffset = (byte)(buffer[offset++]&0xff);
			if (++lEntry.startBitOffset == 8) {
				lEntry.startBitOffset = 0;
				lEntry.startOffset++;
			}

			offset += ApplicationSetup.STRING_BYTE_LENGTH;
		}
		lEntry.termId = 
			(((buffer[offset++]&0xff) << 8 | buffer[offset++]&0xff) << 8 | buffer[offset++]&0xff) << 8 | buffer[offset++]&0xff;
		lEntry.n_t =
			(((buffer[offset++]&0xff) << 8 | buffer[offset++]&0xff) << 8 | buffer[offset++]&0xff) << 8 | buffer[offset++]&0xff;
		lEntry.TF =
			(((buffer[offset++]&0xff) << 8 | buffer[offset++]&0xff) << 8 | buffer[offset++]&0xff) << 8 | buffer[offset++]&0xff;
		
//		lEntry.endOffset = 
//			(((((((buffer[offset++]&0xff) << 8 | buffer[offset++]&0xff) << 8 | buffer[offset++]&0xff) << 8 | buffer[offset++]&0xff) << 8 |
//				   buffer[offset++]&0xff) << 8 | buffer[offset++]&0xff) << 8 | buffer[offset++]&0xff) << 8 | buffer[offset++]&0xff;

		long endOffset = (int)(buffer[offset++] & 0xff);
		for (int j=0; j<7; j++)
			endOffset = endOffset<<8 | (buffer[offset++] & 0xff);
		lEntry.endOffset = endOffset;
		
		lEntry.endBitOffset = (byte)(buffer[offset]&0xff);
		return lEntry;
	}

	/** Returns an interator that gives every item in the lexicon, in lexical order. Underlying implementation is
	  * using a lexicon input stream */
	public Iterator<String> iterator()
	{
		LexiconInputStream tmp=null;
		try{
			tmp = (LexiconInputStream)inputStreamClass.getConstructor(String.class).newInstance(this.lexiconFileName);
		} catch (Exception e) {logger.error(e);}
		final LexiconInputStream _lis=tmp;
		return new Iterator<String>(){
			LexiconInputStream lis = _lis;
			 public boolean hasNext(){
				try{
					return lis.readNextEntry() != -1;
				} catch (IOException ioe) {
					logger.error(ioe);
					return false;
				}
			}
			public String next()
			{
				return lis.getTerm();
			}
			public void remove() { throw new UnsupportedOperationException();}
		};
	}
}


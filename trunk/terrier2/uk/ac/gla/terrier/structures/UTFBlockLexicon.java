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
 * The Original Code is UTFBlockLexicon.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Douglas Johnson <johnsoda{a.}dcs.gla.ac.uk> (original author)
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk> 
 */
package uk.ac.gla.terrier.structures;
import java.io.*;
import java.util.Arrays;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.StringTools;
import uk.ac.gla.terrier.utility.io.RandomDataInput;
import uk.ac.gla.terrier.utility.io.RandomDataOutput;
/**
 * A lexicon class that saves the number of
 * different blocks a term appears in, using UTF encoding of Strings. It is used only during 
 * creating a utf block inverted index. After the utf block inverted
 * index has been created, the utf block lexicon is transformed into 
 * a utf lexicon.
 * @author Douglas Johnson, Vassilis Plachouras
 * @version $Revision: 1.1 $
 */
public class UTFBlockLexicon extends BlockLexicon {
	/** The logger used */
	private static Logger logger = Logger.getRootLogger();
	/**
	 * The size in bytes of an entry in the lexicon file. An entry corresponds
	 * to a string, an int (termCode), an int (docf), an int (tf), a long (the
	 * offset of the end of the term's entry in bytes in the inverted file) and
	 * a byte (the offset in bits of the last byte of the term's entry in the
	 * inverted file.
	 */
	public static final int lexiconEntryLength = 
		2+//two bytes for length written by writeUTF
		ApplicationSetup.STRING_BYTE_LENGTH //the byte representation of the string, ie 3* MAX_TERM_LENGTH
		+ 16 //the four integers
		+ 8 //the long
		+ 1; //the byte
	/**
	 * A default constructor.
	 */
	public UTFBlockLexicon() {
		super();
		
		try {
			numberOfLexiconEntries = (int) (lexiconFile.length() / (long)lexiconEntryLength);
			bufferInput.mark(3 * lexiconEntryLength);
		} catch (IOException ioe) {
			logger.fatal("Input/output exception while opening for reading the lexicon file. Stack trace follows",ioe);
		}
		inputStreamClass = UTFLexiconInputStream.class;
	}

    public UTFBlockLexicon(String path, String prefix)
    {
        this(path + ApplicationSetup.FILE_SEPARATOR + prefix + ApplicationSetup.LEXICONSUFFIX);
    }

	/**
	 * Constructs an instace of BlockLexicon and opens the corresponding file.
	 * @param lexiconName the name of the lexicon file.
	 */
	public UTFBlockLexicon(String lexiconName) {
		super(lexiconName);
		try {
			numberOfLexiconEntries = (int) (lexiconFile.length() / (long)lexiconEntryLength);
			bufferInput.mark(3 * lexiconEntryLength);
		} catch (IOException ioe) {
			logger.fatal("Input/output exception while opening for reading the " +
							"lexicon file. Stack trace follows",ioe);
		}
		inputStreamClass = UTFLexiconInputStream.class;
	}
	

	
	/**
	 * Finds the term given its term code.
	 * 
	 * @return true if the term is found, else return false
	 * @param termId
	 *            the term's id
	 */
	public boolean findTerm(int termId) {
		try {
			idToOffsetFile.seek((long)termId * 8L);
			long lexiconOffset = idToOffsetFile.readLong();
			if (lexiconOffset == 0) {
				startOffset = 0;
				startBitOffset = 0;
				lexiconFile.seek(lexiconOffset);
				
				term = lexiconFile.readUTF();
				lexiconFile.readFully(bt, 0, ApplicationSetup.STRING_BYTE_LENGTH - StringTools.utf8_length(term));
				this.termId = lexiconFile.readInt();
				documentFrequency = lexiconFile.readInt();
				blockFrequency = lexiconFile.readInt();
				termFrequency = lexiconFile.readInt();
				endOffset = lexiconFile.readLong();
				endBitOffset = lexiconFile.readByte();
				return true;
			} else {
				lexiconFile.seek(lexiconOffset - 9);
				//goes to the lexicon offset minus the long offset and a byte
				startOffset = lexiconFile.readLong();
				startBitOffset = lexiconFile.readByte();
				startBitOffset++;
				if (startBitOffset == 8) {
					startBitOffset = 0;
					startOffset++;
				}
				term = lexiconFile.readUTF();
				lexiconFile.readFully(bt, 0, ApplicationSetup.STRING_BYTE_LENGTH - StringTools.utf8_length(term));
				
				this.termId = lexiconFile.readInt();
				documentFrequency = lexiconFile.readInt();
				blockFrequency = lexiconFile.readInt();
				termFrequency = lexiconFile.readInt();
				endOffset = lexiconFile.readLong();
				endBitOffset = lexiconFile.readByte();
				return true;
			}
		} catch (IOException ioe) {
			logger.fatal("Input/Output exception while reading the idToOffset file. Stack trace follows.",ioe);
		}
		return false;
	}
	/**
	 * Performs a binary search in the lexicon in order to locate the given
	 * term. If the term is located, the properties termCharacters,
	 * documentFrequency, termFrequency, startOffset, startBitOffset, endOffset
	 * and endBitOffset contain the values related to the term.
	 * 
	 * @param _term the term to search for.
	 * @return true if the term is found, and false otherwise.
	 */
	public boolean findTerm(String _term) {
		Arrays.fill(buffer, (byte) 0);
		Arrays.fill(bt, (byte) 0);
		byte[] bt = _term.getBytes(); String tmpTerm = null;
		//int termLength = ApplicationSetup.STRING_BYTE_LENGTH;			
		//int _termId = 0;
		long low = -1;
		long high = numberOfLexiconEntries;
		long i;
		while (high-low>1) {
			
			i = (long)(high+low)/2;
			try {
				lexiconFile.seek((long)i * (long)UTFBlockLexicon.lexiconEntryLength);
				tmpTerm = lexiconFile.readUTF();
				lexiconFile.readFully(bt, 0, ApplicationSetup.STRING_BYTE_LENGTH - StringTools.utf8_length(term));
			} catch (IOException ioe) {
				logger.fatal(
					"Input/Output exception while reading from lexicon file. Stack trace follows.",ioe);
			}
			
			int compareResult = 0;
			compareResult = _term.compareTo(tmpTerm);
			
			if (compareResult < 1)
				high = i;
			else
				low = i;			
		}
		if (high == numberOfLexiconEntries)
			return false;
		try {
			lexiconFile.seek((long)high * (long)UTFBlockLexicon.lexiconEntryLength);
			tmpTerm = lexiconFile.readUTF();
			lexiconFile.readFully(bt, 0, ApplicationSetup.STRING_BYTE_LENGTH - StringTools.utf8_length(term));
		} catch (IOException ioe) {
			logger.fatal(
				"Input/Output exception while reading from lexicon file. Stack trace follows.",ioe);
		}	
		
		if (_term.compareTo(tmpTerm)==0) {
			try {
				findTerm(lexiconFile.readInt());
				return true;
			}catch(IOException ioe) {
				logger.fatal("Input/Output exception while reading from lexicon file. Stack trace follows.",ioe);
			}
		}
		return false;
	}

	/**
	 * Returns the block frequency for the given term
	 * @return int The block frequency for the given term
	 */
	public int getBlockFrequency() {
		return blockFrequency;
	}
	/**
	 * Seeks the i-th entry of the lexicon.
	 * @param i
	 *            The index of the entry we are looking for.
	 * @return true if the entry was found, false otherwise.
	 */
	public boolean seekEntry(int i) {
		try {
			if (i > numberOfLexiconEntries)
				return false;
			if (i == 0) {
				lexiconFile.seek((long)i * (long)lexiconEntryLength);
				startOffset = 0;
				startBitOffset = 0;
				term = lexiconFile.readUTF();
				lexiconFile.readFully(bt, 0, ApplicationSetup.STRING_BYTE_LENGTH - StringTools.utf8_length(term));
				
				termId = lexiconFile.readInt();
				documentFrequency = lexiconFile.readInt();
				blockFrequency = lexiconFile.readInt();
				termFrequency = lexiconFile.readInt();
				endOffset = lexiconFile.readLong();
				endBitOffset = lexiconFile.readByte();
				return true;
			} else {
				lexiconFile.seek((long)i * (long)lexiconEntryLength - (long)lexiconEntryLength
						+ 2L + (long)ApplicationSetup.STRING_BYTE_LENGTH + 12L);
				startOffset = lexiconFile.readLong();
				startBitOffset = lexiconFile.readByte();
				startBitOffset++;
				if (startBitOffset == 8) {
					startBitOffset = 0;
					startOffset++;
				}
				
				term = lexiconFile.readUTF();
				lexiconFile.readFully(bt, 0, ApplicationSetup.STRING_BYTE_LENGTH - StringTools.utf8_length(term));
				
				termId = lexiconFile.readInt();
				documentFrequency = lexiconFile.readInt();
				blockFrequency = lexiconFile.readInt();
				termFrequency = lexiconFile.readInt();
				endOffset = lexiconFile.readLong();
				endBitOffset = lexiconFile.readByte();
				return true;
			}
		} catch (IOException ioe) {
			logger.fatal("Input/Output exception while reading the idToOffset file. Stack trace follows.",ioe);
		}
		return false;
	}
	
	/**
	 * In an already stored entry in the lexicon file, the information about the
	 * term frequency, the endOffset in bytes, and the endBitOffset in the last
	 * byte, is updated. The term is specified by the index of the entry.
	 * 
	 * @return true if the information is updated properly, otherwise return
	 *         false
	 * @param i the i-th entry
	 * @param frequency the term's Frequency
	 * @param endOffset the offset of the ending byte in the inverted file
	 * @param endBitOffset the offset in bits in the ending byte in the term's entry in
	 *            inverted file
	 * @deprecated Block Lexicons are used during indexing, but not during
	 *             retrieval.
	 */
	public boolean updateEntry(int i, int frequency, long endOffset,
			byte endBitOffset) {
		if (! (lexiconFile instanceof RandomDataOutput))
            return false;
        RandomDataOutput _lexiconFile = (RandomDataOutput)lexiconFile;
		try {
			long lexiconOffset = (long)i * (long)lexiconEntryLength;
			//we seek the offset where the frequency should be writen
			_lexiconFile.seek(lexiconOffset
					+ (long)ApplicationSetup.STRING_BYTE_LENGTH + 8L);
			_lexiconFile.writeInt(frequency);
			_lexiconFile.writeLong(endOffset);
			_lexiconFile.writeByte(endBitOffset);
		} catch (IOException ioe) {
			logger.fatal("Input/Output exception while updating the lexicon file. Stack trace follows.",ioe);
		}
		return false;
	}

	/** returns the number of entries in the lexicon named by f  */
    public static int numberOfEntries(File f)
    {
        return (int)(f.length()/ (long)lexiconEntryLength);
    }

	/** returns the number of entries in the lexicon named by filename */
    public static int numberOfEntries(String filename)
    {
        return numberOfEntries(new File(filename));
    }

}

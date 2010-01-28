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
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Gianni Amati <gba{a.}fub.it> (original author)
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
 *   Craig Macdonald <craigm{a.}.dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.structures;
import java.io.File;
import java.io.IOException;

import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.StringTools;
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
 * @author Gianni Amati, Vassilis Plachouras, Craig Macdonald
 * @version $Revision: 1.1 $
 */
public class UTFLexicon extends Lexicon {
	/** The logger used */
	private static Logger logger = Logger.getRootLogger();
	/** The term represented as an array of bytes.*/
	protected byte[] termCharacters;

	/** 
	 * The size in bytes of an entry in the lexicon file.
	 * An entry corresponds to a string, an int (termCode), 
	 * an int (docf), an int (tf), a long (the offset of the end 
	 * of the term's entry in bytes in the inverted file) and
	 * a byte (the offset in bits of the last byte of the term's entry 
	 * in the inverted file.
	 */
	public static final int lexiconEntryLength =
	
		2+ //two bytes for length written by writeUTF
		ApplicationSetup.STRING_BYTE_LENGTH //the byte representation of the string, ie 3* MAX_TERM_LENGTH
		
		+12 //the three integers
		+8 //the long
		+1; //the byte
	
	/** 
	 * A default constructor.
	 */
	public UTFLexicon() {
		super();
		try {
			numberOfLexiconEntries = (int) (lexiconFile.length() / (long)UTFLexicon.lexiconEntryLength);
			bufferInput.mark(3 * lexiconEntryLength);
		} catch (IOException ioe) {
			logger.fatal(
				"Input/output exception while opening for reading the lexicon file." +
				" Stack trace follows",ioe);
		}
		inputStreamClass = UTFLexiconInputStream.class;
	}
    public UTFLexicon(String path, String prefix)
    {
        this(path + ApplicationSetup.FILE_SEPARATOR + prefix + ApplicationSetup.LEXICONSUFFIX);
    }

	/**
	 * Constructs an instace of Lexicon and opens
	 * the corresponding file.
	 * 
	 * @param lexiconName the name of the lexicon file.
	 */
	public UTFLexicon(String lexiconName) {
		super(lexiconName);
		try {
			numberOfLexiconEntries = (int) (lexiconFile.length() / (long)UTFLexicon.lexiconEntryLength);
			bufferInput.mark(3 * lexiconEntryLength);
		} catch (IOException ioe) {
			logger.fatal(
				"Input/output exception while opening for reading the lexicon file. Stack trace follows",ioe);
		}
		inputStreamClass = UTFLexiconInputStream.class;
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
			long lexiconOffset = idToOffsetFile.readLong();
			if (lexiconOffset == 0) {
				startOffset = 0;
				startBitOffset = 0;
				lexiconFile.seek(lexiconOffset);
				
				term = lexiconFile.readUTF();				
				lexiconFile.readFully(bt, 0, ApplicationSetup.STRING_BYTE_LENGTH - StringTools.utf8_length(term));
				
				termId = lexiconFile.readInt();
				documentFrequency = lexiconFile.readInt();
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
				
				termId = lexiconFile.readInt();
				documentFrequency = lexiconFile.readInt();
				termFrequency = lexiconFile.readInt();
				endOffset = lexiconFile.readLong();
				endBitOffset = lexiconFile.readByte();
				return true;
			}
		} catch (IOException ioe) {
			logger.fatal(
				"Input/Output exception while reading the idToOffset file. Stack trace follows.",ioe);
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
		byte[] bt = new byte[ApplicationSetup.STRING_BYTE_LENGTH];
		
		//int termLength = ApplicationSetup.STRING_BYTE_LENGTH;			
		//int _termId = 0;
		long low = -1;
		long high = numberOfLexiconEntries;
		long i;
		String currentTerm = null;
		while (high-low>1) {
			
			i = (long)(high+low)/2;
			try {
				lexiconFile.seek((long)i * (long)lexiconEntryLength);
				currentTerm = lexiconFile.readUTF();
				//we don't need to take in the padding as we're seeking between entries
			} catch (IOException ioe) {
				logger.fatal(
					"Input/Output exception while reading from lexicon file. Stack trace follows.",ioe);
			}
			
			if (_term.compareTo(currentTerm) < 1)
				high = i;
			else
				low = i;
		}
		if (high == numberOfLexiconEntries)
			return false;
		try {
			lexiconFile.seek((long)high * (long)lexiconEntryLength);
			currentTerm = lexiconFile.readUTF();
		} catch (IOException ioe) {
			logger.fatal(
				"Input/Output exception while reading from lexicon file. Stack trace follows.",ioe);
		}	
		
		if (_term.compareTo(currentTerm) == 0) {
			try {
				lexiconFile.readFully(bt, 0, ApplicationSetup.STRING_BYTE_LENGTH- StringTools.utf8_length(currentTerm));
				findTerm(lexiconFile.readInt());
				return true;
			}catch(IOException ioe) {
				logger.fatal("Input/Output exception while reading from lexicon file. Stack trace follows.",ioe);
			}
		}
		return false;
	}

	/**
	 * Seeks the i-th entry of the lexicon.
	 * TODO read a byte array from the file and decode it, 
	 * 		instead of reading the different pieces of 
	 *      information separately.
	 * @param i The index of the entry we are looking for.
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
				termFrequency = lexiconFile.readInt();
				endOffset = lexiconFile.readLong();
				endBitOffset = lexiconFile.readByte();
				return true;
			} else {
				lexiconFile.seek(
					(long)i * (long)lexiconEntryLength
						- (long)lexiconEntryLength
						+ 2L//two bytes for the string length written by writeUTF
						+ (long)ApplicationSetup.STRING_BYTE_LENGTH
						+ 12L);
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
				termFrequency = lexiconFile.readInt();
				endOffset = lexiconFile.readLong();
				endBitOffset = lexiconFile.readByte();
				return true;
			}
		} catch (IOException ioe) {
			logger.fatal(
				"Input/Output exception while reading the idToOffset file. " +
				"Stack trace follows.",ioe);
		}
		return false;
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
		int high = (int)numberOfLexiconEntries;
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
					term = lexiconFile.readUTF();
						//new String(buffer,0,ApplicationSetup.STRING_BYTE_LENGTH).trim();
				} else {
					lexiconFile.seek((long)i * (long)lexiconEntryLength);
					term = lexiconFile.readUTF();
					//term = new String(buffer,9,ApplicationSetup.STRING_BYTE_LENGTH).trim();
				}
							
				if ((compareStrings = _term.compareTo(term))< 0)
					high = i;
				else if (compareStrings > 0)
					low = i;
				else { //read the rest and return the data
					if (i==0)
					{
						lexiconFile.seek(0);
						lexiconFile.readFully(buffer, 0, lexiconEntryLength);
					}
					else
					{
						lexiconFile.seek((long)i * (long)(lexiconEntryLength) -9);
						lexiconFile.readFully(buffer, 0, lexiconEntryLength+9);
					}
					return getLexiconEntryFromBuffer(buffer, term, i);
				}
			}
		
			if (high == numberOfLexiconEntries)
				return null;
			
			if (high == 0) {
				lexiconFile.seek(0);
				term = lexiconFile.readUTF();
				lexiconFile.seek(0);
				lexiconFile.readFully(buffer, 0, lexiconEntryLength);
			} else {
				lexiconFile.seek((long)high * (long)lexiconEntryLength);
				term = lexiconFile.readUTF();
				lexiconFile.seek((long)high * (long)(lexiconEntryLength) -9);
				lexiconFile.readFully(buffer, 0, lexiconEntryLength+9);
			}
			
			if (_term.compareTo(term) == 0) {
				return getLexiconEntryFromBuffer(buffer, term, high);
			}	
		} catch(IOException ioe) {
			logger.fatal("IOException while binary searching the lexicon: " , ioe);
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
			offset = ApplicationSetup.STRING_BYTE_LENGTH+2;
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

			offset += 2+ApplicationSetup.STRING_BYTE_LENGTH;
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

	
	/**
	 * In an already stored entry in the lexicon
	 * file, the information about the term frequency,
	 * the endOffset in bytes, and the endBitOffset in the last
	 * byte, is updated. The term is specified by the index of the entry.
	 *
	 * @return true if the information is updated properly, 
	 *         otherwise return false
	 * @param i the i-th entry
	 * @param frequency the term's Frequency
	 * @param endOffset the offset of the ending byte in the inverted file
	 * @param endBitOffset the offset in bits in the ending byte 
	 *        in the term's entry in inverted file
	 * @deprecated The Lexicon class is only used for reading the
	 *             lexicon file, and not for writing any information.
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
			_lexiconFile.seek(  //utf length, string max length, termid, tf 
				lexiconOffset + 2+ ApplicationSetup.STRING_BYTE_LENGTH + 8);
			_lexiconFile.writeInt(frequency);
			_lexiconFile.writeLong(endOffset);
			_lexiconFile.writeByte(endBitOffset);
		} catch (IOException ioe) {
			logger.fatal(
				"Input/Output exception while updating the lexicon file. " +
				"Stack trace follows.",ioe);
		}
		return false;
	}

    public static int numberOfEntries(File f)
    {
        return (int)(f.length()/ (long)lexiconEntryLength);
    }

    public static int numberOfEntries(String filename)
    {
        return numberOfEntries(new File(filename));
    }

}

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
 * The Original Code is BitInputStream.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Gianni Amati <gba{a.}fub.it> (original author)
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
 *   Douglas Johnson <johnsoda{a.}dcs.gla.ac.uk> 
 */
package uk.ac.gla.terrier.compression;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import uk.ac.gla.terrier.utility.Files;
/**
 * This class provides sequential stream access to a compressed BitFile file.
 * @author Gianni Amati, Vassilis Plachouras, Douglas Johnson
 * @version $Revision: 1.1 $
 * @see uk.ac.gla.terrier.compression.BitFile
 */
public class OldBitInputStream extends BitInputStream {
	/**
	 * Reads a binary integer from the already read buffer.
	 * No IO is performed and 0 is returned if noBits == 0.
	 * <b>NB</b>: noBits &gt; than 32 will give undefined results.
	 * @param noBits the number of binary bits to read
	 * @throws IOException if an I/O error occurs
	 * @return the decoded integer, -1 if eof was reached unexpectedly
	 */
	public int readBinary(final int noBits) throws IOException {
		if (noBits == 0)
			return 0;
		int binary = 0;
		boolean endOfFileExpected = false;
		try {
			for (int i = 0; i < noBits; i++) {
				if ((byteRead & (1 << (bitOffset))) != 0) {
					binary = binary + (1 << i);
				}
				bitOffset++;
				if (bitOffset == 8) {
					bitOffset = 0;
					byteOffset++;
					endOfFileExpected = true;
					byteRead = dis.readByte();
				}
			}
		} catch (EOFException eofe) {
			if (endOfFileExpected)
				return binary;
			else //TODO: shouldn't the eof be propagated if unexpected??
				return -1;
		}
		return binary;
	}

	/** Skip a number of bits in the current input stream
	 * @param noBits The number of bits to skip
	 */
	public void skipBits(final int noBits) throws IOException {
		if (noBits == 0)
            return;
		boolean endOfFileExpected = false;
		try {
			for (int i = 0; i < noBits; i++) {
				bitOffset++;
                if (bitOffset == 8) {
                    bitOffset = 0;
                    byteOffset++;
                    endOfFileExpected = true;
                    byteRead = dis.readByte();
                }
			}
		} catch (EOFException eofe) {
            if (endOfFileExpected)
                return;
            else throw new RuntimeException(eofe);
        }
		return;
	}	

	/** The private input stream used internaly.*/
	private DataInputStream dis = null;
	/** The byte offset.*/
	private long byteOffset;
	/** The bit offset.*/
	private byte bitOffset;
	/** 
	 * A byte read from the stream. This byte should be 
	 * initialised during the construction of the class.
	 */
	private byte byteRead;
	/**
	 * Constructs an instance of the class for a given stream. We recommend that
	 * the inputstream passed to this class if buffered in some way - ie using
	 * java.io.BufferedInputStream
	 * @param is java.io.InputStream the underlying input stream
	 * @throws java.io.IOException if an I/O error occurs
	 */
	public OldBitInputStream(InputStream is) throws IOException {
		dis = new DataInputStream(is);
		byteOffset = 0;
		bitOffset = 0;
		byteRead = dis.readByte();
	}
	/** 
	 * Constructs an instance of the class for a given filename
	 * @param filename java.lang.String the name of the undelying file
	 * @throws java.io.IOException if an I/O error occurs
	 */
	public OldBitInputStream(String filename) throws IOException {
		dis = new DataInputStream(Files.openFileStream(filename));
		byteOffset = 0;
		bitOffset = 0;
		byteRead = dis.readByte();
	}
	/**
	 * Constructs an instance of the class for a given file
	 * @param file java.io.File the underlying file
	 * @throws java.io.IOException if an I/O error occurs
	 */
	public OldBitInputStream(File file) throws IOException {
		dis = new DataInputStream( Files.openFileStream(file));
		byteOffset = 0;
		bitOffset = 0;
		byteRead = dis.readByte();
	}
	/** 
	 * Closes the stream.
	 * @throws java.io.IOException if an I/O error occurs
	 */
	public void close() throws IOException {
		dis.close();
	}
	/**
	 * Reads a unary encoded integer from the stream.
	 * @return the decoded integer, or -1 if EOF is found
	 * @throws java.io.IOException if an I/O error occurs
	 */
	public int readUnary() throws IOException {
		int result = 0;
		boolean endOfFileExpected = false;
		try {
			while (true) {
				if ((byteRead & (1 << (bitOffset))) != 0) {
					result++;
					bitOffset++;
					if (bitOffset == 8) {
						bitOffset = 0;
						byteOffset++;
						byteRead = dis.readByte();
					}
				} else {
					result++;
					bitOffset++;
					if (bitOffset == 8) {
						bitOffset = 0;
						byteOffset++;
						endOfFileExpected = true;
						byteRead = dis.readByte();
					}
					break;
				}
			}
		} catch (EOFException eofe) {
			if (endOfFileExpected) 
				return result;
			else 
				return -1;
		}
		return result;
	}
	/**
	 * Reads a gamma encoded integer from the stream
	 * @return the decoded integer, or -1 if EOF is found
	 * @throws java.io.IOException if an I/O error occurs
	 */
	public int readGamma() throws IOException {
		int result = 0;
		int binaryPart = 0;
		boolean endOfFileExpected = false;
		int unaryPart = readUnary();
		if (unaryPart == -1) {
			return -1;
		}
		try {
			for (int i = 0; i < unaryPart - 1; i++) {
				if ((byteRead & (1 << (bitOffset))) != 0) {
					binaryPart = binaryPart + (1 << i);
				}
				bitOffset++;
				if (bitOffset == 8) {
					bitOffset = 0;
					byteOffset++;
					endOfFileExpected = true;
					byteRead = dis.readByte();
				}
			}
			result = binaryPart + (1 << (unaryPart - 1));
		} catch (EOFException eofe) {
			if (endOfFileExpected)
				return (binaryPart + (1 << (unaryPart - 1)));
			else 
				return -1;
		}
		return result;
	}
	/**
	 * Returns the byte offset of the stream. 
	 * It corresponds to the offset of the 
	 * byte from which the next bit will be read.
	 * @return the byte offset in the stream.
	 */
	public long getByteOffset() {
		return byteOffset;
	}
	/**
	 * Returns the bit offset in the last byte. It 
	 * corresponds to the next bit that it will be
	 * read.
	 * @return the bit offset in the stream.
	 */
	public byte getBitOffset() {
		return bitOffset;
	}


    /**
     * Aligns the stream to the next byte
     * @throws IOException if an I/O error occurs
     */
    public void align() throws IOException {
        if ( ( bitOffset & 7 ) == 0 ) return;
        bitOffset = 0;
        byteOffset++;
        byteRead = dis.readByte();
    }
}

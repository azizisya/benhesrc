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
 * The Original Code is BitIn.java.
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *  Craig Macdonald <craigm{a.}dcs.gla.ac.uk
 */

package org.terrier.compression;


import java.io.Closeable;
import java.io.IOException;

/** Interface describing the read compression methods supported
 * by the BitFile and BitInputStream classes.
 * @author Craig Macdonald
 * @version $Revision: 1.4 $
 * @since 2.0
 */
public interface BitIn extends Closeable {

	public static final String USUAL_EXTENSION = ".bf";
	
	/**
	 * Returns the byte offset of the stream.
	 * It corresponds to the position of the 
	 * byte in which the next bit will be written.
	 * Use only when writting
	 * @return the byte offset in the stream.
	 */
	public long getByteOffset();
	/**
	 * Returns the bit offset in the last byte.
	 * It corresponds to the position in which
	 * the next bit will be written.
	 * Use only when writting.
	 * @return the bit offset in the stream.
	 */
	public byte getBitOffset();
	
	/**
	 * Reads a unary encoded integer from the underlying stream 
	 * @return the number read
	 * @throws IOException if an I/O error occurs
	 */
	public int readUnary() throws IOException;
	/**
	 * Reads a gamma encoded integer from the underlying stream
	 * @return the number read
	 * @throws IOException if an I/O error occurs
	 */
	public int readGamma() throws IOException;
	/**
	 * Reads a binary integer from the already read buffer.
	 * @param len the number of binary bits to read
	 * @throws IOException if an I/O error occurs
	 * @return the decoded integer
	 */
	public int readBinary(int len) throws IOException;

	/**
	 * Reads a binary encoded integer, given an upper bound
	 * @param b the upper bound
	 * @return the int read
	 * @throws IOException if an I/O error occurs
	 */
	public int readMinimalBinary( final int b ) throws IOException;	
	
	/**
	 * Reads a minimal binary encoded number, when the upper bound can b zero.
	 * Used to interpolative code	
	 * @param b the upper bound
	 * @return the int read
	 * @throws IOException if an I/O error occurs
	 */
	public int readMinimalBinaryZero(int b) throws IOException;

	/**
	 * Reads a Golomb encoded integer 
	 * @param b the golomb modulus
	 * @return the int read 
	 * @throws IOException if and I/O error occurs
	 */
	public int readGolomb( final int b) throws IOException;
	
	/**
	 * Reads a skewed-golomb encoded integer from the underlying stream
	 * Consider a bucket-vector <code>v = &lt;0, 2b, 4b, ... , 2^i b, ...&gt; </code>
	 * The sum of the elements in the vector goes
	 * 	<code>b, 3b, 7b, 2^(i-1)*b</code>
	 *  
	 * @return the number read
	 * @throws IOException if an I/O error occurs 
	 */	
	public int readSkewedGolomb( final int b ) throws IOException;
	
	/**
	 * Reads a delta encoded integer from the underlying stream
	 * @return the number read
	 * @throws IOException if an I/O error occurs 
	 */
	public int readDelta() throws IOException;
	
	/**
	 * Reads a sequence of numbers from the stream interpolative coded.
	 * @param data the result vector
	 * @param offset offset where to write in the vector
	 * @param len the number of integers to decode.
	 * @param lo a lower bound (the same one passed to writeInterpolativeCoding)
	 * @param hi an upper bound (the same one passed to writeInterpolativeCoding)
	 * @throws IOException if an I/O error occurs
	 */
	public void readInterpolativeCoding( int data[], int offset, int len, int lo, int hi ) throws IOException;
	
	
	/** Skip a number of bits while reading the bit file.
	 * @param len The number of bits to skip
	 * @throws IOException if an I/O error occurs
	 */
    public void skipBits(int len) throws IOException;
    
    /** Skip a number of bytes while reading the bit file.
     * After this opteration, getBitOffset() == 0, so use
     * skipBits to get getBitOffset() to desired value.
	 * @param len The number of bytes to skip
	 * @throws IOException if an I/O error occurs
	 */
    public void skipBytes(long len) throws IOException;

    /**
     * Aligns the stream to the next byte
     * @throws IOException if an I/O error occurs
     */
    public void align() throws IOException;
}

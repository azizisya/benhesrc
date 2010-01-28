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
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *  Craig Macdonald <craigm{a.}dcs.gla.ac.uk
 */

package uk.ac.gla.terrier.compression;


import java.io.Closeable;
import java.io.IOException;

/** Interface describing the read compression methods supported
 * by the BitFile and BitInputStream classes.
 * @author Craig Macdonald
 * @version $Revision: 1.1 $
 * @since 2.0
 */
public interface BitIn extends Closeable {

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

	/** Skip a number of bits while reading the bit file.
	 * @param len The number of bits to skip
	 * @throws IOException if an I/O error occurs
	 */
    public void skipBits(int len) throws IOException;

    /**
     * Aligns the stream to the next byte
     * @throws IOException if an I/O error occurs
     */
    public void align() throws IOException;
}

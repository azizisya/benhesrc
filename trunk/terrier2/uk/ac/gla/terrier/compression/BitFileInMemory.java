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
 * The Original Code is BitFile.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *  Craig Macdonald <craigm{a.}dcs.gla.ac.uk
 */

package uk.ac.gla.terrier.compression;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.utility.Files;

/** Class which enables a BitFile to be read in memory
  * @author Craig Macdonald
  * @version $Revision: 1.1 $
  */
public class BitFileInMemory implements BitInSeekable
{
	/** The logger used */
    protected static Logger logger = Logger.getRootLogger();
	/** buffer in memory for the entire compressed file */
	protected final byte[] inBuffer;
	/** Loads the entire file represented by filename into memory 
	 * @param filename Location of bit file to read into memory
	 * @throws IOException if anything goes wrong reading the file */
	public BitFileInMemory(String filename) throws IOException {
		this(Files.openFileStream(filename), Files.length(filename));
	}
	
	/** Loads the entire specified inputstream into memory. Length is assumed to be
	  * as specified.
	  * @param is InputStream containing the compressed bitfile 
	  * @param length Expected length of the input stream
	  * @throws IOException if anything goes wrong reading the inputstream */
	public BitFileInMemory(final InputStream is, final long length) throws IOException
	{
		
		if (length > Integer.MAX_VALUE)
		{
			logger.fatal("File too big for BitFileInMemory");
			inBuffer = new byte[0];
			return;
		}
		inBuffer = new byte[(int)length];
		final DataInputStream dis = new DataInputStream(is);
		dis.readFully(inBuffer);
		dis.close();
	}

	/** Create an object using the specified data as the compressed data */
	public BitFileInMemory(final byte[] buffer)
	{
		inBuffer = buffer;
	}

	/**
	 * Reads from the file a specific number of bytes and after this
	 * call, a sequence of read calls may follow. The offsets given
	 * as arguments are inclusive. For example, if we call this method
	 * with arguments 0, 2, 1, 7, it will read in a buffer the contents
	 * of the underlying file from the third bit of the first byte to the
	 * last bit of the second byte.
	 * @param startByteOffset the starting byte to read from
	 * @param startBitOffset the bit offset in the starting byte
	 * @param endByteOffset the ending byte
	 *  @param endBitOffset the bit offset in the ending byte.
	 *        This bit is the last bit of this entry.
	 * @return Returns the BitIn object to use to read that data
	 */
	public BitIn readReset(long startByteOffset, byte startBitOffset, long endByteOffset, byte endBitOffset) 
	{
		return new BitInReader(startByteOffset, startBitOffset, endByteOffset, endBitOffset);
	}

	/** Close this object. Does nothing. */
	public void close()
	{
		//do nothing
	}

	class BitInReader implements BitIn
	{
		/** The bit offset.*/
    	protected int bitOffset;
		protected int readByteOffset;
		
		public BitInReader(long startByteOffset, byte startBitOffset, long endByteOffset, byte endBitOffset)
		{
			
			readByteOffset = (int)startByteOffset;
			bitOffset = startBitOffset;
		}
		/**
		* Returns the byte offset of the stream.
		* It corresponds to the position of the
		* byte in which the next bit will be written.
		* Use only when writting
		* @return the byte offset in the stream.
		*/
		public long getByteOffset() {
			return readByteOffset;
		}
		/**
		* Returns the bit offset in the last byte.
		* It corresponds to the position in which
		* the next bit will be written.
		* @return the bit offset in the stream.
		*/
		public byte getBitOffset() {
			return (byte)bitOffset;
		}
	
		/**
		 * Reads a gamma encoded integer from the underlying stream
		 *  @return the number read
		 */
		public int readGamma()  {
			int u = readUnary() - 1;
			return (1 << u) + readBinary(u) ;
		}

		/**
		 * Reads a unary encoded integer from the underlying stream
		 * @return the number read
		 */
		public int readUnary()  {
			int x;
			final int leftA = (inBuffer[readByteOffset] << bitOffset) & 0x00FF;
			if(leftA != 0){
				x = 8 - BitUtilities.MSB_BYTES[ leftA ];
				bitOffset += x ;
				readIn();
				return x;
			}
			x = 8 - bitOffset;
			readByteOffset++;
			while( (inBuffer[readByteOffset]== 0 )) {
				x += 8;
				readByteOffset++;
			}
			x += (bitOffset =  8 -  BitUtilities.MSB_BYTES[ inBuffer[readByteOffset] & 0x00FF] );
			readIn();
			return x;
		}

		/**
		 * Reads a new byte from the InputStream if we have finished with the current one.	
		 */
		protected void readIn(){
			if(bitOffset == 8){
				bitOffset = 0;
				readByteOffset++;
			}
		}
		/**
		 * Aligns the stream to the next byte
		 */
		public void align() {
			if ( ( bitOffset & 7 ) == 0 ) return;
			bitOffset = 0;
			readByteOffset++;
		}

		/**
		 * Reads a binary integer from the already read buffer.
		 * @param len is the number of binary bits to read
		 * @throws IOException if an I/O error occurs
		 * @return the decoded integer
		 */
		public int readBinary(int len) {
			if(8 - bitOffset > len){
				int b = ( ((inBuffer[readByteOffset] << bitOffset) & 0x00FF)) >>> (8-len) ;
				bitOffset += len;
				return b;
			}
		
			int x = inBuffer[readByteOffset] & ( ~ (0xFF << (8-bitOffset) )) &0xFF;
			len +=  bitOffset - 8;
			int i = len >> 3;
			while(i-- != 0){
				readByteOffset++;
				x = x << 8 | (inBuffer[readByteOffset] & 0xFF);
			}
			readByteOffset++;
			bitOffset = len & 7;
			return (x << bitOffset) | ((inBuffer[readByteOffset] & 0xFF) >>> (8-bitOffset)) ;
		}

		/** Skip a number of bits in the current input stream
			* @param len The number of bits to skip
			*/
		public void skipBits(int len)
		{
			if(8 - bitOffset > len){
				bitOffset += len;
				return;
			}
			len +=  bitOffset - 8;
			final int i = len >> 3;
			if (i > 0)
			{
				readByteOffset+= i;
			}
			readByteOffset++;
			bitOffset = len & 7;
		}

		/** nothing to do */
		public void close() {}
	}
}

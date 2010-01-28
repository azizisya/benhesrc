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
 * The Original Code is BitOutputStream.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Gianni Amati <gba{a.}fub.it> (original author)
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
 *   Douglas Johnson <johnsoda{a.}dcs.gla.ac.uk> 
 */
package uk.ac.gla.terrier.compression;
import org.apache.log4j.Logger;
import java.io.*;
import uk.ac.gla.terrier.utility.Files;
/**
 * This class creates sequentially a BitFile file from a stream.
 * @author Gianni Amati, Vassilis Plachouras, Douglas Johnson
 * @version $Revision: 1.1 $
 * @see uk.ac.gla.terrier.compression.BitFile
 * @deprecated
 */
public class OldBitOutputStream extends BitOutputStream {
	/** The logger used */
    protected static Logger logger = Logger.getRootLogger();
	/**
	 * Writes a binary integer to the already read buffer.
	 * <b>NB</b>: bitsToWrite &gt; than 32 will give undefined results.
	 * @param bitsToWrite the number of bits to use for encoding
	 * @param n the integer
	 * @return should return number of bits but it doesn't
	 */
	public int writeBinary(int bitsToWrite, int n) throws IOException {
		byte rem;
		while (n != 0) {
			rem = (byte) (n % 2);
			byteToWrite |= (rem << bitOffset);
			bitOffset++;
			if (bitOffset == 8) {
				bitOffset = 0;
				byteOffset++;
				dos.writeByte(byteToWrite);
				//dos.flush(); //let the IO layer decide when to flush
				byteToWrite = 0;
			}
			n = n / 2;
			bitsToWrite--;
		}
		while (bitsToWrite >0) {
			bitOffset++;
			if (bitOffset == 8) {
				bitOffset = 0;
				byteOffset++;
				dos.write(byteToWrite);
				//dos.flush(); //let the IO layer decide when to flush
				byteToWrite = 0;
			}
			bitsToWrite--;
		}
		return -1;
	}	
	/** The private output stream used internaly.*/
	private DataOutputStream dos = null;
	/** The byte offset.*/
	private long byteOffset;
	/** The bit offset.*/
	private byte bitOffset;
	/** A byte to write to the stream. */
	private byte byteToWrite;
	/** 
	 * The natural logarithm of two. It is used
	 * for changing the base of logarithm from
	 * e to 2.
	 */
	private static final double LOG_2 = Math.log(2.0D);
	/**
	 * Constructs an instance of the class for a given stream. The
	 * OutputStream is should probably be buffered using 
	 * java.io.BufferedOutputStream
	 * @param is java.io.OutputStream the underlying input stream
	 * @throws java.io.IOException if an I/O error occurs
	 */
	public OldBitOutputStream(OutputStream is) throws IOException {
		dos = new DataOutputStream(is);
		byteOffset = 0;
		bitOffset = 0;
		byteToWrite = (byte)0;
	}
	/** 
	 * Constructs an instance of the class for a given filename
	 * @param filename java.lang.String the name of the undelying file
	 * @throws java.io.IOException if an I/O error occurs
	 */
	public OldBitOutputStream(String filename) throws IOException {
		dos = new DataOutputStream(Files.writeFileStream(filename));
		byteOffset = 0;
		bitOffset = 0;
		byteToWrite = (byte)0;
	}
	/**
	 * Constructs an instance of the class for a given file
	 * @param file java.io.File the underlying file
	 * @throws java.io.IOException if an I/O error occurs
	 */
	public OldBitOutputStream(File file) throws IOException {
		dos = new DataOutputStream(Files.writeFileStream(file));
		byteOffset = 0;
		bitOffset = 0;
		byteToWrite = (byte)0;
	}
	/** 
	 * Flushes and closes the stream.
	 * @throws java.io.IOException if an I/O error occurs
	 */
	public void close() throws IOException {
		if (bitOffset!=0)
			dos.writeByte(byteToWrite);
		dos.flush(); //force a flush before closing
		dos.close();
	}
	/**
	 * Writes a unary encoded integer to the stream.
	 * @param n int the integer to write.
	 * @throws java.io.IOException if an I/O error occurs
	 * @return should return number of bits but it doesn't
	 */
	public int writeUnary(int n) throws IOException {
		final byte mask = 1;
		//write n-1 ones
		for (int i=0; i<n-1; i++) {
			byteToWrite |= (mask << bitOffset);
			bitOffset++;
			if (bitOffset == 8) {
				bitOffset = 0;
				byteOffset++;
				dos.writeByte(byteToWrite);
				//dos.flush(); //let the IO layer decide when to flush
				byteToWrite = 0;
			}
		}
		//and end with a zero
		bitOffset++;
		if (bitOffset == 8) {
			bitOffset = 0;
			byteOffset++;
			dos.writeByte(byteToWrite);
			//dos.flush(); //let the IO layer decide when to flush
			byteToWrite = 0;
		}
		return -1;
	}
	/** 
	 * Writes an gamma encoded integer to the stream.
	 * @param n The integer to be encoded and saved in the buffer.
	 * @throws java.io.IOException if an I/O error occurs
	* @return should return number of bits but it doesn't
	*/
	public int writeGamma(int n) throws IOException {
		final byte mask = 1;
		final int floor = (int) Math.floor(Math.log(n)/LOG_2);
		final int secondPart = (int) (n - (1<<(floor))) /*(int) (n - Math.pow(2, floor))*/;
		//write first part as a unary
		writeUnary(1+floor);//was called firstPart
		
		//write the second part as binary
		for (int i=0; i<floor; i++) {
			if ((secondPart & (1 << i)) != 0) {
				byteToWrite |= (mask << bitOffset);
				bitOffset++;
				if (bitOffset == 8) {
					bitOffset = 0;
					byteOffset++;
					dos.writeByte(byteToWrite);
					byteToWrite = 0;
				}
			} else {
				bitOffset++;
				if (bitOffset == 8) {
					bitOffset = 0;
					byteOffset++;
					dos.writeByte(byteToWrite);
					byteToWrite = 0;
				}				
			}
		}	
		return -1;
	}
	/**
	 * Returns the byte offset of the stream.
	 * It corresponds to the position of the 
	 * byte in which the next bit will be written.
	 * @return the byte offset in the stream.
	 */
	public long getByteOffset() {
		return byteOffset;
	}
	/**
	 * Returns the bit offset in the last byte.
	 * It corresponds to the position in which
	 * the next bit will be written.
	 * @return the bit offset in the stream.
	 */
	public byte getBitOffset() {
		return bitOffset;
	}
	
	/** Flush the underlying DataOutputStream to disk */
	public void flush()
	{
		try{
			dos.flush();
		}catch(Exception e){logger.warn("Problem flushing BitOutputStream", e);}
	}
}

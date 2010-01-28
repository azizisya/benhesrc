
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
 * The Original Code is BitByteOutputStream.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *  Roi Blanco (original author) 
 */


package uk.ac.gla.terrier.compression;

import java.io.DataOutputStream;
import java.io.File;
import uk.ac.gla.terrier.utility.Files;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This class provides methods to write compressed integers to an outputstream.<br>
 * The numbers are written into a byte starting from the most significant bit (i.e, left to right).
 * This class does not use any buffering, it relies on the underlying OutputStream
 * to decide when to flush to disk. 
 * It is used for indexing, when keeping many compresed streams in memory (MemoryOutputStream) and a two-level
 * buffering is unnecessary (and slower).  
 * 
 * @author Roi Blanco
 *
 */
public class BitByteOutputStream extends BitOutputStream{
	
	
	/** The private output stream used internaly.*/	
	protected DataOutputStream dos = null;
	/** The byte offset.*/	
	protected long byteOffset;
	/** The bit offset.*/	
	protected int bitOffset;	
	/** A byte to write to the stream. */
	protected byte byteToWrite;
	/** Temporal buffer used for writting ints */
	private byte tempBuffer[] = new byte[ TEMP_BUFFER_SIZE ];
	/** Size of the temporal buffer used for writting ints into the stream. Value is 128 */
	final static int TEMP_BUFFER_SIZE = 128;
		
	/**
	 * Constructs an instance of the class for a given stream
	 * @param is java.io.OutputStream the underlying input stream
	 * @throws java.io.IOException if an I/O error occurs
	 */
	public BitByteOutputStream(OutputStream is) throws IOException {		
		dos = new DataOutputStream(is);
		init();		
	}
	
	/**
	 * Initialises the variables in the stream. Used internally.
	 */
	private void init(){		
		byteToWrite = (byte) 0;
		bitOffset = 0;
		byteOffset = 0;
	}
	
	/** 
	 * Constructs an instance of the class for a given filename
	 * @param filename java.lang.String the name of the underlying file
	 * @throws java.io.IOException if an I/O error occurs
	 */
	public BitByteOutputStream(String filename) throws IOException {
		dos = new DataOutputStream(Files.writeFileStream(filename));
		init();
	}
	
	/**
	 * Constructs an instance of the class for a given file
	 * @param file java.io.File the underlying file
	 * @throws java.io.IOException if an I/O error occurs
	 */
	public BitByteOutputStream(File file) throws IOException {
		dos = new DataOutputStream(Files.writeFileStream(file));
		init();
	}
	
	/** 
	 * Closes the stream.
	 * @throws java.io.IOException if an I/O error occurs
	 */
	public void close() throws IOException {
		if (bitOffset!=0)
			dos.writeByte(byteToWrite);
		dos.flush();
		dos.close();
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
		return (byte)bitOffset;
	}

	/**
	 * Empty constructor, used for subclassing
	 *
	 */
	public BitByteOutputStream(){} 	
	
	/**
	 * Writes an integer x using unary encoding. The encoding is a sequence of x -1 zeroes and 1 one:
	 * 1, 01, 001, 0001, etc ..
	 * This method is not failsafe, it doesn't check if the argument is 0 or negative. 
	 * @param x the number to write
	 * @return the number of bis written
	 * @throws IOException if an I/O error occurs.
	 */
	public int writeUnary( int x ) throws IOException{
		if(x < 9 - bitOffset) return writeInCurrent(1, x);
		final int shift = 8 - bitOffset;
		x -= shift;		
		dos.write( byteToWrite );
		bitOffset = 0;
		byteToWrite = 0;
		int i = x -1  >> 3;		
		byteOffset += i + 1;
		while( i-- != 0 )  dos.write( 0 );
		writeInCurrent( 1, ( (x-1) & 7) + 1  );
		return x + shift ;
	}
	
	/**
	 * Writes a number in the current byte we are using.
	 * @param b the number to write
	 * @param len the length of the number in bits
	 * @return the number of bits written
	 * @throws IOException if an I/O error occurs.
	 */
	private int writeInCurrent( final int b, final int len ) throws IOException {	
		byteToWrite |= b << (8 - (bitOffset+=len));
		if ( bitOffset == 8 ) {			
		     dos.write( byteToWrite );			
			 bitOffset = 0;
			 byteToWrite = 0;
			 byteOffset++;
		}
		return len;
	}
	
	/**
	 * Writes an integer x into the underlying OutputStream. First, it checks if it fits into the current
	 * byte we are using for writting, and then it writes as many bytes as necessary
	 * @param x the int to write
	 * @param len length of the int in bits
	 * @return the number of bits written
	 * @throws IOException if an I/O error occurs.
	 */
	public int writeInt( int x, final int len ) throws IOException {	
		if ( 9 - bitOffset > len  ) return writeInCurrent( x, len );

		// number of bits to be written in the last byte
		final int queue = ( len + bitOffset - 8) & 7; 
		final int blocks =  (len - 8 + bitOffset) >> 3;
		int i = blocks;
		
		if ( queue != 0 ) {
			tempBuffer[ blocks ] = (byte)x;
			x >>= queue;
		}

		byteOffset += i /*+ 1*/;
		
		while( i-- != 0 ) {
			tempBuffer[ i ] = (byte)x;
			x >>>= 8;
		}
		
		writeInCurrent( x, 8 - bitOffset );
		for( i = 0; i < blocks ; i++ ) dos.write( tempBuffer[ i ] );
		writeInCurrent( tempBuffer[blocks] , queue);
		bitOffset = queue;
		return len;
	}
	
	
	/**
	 * Fills the remaining bits of the current byte with 0s
	 * @throws IOException
	 */
	public void pad() throws IOException{
		if (bitOffset != 0) {
			bitOffset = 0;
			byteOffset++;
			dos.writeByte(byteToWrite);
			byteToWrite = 0;
		}
	}
	
	/**	 
	 * @return the actual byte it is being written.
	 */
	public byte getByteToWrite(){
		return byteToWrite;
	}
	
	
	
}

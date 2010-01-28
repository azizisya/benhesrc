/*
 * Terrier - Terabyte Retriever
 * Webpage: http://ir.dcs.gla.ac.uk/terrier
 * Contact: terrier{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * http://www.gla.uk
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
 * The Original Code is DirectInvertedOutputStream.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original author)
 *   
 */
package uk.ac.gla.terrier.structures;

import java.io.IOException;

import uk.ac.gla.terrier.compression.BitOut;
import uk.ac.gla.terrier.compression.BitOutputStream;

/** Writes a block direct or block inverted index, when passed appropriate posting lists.
  * @author Craig Macdonald
  * @since 2.0
  * @version $Revision: 1.1 $
  */
public class DirectInvertedOutputStream implements Closeable {
	/** what to write to */
	protected BitOut output;
	/** the number of field bits to write */
	protected int binaryBits; 
	/** Creates a new output stream, writing a BitOutputStream to the specified file. The number of binary bits
	  * for fields must also be specified.
	  * @param filename Location of the file to write to
	  * @param binaryBits the number of fields in this index 
	  */
	public DirectInvertedOutputStream(String filename, int binaryBits) throws IOException
	{
		this.output = new BitOutputStream(filename);
		this.binaryBits = binaryBits;
	}
	/** Creates a new output stream, writing to the specified BitOut implementation.  The number of binary bits
	  * for fields must also be specified.
	  * @param out BitOut implementation to write the file to 
	  * @param binaryBits the number of fields in this index 
	  */
	public DirectInvertedOutputStream(BitOut out, int binaryBits)
	{
		this.output = out;
	}
	
	/** Write out the specified postings. The delta for the first id must be specified.
	  * @param postings The postings to write out
	  * @param firstId the (delta) value of the first docid to write out.
	  */
	public void writePostings(int[][] postings, int firstId) throws IOException
	{
		if (binaryBits>0) 
			writeFieldPostings(postings, 0, postings[0].length, firstId);
		else 
			writeNoFieldPostings(postings, 0, postings[0].length, firstId);
	}
	
	/** Write out a range of the specified postings. The delta for the first id must be specified.
	  * @param postings The postings to write out
	  * @param startOffset The location of the first posting to write out.
	  * @param Length The number of postings to be written out.
	  * @param firstId the (delta) value of the first docid to write out.
	  */
	public void writePostings(int[][] postings, int startOffset, int Length, int firstId) throws IOException
	{
		if (binaryBits>0) 
			writeFieldPostings(postings, startOffset, Length, firstId);
		else 
			writeNoFieldPostings(postings, startOffset, Length, firstId);
	}
	
	/**
	 * Writes the given postings to the bit file. This method assumes that
	 * field information is provided as well.
	 * @param postings the postings list to write.
	 * @param startOffset The location of the first posting to write out.
	 * @param Length The number of postings to be written out.
	 * @param firstId the first identifier to write. This can be 
	 *        an id plus one, or the gap of the current id and the previous one.
	 */
	protected void writeFieldPostings(int[][] postings, int offset, final int Length, int firstId) 
			throws IOException {
		
		//local variables in order to reduce the number
		//of times we need to access a two-dimensional array
		final int[] postings0 = postings[0];
		final int[] postings1 = postings[1];
		final int[] postings2 = postings[2];
		
		//write the first entry
		output.writeGamma(firstId);
		output.writeUnary(postings1[offset]);
		output.writeBinary(binaryBits, postings2[offset]);
	
		offset++;
		for (; offset < Length; offset++) {
			output.writeGamma(postings0[offset] - postings0[offset - 1]);
			output.writeUnary(postings1[offset]);
			output.writeBinary(binaryBits, postings2[offset]);
		}
	}
	
	/**
	 * Writes the given postings to the bit file. This method assumes that
	 * field information is not provided.
	 * @param postings the postings list to write.
	 * @param firstId the first identifier to write. This can be 
	 *        an id plus one, or the gap of the current id and the previous one.
	 * @param startOffset The location of the first posting to write out.
	 * @param Length The number of postings to be written out.
	 * @throws IOException if an error occurs during writing to a file.
	 */
	protected void writeNoFieldPostings(final int[][] postings, int offset, final int Length, final int firstId) 
			throws IOException {

		//local variables in order to reduce the number
		//of times we need to access a two-dimensional array
		final int[] postings0 = postings[0];
		final int[] postings1 = postings[1];
		
		//write the first entry
		output.writeGamma(firstId);
		output.writeUnary(postings1[offset]);
	
		offset++;
		for (; offset < Length; offset++) {
			output.writeGamma(postings0[offset] - postings0[offset - 1]);
			output.writeUnary(postings1[offset]);
		}
	}
	
	/** close this object. suppresses any exception */
	public void close()
	{
		try{ 
			output.close();
		} catch (IOException ioe) {
			
		}
	}
	
	/** Return the current offset in bytes in the written file */
	public long getByteOffset()
	{
		return output.getByteOffset();
	}
	/** Return the current offset in bits in the written file */
	public byte getBitOffset()
	{
		return output.getBitOffset();
	}
	
	/** Return the underlying BitOut implementation being used by the class */
	public BitOut getBitOut()
	{
		return output;
	}
}

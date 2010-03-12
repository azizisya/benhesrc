/*
 * Terrier - Terabyte Retriever 
 * Webpage: http://terrier.org/
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
 * The Original Code is BitIndexPointer.java
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original contributor)
 */
package org.terrier.structures;

import org.apache.hadoop.io.Writable;

/** A pointer implementation for BitPostingIndex structures. It has all the
 * attrbutes of a Pointer. However, BitIndexPointer supports a FileNumber
 * attribute, which 
 * @author Craig Macdonald
 * @since 3.0
 */
public interface BitIndexPointer extends BitFilePosition, Writable, Pointer
{
	/** largest permissible file id using most implementations */
	public static final byte MAX_FILE_ID = 31;
	
	/** amount to mask byte by to obtain bit offset */
	static final byte BIT_MASK = 0x7;
	/** amount to shift byte by to obtain file id */
	static final byte FILE_SHIFT = 0x3;
	
	
	/** Update this pointer to reflect the same values as the specified
	 * pointer
	 * @param pointer - pointer to use to set the offset, bit offset
	 * and file Id parameters.
	 */ 
	public void setBitIndexPointer(BitIndexPointer pointer);
	/** Set the file number */
	public void setFileNumber(byte fileId);
	
	/** Returns the file number: 0-32 */
	public byte getFileNumber();
	
}

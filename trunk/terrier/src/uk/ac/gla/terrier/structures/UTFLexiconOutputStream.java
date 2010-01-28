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
 * The Original Code is LexiconOutputStream.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk> (original author) 
 *   Craig Macdonald <craigm{a.}.dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.structures;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;

import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.StringTools;
/**
 * This class implements an output stream for the lexicon structure.
 * @author Vassilis Plachouras, Craig Macdonald
 * @version $Revision: 1.1 $
 */
public class UTFLexiconOutputStream extends LexiconOutputStream {
	/** A zero buffer for writing to the file.*/
	private byte[] zeroBuffer = new byte[ApplicationSetup.STRING_BYTE_LENGTH];

	/**
	 * A default constructor.
	 */
	public UTFLexiconOutputStream() {
		super();
	}
	/**
	 * A constructor given the filename.
	 * @param filename java.lang.String the name of the lexicon file.
	 */
	public UTFLexiconOutputStream(String filename) {
		super(filename);
	}
	/**
	 * A constructor given the filename.
	 * @param file java.io.File the name of the lexicon file.
	 */
	public UTFLexiconOutputStream(File file) {
		super(file);
	}
	
	public UTFLexiconOutputStream(String path, String prefix)
	{
		super(path, prefix);
	}
	
	/** Create a lexicon using the specified data stream */
	public UTFLexiconOutputStream(DataOutput out){
		super(out);
	}

	/**
	 * Writes a lexicon entry.
	 * @return the number of bytes written to the file. 
	 * @throws java.io.IOException if an I/O error occurs
	 * @param _term the string representation of the term
	 * @param _termId the terms integer identifier
	 * @param _documentFrequency the term's document frequency in the collection
	 * @param _termFrequency the term's frequency in the collection
	 * @param _endOffset the term's ending byte offset in the inverted file
	 * @param _endBitOffset the term's ending byte bit-offset in the inverted file
	 */
	public int writeNextEntry(
		String _term,
		int _termId,
		int _documentFrequency,
		int _termFrequency,
		long _endOffset,
		byte _endBitOffset)
		throws IOException {
		numPointersWritten += _documentFrequency;
        numTokensWritten += _termFrequency;
        numTermsWritten++;
		lexiconStream.writeUTF(_term);
		lexiconStream.write(
				zeroBuffer,
				0,
				ApplicationSetup.STRING_BYTE_LENGTH - StringTools.utf8_length(_term));
		lexiconStream.writeInt(_termId);
		lexiconStream.writeInt(_documentFrequency);
		lexiconStream.writeInt(_termFrequency);
		lexiconStream.writeLong(_endOffset);
		lexiconStream.writeByte(_endBitOffset);
		return UTFLexicon.lexiconEntryLength;
	}
	/**
	 * Writes a lexicon entry.
	 * @return the number of bytes written.
	 * @throws java.io.IOException if an I/O error occurs
	 * @param _term the byte representation of the term, as written by DataInput.writeUTF(). This
	 * should be ApplicationSetup.STRING_BYTE_LENGTH +2 in length
	 * @param _termId the terms integer identifier
	 * @param _documentFrequency the term's document frequency in the collection
	 * @param _termFrequency the term's frequency in the collection
	 * @param _endOffset the term's ending byte offset in the inverted file
	 * @param _endBitOffset the term's ending byte bit-offset in the inverted file
	 */
	
	public int writeNextEntry(
		byte[] _term,
		int _termId,
		int _documentFrequency,
		int _termFrequency,
		long _endOffset,
		byte _endBitOffset)
		throws IOException {
		final int length = _term.length;
		numPointersWritten += _documentFrequency;
        numTokensWritten += _termFrequency;
		numTermsWritten++;
		lexiconStream.write(_term, 0, length);
		lexiconStream.write(
            zeroBuffer,
       	    0,
			2+ApplicationSetup.STRING_BYTE_LENGTH - length);	
		lexiconStream.writeInt(_termId);
		lexiconStream.writeInt(_documentFrequency);
		lexiconStream.writeInt(_termFrequency);
		lexiconStream.writeLong(_endOffset);
		lexiconStream.writeByte(_endBitOffset);
		return UTFLexicon.lexiconEntryLength;
	}

}

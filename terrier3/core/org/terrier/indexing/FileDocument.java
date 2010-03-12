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
 * The Original Code is FileDocument.java.
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original author)
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
 */
package org.terrier.indexing;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import org.terrier.utility.ApplicationSetup;
/** 
 * Models a document which corresponds to one file.
 * @author Craig Macdonald &amp; Vassilis Plachouras
 * @version $Revision: 1.28 $
 */
public class FileDocument implements Document {
	protected static final Logger logger = Logger.getRootLogger();
	/** The maximum number of digits that are allowed in valid terms. */
	protected final static int maxNumOfDigitsPerTerm = 4;
	/**
	 * The maximum number of consecutive same letters or digits that are
	 * allowed in valid terms.
	 */
	protected final static int maxNumOfSameConseqLettersPerTerm = 3;
	/** The maximum length of a term.*/
	protected final static int MAX_TERM_LENGTH = ApplicationSetup.MAX_TERM_LENGTH;
	/** The input reader. */
	protected Reader br;
	/** End of Document. Set by the last couple of lines in getNextTerm() */
	protected boolean EOD = false;
	
	/** The number of bytes read from the input.*/
	public long counter = 0;

	/** The file that this document represents.; */
	protected File file;
	
	protected Map<String,String> fileProperties = new HashMap<String,String>(5);

	protected FileDocument() {}

	/** 
	 * Constructs an instance of the FileDocument from the 
	 * given input stream.
	 * @param docStream the input stream that reads the file.
	 */
	public FileDocument(String filename, InputStream docStream) {
		this.br = getReader(docStream);
		fileProperties.put("filename", filename);
		fileProperties.put("parser", this.getClass().getName());
	}

	/** Returns the underlying buffered reader, so that client code can tokenise the
	 * document itself, and deal with it how it likes. */
    public Reader getReader()
    {
        return this.br;
    }


	/** 
	 * Returns a buffered reader that encapsulates the
	 * given input stream.
	 * @param docStream an input stream that we want to 
	 *        access as a buffered reader.
	 * @return the buffered reader that encapsulates the 
	 *         given input stream.
	 */
	protected Reader getReader(InputStream docStream) {
		return new BufferedReader(new InputStreamReader(docStream));
	}
	/**Gets the next term from the Document */
	public String getNextTerm()
	{
	
		if (EOD)
			return null;
	
		int ch = 0;
		StringBuilder sw = new StringBuilder(MAX_TERM_LENGTH);
		/* the string to return as a result at the end of this method. */
		String s = null;
		try{
			/* skip non-alphanumeric charaters */
			while (ch != -1 && (ch < 'A' || ch > 'Z') && (ch < 'a' || ch > 'z')
				&& (ch < '0' || ch > '9') 
				 /* removed by Craig: && ch != '<' && ch != '&' */
			) {
				ch = br.read();
				counter++;
			}
			//now accept all alphanumeric charaters
			while (ch != -1 && (
				((ch >= 'A') && (ch <= 'Z'))
				|| ((ch >= 'a') && (ch <= 'z'))
				|| ((ch >= '0') && (ch <= '9'))))
			{
				//transforms the uppercase character to lowercase
				if ((ch >= 'A') && (ch <= 'Z') /*&& lowercase*/)
					ch += 32;
				/* add character to word so far */
				sw.append((char)ch);
				ch = br.read();
				counter++;
			}
		}catch(IOException ioe){
			logger.error("FileDocument got an IOException - skipping document",ioe);
			ch = -1;
		}
		//lastChar = ch;
		if (ch == -1)
		{
			EOD = true;
			try{
				br.close();
			} catch(IOException ioe) {
				logger.error("error closing file in FileDocument: ", ioe);
			}
		}
		s = sw.toString();
		if (s == null)
			return null;
		if (s.length() > MAX_TERM_LENGTH)
			s = s.substring(0,MAX_TERM_LENGTH);
		s = check(s);
		//if (s == null)
		//	return getNextTerm();
		return s;
	}
	/**
	 * Returns null because there is no support for fields with
	 * file documents.
	 * @return null.
	 */
	public Set<String> getFields() {
		return null;
	}
	/** 
	 * Indicates whether the end of a document has been reached.
	 * @return boolean true if the end of a document has been reached, 
	 *         otherwise, it returns false.
	 */
	public boolean endOfDocument() {
		return EOD;
	}
	/**
	 * Checks whether a term is shorter than the maximum allowed length,
	 * and whether a term does not have many numerical digits or many
	 * consecutive same digits or letters.
	 * @param s String the term to check if it is valid.
	 * @return String the term if it is valid, otherwise it returns null.
	 */
	protected String check(String s) {
		//if the s is null
		//or if it is longer than a specified length
		if (s == null)
			return null;
		s = s.trim();
		final int length = s.length();
		int counter = 0;
		int counterdigit = 0;
		int ch = -1;
		int chNew = -1;
		for(int i=0;i<length;i++)
		{
			chNew = s.charAt(i);
			if (chNew >= 48 && chNew <= 57)
				counterdigit++;
			if (ch == chNew)
				counter++;
			else
				counter = 1;
			ch = chNew;
			/* if it contains more than 4 consequtive same letters,
			   or more than 4 digits, then discard the term. */
			if (counter >= maxNumOfSameConseqLettersPerTerm
				|| counterdigit > maxNumOfDigitsPerTerm)
				return null;
		}
		return s;
	}

	public String getProperty(String name){
		return fileProperties.get(name);
	}
	public Map<String,String> getAllProperties(){
		return fileProperties;
	}
}

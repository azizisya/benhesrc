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
 * The Original Code is HTMLDocument.java.
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original author)
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
 */
package org.terrier.indexing;
import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;

import org.terrier.utility.ApplicationSetup;
/**
 * Models an HTML document.
 * @author Vassilis Plachouras
 * @version $Revision: 1.17 $
 */
public class HTMLDocument extends FileDocument {
	protected static final Logger logger = Logger.getRootLogger();
	/** Indicates whether an error has occurred. */
	public boolean error;
	/** The maximum length of a term.*/
	protected static final int MAX_TERM_LENGTH = ApplicationSetup.MAX_TERM_LENGTH;
	
	/**
	 * Saves the last read character between consecutive calls of getNextTerm().
	 */
	protected static int lastChar = -1;
	/**
	 * Change to lowercase? 
	 */
	protected static final boolean lowercase = Boolean.parseBoolean(ApplicationSetup.getProperty("lowercase", "true"));

	/** build an html document from the specified input stream. */
	public HTMLDocument(String filename, InputStream in) {
		super(filename, in);
	}
	/**
	 * Returns the next term from a document.
	 * @return String the next term of the document, or null if the 
	 *         term was discarded during tokenising.
	 */
	public String getNextTerm() {
		//the string to return as a result at the end of this method.
		String s = null;
		StringBuilder sw = null;
		//String tagName = null;
		boolean endOfTagName;
		//are we in a body of a tag?
		boolean btag = true;
		int ch = 0;
		//while not the end of document, or the end of file, or we are in a tag
		while (btag && ch != -1 && !EOD) {
			sw = new StringBuilder(MAX_TERM_LENGTH);
			boolean tag_f = false;
			boolean tag_i = false;
			error = false;
			try {
				if (lastChar == 60)
					ch = lastChar;
				//If not EOF and ch.isNotALetter and ch.isNotADigit and
				//ch.isNot '<' and ch.isNot '&'
				while (ch != -1
						&& (ch < 'A' || ch > 'Z')
						&& (ch < 'a' || ch > 'z')
						&& (ch < '0' || ch > '9')
						&& ch != '<' && ch != '&') {
					ch = br.read();
					counter++;
					//if ch is '>' (end of tag), then there is an error.
					if (ch == '>')
						error = true;
				}
				//if a tag begins
				if (ch == '<') {
					ch = br.read();
					counter++;
					//if it is a closing tag, set tag_f true
					if (ch == '/') {
						ch = br.read();
						counter++;
						tag_f = true;
					} else if (
						ch == '!') { //else if it is a comment, that is <!
						//read until you encounter a '<', or a '>', or the end of file
						while ((ch = br.read()) != '>' && ch != '<' && ch != -1) {
							counter++;
						} 
						counter++;
						
					} else
						tag_i = true; //otherwise, it is an opening tag
				}
				
				if (ch == '&' ) {
					//read until an opening or the end of a tag is encountered, or the 
					//end of file, or a space, or a semicolon,
					//which means the end of the escape sequence &xxx;
					while ((ch = br.read()) != '>' && 
							ch != '<' && 
							ch != ' ' && 
							ch != ';' &&
							ch != -1) {
						counter++;
					} 
					counter++;
					 
				}
				//ignore all the spaces encountered
				while (ch == ' ') {
					ch = br.read();
					counter++;
				}
	
				//if the body of a tag is encountered
				if ((btag = (tag_f || tag_i))) {
					endOfTagName = false;
					//read until the end of file, or the start, or the end 
					//of a tag, and save the content of the tag
					while (ch != -1 && ch != '<' && ch != '>') {
						sw.append((char)ch);
						ch = br.read();
						counter++;
						if (endOfTagName==false && Character.isWhitespace((char)ch)) {
							endOfTagName = true;
							//tagName = sw.toString();
						}
					}
					//if (endOfTagName==false) 
						//tagName = sw.toString();
				} else { //otherwise, if we are not in the body of a tag
					//read a sequence of letters or digits.
					while (ch != -1
						    && (//ch=='&' || 
						    	((ch >= 'A') && (ch <= 'Z'))
							 || ((ch >= 'a') && (ch <= 'z'))
							 || ((ch >= '0') && (ch <= '9')))) {
						sw.append((char)ch);
						ch = br.read();
						counter++;
					}
				}
				lastChar = ch;
				s = sw.toString();
			} catch (IOException ioe) {
				logger.fatal("Input/Output exception while reading tokens.",ioe);
			}
		}
		if (ch == -1) {
			EOD = true;
		}
		if (s == null)
			return null;
		if (lowercase)
			s = s.toLowerCase();
		if (s.length() > MAX_TERM_LENGTH)
			s = s.substring(0,MAX_TERM_LENGTH);
		s = check(s);
		//if (s == null)
		//	return getNextTerm();
		return s;
	}
}

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
 * The Original Code is TRECUTFCollection.java.
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *  Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original author)
 */

package org.terrier.indexing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;

import org.apache.log4j.Logger;

import org.terrier.utility.LookAheadStream;
import org.terrier.utility.LookAheadStreamCaseInsensitive;
import org.terrier.utility.TagSet;

/** Extends TRECCollection to provide support for indexing TREC collection in non-ASCII
  * character sets. To this end, the TRECDocument has been extended so that it accepts
  * any characters said to be Character.isLetterOrDigit(). 
  * <p>
  * <b>Properties</b>
  * <ul>
  * </ul>
  * @since 1.1.0
  * @version $Revision: 1.16 $
  * @author Craig Macdonald
  * @see org.terrier.indexing.TRECCollection
  */
public class TRECUTFCollection extends TRECCollection
{
	protected static final Logger logger = Logger.getRootLogger();
	//protected String desiredEncoding = null;
	/** Child class of TRECDocument, uses Character.isLetterOrDigit() to determing term
	  * validity. */
	static class TRECUTFDocument extends TRECDocument
	{
		/** Calls the constructor of the same nature of TRECDocument */
		public TRECUTFDocument(Reader docReader, Map<String, String> properties)
		{
			super(docReader, properties);
		}
		
		/** Calls the constructor of the same nature of TRECDocument */
		public TRECUTFDocument(Reader docReader, Map<String, String> properties, TagSet _tags, TagSet _exact, TagSet _fields)
		{
			super(docReader, properties, _tags, _exact, _fields);
		}
		
		/**
		* Returns the next term from a document. Overrides TRECDocument.getNextTerm()
		* by using Character.isLetterOrDigit() to determine "valid-ness".
		* @return String the next term of the document, or null if the 
		*		 term was discarded during tokenising.
		*/
		public String getNextTerm() {
			//the string to return as a result at the end of this method.
			String s = null;
			StringBuilder sw = null;
			String tagName = null;
			boolean endOfTagName;
			//are we in a body of a tag?
			boolean btag = true;
			int ch = 0;
			//while not the end of document, or the end of file, or we are in a tag
			while (btag && ch != -1 && !EOD) {
				//initialise the stringbuffer with the maximum length of a term (heuristic)
				sw = new StringBuilder(tokenMaximumLength);
				boolean tag_f = false;
				boolean tag_i = false;
				error = false;
				try {
					if (lastChar == 60)
						ch = lastChar;
					//If not EOF and ch.isNotALetter and ch.isNotADigit and
					//ch.isNot '<' and ch.isNot '&'
					while (ch != -1
							&& ! Character.isLetterOrDigit((char)ch)
							/*&& (ch < 'A' || ch > 'Z')
							&& (ch < 'a' || ch > 'z')
							&& (ch < '0' || ch > '9')*/
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
							ch == '!') { 
							ch = br.read();
							if (ch == '[')
							{
								counter++;
								//CDATA block, read until another [
								while ((ch = br.read()) != '['  && ch != -1) {
									counter++;
								}
							}
							else
							{   //it is a comment   
								//read until you encounter a '<', or a '>', or the end of file
								while ((ch = br.read()) != '>' && ch != '<' && ch != -1) {
									counter++;
								}
						   		counter++;
							}
							
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
								tagName = sw.toString();
							}
						}
						if (endOfTagName==false) 
							tagName = sw.toString();
					} else { //otherwise, if we are not in the body of a tag
						//read a sequence of letters or digits.
						while (ch != -1
							&& (//ch=='&' || 
								/*((ch >= 'A') && (ch <= 'Z'))
								|| ((ch >= 'a') && (ch <= 'z'))
								|| ((ch >= '0') && (ch <= '9'))*/
								Character.isLetterOrDigit((char)ch)
								)) {
							sw.append((char)ch);
							ch = br.read();
							counter++;
						}
					}
					lastChar = ch;
					s = sw.toString();
					if (tag_i) {
						if ((_tags.isTagToProcess(tagName) || _tags.isTagToSkip(tagName)) && !tagName.equals("")) {
							stk.push(tagName.toUpperCase());
							if (_tags.isTagToProcess(tagName)) {
								inTagToProcess = true;
								inTagToSkip = false;
							} else {
								inTagToSkip = true;
								inTagToProcess = false;
								continue;
							}
						}
						if (_fields.isTagToProcess(tagName) && !tagName.equals("")) {
							htmlStk.add(tagName.toUpperCase());
							inHtmlTagToProcess = true;
						}
					}
					if (tag_f) {
						if ((_tags.isTagToProcess(tagName) || _tags.isTagToSkip(tagName)) && !tagName.equals("")) {
							processEndOfTag(tagName.toUpperCase());
							String stackTop = null;
							if (!stk.isEmpty()) {
								stackTop = (String) stk.peek();
								if (_tags.isTagToProcess(stackTop)) {
									inTagToProcess = true;
									inTagToSkip = false;
								} else {
									inTagToProcess = false;
									inTagToSkip = true;
									continue;
								}
							} else {
								inTagToProcess = false;
								inTagToSkip = false;
							}
						}
						if (_fields.isTagToProcess(s) && !s.equals("")) {
							htmlStk.remove(s.toUpperCase());
						}
					}
					
				} catch (IOException ioe) {
					logger.warn("Input/Output exception during reading tokens. Stack trace follows", ioe);
					return null;
				}
			}
			if (ch == -1) {
				EOD = true;
			}
			boolean hasWhitelist = _tags.hasWhitelist();
			if (!btag && 
					(!hasWhitelist || (hasWhitelist && inTagToProcess )) && 
					!inTagToSkip) {
				if (lowercase)
					return check(s.toLowerCase());
				return(check(s));
			}
			return null;
		}
	}
	
	/** Instantiate a new TRECUTFCollection. Calls parent default constructor of TRECCollection */
	public TRECUTFCollection()
	{
		super();
	}

	/** Instantiate a new TRECUTFCollection. Calls parent with inputstream constructor of TRECCollection. */	
	public TRECUTFCollection(InputStream input) {
		super(input);
	}
	
	/** Instantiate a new TRECUTFCollection. Calls parent 4 String constructor of TRECCollection */
	public TRECUTFCollection(String CollectionSpecFilename, 
		String TagSet, 
		String BlacklistSpecFilename,
		String docPointersFilename)
	{
		super(CollectionSpecFilename, TagSet, BlacklistSpecFilename, docPointersFilename);
		//desiredEncoding = ApplicationSetup.getProperty("trec.encoding", Charset.defaultCharset().name());
		reset();//reopens first file with correct encoding
	}

	/** Overrides the getDocument() method in TRECCollection, so a UTF compatible
	  * Document object is returned. */
	public Document getDocument()
	{
		try{
			return trecDocument = new TRECUTFDocument(
				new BufferedReader(
					new InputStreamReader( 
						tags_CaseSensitive
							? new LookAheadStream(br, end_docTag.getBytes(desiredEncoding))
							: new LookAheadStreamCaseInsensitive(br, end_docTag, desiredEncoding),
						desiredEncoding)), 
				DocProperties);
		} catch (java.io.UnsupportedEncodingException uee) {
			logger.warn("Desired encoding ("+desiredEncoding+") unsupported. Resorting to platform default.", uee);
			return trecDocument = new TRECUTFDocument(
				new BufferedReader(
				new InputStreamReader( 
					tags_CaseSensitive
						? new LookAheadStream(br, end_docTag.getBytes())
						: new LookAheadStreamCaseInsensitive(br, end_docTag))), 
				DocProperties );
		}
	}
	
	
	/** A TREC-specific getDocument method, that allows the tags to be specified for
	 * 	each document. 
	 *	@return Document the object of the current document to process.
	 */
	public Document getDocument(TagSet _tags, TagSet _exact, TagSet _fields) {
		try{
			return trecDocument = new TRECUTFDocument(
				new BufferedReader(
					new InputStreamReader(
						tags_CaseSensitive
						? new LookAheadStream(br, end_docTag.getBytes(desiredEncoding))
						: new LookAheadStreamCaseInsensitive(br, end_docTag),
						desiredEncoding)), 
				DocProperties, _tags,_exact, _fields);
		} catch (java.io.UnsupportedEncodingException uee) {
			logger.warn("Desired encoding ("+desiredEncoding+") unsupported. Resorting to platform default.", uee);
			return trecDocument = new TRECUTFDocument(
				new BufferedReader(
					new InputStreamReader( 
						tags_CaseSensitive
						? new LookAheadStream(br, end_docTag)
						: new LookAheadStreamCaseInsensitive(br, end_docTag))),
				 DocProperties, _tags,_exact, _fields);
		}
	}

}

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
 * The Original Code is TRECDocument.java.
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
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.TagSet;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * Models a document in a TREC collection. This class uses the integer property
 * <tt>string.byte.length</tt>, which corresponds to the maximum length in
 * characters of a term and defaults to 20, and the boolean property <tt>lowercase</tt>,
 * which specifies whether characters are transformed to lowercase. The default value
 * of <tt>lowercase</tt> is <tt>true</tt>.
 * @author Craig Macdonald &amp; Vassilis Plachouras 
 * @version $Revision: 1.35 $
 */
public class TRECDocument implements Document {
	protected static final Logger logger = Logger.getRootLogger();
	/** The maximum length of a token in the check method. */
	protected final static int tokenMaximumLength = ApplicationSetup.MAX_TERM_LENGTH;
	
	/** Change to lowercase? */
	protected final static boolean lowercase = Boolean.parseBoolean(ApplicationSetup.getProperty("lowercase", "true"));
	
	/** A temporary String array*/
	protected final String[] stringArray = new String[1];
	
	/** The input reader. */
	protected Reader br;
	
	/** End of Document. Set by the last couple of lines in getNextTerm() */
	protected boolean EOD = false;
	
	/** The number of bytes read from the input.*/
	protected long counter = 0;	
	
	/** Saves the last read character between consecutive calls of getNextTerm().*/
	protected int lastChar = -1;
	
	/** Indicates whether an error has occurred.*/
	protected boolean error;
	
	/**	The tags to process or skip.*/
	protected TagSet _tags = null; 
	
	/** 
	 * The tags to process exactly. For these tags,
	 * the check() method is not applied.
	 */
	protected TagSet _exact = null; 
	
	/** The tags to consider as fields. */
	protected TagSet _fields = null; 
	
	/** The stack where the tags are pushed and popped accordingly. */
	protected Stack<String> stk = new Stack<String>();
	
	/** Indicates whether we are in a tag to process. */
	protected boolean inTagToProcess = false;
	/** Indicates whether we are in a tag to skip. */
	protected boolean inTagToSkip = false;
	
	/** The hash set where the tags, considered as fields, are inserted. */
	protected Set<String> htmlStk = new HashSet<String>();
	/** Specifies whether the tokeniser is in a field tag to process. */
	protected boolean inHtmlTagToProcess = false;

	protected Map<String, String> properties = null;

	/** 
	 * Constructs an instance of the class from the given reader object.
	 * @param docReader Reader the stream from the collection that ends at the 
	 *		end of the current document.
	 */
	public TRECDocument(Reader docReader, Map<String, String> docProperties)
	{
		this.br = docReader;
		properties = docProperties;	
		this._tags = new TagSet(TagSet.TREC_DOC_TAGS);
		this._exact = new TagSet(TagSet.TREC_EXACT_DOC_TAGS);
		this._fields = new TagSet(TagSet.FIELD_TAGS);
	}
	/**
	 * Constructs an instance of the class from the given reader object.
	 * The tags to process, the exact tags and the field tags are passed
	 * as parameters in the constructor.
	 * @param docReader Reader the stream from the collection that ends at the 
	 *		end of the current document.
	 * @param _tags TagSet the tags of the document to process or ignore.
	 * @param _exact TagSet the tags of the document to process exactly.
	 * @param _fields TagSet the tags of the documents to be processed as fields. 
	 */
	public TRECDocument(Reader docReader, Map<String, String> docProperties, TagSet _tags, TagSet _exact, TagSet _fields)
	{
		this.br = docReader;
		properties = docProperties;
		this._tags = _tags;
		this._exact = _exact;
		this._fields = _fields;
	}

	/** Returns the underlying buffered reader, so that client code can tokenise the
	  * document itself, and deal with it how it likes. */
	public Reader getReader()
	{
		return this.br;
	}

	
	protected final StringBuilder sw = new StringBuilder(tokenMaximumLength);
	/**
	 * Returns the next term from a document.
	 * @return String the next term of the document, or null if the 
	 *		 term was discarded during tokenising.
	 */
	public String getNextTerm() {
		//the string to return as a result at the end of this method.
		String s = null;
		//StringBuilder sw = null;
		String tagName = null;
		boolean endOfTagName;
		//are we in a body of a tag?
		boolean btag = true;
		int ch = 0;
		//while not the end of document, or the end of file, or we are in a tag
		while (btag && ch != -1 && !EOD) {
			//initialise the stringbuffer with the maximum length of a term (heuristic)
			//sw = new StringBuilder(tokenMaximumLength);
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
						counter++;
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
						{	//it is a comment	
							//read until you encounter a '<', or a '>', or the end of file
							while ((ch = br.read()) != '>' && ch != '<' && ch != -1) {
								counter++;
							} 
							counter++;
						}
					} else {
						tag_i = true; //otherwise, it is an opening tag
					}
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
								((ch >= 'A') && (ch <= 'Z'))
							 || ((ch >= 'a') && (ch <= 'z'))
							 || ((ch >= '0') && (ch <= '9')))) {
						sw.append((char)ch);
						ch = br.read();
						counter++;
					}
				}
				lastChar = ch;
				s = sw.toString(); sw.setLength(0);
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
							stackTop = stk.peek();
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
				logger.warn("Input/Output exception during reading tokens. Document "+ this.getProperty("docno"), ioe);
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
	/** 
	 * Returns the fields in which the current term appears in.
	 * @return HashSet a hashset containing the fields that the current
	 *		 term appears in.
	 */
	public Set<String> getFields() {
		return htmlStk;
	}
	/**
	 * Indicates whether the tokenizer has reached the end of the 
	 * current document.
	 * @return boolean true if the end of the current document has
	 *		 been reached, otherwise returns false.
	 */
	public boolean endOfDocument() {
		return EOD;
	}
	
	/**
	 * The encounterd tag, which must be a final tag is matched with the tag on
	 * the stack. If they are not the same, then the consistency is restored by
	 * popping the tags in the stack, the observed tag included. If the stack
	 * becomes empty after that, then the end of document EOD is set to true.
	 * 
	 * @param tag The closing tag to be tested against the content of the stack.
	 */
	protected void processEndOfTag(String tag) {
		//if there are no tags in the stack, return
		if (stk.empty())
			return;
		//if the given tag is on the top of the stack then pop it
		if (tag.equals(stk.peek()))
			stk.pop();
		else { //else report an error, and find the tag.
			int counter = 0;
			int x = stk.search(tag);
			while (!stk.empty() & counter < x) {
				counter++;
				stk.pop();
			}
		}
	}
	
	/** The maximum number of digits that are allowed in valid terms. */
	protected final int maxNumOfDigitsPerTerm = 4;
	
	/** 
	 * The maximum number of consecutive same letters or digits that are 
	 * allowed in valid terms.
	 */
	protected final int maxNumOfSameConseqLettersPerTerm = 3;
	
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
		if (length == 0 || length > tokenMaximumLength)
			return null;
		if (!stk.empty() && _exact.isTagToProcess(stk.peek()))
			return s;
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
			/* if it contains more than 3 consequtive same 
			 * letters (or digits), or more than 4 digits, 
			 * then discard the term. 
			 */
			if (counter > maxNumOfSameConseqLettersPerTerm ||
				counterdigit > maxNumOfDigitsPerTerm)
				return null;
		}
		return s;
	}

	/** Allows access to a named property of the Document. Examples might be URL, filename etc.
	  * @param name Name of the property. It is suggested, but not required that this name
	  * should not be case insensitive.
	  * @since 1.1.0 */
	public String getProperty(String name)
	{
		return properties.get(name.toLowerCase());
	}

    /** Returns the underlying map of all the properties defined by this Document.
	  * @since 1.1.0 */	
	public Map<String,String> getAllProperties()
	{
		return properties;
	}

	/**
	 * Static method which dumps a document to System.out
	 * @param args A filename to parse
	 */
	public static void main(String args[])
	{
		if (args.length == 0)
		{
			logger.fatal("ERROR: Please specify a test file on the command line");
			logger.fatal("Exiting ...");
			System.exit(0);
		}
		Document d = generateDocumentFromFile(args[0]);
		if (d !=  null)
			dumpDocument(d);
	}

	/** instantiates a TREC document from a file */
	public static Document generateDocumentFromFile(final String filename)
	{
		BufferedReader b = null;
		try{
			b = new BufferedReader(new FileReader(filename));
		} catch (IOException ioe) {
			logger.fatal("ERROR: Problem opening TRECDocument test file : "+ ioe);
			logger.fatal("Exiting ...");
			ioe.printStackTrace();
		}
		return new TRECDocument(b, null);
	}
	
	/**
	 * Dumps a document to stdout
	 * @param d a Document object
	 */
	public static void dumpDocument(final Document d)
	{
		int terms = 0;
		while(! d.endOfDocument() )
		{
			String t = d.getNextTerm();
			if (t == null)
				continue;
			terms++;
			System.out.print("term: "+ t);
			System.out.print("; fields = {");
			Set<String> fields = d.getFields();
			java.util.Iterator<String> f = fields.iterator();
			if (f.hasNext())
				System.out.print((f.next()));
			while(f.hasNext())
			{
				System.out.print(","+(f.next()));
			}
			System.out.println("}");
		}
		System.out.println("terms: "+terms);
	}

}

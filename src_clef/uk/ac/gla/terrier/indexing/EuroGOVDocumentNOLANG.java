/*
 * Terrier - Terabyte Retriever 
 * Webpage: http://ir.dcs.gla.ac.uk/terrier 
 * Contact: terrier{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * Information Retrieval Group
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
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original author)
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>  
 */
package uk.ac.gla.terrier.indexing;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Stack;
import java.util.ArrayList;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.TagSet;
import java.util.Map;
/**
 * Models a document in a TREC collection. This class uses the integer property
 * <tt>string.byte.length</tt>, which corresponds to the maximum length in
 * characters of a term and defaults to 20, and the boolean property <tt>lowercase</tt>,
 * which specifies whether characters are transformed to lowercase. The default value
 * of <tt>lowercase</tt> is <tt>true</tt>.
 * @author Craig Macdonald &amp; Vassilis Plachouras 
 * @version $Revision: 1.1 $
 */
public class EuroGOVDocumentNOLANG implements Document {
	/** The maximum length of a token in the check method. */
	protected static int tokenMaximumLength = ApplicationSetup.STRING_BYTE_LENGTH;
	
	/** Change to lowercase? */
	protected final static boolean lowercase = (new Boolean(ApplicationSetup.getProperty("lowercase", "true"))).booleanValue();
	
	/** A temporary String array*/
	private final String[] stringArray = new String[1];
	
	/** The input reader. */
	private Reader br;
	
	/** End of Document. Set by the last couple of lines in getNextTerm() */
	private boolean EOD = false;
	
	/** The number of bytes read from the input.*/
	public long counter = 0;	
	
	/** Saves the last read character between consecutive calls of getNextTerm().*/
	public int lastChar = -1;
	
	/** Indicates whether an error has occurred.*/
	public boolean error;
	
	/**	The tags to process or skip.*/
	private TagSet _tags = null; 
	
	/** 
	 * The tags to process exactly. For these tags,
	 * the check() method is not applied.
	 */
	private TagSet _exact = null; 
	
	/** The tags to consider as fields. */
	private TagSet _fields = null; 
	
	/** The stack where the tags are pushed and popped accordingly. */
	protected static Stack stk = new Stack();
	
	/** Indicates whether we are in a tag to process. */
	public boolean inTagToProcess = false;
	/** Indicates whether we are in a tag to skip. */
	public boolean inTagToSkip = false;
	
	/** The hash set where the tags, considered as fields, are inserted. */
	protected HashSet htmlStk = new HashSet();
	/** Specifies whether the tokeniser is in a field tag to process. */
	public boolean inHtmlTagToProcess = false;

	protected boolean sentDocHeader = false;
	protected boolean sentLangHeader = false;

	/** 
	 * Constructs an instance of the class from the given reader object.
	 * @param docReader Reader the stream from the collection that ends at the 
	 *        end of the current document.
	 */
	public EuroGOVDocumentNOLANG(Reader docReader)
	{
		this.br = docReader;
		this._tags = new TagSet(TagSet.TREC_DOC_TAGS);
		this._exact = new TagSet(TagSet.TREC_EXACT_DOC_TAGS);
		this._fields = new TagSet(TagSet.FIELD_TAGS);
	}
	/**
	 * Constructs an instance of the class from the given reader object.
	 * The tags to process, the exact tags and the field tags are passed
	 * as parameters in the constructor.
	 * @param docReader Reader the stream from the collection that ends at the 
	 *        end of the current document.
	 * @param _tags TagSet the tags of the document to process or ignore.
	 * @param _exact TagSet the tags of the document to process exactly.
	 * @param _fields TagSet the tags of the documents to be processed as fields. 
	 */
	public EuroGOVDocumentNOLANG(Reader docReader, TagSet _tags, TagSet _exact, TagSet _fields)
	{
		this.br = docReader;
		this._tags = _tags;
		this._exact = _exact;
		this._fields = _fields;
	}

	public Reader getReader()
	{
		return this.br;
	}

	/**
	 * Returns the next term from a document.
	 * @return String the next term of the document, or null if the 
	 *         term was discarded during tokenising.
	 */
	public String getNextTerm() {
		//the string to return as a result at the end of this method.
		String s = null;
		StringBuffer sw = null;
		String tagName = null;
		boolean endOfTagName;
		//are we in a body of a tag?
		boolean btag = true;
		int ch = 0;
		//while not the end of document, or the end of file, or we are in a tag
		while (btag && ch != -1 && !EOD) {
			//initialise the stringbuffer with the maximum length of a term (heuristic)
			sw = new StringBuffer(tokenMaximumLength);
			boolean tag_f = false;
			boolean tag_i = false;
			error = false;
			try {
				if (lastChar == 60)
					ch = lastChar;
				//If not EOF and ch.isNotALetter and ch.isNotADigit and
				//ch.isNot '<' and ch.isNot '&'
					while (ch != -1
						/*&& (ch < 'A' || ch > 'Z')
						&& (ch < 'a' || ch > 'z')
						&& (ch < '0' || ch > '9')*/
						&& ! Character.isLetterOrDigit((char)ch)
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
							tagName = sw.toString();
						}
					}
					if (endOfTagName==false) 
						tagName = sw.toString();
				} else { //otherwise, if we are not in the body of a tag
					//read a sequence of letters or digits.
					while (ch != -1
						    && (//ch=='&' || 
						    /*	((ch >= 'A') && (ch <= 'Z'))
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
				System.err.println(
					"Input/Output exception during reading tokens. Stack trace follows");
				ioe.printStackTrace();
				System.exit(1);
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
	 *         term appears in.
	 */
	public HashSet getFields() {
		HashSet fields = (HashSet)htmlStk.clone();
		return fields;
	}
	/**
	 * Indicates whether the tokenizer has reached the end of the 
	 * current document.
	 * @return boolean true if the end of the current document has
	 *         been reached, otherwise returns false.
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
		if (tag.equals((String) stk.peek()))
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
		final int length = s.length();
		if (length == 0 || length > tokenMaximumLength)
			return null;
		if (!stk.empty() && _exact.isTagToProcess((String) stk.peek()))
			return s;
		int counter = 0;
		int counterdigit = 0;
		int ch = -1;
		int chNew = -1;
		for(int i=0;i<length;i++)
		{
			chNew = s.charAt(i);
			if (Character.isDigit((char)chNew))
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

	   public String getProperty(String name) {return null;}
    public Map getAllProperties() {return null;}

}

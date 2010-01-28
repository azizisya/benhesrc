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
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.ArrayList;
import java.util.regex.*;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.TagSet;
/**
 * Models a document in a TREC collection. This class uses the integer property
 * <tt>string.byte.length</tt>, which corresponds to the maximum length in
 * characters of a term and defaults to 20, and the boolean property <tt>lowercase</tt>,
 * which specifies whether characters are transformed to lowercase. The default value
 * of <tt>lowercase</tt> is <tt>true</tt>.
 * @author Craig Macdonald &amp; Vassilis Plachouras 
 * @version $Revision: 1.4 $
 */
public class EuroGOVDocument implements Document {

	protected static HashMap allowedEscapes = null;
	protected Map properties = null;

	static {
		allowedEscapes = new HashMap();
		allowedEscapes.put("&Agrave;", new Character((char)192));
		allowedEscapes.put("&Aacute;", new Character((char)193));
		allowedEscapes.put("&Acirc;", new Character((char)194));
		allowedEscapes.put("&Atilde;", new Character((char)195));
		allowedEscapes.put("&Auml;", new Character((char)196));
		allowedEscapes.put("&Aring;", new Character((char)197));
		allowedEscapes.put("&AElig;", new Character((char)198));
		allowedEscapes.put("&CCedil;", new Character((char)199));
		allowedEscapes.put("&Egrave;", new Character((char)200));
		allowedEscapes.put("&Eactue;", new Character((char)201));
		allowedEscapes.put("&Ecirc;", new Character((char)202));
		allowedEscapes.put("&Euml;", new Character((char)203));
		allowedEscapes.put("&lgrave;", new Character((char)204));
		allowedEscapes.put("&lacute;", new Character((char)205));
		allowedEscapes.put("&lcirc;", new Character((char)206));
		allowedEscapes.put("&luml;", new Character((char)207));
		allowedEscapes.put("&ETH;", new Character((char)208));
		allowedEscapes.put("&Ntilde;", new Character((char)209));
		allowedEscapes.put("&Ograve;", new Character((char)210));
		allowedEscapes.put("&Oacute;", new Character((char)211));
		allowedEscapes.put("&Ocirc;", new Character((char)212));
		allowedEscapes.put("&Otilde;", new Character((char)213));
		allowedEscapes.put("&Ouml;", new Character((char)214));
		//no &times;
		allowedEscapes.put("&Oslash;", new Character((char)216));
		allowedEscapes.put("&Ugrave;", new Character((char)217));
		allowedEscapes.put("&Uacute;", new Character((char)218));
		allowedEscapes.put("&Ucirc;", new Character((char)219));
		allowedEscapes.put("&Uuml;", new Character((char)220));
		allowedEscapes.put("&Yacute;", new Character((char)221));
		allowedEscapes.put("&THORN;", new Character((char)222));
		allowedEscapes.put("&szlig;", new Character((char)223));
		allowedEscapes.put("&agrave;", new Character((char)224));
		allowedEscapes.put("&aacute;", new Character((char)225));
		allowedEscapes.put("&acirc;", new Character((char)226));
		allowedEscapes.put("&atilde;", new Character((char)227));
		allowedEscapes.put("&auml;", new Character((char)228));
		allowedEscapes.put("&aring;", new Character((char)229));
		allowedEscapes.put("&aelig;", new Character((char)230));
		allowedEscapes.put("&ccedil;", new Character((char)231));
		allowedEscapes.put("&egrave;", new Character((char)232));
		allowedEscapes.put("&eacute;", new Character((char)233));
		allowedEscapes.put("&ecirc;", new Character((char)234));
		allowedEscapes.put("&euml;", new Character((char)235));
		allowedEscapes.put("&igrave;", new Character((char)236));
		allowedEscapes.put("&iacute;", new Character((char)237));
		allowedEscapes.put("&icirc;", new Character((char)238));
		allowedEscapes.put("&iuml;", new Character((char)239));
		allowedEscapes.put("&eth;", new Character((char)240));
		allowedEscapes.put("&ntilde;", new Character((char)241));
		allowedEscapes.put("&ograve;", new Character((char)242));
		allowedEscapes.put("&oacute;", new Character((char)243));
		allowedEscapes.put("&ocirc;", new Character((char)244));
		allowedEscapes.put("&otilde;", new Character((char)245));
		allowedEscapes.put("&ouml;", new Character((char)246));
		// no &divide;
		allowedEscapes.put("&oslash;", new Character((char)248));
		allowedEscapes.put("&ugrave;", new Character((char)249));
		allowedEscapes.put("&uacute;", new Character((char)509));
		allowedEscapes.put("&ucirc;", new Character((char)251));
		allowedEscapes.put("&uuml;", new Character((char)252));
		allowedEscapes.put("&yacute;", new Character((char)253));
		allowedEscapes.put("&thorn;", new Character((char)254));
		allowedEscapes.put("&yuml;", new Character((char)255));
		//now from http://www.w3.org/TR/WD-html40-970917/sgml/entities.html
		allowedEscapes.put("&Alpha;", new Character((char)913));
		allowedEscapes.put("&Beta;", new Character((char)914));
		allowedEscapes.put("&Gamma;", new Character((char)915));
		allowedEscapes.put("&Delta;", new Character((char)916));
		allowedEscapes.put("&Epsilon;", new Character((char)917));
		allowedEscapes.put("&Zeta;", new Character((char)918));
		allowedEscapes.put("&Eta;", new Character((char)919));
		allowedEscapes.put("&Theta;", new Character((char)920));
		allowedEscapes.put("&Iota;", new Character((char)921));
		allowedEscapes.put("&Kappa;", new Character((char)922));
		allowedEscapes.put("&Lambda;", new Character((char)923));
		allowedEscapes.put("&Mu;", new Character((char)924));
		allowedEscapes.put("&Nu;", new Character((char)925));
		allowedEscapes.put("&Xi;", new Character((char)926));
		allowedEscapes.put("&Omicron;", new Character((char)927));
		allowedEscapes.put("&Pi;", new Character((char)928));
		allowedEscapes.put("&Rho;", new Character((char)929));
		//<!-- (there is no Sigmaf, and no u+03A2 character either) -->
		allowedEscapes.put("&Sigma;", new Character((char)931));
		allowedEscapes.put("&Tau;", new Character((char)932));
		allowedEscapes.put("&Upsilon;", new Character((char)933));
		allowedEscapes.put("&Phi;", new Character((char)934));
		allowedEscapes.put("&Chi;", new Character((char)935));
		allowedEscapes.put("&Psi;", new Character((char)936));
		allowedEscapes.put("&Omega;", new Character((char)937));
		allowedEscapes.put("&alpha;", new Character((char)945));
		allowedEscapes.put("&beta;", new Character((char)946));
		allowedEscapes.put("&gamma;", new Character((char)947));
		allowedEscapes.put("&delta;", new Character((char)948));
		allowedEscapes.put("&epsilon;", new Character((char)949));
		allowedEscapes.put("&zeta;", new Character((char)950));
		allowedEscapes.put("&eta;", new Character((char)951));
		allowedEscapes.put("&theta;", new Character((char)952));
		allowedEscapes.put("&iota;", new Character((char)953));
		allowedEscapes.put("&kappa;", new Character((char)954));
		allowedEscapes.put("&lambda;", new Character((char)955));
		allowedEscapes.put("&mu;", new Character((char)956));
		allowedEscapes.put("&nu;", new Character((char)957));
		allowedEscapes.put("&xi;", new Character((char)958));
		allowedEscapes.put("&omicron;", new Character((char)959));
		allowedEscapes.put("&pi;", new Character((char)960));
		allowedEscapes.put("&rho;", new Character((char)961));
		allowedEscapes.put("&sigmaf;", new Character((char)962));
		allowedEscapes.put("&sigma;", new Character((char)963));
		allowedEscapes.put("&tau;", new Character((char)964));
		allowedEscapes.put("&upsilon;", new Character((char)965));
		allowedEscapes.put("&phi;", new Character((char)966));
		allowedEscapes.put("&chi;", new Character((char)967));
		allowedEscapes.put("&psi;", new Character((char)968));
		allowedEscapes.put("&omega;", new Character((char)969));
		allowedEscapes.put("&thetasym;", new Character((char)977));
		allowedEscapes.put("&upsih;", new Character((char)978));
		allowedEscapes.put("&piv;", new Character((char)982));

	}

	/** The maximum length of a token in the check method. */
	protected static int tokenMaximumLength = ApplicationSetup.MAX_TERM_LENGTH;
	
	/** Change to lowercase? */
	protected final static boolean lowercase = (new Boolean(ApplicationSetup.getProperty("lowercase", "true"))).booleanValue();

	protected final static boolean markLangDoc = (new Boolean(ApplicationSetup.getProperty("clef.markdoclang", "false"))).booleanValue();
	
	/** A temporary String array*/
	protected final String[] stringArray = new String[1];
	
	/** The input reader. */
	protected Reader br;
	
	/** End of Document. Set by the last couple of lines in getNextTerm() */
	protected boolean EOD = false;
	
	/** The number of bytes read from the input.*/
	public long counter = 0;	
	
	/** Saves the last read character between consecutive calls of getNextTerm().*/
	public int lastChar = -1;
	
	/** Indicates whether an error has occurred.*/
	public boolean error;
	
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
	protected Stack stk = new Stack();
	
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

	final public String[] Languages;
	int LanguageNumber = 0; int TermNumber = 0;
	final ArrayList Terms = new ArrayList();

	/** 
	 * Constructs an instance of the class from the given reader object.
	 * @param docReader Reader the stream from the collection that ends at the 
	 *        end of the current document.
	 */
	public EuroGOVDocument(Reader docReader, String[] Languages, Map p)
	{
		this.Languages = Languages;
		this.br = docReader;
		this._tags = new TagSet(TagSet.TREC_DOC_TAGS);
		this._exact = new TagSet(TagSet.TREC_EXACT_DOC_TAGS);
		this._fields = new TagSet(TagSet.FIELD_TAGS);
		this.properties = p;
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
	public EuroGOVDocument(Reader docReader, TagSet _tags, TagSet _exact, TagSet _fields, String[] Languages, Map p)
	{
		this.Languages = Languages;
		this.br = docReader;
		this._tags = _tags;
		this._exact = _exact;
		this._fields = _fields;
		this.properties = p;
	}

	public Reader getReader()
	{
		return this.br;
	}

	public String getNextTerm()
	{
		if (! markLangDoc)
			return prv_getNextTerm();

		String t = null;
		if (! sentDocHeader)
		{
			sentDocHeader = true;
			return "||DOC||";
		}
		if (! sentLangHeader)
		{
			sentLangHeader = true;
			return "||LANG:"+Languages[LanguageNumber];
		}

		if (Languages.length == 1)
			return prv_getNextTerm();

		if (LanguageNumber == 0 && ! EOD)
		{
			t = prv_getNextTerm();
			Terms.add(t); 
		}else{
			if (TermNumber < Terms.size())
			{
				t = (String)Terms.get(TermNumber);
				TermNumber++;
			}
			else //TermNumber == Terms.size()
			{
				if(LanguageNumber == Languages.length)
				{//really the end of the document
					t = null;
				}
				else
				{
					LanguageNumber++;
					TermNumber = 0;
					//send the new language header
					t = "||LANG:"+Languages[LanguageNumber];
				}
			}
		}
		return t;
	}
	
	/**
	 * Returns the next term from a document.
	 * @return String the next term of the document, or null if the 
	 *         term was discarded during tokenising.
	 */
	
	int read() throws IOException
	{
		int c = br.read();
		//System.out.print((char)c);
		//if (((char)c) == '&' )
		//	System.err.println("BREAK!");
		return c;
	}
	
	
	protected String prv_getNextTerm() {
	//public String getNextTerm(){

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
				if (lastChar == 60) //60,otherwise known as '<'
					ch = lastChar;
				//If not EOF and ch.isNotALetter and ch.isNotADigit and
				//ch.isNot '<' and ch.isNot '&'
				while (ch != -1 && ! Character.isLetterOrDigit((char)ch) && ch != '<' && ch != '&') {
					ch = read();
					counter++;
					if (ch == '>')
						error = true;
				}
				//if a tag begins
				if (ch == '<') {
					ch = read();
					counter++;
					//if it is a closing tag, set tag_f true
					if (ch == '/') {
						ch = read();
						counter++;
						tag_f = true;
					} else if (ch == '!') { //else if it is a comment, that is <!
						//read until you encounter a '<', or a '>', or the end of file
						while ((ch = read()) != '>' && ch != '<' && ch != -1) {
							counter++;
						} 
						counter++;
						
					} else
						tag_i = true; //otherwise, it is an opening tag
				}
				
				//decode an encoded character
				if (ch == '&' ) {
					
					//read until an opening or the end of a tag is encountered, or the 
					//end of file, or a space, or a semicolon,
					//which means the end of the escape sequence &xxx;
					counter += decodeEscape(br, sw);
					
					ch = read();
					counter++;
				}
				
				//ignore all the spaces encountered
				while (ch == ' ') {
					ch = read();
					counter++;
				}
				//if the body of a tag is encountered
				if ((btag = (tag_f || tag_i))) {
					endOfTagName = false;
					//read until the end of file, or the start, or the end 
					//of a tag, and save the content of the tag
					while (ch != -1 && ch != '<' && ch != '>') {
						sw.append((char)ch);
						ch = read();
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
					while (ch != -1 && (ch=='&' || Character.isLetterOrDigit((char)ch))) {
						
						if (ch == '&' ) {
							//read until an opening or the end of a tag is encountered, or the 
							//end of file, or a space, or a semicolon,
							//which means the end of the escape sequence &xxx;
							counter += decodeEscape(br, sw);
							ch = read();
							counter++;
						} else {
							sw.append((char)ch);
							ch = read();
							counter++;
						}
					}
				}
				lastChar = ch;
				s = sw.toString();
				if (tag_i) {
					if ((_tags.isTagToProcess(tagName)||_tags.isTagToSkip(tagName))&&!tagName.equals("")) 	{
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
					if ((_tags.isTagToProcess(tagName)||_tags.isTagToSkip(tagName))&&!tagName.equals("")) {
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

			} catch (java.nio.charset.MalformedInputException mie) {
				System.err.println("MalformedInputException : "+ mie + " at char "+counter);
				mie.printStackTrace();
			} catch (sun.io.MalformedInputException mie) {
				System.err.println("MalformedInputException : "+ mie + " at char "+counter);
				mie.printStackTrace();
			} catch (IOException ioe) {
				System.err.println("Input/Output exception during reading tokens. Stack trace follows");
				ioe.printStackTrace();
				System.exit(1);
			}
		}
		if (ch == -1) {
			EOD = true;
		}
		boolean hasWhitelist = _tags.hasWhitelist();
		if (!btag && (!hasWhitelist || (hasWhitelist && inTagToProcess )) && !inTagToSkip) {
			if (lowercase)
				return check(s.toLowerCase());
			return(check(s));
		}
		return null;
	}
	
	int decodeEscape(Reader b, StringBuffer sw) throws IOException
	{
		//System.err.println("found escaped character possibly");
		StringBuffer encodedChar = new StringBuffer(9);
		int count = 0;
		encodedChar.append('&'); int ch = 0;
		while (count < 9 && (ch = read()) != '>' && ch != '<' && ch != ' ' && ch != ';' && ch != -1) {
			//System.out.println("ch: " + (char)ch);
			encodedChar.append((char)ch);
			count++;
		} 
		
		if (count == 9)
		{
			//too many characters read, jump out 
			return count;	
		}
		encodedChar.append(';');	
		String encoded = encodedChar.toString();
		Pattern pNumericEscape = Pattern.compile("^&#(\\d+);$");
		Pattern pNumericHexEscape = Pattern.compile("^&#x([0-9A-Fa-f]+);$");
		Pattern pNamedEscape = Pattern.compile("^&(\\w+);$");

		Matcher m = pNumericEscape.matcher(encoded);
		if(m.matches()) {
			//it's a decimal encoded unicode char number
			int Code = Integer.parseInt(m.group(1));
			if (Code > 32 && Code != 127 &&  Character.isLetterOrDigit((char)Code))
				sw.append((char)Code);
			
		} 
		else 
		{
			m = pNumericHexEscape.matcher(encoded);
			if(m.matches() )
			{
				//it'x a hex encoded unicode char number
				int Code = Integer.parseInt(m.group(1), 16);
				if (Code > 32 && Code != 127 && Character.isLetterOrDigit((char)Code))
					sw.append((char)Code);
			}
			else
			{//some other kind of escape. check allowed ones	
				if(allowedEscapes.containsKey(encoded.toString()))
				{
					//String name = m.group();
					//System.out.println("name: " + name);
					sw.append((Character)allowedEscapes.get(encoded.toString()));
				}
			}
		}
		return count;
	}

	/** 
	 * Returns the fields in which the current term appears in.
	 * @return HashSet a hashset containing the fields that the current
	 *         term appears in.
	 */
	public HashSet getFields() {
		HashSet fields = (HashSet)htmlStk.clone();
		fields.add(Languages[LanguageNumber]);		
		return fields;
	}

	/**
	 * Indicates whether the tokenizer has reached the end of the 
	 * current document.
	 * @return boolean true if the end of the current document has
	 *         been reached, otherwise returns false.
	 */
	public boolean endOfDocument() {
		return EOD && LanguageNumber == Languages.length -1 && TermNumber == Terms.size();
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
    public String getProperty(String name){
		return (String) properties.get(name);
	}
    public Map getAllProperties(){
		return properties;
	}

}

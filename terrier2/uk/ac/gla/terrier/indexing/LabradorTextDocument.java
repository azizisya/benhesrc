package uk.ac.gla.terrier.indexing;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Vector;
import java.util.Stack;
import uk.ac.gla.terrier.utility.TagSet;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.htmlparser.Attribute;
import org.htmlparser.Node;
import org.htmlparser.lexer.InputStreamSource;
import org.htmlparser.lexer.Lexer;
import org.htmlparser.lexer.Page;
import org.htmlparser.nodes.RemarkNode;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.util.ParserException;

import uk.ac.gla.terrier.utility.ApplicationSetup;

public class LabradorTextDocument implements Document {

	/** the set of boundaries we use to identify the end of sentence */
	private static HashSet<String> NonSentenceBoundaryTags = null;
	
	/** the document's properties */
	protected Map<String,String> docProperties = null;
		
	/** the logger */
	protected static Logger logger = Logger.getRootLogger(); 
	
	/** the title */
	protected String title = null;
	
	/** the META keywords */
	protected String keywords = null;
	
	/** the META description */
	protected String description = null;
	
	/** the html lexer */
	protected Lexer lexer = null;
		
	/** the end of the document */
	protected boolean EOD = false;
	
	/** the current text to process */ 
	protected String currentText;
	
	/** the underlying input stream */
	protected InputStream inputStream = null;
		
	/** The size of the buffer that keeps the extract of the document */
	protected int TEXT_INITIAL_LENGTH = 2 * 1024; //2Kbyte of text

	/** The buffer that stores the extract of the document  */
	protected StringBuilder extract = new StringBuilder(TEXT_INITIAL_LENGTH);

	
	/** The tags to process or skip.*/
	protected TagSet _tags = null; 
	/** Indicates whether we are in a tag to process. */
	protected boolean inTagToProcess = false;
	/** Indicates whether we are in a tag to skip. */
	protected boolean inTagToSkip = false;
	/** The stack where the tags are pushed and popped accordingly. */
	protected Stack stk = new Stack();
	
	/** the number of the current document */
	protected String docno = null;

	static {
		NonSentenceBoundaryTags = new HashSet<String>();
		NonSentenceBoundaryTags.add("A");
		NonSentenceBoundaryTags.add("IMG");
		NonSentenceBoundaryTags.add("B");
		NonSentenceBoundaryTags.add("I");
		NonSentenceBoundaryTags.add("FONT");
		NonSentenceBoundaryTags.add("ABBR");
		NonSentenceBoundaryTags.add("S");
		NonSentenceBoundaryTags.add("STRIKE");
		NonSentenceBoundaryTags.add("STRONG");
		NonSentenceBoundaryTags.add("SUB");
		NonSentenceBoundaryTags.add("SUP");
		NonSentenceBoundaryTags.add("U");
		NonSentenceBoundaryTags.add("EM");	
		

		/*sentenceBoundaryTags.add("DIV");
		sentenceBoundaryTags.add("H1");
		sentenceBoundaryTags.add("H2");
		sentenceBoundaryTags.add("H3");
		sentenceBoundaryTags.add("H4");
		sentenceBoundaryTags.add("H5");
		sentenceBoundaryTags.add("H6");
		sentenceBoundaryTags.add("BR");
		sentenceBoundaryTags.add("P");
		sentenceBoundaryTags.add("LI");
		sentenceBoundaryTags.add("OL");
		sentenceBoundaryTags.add("DL");
		sentenceBoundaryTags.add("DT");
		sentenceBoundaryTags.add("TD");
		sentenceBoundaryTags.add("HR");
		*/
	}
	
	
	
	public LabradorTextDocument(InputStream s, Map<String,String> properties) {
		docProperties = properties;
		
		inputStream = s;
		docno = docProperties.get("docid");
		try {
			//base = new URL(docProperties.get("url"));
			lexer = new Lexer(new Page(new InputStreamSource(s)));
		//} catch(MalformedURLException use) {
		//	logger.error("Exception while creating a LabradorDocument: ", use);
		//	logger.error("docProperty url: " + docProperties.get("url"));
			
		} catch(UnsupportedEncodingException uee) {
			logger.error("Exception while creating a LabradorDocument: ", uee);
		}
		
		this._tags = new TagSet(TagSet.TREC_DOC_TAGS);
	}

	public Reader getReader() {
		return new InputStreamReader(inputStream);
	}
	
	/**
	 * use this method in order to determine the size of the document, called after
	 * the end of document has been reached
	 * 
	 * @return
	 */
	public int getLexerPosition() {
		return lexer.getPosition();
	}
	
	public boolean endOfDocument() {
		return EOD;
	}
	
	public String getProperty(String name) {
		return docProperties.get(name);
	}

	public Map<String,String> getAllProperties() {
		return docProperties;
	}
	
	private int currentTextPosition = 0;
	private int currentTextLength = 0;
	
	public String getNextTerm() {
		return null;
	}

	
	/*public String getNextTerm() {
		char ch;
		
		//if the current text is null and we
		//haven't reached the end of document yet,
		//get the next node from the html lexer
		while (currentText==null && !EOD) {
			if ((currentText = getNextNode())!=null) {
				currentTextPosition=0;
				currentTextLength = currentText.length();
			}
		}
		
		//if the current text is still null or 
		//we have reached the end of document
		//return a graceful null
		if (currentText == null || EOD) 
			return null;

		//skip any useless white space or non-letter-digit characteres
		while (currentTextPosition<currentTextLength && 
				!(( (ch = currentText.charAt(currentTextPosition)) >= 'a' && ch <='z') || (ch >= 'A' && ch <='Z') || (ch >= '0' && ch <= '9'))) {
			currentTextPosition++;
		}
		
		//if we've reached the end of current text 
		//return a null as well
		if (currentTextPosition == currentTextLength) {
			currentText = null;
			return null;
		}
		
		//it seems that there is a term to index,
		//so define the string buffer that holds the term to return
		StringBuilder termToReturn = new StringBuilder(tokenMaximumLength);
		
		//the synchronized block will probably make it faster
		//because java's string buffer is synchronized
		synchronized(termToReturn) {
			while (currentTextPosition<currentTextLength && (( (ch = currentText.charAt(currentTextPosition)) >= 'a' && ch <='z') || (ch >= 'A' && ch <='Z') || (ch >= '0' && ch <= '9'))) {
				termToReturn.append(ch);
				currentTextPosition++;
			}
		}
		
		final int length = termToReturn.length();
		if (length == 0 || length > tokenMaximumLength)
			return null;
		return lowercase ? termToReturn.toString().toLowerCase() : termToReturn.toString();
		
	}
	*/
	
	protected boolean justAppendedSentenceBoundary = false;

	
	/**
	 * Reads the next html node
	 * @return the text associated with the node read from the html lexer
	 */

	public String getText() {
		StringBuilder text = new StringBuilder(TEXT_INITIAL_LENGTH);
		synchronized(text)
		{
			while(! EOD)
			{
				String t = getNextNode();
				if (t != null)
				{
					t = t.replaceAll(": ", ": ").replaceAll("http: ","http:").replaceAll("https: ", "https:").replaceAll(";", "; ");
					//text.append(' ');
					text.append(t);
					//if(t.matches("\\w") && ! t.matches("[.?:;]\\s*"))
						//text.append(". ");
				}
			}
		}
		return text.toString()/*.replaceAll("[^a-zA-Z_0-9 \\-:;,.!?&\n\"']"," ")*/;
	}


	public String getNextNode() {		
		String text = null;
		
		//remove the field indicator for tags that 
		if (inMetaDescription) {
			fields.remove("DESCRIPTION");
		} else if (inMetaKeywords) {
			fields.remove("KEYWORDS");
		} 
		StringBuilder content = new StringBuilder();
		inMetaDescription = inMetaKeywords = false;
		try {
			Node node = lexer.nextNode();
			
			//end of document if node is null
			if (node == null) {
				EOD = true;
				//if (logger.isDebugEnabled())
				//	logger.debug("document extract: " + extract.toString().replaceAll("\\s+"," "));
				return null;
			}
			
			if (node instanceof RemarkNode) {//ignore comments
				return null;
			} else if (node instanceof TagNode) {//we are in a tag
				final TagNode tagNode = (TagNode)node;
				final String tagName = tagNode.getTagName();
				if (tagName==null)
					return null;
				if (! NonSentenceBoundaryTags.contains(tagName.toUpperCase()))
				{	//if tag isnt one of the whitelisted tags?
					if (content.lastIndexOf(".") < content.length() -2)
					{
						//check to see if we have recently added a ". "
						content.append(". ");//.[space] for a sentence boundary
					}
				}
				/*else
				{
					// space otherwise
					//content.append(" ");
				}*/

				/*if (tagName.equalsIgnoreCase("TITLE")) {
					inTitle = tagNode.isEndTag() ? false : true;
					if (inTitle)
						fields.add("TITLE");
					else
						fields.remove("TITLE");
					
				} else if (tagName.equalsIgnoreCase("META")) {
					Vector attributes = tagNode.getAttributesEx();
					Attribute attr = null;
					String attrName = null;
					String attrValue = null;
					for (int i=0; i<attributes.size(); i++) { 
						attr = (Attribute)attributes.get(i);
						attrName = attr.getName();
						attrValue = attr.getValue();
						if (attrName==null || attrValue==null) 
							continue;
						if (attrName.equalsIgnoreCase("NAME")) {
							if (attrValue.equalsIgnoreCase("KEYWORDS"))
								inMetaKeywords = true;
							else if (attrValue.equalsIgnoreCase("DESCRIPTION")) 
								inMetaDescription = true;
						} else if (attrName.equalsIgnoreCase("CONTENT")) { 
							text = attrValue;
						}
					}						
					
					if (inMetaDescription && text!=null) {
						extract.append(StringEscapeUtils.unescapeHtml(text).replaceAll("\\s+", " ")+". ");
					}

					if (text!=null && (inMetaKeywords || inMetaDescription)) {
						if (inMetaKeywords)
							fields.add("KEYWORDS");
						else 
							fields.add("DESCRIPTION");
						return StringEscapeUtils.unescapeHtml(text.replaceAll("&nbsp;"," ")).replaceAll("\\s+", " ");
					}
				
				} else if (tagName.equalsIgnoreCase("A")) {
					inLink = tagNode.isEndTag() ? false : true;
					if (inLink) {//get the href attribute
						Vector attributes = tagNode.getAttributesEx();
						Attribute attr = null;
						String attrName = null;
						String attrValue = null;
						for (int i=0; i<attributes.size(); i++) { 
							attr = (Attribute)attributes.get(i);
							attrName = attr.getName();
							attrValue = attr.getValue();

							if (attrName==null || attrValue==null) 
								continue;
							else if (attrName.equalsIgnoreCase("HREF")) {
								try {
									link = null;	
									//link = attrValue;
									//link = new URL(base, link).toString();
								} catch(MalformedURLException use) {
									//no need for this message as it is primarily related to javascript and mailto links
									//logger.error("Exception while creating a URI object. Base: " + base + " link: " + link);
									link = null;
								}
								break;
							}
						}
					}
				} else if (tagName.equalsIgnoreCase("IMG")) {
					if (inLink) {//only get the alt text within a link
						
						Vector attributes = tagNode.getAttributesEx();
						for (int i=0; i<attributes.size(); i++) { 
							Attribute attr = (Attribute)attributes.get(i);
							String attrName = attr.getName();
							String attrValue = attr.getValue();
							if (attrName==null) 
								continue;
							if (attrName.equalsIgnoreCase("ALT")) {
								text = attrValue;
								break;
							}
						}
						/*
						if (link!=null && text!=null && text.matches("\\S+") && urlValidator.isValid(link)) {
							
							if (logger.isDebugEnabled() && text.matches("\\.\\."))
								logger.debug("check out: " + base + " " + link);
							//write the target url, the src docno and the alt text
							inLink = false;
						}


						return text==null ? null : StringEscapeUtils.unescapeHtml(text.replaceAll("&nbsp;"," ")).replaceAll("\\s+", " ");
					}
				} else*/ 
				if (tagName.equalsIgnoreCase("SCRIPT")) {
					inScript = ! tagNode.isEndTag();
				} else if (tagName.equalsIgnoreCase("STYLE")) {
					inStyle = ! tagNode.isEndTag();
				} else if (tagName.equalsIgnoreCase("ATEXT")) {
					if (tagNode.isEndTag()) {
						inAtext = false; //fields.remove("ATEXT");
					} else {
						inAtext = true; //fields.add("ATEXT");
					}
				} else if (tagName.equalsIgnoreCase("ALTTEXT")) {
					if (tagNode.isEndTag()) {
						inAltText = false; fields.remove("ALTTEXT");
					} else {
						inAltText = true; fields.add("ALTTEXT");
					}					
				//} else if (sentenceBoundaryTags.contains(tagName.toUpperCase()) && tagNode.isEndTag() && !justAppendedSentenceBoundary) {
				//	extract.append("."); justAppendedSentenceBoundary = true;
				} else if (tagNode.isEmptyXmlTag()) {
					return content.toString();
				}
				
				
				if (! tagNode.isEndTag()) {
					if ((_tags.isTagToProcess(tagName) || _tags.isTagToSkip(tagName)) && !tagName.equals("")) {
						stk.push(tagName.toUpperCase());
						if (_tags.isTagToProcess(tagName)) {
							inTagToProcess = true;
							inTagToSkip = false;
						}
						else
						{
							inTagToSkip = true;
							inTagToProcess = false;
						}
					}
				}else if ((_tags.isTagToProcess(tagName) || _tags.isTagToSkip(tagName)) && !tagName.equals("")) {
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
						}
					} else {
						inTagToProcess = false;
						inTagToSkip = false;
					}
				}
			} else if (node instanceof TextNode && !inScript && !inStyle) {//we are in text but not in a script tag
				boolean hasWhitelist = _tags.hasWhitelist();
				if (
					(!hasWhitelist || (hasWhitelist && inTagToProcess )) && 
					!inTagToSkip) 
				{
			
				text = node.getText();
				if (!text.equals("")) {
					text = StringEscapeUtils.unescapeHtml(text.replaceAll("&nbsp;"," ")).replaceAll("\\s+", " ");
					/*if (inTitle) {
						title = title==null ? text.trim() : title+ " " + text.trim();
					} else if (extract.length()<EXTRACT_LENGTH_LIMIT && !text.equals(" ")) {
						extract.append(" " + text.trim()); justAppendedSentenceBoundary = false;
					} 
					if (inLink && link!=null && text.matches("\\S+") && urlValidator.isValid(link)) {
						if (logger.isDebugEnabled() && text.matches("\\.\\."))
							logger.debug("check out: " + base + " " + link);
					}*/
					content.append(text);
					return content.toString();
				}
				}
			}
		} catch(ParserException pe) {
			logger.error("IOException while creating a LabradorDocument. ", pe);			
		}
		return content.toString();
	}
	
	public HashSet getFields() {
		return fields;
	}
	
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

	/** is the lexer in a ATEXT tag? */
	protected boolean inAtext = false;
	
	/** is the lexer in a ALTTEXT tag? */
	protected boolean inAltText = false;
	
	/** is the lexer in a script tag? */
	protected boolean inScript = false;
	
	/** is the lexer in a style tag? */
	protected boolean inStyle = false;
	
	/** is the lexer in the title tag? */
	protected boolean inTitle = false;
	
	/** is the lexer in a link tag? */
	protected boolean inLink = false;
	
	/** the link associated with an a tag */
	protected String link = null;
	
	/** is the lexer in the meta keywords? */
	protected boolean inMetaKeywords = false;
	
	/** is the lexer in the meta description? */
	protected boolean inMetaDescription = false;
	
	/** are we processing the url? */
	protected boolean inURL = false;
	
	/** the current fields */
	protected HashSet<String> fields = new HashSet<String>();
}

package uk.ac.gla.terrier.indexing;
import uk.ac.gla.terrier.utility.TagSet;
import uk.ac.gla.terrier.utility.StringTools;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.Vector;
import java.util.Arrays;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.validator.routines.UrlValidator;
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
/** 
  * <br><b>Properties:</b>
  * <ul>
  * <li><tt>labrador.document.sentence.boundary.tags</tt> - list of tags that
  * should be considered as sentence boundaries. Defaults to 
  * <tt>DIV,H1,H2,H3,H4,H5,H6,BR,P,LI,OL,DL,DT,TD</tt></li>
  * <li><tt>labrador.document.extract.length</tt> - the number of Kilobytes
  * that the text extractor should extract. Defaults to 2 (KB).</li>
  * <li><tt>labrador.document.process.meta-description</tt> - defaults to true.
  * <li><tt>labrador.document.process.meta-keywords</tt> - defaults to true.
  * </ul> */
public class LabradorDocument implements Document {
	protected static boolean PROCESS_METADESCRIPTION = 
		Boolean.parseBoolean(ApplicationSetup.getProperty("labrador.document.process.meta-description", "true"));
	protected static boolean PROCESS_METAKEYWORD = 
		Boolean.parseBoolean(ApplicationSetup.getProperty("labrador.document.process.meta-keyword", "true"));

	/** the stream to dump the anchor text to */
	protected PrintWriter anchorTextWriter = null;
	
	/** stores the titles found while parsing the documents */
	protected PrintWriter titleWriter = null;
	
	/** stores the text of documents found while parsing the documents */
	protected PrintWriter abstractWriter = null;
	
	/** stores the content-type of documents */
	protected PrintWriter contentTypeWriter = null;
	
	/** stores the url of documents */
	protected PrintWriter urlWriter = null;
	
		
	/** The maximum length of a token in the check method. */
	protected final static int tokenMaximumLength = ApplicationSetup.MAX_TERM_LENGTH;
	
	/** Change to lowercase? */
	protected final static boolean lowercase = Boolean.parseBoolean(ApplicationSetup.getProperty("lowercase", "true"));
	
	/** the set of boundaries we use to identify the end of sentence */
	private static HashSet<String> sentenceBoundaryTags = null;
	
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
	protected int EXTRACT_LENGTH_LIMIT =
		Integer.parseInt(ApplicationSetup.getProperty("labrador.document.extract.length","2"))
		 * 1024; //2Kbyte of text

	/** The buffer that stores the extract of the document  */
	protected StringBuilder extract = new StringBuilder(EXTRACT_LENGTH_LIMIT);

	/** the base url of the current document */
	protected URL base = null; 
	
	/** the number of the current document */
	protected String docno = null;
	
	/** The tags to process or skip.*/
    protected TagSet _tags = null;

	protected boolean hasWhitelist = false;
	/** Indicates whether we are in a tag not to process. */
	protected boolean inTagToProcess = false;
	/** Indicates whether we are in a tag to skip. */
	protected boolean inTagToSkip = false;

	static {
		final String[] boundaryTags = ApplicationSetup.getProperty(
					"labrador.document.sentence.boundary.tags",
					"DIV,H1,H2,H3,H4,H5,H6,BR,P,LI,OL,DL,DT,TD"
				).toUpperCase().split("\\s*,\\s*");
		sentenceBoundaryTags = new HashSet<String>(Arrays.asList(boundaryTags));
	}
	
	public LabradorDocument(InputStream s, Map<String,String> properties, PrintWriter... writers)
	{ 
		this(s, properties);
		anchorTextWriter = writers[0];
		titleWriter = writers[1];
		abstractWriter = writers[2];
		contentTypeWriter = writers[3];
		urlWriter = writers[4];
	}

	public LabradorDocument(InputStream s, Map<String,String> properties)
	{
		docProperties = properties;
		inputStream = s;
		docno = docProperties.get("docid");
		String url = docProperties.get("url");
		if (url != null)
		{
			try {
				base = new URL(url);
			} catch(MalformedURLException use) {
				logger.error("Exception while creating a LabradorDocument: ", use);
				logger.error("docProperty url: " + docProperties.get("url"));
			}
		}
		else
		{
			logger.warn("No URL property set for this document. Relative URLs may not work as expected");
		}
		String charset = StringTools.normaliseEncoding( docProperties.get("charset") );
		try{
			//recommend the HTTP encoding, if necessary
			if (charset != null)
			{
				lexer = new Lexer(new Page(new InputStreamSource(s, charset)));
			}
			else
			{
				lexer = new Lexer(new Page(new InputStreamSource(s)));
			}
			
		} catch(UnsupportedEncodingException uee) {
			logger.warn("Exception while creating a LabradorDocument, UnsupportedEncodingException for "+ charset +" - trying withouth specifying encoding: ", uee);
			try{
				lexer = new Lexer(new Page(new InputStreamSource(s)));
			} catch (UnsupportedEncodingException uee2) {
				 logger.error("Exception while creating a LabradorDocument, couldnt find a good encoding for this document", uee2);
				//TODO more resiliance - can be default to better encodings, or skip the document entirely
			}
		}
		this._tags = new TagSet(TagSet.TREC_DOC_TAGS);
		this.hasWhitelist = this._tags.hasWhitelist();
	}

	public Reader getReader() {
		return new InputStreamReader(inputStream);
	}
	
	private boolean alreadyPrintMetadata = false;
	
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
		//checking whether the metadata has been printed
		//in an earlier call of this method
		if (EOD && !alreadyPrintMetadata) {
			alreadyPrintMetadata = true;
			String abstractText = extract.toString();
			abstractText = abstractText.substring(0,Math.min(EXTRACT_LENGTH_LIMIT, abstractText.length()));
			abstractText = StringEscapeUtils.unescapeHtml(abstractText).replaceAll("\\s+", " ").replaceAll("\\.(\\s+\\.)+",".").replaceAll("<\\?xml.*?>","").trim();
			if(abstractWriter != null)
				abstractWriter.println(docno + " " + abstractText);
			if (title == null || title.equals("")) {
				title = abstractText.substring(0, Math.min(100,abstractText.length()));
				//title = title.substring(0, title.lastIndexOf(' '));
			} else {
				title = StringEscapeUtils.unescapeHtml(title).replaceAll("\\s+", " ");
			}
			if(titleWriter != null)
				titleWriter.println(docno + " " + title);
			if (urlWriter != null)
				urlWriter.println(docno + " " + base);
			if (contentTypeWriter != null)	
				contentTypeWriter.println(docno + " " + docProperties.get("content-type") + " " + lexer.getPosition());
		}
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
		
		while (currentTextPosition<currentTextLength && (( (ch = currentText.charAt(currentTextPosition)) >= 'a' && ch <='z') || (ch >= 'A' && ch <='Z') || (ch >= '0' && ch <= '9'))) {
			termToReturn.append(ch);
			currentTextPosition++;
		}
			 
		//Vassilis, 27/09/2006
		//the following code can be probably used to allow the indexing
		//of terms followed by #, or a +, or a ++
		//It is hardcoded for the moment
		/*
		//if the term is followed by a +, or # or -, or ++, then do something like keep those characters as part of the term
		if (currentTextPosition<currentTextLength && ((ch = currentText.charAt(currentTextPosition)) == '#')) {
			termToReturn.append(ch);
			currentTextPosition++;
		} else if (currentTextPosition<currentTextLength && ((ch = currentText.charAt(currentTextPosition)) == '+')) {
			termToReturn.append(ch);
			currentTextPosition++;
			if (currentTextPosition<currentTextLength && ((ch = currentText.charAt(currentTextPosition)) == '+')) {
				termToReturn.append(ch);
				currentTextPosition++;
			}
		}
		*/
		
		final int length = termToReturn.length();
		if (length == 0 || length > tokenMaximumLength)
			return null;
		return lowercase ? termToReturn.toString().toLowerCase() : termToReturn.toString();
		
		/*
		 * if (stringToReturn.equals("c") || stringToReturn.equals("C")) {
		 * 		if (currentTextPosition<currentTextLength) {
		 * 			ch = currentText.charAt(currentTextPosition);
		 * 			if (ch='#') {
		 * 				//append
		 * 			} else if (ch='+') {
		 * 				//append and continue
		 * 			}
		 * 	
		 * 		} 
		 * }
		 */
		
	}
	
	protected boolean justAppendedSentenceBoundary = false;

	protected UrlValidator urlValidator = new UrlValidator();

	
	/**
	 * Reads the next html node
	 * @return the text associated with the node read from the html lexer
	 */
	public String getNextNode() {		
		String text = null;
		
		//remove the field indicator for tags that 
		if (inMetaDescription) {
			fields.remove("DESCRIPTION");
		} else if (inMetaKeywords) {
			fields.remove("KEYWORDS");
		} 

		inMetaDescription = inMetaKeywords = false;
		try {
			Node node = lexer.nextNode();
			
			//end of document if node is null
			if (node == null) {
				EOD = true;
				return null;
			}
			
			if (node instanceof RemarkNode) {//ignore comments
				return null;
			} else if (node instanceof TagNode) {//we are in a tag
				final TagNode tagNode = (TagNode)node;
				final String tagName = tagNode.getTagName();
				if (tagName==null)
					return null;
				// support for TrecDocTags.process
				if (hasWhitelist && _tags.isTagToProcess(tagName))
				{
					inTagToProcess = ! tagNode.isEndTag();
				}
				// support for TrecDocTags.skip
				if (_tags.isTagToSkip(tagName))
				{
					inTagToSkip = ! tagNode.isEndTag();
				}
	
				if (tagName.equalsIgnoreCase("TITLE")) {
					inTitle = tagNode.isEndTag() ? false : true;
					if (inTitle)
						fields.add("TITLE");
					else
						fields.remove("TITLE");
				} 
				else if (tagName.equalsIgnoreCase("META"))
				{
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
							if (PROCESS_METADESCRIPTION && attrValue.equalsIgnoreCase("KEYWORDS"))
								inMetaKeywords = true;
							else if (PROCESS_METAKEYWORD && attrValue.equalsIgnoreCase("DESCRIPTION")) 
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
						return ((hasWhitelist && ! inTagToProcess) || inTagToSkip)
							? null
							: StringEscapeUtils.unescapeHtml(text).replaceAll("\\s+", " ");
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
									link = attrValue;
									link = new URL(base, link).toString();
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
							if (attrName==null || attrValue == null) 
								continue;
							if (attrName.equalsIgnoreCase("ALT")) {
								text = attrValue.trim();
								break;
							}
						}
						if (link!=null && text!=null && text.length() > 0) {
							//when checking for the validity of a URL, make sure that
							//the urlValidator does not throw an exception that is a show stopper
							boolean isValid = false;
							try {
								isValid = urlValidator.isValid(link);
								
							} catch(Exception e) {
								if (logger.isDebugEnabled())
									logger.debug("Exception caught while checking the following url for being valid: " + link, e);
							}
							if (isValid) {
								if (logger.isDebugEnabled() && text.matches("\\.\\."))
									logger.debug("check out: " + base + " " + link +" ALTEXT " + text);
								//write the target url, the src docno and the alt text
								if (anchorTextWriter != null)
									anchorTextWriter.println(link + " " + docno + " ALTEXT " + StringEscapeUtils.unescapeHtml(text).replaceAll("\\s+", " "));										
							}
							inLink = false;
						}
						//return text==null ? null : StringEscapeUtils.unescapeHtml(text).replaceAll("\\s+", " ");
					}
				} else if (tagName.equalsIgnoreCase("SCRIPT")) {
					inScript = ! tagNode.isEndTag();
				} else if (tagName.equalsIgnoreCase("STYLE")) {
					inStyle = ! tagNode.isEndTag();
				} else if (tagName.equalsIgnoreCase("ATEXT")) {
					if (tagNode.isEndTag()) {
						inAtext = false; fields.remove("ATEXT");
					} else {
						inAtext = true; fields.add("ATEXT");
					}
				} else if (tagName.equalsIgnoreCase("ALTTEXT")) {
					if (tagNode.isEndTag()) {
						inAltText = false; fields.remove("ALTTEXT");
					} else {
						inAltText = true; fields.add("ALTTEXT");
					}
				} else if (sentenceBoundaryTags.contains(tagName.toUpperCase()) && tagNode.isEndTag() && !justAppendedSentenceBoundary) {
					extract.append("."); justAppendedSentenceBoundary = true;
				} else if (tagNode.isEmptyXmlTag()) {
					return null;
				}
					
			
			} else if (node instanceof TextNode && !inScript && !inStyle) {//we are in text but not in a script tag
				text = node.getText();
				if (!text.equals("")) {
					text = StringEscapeUtils.unescapeHtml(text).replaceAll("\\s+", " ").trim();
					if (inTitle) {
						title = title==null ? text : title+ " " + text;
					} else if (extract.length()<EXTRACT_LENGTH_LIMIT && !text.equals(" ")) {
						extract.append(" " + text); justAppendedSentenceBoundary = false;
					} 
					if (inLink && link!=null && text.length() > 0) {
						
						//make sure that the urlValidator does not throw an exception that is a show stopper
						boolean isValid = false;
						try {
							isValid = urlValidator.isValid(link);
						} catch(Exception e) {
							if (logger.isDebugEnabled())
								logger.debug("Exception caught while checking the following url for being valid: " + link, e);							
						}
						
						if (isValid) {
							if (anchorTextWriter != null)
								anchorTextWriter.println(link + " " + docno + " ATEXT " + StringEscapeUtils.unescapeHtml(text).replaceAll("\\s+", " ") /*text*/);
							if (logger.isDebugEnabled() && text.matches("\\.\\."))
								logger.debug("check out: " + base + " " + link + " ATEXT " + text);
						}
					}
					return ((hasWhitelist && ! inTagToProcess) || inTagToSkip)
							? null 
							: text.trim();
				}
			}
		} catch(ParserException pe) {
			logger.error("IOException while creating a LabradorDocument. ", pe);
		}
		return null;
	}
	
	public Set<String> getFields() {
		return fields;
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

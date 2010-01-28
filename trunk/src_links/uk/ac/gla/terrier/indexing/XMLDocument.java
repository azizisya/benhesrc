package uk.ac.gla.terrier.indexing;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import uk.ac.gla.terrier.utility.ApplicationSetup;

public class XMLDocument implements Document {

	/** The size of the buffer that keeps the extract of the document */
	protected int EXTRACT_LENGTH_LIMIT = 2 * 1024; //2Kbyte of text
	
	private static Logger logger = Logger.getRootLogger();

	protected String docno;
	
	protected MessageDigest md5;
	
	protected HashSet<String> fields;
	
	protected XmlPullParser xpp; 
	
	protected InputStream in; 
	
	protected HashMap<String,String> properties;
		
	protected boolean inTitle = false;
	
	protected boolean inSummary = false;
	
	protected boolean inContent = false;
	
	protected boolean inURL = false;
	
	protected boolean inSource = false;
	
	protected boolean EOD = false;
	
	protected String url;
	
	protected String title;
	
	protected StringBuffer content;
	
	protected String contentType = "text/html";
	
	/** Change to lowercase? */
	protected final static boolean lowercase = (new Boolean(ApplicationSetup.getProperty("lowercase", "true"))).booleanValue();

	/** The maximum length of a token in the check method. */
	protected final static int tokenMaximumLength = ApplicationSetup.MAX_TERM_LENGTH;
	
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

	/** length of document in characters */
	protected int length;
	
	public XMLDocument(XmlPullParser xpp, InputStream in, PrintWriter aTextW, PrintWriter tTextW, PrintWriter eTextW, PrintWriter ctTextW, PrintWriter urlTextW) {
	
		content = new StringBuffer();
		
		anchorTextWriter = aTextW;
		titleWriter = tTextW;
		abstractWriter = eTextW;
		contentTypeWriter = ctTextW;
		urlWriter = urlTextW;
		
		this.xpp = xpp;
		this.in = in;
		fields = new HashSet<String>();
		properties = new HashMap<String,String>();
		try {
			md5 = MessageDigest.getInstance("MD5");
			md5.reset();
		} catch(NoSuchAlgorithmException nsae) {
			logger.error("no such algorithm exception while instantiating md5.", nsae);
		}
	}
	
	public HashSet getFields() {
		return fields;
	}
	
	boolean hasPrintedMetadata = false;
	
	public Reader getReader() {
		return new InputStreamReader(in);
	}
	
	public boolean endOfDocument() {
		if (EOD && !hasPrintedMetadata) {
			hasPrintedMetadata = true;
			title = title!=null ? StringEscapeUtils.unescapeXml(title).replaceAll("\\s+", " ") : "";
			titleWriter.println(docno + " " + title);
			urlWriter.println(docno + " " + url);
			
			String abstractText = content.toString();
			abstractText = abstractText.substring(0,Math.min(EXTRACT_LENGTH_LIMIT, abstractText.length()));
			abstractText = StringEscapeUtils.unescapeXml(abstractText).replaceAll("\\s+", " ");
			abstractWriter.println(docno + " " + content.toString());
			contentTypeWriter.println(docno + " " + contentType);
		}
		return EOD;
	}
	
	public String getDocno() {
		return docno;
	}
	
	public String getProperty(String propName) {
		return (String) properties.get(propName);
	}

	public Map getAllProperties() {
		return properties;
	}
	
	String currentText;
	
	int currentTextPosition;
	int currentTextLength;
	
	public String getRealNextTerm() {
		char ch;
		
		
		//if the current text is null and we
		//haven't reached the end of document yet,
		//get the next node from the html lexer
		while (currentText==null && !EOD) {
			if ((currentText = process())!=null) {
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
		StringBuffer termToReturn = new StringBuffer(tokenMaximumLength);
		
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
	
	public int getPosition() {
		return length;
	}
	
	protected String process() {
		try {
			int eventType = xpp.next();
			if (eventType == XmlPullParser.END_DOCUMENT) {
				//logger.debug("end of document - shouldn't be here");
				return null;
			} else if (eventType == XmlPullParser.START_TAG) {
				String name = xpp.getName();
				if (name.equalsIgnoreCase("URL")) {
					//logger.debug("beginning of url");
					inURL = true;
					length+=5; //length of <url>
				} else if (name.equalsIgnoreCase("TITLE")) {
					inTitle = true;
					//logger.debug("beginning of title");
					fields.add("TITLE");
					length+=7; //length of <title>
				} else if (name.equalsIgnoreCase("SUMMARY")) { 
					inSummary = true;
					//logger.debug("beginning of summary");
					length+=9; //length of <summary>
				} else if (name.equalsIgnoreCase("CONTENT")) {
					inContent = true;
					//logger.debug("beggining of content");
					length+=9; //length of <content>
				} else if (name.equalsIgnoreCase("SOURCE")) {
					inSource = true;
					length+=8; //length of <source>
					//logger.debug("beggining of source");
				}
			} else if (eventType == XmlPullParser.END_TAG) {
				String name = xpp.getName();
				//md5.update(name.getBytes());
				if (name.equalsIgnoreCase("URL")) {
					//logger.debug("end of url");
					length+=6; //length of </url>
					inURL = false;
				} else if (name.equalsIgnoreCase("TITLE")) {
					inTitle = false;
					length+=8; //length of </title>
					//logger.debug("end of title");
					fields.remove("TITLE");
				} else if (name.equalsIgnoreCase("SUMMARY")) { 
					inSummary = false;
					//logger.debug("end of summary");
					length+=10; //length of </summary>
				} else if (name.equalsIgnoreCase("CONTENT")) {
					//logger.debug("end of content");
					inContent = false;
					length+=10; //length of </content>
				} else if (name.equalsIgnoreCase("SOURCE")) {
					//logger.debug("end of source");
					inSource = false;
					length+=9; //length of </source> 
				} else if (name.equalsIgnoreCase("ARTICLE")) {
					//logger.debug("end of article");
					//BigInteger bi = new BigInteger(md5.digest());
					//docno = bi.toString(16);
					docno = bytesToHex(md5.digest()).toLowerCase();
					//logger.debug("docno: " + docno);
					EOD = true;
					length+=10; //length of </article>
					//logger.debug("document length in chars: " + length);
				}
			} else if (eventType == XmlPullParser.TEXT) {
				if (inURL) {
					url = xpp.getText();
					base = new URL(url);
					foundURL = true;
					length+=url.length();
					//logger.debug("url: " + url);
					md5.update(url.getBytes());
					properties.put("url", url);
				} else {
					String text = xpp.getText();
					length+=text.length();
					//md5.update(text.getBytes());
					text = StringEscapeUtils.unescapeXml(text).replaceAll("\\s+", " ");
					if (!inSource && content.length()<EXTRACT_LENGTH_LIMIT && !text.equals(" ")) {
						content.append(" " + text.trim()); 
					}
					if (inTitle) 
						title = text;
					return text;
				}
			}
			
		} catch(IOException ioe) {
			logger.error("io exception.", ioe);
		} catch(XmlPullParserException xppe) {
			logger.error("xml pull parser exception", xppe);
		}
		return null;
	}
	
   /**
     *  Convenience method to convert a byte array to a hex string.
     *
     * @param  data  the byte[] to convert
     * @return String the converted byte[]
     */
    public static String bytesToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            buf.append(byteToHex(data[i]).toUpperCase());
        }
        return (buf.toString());
    }


    /**
     *  method to convert a byte to a hex string.
     *
     * @param  data  the byte to convert
     * @return String the converted byte
     */
    public static String byteToHex(byte data) {
        StringBuffer buf = new StringBuffer();
        buf.append(toHexChar((data >>> 4) & 0x0F));
        buf.append(toHexChar(data & 0x0F));
        return buf.toString();
    }

    /**
     *  Convenience method to convert an int to a hex char.
     *
     * @param  i  the int to convert
     * @return char the converted char
     */
    public static char toHexChar(int i) {
        if ((0 <= i) && (i <= 9)) {
            return (char) ('0' + i);
        } else {
            return (char) ('a' + (i - 10));
        }
    }

	protected static HashSet<String> urlFields = new HashSet<String>(); 
	
	static {
		urlFields.add("URLHOST");
		urlFields.add("URLFILE");
		urlFields.add("URLQUERY");
		urlFields.add("URLPATH1");
		urlFields.add("URLPATH2");
		urlFields.add("URLPATHOTHER");
	}

	
	/** the terms that match part of the url */
	protected HashSet<String> urlTextMatchedTerms = new HashSet<String>(5); //5 is heuristic
	
	/** the terms that have been checked against the url */
	protected HashSet<String> urlTextCheckedTerms = new HashSet<String>(40); //avg document length
    
	protected URL base = null;
	
	protected boolean foundURL = false;
	
	public String getNextTerm() {
		
		//we haven't found the document's url yet
		if (!foundURL) 
			return getRealNextTerm();
		
		fields.removeAll(urlFields);
		String outputTerm = getRealNextTerm();
		if (outputTerm==null)
			return null;
		
		if (urlTextMatchedTerms.contains(outputTerm) || urlTextCheckedTerms.contains(outputTerm))
			return outputTerm;
		
		if (url.contains(outputTerm)) {
			if (base.getHost().contains(outputTerm)) {
				urlTextMatchedTerms.add(outputTerm);
				fields.add("URLHOST");
			}
			if (base.getFile().contains(outputTerm)) {
				urlTextMatchedTerms.add(outputTerm);
				fields.add("URLFILE");
			}
			String query = base.getQuery();
			if (query!=null && query.contains(outputTerm)) {
				urlTextMatchedTerms.add(outputTerm);
				fields.add("URLQUERY");
			}
			
			//get to the path
			String sPath = base.getPath();
			String[] pathBits = sPath.split("/");
			if (pathBits.length > 1) {
				boolean pathFound = false;
				if (pathBits[0].contains(outputTerm)) {
					urlTextMatchedTerms.add(outputTerm);
					pathFound = true;
					fields.add("URLPATH1");
				}
				if (pathBits.length > 1 && pathBits[1].contains(outputTerm)) {
					pathFound = true;
					urlTextMatchedTerms.add(outputTerm);
					fields.add("URLPATH2");
				}
				
				if (!pathFound && sPath.contains(outputTerm)) {
					urlTextMatchedTerms.add(outputTerm);
					fields.add("URLPATHOTHER");
				}
			}
		}
		return outputTerm;
	}

    
}
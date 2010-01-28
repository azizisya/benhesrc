package uk.ac.gla.terrier.indexing;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import uk.ac.gla.terrier.utility.ApplicationSetup;

public class XMLCollection implements Collection {
	
	/** stores the anchor text found while parsing the documents */
	protected PrintWriter anchorTextWriter = null;
	
	/** stores the titles found while parsing the documents */
	protected PrintWriter titleWriter = null;
	
	/** stores the text of documents found while parsing the documents */
	protected PrintWriter abstractWriter = null;
	
	/** stores the content-type of documents */
	protected PrintWriter contentTypeWriter = null;
	
	/** stores the url of documents */
	protected PrintWriter urlWriter = null;
	
	/** Indicates whether the end of the collection has been reached.*/
	protected boolean endOfCollection = false;
	
	/** A boolean which is true when a new file is open.*/
	protected boolean SkipFile = false;

	protected InputStream in = null;
	
	private static Logger logger = Logger.getRootLogger();

	protected XmlPullParser xpp = null;
	
	/**
	 * The list of files to process.
	 */
	protected ArrayList<String> FilesToProcess = new ArrayList<String>();
	/** The index in the FilesToProcess of the currently processed file.*/
	protected int FileNumber = 0;

	
	protected void openAnchorAltWriters() {
		try {
			String filenameNoExt = ApplicationSetup.TERRIER_INDEX_PATH + 
								   ApplicationSetup.FILE_SEPARATOR +
								   ApplicationSetup.TERRIER_INDEX_PREFIX + "."; 
			
			String anchorTextFilename = filenameNoExt + ApplicationSetup.getProperty("extracted.anchor.text.file","anchor-text.gz");
			anchorTextWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(anchorTextFilename)))));
			
			String titleTextFilename = filenameNoExt + ApplicationSetup.getProperty("extracted.title.text.file","title.gz");
			titleWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(titleTextFilename)))));

			String extractTextFilename = filenameNoExt + ApplicationSetup.getProperty("extracted.extract.text.file","extract-text.gz");
			abstractWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(extractTextFilename)))));

			String contentTypeFilename = filenameNoExt + ApplicationSetup.getProperty("extracted.content.type.file","content-type.gz");
			contentTypeWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(contentTypeFilename)))));
			
			String urlTextFilename = filenameNoExt + ApplicationSetup.getProperty("extracted.url.text.file","url-text.gz");
			urlWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(urlTextFilename)))));
			
		} catch(IOException ioe) {
			logger.error("IOException while opening anchor and alt text writers.", ioe);
		}
	}

	
	public XMLCollection() {
		this(ApplicationSetup.COLLECTION_SPEC);
	}

	
	public XMLCollection(String CollectionSpecFilename) {
	
		try {
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
	        factory.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);

	        xpp = factory.newPullParser();			
		} catch(XmlPullParserException xppe) {
			logger.error("xml pull parser exception: ", xppe);
		}
		
		//reads the collection specification file
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(
					CollectionSpecFilename)));
			String filename = null;
			FilesToProcess = new ArrayList<String>();
			while ((filename = br.readLine()) != null) {
				if (!filename.startsWith("#") && !filename.equals(""))
					FilesToProcess.add(filename);
			}
			br.close();
			logger.info("read collection specification");
			
			openNextFile();
			xpp.setInput(in,"ISO-8859-1");
			

		} catch (IOException ioe) {
			logger.error("Input output exception while loading the collection.spec file. ("+CollectionSpecFilename+")Stack trace follows.", ioe);
		} catch(XmlPullParserException xppe) {
			logger.error("xml parser exception while opening the first file.", xppe);
		}
		openAnchorAltWriters();

	}
	
	public boolean nextDocument() {
		try {
			boolean returnValue = true;
			int eventType;
			boolean scanMore = false;
			do {
				//logger.debug("calling nextDocument()");
				do {
					eventType = xpp.next();
					//logger.debug("tag: " + xpp.getName());					
				} while (xpp.getName()==null);

				if (eventType == XmlPullParser.START_TAG) {
					//logger.debug("found start tag: " + xpp.getName());
					if (xpp.getName().equalsIgnoreCase("article")) {
						scanMore = false;
						returnValue = true;
					} else {
						scanMore = true;
					}	
				} else if (eventType == XmlPullParser.END_TAG) {
					//logger.debug("found end tag: " + xpp.getName());
					if (xpp.getName().equalsIgnoreCase(("articles"))) {
						if (openNextFile()) {
							xpp.setInput(in, "ISO-8859-1");
							//logger.debug("tag name:" + xpp.getName());
							scanMore = true;
						} else {
							//logger.debug("found the end of the collection at last");
							endOfCollection = true;
							scanMore = false;
							returnValue = false;
						}						
					}
				} else if (eventType == XmlPullParser.END_DOCUMENT) {
					if (openNextFile()) {
						xpp.setInput(in, "ISO-8859-1");
						//logger.debug("tag name:" + xpp.getName());
						scanMore = true;
					} else {
						//logger.debug("found the end of the collection at last");
						endOfCollection = true;
						scanMore = false;
						returnValue = false;
					}											
				}
			} while (scanMore == true);
			return returnValue;
		} catch(IOException ioe) {
			logger.error("ioexception while parsing the xml input.", ioe);
		} catch(XmlPullParserException xpe) {
			logger.error("exception while parsing the xml input.", xpe);
		}
		return false;
	}

	public Document getDocument() {
		currentDocument = new XMLDocument(xpp, in, anchorTextWriter, titleWriter, abstractWriter, contentTypeWriter, urlWriter);
		return currentDocument;
	}

	XMLDocument currentDocument;
	
	public String getDocid() {
		return currentDocument.getDocno();
	}

	public boolean endOfCollection() {
		if (endOfCollection)
			close();
		return endOfCollection;
	}

	public void reset() {
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
	}

	protected boolean openNextFile() throws IOException {
		//try to close the currently open file
		if (in!=null)
		try{
			in.close();
		}catch (IOException ioe) {/* Ignore, it's not an error */ }
		//keep trying files
		boolean tryFile = true;
		//return value for this fn
		boolean rtr = false;
		while(tryFile)
		{
			if (FileNumber < FilesToProcess.size()) {
				SkipFile = true;
				String filename = (String) FilesToProcess.get(FileNumber);
				FileNumber++;
				//check the filename is sane
				File f = new File(filename);
				if (! f.exists())
				{
					logger.warn("Could not open "+filename+" : File Not Found");
				}
				else if (! f.canRead())
				{
					logger.warn("WARNING: Could not open "+filename+" : Cannot read");
				}
				else
				{//filename seems ok, open it
					in = openFile(filename); //throws an IOException, throw upwards
					logger.info("Processing "+filename);
					//no need to loop again
					tryFile = false;
					//return success
					rtr = true;
				}
			} else {
				//last file of the collection has been read, EOC
				endOfCollection = true;
				rtr = false;
				tryFile = false;
			}
		}
		return rtr;
	}

	/** Opens a reader to the filecalled filename. Provided for easy overriding for encoding support etc in 
	  * child classes. Called from openNextFile().
	  * @param filename File to open.
	  * @return BufferedReader of the file
	  */
	protected InputStream openFile(String filename) throws IOException
	{
		BufferedInputStream rtr = null;
		if (filename.toLowerCase().endsWith("gz")) {
			rtr = new BufferedInputStream(
				new GZIPInputStream(new FileInputStream(filename)));
		} else {
			rtr = new BufferedInputStream(
				new FileInputStream(filename));
		}
		return rtr;
	}

	public void close() {
		anchorTextWriter.flush();
		anchorTextWriter.close();
		titleWriter.flush();
		titleWriter.close();
		abstractWriter.flush();
		abstractWriter.close();
		contentTypeWriter.flush();
		contentTypeWriter.close();
		urlWriter.flush();
		urlWriter.close();
		logger.debug("closing XMLCollection object");
	}

	
}
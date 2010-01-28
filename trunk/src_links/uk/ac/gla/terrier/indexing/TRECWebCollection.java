package uk.ac.gla.terrier.indexing;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.zip.GZIPOutputStream;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.apache.log4j.Logger;

import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.LookAheadStream;
import uk.ac.gla.terrier.utility.NullWriter;

/** A better version of TRECCollection that is able to parse DOCHDR tags and extract
  * useful stuff like URLs from them 
  * <p><b>Properties:</b><ul>
  * <li><tt>trecwebcollection.extract.webstuff</tt> - use this property to disable all the extraction stuff</li>
  * <li><tt>trecwebcollection.dochdr.tag</tt> - SGML tag which contains the DOCHDR tag.</li>
  * <li><tt>trecwebcollection.extract.anchor-text.filename</tt></li>
  * <li><tt>trecwebcollection.extract.title-text.filename</tt></li>
  * <li><tt>trecwebcollection.extract.extract-text.filename</tt></li>
  * <li><tt>trecwebcollection.extract.content-type.filename</tt></li>
  * <li><tt>trecwebcollection.extract.url-text.filename</tt></li>
  * <li><tt>trecwebcollection.use.url-text</tt> - defaults to false</li>
  */
public class TRECWebCollection extends TRECCollection {

	protected static boolean extractWebStuff =
        Boolean.parseBoolean(
            ApplicationSetup.getProperty("trecwebcollection.extract.webstuff","true")
        );
	protected static boolean useURLText = 
		Boolean.parseBoolean(
			ApplicationSetup.getProperty("trecwebcollection.use.url-text", "false")
		);
	protected static Logger logger = Logger.getRootLogger();
	
	private char[] start_dochdrTag;
	private int start_dochdrTagLength;
	private char[] end_dochdrTag;
	private int end_dochdrTagLength;

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
	
	/**
	 * protected method for initialising the
	 * opening and closing document header tags.
	 */
	protected void setTags(String TagSet)
	{
		super.setTags(TagSet);
		final String tagName = ApplicationSetup.getProperty("trecwebcollection.dochdr.tag","DOCHDR");
		final String tmpDocHdrTag = "<"+tagName + ">";
		final String tmpEndDocHdrTag = "</" + tagName + ">";
		start_dochdrTag = tmpDocHdrTag.toCharArray();
		start_dochdrTagLength = start_dochdrTag.length;
		end_dochdrTag = tmpEndDocHdrTag.toCharArray();
		end_dochdrTagLength = end_dochdrTag.length;
	}

	/** protected method initialises the extraction writers used by the Document objects.
	  * All extractors can be disabled by setting property <tt>trecwebcollection.extract.webstuff</tt> to false.
	  * Individual extractors can be disabled by settings their filename to empty */
	protected void openAnchorAltWriters() {
		logger.info("TRECWebCollection opening extraction writers");
		try {
			String filenameNoExt = ApplicationSetup.TERRIER_INDEX_PATH + 
								   ApplicationSetup.FILE_SEPARATOR +
								   ApplicationSetup.TERRIER_INDEX_PREFIX + "."; 

			/* for each writer, use a NullWriter if the filename length is 0, thus disabling that particular extraction writer */
			
			String anchorTextFilename = ApplicationSetup.getProperty("trecwebcollection.extract.anchor-text.filename","anchor-text.gz");
			anchorTextWriter =  new PrintWriter(anchorTextFilename.length() > 0
				? Files.writeFileWriter(filenameNoExt + anchorTextFilename)
				: new NullWriter());
			
			String titleTextFilename = ApplicationSetup.getProperty("trecwebcollection.extract.title-text.filename","title.gz");
			
			titleWriter = new PrintWriter(titleTextFilename.length() > 0
				? Files.writeFileWriter(filenameNoExt + titleTextFilename)
				: new NullWriter());

			String extractTextFilename = ApplicationSetup.getProperty("trecwebcollection.extract.extract-text.filename","extract-text.gz");
			abstractWriter = new PrintWriter(extractTextFilename.length() > 0 
				? Files.writeFileWriter(filenameNoExt + extractTextFilename)
				: new NullWriter());

			String contentTypeFilename = ApplicationSetup.getProperty("trecwebcollection.extract.content-type.filename","content-type.gz");
			contentTypeWriter = new PrintWriter(contentTypeFilename.length() > 0 
				? Files.writeFileWriter(filenameNoExt + contentTypeFilename)
				: new NullWriter());
			
			String urlTextFilename = ApplicationSetup.getProperty("trecwebcollection.extract.url-text.filename","url-text.gz");
			urlWriter = new PrintWriter(urlTextFilename.length() > 0
				? Files.writeFileWriter(filenameNoExt +urlTextFilename)
				: new NullWriter());
			
		} catch(IOException ioe) {
			logger.error("IOException while opening anchor and alt text writers.", ioe);
		}
	}
	
	public TRECWebCollection()
	{
		super();
		docPointersEnabled = false;
		if (extractWebStuff)
			openAnchorAltWriters();
	}

	public TRECWebCollection(String CollectionSpecFilename, String TagSet, String BlacklistSpecFilename, String docPointersFilename) {
		super(CollectionSpecFilename, TagSet, BlacklistSpecFilename, docPointersFilename);
		docPointersEnabled = false;
		if (extractWebStuff)
			openAnchorAltWriters();
	}

	/**
	 * Moves to the next document to process from the collection.
	 * @return boolean true if there are more documents to process in the 
	 *		 collection, otherwise it returns false.
	 */
	public boolean nextDocument() {
		//move the stream to the start of the next document
		//try next file if no DOC tag found. (and set endOfCOllection if
		//no files left)
		/* contains the documentID as it is built up. put into ThisDocID when
		 * a document is fully found */
		StringBuffer DocumentIDContents = null;

		DocProperties = new HashMap<String,String>(15);


		/* state of the parser. This is equal to how many characters have been
		 * found of the currently desired string */
		int State = 0;
		// the most recently found character
		int c;
		if (trecDocument != null) {
			fileOffset += trecDocument.counter + end_docTagLength - 1;
			endDocumentOffset = fileOffset - 1;
			if(docPointersEnabled && docPointersStream != null)
			{
				try {
					docPointersStream.writeLong(startDocumentOffset);
					docPointersStream.writeLong(endDocumentOffset);
					docPointersStream.writeInt(FileNumber);
					documentCounter++;
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
		}

		boolean bScanning = true;
		scanning: while(bScanning)
		{
			try {
				State = 0;
				DocumentIDContents = new StringBuffer();
				//looking for doc tag
				while (State < start_docTagLength) {
					if ((c = br.read()) == -1) {
						if (openNextFile()) {
							continue scanning;//continue;
						} else {
							endOfCollection = true;
							if (docPointersEnabled)
								docPointersStream.close();
							return false;
						}
					}
				
					if (Character.toUpperCase((char)c) == start_docTag[State]) {
						if (State == 0)
							startDocumentOffset = fileOffset;
						State++;
					} else
						State = 0;
					fileOffset++;
				}
				//looking for docno tag
				State = 0;
				while (State < start_docnoTagLength) {
					if ((c = br.read()) == -1) {
						if (openNextFile()) {
							logger.warn("WARNING: 1 Forced a skip");
							continue scanning;
						} else {
							endOfCollection = true;
							if (docPointersEnabled)
								docPointersStream.close();
							return false;
						}
					}
					if (Character.toUpperCase((char)c) == start_docnoTag[State])
						State++;
					else
						State = 0;
					fileOffset++;
				}
				//looking for end of docno
				State = 0;
				synchronized(DocumentIDContents)
				{
					while (State < end_docnoTagLength) {
						if ((c = br.read()) == -1) {
							if (openNextFile()) {
								continue scanning;
							} else {
								endOfCollection = true;
								if (docPointersEnabled)
									docPointersStream.close();
								return false;
							}
						}
						if (Character.toUpperCase((char)c) == end_docnoTag[State])
							State++;
						else {
							State = 0;
						//ThisDocID += (char) c;
						DocumentIDContents.append((char)c);
						}
						fileOffset++;
					}
				}

				//looking for start of dochdr
				State = 0;
				while(State < start_dochdrTagLength) {
					if ((c = br.read()) == -1) {
						if (openNextFile()) {
							continue scanning;
						} else {
							endOfCollection = true;
							if (docPointersEnabled)
								docPointersStream.close();
							return false;
						}
					}
					if (Character.toUpperCase((char)c) == start_dochdrTag[State])
						State ++;
					else {
						State = 0;//consume character
					}
					fileOffset++;
				}

				State = 0;
				StringBuffer DocumentHdrContents = new StringBuffer();
				synchronized(DocumentHdrContents)
				{
					while(State < end_dochdrTagLength) {
						if ((c = br.read()) == -1) {
							if (openNextFile()) {
								continue scanning;
							} else {
								endOfCollection = true;
								if (docPointersEnabled)
									docPointersStream.close();
								return false;
							}
						}
						if (Character.toUpperCase((char)c) == end_dochdrTag[State])
							State++;
						else {
							State = 0;
							//save character
							DocumentHdrContents.append((char)c);
						}
						fileOffset++;
					}
				}
				State = 0;
				//got the dochdr, phew
				//got the document, phew!
				ThisDocID = DocumentIDContents.toString();

				/* we check the document blacklist, and if docid matches
				 * move on to the next document */
				if (DocIDBlacklist.contains(ThisDocID)) {
					//ThisDocID = new String();
					//return nextDocument();
					continue scanning;
				}
				
				//parse the TREC header
				parseHeader(DocumentHdrContents.toString());


			} catch (IOException ioe) {
				logger.error("Error Reading "
					+ FilesToProcess.get(FileNumber) + " : " + ioe
					+ ", skipping rest of file", ioe);
				FileNumber++;
				SkipFile = true;
				continue scanning;
			}
			bScanning = false;
		}
		return true;
	}

	/** Parses the DOCHDR tag contents found for the current document, and populates the
	  * document's properties hashmap, that is passed to the document object.
	  * The first line of the DOCHDR tag is assumed to tbe the URL, possibly followed by the IP
	  * address of the server. Every subsequent line is assumed to be an HTTP response header.
	  * @param hdr the contents of the DOCHDR tag. */
	protected void parseHeader(String hdr)
	{
		DocProperties.put("docid", ThisDocID);
		final String[] lines = hdr.split("\\n+");

		boolean first = false;
		for(int i=0;i<lines.length;i++)
		{
			if (lines[i].length() == 0)
				continue;

			if (! first)
			{   //first line is a special case
				first = true;
				final String[] parts = lines[i].split("\\s+");
				if(parts.length > 0)
				{
					DocProperties.put("url", parts[0]);
					if (parts.length > 1)
						DocProperties.put("ip", parts[1]);
				}
			}
			else
			{
				int Colon;
				if ((Colon = lines[i].indexOf(':') ) > 1)
				{
					/*
						Content-Type: text/html
						becomes
						content-type => text/html
						contenttype => text/html
					*/
					
					DocProperties.put(
						lines[i].substring(0,Colon).trim().toLowerCase(),
						lines[i].substring(Colon+2).trim());
					DocProperties.put(
						lines[i].substring(0,Colon).trim().toLowerCase().replaceAll("-",""),
						lines[i].substring(Colon+2).trim());
				}
			}
		}//for
		String cType = DocProperties.get("contenttype");
		if (cType == null)
			return;	
		cType = cType.toLowerCase();
		if (cType.contains("charset"))
		{
			final Matcher m = charsetMatchPattern.matcher(cType);
			if (m.find());
				DocProperties.put("charset", m.group(1));
		}
	}
	static final Pattern charsetMatchPattern = Pattern.compile("charset=(\\S+)");
	
	public Document getDocument() {
		if (useURLText)
			return new UrlLabradorDocument(
				new LookAheadStream(br, end_docTag), DocProperties, 
				anchorTextWriter, titleWriter, abstractWriter, contentTypeWriter, urlWriter);
		return new LabradorDocument(
			new LookAheadStream(br, end_docTag), DocProperties, 
			anchorTextWriter, titleWriter, abstractWriter, contentTypeWriter, urlWriter);
	}

	/** Close this TRECWebCollection object, and all enabled extractors */
	public void close() {
		super.close();
		if (extractWebStuff)
		{
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
		}
	}
	
}

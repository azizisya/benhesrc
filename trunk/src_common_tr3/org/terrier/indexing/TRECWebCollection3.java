package org.terrier.indexing;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;

import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.LookAheadStream;

/** A better version of TRECCollection that is able to parse DOCHDR tags and extract
  * useful stuff like URLs from them */
public class TRECWebCollection3 extends TRECCollection2 {

	private static Logger logger = Logger.getRootLogger();
	
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

	protected static boolean SKIP_DOCHDR = new Boolean(
		ApplicationSetup.getProperty("trecwebcollection3.skipdochdr", "false")).booleanValue();

	
	/**
	 * protected method for initialising the
	 * opening and closing document and document number
	 * tags.
	 */
	protected void setTags(String TagSet)
	{
		super.setTags(TagSet);
	
		//TODO use properties for these	
		String tmpDocHdrTag = "<DOCHDR>";
		String tmpEndDocHdrTag = "</DOCHDR>";
		start_dochdrTag = tmpDocHdrTag.toCharArray();
		start_dochdrTagLength = start_dochdrTag.length;
		end_dochdrTag = tmpEndDocHdrTag.toCharArray();
		end_dochdrTagLength = end_dochdrTag.length;
		
		// convert all tags to upper case
		for (int i=0; i<start_docTag.length; i++)
			start_docTag[i] = Character.toUpperCase((char)start_docTag[i]);
		for (int i=0; i<start_docnoTag.length; i++)
			start_docnoTag[i] = Character.toUpperCase((char)start_docnoTag[i]);
		end_docTag = end_docTag.toUpperCase();
		for (int i=0; i<end_docnoTag.length; i++)
			end_docnoTag[i] = Character.toUpperCase((char)end_docnoTag[i]);
		//System.out.println("start_docTag: "+String.copyValueOf(start_docTag));
		//System.out.println("start_docnoTag: "+String.copyValueOf(start_docnoTag));
		//System.out.println("end_docTag: "+end_docTag);
		//System.out.println("end_docnoTag: "+String.copyValueOf(end_docnoTag));
	}

	public TRECWebCollection3()
	{
		super();
		docPointersEnabled = false;
	}

	public TRECWebCollection3(String CollectionSpecFilename, String TagSet, String BlacklistSpecFilename, String docPointersFilename) {
		super(CollectionSpecFilename, TagSet, BlacklistSpecFilename, docPointersFilename);
		docPointersEnabled = false;
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
		
		///System.out.println(String.copyValueOf(start_docTag)+" "+String.copyValueOf(start_docnoTag));


		/* state of the parser. This is equal to how many characters have been
		 * found of the currently desired string */
		int State = 0;
		// the most recently found character
		int c;
		if (trecDocument != null) {
			fileOffset += trecDocument.counter + end_docTagLength - 1;
			endDocumentOffset = fileOffset - 1;
			if(docPointersEnabled)
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
					c = br.read();
					//System.out.print((char)c);
					if (c  == -1) {
						//System.out.println("c: "+c);
						if (openNextFile()) {
							//System.out.println("continue scanning");
							continue scanning;//continue;
						} else {
							endOfCollection = true;
							if (docPointersEnabled)
								docPointersStream.close();
							return false;
						}
					}
					//System.out.print((char)c);
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
					//System.out.print((char)c);
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
							//System.out.print((char)c);
						}
						fileOffset++;
					}
				}

				StringBuffer DocumentHdrContents = new StringBuffer();
				if (!SKIP_DOCHDR)
				{

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
					//got the dochdr, phew
				}
				State = 0;
				//got the document, phew!
				ThisDocID = DocumentIDContents.toString();

				/* we check the document blacklist, and if docid matches
				 * move on to the next document */
				if (DocIDBlacklist.contains(ThisDocID)) {
					//ThisDocID = new String();
					//return nextDocument();
					continue scanning;
				}

				DocProperties.put("docid", ThisDocID);
			
				if (! SKIP_DOCHDR)
				{
					//parse the TREC header
					parseHeader(DocumentHdrContents.toString());
				}


			} catch (IOException ioe) {
				logger.error("Error Reading "
					+ FilesToProcess.get(FileNumber) + " : " + ioe
					+ ", skipping rest of file");
				StringWriter sw = new StringWriter();
				ioe.printStackTrace(new PrintWriter(sw));
				logger.error("Stack trace: " + sw.toString());
				FileNumber++;
				SkipFile = true;
				continue scanning;
			}
			bScanning = false;
		}
		return true;
	}

	protected void parseHeader(String hdr)
	{
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
		}
	}
	
	public Document getDocument() {
		return new LabradorTextDocument((new LookAheadStream(br, end_docTag)), DocProperties);
	}

	public void close() {
		super.close();
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
		logger.debug("closing TRECWebCollection object");
	}
	
}

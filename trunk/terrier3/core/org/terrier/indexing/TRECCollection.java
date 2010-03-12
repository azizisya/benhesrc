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
 * The Original Code is TRECCollection.java.
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original author) 
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
 */
package org.terrier.indexing;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.log4j.Logger;

import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.Files;
import org.terrier.utility.LookAheadStream;
import org.terrier.utility.LookAheadStreamCaseInsensitive;
import org.terrier.utility.TagSet;
import org.terrier.utility.io.RandomDataInput;
/**
 * Models a TREC test collection by implementing the interfaces
 * Collection and DocumentExtractor. It provides sequential access
 * to the documents in the collection and also it can return the text
 * of a document as a String.
 * TREC format files are opened
  * using the default encoding unless the <tt>trec.encoding</tt> has been set to a valid
  * supported encoding.
 * <p><b>Properties</b>:
 * <ul><li><tt>trec.encoding</tt> - encoding to use to open all files. Leave unset for System default encoding.</li>
 * </li>
 * @author Craig Macdonald &amp; Vassilis Plachouras
 * @version $Revision: 1.42 $
 */
public class TRECCollection implements Collection, DocumentExtractor {

	/** logger for this class */	
	protected static final Logger logger = Logger.getRootLogger();
	
	/** Filename of current file */
	protected String currentFilename;
	
	/** Counts the number of documents that have been found in this file. */
	protected int documentsInThisFile = 0;

	/** properties for the current document */	
	protected Map<String,String> DocProperties = null;

	/**
	 * Counts the documents that are found in the collection, ignoring those
	 * documents that appear in the black list
	 */
	protected int documentCounter = 0;
	
	/**
	 * The random access file that contains the pointer to the documents in the
	 * collection.
	 */
	protected RandomDataInput docPointers = null;
	/**
	 * The stream that is used for writing the pointers to the documents in the
	 * collection
	 */
	protected DataOutputStream docPointersStream = null;
	/**
	 * Are we using the document pointers file
	 */
	protected boolean docPointersEnabled;
	/**
	 * The offset in the current file being read.
	 */
	protected long fileOffset = 0;
	/**
	 * the current document processed
	 */
	protected TRECDocument trecDocument = null;
	/**
	 * The starting offset of the current document
	 */
	protected long startDocumentOffset;
	/**
	 * The ending offset of the current document
	 */
	protected long endDocumentOffset;
	/**
	 * A black list of document to ignore.
	 */
	protected HashSet<String> DocIDBlacklist = new HashSet<String>();
	/**
	 * The list of files to process.
	 */
	protected ArrayList<String> FilesToProcess = new ArrayList<String>();
	/** The index in the FilesToProcess of the currently processed file.*/
	protected int FileNumber = -1;
	/** The string identifier of the current document.*/
	protected String ThisDocID;
	/** A buffered reader used for reading data.*/
	protected InputStream br;
	/** A boolean which is true when a new file is open.*/
	protected boolean SkipFile = false;
	/** The opening document tag.*/
	protected char[] start_docTag;
	/** The length of the opening document tag.*/
	protected int start_docTagLength;
	/** The closing document tag.*/
	protected String end_docTag;
	/** The length of the closing document tag.*/
	protected int end_docTagLength;
	/** The opening document number tag.*/
	protected char[] start_docnoTag;
	/** The length of the opening document number tag.*/
	protected int start_docnoTagLength;
	/** The closing document number tag.*/
	protected char[] end_docnoTag;
	/** The length of the closing document number tag.*/
	protected int end_docnoTagLength;
	/** Is the markup case-sensitive? */
	protected boolean tags_CaseSensitive;

	/** Encoding to be used to open all files. */	
	protected static String desiredEncoding = ApplicationSetup.getProperty("trec.encoding", Charset.defaultCharset().name());

	/**
	 * protected method for initialising the
	 * opening and closing document and document number
	 * tags.
	 */
	protected void setTags(String TagSet)
	{
		TagSet tagSet = new TagSet(TagSet);
		tags_CaseSensitive = tagSet.isCaseSensitive();
		String tmpDocTag = "<" + tagSet.getDocTag() + ">";
		String tmpEndDocTag = "</" + tagSet.getDocTag() + ">";
		String tmpDocnoTag = "<" + tagSet.getIdTag() + ">";
		String tmpEndDocnoTag = "</" + tagSet.getIdTag() + ">";
		start_docTag = tmpDocTag.toCharArray();
		start_docTagLength = start_docTag.length;
		start_docnoTag = tmpDocnoTag.toCharArray();
		start_docnoTagLength = start_docnoTag.length;
		end_docTag = tmpEndDocTag;
		end_docTagLength = end_docTag.length();
		end_docnoTag = tmpEndDocnoTag.toCharArray();
		end_docnoTagLength = end_docnoTag.length;
	}

	protected void readCollectionSpec(String CollectionSpecFilename)
	{
		//reads the collection specification file
		try {
			BufferedReader br2 = Files.openFileReader(CollectionSpecFilename);
			String filename = null;
			FilesToProcess = new ArrayList<String>();
			while ((filename = br2.readLine()) != null) {
				filename = filename.trim();
				if (!filename.startsWith("#") && !filename.equals(""))
					FilesToProcess.add(filename);
			}
			br2.close();
			logger.info("TRECCollection read collection specification ("+FilesToProcess.size()+" files)");
		} catch (IOException ioe) {
			logger.error("Input output exception while loading the collection.spec file. "
							+ "("+CollectionSpecFilename+")", ioe);
		}
	}

	protected void readDocumentBlacklist(String BlacklistSpecFilename)
	{
		//read the document blacklist
		if (BlacklistSpecFilename != null && BlacklistSpecFilename.length() >0)
		{
			try {
				DocIDBlacklist = new HashSet<String>();
				if (Files.exists(BlacklistSpecFilename)) {
					BufferedReader br2 = Files.openFileReader(BlacklistSpecFilename);
					String blackListedDocid = null;
					while ((blackListedDocid = br2.readLine()) != null) {
						blackListedDocid = blackListedDocid.trim();
						if (!blackListedDocid.startsWith("#")
								&& !blackListedDocid.equals(""))
							DocIDBlacklist.add(blackListedDocid);
					}
					br2.close();
				}
			} catch (IOException ioe) {
				logger.error("Input/Output exception while reading the document black list.", ioe);
			}
		}
	}

	/** Indicates whether the end of the collection has been reached.*/
	protected boolean endOfCollection = false;
	/** Specific constructor: reads the files listed in CollectionSpecFilename,
	 *  the Blacklist of Document IDs in BlacklistSpecFilename, and stores document
	 *  offsets and lengths in the document pointers file docPointersFilename. The collection
	 *  will be parsed according to the TagSet specified by TagSet string
	 *  @param CollectionSpecFilename The collections specification filename. The file contains
	 *  a list of filenames to read. Must be specified, fatal error otherwise.
	 *  @param TagSet the TagSet constructor string to use to obtain the tags to parse for.
	 *  @param BlacklistSpecFilename A filename to a file containing a list of document identifiers
	 *  thay have NOT to be processed. Not loaded if null or length 0
	 *  @param docPointersFilename Where to store document offsets and lengths to. Not used if null.
	*/
	public TRECCollection(String CollectionSpecFilename, String TagSet, String BlacklistSpecFilename,
		 String docPointersFilename) {
		setTags(TagSet);
		readCollectionSpec(CollectionSpecFilename);
		readDocumentBlacklist(BlacklistSpecFilename);
		//check if a docpointers file was specified
		if (docPointersFilename != null && docPointersFilename.length() >0)
		{
			try {
				docPointersEnabled = true;
				//if the pointers file exists, open it as a random access file
				//otherwise open it as an output stream
				if (Files.exists(docPointersFilename)){
					docPointers = Files.openFileRandom(docPointersFilename);
				} else {
					readDocumentBlacklist(BlacklistSpecFilename);
					docPointersStream = new DataOutputStream(
						Files.writeFileStream(docPointersFilename));
				}
			} catch (IOException ioe) {
				logger.warn("TRECCollection couldn't open the docpointer file", ioe);
				docPointersEnabled = false;
			}
		}
		else
		{
			docPointersEnabled = false;
		}
		
		//open the first file
		try {
			openNextFile();
		} catch (IOException ioe) {
			logger.error("IOException opening first file of collection - is the collection.spec correct?", ioe);
		}
	}
	/**
	 * A default constructor that reads the collection specification
	 * file, as configured by the property <tt>collection.spec</tt>,
	 * reads a list of blacklisted document numbers, specified by the
	 * property <tt>trec.blacklist.docids</tt> and opens the
	 * first collection file to process. The default constructor also
	 * uses the property <tt>trec.collection.pointers</tt>, in order to
	 * setup the name of the file in which pointers to the text of the
	 * documents are saved. TagSet TagSet.TREC_DOC_TAGS is used to tokenize
	 * the collection.
	 */
	public TRECCollection()
	{
		this(
			ApplicationSetup.COLLECTION_SPEC, 
			TagSet.TREC_DOC_TAGS, 
			ApplicationSetup.makeAbsolute(
				ApplicationSetup.getProperty("trec.blacklist.docids", ""), 
					ApplicationSetup.TERRIER_ETC), 
			ApplicationSetup.makeAbsolute(
				ApplicationSetup.getProperty("trec.collection.pointers", "docpointers.col"), 
					ApplicationSetup.TERRIER_INDEX_PATH)
			);
	}
	/**
	 * A constructor that reads only the document in the specificed
	 * InputStream. Also reads a list of blacklisted document numbers, specified by the
	 * property <tt>trec.blacklist.docids</tt> and opens the
	 * first collection file to process. */
	public TRECCollection(InputStream input)
	{
 
			setTags(TagSet.TREC_DOC_TAGS); 
			readDocumentBlacklist(ApplicationSetup.makeAbsolute(
				ApplicationSetup.getProperty("trec.blacklist.docids", ""), 
					ApplicationSetup.TERRIER_ETC));
		
		br = input;
		fileOffset = 0;
		documentsInThisFile = 0;
	}
	
	public boolean hasNext() {
		return ! endOfCollection();
	}
	
	public Document next()
	{
		nextDocument();
		return getDocument();
	}
	
	/**
	 * This is unsupported by this Collection implementation, and
	 * any calls will throw UnsupportedOperationException
	 * @throws UnsupportedOperationException on all invocations */
	public void remove()
	{
		throw new UnsupportedOperationException("Iterator.remove() not supported");
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

		DocProperties = new HashMap<String,String>(15);

		/* contains the documentID as it is built up. put into ThisDocID when
		 * a document is fully found */
		StringBuilder DocumentIDContents = null;
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
		final boolean tags_CaseSensitive = this.tags_CaseSensitive;
		boolean bScanning = true;
scanning:
		while(bScanning)
		{
			try {
				State = 0;
				DocumentIDContents = new StringBuilder();
				//looking for doc tag
				while (State < start_docTagLength) {
					if ((c = br.read()) == -1) {
						//print a warning if no documents found in that file!
						if (documentsInThisFile == 0)
						{
							logger.warn(this.getClass().getSimpleName() + " found no documents in " + currentFilename + ". "
								+"Perhaps trec.collection.class is wrongly set, or TrecDocTags are incorrect");
						}
						
						if (openNextFile()) {
							continue scanning;//continue;
						} else {
							endOfCollection = true;
							if (docPointersEnabled && docPointersStream != null)
								docPointersStream.close();
							return false;
						}
					}
					if ((tags_CaseSensitive 
						? (char)c 
						: Character.toUpperCase((char)c)) == start_docTag[State]) {
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
							logger.warn("Forced a skip (1: looking for open DOCNO) - is the collection corrupt?");
							continue scanning;
						} else {
							endOfCollection = true;
							if (docPointersEnabled && docPointersStream != null)
								docPointersStream.close();
							return false;
						}
					}
					if ((tags_CaseSensitive
                        ? (char)c
                        : Character.toUpperCase((char)c)) == start_docnoTag[State])
						State++;
					else
						State = 0;
					fileOffset++;
				}
				//looking for end of docno
				State = 0;
				while (State < end_docnoTagLength) {
					if ((c = br.read()) == -1) {
						if (openNextFile()) {
							logger.warn("Forced a skip (2: looking for end DOCNO) - is the collection corrupt?");
							continue scanning;
						} else {
							endOfCollection = true;
							if (docPointersEnabled && docPointersStream != null)
								docPointersStream.close();
							return false;
						}
					}
					if ((tags_CaseSensitive
                        ? (char)c
                        : Character.toUpperCase((char)c)) == end_docnoTag[State])
						State++;
					else {
						State = 0;
						DocumentIDContents.append((char)c);
					}
					fileOffset++;
				}
				//got the document, phew!
				ThisDocID = DocumentIDContents.toString();
				State = 0;
				documentsInThisFile++;
				
				/* we check the document blacklist, and if docid matches
				 * move on to the next document */
				if (DocIDBlacklist.contains(ThisDocID)) {
					continue scanning;
				}
				DocProperties.put("docno", ThisDocID);
			} catch (IOException ioe) {
				logger.warn("Error Reading "
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

	/**
	 * Returns the current document to process.
	 * @return Document the object of the current document to process.
	 */
	public Document getDocument() {
		try{

			return trecDocument = new TRECDocument(
				new BufferedReader(new InputStreamReader(
					tags_CaseSensitive
                        ? new LookAheadStream(br, end_docTag.getBytes(desiredEncoding))
                        : new LookAheadStreamCaseInsensitive(br, end_docTag),
					desiredEncoding)), 
				DocProperties);

		} catch (java.io.UnsupportedEncodingException uee) {

			logger.warn("Desired encoding ("+desiredEncoding+") unsupported. Resorting to platform default.", uee);

			return trecDocument = new TRECDocument(
				new BufferedReader(new InputStreamReader(
					tags_CaseSensitive
                        ? new LookAheadStream(br, end_docTag.getBytes())
                        : new LookAheadStreamCaseInsensitive(br, end_docTag))),
				DocProperties);
		}
	}
	
	/** A TREC-specific getDocument method, that allows the tags to be specified for
	 * 	each document. 
	 *	@return Document the object of the current document to process.
	 */
	public Document getDocument(TagSet _tags, TagSet _exact, TagSet _fields) {
		try{

			return trecDocument = new TRECDocument(
				new BufferedReader(new InputStreamReader(
					tags_CaseSensitive 
						? new LookAheadStream(br, end_docTag.getBytes()) 
						: new LookAheadStreamCaseInsensitive(br, end_docTag), 
					desiredEncoding)), 
				DocProperties, _tags,_exact, _fields);

		} catch (java.io.UnsupportedEncodingException uee) {

            logger.warn("Desired encoding ("+desiredEncoding+") unsupported. Resorting to platform default.", uee);

			return trecDocument = new TRECDocument(
				new BufferedReader(new InputStreamReader( 
					tags_CaseSensitive
                        ? new LookAheadStream(br, end_docTag.getBytes())
                        : new LookAheadStreamCaseInsensitive(br, end_docTag))),
				DocProperties, _tags,_exact, _fields);

		}
	}

	/**
	 * Returns the document number of the current document.
	 * @return String the document number of the current document.
	 */
	public String getDocid() {
		return ThisDocID;
	}

	/**
	 * Indicates whether the end of the collection has been reached. 
	 * @return boolean true if there are no more documents to process in the
	 *		 collection, otherwise it returns false. 
	 */
	public boolean endOfCollection() {
		return endOfCollection;
	}

	/**
	 * Resets the collection object back to the beginning
	 * of the collection.
	 */
	public void reset() {
		FileNumber = -1; 
		endOfCollection = false;
		ThisDocID = "";
		try {
			openNextFile();
		} catch (IOException ioe) {
			logger.warn("IOException while resetting collection - ie re-opening first file", ioe);
		}
	}

	/**
	 * Opens the next document from the collection specification.
	 * @return boolean true if the file was opened successufully. If there
	 *		 are no more files to open, it returns false.
	 * @throws IOException if there is an exception while opening the 
	 *		 collection files.
	 */
	protected boolean openNextFile() throws IOException {
		//try to close the currently open file
		if (br!=null && FilesToProcess.size() > 0)
			try{
				br.close();
			}catch (IOException ioe) {/* Ignore, it's not an error */ }
		//keep trying files
		boolean tryFile = true;
		//return value for this fn
		boolean rtr = false;
		while(tryFile)
		{
			if (FileNumber < FilesToProcess.size() -1 ) {
				SkipFile = true;
				FileNumber++;
				String filename = (String) FilesToProcess.get(FileNumber);
				//check the filename is sane
				if (! Files.exists(filename))
				{
					logger.warn("Could not open "+filename+" : File Not Found");
				}
				else if (! Files.canRead(filename))
				{
					logger.warn("Could not open "+filename+" : Cannot read");
				}
				else
				{//filename seems ok, open it
					br = Files.openFileStream(filename); //throws an IOException, throw upwards
					logger.info("Processing "+filename);
					currentFilename = filename;
					//no need to loop again
					tryFile = false;
					//return success
					rtr = true;
					//accurately record file offset
					fileOffset = 0;
					documentsInThisFile = 0;
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
	
	/**
	 * Returns the text of a document with the given identifier.
	 * @param docid the internal identifier of a document.
	 * @return String the text of the document as a string.
	 */
	public String getDocumentString(int docid) {
		if(! docPointersEnabled)
			return "";
		final int entryLength = 20; //2 longs and 1 int
		try {
			docPointers.seek((long)docid * (long)entryLength);
			final long startOffset = docPointers.readLong();
			final long endOffset = docPointers.readLong();
			final int fileNumber = docPointers.readInt();
			final String fileToAccess = (String) FilesToProcess.get(fileNumber);
			final DataInputStream dis = new DataInputStream(Files.openFileStream(fileToAccess));
			final byte[] docBuffer = new byte[(int) (endOffset - startOffset+1)];
			dis.skip/*Bytes*/(startOffset); //DataInput has no skipBytes(long)
			dis.readFully(docBuffer);
			dis.close();
			return new String(docBuffer);
		} catch (IOException ioe) {
			logger.warn("Input/Output exception while trying to access the document "+ docid + ".", ioe);
			return null;
		}
	}
	
	/**
	 * Closes the files and streams used by the collection object.
	 */
	public void close() {
		try {
			if (br!=null && FilesToProcess.size() == 0) 
				br.close();
			if (docPointers!=null) 
				docPointers.close();
			if (docPointersStream != null)
				docPointersStream.close();
		} catch(IOException ioe) {
			logger.warn("IOException closing collection. Ignore probably", ioe);
		}
	}
}

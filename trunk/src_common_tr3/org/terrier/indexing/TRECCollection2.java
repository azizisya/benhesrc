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
 * The Original Code is TRECCollection.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original author) 
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
 */
package org.terrier.indexing;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;

import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.Files;
import org.terrier.utility.LookAheadReader;
import org.terrier.utility.TagSet;
/**
 * Models a TREC test collection by implementing the interfaces
 * Collection and DocumentExtractor. It provides sequential access
 * to the documents in the collection and also it can return the text
 * of a document as a String.
 * @author Craig Macdonald &amp; Vassilis Plachouras
 * @version $Revision: 1.1 $
 */
public class TRECCollection2 implements Collection, DocumentExtractor {

	private static Logger logger = Logger.getRootLogger();
	
	protected Map<String,String> DocProperties = null;
	
	protected String currentFilename = null;
	
	protected int currentOffset=0;
	
	protected int[] termOffSets = null;

	/**
	 * Counts the documents that are found in the collection, ignoring those
	 * documents that appear in the black list
	 */
	protected int documentCounter = 0;
	/**
	 * The random access file that contains the pointer to the documents in the
	 * collection.
	 */
	protected RandomAccessFile docPointers = null;
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
	protected int FileNumber = 0;
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
	/**
	 * protected method for initialising the
	 * opening and closing document and document number
	 * tags.
	 */
	protected void setTags(String TagSet)
	{
		TagSet tagSet = new TagSet(TagSet);
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
		//System.out.println("start_docTag: "+String.copyValueOf(start_docTag));
		//System.out.println("start_docnoTag: "+String.copyValueOf(start_docnoTag));
		//System.out.println("end_docTag: "+end_docTag);
		//System.out.println("end_docnoTag: "+String.copyValueOf(end_docnoTag));
		//System.out.println("");
		//System.out.println("");
		//System.out.println("");
		//System.out.println("");
	}
	/** Indicates whether the end of the collection has been reached.*/
	protected boolean endOfCollection = false;
	/** Specific constructor: reads the files listed in CollectionSpecFilename,
	 *  the Blacklist of Document IDs in BlacklistSpecFilename, and stores document
	 *  offsets and lengths in the document pointers file docPointersFilename. The collection
	 *  will be parsed according to the TagSet specified by TagSet string
	 *  @param CollectionSpecFilename The collections specification filename. The file contains
	 *  a list of filenames to read
	 *  @param TagSet the TagSet constructor string to use to obtain the tags to parse for.
	 *  @param BlacklistSpecFilename A filename to a file containing a list of document identifiers
	 *  thay have NOT to be processed. Not loaded if null
	 *  @param docPointersFilename Where to store document offsets and lengths to. Not used if null.
	*/
	public TRECCollection2(String CollectionSpecFilename, String TagSet, String BlacklistSpecFilename,
		 String docPointersFilename) {
		setTags(TagSet);
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
		} catch (IOException ioe) {
			logger.error("Input output exception while loading the collection.spec file. ("+CollectionSpecFilename+")Stack trace follows.", ioe);
		}
		//reads the trec_blacklist_docid file
		if (BlacklistSpecFilename != null && BlacklistSpecFilename.length() >0)
		{
			try {
				DocIDBlacklist = new HashSet<String>();
				if ((new File(BlacklistSpecFilename)).exists()) {
					BufferedReader br = new BufferedReader(new InputStreamReader(
						new FileInputStream(BlacklistSpecFilename)));
					String blackListedDocid = null;
					while ((blackListedDocid = br.readLine()) != null) {
						if (!blackListedDocid.startsWith("#")
								&& !blackListedDocid.equals(""))
							DocIDBlacklist.add(blackListedDocid);
					}
					br.close();					
				}
			} catch (IOException ioe) {
				logger.error("Input/Output exception while reading the document black list. Stack trace follows", ioe);
			}
		}
		//check if a docpointers file was specified
		if (docPointersFilename != null && docPointersFilename.length() >0)
		{
			try {
				docPointersEnabled = true;
				File docPointersFile = new File(docPointersFilename);
				//if the pointers file exists, open it as a random access file
				//otherwise open it as an output stream
				if (docPointersFile.exists()) {
					docPointers = new RandomAccessFile(docPointersFilename, "r");
				} else {
					docPointersStream = new DataOutputStream(
						new BufferedOutputStream(new FileOutputStream(
							docPointersFile)));
				}
			} catch (FileNotFoundException fnfe) {
				fnfe.printStackTrace();
				docPointersEnabled = false;
			}
		}
		else
		{
			docPointersEnabled = false;
		}
		
		//opens the first file
		
		try {
			openNextFile();
		} catch (IOException ioe) {
			ioe.printStackTrace();
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
	public TRECCollection2()
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
	 * Moves to the next document to process from the collection.
	 * @return boolean true if there are more documents to process in the 
	 *         collection, otherwise it returns false.
	 */
	public boolean nextDocument() {
		//move the stream to the start of the next document
		//try next file if no DOC tag found. (and set endOfCOllection if
		//no files left)

		DocProperties = new HashMap<String,String>(15);

		/* contains the documentID as it is built up. put into ThisDocID when
		 * a document is fully found */
		StringBuffer DocumentIDContents = null;
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
scanning:
		while(bScanning)
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
				//got the document, phew!
				ThisDocID = DocumentIDContents.toString();
				State = 0;
				/* we check the document blacklist, and if docid matches
				 * move on to the next document */
				if (DocIDBlacklist.contains(ThisDocID)) {
					//ThisDocID = new String();
					//return nextDocument();
					continue scanning;
				}
			} catch (IOException ioe) {
				logger.error("Error Reading " + FilesToProcess.get(FileNumber) + ", skipping rest of file: " + ioe);
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
		return new TRECDocument(new LookAheadReader(new InputStreamReader(br), end_docTag), DocProperties);
	}
	
	/** A TREC-specific getDocument method, that allows the tags to be specified for
	 * 	each document. 
	 *	@return Document the object of the current document to process.
	 */
	public Document getDocument(TagSet _tags, TagSet _exact, TagSet _fields) {
		return new TRECDocument(new LookAheadReader(new InputStreamReader(br), end_docTag), DocProperties, _tags,_exact, _fields);
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
	 *         collection, otherwise it returns false. 
	 */
	public boolean endOfCollection() {
		if (endOfCollection)
			close();
		return endOfCollection;
	}

	/**
	 * Resets the collection object back to the beginning
	 * of the collection.
	 */
	public void reset() {
		FileNumber = 0; 
		endOfCollection = false;
		ThisDocID = new String();
		try {
			openNextFile();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	/**
	 * Opens the next document from the collection specification.
	 * @return boolean true if the file was opened successufully. If there
	 *         are no more files to open, it returns false.
	 * @throws IOException if there is an exception while opening the 
	 *         collection files.
	 */
	protected boolean openNextFile() throws IOException {
		//try to close the currently open file
		if (br!=null)
		try{
			br.close();
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
					//br = openFile(filename); //throws an IOException, throw upwards
					br = Files.openFileStream(filename);
					currentFilename = filename;
					currentOffset = 0;

					logger.info("Processing "+filename);
					//no need to loop again
					tryFile = false;
					//return success
					rtr = true;
					//accurately record file offset
					fileOffset = 0;
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
			docPointers.seek(docid * entryLength);
			long startOffset = docPointers.readLong();
			long endOffset = docPointers.readLong();
			int fileNumber = docPointers.readInt();
			String fileToAccess = (String) FilesToProcess.get(fileNumber);
			BufferedInputStream bis = null;
			if (fileToAccess.toLowerCase().endsWith("gz"))
				bis = new BufferedInputStream(new GZIPInputStream(
						new FileInputStream(fileToAccess)));
			else
				bis = new BufferedInputStream(new FileInputStream(fileToAccess));
			byte[] docBuffer = new byte[(int) (endOffset - startOffset)];
			bis.skip(startOffset);
			bis.read(docBuffer);
			bis.close();
			return new String(docBuffer);
		} catch (IOException ioe) {
			logger.error("Input/Output exception while trying to access the document " + docid + ": ", ioe);
			return null;
		}
	}
	
	/**
	 * Closes the files and streams used by the collection object.
	 */
	public void close() {
		try {
			if (br!=null) 
				br.close();
			if (docPointers!=null) 
				docPointers.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
}

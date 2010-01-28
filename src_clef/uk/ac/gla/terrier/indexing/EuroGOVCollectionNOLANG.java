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
 * The Original Code is EuroGOVCollection.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original author) 
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.indexing;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.LookAheadReader;
import uk.ac.gla.terrier.utility.TagSet;
import java.util.Map;
/**
 * Models a TREC test collection by implementing the interfaces
 * Collection and DocumentExtractor. It provides sequential access
 * to the documents in the collection and also it can return the text
 * of a document as a String.
 * @author Craig Macdonald &amp; Vassilis Plachouras
 */
public class EuroGOVCollectionNOLANG implements Collection, DocumentExtractor {
	/**
	 * Counts the documents that are found in the collection, ignoring those
	 * documents that appear in the black list
	 */
	private int documentCounter = 0;
	/**
	 * The random access file that contains the pointer to the documents in the
	 * collection.
	 */
	private RandomAccessFile docPointers = null;
	/**
	 * The stream that is used for writing the pointers to the documents in the
	 * collection
	 */
	private DataOutputStream docPointersStream = null;
	/**
	 * Are we using the document pointers file
	 */
	private boolean docPointersEnabled;
	/**
	 * The offset in the current file being read.
	 */
	private long fileOffset = 0;
	/**
	 * the current document processed
	 */
	private EuroGOVDocumentNOLANG trecDocument = null;
	/**
	 * The starting offset of the current document
	 */
	private long startDocumentOffset;
	/**
	 * The ending offset of the current document
	 */
	private long endDocumentOffset;
	/**
	 * A black list of document to ignore.
	 */
	private HashSet DocIDBlacklist = new HashSet();
	/**
	 * The list of files to process.
	 */
	private ArrayList FilesToProcess = new ArrayList();
	/** The index in the FilesToProcess of the currently processed file.*/
	private int FileNumber = 0;
	/** The properties of the current document, from the doc tag attributes. */
	private Hashtable DocProperties = new Hashtable();
	/** A reader used for parsing the collection files.*/
	private Reader br;
	/** The inputstream used to read the collection files. */
	private InputStream is;

	/** A boolean which is true when a new file is open.*/
	private boolean SkipFile = false;
	
	
	/** The opening document tag.*/
	private char[] start_docTag;
	/** The length of the opening document tag.*/
	private int start_docTagLength;
	/** The closing document tag.*/
	private char[] end_docTag;
	/** The length of the closing document tag.*/	
	private int end_docTagLength;
	
	/** The opening document content tag.*/
	private char[] start_ContentTag;
	/** The length of the opening document number tag.*/
	private int start_ContentTagLength;
	
	/** The closing document content tag.*/
	private String end_ContentTag;
	
	private Hashtable DocumentLanguages = new Hashtable();
	
	private String ThisDocID;
	
	/**
	 * private method for initialising the
	 * opening and closing document and document number
	 * tags.
	 */
	private void setTags(String TagSet)
	{
		//TagSet tagSet = new TagSet(TagSet);
		String tmpDocTag = "<EuroGOV:doc".toUpperCase();//"<" + tagSet.getDocTag();
		String tmpendDocTag = "<EuroGOV:content>".toUpperCase();
		
		String tmpStartContentTag = "<![CDATA[";
		String tmpEndContentTag = "]]>"; //"</" + tagSet.getDocTag() ;
		//String tmpDocnoTag = //"<" + tagSet.getIdTag() + ">";
		//String tmpEndDocnoTag = //"</" + tagSet.getIdTag() + ">";
		start_docTag = tmpDocTag.toCharArray();
		start_docTagLength = start_docTag.length;
		
		end_docTag = tmpendDocTag.toCharArray();
		end_docTagLength = end_docTag.length;
		
		start_ContentTag = tmpStartContentTag.toCharArray();
		start_ContentTagLength = start_ContentTag.length;
		
		end_ContentTag = tmpEndContentTag;
		
		//start_docnoTag = tmpDocnoTag.toCharArray();
		//start_docnoTagLength = start_docnoTag.length;
		
		//end_docnoTag = tmpEndDocnoTag.toCharArray();
		//end_docnoTagLength = end_docnoTag.length;
	}
	/** Indicates whether the end of the collection has been reached.*/
	private boolean endOfCollection = false;
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
	public EuroGOVCollectionNOLANG(String CollectionSpecFilename, String TagSet, String BlacklistSpecFilename,
		 String docPointersFilename) {
		setTags(TagSet);
		BufferedReader br = null;
		//reads the collection specification file
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(
					CollectionSpecFilename)));
			String filename = null;
			FilesToProcess = new ArrayList();
			while ((filename = br.readLine()) != null) {
				if (!filename.startsWith("#") && !filename.equals(""))
					FilesToProcess.add(filename);
			}
			br.close();
			System.err.println("read collection specification");
		} catch (IOException ioe) {
			System.err
					.println("Input output exception while loading the collection.spec file. "
							+ "("+CollectionSpecFilename+")Stack trace follows.");
			ioe.printStackTrace();
			System.exit(1);
		}
		//reads the trec_blacklist_docid file
		if (BlacklistSpecFilename != null && BlacklistSpecFilename.length() >0)
		{
			try {
				DocIDBlacklist = new HashSet();
				if ((new File(BlacklistSpecFilename)).exists()) {
					br = new BufferedReader(new InputStreamReader(
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
				System.err.println("Input/Output exception while reading the document black list."
							+ "Stack trace follows");
				ioe.printStackTrace();
				System.exit(1);
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
	public EuroGOVCollectionNOLANG()
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
	 *		 collection, otherwise it returns false.
	 */
	public boolean nextDocument() {
		//move the stream to the start of the next document
		//try next file if no DOC tag found. (and set endOfCOllection if
		//no files left)
		
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
		//System.out.println("looking for opendoc tag");
		
		try {
			//looking for doc tag opening
			while (State < start_docTagLength) {
				if ((c = br.read()) == -1) {
					if (openNextFile()) {
						continue;
					} else {
						endOfCollection = true;
						if (docPointersEnabled && docPointersStream != null)
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
			
			StringBuffer sbufAttributes = new StringBuffer();
			
			//looking for doc tag closing
			State = 0;
			while (State < end_docTagLength) {
				if ((c = br.read()) == -1) {
					if (openNextFile()) {
						continue;
					} else {
						endOfCollection = true;
						if (docPointersEnabled && docPointersStream != null)
							docPointersStream.close();
						return false;
					}
				}
				if (Character.toUpperCase((char)c) == end_docTag[State])
					State++;
				else
				{
					sbufAttributes.append((char)c);
					State = 0;
				}
				fileOffset++;
			}
			
			//got all the attributes for this document
			_setProperties(sbufAttributes.toString().replaceAll("\"[^\"]*$", ""));
			//looking for start of content
			State = 0;
			while (State < start_ContentTagLength) {
				if ((c = br.read()) == -1) {
					if (openNextFile()) {
						continue;
					} else {
						endOfCollection = true;
						if (docPointersEnabled && docPointersStream != null)
							docPointersStream.close();
						return false;
					}
				}
				if (Character.toUpperCase((char)c) == start_ContentTag[State])
					State++;
				else {
					State = 0;
				}
				fileOffset++;
			}
			//got the document, phew!
			
			State = 0;
			/* we check the document blacklist, and if docid matches
			 * move on to the next document */
			if (DocIDBlacklist.contains(ThisDocID)) {
				//ThisDocID = new String();
				return nextDocument();
			}
		} catch (IOException ioe) {
			System.err.println("Error Reading "
					+ FilesToProcess.get(FileNumber) + " : " + ioe
					+ ", skipping rest of file");
			ioe.printStackTrace();
			FileNumber++;
			SkipFile = true;
		}
		return true;
	}
	
	
	private Pattern ReplacePattern0 = Pattern.compile("\\n|\\r");
	private Pattern ReplacePattern1 = Pattern.compile("\\W");

	public Map getDocumentProperties()
	{
		return (Map)DocProperties;
	}
	
	private void _setProperties(String attributes)
	{
		boolean displayID = false;
		DocProperties.clear();
		String[] parts = ReplacePattern0.matcher(attributes).replaceAll("").split("\"");
		for(int i=0;i<parts.length; i+=2)
		{
			try{
				String name = ReplacePattern1.matcher(parts[i]).replaceAll("");
				DocProperties.put(name.toLowerCase(), parts[i+1]);
			}catch(ArrayIndexOutOfBoundsException e) {
				System.out.println("Failed to find a value for "+parts[i]);
				displayID = true;
			}
			//System.out.println(name.toLowerCase() + "=>"+ parts[i+1]);
			
		}
		String oldID = ThisDocID;
		ThisDocID = (String)DocProperties.get("id");
		if (displayID)
			System.out.println("DOCID: "+ThisDocID);
		if (ThisDocID == null)
		{
			System.out.println("Failed to find a docid. Previous doc was : "+oldID);
			System.out.println(attributes);
		}
	}
	
	/**
	 * Returns the current document to process.
	 * @return Document the object of the current document to process.
	 */
	public Document getDocument() {
		trecDocument = new EuroGOVDocumentNOLANG(new LookAheadReader(
			/*new InputStreamReader(is)*/br, end_ContentTag));
		return trecDocument;
	}
	
	/** A TREC-specific getDocument method, that allows the tags to be specified for
	 * 	each document. 
	 *	@return Document the object of the current document to process.
	 */
	public Document getDocument(TagSet _tags, TagSet _exact, TagSet _fields) {
		return new EuroGOVDocumentNOLANG(
			new LookAheadReader(br /*new InputStreamReader(is)*/, end_ContentTag), 
			_tags,_exact, _fields);
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
		FileNumber = 0; 
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
	 *		 are no more files to open, it returns false.
	 * @throws IOException if there is an exception while opening the 
	 *		 collection files.
	 */
	private boolean openNextFile() throws IOException {
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
					System.err.println("WARNING: Could not open "+filename+" : File Not Found");
				}
				else if (! f.canRead())
				{
					System.err.println("WARNING: Could not open "+filename+" : Cannot read");
				}
				else
				{//filename seems ok, open it
					if (filename.toLowerCase().endsWith("gz")) {
						is = new BufferedInputStream(new GZIPInputStream(new FileInputStream(f)));
						br = new InputStreamReader(is);
					} else {
						is = new BufferedInputStream(new FileInputStream(f));
						br = new InputStreamReader(is);
					}
					System.err.println("Processing "+filename);
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
			System.err
					.println("Input/Output exception while trying to access the document "
							+ docid + ".");
			System.err.println(ioe);
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

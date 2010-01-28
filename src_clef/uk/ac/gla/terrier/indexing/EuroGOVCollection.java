
/* Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original author) 
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.indexing;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.io.Reader;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.zip.GZIPInputStream;
//import uk.ac.gla.terrier.utility.EncodingInputStreamReader;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.LookAheadReader;
import uk.ac.gla.terrier.utility.TagSet;
import java.util.Map;
/**
 * Models EuroGOV test collection by implementing the interfaces
 * Collection and DocumentExtractor. It provides sequential access
 * to the documents in the collection and also it can return the text
 * of a document as a String.
 * @author Craig Macdonald &amp; Vassilis Plachouras
 */
public class EuroGOVCollection implements Collection, DocumentExtractor {
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
	protected EuroGOVDocument trecDocument = null;
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
	protected HashSet DocIDBlacklist = new HashSet();
	/**
	 * The list of files to process.
	 */
	protected ArrayList FilesToProcess = new ArrayList();
	/** The index in the FilesToProcess of the currently processed file.*/
	protected int FileNumber = 0;
	/** The properties of the current document, from the doc tag attributes. */
	protected Hashtable DocProperties = new Hashtable();
	/** A buffered reader used for reading data.*/
	//protected Reader br;
	protected java.io.InputStream inStream;
	
	/** A input stream reader for changing the encoding */
	//protected EncodingInputStreamReader encInputStreamReader;
	/** A boolean which is true when a new file is open.*/
	protected boolean SkipFile = false;
	
	
	/** The opening document tag.*/
	protected char[] start_docTag;
	/** The length of the opening document tag.*/
	protected int start_docTagLength;
	/** The closing document tag.*/
	protected char[] end_docTag;
	/** The length of the closing document tag.*/	
	protected int end_docTagLength;
	
	/** The opening document content tag.*/
	protected char[] start_ContentTag;
	/** The length of the opening document number tag.*/
	protected int start_ContentTagLength;
	
	/** The closing document content tag.*/
	protected char[] end_ContentTag;
	protected int end_ContentTagLength;
		
	protected String ThisDocID;
	
	
	
	/**
	 * protected method for initialising the
	 * opening and closing document and document number
	 * tags.
	 */
	protected void setTags(String TagSet)
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
		
		end_ContentTag = tmpEndContentTag.toCharArray();
		end_ContentTagLength = end_ContentTag.length;
		//start_docnoTag = tmpDocnoTag.toCharArray();
		//start_docnoTagLength = start_docnoTag.length;
		
		//end_docnoTag = tmpEndDocnoTag.toCharArray();
		//end_docnoTagLength = end_docnoTag.length;
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
	public EuroGOVCollection(String CollectionSpecFilename, String TagSet, String BlacklistSpecFilename,
		 String docPointersFilename) {
		setTags(TagSet);
		//reads the collection specification file
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(
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
	public EuroGOVCollection()
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



	protected static String LanguagesDir = ApplicationSetup.getProperty("clef.languages.prefix", "");
	protected static String LanguagesSuffix = ApplicationSetup.getProperty("clef.languages.suffix", "");
	protected String currentLanguagesMapFile = null;
	protected Hashtable DocumentLanguages = new Hashtable();
	protected void loadDocumentLanguages(String docid)
	{
		String filename = LanguagesDir + docid.substring(1,3) + LanguagesSuffix;
		if (filename.equals(currentLanguagesMapFile))
			return;

		System.err.println("LOADING: "+filename);
		DocumentLanguages.clear();
		try{			
			BufferedReader br;
			if (filename.toLowerCase().endsWith("gz")) {
				br = new BufferedReader(new InputStreamReader(
					new GZIPInputStream(new FileInputStream(filename))));
			} else {
				br = new BufferedReader(new InputStreamReader(
					new FileInputStream(filename)));
			}
			String line = null;
			while ((line = br.readLine()) != null) {
				String[] parts = line.split(" :: ");
				String[] languages = parts[1].split(" or ");
				for(int j=0;j<languages.length;j++)
					languages[j] = languages[j].trim();
				DocumentLanguages.put(parts[0], languages);			
			}
			br.close();
			currentLanguagesMapFile = filename;
		}catch (Exception e){e.printStackTrace();}
	}

	int read() throws IOException
	{
		int c = inStream.read();
		//System.out.print((char)c);
		return c;
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
			//System.err.println("looking for doc tag opening");
			while (State < start_docTagLength) {
				if ((c = read()) == -1) {
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
			//System.err.println("looking for doc tag closing");
			State = 0;
			while (State < end_docTagLength) {
				if ((c = read()) == -1) {
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
			//System.err.println("looking for start of content");
			while (State < start_ContentTagLength) {
				if ((c = read()) == -1) {
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
				System.err.println(ThisDocID + " in the blacklist, ignoring");
				//ThisDocID = new String();
				return nextDocument();
			}
			if (ThisDocID.equals("Eeu-001-976788") || ThisDocID.equals("Eeu-001-984039")) {
				System.out.println("need to print something now...");
			}
		} catch (IOException ioe) {
			System.err.println("Error Reading "
					+ FilesToProcess.get(FileNumber) + " : " + ioe
					+ ", skipping rest of file");
			ioe.printStackTrace();
			FileNumber++;
			SkipFile = true;
		}
		System.err.println("ThisDocID: " + ThisDocID);
		loadDocumentLanguages(ThisDocID);
		loadDocumentMeta(ThisDocID);
		return true;
	}
	
	
	protected Pattern ReplacePattern0 = Pattern.compile("\\n|\\r");
	protected Pattern ReplacePattern1 = Pattern.compile("\\W");
	protected Pattern matchCharsetPattern = Pattern.compile("charset(?:=|!)(?:\\.?)(\\S+)");

	public Map getDocumentProperties()
	{
		return (Map)DocProperties;
	}
	
	protected void _setProperties(String attributes)
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

		if (DocProperties.containsKey("contenttype"))
		{
			String cntType = (String) DocProperties.get("contenttype");
			Matcher m = matchCharsetPattern.matcher(cntType);
			if (m.find())
			{
				DocProperties.put("charset", m.group(1));//1 is the first set of brackets without ?:
			}
		}

	}

	private static String normaliseEncoding(String encoding)
	{
		if (encoding == null)
			return null;
		encoding = encoding
			.toUpperCase()
			.trim()
			.replaceAll("[^A-Z0-9_\\-]", "");

		if (encoding.length() == 0)
			return null;
	
		if (encoding.startsWith("WINDOWS"))
		{
			if (encoding.indexOf("_") > 0)
			{
				encoding = encoding.replaceFirst("^WINDOWS_","WINDOWS-");
			}
			else if (encoding.indexOf("-") == -1)
			{
				encoding = encoding.replaceFirst("^WINDOWS", "WINDOWS-");
			}
		}
		else if (encoding.startsWith("WIN"))
		{
			encoding = encoding.replaceFirst("^WIN(-|_)?","WINDOWS-");
		}
		return encoding;
	}
	
	protected BufferedReader metaContentTypeBR = null;
	protected static String metaContentTypeFilename = ApplicationSetup.getProperty(
		"clef.metacontenttype.file", "");
	protected uk.ac.gla.terrier.utility.StringComparator sc = new uk.ac.gla.terrier.utility.StringComparator();
	//protected String lastMetaLine = null;
	
	protected static String MetaDir = ApplicationSetup.getProperty("clef.meta.prefix", "");
	protected static String MetaSuffix = ApplicationSetup.getProperty("clef.meta.suffix", "");
	protected String currentMetaMapFile = null;
	protected Hashtable DocumentMeta = new Hashtable();
	
	protected void loadDocumentMeta(String docid)
	{
		if (MetaDir == null || MetaDir.length() == 0)//none specified
			return;
		String filename = MetaDir + docid.substring(1,3) + MetaSuffix;
		if (filename.equals(currentMetaMapFile))
			return;

		System.err.println("LOADING: "+filename);
		DocumentMeta.clear();
		try{			
			BufferedReader br;
			if (filename.toLowerCase().endsWith("gz")) {
				br = new BufferedReader(new InputStreamReader(
					new GZIPInputStream(new FileInputStream(filename))));
			} else {
				br = new BufferedReader(new InputStreamReader(
					new FileInputStream(filename)));
			}
			String line = null;
			while ((line = br.readLine()) != null) {
				/*String[] parts = line.split(" :: ");
				String[] languages = parts[1].split(" or ");
				for(int j=0;j<languages.length;j++)
					languages[j] = languages[j].trim();
				DocumentLanguages.put(parts[0], languages);*/
				int FirstSpace = line.indexOf(' ');
				if (FirstSpace == -1)
					continue;
				String foundDocid = line.substring(0, FirstSpace);
				String value = line.substring(FirstSpace+1);
				Matcher m = matchCharsetPattern.matcher(value);
				if (m.find())
				{
					DocumentMeta.put(foundDocid, m.group(1));//1 is the first set of brackets without ?:
				}
			}
			br.close();
			currentMetaMapFile= filename;
		}catch (Exception e){e.printStackTrace();}
	}
	
	
	private String getMetaEncoding(String docid)
	{
		Object rtr = DocumentMeta.get(docid);
		if (rtr != null)
			return (String) rtr;
		return null;
	}
	
	/*private String getMetaEncoding(String docid)
	{
		if (metaContentTypeBR == null)
		{
			try{
				metaContentTypeBR = new BufferedReader(new FileReader(metaContentTypeFilename));
			}catch (IOException ioe) {
				System.err.println(ioe);
				ioe.printStackTrace();
			}
		}
		
		try{
			
			String foundDocid = null; int cmp; String line; int FirstSpace;
			do{
				line = metaContentTypeBR.readLine();
				//System.err.println("\t\""+line+"\"");
				FirstSpace = line.indexOf(' ');
				if (FirstSpace == -1)
					continue;//FirstSpace = line.length();
				foundDocid = line.substring(0, FirstSpace);
				//if (foundDocid == null)
				//	continue;	
			} while (foundDocid == null ||  (cmp = sc.compare(foundDocid, docid)) <0 );
			if (cmp > 0)
				return null;
			String value = line.substring(FirstSpace+1);
			Matcher m = matchCharsetPattern.matcher(value);
			if (m.find())
			{
				return m.group(1);//1 is the first set of brackets without ?:
			}
		} catch (Exception ioe) {
			System.err.println(ioe);
			ioe.printStackTrace();
			return null;
		}
		return null;
	}*/
	
	/**
	 * Returns the current document to process.
	 * @return Document the object of the current document to process.
	 */
	public Document getDocument() {
		
		
		
		String[] langs = (String[])DocumentLanguages.get(ThisDocID);
		if(langs == null)
			langs = new String[]{"english"};
		else 
			langs = new String[] { langs[0] };

		
		String encoding = getMetaEncoding(ThisDocID);
		System.err.println("Got encoding "+encoding+" from meta");
		if (encoding == null)
		{
			encoding = (String)DocProperties.get("charset");
			System.err.println("Got encoding "+ encoding+" from http headers");
		}

		if (encoding == null)
		{
			System.err.println("Examining language for encoding: " + langs[0]);
			int dashIndex = langs[0].indexOf("-");
			if (dashIndex >= 0) 
				encoding = langs[0].substring(dashIndex+1);
		}

		encoding = normaliseEncoding(encoding);

		byte[] documentContents = readDocument();
//		String doc = new String(documentContents);

//		if (ThisDocID.equals("Ebe-001-281527203"/*"Eeu-001-168769911"*/))
//		{
//			System.err.println("BREAK");
//		}
		InputStreamReader in = null;
		System.err.println("set encoding: " + encoding);
		//try {
			if (encoding == null)
				in = new InputStreamReader(new ByteArrayInputStream(documentContents));
			else
				try{
					in = new InputStreamReader(new ByteArrayInputStream(documentContents), encoding);
				} catch (java.io.UnsupportedEncodingException uee) {
					System.err.println("UEE - resorting do default encoding");
					in = new InputStreamReader(new ByteArrayInputStream(documentContents));
				}
			//encInputStreamReader.setEncoding(encoding);
			System.err.println("\tencoding: " + /*encInputStreamReader*/in.getEncoding());
		/*} catch (IOException ioe) {
			System.err.println(ioe);
			ioe.printStackTrace();
		}*/

		//System.out.println("get encoding: " + encInputStreamReader.getEncoding());
		//if (ThisDocID.equals("Eeu-001-984039"))
		//{
		//	System.err.println("BREAK!");
		//}
		
		trecDocument = new EuroGOVDocument(in/*new LookAheadReader(br, end_ContentTag)*/, new String[]{langs[0]}, DocProperties);
		return trecDocument;
	}
	
	protected byte[] readDocument()
	{
		int c;
		int State = 0;
		
		java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream(); 
		
		//end_ContentTag
		//end_ContentTagLength
		try{
			//System.err.println("looking for start of content");
			while (State < end_ContentTagLength) {
				c = read();
				if (Character.toUpperCase((char)c) == end_ContentTag[State])
					State++;
				else {
					buf.write(c);
					State = 0;
				}
				fileOffset++;
			}
		} catch (IOException ioe) {
			System.err.println("Shock, IOException:" +ioe);
			ioe.printStackTrace();
		}
		return buf.toByteArray();
	}
	
	/** A TREC-specific getDocument method, that allows the tags to be specified for
	 * 	each document. 
	 *	@return Document the object of the current document to process.
	 */
	/*public Document getDocument(TagSet _tags, TagSet _exact, TagSet _fields) {
		String[] langs = (String[])DocumentLanguages.get(ThisDocID);
		if(langs == null)
			langs = new String[]{"english"};
		return new EuroGOVDocument(new LookAheadReader(br, end_ContentTag), _tags,_exact, _fields, langs);
	}*/
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
	protected boolean openNextFile() throws IOException {
		//try to close the currently open file
		if (inStream!=null)
		try{
			inStream.close();
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
						//encInputStreamReader = new EncodingInputStreamReader(new BufferedInputStream(new GZIPInputStream(new FileInputStream(f))));
						//br = encInputStreamReader;
						inStream  = new BufferedInputStream(new GZIPInputStream(new FileInputStream(f)));
					} else {
						//encInputStreamReader = new EncodingInputStreamReader(new BufferedInputStream(new FileInputStream(f)));
						//br = encInputStreamReader;
						inStream  = new BufferedInputStream(new FileInputStream(f));
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
			if (inStream !=null) 
				inStream.close();
			if (docPointers!=null) 
				docPointers.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
}

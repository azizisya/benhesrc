package uk.ac.gla.terrier.indexing;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;

/** Initial implementation of a class that generates a Collection with Documents from a 
  * series of XML files.<p>
  * <b>Properties:</b><ul>
  * <li><tt>lowercase</tt> - lower case all terms obtained. Highly recommended.</li>
  * <li><tt>indexing.simplexmlcollection.reformxml</tt> - will try to reform broken &amp;AMP; entities.</li>
  */
public class SaxXMLCollection implements Collection
{
	protected static Logger logger = Logger.getRootLogger();
	public static final String ELEMENT_ATTR_SEPARATOR = ".";
	public static final int tokenMaximumLength = ApplicationSetup.STRING_BYTE_LENGTH;

	 /** Change to all terms to lowercase? */
    protected final static boolean lowercase = (new Boolean(
		ApplicationSetup.getProperty("lowercase", "true"))).booleanValue();

	/** Reform invalid XML by copying to temporary file. NB This may be dangerous */	
	protected static final boolean bReformXML = new Boolean(
			ApplicationSetup.getProperty("indexing.simplexmlcollection.reformxml", "false")).booleanValue();
	
	class XMLDocument implements Document
	{
		private ArrayList<String> terms = new ArrayList<String>();
		private String currentHolder = null;
		private int termNumber = 0;
		protected String ThisDocId = null;
		public XMLDocument(Node root)
		{
			this.doRecursive(root);
			/* by the time we're here, the entire document has been parsed
			 * all terms are in the terms list */
		}

		/** Process all child nodes of Node p, recursively. 
		  * @param p the node to process */
		private void doRecursive(Node p) {
			if (p == null) {
				return;
			}
			NodeList nodes = p.getChildNodes();
			for (int i = 0; i < nodes.getLength(); i++) {
				Node n = nodes.item(i);
				if (n == null) {
					continue;
				}
				this.doNode(n);
			}
		}

		/** Processes a node: eg Text, element, attribute. */
		private void doNode(Node n)
		{
			String parentNodeName = n.getParentNode().getNodeName();
			switch(n.getNodeType()) {
			case Node.ELEMENT_NODE:

				this.doRecursive(n);
				break;
			case Node.TEXT_NODE:

				if(! DocIdIsAttribute && parentNodeName.toLowerCase().equals(DocIdLocation))
					ThisDocId = n.getNodeValue();

				if (TermElements.contains(parentNodeName.toLowerCase()))
				{
					currentHolder = parentNodeName;
					doText(n.getNodeValue());
					currentHolder = null;
				}
				break;
			case Node.ATTRIBUTE_NODE:
				if (DocIdIsAttribute || TermsInAttributes)
				{
					String attributeName = (parentNodeName + 
						ELEMENT_ATTR_SEPARATOR + n.getNodeName()).toLowerCase();
					if(DocIdIsAttribute && attributeName.equals(DocIdLocation))
						ThisDocId = n.getNodeValue().replaceAll("\n","");
					if(TermsInAttributes && TermElements.contains(attributeName))
					{
						currentHolder = attributeName;
						doText(n.getNodeValue());	
						currentHolder = null;
					}
				}
				break;
			default:
				if(logger.isDebugEnabled()){
				logger.debug("OTHER NODE"+ n.getNodeType() + " : "+ n.getClass());
				}
			}		
		}

		/** Breaks up String t into terms, and saves them */
		private void doText(String t)
		{
			if (t == null || t.length() == 0)
				return;
			
			//TODO max numbers in a row, aka TRECDocument
		
			if(lowercase)
				t = t.toLowerCase();	

			//initialise the stringbuffer with the maximum length of a term (heuristic)
			StringBuilder sw = new StringBuilder(tokenMaximumLength);
			for(int i=0;i<t.length();i++)
			{
				char ch = t.charAt(i);
				if (Character.isLetterOrDigit(ch))
				{
					sw.append(ch);
				}
				else if (sw.length() > 0)
				{
					String term = sw.toString();
					if (term.length() > tokenMaximumLength)
						term = term.substring(0, tokenMaximumLength);
					terms.add(term);
					sw = new StringBuilder(tokenMaximumLength);
				}
			}
		}

		/** Returns true if no more terms can be fetched from this document */
		public boolean endOfDocument()
		{
			return termNumber == terms.size();
		}

		public Set<String> getFields(){return null;}

		/** Returns the next term from the document */
		public String getNextTerm()
		{
			return terms.get(termNumber++);
		}
		public Reader getReader(){ return null; }
	
		public String getProperty(String name){/*TODO*/return null;}
		public Map<String,String> getAllProperties(){/*TODO*/ return null;}

	}

	/* We need to define:
	 * what elements contains an entire document
	 * where the docid is
	 * what elements contain terms
	 * what attributes of which elements also contain terms
	*/

	/** Contains the names of tags that encapsulate entire documents */	
	protected HashSet<String> DocumentElements = new HashSet<String>();
	/** Set if DocumentElements.size &gt; 0 */
	protected boolean DocumentTags = false;
	/** Contains the names of tags and attributes that encapsulate terms */
	protected HashSet<String> TermElements = new HashSet<String>();
	/** Contains the name of the tag that contains the document name */
	protected String DocIdLocation = "DOCNO";
	/** set if DocIdLocation contains ELEMENT_ATTR_SEPARATOR */
	protected boolean DocIdIsAttribute = false; //set if DocIdLocation contains ELEMENT_ATTR_SEPARATOR
	/** set if any TermElements contains ELEMENT_ATTR_SEPARATOR */
	protected boolean TermsInAttributes = false;//set if any TermElements contains ELEMENT_ATTR_SEPARATOR
	
	/** The xml parser factory for DOM */
	protected XMLReader dbFactory;
	
	/** the xml parser */
	protected ContentHandler dBuilder = null;
	/** the parsed structure of the XML file we currently have open */
	protected org.w3c.dom.Document xmlDoc = null;
	
	/** A list of all the document objects in this XML file */
	protected LinkedList<XMLDocument> Documents = new LinkedList<XMLDocument>();
	/** the current XML document that is being read by the indexer */
	protected XMLDocument thisDoc = null;
	/* set if no more documents in the current file and no more files left to process */
	protected boolean EOC = false;
	
	/**
	 * A black list of document to ignore.
	 */
	private HashSet<String> DocIDBlacklist = new HashSet<String>();
	/**
	 * The list of files to process.
	 */
	protected LinkedList<String> FilesToProcess;
	
	public SaxXMLCollection()
	{
		this(ApplicationSetup.COLLECTION_SPEC, 
			ApplicationSetup.makeAbsolute(
				ApplicationSetup.getProperty("xml.blacklist.docids", ""), 
					ApplicationSetup.TERRIER_ETC));
	}
	
	public SaxXMLCollection(String CollectionSpecFilename, String BlacklistSpecFilename)
	{
		try{
			dbFactory = XMLReaderFactory.createXMLReader();
		}catch(SAXException e){
			e.printStackTrace();
			System.exit(1);
		}
		
		//load up the list of files to be processed from the collection.spec
		//reads the collection specification file
		try {
			BufferedReader br = Files.openFileReader(CollectionSpecFilename); 
			String filename = null;
			FilesToProcess = new LinkedList<String>();
			while ((filename = br.readLine()) != null) {
				if (!filename.startsWith("#") && !filename.equals(""))
					FilesToProcess.addLast(filename);
			}
			br.close();
			if(logger.isInfoEnabled()){
			logger.info("Finished reading collection specification");
			}
		} catch (IOException ioe) {
			logger.fatal("Input output exception while loading the collection.spec file. "
				+ "("+CollectionSpecFilename+")Stack trace follows.", ioe);
			logger.fatal("Exiting ...");
			System.exit(1);
		}
		
		//reads the trec_blacklist_docid file
		if (BlacklistSpecFilename != null && BlacklistSpecFilename.length() >0)
		{
			try {
				if ((new File(BlacklistSpecFilename)).exists()) {
					BufferedReader br =Files.openFileReader(BlacklistSpecFilename); 
					String blackListedDocid = null;
					while ((blackListedDocid = br.readLine()) != null) {
						if (!blackListedDocid.startsWith("#")
								&& !blackListedDocid.equals(""))
							DocIDBlacklist.add(blackListedDocid);
					}
					br.close();					
				}
			} catch (IOException ioe) {
				logger.fatal("Input/Output exception while reading the document black list."
							+ "Stack trace follows", ioe);
				logger.fatal("Exiting ...");
				System.exit(1);
			}
		}
		
		//get all the stuff from the properties file

		//1. document elements
		String[] docElements = ApplicationSetup.getProperty("xml.doctag", "DOC").split("\\s*,\\s*");
		for(int i=0;i<docElements.length;i++)
			DocumentElements.add(docElements[i].trim().toLowerCase());

		if(DocumentElements.size() > 0)
			DocumentTags = true;

		//2. what's the name of the docno tag
		DocIdLocation = ApplicationSetup.getProperty("xml.idtag", "DOCNO").trim().toLowerCase();
		if(DocIdLocation.indexOf(ELEMENT_ATTR_SEPARATOR) != -1)
			DocIdIsAttribute = true;
			
		//3. term elements
		String[] termElements = ApplicationSetup.getProperty("xml.terms", "").toLowerCase().split("\\s*,\\s*");
		for(int i=0;i<termElements.length;i++)
		{
			TermElements.add(termElements[i].trim());
			if (termElements[i].indexOf(ELEMENT_ATTR_SEPARATOR) != -1)
				TermsInAttributes = true;			
		}
		
		//dbFactory.setValidating(false);
		
		try{
			dBuilder = dbFactory.getContentHandler();
			//dBuilder.setErrorHandler(new DefaultHandler() );
		}catch (Exception pce) {
			logger.fatal("ERROR: Couldn't build a DOM parser : ",pce);
			logger.fatal("Exiting ...");
			System.exit(1);
		}
	}

	public void close()
	{
		//TODO
	}

	public boolean endOfCollection()
	{
		return EOC;	
	}

	public String getDocid()
	{
		return thisDoc.ThisDocId;
	}

	public boolean nextDocument()
	{//get into the state where getDocument() will return the next document
		if (EOC)
			return false;

		if (Documents.size() == 0)
			openNextFile();

		if (Documents.size() > 0)
		{
			thisDoc = (XMLDocument)Documents.removeFirst();
			if (DocIDBlacklist.contains(thisDoc.ThisDocId))
				return nextDocument();//hmm, bad recursion
			return true;
		}
		EOC = true;
		return false;
	}

	private boolean findDocumentElement(Node n)
	{
		if(n == null)
			return false;
		
		if (DocumentElements.contains(n.getNodeName().toLowerCase()))
		{
			Documents.addLast(new XMLDocument(n));
			return true;//assume no other documents are contained in documents
		}

		NodeList nodes = n.getChildNodes();
		for(int i=0;i<nodes.getLength();i++)
		{
			Node c = nodes.item(i);
			if(n == null)
				continue;
			findDocumentElement(c);
		}
		return false; //dont think this rtr really matters	
	}

	public Document getDocument()
	{
		return (Document)thisDoc;
	}

	public void reset()
	{
		//not implemented
	}

	protected boolean openNextFile()
	{
		if (FilesToProcess.size() == 0)
			return false;
		String filename = (String)FilesToProcess.removeFirst();
		try{

			if(bReformXML)
			{
				//this replaces all &AMP; with &amp;
				/* NB: This operation MAY be dangerous, as it MAY disrupt the encoding
				 * of strings in the document while copying
				 * Use at your own discretion, and test thoroughly.
				 * TODO check */
				if(logger.isDebugEnabled()){
					logger.debug("Copying xml to temporary file");
				}
				
				
				File temp = File.createTempFile("simpleXMLcollection", ".xml");
				Files.copyFile(new File(filename), temp);
				
				if(logger.isDebugEnabled()){
					logger.debug("parsing "+temp.getAbsoluteFile());
				}
				//xmlDoc = dbFactory.parse(temp);
				temp.delete();
			}
			else
			{
				//xmlDoc = dBuilder.parse(Files.openFileStream(filename));
				 //xmlDoc = dBuilder.parse("file:"+filename);
			}
		} catch (Exception saxe) {
			logger.error("WARNING: Error parsing XML file "+ filename+ " : ", saxe);
			return openNextFile(); //bad: Recursion
		}
		

		if (DocumentTags)
		{
			findDocumentElement(xmlDoc);
		}
		else
		{
			Documents.addLast(new XMLDocument(xmlDoc));
		}
		if(logger.isInfoEnabled()){
			logger.info("Found "+Documents.size() + " documents in "+filename);
		}
		xmlDoc = null;
		return true;
	}

	public static void main(String[] args)
	{
		Collection c = new SaxXMLCollection();
		while(c.nextDocument())
		{	
			if(logger.isInfoEnabled()){
				logger.info("DOCID: "+c.getDocid());
			}
			Document d = c.getDocument();
			if(logger.isInfoEnabled()){
				while(! d.endOfDocument())
				{
					System.out.println(d.getNextTerm());
				}
			}
		}
	}

}

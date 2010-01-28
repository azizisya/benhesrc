package uk.ac.gla.terrier.indexing;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.StringComparator;

public class AnchorTextCollection implements Collection
{

	protected boolean UseCollection = true;
	protected Collection coll = null;

	
	protected String currentDocno = null;
	protected BufferedReader br_docnos = null;
	protected boolean eofDocnos = false;	

	protected BufferedReader br_anchors = null;
	protected String currentDocid = null;
	protected ArrayList anchors = new ArrayList();
	protected String[] thisDocumentAnchors = new String[0];
	protected static final String[] typeRef = new String[0];
	protected static final Map emptyProperties = (Map)(new HashMap(0));

	protected static final StringComparator DocIDCompare = new StringComparator();
	
	protected final boolean OnlyAnchors;
	
	/**
	 * 
	 */

	public AnchorTextCollection()
	{
		String AnchorsFilename = ApplicationSetup.getProperty("anchors.file",null);
		OnlyAnchors = true;
		try {
			br_anchors = openFile(AnchorsFilename);
		} catch (IOException ioe) {
			System.err.println(
				"Input/Output exception while opening the anchors file in AnchorTextCollection : "+AnchorsFilename+
				" - " + ioe);
			ioe.printStackTrace();
			System.exit(1);
		}

		final String docno_filename = ApplicationSetup.getProperty("anchors.docnos.file", null);		
		if (docno_filename == null)
		{
			loadCollection(new TRECCollection());
		}
		else
		{
			try{
			br_docnos = openFile(docno_filename);
			} catch (IOException ioe) {
				System.err.println(
					"Input/Output exception while opening the anchors file in AnchorTextCollection : "+AnchorsFilename+
					" - " + ioe);
				ioe.printStackTrace();
				System.exit(1);
			}
			UseCollection = false;
		}
	}
	public AnchorTextCollection(Collection c)
	{
		this();
		loadCollection(c);	

	}

	protected void loadCollection(Collection c)
	{
		coll = c;
		UseCollection = true;
	}


	protected BufferedReader openFile(String filename) throws IOException
	{
		BufferedReader b = null;
		if (filename.toLowerCase().endsWith(".gz"))
			b = new BufferedReader( new InputStreamReader( 
				new GZIPInputStream( new FileInputStream(filename))));
		else
			b = new BufferedReader( new FileReader(filename));
		return b;	
	}

	protected String[] parseLine(String l)
	{
		String[] rtr = new String[2];
		int pos = l.indexOf('\t');
		rtr[0] = l.substring(0,pos);
		rtr[1] = l.substring(pos+1);
		return rtr;
	}
	
	/**
	 *
	 */	
	public boolean nextDocument()
	{

		String desired = null;
		if (UseCollection)
		{
			if (! coll.nextDocument())
				return false;
			desired = coll.getDocid();
		}
		else
		{
			if (eofDocnos)
				return false;
			try{			
				desired = br_docnos.readLine();
			}catch (IOException ioe) {}
			if(desired == null)
			{
				eofDocnos = true;
				return false;
			}
			desired = desired.trim();
			currentDocno = desired;
		}
		final String desired_docid = desired;
		thisDocumentAnchors = new String[0];	

		if (! desired_docid.equals(currentDocid))
			anchors.clear();

		try{
			String line = null;
			NEXTLINE: while((line = br_anchors.readLine()) != null)
			{
				String[] parts = parseLine(line);
				//System.out.print("Comparing found "+parts[0]+" with desired "+desired_docid + ": ");
				//System.out.println(DocIDCompare.compare(parts[0], desired_docid));
				switch (DocIDCompare.compare(parts[0], desired_docid)) {
					case -1: //we're not at this docid in the anchors file yet
						continue NEXTLINE;
						//no break required
					case 0: //at the correct anchors file, keep record of them
						currentDocid = parts[0];
						anchors.add(parts[1]);
						break;
					case 1: //fallen off the end of the anchors for this document
						//save the current anchors
						if (desired_docid.equals(currentDocid))
						{
							thisDocumentAnchors = (String[])anchors.toArray(typeRef);
							//clear them, and add this anchor to the anchors of the next document
							anchors.clear();
						}
						anchors.add(parts[1]);
						break NEXTLINE; //process no more new lines
				}
			}
			//System.out.println(desired_docid + " => "+ thisDocumentAnchors.length +" anchors");
		}catch (IOException ioe) {
			System.err.println("WARNING: IOException reading anchors file"+ioe);
			ioe.printStackTrace();
		}
		return true;
	}


	protected String joinString(final String[] parts, final String delim)
	{
		final int size = parts.length;
		if (size == 0)
			return "";
		final int INITIAL_SIZE_LOAD_FACTOR = 5;
		final StringBuffer s = new StringBuffer(size*INITIAL_SIZE_LOAD_FACTOR);
		for(int i=0;i<size-1;i++)
		{
			s.append(parts[i]);
			s.append(delim);
		}
		s.append(parts[size-1]);
		return s.toString();
	}


	/**
	 */
	public Document getDocument()
	{
		Document d = null;
		if(OnlyAnchors)
		{
			//d = new AnchorsOnlyDocument(coll.getDocument(), thisDocumentAnchors);
			d = new TRECDocument(
				new StringReader(joinString(thisDocumentAnchors, " ")), 
				UseCollection
					? coll.getDocument().getAllProperties()
					: emptyProperties
				);
		}
		else
		{
			//TODO
			//d = new AnchorsAndDocument(coll.getDocument(), thisDocumentAnchors);
		}
		return d;
	}

	/**
	 * Get the String document identifier of the current document.
	 * @return String the document identifier of a document.
	 */
	public String getDocid()
	{
		return UseCollection ? coll.getDocid() : currentDocno;
	}

	/**
	 * Returns true if the end of the collection has been reached
	 * @return boolean true if the end of collection has been reached,
	 *		 otherwise it returns false.
	 */
	public boolean endOfCollection()
	{
		return UseCollection ?  coll.endOfCollection() : eofDocnos;
	}

	/**
	 * Resets the Collection iterator to the start of the collection.
	 */
	public void reset()
	{
		if (UseCollection)	
			coll.reset();
		else
		{/*TODO - reopen docnos file */ }
	}

	public void close() {}

}

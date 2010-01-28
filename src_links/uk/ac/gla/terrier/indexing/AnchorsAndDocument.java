package uk.ac.gla.terrier.indexing;

import java.io.Reader;
import java.util.HashSet;
import java.io.StringReader;
import java.io.Reader;
import java.util.Set;

class AnchorsAndDocument extends AnchorsDocument implements Document
{
	AnchorsAndDocument(Document parent, String[] anchors)
	{
		super(parent, anchors);
	}
	
	public String getNextTerm()
	{
		//TODO
		return null;
	}

	public Set<String> getFields()
	{
		if (finishedAnchors)
			return d.getFields();
		else
			return anchorField;	
	}

	public boolean endOfDocument()
	{
		if(finishedAnchors)
			return d.endOfDocument();
		else
			return false;
	}

	public Reader getReader()
	{
		//TODO should this be supported at all
		//TODO ie return anchors?
		System.err.println("WARNING: getReader method called on AnchorCollection.AnchorAndDocument. This method "+
			"will not return the anchors associated with this document");
		return d.getReader();
	}
}

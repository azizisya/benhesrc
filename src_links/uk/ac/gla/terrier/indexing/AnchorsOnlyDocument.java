package uk.ac.gla.terrier.indexing;

import java.io.Reader;
import java.io.StringReader;
import java.util.HashSet;

class AnchorsOnlyDocument extends AnchorsDocument implements Document
{
	
	AnchorsOnlyDocument(Document parent, String[] anchors)
	{
		super(parent,anchors);
	}
	
	public String getNextTerm()
	{
		//TODO
		return null;
	}

	public HashSet getFields()
	{
		return anchorField;	
	}

	public boolean endOfDocument()
	{
		return finishedAnchors;
	}

	public Reader getReader()
	{
		System.err.println("WARNING: getReader method called on AnchorCollection.AnchorDocument. This method "+
			"will not return the document itself, only the anchors");
		StringBuffer bufAnchors = new StringBuffer();//TODO find an appropriate default length
		for(int i=0;i<anchors.length;i++)
		{
			bufAnchors.append(anchors[i]);
			bufAnchors.append(" ");
		}
		return new StringReader(bufAnchors.toString());
	}
}

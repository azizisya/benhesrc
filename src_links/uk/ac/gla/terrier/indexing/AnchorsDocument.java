package uk.ac.gla.terrier.indexing;

import java.util.HashSet;
import java.util.Map;

abstract class AnchorsDocument implements Document
{
	protected Document d = null;
	protected String[] anchors = null;
	protected final int anchorsLength;
	protected int anchorsProcessed = 0;
	protected boolean finishedAnchors = false;
	protected final HashSet anchorField = new HashSet(1);
	protected static final String DEFAULT_FIELD_NAME = "ATEXT";
	
	AnchorsDocument(Document parent, String[] anchors)
	{
		anchorField.add(DEFAULT_FIELD_NAME);
		anchorsLength = anchors.length;
		//thisDoc = new StringReader(anchors);
		d = parent;
	}

	public String getProperty(String name)
	{
		return d.getProperty(name);
	}
	public Map getAllProperties()
	{
		return d.getAllProperties();
	}
}

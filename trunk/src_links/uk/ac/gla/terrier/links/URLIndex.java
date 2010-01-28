package uk.ac.gla.terrier.links;

import java.io.IOException;

/**
 * Describes the interface that a url accessibility
 * server should implement.
 */
public interface URLIndex {
	/** Returns the URL of a given document.*/
	public String getURL(int docid) throws IOException;

	/** Returns the docid for the given url.*/
	public int getDocid(String url) throws IOException;

	/** Returns the domain for a given docid.*/
	public String getDomain(int docid) throws IOException;

	/** Returns the path for a given docid.*/
	public String getPath(int docid) throws IOException;

	/** Closes the underlying structures.*/
	public void close() throws IOException;
}

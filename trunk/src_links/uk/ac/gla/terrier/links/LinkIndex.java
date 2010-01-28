package uk.ac.gla.terrier.links;
import java.io.IOException;
/**
 * Provides an interface with a connectivity server, whether it 
 * is a local file, or a remote server.
 */
public interface LinkIndex {
	/** 
	 * Return the outgoing links of the document we were looking for.
	 * If the document doesn't have any outgoing links, the it returns null.
	 * @param docid The docid of the document we are looking for.
	 */
	int[] getOutLinks(int docid) throws IOException;
	/** 
	 * Return the incoming links of the document we were looking for.
	 * If the document doesn't have any incoming links, the it returns null.
	 * @param docid The docid of the document we are looking for.
	 */
	int[] getInLinks(int docid) throws IOException;
	/** 
	 * Return the outgoing links of the documen we were looking for, within
	 * a given set of documents. If the document doesn't have any outgoing
	 * links, then it returns null.
	 * @param docid The docid of the document we are looking for.
	 * @param docids The set of documents within which the links should be.
	 */
	int[] getOutLinks(int docid, java.util.HashSet docids) throws IOException;
	/** 
	 * Return the incoming links of the documen we were looking for, within
	 * a given set of documents. If the document does not have any incoming
	 * links, then it returns null.
	 * @param docid The docid of the document we are looking for.
	 * @param docids The set of documents within which the links should be.
	 */
	int[] getInLinks(int docid, java.util.HashSet docids) throws IOException;
	/** 
	 * Returns the number of outgoing links for a specific document.
	 * If the document doesn't have any outgoing links, then it returns zero.
	 * @param docid The docid of the document we are looking for.
	 */
	int getNumberOfOutLinks(int docid) throws IOException;
	/** 
	 * Returns the number of incoming links for a specific document.
	 * If the document doesn't have any incoming links, then it returns zero.
	 * @param docid The docid of the document we are looking for.
	 */
	int getNumberOfInLinks(int docid) throws IOException;
	/** 
	 * Returns the number of outgoing links for a specific document within a 
	 * given set of documents.
	 * If the document doesn't have any outgoing links, then it returns zero.
	 * @param docid The docid of the document we are looking for.
	 * @param docids The set of documents within which the links should be.
	 */
	int getNumberOfOutLinks(int docid, java.util.HashSet docids) throws IOException;
	/** 
	 * Returns the number of incoming links for a specific document within a 
	 * given set of documents.
	 * If the document doesn't have any incoming links, then it returns zero.
	 * @param docid The docid of the document we are looking for.
	 * @param docids The set of documents within which the links should be.
	 */
	int getNumberOfInLinks(int docid, java.util.HashSet docids) throws IOException;
	/** 
	 * Finalises the data structures used for accessing the link structure.
	 */
	void close() throws IOException;
}	

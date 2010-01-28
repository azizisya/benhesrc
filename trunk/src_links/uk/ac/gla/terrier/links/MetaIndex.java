package uk.ac.gla.terrier.links;
import java.io.IOException;
/** 
 * Describes the interface a class that implements a meta server
 * should implement. 
 * @author Craig Macdonald
 */
public interface MetaIndex
{
	/** Obtain the metadata of the document indentified by DocID from the loaded index */
	public String getItem(String Key, int docid)
		throws IOException;

	public String[] getItems(String Key, int[] docids)
		throws IOException;

	public String[] getItems(String[] keys, int docids) throws IOException;
	
	public String[][] getItems(String Key[], int[] docids)
		throws IOException;
	/** Add an items to the metadata index, under named Key for docid, value value. */
	public void addItem(String Key, int docid, String value)
		throws IOException;
	/** Closes the underlying structures.*/
	public void close()
		throws IOException;
}

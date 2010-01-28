package uk.ac.gla.terrier.links;

import uk.ac.gla.terrier.structures.PersistentHashtable;
import uk.ac.gla.terrier.structures.JDBMHashtable;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import java.io.IOException;

/** 
 * Provides a Meta-data provider for different kinds of meta data.
 * This implementation is just a light wrapper over JDBMHashtable.
 * NB: It is important to close() the index after writing. 
 */
public class MetaServer2 implements MetaIndex
{
	/** Obtain a named metadata of the document indentified by DocID */
	PersistentHashtable datastore;

	public MetaServer2()
	{
		this(
			ApplicationSetup.TERRIER_INDEX_PATH+
			ApplicationSetup.FILE_SEPARATOR+
			ApplicationSetup.TERRIER_INDEX_PREFIX + ".metadata");
	}

	/** Create a new MetaServer2.
	 * @param DataFilename is the name where the JDBM should be stored
	 * Defaul is INVERTED_FILE_COLLECTION_DIRECTORY+"/dcs.metadata" 
	 */
	public MetaServer2(String DataFilename)
	{
		datastore = new JDBMHashtable(DataFilename);
	}

	/** Returns a metadata item of name Key for given docid */
	public String getItem(String Key, int docid)
	{
		return datastore.get(Key.toLowerCase()+"|"+docid);
	}

	/** Add an item to the collection */
	public void addItem(String Key, int docid, String Value)
	{
		datastore.put(Key.toLowerCase()+"|"+docid, Value);
	}

	/** Closes the underlying structures. This is important, as
	 * otherwise no changes will be committed to the metadata index 
	 */
	public void close()
	{
		datastore.close();
		datastore = null;
	}

    public String[] getItems(String Key, int[] docids)
        throws IOException
    {
        final int numDocs = docids.length;
        String values[] = new String[docids.length];
        for(int i=0;i<numDocs;i++)
        {
            values[i] = getItem(Key, docids[i]);
        }
        return values;
    }

	public String[] getItems(String[] Keys,int docid)
	{
		final int kLen = Keys.length;
        String[] saOut = new String[kLen];
		for(int i=0;i<kLen;i++)
        {
            saOut[i] = getItem(Keys[i], docid);
        }	
		return saOut;
	}

    public String[][] getItems(String Keys[], int[] docids)
        throws IOException
    {
        final int kLen = Keys.length;
		String[][] saOut = new String[kLen][];
		for(int i=0;i<kLen;i++)
		{
			saOut[i] = getItems(Keys[i], docids);
		}

        return saOut;
    }


	public static void main (String[] args)
	{
		System.out.println( (new MetaServer2()).getItem(args[0], Integer.parseInt(args[1])) );
	}
}

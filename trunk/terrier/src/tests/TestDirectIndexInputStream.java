package tests;
import java.io.IOException;
import uk.ac.gla.terrier.structures.BlockDirectIndexInputStream;
import uk.ac.gla.terrier.structures.DirectIndexInputStream;
import uk.ac.gla.terrier.utility.ApplicationSetup;
/**
 * Tests the creation of the inverted file, assuming that the
 * direct file, document index and lexicon already exist.
 * Creation date: (11/06/2003 09:03:39)
 * @author Vassilis Plachouras
 */
public class TestDirectIndexInputStream {
	/**
	 * Tests the functionality of the inverted index.
	 * @param args an array of command-line arguments
	 */
	public static void main(java.lang.String[] args) throws IOException {
		DirectIndexInputStream dirIndex = null;
		if (ApplicationSetup.BLOCK_INDEXING) 
			dirIndex = new BlockDirectIndexInputStream();
		else 
			dirIndex = new DirectIndexInputStream();
		dirIndex.print();
		dirIndex.close();
	}
}

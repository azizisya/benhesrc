package tests;
import java.io.IOException;

import uk.ac.gla.terrier.structures.indexing.BlockInvertedIndexBuilder;
import uk.ac.gla.terrier.structures.indexing.InvertedIndexBuilder;
import uk.ac.gla.terrier.utility.ApplicationSetup;
/**
 * Tests the creation of the inverted file, assuming that the
 * direct file, document index and lexicon already exist.
 * Creation date: (11/06/2003 09:03:39)
 * @author Vassilis Plachouras
 */
public class TestInvertedIndex {
	/**
	 * Tests the functionality of the inverted index.
	 * @param args an array of command-line arguments
	 */
	public static void main(java.lang.String[] args) {
		System.err.println("Started building the inverted index...");
		long beginTimestamp3 = System.currentTimeMillis();
		InvertedIndexBuilder invIndex = null;
		if (ApplicationSetup.BLOCK_INDEXING) {
			invIndex = new BlockInvertedIndexBuilder();
			
		} else {
			invIndex = new InvertedIndexBuilder();
		}
		invIndex.createInvertedIndex();
		long endTimestamp3 = System.currentTimeMillis();
		System.err.println("Finished building the inverted index...");
		long seconds3 = (endTimestamp3 - beginTimestamp3) / 1000;
		long minutes3 = seconds3 / 60;
		System.err.println("Time elapsed: " + seconds3);
		System.err.println("dumping lexicon file...");
		//lexicon.dumpLexicon();
		//System.err.println("dumping inverted file...");
		//invIndex = new InvertedIndex(lexicon);
		//invIndex.dumpInvertedIndex();
		try {
			invIndex.close();
		} catch(IOException ioe) {
			System.err.println("exception: " + ioe);
			ioe.printStackTrace();
		}
	}
}

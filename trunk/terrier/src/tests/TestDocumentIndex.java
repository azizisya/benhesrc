package tests;
import java.io.IOException;

import uk.ac.gla.terrier.structures.DocumentIndex;
/**
 * Tests the document index class.
 * Creation date: (18/07/2003 12:29:00)
 * @author Vassilis Plachouras
 */
public class TestDocumentIndex {
	/**
	 * Starts the application.
	 * @param args an array of command-line arguments
	 */
	public static void main(java.lang.String[] args) throws IOException {
		// Insert code to start the application here.
		DocumentIndex docIndex = new DocumentIndex();
		docIndex.print();
		System.out.println(docIndex.getDocumentId("WT01-B40-54"));
	}
}

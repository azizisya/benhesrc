package tests;
import uk.ac.gla.terrier.structures.*;
/**
 * A class that utilises the Lexicon class to test its functionalities
 * employing the dumpLexicon method.
 * Creation date: (27/05/2003 19:39:06)
 * @author Vassilis Plachouras
 */
public class TestLexicon {
	/**
	 * Tests the uk.ac.gla.terrier.structures.Lexicon class.
	 * @param args an array of command-line arguments
	 */
	public static void main(java.lang.String[] args) {
		/*
		System.err.println("Started building the lexicon...");
		long beginTimestamp = System.currentTimeMillis();
		Lexicon lexicon = new Lexicon();
		lexicon.createTemporaryLexicon();
		long endTimestamp = System.currentTimeMillis();
		System.err.println("Finished building the lexico...");
		
		long seconds = (endTimestamp - beginTimestamp) / 1000;
		long minutes = seconds / 60;
		
		System.err.println("Time elapsed: " + seconds);
		*/
		Lexicon lexicon = new Lexicon();
		lexicon.print();
		lexicon.close();
	}
}

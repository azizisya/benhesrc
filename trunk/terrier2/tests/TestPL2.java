package tests;
import uk.ac.gla.terrier.matching.models.*;
/**
 * Tests the correctness of the weighting scheme PL2.
 * Creation date: (17/07/2003 16:25:53)
 * @author Vassilis Plachouras
 */
public class TestPL2 {
	/**
	 * Starts the application.
	 * @param args an array of command-line arguments
	 */
	public static void main(java.lang.String[] args) {
		// Insert code to start the application here.
		PL2 pl2 = new PL2();
		pl2.setParameter(1.0D);
		pl2.setAverageDocumentLength(256.0D);
		double score = pl2.score(2.0D, 52.0D, 532.0D, 1034.0D, 1.0D);
		System.err.println("score is " + score);
		pl2.setKeyFrequency(1.0D);
		pl2.setDocumentFrequency(532.0D);
		pl2.setTermFrequency(1034.0D);
		double score2 = pl2.score(2.0D, 52.0D);
		System.err.println("score2 is " + score2);
	}
}

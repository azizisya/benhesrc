package tests;
import uk.ac.gla.terrier.structures.BlockInvertedIndex;
import uk.ac.gla.terrier.structures.InvertedIndex;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.utility.ApplicationSetup;
/**
 * Tests the correctness of the inverted file.
 * Creation date: (29/07/2003 17:09:48)
 * @author Vassilis Plachouras
 */
public class TestPostings {
	/**
	 * Starts the application.
	 * @param args an array of command-line arguments
	 */
	public static void main(String[] args) {
		Lexicon lexicon = new Lexicon();
		InvertedIndex invIndex = null;
		if (ApplicationSetup.BLOCK_INDEXING) {
			invIndex = new BlockInvertedIndex(lexicon);
		} else {
			invIndex = new InvertedIndex(lexicon);
		}
		boolean found = lexicon.findTerm(args[0]);
		if (!found) {
			System.out.println("term " + args[0] + " not found.");
		} else {
			int termCode = lexicon.getTermId();
			int[][] pointers = invIndex.getDocuments(termCode);
			int termFrequency = 0;
			for (int i = 0; i < pointers[0].length; i++) {
				System.out.println(
					"(" + pointers[0][i] + ", " + pointers[1][i] + ") ");
				termFrequency += pointers[1][i];
			}
			System.out.println();
			System.out.println("doc freq: " + lexicon.getNt());
			System.out.println("term freq: " + lexicon.getTF());
			System.out.println("pointers length : " + pointers[0].length);
			System.out.println("term freq read : " + termFrequency);
		}
		invIndex.close();
		lexicon.close();
	}
}

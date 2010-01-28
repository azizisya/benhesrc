package tests;
import uk.ac.gla.terrier.structures.*;
/**
 * Tests whether the term codes return the correct term.
 * Creation date: (21/07/2003 21:31:31)
 * @author Vassilis Plachouras
 */
public class TestLexiconCodes {
	/**
	 * Starts the application.
	 * @param args an array of command-line arguments
	 */
	public static void main(java.lang.String[] args) {
		// Insert code to start the application here.
		Lexicon lexicon = new Lexicon();
		//int termCode = (new Integer(args[0])).intValue();
		String termCode = args[0];
		lexicon.findTerm(termCode);
		String term = lexicon.getTerm();
		int docFreq = lexicon.getNt();
		int freq = lexicon.getTF();
		long byteOffset = lexicon.getEndOffset();
		byte bitOffset = lexicon.getEndBitOffset();
		int readCode = lexicon.getTermId();
		System.out.println(
			"for code "
				+ termCode
				+ " the term is: "
				+ term
				+ " with Nt = "
				+ docFreq
				+ " and freq = "
				+ freq
				+ "\n and the ending offset and bit offset"
				+ " are "
				+ byteOffset
				+ " and "
				+ bitOffset
				+ "\n and the read code is "
				+ readCode);
		System.out.println("--------");
		lexicon.findTerm(readCode);
		term = lexicon.getTerm();
		docFreq = lexicon.getNt();
		freq = lexicon.getTF();
		byteOffset = lexicon.getEndOffset();
		bitOffset = lexicon.getEndBitOffset();
		readCode = lexicon.getTermId();
		System.out.println(
			"for code "
				+ termCode
				+ " the term is: "
				+ term.trim()
				+ " with Nt = "
				+ docFreq
				+ " and freq = "
				+ freq
				+ "\n and the ending offset and bit offset"
				+ " are "
				+ byteOffset
				+ " and "
				+ bitOffset
				+ "\n and the read code is "
				+ readCode);
		InvertedIndex invIndex = new InvertedIndex(lexicon);
		int[][] pointers = invIndex.getDocuments(readCode);
		for (int i = 0; i < pointers[0].length; i++) {
			System.out.print(
				" (" + pointers[0][i] + ", " + pointers[1][i] + ")");
		}
		System.out.println();
	}
}

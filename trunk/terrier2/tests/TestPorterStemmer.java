package tests;
import uk.ac.gla.terrier.terms.PorterStemmer;
/**
 * Tests the functionality of the Porter Stemmer.
 * Creation date: (05/08/2003 10:01:23)
 * @author Vassilis Plachouras
 */
public class TestPorterStemmer {
	/**
	 * Starts the application.
	 * @param args an array of command-line arguments
	 */
	public static void main(String args[]) {
		PorterStemmer stm = new PorterStemmer(null);
		
		String[] s =
			{
				"ies",
				"ion",
				"action",
				"naturalization",
				"tion",
				"oed",
				"ued",
				"ueds",
				"aed",
				"ied" };
		try {
			for (int i = 0; i < s.length; i++)
				System.out.println(stm.stem(s[i]));
		} catch (Exception e) {
		}
	}
}

package tests;
import uk.ac.gla.terrier.utility.*;
/**
 * Tests the functionality of the uk.ac.gla.terrier.utility.TermCodes class.
 * Creation date: (05/08/2003 10:06:23)
 * @author Vassilis Plachouras
 */
public class TestTermCodes {
	/**
	 * Tests the functionality of this class. The main method is only for debugging purposes.
	 * Creation date: (01/07/2003 09:17:49)
	 * @param args java.lang.String[]
	 */
	public static void main(String[] args) {
		String[] terms =
			{
				"counter",
				"map",
				"main",
				"getCode",
				"arity",
				"pair",
				"world",
				"java" };
		long startTime = System.currentTimeMillis();
		for (int i = 0; i < terms.length; i++) {
			System.out.println(
				"code for term "
					+ terms[i]
					+ " is "
					+ TermCodes.getCode(terms[i]));
		}
		final int termsLength = terms.length;
		int code;
		for (int j = 0; j < 1000000; j++) {
			for (int i = 0; i < termsLength; i++) {
				code = TermCodes.getCode(terms[i]);
				//System.out.println("code for term " + terms[i] + " is " + TermCodes.getCode(terms[i]));
			}
		}
		long endTime = System.currentTimeMillis();
		System.out.println(
			"time elapsed: " + ((endTime - startTime) / 1000.0d));
	}
}

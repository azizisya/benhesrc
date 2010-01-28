package tests;
import uk.ac.gla.terrier.applications.TRECQueryingExpansion;
/**
 * This class tests the functionality of the TRECQueryingExpansion class. In order
 * to run, the c value must be given as a command line parameter.
 * @author Ben He
 */
public class TestQueryExpansion {
	/**
	 * Starts the application.
	 * @param args java.lang.String[] an array of command-line arguments
	 */
	public static void main(java.lang.String[] args) {
		// Insert code to start the application here.
		double beta = (new Double(args[0])).doubleValue();
		TRECQueryingExpansion trecQueryingExpansion =
			new TRECQueryingExpansion();
		trecQueryingExpansion.processQueries(beta);
	}
}

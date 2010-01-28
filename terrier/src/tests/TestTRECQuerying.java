package tests;
import uk.ac.gla.terrier.applications.*;
/**
 * This class tests the functionality of the TRECQuerying class. In order
 * to run, the c value must be given as a command line parameter.
 * Creation date: (17/07/2003 13:41:30)
 * @author Vassilis Plachouras
 */
public class TestTRECQuerying {
	/**
	 * Starts the application.
	 * @param args an array of command-line arguments
	 */
	public static void main(java.lang.String[] args) {
		// Insert code to start the application here.
		double beta = (new Double(args[0])).doubleValue();
		TRECQuerying trecQuerying = new TRECQuerying();
		trecQuerying.processQueries(beta);
	}
}

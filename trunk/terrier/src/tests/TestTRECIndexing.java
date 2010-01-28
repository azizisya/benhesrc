package tests;
import uk.ac.gla.terrier.applications.TRECIndexing;
/**
 * This class tests the functionality of the TRECIndexing class.
 * Creation date: (01/07/2003 18:56:53)
 * @author Vassilis Plachouras
 */
public class TestTRECIndexing {
	/**
	 * Starts the application.
	 * @param args an array of command-line arguments
	 */
	public static void main(java.lang.String[] args) {
		boolean	optimiseCodes =	false;
		boolean createDirect = false;
		boolean createInverted = false;
		boolean help = false;
		boolean exit = true;
		TRECIndexing trecIndexing = null;
		if (args.length == 0) {
			trecIndexing = new TRECIndexing();
			trecIndexing.createDirectFile();
			trecIndexing.createInvertedFile();
		} else {
			for (int i=0; i<args.length; i++) {
				if (args[i].equals("--optimize") || args[i].equals("-o"))
					optimiseCodes = true;
				else if (args[i].equals("--inverted") || args[i].equals("-i")) 
					createInverted = true;
				else if (args[i].equals("--direct") || args[i].equals("-d"))
					createDirect = true;
				else if (args[i].equals("--help") || args[i].equals("-h"))
					help = true;
				else {
					exit = true;
				}
			}
		}
		
		if (help) {
			System.out.println("Usage: java TestTRECIndexing [--inverted|-i] [--direct|-d] [--help|-h] [--optimize|-o]");
			System.out.println("where:");
			System.out.println("[--inverted|-i] : builds only the inverted file, assuming that the direct file exists.");
			System.out.println("[--direct|-d] : builds only the direct file.");
			System.out.println("[--help|-h] : prints this message");
			System.out.println("[--optimize|-o] : optimizes term code assignment.");
		}
		
		if (exit)
			System.exit(1);
		
		trecIndexing = new TRECIndexing();
				
		if (createDirect) {
			trecIndexing.createDirectFile();				
		}
		
		if (createInverted) {
			trecIndexing.createInvertedFile();
		}
	}
}

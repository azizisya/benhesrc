package tests;
import uk.ac.gla.terrier.evaluation.NamedPageEvaluation;
/**
 * Evaluates a run for a named/home entity task. The measures provided
 * are the Average Reciprocal Rank, the number of answers found in the 
 * top 1, 5, 10, 20 and 50 documents, as well as the percentage of
 * named entities not found in the top 50 documents.
 * Creation date: (05/08/2003 09:50:12)
 * @author Vassilis Plachouras
 */
public class TestNamedPageEvaluation {
	/**
	 * Starts the evaluation.
	 * Creation date: (21/07/2003 16:35:28)
	 * @param args java.lang.String[] the command line arguments
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println(
				"usage: java NamedPageEvaluation <res filename>");
			System.exit(1);
		}
		NamedPageEvaluation npe = new NamedPageEvaluation();
		npe.evaluate(args[0]);
		npe.writeEvaluationResult();
	}
}

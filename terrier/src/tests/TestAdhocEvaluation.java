package tests;
import uk.ac.gla.terrier.evaluation.*;
import java.io.*;
import uk.ac.gla.terrier.utility.*;
/**
 * Evaluates a run for a TREC task (except the named page task). 
 * The measures provided is the Average Precision.
 * Creation date: (05/08/2003 09:50:12)
 * @author Ben He
 */
public class TestAdhocEvaluation {
	/**
	 * Starts the evaluation.
	 * Creation date: (14/08/2003 22:02:14)
	 * @param args java.lang.String[]
	 */
	public static void main(String[] args) {
		AdhocEvaluation te = new AdhocEvaluation();
			/** list all the result files and then evaluate them */
		File fresdirectory = new File(ApplicationSetup.TREC_RESULTS);
		String[] nomefile = fresdirectory.list();
		for (int i = 0; i < nomefile.length; i++) {
			if (nomefile[i].endsWith(".res")) {
				//System.out.println(nomefile[i]);
				String resultFilename =
					ApplicationSetup.TREC_RESULTS + "/" + nomefile[i];
				String evaluationResultFilename =
					resultFilename.substring(
						0,
						resultFilename.lastIndexOf('.'))
						+ ".txt";
				te.evaluate(resultFilename);
				te.writeEvaluationResult(evaluationResultFilename);
			}
		}
	}
}

package tests;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import uk.ac.gla.terrier.indexing.TRECFullTokenizer;
import uk.ac.gla.terrier.utility.TagSet;
/**
 * A class that utilizes the Tokenizer interface and the classes that implement
 * it to test their functionalities.
 * 
 * @author Vassilis Plachouras
 */
public class TestTokenizer {
	/**
	 * Tests the uk.ac.gla.terrier.indexing.Tokenizer class and the classes that
	 * implement it.
	 * 
	 * @param args
	 *            an array of command-line arguments
	 */
	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.err.println("usage: java tests.TestTokenizer <input file>");
			System.exit(1);
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(args[0])));
		//Tokenizer tokenizer = new StopWordTokenizer(new TRECTokenizer(br));
		//tokenizer = new PorterStemmerTokenizer(new TRECTokenizer(br));
		TRECFullTokenizer tokenizer = new TRECFullTokenizer(
				new TagSet(TagSet.TREC_DOC_TAGS), new TagSet(TagSet.TREC_EXACT_DOC_TAGS), br);
		while (!tokenizer.isEndOfFile()) {
			while (!tokenizer.isEndOfDocument()) {
				String token = tokenizer.nextToken();
				if (tokenizer.inTagToSkip() || (token == null)
						|| (token.length() == 0)) {
					//if (token!=null && token.length()>0)
					//	System.out.println("skipped token: " + token);
					//else
					//	System.out.println("skipped empty tag");
					continue;
				}
				if (tokenizer.inDocnoTag()) {
					System.out.println("docno: " + token);
					StringTokenizer docnoTokens = new StringTokenizer(token,
							" ");
					String docnoToken = null;
					while (docnoTokens.hasMoreTokens())
						docnoToken = docnoTokens.nextToken().trim();
					System.out.println("docno token: " + docnoToken);
				} else if (tokenizer.inTagToProcess()) {
					System.out.println(token);
				}
			}
			tokenizer.nextDocument();
		}
		br.close();
	}
}
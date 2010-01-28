package uk.ac.gla.terrier.querying.parser;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileReader;
import java.io.StringReader;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.matching.MatchingQueryTerms;
import uk.ac.gla.terrier.matching.dsms.DocumentScoreModifier;
import uk.ac.gla.terrier.matching.tsms.TermScoreModifier;
/**
 * TODO: JAVADOC
 * @author graf
 *
 */
class Main {
	
	protected static Logger logger = Logger.getRootLogger();
	
	public static void main(String[] args) {
		try {
			if (args.length==0) {
				TerrierLexer lexer = new TerrierLexer(new DataInputStream(System.in));
				TerrierQueryParser parser = new TerrierQueryParser(lexer);
				Query q = parser.query();
				if(logger.isDebugEnabled()){
					logger.debug(q.toString());
				}
			} else {
				for (int i=0; i<args.length; i++) {
					BufferedReader br = new BufferedReader(new FileReader(args[i]));
					String line = null;
					int j=0; int failed = 0;
					while ((line = br.readLine())!=null) {
						j++;
						String[] entries = line.split("\\t+");
						TerrierLexer lexer = new TerrierLexer(new StringReader(entries[0]));
						TerrierQueryParser parser = new TerrierQueryParser(lexer);
						Query q = parser.query();
						if(q.toString().equals(entries[1]))
						{
							if(logger.isDebugEnabled()){
								logger.debug("OK: ");
								logger.debug("("+j+"): " + line + " : "+ q.toString());
							}
							MatchingQueryTerms terms = new MatchingQueryTerms();
							q.obtainQueryTerms(terms);
							String[] queryTerms = terms.getTerms();
							for (int k=0; k<queryTerms.length; k++) {
								if(logger.isDebugEnabled()){
									logger.debug(queryTerms[k] + " : " + terms.getTermWeight(queryTerms[k]));
								}
								TermScoreModifier[] tsms = terms.getTermScoreModifiers(queryTerms[k]);
								if (tsms!=null) {
									for (int l=0; l<tsms.length; l++)
										if (tsms[l]!=null)
											logger.debug(" " + tsms[l].getName());
										
								}
							}
							DocumentScoreModifier[] dsms = terms.getDocumentScoreModifiers();
							if (dsms!=null) {
								for (int l=0; l<dsms.length; l++)
									if (dsms[l]!=null)
										logger.debug(dsms[l].getName());
							}
						
							logger.debug("--");	
						}
						else
						{
							failed++;
							logger.debug("NOT OK: ");
							logger.debug("(#"+j+"): " + line + " : "+ q.toString());
						}
						
					}
					logger.debug("Failed "+ failed +" tests");
					br.close();
				}
			}
		}
		catch(Exception e) {
			logger.error("An exception occured: ",e);
			
		}
	}
}

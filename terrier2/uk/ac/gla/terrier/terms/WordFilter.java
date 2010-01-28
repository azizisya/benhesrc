/*
 * Created on 12 Sep 2007
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.terms;

import gnu.trove.THashSet;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;

/**
 * @author ben
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class WordFilter implements TermPipeline {
	/** The logger used */
	private static Logger logger = Logger.getRootLogger();
	
	/** The next component in the term pipeline. */
	protected final TermPipeline next;
	
	THashSet<String> whiteWords;
	
	THashSet<String> stopWords;
	
	/**
	 * 
	 */
	public WordFilter(final TermPipeline next) {
		this(next,
				 ApplicationSetup.getProperty("stopwords.filename", "stopword-list.txt"),
				 ApplicationSetup.getProperty("whitewords.filename", "whiteword-list.txt")
				);
	}
	
	public WordFilter(final TermPipeline next, String stopWordListFilename, String whiteWordListFilename){
		this.next = next;
		whiteWords = new THashSet<String>();
		stopWords = new THashSet<String>();
		this.loadWordList(whiteWords, whiteWordListFilename);
		this.loadWordList(stopWords, stopWordListFilename);
		//System.err.println("whiteWords.size()="+whiteWords.size());
		//System.err.println("stopWords.size()="+stopWords.size());
		
		String[] stopTerms = (String[])stopWords.toArray(new String[stopWords.size()]);
		int counter = 0;
		for (int i=0; i<stopTerms.length; i++){
			if (whiteWords.contains(stopTerms[i]))
				counter++;
		}
		//System.err.println("counter: "+counter);
	}
	
	protected void loadWordList(THashSet<String> termSet, String filename)
	{
		if (logger.isDebugEnabled())
			logger.debug("Loading wordlist "+filename);
		//get the absolute filename
		filename = ApplicationSetup.makeAbsolute(filename, ApplicationSetup.TERRIER_SHARE);
		try {
			BufferedReader br = Files.openFileReader(filename);
			String word;
			while ((word = br.readLine()) != null)
			{
				word = word.trim();
				if (word.length() > 0)
				{
					StringTokenizer stk = new StringTokenizer(word, "-., ");
					while (stk.hasMoreTokens())
						termSet.add(stk.nextToken().toLowerCase());
				}
			}
			br.close();
		} catch (IOException ioe) {
			logger.error("Errror: Input/Output Exception while reading word list ("+filename+") :  Stack trace follows.",ioe);
			
		}
		if (termSet.size() == 0)
            logger.error("Error: Empty word list file was used ("+filename+")");
		else if (logger.isDebugEnabled())
			logger.debug(termSet.size()+" terms loaded from the wordlist.");
		
	}
	
	public void processTerm(final String t)
	{
		/*
		if (stopWords.contains(t))
			return;*/
		/*if (whiteWords.contains(t))
			next.processTerm(t);
		else if (stopWords.contains(t))
			return;*/
		if (!whiteWords.contains(t))
			return;
		next.processTerm(t);
	}

}

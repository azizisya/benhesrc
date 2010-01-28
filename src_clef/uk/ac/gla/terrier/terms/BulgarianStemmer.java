package uk.ac.gla.terrier.terms;

import uk.ac.gla.terrier.utility.ApplicationSetup;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.HashMap;

/**
 * Implements a Bulgarian Stemming algorithm by Preslav Nakov. Implemented by Alexander Alexandrov
 * with minor changes by Craig Macdonald when committed to Terrier project. 
 * @author Alexander Alexandrov, e-mail: sencko@mail.bg. Algorithm by Preslav Nakov)
 * @since 2003-9-30
 * @version $Revision: 1.3 $
 * Changes by Craig Macdonald:<br/>
 * <ol>
 * <li>Add package uk.ac.gla.terrier.terms</li>
 * <li>Replace Hashtable with HashMap&lt;String,String&gt;</li>
 * <li>Organise import statements</li>
 * <li>Made static patterns final</li>
 * <li>Formatting &amp; partial Javadoc </li>
 * <li>Lots of public things now protected</li>
 * <li>Made into term pipeline</li>
 * <li>loadStemmingRules() throws IOException</li>
 * </ol>
 * <B>Properties:</b>
 * <ul><li><tt>stemming.hungarian.rules</tt> - where to find the stemmer rules for this stemmer.
 * Expected file path, TERRIER_SHARE will be prepended if the path given is not absolute.</li></ul>
 * <p><b>Notes:</b>
 * This class contains regular expressions using extended characters. It should be compiled in encoding ISO8859-1,
 * not UTF-8. If you have problems compiling this class, set the environment variable LANG to en_GB, or another encoding
 * not containing utf-8. Alternatively, set add "-encoding ISO8859-1" to the javac command line.
 */
public class BulgarianStemmer implements TermPipeline {
 
	/** map of stemming rules loaded from the stemming rules file */ 
	protected HashMap<String, String> stemmingRules = new HashMap<String, String>();
 
	/** From orginal source */ 
	protected int STEM_BOUNDARY = 1;
	/** From orginal source */
	protected final static Pattern vocals = Pattern.compile("[^àúîóåèÿþ]*[àúîóåèÿþ]");
	/** From orginal source */
	protected final static Pattern p = Pattern.compile("([à-ÿ]+)\\s==>\\s([à-ÿ]+)\\s([0-9]+)");
 
	/** Next termpipeline object to pass terms onto */
	protected TermPipeline next = null;

	/** Instantiates a hungarian stemmer object. Loads Stemming rules from
	 * file names by property <tt></tt>
	 * @param next The next object in the termpipeline to pass terms onto
	 */
    public BulgarianStemmer(TermPipeline next)
    {
        this.next = next;
		// get the full file path of the stemming rules file
		String rulesFile = ApplicationSetup.makeAbsolute(
			ApplicationSetup.getProperty("stemming.bulgarian.rules", ""),
			ApplicationSetup.TERRIER_SHARE);

		try{
			loadStemmingRules(rulesFile);
		}catch (IOException ioe) {
			System.err.println("ERROR: Bulgarian stemmer will be useless. Failed to load rules files "+ rulesFile + " - check that "+
			"the rules file is correctly specified using the stemming.hungarian.rules property (default in TERRIER_SHARE) "+ ioe);
			ioe.printStackTrace();
			System.exit(1);
		}
    }
 
  	/** Loads the stemmings rules from the file denoted by filenName
	  * @param fileName file to load stemming rules from */
	public void loadStemmingRules(String fileName) throws IOException {
		stemmingRules.clear();
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "Cp1251"));
		String s = null;
		while ((s = br.readLine()) != null) {
			Matcher m = p.matcher(s);
			if (m.matches()) {
				int j = m.groupCount();
				if (j == 3) {
					if (Integer.parseInt(m.group(3)) > STEM_BOUNDARY) {
						stemmingRules.put(m.group(1), m.group(2));
					}
				}
			}
		}
	}

	/** Stems the term word and returns it, assuming word is a hungarian word */
	public String stem(String word) {
		Matcher m = vocals.matcher(word);
		if (!m.lookingAt()) {
			return word;
		}
		for (int i = m.end() + 1; i < word.length(); i++) {
			String suffix = word.substring(i);
			if ((suffix = stemmingRules.get(suffix)) != null) {
				return word.substring(0, i) + suffix;
			}
		}
		return word;
	}

	/**
	 * Stems the given term, and passes onto the next object in the TermPipeline
	 * @param t String the term to stem.
	 */
    public void processTerm(String t)
    {
        if (t == null)
            return;
        next.processTerm(stem(t));
    }

  
}

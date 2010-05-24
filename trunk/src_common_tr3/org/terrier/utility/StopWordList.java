package org.terrier.utility;

import java.util.HashSet;
import java.io.*;

import org.terrier.utility.ApplicationSetup;

/**
 * This class implements a stop word list as a hash set.
 * Creation date: (01/07/2003 14:52:01)
 * @author Vassilis Plachouras
 */
public class StopWordList {

	/** The hash set containing all the stop words.*/
	protected static HashSet stopWords;

	/** 
	 * The static initialization of the class is responsible for loading the
	 * stop words from a file with a name specified by the stopwords.filename property in the
	 * setup file. There is no default value for this property, therefore, giving a value is
	 * obligatory.
	 */
	static {
       String stopwordsFilename = ApplicationSetup.getProperty("stopwords.filename", "");
       try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(stopwordsFilename)));
            stopWords = new HashSet();

            String word;
            while ((word = br.readLine()) != null)
				stopWords.add(word);

			br.close();
        } catch (IOException ioe) {
            System.err.println("Input/Output Exception while reading stop word list. Stack trace follows.");
			ioe.printStackTrace();
            System.exit(1);

        }
	}

	/**
	 * Tests whether the given term is a stop word or not.
	 * @param term java.lang.String the term to test for beign a stop word
	 * @return true if the term is a stop word, otherwise false
	 */
	public boolean isStopWord(String term) {
		return stopWords.contains(term);
	}
}

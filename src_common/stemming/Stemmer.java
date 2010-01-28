package stemming;

/**
 * This is the interface every stemmer should 
 * implement. There is the method stem(String), which
 * takes as an argument the term to stem and returns
 * the stemmed term.
 * Creation date: (27/05/2003 14:27:55)
 * @author Vassilis Plachouras
 * @version 1
 */
public interface Stemmer {

	/**
	 * Stemming is applied on the given term and the result is returned.
	 * @param term The term to stem.
	 * @return the stemmed term.
	 */
	public String stem(String term);
}

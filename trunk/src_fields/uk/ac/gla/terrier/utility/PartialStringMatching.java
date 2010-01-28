/*
 * Created on 24-Apr-2005
 */
package uk.ac.gla.terrier.utility;

import uk.ac.gla.terrier.matching.models.Idf;
import uk.ac.gla.terrier.structures.Lexicon;

/**
 * Partially matches two strings 
 * @author vassilis
 */
public class PartialStringMatching {
	public static int partialMatch(String pattern, String text) {
		int result = 0;
		for (int i=pattern.length(); i>0; i--) {
			String p = pattern.substring(0,i);
			if (text.indexOf(p) > -1) {
				result = i; 
				break;
			}
		}
		return result;
	}
	
	public static double partialMatch(String[] patterns, String text) {
		double result = 0.0d;
		final int patternsLength = patterns.length;
		for (int i=0; i<patternsLength; i++) {
			result += partialMatch(patterns[i],text) / (double)patterns[i].length();
		}
		return result;
	}
	
	static Idf idf = new Idf();
	
	public static double partialMatch(String[] patterns, String text, Lexicon lexicon) {
		double result = 0.0d;
		final int patternsLength = patterns.length;
		for (int i=0; i<patternsLength; i++) {
			if (lexicon.findTerm(patterns[i])) 
				result += idf.idfDFR(lexicon.getNt()) * partialMatch(patterns[i],text) / (double)patterns[i].length();
		}
		return result;
	}
	
	public static void main(String[] args) {
		String text = "electroniccollection";
		System.out.println(PartialStringMatching.partialMatch("collection",text));
		//String[] queryTermStrings = queryTerms.getTerms();
		String[] patterns = new String[] {"electronic", "collection"};
		
		System.out.println(PartialStringMatching.partialMatch(patterns, text)); 
		
	}
}

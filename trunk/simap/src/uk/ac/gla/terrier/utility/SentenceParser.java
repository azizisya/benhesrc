/*
 * Created on 7 Mar 2008
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.utility;

import gnu.trove.THashSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

import uk.ac.gla.terrier.structures.Span;

public class SentenceParser {
	static final String[] nonBreakingWords = {"al", "Eq", "Dr", "Mr", "Mrs", "Miss", "pp", "Fig", "Figs", "Ltd", "Ref"};
	/**
	 * Algorithm to decide if a word is a section title:
	 * If a term, started with capital letter, is one of the section titles, and followed by a term 
	 * that also starts with a capital letter, then the term indicates the start of a section.
	 */
	static final String[] sectionTitles = {"abstract", "discussion", "introduction", "results", 
		"materials", "methods", "references", "acknowledgment", "conclusions", 
		"acknowledgments", "materials and methods", "433ults", "experimental"};
	
	static public Pattern sentencePattern = Pattern.compile("\\.\\s+|!\\s+|\\|\\s+|\\?\\s+");
	public static String[] extractSentence(String text){
		return sentencePattern.split(text);
	}
	
	private static boolean isNumber(String term){
		int length = term.length();
		for (int i=0; i<length; i++)
			if (!Character.isDigit(term.charAt(i)))
				return false;
		return true;
	}
	
	/**
	 * Split the given text into sentences.
	 * @param text A given text string.
	 * @return An array of sentences.
	 */
	public static Span[] splitIntoSpans(char[] text, String docno){
		// System.out.println(String.copyValueOf(text));
		/* The array list containing the sentences. */
		ArrayList<Span> sentenceList = new ArrayList<Span>();
		StringBuilder buf = new StringBuilder();
		final THashSet<String> nonBreakingWordSet = new THashSet<String>(Arrays.asList(nonBreakingWords));
		final THashSet<String> secTitleSet = new THashSet<String>(Arrays.asList(sectionTitles));
		int textLength = text.length;
		StringBuilder termBuf = new StringBuilder();
		String currentLocation = "CONTENT";
		THashSet<String> foundSecTitleSet = new THashSet<String>();
		for (int i=0; i<text.length; i++){
			char ch = text[i];
			boolean newSentence = false;
			if (ch=='!'||ch=='?'||ch==']'){
				if (buf.length() > 0 && i+1 <textLength){
					char next = text[i+1];
					if (Character.isSpaceChar(next)){	
						newSentence = true;
					}
				}
			}
			else if (ch=='.' && !nonBreakingWordSet.contains(termBuf.toString()) 
					&& !isNumber(termBuf.toString()) 
					// && !secTitleSet.contains(termBuf.toString().toLowerCase())
					){
				/*
				 * If the 2nd previous character is neither space, nor dot, and the next string is a space,
				 * and the next dot or hyphen is not within 20 characters. (This is to prevent splitting references like
				 * "Biochem. Biophys. Res." into three different sentences)
				 */
				boolean dotIn20Chars = false;
				if (i+20<textLength){
					for (int j=i+1; j<=i+20; j++){
						char c = text[j];
						if (c=='.'||c=='-'){
							dotIn20Chars = true;
							break;
						}
					}
				}
				boolean theEndOfSent = false;
				if (!dotIn20Chars && i-2 >= 0 && i+1 <textLength){
					char c = text[i-2];
					char next = text[i+1];
					if (!Character.isSpaceChar(c) && c!='.' && Character.isSpaceChar(next)){
						theEndOfSent=true;
					}
				}
				newSentence = (!dotIn20Chars&&theEndOfSent);
			}
			buf.append(ch);
			if (newSentence){
				Span span = new Span(docno, buf.toString().trim());
				span.setSection(currentLocation);
				sentenceList.add(span);
				buf = new StringBuilder();
			}
			if (Character.isLetterOrDigit(ch))
				termBuf.append(ch);
			else{
				if (isSectionTitle(secTitleSet, termBuf.toString(), text, i, 
						currentLocation, foundSecTitleSet)){
					currentLocation = termBuf.toString().toUpperCase();
					foundSecTitleSet.add(currentLocation.toLowerCase());
				}
				termBuf = new StringBuilder();
			}
		}
		return (Span[])sentenceList.toArray(new Span[sentenceList.size()]);
	}
	
	protected static boolean isSectionTitle(THashSet<String> secTitleSet,
			String word, char[] text, int i, String currentLocation, 
			THashSet<String> preSectionSet){
		/**
		 * Algorithm to decide if a word is a section title:
		 * If a term, started with capital letter, is one of the section titles, and followed by a term 
		 * that also starts with a capital letter, then the term indicates the start of a section.
		 * The word should not be quoted.
		 * The word should not follow a letter in lower case.
		 */
		if (!secTitleSet.contains(word.toLowerCase()))
			return false;
		// is a section title already found in the article, it is unlikely to have a different 
		// section with the same title
		if (preSectionSet.contains(word.toLowerCase()))
			return false;
		
		// special cases:
		
		if (word.equals("433ults"))
			return true;
		
		boolean isSecTitle = true;
		if (word.length()>0){
			// System.out.println("leading char: "+text[i-1-termBuf.length()]);
			
			// if current section is REFERENCES, return false
			if (currentLocation.toLowerCase().equals("references"))
				return false;
			// if current section 
			
			// start with upper case letter?
			if (!Character.isUpperCase(word.charAt(0)))
				return false;
			
			// word is not quoted
			if (text[i-1-word.length()] == '“')
				return false;
			for (int j=1;j<=20;j++){
				if (Character.isSpaceChar(text[i+j]))
					continue;
				if (text[i+j] == '”')
					return false;
			}
			// followed by a term that also starts with an upper case letter
			for (int j=1;j<=20;j++){
				if (Character.isSpaceChar(text[i+j]))
					continue;
				if (Character.isLetter(text[i+j])){
					if (Character.isLowerCase(text[i+j])){
						// unless the word is "Materials" followed by "and"
						if (word.toLowerCase().equals("materials")){
							StringBuilder wordBuf = new StringBuilder();
							wordBuf.append(text[i+j]);
							while ((++j)<=20){
								if (!Character.isLetterOrDigit(text[i+j]))
									break;
								else
									wordBuf.append(text[i+j]);
							}
							if (wordBuf.toString().equals("and"))
								break;
							else
								return false;
						}
						return false;
					}
					else
						break;
				}
			}
			// The word should not follow a letter in lower case.
			for (int k=1;k<=20;k++){
				int termLength = word.length();
				if (Character.isSpaceChar(text[i-k-termLength])){
					continue;
				}else if (Character.isLetterOrDigit(text[i-k-termLength])){
					return false;
				}else{
					break;
				}
			}
			// The precedent word cannot be "in" or "under"
			StringBuilder preWordBuf = new StringBuilder();
			for (int k=20;k>=1;k--){
				int termLength = word.length();
				char ch = text[i-k-termLength];
				if (Character.isLetterOrDigit(ch) || Character.isSpaceChar(ch))
					preWordBuf.append(ch);
			}
			String[] tokens = preWordBuf.toString().trim().split(" ");
			String preWord = tokens[tokens.length-1];
			// System.out.println("preword: "+preWord);
			if (preWord.equalsIgnoreCase("in") || preWord.equalsIgnoreCase("under"))
				return false;
		}
		return isSecTitle;
	}
	
	/**
	 * Split the given text into sentences.
	 * @param text A given text string.
	 * @return An array of sentences.
	 */
	public static String[] splitIntoSentences(char[] text){
		/* The array list containing the sentences. */
		ArrayList<String> sentenceList = new ArrayList<String>();
		StringBuilder buf = new StringBuilder();
		final THashSet<String> nonBreakingWordSet = new THashSet<String>(Arrays.asList(nonBreakingWords));
		int textLength = text.length;
		StringBuilder termBuf = new StringBuilder();
		for (int i=0; i<text.length; i++){
			char ch = text[i];
			boolean newSentence = false;
			if (ch=='!'||ch=='?'||ch==']'){
				if (buf.length() > 0 && i+1 <textLength){
					char next = text[i+1];
					if (Character.isSpaceChar(next)){	
						newSentence = true;
					}
				}
			}else if (ch=='.' && !nonBreakingWordSet.contains(termBuf.toString()) && !isNumber(termBuf.toString())){
				/*
				 * If the 2nd previous character is neither space, nor dot, and the next string is a space,
				 * and the next dot or hyphen is not within 20 characters. (This is to prevent splitting references like
				 * "Biochem. Biophys. Res." into three different sentences)
				 */
				boolean dotIn20Chars = false;
				if (i+20<textLength){
					for (int j=i+1; j<=i+20; j++){
						char c = text[j];
						if (c=='.'||c=='-'){
							dotIn20Chars = true;
							break;
						}
					}
				}
				boolean theEndOfSent = false;
				if (!dotIn20Chars && i-2 >= 0 && i+1 <textLength){
					char c = text[i-2];
					char next = text[i+1];
					if (!Character.isSpaceChar(c) && c!='.' && Character.isSpaceChar(next)){
						theEndOfSent=true;
					}
				}
				newSentence = (!dotIn20Chars&&theEndOfSent);
			}
			buf.append(ch);
			if (Character.isLetterOrDigit(ch))
				termBuf.append(ch);
			else 
				termBuf = new StringBuilder();
			if (newSentence){
				sentenceList.add(buf.toString().trim());
				buf = new StringBuilder();
			}
		}
		return (String[])sentenceList.toArray(new String[sentenceList.size()]);
	}
}

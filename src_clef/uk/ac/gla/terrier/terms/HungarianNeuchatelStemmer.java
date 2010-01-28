package uk.ac.gla.terrier.terms;
/**
 * Stemmer for the hungarian language, implemented as a term pipeline object.
 * Based on <a href="http://www.unine.ch/info/clef/hungarianStemmer.txt">www.unine.ch/info/clef/hungarianStemmer.txt</a>
 * For more stemmers from the University of Neuchatel, see 
 * <a href="http://www.unine.ch/info/clef/">www.unine.ch/info/clef/</a>
 * <p>
 * <b>Deviations from the original</b>:<br/>
 * There should be no morphological deviations from the original C source code. I have
 * in places used regular expressions instead of character array accesses. The use of the 
 * substring() function replaces the C cropping of a string using \0. Most of the comments are
 * as written by Savoy.
 * <p>
 * Hungarian stemmer trying to remove the suffixes corresponding to
 * the different cases, the possessive and the number (plural) for Hungarian nouns.
 * In contrast to the C version, accents such that are often found in the Hungarian language, are removed.
 *
 * @author Jacques Savoy, University of Neuchatel. Coded in Java for Terrier by Craig Macdonald
 * @version $Revision: 1.1 $
 */
public class HungarianNeuchatelStemmer implements TermPipeline {
	protected final TermPipeline next;	
	/**
	 * Constructs an instance of the class, given the next
	 * component in the pipeline. 
	 * @param next TermPipeline the next component in 
	 *		the term pipeline.
	 */
	public HungarianNeuchatelStemmer(TermPipeline next)
	{
		this.next = next;
	}

	/**
	 * Stems the given term, as a TermPipeline instance.
	 * @param t String the term to stem.
	 */
	public void processTerm(String t)
	{
		if (t == null)
			return;
		next.processTerm(stem(t));
	}
	/* end of termpipeline implementation */

	/** 
	 * Returns the stem of a given term
	 * @param word String the term to be stemmed.
	 * @return String the stem of a given term.
	 */
	public String stem(String word) {
		word = remove_accents(word);
		word = remove_case(word);
		word = remove_possessive(word);
		word = remove_plural(word);
		return normalize(word);
	}

	/** Remove accents from String s, such as these accents can occur in the Hungarian language. 
	  * These are acutes on [aeiou], umlaut on [ou], double acutes on [ou]. Also, according to 
      * <a href="http://en.wikipedia.org/wiki/Hungarian_Language">http://en.wikipedia.org/wiki/Hungarian_Language</a>
      * sometimes circumflexes or tildes are used instead of double acute, so this is also taken into account. */
	protected String remove_accents(String s) {
		//Author Craig Macdonald

		/* ACUTES: can occur on [aeiou] */		
		//a acute -> a
		s = s.replaceAll("[\u00C1\u00E1]","a");
		//e acute -> e
		s = s.replaceAll("[\u00C9\u00E9]","e");
		//i acute -> i
		s = s.replaceAll("[\u00CD\u00ED]","i");
		//o acute -> o
		s = s.replaceAll("[\u00D3\u00F3]","o");
		//u acute -> u
		s = s.replaceAll("[\u00DA\u00FA]","u");

		/* UMLAUT (aka Diaeresis): can occur on [ou] */
		//o Umlaut -> o
		s = s.replaceAll("[\u00D6\u00F6]","o");
		//u Umlaut -> u
		s = s.replaceAll("[\u00DC\u00FC]","u");

		/* DOUBLE ACUTES: can occur on [ou] */
		//o double acute -> o
		s = s.replaceAll("[\u0150\u0151]","o");
		//u double acute -> u
		s = s.replaceAll("[\u0170\u0171]","u");

		/* DOUBLE ACUTES(II): Sometimes Circumflex or tilde are used instead of double acute:
		   this stems from when documents were written in Latin-1 codepage, which didn't have double acute
		   characters. Hungarian can be natively written in Latin-2 codepage. See: http://en.wikipedia.org/wiki/Hungarian_Language */
		//o circumflex -> o
		s = s.replaceAll("[\u00D4\u00F4]","o");
		//u circumflex -> u
		s = s.replaceAll("[\u00DB\u00FB]","u");
		//o tilde -> o
		s = s.replaceAll("[\u00D5\u00F5]","o");
		//u tilde -> u
		s = s.replaceAll("[\u0168\u0169]","u");
		
		return s;
	}

	/** For some words, the suggested stemming rules may produce an incorrect stem
	  * e.g. salata  --&gt; salata
	  * salatat --&gt; salat
	  * We try to reduce this kind of errors, by removing trailing aoe in the string.
	  */
	protected String normalize(String word) {
		if (word.length() > 2)
			word = word.replaceFirst("[aoe]$","");
		return word;
	}
	
	/** Remove one of the various suffixes corresponding to a given case */
	protected String remove_case(String sourceWord) {
		final int len = sourceWord.length() -1; 
		char[] tmpChars = new char[len+1];
		sourceWord.getChars(0,len+1, tmpChars, 0);
		final char[] word =	tmpChars;
		if (len > 5)
		{
			/* -kent  modal */
			 if ((word[len]=='t') && (word[len-1]=='n') && 
				(word[len-2]=='e') && (word[len-3]=='k'))
				return sourceWord.substring(0,len-3); //drop 4 char

			/*  Alternative form:
			 *  if (sourceWord.endsWith("kent")
			 *	return sourceWord.substring(0,len-3);
			 */
		}
		if (len > 4)
		{
			/* -n{ae}k dative  */
			if (sourceWord.matches("n[ae]k$"))
				return sourceWord.substring(0,len-2); //drop 3 chars

			/* -C(ae}l instrumentive  (the consonant C is duplicated) */
		   if ((word[len]=='l') && 
				(word[len-2]==word[len-3]) && 
		       (! IsVowel(word[len-2])) &&
		       ((word[len-1]=='a') || (word[len-1]=='e')))
			   return sourceWord.substring(0,len-2); //drop 3 chars

			/* -v(ae}l instrumentive  */
			if ((word[len]=='l') && (word[len-2]=='v') && 
				((word[len-1]=='a') || (word[len-1]=='e')))
				return sourceWord.substring(0,len-2); //drop 3 chars
			
			/* -ert  goal */
			if ((word[len]=='t') && 
				(word[len-1]=='r') && (word[len-2]=='e'))
				return sourceWord.substring(0,len-2); //drop 3 chars
			
			/* -rol  delative */
			if ((word[len]=='l') && 
	       (word[len-1]=='o') && (word[len-2]=='r'))
				return sourceWord.substring(0,len-2); //drop 3 chars
     
			/* -b{ae}n  inessive */
		   if ((word[len]=='n') && (word[len-2]=='b') && 
		       ((word[len-1]=='a') || (word[len-1]=='e')))
		      return sourceWord.substring(0,len-2); //drop 3 chars
     
			 /* -b{o"o}l  elative */
			if ((word[len]=='l') && (word[len-2]=='b') && 
				(word[len-1]=='o'))  
				 return sourceWord.substring(0,len-2); //drop 3 chars
	
			/* -n{ae}l  adessive */
			if ((word[len]=='l') && (word[len-2]=='n') && 
				((word[len-1]=='a') || (word[len-1]=='e')))
				return sourceWord.substring(0,len-2); //drop 3 chars

		     /* -h{oe"o}z  allative */
			if ((word[len]=='z') && (word[len-2]=='h') && 
				((word[len-1]=='o') || (word[len-1]=='e')))
				return sourceWord.substring(0,len-2); //drop 2 chars
	
			 /* -t{o"o}l  ablative */
		   if ((word[len]=='l') && (word[len-2]=='t') && 
			   (word[len-1]=='o'))
		      return sourceWord.substring(0,len-2); //drop 3 chars
		}  /* end if len > 4 */

		if (len > 3) {  
			 /* -{aeo}t  accusative */
		   if ((word[len]=='t') && 
			   ((word[len-1]=='a') || (word[len-1]=='o') || (word[len-1]=='e')))
				return sourceWord.substring(0, len-1); //drop 2 chars
		
			 /* -C(ae} transformative  (the consonant C is duplicated) */
		   if ((word[len-1]==word[len-2]) && (!IsVowel(word[len-1])) && 
			   ((word[len]=='a') || (word[len]=='e')))
				return sourceWord.substring(0, len-1); //drop 2 chars

			 /* -v(ae} transformative  */
		   if ((word[len-1]=='v') && 
			   ((word[len]=='a') || (word[len]=='e')))
				return sourceWord.substring(0, len-1); //drop 2 chars

			 /* C-{oe}n superessive (the consonant C is duplicated)  */
		   if ((word[len]=='n') &&  (!IsVowel(word[len-2])) &&
			   ((word[len-1]=='o') || (word[len-1]=='e')))
				return sourceWord.substring(0, len-1); //drop 2 chars

			 /* -r{ae} sublative  */
		   if ((word[len-1]=='r') && 
			   ((word[len]=='a') || (word[len]=='e')))
				return sourceWord.substring(0, len-1); //drop 2 chars

			 /* -b{ae}  illative */
		   if ((word[len-1]=='b') && 
			   ((word[len]=='a') || (word[len]=='e')))
			   return sourceWord.substring(0, len-1); //drop 2 chars

			 /* -ul  essive */
		   if ((word[len]=='l') &&  (word[len-1]=='u'))
			   return sourceWord.substring(0, len-1); //drop 2 chars

			 /* -ig  terminative */
		   if ((word[len]=='g') &&  (word[len-1]=='i'))
			   return sourceWord.substring(0, len-1); //drop 2 chars

			 /* -t  accusative */
		   if (word[len]=='t')
			   return sourceWord.substring(0, len); //drop 1 char

			 /* -n superessive  */
		   if (word[len]=='n')
			   return sourceWord.substring(0, len); //drop 1 char
			  
		}  /* end if len > 3 */
		return sourceWord;
	}


	/** Removes the possessive suffix added to the end of a noun */
	protected String remove_possessive(String sourceWord)
	{
		final int len = sourceWord.length() -1; 
		char[] tmpChars = new char[len+1];
		sourceWord.getChars(0,len+1, tmpChars, 0);
		final char[] word =	tmpChars;

		/* We need to make the distinction between four possibilities:
			- a single object (object:singular or o:sing) 
					  is the property of one(p:sing) or more(p:plur) beings;
			- two (or more) objects (object:plural or o:plur)
					  are the property of a single (p:sing) or not (p:plur)
		*/

		if (len > 5) {  
			 /* C-{ao}tok  your (p:plur; o:singl) (with a consonant C) */
		   if ((word[len]=='k') && (word[len-2]=='t') && 
			   (word[len-1]=='o') && (!IsVowel(word[len-4])) &&
			   ((word[len-3]=='a') || (word[len-3]=='o'))) 
				return sourceWord.substring(0,len-3); //drop 4 char

			 /* C-etek  your (p:plur; o:singl) (with a consonant C) */
		   if ((word[len]=='k') && (word[len-2]=='t') && 
			   (word[len-1]=='e') && (!IsVowel(word[len-4])) &&
			   (word[len-3]=='e')) 
				return sourceWord.substring(0,len-3); //drop 4 char

			 /* -it(eo)k  your (p:plur; o:plur) */
		   if ((word[len]=='k') && (word[len-2]=='t') && (word[len-3]=='i') && 
			   ((word[len-1]=='e') || (word[len-1]=='o'))) 
				return sourceWord.substring(0,len-3); //drop 4 char
		}  /* end if len > 5 */

		if (len > 4) {  
			 /* C-{u"u}nk  our (p:plur; o:sing) (with a consonant C) */
		   if ((word[len]=='k') && (!IsVowel(word[len-3])) &&  
			   (word[len-1]=='n') && (word[len-2]=='u'))
			   return sourceWord.substring(0,len-2); //drop 3 chars

			 /* C-t{oe}k  your (p:plur; o:sing) (with a consonant C) */
		   if ((word[len]=='k') && (word[len-2]=='t') && (!IsVowel(word[len-3])) &&
			   ((word[len-1]=='o') || (word[len-1]=='e')))
			   return sourceWord.substring(0,len-2); //drop 3 chars

			 /* V-juk  their (p:plur; o:sing) (with a vowel V) */
		   if ((word[len]=='k') && (word[len-1]=='u') && 
			   (word[len-2]=='j') && (IsVowel(word[len-3])))
			   return sourceWord.substring(0,len-2); //drop 3 chars

			 /* -ink  our (p:plur; o:plur) */
		   if ((word[len]=='k') && (word[len-1]=='n') && 
			   (word[len-2]=='i'))
			   return sourceWord.substring(0,len-2); //drop 3 chars
		}  /* end if len > 4 */

		if (len > 3) {  
			 /* C-{aoe}m  my (p:sing; o:sing) (with a consonant C) */
		   if ((word[len]=='m') && (!IsVowel(word[len-2])) &&
			   ((word[len-1]=='a') || (word[len-1]=='e') || (word[len-1]=='o')))
			   return sourceWord.substring(0, len-1); //drop 2 chars

			 /* C-{aoe}d  your (p:sing; o:sing) (with a consonant C) */
		   if ((word[len]=='d') && (!IsVowel(word[len-2])) &&
			   ((word[len-1]=='a') || (word[len-1]=='e') || (word[len-1]=='o')))
			   return sourceWord.substring(0, len-1); //drop 2 chars

			 /* C-uk  their  (p:plur; o:sing) (with a consonant C) */
		   if ((word[len]=='k') && (word[len-1]=='u') &&
			   (!IsVowel(word[len-2])))
			   return sourceWord.substring(0, len-1); //drop 2 chars

			 /* V-nk  our (p:plur; o:sing) (with a vowel V) */
		   if ((word[len]=='k') && (IsVowel(word[len-2])) &&  
			   (word[len-1]=='n'))
			   return sourceWord.substring(0, len-1); //drop 2 chars

			 /* V-j(ae)  her/his (p:sing; o:sing) (with a vowel V) */
		   if ((word[len-1]=='j') && (IsVowel(word[len-2])) &&
			   ((word[len]=='a') || (word[len]=='e')))
			   return sourceWord.substring(0, len-1); //drop 2 chars

			 /* -im  my   (p:sing; o:plur)  */
			 /* -id  your (p:sing; o:plur)  */
			 /* -ik  their (p:plur; o:plur)  */
		   if ((word[len-1]=='i') &&
			   ((word[len]=='m') || (word[len]=='d') || (word[len]=='k')))
			   return sourceWord.substring(0, len-1); //drop 2 chars

		}  /* end if len > 3 */
		if (len > 2) {  
			 /* C-(ae}  her/his (p:sing; o:sing) (with a consonant C)  */
		   if (((word[len]=='a') || (word[len]=='e')) && (!IsVowel(word[len-1])))
			   return sourceWord.substring(0, len); //drop 1 char

			 /* V-m  my (p:sing; o:sing) (with a vowel V) */
		   if ((word[len]=='m') && (IsVowel(word[len-1])))
			   return sourceWord.substring(0, len); //drop 1 char

			 /* V-d  your (p:sing; o:sing)  (with a vowel V) */
		   if ((word[len]=='d') && (IsVowel(word[len-1])))
			   return sourceWord.substring(0, len); //drop 1 char

			 /* -i his/her (p:sing; o:plur) */
		   if (word[len]=='i')
			   return sourceWord.substring(0, len); //drop 1 char

		}  /* end if len > 2 */

		return(sourceWord); 
	}

	/** Remove the plural suffix, usually the -k */
	protected String remove_plural(String sourceWord)
	{ 
		final int len = sourceWord.length() -1; 
		char[] tmpChars = new char[len+1];
		sourceWord.getChars(0,len+1, tmpChars, 0);
		final char[] word =	tmpChars;

		if (len > 3) {  
			 /* -{aoe}k  plural */
		   if ((word[len]=='k') && 
			   ((word[len-1]=='o') || (word[len-1]=='e') || 
				(word[len-1]=='a')))
			   return sourceWord.substring(0, len-1); //drop 2 chars

		   }  /* end if len > 3 */

		if (len > 2) {  
			 /* -k  plural */
		   if (word[len]=='k')
			   return sourceWord.substring(0, len); //drop 1 char

		   }  /* end if len > 2 */
		return(sourceWord); 
	}

	/** Returns true if the character c is a vowel in hungarian */
	protected final boolean IsVowel(char c)
	{
		return ('a'==(c)||'e'==(c)||'i'==(c)||'o'==(c)||'u'==(c)||'y'==(c));
	}
}

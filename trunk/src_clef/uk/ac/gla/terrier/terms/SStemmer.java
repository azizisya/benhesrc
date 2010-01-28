package uk.ac.gla.terrier.terms;

/** This is a simple stemmer, described by Donna Harman in How Effective
  * is Suffixing? JASIS 42(1):7--15, 1991.
  * The algorithm is a follows:
  * IF a word ends in "ies" but not "eies" or "aies"
  * &nbsp; THEN "ies" -&gt; "y"
  * IF a word ends in "es" but not "aes", "ees", or "oes"
  * &nbsp; THEN "es" -&gt; "e"
  * IF a word ends in "s", but not "us" or "ss"
  * &nbsp; THEN "s" -&gt; NULL
  */ 
public class SStemmer implements TermPipeline
{
	final TermPipeline next;
	public SStemmer(final TermPipeline n)
	{
		this.next = n;
	}
	
	public void processTerm(String t)
	{
		if (t.endsWith("ies") && ! (t.endsWith("eies") || t.endsWith("aies")))
			t = t.replaceAll("ies$", "y");
		else if (t.endsWith("es") && ! (t.endsWith("aes") || t.endsWith("ees") || t.endsWith("oes")))
			t = t.replaceAll("es$", "e");
		else if (t.endsWith("s") && ! (t.endsWith("us") || t.endsWith("ss")))
			t = t.replaceAll("s", "");
		next.processTerm(t);
	}
}

package uk.ac.gla.terrier.terms;

/** Spanish stemmer implmented by Snowball.
  * @author Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
  * @version $Revision: 1.1 $
  */
public class SpanishSnowballStemmer extends SnowballStemmer
{
	public SpanishSnowballStemmer(TermPipeline n)
	{
		super("Spanish", n);
	}
}

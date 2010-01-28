package uk.ac.gla.terrier.terms;

/** German stemmer implmented by Snowball.
  * @author Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
  * @version $Revision: 1.1 $
  */

public class GermanSnowballStemmer extends SnowballStemmer
{
	public GermanSnowballStemmer(TermPipeline n)
	{
		super("German", n);
	}
}

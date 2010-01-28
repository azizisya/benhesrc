package uk.ac.gla.terrier.terms;

/** Finnish stemmer implmented by Snowball.
  * @author Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
  * @version $Revision: 1.1 $
  */
public class FinnishSnowballStemmer extends SnowballStemmer
{
	public FinnishSnowballStemmer(TermPipeline n)
	{
		super("Finnish", n);
	}
}

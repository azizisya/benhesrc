package uk.ac.gla.terrier.terms;

/** Turkish stemmer implmented by Snowball.
  * @author Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
  * @version $Revision: 1.1 $
  */
public class TurkishSnowballStemmer extends SnowballStemmer
{
	public TurkishSnowballStemmer(TermPipeline n)
	{
		super("Turkish", n);
	}
}

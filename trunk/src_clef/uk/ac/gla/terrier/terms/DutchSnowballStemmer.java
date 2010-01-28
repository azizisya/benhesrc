package uk.ac.gla.terrier.terms;

/** Dutch stemmer implmented by Snowball.
  * @author Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
  * @version $Revision: 1.1 $
  */
public class DutchSnowballStemmer extends SnowballStemmer
{
	public DutchSnowballStemmer(TermPipeline n)
	{
		super("Dutch", n);
	}
}

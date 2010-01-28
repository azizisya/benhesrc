package uk.ac.gla.terrier.terms;

/** Portuguese stemmer implmented by Snowball.
  * @author Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
  * @version $Revision: 1.1 $
  */
public class PortugueseSnowballStemmer extends SnowballStemmer
{
	public PortugueseSnowballStemmer(TermPipeline n)
	{
		super("Portuguese", n);
	}
}

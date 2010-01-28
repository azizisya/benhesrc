package uk.ac.gla.terrier.terms;

/** Danish stemmer implmented by Snowball.
  * @author Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
  * @version $Revision: 1.1 $
  */
public class DanishSnowballStemmer extends SnowballStemmer
{
	public DanishSnowballStemmer(TermPipeline n)
	{
		super("Danish", n);
	}
}

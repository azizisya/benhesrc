package uk.ac.gla.terrier.terms;

/** Russian stemmer implmented by Snowball.
  * @author Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
  * @version $Revision: 1.1 $
  */
public class RussianSnowballStemmer extends SnowballStemmer
{
	public RussianSnowballStemmer(TermPipeline n)
	{
		super("Russian", n);
	}
}

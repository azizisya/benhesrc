package uk.ac.gla.terrier.terms;

/** Swedish stemmer implmented by Snowball.
  * @author Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
  * @version $Revision: 1.1 $
  */
public class SwedishSnowballStemmer extends SnowballStemmer
{
	public SwedishSnowballStemmer(TermPipeline n)
	{
		super("Swedish", n);
	}
}

package uk.ac.gla.terrier.terms;

/** Norwegian stemmer implmented by Snowball.
  * @author Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
  * @version $Revision: 1.1 $
  */
public class NorwegianSnowballStemmer extends SnowballStemmer
{
	public NorwegianSnowballStemmer(TermPipeline n)
	{
		super("Norwegian", n);
	}
}

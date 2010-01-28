package uk.ac.gla.terrier.terms;

/** French stemmer implmented by Snowball.
  * @author Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
  * @version $Revision: 1.1 $
  */
public class FrenchSnowballStemmer extends SnowballStemmer
{
	public FrenchSnowballStemmer(TermPipeline n)
	{
		super("French", n);
	}
}

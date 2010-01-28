package uk.ac.gla.terrier.terms;

/** Hungerian stemmer implmented by Snowball.
  * @author Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
  * @version $Revision: 1.1 $
  */
public class HungarianSnowballStemmer extends SnowballStemmer
{
	public HungarianSnowballStemmer(TermPipeline n)
	{
		super("Hungarian", n);
	}
}

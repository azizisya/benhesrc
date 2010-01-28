package uk.ac.gla.terrier.terms;

public abstract class AbstractStemmer implements TermPipeline
{
	protected final TermPipeline next;
	public abstract String stem(String word);
	public AbstractStemmer(TermPipeline n)
	{
		this.next = n;
	}
	protected AbstractStemmer(){
		next = null;
	}

	public void processTerm(String term)
	{
		next.processTerm(stem(term));
	}

}

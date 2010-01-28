
package uk.ac.gla.terrier.terms;

public class KStemmer extends org.apache.lucene.analysis.KStemmer implements TermPipeline
{
    /* begining of termpipeline implementation */
    /** The implementation of a term pipeline.*/
    protected TermPipeline next;

    /**
     * Constructs an instance of the class, given the next
     * component in the pipeline.
     * @param next TermPipeline the next component in
     *      the term pipeline.
     */
    public KStemmer(TermPipeline next)
    {
        this.next = next;
    }
    /**
     * Stems the given term.
     * @param t String the term to stem.
     */
    public void processTerm(String t)
    {
        if (t == null)
            return;
        next.processTerm(stem(t));
    }

	public String stem(String t)
	{
		return super.stem(t);
	}

}


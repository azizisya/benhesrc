package uk.ac.gla.terrier.terms;

import org.tartarus.snowball.SnowballProgram;
import java.lang.reflect.Method;

/** 
  * @author Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
  * @version $Revision: 1.2 $
  */
abstract public class SnowballStemmer implements TermPipeline {
	/** The actual snowball object that does the stemming */
	protected SnowballProgram stemmer = null;
	/** The appropriate method. Damn reflection APIs. */
	protected Method stemMethod = null;
	/** The next object in the term pipeline to pass stemmed terms to */
	protected TermPipeline next = null;
	/** The language that we're currently stemming in */
	protected String language = null;

	protected final static Object [] emptyArgs = new Object[0];

	/** Creates a new stemmer object for the language StemLanguage. 
	  * @param StemLanguage Name of the language to generate the stemmer for. Must be a valid snowball stemmer language.
	  * @param next The next object in the term pipeline
	  */
	protected SnowballStemmer(String StemLanguage, TermPipeline next)
	{
		StemLanguage = StemLanguage.toLowerCase();
		try{
			Class stemClass = Class.forName("org.tartarus.snowball." + StemLanguage + "Stemmer");
			stemmer = (SnowballProgram) stemClass.newInstance();
			stemMethod = stemClass.getMethod("stem", new Class[0]);
		}catch(Exception e){
			System.err.println("ERROR: Cannot generate snowball stemmer "+StemLanguage+" : "+e);
			e.printStackTrace();
		}
		language = StemLanguage;
		this.next = next;
	}


	/**
	 * Stems the given term and passes onto the next object in the term pipeline.
	 * @param t String the term to stem.
	 */
	public void processTerm(String t)
	{
		if (t == null)
			return;
		next.processTerm(stem(t));
	}

	/** Stems the given term and returns the stem 
	  * @param term the term to be stemmed.
	  * @return the stemmed form of term */
	public String stem(String term) {
		stemmer.setCurrent(term);
		/* one can only imagine why a stemmer abstract class wouldn't
		   wouldn't have a stem() method. */
		try{
			stemMethod.invoke(stemmer, emptyArgs); //stemmer.stem();		
		}catch (Exception e) {
			System.err.println("ERROR: Cannot use snowball stemmer "+language+" : "+e);
			e.printStackTrace();
		}
		return stemmer.getCurrent();
	}
}

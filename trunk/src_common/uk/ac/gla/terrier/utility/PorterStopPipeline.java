/*
 * Created on 2005-1-26
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.utility;

import uk.ac.gla.terrier.terms.PorterStemmer;
import uk.ac.gla.terrier.terms.Stopwords;
import uk.ac.gla.terrier.terms.TermPipeline;

/**
 * @author ben
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class PorterStopPipeline implements TermPipeline{
	TermPipeline stemmer;
	TermPipeline stop;
	
	/**
	 * The default namespace for the term pipeline classes.
	 */
	private final static String PIPELINE_NAMESPACE = "uk.ac.gla.terrier.terms.";
	
	String out = null;
	public PorterStopPipeline (){
		stemmer = new PorterStemmer(this);
		stop = new Stopwords(stemmer);
	}
	
	public String getProcessedTerm(String term){
		out = null;
		stop.processTerm(term);
		//stemmer.processTerm(term);
		term = out;
		return out;
	}
	
	public void processTerm(String term){
		out = term;
	}
}

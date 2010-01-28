/*
 * Created on 2005-3-2
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.applications;

import uk.ac.gla.terrier.matching.BufferedMatching;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.tuning.CorrelationTuning;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.PorterStopPipeline;

/**
 * @author ben
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class TestCorrelation {
	protected CorrelationTuning tuning;
	
	protected BufferedMatching matching;
	
	protected Index index;
	
	protected PorterStopPipeline pipe;
	
	protected Lexicon lexicon;
	
	protected final boolean debugging = new Boolean(
			ApplicationSetup.getProperty("debugging.mode", "false")).booleanValue();
	
	public TestCorrelation(String methodName, Index index, BufferedMatching matching){
		String packagePrefix = "uk.ac.gla.terrier.smooth.tuning.";
		try{
			tuning = new CorrelationTuning(methodName, index);
		}
		catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
		this.index = index;
		this.matching = matching;
		this.pipe = new PorterStopPipeline();
		lexicon = index.getLexicon();
	}
	
	public double getCorrelationTFLength(String term, double parameter){
		String givenTerm = ""+term;
		term = pipe.getProcessedTerm(term);
		if (term == null){
			System.out.println("Term " + givenTerm + " is a stop-word.");
			return -2;
		}
		lexicon.findTerm(term);
		double[] tf = new double[lexicon.getNt()];
		double[] docLength = new double[lexicon.getNt()];
		matching.accessInvIndex(term, docLength, tf);
		return tuning.getCorrelationTFLength(tf, docLength, parameter);
	}
	
	public double getParameteForTargetCorrelation(String term, double corr,
			boolean increasing, boolean processedTerm){
		String givenTerm = ""+term;
		if (!processedTerm){
			term = pipe.getProcessedTerm(term);
			if (term == null){
				System.out.println("Term " + givenTerm + " is a stop-word.");
				return -2;
			}
		}
		lexicon.findTerm(term);
		double[] tf = new double[lexicon.getNt()];
		double[] docLength = new double[lexicon.getNt()];
		matching.accessInvIndex(term, docLength, tf);
		if (debugging)
			System.out.println("accessing inverted file finished.");
		return tuning.getParameter(tf, docLength, corr, increasing);
	}

	public static void main(String[] args) {
		// -corr -tfl <term> <method> <parameter>
		if (args[1].equalsIgnoreCase("-tfl")){
			String term = args[2];
			String method = args[3];
			double parameter = Double.parseDouble(args[4]);
			Index index = Index.createIndex();
			BufferedMatching matching = new BufferedMatching(index);
			TestCorrelation app = new TestCorrelation(method, index, matching);
			double correlation = app.getCorrelationTFLength(""+term, parameter);
			System.out.println("corr(tf, l) for term " + term + " is: " + correlation);
		}
//		 -corr -f <term> <method> <target> <increasing>
		if (args[1].equalsIgnoreCase("-f")){
			String term = args[2];
			String method = args[3];
			double target = Double.parseDouble(args[4]);
			boolean increasing = Boolean.valueOf(args[5]).booleanValue();
			Index index = Index.createIndex();
			BufferedMatching matching = new BufferedMatching(index);
			TestCorrelation app = new TestCorrelation(method, index, matching);
			double parameter = app.getParameteForTargetCorrelation(
					term, target, increasing, false);
			System.out.println("corr(tf, l) for term " + term + " is: " + parameter);
		}
	}
}

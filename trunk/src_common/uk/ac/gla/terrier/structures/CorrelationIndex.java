/*
 * Created on 2005-3-2
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.structures;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import uk.ac.gla.terrier.simulation.TFRanking;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.InvertedIndex;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.tuning.CorrelationTuning;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.PorterStopPipeline;
import uk.ac.gla.terrier.utility.Rounding;

/**
 * @author ben
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class CorrelationIndex {
	File corrIndex;
	
	String methodName;
	
	protected CorrelationTuning tuning;
	
	protected PorterStopPipeline pipe;
	
	Index index;
	
	protected TFRanking ranking;
	
	protected final boolean debugging = new Boolean(
			ApplicationSetup.getProperty("debugging.mode", "false")).booleanValue();
	
	protected final boolean PROCESS_INVALID_TERM = new Boolean(
			ApplicationSetup.getProperty("process.invalid.term", "false")).booleanValue();
	
	double[] parameters;
	
	protected CollectionStatistics collSta;
	
	double default_parameter;
	
	protected Lexicon lexicon;
	
	protected InvertedIndex invIndex;
	
	protected DocumentIndex docIndex;
	
	public final double TARGET_CORRELATION = Double.parseDouble(
			ApplicationSetup.getProperty("target.correlation", "0d"));
	
	public CorrelationIndex(String methodName, Index index){
		collSta = index.getCollectionStatistics();
		this.methodName = methodName;
		this.index = index;
		String packagePrefix = "uk.ac.gla.terrier.smooth.tuning.";
		try{
			tuning = new CorrelationTuning(methodName, index);
		}
		catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
		this.index = index;
		this.pipe = new PorterStopPipeline();
		invIndex = index.getInvertedIndex();
		lexicon = index.getLexicon();
		docIndex = index.getDocumentIndex();
		
		ranking = new TFRanking(index);
		if (debugging)
			System.out.println("Finished calling super constructor of TFRanking...");
		this.initialise();
	}
	
	public void createCorrelationIndex(){
		System.out.println("Creating correlation index...");
		boolean increasing = true;
		Lexicon lexicon = index.getLexicon();
		long numberOfUniqueTerms = collSta.getNumberOfUniqueTerms();
		long numberOfValidTerms = ranking.getNumberOfValidTerms();
		if (!this.PROCESS_INVALID_TERM)
			System.out.println("number of terms to tune: " + numberOfValidTerms);
		else
			System.out.println("number of terms to tune: " + numberOfUniqueTerms);
		try{
			DataOutputStream output = new DataOutputStream(
					new BufferedOutputStream(new FileOutputStream(this.corrIndex)));
			long counter = 0;// counts the number of processe valid terms
			long effCounter = 0;
			for (int i = 0; i < numberOfUniqueTerms; i++){
				lexicon.findTerm(i);
				String term = lexicon.getTerm();
				if (debugging)
					System.out.println("term: " + term);
				double parameter = this.default_parameter;
				if (!this.PROCESS_INVALID_TERM){
					if (ranking.isValidTerm(lexicon.getTermId())){
						parameter = getParameterForTargetCorrelation(
							term, this.TARGET_CORRELATION, increasing, true);
						if (parameter < 0)
							parameter = this.default_parameter;
						else
							effCounter++;
						counter++;
						if (counter%100==0){
							double percentage = (double)counter*100/numberOfValidTerms;
							System.out.println("processing term " + term +
									"..." + "parameter=" + Rounding.toString(parameter, 2) +
									", " + Rounding.toString(percentage, 2) +
									"% finished. " +
									counter + " processed terms. "
									+effCounter + " effective terms.");
						}
					}
				}
				else{
					parameter = getParameterForTargetCorrelation(
							term, this.TARGET_CORRELATION, increasing, true);
					if (parameter < 0)
						parameter = this.default_parameter;
					else
						effCounter++;
					counter++;
					if (counter%100==0){
						double percentage = (double)counter*100/numberOfUniqueTerms;
						System.out.println("processing term " + term +
								"..." + "parameter=" + Rounding.toString(parameter, 2) +
								", " + Rounding.toString(percentage, 2) +
								"% finished. " +
								counter + " processed terms. "
								+effCounter + " effective terms.");
					}
				}
				if (debugging)
					System.out.println("parameter: " + parameter);
				
				output.writeDouble(parameter);
				
			}
			System.out.println("Process finished. " + effCounter + " effective terms.");
			output.close();
		}
		catch(Exception e){
			e.printStackTrace();
			this.corrIndex.delete();
			System.exit(1);
		}
	}
	
	public double getParameterValue(int termid){
		return this.parameters[termid];
	}
	
	public double getParameterValue(String processedTerm){
		lexicon.findTerm(processedTerm);
		return this.parameters[lexicon.getTermId()];
	}
	
	public void accessInvIndex(String term, double[] docLength, double[] tf){
		
		if (lexicon.findTerm(term)){
			
			int[][] pointers = invIndex.getDocuments(lexicon.getTermId());
			int[] pointers1 = pointers[0];
			int[] pointers2 = pointers[1];

			final int numOfPointers = pointers1.length;

			//for each document that contains 
			//the query term, the score is computed.
			int frequency;
			int length;
			for (int j = 0; j < numOfPointers; j++) {
				frequency = pointers2[j];
				length = docIndex.getDocumentLength(pointers1[j]);
				docLength[j] = (double)length;
				tf[j] = (double)frequency;
			}
		}
		
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
		accessInvIndex(term, docLength, tf);
		return tuning.getCorrelationTFLength(tf, docLength, parameter);
	}
	
	public double getParameterForTargetCorrelation(String term, double corr,
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
		accessInvIndex(term, docLength, tf);
		if (debugging)
			System.out.println("accessing inverted file finished.");
		return tuning.getParameter(tf, docLength, corr, increasing);
	}
	
	public void dump(){
		for (int i = 0; i < this.parameters.length; i++){
			lexicon.findTerm(i);
			System.out.println((i+1)+": " + lexicon.getTerm() +
					", parameter: " + parameters[i]);
		}
	}
	
	public void initialise(){
		String corrIndexSuffix = ApplicationSetup.getProperty(
				"correlation.index.suffix", "cr").concat(
						""+methodName.charAt(methodName.length()-1));
		this.corrIndex = new File(ApplicationSetup.TERRIER_INDEX_PATH, 
				ApplicationSetup.TERRIER_INDEX_PREFIX.concat("."+corrIndexSuffix));
		if (methodName.endsWith("2"))
			this.default_parameter = 7d;
		if (methodName.endsWith("B"))
			this.default_parameter = 0.35d;
		if (methodName.endsWith("3"))
			this.default_parameter = 1000d;
		if (!corrIndex.exists()){
			//this.createCorrelationIndex();
		}
		else
			this.load();
	}
	
	public void load(){
		try{
			DataInputStream in = new DataInputStream(
				new BufferedInputStream(new FileInputStream(this.corrIndex)));
			parameters = new double[(int)collSta.getNumberOfUniqueTerms()];
			for (int i = 0; i < collSta.getNumberOfUniqueTerms(); i++){
				this.parameters[i] = in.readDouble();
			}
			in.close();
		}
		catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
}

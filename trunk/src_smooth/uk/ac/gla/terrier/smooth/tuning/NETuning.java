/*
 * Smooth - Smoothing term frequency normalisation
 * Webpage: http://ir.dcs.gla.ac.uk/smooth
 * Contact: ben{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * Information Retrieval Group
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is NETuning.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.smooth.tuning;

import java.io.IOException;
import java.util.Vector;
import uk.ac.gla.terrier.querying.Manager;
import uk.ac.gla.terrier.smooth.matching.BufferedMatching;
import uk.ac.gla.terrier.smooth.normalisationeffect.NormalisationEffect;
import uk.ac.gla.terrier.smooth.simulation.QuerySimulation;
import uk.ac.gla.terrier.smooth.structures.BasicQuery;
import uk.ac.gla.terrier.statistics.Statistics;
import uk.ac.gla.terrier.smooth.utility.SystemUtility;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.TRECQuery;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Rounding;

/**
 * This class extends ParameterTuning by providing tuning functionalities for
 * the normalisations 2 and B. The latter is the normalisation component of
 * BM25. The methodology is based on the tuning method by measuring normalisation
 * effect.
 * @author Ben He <ben{a.}dcs.gla.ac.uk>
 * @version $Revision: 1.2 $
 */
public class NETuning extends ParameterTuning{
	/** The number of bins that the documents are to be splited into.*/
	protected int NUMBER_OF_BINS = Integer.parseInt(ApplicationSetup.getProperty("number.bins.tuning", "1000"));
	/** The definition of NE. It is recomended to use definition 2. */
	protected int DEFINITION_NE = Integer.parseInt(ApplicationSetup.getProperty("definition.ne", "2"));;
	/** The term pipeline. */
	protected Manager pipe;
	/** The normalisation method. */
	protected NormalisationEffect method;
	/** The BufferedMatching used in the tuning process. */
	protected BufferedMatching matching;
	/** The prefix of the used normailsation effect class. */
	protected CollectionStatistics collSta;
	protected final String methodNamePrefix = 
		"uk.ac.gla.terrier.smooth.normalisationeffect.NE";
	/** A boolean variable indicating whether to run the program under debugging model.
	 * This corresponds to property <tt>debugging.mode</tt>.
	 */
	protected final boolean debugging = new Boolean(
			ApplicationSetup.getProperty("debugging.mode", "false")).booleanValue();
	/** The constructor. 
	 * 
	 * @param methodName The name of the used normalisation method.
	 * @param index The Index used for the tuning.
	 */
	public NETuning(String methodName, Index index){
		super(index);
		this.setNormalisationMethod(methodName);
		this.NUMBER_OF_BINS = 
			Integer.parseInt(ApplicationSetup.getProperty("number.bins.tuning", "1000"));
		this.DEFINITION_NE = 
			Integer.parseInt(ApplicationSetup.getProperty("definition.ne", "2"));
		pipe = new Manager(index);
		matching = new BufferedMatching(index);
		collSta = index.getCollectionStatistics();
	}
	/**
	 * This method sets up the used normalisation method.
	 * @param methodName The name of the used normalisation method.
	 */
	protected void setNormalisationMethod(String methodName){
		if (methodName.endsWith("3")){
			System.err.println("Parameter tuning by measuring normalisation effect " +
					"is not applicable to normalisation 3. Use \"-t 3 -corr\" " +
					"instead. Exit...");
			System.exit(1);
		}
		if (methodName.lastIndexOf('.') < 0)
			methodName = this.methodNamePrefix.concat(methodName);
		try{
			method = (NormalisationEffect)Class.forName(methodName).newInstance();
		}
		catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
		method.setAverageDocumentLength(collSta.getAverageDocumentLength());
		method.setNumberOfTokens(collSta.getNumberOfTokens());
	}
	/**
	 * This method tunes the normalisation parameter using real TREC queries.
	 * @param taskName String The name of the retrieval task. It can be "adhoc"
	 * or "td", where the latter stands for topic-distillation.
	 * @return double The estimated parameter value by the tuning process. 
	 */
	public double tuneRealTRECQuery(String taskName){
		TRECQuery queries = new TRECQuery();
		Vector vecDocLength = new Vector();
		while (queries.hasMoreQueries()){
			BasicQuery query = new BasicQuery(queries.nextQuery(), 
					queries.getQueryId(), pipe);
			System.out.println("processing query " + query.getQueryNumber());
			matching.matchWithoutScoring(query.getQueryNumber(), 
					query.getQueryTermStrings());
			double[] docLength = (double[])matching.getRetrievedDocLength().clone();
			if (docLength.length > this.NUMBER_OF_BINS)
				vecDocLength.addElement(Statistics.splitIntoBin(
					matching.getRetrievedDocLength(), this.NUMBER_OF_BINS));
			else{
				System.out.println("too few retrieved documents. ignore query " +
						"from the tuning process.");
			}
		}
		System.out.println("tuning...");
		return this.tune(vecDocLength);
	}
	/**
	 * This method tunes the normalisation parameter using the simulated queries.
	 * @param taskName String The name of the retrieval task. It can be "adhoc"
	 * or "td", where the latter stands for topic-distillation.
	 * @param numberOfSamples int The number of queries to simulate. It is 
	 * recommended to set it to 200.
	 * @return The estimated parameter value.
	 */
	public double tuneSampling(String taskName, int numberOfSamples){
		QuerySimulation simulation = new QuerySimulation(index);
		Vector vecDocLength = new Vector();
		int minLength = 2;
		int maxLength = 5;
		int queryType = SystemUtility.queryType();
		if (queryType == 1){
			minLength = 2;
			maxLength = 5;
		}
		if (queryType == 2){
			minLength = 8;
			maxLength = 16;
		}
		if (queryType == 7){
			minLength = 16;
			maxLength = 25;
		}
		for (int i = 0; i < numberOfSamples; i++){
			System.out.println(">>>>>>>>>>>>Sample " + (i+1));
			double[] docLength = new double[this.NUMBER_OF_BINS-1];
			BasicQuery query = null;
			while (docLength.length < this.NUMBER_OF_BINS){
				query = simulation.oneStepSimulation(minLength, maxLength);			
				matching.matchWithoutScoring(query.getQueryNumber(), 
						query.getQueryTermStrings());
				docLength = matching.getRetrievedDocLength();
			}
			if (docLength.length >= this.NUMBER_OF_BINS)
				vecDocLength.addElement(
						Statistics.splitIntoBin(docLength, this.NUMBER_OF_BINS));
			query.dumpQuery();
		}
		System.out.println("tuning...");
		return this.tune(vecDocLength);
	}
	/**
	 * This method tunes the normalisation parameter for a given Vector
	 * of document length.
	 * @param objDocLength A Vector containing the document length. It contains
	 * a two-dimensional array.
	 * @return The estimated parameter setting.
	 */
	protected double tune(Vector objDocLength){
		Vector vecDocLength = (Vector)objDocLength;
		double[][] docLength = new double[vecDocLength.size()][this.NUMBER_OF_BINS];
		for (int i = 0; i < vecDocLength.size(); i++){
			docLength[i] = (double[])vecDocLength.get(i);
		}
		return tune(docLength);
	}
	/**
	 * Estimate the normalisation parameter for the given document length.
	 * @param docLength A two-dimensional array of document length. In the array,
	 * the size of the first dimension is the number of queries, and the second
	 * the size of the second dimension is the number of bins that the documents
	 * are splited into. 
	 * @return The estimated parameter setting.
	 */
	public double tune(double[][] docLength){
		
		System.out.println("size of the document length array: " +
				docLength.length + " X " + docLength[0].length);
		
		// tune
		double beta = 0;
    	double targetNED = 0;
    	double left = 0;
    	double right = 0;
    	double interval = 0.01;
    	
    	double[] max = this.getMaxNEDBatchMode(docLength, this.DEFINITION_NE);
    	System.out.println("maxNED = " + Rounding.toString(max[0], 4) +
    			" maxBeta = " + Rounding.toString(max[1], 2));
    	
    	int queryType = SystemUtility.queryType();
    	
		if (this.method.getInfo().endsWith("B")){
			if (this.DEFINITION_NE == 2){
				if (queryType == 1){ // title
					targetNED = max[0] * 0.8571;
					right = max[1];
					left = 0.01;
					beta = left;
				}
    			
				if (queryType == 2){ // desc
					targetNED = max[0] * 0.9878;
					left = max[1];
					right = 1;
					beta = left;
				}
				
				if (queryType == 7){ // title + desc + narr
					targetNED = max[0] * 0.9307;
					left = max[1];
					right = 1;
					beta = left;
				}
			}
		}
    	
    	if (this.method.getInfo().endsWith("2")){
    		if (this.DEFINITION_NE == 1){
    			if (queryType == 1){ // title
    				targetNED = max[0] * 0.9676;
    				left = max[1];
    				right = 48d;
    				beta = left;
    			}
    			
				if (queryType == 2){ // desc
					targetNED = max[0] * 0.9766;
					right = max[1];
					left = 0.50d;
					beta = left;
				}
				
				if (queryType == 7){ // title + desc + narr
					targetNED = max[0] * 0.9826;
					right = max[1];
					left = 0.50d;
					beta = left;
				}
    		}
			if (this.DEFINITION_NE == 2){
				if (queryType == 1){ // title
					targetNED = max[0] * 0.9595;
					left = max[1];
					right = 48d;
					beta = left;
				}
    			
				if (queryType == 2){ // desc
					targetNED = max[0] * 0.9792;
					right = max[1];
					left = 0.50d;
					beta = left;
				}
				
				if (queryType == 7){ // title + desc + narr
					targetNED = max[0] * 0.9874;
					left = max[1];
					right = 48d;
					beta = left;
				}
			}
    	}
    	
    	int counter = 0;
    	
    	boolean increasing = false;
    	
    	if (getNEDBatchMode(docLength, left, this.DEFINITION_NE) < 
    		getNEDBatchMode(docLength, right, this.DEFINITION_NE) )
    		increasing = true;
    	
    	while (beta <= right){
    		double NED = this.getNEDBatchMode(docLength, beta, this.DEFINITION_NE);
    		if (increasing && NED >= targetNED)
    			break;
    		if (!increasing && NED <= targetNED)
    			break;
    			
    		beta += interval;
    		counter++;
    	}
    	
    	return beta;
	}
	/**
	 * Get the maximum normalisation effect for a batch of queries
	 * @param docLength A two-dimensional array of the document length.
	 * @param definition The definition of normalisation effect to use.
	 * @return A double array with size of 2. The first element in the
	 * array contains the maximum normalisation effect, provided by the
	 * parameter value that is in the second element in the array.
	 */
	public double[] getMaxNEDBatchMode(double[][] docLength, int definition){
        double[] max = new double[2];
        
        double left = 0;
        double right = 0;
        double beta = 0;
        double NED = 0;
        double interval = 0.01;
        
        if (this.method.getInfo().startsWith("2")){
            left = 0.3;
            right = 8;
            beta = left;  
        }
        
        if (this.method.getInfo().toLowerCase().startsWith("b")){
            left = interval;
            right = 1;
            beta = left;  
        }
        
        boolean flag = true;
        
        while (flag){
           if (debugging)
	           	System.out.println("left = " + Rounding.toString(left, 2) + 
	                ", beta = " + Rounding.toString(beta, 2) +
	                ", right = " + Rounding.toString(right, 2) +
	                ", NED = " + Rounding.toString(NED, 4)); 
	        
            double NEDbuffer = this.getNEDBatchMode(docLength, beta, definition);
            
            if (NEDbuffer < NED){
                beta -= interval;
                break;
            }
            
            NED = NEDbuffer;
            
            if (beta == right || beta > right)
                break;
            
            if (beta < interval && beta + interval > interval)
                beta = right;
            else
                beta += interval;
            
        }
        
        max[0] = NED;  
        max[1] = beta;
        
        return max;
    }    
    
	
	/**
	 * Get the maximum NED value for given document length and definition of NE.
	 * The method does a simple hill-climbing.
	 * @param docLength The length of documents in which the query terms occur.
	 * @param definition The definition of normalisation effect.
	 * @return An array with two elements. The first element is the maximum
	 * NED value and the second element is the parameter value that
	 * gives the maximum NED.
	 */
	public double[] getMaxNED(double[] docLength, int definition){
		/** double[0] = maxNED, double[1] = maxBeta */
		double[] max = new double[2];
        
		double left = 0;
		double right = 0;
		double beta = 0;
		double NED = 0;
		double interval = 0.01;
        
		if (this.method.getInfo().startsWith("2")){
			left = 0.3;
			right = 8;
			beta = left;  
		}
        
		if (this.method.getInfo().toLowerCase().startsWith("b")){
			left = interval;
			right = 1;
			beta = left;  
		}
        
		//int queryType = queryType();
        
		boolean flag = true;
        
		while (flag){
            
            /**
				System.out.println("left = " + Rounding.toString(left, 2) + 
					", beta = " + Rounding.toString(beta, 2) +
					", right = " + Rounding.toString(right, 2) +
					", NED = " + Rounding.toString(NED, 4)); 
					*/
        
			double NEDbuffer = this.getNED(docLength, beta, definition);
            
			if (NEDbuffer < NED){
				beta -= interval;
				break;
			}
            
			NED = NEDbuffer;
            
			if (beta == right || beta > right)
				break;
            
			if (beta < interval && beta + interval > interval)
				beta = right;
			else
				beta += interval;
            
		}
        
		max[0] = NED;  
		max[1] = beta;
        
		return max;
	}
    
	/**
	 * Get the NED value that corresponds to the given document length, 
	 * parameter setting and definition of NE.
	 * @param docLength An array of document length.
	 * @param parameter The parameter setting.
	 * @param definition The definition of NE.
	 * @return The NED value.
	 */
	public double getNED(
			double[] docLength, double parameter, int definition){
        
        return this.method.getNED( 
				docLength, 
				parameter, 
				definition);
         
    }
	/**
	 * Get the maximum NED value for a set of queries.
	 * @param docLength A two-dimensional array containing document length
	 * with respect to a set of queries. In the first dimension, each element
	 * corresponds to a query. The second dimension contains length of
	 * documents with query terms.
	 * @return An array with two elements. The first element is the maximum
	 * NED value and the second element is the parameter value that
	 * gives the maximum NED.
	 */
	public double[] getMaxNED(double[][] docLength){
		double[] max = new double[2];
        
		double left = 0;
		double right = 0;
		double beta = 0;
		double NED = 0;
		double interval = 0.01;
        
		if (this.method.getInfo().startsWith("2")){
			left = 0.3;
			right = 8;
			beta = left;  
		}
        
		if (this.method.getInfo().toLowerCase().startsWith("b")){
			left = interval;
			right = 1;
			beta = left;  
		}
       
		boolean flag = true;
        
		while (flag){
           
			double NEDbuffer = this.getNEDBatchMode(docLength, beta, this.DEFINITION_NE);
            
			if (NEDbuffer < NED){
				beta -= interval;
				break;
			}
            
			NED = NEDbuffer;
            
			if (beta == right || beta > right)
				break;
            
			if (beta < interval && beta + interval > interval)
				beta = right;
			else
				beta += interval;
            
		}       
		max[0] = NED;  
		max[1] = beta;
        
		return max;
	}
	/**
	 * Compute the mean normalisation effect for a set of queries.
	 * @param docLength A two-dimensional array containing the document length.
	 * @param parameter The used normalisation parameter setting.
	 * @param definition The used definition of normalisation effect.
	 * @return The obtained normalisation effect.
	 */
	public double getNEDBatchMode(double[][] docLength, double parameter,
	        int definition){
	    double[] NED = new double[docLength.length];
	    for (int i = 0; i < NED.length; i++)
	        	NED[i] = this.method.getNED(docLength[i], 
	            parameter, 
	            definition);
	    return Statistics.mean(NED);
	}
}

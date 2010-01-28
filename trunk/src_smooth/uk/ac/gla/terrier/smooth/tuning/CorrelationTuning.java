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
 * The Original Code is CorrelationTuning.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.smooth.tuning;

import gnu.trove.TDoubleDoubleHashMap;

import java.util.StringTokenizer;
import java.util.Vector;

import uk.ac.gla.terrier.smooth.structures.OnelineTRECQuery;
import uk.ac.gla.terrier.matching.models.normalisation.Normalisation;
import uk.ac.gla.terrier.querying.Manager;
import uk.ac.gla.terrier.smooth.matching.BufferedMatching;
import uk.ac.gla.terrier.smooth.simulation.QuerySimulation;
import uk.ac.gla.terrier.smooth.structures.BasicQuery;
import uk.ac.gla.terrier.statistics.Statistics;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.InvertedIndex;
import uk.ac.gla.terrier.structures.TRECQuery;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Rounding;
/**
 * This class provides functionalities for tuning term frequency normalisation
 * parameters by measuring the correlation between term frequency and document
 * length.
 * @author Ben He <ben{a.}dcs.gla.ac.uk>
 * @version $Revision: 1.1 $
 */
public class CorrelationTuning extends ParameterTuning{
	/** The used normalisation method. */
	protected Normalisation method;
	/** The prefix of the name of a normalisation method. */
	protected final String methodNamePrefix = 
		"uk.ac.gla.terrier.matching.models.normalisation.Normalisation";
	/** The term pipeline. */
	protected Manager pipe;
	/** Indicate if it is running under debugging model. This correspondes to property
	 * <tt>debugging.mode</tt>. 
	 * */
	
	protected final boolean debugging = new Boolean(
			ApplicationSetup.getProperty("debugging.mode", "false")).booleanValue();
	
	/**
	 * Create an instance of the class by giving the name of the normalisation
	 * method and the index of the collection.
	 * @param methodName The name of used normalisation method.
	 * @param index The index of the collection.
	 */
	public CorrelationTuning(String methodName, Index index){
		super(index);
		this.setNormalisationMethod(methodName);
		pipe = new Manager(index);
	}
	public void setNumberOfTokens(double numberOfTokens) {
		this.numberOfTokens = numberOfTokens;
		this.method.setNumberOfTokens(numberOfTokens);
	}
	public void setAvl(double avl) {
		this.avl = avl;
		this.method.setAverageDocumentLength(avl);
	}
	/**
	 * Set the normalisation method by giving the method name.
	 * @param methodName The name of used normalisation method.
	 */
	public void setNormalisationMethod(String methodName){
		if (methodName.lastIndexOf('.') < 0)
			methodName = this.methodNamePrefix.concat(methodName);
		try{
			method = (Normalisation)Class.forName(methodName).newInstance();
		}
		catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
		method.setAverageDocumentLength(avl);
		method.setNumberOfTokens(this.numberOfTokens);
	}
	/** 
	 * Get correlation of normalised term frequency with document length 
	 * for a given parameter value.
	 * @param termFrequency The frequency of the term in the whole collection.
	 * @param tf The within document frequency of the term.
	 * @param docLength The length of documents containing the term.
	 * @param parameter The given normalisation parameter value.
	 * @return The correlation of normalised term frequency with document length.
	 */
	public double getCorrelationTFLength(
			double termFrequency, double[] tf, double[] docLength, double parameter){
		double[] tfn = new double[tf.length];
		this.method.setParameter(parameter);
		for (int i = 0; i < tf.length; i++)
			tfn[i] = method.normalise(tf[i], docLength[i], termFrequency);
		return Statistics.correlation(tfn, docLength);
	}
	/**
	 * This method extracts all the query terms in the applied fields. The query 
	 * file(s) is (are) specified in the trec.topics.list.
	 * @return An array of query terms. The terms are processed through the
	 * term pipeline.
	 */
	protected String[] extractQueryTerms(){
		TRECQuery queries = new TRECQuery();
		if (queries.getNumberOfQueries() == 0)
			queries = new OnelineTRECQuery();
		// compute the sum of query length
		
		Vector vecTerms = new Vector();
		InvertedIndex invIndex = index.getInvertedIndex();
		Manager pipe = new Manager(this.index);
		int counter = 0;
		while (queries.hasMoreQueries()){
			String queryTerms = queries.nextQuery();
			BasicQuery query = new BasicQuery(queryTerms, queries.getQueryId(), pipe);
			String[] termStrings = query.getQueryTermStrings();
			
			for (int i = 0; i < termStrings.length; i++){
				counter++;
				try{
					if (lexicon.findTerm(termStrings[i])){
						if (lexicon.getNt() > 1 ) 						
							vecTerms.addElement(termStrings[i].trim());
					}
				}
				catch(Exception e){
					System.err.println("term: " + termStrings[i]);
					e.printStackTrace();
					System.exit(1);
				}
			}
		}
		System.out.println(counter+ " query terms in total.");
		return (String[])vecTerms.toArray(new String[vecTerms.size()]);
	}
	/**
	 * This method does a sweeping minimisation/maximisation of the mean correlation of
	 * term frequency with document length for a set of given terms within a range of parameter values
	 * bordered by the given left and right values.
	 * @param terms A set of given terms in a String array.
	 * @param left The left boundary of the allowed parameter values.
	 * @param right The right boundary of the allowed parameter values.
	 * @return An array with a size of 3 which contains the parameter value
	 * providing the minimal.maximal mean correlation, the corresponding 
	 * minimal/maximal mean correlation, and a value indicating if it is
	 * a minimisation task, resectively. For the last element in the array,
	 * a negative value indicates a minimisation process and a positive value
	 * indicates a maximisation process. A zero value means that the correlation
	 * function is monotonic in the given range of parameter values.
	 */
	public double[] minimiseCorrelationTFLength(
			boolean minimise, String[] terms, double left, double right){
		
		// The number of samples in a data-sweeping.
		int numberOfSamples = 5;
		// The halting criteria of the minimisation/maximisation process, i.e. the
		// percentage of difference between the left bound and right bound of the
		// samples.
		double haltingPercentage = 1;
		// Initialise a first-step sweeping.
		double[] samples = Statistics.uniformSampling(left, right, numberOfSamples);
		double[] sampledData = new double[numberOfSamples];
		
		if (minimise)
			System.out.println("minimising correlation measure...");
		else
			System.out.println("maximising correlation measure...");
		
		if (debugging)
			System.err.println("Sampled data initialised.");
		
		double[] newBoundary = new double[2];
		double previousMinSampledData = right;
		
		int maxSearchDepth = 20;
		
		boolean breakTheLoop = false;
		double minimisedSample = -1;
		double minimisedSampledData = -1;
		// A loop for the re-sampling process until the halting criteria is 
		// satisfied.
		int counter = 0;
		TDoubleDoubleHashMap sampleMap = new TDoubleDoubleHashMap();
		while (! breakTheLoop){
			if (debugging){
				System.err.println("-----------------------------------------");
				System.err.println("left: " + Rounding.toString(left, 6) + 
						", right: " + Rounding.toString(right, 6));
			}
			samples = Statistics.uniformSampling(left, right, numberOfSamples);
			sampledData = new double[numberOfSamples];
			this.getCorrelationTFLength(sampleMap, terms, samples, sampledData);
			if (debugging){
				System.err.println("New Sampled data:");
				for (int i = 0; i < sampledData.length; i++){
					System.err.println(Rounding.toString(samples[i], 4) + ": " 
							+ Rounding.toString(sampledData[i], 6));
				}
			}
			
			newBoundary = this.relocateBoundary(samples, sampledData, minimise);
			counter++;
			for (int i = 0; i < samples.length; i++)
				sampleMap.put(samples[i], sampledData[i]);
			
			double errorRate = Math.abs((newBoundary[3]-newBoundary[0])/newBoundary[0]);
			if (debugging){
				System.err.println("Error rate: " + 
						Rounding.toString(errorRate * 100, 2) + "%");
			}
			if (errorRate * 100 < haltingPercentage || counter>=maxSearchDepth){
				minimisedSample = newBoundary[1];
				minimisedSampledData = newBoundary[2];
				breakTheLoop = true;
			}
			else{
				previousMinSampledData = newBoundary[2];
				left = newBoundary[0];
				right = newBoundary[3];
			}
		}
		// return the results.
		double[] min = new double[3];
		min[0] = minimisedSample;
		min[1] = minimisedSampledData;
		if (minimise)
			min[2] = -1;
		else
			min[2] = 1;
		if (debugging){
			if (minimise)
				System.err.println("Value with the minimal correlation: " + 
						Rounding.toString(minimisedSample, 4));
			else
				System.err.println("Value with the maximal correlation: " + 
						Rounding.toString(minimisedSample, 4));
		}
		// Destroy the hashmap.
		sampleMap.clear();
		sampleMap = null;
		return min;
	}
	/**
	 * Locate the two points within which the minimal/maximal sampled data is.
	 * @param samples The samples.
	 * @param sampledData The corresponding sampled data.
	 * @param minimise True and false indicate a minimisation and maximisation
	 * process respectively.
	 * @return A tuple of double values specifies the new boundary. 
	 */
	public double[] relocateBoundary(double[] samples, double[] sampledData, boolean minimise){
		int indexOfMinSample = 0;
		for (int i = 1; i < samples.length; i++){
			if (sampledData[i] < sampledData[indexOfMinSample] && minimise ||
					sampledData[i] > sampledData[indexOfMinSample] && !minimise)
				indexOfMinSample = i;
		}
		int leftIndex = 0;
		int rightIndex = indexOfMinSample+1;
		if (indexOfMinSample != 0)
			leftIndex = indexOfMinSample - 1;
		if (rightIndex >= samples.length)
			rightIndex--;
		double[] returnedData = 
				{samples[leftIndex], samples[indexOfMinSample], sampledData[indexOfMinSample], samples[rightIndex]};
		return returnedData;
	}
	/**
	 * Get the correlation of term frequency with document length for a set of
	 * given sampled parameter values.
	 * @param terms A set of terms.
	 * @param samples The sampled parameter values.
	 * @param sampledData The corresponding sampled mean correlation.
	 */
	protected void getCorrelationTFLength(String[] terms, double[] samples, double[] sampledData){
		for (int i = 0; i < samples.length; i++)
			sampledData[i] = this.getCorrelationTFLength(samples[i], terms);
	}
	/**
	 * Get the correlation of term frequency with document length for a set of
	 * given sampled parameter values.
	 * @param terms A set of terms.
	 * @param samples The sampled parameter values.
	 * @param sampledData The corresponding sampled mean correlation.
	 */
	private void getCorrelationTFLength(
			TDoubleDoubleHashMap sampleMap, String[] terms, double[] samples, double[] sampledData){
		for (int i = 0; i < samples.length; i++){
			double mappedData = sampleMap.get(samples[i]);
			if (mappedData != 0)
				sampledData[i] = mappedData;
			else			
				sampledData[i] = this.getCorrelationTFLength(samples[i], terms);
		}
	}
	/**
	 * Return the parameter value giving the specified mean correlation $\rho(parameter)$ of
	 * normalised term frequency with document length for a set of terms. It
	 * performs a binary search within a range of parameter values that starts
	 * with the value of argument left and ends with the value of argument
	 * right. The mean correlation should be a monotonic function of the parameter
	 * value within the range. The process starts with checking if the target
	 * $\rho$ value can be provided within [left, right]. If not, the left and right
	 * will be automatically adjusted. If the value of left/right, after the adjustment, exceeds the 
	 * boundary that is specified by leftBound/rightBound, while no value within
	 * [left, right] can not provide the target correlation $\rho$, the process
	 * stops and suggests to use a small/large value. 	 
	 * @param terms The set of terms.
	 * @param left The left (smallest) of the valid range of parameter values.
	 * @param right The right (largest) of the valid range of parameter values.
	 * @param target The mean correlation value that we want to achieve.
	 * @param increasing A boolean variable indicating if $\rho$ is a increasing
	 * or decreasing function of parameter value. True for increasing and false for
	 * decreasing.
	 * @param leftBound 
	 * @param rightBound
	 * @return
	 */
	public double getParameter(String[] terms, double left, double right,
			double target, boolean increasing){
		if (debugging){
			System.err.println("initialising the binary search...");
		}
		double parameter = -1;
		double penalty = 0.0001;
		double interval = 0.1;
		double error = 0.0001;
		
		double leftCorr = this.getCorrelationTFLength(left, terms);	
		double rightCorr = this.getCorrelationTFLength(right, terms);
		
		if (debugging){
			System.out.println("leftCorr: " + Rounding.toString(leftCorr, 4) +
					", rightCorr: " + Rounding.toString(rightCorr, 4) +
					", target: " + Rounding.toString(target, 4));
		}
		if ((leftCorr>target&&rightCorr>target)||
				(leftCorr<target&&rightCorr<target)){
			return -1;
		}
		if (left < 0 || right < 0){
			return -1;
		}
		
		parameter = (left+right)/2;
		System.out.println("Tuning...This may take time, please wait in patience.");
		while (interval > penalty){
			double corr = this.getCorrelationTFLength(parameter, terms);
			if (debugging){
				System.out.println("left: " + Rounding.toString(left, 4) +
						", parameter: " + Rounding.toString(parameter, 4) +
						", right: " + Rounding.toString(right, 4));
				System.out.println("leftCorr: " + Rounding.toString(leftCorr, 4) +
						", corr: " + Rounding.toString(corr, 4) +
						", rightCorr: " + Rounding.toString(rightCorr, 4));
			}
			if (Math.abs(corr-target) < error){
				return parameter;
			}
			if (right - left < error){
				return -1;
			}
			
			
			if (increasing){
				if (corr > target){
					right = parameter;
					parameter = (left+right)/2;
				}
				else{
					left = parameter;
					parameter = (left+right)/2;
				}
			}
			else{
				if (corr < target){
					right = parameter;
					parameter = (left+right)/2;
				}
				else{
					left = parameter;
					parameter = (left+right)/2;
				}
			}
			interval = Math.abs(right - left);
			
		}
		return parameter;
	}
	/**
	 * Get the parameter setting for the given term frequency and document length
	 * tuples. This does the same binary search as the method getParameter(String[], 
	 * double, double, double, boolean, double, double). The difference is that
	 * the tf and docLength tuples are pre-obtained in this method, which could
	 * cause a OutOfMemory problem.
	 * @param termFrequency The term frequency in the collection.
	 * @param tf The within-document frequency.
	 * @param docLength The document length.
	 * @param left The left boundary of the parameter values.
	 * @param right The right boundary of the parameter values.
	 * @param target The target correlation value in the binary search.
	 * @param increasing The monotonicity of the correlation function.
	 * @return The parameter value that gives the target correlation.
	 */
	public double getParameter(double termFrequency, double[] tf, double[] docLength, 
			double left, double right, double target, boolean increasing){
		double leftCorr = this.getCorrelationTFLength(termFrequency, tf, docLength, left);
		double rightCorr = this.getCorrelationTFLength(termFrequency, tf, docLength, right);
		if (debugging){
			System.out.println("leftCorr: " + Rounding.toString(leftCorr, 4) +
					", rightCorr: " + Rounding.toString(rightCorr, 4) +
					", target: " + Rounding.toString(target, 4));
		}
		if ((leftCorr>target&&rightCorr>target)||
				(leftCorr<target&&rightCorr<target)){
			return -1;
		}
		if (left < 0 || right < 0){
			return -1;
		}
		double error = 0.0005;
		double parameter = (left+right)/2;
		while (true){
			double corr = this.getCorrelationTFLength(termFrequency, tf, docLength, parameter);
			if (debugging){
				System.out.println("left: " + Rounding.toString(left, 4) +
						", parameter: " + Rounding.toString(parameter, 4) +
						", right: " + Rounding.toString(right, 4));
				System.out.println("leftCorr: " + Rounding.toString(leftCorr, 4) +
						", corr: " + Rounding.toString(corr, 4) +
						", rightCorr: " + Rounding.toString(rightCorr, 4));
			}
			if (Math.abs(corr-target) < error){
				return parameter;
			}
			if (right - left < 0.001){
				return -1;
			}
			if (increasing){
				if (corr > target){
					right = parameter;
					parameter = (left+right)/2;
				}
				else{
					left = parameter;
					parameter = (left+right)/2;
				}
			}
			else{
				if (corr < target){
					right = parameter;
					parameter = (left+right)/2;
				}
				else{
					left = parameter;
					parameter = (left+right)/2;
				}
			}
		}
	}
	/**
	 * Parameter tuning using real queries.
	 * @param taskName The name of the task. Currently, Smooth support
	 * the parameter tuning for ad-hoc (adhoc) and topic-distillation (td)
	 * tasks.
	 * @return The estimated setting.
	 */
	public double tuneRealTRECQuery(String taskName){
		return this.tune(taskName, this.extractQueryTerms());
	}
	/**
	 * Parameter tuning using sampled queries. However, it is recommended
	 * to use real queries instead.
	 * @param taskName The name of the task. Currently, Smooth support
	 * the parameter tuning for ad-hoc (adhoc) and topic-distillation (td)
	 * tasks.
	 * @param numberOfSamples The number of sampled queries.
	 * @return The estimated setting.
	 */
	public double tuneSampling(String taskName, int numberOfSamples){
		QuerySimulation simulation = new QuerySimulation(index);
		int minLength = 2;
		int maxLength = 4;
		Vector vecTerms = new Vector();
		for (int i = 0; i < numberOfSamples; i++){
			BasicQuery query = simulation.twoStepSimulation(minLength, maxLength);
			StringTokenizer stk = new StringTokenizer(query.getQueryTermString());
			while (stk.hasMoreTokens()){
				vecTerms.addElement(stk.nextToken());
			}
			System.out.println(">>>>>>>>>sample " + (i+1) + ": " + query.getQueryTermString());
		}
		return this.tune(taskName,
				(String[])vecTerms.toArray(new String[vecTerms.size()]));
	}
	/**
	 * This method tunes the tf normalisation parameters for ad-hoc and 
	 * topic-distillation tasks.
	 * @param taskName The name of the retrieval task. Currently, Smooth
	 * provides functionality of tuning parameters for ad-hoc (adhoc) and
	 * topic-distallation (tp) tasks.  
	 * @param terms The terms used to sample the collection.
	 * @return The estimated parameter setting.
	 */
	public double tune(String taskName, String[] terms){
		String methodName = this.method.getInfo();    
		System.out.println(terms.length + " terms are used in the tuning process.");
		
		double parameter = DefaultSetting.getDefaultSetting(methodName, taskName);
		double[] defaultBoundary = DefaultSetting.getDefaultBound(methodName, taskName);
		double left = defaultBoundary[0];
		double right = defaultBoundary[1];
		double leftBound = defaultBoundary[2];
		double rightBound = defaultBoundary[3];
		double target = DefaultSetting.getTargetCorrelation(methodName, taskName);;
		boolean increasing = DefaultSetting.getCorrelationFunctionBehavior(methodName, taskName);
		boolean minimise = DefaultSetting.getCorrelationFunctionProperty(methodName, taskName);
		
		System.out.println("Initialising the tuning process...");
		double[] minimisationData = this.getMinCorrelationTFLength(
				minimise, left, right, terms);
		if (minimise){
			if (increasing){
				left = minimisationData[0];
			}
			else{
				right = minimisationData[0];
			}
		}
		else{
			if (increasing)
				right = minimisationData[0];
			else
				left = minimisationData[0];
		}
					
		System.out.println("task: " + taskName +
				", normalisation method: " + methodName.substring(0, 1) +
				", target correlation: " + target);
		System.err.println("left bound: " + Rounding.toString(left, 4) +
				", right bound: " + Rounding.toString(right, 4));
		parameter = this.getParameter(
				terms, left, right, target, increasing);
		if (parameter < 0)
			parameter = DefaultSetting.getDefaultSetting(methodName, taskName);
		return parameter;
	}
	/**
	 * Get the mean correlation of term frequency with document length for the specified
	 * normalisation method and parameter setting.
	 * @param methodName The normalisation method.
	 * @param parameter The parameter setting.
	 * @param queryTerms A set of terms.
	 * @return The corresponding correlation value.
	 */
	public double getCorrelationTFLength(String methodName, double parameter, String[] queryTerms){		
		this.setNormalisationMethod(methodName.substring(0, 1));
		return this.getCorrelationTFLength(parameter, queryTerms);
	}
	/**
	 * Get the mean correlation of term frequency with document length for the specified
	 * parameter setting.
	 * @param parameter The parameter setting.
	 * @param queryTerms A set of terms.
	 * @return The corresponding correlation value.
	 */
	public double getCorrelationTFLength(double parameter, String[] queryTerms){		
		int sumOfQueryLength = queryTerms.length;
		double[] varDocLength = new double[sumOfQueryLength];
		double[] varTF = new double[sumOfQueryLength];
		double[] varSum = new double[sumOfQueryLength];
		//double[] covar = new double[sumOfQueryLength];
		double[] corr = new double[sumOfQueryLength];
		//double[] lCorr = new double[sumOfQueryLength];
		int counter = 0;
		int effCounter = 0;
		this.method.setParameter(parameter);
		for (int i = 0; i < queryTerms.length; i++){
			String term = queryTerms[i];
			
			double data = this.getCorrelationTFLength(matching, term);
			if (data == 0){
				corr[counter] = 0;
				counter++;
				continue;
			}
			corr[counter] = data;
			if (Double.isNaN(corr[counter]))
				corr[counter] = 0;
			else
				effCounter++;
			counter++;
		}	
		if (effCounter == 0)
			return Double.NaN;
		return Statistics.sum(corr)/effCounter;
	}
	/**
	 * Get the correlation of term frequency with document length for a given term.
	 * @param matching The BufferedMatching for retrieving term frequency-document
	 * length tuples.
	 * @param term The given term.
	 * @return The correlation of term frequency with document length.
	 */
	public double getCorrelationTFLength(BufferedMatching matching, String term){
		String[] terms = new String[1];
		terms[0] = term;
		
		lexicon.findTerm(term);
		double termFrequency = lexicon.getTF();
		double[] docLength = new double[lexicon.getNt()];
		double[] tf = new double[lexicon.getNt()];
		if (docLength.length == 0)
			return 0;
		matching.accessInvIndex(term, docLength, tf);
		//docLength = Statistics.splitIntoBin(docLength, BIN_NUMBER);
		double[] lengthPlusTF = new double[docLength.length];
		double[] TF = new double[docLength.length];

		for (int i = 0; i < docLength.length; i++){
			try{
				TF[i] = method.normalise(tf[i], docLength[i], termFrequency);
			}
			catch(ArrayIndexOutOfBoundsException e){
				e.printStackTrace();
				System.err.println("TF.length: " + TF.length 
						+ ", tf.length: " + tf.length
						+ ", docLength.length: " + docLength.length);
				System.exit(1);
			}
			lengthPlusTF[i] = docLength[i] + TF[i]; 
		}
		double varDocLength = Statistics.variance(docLength);
		double varTF = Statistics.variance(TF);
		double varSum = Statistics.variance(lengthPlusTF);
		double covar = (varSum - varDocLength - varTF)/2;
		/** Compute the Degroot's correlation */
		double corr = covar/(Statistics.standardDeviation(docLength)*Statistics.standardDeviation(TF));
		/** Compute the linear correlation */
		//double corr = Statistics.linearCorrelation(TF, docLength);
		if (varDocLength == 0d)
			corr = 0;
		return corr;
	}
	/**
	 * Set the field weight.
	 * @param value The weight of the field.
	 */
	public void setFieldWeight(double value){
		this.method.setFieldWeight(value);
	}
	/**
	 * Get the mean correlation of term frequency with document length for the real
	 * queries.
	 * @param methodName The name of the normalisation method.
	 * @param matching The BufferedMatching for retrieving term frequency-document
	 * length tuples.
	 * @param parameter The parameter setting.
	 * @return The mean correlation of term frequency with document length.
	 */
	public double getCorrelationTFLengthRealTRECQuery(
			String methodName, BufferedMatching matching, double parameter){
		return this.getCorrelationTFLength(methodName, parameter, this.extractQueryTerms());
	}

	/**
	 * Get the minimal mean correlation of term frequency with document length
	 * for a set of given terms.
	 * @param minimise A boolean variable indicating whether it is a minimisation or
	 * a maximisation task. True for minimisation and false for maximisation.
	 * @param terms A set of given terms.
	 * @return An array with a size of 3 which contains the parameter value
	 * providing the minimal.maximal mean correlation, the corresponding 
	 * minimal/maximal mean correlation, and a value indicating if it is
	 * a minimisation task, resectively. For the last element in the array,
	 * a negative value indicates a minimisation process and a positive value
	 * indicates a maximisation process. A zero value means that the correlation
	 * function is monotonic in the given range of parameter values.
	 */
	public double[] getMinCorrelationTFLength(boolean minimise, double left, double right, String[] terms){
		return this.minimiseCorrelationTFLength(minimise, terms, left, right);
	}

	/**
	 * Get the correlation of term frequency with document length for a single term.
	 * @param matching The BufferedMatching used for retrieving the term frequency-
	 * document length tuples.
	 * @param parameter The parameter setting.
	 * @param term The given term.
	 * @return The correlation of term frequency with document length for a the
	 * given term.
	 */
	public double getCorrelationTFLength(BufferedMatching matching, double parameter, String term){
		String[] terms = new String[1];
		terms[0] = term;
		this.method.setParameter(parameter);
		lexicon.findTerm(term);
		double termFrequency = lexicon.getTF();
		double[] docLength = new double[lexicon.getNt()];
		double[] tf = new double[lexicon.getNt()];
		if (docLength.length == 0){
			return -2;
		}
		DocumentIndex docIndex = index.getDocumentIndex();
		matching.accessInvIndex(term, docLength, tf);
		//docLength = Statistics.splitIntoBin(docLength, BIN_NUMBER);
		double[] lengthPlusTF = new double[docLength.length];
		double[] TF = new double[docLength.length];

		if (this.debugging){
			System.err.println("avl: " + avl);
		}
		for (int i = 0; i < docLength.length; i++){
			TF[i] = method.normalise(tf[i], docLength[i], termFrequency);
			lengthPlusTF[i] = docLength[i] + TF[i]; 
		}
		double varDocLength = Statistics.variance(docLength);
		double varTF = Statistics.variance(TF);
		double varSum = Statistics.variance(lengthPlusTF);
		double covar = (varSum - varDocLength - varTF)/2;
		double corr = covar/(Statistics.standardDeviation(docLength)*Statistics.standardDeviation(TF));
		if (this.debugging){
			System.err.println("varDocLength: " + varDocLength);
			System.err.println("varTF: " + varTF);
			System.err.println("varSum: " + varSum);
			System.err.println("covar: " + covar);
			System.err.println("corr: " + corr);
		}
		if (varDocLength == 0d)
			corr = 0;;
		return corr;
	}
	
}

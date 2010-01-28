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

import java.util.StringTokenizer;
import java.util.Vector;

import uk.ac.gla.terrier.matching.models.normalisation.Normalisation;
import uk.ac.gla.terrier.querying.Manager;
import uk.ac.gla.terrier.smooth.matching.BufferedMatching;
import uk.ac.gla.terrier.smooth.simulation.QuerySimulation;
import uk.ac.gla.terrier.smooth.structures.BasicQuery;
import uk.ac.gla.terrier.smooth.structures.OnelineTRECQuery;
import uk.ac.gla.terrier.statistics.Statistics;
import uk.ac.gla.terrier.structures.CollectionStatistics;
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
public class InternalCorrelationTuning extends ParameterTuning{
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
	public InternalCorrelationTuning(String methodName, Index index){
		super(index);
		this.setNormalisationMethod(methodName);
		pipe = new Manager(index);
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
		if (queries.getNumberOfQueries() == 0){
			System.err.println("Queries are not in TREC format. Loading " +
					"as plain text one-line queries...");
			queries = new OnelineTRECQuery(); 
		}
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
			double target, boolean increasing, double leftBound, double rightBound){
		double parameter = -1;
		double penalty = 0.0001;
		double interval = 0.1;
		double error = 0.0001;
		double originalLeft = left;
		double originalRight = right;
		
		double leftCorr = this.getCorrelationTFLength(left, terms);
		interval = Math.abs(leftCorr - target);
		if (interval < error)
			return left;
		while (leftCorr > target && increasing || leftCorr < target && !increasing){
			left /= 2;
			if (left < leftBound){
				System.err.println("Correlation out of bound. Suggest to use any value" +
						" that is small enough, say around " + originalLeft);
				return originalLeft;
			}
		}
		
		double rightCorr = this.getCorrelationTFLength(right, terms);
		interval = Math.abs(rightCorr - target);
		if (interval < error)
			return right;
		boolean inRange = true;
		while (rightCorr < target && increasing || rightCorr > target && !increasing){
			right *= 2;
			rightCorr = this.getCorrelationTFLength(right, terms);
			if (debugging){
				System.out.println("target: " + Rounding.toString(target, 6)
						+ ", rightCorr: " + Rounding.toString(rightCorr, 6)
						+ ", adjust right to: " + Rounding.toString(right, 2));
			}
			if (right > rightBound){
				inRange = false;
				break;
//				System.err.println("Correlation out of bound. Suggest to use any value" +
//						" that is large enough, say around " + originalRight);
//				return originalRight;
			}
		}
		if (!inRange){
			right = originalRight;
			while (rightCorr < target && increasing || rightCorr > target && !increasing){
				right /= 2;
				rightCorr = this.getCorrelationTFLength(right, terms);
				if (debugging){
					System.out.println("target: " + Rounding.toString(target, 6)
							+ ", rightCorr: " + Rounding.toString(rightCorr, 6)
							+ ", adjust right to: " + Rounding.toString(right, 2));
				}
				if (right < leftBound){
					System.err.println("Correlation out of bound. Suggest to use any value" +
							" that is large enough, say around " + originalRight);
					return originalRight;
				}
			}
			
		}
		
		
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
	
	public double tuneRealTRECQuery(String taskName){
		return this.tune(taskName, this.extractQueryTerms());
	}
	
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
	
	public double tune(String taskName, String[] terms){
		String methodName = this.method.getInfo();    
		System.out.println("number of query terms: " + terms.length);
		double interval = 0.1;
		double parameter = 0;
		double left = 0.01;
		double right = 1;
		double target = -0.0885;
		double leftBound = 0;
		double rightBound = 1;
		boolean increasing = false;
		if (methodName.startsWith("2")){
			left = 0.1;
			right = 112d;
			leftBound = 0.1;
			rightBound = 1000;
			// ad-hoc task
			if (taskName.equalsIgnoreCase("adhoc")){
				target = -0.1043;
				increasing  = true;
			}
			if (taskName.equalsIgnoreCase("d12")){
				target = -0.0884;
				increasing = true;
			}
			if (taskName.equalsIgnoreCase("d45")){
				target =  -0.0957;
				increasing = true;
			}
//			if (taskName.equalsIgnoreCase("d2g")){
//				target = -0.0861;
//				increasing = true;
//			}
			if (taskName.equalsIgnoreCase("d10g")){
				target = -0.1011;
				increasing = true;
			}
			// mixed named-page finding and homepage finding for titles
			if (taskName.equalsIgnoreCase("mixtitle")){
				target = -0.3259;
				increasing = false;
				left = 0.1;
				right = 32d;
				leftBound = 0.1;
				rightBound = 128d;
			}
//			mixed named-page finding and homepage finding for the body
			if (taskName.equalsIgnoreCase("mixbody")){
				target = -0.2670;
				increasing = true;
				left = 0.1;
				right = 64;
				leftBound = 0.1;
				rightBound = 1024d;
			}
			//	mixed named-page finding and homepage finding for anchor texts
			if (taskName.equalsIgnoreCase("mixanchor")){
				target = 0.1050;
				increasing = true;
				left = 4;
				right = 512d;
				leftBound = 0.1;
				rightBound = 1024d;
			}
			// topic-distillation task, excluding the TREC2002 topic-distillation task.
			if (taskName.equalsIgnoreCase("td")){
				target = -0.2944;
				increasing  = true;
			}
			//	field retrieval using titles for named-page finding task
			if (taskName.equalsIgnoreCase("ftnp")){
				target = -0.3796;
				increasing = false;
			}
			//	field retrieval using bodies for named-page finding task
			if (taskName.equalsIgnoreCase("fbnp")){
				left = 0.0001;
				right = 16;
				target = -0.2626;
				increasing = true;
			}
			//	field retrieval using anchor text for named-page finding task
			if (taskName.equalsIgnoreCase("fanp")){
				//target = 0.0247;
				left = 0.0001;
				right = 16;
				target = -0.0058;
				increasing = true;
			}
			//	field retrieval using titles for HOMEPAGE finding task
			if (taskName.equalsIgnoreCase("fthp")){
				target =  -0.3006;
				increasing = false;
			}
			//	field retrieval using bodies for HOMEPAGE finding task
			if (taskName.equalsIgnoreCase("fbhp")){
				target =  -0.2767;
				increasing = true;
			}
			//	field retrieval using anchor text for HOMEPAGE finding task
			if (taskName.equalsIgnoreCase("fahp")){
				target = 0.1087;
				increasing = true;
			}
		}
		if (methodName.startsWith("3")){
			left = 100;
			right = 10000d;
			target = -0.0885;
			leftBound = 1;
			rightBound = 20000d;
			increasing = true;
			if (taskName.equalsIgnoreCase("d12")){
				target = -0.0852;
			}
			if (taskName.equalsIgnoreCase("d45")){
				right = 8000;
				target = -0.0823;
			}
			if (taskName.equalsIgnoreCase("d10g")){
				target = -0.0908;
			}
		}
		
		if (methodName.startsWith("B")){
			left = 0.10;
			right = 0.99d;
			target = -0.0885;
			leftBound = 0.01;
			rightBound = 1;
			increasing = false;
			if (taskName.equalsIgnoreCase("d12")){
				target = -0.0840;
			}
			if (taskName.equalsIgnoreCase("d45")){
				target = -0.0874;
			}
			if (taskName.equalsIgnoreCase("d10g")){
				target = -0.0972;
			}
		}
		System.out.println("task: " + taskName +
				", normalisation method: " + methodName.substring(0, 1) +
				", target correlation: " + target);
		parameter = getParameter(terms, left, right, target, increasing, 
				leftBound, rightBound);
		return parameter;
	}
	public double getCorrelationTFLength(String methodName, double parameter, String[] queryTerms){		
		this.setNormalisationMethod(methodName.substring(0, 1));
		return this.getCorrelationTFLength(parameter, queryTerms);
	}
	public double getCorrelationTFLength(double parameter, String[] queryTerms){		
		int sumOfQueryLength = queryTerms.length;
		double[] varDocLength = new double[sumOfQueryLength];
		double[] varTF = new double[sumOfQueryLength];
		double[] varSum = new double[sumOfQueryLength];
		//double[] covar = new double[sumOfQueryLength];
		double[] corr = new double[sumOfQueryLength];
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
		double corr = covar/(Statistics.standardDeviation(docLength)*Statistics.standardDeviation(TF));
		if (varDocLength == 0d)
			corr = 0;;
		return corr;
	}
	
	public void setFieldWeight(double value){
		this.method.setFieldWeight(value);
	}
	
	public double getCorrelationTFLengthRealTRECQuery(
			String methodName, BufferedMatching matching, double parameter){
		return this.getCorrelationTFLength(methodName, parameter, this.extractQueryTerms());
	}
	
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

		
		for (int i = 0; i < docLength.length; i++){
			TF[i] = method.normalise(tf[i], docLength[i], termFrequency);
			lengthPlusTF[i] = docLength[i] + TF[i]; 
//			if (debugging){
//				System.err.println(tf[i] + " " + docLength[i]);
//			}
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

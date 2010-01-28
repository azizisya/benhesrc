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
 * The Original Code is Statistics.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.statistics;
import java.util.*;

/**
 * This class implements computing some simple statistical concepts as well as
 * some mathematical functionalities.
 * @author Ben He <ben{a.}dcs.gla.ac.uk>
 * @version $Revision: 1.1 $
 */
public class Statistics {
    
    /**
    * This method computes the Stirling formula for the power series.
    * @param n The parameter of the Stirling formula.
    * @param m The parameter of the Stirling formula.
    * @return the approximation of the power series
    */
//    public static double stirlingPower(double n, double m) {
//        uk.ac.gla.terrier.matching.models.Idf i = new uk.ac.gla.terrier.matching.models.Idf();
//        double dif = n - m;
//        return (m + 0.5d) * i.log(n / m) + dif * i.log(n);
//    }
    /**
     * This method computes the standard error of the mean. 
     * @param data The data.
     * @return The standard error of the mean for the given data.
     */
    public static double stdErrorOfTheMean(double[] data){
    	return standardDeviation(data) / Math.sqrt(data.length);
    }
    
    public static double[] gaussianConversion(double[] data){
    	double mean = mean(data);
    	double std = Statistics.standardDeviation(data);
    	int n = data.length;
    	for (int i=0; i<n; i++)
    		data[i] = (data[i]-mean)/std;
    	return data;
    }
    
    public static double max_entropy(double[] data){
    	double[] X = (double[])data.clone();
    	Arrays.sort(X);
    	int n=X.length;
    	for (int i=0; i<n; i++)
    		X[i] /= X[n-1];
    	double entropy = 0d;
    	for (int i=0; i<n-1; i++)
    		entropy += -X[i]*Math.log(X[i])/Math.log(2);
    	return entropy;
    }
    public static double entropy(double[] probs){
    	double entropy = 0d;
    	for (int i=0; i<probs.length; i++)
    		entropy += -probs[i]*Math.log(probs[i])/Math.log(2);
    	return entropy;
    }
    
    public static double maxEntropy(int n, int f){
    	double[] probs = new double[n];
    	Arrays.fill(probs, (double)f/n);
    	return entropy(probs);
    }
    
    public static double map_Entropy(double[] data){
    	double[] probs = mapToProbabilities(data);
    	return entropy(probs);
    }
    
    /**
     * This method creates a mapping of probabilies from values.
     * @param X The given data.
     * @return The mapped probabilities for the given data.
     */
    public static double[] mapToProbabilities(double[] X){
    	double sum = sum(X);
    	double[] Y = new double[X.length];
    	for (int i = 0; i < Y.length; i++)
    		Y[i] = X[i] / sum;
    	return Y;
    }
    //{index, value}
    public static int min(double[] data){
    	int minIndex = 0;
    	for (int i = 1; i < data.length; i++)
    		if (data[i] < data[minIndex]){
    			minIndex = i;
    		}
    	return minIndex;
    }
    
//  {index, value}
    public static int max(double[] data){
    	int maxIndex = 0;
    	for (int i = 1; i < data.length; i++)
    		if (data[i] > data[maxIndex]){
    			maxIndex = i;
    		}
    	return maxIndex;
    }
    
    public static double cosineNormalisation(double[] weights){
    	double sum = sum(weights);
    	double[] sqrWeights = new double[weights.length];
    	int n = weights.length;
    	for (int i=0; i<n; i++)
    		sqrWeights[i] = Math.pow(weights[i], 2d);
    	return sum/Math.sqrt(sum(sqrWeights));
    }
    
    public static double[] uniformSampling(double left, double right, int numberOfSamples){
    	double[] samples = new double[numberOfSamples];
    	double interval = (right-left)/(numberOfSamples-1);
    	samples[0] = left;
    	samples[numberOfSamples-1] = right;
    	for (int i = 1; i < numberOfSamples-1; i++)
    		samples[i] = samples[i-1]+interval;
    	return samples;
    }
    /**
     * This method computes the covariance for the samples of two variables.
     * @param x One of the two variables.
     * @param y One of the two variables.
     * @return The covariance.
     */
    public static double covariance(double[] x, double[] y){
    	double[] sum = new double[x.length];
    	for (int i = 0; i < x.length; i++)
    		sum[i] = x[i] + y[i];
    	return (variance(sum)-variance(x)-variance(y))/2;
    }
    /**
     * This method computes the correlation between two variables.
     * @param x One of the two variables.
     * @param y One of the two variables.
     * @return The correlation.
     */
    public static double correlation(double[] x, double[] y){
    	return covariance(x, y)/(standardDeviation(x)*standardDeviation(y));
    }
    /**
     * This method computes the base x log of the given value y.
     * @param x The base of the logarithm.
     * @param y The given value.
     * @return The base x log of y.
     */
    public static double logx(double x, double y) {
    	return Math.log(y)/Math.log(x);
    }
    /**
     * This method computes the sum of an array of integar values.
     * @param data The given data.
     * @return The sum of the given data.
     */
    public static int sum(int[] data){
    	int sum = 0;
    	for (int i = 0; i < data.length; i++)
    		sum+=data[i];
    	return sum;
    }
    /**
     * This method computes the sum of an array of double values.
     * @param data The given data.
     * @return The sum of the given data.
     */
    public static double sum(double[] data){
    	double sum = 0;
    	for (int i = 0; i < data.length; i++)
    		sum+=data[i];
    	return sum;
    }
    
    /**
     * This method computes the mean of an array of given double values.
     * @param data The given double values.
     * @return The mean of the given double values.
     */
    public static double mean(double[] data) {
		double mean = 0d;
		for (int i=0; i<data.length; i++)
			mean+=data[i];
		mean/=data.length;
		return mean;
    }
    
	/**
	 * This method computes the mean of a segment of values in a double array.
	 * @param data The double array.
	 * @param start The starting index of the segment of the values that count.
	 * @param length The length of the segment of the values that count
	 * @param ascending Indicate if the index of the segment of the array goes in
	 * ascending order. For example,  
	 * @return The mean of the segment of the values in the double array.
	 */
	public static double mean(double[] data, int start, int length, boolean ascending) {
		double mean = 0d;
		if (ascending)
			for (int i = start; i < length; i++)
				mean += data[i];
		else
			for (int i = 0; i < length; i++)
				mean += data[start - i];
		mean /= length;
		return mean;
	}
    /**
     * This method computes the mean of an array of integer values.
     * @param data The given array of integer values.
     * @return The mean of the array.
     */
    public static double mean(int[] data) {
	double mean = 0d;
	for (int i=0; i<data.length; i++)
		mean+=data[i];
	mean/=data.length;
	return mean;
    }
    
    /**
     * This method computes the median of an array of double values.
     * @param data The given double values.
     * @return The median of the array of double values.
     */
    public static double median(double[] data) {
		Arrays.sort(data);
		return data[(data.length-1)/2];
    }
    /**
     * Divide all the values in the double array by the maximum among the array.
     * @param data The given data.
     * @return The normalised data.
     */
    public static double[] normaliseMax(double[] data) {
		int n=data.length;
		double[] buffer = (double[])(data.clone());
        Arrays.sort(data);
		for (int i=0; i<n; i++){
			buffer[i]/=data[n-1];
		}
		return buffer;
    }
    /**
     * Normalise an array of double value. For each element in the array, its
     * value x is normalised by $\frac{x-min}{max-min}$. 
     * @param _data The given array.
     * @return The normalised array of data.
     */
    public static double[] normaliseMaxMin(double[] _data) {
		int n=_data.length;
		double[] data = (double[])(_data.clone());
        Arrays.sort(_data);
		for (int i=0; i<n; i++){
			data[i]=(_data[i]-_data[0])/(_data[n-1]-_data[0]);
		}
		return data;
    }

	/** This method implements the fnuctionality of spliting an array
	*	of data into a number of bins. It returns an array of the mean 
	*	value of the data in each bin.
	*	@param data double[] an array of data to be splited.
	*	@param binNum int the number of bins to generate.
	*	@return double[] an array of the mean of the data in each bin.
	*/
	public static double[] splitIntoBin(double[] data, int binNum) {
		// sort data
		Arrays.sort(data);
		int N = data.length;
		// number of units in a bin
		int inOneBin = N / binNum;
		
		int start = 0;
		int end = inOneBin;
		int index = 0;
		int binCounter = 0;
		
		double[] bin = new double[binNum];
		Arrays.fill(bin, 0);
		
		while (binCounter < binNum){
			for (int i = start; i < end; i++)
				bin[binCounter] += data[i];
			bin[binCounter] /= (end - start);
			start = end;
			end = start + inOneBin;
			binCounter++;
		}
		return bin;
	}
	/**
	 * This method computes the standard deviation of a double array.
	 * @param data The given data.
	 * @return The standard deviation of the given data.
	 */
    public static double standardDeviation(double[] data) {
	
		return Math.sqrt(variance(data));
    }
    /**
     * This method computes the variance of a given array of double values.
     * @param data The given data.
     * @return The variance of the given data.
     */
    public static double variance(double[] data) {
		double var = 0d;
		int n = data.length;
		double mean =mean(data);
		for (int i=0; i<n; i++)
			var+=(data[i]-mean)*(data[i]-mean);
		var /= n;	
		return var;
    }

}

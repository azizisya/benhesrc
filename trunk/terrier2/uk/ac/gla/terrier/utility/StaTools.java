/*
 * Terrier - Terabyte Retriever 
 * Webpage: http://ir.dcs.gla.ac.uk/terrier 
 * Contact: terrier{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * http://www.gla.ac.uk/
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
 * The Original Code is StaTools.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.utility;
import uk.ac.gla.terrier.matching.models.Idf;
import java.util.*;
/**
 * This class implements a series of basic statistical functions.
 */
public class StaTools {
	/**
    * This method provides the contract for implementing the Stirling formula for the power series.
    * @param n The parameter of the Stirling formula.
    * @param m The parameter of the Stirling formula.
    * @return the approximation of the power series
    */
    public static double stirlingPower(double n, double m) {
        double dif = n - m;
        return (m + 0.5d) * Idf.log(n / m) + dif * Idf.log(n);
    }
    
    /**
     * This method returns the standard error of the mean for an array of data.
     * @param data The sampled data.
     * @return The standard error of the mean.
     */
    public static double stdErrorOfTheMean(double[] data){
    	return standardDeviation(data) / Math.sqrt(data.length);
    }
    
    
    /**
     * The sum of an array of integers.
     * @param data The integers.
     * @return The sum.
     */
    public static int sum(int[] data){
    	int sum = 0;
    	for (int i = 0; i < data.length; i++)
    		sum+=data[i];
    	return sum;
    }
    
    /**
     * The mean of an array of double values.
     * @param data The double values.
     * @return The mean.
     */
    public static double mean(double[] data) {
		double mean = 0d;
		for (int i=0; i<data.length; i++)
			mean+=data[i];
		mean/=data.length;
		return mean;
    }
    
	/**
	 * The mean of a sub-array of an array of double values.
	 * @param data The array of double values.
	 * @param start The starting index of the sub-array.
	 * @param length The length of the sub-array.
	 * @param ascending Is the starting index the left (true) or 
	 * right (false) end of the sub-array?
	 * @return The mean of the sub-array.
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
	 * The mean of an array of integers.
	 * @param data The array of integers.
	 * @return The mean.
	 */
    public static double mean(int[] data) {
    	double mean = 0d;
    	for (int i=0; i<data.length; i++)
    		mean+=data[i];
    	mean/=data.length;
    	return mean;
    }
    
    /**
     * The median of an array of double values.
     * @param data The array of double values.
     * @return The median.
     */
    public static double median(double[] data) {
    	double[] copy = (double[])data.clone();
    	Arrays.sort(copy);
    	return data[(copy.length-1)/2];
    }
    /**
     * The standard deviation of an array of double values.
     * @param data The array of double values.
     * @return The standrad deviation.
     */
    public static double standardDeviation(double[] data) {	
		return Math.sqrt(variance(data));
    }
    /**
     * The variance of an array of double values. 
     * @param data The array of double values.
     * @return The variance.
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

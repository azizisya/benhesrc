/*
 * Created on 1 Aug 2007
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.statistics;

import gnu.trove.TIntDoubleHashMap;

import java.util.Arrays;

public class ScoreNormaliser {
	/**
	 * Smooth outliers in an array of performance measures
	 * @param baseline
	 * @param scores An array of scores. The array doesn't have to be sorted but should
	 * represent the resulting scores of a sequence of input values.
	 * @return
	 */
	public static void smoothScores(double[] scores){
		for (int i=1; i<scores.length-2; i++){
			double left = scores[i-1];
			double right = scores[i+1];
			if (scores[i]<left && scores[i]<right){
				if (left-scores[i]/left>0.4 || right-scores[i]/right>0.4){
					double rnd = (Math.random()-0.5)*0.1;
					double mean = (left+right)/2;
					scores[i] = mean*(1+rnd);
				}
			}
		}
	}
	
	public static void zScoreNormalise(double[] scores){
		double std = Statistics.standardDeviation(scores);
		double mean = Statistics.mean(scores);
		int N = scores.length;
		for (int i=0; i<N; i++)
			scores[i] = (scores[i]-mean)/std;
	}
	
	public static void mapToProbabilities(double[] scores){
		double sum = Statistics.sum(scores);
		int N = scores.length;
		for (int i=0; i<N; i++){
			scores[i] /= sum;
		}
	}
	
	public static void normalizeScoresByMax(double[] scores){
		double[] score_buf = scores.clone();
		int N = scores.length;
		Arrays.sort(score_buf);
		double max = score_buf[N-1];
		for (int i=0; i<N; i++)
			scores[i] /= max;
		score_buf = null;
	}
	
	public static void normalizeScoresByMax(TIntDoubleHashMap scoreMap){
		double[] scores = scoreMap.getValues();
		Arrays.sort(scores);
		double max = scores[scores.length-1];
		scores = null;
		for (int key : scoreMap.keys())
			scoreMap.put(key, scoreMap.get(key)/max);
	}
	
	public static void normalizeNegScoresByMax(double[] scores){
		double[] score_buf = new double[scores.length];
		int N = score_buf.length;
		for (int i=0; i<N; i++)
			score_buf[i] = Math.abs(scores[i]);
		Arrays.sort(score_buf);
		double max = score_buf[N-1];
		for (int i=0; i<N; i++)
			scores[i] /= max;
		score_buf = null;
	}
}

/*
 * Created on 1 Aug 2007
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.statistics;

import java.util.Arrays;

public class ScoreNormaliser {
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

package org.terrier.matching.models.proximity;

import java.util.Arrays;

public class NgramCountingThread extends Thread {
	protected NGramCounter broker;
	
	protected int[][] postings1;
	
	protected int[][] postings2;
	
	protected int docid;
	
	protected ProximityModel proxModel;
	
	protected int wSize;
	
	protected int docLength;
	
	public double matchingNGram;
	
	
	public NgramCountingThread(NGramCounter broker, int[][] postings1, int[][] postings2,
			int docid, ProximityModel proxModel, int wSize, int docLength){
		this.broker = broker;
		this.postings1 = postings1;
		this.postings2 = postings2;
		this.docid = docid;
		this.proxModel = proxModel;
		this.wSize = wSize;
		this.docLength = docLength;
	}
	
	public void run(){
		this.matchingNGram = this.count();
		broker.threadFinished(docid, matchingNGram);
	}
	
	protected double count(){
	 	
		int pos1 = Arrays.binarySearch(postings1[0], docid);
		int pos2 = Arrays.binarySearch(postings2[0], docid);
		if (pos1<0 || pos2<0)
		// if (!bgDocidSet.contains(docid))
			return 0d;
		//processed++;
		int postings1Length = postings1[0].length;
		int postings2Length = postings2[0].length;
		
		//find the places where the terms co-occur closely together
		int start1 = postings1[3][pos1];
		int end1 = pos1==postings1Length-1 ? postings1[4].length : (postings1[3][pos1+1]);
	
		int start2 = postings2[3][pos2];
			int end2 = pos2==postings2Length-1 ? postings2[4].length : postings2[3][pos2+1];

	
		return proxModel.getNGramFrequency(postings1[4], start1, end1, postings2[4], start2, end2, wSize, docLength);
	}
}

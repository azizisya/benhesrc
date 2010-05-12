package org.terrier.matching.models.proximity;

import org.terrier.utility.Distance;

public class Sigmoid extends ProximityModel {

	@Override
	public double getNGramFrequency(int[] blocksOfTerm1, int start1, int end1,
			int[] blocksOfTerm2, int start2, int end2, int wSize, int docLength) {
		// TODO Auto-generated method stub
		return Distance.bigramFrequency(blocksOfTerm1, start1, end1, blocksOfTerm2, start2, end2, wSize);
	}
	
	public double getNGramFrequencyOrdered(int[] blocksOfTerm1, int start1, int end1,
			int[] blocksOfTerm2, int start2, int end2, int wSize, int docLength) {
		//TODO: to implement
		return -1;
	}

}

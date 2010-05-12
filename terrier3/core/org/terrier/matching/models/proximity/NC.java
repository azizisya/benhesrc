package org.terrier.matching.models.proximity;

import org.terrier.utility.Distance;

public class NC extends ProximityModel {

	@Override
	public double getNGramFrequency(int[] blocksOfTerm1, int start1, int end1,
			int[] blocksOfTerm2, int start2, int end2, int wSize, int docLength) {
		double nf = Distance.noTimes(blocksOfTerm1, start1, end1, blocksOfTerm2, start2, end2, wSize, docLength);
		return nf;
	}
	
	public double getNGramFrequencyOrdered(int[] blocksOfTerm1, int start1, int end1,
			int[] blocksOfTerm2, int start2, int end2, int wSize, int docLength) {
		double nf = Distance./*noTimesOrdered*/noTimesSameOrder(blocksOfTerm1, start1, end1, blocksOfTerm2, start2, end2, wSize, docLength);
		return nf;
	}

}

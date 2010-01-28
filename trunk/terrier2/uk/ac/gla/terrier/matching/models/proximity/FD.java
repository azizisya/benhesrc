package uk.ac.gla.terrier.matching.models.proximity;

import uk.ac.gla.terrier.utility.Distance;

public class FD extends ProximityModel {

	@Override
	public double getNGramFrequency(int[] blocksOfTerm1, int start1, int end1,
			int[] blocksOfTerm2, int start2, int end2, int wSize, int docLength) {
		return Distance.noTimes(blocksOfTerm1, start1, end1, blocksOfTerm2, start2, end2, wSize, docLength);
	}

}

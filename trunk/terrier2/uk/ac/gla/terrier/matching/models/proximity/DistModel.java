package uk.ac.gla.terrier.matching.models.proximity;

import uk.ac.gla.terrier.utility.Distance;

public class DistModel extends ProximityModel {

	@Override
	public double getNGramFrequency(int[] blocksOfTerm1, int start1, int end1,
			int[] blocksOfTerm2, int start2, int end2, int wSize, int docLength) {
		if( blocksOfTerm1 == null){
			return 0;
		}
		
		if(blocksOfTerm2 == null){
			return 0;
		}
		double nGf = 0d;
		for (int i=start2; i<end2; i++){
			int minDist = Distance.getMinDist(blocksOfTerm2[i], blocksOfTerm1, start1, end1, wSize);
			if (minDist>=0)
				nGf += this.getProbability(wSize-minDist-1);
		}
		return nGf;
	}
		
	@Override
	public double getNGramFrequencyOrdered(int[] blocksOfTerm1, int start1, int end1,
			int[] blocksOfTerm2, int start2, int end2, int wSize, int docLength) {
		if( blocksOfTerm1 == null){
			return 0;
		}
		
		if(blocksOfTerm2 == null){
			return 0;
		}
		double nGf = 0d;
		for (int i=start2; i<end2; i++){
				int minDist = Distance.getMinDistOrdered(blocksOfTerm2[i], blocksOfTerm1, start1, end1, wSize);
				if (minDist>=0)
					nGf += this.getProbability(wSize-minDist-1);
		}
		return nGf;
	}
	
	protected double getProbability(int minDist){
		return 1;
	}

}

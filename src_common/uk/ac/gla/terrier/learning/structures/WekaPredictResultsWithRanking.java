package uk.ac.gla.terrier.learning.structures;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.StringTokenizer;

import uk.ac.gla.terrier.learning.structures.WekaPredictResults.Entry;
import uk.ac.gla.terrier.utility.Files;
import gnu.trove.THashMap;
import gnu.trove.TIntObjectHashMap;

public class WekaPredictResultsWithRanking extends WekaPredictResults{
	
	public WekaPredictResultsWithRanking(String predictFilename, String idMapFilename, String qid, int[] rankDocids){
		super(predictFilename, idMapFilename);
		this.setRanks(qid, rankDocids);
	}
	
	private void setRanks(String qid, int[] rankDocids){
		for (int i=0; i<rankDocids.length; i++){
			String key = qid+"."+rankDocids[i];
			Entry entry = entryMap.get(key);
			if (entry!=null)
				this.entryMap.get(key).setRank(i);
		}
	}
	
	
}

package org.terrier.learning;

import gnu.trove.TIntHashSet;
import java.util.ArrayList;

import org.terrier.evaluation.TRECQrelsInMemory;
public abstract class RFTrainer {

	/**
	 * 
	 * @param fServer
	 * @param methodName
	 * @param args
	 * @param qrels
	 */
	public RFTrainer(String methodName, String args, TRECQrelsInMemory qrelsFilename){}
	
	public abstract ArrayList<int[]> run(String qid, int[] posDocids, int[] negDocids, int[] retDocids, 
			TIntHashSet posCandSet, String outputFolder);
	
}

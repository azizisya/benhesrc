package uk.ac.gla.terrier.structures;

import java.util.ArrayList;

import org.apache.log4j.Logger;

public class BlockIndexDocument {
	protected static Logger logger = Logger.getRootLogger();
	
	protected Index index;
	
	protected int docid;
	
	public static TextWindow[] segmentDocument(Index index, int docid, int wSize){
		ArrayList<TextWindow> windows = new ArrayList<TextWindow>();
		// get document pointers
		int[][] pointers = index.getDirectIndex().getTerms(docid);
		if (wSize >= pointers[4].length){
			logger.debug("Window size larger than document length. Ignore docid "+docid);
			// TODO: should return the document
			return null;
		}
		// map from positions to termids
		int[] sortedTermids = new int[pointers[4].length];
		int blockid = 0;
		for (int i=0; i<pointers[0].length; i++){
			int blockFreq = pointers[3][i];
			for (int j=0; j<blockFreq; j++){
				sortedTermids[pointers[4][blockid++]] = pointers[0][i];
			}
		}
		// initialize each window
		int[] termids = pointers[0];
		
		return (TextWindow[])windows.toArray(new TextWindow[windows.size()]);
	}
}

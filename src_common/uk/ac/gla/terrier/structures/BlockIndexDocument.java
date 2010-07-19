package uk.ac.gla.terrier.structures;

import gnu.trove.THashMap;

import java.util.ArrayList;

import org.apache.log4j.Logger;

public class BlockIndexDocument {
	protected static Logger logger = Logger.getRootLogger();
	
	protected Index index;
	
	protected int docid;
	
	public static TextWindow[] segmentDocument(Index index, int docid, int wSize){
		ArrayList<TextWindow> txtWindowList = new ArrayList<TextWindow>();
		// get document pointers
		int[][] pointers = index.getDirectIndex().getTerms(docid);
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
		
		if (wSize >= pointers[4].length){
			// logger.debug("Window size larger than document length. Ignore docid "+docid);
			TextWindow window = new TextWindow(docid, 0, wSize);
			for (int i=0; i<sortedTermids.length; i++)
				window.addTermOccurrence(sortedTermids[i], i);
			final TextWindow[] windows = {window};
			return windows;
		}
		
		int noBlocks = sortedTermids.length;
		boolean lastWindow = false;
		for (int i=0; i<noBlocks; i++){
			if (i+wSize >= noBlocks)
				lastWindow = true;
			int startBlock = (lastWindow)?(noBlocks-wSize):(i);
			int endBlock = startBlock+wSize;
			String windowId = docid+"-"+startBlock+"-"+endBlock;
			TextWindow window = new TextWindow(docid, startBlock, endBlock);
			for (int j=startBlock; j<endBlock; j++){
				window.addTermOccurrence(sortedTermids[j], j);
			}
			txtWindowList.add(window);
			if (lastWindow)
				break;
			i=endBlock;
		}
		
		return (TextWindow[])txtWindowList.toArray(new TextWindow[txtWindowList.size()]);
	}
	
	/*public static THashMap<String, TextWindow> segmentDocument(Index index, int docid, int wSize){
		THashMap<String, TextWindow> txtWindowMap = new THashMap<String, TextWindow>();
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
		
		int noBlocks = sortedTermids.length;
		boolean lastWindow = false;
		for (int i=0; i<noBlocks; i++){
			if (i+wSize >= noBlocks)
				lastWindow = true;
			int startBlock = (lastWindow)?(noBlocks-wSize):(i);
			int endBlock = startBlock+wSize;
			String windowId = docid+"-"+startBlock+"-"+endBlock;
			TextWindow window = new TextWindow(docid, startBlock, endBlock);
			for (int j=startBlock; j<endBlock; j++){
				window.addTermOccurrence(sortedTermids[j]);
			}
			txtWindowMap.put(windowId, window);
			if (lastWindow)
				break;
			i=endBlock;
		}
		
		return txtWindowMap;
	}*/
}

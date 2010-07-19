package org.terrier.structures;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public class BlockIndexDocument {
	protected static Logger logger = Logger.getRootLogger();
	
	protected Index index;
	
	protected int docid;
	
	public BlockIndexDocument(Index index, int docid){
		this.index = index;
		this.docid = docid;
	}
	
	public TIntIntHashMap getTermidFreqMap(){
		int[][] pointers = null;
		try{
			pointers = index.getDirectIndex().getTerms(docid);
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
		TIntIntHashMap map = new TIntIntHashMap();
		for (int i=0; i<pointers[0].length; i++)
			map.put(pointers[0][i], pointers[1][i]);
		return map;
	}
	
	/**
	 * Get the terms that are no further than wSize tokens from any of the query terms.
	 * @param queryTermids
	 * @param wSize
	 * @return
	 */
	public int[] getProximityTerms(int[] queryTermids, int wSize){
		int[][] pointers = null;
		try{
			pointers = index.getDirectIndex().getTerms(docid);
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
		// map from positions to termids
		int[] sortedTermids = mapFromPositions2Termids(pointers);
		TIntHashSet termidSet = new TIntHashSet();
		TIntHashSet queryTermidSet = new TIntHashSet();
		queryTermidSet.addAll(queryTermids);
		
		for (int i=0; i<sortedTermids.length; i++){
			if (queryTermidSet.contains(sortedTermids[i])){
				int left = Math.max(0, i-wSize+1);
				for (int j=left; j<i; j++)
					if (!queryTermidSet.contains(sortedTermids[j]))
						termidSet.add(sortedTermids[j]);
				int right = Math.min(sortedTermids.length, i+wSize);
				for (int j=i+1; j<right; j++)
					if (!queryTermidSet.contains(sortedTermids[j]))
						termidSet.add(sortedTermids[j]);
			}
		}
		
		queryTermidSet.clear(); queryTermidSet = null;
		return termidSet.toArray();
	}
	
	protected static int[] mapFromPositions2Termids(int[][] pointers){
		int[] sortedTermids = new int[pointers[4].length];
		int blockid = 0;
		for (int i=0; i<pointers[0].length; i++){
			int blockFreq = pointers[3][i];
			for (int j=0; j<blockFreq; j++){
				sortedTermids[pointers[4][blockid++]] = pointers[0][i];
			}
		}
		return sortedTermids;
	}
	
	public static TextWindow[] segmentDocument(Index index, int docid, int wSize){
		ArrayList<TextWindow> txtWindowList = new ArrayList<TextWindow>();
		// get document pointers
		int[][] pointers = null;
		try{
			pointers = index.getDirectIndex().getTerms(docid);
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
		// map from positions to termids
		int[] sortedTermids = mapFromPositions2Termids(pointers);
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
	/**
	 * Segment a document into overlapped windows.
	 * @param index
	 * @param docid
	 * @param wSize
	 * @param overlap A value within [0, 1] to indicates the percentage of overlapping between adjacent windows.
	 * @return
	 */
	public static TextWindow[] segmentDocumentIntoOverlappedWindows(Index index, int docid, int wSize, double overlap){
		ArrayList<TextWindow> txtWindowList = new ArrayList<TextWindow>();
		// get document pointers
		int[][] pointers = null;
		try{
			pointers = index.getDirectIndex().getTerms(docid);
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
		// map from positions to termids
		int[] sortedTermids = mapFromPositions2Termids(pointers);
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
		int overlapSize = (int)(wSize*overlap);
		for (int i=0; i<noBlocks; i++){
			if (i+wSize >= noBlocks)
				lastWindow = true;
			int startBlock = (lastWindow)?(noBlocks-wSize):(i);
			int endBlock = startBlock+wSize;
			// String windowId = docid+"-"+startBlock+"-"+endBlock;
			TextWindow window = new TextWindow(docid, startBlock, endBlock);
			for (int j=startBlock; j<endBlock; j++){
				window.addTermOccurrence(sortedTermids[j], j);
			}
			txtWindowList.add(window);
			if (lastWindow)
				break;
			// i=endBlock;
			i = (int)(endBlock-overlapSize);
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

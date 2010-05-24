package org.terrier.structures;

import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;

public class TextWindow implements Comparable<TextWindow>{
	public TIntIntHashMap termidFreqMap;
	
	public TIntDoubleHashMap termidWeightMap;
	/**
	 * Termids sorted by position.
	 */
	protected int[] termids;
	
	protected double windowScore;
	
	public double getWindowScore() {
		return windowScore;
	}

	public void setWindowScore(double windowScore) {
		this.windowScore = windowScore;
	}

	public int docid;
	
	public int startBlock;
	
	public int endBlock;
	
	/**
	 * A text window starts from startBlock and ends at endBlock-1. Window size equals
	 * to endBlock - startBlock.
	 * @param docid
	 * @param startBlock
	 * @param endBlock 
	 */
	public TextWindow(int docid, int startBlock, int endBlock){
		this.docid = docid;
		this.startBlock = startBlock;
		this.endBlock = endBlock;
		this.termidFreqMap = new TIntIntHashMap();
		this.termidWeightMap = new TIntDoubleHashMap();
		this.termids = new int[this.endBlock-this.startBlock];
	}
	
	public void setTermPosition(int block, int termid){
		termids[block-startBlock] = termid;
	}
	
	public int getTermByPosition(int block){
		if (block<startBlock || block >= endBlock)
			return -1;
		return termids[block-startBlock];
	}
	
	public String getWindowId(){
		return docid+"-"+startBlock+"-"+endBlock;
	}
	
	public int getWindowSize(){
		return this.endBlock - this.startBlock;
	}
	
	public void addTermWeight(int termid, double weight){
		this.termidWeightMap.put(termid, weight);
	}
	
	public void addTermOccurrence(int termid, int block){
		this.termidFreqMap.adjustOrPutValue(termid, 1, 1);
		this.setTermPosition(block, termid);
	}
	
	public double getTermWeight(int termid){
		return this.termidWeightMap.get(termid);
	}
	
	public int getTermFrequency(int termid){
		return this.termidFreqMap.get(termid);
	}
	
	public static TIntIntHashMap[] mergeTextWindowFreq(TextWindow[] windows){
		TIntObjectHashMap<TIntHashSet> docidWindowMap = new TIntObjectHashMap<TIntHashSet>();
		for (int i=0; i<windows.length; i++){
			if (docidWindowMap.containsKey(windows[i].docid)){
				docidWindowMap.get(windows[i].docid).add(i);
			}else{
				TIntHashSet windowSet = new TIntHashSet();
				windowSet.add(i);
				docidWindowMap.put(windows[i].docid, windowSet);
			}
		}
		TIntIntHashMap[] termidFreqMaps = new TIntIntHashMap[docidWindowMap.keys().length];
		final int[] docids = docidWindowMap.keys();
		for (int i=0; i<docids.length; i++){
			final int docid = docids[i];
			int[] indice = docidWindowMap.get(docid).toArray();
			TextWindow[] windowsInDoc = new TextWindow[indice.length];
			for (int j=0; j<indice.length; j++)
				windowsInDoc[j] = windows[indice[j]];
			termidFreqMaps[i] = mergeTextWindowFreqInOneDocument(windowsInDoc);
		}
		return termidFreqMaps;
	}
	
	/**
	 * Merge termid frequency maps of different text windows in the same document.
	 * @param windows
	 * @return
	 */
	public static TIntIntHashMap mergeTextWindowFreqInOneDocument(TextWindow[] windows){
		int startBlock = 0;
		int endBlock = 0;
		for (TextWindow window : windows){
			startBlock = Math.min(window.startBlock, startBlock);
			endBlock = Math.max(window.endBlock, endBlock);
		}
		TIntIntHashMap map = new TIntIntHashMap();
		for (int i=startBlock; i<endBlock; i++){
			for (TextWindow window : windows){
				final int termid = window.getTermByPosition(i);
				if (termid >= 0){
					map.adjustOrPutValue(termid, 1, 1);
					break;
				}
			}
		}
		return map;
	}
	
	public int compareTo(TextWindow o) {
		if (o.getWindowScore() > this.getWindowScore()) {
			return 1;
		}
		else if (o.getWindowScore() < this.getWindowScore()) {
			return -1;
		}
		else {
			return 0;
		}
	}
}

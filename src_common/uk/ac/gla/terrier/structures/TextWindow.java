package uk.ac.gla.terrier.structures;

import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntIntHashMap;

public class TextWindow {
	public TIntIntHashMap termidFreqMap;
	
	public TIntDoubleHashMap termidWeightMap;
	
	public int docid;
	
	public int startBlock;
	
	public int endBlock;
	
	public TextWindow(int docid, int startBlock, int endBlock){
		this.docid = docid;
		this.startBlock = startBlock;
		this.endBlock = endBlock;
		this.termidFreqMap = new TIntIntHashMap();
		this.termidWeightMap = new TIntDoubleHashMap();
	}
	
	public int getWindowSize(){
		return this.endBlock - this.startBlock + 1;
	}
	
	public void addTermWeight(int termid, double weight){
		this.termidWeightMap.put(termid, weight);
	}
	
	public void addTermOccurrence(int termid){
		this.termidFreqMap.adjustOrPutValue(termid, 1, 1);
	}
	
	public double getTermWeight(int termid){
		return this.termidWeightMap.get(termid);
	}
	
	public int getTermFrequency(int termid){
		return this.termidFreqMap.get(termid);
	}
}

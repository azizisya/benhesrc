package org.terrier.matching.models.proximity;

import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;
import org.terrier.structures.Index;

public class NGramCounter {
	protected Index index;
	
	protected int threadRunning;
	
	protected int threadFinished;
	
	protected int maxThreads = 5;
	
	protected TIntObjectHashMap<NgramCountingThread> threadMap;
	
	protected int wSize;
	
	protected TIntDoubleHashMap ngramFrequencies = new TIntDoubleHashMap();
	
	protected int ngramDocumentFrequency = 0;
	protected double ngramCollectionFrequency = 0;
	
	public void count(int[][] postings1, int[][] postings2, int wSize, 
			TIntIntHashMap docidLengthMap, ProximityModel proxModel){
		this.threadMap = new TIntObjectHashMap<NgramCountingThread>();
		int i = 0;
		for (int docid : docidLengthMap.keys()){
			if (threadMap.size()<this.maxThreads){
				NgramCountingThread thread = new NgramCountingThread(this, postings1, postings2,
						docid, proxModel, wSize, docidLengthMap.get(docid));
				threadMap.put(docid, thread);
				thread.start();
			}else
				try{
					Thread.sleep(10);
				}catch(Exception e){
					e.printStackTrace();
					System.exit(1);
				}
			i++;
		}
		
		this.threadFinished = 0;
	}
	
	public int getNgramDocumentFrequency() {
		return ngramDocumentFrequency;
	}

	public double getNgramCollectionFrequency() {
		return ngramCollectionFrequency;
	}
	
	public TIntDoubleHashMap getNGramFrequencies(){
		return this.ngramFrequencies;
	}

	public synchronized void threadFinished(int threadId, double ngramFreq){
		if (ngramFreq>0d){
			this.ngramCollectionFrequency++;
			this.ngramCollectionFrequency+=ngramFreq;
		}
		this.ngramFrequencies.put(threadId, ngramFreq);
		NgramCountingThread thread = threadMap.get(threadId);
		thread=null;
		threadMap.remove(threadId);
	}
}

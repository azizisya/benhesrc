package org.terrier.documentfeature;

public abstract class DocumentFeatureServer {
	public abstract int[] getDocids(String qid);
	
	public abstract double[] getAllFeatures(String qid, int docid);
	
	public abstract double[] getOddFeatures(String qid, int docid);
	
	public abstract double[] getEvenFeatures(String qid, int docid);
	
}

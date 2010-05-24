package org.terrier.documentfeature;

import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntObjectHashMap;

public interface DocumentFeatureExtractor {
	/**
	 * Extract document features that are stored in a given object.
	 * @param docid
	 * @param docFeature
	 */
	public void extractDocumentFeature(int docid, String queryid, int[] queryTermids, TIntObjectHashMap featureMap);
	
	public void formatDocumentFeature(int docid, String qid, TIntObjectHashMap featureMap, StringBuffer buf, int label);
	
	public void extractAndWriteDocumentFeature(int[] docids, int[] labels, String queryid, int[] queryTermids, String outputFilename);
	
	public void extractAndWriteDocumentFeature(String qrelsFilename, String queryid, String outputFilename);
}

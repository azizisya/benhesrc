package org.terrier.documentfeature;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

import org.terrier.structures.Index;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.Files;

import gnu.trove.THashMap;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntObjectHashMap;
import org.terrier.evaluation.TRECResultsInMemory;

public class RelevanceScoreFeature extends DocumentFeature{
	
	/**
	 * Map from qid to a map that maps from docid to feature value.
	 */
	protected THashMap<String, TIntObjectHashMap<Double>> cache = new THashMap<String, TIntObjectHashMap<Double>>();

	protected TRECResultsInMemory results;
	
	public RelevanceScoreFeature(Index index){
		super(index);
		File fCache = new File(this.cacheFolder+ApplicationSetup.FILE_SEPARATOR+this.getInfo());
		this.CACHED = fCache.exists();
		if (CACHED)
			this.loadCache(fCache);
		else
			results  = 
				new TRECResultsInMemory(
						ApplicationSetup.getProperty("doc.feature.result.filename", 
						"/users/grad/ben/tr.ben/uniworkspace/etc/gov2/svm_train/feedbackDocs/Baseline/DPH_0.res"));
		this.featureId = 3;
	}
	
	public String getInfo(){
		return "RelevanceScoreFeature";
	}
	
	/**
	 * Each line contains:
	 * docid qid value
	 */
	protected void loadCache(File fCache){
		try{
			BufferedReader br = Files.openFileReader(fCache);
			String line = null;
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				int docid = Integer.parseInt(tokens[0]);
				String queryid = tokens[1];
				double value = Double.parseDouble(tokens[2]);
				// int label = Integer.parseInt(tokens[3]);
				if (!cache.containsKey(queryid)){
					TIntObjectHashMap<Double> map = new TIntObjectHashMap<Double>();
					map.put(docid, value);
					cache.put(queryid, map);
				}else{
					cache.get(queryid).put(docid, value);
				}					
			}
			br.close();
		}catch(IOException e){
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void loadMetaData(){
		// No meta data to load.
	}
	
	// extract the relevance scores of the given document in the result file for the given docid.
	public void extractDocumentFeature(int docid, String queryid,
			int[] queryTermids, TIntObjectHashMap featureMap) {
		if (CACHED){
			TIntObjectHashMap<Double> map = this.cache.get(queryid);
			if (map!=null)
				featureMap.put(featureId, map.get(docid));
		}else{
			double score = results.getScore(queryid, ""+docid);
			featureMap.put(featureId, score);
		}
	}
	
	public static void main(String[] args){
		// --preprocess indexpath indexprefix qrelsname queryid outputname
		if (args[0].equals("--preprocess")){
			String indexpath = args[1];
			String indexprefix = args[2];
			String qrelsFilename = args[3];
			String queryid = args[4];
			String outputFilename = args[5];
			RelevanceScoreFeature app = new RelevanceScoreFeature(Index.createIndex(indexpath, indexprefix));
			app.preProcess(qrelsFilename, queryid, outputFilename, app.featureId);
		}else if (args[0].equals("--preprocessall")){
			String indexpath = args[1];
			String indexprefix = args[2];
			String qrelsFilename = args[3];
			// String outputFilename = args[4];
			RelevanceScoreFeature app = new RelevanceScoreFeature(Index.createIndex(indexpath, indexprefix));
			app.preProcessAll(qrelsFilename, app.cacheFolder+ApplicationSetup.FILE_SEPARATOR+app.getInfo(), app.featureId);
		}
	}
}

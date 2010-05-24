package org.terrier.documentfeature;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

import org.terrier.structures.Index;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.BlockDocumentSetEntropy;
import org.terrier.utility.Files;

import gnu.trove.THashMap;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntObjectHashMap;
/**
 * Entropy of original query term's distribution in the feedback documents.
 * @author ben
 *
 */
public class EntropyFeature extends DocumentFeature {
	public int featureId;
	
	/**
	 * Map from qid to a map that maps from docid to feature value.
	 */
	protected THashMap<String, TIntObjectHashMap<Double>> cache = new THashMap<String, TIntObjectHashMap<Double>>();
	
	protected BlockDocumentSetEntropy app;
	
	/**
	 * Not used anyway since Laplace smoothing is applied.
	 */
	protected double alpha = 1;
	
	public String getInfo(){
		return "EntropyFeature";
	}
	
	public void loadMetaData(){
		// No meta data to load.
	}
	
	public EntropyFeature(Index index){
		super(index);
		File fCache = new File(this.cacheFolder+ApplicationSetup.FILE_SEPARATOR+this.getInfo());
		this.CACHED = fCache.exists();
		if (CACHED)
			this.loadCache(fCache);
		else
			app = new BlockDocumentSetEntropy(index);
		this.featureId = 6;
	}

	@Override
	public void extractDocumentFeature(int docid, String queryid,
			int[] queryTermids, TIntObjectHashMap featureMap) {
		if (CACHED){
			TIntObjectHashMap<Double> map = this.cache.get(queryid);
			if (map!=null)
				featureMap.put(featureId, map.get(docid));
		}else{
			double entropy = app.getMeanEntropy(queryTermids, docid, alpha);
			featureMap.put(featureId, entropy);
			System.err.println("featureid: "+featureId+", entropy: "+entropy);
		}
	}
	
	/**
	 * Each line contains:
	 * docid qid value label
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
	
	public static void main(String[] args){
		// --preprocess indexpath indexprefix qrelsname queryid outputname
		if (args[0].equals("--preprocess")){
			String indexpath = args[1];
			String indexprefix = args[2];
			String qrelsFilename = args[3];
			String queryid = args[4];
			String outputFilename = args[5];
			EntropyFeature app = new EntropyFeature(Index.createIndex(indexpath, indexprefix));
			app.preProcess(qrelsFilename, queryid, outputFilename, app.featureId);
		}else if (args[0].equals("--preprocessall")){
			String indexpath = args[1];
			String indexprefix = args[2];
			String qrelsFilename = args[3];
			// String outputFilename = args[4];
			EntropyFeature app = new EntropyFeature(Index.createIndex(indexpath, indexprefix));
			app.preProcessAll(qrelsFilename, app.cacheFolder+ApplicationSetup.FILE_SEPARATOR+app.getInfo(), app.featureId);
		}
	}

}

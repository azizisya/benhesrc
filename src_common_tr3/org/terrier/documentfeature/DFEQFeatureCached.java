package org.terrier.documentfeature;

import java.io.BufferedReader;
import java.io.IOException;

import org.terrier.structures.Index;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.Files;

import gnu.trove.THashMap;
import gnu.trove.TIntDoubleHashMap;

/**
 * Use cached DFEQ values instead of computing them online. 
 * @author ben
 *
 */
public class DFEQFeatureCached extends DFEQFeature {
	
	protected THashMap<String, TIntDoubleHashMap> cache = new THashMap<String, TIntDoubleHashMap>();
	
	public DFEQFeatureCached(Index index){
		super(index);
		this.loadCache(ApplicationSetup.getProperty("doc.feature.DFEQ.cache", ""));
	}
	
	public void extractDocumentFeature(int docid, String queryid, int[] queryTermids, TIntDoubleHashMap featureMap) {
		if (docid!=-1){
			TIntDoubleHashMap map = cache.get(queryid);
			if (map!=null)
				featureMap.put(featureId, map.get(docid));
		}
	}
	
	private void loadCache(String filename){
		try{
			BufferedReader br = Files.openFileReader(filename);
			String line = null;
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				int docid = Integer.parseInt(tokens[0]);
				String queryid = tokens[1];
				double value = Double.parseDouble(tokens[2]);
				// int label = Integer.parseInt(tokens[3]);
				if (!cache.containsKey(queryid)){
					TIntDoubleHashMap map = new TIntDoubleHashMap();
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
}

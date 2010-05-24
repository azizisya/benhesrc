/**
 * 
 */
package org.terrier.documentfeature;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

import org.terrier.structures.Index;
import org.terrier.structures.SingleLineTRECQuery;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.Files;

import gnu.trove.THashMap;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectDoubleHashMap;
import uk.ac.gla.terrier.statistics.CosineSimilarity;

/**
 * This class implements the per-doc similarity, the similarity of the most informative
 * terms in a feedback document to those in the whole feedback document set. The most 
 * informative terms are stored on disk. 
 * @author ben
 *
 */
public class PSIFeature extends DocumentFeature {
	
	protected String expTermFilename;
	
	protected int expTerms;
	
	/**
	 * Map from qid to a map that maps from docid to feature value.
	 */
	protected THashMap<String, TIntObjectHashMap<Double>> cache;
	
	/**
	 * THashMap<qid -> THashMap>, TIntObjectHashMap<docid -> TIntDoubleHashMap>, termid -> weight
	 */
	protected THashMap<String,TIntObjectHashMap<TIntDoubleHashMap>> entries;
	/**
	 * Entries of expansion terms from the whole feedback document set.
	 */
	protected THashMap<String, TIntDoubleHashMap> allDocsEntries;

	/**
	 * @param index
	 */
	public PSIFeature(Index index) {
		super(index);
		File fCache = new File(this.cacheFolder+ApplicationSetup.FILE_SEPARATOR+this.getInfo());
		this.CACHED = fCache.exists();
		this.featureId = 5;
		if (!CACHED)
		{
			this.expTermFilename = ApplicationSetup.getProperty("doc.feature.expterm.filename", "Full path must be given");
			this.expTerms = Integer.parseInt(ApplicationSetup.getProperty("expansion.terms", "10"));
			this.loadMetaData();
		}
	}

	/* (non-Javadoc)
	 * @see uk.ac.gla.terrier.documentfeature.features.DocumentFeature#extractDocumentFeature(int, java.lang.String, int[], gnu.trove.TIntDoubleHashMap)
	 */
	@Override
	public void extractDocumentFeature(int docid, String queryid,
			int[] queryTermids, TIntObjectHashMap featureMap) {
		if (CACHED){
			if (cache==null){
				cache = new THashMap<String, TIntObjectHashMap<Double>>();
				this.loadCache(new File(this.cacheFolder+ApplicationSetup.FILE_SEPARATOR+this.getInfo()));
			}
				
			TIntObjectHashMap<Double> map = this.cache.get(queryid);
			if (map!=null)
				featureMap.put(featureId, map.get(docid));
		}else{
			TIntDoubleHashMap globalExpTerms = allDocsEntries.get(queryid);
			TIntDoubleHashMap localExpTerms = entries.get(queryid).get(docid);
			double value = CosineSimilarity.cosine(globalExpTerms, localExpTerms);
			featureMap.put(featureId, value);
		}
	}

	/* (non-Javadoc)
	 * @see uk.ac.gla.terrier.documentfeature.features.DocumentFeature#getInfo()
	 */
	@Override
	public String getInfo() {
		return "PSIFeature";
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

	/* (non-Javadoc)
	 * @see uk.ac.gla.terrier.documentfeature.features.DocumentFeature#loadMetaData()
	 */
	@Override
	protected void loadMetaData() {
		// Load expansion terms from log file.
		entries = new THashMap<String, TIntObjectHashMap<TIntDoubleHashMap>>();
		allDocsEntries = new THashMap<String, TIntDoubleHashMap>();
		try{
			BufferedReader br = Files.openFileReader(this.expTermFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				String[] segments = line.split(":");
				String[] tokens = segments[0].split(" ");
				String qid = tokens[0];
				/*String docidString = "";
				for (int i=1;i<tokens.length;i++)
					docidString = docidString.concat(tokens[i]);*/
				TObjectDoubleHashMap<String> termWeightMap = SingleLineTRECQuery.parseQueryStringWithWeights(segments[1]);
				String[] terms = termWeightMap.keys(new String[termWeightMap.size()]);
				TIntDoubleHashMap termidWeightMap = new TIntDoubleHashMap();
				for (int i=0; i<terms.length; i++)
					termidWeightMap.put(Integer.parseInt(terms[i]), termWeightMap.get(terms[i]));
				termWeightMap.clear(); termWeightMap = null;
				if (tokens.length>2)
					allDocsEntries.put(qid, termidWeightMap);
				else{
					int docid = Integer.parseInt(tokens[1]);
					if (entries.containsKey(qid))
						entries.get(qid).put(docid, termidWeightMap);
					else{
						TIntObjectHashMap<TIntDoubleHashMap> map = new TIntObjectHashMap<TIntDoubleHashMap>();
						map.put(docid, termidWeightMap);
						entries.put(qid, map);
					}
				}
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
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
			PSIFeature app = new PSIFeature(Index.createIndex(indexpath, indexprefix));
			app.preProcess(qrelsFilename, queryid, outputFilename, app.featureId);
		}else if (args[0].equals("--preprocessall")){
			String indexpath = args[1];
			String indexprefix = args[2];
			String qrelsFilename = args[3];
			// String outputFilename = args[4];
			PSIFeature app = new PSIFeature(Index.createIndex(indexpath, indexprefix));
			app.preProcessAll(qrelsFilename, app.cacheFolder+ApplicationSetup.FILE_SEPARATOR+app.getInfo(), app.featureId);
		}
	}
}

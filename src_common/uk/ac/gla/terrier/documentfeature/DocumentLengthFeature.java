/**
 * 
 */
package uk.ac.gla.terrier.documentfeature;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

import gnu.trove.THashMap;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectDoubleHashMap;
import uk.ac.gla.terrier.statistics.CosineSimilarity;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.SingleLineTRECQuery;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;

/**
 * This class implements the per-doc similarity, the similarity of the most informative
 * terms in a feedback document to those in the whole feedback document set. The most 
 * informative terms are stored on disk. 
 * @author ben
 *
 */
public class DocumentLengthFeature extends DocumentFeature {
	
	protected THashMap<String, TIntObjectHashMap<Double>> cache = new THashMap<String, TIntObjectHashMap<Double>>();

	/**
	 * @param index
	 */
	public DocumentLengthFeature(Index index) {
		super(index);
		File fCache = new File(this.cacheFolder+ApplicationSetup.FILE_SEPARATOR+this.getInfo());
		this.CACHED = fCache.exists();
		if (CACHED)
			this.loadCache(fCache);
		this.featureId = 8;
		
	}

	/* (non-Javadoc)
	 * @see uk.ac.gla.terrier.documentfeature.features.DocumentFeature#extractDocumentFeature(int, java.lang.String, int[], gnu.trove.TIntDoubleHashMap)
	 */
	@Override
	public void extractDocumentFeature(int docid, String queryid,
			int[] queryTermids, TIntObjectHashMap featureMap) {
		if (CACHED){
			TIntObjectHashMap<Double> map = this.cache.get(queryid);
			if (map!=null)
				featureMap.put(featureId, map.get(docid));
		}else{
			double doclength = this.docIndex.getDocumentLength(docid);
			featureMap.put(featureId, doclength);
		}
	}

	/* (non-Javadoc)
	 * @see uk.ac.gla.terrier.documentfeature.features.DocumentFeature#getInfo()
	 */
	@Override
	public String getInfo() {
		return "DocumentLengthFeature";
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
		
	}
	public static void main(String[] args){
		// --preprocess indexpath indexprefix qrelsname queryid outputname
		if (args[0].equals("--preprocess")){
			String indexpath = args[1];
			String indexprefix = args[2];
			String qrelsFilename = args[3];
			String queryid = args[4];
			String outputFilename = args[5];
			DocumentLengthFeature app = new DocumentLengthFeature(Index.createIndex(indexpath, indexprefix));
			app.preProcess(qrelsFilename, queryid, outputFilename, app.featureId);
		}else if (args[0].equals("--preprocessall")){
			String indexpath = args[1];
			String indexprefix = args[2];
			String qrelsFilename = args[3];
			// String outputFilename = args[4];
			DocumentLengthFeature app = new DocumentLengthFeature(Index.createIndex(indexpath, indexprefix));
			app.preProcessAll(qrelsFilename, app.cacheFolder+ApplicationSetup.FILE_SEPARATOR+app.getInfo(), app.featureId);
		}
	}
}

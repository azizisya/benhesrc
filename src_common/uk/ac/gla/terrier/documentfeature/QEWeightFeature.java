package uk.ac.gla.terrier.documentfeature;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import uk.ac.gla.terrier.matching.models.WeightingModel;
import uk.ac.gla.terrier.querying.QueryExpansion;
import uk.ac.gla.terrier.statistics.Statistics;
import uk.ac.gla.terrier.structures.ExpansionTerm;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.LexiconEntry;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.IndexUtility;
import uk.ac.gla.terrier.utility.QueryUtility;
import gnu.trove.THashMap;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntObjectHashMap;

/**
 * Number of documents in the collection containing each of the expansion terms and all original 
 * query terms (DFEQ): see if the co-occurrance of the expansion terms  with the original query 
 * terms in the feedback document is by chance or not.
 * @author ben
 *
 */
public class QEWeightFeature extends PSIFeature {
	
	public int featureId;
	
	/**
	 * Map from qid to a map that maps from docid to feature value.
	 */
	protected THashMap<String, TIntObjectHashMap<Double>> cache = new THashMap<String, TIntObjectHashMap<Double>>();
	
	// protected WeightingModel model = WeightingModel.getWeightingModel(ApplicationSetup.getProperty("trec.model", "KL"));
	
	public QEWeightFeature(Index index){
		super(index);
		File fCache = new File(this.cacheFolder+ApplicationSetup.FILE_SEPARATOR+this.getInfo());
		this.CACHED = fCache.exists();
		if (CACHED)
			this.loadCache(fCache);
		this.featureId = 1;		
	}
	
	public String getInfo(){
		return "QEWeightFeature";
	}
	

	@Override
	public void extractDocumentFeature(int docid, String queryid,
			int[] queryTermids, TIntObjectHashMap featureMap) {
		if (CACHED){
			TIntObjectHashMap<Double> map = this.cache.get(queryid);
			if (map!=null)
				featureMap.put(featureId, map.get(docid));
		}else{
			/**
			int[][] terms = directIndex.getTerms(docid);
			int[] oneDocid = {docid};
			if (terms==null)
				return;
			ExpansionTerm[] expterms = QueryExpansion.expandFromDocuments(oneDocid, 
					null, terms[0].length, 
					directIndex, docIndex, collStats, lexicon, model);
			Arrays.sort(expterms);
			TIntHashSet expTermidSet = new TIntHashSet();
			TIntHashSet originalTermidSet = new TIntHashSet();
			originalTermidSet.addAll(queryTermids);
			for (int i=0; i<expterms.length; i++){
				if (!originalTermidSet.contains(expterms[i].getTermID()))
					expTermidSet.add(expterms[i].getTermID());
			}
			int[] expTermids = QueryUtility.filterTerms(expTermidSet.toArray(), lexicon, collStats.getNumberOfDocuments(), 0.05);
			*/
			
			double[] expWeights = entries.get(queryid).get(docid).getValues();
			if (expWeights==null)
				featureMap.put(featureId, 0);
			else
				featureMap.put(featureId, Statistics.mean(expWeights));
		}
	}
	
	/*public static int[] retainAll(int[] data, int[] toRetain){
		Arrays.sort(data); Arrays.sort(toRetain);
		TIntHashSet retainSet = new TIntHashSet();
		int dataIndex = 0; int toRetainLength = toRetain.length;
		int dataLength = data.length;
		for (int i=0; i<toRetainLength; i++){
			while (toRetain[i]>data[dataIndex])
				dataIndex++;
			if (toRetain[i] == data[dataIndex]){
				retainSet.add(data[dataIndex++]);
			}
			if (dataIndex == dataLength)
				break;
		}
		return retainSet.toArray();
	}*/
	
	/*public static void retainAll(TIntHashSet set, int[] toRetain, TIntHashSet globalSet){
		globalSet.removeAll(toRetain);
		set.removeAll(toRetain);
	}*/
	
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

	public static void main(String[] args){
		// --preprocess indexpath indexprefix qrelsname queryid outputname
		if (args[0].equals("--preprocess")){
			String indexpath = args[1];
			String indexprefix = args[2];
			String qrelsFilename = args[3];
			String queryid = args[4];
			String outputFilename = args[5];
			QEWeightFeature app = new QEWeightFeature(Index.createIndex(indexpath, indexprefix));
			app.preProcess(qrelsFilename, queryid, outputFilename, app.featureId);
		}else if (args[0].equals("--preprocessall")){
			String indexpath = args[1];
			String indexprefix = args[2];
			String qrelsFilename = args[3];
			// String outputFilename = args[4];
			QEWeightFeature app = new QEWeightFeature(Index.createIndex(indexpath, indexprefix));
			app.preProcessAll(qrelsFilename, app.cacheFolder+ApplicationSetup.FILE_SEPARATOR+app.getInfo(), app.featureId);
			// app.preProcessAll(qrelsFilename, app.cacheFolder+ApplicationSetup.FILE_SEPARATOR+app.getInfo()+args[4], app.featureId);
		}
	}
	
}

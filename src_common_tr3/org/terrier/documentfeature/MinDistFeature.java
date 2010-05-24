package org.terrier.documentfeature;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.terrier.matching.models.Idf;
import org.terrier.matching.models.WeightingModel;
import org.terrier.matching.models.queryexpansion.QueryExpansionModel;
import org.terrier.querying.QueryExpansion;
import org.terrier.querying.termselector.TermSelector;
import org.terrier.structures.ExpansionTerm;
import org.terrier.structures.Index;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.Files;

import uk.ac.gla.terrier.statistics.Statistics;
import gnu.trove.THashMap;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntObjectHashMap;


/**
 *  Proximity of expanded query terms to the original query terms. As defined in Cao et al. 2008:

$$log_2\sum^{e}_{j\in e}\frac{1}{n}\frac{\sum^{n}_{i=1}tf_{p}\cdot dist(t, t_i)}{ \sum^{n}_{i=1}tf_{p}  }/\#e$$
 * @author ben
 *
 */
public class MinDistFeature extends DocumentFeature {
	protected int featureId;
	
	/**
	 * Map from qid to a map that maps from docid to feature value.
	 */
	protected THashMap<String, TIntObjectHashMap<Double>> cache = new THashMap<String, TIntObjectHashMap<Double>>();
	
	protected int expTerms = Integer.parseInt(ApplicationSetup.getProperty("expansion.terms", "10"));
	
	protected int ngramLength = Integer.parseInt(ApplicationSetup.getProperty("proximity.ngram.length", "12"));
	
	protected QueryExpansionModel model = QueryExpansionModel.getQueryExpansionModel(ApplicationSetup.getProperty("trec.model", "KL"));
	
	public MinDistFeature(Index index){
		super(index);
		File fCache = new File(this.cacheFolder+ApplicationSetup.FILE_SEPARATOR+this.getInfo());
		this.CACHED = fCache.exists();
		if (CACHED)
			this.loadCache(fCache);
		this.featureId = 2;
	}
	
	public String getInfo(){
		return "MinDistFeature";
	}
	
	public void loadMetaData(){
		// No meta data to load.
	}

	@Override
	public void extractDocumentFeature(int docid, String queryid,
			int[] queryTermids, TIntObjectHashMap featureMap) {
		if (CACHED){
			TIntObjectHashMap<Double> map = this.cache.get(queryid);
			if (map!=null)
				featureMap.put(featureId, map.get(docid));
		}else{
			int[][] terms = null;
			try{
				terms = directIndex.getTerms(docid);
			}catch(IOException ioe){
				ioe.printStackTrace();
			}
			int[] oneDocid = {docid};
			if (terms==null)
				return;
			TermSelector selector = TermSelector.getDefaultTermSelector(index); 
			QueryExpansion qe = new QueryExpansion();
			ExpansionTerm[] expterms = qe.expandFromDocuments(oneDocid, null, terms[0].length, index, model, selector);
				
				/*QueryExpansion.expandFromDocuments(oneDocid, 
					null, terms[0].length, 
					directIndex, docIndex, collStats, lexicon, model);*/
			Arrays.sort(expterms);
			TIntHashSet expTermidSet = new TIntHashSet();
			TIntHashSet originalTermidSet = new TIntHashSet();
			originalTermidSet.addAll(queryTermids);
			for (int i=0; i<expterms.length; i++){
				if (!originalTermidSet.contains(expterms[i].getTermID()))
					expTermidSet.add(expterms[i].getTermID());
			}
			int[] expTermids = expTermidSet.toArray();
			double weightedDist = 0d;
			int[][] pointers = null;
			try{
				pointers = directIndex.getTerms(docid);
			}catch(IOException ioe){
				ioe.printStackTrace();
			}
			int[] topTermids = (queryTermids.length>2)?(QueryTermCooccurrenceFeature.getTop2Terms(queryTermids, lexicon, collStats)):(queryTermids);
			for (int i=0; i<expTermids.length; i++){
				double dist = getWeightedMinDistance(expTermids[i], topTermids, pointers, ngramLength);
				weightedDist += dist;
				//System.err.println("dist="+dist);
			}
			//System.err.println("weightedDist="+weightedDist);
			featureMap.put(featureId, weightedDist/expTermids.length);
		}
	}
	
	public static double getWeightedMinDistance(int expTermid, int[] queryTermids, int[][] pointers, int windowSize){
		int[][] blocks = new int[queryTermids.length+1][];
		int[] expTermBlocks = null;
		int numberOfUniqTerms = pointers[0].length;
		int blockIndex = 0;
		Arrays.sort(queryTermids);
		int termIndex = 0;
		int docLength = pointers[4].length;
		// get the positions of each term in the document
		for (int i=0; i<numberOfUniqTerms; i++){
			if (termIndex < queryTermids.length && pointers[0][i] == queryTermids[termIndex]){
				blocks[termIndex] = Arrays.copyOfRange(pointers[4], blockIndex, blockIndex+pointers[3][i]);
				Arrays.sort(blocks[termIndex]);
				termIndex++;
			}else if (pointers[0][i] == expTermid){
				expTermBlocks = Arrays.copyOfRange(pointers[4], blockIndex, blockIndex+pointers[3][i]);
			}
			blockIndex += pointers[3][i];
		}
		int[] minDist = new int[queryTermids.length];
		int[] freq = new int[queryTermids.length];
		double upperSum = 0d;
		for (int i=0; i<queryTermids.length; i++){
			int dist = windowSize;
			if (blocks[i] == null){
				minDist[i] = dist;
				freq[i] = 0;
				continue;
			}
			for (int j=0; j<blocks[i].length; j++)
				for (int t=0; t<expTermBlocks.length; t++)
					dist = Math.min(dist, Math.abs(blocks[i][j]-expTermBlocks[t]));
			freq[i] = QueryTermCooccurrenceFeature.countNgramFrequency(
					expTermid, queryTermids[i], pointers, windowSize);
			minDist[i] = dist;
			upperSum += (double)freq[i]*minDist[i];
		}
		double sumFreq = Statistics.sum(freq);
		// double dist = (sumFreq==0)?(docLength):(Idf.log(upperSum/Statistics.sum(freq)));
		double dist = (sumFreq==0)?(windowSize):(Idf.log(upperSum/Statistics.sum(freq)));
		return dist;
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
	public static void main(String[] args){
		// --preprocess indexpath indexprefix qrelsname queryid outputname
		if (args[0].equals("--preprocess")){
			String indexpath = args[1];
			String indexprefix = args[2];
			String qrelsFilename = args[3];
			String queryid = args[4];
			String outputFilename = args[5];
			MinDistFeature app = new MinDistFeature(Index.createIndex(indexpath, indexprefix));
			app.preProcess(qrelsFilename, queryid, outputFilename, app.featureId);
		}else if (args[0].equals("--preprocessall")){
			String indexpath = args[1];
			String indexprefix = args[2];
			String qrelsFilename = args[3];
			//String outputFilename = args[4];
			MinDistFeature app = new MinDistFeature(Index.createIndex(indexpath, indexprefix));
			app.preProcessAll(qrelsFilename, app.cacheFolder+ApplicationSetup.FILE_SEPARATOR+app.getInfo(), app.featureId);
		}
	}

}

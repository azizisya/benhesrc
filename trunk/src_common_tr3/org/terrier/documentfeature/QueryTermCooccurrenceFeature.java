package org.terrier.documentfeature;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.terrier.structures.CollectionStatistics;
import org.terrier.structures.Index;
import org.terrier.structures.Lexicon;
import org.terrier.structures.LexiconEntry;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.Distance;
import org.terrier.utility.Files;

import uk.ac.gla.terrier.statistics.Statistics;
import gnu.trove.THashMap;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntObjectHashMap;

/**
 * Co-occurrance of query terms. 
 * @author ben
 *
 */
public class QueryTermCooccurrenceFeature extends DocumentFeature {
	protected int featureId;
	
	protected int ngramLength = Integer.parseInt(ApplicationSetup.getProperty("proximity.ngram.length", "12"));
	
	/**
	 * Map from qid to a map that maps from docid to feature value.
	 */
	protected THashMap<String, TIntObjectHashMap<Double>> cache = new THashMap<String, TIntObjectHashMap<Double>>();
	
	public QueryTermCooccurrenceFeature(Index index){
		super(index);
		File fCache = new File(this.cacheFolder+ApplicationSetup.FILE_SEPARATOR+this.getInfo());
		this.CACHED = fCache.exists();
		if (CACHED)
			this.loadCache(fCache);
		this.featureId = 4;
	}
	
	public String getInfo(){
		return "QueryTermCooccurrenceFeature";
	}
	
	public void loadMetaData(){
		// No meta data to load.
	}

	@Override
	// count number of cooccurances of the query terms in a window
	public void extractDocumentFeature(int docid, String queryid,
			int[] queryTermids, TIntObjectHashMap featureMap) {
		if (CACHED){
			TIntObjectHashMap<Double> map = this.cache.get(queryid);
			if (map!=null)
				featureMap.put(featureId, map.get(docid));
		}else{
			if (queryTermids.length < 2){
				featureMap.put(featureId, 0);
			}
			// int[] topTermids = this.getTop2Terms(queryTermids);
			int[][] pointers = null;
			try{
				pointers = directIndex.getTerms(docid);
			}catch(IOException ioe){
				ioe.printStackTrace();
			}
			
			if (pointers == null)
				return;
			int freq = 0;
			for (int i=0; i<queryTermids.length-1; i++)
				for (int j=i+1; j<queryTermids.length; j++){
					int thisFreq = countNgramFrequency(queryTermids[i], queryTermids[j], pointers, ngramLength); 
					freq+=thisFreq;
					
				}
			double n = (double)queryTermids.length;
			featureMap.put(featureId, (double)freq/(n*(n-1)/2));
		}
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
	
	/**
	 * Count number of co-occurrances of a pair of given terms in a fixed window size in a given document
	 * @param termids The identifiers of terms
	 * @param pointers Posting list of the document
	 * @return The count.
	 */
	public static int countNgramFrequency(int termid1, int termid2, int[][] pointers, int windowSize){
		int[][] blocks = new int[2][];
		int numberOfUniqTerms = pointers[0].length;
		int blockIndex = 0;
		int[] termids = {termid1, termid2};
		Arrays.sort(termids);
		int termIndex = 0;
		int docLength = Statistics.sum(pointers[1]);
		// get the positions of each term in the document
		for (int i=0; i<numberOfUniqTerms; i++){
			if (pointers[0][i] == termids[termIndex]){
				blocks[termIndex] = Arrays.copyOfRange(pointers[4], blockIndex, blockIndex+pointers[3][i]);
				Arrays.sort(blocks[termIndex]);
				termIndex++;
			}
			blockIndex += pointers[3][i];
			if (termIndex == termids.length)
				break;
		}
		if (termIndex!=termids.length)
			return 0;
		
		// count co-occurrences
		/*int freq = 0;
		for (int term1=0; term1<blocks[0].length; term1++){
			for (int term2=0; term2<blocks[1].length;term2++){
				if (Math.abs(blocks[1][term2]-blocks[0][term1])<windowSize){
					freq++;
				}
			}
		}	*/
		int newfreq = Distance.noTimes(blocks[0], 0, blocks[0].length, blocks[1], 0, blocks[1].length, windowSize, docLength);
		// System.err.println("freq: "+freq+", Distance.noTimes: "+newfreq);
		// System.err.println("Distance.noTimes: "+newfreq);
		return newfreq;
	}
	
	public static int[] getTop2Terms(int[] queryTermids, Lexicon lexicon, CollectionStatistics collStats){
		// remove terms that occur in more than given percentage of spans in the span collection
		double[] DF = new double[queryTermids.length];
		int[] localTermids = (int[])queryTermids.clone();
		for (int j=0; j<queryTermids.length; j++){
			LexiconEntry lexEntry = (LexiconEntry)lexicon.getLexiconEntry(queryTermids[j]).getValue();
			if (lexEntry!=null){
				DF[j] = lexEntry.getDocumentFrequency();
			}else
				DF[j] = collStats.getNumberOfDocuments();
		}
		short[] dummy = new short[queryTermids.length];
		Arrays.fill(dummy, (short)1);
		org.terrier.utility.HeapSort.ascendingHeapSort(DF, localTermids, dummy);
		int[] top2Termids = {localTermids[0], localTermids[1]};
		return top2Termids;
	}
	
	public static void main(String[] args){
		// --preprocess indexpath indexprefix qrelsname queryid outputname
		if (args[0].equals("--preprocess")){
			String indexpath = args[1];
			String indexprefix = args[2];
			String qrelsFilename = args[3];
			String queryid = args[4];
			String outputFilename = args[5];
			QueryTermCooccurrenceFeature app = new QueryTermCooccurrenceFeature(Index.createIndex(indexpath, indexprefix));
			app.preProcess(qrelsFilename, queryid, outputFilename, app.featureId);
		}else if (args[0].equals("--preprocessall")){
			String indexpath = args[1];
			String indexprefix = args[2];
			String qrelsFilename = args[3];
			// String outputFilename = args[4];
			QueryTermCooccurrenceFeature app = new QueryTermCooccurrenceFeature(Index.createIndex(indexpath, indexprefix));
			app.preProcessAll(qrelsFilename, app.cacheFolder+ApplicationSetup.FILE_SEPARATOR+app.getInfo(), app.featureId);
		}
	}

}

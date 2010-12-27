package org.terrier.applications;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;

import org.terrier.evaluation.TRECResultsInMemory;
import org.terrier.evaluation.TRECResultsInMemory.RetrievedDoc;
import org.terrier.utility.Files;

import uk.ac.gla.terrier.statistics.Statistics;

import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntObjectHashMap;

public class TopicalOpinion {
	public static TIntObjectHashMap<TIntDoubleHashMap> loadResultFile(String resultFilename){
		TIntObjectHashMap<TIntDoubleHashMap> queryidResultMap = new TIntObjectHashMap<TIntDoubleHashMap>();
		TRECResultsInMemory results = new TRECResultsInMemory(resultFilename);
		String[] queryids = results.getQueryids();
		for (String queryid : queryids){
			RetrievedDoc[] docs = results.getRetrievedDocuments(queryid);
			TIntDoubleHashMap scoreMap = new TIntDoubleHashMap();
			for (RetrievedDoc doc : docs){
				scoreMap.put(Integer.parseInt(doc.docno), doc.score);
			}
			queryidResultMap.put(Integer.parseInt(queryid), scoreMap);
		}
		return queryidResultMap;
	}
	
	private static TIntDoubleHashMap loadPostLMFile(String postLMFilename){
		TIntDoubleHashMap scoreMap = new TIntDoubleHashMap();
		try{
			BufferedReader br = Files.openFileReader(postLMFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				String[] strs = line.split(" ");
				scoreMap.put(Integer.parseInt(strs[0]), Double.parseDouble(strs[1]));
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		return scoreMap;
	}
	
	public static void printCorrelation(String postLMFilename, String resultFilename){
		TIntDoubleHashMap postLMMap = loadPostLMFile(postLMFilename);
		TIntObjectHashMap<TIntDoubleHashMap> qidScoreMap = loadResultFile(resultFilename);
		int[] qids = qidScoreMap.keys();
		Arrays.sort(qids);
		for (int qid : qids){
			System.out.println(qid+" "+computeCorrelation(postLMMap, qidScoreMap.get(qid)));
		}
	}
	
	public static double computeCorrelation(TIntDoubleHashMap postLMScoreMap, TIntDoubleHashMap scoreMap){
		double[] scores = new double[scoreMap.size()];
		double[] postLMScores = new double[scoreMap.size()];
		int counter = 0;
		for (int docid : scoreMap.keys()){
			scores[counter] = scoreMap.get(docid);
			postLMScores[counter] = postLMScoreMap.get(docid);
		}
		return Statistics.correlation(scores, postLMScores);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args[0].equals("--printcorr")){
			// --printcorr postLMFIlename resultFilename
			TopicalOpinion.printCorrelation(args[1], args[2]);
		}

	}

}

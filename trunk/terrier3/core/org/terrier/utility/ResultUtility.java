package org.terrier.utility;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;

import gnu.trove.TDoubleArrayList;
import gnu.trove.THashSet;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntDoubleHashMap;
import org.terrier.evaluation.TRECQrelsInMemory;
import org.terrier.evaluation.TRECResultsInMemory;
import org.terrier.evaluation.TRECResultsInMemory.RetrievedDoc;
import uk.ac.gla.terrier.statistics.CosineSimilarity;
import uk.ac.gla.terrier.utility.StringUtility;
import uk.ac.gla.terrier.utility.TroveUtility;

public class ResultUtility{
	
	public static void mergeResults(String[] filenames, String outputFilename, int resultSize){
		TRECResultsInMemory[] results = new TRECResultsInMemory[filenames.length];
		for (int i=0; i<results.length; i++)
			results[i] = new TRECResultsInMemory(filenames[i]);
		String[] qids = results[0].getQueryids();
		try{
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			for (String qid : qids){
				THashSet<RetrievedDoc> docSet = new THashSet<RetrievedDoc>();
				for (TRECResultsInMemory result : results){
					docSet.addAll(Arrays.asList(result.getRetrievedDocs(qid)));
				}
				RetrievedDoc[] docs = docSet.toArray(new RetrievedDoc[docSet.size()]);  
				Arrays.sort(docs);
				
				int size = Math.min(resultSize, docs.length);
				for (int i=0; i<size; i++)
					bw.write(qid+" Q0 "+docs[i].docno+" "+i+" "+docs[i].score+" merged"+ApplicationSetup.EOL);
			}
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void printResultFileSimilarity(String resultFilenameA, String resultFilenameB){
		TRECResultsInMemory resultA = new TRECResultsInMemory(resultFilenameA);
		TRECResultsInMemory resultB = new TRECResultsInMemory(resultFilenameB);
		
		THashSet<String> qidSet = new THashSet<String>(Arrays.asList(resultA.getQueryids()));
		THashSet<String> tmpHashSet = new THashSet<String>(Arrays.asList(resultB.getQueryids()));
		qidSet.retainAll(tmpHashSet);
		String[] qids = qidSet.toArray(new String[qidSet.size()]);
		Arrays.sort(qids);
		for (String qid : qids){
			int[] docidsA = TroveUtility.stringArrayToIntArray(resultA.getDocnoSet(qid));
			int[] docidsB = TroveUtility.stringArrayToIntArray(resultB.getDocnoSet(qid));
			double[] scoresA = resultA.getScores(qid);
			double[] scoresB = resultB.getScores(qid);
			double sim = getResultSimilarity(docidsA, docidsB, scoresA, scoresB);
			System.out.println(qid+": "+Rounding.toString(sim, 4));
		}
	}
	
	public static void convertDocnoToDocidFromResults(String indexPath, String indexPrefix, String resultFilename, String outputFilename){
		IndexUtility.convertDocnoToDocidFromResults(indexPath, indexPrefix, resultFilename, outputFilename);
	}
	
	public static void generateFBDocs(String resultFilename, String folder, int topX){
		TRECResultsInMemory results = new TRECResultsInMemory(resultFilename);
		String[] qids = results.getQueryids();
		for (int rank=0; rank<topX; rank++){
			StringBuffer buf = new StringBuffer();
			for (String qid : qids){
				String docno = results.getDocno(qid, rank);
				if (docno!=null)
					buf.append(qid+" 0 "+docno+" 1"+ApplicationSetup.EOL);
			}
			String outputFilename = folder+ApplicationSetup.FILE_SEPARATOR+"fb_rank"+rank;
			try{
				BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
				bw.write(buf.toString());
				bw.close();
			}catch(IOException e){
				e.printStackTrace();
				System.exit(1);
			}
		}
		
	}
	
	public static double getResultSimilarity(int[] docidsA, int[] docidsB, double[] scoresA, double[] scoresB){
		TIntDoubleHashMap mapA = TroveUtility.mapIntArrayToDoubleArray(docidsA, scoresA);
		TIntDoubleHashMap mapB = TroveUtility.mapIntArrayToDoubleArray(docidsB, scoresB);
		return CosineSimilarity.cosine(mapA, mapB);
	}
	/**
	 * Remove documents retrieved in B from A.
	 * @param resultFilenameA
	 * @param resultFilenameB
	 * @param outputFilename
	 */
	public static void removeRedundantResults(String resultFilenameA, String resultFilenameB, String outputFilename){
		TRECResultsInMemory resultA = new TRECResultsInMemory(resultFilenameA);
		TRECResultsInMemory resultB = new TRECResultsInMemory(resultFilenameB);
		String[] qids = resultA.getQueryids();
		int numOfQueries = qids.length;
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<numOfQueries; i++){
			String[] docnos = resultA.getDocnoSet(qids[i]);
			for (String docno : docnos){
				if (!resultB.isRetrieved(qids[i], docno))
					buf.append(qids[i]+" Q0 "+docno+" "+resultA.getRank(qids[i], docno)+" "+
							resultA.getScore(qids[i], docno)+" filtered"+ApplicationSetup.EOL);
			}
		}
		try{
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			bw.write(buf.toString());
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void resultsToQrelsFormat(String resultFilename, String outputFilename, int defaultLabel){
		try{
			BufferedReader br = Files.openFileReader(resultFilename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				bw.write(tokens[0]/*qid*/+" 0 "+tokens[2]/*docid*/+" "+defaultLabel+ApplicationSetup.EOL);
			}
			br.close(); bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		System.out.println("Done. Data save in file "+outputFilename+" in qrels format with default label "+defaultLabel);
	}
	
	/**
	 * Remove judged documents from the result file. 
	 * @param qrelsFilename The qrels file containing judged documents.
	 * @param resultFilename The result filename.
	 * @param outputFilename The output filename.
	 */
	public static void removeJudgedFromResults(String qrelsFilename, String resultFilename, String outputFilename){
		TRECQrelsInMemory qrels = new TRECQrelsInMemory(qrelsFilename);
		try{
			BufferedReader br = Files.openFileReader(resultFilename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			String line = null;
			int removed = 0;
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				String qid = StringUtility.keepNumericChars(tokens[0]);
				String docno = tokens[2];
				if (qrels.isRelevant(qid, docno) || qrels.isJudgedNonRelevant(qid, docno)){
					removed++;
				}else{
					bw.write(line+ApplicationSetup.EOL);
				}
			}
			br.close(); bw.close();
			System.out.println("Done. "+removed+" entries removed from results.");	
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

	}

}

package org.terrier.applications;

import gnu.trove.THashMap;

import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectIntHashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.StringTokenizer;

import org.terrier.structures.DocumentIndex;
import org.terrier.structures.Index;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.Files;

import org.terrier.evaluation.TRECResultsInMemory;
import org.terrier.evaluation.TRECResultsInMemory.RetrievedDoc;

public class SIGIR10 {
	
	public static void mergeFiles(String withDocidFilename, String weightFilename, String outputFilename){
		// load key to docid map
		TObjectIntHashMap<String> docidMap = new TObjectIntHashMap<String>();
		try{
			BufferedReader br = Files.openFileReader(withDocidFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				// key=qid.score.length
				String key = tokens[0]+"."+tokens[2]+"."+tokens[3];
				int docid = Integer.parseInt(tokens[1]);
				docidMap.put(key, docid);
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	//	System.out.println("size: "+);
		// load weight file and write
		try{
			BufferedReader br = Files.openFileReader(weightFilename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				String[] tokens = line.split("\\,");
				// key=qid.score.length
				String key = tokens[0]+"."+tokens[1]+"."+tokens[2];
				System.out.println("key: "+key);
				int docid = docidMap.get(key);
				if (docid!=0)
					bw.write(docid+","+line+ApplicationSetup.EOL);
			}
			
			br.close(); bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void writeDocnoLength(String indexPath, String indexPrefix, String qrelsFilename, String resultFilename, String evalFilename, String outputFilename){
		DocumentIndex di = Index.createIndex(indexPath, indexPrefix).getDocumentIndex();
		try{
			// load map values
			TObjectDoubleHashMap<String> map = new TObjectDoubleHashMap<String>();
			BufferedReader br = Files.openFileReader(evalFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				StringTokenizer stk = new StringTokenizer(line);
				stk.nextToken();// skip map
				String qid = stk.nextToken();
				double ap = Double.parseDouble(stk.nextToken());
				map.put(qid, ap);
			}
			
			br.close();
			
			// load results
				// qid -> (docid -> score)
			THashMap<String, TIntDoubleHashMap> results = new THashMap<String, TIntDoubleHashMap>();
			br = Files.openFileReader(resultFilename);
			line = null;
			while ((line=br.readLine())!=null){
				StringTokenizer stk = new StringTokenizer(line);
				String qid = stk.nextToken();
				stk.nextToken(); // skip Q0
				int docid = Integer.parseInt(stk.nextToken());
				stk.nextToken(); // skip rank
				double score = Double.parseDouble(stk.nextToken());
				if (results.get(qid)==null){
					TIntDoubleHashMap tmpMap = new TIntDoubleHashMap();
					tmpMap.put(docid, score);
					results.put(qid, tmpMap);
				}else
					results.get(qid).put(docid, score);
			}
			
			br.close();
			
			br = Files.openFileReader(qrelsFilename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			
			
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				String qid = tokens[0];
				int docid = Integer.parseInt(tokens[2]);
				String label = tokens[3];
				int length = di.getDocumentLength(docid);
				if (results.get(qid)==null)
					continue;
				double score = results.get(qid).get(docid);
				if (length==0 && label.equals("0")){
					System.err.println(line+" "+score);
				}
				double ap = map.get(qid);
				if (score<=0d)
					System.err.println(qid+" "+docid+" not found in results.");
				else
					// output format: qid score length label
					bw.write(qid+" "+docid+" "+	score+" "+length+" "+ap+" "+label+ApplicationSetup.EOL);
			}
			
			br.close(); bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void writeDocnoLengthClueB(String indexPath, String indexPrefix, String prelsFilename, String resultFilename, String evalFilename, String outputFilename){
		DocumentIndex di = Index.createIndex(indexPath, indexPrefix).getDocumentIndex();
		try{
			// load map values
			TObjectDoubleHashMap<String> map = new TObjectDoubleHashMap<String>();
			BufferedReader br = Files.openFileReader(evalFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				StringTokenizer stk = new StringTokenizer(line);
				stk.nextToken();// skip map
				String qid = stk.nextToken();
				double ap = Double.parseDouble(stk.nextToken());
				map.put(qid, ap);
			}
			
			br.close();
			
			// load results
				// qid -> (docid -> score)
			THashMap<String, TIntDoubleHashMap> results = new THashMap<String, TIntDoubleHashMap>();
			br = Files.openFileReader(resultFilename);
			line = null;
			while ((line=br.readLine())!=null){
				StringTokenizer stk = new StringTokenizer(line);
				String qid = stk.nextToken();
				stk.nextToken(); // skip Q0
				int docid = Integer.parseInt(stk.nextToken());
				stk.nextToken(); // skip rank
				double score = Double.parseDouble(stk.nextToken());
				if (results.get(qid)==null){
					TIntDoubleHashMap tmpMap = new TIntDoubleHashMap();
					tmpMap.put(docid, score);
					results.put(qid, tmpMap);
				}else
					results.get(qid).put(docid, score);
			}
			
			br.close();
			
			br = Files.openFileReader(prelsFilename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			
			
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				String qid = tokens[0];
				int docid = Integer.parseInt(tokens[1]);
				String label = tokens[2];
				int length = di.getDocumentLength(docid);
				if (results.get(qid)==null)
					continue;
				double score = results.get(qid).get(docid);
				if (length==0 && label.equals("0")){
					System.err.println(line+" "+score);
				}
				double ap = map.get(qid);
				if (score<=0d)
					System.err.println(qid+" "+docid+" not found in results.");
				else
					// output format: qid score length label
					bw.write(qid+" "+docid+" "+	score+" "+length+" "+ap+" "+label+ApplicationSetup.EOL);
			}
			
			br.close(); bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void addDocid(String weightFilename, String resultsFilename, String outputFilename){
		// load qrels and results
		// mapping from qid to a mapping from score and docid to docid
	}
	
	public static void correctQids(String weightFilename, String outputFilename){
		try{
			BufferedReader br = Files.openFileReader(weightFilename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(",");
				if (tokens[0].length()<3)
					bw.write("0"+line+ApplicationSetup.EOL);
				else
					bw.write(line+ApplicationSetup.EOL);
			}
			
			br.close(); bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		
	}
	
	/**
	 * 
	 * @param resultFilename
	 * @param weightFilename
	 * @param modelNo 1-based
	 * @param alpha
	 * @param outputFilename
	 */
	public static void produceFinalRanking(String resultFilename, String weightFilename, int modelNo, double alpha, String outputFilename){
		TRECResultsInMemory results = new TRECResultsInMemory(resultFilename);
		
		
		// load the weights into memory
		THashMap<String, TIntDoubleHashMap> weightMap = new THashMap<String, TIntDoubleHashMap>();// mapping from qid to < docid to weight >
		try{
			BufferedReader br = Files.openFileReader(weightFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(",");
				double weight = Double.parseDouble(tokens[modelNo+5]);
				String qid = tokens[0];
				int docid = Integer.parseInt(tokens[1]);
				//double score = Double.parseDouble(tokens[2]);
				//score += alpha*weight;
				if (!weightMap.contains(qid)){
					TIntDoubleHashMap docidWMap = new TIntDoubleHashMap();
					weightMap.put(qid, docidWMap);
				}
				weightMap.get(qid).put(docid, weight);
				if (qid.equals("051")&&docid==364470)
					System.out.println("weight: "+weight);
				
				// if (!weightMap.contains(tokens[0]))
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		// combine and write
		try{
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			String[] qids = results.getQueryids();
			Arrays.sort(qids);
			for (String qid : qids){
				RetrievedDoc[] docs = results.getRetrievedDocs(qid);
				for (RetrievedDoc doc : docs){
					if (qid.equals("051")&&Integer.parseInt(doc.docno)==364470)
						System.out.println("score: "+doc.score);
					int docid = Integer.parseInt(doc.docno);
					double weight = weightMap.get(qid).get(docid);
					doc.score += alpha*weight;
					if (qid.equals("051")&&Integer.parseInt(doc.docno)==364470)
						System.out.println("score: "+doc.score);
				}
				Arrays.sort(docs);
				int counter = 0;
				for (RetrievedDoc doc : docs){
					bw.write(qid+" Q0 "+doc.docno+" "+(counter++)+" "+doc.score+" model"+modelNo+ApplicationSetup.EOL);
				}
			}
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args[0].equals("--writedocnolength")){
			// --writedocnolength indexPath indexPrefix qrelsFilename resultFilename evalFilename outputFilename 
			SIGIR10.writeDocnoLength(args[1], args[2], args[3], args[4], args[5], args[6]);
		}else if (args[0].equals("--adddocid")){
			// --adddocid withDocidFilename, weightFilename, outputFilename
			SIGIR10.mergeFiles(args[1], args[2], args[3]);
		}else if (args[0].equals("--correctqid")){
			// --correctqid weightFilename, outputFilename
			SIGIR10.correctQids(args[1], args[2]);
		}else if (args[0].equals("--mergeweights")){
			// --mergeweights resultFilename, weightFilename, modelNo, alpha, outputFilename
			SIGIR10.produceFinalRanking(args[1], args[2], Integer.parseInt(args[3]), Double.parseDouble(args[4]), args[5]);
		}else if (args[0].equals("--writedocnolengthclueb")){
			// --writedocnolength indexPath indexPrefix prelsFilename resultFilename evalFilename outputFilename 
			SIGIR10.writeDocnoLengthClueB(args[1], args[2], args[3], args[4], args[5], args[6]);
		}

	}

}

/**
 * 
 */
package uk.ac.gla.terrier.applications;

import gnu.trove.THashMap;
import gnu.trove.TIntDoubleHashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;

import uk.ac.gla.terrier.evaluation.TRECResultsInMemory;
import uk.ac.gla.terrier.evaluation.TRECResultsInMemory.RetrievedDoc;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;

/**
 * @author ben
 *
 */
public class BM35Querying extends TRECQuerying {

	/**
	 * 
	 */
	public BM35Querying() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param i
	 */
	public BM35Querying(Index i) {
		super(i);
		// TODO Auto-generated constructor stub
	}
	
	public static void combineScores(String resultFilename, String weightFilename, int modelNo, double alpha, String outputFilename){
		
		TRECResultsInMemory results = new TRECResultsInMemory(resultFilename);
		
		
		// load the weights into memory
		THashMap<String, TIntDoubleHashMap> weightMap = new THashMap<String, TIntDoubleHashMap>();// mapping from qid to < docid to weight >
		try{
			BufferedReader br = Files.openFileReader(weightFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(",");
				try{
					double weight = Double.parseDouble(tokens[modelNo+5]);
					String qid = tokens[0];
					int docid = Integer.parseInt(tokens[1]);
					
					if (!weightMap.contains(qid)){
						TIntDoubleHashMap docidWMap = new TIntDoubleHashMap();
						weightMap.put(qid, docidWMap);
					}
					weightMap.get(qid).put(docid, weight);
				}catch(ArrayIndexOutOfBoundsException e){
					System.err.println("line: "+line);
					e.printStackTrace();
					System.exit(1);
				}
				
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
					/**
					if (qid.equals("051")&&Integer.parseInt(doc.docno)==364470)
						System.out.println("score: "+doc.score);
						*/
					int docid = Integer.parseInt(doc.docno);
					try{
						if (weightMap.containsKey(qid)&&weightMap.get(qid).containsKey(docid)){
							double weight = weightMap.get(qid).get(docid);
							doc.score += alpha*weight;
						}
					}catch(NullPointerException e){
						e.printStackTrace();
						System.exit(1);
					}
					/**
					if (qid.equals("051")&&Integer.parseInt(doc.docno)==364470)
						System.out.println("score: "+doc.score);
						*/
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
	
	public String processQueries(){
		String resultFilename = ApplicationSetup.getProperty("bm35.results", "");
		String weightFilename = ApplicationSetup.getProperty("bm35.weights", "");
		int modelNo = Integer.parseInt(ApplicationSetup.getProperty("bm35.model", "6")); 
		double alpha = Double.parseDouble(ApplicationSetup.getProperty("bm35.alpha", "1d")); 
		String outputFilename = resultFilename+".bm35";
		BM35Querying.combineScores(resultFilename, weightFilename, modelNo, alpha, outputFilename);
		return outputFilename;
	}
	
	public static void main(String[] args) {
		if (args.length==0){
			System.out.println("Usage: resultFilename weightFilename modelNo alpha outputFilename");
		}else{
			BM35Querying.combineScores(args[0], args[1], Integer.parseInt(args[2]), Double.parseDouble(args[3]), args[4]);
		}
		
		
	}

}

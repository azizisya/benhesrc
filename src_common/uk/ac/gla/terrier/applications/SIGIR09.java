package uk.ac.gla.terrier.applications;

import gnu.trove.TDoubleArrayList;
import gnu.trove.THashSet;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntHashSet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.StringTokenizer;

import tests.CoherenceScore;
import uk.ac.gla.terrier.evaluation.TRECQrelsInMemory;
import uk.ac.gla.terrier.evaluation.TRECResultsInMemory;
import uk.ac.gla.terrier.statistics.CosineSimilarity;
import uk.ac.gla.terrier.statistics.Statistics;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Debugging;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.Rounding;

public class SIGIR09 {
	public static void printExpansionWeightSimilarity(THashSet<TIntDoubleHashMap> mapSet, 
			TIntDoubleHashMap allMap, String qid){
		TIntDoubleHashMap[] scoreMaps = (TIntDoubleHashMap[])mapSet.toArray(new TIntDoubleHashMap[mapSet.size()]);
		TIntHashSet termidSet = new TIntHashSet(allMap.keys());
		for (int i=0; i<scoreMaps.length; i++)
			termidSet.addAll(scoreMaps[i].keys());
		double simsum = 0d;
		double[] allvec = DocumentSimilarity.makeVector(allMap, termidSet);
		System.out.println("Cosine similarities for query "+qid);
		TDoubleArrayList simArray = new TDoubleArrayList();
		for (int i=0; i<scoreMaps.length; i++){
			double[] vec = DocumentSimilarity.makeVector(scoreMaps[i], termidSet);
			double sim = CosineSimilarity.cosine(vec, allvec);
			if (Double.isNaN(sim)||Double.isInfinite(sim)){
				for (int j=0; j<vec.length; j++)
					System.err.println((j+1)+": "+vec[j]+", "+allvec[j]);
				Debugging.dumpIntDoubleHashMap(scoreMaps[i], "one doc map");
				Debugging.dumpIntDoubleHashMap(allMap, "all docs map");
				System.out.println("dump both hash maps");
				TIntDoubleHashMap[] maps = {scoreMaps[i], allMap};
				Debugging.dumpIntDoubleHashMaps(maps, termidSet);
			}
			System.out.println("sim "+(i+1)+"="+sim);
			simsum+=sim;
			simArray.add(sim);
		}
		double[] sims = simArray.toNativeArray();
		Arrays.sort(sims);
		System.out.println("psi1: "+simsum/scoreMaps.length);
		System.out.println("mean sim/dev: "+simsum/(Statistics.standardDeviation(sims)*scoreMaps.length));
		System.out.println("psi3: "+(Statistics.sum(sims)-sims[0]-sims[sims.length-1])/(sims.length-2));
		// compute cohenrence among vectors of single docs
		double coherence = 0d;
		int counter = 0;
		termidSet = new TIntHashSet();
		for (int i=0; i<scoreMaps.length; i++)
			termidSet.addAll(scoreMaps[i].keys());
		for (int i=0; i<scoreMaps.length-1; i++)
			for (int j=i+1; j<scoreMaps.length; j++){
				double[] v1 = DocumentSimilarity.makeVector(scoreMaps[i], termidSet);
				double[] v2 = DocumentSimilarity.makeVector(scoreMaps[j], termidSet);
				double sim = CosineSimilarity.cosine(v1, v2);
				coherence+=sim;
				counter++;
			}
		System.out.println("psi2: "+coherence/counter);
	}
	
	public static void printExpansionWeightSimilarity(String cacheFilename){
		try{
			BufferedReader br = Files.openFileReader(cacheFilename);
			String line = null;
			String currentQid = "";
			THashSet<TIntDoubleHashMap> mapSet = null; 
			TIntDoubleHashMap allMap = new TIntDoubleHashMap();
			while ((line=br.readLine())!=null){
				String[] entries = line.split(" ");
				int docid = Integer.parseInt(entries[0]);
				String qid = entries[1];
				int relevance = Integer.parseInt(entries[2]); 
				TIntDoubleHashMap map = new TIntDoubleHashMap();
				for (int i=3; i<entries.length; i++){
					String[] tokens = entries[i].split(",");
					int termid = Integer.parseInt(tokens[0]);
					double weight = Double.parseDouble(tokens[2]);
					map.put(termid, weight);
				}
				if (mapSet==null){
					// first line
					mapSet = new THashSet<TIntDoubleHashMap>();
					if (relevance == -2)
						allMap = map;
					else
						mapSet.add(map);
					currentQid = qid;
				}else if (currentQid.equals(qid)){
					// same query
					if (relevance == -2)
						allMap = map;
					else
						mapSet.add(map);
				}else{
					// new query
					// compute the coherence measure
					printExpansionWeightSimilarity(mapSet, allMap, currentQid);
					/** debugging: print content */
					/*if (currentQid.equals("314")){
						TIntDoubleHashMap[] scoreMaps = (TIntDoubleHashMap[])mapSet.toArray(new TIntDoubleHashMap[mapSet.size()]);
						for (int i=0; i<scoreMaps.length; i++){
							int[] keys = scoreMaps[i].keys();
							Arrays.sort(keys);
							for (int key : keys)
								System.err.print(key+":"+Rounding.toString(scoreMaps[i].get(key), 4)+" ");
							System.err.println();
						}
						int[] keys = allMap.keys();
						Arrays.sort(keys);
						for (int key: keys)
							System.err.print(key+":"+Rounding.toString(allMap.get(key), 4)+" ");
					}*/
						
					/** end of debugging */
					mapSet = new THashSet<TIntDoubleHashMap>();
					if (relevance == -2)
						allMap = map;
					else
						mapSet.add(map);
					allMap = new TIntDoubleHashMap();
					currentQid = qid;
				}
			}
			printExpansionWeightSimilarity(mapSet, allMap, currentQid);
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void labelFeedbackDocuments(String resultFilename, double threshold, String outputFilename){
		try{
			BufferedReader br = Files.openFileReader(resultFilename);
			String str = null;
			StringBuffer buf = new StringBuffer();
			while ((str=br.readLine())!=null){
				String[] tokens = str.split(" ");
				String qid = tokens[0];
				String docid = tokens[1];
				double bmap = Double.parseDouble(tokens[2]);
				double map = Double.parseDouble(tokens[3]);
				double percentage = Math.abs(map-bmap)/bmap;
				if (percentage>=threshold){
					if (map>bmap){
						String output = qid+" 0 "+docid+" 1";
						System.out.println(">>>>entry: "+str+", percentage: "+Rounding.toString(percentage*100, 2)+"%, output: "+output);
						buf.append(output+ApplicationSetup.EOL);
					}
					else{
						String output = qid+" 0 "+docid+" 0";
						System.out.println(">>>>entry: "+str+", percentage: "+Rounding.toString(percentage*100, 2)+"%, output: "+output);
						buf.append(output+ApplicationSetup.EOL);
					}
				}
					
			}
			br.close();
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			bw.write(buf.toString());
			bw.close();
			System.out.println("Done. Data saved in "+outputFilename);
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	/**
	 * Get the topX returned documents and write them to a file in qrels format. Ground truth is given by qrels. 
	 * @param qrels
	 * @param resultFilename
	 * @param topX
	 * @param outputFilename
	 */
	public static void getTopXFromResults(String qrelsFilename, String resultFilename, int topX, String outputFilename){
		TRECQrelsInMemory qrels = new TRECQrelsInMemory(qrelsFilename);
		TRECResultsInMemory results = new TRECResultsInMemory(resultFilename);
		System.out.println("Extracting top "+topX+" documents from "+resultFilename);
		String[] qids = results.getQueryids();
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<qids.length; i++){
			String[] docnos = results.getRetrievedDocnos(qids[i], topX-1);
			for (int j=0; j<docnos.length; j++){
				if (qrels.isRelevant(qids[i], docnos[j]))
					buf.append(qids[i]+" 0 "+docnos[j]+" 1"+ApplicationSetup.EOL);
				else
					buf.append(qids[i]+" 0 "+docnos[j]+" 0"+ApplicationSetup.EOL);
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
		System.out.println("Done. Saved in file "+outputFilename);
	}
	
	public static void printMeanFromDocumentCache(String cacheFilename){
		try{
			BufferedReader br = Files.openFileReader(cacheFilename);
			String line = null;
			String currentQid = null;
			double valueSum = 0d;
			int counter = 0;
			while ((line=br.readLine())!=null){
				String[] entries = line.split(" ");
				int docid = Integer.parseInt(entries[0]);
				String qid = entries[1];
				double value = Double.parseDouble(entries[2]);
				int relevance = Integer.parseInt(entries[3]);
				if (currentQid == null){
					// new query
					currentQid = qid;
					valueSum+=value;
					counter++;
				}else if (currentQid.equals(qid)){
					// same query
					valueSum+=value;
					counter++;
				}else{
					// new query
					System.out.println(currentQid+" "+valueSum/counter);
					valueSum=0;
					currentQid = qid;
					counter=0;
				}		
			}
			System.out.println(currentQid+" "+valueSum/counter);
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}

	public static void selectiveCE(String cacheFilename, double k){
		try{
			BufferedReader br = Files.openFileReader(cacheFilename);
			String line = null;
			TDoubleArrayList localAPList = new TDoubleArrayList();
			TDoubleArrayList localQEAPList = new TDoubleArrayList();
			TDoubleArrayList extQEAPList = new TDoubleArrayList();
			TDoubleArrayList localValueList = new TDoubleArrayList();
			TDoubleArrayList extValueList = new TDoubleArrayList();
			while ((line=br.readLine())!=null){
				StringTokenizer stk = new StringTokenizer(line.trim());
				try{
					localAPList.add(Double.parseDouble(stk.nextToken()));
					localQEAPList.add(Double.parseDouble(stk.nextToken()));
					extQEAPList.add(Double.parseDouble(stk.nextToken()));
					localValueList.add(Double.parseDouble(stk.nextToken()));
					extValueList.add(Double.parseDouble(stk.nextToken()));
				}catch(Exception e){
					System.err.println("line: "+line);
					e.printStackTrace();
					System.exit(1);
				}
			}
			br.close();
			double[] localAP = localAPList.toNativeArray();
			double[] localQEAP = localQEAPList.toNativeArray();
			double[] extQEAP = extQEAPList.toNativeArray();
			double[] localValue = localValueList.toNativeArray();
			double[] extValue = extValueList.toNativeArray();
			int nQ = localAP.length;
			int correct = 0; int wrong = 0;
			int noQE = 0; int localQE = 0; int extQE = 0;
			double[] selAP = new double[nQ];
			double[] oracleAP = new double[nQ];
			for (int i=0; i<nQ; i++){
				double[] values = {localAP[i], localQEAP[i], extQEAP[i]};
				Arrays.sort(values);
				oracleAP[i] = values[values.length-1];
				if (localValue[i]<k && extValue[i]<k){
					selAP[i] = localAP[i];
					noQE++;
				}
				else if (localValue[i]<extValue[i]){
					selAP[i] = extQEAP[i];
					extQE++;
				}else{
					selAP[i] = localQEAP[i];
					localQE++;
				}
				if (selAP[i]==oracleAP[i])
					correct++;
				else
					wrong++;
			}
			System.out.println("localMAP: "+Statistics.mean(localAP));
			System.out.println("localQEMAP: "+Statistics.mean(localQEAP));
			System.out.println("extQEMAP: "+Statistics.mean(extQEAP));
			System.out.println("selMAP: "+Statistics.mean(selAP));
			System.out.println("oracleMAP: "+Statistics.mean(oracleAP));
			System.out.println("correct: "+correct+", wrong: "+wrong);
			System.out.println("no QE: "+noQE+", localQE: "+localQE+", extQE: "+extQE);
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// --labelfeedbackdocs resultFilename threshold outputFilename
		// --labelfeedbackdocs /users/grad/ben/tr.ben/uniworkspace/etc/gov2/svm_train/feedbackDocs/singleTopics/fbresults 0.05 
		// /users/grad/ben/tr.ben/uniworkspace/etc/gov2/svm_train/feedbackDocs/singleTopics/fbresults.qrels
		if (args[0].equals("--labelfeedbackdocs")){
			SIGIR09.labelFeedbackDocuments(args[1], Double.parseDouble(args[2]), args[3]);
		}else if (args[0].equals("--gettopxfromresults")){
			// --gettopxfromresults qrelsFilename, resultFilename, topX, outputFilename
			SIGIR09.getTopXFromResults(args[1], args[2], Integer.parseInt(args[3]), args[4]);
		}else if (args[0].equals("--printexpweightsim")){
			// --printexpweightsim cacheFilename
			SIGIR09.printExpansionWeightSimilarity(args[1]);
		}else if (args[0].equals("--printmeanfromdoccache")){
			// --printmeanfromdoccache cachefilename
			SIGIR09.printMeanFromDocumentCache(args[1]);
		}else if (args[0].equals("--selcefromcache")){
			// --selcefromcache cachefilename k
			SIGIR09.selectiveCE(args[1], Double.parseDouble(args[2]));
		}

	}

}

package uk.ac.gla.terrier.applications;

import gnu.trove.THashSet;
import gnu.trove.TIntHashSet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import uk.ac.gla.terrier.evaluation.TRECQrelsInMemory;
import uk.ac.gla.terrier.statistics.Statistics;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.Rounding;
import uk.ac.gla.terrier.utility.Shuffling;
import uk.ac.gla.terrier.utility.StringUtility;

public class ECIR09 {
	/**
	 * Remove redundant docs that appear in 
	 * @param rootFolder
	 * @param rank
	 */
	public static void removeRedudantDocs(String rootFolder, int rank){
		
	}
	/**
	 * Randomly sample relevant and non-relevant docs
	 * @param rootFolder
	 * @param initRank
	 * @param endRank
	 */
	public static void randomSample(String rootFolder, int initRank, int endRank, int relDocs, int nonrelDocs){
		TIntHashSet[] relDocidSet = null;
		TIntHashSet[] nonrelDocidSet = null;
		for (int rank=initRank; rank<=endRank; rank++){
			String qrelsFilename = rootFolder+ApplicationSetup.FILE_SEPARATOR+"adhoc.qrels.merged_"+rank+".nonunjudged.docids";
			TRECQrelsInMemory qrels = new TRECQrelsInMemory(qrelsFilename);
			String[] queryids = qrels.getQueryids();
			Arrays.sort(queryids);
			// initialise sampled docid sets
			if (relDocidSet == null){
				relDocidSet = new TIntHashSet[queryids.length];
				nonrelDocidSet = new TIntHashSet[queryids.length];
				for (int i=0; i<queryids.length; i++){
					relDocidSet[i] = new TIntHashSet();
					nonrelDocidSet[i] = new TIntHashSet();
				}
			}
			StringBuffer buf = new StringBuffer();
			for (int i=0; i<queryids.length; i++){
				// sample relevant docs
				TIntHashSet thisSampledRelDocidSet = new TIntHashSet();
				TIntHashSet thisSampledNonRelDocidSet = new TIntHashSet();
				String[] docidStrs = qrels.getRelevantDocumentsToArray(queryids[i]);
				if (docidStrs == null || docidStrs.length == 0){
					System.out.println("No more rel docs for query "+queryids[i]);
					continue;
				}
				int[] relDocids = StringUtility.stringsToInts(docidStrs);
				int ignoredRel = 0; int ignoredNonrel = 0;
				int counter = 0;
				Collections.shuffle(Arrays.asList(relDocids));
				while (thisSampledRelDocidSet.size()<relDocs){
					//System.err.println("docidStrs.length: "+docidStrs.length);
					if (!relDocidSet[i].contains(relDocids[counter])){
						thisSampledRelDocidSet.add(relDocids[counter]);
						relDocidSet[i].add(relDocids[counter]);
					}else
						ignoredRel++;
					counter++;
					if (counter == relDocids.length)
						break;
					/*int rndInt = (int)(Math.random()*relDocids.length);
					if (!sampledRelDocidSet[i].contains(relDocids[rndInt])){
						thisSampledRelDocidSet.add(relDocids[rndInt]);
						sampledRelDocidSet[i].add(relDocids[rndInt]);
					}else
						ignoredRel++;*/
				}
				for (int docid : thisSampledRelDocidSet.toArray()){
					buf.append(queryids[i]+" 0 "+docid+" 1"+ApplicationSetup.EOL);
					//System.out.println(queryids[i]+" 0 "+docid+" 1");
				}
				relDocidSet[i].addAll(relDocids);
				// sample non-relevant docs
				docidStrs = qrels.getNonRelevantDocumentsToArray(queryids[i]);
				if (docidStrs == null || docidStrs.length == 0){
					System.out.println("No more non-rel docs for query "+queryids[i]);
					continue;
				}
				int[] nonrelDocids = StringUtility.stringsToInts(docidStrs);
					
				counter = 0;
				Collections.shuffle(Arrays.asList(nonrelDocids));
				while (thisSampledNonRelDocidSet.size()<nonrelDocs){
					if (!nonrelDocidSet[i].contains(nonrelDocids[counter])){
						thisSampledNonRelDocidSet.add(nonrelDocids[counter]);
						nonrelDocidSet[i].add(nonrelDocids[counter]);
					}else
						ignoredNonrel++;
					counter++;
					if (counter == nonrelDocids.length)
						break;
					/*int rndInt = (int)(Math.random()*nonrelDocids.length);
					if (!sampledNonRelDocidSet[i].contains(nonrelDocids[rndInt])){
						thisSampledNonRelDocidSet.add(nonrelDocids[rndInt]);
						sampledNonRelDocidSet[i].add(nonrelDocids[rndInt]);
					}else
						ignoredNonrel++;*/
				}
				for (int docid : thisSampledNonRelDocidSet.toArray()){
					buf.append(queryids[i]+" 0 "+docid+" 0"+ApplicationSetup.EOL);
					//System.out.println(queryids[i]+" 0 "+docid+" 0");
				}
				nonrelDocidSet[i].addAll(nonrelDocids);
				System.out.println("rank "+rank+", query "+queryids[i]+": sampled "+thisSampledRelDocidSet.size()+" rel "+
						thisSampledNonRelDocidSet.size()+" nonrel, ignored "+ignoredRel+" rel "+ignoredNonrel+" nonrel");
			}
			// write the sampled docids
			String outputFilename = qrelsFilename+".sampled_"+relDocs+"_"+nonrelDocs;
			try{
				BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
				bw.write(buf.toString());
				bw.close();
			}catch(IOException e){
				e.printStackTrace();
				System.exit(1);
			}
			qrels = null; buf = null; System.gc();
		}
	}
	
	public static String[] getRelevantDocnos(TRECQrelsInMemory[] qrels, String queryid){
		THashSet<String> docnoSet = new THashSet<String>();
		for (int i=0; i<qrels.length; i++){
			String[] docnos = qrels[i].getRelevantDocumentsToArray(queryid);
			for (int j=0; j<docnos.length; j++)
				docnoSet.add(docnos[j]);
		}
		return (String[])docnoSet.toArray(new String[docnoSet.size()]);
	}
	
	public static String[] getNonRelevantDocnos(TRECQrelsInMemory[] qrels, String queryid){
		THashSet<String> docnoSet = new THashSet<String>();
		for (int i=0; i<qrels.length; i++){
			String[] docnos = qrels[i].getNonRelevantDocumentsToArray(queryid);
			for (int j=0; j<docnos.length; j++)
				docnoSet.add(docnos[j]);
		}
		return (String[])docnoSet.toArray(new String[docnoSet.size()]);
	}
	
	/**
	 * Randomly sample relevant and non-relevant docs
	 * @param rootFolder
	 * @param initRank
	 * @param endRank
	 */
	public static void randomSample(String rootFolder, int initRank, int endRank, int interval, int relDocs, int nonrelDocs){
		TIntHashSet[] relDocidSet = null;
		TIntHashSet[] nonrelDocidSet = null;
		for (int rank=initRank; rank<=endRank; rank+=interval){
			TRECQrelsInMemory[] qrels = new TRECQrelsInMemory[interval];
			for (int i=0; i<interval; i++){
				String qrelsFilename = rootFolder+ApplicationSetup.FILE_SEPARATOR+"adhoc.qrels.merged_"+rank+".nonunjudged.docids";
				qrels[i] = new TRECQrelsInMemory(qrelsFilename);
			}
			//TRECQrelsInMemory qrels = new TRECQrelsInMemory(qrelsFilename);
			String[] queryids = qrels[0].getQueryids();
			Arrays.sort(queryids);
			// initialise sampled docid sets
			if (relDocidSet == null){
				relDocidSet = new TIntHashSet[queryids.length];
				nonrelDocidSet = new TIntHashSet[queryids.length];
				for (int i=0; i<queryids.length; i++){
					relDocidSet[i] = new TIntHashSet();
					nonrelDocidSet[i] = new TIntHashSet();
				}
			}
			StringBuffer buf = new StringBuffer();
			for (int i=0; i<queryids.length; i++){
				// sample relevant docs
				TIntHashSet thisSampledRelDocidSet = new TIntHashSet();
				TIntHashSet thisSampledNonRelDocidSet = new TIntHashSet();
				String[] docidStrs = getRelevantDocnos(qrels, queryids[i]);
				if (docidStrs == null || docidStrs.length == 0){
					System.out.println("No more rel docs for query "+queryids[i]);
					continue;
				}
				int[] relDocids = StringUtility.stringsToInts(docidStrs);
				int ignoredRel = 0; int ignoredNonrel = 0;
				int counter = 0;
				relDocids = Shuffling.shuffleInts(relDocids);
				while (thisSampledRelDocidSet.size()<relDocs){
					if (!relDocidSet[i].contains(relDocids[counter])){
						thisSampledRelDocidSet.add(relDocids[counter]);
						relDocidSet[i].add(relDocids[counter]);
					}else
						ignoredRel++;
					counter++;
					if (counter == relDocids.length)
						break;
					/*int rndInt = (int)(Math.random()*relDocids.length);
					if (!sampledRelDocidSet[i].contains(relDocids[rndInt])){
						thisSampledRelDocidSet.add(relDocids[rndInt]);
						sampledRelDocidSet[i].add(relDocids[rndInt]);
					}else
						ignoredRel++;*/
				}
				for (int docid : thisSampledRelDocidSet.toArray()){
					buf.append(queryids[i]+" 0 "+docid+" 1"+ApplicationSetup.EOL);
					//System.out.println(queryids[i]+" 0 "+docid+" 1");
				}
				relDocidSet[i].addAll(relDocids);
				// sample non-relevant docs
				docidStrs = getNonRelevantDocnos(qrels, queryids[i]);
				if (docidStrs == null || docidStrs.length == 0){
					System.out.println("No more non-rel docs for query "+queryids[i]);
					continue;
				}
				int[] nonrelDocids = StringUtility.stringsToInts(docidStrs);
					
				counter = 0;
				nonrelDocids = Shuffling.shuffleInts(nonrelDocids);
				while (thisSampledNonRelDocidSet.size()<nonrelDocs){
					if (!nonrelDocidSet[i].contains(nonrelDocids[counter])){
						thisSampledNonRelDocidSet.add(nonrelDocids[counter]);
						nonrelDocidSet[i].add(nonrelDocids[counter]);
					}else
						ignoredNonrel++;
					counter++;
					if (counter == nonrelDocids.length)
						break;
					/*int rndInt = (int)(Math.random()*nonrelDocids.length);
					if (!sampledNonRelDocidSet[i].contains(nonrelDocids[rndInt])){
						thisSampledNonRelDocidSet.add(nonrelDocids[rndInt]);
						sampledNonRelDocidSet[i].add(nonrelDocids[rndInt]);
					}else
						ignoredNonrel++;*/
				}
				for (int docid : thisSampledNonRelDocidSet.toArray()){
					buf.append(queryids[i]+" 0 "+docid+" 0"+ApplicationSetup.EOL);
					//System.out.println(queryids[i]+" 0 "+docid+" 0");
				}
				nonrelDocidSet[i].addAll(nonrelDocids);
				System.out.println("rank "+rank+", query "+queryids[i]+": sampled "+thisSampledRelDocidSet.size()+" rel "+
						thisSampledNonRelDocidSet.size()+" nonrel, ignored "+ignoredRel+" rel "+ignoredNonrel+" nonrel");
			}
			// write the sampled docids
			String outputFilename = rootFolder+ApplicationSetup.FILE_SEPARATOR+"adhoc.qrels.merged_"+
					rank+"-"+(rank+interval-1)+".nonunjudged.docids"+".sampled_"+relDocs+"_"+nonrelDocs;
			try{
				BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
				bw.write(buf.toString());
				bw.close();
			}catch(IOException e){
				e.printStackTrace();
				System.exit(1);
			}
			qrels = null; buf = null; System.gc();
		}
	}
	
	public static double[] loadValues(String filename){
		ArrayList<Double> valueList = new ArrayList<Double>();
		try{
			BufferedReader br = Files.openFileReader(filename);
			String str = null;
			while ((str=br.readLine())!=null){
				valueList.add(new Double(Double.parseDouble(str)));
			}
			br.close();
		}catch(IOException e){
			e.printStackTrace();
			System.exit(1);
		}
		double[] values = new double[valueList.size()];
		for (int i=0; i<values.length; i++)
			values[i] = valueList.get(i).doubleValue();
		return values;
	}
	/**
	 * Compute mean Pre@5 from a list of evaluation outputs.
	 * @param evalFilenameList
	 */
	public static void mergeEval(String evalFilenameList){
		ArrayList<String> fileList = new ArrayList<String>();
		try{
			BufferedReader br = Files.openFileReader(evalFilenameList);
			String str = null;
			while ((str=br.readLine())!=null){
				fileList.add(str);
			}
			br.close(); 
		}catch(IOException e){
			e.printStackTrace();
			System.exit(1);
		}
		int numberOfRuns = fileList.size();
		double[] values = loadValues(fileList.get(0));
		int numberOfQueries = values.length;
		double[][] p5 = new double[numberOfQueries][numberOfRuns];
		for (int i=0; i<numberOfQueries; i++)
			p5[i][0] = values[i];
		for (int i=1; i<numberOfRuns; i++){
			values = loadValues(fileList.get(i));
			for (int j=0; j<numberOfQueries; j++)
				p5[j][i] = values[j];
		}
		for (int i=0; i<numberOfQueries; i++)
			System.out.println(Rounding.toString(Statistics.mean(p5[i]), 4));
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args[0].equals("--samplefromqrels")){
			// --samplefromqrels rootFolder, initRank, endRank, interval, relDocs, nonrelDocs
			if (args.length==6)
				ECIR09.randomSample(args[1], 
						Integer.parseInt(args[2]), 
						Integer.parseInt(args[3]), 
						Integer.parseInt(args[4]), 
						Integer.parseInt(args[5]));
			else
				ECIR09.randomSample(args[1], 
						Integer.parseInt(args[2]), 
						Integer.parseInt(args[3]), 
						Integer.parseInt(args[4]), 
						Integer.parseInt(args[5]),
						Integer.parseInt(args[6]));
		}else if (args[0].equals("--mergeeval")){
			// --mergeeval evalFilenameList
			ECIR09.mergeEval(args[1]);
		}
	}

}

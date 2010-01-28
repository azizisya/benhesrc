package tests;

import gnu.trove.TDoubleHashSet;
import gnu.trove.TDoubleStack;
import gnu.trove.THashSet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.StringTokenizer;

import uk.ac.gla.terrier.evaluation.TRECQrelsInMemory;
import uk.ac.gla.terrier.statistics.Statistics;
import uk.ac.gla.terrier.structures.TRECQuery;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.SystemUtility;


/**
 * @author ben
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class TestBPref {
	//String[] measures = {"bpref", "map", "infAP", "ndcg_trec"};
	final String targetMeasure = "map";
	//String[] models = {"BM25", "PL2"};
	final int[] rates = //{1, 2, 3, 4, 5, 10};
		//{15, 20, 25, 30, 40, 50, 60, 70, 80, 90};
	//{15, 20, 25, 30, 40, 50, 60, 70, 80, 90};
	{1, 2, 3, 4, 5, 10, 15, 20, 25, 30, 40, 50, 60, 70, 80, 90};
	final int[] trecs = {6, 7, 9, 10};
	final String[] collections = {"disk45", "WT10G"};
	//String[] logrootdirs = {"d45_logs", "g10_logs"};
	final int bootstrep = 10;
	final String filenameSuffix = ".topics_train";
	final String EOL = ApplicationSetup.EOL;
	
	public void computeEvaluationResult(String rootDirname, String model, String outFilename, String incMeasure){
		File outFile = new File(outFilename);
		String evalSuffix = ApplicationSetup.getProperty("bpref.eval.suffix", "res.full.eval.gz");
		//String logdirPrefix="bpref_";
		String logdirSuffix = "_1-10";
		int trecCounter = 0;
		final String[] measures = {incMeasure, targetMeasure};
		double[][][] stds = new double[rates.length][trecs.length][measures.length];
		// /logs/1/bpref_disk45_trec6.topics_BM25_1-10/1/bpref.eval.gz
		// d45_logs/1/bpref_disk45_trec6.topics_PL2_1-10/1/map.res.eval
		for (int i=0; i<collections.length; i++){
			for (int j=0;j<rates.length; j++){
				for (int k=trecCounter; k<trecCounter+2; k++){//trec
					double[] bprefB = new double[bootstrep];
					double[] bprefM = new double[bootstrep];
					double[] mapB = new double[bootstrep];
					double[] mapM = new double[bootstrep];
					double[] bprefDiff = new double[bootstrep];
					double[] mapDiff = new double[bootstrep];
					for (int t=1; t<=bootstrep; t++){
						String bprefEvalFilename = rootDirname+"/"+
								//logrootdirs[i] + "/" +
								rates[j] + "/"+
								incMeasure+"_"+collections[i]+"_trec"+trecs[k]+".topics_"+
								model+logdirSuffix+"/"+
								t+"/"+
								incMeasure+evalSuffix;
						String mapEvalFilename = rootDirname+"/"+
								//logrootdirs[i] + "/" +
								rates[j] + "/"+
								targetMeasure+"_"+collections[i]+"_trec"+trecs[k]+".topics_"+
								model+logdirSuffix+"/"+
								t+"/"+
								targetMeasure+evalSuffix;
						
						bprefB[t-1] = (incMeasure.equals("ind_map"))?
								(SystemUtility.loadAllEvalMeasure(bprefEvalFilename, "map")):
									(SystemUtility.loadAllEvalMeasure(bprefEvalFilename, incMeasure));
						bprefM[t-1] = (incMeasure.equals("ind_map"))?
								(SystemUtility.loadAllEvalMeasure(mapEvalFilename, "map")):
									(SystemUtility.loadAllEvalMeasure(mapEvalFilename, incMeasure));
						mapB[t-1] = (targetMeasure.equals("ind_map"))?
								(SystemUtility.loadAllEvalMeasure(bprefEvalFilename, "map")):
									(SystemUtility.loadAllEvalMeasure(bprefEvalFilename, targetMeasure));
						mapM[t-1] = (targetMeasure.equals("ind_map"))?
								(SystemUtility.loadAllEvalMeasure(mapEvalFilename, "map")):
									(SystemUtility.loadAllEvalMeasure(mapEvalFilename, targetMeasure));
						bprefDiff[t-1] =  Math.abs(bprefB[t-1] - bprefM[t-1]);
						mapDiff[t-1] =  Math.abs(mapB[t-1] - mapM[t-1]);
						
						/*System.out.println("bprefFile: "+bprefEvalFilename);
						System.out.println("mapFile: "+mapEvalFilename);
						System.out.println("bprefB: "+bprefB[t-1]);
						System.out.println("bprefM: "+bprefM[t-1]);
						System.out.println("mapB: "+mapB[t-1]);
						System.out.println("mapM: "+mapM[t-1]);
						System.out.println("bprefDiff:" +bprefDiff[t-1]);
						System.out.println("mapDiff:" +mapDiff[t-1]);*/
					}
					bprefDiff = Statistics.normaliseMaxMin(bprefDiff);
					mapDiff = Statistics.normaliseMaxMin(mapDiff);
					stds[j][k][0] = 1/Statistics.standardDeviation(bprefDiff);
					stds[j][k][1] = 1/Statistics.standardDeviation(mapDiff);
						//Math.abs(Statistics.mean(bprefB)-Statistics.mean(bprefM));
					//diffs[j][k][1] = Math.abs(Statistics.mean(mapB)-Statistics.mean(mapM));
				}
			}
			trecCounter+=2;
		}
		StringBuffer buffer = new StringBuffer();
		int counter = 0;
		int ties = 0;
		int total=stds.length*stds[0].length;
		for (int i=0; i<rates.length; i++){
			buffer.append(rates[i]+" ");
			for (int j=0; j<trecs.length; j++){
				for (int k=0; k<measures.length; k++){
					buffer.append(stds[i][j][k]+" ");
				}
				if (stds[i][j][0]>stds[i][j][1])
					counter++;
				else if (stds[i][j][0]==stds[i][j][1])
					ties++;		
			}
			buffer.append(EOL);
		}
		try{
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outFile);
			bw.write(buffer.toString());
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		System.out.println(measures[0]+" > "+counter+ " =  "+ties+" out of "+total+" cases.");
	}
	
	/**
	  * 
	  * @param percentage The percentage of relevant documents to be removed per query. 
	  * @param outputFilename
	  */
	public void randomQrelsRemoval(double percentage, String outputFilename){
		// File qrels = new File(qrelsFilename);
		 File outputFile = new File(outputFilename);
		 TRECQrelsInMemory qrels = new TRECQrelsInMemory();
		 TRECQuery queries = new TRECQuery();
		 StringBuffer buffer = new StringBuffer();
		 while (queries.hasMoreQueries()){
			 queries.nextQuery();
			 String queryid = queries.getQueryId();
			 System.out.print("Removing for query "+queryid+"...");
			 
			 THashSet docnos = qrels.getRelevantDocuments(queryid);
			 String[] docnoString = qrels.getRelevantDocumentsToArray(queryid);
			 int numberOfDocsToRemove = (int)((double)docnos.size()*percentage);
			 if (numberOfDocsToRemove == docnos.size())
				 numberOfDocsToRemove--;
			 int numberOfDocs = docnos.size();
			 for (int i=0; i<numberOfDocsToRemove; i++){
				 double random = Math.random();
				 String docno = docnoString[(int)(random*numberOfDocs)];
				 if (docnos.contains(docno))
					 docnos.remove(docno);
				 else
					 i--;
			 }
			 docnoString = (String[])docnos.toArray(new String[docnos.size()]);
			 int N = docnoString.length;
			 for (int i=0;i<N;i++){
				buffer.append(queryid+" 0 "+docnoString[i]+" 1"+EOL);
			 }
			 System.out.print("Removed "+numberOfDocsToRemove+" out of "+numberOfDocs +" rel and ");
			 
			 docnos = qrels.getNonRelevantDocuments(queryid);
			 docnoString = qrels.getNonRelevantDocumentsToArray(queryid);
			 numberOfDocsToRemove = (int)((double)docnos.size()*percentage);
			 if (numberOfDocsToRemove == docnos.size())
				 numberOfDocsToRemove--;
			 numberOfDocs = docnos.size();
			 for (int i=0; i<numberOfDocsToRemove; i++){
				 double random = Math.random();
				 String docno = docnoString[(int)(random*numberOfDocs)];
				 if (docnos.contains(docno))
					 docnos.remove(docno);
				 else
					 i--;
			 }
			 docnoString = (String[])docnos.toArray(new String[docnos.size()]);
			 N = docnoString.length;
			 for (int i=0;i<N;i++){
				 buffer.append(queryid+" 0 "+docnoString[i]+" 0"+EOL);
			 }
			 System.out.println(numberOfDocsToRemove+" out of "+numberOfDocs +" nonrel docs.");

		 }
		 System.out.print("writting to file "+outputFilename+"...");
		 try{
			 BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
			 bw.write(buffer.toString());
			 bw.close();
		 }catch(IOException ioe){
			 ioe.printStackTrace();
			 System.exit(1);
		 }
		 System.out.println("Done!");
	 }
	
	public static void writeUnjudged(String qrelsFilename, String partialQrelsFilename){
		TRECQrelsInMemory parqrels = new TRECQrelsInMemory(partialQrelsFilename);
		// for each query in partial qrels
		String outputFilename = partialQrelsFilename+".unj";
		String[] queryids = parqrels.getQueryids();
		try{
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			BufferedReader br = Files.openFileReader(qrelsFilename);
			String currentQueryid = "";
			String str = null;
			THashSet<String> retDocSet = null;
			while ((str=br.readLine())!=null){
				str = str.trim();
				if (str.length()==0) continue;
				StringTokenizer stk = new StringTokenizer(str);
				String queryid = stk.nextToken();
				stk.nextToken();
				String docno = stk.nextToken();
				if (!queryid.equals(currentQueryid)){
					retDocSet = new THashSet<String>();
					currentQueryid = ""+queryid;
					String[] relDocs = parqrels.getRelevantDocumentsToArray(queryid);
					if (relDocs!=null)
						for (int i=0; i<relDocs.length; i++)
							retDocSet.add(relDocs[i]);
					String[] nonRelDocs = parqrels.getNonRelevantDocumentsToArray(queryid);
					if (nonRelDocs!=null)
						for (int i=0; i<nonRelDocs.length; i++)
							retDocSet.add(nonRelDocs[i]);
				}
				String grade = stk.nextToken();
				if (retDocSet.contains(docno))
					bw.write(str+ApplicationSetup.EOL);
				else
					bw.write(queryid+" 0 "+docno+" -1"+ApplicationSetup.EOL);
			}
			bw.close(); br.close();
		}catch(IOException ioe){
			 ioe.printStackTrace();
			 System.exit(1);
		 }
		
		parqrels = null;
	}
	
	public void computeMeanEvaluationResult(
			String rootDirname, 
			String model, 
			String outFilename,
			String incMeasure,
			int scalor){
		File outFile = new File(outFilename);
		String evalSuffix = ApplicationSetup.getProperty("bpref.eval.suffix", ".res.full.eval.gz");
		//String logdirPrefix="bpref_";
		String logdirSuffix = "_1-10";
		int trecCounter = 0;
		final String[] measures = {incMeasure, targetMeasure};
		double[][][] means = new double[rates.length][trecs.length][measures.length*2];
		double[][][] stds = new double[rates.length][trecs.length][measures.length*2];
		// /logs/1/bpref_disk45_trec6.topics_BM25_1-10/1/bpref.eval.gz
		// d45_logs/1/bpref_disk45_trec6.topics_PL2_1-10/1/map.res.eval
		for (int i=0; i<collections.length; i++){
			for (int j=0;j<rates.length; j++){
				for (int k=trecCounter; k<trecCounter+2; k++){//trec
					double[] bprefB = new double[bootstrep];
					double[] bprefM = new double[bootstrep];
					double[] mapB = new double[bootstrep];
					double[] mapM = new double[bootstrep];
					double[] bprefDiff = new double[bootstrep];
					double[] mapDiff = new double[bootstrep];
					for (int t=1; t<=bootstrep; t++){
						String bprefEvalFilename = rootDirname+"/"+
								//logrootdirs[i] + "/" +
								rates[j] + "/"+
								incMeasure+"_"+collections[i]+"_trec"+trecs[k]+".topics_"+
								model+logdirSuffix+"/"+
								t+"/"+
								incMeasure+evalSuffix;
						String mapEvalFilename = rootDirname+"/"+
								//logrootdirs[i] + "/" +
								rates[j] + "/"+
								targetMeasure+"_"+collections[i]+"_trec"+trecs[k]+".topics_"+
								model+logdirSuffix+"/"+
								t+"/"+
								targetMeasure+evalSuffix;
						
						bprefB[t-1] = (incMeasure.equals("ind_map"))?
								(SystemUtility.loadAllEvalMeasure(bprefEvalFilename, "map")):
									(SystemUtility.loadAllEvalMeasure(bprefEvalFilename, incMeasure));
						bprefM[t-1] = (incMeasure.equals("ind_map"))?
								(SystemUtility.loadAllEvalMeasure(mapEvalFilename, "map")):
									(SystemUtility.loadAllEvalMeasure(mapEvalFilename, incMeasure));
						mapB[t-1] = (targetMeasure.equals("ind_map"))?
								(SystemUtility.loadAllEvalMeasure(bprefEvalFilename, "map")):
									(SystemUtility.loadAllEvalMeasure(bprefEvalFilename, targetMeasure));
						mapM[t-1] = (targetMeasure.equals("ind_map"))?
								(SystemUtility.loadAllEvalMeasure(mapEvalFilename, "map")):
									(SystemUtility.loadAllEvalMeasure(mapEvalFilename, targetMeasure));
						/*bprefDiff[t-1] =  Math.abs(bprefB[t-1] - bprefM[t-1]);
						mapDiff[t-1] =  Math.abs(mapB[t-1] - mapM[t-1]);*/
						
						/*System.out.println("bprefFile: "+bprefEvalFilename);
						System.out.println("mapFile: "+mapEvalFilename);
						System.out.println("bprefB: "+bprefB[t-1]);
						System.out.println("bprefM: "+bprefM[t-1]);
						System.out.println("mapB: "+mapB[t-1]);
						System.out.println("mapM: "+mapM[t-1]);
						System.out.println("bprefDiff:" +bprefDiff[t-1]);
						System.out.println("mapDiff:" +mapDiff[t-1]);*/
					}
					means[j][k][0] = Statistics.mean(bprefB);
					means[j][k][1] = Statistics.mean(bprefM);
					means[j][k][2] = Statistics.mean(mapB);
					means[j][k][3] = Statistics.mean(mapM);
					stds[j][k][0] = Statistics.standardDeviation(bprefB)/scalor;
					stds[j][k][1] = Statistics.standardDeviation(bprefM)/scalor;
					stds[j][k][2] = Statistics.standardDeviation(mapB)/scalor;
					stds[j][k][3] = Statistics.standardDeviation(mapM)/scalor;
						//Math.abs(Statistics.mean(bprefB)-Statistics.mean(bprefM));
					//diffs[j][k][1] = Math.abs(Statistics.mean(mapB)-Statistics.mean(mapM));
				}
			}
			trecCounter+=2;
		}
		StringBuffer buffer = new StringBuffer();
		int counter = 0;
		int ties = 0;
		int total=means.length*means[0].length;
		for (int i=0; i<rates.length; i++){
			buffer.append(rates[i]+" ");
			for (int j=0; j<trecs.length; j++){
				//for (int k=0; k<measures.length*2; k++){
				//	buffer.append(means[i][j][k]+" "/*+stds[i][j][k]+" "*/);
				//}
				buffer.append(means[i][j][2]+" "+means[i][j][3]+" ");
				/**
				if (stds[i][j][0]>stds[i][j][1])
					counter++;
				else if (stds[i][j][0]==stds[i][j][1])
					ties++;	
				*/
				if (means[i][j][2]>means[i][j][3])
					counter++;
				else if (means[i][j][2]==means[i][j][3])
					ties++;
			}
			buffer.append(EOL);
		}
		try{
			BufferedWriter bw = new BufferedWriter(new FileWriter(outFile));
			bw.write(buffer.toString());
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		System.out.println(measures[0]+" > "+counter+ " =  "+ties+" out of "+total+" cases.");
	}
	
	public void computeStd(String dirname, String model, String outFilename, String incMeasure){
		File outFile = new File(outFilename);
		double[][][] vars = new double[rates.length][trecs.length][2];
		final String[] measures = {incMeasure, targetMeasure};
		for (int i=0; i<trecs.length; i++){
			for (int j=0; j<2; j++){
				for (int k=0; k<rates.length; k++){
					String prefix = model+"_"+measures[j]+"_"+rates[k] + "_trec" + trecs[i];
					String filename = prefix.concat(filenameSuffix);
					vars[k][i][j] = TestBPref.getStdOfOptimisedParameterValues(
							dirname+"/"+filename);
				}
			}
		}
		
		StringBuffer buffer = new StringBuffer();
		int counter = 0;
		int ties = 0;
		int total=rates.length*trecs.length;
		for (int i=0; i<rates.length; i++){
			buffer.append(rates[i]+" ");
			for (int j=0; j<trecs.length; j++){
				for (int k=0; k<measures.length; k++){
					buffer.append(1d/vars[i][j][k]+" ");
				}
				if (vars[i][j][0]>vars[i][j][1])
					counter++;
	                        else if (vars[i][j][0]==vars[i][j][1])
	                                ties++;
			}
			buffer.append(EOL);
		}
		try{
			BufferedWriter bw = new BufferedWriter(new FileWriter(outFile));
			bw.write(buffer.toString());
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		System.out.println(measures[0]+" > "+counter+ " =  "+ties+" out of "+total+" cases.");
		
	}
	
	public static void printStdOfOptimisedParameterValues(String filename){
		double var = getStdOfOptimisedParameterValues(filename);
		System.out.println(filename+" var "+var);
	}
	
	public static void printMeanValues(String filename){
		try{
			BufferedReader br = Files.openFileReader(filename);
			String str = null;
			while ((str=br.readLine())!=null){
				str = str.trim();
				if (str.length()==0) continue;
				String[] tokens = str.split(" ");
				double[] values = new double[tokens.length-1];
				for (int i=0; i<values.length; i++)
					values[i] = Double.parseDouble(tokens[i+1]);
				System.out.println(tokens[0]+": "+Statistics.mean(values));
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public static double getStdOfOptimisedParameterValues(String filename){
		double var = 0;
		try{
			BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
			TDoubleHashSet valueSet = new TDoubleHashSet();
			String str = null;
			while ((str=br.readLine())!=null){
				valueSet.add(Double.parseDouble(str));
				/*if (valueSet.size()>=5)
					break;*/
			}
			br.close();
			double[] values = valueSet.toArray();
			var = Statistics.standardDeviation(values);
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		return var;
	}
	public static void printOptions(){
		System.out.println("--printstd <filename> ");
		System.out.println("--computestdpara <dir> <model> <outfilename> <incMeasure>");
		System.out.println("--computestdeval <dir> <model> <outfilename> <incMeasure>");
		System.out.println("--computemeaneval <dir> <model> <outfilename> <incMeasure> <scalor>");
		System.out.println("--printmeanvalues <filename>");
		System.out.println("--writeunjudgedqrels <qrelsfilename> <partialQrelsFilename>");
	}

	public static void main(String[] args) {
		try{
			if (args[0].equals("--printstd")){
				TestBPref.printStdOfOptimisedParameterValues(args[1]);
			}else if(args[0].equals("--computestdpara")){
				TestBPref app = new TestBPref();
				app.computeStd(args[1], args[2], args[3], args[4]);
			}else if(args[0].equals("--computestdeval")){
				TestBPref app = new TestBPref();
				app.computeEvaluationResult(args[1], args[2], args[3], args[4]);
			}else if(args[0].equals("--computemeaneval")){
				TestBPref app = new TestBPref();
				app.computeMeanEvaluationResult(args[1], args[2], args[3], args[4], Integer.parseInt(args[5]));
			}else if(args[0].equals("--writeunjudgedqrels")){
				writeUnjudged(args[1], args[2]);
			}else if(args[0].equals("--printmeanvalues")){
				TestBPref.printMeanValues(args[1]);
			}
			else if (args[0].equals("--help"))
				TestBPref.printOptions();
			else{
				TestBPref.printOptions();
			}
		}catch(java.lang.ArrayIndexOutOfBoundsException e){
			TestBPref.printOptions();
		}
	}
}

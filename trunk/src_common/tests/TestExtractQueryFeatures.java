/*
 * Created on 2005-1-1
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package tests;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.Vector;

import uk.ac.gla.terrier.clustering.ClusteringMethod;

import uk.ac.gla.terrier.matching.BufferedMatching;

//import stemming.PorterStemmer;
//import stemming.Stemmer;
import uk.ac.gla.terrier.structures.BasicQuery;
import uk.ac.gla.terrier.structures.CVector;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.QueryFeatures;
import uk.ac.gla.terrier.structures.TRECQuery;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.PorterStopPipeline;
import uk.ac.gla.terrier.utility.ResultForEachQuery;
import uk.ac.gla.terrier.utility.Rounding;
//import utility.StopWordList;

/**
 * @author ben
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class TestExtractQueryFeatures {
	protected TRECQuery queries = new TRECQuery();
	
	//protected Stemmer stemmer = new PorterStemmer();
	
	//protected StopWordList stop = new StopWordList();
	
	protected PorterStopPipeline pipe = new PorterStopPipeline();
	
	protected int numberOfFeatures;
	
	protected BufferedMatching matching = new BufferedMatching(Index.createIndex());
	
	public double[][] extractQueryFeatures(){
		double[][] qfs = new double[queries.getNumberOfQueries()][6];
		this.numberOfFeatures = qfs[0].length;
		int counter = 0;
		while(queries.hasMoreQueries()){
			String queryString = queries.nextQuery();
			String queryid = queries.getQueryId();
			if (this.ignoreQuery(queryid))
				continue;
			QueryFeatures qf = new QueryFeatures(new BasicQuery(queryString, queryid, pipe), matching);
			qfs[counter] = qf.getFeatureVector();
			counter++;
		}
		queries.reset();
		return qfs;
	}
	
	public double[] extractQueryFeatures(String queryid){ 
		String queryString = queries.getQuery(queryid);
		QueryFeatures qf = new QueryFeatures(new BasicQuery(queryString, queryid, pipe), matching);
		return qf.getFeatureVector();
	}
	
	public boolean ignoreQuery(String queryid){
		if (queryid.equals("672"))
			return true;
		return false;
	}
	
	public void writeQueryFeatures(File fOut){
		StringBuffer buffer = new StringBuffer();
		String EOL = ApplicationSetup.EOL;
		String sep = ":";
		String space = " ";
		try{
			while(queries.hasMoreQueries()){
				String str = queries.nextQuery();
				String queryid = queries.getQueryId();
				if (this.ignoreQuery(queryid))
					continue;
				System.out.println(">>>>>>>>>>>>Extracting features of query " + queryid);
				double[] qfs = this.extractQueryFeatures(queryid);
				buffer.append("100");
				for (int i = 0; i < qfs.length; i++)
					buffer.append(space+(i+1)+sep+Rounding.toString(qfs[i], 6));
				buffer.append(EOL);
			}
			BufferedWriter bw = new BufferedWriter(new FileWriter(fOut));
			bw.write(buffer.toString());
			bw.close();
			queries.reset();
		}
		catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public void writeClusterQueryFeatures(File[] fResult, File fOut, ClusteringMethod cmethod){
		StringBuffer buffer = new StringBuffer();
		String EOL = ApplicationSetup.EOL;
		String sep = ":";
		String space = " ";
		HashSet queryidsBuf = new HashSet();
		HashSet qfBuf = new HashSet();
		
		ResultForEachQuery[] results = new ResultForEachQuery[fResult.length];
		for (int i = 0; i < fResult.length; i++){
			results[i] = new ResultForEachQuery(fResult[i]);
			//System.out.println("class "+i+": "+results[i].model);
			while (results[i].hasMoreQueries()){
				String queryid = results[i].nextQueryid();
				if (queryidsBuf.contains(queryid))
					continue;
				queryidsBuf.add(queryid);
				double[] qf = extractQueryFeatures(queryid);
				qfBuf.add(qf);
			}
		}
		
		String[] queryids = new String[queryidsBuf.size()];
		queryidsBuf.toArray(queryids); 
		double[][] qfs = new double[qfBuf.size()][this.numberOfFeatures];
		qfBuf.toArray(qfs);
		CVector[] elements = new CVector[queryids.length]; 
		for (int i = 0; i < elements.length; i++)
			elements[i] = new CVector(qfs[i], queryids[i]);
		cmethod.setElements(elements);
		cmethod.cluster();
		
		for (int i = 0; i < queryids.length; i++){
			buffer.append(cmethod.getClusterNo(queryids[i]));
			for (int j = 0; j < qfs[i].length; j++)
				buffer.append(space+(j+1)+sep+qfs[i][j]);
			buffer.append(EOL);
		}
		
//		int[] bestPerformingModel = new int[queryids.length];
//		for (int i = 0; i < queryids.length; i++){
//			int bestIndex = 0;
//			double bestAP = results[0].getAveragePrecision(queryids[i]);
//			for (int j = 1; j < results.length; j++){
//				double currentAP = results[j].getAveragePrecision(queryids[i]);
//				if (currentAP > bestAP){
//					bestAP = currentAP;
//					bestIndex = j;
//				} else if(currentAP==bestAP && 
//						results[j].meanAveragePrecision > results[bestIndex].meanAveragePrecision){
//					bestAP = currentAP;
//					bestIndex = j;
//				}
//			}
//			buffer.append(bestIndex);
//			for (int j = 0; j < qfs[i].length; j++){
//				buffer.append(space+(j+1)+sep+qfs[i][j]);
//			}
//			buffer.append(EOL);
//		}
//		
		try{
			BufferedWriter bw = new BufferedWriter(new FileWriter(fOut));
			bw.write(buffer.toString());
			bw.close();
		}
		catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public void writeClassQueryFeatures(File[] fResult, File fOut){
		StringBuffer buffer = new StringBuffer();
		String EOL = ApplicationSetup.EOL;
		String sep = ":";
		String space = " ";
		HashSet queryidsBuf = new HashSet();
		HashSet qfBuf = new HashSet();
		
		ResultForEachQuery[] results = new ResultForEachQuery[fResult.length];
		for (int i = 0; i < fResult.length; i++){
			results[i] = new ResultForEachQuery(fResult[i]);
			System.out.println("class "+i+": "+results[i].model);
			while (results[i].hasMoreQueries()){
				String queryid = results[i].nextQueryid();
				if (queryidsBuf.contains(queryid))
					continue;
				queryidsBuf.add(queryid);
				double[] qf = extractQueryFeatures(queryid);
				qfBuf.add(qf);
			}
		}
		
		String[] queryids = new String[queryidsBuf.size()];
		queryidsBuf.toArray(queryids); 
		double[][] qfs = new double[qfBuf.size()][this.numberOfFeatures];
		qfBuf.toArray(qfs);
		
		int[] bestPerformingModel = new int[queryids.length];
		for (int i = 0; i < queryids.length; i++){
			int bestIndex = 0;
			double bestAP = results[0].getAveragePrecision(queryids[i]);
			for (int j = 1; j < results.length; j++){
				double currentAP = results[j].getAveragePrecision(queryids[i]);
				if (currentAP > bestAP){
					bestAP = currentAP;
					bestIndex = j;
				} else if(currentAP==bestAP && 
						results[j].meanAveragePrecision > results[bestIndex].meanAveragePrecision){
					bestAP = currentAP;
					bestIndex = j;
				}
			}
			buffer.append(bestIndex);
			for (int j = 0; j < qfs[i].length; j++){
				buffer.append(space+(j+1)+sep+qfs[i][j]);
			}
			buffer.append(EOL);
		}
		
		try{
			BufferedWriter bw = new BufferedWriter(new FileWriter(fOut));
			bw.write(buffer.toString());
			bw.close();
		}
		catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public void writeAveragePrecisionQueryFeatures(File fResult, File fOut){
		//System.out.println("number of queries: " + queries.getNumberOfQueries());
		StringBuffer buffer = new StringBuffer();
		String EOL = ApplicationSetup.EOL;
		String sep = ":";
		String space = " ";
		try{
			BufferedReader br = new BufferedReader(new FileReader(fResult));
			String str = null;
			while((str=br.readLine())!=null){
				if (str.trim().length()==0)
					continue;
				StringTokenizer stk = new StringTokenizer(str);
				String queryid = stk.nextToken();
				if (this.ignoreQuery(queryid))
					continue;
				String ap = stk.nextToken();
				System.out.println(">>>>>>>>>>>>Extracting features of query " + queryid);
				double[] qfs = this.extractQueryFeatures(queryid);
				buffer.append(ap);
				for (int i = 0; i < qfs.length; i++)
					buffer.append(space+(i+1)+sep+Rounding.toString(qfs[i], 6));
				buffer.append(EOL);
			}
			br.close();
			BufferedWriter bw = new BufferedWriter(new FileWriter(fOut));
			bw.write(buffer.toString());
			bw.close();
			queries.reset();
		}
		catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}

	public static void main(String[] args) {
		// -r -q -ap <.apq> <fOut>
		if (args[2].equalsIgnoreCase("-ap")){
			File fResult = new File(ApplicationSetup.TREC_RESULTS, args[3]);
			File fOut = new File(ApplicationSetup.TREC_RESULTS, args[4]);
			TestExtractQueryFeatures app = new TestExtractQueryFeatures();
			app.writeAveragePrecisionQueryFeatures(fResult, fOut);
			System.out.println("Data saved in file " + fOut.getAbsoluteFile());
			System.out.println("Finished.");
		}
		
		if (args[2].equalsIgnoreCase("-c")){			
			File fDir = new File(ApplicationSetup.TREC_RESULTS);
			String[] filenames = fDir.list();
			Vector files = new Vector();
			for (int i = 0; i <filenames.length; i++)
				if (filenames[i].endsWith(".apq"))
					files.addElement(new File(ApplicationSetup.TREC_RESULTS, filenames[i]));
			File fOut = new File(ApplicationSetup.TREC_RESULTS, args[3]);
			TestExtractQueryFeatures app = new TestExtractQueryFeatures();
			app.writeClassQueryFeatures((File[])files.toArray(new File[files.size()]), fOut);
			System.out.println("Data saved in file " + fOut.getAbsoluteFile());
			System.out.println("Finished.");
		}
		
		// -r -q -clst <clusteringMethodName> <output filename>
		if (args[2].equalsIgnoreCase("-clst")){			
			File fDir = new File(ApplicationSetup.TREC_RESULTS);
			String[] filenames = fDir.list();
			Vector files = new Vector();
			for (int i = 0; i <filenames.length; i++)
				if (filenames[i].endsWith(".apq"))
					files.addElement(new File(ApplicationSetup.TREC_RESULTS, filenames[i]));
			String methodName = args[3];
			if (methodName.lastIndexOf('.') < 0)
				methodName = "clustering.".concat(methodName);
			ClusteringMethod cmethod = null;
			try{
				cmethod = (ClusteringMethod)Class.forName(methodName).newInstance();
			}
			catch(Exception e){
				e.printStackTrace();
				System.exit(1);
			}
			
			File fOut = new File(ApplicationSetup.TREC_RESULTS, args[4]);
			TestExtractQueryFeatures app = new TestExtractQueryFeatures();
			app.writeClusterQueryFeatures((File[])files.toArray(new File[files.size()]), fOut, cmethod);
			System.out.println("Data saved in file " + fOut.getAbsoluteFile());
			System.out.println("Finished.");
		}
		
		if (args[2].equalsIgnoreCase("-t")){
			File fOut = new File(ApplicationSetup.TREC_RESULTS, args[3]);
			TestExtractQueryFeatures app = new TestExtractQueryFeatures();
			app.writeQueryFeatures(fOut);
			System.out.println("Data saved in file " + fOut.getAbsoluteFile());
			System.out.println("Finished.");
		}
	}
}

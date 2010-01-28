/*
 * Created on 2004-12-28
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package tests;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

import uk.ac.gla.terrier.matching.BufferedMatching;
import uk.ac.gla.terrier.structures.*;
import uk.ac.gla.terrier.simulation.TFRanking;
import uk.ac.gla.terrier.structures.TRECQuery;
import uk.ac.gla.terrier.structures.trees.TermTreeNode;
import uk.ac.gla.terrier.statistics.Statistics;
import uk.ac.gla.terrier.tfnormalisation.*;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.BubbleSort;
import uk.ac.gla.terrier.utility.PorterStopPipeline;
import uk.ac.gla.terrier.utility.Rounding;
import uk.ac.gla.terrier.utility.SystemUtility;

/**
 * @author ben
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TestRetrieval {
	
	protected BufferedMatching matching;
	
	protected TermFrequencyNormalisation method;
	
	protected CollectionStatistics collSta;
	
	protected final boolean debugging = new Boolean(
			ApplicationSetup.getProperty("debugging.mode", "false")).booleanValue();
	
	//protected Stemmer stemmer = new PorterStemmer();
	
	//protected StopWordList stop = new StopWordList();
	
	protected int BIN_NUMBER;
	
	protected TFRanking tfRanking; 
	
	protected final String normPackagePrefix = "uk.ac.gla.terrier.tfnormalisation.";
	
	protected final String normMethodPrefix = "Normalisation";
	
	protected int querytype = SystemUtility.queryType();
	
	public TestRetrieval(){
		this.BIN_NUMBER = Integer.parseInt(ApplicationSetup.getProperty("bin.number", "1000"));
		Index index = Index.createIndex();
		this.matching = new BufferedMatching(index);
		collSta = index.getCollectionStatistics();
//		this.tfRanking = new TFRanking(
//				Index.createIndex(ApplicationSetup.TERRIER_INDEX_PATH).getLexicon());
	}
	
	public void processnl(String methodName, double parameter){
		TRECQuery queries = new TRECQuery();
		double[] varDocLength = new double[queries.getNumberOfQueries()];
		double[] varNE = new double[queries.getNumberOfQueries()];
		double[] varSum = new double[queries.getNumberOfQueries()];
		double[] covar = new double[queries.getNumberOfQueries()];
		double[] corr = new double[queries.getNumberOfQueries()];
		int counter = 0;
		PorterStopPipeline pipe = new PorterStopPipeline();
		while (queries.hasMoreQueries()){
			String queryTerms = queries.nextQuery();
			BasicQuery query = new BasicQuery(queryTerms, queries.getQueryId(), pipe);
			this.setNormalisation(methodName, parameter);
			double[] data = this.checkCovarianceNELength(matching, query);
			System.out.println(">>>>>>>Queryid: " + query.getQueryNumber() +
				", query type: " + SystemUtility.queryType());
			System.out.println("Var(docLength): " + data[0]);
			System.out.println("Var(ne): " + data[1]);
			System.out.println("Var(lengthPlusNE: )" + data[2]);		
			System.out.println("Covariance: " + data[3]);
			System.out.println("Correlation: " + data[4]);
			varDocLength[counter] = data[0];
			varNE[counter] = data[1];
			varSum[counter] = data[2];
			covar[counter] = data[3];
			corr[counter] = data[4];
			counter++;
		}	
		System.out.println("-------------------------------------");
		System.out.println("overall:");
		System.out.println("Var(docLength): " + Statistics.mean(varDocLength));
		System.out.println("Var(ne): " + Statistics.mean(varNE));
		System.out.println("Var(lengthPlusNE: )" + Statistics.mean(varSum));
		System.out.println("Covariance: " + Statistics.mean(covar));
		System.out.println("Correlation: " + Statistics.mean(corr));
	}
	
	public void processtl(String methodName, double parameter){
		TRECQuery queries = new TRECQuery();
		// compute the sum of query length
		
		String[] queryTerms = this.extractQueryTerms();
		int sumOfQueryLength = queryTerms.length;
		double[] varDocLength = new double[sumOfQueryLength];
		double[] varTF = new double[sumOfQueryLength];
		double[] varSum = new double[sumOfQueryLength];
		double[] covar = new double[sumOfQueryLength];
		double[] corr = new double[sumOfQueryLength];
		int counter = 0;
		this.setNormalisation(methodName, parameter);
		for (int i = 0; i < sumOfQueryLength; i++){
			String term = queryTerms[i];
			
			double[] data = this.checkCovarianceTFLength(matching, term, "test");
			
			System.out.println(">>>>>>>>>>term: " + term + ", " +(i+1) +" terms processed out of " +
					sumOfQueryLength);
			System.out.println("Var(docLength): " + data[0]);
			System.out.println("Var(TF): " + data[1]);
			System.out.println("Var(lengthPlusNE: )" + data[2]);		
			System.out.println("Covariance: " + data[3]);
			System.out.println("Correlation: " + data[4]);
			varDocLength[counter] = data[0];
			varTF[counter] = data[1];
			varSum[counter] = data[2];
			covar[counter] = data[3];
			corr[counter] = data[4];
			counter++;
		}	
		System.out.println("-------------------------------------");
		System.out.println("overall:");
		System.out.println("Var(docLength): " + Statistics.mean(varDocLength));
		System.out.println("Var(TF): " + Statistics.mean(varTF));
		System.out.println("Var(lengthPlusNE: )" + Statistics.mean(varSum));
		System.out.println("Covariance: " + Statistics.mean(covar));
		System.out.println("Correlation: " + Statistics.mean(corr));
		System.out.println("Scaled: " + this.map(Statistics.mean(corr)));
	}
	
	public void setNormalisation(String methodName, double parameter){
		if (methodName.endsWith("4")){
			methodName = "3";
		}
		if (methodName.lastIndexOf('.')<0){
			if (methodName.trim().length() == 1)
				methodName = normPackagePrefix + normMethodPrefix + methodName;
			else
				methodName = normPackagePrefix.concat(methodName);
		}
		try {
			method = (TermFrequencyNormalisation) Class.forName(methodName).newInstance();
			method.setParameter(parameter);
			method.setAverageDocumentLength(collSta.getAverageDocumentLength());
		} 
		catch(InstantiationException ie) {
			System.err.println("Exception while loading the term frequency normalisation method:\n" + ie);
			System.err.println("Exiting...");
			System.exit(1);
		} catch(IllegalAccessException iae) {
			System.err.println("Exception while loading the term frequency normalisation method:\n" + iae);
			System.err.println("Exiting...");
			System.exit(1);
		} catch(ClassNotFoundException cnfe) {
			System.err.println("Exception while loading the term frequency normalisation method:\n" + cnfe);
			System.err.println("Exiting...");
			System.exit(1);
		}	
	}
	
	protected String[] selectQueryTerms(String[] terms, int number){
		String[] selected = new String[number];
		for (int i = 0; i < number; i++){
			int index = (int)(Math.random() * terms.length);
			selected[i] = terms[index];
		}
		return selected;
	}
	
	protected String[] extractQueryTerms(){
		TRECQuery queries = new TRECQuery();
		// compute the sum of query length
		
		Vector vecTerms = new Vector();
		Index index = Index.createIndex();
		Lexicon lexicon = index.getLexicon();
		InvertedIndex invIndex = index.getInvertedIndex();
		PorterStopPipeline pipe = new PorterStopPipeline();
		int counter = 0;
		while (queries.hasMoreQueries()){
			String queryTerms = queries.nextQuery();
			BasicQuery query = new BasicQuery(queryTerms, queries.getQueryId(), pipe);
			String[] termStrings = query.getQueryTermStrings();
			
			for (int i = 0; i < termStrings.length; i++){
				counter++;
				if (lexicon.findTerm(termStrings[i])){
//					System.out.println(termStrings[i]+
//							": " + "Nt=" + lexicon.getNt());
					if (lexicon.getNt() > 1 ) //&& 
							//invIndex.getDocuments(TermCodes.getCode(termStrings[i]))[0].length > 1)
						vecTerms.addElement(termStrings[i].trim());
				}
			}
		}
		System.out.println(counter+ " query terms in total.");
		//return this.selectQueryTerms(
				//(String[])vecTerms.toArray(new String[vecTerms.size()]), 100);
		return (String[])vecTerms.toArray(new String[vecTerms.size()]);
	}
	
	protected double getCorrelationTFLength(String methodName, double parameter, String[] queryTerms){		
		int sumOfQueryLength = queryTerms.length;
		double[] varDocLength = new double[sumOfQueryLength];
		double[] varTF = new double[sumOfQueryLength];
		double[] varSum = new double[sumOfQueryLength];
		//double[] covar = new double[sumOfQueryLength];
		double[] corr = new double[sumOfQueryLength];
		int counter = 0;
		
		int effCounter = 0;
		this.setNormalisation(methodName, parameter);
		for (int i = 0; i < queryTerms.length; i++){
			String term = queryTerms[i];
			
			double[] data = this.checkCovarianceTFLength(matching, term, "test");
			if (data == null){
				corr[counter] = 0;
				counter++;
				continue;
			}
			varDocLength[counter] = data[0];
			varTF[counter] = data[1];
			varSum[counter] = data[2];
			//covar[counter] = data[3];
			corr[counter] = data[4];
			if (Double.isNaN(corr[counter]))
				corr[counter] = 0;
			else
				effCounter++;
			counter++;
		}	
		if (effCounter == 0)
			return Double.NaN;
		return Statistics.sum(corr)/effCounter;
	}
	
	protected double getCorrelationTFLength(String methodName, 
			double parameter, ArrayList listDocLength, ArrayList listFrequency){		
		int sumOfQueryLength = listDocLength.size();
		double[] corr = new double[sumOfQueryLength];
		int counter = 0;
		
		this.setNormalisation(methodName, parameter);
		for (int i = 0; i < sumOfQueryLength; i++){
			double[] docLength = (double[])listDocLength.get(i);
			double[] tfn = new double[docLength.length];
			double[] tf = (double[])listFrequency.get(i);
			for (int j = 0; j < docLength.length; j++)
				tfn[j] = this.method.getNormalisedTermFrequency(tf[j], docLength[j]);
			corr[counter] = Statistics.correlation(tfn, docLength);
			counter++;
			System.gc();
		}	
		return Statistics.mean(corr);
	}
	
	
	public double[] checkCovarianceNELength(BufferedMatching matching, BasicQuery query){
		System.out.println("checking covariance for query " + query.getQueryNumber());
		String[] terms = new String[query.getQueryLength()];
		TermTreeNode[] treeNodes = query.getQueryTerms();
		for (int i = 0; i < terms.length; i++)
			terms[i] = treeNodes[i].term;
		matching.matchWithoutScoring(query.getQueryNumber(), terms);
		double[] docLength = matching.getRetrievedDocLength();
		docLength = Statistics.splitIntoBin(docLength, BIN_NUMBER);
		NormalisationEffect norm = new NormalisationEffect(method);
		double[] lengthPlusNE = new double[docLength.length];
		double[] ne = new double[docLength.length];
		method.setAverageDocumentLength(collSta.getAverageDocumentLength());
		for (int i = 0; i < docLength.length; i++){
			ne[i] = method.getNormalisedTermFrequency(1d, docLength[i]);
			lengthPlusNE[i] = docLength[i] + ne[i]; 
		}
		double varDocLength = Statistics.variance(docLength);
		double varNE = Statistics.variance(ne);
		double varSum = Statistics.variance(lengthPlusNE);
		double covar = (varSum - varDocLength - varNE)/2;
		double corr = covar/(Statistics.standardDeviation(docLength)*Statistics.standardDeviation(ne));
		
		double[] data = new double[5];
		data[0] = varDocLength;
		data[1] = varNE;
		data[2] = varSum;
		data[3] = covar;
		data[4] = corr;
		System.gc();
		return data;
	}
	
	public double getCorrelationTFLength(String methodName, 
			double parameter, double[] tf, double[] docLength){
			
		this.setNormalisation(methodName, parameter);
		int numberOfDocs = tf.length;
		double[] tfn = new double[numberOfDocs];
		double F_t = Statistics.sum(tf);
		method.setTF(F_t);
		for (int i = 0; i < numberOfDocs; i++)
			tfn[i] = method.getNormalisedTermFrequency(tf[i], docLength[i]);
		return Statistics.correlation(tfn, docLength); 
	}
	
	public double[] checkMeanPeakCorrelationTFLength(String methodName){
		double left = 0.5;
		double right = 1.0;
		double inc = 0.01;
		double[] result = new double[2];
		double[] parameters = new double[(int)((right-left)/inc)];
		System.out.println("number of parameters to test: " + parameters.length);
		String[] queryTerms = this.extractQueryTerms();
		double[][] corr = new double[parameters.length][queryTerms.length]; 
		for (int i = 0; i < parameters.length; i++)
			parameters[i] = 0.5 + inc*(i+1);
		
		for (int i = 0; i < queryTerms.length; i++){
			String[] term = {queryTerms[i]};
			matching.matchWithoutScoring("test", term);
			double[] docLength = matching.getRetrievedDocLength();
			double[] tf = matching.getWithinDocFrequency();
			int numberOfDocs = tf.length;
			//double[] tfn = new double[numberOfDocs];
			System.out.println("computing correlations for term " + queryTerms[i] +
					", " + i + " terms processed out of " + queryTerms.length);
			for (int j = 0; j < parameters.length; j++){
				corr[j][i] = this.getCorrelationTFLength(methodName,
						parameters[j], tf, docLength);
			}
		}
		System.out.println("--------------------------------------------");
		double[] meanCorr = new double[parameters.length];
		for (int i = 0; i < parameters.length; i++)
			meanCorr[i] = Statistics.mean(corr[i]);
		int minIndex = BubbleSort.getOrder(meanCorr)[0];

		System.out.println("parameter: " + parameters[minIndex] +
				", peak mean corr: " + meanCorr[minIndex] +
				", scaled: " + this.map(meanCorr[minIndex]));
		result[0] = parameters[minIndex];
		result[1] = meanCorr[minIndex];
		
		return result;
	}
	
	public void linkCorrelationNE(String normMethodName){
		TRECQuery queries = new TRECQuery();
		int numberOfQueries = queries.getNumberOfQueries();
		int definition = 2;
		
		double[] parameters = null;
		if (normMethodName.endsWith("2")){
			double[] buf = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 
					1.0, 1.2, 1.4, 1.6, 1.8, 2
					, 2.5, 3, 4,
					5, 6, 7
					//, 8, 10, 12, 16, 20, 24, 32
			};
			parameters = (double[])(buf.clone());
		}
		if (normMethodName.endsWith("B")){
			double[] buf = {
					//0.05, 0.10, 0.15, 0.20, 0.25,
					0.30, 0.35, 0.40
					, 0.45, 0.50, 0.55, 0.60, 0.65, 0.70, 0.75, 0.80
					//, 0.85, 0.90, 0.95, 1
			};
			parameters = (double[])(buf.clone());
		}
		
		double[][] mean_rho_tfn_length = new double[numberOfQueries][parameters.length];
		double[][] NE = new double[numberOfQueries][parameters.length];
		
		for (int i = 0; i < numberOfQueries; i++){
			Arrays.fill(mean_rho_tfn_length[i], 0d);
			Arrays.fill(NE[i], 0d);
		}
		
		PorterStopPipeline pipe = new PorterStopPipeline();
		int queryCounter = 0;
		double sumLinkCorr = 0d;
		//double sumPValue = 0d;
		int effQueryCounter = 0;
		while (queries.hasMoreQueries()){
			BasicQuery query = new BasicQuery(queries.nextQuery(), queries.getQueryId(), pipe);
			System.out.print("processing query " + query.getQueryNumber() +"...");
			String[] terms = query.getQueryTermStrings();
			//double[] rhoPerTerm = new double[terms.length];
			
			//compute NE
			matching.matchWithoutScoring(query.getQueryNumber(), terms);
			double[] docLength = Statistics.splitIntoBin(matching.getRetrievedDocLength(),
					this.BIN_NUMBER);
			for (int i = 0; i < parameters.length; i++){
				NormalisationEffect normEffect = new NormalisationEffect(normMethodName);
				NE[queryCounter][i] = normEffect.getNED(docLength, 
						parameters[i], definition);
			}
			
			//compute rho
			for (int i = 0; i < parameters.length; i++)
				mean_rho_tfn_length[queryCounter][i] =
					this.getCorrelationTFLength(normMethodName, parameters[i], terms);
			double linkCorr = Statistics.correlation(
					mean_rho_tfn_length[queryCounter],
					NE[queryCounter]);
//			double pValue = Statistics.significance(
//					mean_rho_tfn_length[queryCounter],
//					NE[queryCounter]); 
			//double pValue = Statistics.significance(linkCorr, 
					//(double)NE[queryCounter].length);
			if (!Double.isNaN(linkCorr) //&& !Double.isNaN(pValue)
					){
				sumLinkCorr += linkCorr;
				//sumPValue += pValue;
				effQueryCounter++;
			}
			System.out.println("correlation: " + Rounding.toString(linkCorr, 4) 
					//+ ", p-value: " + pValue
					+", effQueryCounter: " + effQueryCounter
					);
			queryCounter++;
			System.gc();
		}
		
		// computer correlations
		
//		for (int i = 0; i < numberOfQueries; i++){
//			linkCorr[i] = Statistics.correlation(mean_rho_tfn_length[i], NE[i]);
//			System.out.println((i+1)+": " + linkCorr[i]);
//		}
		System.out.println("---------------------------------");
		System.out.println("number of effective queries: " + effQueryCounter);
		System.out.println("mean correlation: " + sumLinkCorr/effQueryCounter);
		//System.out.println("mean p-value: " + sumPValue/effQueryCounter);
		
	}
	
	public void printCorrelation(String methodName){
		String[] queryTerms = this.extractQueryTerms();
		double[] parameters = null;
		if (methodName.endsWith("2")){
			double[] buf = {0.0001, 0.001, 0.01, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.8, 1d,
					2d, 3, 4, 5, 6, 7, 8, 10, 12, 16, 24, 32};
			parameters = buf;
		}
		if (methodName.endsWith("B")||methodName.endsWith("b")){
			double[] buf = {0.05, 0.10, 0.15, 0.20, 0.25, 0.30, 0.35, 0.40, 0.45, 0.50,
					0.55, 0.60, 0.65, 0.70, 0.75, 0.80, 0.85, 0.90, 0.95, 1.00};
			parameters = buf;
		}
		if (methodName.endsWith("3")){
			double[] buf = {100, 200, 400, 600, 800, 1000, 4000, 8000};
			parameters = buf;
		}
		double[][] corr = new double[parameters.length][queryTerms.length]; 
		StringBuffer buffer = new StringBuffer();
		String EOL = ApplicationSetup.EOL;
		for (int i = 0; i < queryTerms.length; i++){
			buffer.append(queryTerms[i]);
			String[] term = {queryTerms[i]};
			matching.matchWithoutScoring("test", term);
			double[] docLength = matching.getRetrievedDocLength();
			double[] tf = matching.getWithinDocFrequency();
			int numberOfDocs = tf.length;
			//double[] tfn = new double[numberOfDocs];
			System.out.println("computing correlations for term " + queryTerms[i] +
					", " + i + " terms processed out of " + queryTerms.length);
//			System.out.println("tf.length: " + tf.length + 
//					", docLength.length: " + docLength.length);
			for (int j = 0; j < parameters.length; j++){
				corr[j][i] = this.getCorrelationTFLength(methodName,
						parameters[j], tf, docLength);
				if (debugging){
					System.out.println("mu=" + parameters[j]
						+ ", corr=" + corr[j][i]);
				}
				buffer.append(" & " + Rounding.toString(corr[j][i], 4));
			}
			buffer.append("\\\\" + EOL);
		}
		System.out.println("--------------------------------------------");
		buffer.append("--------------------------------------------" + EOL);
		for (int i = 0; i < parameters.length; i++){
			double correlation = Statistics.mean(corr[i]);
			buffer.append("parameter: " + parameters[i] +
					", correlation: " + correlation +
					", scaled corr: " + this.map(correlation) + EOL);
			System.out.println("parameter: " + parameters[i] +
					", correlation: " + correlation +
					", scaled corr: " + this.map(correlation));
		}
		
		try{
			BufferedWriter bw = new BufferedWriter(new FileWriter(
					new File(ApplicationSetup.TREC_RESULTS, "corr"+SystemUtility.queryType()+".txt")));
			bw.write(buffer.toString());
			bw.close();
		}
		catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		
	}
	
	public double[] getMeanCorrelatioTFLength(String methodName, double[] parameters,
			Query query){
		TermTreeNode[] termTreeNodes = query.getQueryTerms();
		double[][] corr = new double[parameters.length][termTreeNodes.length];
		for (int i = 0; i < termTreeNodes.length; i++){
			String[] term = {termTreeNodes[i].term};
			matching.matchWithoutScoring("test", term);
			double[] tf = matching.getTF();
			double[] docLength = matching.getRetrievedDocLength();
			for (int j = 0; j < parameters.length; j++)
				corr[j][i] = this.getCorrelationTFLength(methodName,
						parameters[j], tf, docLength);
		}
		double[] meanCorr = new double[parameters.length];
		for (int i = 0; i < meanCorr.length; i++)
			meanCorr[i] = Statistics.mean(corr[i]);
		return meanCorr;
	}
	
	public void printCorrelation(String methodName, String _term){
		double[] parameters = null;
		if (methodName.endsWith("2")){
			double[] buf = {0.0001, 0.001, 0.01, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.8, 1d,
					2d, 3, 4, 5, 6, 7, 8, 10, 12, 16, 24, 32};
			parameters = buf;
		}
		if (methodName.endsWith("B")||methodName.endsWith("b")){
			double[] buf = {0.05, 0.10, 0.15, 0.20, 0.25, 0.30, 0.35, 0.40, 0.45, 0.50,
					0.55, 0.60, 0.65, 0.70, 0.75, 0.80, 0.85, 0.90, 0.95, 1.00};
			parameters = buf;
		}
		if (methodName.endsWith("3")){
			double[] buf = {10, 30, 50, 80, 100, 125, 150, 175, 200, 400, 600, 800, 
					1000, 2000, 4000, 6000, 8000, 10000};
			parameters = buf;
		}
		double[] corr = new double[parameters.length]; 
		
		
		String[] term = {_term};
		matching.matchWithoutScoring("test", term);
		double[] docLength = matching.getRetrievedDocLength();
		double[] tf = matching.getWithinDocFrequency();
		int numberOfDocs = tf.length;

		System.out.println("computing correlations for term " + _term + "... ");
		
		System.out.println("--------------------------------------------");
		for (int i = 0; i < parameters.length; i++){
			double correlation = corr[i];
			System.out.println("parameter: " + parameters[i] +
					", correlation: " + correlation +
					", scaled corr: " + this.map(correlation));
		}
	}
	
	public double map(double x){
		return Math.exp(x);
	}
	
	public double reverseMap(double y){
		return Math.log(y);
	}
	
	public void tune(String methodName, String taskName){
//		System.out.println("seeking the peak of the correlation curve...");
//		double[] peak = this.checkMeanPeakCorrelationTFLength(methodName);
//		System.out.println("parameter: " + peak[0] +
//				", peak corr: " + peak[1]);
		String[] queryTerms = this.extractQueryTerms();    
		System.out.println("number of query terms: " + queryTerms.length);
		int querytype = SystemUtility.queryType();
		double penalty = 0.0001;
		double interval = 0.1;
		System.out.println("task: " + taskName +
				"normalisation method: " + methodName +
				", query type: " + querytype +
				", penalty threshold: " + penalty);
		if (methodName.endsWith("2")){
			double left = 0.1;
			double right = 64d;
			double parameter = (left+right)/2;
			double target = 0d;
//			if (querytype == 1)
//				target = -0.127838;
//			if (querytype == 2)
//				target = -0.169274 ;
//			if (querytype == 7)
//				target = -0.167668;
			if (taskName.equalsIgnoreCase("adhoc")){
				target = -0.0885;
			}
			if (taskName.equalsIgnoreCase("td")){
				target = -0.2944;
			}
			while (Math.abs(interval) > penalty){
				parameter = (left+right)/2;
				double corr = this.getCorrelationTFLength(methodName, 
						parameter, queryTerms);
				
				interval = corr - target;
				System.out.println("left: " + Rounding.toString(left, 4)
						+ ", c: " + Rounding.toString(parameter, 4)
						+ ", right: " + Rounding.toString(right, 4)
						+ ", corr: " + Rounding.toString(corr, 6)
						+ ", target: " + Rounding.toString(target, 6)
						+ ", diff: " + Rounding.toString(interval, 6));
				if (taskName.equalsIgnoreCase("adhoc")){
					if (interval > 0 && Math.abs(interval) > penalty){
						right = parameter;
						parameter = (left+right)/2;
					}
					if (interval < 0 && Math.abs(interval) > penalty){
						left = parameter;
						parameter = (left+right)/2;
					}
				}
				if (taskName.equalsIgnoreCase("td")){
					if (interval > 0 && Math.abs(interval) > penalty){
						right = parameter;
						parameter = (left+right)/2;
					}
					if (interval < 0 && Math.abs(interval) > penalty){
						left = parameter;
						parameter = (left+right)/2;
					}
				}
			}
			System.out.println("Finished tuning. c = " + parameter);
		}
		if (methodName.endsWith("3")){
			double left = 100;
			double right = 10000d;
			double parameter = (left+right)/2;
			double target = 0d;
//			if (querytype == 1)
//				target = -0.107865;
//			if (querytype == 2)
//				//target = -0.101439;
//				target = -0.107865;
//			if (querytype == 7)
//				//target = -0.116589;
//				target = -0.107865;
			target = -0.0885;
			while (Math.abs(interval) > penalty){
				parameter = (left+right)/2;
				double corr = this.getCorrelationTFLength(methodName, 
						parameter, queryTerms);
				
				interval = corr - target;
				System.out.println("left: " + Rounding.toString(left, 4)
						+ ", mu: " + Rounding.toString(parameter, 4)
						+ ", right: " + Rounding.toString(right, 4)
						+ ", corr: " + Rounding.toString(corr, 6)
						+ ", target: " + Rounding.toString(target, 6)
						+ ", diff: " + Rounding.toString(interval, 6));
				if (interval > 0 && Math.abs(interval) > penalty){
					right = parameter;
					parameter = (left+right)/2;
				}
				if (interval < 0 && Math.abs(interval) > penalty){
					left = parameter;
					parameter = (left+right)/2;
				}
			}
			System.out.println("Finished tuning. mu = " + parameter);
		}
		
		if (methodName.endsWith("4")){
			double left = 100;
			double right = 10000d;
			double parameter = (left+right)/2;
			double target = 0d;
			if (querytype == 1)
				target = -0.0797990;
			if (querytype == 2)
				//target = -0.101439;
				target = -0.0797990;
			if (querytype == 7)
				//target = -0.116589;
				target = -0.0797990;
			while (Math.abs(interval) > penalty){
				parameter = (left+right)/2;
				double corr = this.getCorrelationTFLength(methodName, 
						parameter, queryTerms);
				
				interval = corr - target;
				System.out.println("left: " + Rounding.toString(left, 4)
						+ ", mu: " + Rounding.toString(parameter, 4)
						+ ", right: " + Rounding.toString(right, 4)
						+ ", corr: " + Rounding.toString(corr, 6)
						+ ", target: " + Rounding.toString(target, 6)
						+ ", diff: " + Rounding.toString(interval, 6));
				if (interval > 0 && Math.abs(interval) > penalty){
					right = parameter;
					parameter = (left+right)/2;
				}
				if (interval < 0 && Math.abs(interval) > penalty){
					left = parameter;
					parameter = (left+right)/2;
				}
			}
			System.out.println("Finished tuning. mu = " + parameter);
		}
		
		if (methodName.endsWith("B")){
			double left = 0.01;
			double right = 1d;
			double parameter = (left+right)/2;
			double target = 0d;
//			if (querytype == 1)
//				target = -0.128430;
//			if (querytype == 2)
//				target = -0.170439;
//			if (querytype == 7)
//				target = -0.170546;
			target = -0.0885;
			while (Math.abs(interval) > penalty){
				parameter = (left+right)/2;
				double corr = this.getCorrelationTFLength(methodName, 
						parameter, queryTerms);
				
				interval = corr - target;
				System.out.println("left: " + Rounding.toString(left, 4)
						+ ", b: " + Rounding.toString(parameter, 4)
						+ ", right: " + Rounding.toString(right, 4)
						+ ", corr: " + Rounding.toString(corr, 6)
						+ ", target: " + Rounding.toString(target, 6)
						+ ", diff: " + Rounding.toString(interval, 6));
				if (interval < 0 && Math.abs(interval) > penalty){
					right = parameter;
					parameter = (left+right)/2;
				}
				if (interval > 0 && Math.abs(interval) > penalty){
					left = parameter;
					parameter = (left+right)/2;
				}
			}
			System.out.println("Finished tuning. b = " + parameter);
		}
	}
	
	public double[] checkCovarianceTFLength(BufferedMatching matching, 
			String term, String queryid){
		String[] terms = new String[1];
		terms[0] = term;

		matching.matchWithoutScoring(queryid, terms);
		double[] docLength = matching.getRetrievedDocLength();
		if (docLength.length == 0)
			return null;
		//docLength = Statistics.splitIntoBin(docLength, BIN_NUMBER);
		NormalisationEffect norm = new NormalisationEffect(method);
		double[] lengthPlusTF = new double[docLength.length];
		double[] TF = new double[docLength.length];
		double[] tf = matching.getWithinDocFrequency();
		method.setAverageDocumentLength(collSta.getAverageDocumentLength());
		for (int i = 0; i < docLength.length; i++){
			try{
				TF[i] = method.getNormalisedTermFrequency(tf[i], docLength[i]);
			}
			catch(ArrayIndexOutOfBoundsException e){
				e.printStackTrace();
				System.err.println("TF.length: " + TF.length 
						+ ", tf.length: " + tf.length
						+ ", docLength.length: " + docLength.length);
				System.exit(1);
			}
			lengthPlusTF[i] = docLength[i] + TF[i]; 
		}
		double varDocLength = Statistics.variance(docLength);
		double varTF = Statistics.variance(TF);
		double varSum = Statistics.variance(lengthPlusTF);
		double covar = (varSum - varDocLength - varTF)/2;
		double corr = covar/(Statistics.standardDeviation(docLength)*Statistics.standardDeviation(TF));
		if (varDocLength == 0d)
			corr = 0;
		
		double[] data = new double[5];
		data[0] = varDocLength;
		data[1] = varTF;
		data[2] = varSum;
		data[3] = covar;
		data[4] = corr;
		System.gc();
		return data;
	}
	
	public static void main(String[] args){
		TestRetrieval test = new TestRetrieval();
		if (args[2].equalsIgnoreCase("-corr")){
			String methodName = args[4];
			double parameter = Double.parseDouble(args[5]);
			//-r -d -cov -nl <method> <parameter>
			if (args[3].equalsIgnoreCase("-nl")){
				test.processnl(methodName, parameter);
			}
			
			if (args[3].equalsIgnoreCase("-tl")){
				test.processtl(methodName, parameter);
			}			
		}
		
		// -r -d -link <method>
		if (args[2].equalsIgnoreCase("-link")){
			String methodName = args[3];
			test.linkCorrelationNE(methodName);
		}
		
		// -r -d -min <methodName>
		if (args[2].equalsIgnoreCase("-min")){
			test.checkMeanPeakCorrelationTFLength(args[3]);
		}
		
//		 -r -d -tune <methodName> <taskName>
		if (args[2].equalsIgnoreCase("-tune")){
			test.tune(args[3], args[4]);
		}
		// -r -d -pcorr <methodName>
		if (args[2].equalsIgnoreCase("-pcorr")){
			test.printCorrelation(args[3]);
		}
	}
}

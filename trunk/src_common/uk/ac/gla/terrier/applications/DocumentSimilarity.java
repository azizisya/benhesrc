/*
 * Created on 30 Jul 2008
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.applications;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;

import uk.ac.gla.terrier.evaluation.TRECQrelsInMemory;
import uk.ac.gla.terrier.matching.dsms.DistanceModifier;
import uk.ac.gla.terrier.matching.models.WeightingModel;
import uk.ac.gla.terrier.statistics.Statistics;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.DirectIndex;
import uk.ac.gla.terrier.structures.ExpansionTerms;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.ExpansionTerm;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.QueryUtility;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectDoubleHashMap;

public class DocumentSimilarity {
	
	public static void getTerms(int[] docids, 
			TIntDoubleHashMap scoreMap, 
			Index index,
			WeightingModel qemodel){
		double length = 0d;
		for (int i=0; i<docids.length; i++)
			length+=index.getDocumentIndex().getDocumentLength(docids[i]);
		//System.err.println("getTerms "+index.getCollectionStatistics());
		CollectionStatistics collSta = index.getCollectionStatistics();
		ExpansionTerms expTerms = new ExpansionTerms(
				collSta.getNumberOfDocuments(),
				collSta.getNumberOfTokens(),
				collSta.getAverageDocumentLength(),
				length, 
				index.getLexicon());
		//System.out.println(collSta.getNumberOfDocuments()+" "+collSta.getNumberOfTokens()+" "+collSta.getAverageDocumentLength()+" "+length);
		DirectIndex di = index.getDirectIndex();
		for (int i=0; i<docids.length; i++){
			int[][] terms = di.getTerms(docids[i]);
			for (int j=0; j<terms[0].length; j++){
				expTerms.insertTerm(terms[0][j], terms[1][j]);
			}
		}
		int topT = (ApplicationSetup.EXPANSION_TERMS==0)?(expTerms.getNumberOfUniqueTerms()):(ApplicationSetup.EXPANSION_TERMS);
		TIntObjectHashMap<ExpansionTerm> termMap = 
			expTerms.getExpandedTermHashSet(topT, qemodel);
		for (int k : termMap.keys()){
			//System.out.println(termMap.get(k).getWeightExpansion());
			scoreMap.put(k, termMap.get(k).getWeightExpansion());
		}
	}
	
	public static double[] makeVector(TIntDoubleHashMap scoreMap, TIntHashSet termidSet){
		double[] vector = new double[termidSet.size()];
		Arrays.fill(vector, 0d);
		int[] termids = termidSet.toArray();
		Arrays.sort(termids);
		for (int i=0; i<termids.length; i++)
			vector[i] = scoreMap.get(termids[i]);
		return vector;
	}
	
	public static double getCosineSimilarity(int[] docidsA, int[] docidsB, WeightingModel qemodel, Index index){
		TIntHashSet allTermidSet = new TIntHashSet();
		TIntDoubleHashMap scoreMapA = new TIntDoubleHashMap(); // termid -> score
		TIntDoubleHashMap scoreMapB = new TIntDoubleHashMap(); // termid -> score
		DirectIndex di = index.getDirectIndex();
		// aggregate term frequencies in the two document sets
		// in the mean while, record all terms in two sets
		getTerms(docidsA, scoreMapA, index, qemodel);
		getTerms(docidsB, scoreMapB, index, qemodel);
		allTermidSet.addAll(scoreMapA.keys());
		allTermidSet.addAll(scoreMapB.keys());
		// construct vectors
		double[] vecA = makeVector(scoreMapA, allTermidSet);
		double[] vecB = makeVector(scoreMapB, allTermidSet);
		// compute cosine
		double sim = DistanceModifier.cosine1(vecA, vecB);
		if (vecA.length<50)
			for (int i=0; i<vecA.length; i++){
				//System.out.println(i+": "+vecA[i]+", "+vecB[i]);
			}
		//index.close();
		//System.out.println("vecA.length: "+vecA.length+", vecB.length: "+vecB.length+", sim: "+sim);
		return sim;
	}
	
	public static double getCosineSimilarity(int docidA, int docidB, WeightingModel qemodel, Index index){
		int[] docidsA = {docidA};
		int[] docidsB = {docidB};
		return getCosineSimilarity(docidsA, docidsB, qemodel, index);
	}
	
	public static void printQueryCosineSimilarity(String queryFileA, String queryFileB){
		THashMap<String, String> queryMapA = new THashMap<String, String>();
		THashMap<String, String> queryMapB = new THashMap<String, String>();
		THashSet<String> queryidSet = new THashSet<String>();
		boolean hasQueryId = Boolean.parseBoolean(
				ApplicationSetup.getProperty("SingleLineTRECQuery.queryid.exists", "true"));
		int queryCounter = 0;
		try{
			BufferedReader br = Files.openFileReader(queryFileA);
			String line = null;
			while ((line=br.readLine())!=null){
				if (hasQueryId){
					queryMapA.put(line.substring(0, line.indexOf(' ')), line);
					queryidSet.add(line.substring(0, line.indexOf(' ')));
				}
				else{
					queryMapA.put(""+(queryCounter++), line);
					queryidSet.add(""+(queryCounter++));
				}
			}
			br.close();
			queryCounter = 0;
			br = Files.openFileReader(queryFileB);
			while ((line=br.readLine())!=null){
				if (hasQueryId){
					queryMapB.put(line.substring(0, line.indexOf(' ')), line);
					queryidSet.add(line.substring(0, line.indexOf(' ')));
				}
				else{
					queryMapB.put(""+(queryCounter++), line);
					queryidSet.add(""+(queryCounter++));
				}
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		String[] queryids = queryidSet.toArray(new String[queryidSet.size()]);
		Arrays.sort(queryids);
		double[] sim = new double[queryids.length];
		for (int i=0; i<queryids.length; i++){
			String queryA = queryMapA.get(queryids[i]);
			String queryB = queryMapB.get(queryids[i]);
			sim[i] = getCosineSimilarity(queryA, queryB);
			if (hasQueryId){
				//System.out.println(queryA);
				//System.out.println(queryB);
				System.out.println(queryids[i]+" "+sim[i]);
			}else{
				//System.out.println(queryids[i]+" "+queryA);
				//System.out.println(queryids[i]+" "+queryB);
				System.out.println(queryids[i]+" "+sim[i]);
			}
		}
		System.out.println("mean cosine similarity: "+Statistics.mean(sim));
	}
	
	public static double getCosineSimilarity(
			TObjectDoubleHashMap<String> mapA,
			TObjectDoubleHashMap<String> mapB){
		THashSet<String> termSet = new THashSet<String>();
		String[] termsA = mapA.keys(new String[mapA.size()]);
		String[] termsB = mapB.keys(new String[mapB.size()]);
		for (int i=0; i<termsA.length; i++)
			termSet.add(termsA[i]);
		for (int i=0; i<termsB.length; i++)
			termSet.add(termsB[i]);
		double[] vA = new double[termSet.size()];
		double[] vB = new double[termSet.size()];
		String[] allTerms = termSet.toArray(new String[termSet.size()]);
		for (int i=0; i<allTerms.length; i++){
			vA[i] = mapA.get(allTerms[i]);
			vB[i] = mapB.get(allTerms[i]);
		}
		return DistanceModifier.cosine1(vA, vB);
	}
	
	public static double getCosineSimilarity(String queryA, String queryB){
		// map from term string to weight
		TObjectDoubleHashMap<String> mapA = new TObjectDoubleHashMap<String>();
		TObjectDoubleHashMap<String> mapB = new TObjectDoubleHashMap<String>();
		QueryUtility.decomposeOneLineQuery(queryA, mapA);
		QueryUtility.decomposeOneLineQuery(queryB, mapB);
		return getCosineSimilarity(mapA, mapB);
	}
	
	public static void printSimilarityPosNeg(String qrelsidFilename, String queryid, String qemodelname){
		TRECQrelsInMemory qrels = new TRECQrelsInMemory(qrelsidFilename);
		String[] relDocs = qrels.getRelevantDocumentsToArray(queryid);
		String[] nonrelDocs = qrels.getNonRelevantDocumentsToArray(queryid);
		int[] docidsA = new int[relDocs.length];
		int[] docidsB = new int[nonrelDocs.length];
		for (int i=0; i<relDocs.length; i++)
			docidsA[i] = Integer.parseInt(relDocs[i]);
		for (int i=0; i<nonrelDocs.length; i++)
			docidsB[i] = Integer.parseInt(nonrelDocs[i]);
		// initiate QE model
		WeightingModel qemodel = WeightingModel.getWeightingModel(qemodelname);
		Index index = Index.createIndex();
		System.out.println("cosine: "+getCosineSimilarity(docidsA, docidsB, qemodel, index));
	}
	
	public static void printCosineSimilarity(String[] args){
		int numberOfDocsA = Integer.parseInt(args[1]);
		int numberOfDocsB = Integer.parseInt(args[2]);
		int[] docidsA = new int[numberOfDocsA];
		int[] docidsB = new int[numberOfDocsB];
		int counter = 0;
		System.out.print("doc set A: ");
		for (int i=3; i<=numberOfDocsA+2; i++){
			docidsA[counter++]=Integer.parseInt(args[i]);
			System.out.print(docidsA[counter-1]+" ");
		}
		System.out.println();
		counter = 0;
		System.out.print("doc set B: ");
		for (int i=numberOfDocsA+3; i<args.length-1; i++){
			docidsB[counter++]=Integer.parseInt(args[i]);
			System.out.print(docidsB[counter-1]+" ");
		}
		System.out.println();
		String qemodelname = args[args.length-1];
		// initiate QE model
		WeightingModel qemodel = WeightingModel.getWeightingModel(qemodelname);
		Index index = Index.createIndex();
		System.out.println("cosine: "+getCosineSimilarity(docidsA, docidsB, qemodel, index));
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if (args[0].equals("--printcossim")){
			// --printcossim numberOfPosDocs numberOfNegDocs <posdocids...> <negdocids...> qemodelname numberOfTopTerms
			DocumentSimilarity.printCosineSimilarity(args);
		}else if(args[0].equals("--printcossimposneg")){
			// --printcossimposneg qrelsidFilename queryid qemodelname
			DocumentSimilarity.printSimilarityPosNeg(args[1], args[2], args[3]);
		}else if(args[0].equals("--printcosinesimonelinequeries")){
			// --printcosinesimonelinequeries queryfileA queryfileB
			DocumentSimilarity.printQueryCosineSimilarity(args[1], args[2]);
		}
	}

}

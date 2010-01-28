package uk.ac.gla.terrier.applications;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectDoubleHashMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import uk.ac.gla.terrier.matching.models.Idf;
import uk.ac.gla.terrier.matching.models.WeightingModel;
import uk.ac.gla.terrier.statistics.Statistics;
import uk.ac.gla.terrier.structures.DirectIndex;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.ExpansionTerm;
import uk.ac.gla.terrier.structures.ExpansionTerms;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.InvertedIndex;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.IndexUtility;
import uk.ac.gla.terrier.utility.Rounding;

public class DocumentSetEntropy {
	
	protected Index index;
	
	protected DocumentIndex docIndex;
	
	protected DirectIndex directIndex;
	
	protected InvertedIndex invIndex;
	
	protected Lexicon lexicon;
	
	protected long numberOfTokens;
	
	protected double lambda = Double.parseDouble(ApplicationSetup.getProperty("jm.smoothing.lambda", "0.6"));
	
	protected Idf idf;
	
	public DocumentSetEntropy(String indexPath, String indexPrefix){
		index = Index.createIndex(indexPath, indexPrefix);
		docIndex = index.getDocumentIndex();
		directIndex = index.getDirectIndex();
		lexicon = index.getLexicon();
		invIndex = index.getInvertedIndex();
		numberOfTokens = index.getCollectionStatistics().getNumberOfTokens();
		idf = new Idf();
		//System.err.println("Constructor "+index.getCollectionStatistics());
	}
	
	public double getEntropy(int[] docids){
		TIntIntHashMap map = new TIntIntHashMap();
		int length = 0;
		for (int i=0; i<docids.length; i++){
			int[][] terms = directIndex.getTerms(docids[i]);
			for (int j=0; j<terms[0].length; j++){
				map.adjustOrPutValue(terms[0][j], terms[1][j], terms[1][j]);
				length += terms[1][j];
			}
		}
		return getEntropy(map);
	}
	
	public double getCoherence(int[] docids){
		double sim = 0d;
		//System.err.println("getCoherence "+index.getCollectionStatistics());
		WeightingModel model = WeightingModel.getWeightingModel(ApplicationSetup.getProperty("trec.qemodel", "Bo1"));
		model.setAverageDocumentLength(index.getCollectionStatistics().getAverageDocumentLength());
		model.setNumberOfDocuments(index.getCollectionStatistics().getNumberOfDocuments());
		model.setNumberOfPointers(index.getCollectionStatistics().getNumberOfPointers());
		model.setNumberOfTokens(index.getCollectionStatistics().getNumberOfTokens());
		model.setNumberOfUniqueTerms(index.getCollectionStatistics().getNumberOfUniqueTerms());
		for (int i=0; i<docids.length-1; i++)
			for (int j=i+1; j<docids.length; j++)
				sim += DocumentSimilarity.getCosineSimilarity(docids[i], docids[j], model, index);
		int n = docids.length * (docids.length -1 )/2;
		return sim / n;
	}
	
	public void printCoherenceOfTopRankedDocs(String resultFilename, int minX, int maxX){
		TObjectDoubleHashMap<String>[] maps = new TObjectDoubleHashMap[maxX-minX+1];
		int counter = 0;
		for (int X=minX; X<=maxX; X++){
			System.out.print("Computing for top "+X+" documents...");
			maps[counter++] = this.getCoherenceOfTopRankedDocs(resultFilename, X);
			System.out.println("Done.");
		}
		String[] queryids = (String[])maps[0].keys(new String[maps[0].size()]);
		Arrays.sort(queryids);
		for (int i=0; i<queryids.length; i++){
			System.out.print(queryids[i]+": ");
			for (int j=0; j<maps.length; j++){
				System.out.print(Rounding.toString(maps[j].get(queryids[i]), 4)+" ");
			}
			System.out.println();
		}
		System.out.print("Avg: ");
		for (int i=0; i<maps.length; i++){
			System.out.print(Rounding.toString(Statistics.mean(maps[i].getValues()), 4)+" ");
		}
	}
	
	public TObjectDoubleHashMap<String> getCoherenceOfTopRankedDocs(String resultFilename, int topX){
		TObjectDoubleHashMap<String> queryidCoherenceMap = new TObjectDoubleHashMap<String>();
		try{
			BufferedReader br = Files.openFileReader(resultFilename);
			String currentQueryid = "1st";
			String line = null;
			TIntHashSet docidSet = new TIntHashSet();
			int counter = 0;
			ArrayList<Double> entropies = new ArrayList<Double>();
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				String queryid = tokens[0];
				int docid = Integer.parseInt(tokens[2]);
				if (currentQueryid.equals("1st")){
					currentQueryid = queryid;
				}else if (!currentQueryid.equals(queryid)){
					//System.err.print(currentQueryid+": ");
					int[] docids = docidSet.toArray();
					//for (int i=0; i<docids.length; i++)
						//System.err.print(docIndex.getDocumentNumber(docids[i])+", ");
					double coherence = getCoherence(docids);
					//System.err.println(entropy);
					queryidCoherenceMap.put(currentQueryid, coherence);
					entropies.add(coherence);
					docidSet = new TIntHashSet();
					currentQueryid = queryid;
					counter = 0;
				}
				if (counter < topX)
					docidSet.add(docid);
				counter++;
			}
			System.err.print(currentQueryid+": ");
			int[] docids = docidSet.toArray();
			//for (int i=0; i<docids.length; i++)
				//System.err.print(docIndex.getDocumentNumber(docids[i])+", ");
			double coherence = getEntropy(docids);
			//System.err.println(entropy);
			queryidCoherenceMap.put(currentQueryid, coherence);
			entropies.add(coherence);
			docidSet = new TIntHashSet();
			br.close();
			Object[] obj = entropies.toArray(new Double[entropies.size()]);
			double[] values = new double[entropies.size()];
			for (int i=0; i<values.length; i++)
				values[i] = ((Double)obj[i]).doubleValue();
			//System.err.println("Mean: "+Statistics.mean(values));
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		return queryidCoherenceMap;
	}
	
	public double getEntropyForTopTerms(int[] docids){
		TIntIntHashMap map = new TIntIntHashMap();
		int length = 0;
		for (int i=0; i<docids.length; i++)
			length+= docIndex.getDocumentLength(docids[i]);
		ExpansionTerms expTerms = new ExpansionTerms(index.getCollectionStatistics(), (double)length, lexicon);
		for (int i=0; i<docids.length; i++){
			int[][] terms = directIndex.getTerms(docids[i]);
			for (int j=0; j<terms[0].length; j++){
				map.adjustOrPutValue(terms[0][j], terms[1][j], terms[1][j]);
				expTerms.insertTerm(terms[0][j], terms[1][j]);
			}
		}
		TIntObjectHashMap<ExpansionTerm> expTermMap = expTerms.getExpandedTermHashSet(Math.min(ApplicationSetup.EXPANSION_TERMS, map.size()), 
				WeightingModel.getWeightingModel(ApplicationSetup.getProperty("trec.qemodel", "Bo1")), map);
		for (int termid : map.keys()){
			if (!expTermMap.contains(termid))
				map.remove(termid);
		}
		return getEntropy(map);
	}
	
	public double getNormalisedEntropy(int[] docids){
		TIntIntHashMap map = new TIntIntHashMap();
		int length = 0;
		for (int i=0; i<docids.length; i++){
			int[][] terms = directIndex.getTerms(docids[i]);
			for (int j=0; j<terms[0].length; j++){
				map.adjustOrPutValue(terms[0][j], terms[1][j], terms[1][j]);
				length += terms[1][j];
			}
		}
		return getEntropy(map)/map.size();
	}
	
	/**
	 * 
	 * @param map Mapping from termid to frequency.
	 * @return
	 */
	public double getEntropy(TIntIntHashMap map){
		int length = 0;
		for (int freq : map.getValues())
			length += freq;
		double[] probs = new double[map.size()];
		int[] termids = map.keys();
		for (int i=0; i<termids.length; i++){
			//probs[i] = lambda*(double)map.get(termids[i])/length+(1-lambda)*lexicon.getLexiconEntry(termids[i]).TF/numberOfTokens;
			probs[i] = (double)map.get(termids[i])/length;
		}
		return getEntropy(probs);
	}
	
	public double getEntropy(double[] probs){
		double entropy = 0d;
		for (int i=0; i<probs.length; i++){
			if (probs[i] > 0)
				entropy += (-probs[i]*idf.log(probs[i]));
		}
		return entropy;
	}
	
	public void printEntropyOfTopRankedDocs(String resultFilename, int minX, int maxX){
		TObjectDoubleHashMap<String>[] maps = new TObjectDoubleHashMap[maxX-minX+1];
		int counter = 0;
		for (int X=minX; X<=maxX; X++){
			System.out.print("Computing for top "+X+" documents...");
			maps[counter++] = this.getEntropyOfTopRankedDocs(resultFilename, X);
			System.out.println("Done.");
		}
		String[] queryids = (String[])maps[0].keys(new String[maps[0].size()]);
		Arrays.sort(queryids);
		for (int i=0; i<queryids.length; i++){
			System.out.print(queryids[i]+": ");
			for (int j=0; j<maps.length; j++){
				System.out.print(Rounding.toString(maps[j].get(queryids[i]), 4)+" ");
			}
			System.out.println();
		}
		System.out.print("Avg: ");
		for (int i=0; i<maps.length; i++){
			System.out.print(Rounding.toString(Statistics.mean(maps[i].getValues()), 4)+" ");
		}
	}
	
	public void printEntropyOfTopRankedDocsWithTopTerms(String resultFilename, int minX, int maxX){
		TObjectDoubleHashMap<String>[] maps = new TObjectDoubleHashMap[maxX-minX+1];
		int counter = 0;
		for (int X=minX; X<=maxX; X++){
			System.out.print("Computing for top "+X+" documents...");
			maps[counter++] = this.getEntropyOfTopRankedDocsForTopTerms(resultFilename, X);
			System.out.println("Done.");
		}
		String[] queryids = (String[])maps[0].keys(new String[maps[0].size()]);
		Arrays.sort(queryids);
		for (int i=0; i<queryids.length; i++){
			System.out.print(queryids[i]+": ");
			for (int j=0; j<maps.length; j++){
				System.out.print(Rounding.toString(maps[j].get(queryids[i]), 4)+" ");
			}
			System.out.println();
		}
		System.out.print("Avg: ");
		for (int i=0; i<maps.length; i++){
			System.out.print(Rounding.toString(Statistics.mean(maps[i].getValues()), 4)+" ");
		}
	}
	
	public TObjectDoubleHashMap<String> getEntropyOfTopRankedDocs(String resultFilename, int topX){
		TObjectDoubleHashMap<String> queryidEntropyMap = new TObjectDoubleHashMap<String>();
		try{
			BufferedReader br = Files.openFileReader(resultFilename);
			String currentQueryid = "1st";
			String line = null;
			TIntHashSet docidSet = new TIntHashSet();
			int counter = 0;
			ArrayList<Double> entropies = new ArrayList<Double>();
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				String queryid = tokens[0];
				int docid = Integer.parseInt(tokens[2]);
				if (currentQueryid.equals("1st")){
					currentQueryid = queryid;
				}else if (!currentQueryid.equals(queryid)){
					//System.err.print(currentQueryid+": ");
					int[] docids = docidSet.toArray();
					//for (int i=0; i<docids.length; i++)
						//System.err.print(docIndex.getDocumentNumber(docids[i])+", ");
					double entropy = getEntropy(docids);
					//System.err.println(entropy);
					queryidEntropyMap.put(currentQueryid, entropy);
					entropies.add(entropy);
					docidSet = new TIntHashSet();
					currentQueryid = queryid;
					counter = 0;
				}
				if (counter < topX)
					docidSet.add(docid);
				counter++;
			}
			System.err.print(currentQueryid+": ");
			int[] docids = docidSet.toArray();
			//for (int i=0; i<docids.length; i++)
				//System.err.print(docIndex.getDocumentNumber(docids[i])+", ");
			double entropy = getEntropy(docids);
			//System.err.println(entropy);
			queryidEntropyMap.put(currentQueryid, entropy);
			entropies.add(entropy);
			docidSet = new TIntHashSet();
			br.close();
			Object[] obj = entropies.toArray(new Double[entropies.size()]);
			double[] values = new double[entropies.size()];
			for (int i=0; i<values.length; i++)
				values[i] = ((Double)obj[i]).doubleValue();
			//System.err.println("Mean: "+Statistics.mean(values));
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		return queryidEntropyMap;
	}
	
	public TObjectDoubleHashMap<String> getEntropyOfTopRankedDocsForTopTerms(String resultFilename, int topX){
		TObjectDoubleHashMap<String> queryidEntropyMap = new TObjectDoubleHashMap<String>();
		try{
			BufferedReader br = Files.openFileReader(resultFilename);
			String currentQueryid = "1st";
			String line = null;
			TIntHashSet docidSet = new TIntHashSet();
			int counter = 0;
			ArrayList<Double> entropies = new ArrayList<Double>();
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				String queryid = tokens[0];
				int docid = Integer.parseInt(tokens[2]);
				if (currentQueryid.equals("1st")){
					currentQueryid = queryid;
				}else if (!currentQueryid.equals(queryid)){
					int[] docids = docidSet.toArray();
					double entropy = this.getEntropyForTopTerms(docids);
					//System.err.println(entropy);
					queryidEntropyMap.put(currentQueryid, entropy);
					entropies.add(entropy);
					docidSet = new TIntHashSet();
					currentQueryid = queryid;
					counter = 0;
				}
				if (counter < topX)
					docidSet.add(docid);
				counter++;
			}
			System.err.print(currentQueryid+": ");
			int[] docids = docidSet.toArray();
			//for (int i=0; i<docids.length; i++)
				//System.err.print(docIndex.getDocumentNumber(docids[i])+", ");
			double entropy = getEntropy(docids);
			//System.err.println(entropy);
			queryidEntropyMap.put(currentQueryid, entropy);
			entropies.add(entropy);
			docidSet = new TIntHashSet();
			br.close();
			Object[] obj = entropies.toArray(new Double[entropies.size()]);
			double[] values = new double[entropies.size()];
			for (int i=0; i<values.length; i++)
				values[i] = ((Double)obj[i]).doubleValue();
			//System.err.println("Mean: "+Statistics.mean(values));
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		return queryidEntropyMap;
	}
	
	public void printEntropyOfTopRankedDocs(String resultFilename, int topX){
		try{
			BufferedReader br = Files.openFileReader(resultFilename);
			String currentQueryid = "1st";
			String line = null;
			TIntHashSet docidSet = new TIntHashSet();
			int counter = 0;
			ArrayList<Double> entropies = new ArrayList<Double>();
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				String queryid = tokens[0];
				int docid = Integer.parseInt(tokens[2]);
				if (currentQueryid.equals("1st")){
					currentQueryid = queryid;
				}else if (!currentQueryid.equals(queryid)){
					System.err.print(currentQueryid+": ");
					int[] docids = docidSet.toArray();
					for (int i=0; i<docids.length; i++)
						System.err.print(docIndex.getDocumentNumber(docids[i])+", ");
					double entropy = getEntropy(docids);
					System.err.println(entropy);
					entropies.add(entropy);
					docidSet = new TIntHashSet();
					currentQueryid = queryid;
					counter = 0;
				}
				if (counter < topX)
					docidSet.add(docid);
				counter++;
			}
			System.err.print(currentQueryid+": ");
			int[] docids = docidSet.toArray();
			for (int i=0; i<docids.length; i++)
				System.err.print(docIndex.getDocumentNumber(docids[i])+", ");
			double entropy = getEntropy(docids);
			System.err.println(entropy);
			entropies.add(entropy);
			docidSet = new TIntHashSet();
			br.close();
			Object[] obj = entropies.toArray(new Double[entropies.size()]);
			double[] values = new double[entropies.size()];
			for (int i=0; i<values.length; i++)
				values[i] = ((Double)obj[i]).doubleValue();
			System.err.println("Mean: "+Statistics.mean(values));
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public void printEntropyOfTopRankedDocsForTopTerms(String resultFilename, int topX){
		try{
			BufferedReader br = Files.openFileReader(resultFilename);
			String currentQueryid = "1st";
			String line = null;
			TIntHashSet docidSet = new TIntHashSet();
			int counter = 0;
			ArrayList<Double> entropies = new ArrayList<Double>();
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				String queryid = tokens[0];
				int docid = Integer.parseInt(tokens[2]);
				if (currentQueryid.equals("1st")){
					currentQueryid = queryid;
				}else if (!currentQueryid.equals(queryid)){
					System.err.print(currentQueryid+": ");
					int[] docids = docidSet.toArray();
					for (int i=0; i<docids.length; i++)
						System.err.print(docIndex.getDocumentNumber(docids[i])+", ");
					double entropy = getEntropy(docids);
					System.err.println(entropy);
					entropies.add(entropy);
					docidSet = new TIntHashSet();
					currentQueryid = queryid;
					counter = 0;
				}
				if (counter < topX)
					docidSet.add(docid);
				counter++;
			}
			System.err.print(currentQueryid+": ");
			int[] docids = docidSet.toArray();
			for (int i=0; i<docids.length; i++)
				System.err.print(docIndex.getDocumentNumber(docids[i])+", ");
			double entropy = getEntropy(docids);
			System.err.println(entropy);
			entropies.add(entropy);
			docidSet = new TIntHashSet();
			br.close();
			Object[] obj = entropies.toArray(new Double[entropies.size()]);
			double[] values = new double[entropies.size()];
			for (int i=0; i<values.length; i++)
				values[i] = ((Double)obj[i]).doubleValue();
			System.err.println("Mean: "+Statistics.mean(values));
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if (args[0].equals("--convertresultswithdocids")){
			// --convertresultswithdocids docidOutputFilename resultFilename outputFilename
			IndexUtility.convertResultsWithDocid(args[1], args[2], args[3]);
		}else if (args[0].equals("--printentropyfromconvertedresults")){
			DocumentSetEntropy app = new DocumentSetEntropy(args[1], args[2]);
			// --printentropyfromconvertedresults indexpath indexprefix resultFilename(converted with docids) minX maxX
			app.printEntropyOfTopRankedDocs(args[3], Integer.parseInt(args[4]), Integer.parseInt(args[5]));
		}else if (args[0].equals("--printcoherencefromconvertedresults")){
			DocumentSetEntropy app = new DocumentSetEntropy(args[1], args[2]);
			// --printcoherencefromconvertedresults indexpath indexprefix resultFilename(converted with docids) minX maxX
			app.printCoherenceOfTopRankedDocs(args[3], Integer.parseInt(args[4]), Integer.parseInt(args[5]));
		}else if (args[0].equals("--printentropyfortopterms")){
			DocumentSetEntropy app = new DocumentSetEntropy(args[1], args[2]);
			// --printentropyfromconvertedresults indexpath indexprefix resultFilename(converted with docids) minX maxX
			app.printEntropyOfTopRankedDocsWithTopTerms(args[3], Integer.parseInt(args[4]), Integer.parseInt(args[5]));
		}
	}

}

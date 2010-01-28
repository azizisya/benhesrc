package tests;

import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntObjectHashMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;

import uk.ac.gla.terrier.matching.models.WeightingModel;
import uk.ac.gla.terrier.statistics.CosineSimilarity;
import uk.ac.gla.terrier.statistics.Statistics;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.structures.LexiconInputStream;
import uk.ac.gla.terrier.terms.PorterStemmer;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;

public class TestClarity {
	
	protected Index index;
	
	protected WeightingModel wmodel;
	
	protected double c;
	
	protected final double k = Double.parseDouble(ApplicationSetup.getProperty("ec.k", "0.00394041"));
	
	
	protected TIntObjectHashMap<int[]> termidEntryMap;
	
	public TestClarity(){
		System.err.println("Creating index...");
		index = Index.createIndex();
		System.err.println("IndexCreated.");
		this.getWeightingModel();
		System.err.println("Loading lexicon...");
		this.loadLexicon();
		System.err.println("Lexicon loaded.");
	}
	
	protected TIntDoubleHashMap constructDocVector(int docid){
		int[][] terms = index.getDirectIndex().getTerms(docid);
		int docLength = index.getDocumentIndex().getDocumentLength(docid);
		return constructDocVector(terms, docLength);
	}
	
	protected TIntDoubleHashMap constructDocVector(int[][] terms, int docLength){
		TIntDoubleHashMap vMap = new TIntDoubleHashMap();
		int n = terms[0].length;
		//double maxScore = 0d;
		for (int i=0; i<n; i++){
			//lex.findTerm(terms[0][i]);
			int[] entry = termidEntryMap.get(terms[0][i]);
			double score = wmodel.score((double)terms[1][i], (double)docLength,(double) entry[0], (double)entry[1], 1d);
			vMap.put(terms[0][i], score);
		}
		//System.out.println("maxscore: "+maxScore);
		return vMap;
	}
	// load lexicon entries in memory to speed up process. This could cause outofmemory exception.
	protected void loadLexicon(){
		this.termidEntryMap = new TIntObjectHashMap<int[]>();
		try{
			LexiconInputStream lexin = new LexiconInputStream(index.getPath(), index.getPrefix());
			while (lexin.readNextEntry()!=-1){
				int[] entry = {lexin.getNt(), lexin.getTF()};
				termidEntryMap.put(lexin.getTermId(), entry);
			}
			lexin.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	private void loadFilter(String filename, TIntHashSet whiteTermidSet){
		try{
			BufferedReader br = Files.openFileReader(filename);
			Lexicon lexicon = index.getLexicon();
			String str = null;
			PorterStemmer stemmer = new PorterStemmer(null);
			while ((str=br.readLine())!=null){
				str = str.trim();
				if (str.length()==0) continue;
				str = stemmer.stem(str);
				if (lexicon.findTerm(str))
					whiteTermidSet.add(lexicon.getTermId());
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public void computeThresholdFromRandomSamples(double percentage, int samplesize, int repeat){
		double[] k = new double[repeat];
		for (int i=0; i<repeat; i++){
			System.out.print("Sample "+i+"...");
			k[i] = computeThresholdFromRandomSamples(percentage, samplesize);
			System.out.println(k[i]);
		}
		System.out.println("Mean threshold: "+Statistics.mean(k));
	}
	
	public double computeThresholdFromRandomSamples(double percentage, int samplesize){
		int[] docids = randomSampleDocids(samplesize);
		TIntObjectHashMap<TIntDoubleHashMap> docidVMap = new TIntObjectHashMap<TIntDoubleHashMap>();
		constructDocVectors(docids, docidVMap);
		return computeThreshold(percentage, docidVMap);
	}
	
	public double computeThreshold(double percentage, TIntObjectHashMap<TIntDoubleHashMap> docidVMap){
		int size = docidVMap.size();
		int n = size*(size-1)/2;
		double[] cosineSims = new double[n];
		int counter = 0;
		int[] keys = docidVMap.keys();
		for (int i=0; i<size-1; i++)
			for (int j=i+1; j<size; j++)
				cosineSims[counter++] = CosineSimilarity.cosine(docidVMap.get(keys[i]), docidVMap.get(keys[j]));
		Arrays.sort(cosineSims);
		return cosineSims[(int)(percentage*n)];
	}
	
	public double getInterDocCosine(int docid1, int docid2){
		 return CosineSimilarity.cosine(this.constructDocVector(docid1), this.constructDocVector(docid2));
	}
	
	private void constructDocVectors(int[] docids, TIntObjectHashMap<TIntDoubleHashMap> docidVMap){
		for (int i=0; i<docids.length; i++)
			docidVMap.put(docids[i], this.constructDocVector(docids[i]));
	}
	
	private int[] randomSampleDocids(int numberOfSamples){
		DocumentIndex doci = index.getDocumentIndex();
		TIntHashSet docidSet = new TIntHashSet();
		int N = index.getCollectionStatistics().getNumberOfDocuments();
		while (docidSet.size()<numberOfSamples){
			int randomid = (int)(Math.random()*(N-1));
			if (doci.getDocumentLength(randomid)>0)
				docidSet.add(randomid);
		}
		return docidSet.toArray();
	}

	protected void getWeightingModel(){
		String namespace = "uk.ac.gla.terrier.matching.models.";
		String modelName = namespace.concat(ApplicationSetup.getProperty("trec.model", "TF_IDF"));
		double c = Double.parseDouble(ApplicationSetup.getProperty("c", "0.35"));
		try{
			if (modelName.indexOf("(") > 0){
				String params = modelName.substring( 
					modelName.indexOf("(")+1, modelName.indexOf(")"));
				String[] parameters = params.split("\\s*,\\s*");
				
				wmodel = (WeightingModel) Class.forName(
								modelName.substring(0,modelName.indexOf("(")))
						.getConstructor(
								new Class[]{String[].class})
						.newInstance(
								new Object[]{parameters});
			}else{						
				wmodel = (WeightingModel) Class.forName(modelName).newInstance();
			}
		}catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
		wmodel.setParameter(c);
		CollectionStatistics collStat = index.getCollectionStatistics();
		wmodel.setAverageDocumentLength(collStat.getAverageDocumentLength());
		wmodel.setNumberOfDocuments(collStat.getNumberOfDocuments());
		wmodel.setNumberOfPointers(collStat.getNumberOfPointers());
		wmodel.setNumberOfTokens(collStat.getNumberOfTokens());
		wmodel.setNumberOfUniqueTerms(collStat.getNumberOfUniqueTerms());
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args[1].equals("--samplethreshold")){
			// -tc --samplethreshold <percentage> <samplesize> <repeat>
			TestClarity tc = new TestClarity();
			tc.computeThresholdFromRandomSamples(Double.parseDouble(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
		}
	}

}

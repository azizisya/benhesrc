/*
 * Created on 2005-1-1
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package tests;

import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectIntHashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.StringTokenizer;

import uk.ac.gla.terrier.evaluation.AdhocEvaluation;
import uk.ac.gla.terrier.evaluation.TRECQrelsInMemory;
import uk.ac.gla.terrier.evaluation.TRECResultsInMemory;
import uk.ac.gla.terrier.matching.models.Idf;
import uk.ac.gla.terrier.matching.models.WeightingModel;
import uk.ac.gla.terrier.matching.models.queryexpansion.QueryExpansionModel;
import uk.ac.gla.terrier.querying.QueryExpansion;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.HeapSort;
import uk.ac.gla.terrier.statistics.ScoreNormaliser;
import uk.ac.gla.terrier.statistics.Statistics;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.DirectIndex;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.ExpansionTerms;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.structures.LexiconOutputStream;
import uk.ac.gla.terrier.structures.TRECQuery;
import uk.ac.gla.terrier.terms.PorterStemmer;
import uk.ac.gla.terrier.utility.ApplicationSetup;

/**
 * @author ben
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class TestEvaluation {
	
	static public void computeRelDocLength(){
		TRECQrelsInMemory qrels = new TRECQrelsInMemory();
		Index index = Index.createIndex();
		DocumentIndex docIndex = index.getDocumentIndex();
		TIntArrayList relDocLength = new TIntArrayList();
		String[] queryids = qrels.getQueryids();
		for (int i=0; i<queryids.length; i++){
			String[] docnos = qrels.getRelevantDocumentsToArray(queryids[i]);
			System.err.println("query "+queryids[i]+" has "+docnos.length+" pooled relevant documents.");
			for (int j=0; j<docnos.length; j++){
				relDocLength.add(docIndex.getDocumentId(docnos[j]));
				System.err.println(docnos[j]+": "+docIndex.getDocumentId(docnos[j]));
			}
		}
		int[] docLength = relDocLength.toNativeArray();
		double avl = Statistics.mean(docLength);
		System.err.println("Average document length of the pooled relevant documents: "+
				avl);
		index.close();
	}

	
	static public void computeAverageRankingOfFeedbackDocuments(
			String feedbackFilename,
			String resultFilename
			){
		int counter = 0;
		THashMap queryidRelDocnoMap = new THashMap();
		// get queryids from the result file
		TRECResultsInMemory results = new TRECResultsInMemory(resultFilename);
		String[] queryids = results.getQueryids();
		// load relevance information
		loadRelevanceInformation(feedbackFilename, queryidRelDocnoMap);
		// for each query
		double sumRankings = 0d;
		for (int i=0; i<queryids.length; i++){
			// sum the ranking of feedback documents in the results
			String[] docnos = (String[])queryidRelDocnoMap.get(queryids[i]);
			if (docnos==null) continue;
			System.out.println(queryids[i]+", docnos.length: "+docnos.length);
			for (int j=0; j<docnos.length; j++){
				int ranking = results.getRank(queryids[i], docnos[j]);
				if (ranking != -1)
					sumRankings += ranking;
			}
			counter+=docnos.length;
		// end for
		}
		// print mean rankings
		System.out.println("mean ranking: "+sumRankings/counter);
	}
	
	static public void loadRelevanceInformation(String filename, THashMap queryidRelDocnoMap){
		try{
			BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
			THashSet queryids = new THashSet();
			String line = null;
			while ((line=br.readLine())!=null){
				line=line.trim();
				if (line.length()==0)
					continue;
				StringTokenizer stk = new StringTokenizer(line);
				String[] relDocnos = new String[stk.countTokens()-1];
				String queryid = stk.nextToken();
				queryids.add(queryid);
				for (int i=0; i<relDocnos.length; i++)
					relDocnos[i]=stk.nextToken();
				queryidRelDocnoMap.put(queryid, relDocnos);
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	static public void createQrels(String qrelsFilename, int[] grades){
		TRECQrelsInMemory qrels = new TRECQrelsInMemory(); 
		// for each query
		String[] queryids = qrels.getQueryids();
		for (int i=0; i<queryids.length; i++){
			// get docnos
			//String[] relDocnos = qrels.getRelevantDocumentsToArray(queryids[i], );
			//String[]
		
			// write new qrels
		}
		// end for
		
	}
	
	static public void buildQrelsLexicon(int[] grades){
		// initialise data structure
		// terms in the pool
		TIntHashSet termidSet = new TIntHashSet();
		// hashmaps from terms to their Nt and TF in the pool
		TIntIntHashMap termNtMap = new TIntIntHashMap();
		TIntIntHashMap termTFMap = new TIntIntHashMap();
		
		// Load qrels
		TRECQrelsInMemory qrels = new TRECQrelsInMemory();
		
		// get docnos of all relevant documents
		THashSet<String> relDocnoSet = new THashSet<String>();
		String[] relDocnos = null;
		if (grades.length==1){
			if (grades[0]==0)
				relDocnos = qrels.getAllRelevantDocumentsToArray();
			else
				relDocnos = qrels.getRelevantDocumentsToArray(grades[0]);
		}
		else{
			for (int i=0; i<grades.length; i++){
				relDocnos = qrels.getRelevantDocumentsToArray(grades[i]);
				for (int j=0; j<relDocnos.length; j++)
					relDocnoSet.add(relDocnos[j]);
			}
			relDocnos = (String[])relDocnoSet.toArray(new String[relDocnoSet.size()]);
		}
		
		// create index
		Index index = Index.createIndex();
		DirectIndex directIndex = index.getDirectIndex();
		DocumentIndex docIndex = index.getDocumentIndex();
		Lexicon lex = index.getLexicon();
		
		// Compute Nt and TF in the pool for each term in the relevant documents
		System.out.print("Parsing pooled relevant documents...");
		for (int i=0; i<relDocnos.length; i++){
			int[][] terms = directIndex.getTerms(docIndex.getDocumentId(relDocnos[i]));
			if (terms == null)
				continue;
			else
				for (int j = 0; j < terms[0].length; j++){
					int termid = terms[0][j];
					int tf = terms[1][j];
					termidSet.add(termid);
					termNtMap.adjustOrPutValue(termid, 1, 1);
					termTFMap.adjustOrPutValue(termid, tf, tf);
				}
		}
		System.out.println("Done!");
		System.out.println("Found "+termidSet.size()+" terms in the pooled relevant documents.");
		
		// write the new lexicon
		System.out.print("Writing to the new lexicon file...");
		int[] termids = termidSet.toArray();
			// map from terms to termids
		String[] terms = new String[termids.length];
		TObjectIntHashMap termMap = new TObjectIntHashMap();
		int numberOfTerms = termids.length;
		for (int i=0; i<numberOfTerms; i++){
			lex.findTerm(termids[i]);
			termMap.put(lex.getTerm(), termids[i]);
			terms[i] = lex.getTerm();
		}
		
		String gradeString = ""+grades[0];
		for (int i=1; i<grades.length; i++)
			gradeString=gradeString+"-"+grades[i];
		
		String lexoutFilename = ApplicationSetup.TREC_RESULTS+"/"+"qrels"+gradeString+".lex";
		LexiconOutputStream lexout = new LexiconOutputStream(lexoutFilename);
		Arrays.sort(terms);
		for (int i=0; i<numberOfTerms; i++){
			try{
				lexout.writeNextEntry(terms[i], termMap.get(terms[i]), 
						termNtMap.get(termids[i]), 
						termTFMap.get(termids[i]), 0L, (byte)0);
			}catch(IOException ioe){
				ioe.printStackTrace();
				System.exit(1);
			}
		}
		System.out.println("Done!");
		try{
			System.out.print("Building lexid...");
			lexout.close();
			Arrays.sort(termids);
			TestIndex.createLexiconIndex(lexoutFilename, lexoutFilename+"id", termids[termids.length-1]);
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		System.out.println("Done! New lexicon file written to "+lexoutFilename);
		index.close();
	}
	
	
	
	static public void writeOneLineQuery(String filename){
		try{
			StringBuffer buffer = new StringBuffer();
			String outputFilename = filename+"_oneline.topics";
			// load terms and weights
			BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
			//if (!new File(outputFilename).exists())
				//new File(outputFilename).createNewFile();
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputFilename)));
			String str = null;
			
			while ((str=br.readLine())!=null){
				str=str.trim();
				if (str.length()==0) continue;
				StringTokenizer stk = new StringTokenizer(str);
				String term = stk.nextToken();
				String weight = stk.nextToken();
				bw.write(term+"^"+weight+" ");
			}
			br.close();
			bw.close();
				
			
			System.out.println("Done. One line query saved in file "+outputFilename);
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	static public void writeTopOneLineQuery(String filename, int topx){
		try{
			StringBuffer buffer = new StringBuffer();
			String outputFilename = filename+"_oneline.topics.top"+topx;
			// load terms and weights
			BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
			//if (!new File(outputFilename).exists())
				//new File(outputFilename).createNewFile();
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputFilename)));
			String str = null;
			int counter = 0;
			while ((str=br.readLine())!=null){
				str=str.trim();
				if (str.length()==0) continue;
				StringTokenizer stk = new StringTokenizer(str);
				String term = stk.nextToken();
				String weight = stk.nextToken();
				bw.write(term+"^"+weight+" ");
				//bw.write(term+"^"+"1.0 ");
				counter++;
				if (counter >= topx)
					break;
			}
			br.close();
			bw.close();
				
			
			System.out.println("Done. One line query saved in file "+outputFilename);
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	static public void computeDivTermQrels(
			String qemodelName, 
			String wordlist,
			String bgLexiconFilename,
			int grades[]
			){
		// initiate QE model
		QueryExpansion qe = new QueryExpansion();
		WeightingModel qemodel = WeightingModel.getWeightingModel(qemodelName);
		System.out.print("grades: ");
		for (int i=0; i<grades.length; i++)
			System.out.print(grades[i]+" ");
		// Get pseudoLength
		System.out.print("Computing pseudoLength...");
		TRECQrelsInMemory qrels = new TRECQrelsInMemory();
		Index index = Index.createIndex();
		DocumentIndex docIndex = index.getDocumentIndex();
		String[] queryids = qrels.getQueryids();
		
		// get docnos of all relevant documents
		THashSet relDocnoSet = new THashSet();
		String[] relDocnos = null;
		if (grades.length==1){
			if (grades[0]==0)
				relDocnos = qrels.getAllRelevantDocumentsToArray();
			else
				relDocnos = qrels.getRelevantDocumentsToArray(grades[0]);
		}
		else{
			for (int i=0; i<grades.length; i++){
				relDocnos = qrels.getRelevantDocumentsToArray(grades[i]);
				for (int j=0; j<relDocnos.length; j++)
					relDocnoSet.add(relDocnos[j]);
			}
			relDocnos = (String[])relDocnoSet.toArray(new String[relDocnoSet.size()]);
		}
		
		double pseudoLength = 0d;
		for (int i=0; i<relDocnos.length; i++){
			pseudoLength += docIndex.getDocumentLength(relDocnos[i]);
		}
		System.out.println("Done. PseudoLength="+pseudoLength);
		// initiate expansionTerms
		System.out.print("Initialising expansionTerms...");
		CollectionStatistics collSta = index.getCollectionStatistics();
		
		String[] bgDocnos = qrels.getAllRelevantDocumentsToArray();
		//System.out.println("bgDocnos.length: "+bgDocnos.length);
		int numberOfDocuments = bgDocnos.length;
		long numberOfTokens = 0;
		for (int i=0; i<bgDocnos.length; i++)
			numberOfTokens += docIndex.getDocumentLength(bgDocnos[i]);
		double avl = (double)numberOfTokens/numberOfDocuments;
		Lexicon bgLexicon = new Lexicon(bgLexiconFilename);
		//System.out.println("background lexicon filename: "+bgLexiconFilename);
		//bgLexicon.print();
		
		ExpansionTerms expansionTerms = new ExpansionTerms(
				numberOfDocuments,
				numberOfTokens,
				avl,
				pseudoLength, 
				bgLexicon);
		DirectIndex df = index.getDirectIndex();
		for (int i=0; i<relDocnos.length; i++){
			int[][] terms = df.getTerms(docIndex.getDocumentId(relDocnos[i]));
			if (terms == null)
				continue;
			else
				for (int j = 0; j < terms[0].length; j++)
					expansionTerms.insertTerm(terms[0][j], (double)terms[1][j]);
		}
		System.out.println("Done.");
		// assign weights and get the term weights
		System.out.print("Loading opinionated words...");
		expansionTerms.assignWeights(qemodel);
		THashSet termSet = new THashSet();
		File fWordlist = new File(wordlist);
		THashMap termStemmingMap = new THashMap();// map from stemmed terms to their original forms
		try{
			BufferedReader br = new BufferedReader(new FileReader(fWordlist));
			String str = null;
			PorterStemmer stemmer = new PorterStemmer(null);
			while ((str=br.readLine())!=null){
				str = str.trim().toLowerCase();
				if (str.length() == 0)
					continue;
				String stemmed = stemmer.stem(str);
				termSet.add(stemmed);
				termStemmingMap.put(stemmed, str);
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		String[] terms = (String[])termSet.toArray(new String[termSet.size()]);
		double[] weights = new double[terms.length];
		System.out.println("Done.");
		System.out.print("Computing term weights...");
		for (int i=0; i<terms.length; i++)
			weights[i] = expansionTerms.getOriginalExpansionWeight(terms[i]);
		System.out.println("Done.");
		// write term weights
		System.out.print("Writing term weights...");
		
		String gradeString = ""+grades[0];
		for (int i=1; i<grades.length; i++)
			gradeString=gradeString+"-"+grades[i];
		
		String filename = wordlist+"_Weights_"+
				gradeString;
		try{
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(
					filename)));
			for (int i=0; i<terms.length; i++)
				if (weights[i] >0)
					bw.write(termStemmingMap.get(terms[i])+" "+weights[i]+ApplicationSetup.EOL);
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		index.close();
		System.out.println("Done. Term weights saved in file "+filename);
	}
	
	static public void computeProbTermQrels(String qemodelName, String wordlist){
		// initiate QE model
		//QueryExpansion qe = new QueryExpansion();
		WeightingModel qemodel = WeightingModel.getWeightingModel(qemodelName);
		// Get pseudoLength
		System.out.print("Computing pseudoLength...");
		TRECQrelsInMemory qrels = new TRECQrelsInMemory();
		THashSet relDocnosSet = new THashSet();
		Index index = Index.createIndex();
		DocumentIndex docIndex = index.getDocumentIndex();
		String[] queryids = qrels.getQueryids();
		double pseudoLength = 0d;
		for (int i=0; i<queryids.length; i++){
			String[] docnosTmp = qrels.getRelevantDocumentsToArray(queryids[i]);
			for (int j=0; j<docnosTmp.length; j++){
				pseudoLength += docIndex.getDocumentLength(docnosTmp[j]);
				relDocnosSet.add(docnosTmp[j]);
			}
		}
		String[] relDocnos = (String[])relDocnosSet.toArray(new String[relDocnosSet.size()]);
		System.out.println("Done. PseudoLength="+pseudoLength);
		// initiate expansionTerms
		System.out.print("Initialising expansionTerms...");
		CollectionStatistics collSta = index.getCollectionStatistics();
		ExpansionTerms expansionTerms = new ExpansionTerms(collSta, pseudoLength, index.getLexicon());
		DirectIndex df = index.getDirectIndex();
		for (int i=0; i<relDocnos.length; i++){
			int[][] terms = df.getTerms(docIndex.getDocumentId(relDocnos[i]));
			//System.out.println("Parsing document "+relDocnos[i]+" "+(i+1)+
					//" out of "+relDocnos.length);
			if (terms == null)
				continue;
			else
				for (int j = 0; j < terms[0].length; j++)
					expansionTerms.insertTerm(terms[0][j], (double)terms[1][j]);
		}
		System.out.println("Done.");
		// assign weights and get the term weights
		System.out.print("Loading opinionated words...");
		expansionTerms.assignWeights(qemodel);
		THashSet termSet = new THashSet();
		File fWordlist = new File(wordlist);
		try{
			BufferedReader br = new BufferedReader(new FileReader(fWordlist));
			String str = null;
			PorterStemmer stemmer = new PorterStemmer(null);
			while ((str=br.readLine())!=null){
				str = str.trim();
				if (str.length() == 0)
					continue;
				termSet.add(stemmer.stem(str));
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		String[] terms = (String[])termSet.toArray(new String[termSet.size()]);
		double[] weights = new double[terms.length];
		System.out.println("Done.");
		System.out.print("Computing term weights...");
		for (int i=0; i<terms.length; i++)
			weights[i] = expansionTerms.getOriginalExpansionWeight(terms[i]);
		System.out.println("Done.");
		// write term weights
		System.out.print("Writing term weights...");
		try{
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(
					ApplicationSetup.TREC_RESULTS, "opinionTermWeights")));
			for (int i=0; i<terms.length; i++)
				if (weights[i]>0d)
					bw.write(terms[i]+" "+weights[i]+ApplicationSetup.EOL);
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		index.close();
		System.out.println("Done.");
	}
	
	public static void writeClickedDocuments(int grade, int numberOfClickedDocumentsPerQuery){
		try{
			TRECQrelsInMemory qrels = new TRECQrelsInMemory();
			String outputFilename = ApplicationSetup.TREC_RESULTS+
					ApplicationSetup.FILE_SEPARATOR+"clickedDocuments_"+grade+"_"+numberOfClickedDocumentsPerQuery;
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputFilename)));
			// get queryids
			String[] queryids = qrels.getQueryids();
			for (int i=0; i<queryids.length; i++){
				// for each query, get relevant documents with the given grade
				String[] docnos = null;
				if (grade!=0)
					docnos = qrels.getRelevantDocumentsToArray(queryids[i], grade);
				else
					docnos = qrels.getRelevantDocumentsToArray(queryids[i]);
				// write disk
				if (docnos == null || docnos.length == 0)
					continue;
				int numberOfDocumentsToWrite = numberOfClickedDocumentsPerQuery;
				if (numberOfClickedDocumentsPerQuery > docnos.length){
					if (docnos.length == 1)
						continue;
					else
						numberOfDocumentsToWrite = docnos.length/2;
				}
				THashSet clickedDocnos = new THashSet();
				bw.write(queryids[i]+" ");
				for (int j=0; j<numberOfDocumentsToWrite; j++){
					int randomPosition = (int)(Math.random()*(docnos.length));
					if (clickedDocnos.contains(randomPosition))
						continue;
					else{
						clickedDocnos.add(docnos[randomPosition]);
						bw.write(docnos[randomPosition]+" ");
					}
				}
				bw.write(ApplicationSetup.EOL);
			}
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		 
	}
	
	public static void writeLongestClickedDocuments(int grade, int numberOfClickedDocumentsPerQuery){
		try{
			TRECQrelsInMemory qrels = new TRECQrelsInMemory();
			String outputFilename = ApplicationSetup.TREC_RESULTS+
					ApplicationSetup.FILE_SEPARATOR+"longestClickedDocuments_"+grade+"_"+numberOfClickedDocumentsPerQuery;
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputFilename)));
			
			Index index = Index.createIndex();
			DocumentIndex docIndex = index.getDocumentIndex();
			
			// get queryids
			String[] queryids = qrels.getQueryids();
			for (int i=0; i<queryids.length; i++){
				// for each query, get relevant documents with the given grade
				String[] docnos = null;
				if (grade!=0)
					docnos = qrels.getRelevantDocumentsToArray(queryids[i], grade);
				else
					docnos = qrels.getRelevantDocumentsToArray(queryids[i]);
				// write disk
				if (docnos == null || docnos.length == 0)
					continue;
				int numberOfDocumentsToWrite = numberOfClickedDocumentsPerQuery;
				if (numberOfClickedDocumentsPerQuery > docnos.length){
					if (docnos.length == 1)
						continue;
					else
						numberOfDocumentsToWrite = docnos.length/2;
				}
				THashSet clickedDocnos = new THashSet();
				bw.write(queryids[i]+" ");
				// get doc length
				int relDocCount = docnos.length;
				int[] docLength = new int[relDocCount];
				for (int j=0; j<relDocCount; j++){
					docLength[j] = docIndex.getDocumentLength(docnos[j]);
				}
				
				// get the longest documents
				for (int x=0; x<numberOfDocumentsToWrite; x++){
					int longestIndex = x;
					for (int y=x+1 ;y<relDocCount; y++){
						if (docLength[y] > docLength[longestIndex])
							longestIndex = y;
					}
					bw.write(docnos[longestIndex]+" ");
					String docnoTmp = docnos[x]+"";
					int docLengthTmp = docLength[x];
					docnos[x] = docnos[longestIndex]+"";
					docLength[x] = docLength[longestIndex];
					docnos[longestIndex] = docnoTmp+"";
					docLength[longestIndex] = docLengthTmp;
				}
				bw.write(ApplicationSetup.EOL);
				index.close();
			}
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		 
	}
	
	public static void combineOpinionPriors(
			String resultFilename, 
			String opinionWeightIndexFilename,
			double k){
		try{
			THashSet docnoSet = new THashSet();
			TObjectDoubleHashMap docnoOpinionWeightMap = new TObjectDoubleHashMap();
			if (opinionWeightIndexFilename.endsWith(".opf"))
				loadOpinionFinderWeights(opinionWeightIndexFilename, docnoSet, docnoOpinionWeightMap);
			else if (opinionWeightIndexFilename.endsWith(".ext"))
				loadExtraOpinionFinderWeights(opinionWeightIndexFilename, docnoSet, docnoOpinionWeightMap);
			else
				loadOpinionWeights(opinionWeightIndexFilename, docnoSet, docnoOpinionWeightMap);
			// normalise opinion weights
			String[] docnos = (String[])docnoSet.toArray(new String[docnoSet.size()]);
			int docnoCounter = docnos.length;
			double[] opinionWeights = new double[docnoCounter];
			for (int i=0; i<docnoCounter; i++)
				opinionWeights[i] = docnoOpinionWeightMap.get(docnos[i]);
			//ScoreNormaliser.zScoreNormalise(opinionWeights);
			ScoreNormaliser.mapToProbabilities(opinionWeights);
			Idf idf = new Idf();
			for (int i=0; i<docnoCounter; i++){
				opinionWeights[i] = -k/idf.log(opinionWeights[i]);
				docnoOpinionWeightMap.put(docnos[i], opinionWeights[i]);
			}
			System.out.print("Combining document weights...");
			
			BufferedReader br = new BufferedReader(new FileReader(new File(resultFilename)));
			String outputFilename = resultFilename.substring(0, resultFilename.lastIndexOf('.'))+
					"_opinionated_prior"+k+".res";
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputFilename)));
			// loop
			String previousQueryid = "";
			int resultSize = 1000;
			docnos = new String[resultSize];
			int[] docnosIndex = new int[resultSize];
			double[] scores = new double[resultSize];
			short[] dummy = new short[resultSize];
			Arrays.fill(dummy, (short)0);
			int counter = 0;
			String runid = null;
			String Q = null;
			String str = null;
			boolean firstLine = false;
			int modifiedCounter = 0;
			while ((str=br.readLine())!=null){
				str = str.trim();
				if (str.length()==0) continue;
				// load one line
				StringTokenizer stk = new StringTokenizer(str);
				String queryid = stk.nextToken();
				Q = stk.nextToken(); //skip query Q0
				String docno = stk.nextToken();
				String rank = stk.nextToken(); //skip rank
				double score = Double.parseDouble(stk.nextToken());
				runid = stk.nextToken();
				// normalise relevance score
				if (!queryid.equals(previousQueryid)){
					if (!firstLine){
						// normalise scores
						if (counter < scores.length){
							double[] scoresTmp = new double[counter];
							int[] docnosIndexTmp = new int[counter];
							for (int i=0; i<counter; i++){
								scoresTmp[i] = scores[i];
								docnosIndexTmp[i] = docnosIndex[i];
							}
							dummy = new short[counter];
							Arrays.fill(dummy, (short)1);
							scores = scoresTmp; docnosIndex = docnosIndexTmp;
						}
						//ScoreNormaliser.zScoreNormalise(scores);
						// combine scores
						for (int i=0; i<counter; i++){
							double opinionWeight = docnoOpinionWeightMap.get(docnos[docnosIndex[i]]);
							if (opinionWeight>0d){
								modifiedCounter++;
								scores[i] = scores[i] + opinionWeight;
							}
						}
						HeapSort.descendingHeapSort(scores, docnosIndex, dummy, counter);
						for (int i=0; i<counter; i++)
							bw.write(previousQueryid+" "+Q+" "+docnos[docnosIndex[i]]+" "+i+" "+
									scores[i]+" "+runid+ApplicationSetup.EOL);
					}else{
						firstLine = false;
					}
					docnos = new String[resultSize];
					docnosIndex = new int[resultSize];
					for (int i=0; i<resultSize; i++)
						docnosIndex[i] = i;
					scores = new double[resultSize];
					dummy = new short[resultSize];
					Arrays.fill(dummy, (short)0);
					counter = 0;
					//normaliser = score;
					previousQueryid = queryid+"";
				}
				//score /= normaliser;
				// interpolate
				double opinionWeight = docnoOpinionWeightMap.get(docno);
				scores[counter] = score;
				docnos[counter] = docno;
				counter++;
			}
			//	normalise scores
			if (counter < scores.length){
				double[] scoresTmp = new double[counter];
				int[] docnosIndexTmp = new int[counter];
				for (int i=0; i<counter; i++){
					scoresTmp[i] = scores[i];
					docnosIndexTmp[i] = docnosIndex[i];
				}
				dummy = new short[counter];
				Arrays.fill(dummy, (short)1);
				scores = scoresTmp; docnosIndex = docnosIndexTmp;
			}
			//ScoreNormaliser.zScoreNormalise(scores);
			// combine scores
			//for (int i=0; i<counter; i++)
				//scores[i] = scores[i]+docnoOpinionWeightMap.get(docnos[docnosIndex[i]]);
			for (int i=0; i<counter; i++){
				double opinionWeight = docnoOpinionWeightMap.get(docnos[docnosIndex[i]]);
				if (opinionWeight>0d){
					modifiedCounter++;
					scores[i] = scores[i] + opinionWeight;
				}
			}
			HeapSort.descendingHeapSort(scores, docnosIndex, dummy, counter);
			for (int i=0; i<counter; i++)
				bw.write(previousQueryid+" "+Q+" "+docnos[docnosIndex[i]]+" "+i+" "+
						scores[i]+" "+runid+ApplicationSetup.EOL);
			// loop ends
			br.close();
			bw.close();
			System.out.println("Done! Results saved in file "+outputFilename);
			System.out.println("Modified scores of "+modifiedCounter+" returned documents.");
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	private static void loadOpinionWeights(
			DocumentIndex docIndex,
			String opinionWeightIndexFilename,
			TIntHashSet docidSet,
			TIntDoubleHashMap docidOpinionWeightMap,
			TObjectIntHashMap docidCache
			){
		try{
			//	load opinion weights
			System.out.print("Loading opinion weight from "+opinionWeightIndexFilename+"...");
			//double normaliser = 0d;
			boolean firstLine = true;
			BufferedReader br = new BufferedReader(new FileReader(new File(opinionWeightIndexFilename)));
			String str = null;
			while ((str=br.readLine())!=null){
				str = str.trim();
				if (str.length()==0) continue;
				StringTokenizer stk = new StringTokenizer(str);
				stk.nextToken(); //skip query id
				stk.nextToken(); //skip query Q0
				String docno = stk.nextToken();
				stk.nextToken(); //skip rank
				double score = Double.parseDouble(stk.nextToken());
				if (firstLine){
					//normaliser = score;
					firstLine = false;
				}
				// normalise opinion weight
				//score /= normaliser;
				int docid = -1;
				if (docidCache.contains(docno))
					docid = docidCache.get(docno);
				else{
					docid = docIndex.getDocumentId(docno);
					docidCache.put(docno, docid);
				}
				docidSet.add(docid);
				docidOpinionWeightMap.put(docid, score);
			}
			br.close();
			System.out.println("Done! "+docidSet.size()+" doc weights are loaded.");
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	private static void loadOpinionFinderWeights(
			String opinionWeightIndexFilename,
			THashSet docnoSet,
			TObjectDoubleHashMap docnoOpinionWeightMap
			){
		try{
			//	load opinion weights
			System.out.print("Loading opinion weights from "+opinionWeightIndexFilename+"...");
			//double normaliser = 0d;
			boolean firstLine = true;
			BufferedReader br = new BufferedReader(new FileReader(new File(opinionWeightIndexFilename)));
			String str = null;
			while ((str=br.readLine())!=null){
				str = str.trim();
				if (str.length()==0) continue;
				StringTokenizer stk = new StringTokenizer(str);
				String docno = stk.nextToken();
				int subSent = Integer.parseInt(stk.nextToken());// skip #subSent
				stk.nextToken();// skip #objSent
				stk.nextToken();// skip #unknownSent
				double ratio = Double.parseDouble(stk.nextToken());// skip #subSent/#objSent
				double percentage = Double.parseDouble(stk.nextToken());// skip %subSent
				double sumDiff1 = Double.parseDouble(stk.nextToken());
				double sumDiff2 = Double.parseDouble(stk.nextToken());
				if (firstLine){
					//normaliser = score;
					firstLine = false;
				}
				// normalise opinion weight
				//score /= normaliser;
				//System.out.println("ratio: "+ratio);
				//if (Double.isInfinite(ratio))
					//System.out.println("ratio: "+ratio);
				if (ratio>0.0d && !Double.isInfinite(ratio)){
					docnoSet.add(docno);
					docnoOpinionWeightMap.put(docno, ratio);
					//System.out.println("sum: "+(sumDiff1+sumDiff2));
				}
			}
			br.close();
			System.out.println("Done! "+docnoSet.size()+" doc weights are loaded.");
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	private static void loadExtraOpinionFinderWeights(
			String opinionWeightIndexFilename,
			THashSet docnoSet,
			TObjectDoubleHashMap docnoOpinionWeightMap
			){
		try{
			//	load opinion weights
			System.out.print("Loading opinion weights from "+opinionWeightIndexFilename+"...");
			//double normaliser = 0d;
			boolean firstLine = true;
			BufferedReader br = new BufferedReader(new FileReader(new File(opinionWeightIndexFilename)));
			String str = null;
			while ((str=br.readLine())!=null){
				str = str.trim();
				if (str.length()==0) continue;
				StringTokenizer stk = new StringTokenizer(str);
				String docno = stk.nextToken();
				stk.nextToken();// skip #subSent
				stk.nextToken();// skip #objSent
				stk.nextToken();// skip #unknownSent
				stk.nextToken();// skip 
				stk.nextToken();// skip #subSent/#objSent
				double percentage = Double.parseDouble(stk.nextToken());// skip %subSent
				double sumDiff1 = Double.parseDouble(stk.nextToken());
				double sumDiff2 = Double.parseDouble(stk.nextToken());
				if (firstLine){
					//normaliser = score;
					firstLine = false;
				}
				// normalise opinion weight
				//score /= normaliser;
				docnoSet.add(docno);
				docnoOpinionWeightMap.put(docno, (sumDiff1+sumDiff2));
			}
			br.close();
			System.out.println("Done! "+docnoSet.size()+" doc weights are loaded.");
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	private static void loadOpinionWeights(
			String opinionWeightIndexFilename,
			THashSet docnoSet,
			TObjectDoubleHashMap docnoOpinionWeightMap
			){
		try{
			//	load opinion weights
			System.out.print("Loading opinion weight from "+opinionWeightIndexFilename+"...");
			//double normaliser = 0d;
			boolean firstLine = true;
			BufferedReader br = new BufferedReader(new FileReader(new File(opinionWeightIndexFilename)));
			String str = null;
			while ((str=br.readLine())!=null){
				str = str.trim();
				if (str.length()==0) continue;
				StringTokenizer stk = new StringTokenizer(str);
				stk.nextToken(); //skip query id
				stk.nextToken(); //skip query Q0
				String docno = stk.nextToken();
				stk.nextToken(); //skip rank
				double score = Double.parseDouble(stk.nextToken());
				if (firstLine){
					//normaliser = score;
					firstLine = false;
				}
				// normalise opinion weight
				//score /= normaliser;
				docnoSet.add(docno);
				docnoOpinionWeightMap.put(docno, score);
			}
			br.close();
			System.out.println("Done! "+docnoSet.size()+" doc weights are loaded.");
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	private static void normaliseScores(TIntHashSet docidSet, TIntDoubleHashMap scoreMap){
		double[] scores = new double[docidSet.size()];
		int[] docids = docidSet.toArray();
		int N = docids.length;
		for (int i=0; i<N; i++)
			scores[i] = scoreMap.get(docids[i]);
		ScoreNormaliser.zScoreNormalise(scores);
		/*ScoreNormaliser.mapToProbabilities(scores);
		Idf idf = new Idf();
		double k=400d;
		for (int i=0; i<N; i++)
	        	scores[i] = -k/idf.log(scores[i]);*/
		for (int i=0; i<N; i++)
			scoreMap.put(docids[i], scores[i]);
	}
	
	public static void polarifyResults(
			String resultFilename,
			String negativeFilename,
			String mixedFilename,
			String positiveFilename,
			String polarityFilename
			){
		final int NEGATIVE = 2;
		final int MIXED = 3;
		final int POSITIVE = 4;
		// initiate index
		Index index = Index.createIndex();
		DocumentIndex docIndex = index.getDocumentIndex();
		// load three idx files
		TIntHashSet docidSet = new TIntHashSet();
		TIntDoubleHashMap positiveMap = new TIntDoubleHashMap();
		TIntDoubleHashMap negativeMap = new TIntDoubleHashMap();
		TIntDoubleHashMap mixedMap = new TIntDoubleHashMap();
		TObjectIntHashMap docidCache = new TObjectIntHashMap();
		loadOpinionWeights(docIndex, negativeFilename, docidSet, negativeMap, docidCache);
		normaliseScores(docidSet, negativeMap);
		loadOpinionWeights(docIndex, mixedFilename, docidSet, mixedMap, docidCache);
		normaliseScores(docidSet, mixedMap);
		loadOpinionWeights(docIndex, positiveFilename, docidSet, positiveMap, docidCache);
		normaliseScores(docidSet, positiveMap);
		docidCache.clear();
		docidCache = null;
		// normalise scores?
		// for each query
		TRECResultsInMemory results = new TRECResultsInMemory(resultFilename);
		String[] queryids = results.getQueryids();
		try{
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(polarityFilename)));
			for (int i=0; i<queryids.length; i++){
				// for each returned document
				String[] docnos = results.getRetrievedDocnos(queryids[i]);
				int docnoCount = docnos.length;
				for (int j=0; j<docnoCount; j++){
					// decide the polarity
					int docid = docIndex.getDocumentId(docnos[j]);
					double negativeW = negativeMap.get(docid);
					double mixedW = mixedMap.get(docid);
					double positiveW = positiveMap.get(docid);
					int polarity = (negativeW>positiveW)?
							((negativeW>mixedW)?(NEGATIVE):(MIXED))
							:((positiveW>mixedW)?(POSITIVE):(MIXED));
					// write to output file
					bw.write(queryids[i]+" "+docnos[j]+" "+polarity+ApplicationSetup.EOL);
				}
			// end for
			}
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		index.close();
		System.out.println("Done. Results saved in file "+polarityFilename);
	}
	
	public static void evaluatePolarityResults(String qrelsFilename, String resultFilename){
		final int NEGATIVE = 2;
		final int MIXED = 3;
		final int POSITIVE = 4;
		int negativeHit = 0;
		int mixedHit = 0;
		int positiveHit = 0;
		int negativeRetrieved = 0;
		int mixedRetrieved = 0;
		int positiveRetrieved = 0;
		TRECQrelsInMemory qrels = new TRECQrelsInMemory(qrelsFilename);
		THashSet relDocnos = new THashSet();
		THashSet negDocnos = new THashSet();
		THashSet mixDocnos = new THashSet();
		THashSet posDocnos = new THashSet();
		try{
			BufferedReader br = new BufferedReader(new FileReader(new File(resultFilename)));
			String str = null;
			THashSet queryidSet = new THashSet();
			while ((str=br.readLine())!=null){
				str = str.trim();
				if (str.length()==0)
					continue;
			// for each line
				StringTokenizer stk = new StringTokenizer(str);
				String queryid = stk.nextToken();
				String docno = stk.nextToken();
				int polarity = Integer.parseInt(stk.nextToken());
				if (!queryidSet.contains(queryid)){
					queryidSet.add(queryid);
					relDocnos = qrels.getAllRelevantDocuments();
					negDocnos = qrels.getRelevantDocuments(NEGATIVE);
					mixDocnos = qrels.getRelevantDocuments(MIXED);
					posDocnos = qrels.getRelevantDocuments(POSITIVE);
				}
				boolean hit = false;
				// check if polarity is correct
				if (relDocnos.contains(docno)){
					if (polarity==NEGATIVE){
						negativeRetrieved++;
						hit = (negDocnos.contains(docno));
						if (hit){
							negativeHit++;
						}
					}
					else if (polarity==MIXED){
						mixedRetrieved++;
						hit = (mixDocnos.contains(docno));
						if (hit){
							mixedHit++;
						}
					}
					else if (polarity==POSITIVE){
						positiveRetrieved++;
						hit = (posDocnos.contains(docno));
						if (hit){
							positiveHit++;
						}
					}
				}
			// end for
			// print results
				
			}
			System.out.println("negativeHit: "+negativeHit+", negativeRetrieved: "+negativeRetrieved+", "+(double)100d*negativeHit/negativeRetrieved);
			System.out.println("mixedHit: "+mixedHit+", mixedRetrieved: "+mixedRetrieved+", "+(double)100d*mixedHit/mixedRetrieved);
			System.out.println("positiveHit: "+positiveHit+", positiveRetrieved: "+positiveRetrieved+", "+(double)100d*positiveHit/positiveRetrieved);
			System.out.println("Hit: "+(negativeHit+mixedHit+positiveHit)+
					", retrieved: "+(negativeRetrieved+mixedRetrieved+positiveRetrieved)+
					", "+(double)100d*(negativeHit+mixedHit+positiveHit)/(negativeRetrieved+mixedRetrieved+positiveRetrieved));
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void randomQueryQrelsSelection(
			String topicFilename,
			String qrelsFilename,
			int numberOfSelectedQueries
			){
		TRECQuery queries = new TRECQuery(topicFilename);
		TRECQrelsInMemory qrels = new TRECQrelsInMemory(qrelsFilename);
		String topicOutputFilename = topicFilename+"."+numberOfSelectedQueries;
		String qrelsOutputFilename = qrelsFilename+"."+numberOfSelectedQueries;
		String[] queryids = queries.getQueryids();
		int numberOfQueries = queries.getNumberOfQueries();
		TIntHashSet selectedPos = new TIntHashSet();
		selectedPos.add(-1);
		StringBuffer topicBuffer = new StringBuffer();
		StringBuffer qrelsBuffer = new StringBuffer();
		for (int i=0; i<numberOfSelectedQueries; i++){
			int pos = -1;
			while (selectedPos.contains(pos)){
				pos = (int)(Math.random()*(numberOfQueries-1));
			}
			selectedPos.add(pos);			
		}
		selectedPos.remove(-1);
		int[] pos = selectedPos.toArray();
		Arrays.sort(pos);
		for (int i=0; i<numberOfSelectedQueries; i++){
			topicBuffer.append(queryids[pos[i]]+" "+queries.getQuery(queryids[pos[i]])+
					ApplicationSetup.EOL);
			String[] nonRelDocnos = qrels.getNonRelevantDocumentsToArray(queryids[pos[i]]);
			for (int j=0; j<nonRelDocnos.length; j++)
				qrelsBuffer.append(queryids[pos[i]]+" 0 "+nonRelDocnos[j]+" 0"+ApplicationSetup.EOL);
			for (int t=1; t<=4; t++){
				String[] relDocnos = qrels.getRelevantDocumentsToArray(queryids[pos[i]], t);
				if (relDocnos!=null)
					for (int j=0; j<relDocnos.length; j++)
						qrelsBuffer.append(queryids[pos[i]]+" 0 "+relDocnos[j]+" "+t+ApplicationSetup.EOL);
			}
		}
		try{
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(topicOutputFilename);
			bw.write(topicBuffer.toString());
			bw.close();
			bw = (BufferedWriter)Files.writeFileWriter(qrelsOutputFilename);
			bw.write(qrelsBuffer.toString());
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void randomQuerySelection(
			String topicFilename,
			int numberOfSelectedQueries,
			String outputFilename
			){
		TRECQuery queries = new TRECQuery(topicFilename);
		String[] queryids = queries.getQueryids();
		int numberOfQueries = queries.getNumberOfQueries();
		TIntHashSet selectedPos = new TIntHashSet();
		selectedPos.add(-1);
		StringBuffer strBuffer = new StringBuffer();
		for (int i=0; i<numberOfSelectedQueries; i++){
			int pos = -1;
			while (selectedPos.contains(pos)){
				pos = (int)(Math.random()*(numberOfQueries-1));
			}
			selectedPos.add(pos);			
		}
		selectedPos.remove(-1);
		int[] pos = selectedPos.toArray();
		Arrays.sort(pos);
		for (int i=0; i<numberOfSelectedQueries; i++){
			strBuffer.append(queryids[pos[i]]+" "+queries.getQuery(queryids[pos[i]])+
					ApplicationSetup.EOL);
		}
		try{
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			bw.write(strBuffer.toString());
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void combineOpinionWeight(
			String resultFilename, 
			String opinionWeightIndexFilename,
			double alpha){
		try{
			THashSet docnoSet = new THashSet();
			TObjectDoubleHashMap docnoOpinionWeightMap = new TObjectDoubleHashMap();
			if (opinionWeightIndexFilename.endsWith(".opf"))
				loadOpinionFinderWeights(opinionWeightIndexFilename, docnoSet, docnoOpinionWeightMap);
			else
				loadOpinionWeights(opinionWeightIndexFilename, docnoSet, docnoOpinionWeightMap);
			// normalise opinion weights
			String[] docnos = (String[])docnoSet.toArray(new String[docnoSet.size()]);
			int docnoCounter = docnos.length;
			double[] opinionWeights = new double[docnoCounter];
			for (int i=0; i<docnoCounter; i++)
				opinionWeights[i] = docnoOpinionWeightMap.get(docnos[i]);
			ScoreNormaliser.zScoreNormalise(opinionWeights);
			for (int i=0; i<docnoCounter; i++)
				docnoOpinionWeightMap.put(docnos[i], opinionWeights[i]);
			System.out.print("Combining document weights...");
			
			BufferedReader br = new BufferedReader(new FileReader(new File(resultFilename)));
			String outputFilename = resultFilename.substring(0, resultFilename.lastIndexOf('.'))+
					"_opinionated"+alpha+".res";
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputFilename)));
			// loop
			String previousQueryid = "";
			int resultSize = 1000;
			docnos = new String[resultSize];
			int[] docnosIndex = new int[resultSize];
			double[] scores = new double[resultSize];
			short[] dummy = new short[resultSize];
			Arrays.fill(dummy, (short)0);
			int counter = 0;
			String runid = null;
			String Q = null;
			String str = null;
			boolean firstLine = false;
			while ((str=br.readLine())!=null){
				str = str.trim();
				if (str.length()==0) continue;
				// load one line
				StringTokenizer stk = new StringTokenizer(str);
				String queryid = stk.nextToken();
				Q = stk.nextToken(); //skip query Q0
				String docno = stk.nextToken();
				String rank = stk.nextToken(); //skip rank
				double score = Double.parseDouble(stk.nextToken());
				runid = stk.nextToken();
				// normalise relevance score
				if (!queryid.equals(previousQueryid)){
					if (!firstLine){
						// normalise scores
						if (counter < scores.length){
							double[] scoresTmp = new double[counter];
							int[] docnosIndexTmp = new int[counter];
							for (int i=0; i<counter; i++){
								scoresTmp[i] = scores[i];
								docnosIndexTmp[i] = docnosIndex[i];
							}
							dummy = new short[counter];
							Arrays.fill(dummy, (short)1);
							scores = scoresTmp; docnosIndex = docnosIndexTmp;
						}
						ScoreNormaliser.zScoreNormalise(scores);
						// combine scores
						for (int i=0; i<counter; i++)
							scores[i] = 10d+alpha*scores[i]+(1-alpha)*docnoOpinionWeightMap.get(docnos[docnosIndex[i]]);
						HeapSort.descendingHeapSort(scores, docnosIndex, dummy, counter);
						for (int i=0; i<counter; i++)
							bw.write(previousQueryid+" "+Q+" "+docnos[docnosIndex[i]]+" "+i+" "+
									scores[i]+" "+runid+ApplicationSetup.EOL);
					}else{
						firstLine = false;
					}
					docnos = new String[resultSize];
					docnosIndex = new int[resultSize];
					for (int i=0; i<resultSize; i++)
						docnosIndex[i] = i;
					scores = new double[resultSize];
					dummy = new short[resultSize];
					Arrays.fill(dummy, (short)0);
					counter = 0;
					//normaliser = score;
					previousQueryid = queryid+"";
				}
				//score /= normaliser;
				// interpolate
				double opinionWeight = docnoOpinionWeightMap.get(docno);
				scores[counter] = score;
				docnos[counter] = docno;
				counter++;
			}
			//	normalise scores
			if (counter < scores.length){
				double[] scoresTmp = new double[counter];
				int[] docnosIndexTmp = new int[counter];
				for (int i=0; i<counter; i++){
					scoresTmp[i] = scores[i];
					docnosIndexTmp[i] = docnosIndex[i];
				}
				dummy = new short[counter];
				Arrays.fill(dummy, (short)1);
				scores = scoresTmp; docnosIndex = docnosIndexTmp;
			}
			ScoreNormaliser.zScoreNormalise(scores);
			// combine scores
			for (int i=0; i<counter; i++)
				scores[i] = 10d+alpha*scores[i]+(1-alpha)*docnoOpinionWeightMap.get(docnos[docnosIndex[i]]);

			HeapSort.descendingHeapSort(scores, docnosIndex, dummy, counter);
			for (int i=0; i<counter; i++)
				bw.write(previousQueryid+" "+Q+" "+docnos[docnosIndex[i]]+" "+i+" "+
						scores[i]+" "+runid+ApplicationSetup.EOL);
			// loop ends
			br.close();
			bw.close();
			System.out.println("Done! Results saved in file "+outputFilename);
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void parseTRECTable(String filename){
		double[] best = new double[50];
		double[] median = new double[50];
		double[] worst = new double[50];
		try{
			 BufferedReader br = Files.openFileReader(filename);
			 String str = null;
			 int counter = 0;
			 while ((str=br.readLine())!=null){
				 StringTokenizer stk = new StringTokenizer(str);
				 stk.nextToken(); stk.nextToken();
				 best[counter] = Double.parseDouble(stk.nextToken());
				 median[counter] = Double.parseDouble(stk.nextToken());
				 worst[counter] = Double.parseDouble(stk.nextToken());
				 counter++;
			 }
			 br.close();
		 }catch(IOException e){
			 e.printStackTrace();
			 System.exit(1);
		 }
		 System.err.println("mean best: "+Statistics.mean(best));
		 System.err.println("mean median: "+Statistics.mean(median));
		 System.err.println("mean worst: "+Statistics.mean(worst));
	}

	public static void main(String[] args) {
		if (args[1].equals("--avgrel"))
			TestEvaluation.computeRelDocLength();
		else if (args[1].equals("--computedivergence"))
			// -e --computedivergence <qemodelName> <workdlistfilename>
			// -e --computedivergence Bo1 /users/tr.ben/Blog06/wordlist
			TestEvaluation.computeProbTermQrels(args[2], args[3]);
		else if (args[1].equals("--buildqrelslex")){
			// -e -buildqrelslex <grades ...>
			// -e -buildqrelslex 0 # for all relevant documents
			// -e -buildqrelslex 2 3 # all relevant documents with relevance degrees 2 and 3
			int[] grades = new int[args.length-2];
			for (int i=0; i<grades.length; i++)
				grades[i] = Integer.parseInt(args[i+2]);
			TestEvaluation.buildQrelsLexicon(grades);
		}
		else if (args[1].equals("--selectqueries")){
			// -e --selectqueries topicfilename outputfilename #selectedqueries
			TestEvaluation.randomQuerySelection(args[2], Integer.parseInt(args[4]), args[3]);
		}
		else if (args[1].equals("--selectquerywithqrels")){
			// -e --selectquerywithqrels topicfilename qrelsfilename #selectedqueries
			TestEvaluation.randomQueryQrelsSelection(args[2], args[3], Integer.parseInt(args[4]));
		}
		else if (args[1].equals("--computeopiniontermweightsgraded")){
			// -e --computeopiniontermweightsgraded <qemodelName> <wordlistfilename> <bglexiconfilename> <grades...>
			// -e --computeopiniontermweightsgraded Bo1 /users/tr.ben/Blog06/wordlist /users/tr.ben/Blog06/qrels0.lex 1 3
			int[] grades = new int[args.length-5];
			for (int i=0; i<grades.length; i++)
				grades[i] = Integer.parseInt(args[i+5]);
			TestEvaluation.computeDivTermQrels(args[2], args[3], args[4], grades);
		}
		else if (args[1].equals("--writeonelinequery"))
			// -e --writeonelinequery <filename>
			TestEvaluation.writeOneLineQuery(args[2]);
		else if (args[1].equals("--writetoponelinequery"))
			// -e --writetoponelinequery <filename> <topX>
			TestEvaluation.writeTopOneLineQuery(args[2], Integer.parseInt(args[3]));
		else if (args[1].equals("--writeclickeddocuments"))
			// -e --writeclickeddocuments <grade> <numberOfDocsPerQuery>
			// -e --writeclickeddocuments 2 3
			TestEvaluation.writeClickedDocuments(Integer.parseInt(args[2]),
					Integer.parseInt(args[3]));
		else if (args[1].equals("--writelongestclickeddocuments"))
			// -e --writeclickeddocuments <grade> <numberOfDocsPerQuery>
			// -e --writeclickeddocuments 2 3
			TestEvaluation.writeLongestClickedDocuments(Integer.parseInt(args[2]),
					Integer.parseInt(args[3]));
		else if(args[1].equals("--combineopinionweights")){
			// -e --combineopinionweights <resultFilename> <opinionWeightIndexFilename> <alpha>
			// -e --combineopinionweights 
			TestEvaluation.combineOpinionWeight(args[2], args[3], Double.parseDouble(args[4]));
		}
		else if(args[1].equals("--combineopinionpriors")){
			// -e --combineopinionpriors <resultFilename> <opinionWeightIndexFilename> <k>
			TestEvaluation.combineOpinionPriors(args[2], args[3], Double.parseDouble(args[4]));
		}
		else if(args[1].equals("--computeaveragefeedbackdocumentranking")){
			// -e --computeaveragefeedbackdocumentranking <feedbackfilename> <resultfilename>
			TestEvaluation.computeAverageRankingOfFeedbackDocuments(args[2], args[3]);
		}
		else if(args[1].equals("--polarify")){
			// -e --polarify <resultFilename> <negativefilename> <mixedfilename> <positivefilename> <outputfilename>
			TestEvaluation.polarifyResults(args[2], args[3], args[4], args[5], args[6]);
		}
		else if(args[1].equals("--evaluatepolarification")){
			// -e --evaluatepolarification <qrelsFilename> <resultFilename>
			TestEvaluation.evaluatePolarityResults(args[2], args[3]);
		}else if (args[1].equals("--parsetrectable")){
			// -e --parsetrectable <filename>
			TestEvaluation.parseTRECTable(args[2]);
		}
		else if (args[1].equals("-q")){
			AdhocEvaluation eval = new AdhocEvaluation();
			/** list all the result files and then evaluate them */
			File fresdirectory = new File(ApplicationSetup.TREC_RESULTS);
			String[] nomefile = fresdirectory.list();
			for (int i = 0; i < nomefile.length; i++) {
				if (nomefile[i].endsWith(".res")) {
	
					String resultFilename = ApplicationSetup.TREC_RESULTS+ "/" + nomefile[i];
					String evaluationResultFilename =
						resultFilename.substring(
							0,
							resultFilename.lastIndexOf('.'))
							+ ".eval";
	
					eval.evaluate(resultFilename);
					eval.writeEvaluationResult(evaluationResultFilename);
					evaluationResultFilename =
						resultFilename.substring(
							0,
							resultFilename.lastIndexOf('.'))
							+ ".apq"; 
					eval.writeEvaluationResultOfEachQuery(evaluationResultFilename);
				}
			}
		}
	}
}

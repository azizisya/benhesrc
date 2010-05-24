/*
 * Created on 2005-1-1
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package tests;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TDoubleDoubleHashMap;
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
import java.io.IOException;
import java.util.Arrays;
import java.util.StringTokenizer;

import org.terrier.matching.models.Idf;
import org.terrier.matching.models.WeightingModel;
import org.terrier.matching.models.queryexpansion.QueryExpansionModel;
import org.terrier.querying.QueryExpansion;
import org.terrier.simulation.TFRanking;
import org.terrier.structures.CollectionStatistics;
import org.terrier.structures.DirectIndex;
import org.terrier.structures.DocumentIndex;
import org.terrier.structures.DocumentIndexInputStream;
import org.terrier.structures.ExpansionTerms;
import org.terrier.structures.Index;
import org.terrier.structures.Lexicon;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.LexiconInputStream;
import org.terrier.structures.LexiconOutputStream;
import org.terrier.structures.SingleLineTRECQuery;
import org.terrier.structures.TRECQuery;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.Files;
import org.terrier.utility.HeapSort;
import org.terrier.utility.Rounding;

import uk.ac.gla.terrier.evaluation.TRECQrelsInMemory;
import uk.ac.gla.terrier.evaluation.TRECResultsInMemory;
import uk.ac.gla.terrier.matching.dsms.DistanceModifier;
import uk.ac.gla.terrier.statistics.ScoreNormaliser;
import uk.ac.gla.terrier.statistics.Statistics;
import uk.ac.gla.terrier.terms.PorterStemmer;

/**
 * @author ben
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class TestBlogOpinion {
	protected TIntDoubleHashMap docidOpinionWeightMap;
	
	protected boolean USE_SKEWED_MODEL = Boolean.parseBoolean(
			ApplicationSetup.getProperty("use.skewed.model", "false"));
	protected TFRanking ranking;
	
	static protected final Idf idf = new Idf();
	
	protected TObjectIntHashMap<String> docHashMap;
	
	public TestBlogOpinion(String opinionWeightIndexFilename){
		this.createDocHashMap();
		//System.out.println("opinion weight index file: "+opinionWeightIndexFilename);
		docidOpinionWeightMap = new TIntDoubleHashMap();
		if (opinionWeightIndexFilename.endsWith(".opf")||opinionWeightIndexFilename.endsWith(".opf.gz"))
			loadOpinionFinderWeights(opinionWeightIndexFilename);
		else if (opinionWeightIndexFilename.endsWith(".ext")||opinionWeightIndexFilename.endsWith(".ext.gz"))
			loadExtraOpinionFinderWeights(opinionWeightIndexFilename);
		else if (opinionWeightIndexFilename.endsWith(".idx")||opinionWeightIndexFilename.endsWith(".idx.gz"))
			loadOpinionWeights(opinionWeightIndexFilename);
	}
	
	public TestBlogOpinion(){}
	
	private void createDocHashMap(){
		docHashMap = new TObjectIntHashMap<String>();
		try{
			DocumentIndexInputStream docin = new DocumentIndexInputStream();
			while (docin.readNextEntry()!=-1){
				docHashMap.put(docin.getDocumentNumber(), docin.getDocumentId());
			}
			docin.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
	}
	
	static public void removeNonRelevantDocuments(String resultFilename, String qrelsFilename){
		try{
			TRECResultsInMemory results = new TRECResultsInMemory(resultFilename);
			TRECQrelsInMemory qrels = new TRECQrelsInMemory(qrelsFilename);
			
			String outFilename = (resultFilename.endsWith(".gz"))?
					(resultFilename.substring(0, resultFilename.length()-7)+".rel.res.gz"):
						(resultFilename.substring(0, resultFilename.length()-4)+".rel.res");
			File outFile = new File(outFilename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outFile);
			String[] queryids = results.getQueryids();
			String EOL = ApplicationSetup.EOL;
			Arrays.sort(queryids);
			for (int i=0; i<queryids.length; i++){
				String[] retrievedDocnos = results.getRetrievedDocnos(queryids[i]);
				int n = retrievedDocnos.length;
				for (int j=0; j<n; j++){
					if (qrels.isRelevant(queryids[i], retrievedDocnos[j]))
						bw.write(queryids[i]+" Q0 "+
								results.getRank(queryids[i], retrievedDocnos[j])+" "+
								results.getScore(queryids[i], retrievedDocnos[j])+" tag"+EOL
								);
				}
			}
			
			bw.close();
			
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public void computeRelDocLength(){
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
	
	public void loadRelevanceInformation(String filename, THashMap<String, String[]> queryidRelDocnoMap){
		try{
			BufferedReader br = Files.openFileReader(filename);
			THashSet<String> queryids = new THashSet<String>();
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
		}
	}
	
	static public void loadXidEvaluationResults(
			String dirname, String prefix, int minid, int maxid, String rest,
			String model, String qemodel, String dict, String combtype, 
			int minX, int maxX, int intX 
			){
		//int nX = (maxX-minX)/intX +1;
		int nid = maxid-minid+1;
		for (int X=minX; X<=maxX; X+=intX){
			double[] baselines = new double[nid];
			double[] evalMaxs = new double[nid];
			int counter = 0;
			for (int id=minid; id<=maxid; id++){
				double[] values = loadXidEvaluationResults(
						dirname, prefix, ""+id, rest, model, qemodel, dict, combtype, ""+X);
				evalMaxs[counter] = values[1];
				baselines[counter] = values[0];
				counter++;
			}
			System.out.println(X+" "+Rounding.toString(Statistics.mean(evalMaxs), 4)+" "+
					Rounding.toString(Statistics.mean(baselines), 4));
			evalMaxs = null;
		}
	}
	static public void loadTestResult(String filename){
		try{
			BufferedReader br = Files.openFileReader(filename);
			String str = null;
			TDoubleDoubleHashMap evalMap = new TDoubleDoubleHashMap();
			double k = 0; 
			while ((str=br.readLine())!=null){
				if (str.endsWith(".res")){
					k = getCombinationParameter(str);
				}else{
					String[] tokens = str.replaceAll("\t", " ").split(" ");
					evalMap.put(k, Double.parseDouble(tokens[tokens.length-1]));
				}
			}
			br.close();
			double[] keys = evalMap.keys();
			Arrays.sort(keys);
			for (int i=0; i<keys.length; i++)
				System.out.println(keys[i]+" "+evalMap.get(keys[i]));
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	static public void renameToMatlab(String dirname){
		try{
			File dir = new File(dirname);
			String[] files = dir.list();
			for (int i=0; i<files.length; i++){
				String str = files[i].replaceAll("\\.", "_");
				File file = new File(dirname, files[i]);
				if (!file.renameTo(new File(dirname, str)))
					System.err.println("Renaming unsuccessful.");
			}
		}catch(Exception ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	// returns [0]: baseline [1]: evalMax
	static private double[] loadXidEvaluationResults(
			String dirname, String prefix, String id, String rest,
			String model, String qemodel, String dict, String combtype, String X 
			){
		String sep = ApplicationSetup.FILE_SEPARATOR;
		String suffix = "opinion_eval";
		String filename = dirname+sep+prefix+"."+id+"."+rest+"."+model+".res.gz."+
				qemodel+"."+dict+"."+combtype+".top"+X+"."+id+"."+suffix;
		//System.out.println(filename);
		TDoubleDoubleHashMap XidEvalMap = new TDoubleDoubleHashMap();  
		try{
			BufferedReader br = Files.openFileReader(filename);
			String str = null;
			double k = 0;
			while ((str=br.readLine())!=null){
				if (str.endsWith(".res"))
					k = getCombinationParameter(str);
				else if (str.endsWith(".gz"))
					k = -1;// baseline
				else {
					String[] strs = str.replaceAll("\t", " ").split(" ");
					double eval = Double.parseDouble(strs[strs.length-1].trim());
					XidEvalMap.put(k, eval);
				}
			}
			br.close();
		}catch(Exception ioe){
			System.err.println(filename);
			ioe.printStackTrace();
			System.exit(1);
		}
		double[] keys = XidEvalMap.keys();
		Arrays.sort(keys);
		double maxEval = 0d;
		double baseline = 0d;
		for (int i=0; i<keys.length; i++){
			if (keys[i]<0)
				baseline = XidEvalMap.get(keys[i]);
			else
				maxEval = Math.max(maxEval, XidEvalMap.get(keys[i]));
		}
		//System.out.println("max  eval: "+maxEval);
		//System.out.println("mean eval: "+Statistics.mean(XidEvalMap.getValues()));
		double[] values = {baseline, maxEval};
		return values;
	}
	
	public void buildQrelsLexicon(String qrelsFilename, int[] grades){
		// initialise data structure
		// terms in the pool
		TIntHashSet termidSet = new TIntHashSet();
		// hashmaps from terms to their Nt and TF in the pool
		TIntIntHashMap termNtMap = new TIntIntHashMap();
		TIntIntHashMap termTFMap = new TIntIntHashMap();
		
		// Load qrels
		TRECQrelsInMemory qrels = new TRECQrelsInMemory(qrelsFilename);
		
		// get docnos of all relevant documents
		THashSet<String> relDocnoSet = new THashSet<String>();
		String[] relDocnos = null;
		if (grades.length==1){
			if (grades[0]==0)// all relevant
				relDocnos = qrels.getAllRelevantDocumentsToArray();
			else if (grades[0]==-1){// all docs in pool
				THashSet<String> docnoSet = new THashSet<String>();
				String[] docnos = qrels.getAllRelevantDocumentsToArray();
				for (int i=0; i<docnos.length; i++)
					docnoSet.add(docnos[i]);
				docnos = qrels.getNonRelevantDocumentsToArray();
				for (int i=0; i<docnos.length; i++)
					docnoSet.add(docnos[i]);
				relDocnos = (String[])docnoSet.toArray(new String[docnoSet.size()]);
			}
			else // specified grades
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
		TObjectIntHashMap<String> termMap = new TObjectIntHashMap<String>();
		int numberOfTerms = termids.length;
		for (int i=0; i<numberOfTerms; i++){
			lex.findTerm(termids[i]);
			termMap.put(lex.getTerm(), termids[i]);
			terms[i] = lex.getTerm();
		}
		
		String gradeString = ""+grades[0];
		for (int i=1; i<grades.length; i++)
			gradeString=gradeString+"-"+grades[i];
		
		String lexoutFilename = qrelsFilename+gradeString+".lex";
		LexiconOutputStream lexout = new LexiconOutputStream(lexoutFilename);
		Arrays.sort(terms);
		for (int i=0; i<numberOfTerms; i++){
			try{
				lexout.writeNextEntry(terms[i], termMap.get(terms[i]), 
						termNtMap.get(termids[i]), 
						termTFMap.get(termids[i]), 0L, (byte)0);
			}catch(IOException ioe){
				ioe.printStackTrace();
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
		}
		System.out.println("Done! New lexicon file written to "+lexoutFilename);
		index.close();
	}
	
	
	
	public void writeOneLineQuery(String filename){
		try{
			//StringBuffer buffer = new StringBuffer();
			String outputFilename = filename+"_oneline.topics";
			// load terms and weights
			BufferedReader br = Files.openFileReader(filename);
			//if (!new File(outputFilename).exists())
				//new File(outputFilename).createNewFile();
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
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
		}
	}
	
	public void writeTopOneLineQuery(String filename, int topx){
		try{
			//StringBuffer buffer = new StringBuffer();
			String outputFilename = filename+"_topics.top"+topx;
			// load terms and weights
			BufferedReader br = Files.openFileReader(filename);
			//if (!new File(outputFilename).exists())
				//new File(outputFilename).createNewFile();
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
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
	
	public static void stemWordlist(String filename){
		PorterStemmer stemmer = new PorterStemmer(null);
		String outputFilename = filename+".stemmed";
		try{
			BufferedReader br = Files.openFileReader(filename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			String str = null;
			THashSet<String> termSet = new THashSet<String>();
			while ((str=br.readLine())!=null){
				str=str.trim();
				if (str.length()==0) continue;
				str = stemmer.stem(str);
				if (!termSet.contains(str)){
					termSet.add(str);
					bw.write(str+ApplicationSetup.EOL);
				}
			}
						
			br.close();
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void filterWordlist(String indexPath, String indexPrefix, String filename){
		Index index = Index.createIndex(indexPath, indexPrefix);
		Lexicon lexicon = index.getLexicon();
		int N = index.getCollectionStatistics().getNumberOfDocuments();
		PorterStemmer stemmer = new PorterStemmer(null);
		boolean skewed = Boolean.parseBoolean(
				ApplicationSetup.getProperty("use.skewed.model", "false"));
		TFRanking ranking = null;
		if (skewed)
			ranking = new TFRanking(indexPath, indexPrefix);
		try{
			String outputFilename = filename+".filtered";
			BufferedReader br = Files.openFileReader(filename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			
			String str = null;
			while ((str=br.readLine())!=null){
				str=str.trim();
				if (str.length()==0) continue;
				String stem = stemmer.stem(str);
				if (lexicon.findTerm(stem) && N>=lexicon.getTF() &&
						(!skewed||ranking.isValidTerm(lexicon.getTermId())))
					bw.write(str+ApplicationSetup.EOL);
				else
					System.out.println(str+" filtered. N: "+N+", TF: "+lexicon.getTF());
			}
			
			br.close();
			bw.close();
			System.out.println("Filtered wordlist saved at "+outputFilename);
		}catch(IOException e){
			e.printStackTrace();
			System.exit(1);
		}
		index.close();
	}
	
	public void extractWordsFromUnstemmedLexicon(String indexPath, String indexPrefix){
		// load terms in the lexicon, and write to a wordlist file
		Index index = Index.createIndex(indexPath, indexPrefix);
		LexiconInputStream lexInputStream = new LexiconInputStream(indexPath, indexPrefix);
		String outputFilename = indexPath+ApplicationSetup.FILE_SEPARATOR+
				indexPrefix+ApplicationSetup.LEXICONSUFFIX+".dict";
		try{
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			String EOL = ApplicationSetup.EOL;
			while (lexInputStream.readNextEntry()!=-1){
				bw.write(lexInputStream.getTerm()+EOL);
			}
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
		index.close();
		lexInputStream.close();
	}
	
	public void computeDivTermQrels(
			String qemodelName, 
			String wordlist,
			String bgLexiconFilename,
			String qrelsFilename,
			int grades[]
			){
		// initiate QE model
		//QueryExpansion qe = new QueryExpansion();
		WeightingModel qemodel = WeightingModel.getWeightingModel(qemodelName);
		System.out.print("grades: ");
		for (int i=0; i<grades.length; i++)
			System.out.print(grades[i]+" ");
		// Get pseudoLength
		System.out.print("Computing pseudoLength...");
		TRECQrelsInMemory qrels = new TRECQrelsInMemory(qrelsFilename);
		Index index = Index.createIndex();
		DocumentIndex docIndex = index.getDocumentIndex();
		
		Index unstemmedIndex = Index.createIndex(
				ApplicationSetup.getProperty("unstemmed.index.path", "/local/terrier3/Indices/Blogs06/classical/nostemming/"),
				ApplicationSetup.getProperty("unstemmed.index.prefix", "data"));
		Lexicon unstemmedLexicon = unstemmedIndex.getLexicon();
		
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
		
		double pseudoLength = 0d;
		for (int i=0; i<relDocnos.length; i++){
			try{
				pseudoLength += docIndex.getDocumentLength(docIndex.getDocumentId(relDocnos[i]));
			}catch(ArrayIndexOutOfBoundsException e){
				System.err.println("relDocnos[i]: "+relDocnos[i]);
				System.err.println("DOCNO_BYTE_LENGTH: "+docIndex.DOCNO_BYTE_LENGTH);
				e.printStackTrace();
				System.exit(1);
			}
		}
		System.out.println("Done. PseudoLength="+pseudoLength);
		// initiate expansionTerms
		System.out.print("Initialising expansionTerms...");
		int N = index.getCollectionStatistics().getNumberOfDocuments();
		
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
		if (USE_SKEWED_MODEL)
			ranking = new TFRanking(index);
		for (int i=0; i<relDocnos.length; i++){
			int[][] terms = df.getTerms(docIndex.getDocumentId(relDocnos[i]));
			if (terms == null)
				continue;
			else
				for (int j = 0; j < terms[0].length; j++){
					if (USE_SKEWED_MODEL){
						if (ranking.isValidTerm(terms[0][j]))
							expansionTerms.insertTerm(terms[0][j], (double)terms[1][j]);
					}
					else{
						expansionTerms.insertTerm(terms[0][j], (double)terms[1][j]);
					}
				}
		}
		System.out.println("Done.");
		// assign weights and get the term weights
		System.out.print("Loading opinionated words...");
		expansionTerms.assignWeights(qemodel);
		THashSet<String> termSet = new THashSet<String>();
		File fWordlist = new File(wordlist);
		THashMap<String, String> termStemmingMap = new THashMap<String, String>();// map from stemmed terms to their original forms
		try{
			BufferedReader br = Files.openFileReader(fWordlist);
			String str = null;
			PorterStemmer stemmer = new PorterStemmer(null);
			while ((str=br.readLine())!=null){
				str = str.trim().toLowerCase();
				if (str.length() == 0)
					continue;
				String stemmed = stemmer.stem(str);
				if (bgLexicon.findTerm(stemmed)&&bgLexicon.getTF()<N&&!termSet.contains(stemmed)){
					if (USE_SKEWED_MODEL){
						if (ranking.isValidTerm(bgLexicon.getTermId())){
							termSet.add(stemmed);
							termStemmingMap.put(stemmed, str);
						}
					}else{
						termSet.add(stemmed);
						// a possible situation is that two different unstemmed terms have the same
						// stemmed form. for example, "sai" and "say" are both reduced to "sai" by
						// Porter's stemmer. 
						String unstemmed = termStemmingMap.get(stemmed);
						if (unstemmed!=null){
							LexiconEntry lexEntry = unstemmedLexicon.getLexiconEntry(unstemmed);
							if (lexEntry!=null){
								if (unstemmedLexicon.getLexiconEntry(str).TF>lexEntry.TF)
									termStemmingMap.put(stemmed, str);
								// else, don't change
							}
						}else
							termStemmingMap.put(stemmed, str);
					}
				}
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
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
		String dict = ApplicationSetup.getProperty("opinion.dictionary", "ext");
		String filename = qrelsFilename+"."+dict+"."+qemodelName;
		try{
			
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(filename);
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
	
	public void computeProbTermQrels(String qemodelName, String wordlist){
		// initiate QE model
		//QueryExpansion qe = new QueryExpansion();
		WeightingModel qemodel = WeightingModel.getWeightingModel(qemodelName);
		// Get pseudoLength
		System.out.print("Computing pseudoLength...");
		TRECQrelsInMemory qrels = new TRECQrelsInMemory();
		THashSet<String> relDocnosSet = new THashSet<String>();
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
		CollectionStatistics collSta = null;
		collSta = index.getCollectionStatistics();
		
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
		THashSet<String> termSet = new THashSet<String>();
		File fWordlist = new File(wordlist);
		try{
			BufferedReader br = Files.openFileReader(fWordlist);
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
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(new File(
					ApplicationSetup.TREC_RESULTS, "opinionTermWeights"));
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
	
	private void convertToPriors(TIntDoubleHashMap docidOpinionWeightMap,
			double k
			){
		int[] docids = docidOpinionWeightMap.keys();
		int docnoCounter = docids.length;
		double[] opinionWeights = new double[docnoCounter];
		for (int i=0; i<docnoCounter; i++)
			opinionWeights[i] = docidOpinionWeightMap.get(docids[i]);
		//ScoreNormaliser.zScoreNormalise(opinionWeights);
		ScoreNormaliser.mapToProbabilities(opinionWeights);
		Idf idf = new Idf();
		for (int i=0; i<docnoCounter; i++){
			opinionWeights[i] = -k/idf.log(opinionWeights[i]);
			docidOpinionWeightMap.put(docids[i], opinionWeights[i]);
		}
	}
	/**
	 * @deprecated
	 * @param resultFilename
	 * @param opinionWeightIndexFilename
	 * @param k
	 */
	public void combineOpinionPriors(
			String resultFilename, 
			String opinionWeightIndexFilename,
			double k){
		try{
			// normalise opinion weights
			convertToPriors(docidOpinionWeightMap, k);
			System.out.print("Combining document weights...");
			
			BufferedReader br = Files.openFileReader(resultFilename);
			String outputFilename = resultFilename.substring(0, resultFilename.lastIndexOf('.'))+
					"_opinionated_prior"+k+".res";
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			// loop
			String previousQueryid = "";
			int resultSize = 1000;
			String[] docnos = new String[resultSize];
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
							double opinionWeight = docidOpinionWeightMap.get(docHashMap.get(docnos[docnosIndex[i]]));
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
				double opinionWeight = docidOpinionWeightMap.get(docHashMap.get(docno));
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
				double opinionWeight = docidOpinionWeightMap.get(docHashMap.get(docnos[docnosIndex[i]]));
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
		}
	}
	
	static private double getCombinationParameter(String filename){
		//System.out.println(filename);
		String[] strs = filename.split("\\.");
		double k = 0;
		k=Double.parseDouble(strs[strs.length-3]+"."+strs[strs.length-2]);
		return k;
	}
	
	public double combinePrior(double raw_score, double opinionWeight){
		return raw_score+opinionWeight;
	}
	
	public double combineRank(double raw_score, int opinionRank){
		return raw_score/Math.log(opinionRank+1);
	}
	
	public double combineWeight(double raw_score, double opinionWeight, double k){
		return 10d+k*raw_score+(1-k)*opinionWeight; // add an additional 10d to avoid negative score produced by score normalisation
	}
	
	public void combineOpinionScores(String resultFilename, 
			String combinationType,
			String dict,
			int X,
			double minx, double maxk, double intk
			){
		double k = minx;
		while (k<=maxk){
			combineOpinionScores(resultFilename, combinationType, dict, X, k);
			k+=intk;
		}
	}
	
	private void combineOpinionScores(
			String resultFilename, 
			String combinationType,
			String dict,
			int X,
			double k){
		try{
			TIntDoubleHashMap localMap = (TIntDoubleHashMap)docidOpinionWeightMap.clone();
			// normalise opinion weights
			if (combinationType.equals("prior"))
				convertToPriors(localMap, k);
			else if (combinationType.equals("weight"))
				normaliseScores(localMap);
			else if (combinationType.equals("rank"))
				convertToRank(localMap);
			System.out.println("resultfile: "+resultFilename);
			System.out.println("combinationType: "+combinationType);
			System.out.println("dict: "+dict+", X: "+X+", k: "+k);
			System.out.print("Combining document weights...");
			
			BufferedReader br = Files.openFileReader(resultFilename);
			String outputFilename = resultFilename.substring(0, resultFilename.lastIndexOf('.'))+
					"."+combinationType+"."+dict+"."+X+"."+Rounding.toString(k, 2)+".res";
			if (combinationType.equals("rank"))
				outputFilename = resultFilename.substring(0, resultFilename.lastIndexOf('.'))+
					"_"+combinationType+".res";
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			// loop
			String previousQueryid = "";

			int resultSize = Integer.parseInt(ApplicationSetup.getProperty("matching.retrieved_set_size", "1000"));
			int outResultSize = Integer.parseInt(ApplicationSetup.getProperty("out.size", "1000"));
			/* The depth of the returned docs to be reweighed with opinion scores */
			int parsingDepth = Integer.parseInt(ApplicationSetup.getProperty("opinion.parsing.depth", ""+resultSize));
			String[] docnos = new String[resultSize];
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
			int linecounter = 0;
			while ((str=br.readLine())!=null){
				linecounter++;
				str = str.trim();
				if (str.length()==0) continue;
				// load one line
				StringTokenizer stk = new StringTokenizer(str);
				String queryid = stk.nextToken();
				Q = stk.nextToken(); //skip query Q0
				String docno = stk.nextToken();
				stk.nextToken(); //skip rank
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
						int[] ranks = null;
						if (combinationType.equals("rank")){
							double[] localRanks = new double[scores.length];
							int[] index = new int[scores.length];
							for (int i=0; i<counter; i++){
								index[i] = i;
								double rank = localMap.get(docHashMap.get(docnos[docnosIndex[i]]));
								localRanks[i] = (rank>0)?(rank):(-1);
							}
							dummy = new short[localRanks.length];
							Arrays.fill(dummy, (short)1);
							HeapSort.descendingHeapSort(localRanks, index, dummy);
							ranks = new int[scores.length];
							for (int i=0; i<counter; i++){
								ranks[index[i]] = (int)localRanks[i];
							}
						}
						//ScoreNormaliser.zScoreNormalise(scores);
						// combine scores
						for (int i=0; i<counter; i++){
							double opinionWeight = localMap.get(docHashMap.get(docnos[docnosIndex[i]]));
							if (opinionWeight>0d && docnosIndex[i] < parsingDepth){
								modifiedCounter++;
								if (combinationType.equals("prior"))
									scores[i] = combinePrior(scores[i], opinionWeight);
								else if (combinationType.equals("weight"))
									scores[i] = combineWeight(scores[i], opinionWeight, k);
								else if (combinationType.equals("rank"))
									scores[i] = (ranks[i]>=0)?(combineRank(scores[i], ranks[i])):
										(combineRank(scores[i], ranks.length+1));
							}
						}
						HeapSort.descendingHeapSort(scores, docnosIndex, dummy, counter);
						int outputCounter = Math.min(counter, outResultSize);
						for (int i=0; i<outputCounter; i++)
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
				double opinionWeight = localMap.get(docHashMap.get(docno));
				try{
					scores[counter] = score;
				}catch(ArrayIndexOutOfBoundsException e){
					System.err.println("queryid: "+queryid+", counter: "+counter+", line: "+linecounter+
							", parsingDepth: "+parsingDepth);
					System.err.println(str);
					e.printStackTrace();
				}
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
				//scores[i] = scores[i]+localMap.get(docnos[docnosIndex[i]]);
			int[] ranks = null;
			if (combinationType.equals("rank")){
				double[] localRanks = new double[scores.length];
				int[] index = new int[scores.length];
				for (int i=0; i<counter; i++){
					index[i] = i;
					double rank = localMap.get(docHashMap.get(docnos[docnosIndex[i]]));
					localRanks[i] = (rank>0)?(rank):(-1);
				}
				dummy = new short[localRanks.length];
				Arrays.fill(dummy, (short)1);
				HeapSort.descendingHeapSort(localRanks, index, dummy);
				ranks = new int[scores.length];
				for (int i=0; i<counter; i++){
					ranks[index[i]] = (int)localRanks[i];
				}
			}
			for (int i=0; i<counter; i++){
				double opinionWeight = localMap.get(docHashMap.get(docnos[docnosIndex[i]]));
				if (opinionWeight>0d && docnosIndex[i] < parsingDepth){
					modifiedCounter++;
					if (combinationType.equals("prior"))
						scores[i] = combinePrior(scores[i], opinionWeight);
					else if (combinationType.equals("weight"))
						scores[i] = combineWeight(scores[i], opinionWeight, k);
					else if (combinationType.equals("rank"))
						scores[i] = (ranks[i]>=0)?(combineRank(scores[i], ranks[i])):
							(combineRank(scores[i], ranks.length+1));
				}
			}
			HeapSort.descendingHeapSort(scores, docnosIndex, dummy, counter);
			int outputCounter = Math.min(counter, outResultSize);
			for (int i=0; i<outputCounter; i++)
				bw.write(previousQueryid+" "+Q+" "+docnos[docnosIndex[i]]+" "+i+" "+
						scores[i]+" "+runid+ApplicationSetup.EOL);
			// loop ends
			br.close();
			bw.close();
			localMap.clear(); localMap = null;
			System.out.println("Done! Results saved in file "+outputFilename);
			System.out.println("Modified scores of "+modifiedCounter+" returned documents.");
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	private void loadOpinionWeights(
			DocumentIndex docIndex,
			String opinionWeightIndexFilename,
			TIntHashSet docidSet,
			TIntDoubleHashMap docidOpinionWeightMap,
			TObjectIntHashMap<String> docidCache
			){
		try{
			//	load opinion weights
			System.out.print("Loading opinion weight from "+opinionWeightIndexFilename+"...");
			//double normaliser = 0d;
			boolean firstLine = true;
			BufferedReader br = Files.openFileReader(opinionWeightIndexFilename);
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
		}
	}
	
	private void loadOpinionFinderWeights(
			String opinionWeightIndexFilename
			){
		try{
			//	load opinion weights
			System.out.print("Loading opinion weights from "+opinionWeightIndexFilename+"...");
			//double normaliser = 0d;
			boolean firstLine = true;
			BufferedReader br = Files.openFileReader(opinionWeightIndexFilename);
			String str = null;
			while ((str=br.readLine())!=null){
				str = str.trim();
				if (str.length()==0) continue;
				StringTokenizer stk = new StringTokenizer(str);
				String docno = stk.nextToken();
				int subjClass1 = Integer.parseInt(stk.nextToken());//
				double diff1 = Double.parseDouble(stk.nextToken());
				int subjClass2 = Integer.parseInt(stk.nextToken());//
				double diff2 = Double.parseDouble(stk.nextToken());
				int subjBoth = Integer.parseInt(stk.nextToken());//
				double diff = Double.parseDouble(stk.nextToken());
				int sent = Integer.parseInt(stk.nextToken());
				if (firstLine){
					//normaliser = score;
					firstLine = false;
				}
				// normalise opinion weight
				//score /= normaliser;
				//System.out.println("ratio: "+ratio);
				//if (Double.isInfinite(ratio))
					//System.out.println("ratio: "+ratio);
				double score = diff1*(double)subjClass1/sent;
				if (score>0.0d){
					docidOpinionWeightMap.put(docHashMap.get(docno), score);
					//System.out.println("sum: "+(sumDiff1+sumDiff2));
				}
			}
			br.close();
			System.out.println("Done! "+docidOpinionWeightMap.size()+" doc weights are loaded.");
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
	}
	
	private void loadExtraOpinionFinderWeights(
			String opinionWeightIndexFilename
			){
		try{
			//	load opinion weights
			System.out.print("Loading opinion weights from "+opinionWeightIndexFilename+"...");
			//double normaliser = 0d;
			boolean firstLine = true;
			BufferedReader br = Files.openFileReader(opinionWeightIndexFilename);
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
				docidOpinionWeightMap.put(docHashMap.get(docno), (sumDiff1+sumDiff2));
			}
			br.close();
			System.out.println("Done! "+docidOpinionWeightMap.size()+" doc weights are loaded.");
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
	}
	
	private void loadOpinionWeights(
			String opinionWeightIndexFilename
			){
		try{
			//	load opinion weights
			System.out.print("Loading opinion weight from "+opinionWeightIndexFilename+"...");
			//double normaliser = 0d;
			boolean firstLine = true;
			BufferedReader br = Files.openFileReader(new File(opinionWeightIndexFilename));
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
				docidOpinionWeightMap.put(docHashMap.get(docno), score);
			}
			br.close();
			System.out.println("Done! "+docidOpinionWeightMap.size()+" doc weights are loaded.");
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
	}
	
	private void normaliseScores(TIntDoubleHashMap scoreMap){
		double[] scores = scoreMap.getValues();
		int[] docids = scoreMap.keys();
		int N = docids.length;
		String normMethod = ApplicationSetup.getProperty("score.normalisation.type", "max");
		if (normMethod.equals("max"))
			scores=Statistics.normaliseMax(scores);
		else if (normMethod.equals("minmax"))
			scores=Statistics.normaliseMaxMin(scores);
		else if (normMethod.equalsIgnoreCase("z"))
			ScoreNormaliser.zScoreNormalise(scores);
		/*ScoreNormaliser.mapToProbabilities(scores);
		Idf idf = new Idf();
		double k=400d;
		for (int i=0; i<N; i++)
	        	scores[i] = -k/idf.log(scores[i]);*/
		for (int i=0; i<N; i++)
			scoreMap.put(docids[i], scores[i]);
	}
	
	private void convertToRank(TIntDoubleHashMap scoreMap){
		double[] scores = scoreMap.getValues();
		int[] docids = scoreMap.keys();
		short[] dummy = new short[docids.length];
		Arrays.fill(dummy, (short)1);
		HeapSort.descendingHeapSort(scores, docids, dummy);
		for (int i=0; i<docids.length; i++)
			scoreMap.put(docids[i], (double)(i+1));
	}
	
	public void polarifyResults(
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
		TObjectIntHashMap<String> docidCache = new TObjectIntHashMap<String>();
		loadOpinionWeights(docIndex, negativeFilename, docidSet, negativeMap, docidCache);
		normaliseScores(negativeMap);
		loadOpinionWeights(docIndex, mixedFilename, docidSet, mixedMap, docidCache);
		normaliseScores(mixedMap);
		loadOpinionWeights(docIndex, positiveFilename, docidSet, positiveMap, docidCache);
		normaliseScores(positiveMap);
		docidCache.clear();
		docidCache = null;
		// normalise scores?
		// for each query
		TRECResultsInMemory results = new TRECResultsInMemory(resultFilename);
		String[] queryids = results.getQueryids();
		try{
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(new File(polarityFilename));
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
		THashSet<String> relDocnos = new THashSet<String>();
		THashSet<String> negDocnos = new THashSet<String>();
		THashSet<String> mixDocnos = new THashSet<String>();
		THashSet<String> posDocnos = new THashSet<String>();
		try{
			BufferedReader br = Files.openFileReader(new File(resultFilename));
			String str = null;
			THashSet<String> queryidSet = new THashSet<String>();
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
		TRECQuery queries = null;
		String queryParser = ApplicationSetup.getProperty("trec.topics.parser", "TRECQuery"); 
		if (queryParser.equals("TRECQuery"))
			queries = new TRECQuery(topicFilename);
		else if (queryParser.equals("SingleLineTRECQuery"))
			queries = new SingleLineTRECQuery(topicFilename);
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
			if (nonRelDocnos!=null)
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
		System.out.println("Sampled topics saved in file "+topicOutputFilename);
		System.out.println("Sampled qrels saved in file "+qrelsOutputFilename);
		// get the rest of the topics and corresponding qrels
		TIntHashSet restPosSet = new TIntHashSet();
		for (int i=0; i<queryids.length; i++)
			if (!selectedPos.contains(i))
				restPosSet.add(i);
		pos = restPosSet.toArray();
		Arrays.sort(pos);
		topicBuffer = new StringBuffer();
		qrelsBuffer = new StringBuffer();
		for (int i=0; i<pos.length; i++){
			topicBuffer.append(queryids[pos[i]]+" "+queries.getQuery(queryids[pos[i]])+
					ApplicationSetup.EOL);
			String[] nonRelDocnos = qrels.getNonRelevantDocumentsToArray(queryids[pos[i]]);
			if (nonRelDocnos!=null)
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
			topicOutputFilename = topicOutputFilename+".rest";
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(topicOutputFilename);
			bw.write(topicBuffer.toString());
			bw.close();
			qrelsOutputFilename = qrelsOutputFilename+".rest";
			bw = (BufferedWriter)Files.writeFileWriter(qrelsOutputFilename);
			bw.write(qrelsBuffer.toString());
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		System.out.println("Sampled topics saved in file "+topicOutputFilename);
		System.out.println("Sampled qrels saved in file "+qrelsOutputFilename);
	}
	
	public static void randomQueryQrelsSplit(
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
	
	
	static public void computePMI(String dictFilename, String lexFilename1, String lexFilename2, int N1, int N2){
		String outputFilename = dictFilename+".pmi";
		Lexicon lex1 = new Lexicon(lexFilename1);
		Lexicon lex2 = new Lexicon(lexFilename2);
		PorterStemmer stemmer = new PorterStemmer(null);
		try{
			BufferedReader br = Files.openFileReader(dictFilename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			String str = null;
			THashSet<String> termSet = new THashSet<String>();
			while ((str=br.readLine())!=null){
				str = stemmer.stem(str);
				if (termSet.contains(str)) continue;
				termSet.add(str);
				double PMI = getPMI(lex1, lex2, str, N1, N2);
				bw.write(str+" "+PMI+ApplicationSetup.EOL);
			}
			br.close();
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		lex1.close(); lex2.close();
		System.out.println("Saved in file "+outputFilename);
	}
	
	static private double getPMI(Lexicon lex_x, Lexicon lex_y, String term, int Nx, int Ny){
		double PMI = 0;
		if (!lex_x.findTerm(term)||!lex_y.findTerm(term)){
			return 0;
		}
		double p_xy = (double)lex_x.getNt()/Nx;
		double p_x = (double)lex_y.getNt()/Ny;
		//double p_y = (double)Nx / Ny;
		PMI = idf.log(p_xy/(p_x));
		if (PMI < 0)
			System.err.println(term+" "+lex_x.getNt()+" "+lex_y.getNt()+" "+Nx+" "+Ny);
		return PMI;
	}
	
	public static void computMeanOnelineQuerySimilarity(String dirname){
		File fDir = new File(dirname);
		String[] filenames = fDir.list();
		int n = filenames.length*(filenames.length-1)/2;
		double[] sims = new double[n];
		int counter = 0;
		for (int i=0; i<filenames.length-2; i++)
			for (int j=i+1; j<filenames.length-1; j++){
				double sim = getOnelineQuerySimilarity(
						dirname+ApplicationSetup.FILE_SEPARATOR+filenames[i], 
						dirname+ApplicationSetup.FILE_SEPARATOR+filenames[j]);
				System.out.println("sim("+filenames[i]+", "+filenames[j]+"): "+sim);
				sims[counter++] = sim;
			}
		System.out.println("mean: "+Statistics.mean(sims));
	}

    public static double getMeanOnelineQuerySimilarity(
    		String sampleDir, String year, String prefix,
    		int minid, int maxid, String dict, String qemodel, String X
    		){
    	int n = (maxid-minid+1)*(maxid-minid)/2;
    	double[] sims = new double[n];
    	int counter = 0;
    	String sep = ApplicationSetup.FILE_SEPARATOR;
    	String dot = ".";
    	for (int i=0; i<maxid; i++){
    		for (int j=i+1; j<=maxid; j++){
    			String filename1 = sampleDir+sep+year+sep+prefix+dot+i+dot+
    					dict+dot+qemodel+"_topics.top"+X;
    			String filename2 = sampleDir+sep+year+sep+prefix+dot+j+dot+
						dict+dot+qemodel+"_topics.top"+X;
    			sims[counter] = getOnelineQuerySimilarity(filename1, filename2);
    			System.out.println(sims[counter]);
    			counter++;
    		}
    	}
    	double mean = Statistics.mean(sims);
    	System.out.println("mean: "+mean);
    	return mean;
    }
    
    public static void printPromotionProb(){
    	
    }
    
    public static void printPromotionProb(String resultFilename, String qrelsFilename){
		TRECResultsInMemory results = new TRECResultsInMemory(resultFilename);
		TRECQrelsInMemory qrels = new TRECQrelsInMemory(qrelsFilename);
		int numberOfQueries = results.getNumberOfQueries();
		String[] queryids = results.getQueryids();
		TDoubleArrayList probList = new TDoubleArrayList();
		TDoubleArrayList probMList = new TDoubleArrayList();
		TDoubleArrayList probNList = new TDoubleArrayList();
		TIntArrayList lastRankList = new TIntArrayList();
		for (int i=0; i<numberOfQueries; i++){
			String[] retDocnos = results.getRetrievedDocnos(queryids[i]);
			int[] relevant = {1, 2, 3, 4};
			int[] opinionated = {2, 3, 4};
			THashSet<String> relDocnoSet = qrels.getRelevantDocuments(queryids[i], relevant);
			THashSet<String> opnDocnoSet = qrels.getRelevantDocuments(queryids[i], opinionated);
			// find relevant retrieved
			THashSet<String> retRelDocnoSet = new THashSet<String>();
			for (int j=0; j<retDocnos.length; j++){
				if (relDocnoSet.contains(retDocnos[j]))
					retRelDocnoSet.add(retDocnos[j]);
			}
			// compute prob
			int nonOpnCounter = 0; int opnCounter = 0;
			double sumProb = 0d;
			double sumProbM = 0d;
			double sumProbN = 0d;
			int n = retRelDocnoSet.size();
			int lastRank = -1;
			if (relDocnoSet.contains(retDocnos[0])){
				n--;
				lastRank = 0;
			}
			if (opnDocnoSet.contains(retDocnos[0]))
				opnCounter++;
			else
				nonOpnCounter++;
			if (n==0) continue;
			double prob_i = 1d/n; // prior is assumed to be uniform
			
			for (int j=1; j<retDocnos.length; j++){
				if (relDocnoSet.contains(retDocnos[j])){
					if (opnDocnoSet.contains(retDocnos[j])){
						// assume uniform likelihood distribution again
						double likelihood = (double)nonOpnCounter/(nonOpnCounter+opnCounter);
						sumProb+= likelihood*prob_i;
						double likelihoodN=(double)opnCounter/(nonOpnCounter+opnCounter);
						sumProbN+=likelihoodN*prob_i;
					}else{
						// assume uniform likelihood distribution again
						double likelihood = (double)opnCounter/(nonOpnCounter+opnCounter);
						sumProbM+=likelihood*prob_i;
						double likelihoodN=(double)nonOpnCounter/(nonOpnCounter+opnCounter);
						sumProbN+=likelihoodN*prob_i;
					}
					lastRank = j;
				}
				
				if (opnDocnoSet.contains(retDocnos[j]))
					opnCounter++;
				else
					nonOpnCounter++;
			}
			if (Double.isNaN(sumProb))
				System.err.println("n: "+n);
			probList.add(sumProb);
			probMList.add(sumProbM);
			probNList.add(sumProbN);
			System.out.println(queryids[i]+": "+sumProb+", "+sumProbM+", "+sumProbN+", "+lastRank);
			lastRankList.add(lastRank);
		}
		System.out.println("mean: "+Statistics.mean(probList.toNativeArray())+", "+
				Statistics.mean(probMList.toNativeArray())+", "+
				Statistics.mean(probNList.toNativeArray())+", "+
				Statistics.mean(lastRankList.toNativeArray()));
	}
	
	public static double getOnelineQuerySimilarity(String filename1, String filename2){
		double sim = 0;
		//System.out.print(filename1+", "+filename2+": ");
		try{
			BufferedReader br1 = Files.openFileReader(filename1);
			BufferedReader br2 = Files.openFileReader(filename2);
			String queryString1 = br1.readLine();
			String queryString2 = br2.readLine(); 
			//System.out.println(queryString1);
			//System.out.println(queryString2);
			String[] query1 = queryString1.replaceAll("\\^", " ").split(" ");
			String[] query2 = queryString2.replaceAll("\\^", " ").split(" ");
			THashSet<String> termSet = new THashSet<String>();
			TObjectDoubleHashMap<String> map1 = new TObjectDoubleHashMap<String>();
			TObjectDoubleHashMap<String> map2 = new TObjectDoubleHashMap<String>();
			for (int i=0; i<query1.length; i+=2){
				termSet.add(query1[i].trim());
				map1.put(query1[i].trim(), Double.parseDouble(query1[i+1].trim()));
				//System.out.println(">>q1: "+query1[i]+" "+query1[i+1].trim());
			}
			for (int i=0; i<query2.length; i+=2){
				termSet.add(query2[i].trim());
				map2.put(query2[i].trim(), Double.parseDouble(query2[i+1].trim()));
				//System.out.println(">>q2: "+query2[i]+" "+query2[i+1].trim());
			}
			double[] vec1 = new double[termSet.size()];
			double[] vec2 = new double[termSet.size()];
			Arrays.fill(vec1, 0d); Arrays.fill(vec2, 0d);
			String[] terms = (String[])termSet.toArray(new String[termSet.size()]);
			for (int i=0; i<termSet.size(); i++){
				if (map1.contains(terms[i]))
					vec1[i] = map1.get(terms[i]);
				if (map2.contains(terms[i]))
					vec2[i] = map2.get(terms[i]);
				//System.out.println(terms[i]+": "+map1.get(terms[i])+", "+map2.get(terms[i]));
			}
			sim = DistanceModifier.cosine1(vec1, vec2);
			br1.close();
			br2.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		System.out.println(sim);
		return sim;
	}

	public static void main(String[] args) {
		if (args[1].equals("--avgrel"))
			(new TestBlogOpinion()).computeRelDocLength();
		else if (args[1].equals("--computedivergence"))
			// -blog --computedivergence <qemodelName> <workdlistfilename>
			// -blog --computedivergence Bo1 /users/tr.ben/Blog06/wordlist
			(new TestBlogOpinion()).computeProbTermQrels(args[2], args[3]);
		else if (args[1].equals("--buildqrelslex")){
			// -blog --buildqrelslex <qrelsFilename> <grades ...>
			// -blog --buildqrelslex <qrelsFilename> 0 # for all relevant documents
			// -blog --buildqrelslex <qrelsFilename> 2 3 # all relevant documents with relevance degrees 2 and 3
			int[] grades = new int[args.length-3];
			for (int i=0; i<grades.length; i++)
				grades[i] = Integer.parseInt(args[i+3]);
			(new TestBlogOpinion()).buildQrelsLexicon(args[2], grades);
		}
		else if (args[1].equals("--selectqueries")){
			// -blog --selectqueries topicfilename outputfilename #selectedqueries
			TestBlogOpinion.randomQuerySelection(args[2], Integer.parseInt(args[4]), args[3]);
		}
		else if (args[1].equals("--selectquerywithqrels")){
			// -blog --selectquerywithqrels topicfilename qrelsfilename #selectedqueries
			TestBlogOpinion.randomQueryQrelsSelection(args[2], args[3], Integer.parseInt(args[4]));
		}
		else if (args[1].equals("--computeopiniontermweightsgraded")){
			// -blog --computeopiniontermweightsgraded <qemodelName> <wordlistfilename> <bglexiconfilename> <qrelsFilename> <grades...>
			// -blog --computeopiniontermweightsgraded Bo1 /users/tr.ben/Blog06/wordlist /users/tr.ben/Blog06/qrels0.lex /users/tr.ben/Blog06/06.qrels 1 3
			int[] grades = new int[args.length-6];
			for (int i=0; i<grades.length; i++)
				grades[i] = Integer.parseInt(args[i+6]);
			(new TestBlogOpinion()).computeDivTermQrels(args[2], args[3], args[4], args[5], grades);
		}
		else if (args[1].equals("--writeonelinequery"))
			// -blog --writeonelinequery <filename>
			(new TestBlogOpinion()).writeOneLineQuery(args[2]);
		else if (args[1].equals("--writetoponelinequery"))
			// -blog --writetoponelinequery <filename> <topX>
			(new TestBlogOpinion()).writeTopOneLineQuery(args[2], Integer.parseInt(args[3]));
		else if(args[1].equals("--combineopinionweights")){
			// -blog --combineopinionweights <resultFilename> <opinionWeightIndexFilename> <dict> <X> <mink> <maxk> <intk>
			TestBlogOpinion app = new TestBlogOpinion(args[3]);
			app.combineOpinionScores(args[2], "weight", args[4], Integer.parseInt(args[5]), 
					Double.parseDouble(args[6]), Double.parseDouble(args[7]), Double.parseDouble(args[8]));
		}
		else if(args[1].equals("--combineopinionpriors")){
			// -blog --combineopinionpriors <resultFilename> <opinionWeightIndexFilename> <dict> <X> <mink> <maxk> <intk>
			TestBlogOpinion app = new TestBlogOpinion(args[3]);
			app.combineOpinionScores(args[2], "prior", args[4], Integer.parseInt(args[5]), 
					Double.parseDouble(args[6]), Double.parseDouble(args[7]), Double.parseDouble(args[8]));
		}else if(args[1].equals("--combineopinionranks")){
			// -blog --combineopinionweights <resultFilename> <opinionWeightIndexFilename> <dict> <X> <mink> <maxk> <intk>
			TestBlogOpinion app = new TestBlogOpinion(args[3]);
			app.combineOpinionScores(args[2], "rank", args[4], Integer.parseInt(args[5]), 
					Double.parseDouble(args[6]), Double.parseDouble(args[7]), Double.parseDouble(args[8]));
		}
		else if(args[1].equals("--polarify")){
			// -blog --polarify <resultFilename> <negativefilename> <mixedfilename> <positivefilename> <outputfilename>
			(new TestBlogOpinion()).polarifyResults(args[2], args[3], args[4], args[5], args[6]);
		}
		else if(args[1].equals("--evaluatepolarification")){
			// -blog --evaluatepolarification <qrelsFilename> <resultFilename>
			TestBlogOpinion.evaluatePolarityResults(args[2], args[3]);
		}
		else if(args[1].equals("--extractwordsfromunstemmedlexicon")){
			// -blog --extractwordsfromunstemmedlexicon <indexPath> <indexPrefix>
			(new TestBlogOpinion()).extractWordsFromUnstemmedLexicon(args[2], args[3]);
		}
		else if (args[1].equals("--filterwordlist")){
			// -blog --filterwordlist <indexPath> <indexPrefix> <wordlistFilename>
			filterWordlist(args[2], args[3], args[4]);
		}else if (args[1].equals("--removenonrelevantdocuments")){
			// -blog --removenonrelevantdocuments <resultfilename> <qrelsfilename>
			removeNonRelevantDocuments(args[2], args[3]);
		}else if(args[1].equals("--stemwordlist")){
			// -blog --stemwordlist <wordlistFilename>
			stemWordlist(args[2]);
		}else if(args[1].equals("--onelinequerysim")){
			// -blog --onelinequerysim <filename1> <filename2>
			TestBlogOpinion.getOnelineQuerySimilarity(args[2], args[3]);
		}else if(args[1].equals("--onelinequerymeansim")){
			// -blog --onelinequerymeansim sampleDir year prefix minid maxid dict qemodel X
			TestBlogOpinion.getMeanOnelineQuerySimilarity(args[2], args[3], args[4], Integer.parseInt(args[5]),
					Integer.parseInt(args[6]), args[7], args[8], args[9]);
		}else if(args[1].equals("--allonelinequerymeansim")){
			// -blog --allonelinequerymeansim <foldername>
			TestBlogOpinion.computMeanOnelineQuerySimilarity(args[2]);
		}
		else if(args[1].equals("--loadevalresult")){
			// -blog --loadevalresult dirname prefix <minid> <maxid> rest model qemodel dict combtype <minX> <maxX> <intX>
			TestBlogOpinion.loadXidEvaluationResults(args[2], args[3], 
					Integer.parseInt(args[4]), Integer.parseInt(args[5]), 
					args[6], args[7], args[8], args[9], args[10], 
					Integer.parseInt(args[11]), Integer.parseInt(args[12]), 
					Integer.parseInt(args[13]));
		}else if(args[1].equals("--computepmi")){
			// -blog --computepmi <dictFilename> <lexFilename1> <lexFilename2> <N1> <N2>
			TestBlogOpinion.computePMI(args[2], args[3], args[4], Integer.parseInt(args[5]), Integer.parseInt(args[6]));
		}else if(args[1].equals("--loadtestresults")){
			// -blog --loadtestresults filename
			TestBlogOpinion.loadTestResult(args[2]);
		}else if(args[1].equals("--renametomatlab")){
			// -blog --renametomatlab dirname
			TestBlogOpinion.renameToMatlab(args[2]);
		}else if(args[1].equals("--printpromotionprobs")){
			// -blog --printpromotionprobs resultFilename qrelsFilename
			TestBlogOpinion.printPromotionProb(args[2], args[3]);
		}else if(args[1].equals("--help")){
			System.out.println("Usage: ");
			System.out.println("-blog --computedivergence <qemodelName> <workdlistfilename>");
			System.out.println("-blog --buildqrelslex <qrelsFilename> <grades ...>");
			System.out.println("-blog --selectqueries topicfilename outputfilename #selectedqueries");
			System.out.println("-blog --selectquerywithqrels topicfilename qrelsfilename #selectedqueries");
			System.out.println("-blog --computeopiniontermweightsgraded <qemodelName> <wordlistfilename> <bglexiconfilename> <qrelsFilename> <grades...>");
			System.out.println("-blog --writeonelinequery <filename>");
			System.out.println("-blog --writetoponelinequery <filename> <topX>");
			System.out.println("-blog --combineopinionweights <resultFilename> <opinionWeightIndexFilename> <dict> <X> <mink> <maxk> <intk>");
			System.out.println("-blog --combineopinionpriors <resultFilename> <opinionWeightIndexFilename> <dict> <X> <mink> <maxk> <intk>");
			System.out.println("-blog --combineopinionweights <resultFilename> <opinionWeightIndexFilename> <dict> <X> <mink> <maxk> <intk>");
			System.out.println("-blog --polarify <resultFilename> <negativefilename> <mixedfilename> <positivefilename> <outputfilename>");
			System.out.println("-blog --evaluatepolarification <qrelsFilename> <resultFilename>");
			System.out.println("-blog --extractwordsfromunstemmedlexicon <indexPath> <indexPrefix>");
			System.out.println("-blog --filterwordlist <indexPath> <indexPrefix> <wordlistFilename>");
			System.out.println("-blog --removenonrelevantdocuments <resultfilename> <qrelsfilename>");
			System.out.println("-blog --stemwordlist <wordlistFilename>");
			System.out.println("-blog --onelinequerysim <filename1> <filename2>");
			System.out.println("-blog --onelinequerymeansim sampleDir year prefix minid maxid dict qemodel X");
			System.out.println("-blog --allonelinequerymeansim <foldername>");
			System.out.println("-blog --loadevalresult dirname prefix <minid> <maxid> rest model qemodel dict combtype <minX> <maxX> <intX>");
			System.out.println("-blog --computepmi <dictFilename> <lexFilename1> <lexFilename2> <N1> <N2>");
			System.out.println("-blog --loadtestresults filename");
			System.out.println("-blog --renametomatlab dirname");
			System.out.println("-blog --printpromotionprobs resultFilename qrelsFilename");
		}
	}
}

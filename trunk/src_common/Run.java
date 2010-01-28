/*
 * Created on 2004-12-28
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

import gnu.trove.TDoubleHashSet;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TObjectDoubleHashMap;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Pattern;

import tests.*;
import uk.ac.gla.terrier.applications.CheckRedundancyOfDiffQueryTypes;
import uk.ac.gla.terrier.applications.CheckResultFile;
import uk.ac.gla.terrier.applications.ChopResultFile;
import uk.ac.gla.terrier.applications.ECIR09;
import uk.ac.gla.terrier.applications.PrintAverageQueryLength;
import uk.ac.gla.terrier.applications.PrintStdOfDocLength;
import uk.ac.gla.terrier.applications.SortStopwordList;
import uk.ac.gla.terrier.applications.TRECBasicQuerying;
import uk.ac.gla.terrier.applications.TestCorrelation;
import uk.ac.gla.terrier.applications.Tuning;
import uk.ac.gla.terrier.compression.BitFile;
import uk.ac.gla.terrier.compression.BitOutputStream;
import uk.ac.gla.terrier.evaluation.TRECQrelsInMemory;
import uk.ac.gla.terrier.matching.models.normalisation.Normalisation;
import uk.ac.gla.terrier.matching.models.normalisation.NormalisationB;
import uk.ac.gla.terrier.matching.models.queryexpansion.QueryExpansionModel;
import uk.ac.gla.terrier.querying.Manager;
import uk.ac.gla.terrier.simulation.QuerySimulation;
import uk.ac.gla.terrier.utility.HeapSort;
import uk.ac.gla.terrier.statistics.Statistics;
import uk.ac.gla.terrier.structures.BasicQuery;
import uk.ac.gla.terrier.structures.BlockDirectIndexInputStream;
import uk.ac.gla.terrier.structures.BlockInvertedIndex;
import uk.ac.gla.terrier.structures.BlockLexicon;
import uk.ac.gla.terrier.structures.BlockLexiconInputStream;
import uk.ac.gla.terrier.structures.BlockLexiconOutputStream;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.DirectIndex;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.InvertedIndex;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.structures.LexiconEntry;
import uk.ac.gla.terrier.structures.LexiconInputStream;
import uk.ac.gla.terrier.structures.LexiconOutputStream;
import uk.ac.gla.terrier.structures.Matrix;
import uk.ac.gla.terrier.structures.TRECQuery;
import uk.ac.gla.terrier.structures.TRECResult;
import uk.ac.gla.terrier.structures.indexing.MatrixBuilder;
import uk.ac.gla.terrier.terms.PorterStemmer;
import uk.ac.gla.terrier.utility.*;
/**
 * @author ben
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Run {
	 boolean flag = false;
	 
	 protected final boolean debugging = new Boolean(
			ApplicationSetup.getProperty("debugging.mode", "false")).booleanValue();
	 
	 protected String EOL = ApplicationSetup.EOL;
	 
	 public Run(String[] args){
	 	try{
	 		if (args.length == 0){
	 			this.printOptions();
	 			flag = true;
	 			System.exit(0);
	 		}else if (args[0].equalsIgnoreCase("-entropy")){
	 			// -entropy <filename> <index>
	 			this.printEntropy(args[1], Integer.parseInt(args[2]));
	 		}
	 		else if(args[0].equalsIgnoreCase("-inl")){
	 			this.inLexicon(args[1], args[2]);
	 		}else if(args[0].equalsIgnoreCase("-binl")){
	 			this.inBlockLexicon(args[1], args[2]);
	 		}else if(args[0].equalsIgnoreCase("-popt")){
	 			// -popt map /local/terrier_tmp/ben/WT2G/bin/PL2_OQ1_opt.out
	 			this.processOptimisationOutput(args[1], args[2]);
	 		}else if(args[0].equalsIgnoreCase("-wd")){
	 			// -wd <numberOfTokens>
	 			double numberOfTokens = Double.parseDouble(args[1]);
	 			this.weighDocuments(numberOfTokens);
	 		}else if(args[0].equalsIgnoreCase("-cqrels")){
	 			this.createQrels();
	 		}else if(args[0].equalsIgnoreCase("-elite")){
	 			this.computeEliteness(args[1]);
	 		}else if(args[0].equals("-tc")){
	 			TestClarity.main(args);
	 		}
	 		else if(args[0].equalsIgnoreCase("-crglex")){
	 			int n = args.length - 1;
	 			int numberOfIndices = n/2-1;
	 			String[] path = new String[numberOfIndices];
	 			String[] prefix = new String[numberOfIndices];
	 			for (int i=0; i<numberOfIndices; i++){
	 				path[i] = args[1+i*2];
	 				prefix[i] = args[2+i*2];
	 			}
	 			this.createGlobalLex(path, prefix, args[n-1], args[n]);
	 		}
	 		else if(args[0].equalsIgnoreCase("-crglexs")){
	 			int n = args.length - 1;
	 			int numberOfIndices = n/2-1;
	 			String[] path = new String[numberOfIndices];
	 			String[] prefix = new String[numberOfIndices];
	 			for (int i=0; i<numberOfIndices; i++){
	 				path[i] = args[1+i*2];
	 				prefix[i] = args[2+i*2];
	 			}
	 			this.createGlobalLexSlow(path, prefix, args[n-1], args[n]);
	 		}
	 		else if(args[0].equalsIgnoreCase("-bpref")){
	 			TestBPref.main(args);
	 		}
	 		else if (args[0].equals("-i")){
	 			TestIndex.main(args);
	 		}
	 		else if (args[0].equals("-blog")){
	 			TestBlogOpinion.main(args);
	 		}
	 		else if (args[0].equals("-p")){
	 			QueryPrediction.main(args);
	 		}
	 		else if(args[0].equalsIgnoreCase("-rl")){
	 			this.rebuildLexicon(args[1], args[2]);
	 		}
	 		else if(args[0].equalsIgnoreCase("-brl")){
	 			this.rebuildBlockLexicon(args[1], args[2]);
	 		}
	 		else if(args[0].equalsIgnoreCase("-rbdf")){
	 			this.alignBlockDirectIndexTermid(args[1], args[2]);
	 		}
	 		
	 		else if (args[0].equalsIgnoreCase("-ckl")){
	 			this.checkLex(args[1], args[2]);
	 		}
	 		else if (args[0].equalsIgnoreCase("-ckbl")){
	 			this.checkBlockLex(args[1], args[2]);
	 		}
	 		else if (args[0].equalsIgnoreCase("-copl")){
	 			flag = true;
	 			this.compactLexicon(args[1], args[2]);
	 		}else if (args[0].equalsIgnoreCase("-copsubl")){
	 			flag = true;
	 			this.compactSubLexicon(args[1], args[2], args[3]);
	 		}
	 		else if (args[0].equalsIgnoreCase("-copbl")){
	 			flag = true;
	 			this.compactBlockLexicon(args[1], args[2]);
	 		}else if (args[0].equalsIgnoreCase("-copsubbl")){
	 			flag = true;
	 			this.compactSubBlockLexicon(args[1], args[2], args[3]);
	 		}else if (args[0].equals("-q")){
	 			TestQuery.main(args);
	 		}
	 		
	 		else if (args[0].equalsIgnoreCase("-crli")){
	 			flag = true;
	 			try{
	 				createLexiconIndex(args[1], args[2]);
	 			}
	 			catch(IOException ioe){
	 				ioe.printStackTrace();
	 				System.exit(1);
	 			}
	 		}
	 		
	 		else if (args[0].equals("-b")){
	 			flag = true;
	 			BuildMatrix.main(args);
	 		}
	 		
	 		else if (args[0].equalsIgnoreCase("-qr")){
	 			flag = true;
	 			CheckRedundancyOfDiffQueryTypes.main(args);
	 		}
	 		
	 		else if (args[0].equalsIgnoreCase("-avql")){
	 			flag = true;
	 			PrintAverageQueryLength.main(null);
	 		}
	 		
	 		else if (args[0].equalsIgnoreCase("-stdl")){
	 			flag = true;
	 			PrintStdOfDocLength.main(null);
	 		}
	 		
	 		else if (args[0].equalsIgnoreCase("-t")||args[0].equalsIgnoreCase("-it")){
	 			flag = true;
	 			Tuning.main(args);
	 		}
	 		else if (args[0].equals("-dist")){
	 			TestDistance.main(args);
	 		}
	 		/*else if (args[0].equalsIgnoreCase("-remQrels")){
	 			flag = true;
	 			this.randomQrelsRemoval(Double.parseDouble(args[1]), args[2]);
	 		}*/
	 		else if (args[0].equalsIgnoreCase("-ret")||
	 				args[0].equalsIgnoreCase("-retqe")){
	 			flag = true;
	 			TRECBasicQuerying.main(args);
	 		}
	 		
	 		else if (args[0].equalsIgnoreCase("-corr")){
	 			flag = true;
	 			TestCorrelation.main(args);
	 		}
	 		
	 		else if (args[0].equalsIgnoreCase("-crf")){
	 			flag = true;
	 			CheckResultFile.main(args);
	 		}
	 		
	 		else if (args[0].equalsIgnoreCase("-cprf")){
	 			flag = true;
	 			ChopResultFile.main(args);
	 		}
	 		
	 		else if (args[0].equalsIgnoreCase("-sig05")||args[0].equalsIgnoreCase("-sig06")){
	 			flag = true;
	 			TestSIGIR05.main(args);
	 		}
		 	// print collection statistics
	 		else if (args[0].equalsIgnoreCase("-collSta")){
	 			try{
	 				CollectionStatistics collSta = Index.createIndex().getCollectionStatistics();
	 				flag = true;
	 				System.out.println("Number of unique terms: " + collSta.getNumberOfUniqueTerms());
	 				System.out.println("Number of tokens: " + collSta.getNumberOfTokens());
	 				System.out.println("Number of Documents: " + collSta.getNumberOfDocuments());
	 				System.out.println("Average document length: " + collSta.getAverageDocumentLength());
	 			}catch(Exception ioe){
	 				ioe.printStackTrace();
	 				System.exit(1);
	 			}
		 	}
		 	
	 		else if (args[0].equalsIgnoreCase("-lex")){
		 		flag = true;
		 		Lexicon lexicon = Index.createIndex().getLexicon();
		 		lexicon.findTerm(Integer.parseInt(args[1]));
		 		System.out.println("term: " + lexicon.getTerm());
		 	}else if(args[0].equalsIgnoreCase("-dlex")){
		 		this.dumpLexicon(args[1]);
		 	}
		 	
	 		else if (args[0].equalsIgnoreCase("-qt")){
		 		flag = true;
		 		System.out.println("query type: " + SystemUtility.queryType());
		 	}
		 	
	 		else if (args[0].equalsIgnoreCase("-i")){
		 		flag = true;
		 		TestIndex.main(args);
		 	}
		 	
	 		else if (args[0].equalsIgnoreCase("-r")){
		 		if (args[1].equalsIgnoreCase("-d")){
		 			flag = true;
		 			TestRetrieval.main(args);
		 		}
		 		
		 		else if (args[1].equalsIgnoreCase("-o")){
		 			flag = true;
		 			TestResults.main(args);
		 		}
		 		
		 		else if (args[1].equalsIgnoreCase("-q")){
		 			flag = true;
		 			// -r -q
		 			TestExtractQueryFeatures.main(args);
		 		}
		 	}
		 	
	 		else if (args[0].equalsIgnoreCase("-res")){
		 		flag = true;
		 		TestResults.main(args);
		 	}
		 	
	 		else if (args[0].equalsIgnoreCase("-s")){
		 		flag = true;
		 		this.createScript(args[1], 
		 				Double.parseDouble(args[2]), 
		 				Double.parseDouble(args[3]), 
		 				Double.parseDouble(args[4]), 
		 				Integer.parseInt(args[5]), 
		 				args[6],
		 				Double.parseDouble(args[7]), 
		 				Double.parseDouble(args[8]), 
		 				Double.parseDouble(args[9]), 
		 				Integer.parseInt(args[10]), 
		 				args[11]);
		 	}
		 	
	 		else if (args[0].equalsIgnoreCase("-e")){
		 		flag = true;
		 		TestEvaluation.main(args);
		 	}
	 		else if (args[0].equals("-m")){
	 			flag = true;
	 			TestMedline.main(args);
	 		}
		 	
	 		else if (args[0].equalsIgnoreCase("-debug")){
		 		flag = true;
		 		this.debug(args);
		 	}
		 	
	 		else 
		 		this.printOptions();
	 	}
	 	catch(ArrayIndexOutOfBoundsException e){
	 		e.printStackTrace();
	 		//this.printOptions();
	 		System.exit(0);
	 	}
	 }
	 /**
	  * 
	  * @param percentage The percentage of relevant documents to be removed per query. 
	  * @param outputFilename
	  */
	 /*public void randomQrelsRemoval(double percentage, String outputFilename){
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
	 }*/
	 
	 public void paraSens_extractMAPs(
			 String noRfilename, 
			 String Rfilename, 
			 String TRfilename,
			 String outfilename){
		 File noRFile = new File(noRfilename);
		 File RFile = new File(Rfilename);
		 File TRFile = new File(TRfilename);
		 File outFile = new File(outfilename);
		 
		 double[] noR = new double[32];
		 double[] R = new double[32];
		 double[] TR = new double[16];
		 
		 double[][] diffnoR = new double[2][8];
		 double[][] diffR = new double[2][8];
		 double[][] diffTR = new double[2][4];
		 
		 try{
			 BufferedReader br = new BufferedReader(new FileReader(noRFile));
			 String str = null;
			 int counter = 31;
			 while ((str=br.readLine())!=null){
				 StringTokenizer stk = new StringTokenizer(str);
				 stk.nextToken();
				 stk.nextToken();
				 noR[counter--] = Double.parseDouble(stk.nextToken());
			 }
			 br.close();
			 
			 br = new BufferedReader(new FileReader(RFile));
			 counter = 31;
			 while ((str=br.readLine())!=null){
				 StringTokenizer stk = new StringTokenizer(str);
				 stk.nextToken();
				 stk.nextToken();
				 R[counter--] = Double.parseDouble(stk.nextToken());
			 }
			 br.close();
			 
			 br = new BufferedReader(new FileReader(TRFile));
			 counter = 15;
			 while ((str=br.readLine())!=null){
				 StringTokenizer stk = new StringTokenizer(str);
				 stk.nextToken();
				 stk.nextToken();
				 TR[counter--] = Double.parseDouble(stk.nextToken());
			 }
			 br.close();
			 
			 for (int i = 0; i < 8; i++){
				 diffnoR[0][i] = noR[i*4+1] - noR[i*4];
				 diffnoR[1][i] = noR[i*4+3] - noR[i*4+2];
				 diffR[0][i] = R[i*4+1] - R[i*4];
				 diffR[1][i] = R[i*4+3] - R[i*4+2];
			 }
			 for (int i = 0; i < 4; i++){
				 diffTR[0][i] = TR[i*4+1] - TR[i*4];
				 diffTR[1][i] = TR[i*4+3] - TR[i*4+2];
			 }
			 
			 // output. What format?
			 StringBuffer buffer = new StringBuffer();
			 for (int i = 0; i < 4; i++){
				 buffer.append(diffnoR[0][i]+" "+diffnoR[1][i]+
						 " "+diffnoR[0][i+4]+" "+diffnoR[1][i+4]+
						 diffR[0][i]+" "+diffR[1][i]+
						 " "+diffR[0][i+4]+" "+diffR[1][i+4]+
						 " "+diffTR[0][i]+" "+diffTR[1][i]+EOL
						 );
			 }
			 BufferedWriter bw = new BufferedWriter(new FileWriter(outFile));
			 bw.write(buffer.toString());
			 bw.close();
			 
		 }catch(IOException ioe){
			 ioe.printStackTrace();
			 System.exit(1);
		 }
	 }
	 
	 public void debugSimulation(){
	 	QuerySimulation simulation = new QuerySimulation();
	 	System.out.println(simulation.twoStepSimulation(10));
	 	//TFRanking ranking = new TFRanking();
	 	//ranking.dumpTFLog(20);
	 }
	 
	 public void printEntropy(String filename, int index){
		 File f = new File (filename);
		 TDoubleHashSet valueSet = new TDoubleHashSet();
		 try{
			 BufferedReader br = new BufferedReader(new FileReader(f));
			 String str = null;
			 while ((str=br.readLine())!=null){
				 str = str.trim();
				 if (str.length()==0)
					 continue;
				 StringTokenizer stk = new StringTokenizer(str);
				 for (int i=0;i<index;i++)
					 stk.nextToken();
				 valueSet.add(Double.parseDouble(stk.nextToken()));
			 }
			 br.close();
		 }catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		 double entropy = Statistics.entropy(valueSet.toArray());
		 System.out.println("Entropy: "+entropy);
	 }
	 
	 //public void debug(){
	 	 /**
	 	TRECQuery queries = new TRECQuery();
	 	double[] ql = new double[queries.getNumberOfQueries()];
	 	int counter = 0;
	 	while (queries.hasMoreQueries()){
	 		String query = queries.nextQuery();
	 		StringTokenizer stk = new StringTokenizer(query);
	 		int length = stk.countTokens();
	 		System.out.println(queries.getQueryId() + ": " + length);
	 		ql[counter++] = length;
	 	}
	 	System.out.println("mean ql: " + Statistics.mean(ql));
	 	*/
//	 	QuerySimulation simulation = new QuerySimulation();
//	 	for (int i = 0; i < 50; i++){
//	 		BasicQuery query = simulation.twoStepSimulation(2, 5);
//	 		query.dumpQuery();
//	 	}
	 //}
	 
	 public void createQrels(){
		 // load reference file
		System.out.println("Loading reference documents...");
		THashSet refDocnosList = new THashSet();
		File refFile = new File(ApplicationSetup.getProperty("reference.file", ""));
		double ref_threshold = Double.parseDouble(ApplicationSetup.getProperty("reference.threshold", "15"));
		Index index = Index.createIndex();
		DocumentIndex docIndex = index.getDocumentIndex();
		try{
			BufferedReader br = new BufferedReader(new FileReader(refFile));
			String str = null;
			
			
			while ((str=br.readLine())!=null){
				str = str.trim();
				if (str.length()==0)
					continue;
				StringTokenizer stk = new StringTokenizer(str);
				double score = Double.parseDouble(stk.nextToken());
				stk.nextToken();
				int docid = Integer.parseInt(stk.nextToken());
				
				if (score < ref_threshold)
					continue;
				String docno = docIndex.getDocumentNumber(docid);
				if (docno!=null)
					refDocnosList.add(docno);
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		index.close();
		System.out.println("Loaded "+refDocnosList.size()+" reference documents.");
		String[] docnos = (String[])refDocnosList.toArray(new String[refDocnosList.size()]);
		int size = docnos.length;
		TRECQuery queries = new TRECQuery();
		try{
			BufferedWriter bw = new BufferedWriter(new FileWriter(
					new File(ApplicationSetup.TREC_RESULTS,ApplicationSetup.TERRIER_INDEX_PREFIX+".qrels")));
			while (queries.hasMoreQueries()){
				queries.nextQuery();
				String queryid = queries.getQueryId();
				for (int i=0; i<size; i++)
					bw.write(queryid+" 0 "+docnos[i]+" 1"+EOL);
			}
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	 }
	 
	 public void computeEliteness(String resultFilename){
		 
//		 load reference file
			System.out.println("Loading reference documents...");
			//THashSet refDocnosList = new THashSet();
			File refFile = new File(ApplicationSetup.getProperty("reference.file", ""));
			double ref_threshold = Double.parseDouble(ApplicationSetup.getProperty("reference.threshold", "15"));
			Index index = Index.createIndex();
			DocumentIndex docIndex = index.getDocumentIndex();
			TObjectDoubleHashMap docnoScoreMap = new TObjectDoubleHashMap();
			THashSet eliteDocnos = new THashSet();
			try{
				BufferedReader br = new BufferedReader(new FileReader(refFile));
				String str = null;
				
				
				while ((str=br.readLine())!=null){
					str = str.trim();
					if (str.length()==0)
						continue;
					StringTokenizer stk = new StringTokenizer(str);
					double score = Double.parseDouble(stk.nextToken());
					stk.nextToken();
					int docid = Integer.parseInt(stk.nextToken());
					
					if (score < ref_threshold)
						continue;
					String docno = docIndex.getDocumentNumber(docid);
					if (docno!=null){
						docnoScoreMap.put(docno, score);
						eliteDocnos.add(docno);
					}
				}
				br.close();
			}catch(IOException ioe){
				ioe.printStackTrace();
				System.exit(1);
			}
			index.close();
			System.out.println("Loaded "+docnoScoreMap.size()+" reference documents.");
			
			// load the result file
			TRECResult results = new TRECResult(resultFilename); 
			String[] queryids = results.getQueryids();
			Arrays.sort(queryids);
			int numberOfQueries = queryids.length;
			double[] eliteness = new double[numberOfQueries];
			Arrays.fill(eliteness, 0d);
			for (int i=0; i<numberOfQueries; i++){
				String[] docnos = results.getRankedDocnos(queryids[i]);
				int numberOfDocs = docnos.length;
				for (int j=0; j<numberOfDocs; j++){
					if (!eliteDocnos.contains(docnos[j]))
						continue;
					//double score = docnoScoreMap.get(docnos[j])/(j+1);
					else
					eliteness[i] += 1d/(j+1);
				}
			}
			int counter = 0;
			File fOut = new File(ApplicationSetup.TREC_RESULTS, "elite"+counter++);
			while (fOut.exists()){
				fOut = new File(ApplicationSetup.TREC_RESULTS, "elite"+counter++);
			}
			try{
				BufferedWriter bw = new BufferedWriter(new FileWriter(fOut));
				for (int i=0; i<numberOfQueries; i++)
					bw.write(queryids[i]+" "+eliteness[i]+EOL);
				bw.close();
			}catch(IOException ioe){
				ioe.printStackTrace();
				System.exit(1);
			}
	 }
	 
	 public void weighDocuments(double numberOfTokens){
		 QueryExpansionModel qemodel = null;
		 String modelName = "uk.ac.gla.terrier.matching.models.queryexpansion.Bo1";
		 try{
			 qemodel = (QueryExpansionModel) Class.forName(modelName).newInstance();
		 }
		 catch(Exception e)
		 {
			System.err.println("Problem with postprocess named: "+modelName+" : "+e);
			e.printStackTrace();
			System.exit(1);
		 }
		 
		 
		 Index index = Index.createIndex();
		 File fOut = new File(ApplicationSetup.TREC_RESULTS, ApplicationSetup.TERRIER_INDEX_PREFIX+".dw");
		 DocumentIndex docIndex = index.getDocumentIndex();
		 DirectIndex directIndex = index.getDirectIndex();
		 String lexFilename = ApplicationSetup.getProperty("global.lexicon.filename", "");
		 Lexicon lexicon = new Lexicon(lexFilename);
		 
		 long N = docIndex.getNumberOfDocuments();
		 qemodel.setCollectionLength(numberOfTokens);
		 qemodel.setNumberOfDocuments(N);
		 qemodel.setAverageDocumentLength(numberOfTokens/N);
		 
		 double[] scores = new double[(int)N];
		 int[] docids = new int[(int)N];
		 short[] dummy = new short[(int)N];
		 Arrays.fill(dummy, (short)1);
		 
		 for (int i=0; i<N; i++){
			 docids[i] = i;
			 int[][] terms = directIndex.getTerms(i);
			 int docLength = docIndex.getDocumentLength(i);
			 if (terms==null)
				 scores[i] = 0;
			 else{
				 int n = terms[0].length;
				 double[] weights = new double[n];
				 qemodel.setTotalDocumentLength(docLength);
				 
				 for (int j=0; j<n; j++){
					 lexicon.findTerm(terms[0][j]);
					 weights[j] = qemodel.score(terms[1][j], lexicon.getTF());
				 }
				 scores[i] = Statistics.cosineNormalisation(weights);
				 weights = null;
			 }
			 System.out.println("docid: "+docids[i]+", score: "+scores[i]);
		 }
		 
		 HeapSort.descendingHeapSort(scores, docids, dummy);
		 
		 try{
			 BufferedWriter bw = new BufferedWriter(new FileWriter(fOut));
			 for (int i=0; i<N; i++)
				 bw.write(scores[i]+" "+ dummy[i]+" "+ docids[i]+EOL);
			 bw.close();
		 }catch(IOException ioe){
			 ioe.printStackTrace();
			 System.exit(1);
		 }
		 
		 index.close();
	 }
	 
	 public void processOptimisationOutput(String measure, String outputFilename){
		 try{
			 BufferedReader br = new BufferedReader(new FileReader(outputFilename));
			 String str = null;
			 double[] parameter = new double[10000];
			 double[] value = new double[10000];
			 int pCounter = 0;
			 int vCounter = 0;
			 int max = 0;
			 while ((str=br.readLine())!=null){
				 if (str.trim().length()==0)
					 continue;
				 if (str.startsWith(measure.toUpperCase())||str.startsWith(measure.toLowerCase())){
					 StringTokenizer stk = new StringTokenizer(str);
					 stk.nextToken();
					 stk.nextToken();
					 value[vCounter++] = Double.parseDouble(stk.nextToken());
					 if (value[max] < value[vCounter-1])
						 max = vCounter-1;
				 }else{
					 StringTokenizer stk = new StringTokenizer(str);
					 String tmpStr = stk.nextToken();
					 parameter[pCounter++] = Double.parseDouble(
							 tmpStr.substring(tmpStr.indexOf('=')+1));
				 }
			 }
			 br.close();
			 System.out.println(outputFilename+" parameter: "+parameter[max]+", measure: "+value[max]);
		 }catch(IOException ioe){
			 ioe.printStackTrace();
			 System.exit(1);
		 }
	 }
	 
	 public void getTFLengthCorrelation(String filename, double c, String method){
		 Normalisation norm = null;
		 try{
			 String methodName = "uk.ac.gla.terrier.matching.models.normalisation.Normalisation"+method;
			 norm = (Normalisation)Class.forName(methodName).newInstance();
		 }catch(Exception e){
			 e.printStackTrace();
			 System.exit(1);
		 }
		 
		 // load docnos
		 THashMap qidDocnosMap = new THashMap(); 
		 THashSet idsSet = new THashSet();
		 try{
			 BufferedReader br = new BufferedReader(new FileReader(filename));
			 String str = null;
			 THashSet docnosSet = new THashSet();
			 String id = null;
			 while ((str=br.readLine())!=null){
				 str = str.trim();
				 if (str.length()==0)
					 continue;
				 if (str.startsWith("id")){
					 if (docnosSet.size()!=0){
						 String[] docnos = (String[])docnosSet.toArray(new String[docnosSet.size()]);
						 qidDocnosMap.put(id, docnos);
						 docnosSet.clear();
						 docnosSet = new THashSet();
						 idsSet.add(id);
					 }
					 StringTokenizer stk = new StringTokenizer(str);
					 stk.nextToken();
					 id = stk.nextToken();
				 }else
					 docnosSet.add(str);
			 }
			 br.close();
		 }catch(IOException ioe){
			 ioe.printStackTrace();
			 System.exit(1);
		 }
		 
		 // get doclength
		 THashMap qidLengthMap = new THashMap();
		 Index index = Index.createIndex();
		 DocumentIndex docIndex = index.getDocumentIndex();
		 String[] ids = (String[])idsSet.toArray(new String[idsSet.size()]);
		 for (int i=0; i<ids.length; i++){
			 String[] docnos = (String[])qidDocnosMap.get(ids[i]);
			 double[] docLength = new double[docnos.length];
			 for (int j=0; j<docnos.length; j++){
				 docLength[j] = docIndex.getDocumentLength(docnos[j]);
			 }
			 qidLengthMap.put(ids[i], docLength);
		 }
		 
		 // normalise
		 norm.setParameter(c);
		 THashMap qidtfnMap = new THashMap();
		 for (int i=0; i<ids.length; i++){
			 double[] docLength = (double[])qidLengthMap.get(ids[i]);
			 double[] tfn = new double[docLength.length];
			 for (int j=0; j<tfn.length; j++){
				 //tfn[j] = norm.normalise(tf, docLength, termFrequency);
			 }
			 qidLengthMap.put(ids[i], docLength);
		 }
		 
		 // compute correlation
		 
	 }
	 
	 public void createGlobalLex(
			 String path1, String prefix1,
			 String path2, String prefix2,
			 String path3, String prefix3,
			 String rawLexFilename,
			 String outputFilename
	 ){
		 try{
			 System.out.println(path1+" "+prefix1);
			 System.out.println(path2+" "+prefix2);
			 System.out.println(path3+" "+prefix3);
			 
			 InvertedIndex invIndex1 = new InvertedIndex(
					 new Lexicon(path1+"/"+prefix1+".lex"), path1+"/"+prefix1+".if");
			 InvertedIndex invIndex2 = new InvertedIndex(
					 new Lexicon(path2+"/"+prefix2+".lex"),
					 path2+"/"+prefix2+".if");
			 InvertedIndex invIndex3 = new InvertedIndex(
					 new Lexicon(path3+"/"+prefix3+".lex"),
					 path3+"/"+prefix3+".if");
			 
			 LexiconInputStream lex1 = new LexiconInputStream(path1+"/"+prefix1+".lex");
			 LexiconInputStream lex2 = new LexiconInputStream(path2+"/"+prefix2+".lex");
			 LexiconInputStream lex3 = new LexiconInputStream(path3+"/"+prefix3+".lex");
			 
			 
			 LexiconInputStream globalLex = new LexiconInputStream(rawLexFilename);
			 LexiconOutputStream lex = new LexiconOutputStream(outputFilename);
			 String term1 = "";
			 String term2 = "";
			 String term3 = "";
			 while (globalLex.readNextEntry()>=0){
				 TIntHashSet docids = new TIntHashSet();
				 String term = globalLex.getTerm();
				 if (term.compareTo(term1)>0){
					 while (lex1.readNextEntry()>=0){
						 term1 = lex1.getTerm();
						 if (term.compareTo(term1)>0)
							 continue;
						 else if (term.equals(term1)){
							 //a=1;
							 int[][] postings = invIndex1.getDocuments(lex1.getTermId());
							 docids.addAll(postings[0]);
						 }else{
							 break;
						 }
					 }
				 }else if (term.equals(term1)){
					 int[][] postings = invIndex1.getDocuments(lex1.getTermId());
					 docids.addAll(postings[0]);
				 }
				 if (term.compareTo(term2)>0){
					 while (lex2.readNextEntry()>=0){
						 term2 = lex2.getTerm();
						 if (term.compareTo(term2)>0)
							 continue;
						 else if (term.equals(term2)){
							 int[][] postings = invIndex2.getDocuments(lex2.getTermId());
							 docids.addAll(postings[0]);
						 }else{
							 break;
						 }
					 }
				 }else if (term.equals(term2)){
					 int[][] postings = invIndex2.getDocuments(lex2.getTermId());
					 docids.addAll(postings[0]);
				 }
				 if (term.compareTo(term3)>0){
					 while (lex3.readNextEntry()>=0){
						 term3 = lex3.getTerm();
						 if (term.compareTo(term3)>0)
							 continue;
						 else if (term.equals(term3)){
							 int[][] postings = invIndex3.getDocuments(lex3.getTermId());
							 docids.addAll(postings[0]);
						 }else{
							 break;
						 }
					 }
				 }else if (term.equals(term3)){
					 int[][] postings = invIndex3.getDocuments(lex3.getTermId());
					 docids.addAll(postings[0]);
				 }
				 lex.writeNextEntry(term, globalLex.getTermId(), docids.size(), 
						 globalLex.getTF(), 0L, (byte)0);
			 }
			 globalLex.close();
			 lex.close();
			 lex1.close();
			 lex2.close();
			 lex3.close();
			 invIndex1.close();
			 invIndex2.close();
			 invIndex3.close();
		 }catch(IOException ioe){
			 ioe.printStackTrace();
			 System.exit(1);
		 }
	 }
	 
	 public void createGlobalLex(
			 String[] path, String[] prefix,
			 String rawLexFilename,
			 String outputFilename
	 ){
		 try{
			 System.out.println(rawLexFilename+ ", "+outputFilename);
			 InvertedIndex[] invIndex = new InvertedIndex[path.length];
			 LexiconInputStream[] lexin = new LexiconInputStream[path.length];
			 for (int i=0; i<path.length; i++){
				 System.out.println(path[i]+" "+prefix[i]);
				 invIndex[i] = new InvertedIndex(
						 new Lexicon(path[i]+"/"+prefix[i]+".lex"),
						 path[i]+"/"+prefix[i]+".if"
						 );
				 lexin[i] = new LexiconInputStream(path[i]+"/"+prefix[i]+".lex");
			 }
			 
			 
			 LexiconInputStream globalLex = new LexiconInputStream(rawLexFilename);
			 LexiconOutputStream lex = new LexiconOutputStream(outputFilename);
			 String[] terms = new String[path.length];
			 Arrays.fill(terms, "");
			 
			 while (globalLex.readNextEntry()>=0){
				 TIntHashSet docids = new TIntHashSet();
				 int TF = 0;
				 String term = globalLex.getTerm();
				 for (int i=0; i<path.length; i++){
					 if (term.compareTo(terms[i])>0){
						 while (lexin[i].readNextEntry()>=0){
							 terms[i] = lexin[i].getTerm();
							 if (term.compareTo(terms[i])>0)
								 continue;
							 else if (term.equals(terms[i])){
								 //a=1;
								 int[][] postings = invIndex[i].getDocuments(lexin[i].getTermId());
								 docids.addAll(postings[0]);
								 TF += lexin[i].getTF();
								 break;
							 }else{
								 break;
							 }
						 }
					 }else if (term.equals(terms[i])){
						 int[][] postings = invIndex[i].getDocuments(lexin[i].getTermId());
						 docids.addAll(postings[0]);
						 TF += lexin[i].getTF();
						 break;
					 }
				 }
				 if (globalLex.getTF() != TF)
					 System.err.println(globalLex.getTerm()+": globalTF: "+globalLex.getTF()+", TF: "+TF);
				 lex.writeNextEntry(term, globalLex.getTermId(), docids.size(), 
						 globalLex.getTF(), 0L, (byte)0);
			 }
			 globalLex.close();
			 lex.close();
			 for (int i=0; i<path.length; i++){
				 lexin[i].close();
				 invIndex[i].close();
			 }
		 }catch(IOException ioe){
			 ioe.printStackTrace();
			 System.exit(1);
		 }
	 }
	 
	 public void createGlobalLexSlow(
			 String[] path, String[] prefix,
			 String rawLexFilename,
			 String outputFilename
	 ){
		 try{
			 System.out.println(rawLexFilename+ ", "+outputFilename);
			 InvertedIndex[] invIndex = new InvertedIndex[path.length];
			 Lexicon[] lexin = new Lexicon[path.length];
			 for (int i=0; i<path.length; i++){
				 System.out.println(path[i]+" "+prefix[i]);
				 invIndex[i] = new InvertedIndex(
						 new Lexicon(path[i]+"/"+prefix[i]+".lex"),
						 path[i]+"/"+prefix[i]+".if"
						 );
				 lexin[i] = new Lexicon(path[i]+"/"+prefix[i]+".lex");
			 }
			 
			 
			 LexiconInputStream globalLex = new LexiconInputStream(rawLexFilename);
			 LexiconOutputStream lex = new LexiconOutputStream(outputFilename);
			 String[] terms = new String[path.length];
			 Arrays.fill(terms, "");
			 
			 while (globalLex.readNextEntry()>=0){
				 TIntHashSet docids = new TIntHashSet();
				 int TF = 0;
				 String term = globalLex.getTerm();
				 for (int i=0; i<path.length; i++){
					 if (lexin[i].findTerm(term)){
						 int[][] postings = invIndex[i].getDocuments(lexin[i].getTermId());
						 docids.addAll(postings[0]);
						 TF += lexin[i].getTF();
					 }
				 }
				 if (globalLex.getTF() != TF)
					 System.err.println(globalLex.getTerm()+": globalTF: "+globalLex.getTF()+", TF: "+TF);
				 lex.writeNextEntry(term, globalLex.getTermId(), docids.size(), 
						 globalLex.getTF(), 0L, (byte)0);
			 }
			 globalLex.close();
			 lex.close();
			 for (int i=0; i<path.length; i++){
				 lexin[i].close();
				 invIndex[i].close();
			 }
		 }catch(IOException ioe){
			 ioe.printStackTrace();
			 System.exit(1);
		 }
	 }
	 
	 public void dumpLexicon(String filename){
		 try{
			 LexiconInputStream lex = new LexiconInputStream(filename);
			 while (lex.readNextEntry()>=0){
				 System.out.println(lex.getTerm()+": "+lex.getNt()+", "+lex.getTF());
			 }
			 lex.close();
		 }catch(IOException e){
			 e.printStackTrace();
			 System.exit(1);
		 }
	 }
	 
	 public void debug(String[] args){
		 
		 
		 /*try{
			 String filename = "/local/terrier/Collections/genomics/2006/documents/temp/jbc-2000/10799482.html";
			 FileInputStream fin = new FileInputStream(filename);
			 int len = 313;
			 int offset = 13380;
			 char[] str = new char[len];
			 //fin.skip(offset);
			 InputStreamReader ipr = new InputStreamReader(fin);
			 ipr.skip(offset);
			 for (int i=0; i<len; i++)
				 str[i] = (char)ipr.read(); 
			 //ipr.read(str, offset, len);
			 //fin.read(str, offset, len);
			 String sentence = new String(str);
			 System.out.println(sentence);
			 *//**
			 BufferedReader br = new BufferedReader(new InputStreamReader(fin));
			 String str = null;
			 while ((str=br.readLine())!=null)
				 System.out.println(str);
				 *//*
			 fin.close();
			 //br.close();
		 }catch(IOException e){
			 e.printStackTrace();
			 System.exit(1);
		 }*/
		 
		 /*Pattern sentencePattern = Pattern.compile("\\.\\s+|!\\s+|\\|\\s+|\\?\\s+");
		 try{
			 BufferedReader br = Files.openFileReader("/users/tr.ben/medlive/BROKER_TERRIER/var/index/data.extract-text.gz");
			 String text = br.readLine();
			 String[] sentences = sentencePattern.split(text, 50);
			 for (int i=0; i<sentences.length; i++)
				 System.out.println((i+1)+": "+sentences[i]);
			 br.close();
		 }catch(IOException e){
			 e.printStackTrace();
			 System.exit(1);
		 }*/
		 
		 /*String[] cmd = {"/usr/local/bin/bash", "-c", "head /users/grad/ben/Downloads/proxy.pac | tail -1"};
		 try{
			 Process p = Runtime.getRuntime().exec(cmd);
			 String line = null;
			 BufferedReader input =
			       new BufferedReader
			         (new InputStreamReader(p.getInputStream()));
			 int counter = 0;
			   while ((line = input.readLine()) != null) {
				   counter++;
			       System.out.println(line);
			   }
			 input.close();
			 System.out.println("counter: "+counter);
			 input =
			       new BufferedReader
			         (new InputStreamReader(p.getErrorStream()));
			 counter = 0;
			   while ((line = input.readLine()) != null) {
				   counter++;
			       System.out.println(line);
			   }
			 input.close();
			 System.out.println("counter: "+counter);
		 }catch(IOException e){
			 e.printStackTrace();
		 }*/
		 
		 
		 
		 /*String str = "id s1 s2 s3";
		 String[] tokens = str.split("\\s* \\s*");
		 for (int i=0; i<tokens.length; i++)
			 System.out.println(tokens[i]);*/
		 /*Index index = Index.createIndex();
		 String filename = ApplicationSetup.TERRIER_HOME+
		 		ApplicationSetup.FILE_SEPARATOR+"list";
		 long sumLength = 0;
		 int docCounter = 0;
		 try{
			 CollectionStatistics collSta = new CollectionStatistics();
			 BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
			 DocumentIndex docIndex = index.getDocumentIndex();
			 String str = null;
			 while ((str=br.readLine())!=null){
				 str = str.trim();
				 if (str.length()==0) continue;
				 docCounter++;
				 sumLength += docIndex.getDocumentLength(str);
			 }
			 
			 System.out.println(collSta.getNumberOfTokens() - sumLength);
			 System.out.println(collSta.getNumberOfDocuments() - docCounter);
			 System.out.println((double)sumLength/collSta.getNumberOfTokens());
			 System.out.println((double)docCounter/collSta.getNumberOfDocuments());
			 br.close();
		 }catch(IOException e){
			 e.printStackTrace();
			 System.exit(1);
		 }
		 
		 index.close();*/
		 
		   /*TRECQrelsInMemory qrels = new TRECQrelsInMemory();
		   String[] queryids = qrels.getQueryids();
		   int[] grades = {1, 2, 3, 4};
		   for (int i=0; i<queryids.length; i++){
			   int num = qrels.getRelevantDocuments(queryids[i]).size();
			   System.out.print(queryids[i]+": "+num);
			   num = 0;
			   for (int j=0; j<grades.length; j++){
				   THashSet docnos = qrels.getRelevantDocuments(queryids[i], grades[j]);
				   if (docnos!=null)
				   num+=docnos.size();
			   }
			   System.out.println(", "+num);
		   }*/
		 
/*	 	 File f = new File (args[1]);
	 	 try{
	 		 BufferedReader br = new BufferedReader(new FileReader(f));
	 		 String str = null;
	 		 double sum1 = 0;
	 		 double sum2 = 0;
	 		 int lines = 0;
	 		 while ((str=br.readLine())!=null){
	 			 if (str.trim().length()==0)
	 				 continue;
	 			 StringTokenizer stk = new StringTokenizer(str);
	 			 sum1+=Double.parseDouble(stk.nextToken());
	 			 sum2+=Double.parseDouble(stk.nextToken());
	 			 lines++;
	 		 }
	 		 br.close();
	 		 System.out.println("avg1: "+sum1/lines);
	 		 System.out.println("avg2: "+sum2/lines);
	 		 System.out.println("diff: "+
	 				 (Math.abs(sum1-sum2)/lines)/Math.min(sum1/lines, sum2/lines));
	 	 }catch(IOException ioe){
	 		 ioe.printStackTrace();
	 		 System.exit(1);
	 	 }
*/	 }
	 
	 public void compactBlockLexicon(String lexFilename, String outputFilename){
		 BlockLexiconInputStream lex = new BlockLexiconInputStream(lexFilename);
		 BlockLexiconOutputStream outLex = new BlockLexiconOutputStream(outputFilename);
		 double N = 25205179d;
		 //double CollF = 17430767689;
		 int counter = 0;
		 int minNt = Integer.parseInt(ApplicationSetup.getProperty(
				 "compact.lexicon.mindf", "10"));
		 double rate = Double.parseDouble(ApplicationSetup.getProperty(
				 "compact.lexicon.rate", "10"));
		 System.out.println("lexicon to compact: "+lexFilename);
		 System.out.println("output lexicon: "+outputFilename);
		 System.out.print("start compacting lexicon...");
		 long ignoredCounter = 0;
		 try{
			 while (lex.readNextEntry()>=0){
				 //System.out.println(lex.getNt()+", "+N/2);
				 if (lex.getNt()<=minNt || (double)lex.getNt()>N/rate){					 
					 ignoredCounter++;
					 continue;
				 }
				 else{
					 String term = lex.getTerm();
					 outLex.writeNextEntry(term, counter++, lex.getNt(), lex.getTF(), lex.getBlockFrequency(), lex.getEndOffset(), lex.getEndBitOffset());
				 }
			 } 
			 System.out.println(ignoredCounter+" terms are ignored. "+ 
					 (double)ignoredCounter*100d/N+". minNt: "+minNt+", rate: "+rate);
			 (new File(outputFilename+"id")).createNewFile();
			 this.createBlockLexiconIndex(outputFilename, outputFilename+"id");
			 
		 }catch (IOException ioe){
			 ioe.printStackTrace();
			 System.exit(1);
		 }
		 System.out.println("Done.");
	 }
	 
	 public void compactLexicon(String lexFilename, String outputFilename){
		 LexiconInputStream lex = new LexiconInputStream(lexFilename);
		 LexiconOutputStream outLex = new LexiconOutputStream(outputFilename);
		 double N = 25205179d;
		 //double CollF = 17430767689;
		 int counter = 0;
		 int minNt = Integer.parseInt(ApplicationSetup.getProperty(
				 "compact.lexicon.mindf", "10"));
		 double rate = Double.parseDouble(ApplicationSetup.getProperty(
				 "compact.lexicon.rate", "10"));
		 System.out.println("lexicon to compact: "+lexFilename);
		 System.out.println("output lexicon: "+outputFilename);
		 System.out.print("start compacting lexicon...");
		 long ignoredCounter = 0;
		 try{
			 while (lex.readNextEntry()>=0){
				 //System.out.println(lex.getNt()+", "+N/2);
				 if (lex.getNt()<=minNt || (double)lex.getNt()>N/rate){					 
					 ignoredCounter++;
					 continue;
				 }
				 else{
					 String term = lex.getTerm();
					 outLex.writeNextEntry(term, counter++, lex.getNt(), lex.getTF(), lex.getEndOffset(), lex.getEndBitOffset());
				 }
			 } 
			 System.out.println(ignoredCounter+" terms are ignored. "+ 
					 (double)ignoredCounter*100d/N+". minNt: "+minNt+", rate: "+rate);
			 (new File(outputFilename+"id")).createNewFile();
			 this.createLexiconIndex(outputFilename, outputFilename+"id");
			 
		 }catch (IOException ioe){
			 ioe.printStackTrace();
			 System.exit(1);
		 }
		 System.out.println("Done.");
	 }
	 
	 public void compactSubLexicon(String globalFilename,
			 String subFilename, String outputFilename){
		 LexiconInputStream globalLex = new LexiconInputStream(globalFilename);
		 LexiconInputStream subLex = new LexiconInputStream(subFilename);
		 LexiconOutputStream outLex = new LexiconOutputStream(outputFilename);
		 //double CollF = 17430767689;
		 int counter = 0;
		 System.out.println("global lexicon: "+globalFilename);
		 System.out.println("lexicon to compact: "+subFilename);
		 System.out.println("output lexicon: "+outputFilename);
		 System.out.print("start compacting lexicon...");
		 int ignoredCounter = 0;
		 long N = (new Lexicon(subFilename)).getNumberOfLexiconEntries();
		 try{
			 String _term = "";
			 while (subLex.readNextEntry()>=0){
				 String term = subLex.getTerm();
				 if (_term.equals(term))
					 outLex.writeNextEntry(term, subLex.getTermId(), subLex.getNt(), subLex.getTF(), subLex.getEndOffset(), subLex.getEndBitOffset());
				 else if (_term.compareTo(term)>0)
					 continue;
				 else
					 while (globalLex.readNextEntry()>=0){
						 _term = globalLex.getTerm();
						 if (_term.compareTo(term)<0)
							 continue;
						 else if (_term.equals(term)){
							 outLex.writeNextEntry(term, subLex.getTermId(), subLex.getNt(), subLex.getTF(), subLex.getEndOffset(), subLex.getEndBitOffset());
							 break;
						 }else {ignoredCounter++; break;}
					 }
			 } 
			 System.out.println("Done. "+ignoredCounter+" terms are ignored. "+ 
					 (double)ignoredCounter*100d/N);
			 (new File(outputFilename+"id")).createNewFile();
			 this.createLexiconIndex(outputFilename, outputFilename+"id");
			 globalLex.close();
			 subLex.close();
			 outLex.close();
			 
		 }catch (IOException ioe){
			 ioe.printStackTrace();
			 System.exit(1);
		 }
		 System.out.println("Done.");
	 }
	 
	 public void compactSubBlockLexicon(String globalFilename,
			 String subFilename, String outputFilename){
		 BlockLexiconInputStream globalLex = new BlockLexiconInputStream(globalFilename);
		 BlockLexiconInputStream subLex = new BlockLexiconInputStream(subFilename);
		 BlockLexiconOutputStream outLex = new BlockLexiconOutputStream(outputFilename);
		 //double CollF = 17430767689;
		 int counter = 0;
		 System.out.println("global lexicon: "+globalFilename);
		 System.out.println("lexicon to compact: "+subFilename);
		 System.out.println("output lexicon: "+outputFilename);
		 System.out.print("start compacting lexicon...");
		 int ignoredCounter = 0;
		 long N = (new BlockLexicon(subFilename)).getNumberOfLexiconEntries();
		 try{
			 String _term = "";
			 while (subLex.readNextEntry()>=0){
				 String term = subLex.getTerm();
				 if (_term.equals(term))
					 outLex.writeNextEntry(term, globalLex.getTermId(), subLex.getNt(), subLex.getTF(), subLex.getBlockFrequency(), subLex.getEndOffset(), subLex.getEndBitOffset());
				 else if (_term.compareTo(term)>0)
					 continue;
				 else
					 while (globalLex.readNextEntry()>=0){
						 _term = globalLex.getTerm();
						 if (_term.compareTo(term)<0)
							 continue;
						 else if (_term.equals(term)){
							 outLex.writeNextEntry(term, globalLex.getTermId(), subLex.getNt(), subLex.getTF(), subLex.getEndOffset(), subLex.getEndBitOffset());
							 break;
						 }else {ignoredCounter++; break;}
					 }
			 } 
			 System.out.println("Done. "+ignoredCounter+" terms are ignored. "+ 
					 (double)ignoredCounter*100d/N);
			 (new File(outputFilename+"id")).createNewFile();
			 this.createBlockLexiconIndex(outputFilename, outputFilename+"id");
			 globalLex.close();
			 subLex.close();
			 outLex.close();
			 
		 }catch (IOException ioe){
			 ioe.printStackTrace();
			 System.exit(1);
		 }
		 System.out.println("Done.");
	 }
	 
	 public void rebuildLexicon(String lexFilename, String outFilename){
         try{
        	 LexiconInputStream lex = new LexiconInputStream(lexFilename);
             LexiconOutputStream outLex = new LexiconOutputStream(outFilename);
             System.out.print("Rebuilding lexicon file "+lexFilename+"...");
             int counter = 0;
             while (lex.readNextEntry()>=0){
            	 outLex.writeNextEntry(lex.getTerm(), counter++, lex.getNt(), lex.getTF(), lex.getEndOffset(), lex.getEndBitOffset());
             }
             lex.close();
             outLex.close();
             System.out.println("Done. New lexicon written in file "+outFilename+".");
             System.out.print("Creating lexicon index...");
             this.createLexiconIndex(outFilename, outFilename+"id");
             System.out.println("Done.");
         }catch (IOException ioe){
             ioe.printStackTrace();
             System.exit(1);
         }
	 }
	 
	 public void rebuildBlockLexicon(String lexFilename, String outFilename){
         try{
        	 BlockLexiconInputStream lex = new BlockLexiconInputStream(lexFilename);
             BlockLexiconOutputStream outLex = new BlockLexiconOutputStream(outFilename);
             System.out.print("Rebuilding lexicon file "+lexFilename+"...");
             int counter = 0;
             while (lex.readNextEntry()>=0){
                     outLex.writeNextEntry(lex.getTerm(), counter++, lex.getNt(), lex.getTF(),
                    		 lex.getBlockFrequency(), lex.getEndOffset(), lex.getEndBitOffset());
             }
             lex.close();
             outLex.close();
             System.out.println("Done. New lexicon written in file "+outFilename+".");
             System.out.print("Creating lexicon index...");
             this.createBlockLexiconIndex(outFilename, outFilename+"id");
             System.out.println("Done.");
         }catch (IOException ioe){
             ioe.printStackTrace();
             System.exit(1);
         }
	 }
	 
	 // alter the termids in the direct file according to a given lexicon. The direct file is 
	 // specified by the properties.
	 public void alignBlockDirectIndexTermid(
			 String alignedLexFilename,
			 String outputFilename){
		 int tags = FieldScore.FIELDS_COUNT;
		 LexiconInputStream alignedLex = new LexiconInputStream(alignedLexFilename);
		 BitFile outFile = new BitFile(outputFilename, "rw"); 
		 int numberOfEntries = (int)(new Lexicon(alignedLexFilename)).getNumberOfLexiconEntries();
		 int[] termidMap = new int[numberOfEntries];
		 int counter = 0;
		 // map the natual termids (i.e. 0, 1, 2...) onto the actual termids in the 
		 // given lexicon. 
		 try{
			 while (alignedLex.readNextEntry()>=0)
				 termidMap[counter++] = alignedLex.getTermId();
		 }catch(IOException ioe){
			 ioe.printStackTrace();
			 System.exit(1);
		 }
		 
		 BlockDirectIndexInputStream directIndex = new BlockDirectIndexInputStream();
			 
		 try{
			 outFile.writeReset();
			 int[][] terms = null;
			 int docCounter = 0;
			 int docPerBuffer = 100;
			 while ((terms=directIndex.getNextTerms())!=null){
				 int blockCounter = 0;
				 int numberOfTerms = terms[0].length;
				 int termid = termidMap[terms[0][0]];
				 int tf = terms[1][0];
				 outFile.writeGamma(termid+1);
				 outFile.writeUnary(tf);
				 outFile.writeBinary(tags, terms[2][0]);
				 int blockFreq = terms[3][0];
				 outFile.writeUnary(blockFreq);
				 //
				 outFile.writeGamma(terms[4][0]+1);
				 blockCounter++;
				 for (int i=1; i<blockFreq; i++){
					 outFile.writeGamma(terms[4][i]-terms[4][i-1]);
					 blockCounter++;
				 }
				 
				 int previousTermid = termid;
				 for (int i=1; i<numberOfTerms; i++){
					//
					 try{
						 termid = termidMap[terms[0][i]];
					 }catch(ArrayIndexOutOfBoundsException e){
						 e.printStackTrace();
						 System.exit(1);
					 }
					 outFile.writeGamma(termid-previousTermid);
					 outFile.writeUnary(terms[1][i]);
					 outFile.writeBinary(tags, terms[2][i]);
					 blockFreq = terms[3][i];
					 outFile.writeUnary(blockFreq);
					 try{
						 outFile.writeGamma(terms[4][blockCounter]+1);
						 blockCounter++;
					 }catch(NullPointerException e){
						 System.err.println("blockCounter: "+blockCounter+
								 ", numberOfTerms: "+numberOfTerms+", terms[4].length: "+
								 terms[4].length+", i: "+i+", "+terms[4][blockCounter]);
						 /*for (int j=0; j<terms[4].length; j++){
							 System.out.println(terms[4][j]);
						 }*/
						 e.printStackTrace();
						 System.exit(1);
					 }
					 for (int j=1; j<blockFreq; j++){
						 outFile.writeGamma(terms[4][blockCounter]-terms[4][blockCounter-1]);
						 blockCounter++;
					 }
					 previousTermid = termid;
				 }
				 docCounter++;
				 if (docCounter>=docPerBuffer){
					 outFile.writeFlush();
					 outFile.writeReset();
					 docCounter = 0;
				 }
			 }		
			 outFile.writeFlush();
			 outFile.writeReset();
		 }catch(IOException ioe){
			 ioe.printStackTrace();
			 System.exit(1);
		 }
		 directIndex.close();
		 outFile.close();
	 }
	 
	 public void alignBlockInvertedIndexTermid(
			 String rawLexFilename,
			 String alignedLexFilename, 
			 String invIndexFilename,
			 String outputFilename){
		 // for every term in the lexicon, get the posting from a mapped natual termid and
		 // write the postings.
		 LexiconInputStream lex = new LexiconInputStream(alignedLexFilename);
		 try{
			 int numberOfTerms = (int)(new Lexicon(alignedLexFilename)).getNumberOfLexiconEntries();
			 int rawTermid = 0;
			 BitOutputStream bitOutput = new BitOutputStream(outputFilename);
			 BlockInvertedIndex invIndex = new BlockInvertedIndex(new Lexicon(rawLexFilename), invIndexFilename);
			 while (lex.readNextEntry()>=0){
				 int[][] docs = invIndex.getDocuments(rawTermid);
				 rawTermid++;
			 }
			 lex.close();
			 bitOutput.close();
		 }catch(IOException ioe){
			 ioe.printStackTrace();
			 System.exit(1);
		 }
		 
	 }
	 
	 protected void writeFieldPostings(int[][] postings, int firstId, BitOutputStream output)
			throws IOException {
		 int tags = FieldScore.FIELDS_COUNT;
	

		 //local variables in order to reduce the number
		 //of times we need to access a two-dimensional array
		 final int[] postings0 = postings[0];
		 final int[] postings1 = postings[1];
		 final int[] postings2 = postings[2];
		 final int[] postings3 = postings[3];
		final int[] postings4 = postings[4];
		
		//write the first posting from the term's postings list
		output.writeGamma(firstId);						//write document id 
		output.writeUnary(postings1[0]);    			//write frequency
		output.writeBinary(tags, postings2[0]);	//write fields if binaryBits>0
		int blockIndex = 0;								//the index of the current block id
		int blockFrequency = postings3[0];				//the number of block ids to write
		output.writeUnary(blockFrequency);    			//write block frequency
		output.writeGamma(postings4[blockIndex]+1);	//write the first block id
		blockIndex++;									//move to the next block id
		for (int i=1; i<blockFrequency; i++) {			//write the next blockFrequency-1 ids
			//write the gap between consequtive block ids
			output.writeGamma(postings4[blockIndex]-postings4[blockIndex-1]);
			blockIndex++;
		}
		
		//write the rest of the postings from the term's postings list
		final int length = postings[0].length;
		for (int k = 1; k < length; k++) {
			output.writeGamma(postings0[k] - postings0[k - 1]);	//write gap of document ids
			output.writeUnary(postings1[k]);					//write term frequency
			output.writeBinary(tags, postings2[k]);		//write fields if binaryBits>0
			blockFrequency = postings3[k];						//number of block ids to write
			output.writeUnary(blockFrequency);					//write block frequency
			output.writeGamma(postings4[blockIndex]+1);			//write the first block id
			blockIndex++;										//move to the next block id
			for (int i=1; i<blockFrequency; i++) {
				//write the gap between consequtive block ids
				output.writeGamma(postings4[blockIndex]-postings4[blockIndex-1]);
				blockIndex++;
			}
		}
	 }
	 
	 public void checkLex(String globalFilename, String subFilename){
		 Lexicon globalLex = new Lexicon(globalFilename);
		 LexiconInputStream lex = new LexiconInputStream(subFilename);
		 System.out.println("global lexicon: "+globalFilename);
		 System.out.println("sub lexicon: "+subFilename);
		 StringBuffer buffer = new StringBuffer();
		 int counter = 0;
		 System.out.print("start integrity check...");
		 try{
			 while (lex.readNextEntry()>=0){
				 //System.out.print("checking term "+globalLex.getTerm()+" with id "+globalLex.getTermId()+"...");
				 if (globalLex.findTerm(lex.getTermId())){
					 if (!globalLex.getTerm().trim().equals(lex.getTerm().trim())){
						 System.out.println(globalLex.getTerm()+" "+lex.getTerm());
						 buffer.append(lex.getTerm()+" ");
						 counter++;
					 }
					 else {
						 //System.out.println("Verified!");
					 }
				 }
				 else{
					 System.out.println(lex.getTerm()+" not found in global lexicon.");
					 buffer.append(lex.getTerm()+" ");
					 counter++;
				 }
			 }
		 }catch (IOException ioe){
			 ioe.printStackTrace();
			 System.exit(1);
		 }
		 System.out.println("Done.");
		 System.out.println("Unmatched terms: "+buffer.toString());
		 System.out.println(counter+" terms in total.");
	 }
	 
	 public void checkBlockLex(String globalFilename, String subFilename){
		 BlockLexicon globalLex = new BlockLexicon(globalFilename);
		 BlockLexiconInputStream lex = new BlockLexiconInputStream(subFilename);
		 System.out.println("global lexicon: "+globalFilename);
		 System.out.println("sub lexicon: "+subFilename);
		 StringBuffer buffer = new StringBuffer();
		 int counter = 0;
		 System.out.print("start integrity check...");
		 try{
			 while (lex.readNextEntry()>=0){
				 //System.out.print("checking term "+globalLex.getTerm()+" with id "+globalLex.getTermId()+"...");
				 if (globalLex.findTerm(lex.getTermId())){
					 if (!globalLex.getTerm().trim().equals(lex.getTerm().trim())){
						 System.out.println(globalLex.getTerm()+" "+lex.getTerm());
						 buffer.append(lex.getTerm()+" ");
						 counter++;
					 }
					 else {
						 //System.out.println("Verified!");
					 }
				 }
				 else{
					 System.out.println(lex.getTerm()+" not found in global lexicon.");
					 buffer.append(lex.getTerm()+" ");
					 counter++;
				 }
			 }
		 }catch (IOException ioe){
			 ioe.printStackTrace();
			 System.exit(1);
		 }
		 System.out.println("Done.");
		 System.out.println("Unmatched terms: "+buffer.toString());
		 System.out.println(counter+" terms in total.");
	 }
	 
	 public void inLexicon(String queryTermFilename, String lexFilename){
		 THashSet termSet = new THashSet();
		 try{
			 BufferedReader br = new BufferedReader(new FileReader(new File(queryTermFilename)));
			 String str = null;
			 while ((str=br.readLine())!=null){
				 if (str.trim().length()==0)
					 continue;
				 termSet.add(str.trim());
			 }
			 br.close();
			 String[] terms = (String[])termSet.toArray(new String[termSet.size()]);
			 int  n = terms.length;
			 Arrays.sort(terms);
			 Lexicon lex = new Lexicon(lexFilename);
			 int match = 0;
			 for (int i=0; i<n; i++){
				 if (lex.findTerm(terms[i]))
					 match++;
			 }
			 lex.close();
			 System.out.println(match+" out of "+n+" terms are found. ratio: "+(double)match/n);
			 
		 }catch (IOException ioe){
			 ioe.printStackTrace();
			 System.exit(1);
		 }
	 }
	 
	 public void inBlockLexicon(String queryTermFilename, String lexFilename){
		 THashSet termSet = new THashSet();
		 try{
			 BufferedReader br = new BufferedReader(new FileReader(new File(queryTermFilename)));
			 String str = null;
			 while ((str=br.readLine())!=null){
				 if (str.trim().length()==0)
					 continue;
				 termSet.add(str.trim());
			 }
			 br.close();
			 String[] terms = (String[])termSet.toArray(new String[termSet.size()]);
			 int  n = terms.length;
			 Arrays.sort(terms);
			 BlockLexicon lex = new BlockLexicon(lexFilename);
			 int match = 0;
			 for (int i=0; i<n; i++){
				 if (lex.findTerm(terms[i]))
					 match++;
			 }
			 lex.close();
			 System.out.println(match+" out of "+n+" terms are found. ratio: "+(double)match/n);
			 
		 }catch (IOException ioe){
			 ioe.printStackTrace();
			 System.exit(1);
		 }
	 }
	 
	 public void createBlockLexiconIndex(String lexFilename, String outputFilename) throws IOException {
			/*
			 * This method reads from the lexicon the term ids and stores the
			 * corresponding offsets in an array. Then this array is sorted 
			 * according to the term id.
			 */
			
			//TODO use the class LexiconInputStream
			//System.out.println("target lexicon:"+ApplicationSetup.getProperty("target.lexicon", ""));
			//System.out.println("output index:"+ApplicationSetup.getProperty("out.lexicon","")+"id");
			File lexiconFile = new File(lexFilename);
			File lexid = new File(outputFilename);
			if (!lexid.exists())
				lexid.createNewFile();
			BlockLexicon lex = new BlockLexicon(lexFilename);
			//int lexiconEntries = (int) lexiconFile.length() / Lexicon.lexiconEntryLength;
			//System.out.println("Lexicon.lexiconEntryLength:"+Lexicon.lexiconEntryLength+", ApplicationSetup.STRING_BYTE_LENGTH:"+ApplicationSetup.STRING_BYTE_LENGTH);
			//System.out.println("numberOfLexiconEntries: "+lex.numberOfLexiconEntries);
			int lexiconEntries = (int)lex.getNumberOfLexiconEntries();
			System.out.println("lexiconEntries: "+lexiconEntries);
			
			
			DataInputStream lexicon =
				new DataInputStream(
					new BufferedInputStream(new FileInputStream(lexiconFile)));
			//the i-th element of offsets contains the offset in the
			//lexicon file of the term with term identifier equal to i.
			//long[] offsets = new long[lexiconEntries];
			long offset;
			final int termLength = ApplicationSetup.STRING_BYTE_LENGTH;
			int termid;
			byte[] buffer = new byte[termLength];
			//File lexid = new File(outputFilename);
			DataOutputStream dosLexid =
				new DataOutputStream(
					new BufferedOutputStream(new FileOutputStream(lexid)));
			int previousId = -1;
			for (int i = 0; i < lexiconEntries; i++) {
				int read = lexicon.read(buffer, 0, termLength);
				termid = lexicon.readInt();
				int docFreq = lexicon.readInt();
				int freq = lexicon.readInt();
				offset = i * Lexicon.lexiconEntryLength;
				lexicon.readLong();
				lexicon.readByte();
				for (int j=previousId+1; j<=termid; j++)
					dosLexid.writeLong(offset);
				previousId = termid;
			}
			lexicon.close();
			//save the offsets to a file with the same name as 
			//the lexicon and extension .lexid
			dosLexid.close();
			System.out.println("Done.");
		}
	 
	 public void createLexiconIndex(String lexFilename, String outputFilename) throws IOException {
			/*
			 * This method reads from the lexicon the term ids and stores the
			 * corresponding offsets in an array. Then this array is sorted 
			 * according to the term id.
			 */
			
			//TODO use the class LexiconInputStream
			//System.out.println("target lexicon:"+ApplicationSetup.getProperty("target.lexicon", ""));
			//System.out.println("output index:"+ApplicationSetup.getProperty("out.lexicon","")+"id");
			File lexiconFile = new File(lexFilename);
			File lexid = new File(outputFilename);
			if (!lexid.exists())
				lexid.createNewFile();
			Lexicon lex = new Lexicon(lexFilename);
			//int lexiconEntries = (int) lexiconFile.length() / Lexicon.lexiconEntryLength;
			//System.out.println("Lexicon.lexiconEntryLength:"+Lexicon.lexiconEntryLength+", ApplicationSetup.STRING_BYTE_LENGTH:"+ApplicationSetup.STRING_BYTE_LENGTH);
			//System.out.println("numberOfLexiconEntries: "+lex.numberOfLexiconEntries);
			int lexiconEntries = (int)lex.getNumberOfLexiconEntries();
			System.out.println("lexiconEntries: "+lexiconEntries);
			
			
			DataInputStream lexicon =
				new DataInputStream(
					new BufferedInputStream(new FileInputStream(lexiconFile)));
			//the i-th element of offsets contains the offset in the
			//lexicon file of the term with term identifier equal to i.
			//long[] offsets = new long[lexiconEntries];
			long offset;
			final int termLength = ApplicationSetup.STRING_BYTE_LENGTH;
			int termid;
			byte[] buffer = new byte[termLength];
			//File lexid = new File(outputFilename);
			DataOutputStream dosLexid =
				new DataOutputStream(
					new BufferedOutputStream(new FileOutputStream(lexid)));
			int previousId = -1;
			for (int i = 0; i < lexiconEntries; i++) {
				int read = lexicon.read(buffer, 0, termLength);
				termid = lexicon.readInt();
				int docFreq = lexicon.readInt();
				int freq = lexicon.readInt();
				offset = i * Lexicon.lexiconEntryLength;
				lexicon.readLong();
				lexicon.readByte();
				for (int j=previousId+1; j<=termid; j++)
					dosLexid.writeLong(offset);
				previousId = termid;
			}
			lexicon.close();
			//save the offsets to a file with the same name as 
			//the lexicon and extension .lexid
			dosLexid.close();
			System.out.println("Done.");
		}
	 
	 public void debug(){
		 //System.out.println(FieldScore.FIELDS_COUNT);
		
		try{
			BufferedReader br1 = new BufferedReader(new FileReader(
				new File("/users/grad/craigm/pj_prior_corr/IN.GOV.txt")));
			BufferedReader br2 = new BufferedReader(new FileReader(
			        new File("/users/grad/craigm/pj_prior_corr/PR.GOV.txt")));
			int N = 1247753;
			double[] X = new double[N];
			double[] Y = new double[N];
			for (int i=0; i<N; i++){				
				X[i] = Double.parseDouble(br1.readLine());
				Y[i] = Double.parseDouble(br2.readLine());
			}
			double corr = Statistics.correlation(X, Y);
			System.out.println(corr);
			br1.close();
			br2.close();
		}catch(IOException e){
			e.printStackTrace();
			System.exit(1);
		}
		
		 
		/* File outputFile = new File(ApplicationSetup.TREC_RESULTS, "out");
		 File writeFile = new File(ApplicationSetup.TREC_RESULTS, "queryterms");
		 THashSet terms = new THashSet();
		 try{
			BufferedReader br = new BufferedReader(new FileReader(outputFile));
			String str = null;
			while ((str=br.readLine())!=null){
				if (str.trim().length()==0)
					continue;
				StringTokenizer stk = new StringTokenizer(str);
				stk.nextToken();
				terms.add(stk.nextToken());
			}
			br.close();
			BufferedWriter bw = new BufferedWriter(new FileWriter(writeFile));
			String[] termStrings = (String[])terms.toArray(new String[terms.size()]);
			for (int i=0; i<terms.size(); i++)
				bw.write(termStrings[i]+EOL);
			bw.close();
			System.out.println(terms.size()+" query terms in total.");
		 }catch(IOException ioe){
			 ioe.printStackTrace();
			 System.exit(1);
		 }*/
		 
	/** 
	     LexiconInputStream globalLex = new LexiconInputStream(
	                 ApplicationSetup.getProperty("global.lexicon.filename", ""));
	     try{
	     	while (globalLex.readNextEntry()>=0){
	        	System.out.println(globalLex.getTerm()+" "+globalLex.getTermId());
	     	}
	     }catch (IOException e){
	         e.printStackTrace();
		 System.exit(1);
	     }*/
		
		/* LexiconInputStream globalLex = new LexiconInputStream(
				 ApplicationSetup.getProperty("global.lexicon.filename", "Must be given."));
		 Lexicon lex = new Lexicon(ApplicationSetup.getProperty(
				 "target.lexicon", "must be given"));
		 LexiconOutputStream outLex = new LexiconOutputStream(
				 ApplicationSetup.TREC_RESULTS+"/"+
				 ApplicationSetup.getProperty("out.lexicon", ""));
		 System.out.println("global lexicon: "+ApplicationSetup.getProperty("global.lexicon.filename", ""));
		 System.out.println("target lexicon: "+ApplicationSetup.getProperty("target.lexicon", ""));
		 System.out.println("output lexicon: "+ApplicationSetup.getProperty("out.lexicon", ""));
		 TerrierTimer timer = new TerrierTimer();
		 timer.start();
		 try{
			 while (globalLex.readNextEntry()>=0){
				 String term = globalLex.getTerm();
				 //System.out.print("processing "+term+"...");
				 int termid = globalLex.getTermId();
				 if (lex.findTerm(term)){
					 outLex.writeNextEntry(term, termid, lex.getNt(), lex.getTF(), 0L, (byte)0);
					 //System.out.println("Written in the new lexicon.");
				 }
				 else{
					 //System.out.println("Not found in target lexicon. Ingored.");
				 }
			 } 
			 globalLex.close();
			 lex.close();
			 outLex.close();
		 }
		 catch(Exception e){
			 e.printStackTrace();
			 System.exit(1);
		 }
		 timer.setBreakPoint();
		 System.out.println("Time elapsed: "+timer.toStringMinutesSeconds());*/
		 
		 
		 /*int[] values = {1, 2, 3, 4, 5};
		 Arrays.sort(values);
		 for (int i=0;i<values.length; i++)
			 System.out.println(values[i]);*/
		 
	     /*String query = "+ben++he";
	     System.out.println(query);
	     query = query.replaceAll("\\+", ":");
	     System.out.println(query);
	     query = query.replaceAll(":{2,}?", "\\+");
	 query = query.replaceAll(":", " ");
	     System.out.println(query);*/
	 /**
		try{	
			File f = new File("/local/terrier_tmp/ben/WT2G/var/results/corr");
			BufferedReader br = new BufferedReader(new FileReader(f));
			String str = null;
			double sum = 0d;
			int counter = 0;
			while ((str=br.readLine())!=null){
				str = str.trim();
				if (str.length()==0)
					continue;
				sum += Double.parseDouble(str);
				counter++;
			}
			br.close();
			System.out.println(sum/counter);
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}*/
	 
		// THashSet set = new THashSet();
		// set.add("test");
		// System.out.println(set.size());
		 
//		 ApplicationSetup.setProperty("TrecQueryTags.process", "TOP,NUM,TITLE");
//		 ApplicationSetup.setProperty("TrecQueryTags.skip", "DESC,NARR");
//		 TRECQuery queries = new TRECQuery();
//		 String query = queries.nextQuery();
//		 System.out.println(query);
//		 
//		 ApplicationSetup.setProperty("TrecQueryTags.process", "TOP,NUM,TITLE,DESC,NARR");
//		 ApplicationSetup.setProperty("TrecQueryTags.skip", "");
//		 queries = new TRECQuery();
//		 query = queries.nextQuery();
//		 System.out.println(query);
		 
//		 double[] data = new double[10];
//		 for (int i=1; i<=10; i++)
//			 data[i-1] = i;
//		double[] bins = Statistics.splitIntoBin(data, 10);
//		for (int i=0; i<bins.length; i++)
//			System.out.println("bins["+i+"]="+bins[i]);
//		double[] tf = {4d, 1d, 2d, 1d};
//		double[] l = {86d, 44d, 50d, 64d};
//		double[] b = new double[21];
//		double[][] tfn = new double[b.length][l.length];
//		double initialB = 0;
//		for (int i =0; i<b.length; i++){
//			b[i] = initialB;
//			initialB+=0.05;
//		}
//		
//		NormalisationB norm = new NormalisationB();
//		norm.setAverageDocumentLength(25);
//		norm.setFieldWeight(1);
//		for (int i=0; i<b.length; i++){
//			norm.setParameter(b[i]);
//			for (int j=0; j<l.length; j++)
//				tfn[i][j] = norm.normalise(1, l[j], 0);
//		}
//		
//		double[] var = new double[l.length];
//		StringBuffer buffer = new StringBuffer();
//		for (int i = 0; i < tfn.length; i++){
//			//System.out.println("b="+b[i]+", var="+Statistics.variance(tfn[i]));
//			buffer.append(b[i]+" "+Statistics.variance(tfn[i])+ApplicationSetup.EOL);
//		}
//		System.out.print(buffer.toString());
		
		 // find maximum and minimum numbers of relevant documents for a set of queries. 
//		 TRECQrelsInMemory qrels = new TRECQrelsInMemory();
//		 System.err.println("min: " + qrels.getMinumumNumberOfRelevantDocuments() +
//				 ", max: " + qrels.getMaximumNumberOfRelevantDocuments());
	 
//		TRECQuery queries = new TRECQuery();
//	 	double[] ql = new double[queries.getNumberOfQueries()];
//	 	int counter = 0;
//	 	while (queries.hasMoreQueries()){
//	 		String query = queries.nextQuery();
//	 		StringTokenizer stk = new StringTokenizer(query);
//	 		int length = stk.countTokens();
//	 		System.out.println(queries.getQueryId() + ": " + length);
//	 		ql[counter++] = length;
//	 	}
//	 	System.out.println("mean ql: " + Statistics.mean(ql));
	 	
//	 	LexiconInputStream newLex = new LexiconInputStream(
//	 			"/local/terrier_tmp/GOV2_index2005/indices/weak/atext_title_body.lex");
//	 	LexiconInputStream oldLex = new LexiconInputStream(
//			"/local/terrier_tmp/GOV2_index2005/indices/weak/oldlexicons/atext_title_body.lex");
//	 	try{
//	 		int newMore = newLex.readNextEntry();
//	 		int oldMore = oldLex.readNextEntry();
//	 		int counter = 0;
//	 		long compareCounter = 0;
//	 		char firstChar = ' ';
//	 		while (newMore>=0 & oldMore>= 0){
//	 			String newTerm = newLex.getTerm();
//	 			String oldTerm = oldLex.getTerm();
//	 			int compare = newTerm.compareTo(oldTerm); 
//	 			if (compare>0){
//	 				oldMore = oldLex.readNextEntry();
//	 				continue;
//	 			}
//	 			else if (compare<0){
//	 				newMore = newLex.readNextEntry();
//	 				continue;
//	 			}
//	 			else{
//	 				if (newTerm.charAt(0)!=firstChar){
//	 					firstChar = newTerm.charAt(0);
//	 					System.err.println("processing terms that start with "+firstChar);
//	 				}
//	 				compareCounter++;
//	 				if (newLex.getTF()!=oldLex.getTF()){
//	 					System.err.println(newTerm+": "+newLex.getTF()+", "+oldLex.getTF());
//	 					counter++;
//	 				}
//	 				newMore = newLex.readNextEntry();
//	 				oldMore = oldLex.readNextEntry();
//	 			}
//	 		}
//	 		System.err.println("Parsing finished with "+counter+" corrupt entries.");
//	 		System.err.println(compareCounter+" comparisons");
//	 	}
//	 	catch(IOException e){
//	 		e.printStackTrace();
//	 		System.exit(1);
//	 	}
	 	
//	 	File fRes = new File(ApplicationSetup.TREC_RESULTS, "uogTB05SQE.res");
//	 	StringBuffer buf = new StringBuffer();
//	 	try{
//	 		BufferedReader br = new BufferedReader(new FileReader(fRes));
//	 		
//	 		String previousid = "";
//	 		String str = null;
//	 		int counter = 0;
//	 		while ((str=br.readLine())!=null){
//	 			if (str.length()==0)
//	 				continue;
//	 			StringTokenizer stk = new StringTokenizer(str);
//	 			String id = stk.nextToken();
//	 			if (!previousid.equals(id)){
//	 				counter = 0;
//	 				previousid = ""+id;
//	 			}
//	 			counter++;
//	 			if (counter <= 1000)
//	 				buf.append(str+EOL);
//	 		}
//	 		br.close();
//	 		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(ApplicationSetup.TREC_RESULTS, "out")));
//	 		bw.write(buf.toString());
//	 		bw.close();
//	 	}
//	 	catch(IOException e){
//	 		e.printStackTrace();
//	 		System.exit(1);
//	 	}
	 	
//	 	File f = new File("/users/grad/ben", "map.txt");
//	 	File fRun = new File("/users/grad/ben", "run.txt");
//	 	double map = 0;
//	 	try{
//	 		BufferedReader br = new BufferedReader(new FileReader(f));
//	 		BufferedReader brRun = new BufferedReader(new FileReader(fRun));
//	 		String str = null;
//	 		int counter = 0;
//	 		int same = 0;
//	 		while ((str=br.readLine())!=null){
//	 			counter++;
//	 			map+=Double.parseDouble(str);
//	 			double themap = Double.parseDouble(str);
//	 			while ((str=brRun.readLine())!=null){
//	 				if (str.startsWith("map")){
//	 					StringTokenizer stk = new StringTokenizer(str);
//	 					stk.nextToken(); String id = stk.nextToken();
//	 					if (Double.parseDouble(stk.nextToken()) == themap){
//	 						same++;
//	 						System.err.println(id);
//	 					}
//	 					break;
//	 				}
//	 					
//	 			}
//	 		}
//	 		br.close();
//	 		brRun.close();
//	 		System.err.println("map: "+map/counter+" with "+counter+" queries.");
//	 		System.err.println("same: " + same);
//	 	}
//	 	catch(IOException e){
//	 		e.printStackTrace();
//	 		System.exit(1);
//	 	}
	 /*	
	 	File f = new File("/users/grad/ben", "map.txt");
	 	File fRun = new File("/users/grad/ben", "run.txt");
	 	double map = 0;
	 	try{
	 		BufferedReader br = new BufferedReader(new FileReader(f));
	 		BufferedReader brRun = new BufferedReader(new FileReader(fRun));
	 		String str = null;
	 		int counter = 0;
	 		int same = 0;
	 		while ((str=br.readLine())!=null){
	 			counter++;
	 			map+=Double.parseDouble(str);
	 			double themap = Double.parseDouble(str);
	 			while ((str=brRun.readLine())!=null){
	 				if (str.startsWith("map")){
	 					StringTokenizer stk = new StringTokenizer(str);
	 					stk.nextToken(); String id = stk.nextToken();
	 					if (Double.parseDouble(stk.nextToken()) == themap){
	 						same++;
	 						System.err.println(id);
	 					}
	 					break;
	 				}
	 					
	 			}
	 		}
	 		br.close();
	 		brRun.close();
	 		System.err.println("map: "+map/counter+" with "+counter+" queries.");
	 		System.err.println("same: " + same);
	 	}
	 	catch(IOException e){
	 		e.printStackTrace();
	 		System.exit(1);
	 	}*/
			   
//File dir = new File(ApplicationSetup.TREC_RESULTS);
			   
//	 	String[] fnames = dir.list();
//	 	for (int i = 0; i < fnames.length; i++){
//	 		Vector MAP = new Vector();
//	 		Vector parameter = new Vector();
//	 		try{
//	 			System.out.println("processing file " + fnames[i]);
//	 			BufferedReader br = new BufferedReader(new FileReader(
//	 					new File(dir, fnames[i])));
//	 			String str = null;
//	 			while ((str=br.readLine())!=null){
//	 				if (str.trim().length() == 0)
//	 					continue;
//	 				str = str.trim();
//	 				MAP.addElement(new Double(Double.parseDouble(
//	 						str.substring(0, str.indexOf(' ')))));
//	 				parameter.addElement(new Double(Double.parseDouble(
//	 						str.substring(str.indexOf(' ')+1, str.length()))));
//	 			}
//	 			br.close();
//	 			double[] maps = new double[MAP.size()];
//	 			double[] paras = new double[MAP.size()];
//	 			for (int j = 0; j < maps.length; j++){
//	 				maps[j] = ((Double)(MAP.get(j))).doubleValue();
//	 				paras[j] = ((Double)(parameter.get(j))).doubleValue();
//	 			}
//	 			double[] copy = (double[])paras.clone();
//	 			int[] order = BubbleSort.getOrder(copy);
//	 			StringBuffer buffer = new StringBuffer();
//	 			for (int j = 0; j < maps.length; j++)
//	 				buffer.append(paras[order[j]] + " ");
//	 			buffer.append(EOL);
//	 			for (int j = 0; j < maps.length; j++)
//	 				buffer.append(maps[order[j]] + " ");
//	 			BufferedWriter bw = new BufferedWriter(new FileWriter(
//	 					new File(dir, fnames[i])));
//	 			bw.write(buffer.toString());
//	 			bw.close();
//	 		}
//	 		catch(IOException e){
//	 			e.printStackTrace();
//	 			System.exit(1);
//	 		}
//	 	}
//	 	System.out.println("Done.");
	 	 /**
	 	 TRECQuery queries = new TRECQuery();
	 	 int num = 0;
	 	 int sum = queries.getNumberOfQueries();
	 	 while (queries.hasMoreQueries()){
	 	 	 int ql = new StringTokenizer(queries.nextQuery()).countTokens();
	 	 	 if (ql > 5) 
	 	 	 	 num++;
	 	 }
	 	 System.out.println("num: " + num +", total: " +sum);
	 	 */
	 	 //System.out.println(SystemUtility.getAverageQueryLength());
	 	 /**
	 	File dir = new File(ApplicationSetup.TREC_RESULTS);
	 	String[] files = dir.list();
	 	for (int i = 0; i < files.length; i++){
	 		try{
	 			if (!files[i].endsWith(".res"))
	 				continue;
	 			System.out.println("processing file " + files[i]);
	 			BufferedReader br = new BufferedReader(new FileReader(
	 					new File(dir, files[i])));
	 			StringBuffer buffer = new StringBuffer();
	 			String str = null;
	 			while ((str=br.readLine())!=null){
	 				if (str.trim().length() == 0)
	 					continue;
	 				String queryid = SystemUtility.stripNumbersAtTheEnd(
	 						str.substring(0, str.trim().indexOf(' ')));
	 				buffer.append(queryid + 
	 						str.substring(str.indexOf(' '), str.length()) + EOL);
	 			}
	 			br.close();
	 			BufferedWriter bw = new BufferedWriter(new FileWriter(
	 					new File(dir, files[i])));
	 			bw.write(buffer.toString());
	 			bw.close();
	 		}
	 		catch(IOException e){
	 			e.printStackTrace();
	 			System.exit(1);
	 		}
	 	}
	 	*/
	 	
	 }
	 
	 public void createScript(String var1, double left1, double right1, double interval1, int eff1,
			 String var2, double left2, double right2, double interval2, int eff2, String command){
		 File dir = new File(ApplicationSetup.TERRIER_SHARE);
		 	File fout = new File(dir, "script.sh");
		 	double value1 = left1;
		 	double value2 = left2;
		 	StringBuffer buffer = new StringBuffer();
		 	while (value1 <= right1){
		 		value2=left2;
		 		while (value2 <= right2){
		 			buffer.append(
							"TERRIER_OPTIONS=\"-D"+var1+"="+Rounding.toString(value1, eff1)+
							" -D" +var2 + "="+Rounding.toString(value2, eff2)+
							"\" "+command+" 1>out 2>&1 &&"+EOL
							//"$TERRIER_HOME/bin/anyclass.sh \\"+EOL+
		 					//"-Dserver.names=\"$NODELIST\" \\"+EOL+
		 					//"-Djava.security.policy=$TERRIER_HOME/bin/policy.all \\"+EOL+
		 					//"-Dthread=true \\"+EOL +
		 					//"-Danchor.weight="+ Rounding.toString(aw, 1) + " \\" + EOL +
		 					//"-Dtitle.weight=" + Rounding.toString(tw, 1) + " \\" + EOL +
							//"uk.ac.gla.terrier.distr.applications.DistributedThreeTRECQueryingExpansion" + EOL + EOL
							);
		 			value2+=interval2;
		 		}
		 		value1+=interval1;
		 	}
			buffer.append("./run.sh -e &&"+EOL);
			buffer.append("./run.sh -r -o -orgres simple results");
		 	try{
		 		BufferedWriter bw = new BufferedWriter(new FileWriter(fout));
		 		bw.write(buffer.toString());
		 		bw.close();
		 	}
		 	catch(Exception e){
		 		e.printStackTrace();
		 		System.exit(1);
		 	}
		 	System.err.println("save in file "+fout.getPath());
	 }
	 
	 public void printOptions(){
	 	System.out.println("Usage: ");
	 	System.out.println("-avql Print the average query length.");
	 	System.out.println("-collSta Print collection statistics.");
	 	System.out.println("-corr Correlation related.");
	 	System.out.println("-corr -tfl <term> <method> <parameter> Print correlation " +
	 			"of tf with document length for the given term.");
	 	System.out.println("-e --avgrel Print the average length of the pooled relevant documents.");
	 	System.out.println("-i Index related");
	 	System.out.println("-i --pterm <term> Print information about a given term.");
	 	System.out.println("-i --dumplexicon <lexiconfilename> Dump the given lexicon.");
	 	System.out.println("-qt Print query type.");
	 	System.out.println("-r Retrieval relared");
	 	System.out.println("-r -d Document length related");
	 	System.out.println("-r -d -corr -nl <method> <parameter> Check covariance between " +
	 			"NE and document length.");
	 	System.out.println("-r -d -corr -tl <method> <parameter> Check covariance between " +
	 			"tfn and document length.");
	 	System.out.println("-r -d -link <method> Link correlation of tf with length" +
	 			" with NE.");
	 	System.out.println("-r -d -min <methodName> Seek for the mean peak correlation of " +
	 			"tfn with document length.");
	 	System.out.println("-r -d -pcorr <methodName> Print correlation of tfn with " +
	 			"document length for a list of parameter values.");
	 	System.out.println("-r -d -tune <methodName> <taskName, adhoc; td> Tune the parameter for the" +
	 			"Speficied tf normalisation method and retrieval task.");
	 	System.out.println("-r -o Result related");
	 	System.out.println("-r -o -concatap f1 f2 fout Concat average precisions of each query.");
	 	System.out.println("-r -o -orgres <option 10 ap c> <output filename> Rank runs by " +
	 			"the specified order and store results in the sperified file");
	 	System.out.println("-r -o -sc f1 pos1 f2 pos2 Compute the Spearman's correlation.");
	 	System.out.println("-r -o -prnmtl <filename> Convert the data in the given file " +
	 			"in matlab format");
	 	System.out.println("-r -o -sort <filename> <ColumnIndex> Sort the data according to the given column.");
	 	System.out.println("-r -o -sortmtl <filename> <RowIndex> Sort the data according to the given row.");
	 	System.out.println("-r -o -trecify <relative filename> <tag> Convert the result file into trec submission format.");
	 	System.out.println("-r -q Query related");
	 	System.out.println("-r -q -ap <apq filename> <output filename> Extract average " +
	 			"precision in the specified .apq file and query features, and write " +
	 			"the data into the specified output file in LIBSVM format ");
	 	System.out.println("-r -q -c <output filename> Extract the average precision in " +
	 			"the .apq files in the result directory, and write the data in LIBSVM " +
	 			"format for classification.");
	 	System.out.println("-r -q -clst <clusteringMethodName> <output filename> Cluster " +
	 			"queries and output query features in LIBSVM format for classification.");
	 	
	 	System.out.println("-r -q -t <output filename> Extract the query features in the" +
	 			" topic file and write the data in the speficified file in LIBSVM format" +
	 			" for prediction");
	 	System.out.println("-ret <c> <expansion?> <reformulation?> Do retrieval.");
	 	System.out.println("-s var1 left1 right1 interval1 eff1 var2 left2 right2" +
	 			" interval2 eff2 command. Generate a script for a 2-dimensional sweeping.");
	 	System.out.println("-t Tuning related applications.");
	 	System.out.println("-t <normMethod> <tuningType -NE -corr> <samplingType -real " +
	 			"-sim> [number of samples]");
	 	System.out.println("-sig05 Programs for SIGIR 2005.");
	 	System.out.println("-sig05 -m <f1> <f2> <pos> <fOut> Merge the average precision values " +
	 			"in the two given files for statistics test.");
	 	System.out.println("-stdl Print the standard deviation of document length.");
	 }
	 
	 public static void main(String[] args) {
	 	System.out.println("Start...");
	 	TerrierTimer timer = new TerrierTimer();
		Run run = new Run(args);
		timer.setBreakPoint();
		System.out.println("Finished. Time elapsed: " + timer.toStringMinutesSeconds());
	}
}

	/*
	 * Created on 29 Mar 2008
	 *
	 * To change the template for this generated file go to
	 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
	 */
	package org.terrier.applications;
	
	import gnu.trove.THashSet;
	import gnu.trove.TIntHashSet;
	import gnu.trove.TObjectIntHashMap;
	
	import java.io.BufferedReader;
	import java.io.BufferedWriter;
	import java.io.File;
	import java.io.IOException;
	import java.util.ArrayList;
	import java.util.Arrays;
	import java.util.StringTokenizer;

import org.terrier.querying.Manager;
import org.terrier.structures.DocumentIndex;
import org.terrier.structures.Index;
import org.terrier.structures.Lexicon;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.TRECQuery;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.DataUtility;
import org.terrier.utility.Files;
import org.terrier.utility.IndexUtility;
import org.terrier.utility.QueryUtility;
import org.terrier.utility.ResultUtility;
import org.terrier.utility.Rounding;
	
	import org.terrier.evaluation.TRECQrelsInMemory;
import org.terrier.evaluation.TRECResultsInMemory;
	import uk.ac.gla.terrier.statistics.Statistics;
	
	public class UtilApps {
		
		public static void debug(){
			TRECQrelsInMemory qrels = new TRECQrelsInMemory("/media/tr.ben/uniworkspace/etc/disk12/docFeatures/Data/naive/qrels.gz");
			System.err.println(qrels.isRelevant("057", "502630"));
		}
		
		public static void convertQrelsToFilterWithDocid(String docIdxOutputFilename, String feedbackFilename, String outputFilename){
			// load docno -> docid map
			TObjectIntHashMap<String> docmap = new TObjectIntHashMap<String>();
			try{
				System.err.println("Check docnos in "+feedbackFilename+ "...");
				BufferedReader br = Files.openFileReader(feedbackFilename);
				String line = null;
				while ((line=br.readLine())!=null){
					String[] tokens = line.split(" ");
					String docno = tokens[2];
					docmap.put(docno, -1);
				}
				br.close();
			}catch(IOException ioe){
				ioe.printStackTrace();
				System.exit(1);
			}
			System.out.println(docmap.size()+" docnos loaded.");
			try{
				System.err.println("Loading docidx ...");
				BufferedReader br = Files.openFileReader(docIdxOutputFilename);
				String line = null;
				while ((line=br.readLine())!=null){
					String[] tokens = line.split(",");
					if (tokens.length!=5)
						continue;
					String docno = tokens[2].trim();
					if (docmap.containsKey(docno)){
						docmap.put(docno, Integer.parseInt(tokens[0].trim()));
					}
				}
				br.close();
				System.out.println(docmap.size()+" entries loaded.");
			}catch(IOException ioe){
				ioe.printStackTrace();
				System.exit(1);
			}
			
			boolean RelOnly = Boolean.parseBoolean(ApplicationSetup.getProperty("only.positive.to.filter", "false"));
			
			try{
				System.err.println("Converting docnos to docids for "+feedbackFilename+"...");
				BufferedReader br = Files.openFileReader(feedbackFilename);
				BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
				String line = null;
				String currentQueryid = "1st";
				StringBuffer buf = new StringBuffer();
				while ((line=br.readLine())!=null){
					String[] tokens = line.split(" ");
					String queryid = tokens[0];
					int docid = docmap.get(tokens[2]);
					if (currentQueryid.equals("1st")){
						buf.append(queryid);
						currentQueryid = queryid;
					}else if (!currentQueryid.equals(queryid)){
						buf.append(ApplicationSetup.EOL+queryid);
						currentQueryid = queryid;
					}
					currentQueryid = queryid;
					if (RelOnly){
						int status = Integer.parseInt(tokens[tokens.length-1]);
						if (status > 0)
							buf.append(" "+docid);
					}else
						buf.append(" "+docid);
				}
				bw.write(buf.toString());
				br.close(); bw.close();
				System.out.println("Done. Saved in "+outputFilename);
			}catch(IOException ioe){
				ioe.printStackTrace();
				System.exit(1);
			}
			
			// load feedback file and convert
		}
		
		public static void convertQrelsToFilterList(String qrelsFilename, String outputFilename){
			try{
				BufferedReader br = Files.openFileReader(qrelsFilename);
				// for each line in qrels, load, convert and write
				BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
				String str = null;
				String currentQueryid = "1st";
				THashSet<String> docnoSet = new THashSet<String>();
				while ((str=br.readLine())!=null){
					if (str.trim().length()==0) continue;
					str = str.trim();
					String[] tokens = str.split("\\s+");
					if (currentQueryid.equals("1st")){
						currentQueryid = tokens[0];
					}else if (!currentQueryid.equals(tokens[0])){
						// write 
						StringBuffer buf = new StringBuffer();
						buf.append(currentQueryid);
						Object[] arr = docnoSet.toArray();
						for (int i=0; i<docnoSet.size(); i++){
							buf.append(" "+(String)arr[i]);
						}
						bw.write(buf.toString()+ApplicationSetup.EOL);
						// reinitiate data structures
						buf = null;
						docnoSet = new THashSet<String>();
						currentQueryid = tokens[0];
					}
					docnoSet.add(tokens[2]);
				}	
				br.close(); bw.close();
			}catch(IOException ioe){
	                        ioe.printStackTrace();
	                        System.exit(1);
	                }	
		}
		
		public static void changeOFBaselineScores(String baselineFilename, 
				String trainedScoreFilename, String outputFilename, int numberOfQueries, int resultsize){
			double[][] scores = new double[numberOfQueries][resultsize];
			try{
				BufferedReader br = Files.openFileReader(trainedScoreFilename);
				String str = null;
				int querycounter = 0;
				int doccounter = 0;
				int linecounter = 0;
				while ((str=br.readLine())!=null){
					linecounter++;
					StringTokenizer stk = new StringTokenizer(str);
					stk.nextToken();// skip topicid
					stk.nextToken(); // skip rank
					double score = Double.parseDouble(stk.nextToken());
					if (doccounter == resultsize){
						doccounter = 0;
						querycounter++;
					}
					try{
						scores[querycounter][doccounter++] = score;
						//System.err.println("scores["+querycounter+"]["+(doccounter-1)+"]="+score);
					}catch(ArrayIndexOutOfBoundsException e){
						System.err.println("querycounter: "+querycounter+", doccounter: "+doccounter+", linecounter: "+linecounter);
						e.printStackTrace();
						System.exit(1);
					}
				}
				br.close();
				
				br = Files.openFileReader(baselineFilename);
				BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
				querycounter=-1;
				doccounter=0;
				String previousQueryid = "";
				while ((str=br.readLine())!=null){
					StringTokenizer stk = new StringTokenizer(str);
					String queryid = stk.nextToken();
					String Q0 = stk.nextToken();
					String docno = stk.nextToken();
					String rank = stk.nextToken();
					stk.nextToken(); // skip score
					String tag = stk.nextToken();
					if (!queryid.equals(previousQueryid)){
						querycounter++;
						doccounter=0;
						previousQueryid = queryid;
					}
					try{
						bw.write(queryid+" "+Q0+" "+docno+" "+rank+" "+scores[querycounter][doccounter++]+" "+tag+ApplicationSetup.EOL);
					}catch(ArrayIndexOutOfBoundsException e){
						e.printStackTrace();
						System.exit(1);
					}
				}
				br.close();bw.close();
			}catch(IOException ioe){
				ioe.printStackTrace();
				System.exit(1);
			}
			System.out.println("Done. Results saved in "+outputFilename);
		}
		
		public static void printTRECResultTable(String tableFilename, int lines, int pos){// zero based position
			try{
				BufferedReader br = Files.openFileReader(tableFilename);
				double[] scores = new double[lines];
				String str = null;
				int counter=0;
				while ((str=br.readLine())!=null){
					StringTokenizer stk = new StringTokenizer(str);
					int tokenCounter = 0;
					while (stk.hasMoreTokens()){
						if (tokenCounter == pos){
							try{
								scores[counter++] = Double.parseDouble(stk.nextToken());
							}catch(ArrayIndexOutOfBoundsException e){
								System.err.println("counter: "+counter+", str: "+str);
								e.printStackTrace();
								System.exit(1);
							}
							break;
						}else{
							stk.nextToken();
							tokenCounter++;
						}
					}
				}
				br.close();
				Arrays.sort(scores);
				System.out.println("Best: "+scores[lines-1]+", median: "+scores[lines/2]+", mean: "+Statistics.mean(scores)+
						", worst: "+scores[0]);
			}catch(IOException ioe){
				ioe.printStackTrace();
				System.exit(1);
			}
		}
		
		
		
		public static void roundValues(String filename, int digit){
			try{
				String outputfilename = filename+".rounded";
				File file = new File(filename);
				File outf = new File(outputfilename);
				BufferedReader br = Files.openFileReader(filename);
				BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputfilename);
				String str = null;
				StringBuffer buf = new StringBuffer();
				while ((str=br.readLine())!=null){
					str = str.trim();
					if (str.length()==0) continue;
					String[] tokens = str.split(" ");
					buf.append(tokens[0]);
					for (int i=1; i<tokens.length; i++)
						buf.append(" "+Rounding.toString(Double.parseDouble(tokens[i]), digit));
					buf.append(ApplicationSetup.EOL);
				}
				br.close();
				bw.write(buf.toString());
				bw.close();
				if (file.delete())
					outf.renameTo(file);
			}catch(IOException ioe){
				ioe.printStackTrace();
				System.exit(1);
			}
		}
		
		public static void inverseValues(String filename){
			try{
				String outputfilename = filename+".inversed";
				BufferedReader br = Files.openFileReader(filename);
				BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputfilename);
				String str = null;
				StringBuffer buf = new StringBuffer();
				while ((str=br.readLine())!=null){
					str = str.trim();
					if (str.length()==0) continue;
					String[] tokens = str.split(" ");
					buf.append(tokens[0]);
					for (int i=1; i<tokens.length; i++)
						buf.append(" "+1/Double.parseDouble(tokens[i]));
					buf.append(ApplicationSetup.EOL);
				}
				br.close();
				bw.write(buf.toString());
				bw.close();
			}catch(IOException ioe){
				ioe.printStackTrace();
				System.exit(1);
			}
		}
		/**
		 * 
		 * @param onelineTopicFilename
		 * @param outputFilename
		 * @param rate For example, rate=60 means 60% of the topics are chosen.
		 */
		public static void createRandomTopics(String onelineTopicFilename, String outputFilename, double rate){
			// read all topics
			ArrayList<String> lines = new ArrayList<String>();
			try{
				BufferedReader br = Files.openFileReader(onelineTopicFilename);
				String line = null;
				while ((line=br.readLine())!=null){
					lines.add(line);
				}
				br.close();
			}catch(IOException ioe){
				ioe.printStackTrace();
				System.exit(1);
			}
			String[] topics = (String[])lines.toArray(new String[lines.size()]);
			Arrays.sort(topics);
			int numberOfTopics = topics.length;
			int counter = 0;
			int toExtract = (int)(rate*numberOfTopics);
			TIntHashSet chosenSet = new TIntHashSet();
			while (chosenSet.size()<toExtract){
				chosenSet.add((int)(Math.random()*numberOfTopics));
			}
			try{
				BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
				int[] indices = chosenSet.toArray();
				for (int i=0; i<indices.length; i++)
					bw.write(topics[indices[i]]+ApplicationSetup.EOL);
				bw.close();
				// write the remaining topics
				bw = (BufferedWriter)Files.writeFileWriter(outputFilename+".rest");
				for (int i=0; i<numberOfTopics; i++){
					if (!chosenSet.contains(i))
						bw.write(topics[i]+ApplicationSetup.EOL);
				}
				bw.close();
			}catch(IOException ioe){
				ioe.printStackTrace();
				System.exit(1);
			}
		}
		
		public static void createRFTrackFeedback(String resultFilename, 
				String qrelsFilename,
				String RF_Level){
			int topdocs = 50;// feedback documents are taken randomly from the top ranked documents
			int[] RFSetting = getFeedbackSetting(RF_Level);
			createRFTrackFeedback(resultFilename, qrelsFilename, topdocs, RFSetting[0], RFSetting[1], RF_Level);
		}
		
		public static void createRFTrackFeedback(String resultFilename, 
				String qrelsFilename,
				int lowRank,
				int highRank,
				String RF_Level){
			int[] values = getFeedbackSetting(RF_Level);
			createRFTrackFeedback(resultFilename, qrelsFilename, lowRank, highRank, values[0], values[1], RF_Level);
		}
		
		public static int[] getFeedbackSetting(String RF_Level){
			int numberOfPosDocsPerQuery = 0;
			int numberOfNegDocsPerQuery = 0;
			if (RF_Level.equals("B")){
				numberOfPosDocsPerQuery = 1;
				numberOfNegDocsPerQuery = 0;
			}else if (RF_Level.equals("C")){
				numberOfPosDocsPerQuery = 3;
				numberOfNegDocsPerQuery = 3;
			}else if (RF_Level.equals("D")){
				numberOfPosDocsPerQuery = 4;
				numberOfNegDocsPerQuery = 6;
			}else if (RF_Level.equals("F")){
				numberOfPosDocsPerQuery = 4;
				numberOfNegDocsPerQuery = 100;
			}else if (RF_Level.equals("G")){
				numberOfPosDocsPerQuery = 4;
				numberOfNegDocsPerQuery = 400;
			}
			int[] values = {numberOfPosDocsPerQuery, numberOfNegDocsPerQuery};
			return values;
		}
		
		public static void createRFTrackFeedback(String resultFilename, 
				String qrelsFilename,
				int topdocs,
				int numberOfPosDocsPerQuery,
				int numberOfNegDocsPerQuery,
				String RF_label){
			TRECResultsInMemory results = new TRECResultsInMemory(resultFilename);
			TRECQrelsInMemory qrels = new TRECQrelsInMemory(qrelsFilename);
			String[] queryids = results.getQueryids();
			Arrays.sort(queryids);
			StringBuffer buf = new StringBuffer();
			for (int i=0; i<queryids.length; i++){
				String[] docnos = results.getDocnoSet(queryids[i]);
				docnos = Arrays.copyOfRange(docnos, 0, Math.min(topdocs-1, docnos.length-1));
				Arrays.sort(docnos);
				int reladded = 0; int nonreladded = 0;
				for (int j=0; j<docnos.length; j++){
					int status = qrels.checkDocStatus(queryids[i], docnos[j]);
					if (status == 1 && reladded < numberOfPosDocsPerQuery){
						buf.append(queryids[i]+" 0 "+docnos[j]+" 1"+ApplicationSetup.EOL);
						reladded++;
					}else if (status == 0 && nonreladded < numberOfNegDocsPerQuery){
						buf.append(queryids[i]+" 0 "+docnos[j]+" 0"+ApplicationSetup.EOL);
						nonreladded++;
					}
					if (reladded == numberOfPosDocsPerQuery && nonreladded == numberOfNegDocsPerQuery)
						break;
				}
				if (reladded!=numberOfPosDocsPerQuery||nonreladded!=numberOfNegDocsPerQuery)
					System.err.println("query "+queryids[i]+" doesn't have enough feedback documents. rel added: "+reladded+
							", nonreladded: "+nonreladded);
			}
			String feedbackFilename = qrelsFilename+"."+RF_label;
			try{
				BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(feedbackFilename);
				bw.write(buf.toString());
				bw.close();
			}catch(IOException ioe){
				ioe.printStackTrace();
				System.exit(1);
			}
			convertQrelsToFilterList(feedbackFilename, feedbackFilename+".filter");
		}
		
		/**
		 * Create feedback document using an array of result files for reference.
		 * @param resultFilenames
		 * @param qrelsFilename
		 * @param lowRank
		 * @param highRank
		 * @param numberOfPosDocsPerQuery
		 * @param numberOfNegDocsPerQuery
		 * @param RF_label
		 */
		public static void createRFTrackFeedbackForResults(String resultList, 
				String qrelsFilename,
				int lowRank,
				int highRank,
				String RF_label){
			THashSet<String> resultSet = new THashSet<String>();
			try{
				BufferedReader br = Files.openFileReader(resultList);
				String str = null;
				while ((str=br.readLine())!=null)
					resultSet.add(str);
				br.close();
			}catch(IOException ioe){
				ioe.printStackTrace();
				System.exit(1);
			}
			String[] resultFilenames = (String[])resultSet.toArray(new String[resultSet.size()]);
			int values[] = getFeedbackSetting(RF_label);
			createRFTrackFeedback(resultFilenames, 
					qrelsFilename,
					lowRank,
					highRank,
					values[0],
					values[1],
					RF_label);
		}
		
		public static void printAverageDocumentLengthInQrels(String indexPath, String indexPrefix, String qrelsFilename, String docIdxOutputFilename){
			try{
				Index index = Index.createIndex(indexPath, indexPrefix);
				DocumentIndex docIndex = index.getDocumentIndex();
				TObjectIntHashMap docmap = IndexUtility.getDocidsFromFile(docIdxOutputFilename, qrelsFilename, 2);
				BufferedReader br = Files.openFileReader(qrelsFilename);
				String str = null;
				long doclength = 0;
				int counter = 0;
				while ((str=br.readLine())!=null){
					StringTokenizer stk = new StringTokenizer(str);
					stk.nextToken();//skip queryid
					stk.nextToken();// skip Q0
					String docno = stk.nextToken();
					int docid = docmap.get(docno);
					if (docid>=0){
						doclength += docIndex.getDocumentLength(docid);
						counter++;
					}
				}
				System.out.println("Average length of "+counter+" documents in pool: "+(double)doclength/counter);
				br.close();
				index.close();
			}catch(IOException e){
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		/**
		 * Create feedback document using an array of result files for reference.
		 * @param resultFilenames
		 * @param qrelsFilename
		 * @param lowRank
		 * @param highRank
		 * @param numberOfPosDocsPerQuery
		 * @param numberOfNegDocsPerQuery
		 * @param RF_label
		 */
		public static void createRFTrackFeedback(String[] resultFilenames, 
				String qrelsFilename,
				int lowRank,
				int highRank,
				int numberOfPosDocsPerQuery,
				int numberOfNegDocsPerQuery,
				String RF_label){
			TRECResultsInMemory[] results = new TRECResultsInMemory[resultFilenames.length];
			for (int i=0; i<results.length; i++){
				System.out.println("Loading results from "+resultFilenames[i]);
				results[i] = new TRECResultsInMemory(resultFilenames[i], lowRank, highRank);
			}
			TRECQrelsInMemory qrels = new TRECQrelsInMemory(qrelsFilename);
			TRECQuery queries = QueryUtility.getQueryParser();
			String[] queryids = results[0].getQueryids();
			Arrays.sort(queryids);
			StringBuffer buf = new StringBuffer();
			StringBuffer topicBuf = new StringBuffer();
			for (int i=0; i<queryids.length; i++){
				//String[] docnos = results.getDocnoSet(queryids[i]);
				//docnos = Arrays.copyOfRange(docnos, lowRank, Math.min(highRank, docnos.length-1));
				//Arrays.sort(docnos);
				int maxResultSize = 0;
				String[][] docnos = new String[results.length][];
				for (int j=0; j<results.length; j++){
					String[] localDocnos = results[j].getDocnoSet(queryids[i]);
					//System.err.println("localDocnos.length: "+localDocnos.length);
					if (localDocnos.length<1)
						continue;
					if (lowRank==highRank){
						docnos[j] = localDocnos;
					}
					else
						docnos[j] = Arrays.copyOfRange(localDocnos, 0, Math.min(highRank-lowRank+1, localDocnos.length));
					maxResultSize = Math.max(maxResultSize, docnos[j].length);
				}
				
				int reladded = 0; int nonreladded = 0;
				THashSet<String> addedDocnoSet = new THashSet<String>();
				//System.err.println("maxResultSize: "+maxResultSize);
				//for (int j=0; j<docnos.length; j++){
				//	System.err.println("docnos["+j+"].length: "+docnos[j].length);
				//}
				for (int j=0; j<maxResultSize; j++){
					for (int t=0; t<results.length; t++){
						if (docnos[t] == null || docnos[t].length<=j)
							continue;
						else if (addedDocnoSet.contains(docnos[t][j]))
							continue;
						int status = qrels.checkDocStatus(queryids[i], docnos[t][j]);
						if (lowRank == highRank){
							buf.append(queryids[i]+" 0 "+docnos[t][j]+" "+status+ApplicationSetup.EOL);
							addedDocnoSet.add(docnos[t][j]);
							if (status == 0)
								nonreladded++;
							else if (status >= 1)
								reladded++;
						}
						else if (status == 1 && reladded < numberOfPosDocsPerQuery){
							buf.append(queryids[i]+" 0 "+docnos[t][j]+" 1"+ApplicationSetup.EOL);
							addedDocnoSet.add(docnos[t][j]);
							reladded++;
						}else if (status == 0 && nonreladded < numberOfNegDocsPerQuery){
							buf.append(queryids[i]+" 0 "+docnos[t][j]+" 0"+ApplicationSetup.EOL);
							addedDocnoSet.add(docnos[t][j]);
							nonreladded++;
						}
					}
					if (reladded == numberOfPosDocsPerQuery && nonreladded == numberOfNegDocsPerQuery)
						break;
				}
				if (reladded<numberOfPosDocsPerQuery||nonreladded<numberOfNegDocsPerQuery)
					System.err.println("query "+queryids[i]+" doesn't have enough feedback documents. rel added: "+reladded+
							", nonreladded: "+nonreladded);
				else
					topicBuf.append(queryids[i]+" "+queries.getQuery(queryids[i])+ApplicationSetup.EOL);
			}
			String feedbackFilename = (lowRank==highRank)?(qrelsFilename+"_"+lowRank):
					(qrelsFilename+"."+RF_label+"_"+lowRank+"_"+highRank);
			String topicsOutputFilename = feedbackFilename+".topics";
			try{
				BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(feedbackFilename);
				bw.write(buf.toString());
				bw.close();
				bw = (BufferedWriter)Files.writeFileWriter(topicsOutputFilename);
				bw.write(topicBuf.toString());
				bw.close();
			}catch(IOException ioe){
				ioe.printStackTrace();
				System.exit(1);
			}
			//convertQrelsToFilterList(feedbackFilename, feedbackFilename+".filter");
		}
		
		public static void createRFTrackFeedback(String resultFilename, 
				String qrelsFilename,
				int lowRank,
				int highRank,
				int numberOfPosDocsPerQuery,
				int numberOfNegDocsPerQuery,
				String RF_label){
			TRECResultsInMemory results = new TRECResultsInMemory(resultFilename);
			TRECQrelsInMemory qrels = new TRECQrelsInMemory(qrelsFilename);
			TRECQuery queries = QueryUtility.getQueryParser();
			String[] queryids = results.getQueryids();
			Arrays.sort(queryids);
			StringBuffer buf = new StringBuffer();
			StringBuffer topicBuf = new StringBuffer();
			for (int i=0; i<queryids.length; i++){
				String[] docnos = results.getDocnoSet(queryids[i]);
				docnos = Arrays.copyOfRange(docnos, lowRank, Math.min(highRank, docnos.length-1));
				//Arrays.sort(docnos);
				int reladded = 0; int nonreladded = 0;
				for (int j=0; j<docnos.length; j++){
					int status = qrels.checkDocStatus(queryids[i], docnos[j]);
					if (status == 1 && reladded < numberOfPosDocsPerQuery){
						buf.append(queryids[i]+" 0 "+docnos[j]+" 1"+ApplicationSetup.EOL);
						reladded++;
					}else if (status == 0 && nonreladded < numberOfNegDocsPerQuery){
						buf.append(queryids[i]+" 0 "+docnos[j]+" 0"+ApplicationSetup.EOL);
						nonreladded++;
					}
					if (reladded == numberOfPosDocsPerQuery && nonreladded == numberOfNegDocsPerQuery)
						break;
				}
				if (reladded!=numberOfPosDocsPerQuery||nonreladded!=numberOfNegDocsPerQuery)
					System.err.println("query "+queryids[i]+" doesn't have enough feedback documents. rel added: "+reladded+
							", nonreladded: "+nonreladded);
				else
					topicBuf.append(queryids[i]+" "+queries.getQuery(queryids[i])+ApplicationSetup.EOL);
			}
			String feedbackFilename = qrelsFilename+"."+RF_label+"_"+lowRank+"_"+highRank;
			String topicsOutputFilename = feedbackFilename+".topics";
			try{
				BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(feedbackFilename);
				bw.write(buf.toString());
				bw.close();
				bw = (BufferedWriter)Files.writeFileWriter(topicsOutputFilename);
				bw.write(topicBuf.toString());
				bw.close();
			}catch(IOException ioe){
				ioe.printStackTrace();
				System.exit(1);
			}
			//convertQrelsToFilterList(feedbackFilename, feedbackFilename+".filter");
		}
		
		
		
		/**
		 * 
		 * @param onelineTopicFilename
		 * @param outputFilename
		 * @param rate For example, rate=60 means 60% of the topics are chosen.
		 */
		public static void createRFTrackTopics(String onelineTopicFilename){
			// read all topics
			ArrayList<String> lines = new ArrayList<String>();
			try{
				BufferedReader br = Files.openFileReader(onelineTopicFilename);
				String line = null;
				while ((line=br.readLine())!=null){
					lines.add(line);
				}
				br.close();
			}catch(IOException ioe){
				ioe.printStackTrace();
				System.exit(1);
			}
			String[] topics = (String[])lines.toArray(new String[lines.size()]);
			Arrays.sort(topics);
			int numberOfTopics = topics.length;
			int counter = 0;
			try{
				BufferedWriter bwTrain = (BufferedWriter)Files.writeFileWriter(onelineTopicFilename+".train");
				BufferedWriter bwTest = (BufferedWriter)Files.writeFileWriter(onelineTopicFilename+".test");
				for (int i=0; i<topics.length; i++){
					int queryid = Integer.parseInt(topics[i].split("\\s+")[0]);
					boolean odd = (queryid%2==1);
					if (odd)
						bwTrain.write(topics[i]+ApplicationSetup.EOL);
					else
						bwTest.write(topics[i]+ApplicationSetup.EOL);
				}
				bwTrain.close();
				bwTest.close();
			}catch(IOException ioe){
				ioe.printStackTrace();
				System.exit(1);
			}
		}
		
		public static void createRandomFeedbackDocuments(String qrelsFilename, String outputFilename,
				int numberOfPosDocsPerQuery, int numberOfNegDocsPerQuery){
			TRECQrelsInMemory qrels = new TRECQrelsInMemory(qrelsFilename);
			String[] queryids = qrels.getQueryids();
			StringBuffer buf = new StringBuffer();
			for (int i=0; i<queryids.length; i++){
				String[] relDocs = qrels.getRelevantDocumentsToArray(queryids[i]);
				String[] nonRelDocs = qrels.getNonRelevantDocumentsToArray(queryids[i]);
				if (relDocs.length<numberOfPosDocsPerQuery||nonRelDocs.length<numberOfNegDocsPerQuery){
					System.err.println("query "+queryids[i]+"ignored. relDocs.length: "+relDocs.length+", nonRelDocs.length: "+nonRelDocs.length);
					continue;
				}
				// randomly sample 
			}
		}
		
		public static void printOptions(){
			System.out.println("--findnotin setfilename parfilename");
			System.out.println("--csv2svm csvFilename outputFilename normalDelimiter");
			System.out.println("--sortcollectionspec specfilename");
			System.out.println("--csv2svm csvFilename outputFilename normalDelimiter");
			System.out.println("--dumpdoc docid indexPath indexPrefix");
			System.out.println("--inversevalues filename");
			System.out.println("--processoptermlist filename outputfilename");
			System.out.println("--changeofbaselinescores baselinefilename scorefilename outputfilename numberofqueries resultsize");
			System.out.println("--printtrectable tableFilename lines pos");
			System.out.println("--convertqrels docidxFilename feedbackFilename outputFilename");
			System.out.println("--createrandomonelinetopics onelineTopicFilename outputFilename rate");
			System.out.println("--convertqrels2filter qrelsfilename outputfilename");
			System.out.println("--createrRFtracktopics topicfilename");
			System.out.println("--createRFtrackfeedback resultFilename, qrelsFilename, RF_Level(B/C/D)");
			System.out.println("--createRFtrackfeedback resultFilename, qrelsFilename, lowRank, highRank, RF_Level(B/C/D)");
			System.out.println("--printmean resultfilename column");
			System.out.println("--filteropinionwordlist opinionwordlist");
			System.out.println("--convertdocno2docidfromresults docIdxFilename, resultFilename, outputFilename");
			System.out.println("--printavglinqrels indexPath, indexPrefix, qrelsFilename, docIdxOutputFilename");
			System.out.println("--removeqrelsfromresults qrelsFilename resultFilename outputFilename");
			System.out.println("--printresultsim resultFilename1 resultFilename2");
			System.out.println("--printtermstats lexiconFilename termString");
			System.out.println("--trecquery2oneline trectopicFilename outputFilename");
			System.out.println("--results2docnos docidxFilename resultFilename outputFilename");
			System.out.println("--results2qrels resultFilename, outputFilename, defaultLabel");
			System.out.println("--mergeresults filename1 filename2...filenamei outputFilename resultSize");
			System.out.println("--results2docids docidxFilename, resultFilename, outputFilename");
			System.out.println("--prels2docids docidxFilename, feedbackFilename, outputFilename");
			System.out.println("--qrels2docids docidxFilename feedbackFilename outputFilename");
			System.out.println("--docnos2docids docidxFilename, docnosFilename, column, outputFilename");
			System.out.println("--genfbdocs resultFilename folder topX");
			System.out.println("--smoothscores inputFilename column outputFilename");
			// System.out.println("");
			// System.out.println("");
			// System.out.println("");
			// System.out.println("");
		}
		
	
		/**
		 * @param args
		 */
		public static void main(String[] args) {
			if (args.length!=0){
				if (args[0].equals("--findnotin")){
					// --findnotin setfilename parfilename
					DataUtility.findNotIn(args[1], args[2]);
				}else if (args[0].equals("--csv2svm")){
					// --csv2svm csvFilename outputFilename normalDelimiter
					DataConversion.CSV2SVMLight(args[1], args[2], args[3]);
					System.out.println("Done. Data saved in "+args[2]);
				}
				else if(args[0].equals("--sortcollectionspec")){
					// --sortcollectionspec specfilename
					IndexUtility.sortCollectionSpec(args[1]);
				}else if(args[0].equals("--roundvalues")){
					// --roundvalues filename place
					UtilApps.roundValues(args[1], Integer.parseInt(args[2]));
				}else if(args[0].equals("--inversevalues")){
					// --inversevalues filename
					UtilApps.inverseValues(args[1]);
				}else if(args[0].equals("--changeofbaselinescores")){
					// --changeofbaselinescores baselinefilename scorefilename outputfilename numberofqueries resultsize
					UtilApps.changeOFBaselineScores(args[1], args[2], args[3], 
							Integer.parseInt(args[4]), Integer.parseInt(args[5]));
				}else if(args[0].equals("--printtrectable")){
					// --printtrectable tableFilename lines pos
					UtilApps.printTRECResultTable(args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]));
				}else if(args[0].equals("--createrandomonelinetopics")){
					// --createrandomonelinetopics onelineTopicFilename outputFilename rate
					UtilApps.createRandomTopics(args[1], args[2], Double.parseDouble(args[3]));
				}else if(args[0].equals("--convertqrels2filter")){
					// --convertqrels2filter qrelsfilename outputfilename
					UtilApps.convertQrelsToFilterList(args[1], args[2]);
				}else if(args[0].equals("--createRFtracktopics")){
					// --createrRFtracktopics topicfilename
					UtilApps.createRFTrackTopics(args[1]);
				}else if(args[0].equals("--createRFtrackfeedback")){
					// --createRFtrackfeedback resultFilename, qrelsFilename, RF_Level(B/C/D)
					UtilApps.createRFTrackFeedback(args[1], args[2], args[3]);
				}else if(args[0].equals("--createRFtrackfeedback")){
					// --createRFtrackfeedback resultFilename, qrelsFilename, lowRank, highRank, RF_Level(B/C/D)
					UtilApps.createRFTrackFeedback(args[1], args[2], Integer.parseInt(args[3]), Integer.parseInt(args[4]), args[5]);
				}else if(args[0].equals("--createRFtrackfeedbackgivenrangeforresults")){
					// --createRFtrackfeedback resultList, qrelsFilename, lowRank, highRank, RF_Level(B/C/D)
					UtilApps.createRFTrackFeedbackForResults(args[1], args[2], Integer.parseInt(args[3]), Integer.parseInt(args[4]), args[5]);
				}
				else if(args[0].equals("--convertqrels2filterswithdocid")){
					// 	 docIdxOutputFilename feedbackFilename outputFilename
					UtilApps.convertQrelsToFilterWithDocid(args[1], args[2], args[3]);
				}else if(args[0].equals("--createRFtrackfeedbackgivenX")){
					// --createRFtrackfeedbackgivenX resultFilename qrelsFilename topdocs topXRel topXNonRel label
					int topdocs = Integer.parseInt(args[3]);
					int topXRel = Integer.parseInt(args[4]);
					int topXNonRel = Integer.parseInt(args[5]);
					String label = "_"+topdocs+"_"+topXRel+"_"+topXNonRel;
					UtilApps.createRFTrackFeedback(args[1], args[2], topdocs, topXRel, topXNonRel, label);
				}else if (args[0].equals("--dumpdoc")){
					// --dumpdoc docid indexPath indexPrefix
					IndexUtility.dumpDoc(Integer.parseInt(args[1]), Index.createIndex(args[2], args[3]));
				}
				else if(args[0].equals("--printmean")){
					// --printmean resultfilename column
					DataUtility.printMeanValue(args[1], Integer.parseInt(args[2]));
				}else if (args[0].equals("--convertdocno2docidfromresults")){
					// --convertdocno2docidfromresults indexPath, indexPrefix resultFilename, outputFilename
					IndexUtility.convertDocnoToDocidFromResults(args[1], args[2], args[3], args[4]);
				}else if (args[0].equals("--printavglinqrels")){
					// --printavglinqrels indexPath, indexPrefix, qrelsFilename, docIdxOutputFilename
					UtilApps.printAverageDocumentLengthInQrels(args[1], args[2], args[3], args[4]);
				}else if (args[0].equals("--removeqrelsfromresults")){
					// --removeqrelsfromresults qrelsFilename resultFilename outputFilename
					ResultUtility.removeJudgedFromResults(args[1], args[2], args[3]);
				}else if (args[0].equals("--printresultsim")){
					// --printresultsim resultFilename1 resultFilename2
					ResultUtility.printResultFileSimilarity(args[1], args[2]);
				}else if (args[0].equals("--printtermstats")){
					// --printtermstats indexpath indexprefix termString
					IndexUtility.printTermStats(args[1], args[2], args[3]);
				}else if (args[0].equals("--trecquery2oneline")){
					// --trecquery2oneline trectopicFilename outputFilename
					QueryUtility.TRECQueryToOneLineQuery(args[1], args[2]);
				}else if (args[0].equals("--results2docnos")){
					// --results2docnos indexPath indexPrefix resultFilename outputFilename
					IndexUtility.docids2docnosInResults(args[1], args[2], args[3], args[4]);
				}else if (args[0].equals("--results2qrels")){
					// --results2qrels resultFilename, outputFilename, defaultLabel
					ResultUtility.resultsToQrelsFormat(args[1], args[2], Integer.parseInt(args[3]));
				}else if (args[0].equals("--mergeresults")){
					// --mergeresults filename1 filename2...filenamei outputFilename resultSize
					String[] filenames = new String[args.length-3];
					System.arraycopy(args, 1, filenames, 0, filenames.length);
					ResultUtility.mergeResults(filenames, args[args.length-2], Integer.parseInt(args[args.length-1]));
				}else if (args[0].equalsIgnoreCase("--results2docids")){
					// --results2docids indexPath, indexPrefix resultFilename, outputFilename
					ResultUtility.convertDocnoToDocidFromResults(args[1], args[2], args[3], args[4]);
				}else if(args[0].equals("--qrels2docids")){
					// --qrels2docids indexPath indexPrefix feedbackFilename outputFilename
					IndexUtility.convertFeedbackQrels(args[1], args[2], args[3], args[4]);
				}else if (args[0].equals("--prels2docids")){
					// --prels2docids indexPath indexPrefix, feedbackFilename, outputFilename
					IndexUtility.prelsToDocids(args[1], args[2], args[3], args[4]);
				}else if (args[0].equals("--docnos2docids")){
					// --docnos2docids indexPath, indexPrefix docnosFilename, column, outputFilename
					IndexUtility.docnosToDocids(args[1], args[2], args[3], Integer.parseInt(args[4]), args[5]);
				}else if (args[0].equals("--smoothscores")){
					// --smoothscores inputFilename column outputFilename
					DataConversion.smoothScores(args[1], Integer.parseInt(args[2]), args[3]);
				}
				
				else if(args[0].equals("--genfbdocs")){
					// --genfbdocs resultFilename folder topX
					ResultUtility.generateFBDocs(args[1], args[2], Integer.parseInt(args[3]));
				}else if(args[0].equals("--debug")){
					UtilApps.debug();
				}
				else
					UtilApps.printOptions();
			}
			else UtilApps.printOptions();
	
		}
	
	}

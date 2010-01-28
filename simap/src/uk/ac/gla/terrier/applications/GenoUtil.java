/*
 * Created on 5 May 2008
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.applications;

import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.StringTokenizer;

import uk.ac.gla.terrier.evaluation.TRECResultsInMemory;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;

public class GenoUtil {
	/**
	 * Check if the sentences in the qrels exist in the collection.
	 * @param qrelsFilename
	 * @param indexpath
	 * @param indexprefix
	 */
	public static void checkPool(String qrelsFilename, String indexpath, String indexprefix){
		try{
			// load docnos from qrels
			THashSet<String> qrelsDocnoSet = new THashSet<String>();
			System.out.print("Loading docnos from qrels "+qrelsFilename+"...");
			
			BufferedReader br = Files.openFileReader(qrelsFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				StringTokenizer stk = new StringTokenizer(line);
				stk.nextToken();// skip queryid
				String pmid = stk.nextToken();
				String offset = stk.nextToken();
				String length = stk.nextToken();
				String docno = pmid+"-"+offset+"-"+length;
				qrelsDocnoSet.add(docno);
			}
			br.close();
			System.out.println("Done. "+qrelsDocnoSet.size()+" docnos loaded.");
			// check docnos in the index
			Index index = Index.createIndex(indexpath, indexprefix);
			DocumentIndex docindex = index.getDocumentIndex();
			int numberOfDocs = index.getCollectionStatistics().getNumberOfDocuments();
			System.out.print("Checking document index...");
			for (int docid=0; docid<numberOfDocs; docid++){
				String docno = docindex.getDocumentNumber(docid);
				String[] tokens = docno.split("-");
				String normalDocno = (tokens.length==3)?(docno):(tokens[0]+"-"+tokens[1]+"-"+tokens[2]);
				if (qrelsDocnoSet.contains(normalDocno)){
					qrelsDocnoSet.remove(normalDocno);
					if (tokens.length==4)
						System.out.println(docno+" considered empty during indexing.");
				}
			}
			System.out.println("Done. "+qrelsDocnoSet.size()+" docnos unfound in index:");
			index.close();
			String[] docnos = (String[])qrelsDocnoSet.toArray(new String[qrelsDocnoSet.size()]);
			for (int i=0; i<docnos.length; i++)
				System.err.println(docnos[i]);
			
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void rawRelevanceToTRECQrels(String rawRelevanceFilename, String outputFilename, String year){
		try{
			BufferedReader br = Files.openFileReader(rawRelevanceFilename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				if (line.startsWith("#")||line.trim().length()==0)
					continue;
				StringTokenizer stk = new StringTokenizer(line);
				String topicid = stk.nextToken();
				String pmid = stk.nextToken();
				String offset = stk.nextToken();
				String length = stk.nextToken();
				if (year.equals("2006"))
					stk.nextToken();// skip spanid
					
				String relevance = null;
				try{
					relevance=stk.nextToken();
				}catch(java.util.NoSuchElementException e){
					System.err.println("entry: "+line);
					e.printStackTrace();
					System.exit(1);
				}
				bw.write(topicid+" 0 "+pmid+"-"+offset+"-"+length+" ");
				if (year.equals("2006")){
					if (relevance.equals("NOT"))
						bw.write("0"+ApplicationSetup.EOL);
					else
						bw.write("1"+ApplicationSetup.EOL);
				}else if (year.equals("2007")){
					if (relevance.equals("NOT_RELEVANT"))
						bw.write("0"+ApplicationSetup.EOL);
					else
						bw.write("1"+ApplicationSetup.EOL);
				}
			}
			br.close(); bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		System.out.println("Done. Qrels saved at "+outputFilename);
	}
	
	public static void rawRelevanceToTRECDocQrels(String rawRelevanceFilename, String outputFilename, String year){
		try{
			BufferedReader br = Files.openFileReader(rawRelevanceFilename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			String line = null;
			TIntIntHashMap map = null;// maps from pmid to relevance [0/1]
			String currentTopicid = "1st";
			while ((line=br.readLine())!=null){
				if (line.startsWith("#")||line.trim().length()==0)
					continue;
				StringTokenizer stk = new StringTokenizer(line);
				String topicid = stk.nextToken();
				int pmid = Integer.parseInt(stk.nextToken());
				String offset = stk.nextToken();
				String length = stk.nextToken();
				if (year.equals("2006"))
					stk.nextToken();// skip spanid
					
				String relevance = null;
				try{
					relevance=stk.nextToken();
				}catch(java.util.NoSuchElementException e){
					System.err.println("entry: "+line);
					e.printStackTrace();
					System.exit(1);
				}
				if (!currentTopicid.equals(topicid)){
					// if not 1st, flush hashmap
					if (!currentTopicid.equals("1st")){
						// write
						int[] pmids = map.keys();
						for (int i=0; i<pmids.length; i++){
							bw.write(currentTopicid+" 0 "+pmids[i]+" "+map.get(pmids[i])+ApplicationSetup.EOL);
						}
					}
					currentTopicid = topicid;
					map = new TIntIntHashMap();				
				}
				// bw.write(topicid+" 0 "+pmid+"-"+offset+"-"+length+" ");
				if (year.equals("2006")){
					if (relevance.equals("NOT")&&!map.containsKey(pmid)){
						map.put(pmid, 0);						
					}
					else {
						map.put(pmid, 1);
					}
				}else if (year.equals("2007")){
					if (relevance.equals("NOT_RELEVANT")&&!map.containsKey(pmid)){
						map.put(pmid, 0);						
					}
					else {
						map.put(pmid, 1);
					}
				}
			}
			// write
			int[] pmids = map.keys();
			for (int i=0; i<pmids.length; i++){
				bw.write(currentTopicid+" 0 "+pmids[i]+" "+map.get(pmids[i])+ApplicationSetup.EOL);
			}
			br.close(); bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		System.out.println("Done. Qrels saved at "+outputFilename);
	}
	
	public static void extractSentenceForPmids(String sentenceFilename, String pmidFilename, String outputFilename){
		try{
			// load pmids
			BufferedReader br = Files.openFileReader(pmidFilename);
			String pmid = null;
			System.out.print("Loading pmids...");
			THashSet<String> pmidSet = new THashSet<String>();
			while ((pmid=br.readLine())!=null){
				pmidSet.add(pmid);
			}
			br.close();
			System.out.println(pmidSet.size()+" pmids loaded.");
			// read sentence file
			br = Files.openFileReader(sentenceFilename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			String line = null;
			System.out.print("Extracting sentences...");
			int sentenceCounter = 0;
			THashSet<String> foundPmidSet = new THashSet<String>();
			while ((line=br.readLine())!=null){
				// if pmid in, write
				String[] tokens = line.split("\t");
				if (pmidSet.contains(tokens[0])){
					bw.write(line+ApplicationSetup.EOL);
					foundPmidSet.add(tokens[0]);
					sentenceCounter++;
				}else if (foundPmidSet.size() == pmidSet.size())
					break;
			}
			br.close(); bw.close();
			System.out.println(sentenceCounter+" sentences extracted to "+outputFilename);
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}

	public static void getSpanRankInDocRetrieval(String docResultFilename,
			String spanGoldFilename){
		try{
			BufferedReader br = Files.openFileReader(spanGoldFilename);
			String str = null;
			TRECResultsInMemory results = new TRECResultsInMemory(docResultFilename);
			int numberOfQueries = results.getNumberOfQueries();
			double totalRanking = 0d;
			double totalRetRel = 0d;
			double totalRetRelInTop = 0d;
			int totalRelDocs = 0;
			double[] avgRanking = new double[numberOfQueries];
			Arrays.fill(avgRanking, 0d);
			String[] queryids = new String[numberOfQueries];
			double[] numberOfRetRel = new double[numberOfQueries];
			double[] numberOfRetRelInTop = new double[numberOfQueries];
			Arrays.fill(numberOfRetRel, 0d);
			Arrays.fill(numberOfRetRelInTop, 0d);
			int X = ApplicationSetup.EXPANSION_DOCUMENTS;
			THashSet<String> pmidSet = null;
			String currentQueryid = "";
			int querycounter = 0;
			while ((str=br.readLine())!=null){
				str = str.trim();
				if (str.length()==0)
					continue;
				StringTokenizer stk = new StringTokenizer(str);
				String queryid = stk.nextToken();
				String pmid = stk.nextToken();
				if (currentQueryid.length() == 0){
					currentQueryid = queryid;
					pmidSet = new THashSet<String>();
					pmidSet.add(pmid);
					int rank = results.getRank(currentQueryid, pmid);
					if (rank >= 0){
						avgRanking[querycounter]+=(rank+1);
						totalRanking += (rank+1);
						numberOfRetRel[querycounter]++;
						totalRetRel ++;
						if (rank < X){
							numberOfRetRelInTop[querycounter]++;
							totalRetRelInTop++;
						}
					}
					queryids[querycounter] = queryid;
				}else if (!currentQueryid.equals(queryid)){
					if (!pmidSet.contains(pmid)){
						pmidSet.add(pmid);
						int rank = results.getRank(currentQueryid, pmid);
						if (rank >= 0){
							avgRanking[querycounter]+=(rank+1);
							totalRanking += (rank+1);
							numberOfRetRel[querycounter]++;
							totalRetRel ++;
							if (rank < X){
								numberOfRetRelInTop[querycounter]++;
								totalRetRelInTop++;
							}
						}
						avgRanking[querycounter] /= pmidSet.size();
						numberOfRetRel[querycounter] /= pmidSet.size();
						numberOfRetRelInTop[querycounter] /= pmidSet.size();
						totalRelDocs += pmidSet.size();
					}
					pmidSet = new THashSet<String>();
					querycounter++;
					currentQueryid = queryid;
					queryids[querycounter] = queryid;
				}else{
					if (!pmidSet.contains(pmid)){
						pmidSet.add(pmid);
						int rank = results.getRank(currentQueryid, pmid);
						if (rank >= 0){
							avgRanking[querycounter]+=(rank+1);
							totalRanking += (rank+1);
							numberOfRetRel[querycounter]++;
							totalRetRel ++;
							if (rank < X){
								numberOfRetRelInTop[querycounter]++;
								totalRetRelInTop++;
							}
						}
					}
				}
			}
			avgRanking[querycounter] /= pmidSet.size();
			numberOfRetRel[querycounter] /= pmidSet.size();
			numberOfRetRelInTop[querycounter] /= pmidSet.size();
			totalRelDocs += pmidSet.size();
			totalRanking /= totalRelDocs;
			totalRetRel /= totalRelDocs;
			totalRetRelInTop /= totalRelDocs;
			pmidSet = null;
			br.close();
			results = null;
			for (int i=0; i<numberOfQueries; i++)
				System.out.println(queryids[i]+": "+avgRanking[i]+", "+numberOfRetRel[i]*100+", "+numberOfRetRelInTop[i]*100);
			System.err.println(totalRanking+", "+totalRetRel*100+", "+totalRetRelInTop*100);
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void matchPmidISSN(String pmidFilename, String issnFilename, String outputFilename){
		try{
			// load issns and journal names, map from journal names to issns
			THashMap<String, String> journalNameIssnMap = new THashMap<String, String>();
			BufferedReader br = Files.openFileReader(issnFilename);
			String line = null;
			System.out.print("Loading issn-journalname from "+issnFilename+"...");
			while ((line=br.readLine())!=null){
				//line = line.toLowerCase();
				StringTokenizer stk = new StringTokenizer(line);
				String issn = stk.nextToken();
				String journalName = line.substring(line.indexOf(issn)+issn.length()+1, line.length()).trim();
				journalNameIssnMap.put(journalName.toLowerCase(), issn);
			}
			br.close();
			System.out.println("Done. "+journalNameIssnMap.size()+" journals loaded.");
			// load pmids and journal names, macth & write
			br = Files.openFileReader(pmidFilename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			while ((line=br.readLine())!=null){
				//line = line.toLowerCase();
				StringTokenizer stk = new StringTokenizer(line);
				String pmid = stk.nextToken();
				String journalName = line.substring(line.indexOf(pmid)+pmid.length()+1, line.length()).trim();
				String issn = journalNameIssnMap.get(journalName.toLowerCase());
				if (issn == null)
					System.out.println("journal ["+journalName+"] not found");
				else
					bw.write(issn+" "+pmid+ApplicationSetup.EOL);
			}
			br.close(); bw.close();
			
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		   
	}
	/**
	 * Find entries in file 1 but not in file 2. positions are 0-based. 
	 */
	public static void findIntNotIn(String filename1, int pos1, 
			String filename2, int pos2, String outputFilename){
		try{
			// load all entries from file2
			BufferedReader br = Files.openFileReader(filename2);
			String line = null;
			System.out.print("loading from "+filename2+"...");
			TIntHashSet entrySet2 = new TIntHashSet();
			while ((line=br.readLine())!=null){
				StringTokenizer stk = new StringTokenizer(line);
				for (int i=0; i<pos2; i++)
					stk.nextToken();
				int entry = Integer.parseInt(stk.nextToken());
				entrySet2.add(entry);
				//System.err.println(line+", "+entry);
			}
			br.close();
			System.out.println(entrySet2.size()+" entries at position "+pos2+ " loaded.");
			// go through file1 and find entries not in file2
			System.out.print("checking "+filename1+"...");
			br = Files.openFileReader(filename1);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			int counter = 0;
			while ((line=br.readLine())!=null){
				StringTokenizer stk = new StringTokenizer(line);
				for (int i=0; i<pos1; i++)
					stk.nextToken();
				int entry = Integer.parseInt(stk.nextToken());
				if (!entrySet2.contains(entry)){
					bw.write(line+ApplicationSetup.EOL);
					counter++;
				}
			}
			System.out.println(counter+" entries not in.");
			br.close(); bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Find entries in file 1 but not in file 2. positions are 0-based. 
	 */
	public static void findNotIn(String filename1, int pos1, 
			String filename2, int pos2){
		try{
			// load all entries from file2
			BufferedReader br = Files.openFileReader(filename2);
			String line = null;
			System.out.print("loading from "+filename2+"...");
			THashSet<String> entrySet2 = new THashSet<String>();
			while ((line=br.readLine())!=null){
				StringTokenizer stk = new StringTokenizer(line);
				for (int i=0; i<pos2; i++)
					stk.nextToken();
				String entry = stk.nextToken();
				entrySet2.add(entry);
				//System.err.println(line+", "+entry);
			}
			br.close();
			System.out.println(entrySet2.size()+" entries at position "+pos2+ " loaded.");
			// go through file1 and find entries not in file2
			System.out.print("checking "+filename1+"...");
			br = Files.openFileReader(filename1);			
			int counter = 0;
			while ((line=br.readLine())!=null){
				StringTokenizer stk = new StringTokenizer(line);
				for (int i=0; i<pos1; i++)
					stk.nextToken();
				String entry = stk.nextToken();
				if (!entrySet2.contains(entry)){
					System.out.println(line);
					counter++;
				}
			}
			System.out.println(counter+" entries not in.");
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void trecFormat2GenoFormat(String trecResultFilename, String outputFilename){
		try{
			BufferedReader br = Files.openFileReader(trecResultFilename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				StringTokenizer stk = new StringTokenizer(line);
				String queryid = stk.nextToken();
				String filler = stk.nextToken();// Q0
				String spanid = stk.nextToken();
				int rank = Integer.parseInt(stk.nextToken());
				String score = stk.nextToken();
				String tag = stk.nextToken();
				String[] tokens = spanid.split("-");
				String pmid = tokens[0]; String offset = tokens[1]; String length = tokens[2];
				bw.write(queryid+" "+pmid+" "+(rank+1)+" "+score+" "+offset+" "+length+" "+tag+ApplicationSetup.EOL);
			}
			br.close(); bw.close();
			System.out.println("Done. Geno format results written in "+outputFilename);
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args[0].equals("--printspanrankindocretrieval")){
			// --printspanrankindocretrieval <docResultFilename> <goldFilename>
			GenoUtil.getSpanRankInDocRetrieval(args[1], args[2]);
		}else if (args[0].equals("--extractsentencesbypmids")){
			// --extractsentencesbypmids sentenceFilename pmidFilename outputFilename
			GenoUtil.extractSentenceForPmids(args[1], args[2], args[3]);
		}else if (args[0].equals("--matchpmidissn")){
			// --matchpmidissn pmidFilename issnFilename outputFilename
			GenoUtil.matchPmidISSN(args[1], args[2], args[3]);
		}else if (args[0].equals("--findintnotin")){
			// --findintnotin filename1 pos1 filename2 pos2 outputFilename
			GenoUtil.findIntNotIn(args[1], Integer.parseInt(args[2]), args[3], 
					Integer.parseInt(args[4]), args[5]);
		}else if (args[0].equals("--findnotin")){
			// --findnotin filename1 pos1 filename2 pos2
			GenoUtil.findNotIn(args[1], Integer.parseInt(args[2]), args[3], 
					Integer.parseInt(args[4]));
		}else if (args[0].equals("--checkpool")){
			// --checkpool qrelsFilename indexpath indexprefix
			GenoUtil.checkPool(args[1], args[2], args[3]);
		}else if (args[0].equals("--rawrelevance2trecqrels")){
			// --rawrelevance2trecqrels rawRelevanceFilename outputFilename year
			GenoUtil.rawRelevanceToTRECQrels(args[1], args[2], args[3]);
		}else if (args[0].equals("--rawrelevance2trecdocqrels")){
			// --rawrelevance2trecdocqrels rawRelevanceFilename outputFilename year
			GenoUtil.rawRelevanceToTRECDocQrels(args[1], args[2], args[3]);
		}else if (args[0].equals("--trec2geno")){
			// --trec2geno trecResultFilename outputFilename
			GenoUtil.trecFormat2GenoFormat(args[1], args[2]);
		}

	}

}

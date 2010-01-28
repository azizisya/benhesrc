/*
 * Created on 9 Apr 2008
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.applications;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import gnu.trove.THashSet;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.indexing.HtmlTokenizer;
import uk.ac.gla.terrier.links.MetaServer4a;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.Span;
import uk.ac.gla.terrier.utility.Files;

/**
 * @author ben
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class GenoPassageExtraction extends SentenceExtraction {
	protected static Logger logger = Logger.getRootLogger();

	public GenoPassageExtraction(String sentwModelname, String qemodelName){
		super(sentwModelname, qemodelName);
	}
	
	public String getInfo(){
		return this.wModel.getInfo();
	}
	
	static public Span[] getLegalSpans(MetaServer4a server, int docid, String pmid){
		Span[] legalSpans = null;
		int offset = 0;
		int start = 0;
		int length = 0;
		String filename = null;
		
		try{
			String entry = server.getItem("legalspan", docid);
			String[] tokens = entry.split(" ");
			filename = tokens[0];
			//BufferedReader br = Files.openFileReader(tokens[0]);
			int numberOfLegalSpans = (tokens.length-1)/2;
			legalSpans = new Span[numberOfLegalSpans];
			int counter = 1;
			for (int i=0; i<numberOfLegalSpans; i++){
			//for (int i=0; i<3; i++){
				start = Integer.parseInt(tokens[counter++]);
				length = Integer.parseInt(tokens[counter++]);
				//br.skip(start-offset);
				offset = start;
				//char[] buf = new char[length];
				//br.read(buf);
				//String spanString = cleanSpan(String.copyValueOf(buf)).trim();
				String spanString = 
					cleanSpan(String.copyValueOf(ReadFile.getTextByOffset(filename, offset, length)));
				legalSpans[i] = new Span(pmid, offset, length, spanString);
			}
			//br.close();
		}catch(Exception ioe){
			System.err.println("offset: "+offset+", start: "+start+", length: "+length);
			System.err.println("filename: "+filename);
			ioe.printStackTrace();
			System.exit(1);
		}
		return legalSpans;
	}
	
	static public String cleanSpan(String text){
		StringBuilder buf = new StringBuilder();
		HtmlTokenizer htk = new HtmlTokenizer();
		htk.setInput(text);
		String token = null;
		//System.out.println(text);
		//System.out.print(">>>>>>>>>>>>>>>");
		while (!htk.isEndOfDocument()){
			token = htk.nextToken();
			if (token!=null){
				//System.out.print(" "+token);
				buf.append(" "+token.trim());
			}
		}
		//System.out.println();
		//System.out.println(">>>>>>>>>>>>>>>");
		return buf.toString().trim();
	}
	
	static public void convertResultFile(String filename, String outputFilename){
		try{
			BufferedReader br = Files.openFileReader(filename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			String str = null;
			String eol = ApplicationSetup.EOL;
			while ((str=br.readLine())!=null){
				str = str.trim();
				if (str.length()==0)
					continue;
				String[] tokens = str.split(" ");
				// 901 Q0 BLOG06-20051211-088-0020408824 23 12.75751483899876 PL
				String queryid = tokens[0];
				String docno = tokens[2];
				int rank = Integer.parseInt(tokens[3])+1;
				String score = tokens[4];
				String tag = tokens[5];
				String[] spanDecomp = docno.split("-");
				String pmid = spanDecomp[0];
				String offset = spanDecomp[1];
				String length = spanDecomp[2];
				bw.write(queryid+" "+pmid+" "+rank+" "+score+" "+offset+" "+length+
						" "+tag+eol);
			}
			br.close();
			bw.close();
			logger.info("Result saved in "+outputFilename);
		}catch(Exception ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	protected Span[] getSentences(int docid) {
		return GenoPassageExtraction.getLegalSpans(metaServer, docid, docIndex.getDocumentNumber(docid));
	}
	
	public static String removePunctuation(char[] text){
		StringBuilder buf = new StringBuilder();
		int length = text.length;
		for (int i=0; i<length; i++){
			char ch = text[i];
			if (Character.isLetterOrDigit(ch))
				buf.append(ch);
			else buf.append(" ");
		}
		return buf.toString();
	}
	
	public static void createCollectionFromPool(String rawQrels, String outputDir){
		int maxSentPerFile = 100000;
		try{
			Index index = Index.createIndex(ApplicationSetup.TERRIER_INDEX_PATH,
					ApplicationSetup.TERRIER_INDEX_PREFIX);
			DocumentIndex docindex = index.getDocumentIndex();
			MetaServer4a server = new MetaServer4a(ApplicationSetup.TERRIER_INDEX_PATH,
					ApplicationSetup.TERRIER_INDEX_PREFIX);
			BufferedReader br = Files.openFileReader(rawQrels);
			// for each entry in the pool
				// get text
			String line = null;
			StringBuilder buf = new StringBuilder();
			int sentCounter = 0;
			int processedCounter = 0;
			THashSet<String> docnoSet = new THashSet<String>();
			while ((line=br.readLine())!=null){
				StringTokenizer stk = new StringTokenizer(line);
				stk.nextToken();// skip queryid
				String pmid = stk.nextToken();
				int offset = Integer.parseInt(stk.nextToken());
				int length = Integer.parseInt(stk.nextToken());
				int docid = docindex.getDocumentId(pmid);
				String docno = pmid+"-"+offset+"-"+length;
				if (docnoSet.contains(docno))
					continue;
				else
					docnoSet.add(docno);
				if (docid < 0){
					System.err.println("Warning: "+pmid+" not found in index.");
					continue;
				}
				String entry = null;
				try{
					entry = server.getItem("legalspan", docid);
				}catch(ArrayIndexOutOfBoundsException e){
					System.err.println("entry: "+entry);
					System.err.println("line: "+line);
					System.err.println("pmid: "+pmid+", docid: "+docid);
					e.printStackTrace();
					System.exit(1);
				}
				String[] tokens = entry.split(" ");
				String filename = tokens[0];
				char[] originalText = ReadFile.getTextByOffset(filename, offset, length);
				if (originalText!=null){
					String text = removePunctuation(originalText); 
						//cleanSpan(originalText);
					if (text.length()>0){
						buf.append("<doc><docno>"+docno
							+"</docno>"+text+"</doc>"+ApplicationSetup.EOL);
						sentCounter++; processedCounter++;
					}else continue;
				}
				if (sentCounter >= maxSentPerFile ){
					String outputFilename = outputDir+ApplicationSetup.FILE_SEPARATOR+"sentences-"+processedCounter+".gz";
					BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
					bw.write(buf.toString());
					bw.close();
					logger.info(sentCounter+" sentences saved in file "+outputFilename);
					sentCounter = 0;
					buf = null; bw = null;
					System.gc();
					buf = new StringBuilder();
				}	
			}
			if (buf.length() > 0){
				String outputFilename = outputDir+ApplicationSetup.FILE_SEPARATOR+"sentences-"+processedCounter+".gz";
				BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
				bw.write(buf.toString());
				bw.close();
				logger.info(sentCounter+" sentences saved in file "+outputFilename);
				sentCounter = 0;
				buf = null; bw = null;
				System.gc();
				buf = new StringBuilder();
			}
			br.close();
			server.close(); index.close();
		}catch(Exception ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void convertRawCornellSentences(String cornellFilename, String outputFilename,
			boolean withText){
		try{
			BufferedReader br = Files.openFileReader(cornellFilename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			String line = null;
			int counter = 0;
			String score = "1.0";
			while ((line=br.readLine())!=null){
				line=line.trim(); if (line.length()==0) continue;
				StringTokenizer stk = new StringTokenizer(line);
				String pmid = stk.nextToken();
				String sentid = stk.nextToken();
				String type = stk.nextToken();
				if (withText){
					String sentence = line.substring(line.lastIndexOf(type)+type.length(), line.length());
					bw.write(pmid+"\t"+sentid+"\t"+sentid+"\t"+score+"\t"+sentence.trim()+ApplicationSetup.EOL);
				}
				else
					bw.write(pmid+"\t"+sentid+"\t"+sentid+"\t"+score+ApplicationSetup.EOL);
			}
			br.close();
			bw.close();
		}catch(Exception ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void printSpan(String pmid, int offset, int length){
		String mapFilename = ApplicationSetup.getProperty("pmid.map.filename", "/local/tank/terrier/Collections/genomics/2006/documents/unzipped/map.txt");
		try{
			BufferedReader br = Files.openFileReader(mapFilename);
			String line = null;
			while((line=br.readLine())!=null){
				String thispmid = line.substring(line.lastIndexOf('/')+1, line.lastIndexOf('.'));
				if (thispmid.equals(pmid)){
					ReadFile.getStringByOffset(line, offset, length);
					break;
				}
			}
			br.close();
		}catch(Exception ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void printCleanSpan(String pmid, int offset, int length){
		String mapFilename = ApplicationSetup.getProperty("pmid.map.filename", "/local/tank/terrier/Collections/genomics/2006/documents/unzipped/map.txt");
		try{
			BufferedReader br = Files.openFileReader(mapFilename);
			String line = null;
			while((line=br.readLine())!=null){
				String thispmid = line.substring(line.lastIndexOf('/')+1, line.lastIndexOf('.'));
				if (thispmid.equals(pmid)){
					//ReadFile.getStringByOffset(line, offset, length);
					System.out.println("-----------------");
					System.out.println(cleanSpan(String.copyValueOf(ReadFile.getTextByOffset(line, offset, length))));
					System.out.println("-----------------");
					System.out.println("filename: "+line+", offset: "+offset+", length: "+length);
					break;
				}
			}
			br.close();
		}catch(Exception ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public static double countPunctuations(String sentence){
		int letterCounter = 0;
		int puncCounter = 0;
		sentence = sentence.trim();
		int length = sentence.length();
		for (int i=0; i<length; i++){
			char ch = sentence.charAt(i);
			if (Character.isLetterOrDigit(ch))
				letterCounter++;
			else if (ch==':'||ch=='.'||ch==',')
				puncCounter++;
		}
		return (double)(puncCounter)/length * 100d;
	}
	
	public static void createPreprocessedCornellSentenceCollection(String cornellFilename, String offsetFilename, 
			String outputDir, int numberOfSentences){
		int maxSentPerFile = 200000;
		final boolean ignoreRef = Boolean.parseBoolean(ApplicationSetup.getProperty("cornell.ignore.ref", "true"));
		final int minSentenceLength = Integer.parseInt(ApplicationSetup.getProperty("cornell.min.sentence.length", "50"));
		final double puncThreshold = Double.parseDouble(ApplicationSetup.getProperty("cornell.punctuation.threshold", "3.19d"));
		try{
			StringBuilder buf = new StringBuilder();
			String eol = ApplicationSetup.EOL;
			String startSentid = "";
			String endSentid = "";
			int sentCounter = 0;
			int processedSentCounter = 0;
			BufferedReader cornellReader = Files.openFileReader(cornellFilename);
			BufferedReader offsetReader = Files.openFileReader(offsetFilename);
			String cornellEntry = null; String offsetEntry = null;
			int rankCounter = -1;
			while ((offsetEntry=offsetReader.readLine())!=null){
				StringTokenizer stk = new StringTokenizer(offsetEntry);
				stk.nextToken();
				String offsetPmid = stk.nextToken();
				int rank = Integer.parseInt(stk.nextToken());// get rank
				stk.nextToken();// skip score
				String offset = stk.nextToken();
				String length = stk.nextToken();
				
				while ((cornellEntry=cornellReader.readLine())!=null){
					rankCounter++;
					if (rankCounter!=rank)
						continue;
					else{
						stk = new StringTokenizer(cornellEntry);
						String pmid = stk.nextToken();
						String sentid = stk.nextToken();
						String type = stk.nextToken();
						String sentence = cornellEntry.substring(cornellEntry.lastIndexOf(type)+
								type.length(), cornellEntry.length());
						sentence = sentence.trim().replaceAll("<", " ").replaceAll(">", " ");
						if (startSentid.length()==0)
							startSentid = sentid;
						if ((!type.equals("REF") || !ignoreRef) && (sentence.length() >= minSentenceLength)
								&& countPunctuations(sentence) < puncThreshold){
							buf.append("<doc><docno>"+pmid+"-"+offset+"-"+length
										+"</docno>"+sentence+"</doc>"+eol);
							sentCounter++;
						}else {
						//	logger.debug(cornellEntry+" ignored.");
						}
						processedSentCounter++;
						if (sentCounter >= maxSentPerFile || processedSentCounter == numberOfSentences){
							endSentid = sentid;
							String outputFilename = outputDir+ApplicationSetup.FILE_SEPARATOR+"sentences-"+startSentid+"-"+endSentid+".gz";
							BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
							bw.write(buf.toString());
							bw.close();
							logger.info(sentCounter+" sentences saved in file "+outputFilename+", "+
									(double)processedSentCounter/numberOfSentences*100+"% finished");
							startSentid = "";
							endSentid = "";
							sentCounter = 0;
							buf = null; bw = null;
							System.gc();
							buf = new StringBuilder();
						}	
						break;
					}
				}
				
				
			}
			cornellReader.close(); offsetReader.close();
		}catch(Exception ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void findUndoneSentences(String cornellFilename, String offsetFilename, 
			String outputFilename){
		try{
			// removing duplicate offsets
			System.out.println("Removing duplicate offsets...");
			removeDuplicates(offsetFilename);
			// load all ranks from offset file
			TIntHashSet rankSet = new TIntHashSet();
			BufferedReader br = Files.openFileReader(offsetFilename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			String line = null;
			System.out.println("Loading ranks from "+offsetFilename+"...");
			while ((line=br.readLine())!=null){
				StringTokenizer stk = new StringTokenizer(line);
				stk.nextToken();// skip 100
				stk.nextToken();// skip pmid
				int rank = Integer.parseInt(stk.nextToken());
				if (rankSet.contains(rank))
					System.out.println("duplicate rank "+rank+" found!");
				else
					rankSet.add(rank);
			}
			br.close();
			System.out.println(rankSet.size()+" ranks loaded.");
			// for each entry in cornell file, check and write
			br = Files.openFileReader(cornellFilename);
			System.out.println("Looking for undone entries in file "+cornellFilename);
			int undoneCounter = 0;
			while ((line=br.readLine())!=null){
				StringTokenizer stk = new StringTokenizer(line);
				stk.nextToken();// skip pmid
				stk.nextToken();// skip sentence id
				int rank = Integer.parseInt(stk.nextToken());
				if (!rankSet.contains(rank)){
					bw.write(line+ApplicationSetup.EOL);
					undoneCounter++;
				}
			}
			br.close();
			bw.close();
			System.out.println("Found "+undoneCounter+" undone entries.");
		}catch(Exception ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void createCornellSentenceCollection(String cornellFilename, String outputDir, int numberOfSentences){
		int maxSentPerFile = 200000;
		final boolean ignoreRef = Boolean.parseBoolean(ApplicationSetup.getProperty("cornell.ignore.ref", "true"));
		try{
			StringBuilder buf = new StringBuilder();
			String eol = ApplicationSetup.EOL;
			String startSentid = "";
			String endSentid = "";
			int sentCounter = 0;
			int processedSentCounter = 0;
			BufferedReader br = Files.openFileReader(cornellFilename);
			String str = null;
			while ((str=br.readLine())!=null){
				str = str.trim();
				if (str.length()==0) continue;
				StringTokenizer stk = new StringTokenizer(str);
				String pmid = stk.nextToken();
				String sentid = stk.nextToken();
				String type = stk.nextToken();
				String sentence = str.substring(str.lastIndexOf(type)+type.length(), str.length());
				if (startSentid.length()==0)
					startSentid = sentid;
				if (!type.equals("REF") || !ignoreRef){
					buf.append("<DOC><DOCNO>"+pmid+"-"+sentid+"-"+type
								+"</DOCNO>"+sentence.replaceAll("\u0096", " ").replaceAll("\u0097", " ")
								.replace("<", " ").replace(">", " ")
								+"</DOC>"+eol);
					sentCounter++;
				}
				processedSentCounter++;
				if (sentCounter >= maxSentPerFile || processedSentCounter == numberOfSentences){
					endSentid = sentid;
					String outputFilename = outputDir+ApplicationSetup.FILE_SEPARATOR+"sentences-"+startSentid+"-"+endSentid+".gz";
					BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
					bw.write(buf.toString());
					bw.close();
					logger.info(sentCounter+" sentences saved in file "+outputFilename+", "+
							(double)processedSentCounter/numberOfSentences*100+"% finished");
					startSentid = "";
					endSentid = "";
					sentCounter = 0;
					buf = null; bw = null;
					System.gc();
					buf = new StringBuilder();
				}
			}
			br.close();
		}catch(Exception ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void createCornellSentenceCollectionWithOffsets(String cornellFilename, 
			String offsetFilename, String outputDir, int numberOfSentences){
		// load offsets
		TIntIntHashMap sentidOffsetMap = new TIntIntHashMap();
		TIntIntHashMap sentidLengthMap = new TIntIntHashMap();
		
		try{
			logger.info("");
			logger.info("Loading offsets...");
			BufferedReader br = Files.openFileReader(offsetFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				StringTokenizer stk = new StringTokenizer(line);
				stk.nextToken();// skip dummy topicid
				int pmid = Integer.parseInt(stk.nextToken());
				int sentid = Integer.parseInt(stk.nextToken());
				stk.nextToken(); // skip score
				int offset = Integer.parseInt(stk.nextToken());
				int length = Integer.parseInt(stk.nextToken());
				sentidOffsetMap.put(sentid, offset);
				sentidLengthMap.put(sentid, length);
			}
			logger.info("Done. "+sentidOffsetMap.size()+" offsets loaded.");
			br.close();
		}catch(Exception ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		
		int maxSentPerFile = 200000;
		final boolean ignoreRef = Boolean.parseBoolean(ApplicationSetup.getProperty("cornell.ignore.ref", "true"));
		try{
			StringBuilder buf = new StringBuilder();
			String eol = ApplicationSetup.EOL;
			String startSentid = "";
			String endSentid = "";
			int sentCounter = 0;
			int processedSentCounter = 0;
			BufferedReader br = Files.openFileReader(cornellFilename);
			String str = null;
			int unknownCounter = 0;
			while ((str=br.readLine())!=null){
				str = str.trim();
				if (str.length()==0) continue;
				StringTokenizer stk = new StringTokenizer(str);
				String pmid = stk.nextToken();
				String sentidString = stk.nextToken();
				String type = stk.nextToken();
				String sentence = str.substring(str.lastIndexOf(type)+type.length(), str.length());
				if (startSentid.length()==0)
					startSentid = sentidString;
				if (!type.equals("REF") || !ignoreRef){
					int sentid = Integer.parseInt(sentidString);
					int offset = sentidOffsetMap.get(sentid);
					int length = sentidLengthMap.get(sentid);
					String docno = pmid+"-"+offset+"-"+length;
					if (offset==0 || length ==0)
						docno = docno+"-"+(unknownCounter++);
					buf.append("<DOC><DOCNO>"+docno
								+"</DOCNO>"+sentence.replace("<", " ").replace(">", " ")
								+"</DOC>"+eol);
					sentCounter++;
				}
				processedSentCounter++;
				if (sentCounter >= maxSentPerFile || processedSentCounter == numberOfSentences){
					endSentid = sentidString;
					String outputFilename = outputDir+ApplicationSetup.FILE_SEPARATOR+"sentences-"+startSentid+"-"+endSentid+".gz";
					BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
					bw.write(buf.toString());
					bw.close();
					logger.info(sentCounter+" sentences saved in file "+outputFilename+", "+
							(double)processedSentCounter/numberOfSentences*100+"% finished");
					startSentid = "";
					endSentid = "";
					sentCounter = 0;
					buf = null; bw = null;
					System.gc();
					buf = new StringBuilder();
				}
			}
			br.close();
			logger.info("Done. unknownCounter = "+unknownCounter);
		}catch(Exception ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void removeDuplicates(String offsetFilename){
		try{
			String outputFilename = offsetFilename+".tmp";
			// load all ranks from offset file
			TIntHashSet rankSet = new TIntHashSet();
			BufferedReader br = Files.openFileReader(offsetFilename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				StringTokenizer stk = new StringTokenizer(line);
				stk.nextToken();// skip 100
				stk.nextToken();// skip pmid
				int rank = Integer.parseInt(stk.nextToken());
				if (rankSet.contains(rank))
					continue;
				else{
					rankSet.add(rank);
					bw.write(line+ApplicationSetup.EOL);
				}
			}
			br.close();bw.close();
			if ((new File(offsetFilename)).delete())
				(new File(outputFilename)).renameTo(new File(offsetFilename));
		}catch(Exception ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if (args[0].equals("--printlegalspans")){
			// --printlegalspans docid
			try{
				MetaServer4a server = new MetaServer4a(ApplicationSetup.TERRIER_INDEX_PATH,
						ApplicationSetup.TERRIER_INDEX_PREFIX);
				Span[] passages = GenoPassageExtraction.getLegalSpans(server, Integer.parseInt(args[1]), "N/A");
				server.close();
				for (int i=0; i<passages.length; i++)
					if (passages[i].getSpanString().trim().length()!=0)
						System.out.println(">>>>>>>>>>>>>>>"+(i+1)+": "+passages[i].getSpanString());
			}catch(Exception ioe){
				ioe.printStackTrace();
				System.exit(1);
			}
		}else if (args[0].equals("--scoredocument")){
			// --scoredocument sentWModelName qemodelname docid pseudodocids
			GenoPassageExtraction app = new GenoPassageExtraction(args[1], args[2]);
			int docid = Integer.parseInt(args[3]);
			int[] pseudoDocids = new int[args.length - 4];
			for (int i=4; i<args.length; i++){
				pseudoDocids[i-4] = Integer.parseInt(args[i]);
			}
			app.sentenceScoring(docid, pseudoDocids);
		}else if (args[0].equals("--createlegalspancollection")){
			// --createlegalspancollection <cleanCollectionOutputDir> minlength
			int maxSpansPerFile = 50000;
			int minSpanLength = Integer.parseInt(args[2]);
			try{
				Index index = Index.createIndex();
				int N = index.getCollectionStatistics().getNumberOfDocuments();
				DocumentIndex docIndex = index.getDocumentIndex();
				MetaServer4a server = new MetaServer4a(ApplicationSetup.TERRIER_INDEX_PATH,
						ApplicationSetup.TERRIER_INDEX_PREFIX);
				StringBuilder buf = new StringBuilder();
				String eol = ApplicationSetup.EOL;
				String startDocno = "";
				String endDocno = "";
				int spanCounter = 0;
				String collDir = args[1];
				int docCounter = 0;
				for (int docid=0; docid<N; docid++){
					String docno = docIndex.getDocumentNumber(docid).trim();
					if (startDocno.length()==0)
						startDocno = docno;
					Span[] spans = GenoPassageExtraction.getLegalSpans(server, docid, docno);
					for (int i=0; i<spans.length; i++)
						if (minSpanLength <= 0 || spans[i].getSpanString().length()>=minSpanLength ){
							buf.append("<DOC><DOCNO>"+docno+"-"+spans[i].getOffset()+"-"+spans[i].getByteLength()
									+"</DOCNO>"+spans[i].getSpanString()+"</DOC>"+eol);
							spanCounter++;
						}else {
							logger.debug("Span "+docno+"-"+spans[i].getOffset()+"-"+spans[i].getByteLength()+" is ignored.");
						}
					docCounter++;
					if (spanCounter >= maxSpansPerFile || docid == N-1){
						endDocno = docno;
						String outputFilename = collDir+ApplicationSetup.FILE_SEPARATOR+"legalspans-"+startDocno+"-"+endDocno+".gz";
						BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
						bw.write(buf.toString());
						bw.close();
						logger.info(spanCounter+" legal spans saved in file "+outputFilename+", "+(double)docCounter/N*100+"% finished");
						startDocno = "";
						endDocno = "";
						spanCounter = 0;
						buf = null; spans = null; bw = null;
						System.gc();
						buf = new StringBuilder();
					}
				}
				index.close();
				server.close();
			}catch(Exception ioe){
				ioe.printStackTrace();
				System.exit(1);
			}
		}else if(args[0].equals("--convertresultfile")){
			// --convertresultfile <resultfilename> <outoutfilename>
			GenoPassageExtraction.convertResultFile(args[1], args[2]);
		}else if(args[0].equals("--createcornellsentencecollection")){
			// --createcornellsentencecollection <sentFilename> <outputDir> numberOfSentences
			GenoPassageExtraction.createCornellSentenceCollection(args[1], args[2], Integer.parseInt(args[3]));
		}else if(args[0].equals("--createcornellsentencecollectionwithoffsets")){
			// --createcornellsentencecollectionwithoffsets <sentFilename> <offsetFilename> <outputDir> numberOfSentences
			GenoPassageExtraction.createCornellSentenceCollectionWithOffsets(args[1], args[2], args[3], Integer.parseInt(args[4]));
		}
		else if(args[0].equals("--convertrawcornellsentences")){
			// --convertrawcornellsentences <sentFilename> <outputFilename>
			GenoPassageExtraction.convertRawCornellSentences(args[1], args[2], Boolean.parseBoolean(args[3]));
		}else if(args[0].equals("--createpreprocesscornellcollection")){
			// --createpreprocesscornellcollection cornellFilename offsetFilename outputDir
			// numberOfSentences
			GenoPassageExtraction.createPreprocessedCornellSentenceCollection(args[1], args[2], args[3], Integer.parseInt(args[4]));
		}else if(args[0].equals("--findundonesentences")){
			// --findundonesentences cornellFilename(for locator) offsetFilename outputfilename
			GenoPassageExtraction.findUndoneSentences(args[1], args[2], args[3]);
		}else if(args[0].equals("--removeduplicateoffsets")){
			// --removeduplicateoffsets offsetFilename
			GenoPassageExtraction.removeDuplicates(args[1]);
		}else if(args[0].equals("--createcollectionfrompool")){
			// --createcollectionfrompool rawqrelsfilename outputdir
			GenoPassageExtraction.createCollectionFromPool(args[1], args[2]);
		}else if(args[0].equals("--printspan")){
			// --printspan pmid offset length
			GenoPassageExtraction.printSpan(args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]));
		}else if(args[0].equals("--printcleanspan")){
			// --printcleanspan pmid offset length
			GenoPassageExtraction.printCleanSpan(args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]));
		}
	}

}

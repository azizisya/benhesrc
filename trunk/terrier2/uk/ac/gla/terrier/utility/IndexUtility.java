package uk.ac.gla.terrier.utility;

import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TObjectIntHashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.evaluation.TRECQrelsInMemory;
import uk.ac.gla.terrier.structures.DirectIndex;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.DocumentIndexEncoded;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.InvertedIndex;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.structures.LexiconEntry;
import uk.ac.gla.terrier.utility.io.RandomDataInput;

/**
 * This class provides methods for index-related utilities.
 * @author ben
 *
 */
public class IndexUtility {
	protected static Logger logger = Logger.getRootLogger();
	/**
	 * Convert a column of docnos to docids given the docid output file. Column is zero-based.
	 * @param docIdxOutputFilename
	 * @param filename
	 * @param column
	 * @return Mapping from docnos to docids.
	 */
	public static TObjectIntHashMap<String> getDocidsFromFile(String docIdxOutputFilename, String filename, int column){
		// load docnos in result file
			// initialise a mapping from docnos to spanids
		TObjectIntHashMap<String> docmap = new TObjectIntHashMap<String>();
		try{
			System.err.println("Check docnos in "+filename+ "...");
			BufferedReader br = Files.openFileReader(filename);
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
		System.out.println(docmap.size()+" docnos loaded from "+filename);
		// read through docid output file and find corresponding spanids
		try{
			System.err.println("Loading docidx ...");
			THashSet<String> processedDocnoSet = new THashSet<String>();
			BufferedReader br = Files.openFileReader(docIdxOutputFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(",");
				if (tokens.length!=5)
					continue;
				String docno = tokens[2].trim();
				if (docmap.containsKey(docno)){
					docmap.put(docno, Integer.parseInt(tokens[0].trim()));
					processedDocnoSet.add(docno);
				}else if (processedDocnoSet.size() == docmap.size())
					break;
			}
			br.close();
			System.out.println(docmap.size()+" entries loaded.");
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		return docmap;
	}
	
	public static int getTermFrequencyFromDirect(DirectIndex di, int termid, int docid){
		int[][] terms = di.getTerms(docid);
		if (terms == null)
			return 0;
		int index = Arrays.binarySearch(terms[0], termid);
		int tf = index>=0?terms[1][index]:0;
		terms = null;
		return tf;
	}
	
	public static int[] getTermFrequenciesFromInverted(InvertedIndex invIdx, int termid, int docids[]){
		int[][] postings = invIdx.getDocuments(termid);
		int[] tfs = new int[docids.length];
		for (int i=0; i<docids.length; i++){
			int index = Arrays.binarySearch(postings[0], docids[i]);
			tfs[i] = index>=0?postings[1][index]:0;
		}
		postings = null;
		return tfs;
	}

	public static int getTermFrequencyFromInverted(InvertedIndex invIdx, int termid, int docid){
		int[][] postings = invIdx.getDocuments(termid);
		int index = Arrays.binarySearch(postings[0], docid);
		int tf = index>=0?postings[1][index]:0;
		postings = null;
		return tf;
	}
	
	public static int[] getTermFrequenciesFromDirect(DirectIndex di, int docid, int[] termids){
		int[][] terms = di.getTerms(docid);
		int[] tfs = new int[termids.length];
		if (terms==null){
			Arrays.fill(tfs, 0);
			return tfs;
		}
		for (int i=0; i<termids.length; i++){
			int index = Arrays.binarySearch(terms[0], termids[i]);
			tfs[i] = index>=0?terms[1][index]:0;
		}
		return tfs;
	}
	
	public static void docids2docnosInResults(String docidxFilename, String resultFilename, String outputFilename){
		try{
			DocumentIndex docidx = new DocumentIndex(docidxFilename);
			BufferedReader br = Files.openFileReader(resultFilename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				int docid = Integer.parseInt(tokens[2]);
				String docno = docidx.getDocumentNumber(docid);
				bw.write(tokens[0]+" "+tokens[1]+" "+docno+" "+tokens[3]+" "+tokens[4]+
						" "+tokens[5]+ApplicationSetup.EOL);
			}
			bw.close(); br.close(); docidx.close();
		}
		catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Convert docnos in a result file to docids.
	 * @param docIdxOutputFilename
	 * @param resultFilename
	 * @param outputFilename
	 */
	public static void convertResultsWithDocid(String docidxFilename, String resultFilename, String outputFilename){
		// load docno -> docid map
		TObjectIntHashMap<String> docmap = new TObjectIntHashMap<String>();
		try{
			System.err.println("Check docnos in "+resultFilename+ "...");
			BufferedReader br = Files.openFileReader(resultFilename);
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
			DocumentIndex docidx = new DocumentIndex(docidxFilename);
			int N = docidx.getNumberOfDocuments();
			byte[] buffer = new byte[docidx.DOCNO_BYTE_LENGTH];
			RandomDataInput docIndex = Files.openFileRandom(docidxFilename);
			int docid = 0; int docLength = 0; String docno = null; 
			long endOffset = 0L; byte endBitOffset = 0;
			int counter = 0;
			for (int i = 0; i < N; i++) {
				docIndex.seek(i * docidx.entryLength);
				docid = docIndex.readInt();
				docLength = docIndex.readInt();
				docIndex.readFully(buffer, 0, docidx.DOCNO_BYTE_LENGTH);
				docno = new String(buffer).trim();
				endOffset = docIndex.readLong();
				endBitOffset = docIndex.readByte();
				if (docmap.containsKey(docno)){
					docmap.put(docno, docid);
					counter++;
					if (counter == docmap.size())
						break;
				}
			}
			System.out.println(docmap.size()+" entries loaded.");
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		
		try{
			System.err.println("Converting docnos to docids for "+resultFilename+"...");
			BufferedReader br = Files.openFileReader(resultFilename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			String line = null;
			StringBuffer buf = new StringBuffer();
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				String queryid = tokens[0];
				String Q0 = tokens[1];
				int docid = docmap.get(tokens[2]);
				int rank = Integer.parseInt(tokens[3]);
				double score = Double.parseDouble(tokens[4]);
				String tag = tokens[5];
				buf.append(queryid+" Q0 "+docid+" "+rank+" "+score+" "+tag+ApplicationSetup.EOL);
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
	/**
	 * Sort the collection specification file by file basenames.
	 * @param filename Path to the collection specification file.
	 */
	public static void sortCollectionSpec(String filename){
		ArrayList<String> basenameList = new ArrayList<String>();
		THashMap<String, String> nameMap = new THashMap<String, String>(); // mapping from basenames to full filenames
		try{
			BufferedReader br = Files.openFileReader(filename);
			String str = null;
			while ((str=br.readLine())!=null){
				String basename = str.substring(str.lastIndexOf('/')+1, str.length());
				basenameList.add(basename);
				nameMap.put(basename, str);
			}
			br.close();
			String[] basenames = (String[])basenameList.toArray(new String[basenameList.size()]);
			Arrays.sort(basenames);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(filename+".sorted");
			String eol = ApplicationSetup.EOL;
			for (int i=0; i<basenames.length; i++)
				bw.write(nameMap.get(basenames[i])+eol);
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		System.out.println("Sorted filenames written in file "+filename+".sorted");
	}
	
	/**
	 * Dump indexed content of a document.
	 * @param docid
	 * @param index
	 */
	public static void dumpDoc(int docid, Index index){
		DirectIndex directIndex = index.getDirectIndex();
		Lexicon lexicon = index.getLexicon();
		dumpDoc(docid, directIndex, lexicon);
	}
	
	public static void dumpDoc(int docid, DirectIndex directIndex, Lexicon lexicon){
		int[][] pointers = directIndex.getTerms(docid);
		int blockindex = 0;
		for (int i = 0; i < pointers[0].length; i++) {
			int termid = pointers[0][i];
			int tf = pointers[1][i];
			String term = lexicon.getLexiconEntry(pointers[0][i]).term;
			System.out.println(termid+", "+term+", "+tf);
			/*try{
			System.out.print(
				"("
					+ pointers[0][j]
					+ ", "
					+ lexicon.getLexiconEntry(pointers[0][j]).term
					+ ", "
					+ pointers[1][j]
					+ ", "
					+ pointers[2][j]
					+ ", "
					+ pointers[3][j]);
			}catch(NullPointerException e){
				System.err.println("lexEntry: "+lexicon.getLexiconEntry(pointers[0][j]));
				System.err.println("termid: "+pointers[0][j]);
				e.printStackTrace();
				System.exit(1);
			}*/
			/*for (int k = 0; k < pointers[3][j]; k++) {
				System.out.print(", " + pointers[4][blockindex]);
				blockindex++;
			}
			System.out.print(")");*/
		}
	}
	/**
	 * Get identifiers and summed frequencies of terms in an array of documents. 
	 * @param docids
	 * @param freqMap A mapping from termids to summed frequencies.
	 * @param di
	 */
	public static void getTerms(int[] docids, TIntIntHashMap freqMap, DirectIndex di){
		for (int i=0; i<docids.length; i++){
			int[][] terms = di.getTerms(docids[i]);
			for (int j=0; j<terms[0].length; j++){
				freqMap.adjustOrPutValue(terms[0][j], terms[1][j], terms[1][j]);
			}
		}
	}
	/**
	 * Convert docids of a given index to those of another index.
	 * @param qrelsFilename
	 * @param indexPath1 Index path of the original docids.
	 * @param indexPrefix1
	 * @param docidxOutputFilename Document index output of the index to which docids are converted.
	 * @param outputFilename
	 */
	/**
	public static void convertQrelsDocids(String qrelsFilename, 
			String indexPath1, String indexPrefix1,
			String docidxOutputFilename, String outputFilename){
		try{
			// convert original docids to original docnos
			Index index = Index.createIndex(indexPath1, indexPrefix1);
			DocumentIndex docindex = index.getDocumentIndex();
			BufferedReader br = Files.openFileReader(qrelsFilename);
			String line = null; StringBuffer buf = new StringBuffer();
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				String qid = tokens[0];
				int docid = Integer.parseInt(tokens[2]);
				String label = tokens[3];
				String docno = docindex.getDocumentNumber(docid);
				buf.append(qid+" 0 "+docno+" "+label+ApplicationSetup.EOL);
			}
			br.close();
			index.close();
			String tmpFilename = qrelsFilename+".tmp";
			File tmpFile = new File(tmpFilename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(tmpFile);
			bw.write(buf.toString());
			bw.close();
			// convert qrels docids
			convertFeedbackQrels(docidxOutputFilename, tmpFilename, outputFilename);
			tmpFile.deleteOnExit();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	*/
	/**
	public static void convertFeedbackQrels(String docIdxOutputFilename, String feedbackFilename, String outputFilename){
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
		
		try{
			System.err.println("Converting docnos to docids for "+feedbackFilename+"...");
			BufferedReader br = Files.openFileReader(feedbackFilename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				int docid = docmap.get(tokens[2]);
				if (docid>=0)
					bw.write(tokens[0]+" "+tokens[1]+" "+docid+" "+tokens[3]+ApplicationSetup.EOL);
			}
			br.close(); bw.close();
			System.out.println("Done. Saved in "+outputFilename);
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		
		// load feedback file and convert
	}*/
	
	public static void convertFeedbackQrels(String docidxFilename, String feedbackFilename, String outputFilename){
		// load docno -> docid map
		TObjectIntHashMap<String> docmap = new TObjectIntHashMap<String>();
		try{
			System.err.println("Check docnos in "+feedbackFilename+ "...");
			BufferedReader br = Files.openFileReader(feedbackFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				// String[] tokens = line.split(" ");
				StringTokenizer stk = new StringTokenizer(line);
				stk.nextToken();// topic id
				stk.nextToken();// dummy 0
				String docno = stk.nextToken();
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
			DocumentIndex docidx = new DocumentIndex(docidxFilename);
			int N = docidx.getNumberOfDocuments();
			byte[] buffer = new byte[docidx.DOCNO_BYTE_LENGTH];
			RandomDataInput docIndex = Files.openFileRandom(docidxFilename);
			int docid = 0; int docLength = 0; String docno = null; 
			long endOffset = 0L; byte endBitOffset = 0;
			int counter = 0;
			for (int i = 0; i < N; i++) {
				docIndex.seek(i * docidx.entryLength);
				docid = docIndex.readInt();
				docLength = docIndex.readInt();
				docIndex.readFully(buffer, 0, docidx.DOCNO_BYTE_LENGTH);
				docno = new String(buffer).trim();
				endOffset = docIndex.readLong();
				endBitOffset = docIndex.readByte();
				if (docmap.containsKey(docno)){
					docmap.put(docno, docid);
					counter++;
					if (counter == docmap.size())
						break;
				}
			}
			System.out.println(docmap.size()+" entries loaded.");
			docIndex.close(); docidx.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		// load feedback file and convert
		try{
			System.err.println("Converting docnos to docids for "+feedbackFilename+"...");
			BufferedReader br = Files.openFileReader(feedbackFilename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			String line = null;
			while ((line=br.readLine())!=null){
//				 String[] tokens = line.split(" ");
				StringTokenizer stk = new StringTokenizer(line);
				String qid = stk.nextToken();// topic id
				String dummy = stk.nextToken();// dummy 0
				int docid = docmap.get(stk.nextToken());
				String rel = stk.nextToken();
				if (docid>=0)
					bw.write(qid+" "+dummy+" "+docid+" "+rel+ApplicationSetup.EOL);
			}
			br.close(); bw.close();
			System.out.println("Done. Saved in "+outputFilename);
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void prelsToDocids(String docidxFilename, String feedbackFilename, String outputFilename){
		// load docno -> docid map
		TObjectIntHashMap<String> docmap = new TObjectIntHashMap<String>();
		try{
			System.err.println("Check docnos in "+feedbackFilename+ "...");
			BufferedReader br = Files.openFileReader(feedbackFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				// String[] tokens = line.split(" ");
				StringTokenizer stk = new StringTokenizer(line);
				stk.nextToken();// topic id
				String docno = stk.nextToken();
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
			DocumentIndex docidx = new DocumentIndex(docidxFilename);
			int N = docidx.getNumberOfDocuments();
			byte[] buffer = new byte[docidx.DOCNO_BYTE_LENGTH];
			RandomDataInput docIndex = Files.openFileRandom(docidxFilename);
			int docid = 0; int docLength = 0; String docno = null; 
			long endOffset = 0L; byte endBitOffset = 0;
			int counter = 0;
			for (int i = 0; i < N; i++) {
				docIndex.seek(i * docidx.entryLength);
				docid = docIndex.readInt();
				docLength = docIndex.readInt();
				docIndex.readFully(buffer, 0, docidx.DOCNO_BYTE_LENGTH);
				docno = new String(buffer).trim();
				endOffset = docIndex.readLong();
				endBitOffset = docIndex.readByte();
				if (docmap.containsKey(docno)){
					docmap.put(docno, docid);
					counter++;
					if (counter == docmap.size())
						break;
				}
			}
			System.out.println(docmap.size()+" entries loaded.");
			docIndex.close(); docidx.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		// load feedback file and convert
		try{
			System.err.println("Converting docnos to docids for "+feedbackFilename+"...");
			BufferedReader br = Files.openFileReader(feedbackFilename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			String line = null;
			while ((line=br.readLine())!=null){
//				 String[] tokens = line.split(" ");
				StringTokenizer stk = new StringTokenizer(line);
				String qid = stk.nextToken();// topic id
				int docid = docmap.get(stk.nextToken());
				String rel1 = stk.nextToken();
				String rel2 = stk.nextToken();
				String rel3 = stk.nextToken();
				if (docid>=0)
					bw.write(qid+" "+docid+" "+rel1+" "+rel2+" "+rel3+ApplicationSetup.EOL);
			}
			br.close(); bw.close();
			System.out.println("Done. Saved in "+outputFilename);
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	/**
	 * 
	 * @param docidxFilename
	 * @param docnosFilename
	 * @param column 0-based
	 * @param outputFilename
	 */
	public static void docnosToDocids(String docidxFilename, String docnosFilename, int column, String outputFilename){
		// load docno -> docid map
		TObjectIntHashMap<String> docmap = new TObjectIntHashMap<String>();
		try{
			System.err.println("Check docnos in "+docnosFilename+ "...");
			BufferedReader br = Files.openFileReader(docnosFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				// String[] tokens = line.split(" ");
				StringTokenizer stk = new StringTokenizer(line);
				for (int i=0; i<column; i++)
					stk.nextToken();// skip
				String docno = stk.nextToken();
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
			DocumentIndex docidx = new DocumentIndex(docidxFilename);
			int N = docidx.getNumberOfDocuments();
			byte[] buffer = new byte[docidx.DOCNO_BYTE_LENGTH];
			RandomDataInput docIndex = Files.openFileRandom(docidxFilename);
			int docid = 0; int docLength = 0; String docno = null; 
			long endOffset = 0L; byte endBitOffset = 0;
			int counter = 0;
			for (int i = 0; i < N; i++) {
				docIndex.seek(i * docidx.entryLength);
				docid = docIndex.readInt();
				docLength = docIndex.readInt();
				docIndex.readFully(buffer, 0, docidx.DOCNO_BYTE_LENGTH);
				docno = new String(buffer).trim();
				endOffset = docIndex.readLong();
				endBitOffset = docIndex.readByte();
				if (docmap.containsKey(docno)){
					docmap.put(docno, docid);
					counter++;
					if (counter == docmap.size())
						break;
				}
			}
			System.out.println(docmap.size()+" entries loaded.");
			docIndex.close(); docidx.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		// load feedback file and convert
		try{
			System.err.println("Converting docnos to docids for "+docnosFilename+"...");
			BufferedReader br = Files.openFileReader(docnosFilename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				StringTokenizer stk = new StringTokenizer(line);
				StringBuffer buf = new StringBuffer();
				int counter = 0;
				int docid = -1;
				while (stk.hasMoreTokens()){
					if (counter==column){
						docid = docmap.get(stk.nextToken());
						buf.append(docid+" ");
					}
					else
						buf.append(stk.nextToken()+" ");
					counter++;
				}
				if (docid>=0)
					bw.write(buf.toString()+ApplicationSetup.EOL);
			}
			br.close(); bw.close();
			System.out.println("Done. Saved in "+outputFilename);
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void convertDocnoToDocidFromResults(String docidxFilename, String resultFilename, String outputFilename){
		// load docno -> docid map from results
		TObjectIntHashMap<String> docmap = new TObjectIntHashMap<String>();
		try{
			System.err.println("Check docnos in "+resultFilename+ "...");
			BufferedReader br = Files.openFileReader(resultFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				//String[] tokens = line.split(" ");
				StringTokenizer stk = new StringTokenizer(line);
				stk.nextToken();// skip queryid
				stk.nextToken();// skip Q0
				String docno = stk.nextToken();
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
			DocumentIndex docidx = new DocumentIndex(docidxFilename);
			int N = docidx.getNumberOfDocuments();
			byte[] buffer = new byte[docidx.DOCNO_BYTE_LENGTH];
			RandomDataInput docIndex = Files.openFileRandom(docidxFilename);
			int docid = 0; int docLength = 0; String docno = null; 
			long endOffset = 0L; byte endBitOffset = 0;
			int counter = 0;
			for (int i = 0; i < N; i++) {
				docIndex.seek(i * docidx.entryLength);
				docid = docIndex.readInt();
				docLength = docIndex.readInt();
				docIndex.readFully(buffer, 0, docidx.DOCNO_BYTE_LENGTH);
				docno = new String(buffer).trim();
				endOffset = docIndex.readLong();
				endBitOffset = docIndex.readByte();
				if (docmap.containsKey(docno)){
					docmap.put(docno, docid);
					counter++;
					if (counter == docmap.size())
						break;
				}
			}
			System.out.println(docmap.size()+" entries loaded.");
			docIndex.close(); docidx.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		
		try{
			System.err.println("Converting docnos to docids for "+resultFilename+"...");
			BufferedReader br = Files.openFileReader(resultFilename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			String line = null;
			StringBuffer buf = new StringBuffer();
			while ((line=br.readLine())!=null){
				StringTokenizer stk = new StringTokenizer(line);
				String queryid = stk.nextToken();
				stk.nextToken();// skip Q0
				String docno = stk.nextToken();
				int docid = docmap.get(docno);
				String rank = stk.nextToken();
				String score = stk.nextToken();
				String tag = stk.nextToken();
				buf.append(queryid+" Q0 "+docid+" "+rank+" "+score+" "+tag+ApplicationSetup.EOL);
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
	
	public static void printTermStats(String lexiconFilename, String term){
		Lexicon lexicon = new Lexicon(lexiconFilename);
		LexiconEntry lexEntry = lexicon.getLexiconEntry(term);
		if (lexEntry == null)
			System.out.println("Term "+term+" not found in lexicon.");
		else
			System.out.println(term+", id: "+lexEntry.termId+", TF: "+lexEntry.TF+", DF: "+lexEntry.n_t);
	}
	
	/**
	 * Get number of documents in which a given term co-occurs with a set of given terms.
	 * @param termid
	 * @param termids
	 * @param invIndex
	 * @return
	 */
	public static int getCooccurrenceInCollection(int termid, int[] toRetain, InvertedIndex invIndex, int numberOfDocuments){
		TIntHashSet docidSet = new TIntHashSet();
		docidSet.addAll(invIndex.getDocuments(termid)[0]);
		docidSet.retainAll(toRetain);
		return docidSet.size();
		/*int[] retain = invIndex.getDocuments(termid)[0];
		for (int i=0; i<termids.length; i++)
			retain = retainAll(retain, invIndex.getDocuments(termids[i])[0]);
		return retain.length;*/
	}
	/**
	 * Get identifiers of documents in which a given set of terms co-occur.
	 * @param termids
	 * @param invIndex
	 * @return
	 */
	public static int[] getCooccurredDocuments(int[] termids, InvertedIndex invIndex){
		TIntHashSet docidSet = new TIntHashSet();
		int[][] pointers = invIndex.getDocuments(termids[0]);
		docidSet.addAll(invIndex.getDocuments(termids[0])[0]);
		for (int i=1;i<termids.length;i++)
			docidSet.retainAll(invIndex.getDocuments(termids[i])[0]);
		return docidSet.toArray();
	}
	
	public static void main(String[] args){
		if (args[0].equals("--dumpdoc")){
			// --dumpdoc indexpath indexprefix docid
			IndexUtility.dumpDoc(Integer.parseInt(args[3]), Index.createIndex(args[1], args[2]));
		}
	}
}

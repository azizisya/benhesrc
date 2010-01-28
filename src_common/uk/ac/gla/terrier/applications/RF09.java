package uk.ac.gla.terrier.applications;

import gnu.trove.TObjectIntHashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import uk.ac.gla.terrier.evaluation.TRECQrelsInMemory;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.io.RandomDataInput;

public class RF09 {
	protected final static String docnoTag = "WARC-TREC-ID";
	
	protected final static String docHead = "WARC/0.18";
	
	protected final static String clTag = "Content-Length";
	
	public static void preProcessFile(String filename, String outputFilename){
		try{
			BufferedReader br = Files.openFileReader(filename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			String line = null;
			StringBuffer docBuf = new StringBuffer();
			String EOL = ApplicationSetup.EOL;
			boolean inDoc = false; String currentDocno = null;
			int counter = 0; int clCounter = 0;
			while ((line=br.readLine())!=null){
				line = line.trim();
				if (line.startsWith(docHead)){
					if (inDoc){
						// write buffer
						docBuf.append("</DOC>"+EOL);
						bw.write(docBuf.toString());
						docBuf = null;
						counter++;
						System.out.println("Flushed "+currentDocno+" to disk. "+counter+" documents processed.");
						if (counter % 1000 == 0)
							System.gc();
						inDoc = false;
					}
					// else
						// ignore
				}else if (line.startsWith(docnoTag)){
					// update docno
					currentDocno = line.split(" ")[1].trim();
					clCounter=0;
					// System.err.println("Docno: "+currentDocno);
				}else if (line.startsWith(clTag)){
					if (inDoc)
						docBuf.append(line+EOL);
					else{
						clCounter++;
						inDoc = (clCounter==2);
						if (inDoc){
							// indoc
							// start expanding buffer
							docBuf = new StringBuffer();
							docBuf.append("<DOC><DOCNO>"+currentDocno+"</DOCNO>"+EOL);
							//if (line.indexOf(' ') > 0){
								//docBuf.append(line.substring(0, line.indexOf(' '))+">"+EOL);
							//}else
								// docBuf.append(line+EOL);
						}
					}
				}else{
					if (inDoc)
						docBuf.append(line+EOL);
				}
			}
			// if (inDoc){
				// write buffer
				docBuf.append("</DOC>");
				bw.write(docBuf.toString());
			//}
			bw.close(); br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void createPhase1DocnoDocidMap(String docidxFilename, String feedbackFilename, String outputFilename){
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
				String[] tokens = line.split(" ");
				int docid = docmap.get(tokens[2]);
				if (docid>=0)
					bw.write(tokens[0]+" "+tokens[2]+" "+docid+" "+tokens[3]+ApplicationSetup.EOL);
			}
			br.close(); bw.close();
			System.out.println("Done. Saved in "+outputFilename);
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void results2Qrels(String qrelsFilename, String docmapFilename, String resultFilename, String outputFilename){
		// load the map
		TObjectIntHashMap<String> docmap = new TObjectIntHashMap<String>(); // map from docno to a map from docid to relevance grade
		try{
			BufferedReader br = Files.openFileReader(docmapFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				String docno = tokens[1];
				int docid = Integer.parseInt(tokens[2]);
				docmap.put(docno, docid);
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		TRECQrelsInMemory qrels = new TRECQrelsInMemory(qrelsFilename);
		// load the result file and convert
		try{
			BufferedReader br = Files.openFileReader(resultFilename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				if (tokens.length!=6)
					continue;
				String qid = tokens[0];
				String docno = tokens[2];
				// int rank = Integer.parseInt(tokens[3]);
				// double score = Double.parseDouble(tokens[4]);
				// String tag = tokens[5];
				// System.out.println("docno="+docno+", qid="+qid);
				int rel = (qrels.isRelevant(qid, docno))?(1):(0);
				int docid = docmap.get(docno);
				bw.write(qid+" 0 "+docid+" "+rel+ApplicationSetup.EOL);
			}
			br.close(); bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args[0].equalsIgnoreCase("--preprocessfile")){
			// --preprocessfile filename, outputFilename
			RF09.preProcessFile(args[1], args[2]);
		}else if (args[0].equalsIgnoreCase("--createp1docmap")){
			// docidxFilename, feedbackFilename, outputFilename
			RF09.createPhase1DocnoDocidMap(args[1], args[2], args[3]);
		}else if (args[0].equals("--results2qrels")){
			// --results2qrels qrelsFilename, docmapFilename, resultFilename, outputFilename
			RF09.results2Qrels(args[1], args[2], args[3], args[4]);
		}

	}

}

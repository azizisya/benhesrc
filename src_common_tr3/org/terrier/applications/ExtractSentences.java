/*
 * Created on 4 Mar 2008
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.terrier.applications;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.regex.Pattern;

import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.Files;


public class ExtractSentences {
	
	public static void convertRawMetaFile(String rawExtractFilename,
			String outputFilename,
			String idtag, String contentTag){
		try{
			BufferedReader br = Files.openFileReader(rawExtractFilename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			String EOL = ApplicationSetup.EOL;
			String str = null;
			String id = "";
			String content = "";
			while ((str=br.readLine())!=null){
				str = str.trim();
				if (str.startsWith("<"+idtag+">")){
					id = str.substring(str.indexOf('>')+1, str.lastIndexOf('<')).trim();
				}
				else if (str.startsWith("<"+contentTag+">")){
					content = str.substring(str.indexOf('>')+1, str.lastIndexOf('<')).trim();
					//System.out.println(id+" "+content);
					bw.write(id+" "+content+EOL);
					id = ""; content = "";
				}
			}
			br.close();
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	// extract 
	public static void extractSentenes(String cleanExtractFilename, String indexPath, String indexPrefix){
		Pattern sentencePattern = Pattern.compile("\\.\\s+|!\\s+|\\|\\s+|\\?\\s+");
		try{
			//BufferedWriter filenamebw = (BufferedWriter)Files.writeFileWriter(indexPath+
				//	ApplicationSetup.FILE_SEPARATOR+indexPrefix+".filenames.gz");
			BufferedWriter sentencebw = (BufferedWriter)Files.writeFileWriter(indexPath+
					ApplicationSetup.FILE_SEPARATOR+indexPrefix+".sentences.gz");
			//BufferedWriter offsetbw = (BufferedWriter)Files.writeFileWriter(indexPath+
					//ApplicationSetup.FILE_SEPARATOR+indexPrefix+".sentoffsets.gz");
			//FileInputStream br = (FileInputStream)Files.openFileStream(cleanExtractFilename);
			BufferedReader br = Files.openFileReader(cleanExtractFilename);
			String EOL = ApplicationSetup.EOL;
			String str = null;
			long offset = 0L;
			int docCounter = 0;
			while ((str=br.readLine())!=null){
				String docno = str.substring(0, str.indexOf(' ')).trim();
				offset += docno.length()+1;
				str = str.substring(str.indexOf(' '), str.length());
				// split sentences
				String[] sentences = sentencePattern.split(str, 1000);
				// get sentence offsets
				/*long[] sentOffsets = new long[sentences.length];
				for (int i=0; i<sentences.length; i++){
					sentOffsets[i] = offset;
					offset += (sentences[i].length()+1);
				}
				// EOL
				offset++;*/
				// write 
					// write sentences
				sentencebw.write(docno+" ");
				for (int i=0; i<sentences.length; i++){
					sentences[i] = "<sent>"+sentences[i].trim()+"</sent>";
					// write sentence offsets
					sentencebw.write((i==sentences.length-1)?(sentences[i]):(sentences[i]));
				}
				sentencebw.write(EOL);
				
			}
			br.close();
			//filenamebw.close();
			sentencebw.close();
			//offsetbw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args[0].equals("--extractsentences")){
			String cleanExtractFilename = args[1];
			String indexPath = args[2];
			String indexPrefix = args[3];
			ExtractSentences.extractSentenes(cleanExtractFilename, indexPath, indexPrefix);
		}else if (args[0].equals("--convertrawmetafile")){
			String rawExtractFilename = args[1];
			String outputFilename = args[2];
			String idtag = args[3];
			String contentTag = args[4];
			ExtractSentences.convertRawMetaFile(rawExtractFilename, outputFilename, idtag, contentTag);
		}
	}

}

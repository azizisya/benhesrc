/*
 * Created on 2005-1-14
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package tests;

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
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import uk.ac.gla.terrier.structures.DirectIndex;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.DocumentIndexInputStream;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.structures.LexiconInputStream;
import uk.ac.gla.terrier.structures.UTFLexicon;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;

/**
 * @author ben
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class TestIndex {
	
	public void printTerm(String term){
		Lexicon lexicon = Index.createIndex().getLexicon();
		if (!lexicon.findTerm(term)){
			System.out.println("Term " + term + " does not exist in the lexicon.");
		}
		else{
			System.out.println("Nt = " + lexicon.getNt() +
					", F_t = " +lexicon.getTF() +
					", termid: " + lexicon.getTermId());
		}
	}
	
	static public void createLexiconIndex(String lexFilename, String outputFilename){
		// get maxTermid
		try{
			Lexicon lexicon = new Lexicon(lexFilename);
			long maxTermid = lexicon.getNumberOfLexiconEntries()-1;
			lexicon.close();
			createLexiconIndex(lexFilename, outputFilename, (int)maxTermid);
		}catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
		// call method
	}
	
	static public void closeTag(String filename, String outFilename, String tag){
		try{
			BufferedReader br = null;
			BufferedWriter bw = null;
			if (filename.toLowerCase().endsWith(".gz")){
				br = new BufferedReader(new InputStreamReader(
		                new GZIPInputStream(new FileInputStream(filename))));
			}
			else br = new BufferedReader(new FileReader(new File(filename)));
			
			if (outFilename.toLowerCase().endsWith(".gz")){
				bw = new BufferedWriter(new OutputStreamWriter(
						new GZIPOutputStream(new FileOutputStream(outFilename))));
			}
			else bw = new BufferedWriter(new FileWriter(new File(outFilename)));
			String openTag = "<"+tag.toLowerCase()+" ";
			String str = null;
			String eol = ApplicationSetup.EOL;
			while ((str=br.readLine())!=null){
				if (str.trim().toLowerCase().startsWith(openTag))
					bw.write(openTag.trim()+">"+eol);
				else bw.write(str+eol);
			}
			
			bw.close();
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	static public void printDocumentInDirectIndex(int docid){
		Index index = Index.createIndex();
		DirectIndex directIndex = index.getDirectIndex();
		Lexicon lex = index.getLexicon();
		int[][] terms = directIndex.getTerms(docid);
		for (int i=0; i<terms[0].length; i++){
			lex.findTerm(terms[0][i]);
			System.out.print("("+lex.getTerm()+", "+lex.getTermId()+", "+terms[1][i]+")");
		}
		index.close();
	}
	
	static public void createLexiconIndex(
			String lexFilename, 
			String outputFilename,
			int maxTermid
			) throws IOException {
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
		
		
		DataInputStream lexicon =
			new DataInputStream(
				new BufferedInputStream(new FileInputStream(lexiconFile)));
		//the i-th element of offsets contains the offset in the
		//lexicon file of the term with term identifier equal to i.
		//long[] offsets = new long[lexiconEntries];
		final int termLength = ApplicationSetup.STRING_BYTE_LENGTH;
		int termid;
		byte[] buffer = new byte[termLength];
		//File lexid = new File(outputFilename);
		DataOutputStream dosLexid =
			new DataOutputStream(
				new BufferedOutputStream(new FileOutputStream(lexid)));
		long[] offsets = new long[maxTermid+1];
		Arrays.fill(offsets, 0L);
		for (int i = 0; i < lexiconEntries; i++) {
			int read = lexicon.read(buffer, 0, termLength);
			termid = lexicon.readInt();
			int docFreq = lexicon.readInt();
			int freq = lexicon.readInt();
			try{
				offsets[termid] = i * Lexicon.lexiconEntryLength;
			}catch(ArrayIndexOutOfBoundsException e){
				System.err.println("termid: "+termid+", maxTermid: "+maxTermid);
				e.printStackTrace();
				System.exit(1);
			}
			lexicon.readLong();
			lexicon.readByte();
		}
		for (int i=0; i<maxTermid; i++)
			dosLexid.writeLong(offsets[i]);
		lexicon.close();
		//save the offsets to a file with the same name as 
		//the lexicon and extension .lexid
		dosLexid.close();
	}
	
	static public void createDocnoIdMapping(String indexPath, String indexPrefix){
		try{
			DocumentIndexInputStream docindex = new DocumentIndexInputStream(indexPath, indexPrefix);
			String outFilename = indexPath+ApplicationSetup.FILE_SEPARATOR+indexPrefix+".map.gz";
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outFilename);
			String eol = ApplicationSetup.EOL;
			while ((docindex.readNextEntry())!=-1){
				bw.write(docindex.getDocumentNumber()+" "+docindex.getDocumentId()+" "
						+docindex.getDocumentLength()+eol);
			}
			bw.close();
			docindex.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	static public void printAverageFrequencyOfDictionary(String lexiconFilename, String filename){
		Lexicon lex = new Lexicon(lexiconFilename);
		try{
			double TF = 0;
			BufferedReader br = Files.openFileReader(filename);
			String str = null;
			int found = 0;
			int notFound = 0;
			while ((str=br.readLine())!=null){
				if (lex.findTerm(str.trim())){
					TF+=lex.getTF();
					found++;
				}else
					notFound++;
			}
			System.out.println("Average frequency in collection: "+TF/found);
			System.out.println("Found: "+found+", not found: "+notFound);
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		
		lex.close();
	}
	
	static void printNumberOfTokensLexicon(String lexiconFilename){
		LexiconInputStream lis = new LexiconInputStream(lexiconFilename);
		long count = 0L;
		try{
			while (lis.readNextEntry()!=-1){
				count+=lis.getTF();
			}
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		System.out.println("Number of tokens: "+count);
		lis.close();
	}
	
	public void dumpLexicon(String lexiconFilename){
		
		Lexicon lexicon = (ApplicationSetup.getProperty("string.use_utf", "false").equals("true"))?
				(new UTFLexicon(lexiconFilename)):(new Lexicon(lexiconFilename));
		lexicon.print();
	}

	public static void main(String[] args) {
		TestIndex app = new TestIndex();
		if (args[1].equalsIgnoreCase("--pterm")){
			app.printTerm(args[2]);
		}else if (args[1].equals("--dumplexicon")){
			// -i --dumplexicon <lexiconfilename>
			app.dumpLexicon(args[2]);
		}else if (args[1].equals("--createlexiconindex")){
			// -i --createlexiconindex <lexiconfilename> <outputfilename>
			// -i --createlexiconindex /users/tr.ben/indices/GOV/fields_full/global.lex \ 
			// /users/tr.ben/indices/GOV/fields_full/global.lexi
			TestIndex.createLexiconIndex(args[2], args[3]);
		}else if (args[1].equals("--closetag")){
			// -i --closetag <filename> <outputFilename> <tag>
			TestIndex.closeTag(args[2], args[3], args[4]);
		}else if (args[1].equals("--printdocument")){
			TestIndex.printDocumentInDirectIndex(Integer.parseInt(args[2]));
		}else if (args[1].equals("--createdocnoidmapping")){
			// -i --createdocnoidmapping <indexpath> <indexprefix>
			TestIndex.createDocnoIdMapping(args[2], args[3]);
		}else if (args[1].equals("--printaveragefrequency")){
			// -i --printaveragefrequency <lexiconFilename> <dictFilename>
			TestIndex.printAverageFrequencyOfDictionary(args[2], args[3]);
		}else if (args[1].equals("--printnumberoftokens")){
			// -i --printnumberoftokens <lexiconFilename>
			TestIndex.printNumberOfTokensLexicon(args[2]);
		}
	}
}

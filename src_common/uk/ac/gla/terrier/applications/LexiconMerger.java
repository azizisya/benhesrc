/*
 * Created on 11-Aug-2004
 */
package uk.ac.gla.terrier.applications;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import uk.ac.gla.terrier.structures.LexiconInputStream;
import uk.ac.gla.terrier.structures.LexiconOutputStream;

/**
 * @@author vassilis
 */
public class LexiconMerger {
	
	/** The filename of the first inverted file */
	protected String lexiconFile1 = null;
	
	/** The filename of the second inverted file */
	protected String lexiconFile2 = null;
	
	/** The filename of the output merged inverted file */
	protected String lexiconFileOutput = null;

	protected long numberOfTerms;
	
	/**
	 * A constructor that sets the filenames of the lexicon
	 * files to merge
	 * @@param _filename1 the first lexicon file to merge
	 * @@param _filename2 the second lexicon file to merge
	 */
	public LexiconMerger(String _filename1, String _filename2) {
		lexiconFile1 = _filename1;
		lexiconFile2 = _filename2;
		numberOfTerms = 0;
	}
	
	/**
	 * Sets the output filename of the merged lexicon file
	 * @@param _outputName the filename of the merged lexicon file
	 */
	public void setOutputFilename(String _outputName) {
		lexiconFileOutput = _outputName;
	}	
	
	/**
	 * Merges the two lexicons into one. After this stage, the offsets in the
	 * lexicon are ot correct. They will be updated only after creating the 
	 * inverted file.
	 */
	protected void mergeLexicons() {
		try {
			
			//setting the input streams
			String lexFilename1 = lexiconFile1;
			String lexFilename2 = lexiconFile2;
	
			LexiconInputStream lexInStream1 = new LexiconInputStream(lexFilename1);

			LexiconInputStream lexInStream2 = new LexiconInputStream(lexFilename2);
		
			//setting the output stream
			String lexOutputName = lexiconFileOutput;

			LexiconOutputStream lexOutStream = new LexiconOutputStream(lexOutputName);
			
			int hasMore1 = -1;
			int hasMore2 = -1;
			String term1;
			String term2;
		
			hasMore1 = lexInStream1.readNextEntry();
			hasMore2 = lexInStream2.readNextEntry();
			while (hasMore1 >=0 && hasMore2 >= 0) {
				term1 = lexInStream1.getTerm();
				term2 = lexInStream2.getTerm();
				//System.out.println("term1 : " + term1 + "with id " + lexInStream1.getTermId());
				//System.out.println("term2 : " + term2 + "with id " + lexInStream2.getTermId());
				int lexicographicalCompare = term1.compareTo(term2);
				if (lexicographicalCompare < 0) {
					
					lexOutStream.writeNextEntry(term1,
									   0,
									   lexInStream1.getNt(),
									   lexInStream1.getTF(),
									   0L,
									   (byte)0);
					hasMore1 = lexInStream1.readNextEntry();
				
				} else if (lexicographicalCompare > 0) {
					
					lexOutStream.writeNextEntry(term2,
									   			0,
									   			lexInStream2.getNt(),
									   			lexInStream2.getTF(),
									   			0L,
									   			(byte)0);
					hasMore2 = lexInStream2.readNextEntry();
				} else {
					lexOutStream.writeNextEntry(term1,
												0,
												(lexInStream1.getNt() + lexInStream2.getNt()),
												(lexInStream1.getTF() + lexInStream2.getTF()),
												0L,
												(byte)0);
					hasMore1 = lexInStream1.readNextEntry();
					hasMore2 = lexInStream2.readNextEntry();
				}
			}
			
			if (hasMore1 >= 0) {
				while (hasMore1 >= 0) {
					lexOutStream.writeNextEntry(lexInStream1.getTerm(),
									   			0,
									   			lexInStream1.getNt(),
									   			lexInStream1.getTF(),
									   			0L,
												(byte)0);
					hasMore1 = lexInStream1.readNextEntry();
				}
			} else if (hasMore2 >= 0) {
				while (hasMore2 >= 0) {
					lexOutStream.writeNextEntry(lexInStream2.getTerm(),
												0,
												lexInStream2.getNt(),
												lexInStream2.getTF(),
												0L,
												(byte)0);
					hasMore2 = lexInStream2.readNextEntry();		
				}		
			}
			
			lexInStream1.close();
			lexInStream2.close();
			lexOutStream.close();
		} catch(IOException ioe) {
			System.err.println("IOException while merging lexicons.");
			System.err.println(ioe);
			System.exit(1);
		}
		// create an empty lexid file
		try{
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(
					this.lexiconFileOutput+"id")));
			bw.write(" ");
			bw.close();
		}
		catch(IOException e){
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void main(String[] args) {
		if (args.length!=3) {
			System.out.println("usage: java LexiconMerger [lexicon file 1] [lexicon file 2] [output lexicon file]");
			System.exit(1);
		}
		LexiconMerger lMerger = new LexiconMerger(args[0],args[1]);
		lMerger.setOutputFilename(args[2]);
		long start = System.currentTimeMillis();
		System.out.println("started at " + (new Date()));
		lMerger.mergeLexicons();
		System.out.println("finished at " + (new Date()));
		long end = System.currentTimeMillis();
		System.out.println("time elapsed: " + ((end-start)*1.0d/1000.0d) + " sec.");
	}
}
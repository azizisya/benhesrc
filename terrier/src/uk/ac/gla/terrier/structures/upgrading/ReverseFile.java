/*
 * Terrier - Terabyte Retriever 
 * Webpage: http://ir.dcs.gla.ac.uk/terrier 
 * Contact: terrier{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * http://www.gla.ac.uk/
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is ReverseFile.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *  Roi Blanco
 */

package uk.ac.gla.terrier.structures.upgrading;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import uk.ac.gla.terrier.compression.BitFile;
import uk.ac.gla.terrier.compression.OldBitFile;
import uk.ac.gla.terrier.structures.BlockInvertedIndex;
import uk.ac.gla.terrier.structures.InvertedIndex;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.FieldScore;
import uk.ac.gla.terrier.utility.Files;
/**
 * This class provides some utilities for handling inverted files from the old terrier version.
 * Concretely:<br>
 * - Reverses an Inverted File byte to byte (the byte sequence remains the same)
 * abcdefgh| ... --> hgfedcba | ... <br>
 * - Converts an inverted file from the old terrier version (using OldBitFile) to this one (reading the contents of the 
 * file and writting the postings in the new format). <br>
 * - Checks if two inverted files have the same information (this is, the same number of posting lists, with the same 
 * data in them).
 * 
 * @author Roi Blanco
 *
 */
public class ReverseFile {
	
	
	/**
	 * Traverses an inverted file written with the old terrier version, and writes it in the new compressed format. 
	 * @param output String containing the name of the new inverted file (should replace the old one).
	 */
	// TODO Support for blocks
	public static void reverse(String output){
		BitFile newFile = new BitFile(output, "rw");
		Lexicon lexicon = new Lexicon();
		OldBitFile file = new OldBitFile(ApplicationSetup.INVERTED_FILENAME, "rw");
		try{
			newFile.writeReset();
			for (int currentTerm = 0; currentTerm < lexicon.getNumberOfLexiconEntries(); currentTerm++) {
				lexicon.seekEntry(currentTerm);			
				byte startBitOffset = lexicon.getStartBitOffset();
				long startOffset = lexicon.getStartOffset();
				byte endBitOffset = lexicon.getEndBitOffset();
				long endOffset = lexicon.getEndOffset();					
				final int fieldCount = FieldScore.FIELDS_COUNT;
				final boolean loadTagInformation = FieldScore.USE_FIELD_INFORMATION;
				int df = lexicon.getNt();			
				file.readReset(startOffset, startBitOffset, endOffset, endBitOffset);		
				if (loadTagInformation) { 
					for(int i = 0; i < df ; i++){
						newFile.writeGamma(file.readGamma());				
						newFile.writeUnary(file.readUnary());
						newFile.writeBinary(file.readBinary(fieldCount), fieldCount);
					}
				} else { 		
					for(int i = 0; i < df; i++){							
						newFile.writeGamma(file.readGamma());
						newFile.writeUnary(file.readUnary());													
					}
				}													
			}			
			newFile.close();
			file.close();
			lexicon.close();
		}catch(IOException e){
			System.err.println("Error writting the compressed file ");
			e.printStackTrace();
		}
	}
	
	/**
	 * Checks if two inverted files contain the same information, without considering the termid or the
	 * order of the terms in the files.
	 * @param  invertedFile String file name of the inverted file
	 * @param  lexiconFile String file name of the lexicon file
	 */
	public static void checkIF(String invertedFile, String lexiconFile){
		Lexicon lexicon = new Lexicon(); 
		Lexicon lexicon2 = new Lexicon(lexiconFile);
		InvertedIndex index1;
		InvertedIndex index2;
		if (ApplicationSetup.BLOCK_INDEXING) {
			System.err.println("block html inverted index");
			index1 = new BlockInvertedIndex(lexicon);
			index2 = new BlockInvertedIndex(lexicon2);
		} else {
			System.err.println("html inverted index");
			index1 = new InvertedIndex(lexicon);
			index2 = new InvertedIndex(lexicon2);
		}
		
		if(lexicon.getNumberOfLexiconEntries() != lexicon2.getNumberOfLexiconEntries()){
			System.err.println("The number of entries of the lexicons ("+lexicon.getNumberOfLexiconEntries()+","+lexicon2.getNumberOfLexiconEntries()+") does not match ");
			System.exit(0);
		}
		String term;
		long entries = lexicon.getNumberOfLexiconEntries();
		long check = entries / 10;
		
		for(int i = 0; i < entries; i++){
			if(i%check == 0){
				System.out.print((i/check)*10+"% ...");
			}
			lexicon.seekEntry(i);
			term = lexicon.getTerm();
			if(!lexicon2.findTerm(term)){
				System.err.println("\nTerm "+term+" not found!");
				System.exit(0);
			}
			//get the data from the two inverted files and check it
			if(lexicon.getNt() != lexicon2.getNt()){
				System.err.println("\nDifferent df for term "+term+"("+lexicon.getNt()+","+lexicon2.getNt()+")");
				System.exit(0);
			}
			if(lexicon.getTF() != lexicon2.getTF()){
				System.err.println("\nDifferent df for term "+term+"("+lexicon.getTF()+","+lexicon2.getTF()+")");
				System.exit(0);
			}
		
			if(!checkArrays(index1.getDocuments(lexicon.getTermId()),index2.getDocuments(lexicon2.getTermId()))){
				System.err.println("\nPostings for term "+term+" do not match:\n"+index1.getInfo(lexicon.getTermId())+"\n"+index2.getInfo(lexicon2.getTermId()));
				System.exit(0);
			}
		}
		
		System.out.println("\nThe inverted files match");				
	}
	
	/**
	 * Checks whether two int[][] arrays have the same elements 
	 * @param one the first int[][] array
	 * @param two the second int[][] array
	 * @return true if the arrays match
	 */
	public static boolean checkArrays(int[][] one, int[][] two){
		for(int i = 0; i < one.length; i++)
			for(int j = 0; j < one[i].length; j++){
				if(one[i][j] != two[i][j])
					return false;
			}
		return true;
	}
	
	/** 
	 * Reverses a file reading byte to byte
	 * @param input String with the file to read 
	 * @param output String output filenae
	 */
	public static void reverseByteByByte(String input, String output){
		// Open the input file for reading
		DataInputStream dis = null;
		DataOutputStream dos = null; 
		try {
			dis = new DataInputStream(Files.openFileStream(input));
		} catch (IOException e) {
			System.err.println("Error while opening "+input+" stack trace follows");
			e.printStackTrace();
		}
		try {
			dos = new DataOutputStream(Files.writeFileStream(output));
		} catch (IOException e) {			
			System.err.println("Error while opening "+output+" stack trace follows");
			e.printStackTrace();
		}
		int byteRead;
		int i = 0;
		try{
			while((byteRead = dis.read())!=-1){
				i++;
				dos.writeByte(reverseByte((byteRead)));								
			}
		}catch(Exception e){
			System.err.println("An error ocurred while writing, stack trace follows :");
			e.printStackTrace();
		}
		try{
			System.out.println("Bytes written "+i);
			dis.close();
			dos.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Reverses one byte.
	 * @param byteIn byte to reverse.
	 * @return byte reversed.
	 */
	public static int reverseByte(int byteIn){
		int byteOut = 0;
		for(int i = 0; i < 8; i++ ){
			// shift right with 0s and mask
			byteOut = byteOut >> 1;
			byteOut |= byteIn & 128;			
			byteIn = byteIn << 1;
		}		
		return ~ byteOut;
	}
	
	/**
	 * Main class
	 * @param args (see help)
	 */
	public static void main(String args[]){
		if(args.length < 1){
			printHelp();
		}
		if(args[0].equals("-c")){
			if(args.length < 2){
				System.out.println("Path for the second IF needed");
				System.exit(0);
			}
			String filenameTemplate = args[1]+ ApplicationSetup.FILE_SEPARATOR + ApplicationSetup.TERRIER_INDEX_PREFIX;
			System.out.println("Checking "+ApplicationSetup.INVERTED_FILENAME+" with "+filenameTemplate+ApplicationSetup.IFSUFFIX);			
			checkIF(filenameTemplate+ApplicationSetup.IFSUFFIX, filenameTemplate+ApplicationSetup.LEXICONSUFFIX);	
		}
		else if(args[0].equals("-r")){
			if(args.length < 2){
				System.out.println("Output file name needed");
				System.exit(0);
			}
			System.out.println("Reversing to "+args[1]);		
			reverse(args[0]);			
		}
		else if(args[0].equals("-h")){
			printHelp();
		}
		else{
			System.out.println("Option "+args[0]+" not recognised");
			printHelp();
		}			
		System.exit(0);
	}
	
	/**
	 * Prints the help.<br>
	 * Use: java ReverseFile  -c <newInvertedFilePath>	(checks if the content of two inverted files match) <br>
	 * Use: java ReverseFile  -r <outputFile>		(reverses the byte encoding of the inverted file).
	 */
	private static void printHelp(){
		System.out.println("Use: java ReverseFile  -c <newInvertedFilePath>	(checks if the content of two inverted files match)");
		System.out.println("Use: java ReverseFile  -r <outputFile>		(reverses the byte encoding of the inverted file)");	
		System.exit(-1);
	}	
}

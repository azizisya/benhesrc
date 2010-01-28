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
 * The Original Code is BinarySearchTest.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
 */
package tests;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import junit.framework.TestCase;
import uk.ac.gla.terrier.structures.BlockLexicon;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.DocumentIndexEncoded;
import uk.ac.gla.terrier.structures.DocumentIndexInMemory;
import uk.ac.gla.terrier.structures.FilePosition;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.structures.indexing.DocumentIndexBuilder;

/**
 * Tests the binary search of the document index class
 * 
 * TODO add tests for other binary search methods
 * 
 * @author Vassilis Plachouras
 * @version $Revision: 1.1 $
 */
public class BinarySearchTest extends TestCase {
	public void testDocumentIndexBinarySearch() {
		
		String testFileName = "test.docid";
		//building the test document index
		DocumentIndexBuilder dIndexBuilder = new DocumentIndexBuilder(testFileName);
		try {
			dIndexBuilder.addEntryToBuffer("1", 3, new FilePosition(4L, (byte)3)); //docid 0
			dIndexBuilder.addEntryToBuffer("2", 5, new FilePosition(6L, (byte)2)); //docid 1
			dIndexBuilder.addEntryToBuffer("4", 3, new FilePosition(7L, (byte)3)); //docid 2
			dIndexBuilder.addEntryToBuffer("6", 10, new FilePosition(8L, (byte)2)); //docid 3
			dIndexBuilder.addEntryToBuffer("7", 2, new FilePosition(9L, (byte)3)); //docid 4
			dIndexBuilder.addEntryToBuffer("8", 34, new FilePosition(30L, (byte)2)); //docid 5
			dIndexBuilder.addEntryToBuffer("105", 20, new FilePosition(40L, (byte)3)); //docid 6
		} catch(IOException ioe) {
			System.err.println("IOException while creating test file.");
			ioe.printStackTrace();
		}
		dIndexBuilder.close();
		
		//reading the test document index
		DocumentIndex dIndex = new DocumentIndex(testFileName);
		assertEquals(-1, dIndex.getDocumentId("0"));
		assertEquals(0, dIndex.getDocumentId("1"));
		assertEquals(1, dIndex.getDocumentId("2"));
		assertEquals(-1, dIndex.getDocumentId("3"));
		assertEquals(2, dIndex.getDocumentId("4"));
		assertEquals(5, dIndex.getDocumentId("8"));
		assertEquals(-1, dIndex.getDocumentId("9"));
		assertEquals(-1, dIndex.getDocumentId("10"));
		
		dIndex.close();
		
		//reading the test document index
		DocumentIndexEncoded dIndex1 = new DocumentIndexEncoded(testFileName);
		assertEquals(-1, dIndex1.getDocumentId("0"));
		assertEquals(0, dIndex1.getDocumentId("1"));
		assertEquals(1, dIndex1.getDocumentId("2"));
		assertEquals(-1, dIndex1.getDocumentId("3"));
		assertEquals(2, dIndex1.getDocumentId("4"));
		assertEquals(5, dIndex1.getDocumentId("8"));
		assertEquals(-1, dIndex1.getDocumentId("9"));
		assertEquals(-1, dIndex1.getDocumentId("10"));
		
		dIndex1.close();
		
		//reading the test document index
		DocumentIndexInMemory dIndex2 = new DocumentIndexInMemory(testFileName);
		assertEquals(-1, dIndex2.getDocumentId("0"));
		assertEquals(0, dIndex2.getDocumentId("1"));
		assertEquals(1, dIndex2.getDocumentId("2"));
		assertEquals(-3, dIndex2.getDocumentId("3"));
		assertEquals(2, dIndex2.getDocumentId("4"));
		assertEquals(5, dIndex2.getDocumentId("8"));
		assertEquals(-7, dIndex2.getDocumentId("9"));
		assertEquals(-7, dIndex2.getDocumentId("10"));
		
		dIndex2.close();
		
		assertTrue((new File(testFileName)).delete());
	}
	
	public void testLexiconBinarySearch() {
		
		String lexFilename= "test.lex";
		String lexidFilename = "test.lexid";
		//building the test lexicon
		
		String term;
		int length;
		long offset = 0;
		
		try {
			DataOutputStream lexFile = new DataOutputStream(new FileOutputStream(lexFilename));
			DataOutputStream lexidFile = new DataOutputStream(new FileOutputStream(lexidFilename));
			term ="1";
			lexidFile.writeLong(offset); offset+=Lexicon.lexiconEntryLength;
			lexFile.write(term.getBytes());
			length = 20 - term.length();
			for (int i=0; i<length; i++) lexFile.writeByte((byte)0);
			lexFile.writeInt(0);
			lexFile.writeInt(10);
			lexFile.writeInt(20);
			lexFile.writeLong(30);
			lexFile.writeByte((byte)1);
			

			term ="105";
			lexidFile.writeLong(offset); offset+=Lexicon.lexiconEntryLength;
			lexFile.write(term.getBytes());
			length = 20 - term.length();
			for (int i=0; i<length; i++) lexFile.writeByte((byte)0);
			lexFile.writeInt(6);
			lexFile.writeInt(3);
			lexFile.writeInt(3);
			lexFile.writeLong(59);
			lexFile.writeByte((byte)6);
			
			term ="2";
			lexidFile.writeLong(offset); offset+=Lexicon.lexiconEntryLength;
			lexFile.write(term.getBytes());
			length = 20 - term.length();
			for (int i=0; i<length; i++) lexFile.writeByte((byte)0);
			lexFile.writeInt(1);
			lexFile.writeInt(5);
			lexFile.writeInt(10);
			lexFile.writeLong(40);
			lexFile.writeByte((byte)3);
			
			term ="4";
			lexidFile.writeLong(offset); offset+=Lexicon.lexiconEntryLength;
			lexFile.write(term.getBytes());
			length = 20 - term.length();
			for (int i=0; i<length; i++) lexFile.writeByte((byte)0);
			lexFile.writeInt(2);
			lexFile.writeInt(3);
			lexFile.writeInt(4);
			lexFile.writeLong(35);
			lexFile.writeByte((byte)4);
			
			term ="6";
			lexidFile.writeLong(offset); offset+=Lexicon.lexiconEntryLength;
			lexFile.write(term.getBytes());
			length = 20 - term.length();
			for (int i=0; i<length; i++) lexFile.writeByte((byte)0);
			lexFile.writeInt(3);
			lexFile.writeInt(4);
			lexFile.writeInt(4);
			lexFile.writeLong(41);
			lexFile.writeByte((byte)3);
			
			term ="7";
			lexidFile.writeLong(offset); offset+=Lexicon.lexiconEntryLength;
			lexFile.write(term.getBytes());
			length = 20 - term.length();
			for (int i=0; i<length; i++) lexFile.writeByte((byte)0);
			lexFile.writeInt(4);
			lexFile.writeInt(2);
			lexFile.writeInt(2);
			lexFile.writeLong(50);
			lexFile.writeByte((byte)6);
			
			term ="8";
			lexidFile.writeLong(offset); offset+=Lexicon.lexiconEntryLength;
			lexFile.write(term.getBytes());
			length = 20 - term.length();
			for (int i=0; i<length; i++) lexFile.writeByte((byte)0);
			lexFile.writeInt(5);
			lexFile.writeInt(1);
			lexFile.writeInt(1);
			lexFile.writeLong(55);
			lexFile.writeByte((byte)1);
			

			
			lexFile.close();	
			lexidFile.close();
		} catch(IOException ioe) {
			System.out.println("IOException while creating the test lexicon");
		}

		
		//reading the test document index
		Lexicon lexicon= new Lexicon(lexFilename);
		assertFalse(lexicon.findTerm("0"));
		assertTrue(lexicon.findTerm("1"));
		assertTrue(lexicon.getNt()== 10);
		assertTrue(lexicon.getTF()== 20);
		assertTrue(lexicon.findTerm("2"));
		assertFalse(lexicon.findTerm("3"));
		assertTrue(lexicon.findTerm("4"));
		assertTrue(lexicon.getNt()== 3);
		assertTrue(lexicon.getTF()== 4);
		
		assertTrue(lexicon.findTerm("8"));
		assertFalse(lexicon.findTerm("9"));
		assertFalse(lexicon.findTerm("10"));
		assertTrue(lexicon.findTerm("105"));
		lexicon.close();
		
		assertTrue((new File(lexFilename)).delete());
		assertTrue((new File(lexidFilename)).delete());
	}
	
	public void testBlockLexiconBinarySearch() {
		
		String lexFilename= "test.lex";
		String lexidFilename = "test.lexid";
		//building the test lexicon
		
		String term;
		int length;
		long offset = 0;
		
		try {
			DataOutputStream lexFile = new DataOutputStream(new FileOutputStream(lexFilename));
			DataOutputStream lexidFile = new DataOutputStream(new FileOutputStream(lexidFilename));
			term ="1";
			lexidFile.writeLong(offset); offset+=BlockLexicon.lexiconEntryLength;
			lexFile.write(term.getBytes());
			length = 20 - term.length();
			for (int i=0; i<length; i++) lexFile.writeByte((byte)0);
			lexFile.writeInt(0);
			lexFile.writeInt(10);
			lexFile.writeInt(20);
			lexFile.writeInt(25);
			lexFile.writeLong(30);
			lexFile.writeByte((byte)1);
			
			
			term ="2";
			lexidFile.writeLong(offset); offset+=BlockLexicon.lexiconEntryLength;
			lexFile.write(term.getBytes());
			length = 20 - term.length();
			for (int i=0; i<length; i++) lexFile.writeByte((byte)0);
			lexFile.writeInt(1);
			lexFile.writeInt(5);
			lexFile.writeInt(10);
			lexFile.writeInt(25);
			lexFile.writeLong(40);
			lexFile.writeByte((byte)3);
			
			term ="4";
			lexidFile.writeLong(offset); offset+=BlockLexicon.lexiconEntryLength;
			lexFile.write(term.getBytes());
			length = 20 - term.length();
			for (int i=0; i<length; i++) lexFile.writeByte((byte)0);
			lexFile.writeInt(2);
			lexFile.writeInt(3);
			lexFile.writeInt(4);
			lexFile.writeInt(25);
			lexFile.writeLong(35);
			lexFile.writeByte((byte)4);
			
			term ="6";
			lexidFile.writeLong(offset); offset+=BlockLexicon.lexiconEntryLength;
			lexFile.write(term.getBytes());
			length = 20 - term.length();
			for (int i=0; i<length; i++) lexFile.writeByte((byte)0);
			lexFile.writeInt(3);
			lexFile.writeInt(4);
			lexFile.writeInt(4);
			lexFile.writeInt(25);
			lexFile.writeLong(41);
			lexFile.writeByte((byte)3);
			
			term ="7";
			lexidFile.writeLong(offset); offset+=BlockLexicon.lexiconEntryLength;
			lexFile.write(term.getBytes());
			length = 20 - term.length();
			for (int i=0; i<length; i++) lexFile.writeByte((byte)0);
			lexFile.writeInt(4);
			lexFile.writeInt(2);
			lexFile.writeInt(2);
			lexFile.writeInt(25);
			lexFile.writeLong(50);
			lexFile.writeByte((byte)6);
			
			term ="8";
			lexidFile.writeLong(offset); offset+=BlockLexicon.lexiconEntryLength;
			lexFile.write(term.getBytes());
			length = 20 - term.length();
			for (int i=0; i<length; i++) lexFile.writeByte((byte)0);
			lexFile.writeInt(5);
			lexFile.writeInt(1);
			lexFile.writeInt(1);
			lexFile.writeInt(25);
			lexFile.writeLong(55);
			lexFile.writeByte((byte)1);
			
			term ="105";
			lexidFile.writeLong(offset); offset+=BlockLexicon.lexiconEntryLength;
			lexFile.write(term.getBytes());
			length = 20 - term.length();
			for (int i=0; i<length; i++) lexFile.writeByte((byte)0);
			lexFile.writeInt(6);
			lexFile.writeInt(3);
			lexFile.writeInt(3);
			lexFile.writeInt(25);
			lexFile.writeLong(59);
			lexFile.writeByte((byte)6);
			
			lexFile.close();	
			lexidFile.close();
		} catch(IOException ioe) {
			System.out.println("IOException while creating the test lexicon");
		}

		
		//reading the test document index
		BlockLexicon lexicon= new BlockLexicon(lexFilename);
		assertFalse(lexicon.findTerm("0"));
		assertTrue(lexicon.findTerm("1"));
		assertTrue(lexicon.findTerm("2"));
		assertFalse(lexicon.findTerm("3"));
		assertTrue(lexicon.findTerm("4"));
		assertTrue(lexicon.findTerm("8"));
		assertFalse(lexicon.findTerm("9"));
		assertFalse(lexicon.findTerm("10"));
		
		lexicon.close();
		
		assertTrue((new File(lexFilename)).delete());
		assertTrue((new File(lexidFilename)).delete());
	}
}

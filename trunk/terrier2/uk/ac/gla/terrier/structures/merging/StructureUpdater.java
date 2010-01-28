package uk.ac.gla.terrier.structures.merging;

import gnu.trove.THashSet;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.StringTokenizer;

import uk.ac.gla.terrier.compression.BitInputStream;
import uk.ac.gla.terrier.compression.BitOutputStream;
import uk.ac.gla.terrier.sorting.SortAscendingPairedVectors;
import uk.ac.gla.terrier.sorting.SortAscendingTripleVectors;
import uk.ac.gla.terrier.structures.DirectIndex;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.DocumentIndexEncoded;
import uk.ac.gla.terrier.structures.DocumentIndexInputStream;
import uk.ac.gla.terrier.structures.FilePosition;
import uk.ac.gla.terrier.structures.InvertedIndex;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.structures.LexiconInputStream;
import uk.ac.gla.terrier.structures.LexiconOutputStream;
import uk.ac.gla.terrier.structures.UTFLexicon;
import uk.ac.gla.terrier.structures.UTFLexiconInputStream;
import uk.ac.gla.terrier.structures.UTFLexiconOutputStream;
import uk.ac.gla.terrier.structures.indexing.DocumentIndexBuilder;
import uk.ac.gla.terrier.utility.ApplicationSetup;

public class StructureUpdater extends IncrementalStructureMerger {
	public StructureUpdater(String _filename1, String _filename2) {
		super(_filename1, _filename2);
	}
	
	/**
	 * Merges the two lexicons into one. After this stage, the offsets in the
	 * lexicon are ot correct. They will be updated only after creating the 
	 * inverted file.
	 */
	protected void mergeInvertedFiles() {
		reducedDocs = 0;
		reducedPointers = 0;
		reducedTokens = 0;
		
		updatedDocnos = this.findUpdatedDocuments(oldIndexPath, oldIndexPrefix, newIndexPath, newIndexPrefix);
		TIntIntHashMap termidNtMap = new TIntIntHashMap();
		TIntIntHashMap termidTFMap = new TIntIntHashMap();
		TIntHashSet negativeTermidSet = new TIntHashSet(); 
		DocumentIndex docIndex = new DocumentIndex(oldIndexPath, oldIndexPrefix);
		int numberOfUpdatedDocs = updatedDocnos.length;
		reducedDocs = numberOfUpdatedDocs;
		int[] updatedDocids = new int[numberOfUpdatedDocs];
		TIntHashSet updatedDocidSet = new TIntHashSet();
		for (int i=0; i<numberOfUpdatedDocs; i++){
			updatedDocids[i] = docIndex.getDocumentId(updatedDocnos[i]);
			updatedDocidSet.add(updatedDocids[i]);
		}
		docIndex.close();
		TIntObjectHashMap<int[][]> docidTermsMap = new TIntObjectHashMap<int[][]>();
		try{
			// scan the direct file
			// store entries of the removed documents in memory
			DirectIndex directIndex = new DirectIndex(new DocumentIndex(oldIndexPath, oldIndexPrefix),
					oldIndexPath, oldIndexPrefix);
			DocumentIndexInputStream docIndexInputStream = new DocumentIndexInputStream(oldIndexPath, oldIndexPrefix);  
			int[][] terms = null;
			while (docIndexInputStream.readNextEntry()!=-1){
				int docid = docIndexInputStream.getDocumentId();
				terms = directIndex.getTerms(docid);
				if (updatedDocidSet.contains(docid)){
					if (terms!=null)
						docidTermsMap.put(docid, terms);
				}
			}
			directIndex.close();
			docIndexInputStream.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		
		// compute df and TF of terms in the removed documents
		for (int i=0; i<numberOfUpdatedDocs; i++){
			int[][] terms = docidTermsMap.get(updatedDocids[i]);
			if (terms==null) continue;
			for (int j=0; j<terms[0].length; j++){
				termidNtMap.adjustOrPutValue(terms[0][j], 1, 1);
				reducedPointers++;
				termidTFMap.adjustOrPutValue(terms[0][j], terms[1][j], terms[1][j]);
				reducedTokens+=terms[1][j];
				negativeTermidSet.add(terms[0][j]);
			}
		}
		//getting the number of entries in the first document index, 
		//in order to assign the correct docids to the documents 
		//of the second inverted file.
		String docidFilename1 = invertedFile1.substring(0,invertedFile1.lastIndexOf(".")) + 
							  ApplicationSetup.DOC_INDEX_SUFFIX;
		DocumentIndex docidInput1 = new DocumentIndex(docidFilename1);
		int numberOfDocs1 = docidInput1.getNumberOfDocuments();
		docidInput1.close();
		
		String docidFilename2 = invertedFile2.substring(0,invertedFile2.lastIndexOf(".")) + 
							  ApplicationSetup.DOC_INDEX_SUFFIX;
		DocumentIndex docidInput2 = new DocumentIndex(docidFilename2);
		int numberOfDocs2 = docidInput2.getNumberOfDocuments();
		docidInput2.close();			
		
		numberOfDocuments = numberOfDocs1 + numberOfDocs2;
		
		
		//creating a new map between new and old term codes
		termcodeHashmap = new TIntIntHashMap();

		//setting the input streams
		String lexFilename1 = invertedFile1.substring(0,invertedFile1.lastIndexOf(".")) + 
							  ApplicationSetup.LEXICONSUFFIX;
		String lexFilename2 = invertedFile2.substring(0,invertedFile2.lastIndexOf(".")) + 
									  ApplicationSetup.LEXICONSUFFIX;
		
		String lexOutputName = invertedFileOutput.substring(0,invertedFileOutput.lastIndexOf(".")) + 
							   ApplicationSetup.LEXICONSUFFIX;

		LexiconInputStream lexInStream1 = null;
		LexiconInputStream lexInStream2 = null;
		Lexicon lexicon1 = null;
		Lexicon lexicon2 = null;
		LexiconOutputStream lexOutStream = null;

		Lexicon tmpLex = null; int newCodes;
		if (ApplicationSetup.getProperty("string.use_utf","false").equals("true")) {
			//obtaininign the highest term code assigned to terms in the first index
			tmpLex = new UTFLexicon(lexFilename1);
			newCodes = (int) tmpLex.getNumberOfLexiconEntries();
			tmpLex.close();

			lexInStream1 = new UTFLexiconInputStream(lexFilename1);
			lexicon1 = new UTFLexicon(lexFilename1);

			lexInStream2 = new UTFLexiconInputStream(lexFilename2);
			lexicon2 = new UTFLexicon(lexFilename2);

			lexOutStream = new UTFLexiconOutputStream(lexOutputName);


		} else {
			//obtaininign the highest term code assigned to terms in the first index
			tmpLex = new Lexicon(lexFilename1);
			newCodes = (int) tmpLex.getNumberOfLexiconEntries();
			tmpLex.close();	
			
			lexInStream1 = new LexiconInputStream(lexFilename1);
			lexicon1 = new Lexicon(lexFilename1);
			lexInStream2 = new LexiconInputStream(lexFilename2);
			lexicon2 = new Lexicon(lexFilename2);

			lexOutStream = new LexiconOutputStream(lexOutputName);
		}

		
		InvertedIndex inverted1 = new InvertedIndex(lexicon1, invertedFile1);
		InvertedIndex inverted2 = new InvertedIndex(lexicon2, invertedFile2);

		BitOutputStream invertedOutput = null;

		int hasMore1 = -1;
		int hasMore2 = -1;
		String term1;
		String term2;
		try{
			invertedOutput = new BitOutputStream(invertedFileOutput);
			hasMore1 = lexInStream1.readNextEntry();
			hasMore2 = lexInStream2.readNextEntry();
			while (hasMore1 >=0 && hasMore2 >= 0) {
				term1 = lexInStream1.getTerm();
				term2 = lexInStream2.getTerm();

				int lexicographicalCompare = term1.compareTo(term2);
				if (lexicographicalCompare < 0) {
					
					//write to inverted file as well.
					int[][] docs = inverted1.getDocuments(lexInStream1.getTermId());
					boolean negativeTerm = negativeTermidSet.contains(lexInStream1.getTermId());
					boolean write = true;
					if (negativeTerm){
						if (termidTFMap.get(lexInStream1.getTermId()) == lexInStream1.getTF())
							write = false;
						else{
							// get rid of updated docs from the old inverted index
							TIntHashSet docidSet = new TIntHashSet();
							TIntIntHashMap docidFreqMap = new TIntIntHashMap();
							TIntIntHashMap docidFieldMap = new TIntIntHashMap();
							boolean useField = (docs.length>2);
							for (int i=0; i<docs[0].length; i++){
								if (!updatedDocidSet.contains(docs[0][i])){
									docidSet.add(docs[0][i]);
									docidFreqMap.put(docs[0][i], docs[1][i]);
									if (useField)
										docidFieldMap.put(docs[0][i], docs[2][i]);
								}
							}
							if (docs[0].length!=docidSet.size()){
								int[] docids = docidSet.toArray();
								Arrays.sort(docids);
								if (useField)
									docs = new int[3][docidSet.size()];
								else
									docs = new int[2][docidSet.size()];
								for (int i=0; i<docidSet.size(); i++){
									docs[0][i] = docids[i];
									docs[1][i] = docidFreqMap.get(docids[i]);
									if (useField)
										docs[2][i] = docidFieldMap.get(docids[i]);
								}
							}
						}
					}
					if (write){
						writePostings(docs, docs[0][0]+1, invertedOutput, binaryBits);
						numberOfPointers+=docs[0].length;
						long endOffset = invertedOutput.getByteOffset();
						byte endBitOffset = invertedOutput.getBitOffset();
						endBitOffset--;
						if (endBitOffset < 0 && endOffset > 0) {
							endBitOffset = 7;
							endOffset--;
						}
						if (negativeTermidSet.contains(lexInStream1.getTermId()))
							lexOutStream.writeNextEntry(term1,
									   lexInStream1.getTermId(),
									   lexInStream1.getNt()-termidNtMap.get(lexInStream1.getTermId()),
									   lexInStream1.getTF()-termidTFMap.get(lexInStream1.getTermId()),
									   endOffset,
									   endBitOffset);
						else	
							lexOutStream.writeNextEntry(term1,
									   lexInStream1.getTermId(),
									   lexInStream1.getNt(),
									   lexInStream1.getTF(),
									   endOffset,
									   endBitOffset);
					}
					hasMore1 = lexInStream1.readNextEntry();
				
				} else if (lexicographicalCompare > 0) {
					//write to inverted file as well.
					int[][] docs = inverted2.getDocuments(lexInStream2.getTermId());
					
					writePostings(docs, docs[0][0]+numberOfDocs1+1, invertedOutput, binaryBits);
					numberOfPointers+=docs[0].length;
					long endOffset = invertedOutput.getByteOffset();
					byte endBitOffset = invertedOutput.getBitOffset();
					endBitOffset--;
					if (endBitOffset < 0 && endOffset > 0) {
						endBitOffset = 7;
						endOffset--;
					}
					
					int newCode = newCodes++;
					termcodeHashmap.put(lexInStream2.getTermId(), newCode);
					
					lexOutStream.writeNextEntry(term2,
									   			newCode,
									   			lexInStream2.getNt(),
									   			lexInStream2.getTF(),
									   			endOffset,
									   			endBitOffset);
					hasMore2 = lexInStream2.readNextEntry();
				} else {
					//write to inverted file as well.
					int[][] docs1 = inverted1.getDocuments(lexInStream1.getTermId());
					int[][] docs2 = inverted2.getDocuments(lexInStream2.getTermId());
					boolean negativeTerm = negativeTermidSet.contains(lexInStream1.getTermId());
					boolean write = true;
					if (negativeTerm){
						if (termidTFMap.get(lexInStream1.getTermId()) == lexInStream1.getTF()){
							write = false;
						}else{
							// get rid of updated docs from the old inverted index
							TIntHashSet docidSet = new TIntHashSet();
							TIntIntHashMap docidFreqMap = new TIntIntHashMap();
							TIntIntHashMap docidFieldMap = new TIntIntHashMap();
							boolean useField = (docs1.length>2);
							for (int i=0; i<docs1[0].length; i++){
								if (!updatedDocidSet.contains(docs1[0][i])){
									docidSet.add(docs1[0][i]);
									docidFreqMap.put(docs1[0][i], docs1[1][i]);
									if (useField)
										docidFieldMap.put(docs1[0][i], docs1[2][i]);
								}
							}
							if (docs1[0].length!=docidSet.size()){
								int[] docids = docidSet.toArray();
								Arrays.sort(docids);
								if (useField)
									docs1 = new int[3][docidSet.size()];
								else
									docs1 = new int[2][docidSet.size()];
								for (int i=0; i<docidSet.size(); i++){
									docs1[0][i] = docids[i];
									docs1[1][i] = docidFreqMap.get(docids[i]);
									if (useField)
										docs1[2][i] = docidFieldMap.get(docids[i]);
								}
							}
						}
					}
					if (write)
						writePostings(docs1, docs1[0][0]+1, invertedOutput, binaryBits);
					numberOfPointers+=docs1[0].length;
					writePostings(docs2, 
										docs2[0][0] + numberOfDocs1 - docs1[0][docs1[0].length-1], 
										invertedOutput, binaryBits);
					numberOfPointers+=docs2[0].length;
					long endOffset = invertedOutput.getByteOffset();
					byte endBitOffset = invertedOutput.getBitOffset();
					endBitOffset--;
					if (endBitOffset < 0 && endOffset > 0) {
						endBitOffset = 7;
						endOffset--;
					}
					
					int newCode = lexInStream1.getTermId();
					termcodeHashmap.put(lexInStream2.getTermId(), newCode);
					if (negativeTerm)
						lexOutStream.writeNextEntry(term1,
								newCode,
								(lexInStream1.getNt() + lexInStream2.getNt() - termidNtMap.get(newCode)),
								(lexInStream1.getTF() + lexInStream2.getTF() - termidTFMap.get(newCode)),
								endOffset,
								endBitOffset);
					else
						lexOutStream.writeNextEntry(term1,
								newCode,
								(lexInStream1.getNt() + lexInStream2.getNt()),
								(lexInStream1.getTF() + lexInStream2.getTF()),
								endOffset,
								endBitOffset);
						
					hasMore1 = lexInStream1.readNextEntry();
					hasMore2 = lexInStream2.readNextEntry();
				}
			}
			
			if (hasMore1 >= 0) {
				while (hasMore1 >= 0) {
					//write to inverted file as well.
					int[][] docs = inverted1.getDocuments(lexInStream1.getTermId());
					boolean negativeTerm = negativeTermidSet.contains(lexInStream1.getTermId());
					boolean write = true;
					if (negativeTerm){
						if (termidTFMap.get(lexInStream1.getTermId()) == lexInStream1.getTF()){
							write = false;
						}else{
							// get rid of updated docs from the old inverted index
							TIntHashSet docidSet = new TIntHashSet();
							TIntIntHashMap docidFreqMap = new TIntIntHashMap();
							TIntIntHashMap docidFieldMap = new TIntIntHashMap();
							boolean useField = (docs.length>2);
							for (int i=0; i<docs[0].length; i++){
								if (!updatedDocidSet.contains(docs[0][i])){
									docidSet.add(docs[0][i]);
									docidFreqMap.put(docs[0][i], docs[1][i]);
									if (useField)
										docidFieldMap.put(docs[0][i], docs[2][i]);
								}
							}
							if (docs[0].length!=docidSet.size()){
								int[] docids = docidSet.toArray();
								Arrays.sort(docids);
								if (useField)
									docs = new int[3][docidSet.size()];
								else
									docs = new int[2][docidSet.size()];
								for (int i=0; i<docidSet.size(); i++){
									docs[0][i] = docids[i];
									docs[1][i] = docidFreqMap.get(docids[i]);
									if (useField)
										docs[2][i] = docidFieldMap.get(docids[i]);
								}
							}
						}
					}
					if (write){
						writePostings(docs, docs[0][0]+1, invertedOutput, binaryBits);
						numberOfPointers+=docs[0].length;
						long endOffset = invertedOutput.getByteOffset();
						byte endBitOffset = invertedOutput.getBitOffset();
						endBitOffset--;
						if (endBitOffset < 0 && endOffset > 0) {
							endBitOffset = 7;
							endOffset--;
						}
						if (negativeTerm)
							lexOutStream.writeNextEntry(lexInStream1.getTerm(),
									lexInStream1.getTermId(),
									lexInStream1.getNt()-termidNtMap.get(lexInStream1.getTermId()),
									lexInStream1.getTF()-termidTFMap.get(lexInStream1.getTermId()),
									endOffset,
									endBitOffset);
						else
							lexOutStream.writeNextEntry(lexInStream1.getTerm(),
									lexInStream1.getTermId(),
									lexInStream1.getNt(),
									lexInStream1.getTF(),
									endOffset,
									endBitOffset);
					}
					hasMore1 = lexInStream1.readNextEntry();
				}
			} else if (hasMore2 >= 0) {
				while (hasMore2 >= 0) {
					//write to inverted file as well.
					int[][] docs = inverted2.getDocuments(lexInStream2.getTermId());

					writePostings(docs, docs[0][0]+numberOfDocs1+1, invertedOutput, binaryBits);
					numberOfPointers+=docs[0].length;
					long endOffset = invertedOutput.getByteOffset();
					byte endBitOffset = invertedOutput.getBitOffset();
					endBitOffset--;
					if (endBitOffset < 0 && endOffset > 0) {
						endBitOffset = 7;
						endOffset--;
					}
					
					int newCode = newCodes++;
					termcodeHashmap.put(lexInStream2.getTermId(), newCode);
						
					lexOutStream.writeNextEntry(lexInStream2.getTerm(),
												newCode,
												lexInStream2.getNt(),
												lexInStream2.getTF(),
												endOffset,
												endBitOffset);
					hasMore2 = lexInStream2.readNextEntry();		
				}		
			}
			
			lexInStream1.close();
			lexInStream2.close();
			lexOutStream.close();
			lexicon1.close();
			lexicon2.close();
			inverted1.close();
			inverted2.close();
			invertedOutput.close();					
		} catch(IOException ioe) {
			logger.error("IOException while merging lexicons.", ioe);
		}
	}
	
	/**
	 * Merges the two direct files and the corresponding document id files.
	 */
	protected void mergeDocumentIndexFiles() {
		try {
		
			//the output docid file
			String docidOutputName = invertedFileOutput.substring(0,invertedFileOutput.lastIndexOf(".")) + 
									 ApplicationSetup.DOC_INDEX_SUFFIX;
			//DocumentIndex docidOutput = new DocumentIndex(docidOutputName);
			DocumentIndexBuilder docidOutput = new DocumentIndexBuilder(docidOutputName);
		
			//opening the first set of files.
			String docidFilename1 = invertedFile1.substring(0,invertedFile1.lastIndexOf(".")) + 
								  ApplicationSetup.DOC_INDEX_SUFFIX;
			DocumentIndexInputStream docidInput1 = new DocumentIndexInputStream(docidFilename1);
			THashSet<String> updatedDocnoSet = new THashSet<String>();
			for (int i=0; i<updatedDocnos.length; i++)
				updatedDocnoSet.add(updatedDocnos[i]);
			
			//traversing the first set of files, without any change
			while (docidInput1.readNextEntry() >= 0) {
				if (!updatedDocnoSet.contains(docidInput1.getDocumentNumber()))
					docidOutput.addEntryToBuffer(docidInput1.getDocumentNumber(), 
									 docidInput1.getDocumentLength(),
									 new FilePosition(0L, (byte)0));
			}
			
			//processing the second file
			String docidFilename2 = invertedFile2.substring(0,invertedFile2.lastIndexOf(".")) + 
												  ApplicationSetup.DOC_INDEX_SUFFIX;
			DocumentIndexInputStream docidInput2 = new DocumentIndexInputStream(docidFilename2);
			
			while (docidInput2.readNextEntry() >= 0) {
				
				docidOutput.addEntryToBuffer(docidInput2.getDocumentNumber(), 
									 docidInput2.getDocumentLength(),
									 new FilePosition(0L, (byte)0));
			}

			docidOutput.close();
			docidInput2.close();
			docidInput1.close();
			
			
		} catch(IOException ioe) {
			logger.error("IOException while merging docid files.", ioe);
		}
	}
	
	/**
	 * Merges the two direct files and the corresponding document id files.
	 */
	protected void mergeDirectFiles() {
		THashSet<String> updatedDocnoSet = new THashSet<String>();
		for (int i=0; i<updatedDocnos.length; i++)
			updatedDocnoSet.add(updatedDocnos[i]);
		try {
		
			//the output docid file
			String docidOutputName = invertedFileOutput.substring(0,invertedFileOutput.lastIndexOf(".")) + 
									 ApplicationSetup.DOC_INDEX_SUFFIX;
			//DocumentIndex docidOutput = new DocumentIndex(docidOutputName);
			DocumentIndexBuilder docidOutput = new DocumentIndexBuilder(docidOutputName);
	
			//the output direct file
			String dfoutputName = invertedFileOutput.substring(0,invertedFileOutput.lastIndexOf(".")) + 
								  ApplicationSetup.DF_SUFFIX;
			BitOutputStream dfOutput = new BitOutputStream(dfoutputName);
	
			//opening the first set of files.
			String docidFilename1 = invertedFile1.substring(0,invertedFile1.lastIndexOf(".")) + 
								  ApplicationSetup.DOC_INDEX_SUFFIX;
			DocumentIndexInputStream docidInput1 = new DocumentIndexInputStream(docidFilename1);
			String dfInputName1 = invertedFile1.substring(0,invertedFile1.lastIndexOf(".")) + 
											  ApplicationSetup.DF_SUFFIX;
			BitInputStream dfInput1 = new BitInputStream(dfInputName1);
			
			//traversing the first set of files, without any change
			long endOffset;
			byte endBitOffset;
			long currentEndOffset;
			byte currentEndBitOffset;
			while (docidInput1.readNextEntry() >= 0) {
				endOffset = docidInput1.getEndOffset();
				endBitOffset = docidInput1.getEndBitOffset();
				if (updatedDocnoSet.contains(docidInput1.getDocumentNumber()))
					continue;
				docidOutput.addEntryToBuffer(docidInput1.getDocumentNumber(), 
									 docidInput1.getDocumentLength(),
									 new FilePosition(docidInput1.getEndOffset(),  docidInput1.getEndBitOffset()));
						
				if (docidInput1.getDocumentLength() > 0) { 
					currentEndOffset = dfInput1.getByteOffset();
					currentEndBitOffset = dfInput1.getBitOffset();
					while((currentEndOffset < endOffset) || ((currentEndOffset == endOffset) && (currentEndBitOffset < endBitOffset))) {
						dfOutput.writeGamma(dfInput1.readGamma());
						dfOutput.writeUnary(dfInput1.readUnary());
						dfOutput.writeBinary(binaryBits, dfInput1.readBinary(binaryBits));
						currentEndOffset = dfInput1.getByteOffset();
						currentEndBitOffset = dfInput1.getBitOffset();
					}
				}
			}
			
			//processing the second file
			String docidFilename2 = invertedFile2.substring(0,invertedFile2.lastIndexOf(".")) + 
												  ApplicationSetup.DOC_INDEX_SUFFIX;
			DocumentIndexEncoded docidInput2 = new DocumentIndexEncoded(docidFilename2);
			String dfInputName2 = invertedFile2.substring(0,invertedFile2.lastIndexOf(".")) + 
											  ApplicationSetup.DF_SUFFIX;
			DirectIndex dfInput2 = new DirectIndex(docidInput2, dfInputName2);
			
			int numOfDocs = docidInput2.getNumberOfDocuments();
			for (int i=0; i<numOfDocs; i++) {
			
			
				if (docidInput2.getDocumentLength(i) > 0) { 
					int[][] terms = dfInput2.getTerms(i);
					
					int length = terms[0].length;
					for (int j=0; j<length; j++) {
						terms[0][j] = termcodeHashmap.get(terms[0][j]);
					}
					if (binaryBits>0) {
						SortAscendingTripleVectors.sort(terms[0], terms[1], terms[2]);
					} else {
						SortAscendingPairedVectors.sort(terms[0], terms[1]);
					}
					
					writePostings(terms, terms[0][0]+1, dfOutput, binaryBits);
				}
				long endByte = dfOutput.getByteOffset();
				byte endBit = dfOutput.getBitOffset();
				endBit--;

				if (endBit < 0 && endByte > 0) {
					endBit = 7;
					endByte--;
				}
				
				docidOutput.addEntryToBuffer(docidInput2.getDocumentNumber(i), 
									 docidInput2.getDocumentLength(i),
									 new FilePosition(endByte, endBit));
			
			}

			dfOutput.close();
			docidOutput.close();
			docidInput2.close();
			docidInput1.close();
			dfInput1.close();
			dfInput2.close();
			
			
		} catch(IOException ioe) {
			logger.error("IOException while merging df and docid files.", ioe);
		}
	}
	
	/**
	 * creates the statistics for the merged files
	 */
	protected void createStatistics() {
		try {
			
			String statsName1 = invertedFile1.substring(0,invertedFile1.lastIndexOf(".")) + 
								ApplicationSetup.LOG_SUFFIX;
			String statsName2 = invertedFile2.substring(0,invertedFile2.lastIndexOf(".")) + 
								ApplicationSetup.LOG_SUFFIX;
			String statsOutput = invertedFileOutput.substring(0,invertedFileOutput.lastIndexOf(".")) + 
								ApplicationSetup.LOG_SUFFIX;
								
			BufferedReader br1 = new BufferedReader(new FileReader(statsName1));
			BufferedReader br2 = new BufferedReader(new FileReader(statsName2));
			BufferedWriter bw  = new BufferedWriter(new FileWriter(statsOutput));
			
			String line1 = br1.readLine(); 
			String line2 = br2.readLine();
			StringTokenizer tokens1 = new StringTokenizer(line1, " ");
			StringTokenizer tokens2 = new StringTokenizer(line2, " ");
			
			long docs1 = Long.parseLong(tokens1.nextToken());
			long docs2 = Long.parseLong(tokens2.nextToken());
			
			long numTokens1 = Long.parseLong(tokens1.nextToken());
			long numTokens2 = Long.parseLong(tokens2.nextToken());
			
			tokens1.nextToken();
			tokens2.nextToken();
			
			long pointers1 = Long.parseLong(tokens1.nextToken());
			long pointers2 = Long.parseLong(tokens2.nextToken());
			
			String classes[] = br1.readLine().split("\\s+");
			final int maxDocsEncodedDocid = Integer.parseInt(ApplicationSetup.getProperty("indexing.max.encoded.documentindex.docs","5000000"));
			if (docs1+docs2 > maxDocsEncodedDocid)
				classes[0] = "uk.ac.gla.terrier.structures.DocumentIndex";
			br1.close();
			br2.close();
			
			bw.write((docs1+docs2-reducedDocs) + " " + (numTokens1+numTokens2-reducedTokens) + " " + (numberOfTerms) + " " + (pointers1+pointers2-reducedPointers) + "\n");
			bw.write(classes[0] + " " + classes[1] + " " + classes[2] + " " + classes[3] + "\n");
			bw.close();
			
		} catch(IOException ioe) {
			logger.error("IOException while creating the statistics.", ioe);
		}						
	}
}

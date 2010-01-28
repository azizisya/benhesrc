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
 * The Original Code is IncremetalStructureMerger.java.
 *
 * The Original Code is Copyright (C) 2004-2007 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk> (original author) 
 */
package uk.ac.gla.terrier.structures.merging;

import gnu.trove.THashSet;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import uk.ac.gla.terrier.compression.BitOutputStream;
import uk.ac.gla.terrier.structures.DirectIndex;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.DocumentIndexInputStream;
import uk.ac.gla.terrier.structures.FilePosition;
import uk.ac.gla.terrier.structures.InvertedIndex;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.structures.LexiconInputStream;
import uk.ac.gla.terrier.structures.LexiconOutputStream;
import uk.ac.gla.terrier.structures.UTFLexicon;
import uk.ac.gla.terrier.structures.UTFLexiconInputStream;
import uk.ac.gla.terrier.structures.UTFLexiconOutputStream;
import uk.ac.gla.terrier.structures.indexing.DirectIndexBuilder;
import uk.ac.gla.terrier.structures.indexing.DocumentIndexBuilder;
import uk.ac.gla.terrier.structures.indexing.LexiconBuilder;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.TerrierTimer;
/**
 * This class implements the functionality of merging a new index into
 * an old one where the new index may contain updates of documents in
 * the old one.
 * @author Ben He
 * @version $Revision: 1.1 $
 */
public class IncrementalStructureMerger extends StructureMerger {
	/** The path to the old index. */
	protected String oldIndexPath;
	/** The prefix of the old index. */
	protected String oldIndexPrefix;
	/** The path to the new index. */
	protected String newIndexPrefix;
	/** The prefix of the new index. */
	protected String newIndexPath;
	/** The document numbers of the updated documents. */
	protected String[] updatedDocnos;
	/** The number of updated documents that are deleted from the old index. */
	protected int reducedDocs;
	/** The number of pointers in the deleted documents from the old index. */
	protected int reducedPointers;
	/** The number of tokens in the deleted documents from the old index. */
	protected long reducedTokens;
	/** Is UTF support enabled? */
	protected boolean USE_UTF = Boolean.parseBoolean(ApplicationSetup.getProperty("string.use_utf", "false"));
	/** The default suffix of temporiary index created during the merging. */
	protected String tmpSuffix = ".tmp";
	/**
	 * The constructor of the class.
	 * @param _filename1 The filename of the old inverted file.
	 * @param _filename2 The filename of the new inverted file.
	 */
	public IncrementalStructureMerger(String _filename1, String _filename2) {
		super(_filename1, _filename2);
		oldIndexPath = invertedFile1.substring(0, invertedFile1.lastIndexOf(ApplicationSetup.FILE_SEPARATOR));
		newIndexPath = invertedFile2.substring(0, invertedFile2.lastIndexOf(ApplicationSetup.FILE_SEPARATOR));
		oldIndexPrefix = invertedFile1.substring(invertedFile1.lastIndexOf(ApplicationSetup.FILE_SEPARATOR)+1, invertedFile1.lastIndexOf("."));
		newIndexPrefix = invertedFile2.substring(invertedFile2.lastIndexOf(ApplicationSetup.FILE_SEPARATOR)+1, invertedFile2.lastIndexOf("."));
	}
	/**
	 * This method checks if there exists updates for the old documents in
	 * the new index. If yes, it deletes the updated docuements from the old
	 * index, and then merge the two indices. Otherwise, it performs a normal
	 * index merging. 
	 * @param oldIndexPath The path to the old index. 
	 * @param oldIndexPrefix The prefix of the old index.
	 * @param newIndexPath The path to the new index.
	 * @param newIndexPrefix The prefix of the new index.
	 */
	public void mergeStructures(String oldIndexPath,
			String oldIndexPrefix,
			String newIndexPath,
			String newIndexPrefix){
		// Look for updated documents.
		String[] updatedDocnos = findUpdatedDocuments(
				oldIndexPath, 
				oldIndexPrefix, 
				newIndexPath,
				newIndexPrefix
				);
		
		if (updatedDocnos.length!=0){
			// if there exists updates, delete the updated documents from the old index
			deleteDocuments(updatedDocnos, oldIndexPath, oldIndexPrefix);
			invertedFile1 = oldIndexPath+ApplicationSetup.FILE_SEPARATOR+oldIndexPrefix+tmpSuffix+
					ApplicationSetup.IFSUFFIX;
		}else{
			System.out.println("Found no updated document. Perform a normal index merging.");
		}
		// merge the two indices
		TerrierTimer timer = new TerrierTimer();
		timer.start();
		this.mergeStructures();
		String[] args = {"--createlexiconhash", newIndexPath, newIndexPrefix};
		LexiconBuilder.main(args);
		timer.setBreakPoint();
		System.out.println("Merging finished in "+timer.toStringMinutesSeconds());
		// remove temporiary data structures
		(new File(oldIndexPath, oldIndexPrefix+tmpSuffix+ApplicationSetup.DF_SUFFIX)).delete();
		(new File(oldIndexPath, oldIndexPrefix+tmpSuffix+ApplicationSetup.DOC_INDEX_SUFFIX)).delete();
		(new File(oldIndexPath, oldIndexPrefix+tmpSuffix+ApplicationSetup.IFSUFFIX)).delete();
		(new File(oldIndexPath, oldIndexPrefix+tmpSuffix+ApplicationSetup.LEXICONSUFFIX)).delete();
		(new File(oldIndexPath, oldIndexPrefix+tmpSuffix+ApplicationSetup.LEXICON_INDEX_SUFFIX)).delete();
		(new File(oldIndexPath, oldIndexPrefix+tmpSuffix+ApplicationSetup.LOG_SUFFIX)).delete();
		(new File(oldIndexPath, oldIndexPrefix+tmpSuffix+".lexhash")).delete();
	}
	/**
	 * Delete the given documents from the specified index.
	 * @param docnos The document numbers of the documents to be deleted.
	 * @param indexPath The path to the given index.
	 * @param indexPrefix The prefix of the given index.
	 */
	protected void deleteDocuments(String[] docnos, String indexPath, String indexPrefix){
		DocumentIndex docIndex = new DocumentIndex(indexPath, indexPrefix);
		int n = docnos.length;
		int[] docids = new int[n];
		for (int i=0; i<n; i++)
			docids[i] = docIndex.getDocumentId(docnos[i]);
		docIndex.close();
		deleteDocuments(docids, indexPath, indexPrefix);
	}
	/**
	 * Search for updated documents.
	 * @param oldIndexPath The path to the old index.
	 * @param oldIndexPrefix The prefix of the old index.
	 * @param newIndexPath The path to the new index.
	 * @param newIndexPrefix The prefix of the new index.
	 * @return The document numbers of the updated documents.
	 */
	protected String[] findUpdatedDocuments(String oldIndexPath, 
			String oldIndexPrefix,
			String newIndexPath,
			String newIndexPrefix
			){
		THashSet<String> docnos = new THashSet<String>();
		DocumentIndexInputStream oldDocIndexInputStream = new DocumentIndexInputStream(oldIndexPath, oldIndexPrefix);
		DocumentIndexInputStream newDocIndexInputStream = new DocumentIndexInputStream(newIndexPath, newIndexPrefix);
		try{
			int olddocid = oldDocIndexInputStream.readNextEntry();
			int newdocid = newDocIndexInputStream.readNextEntry();
			String olddocno = oldDocIndexInputStream.getDocumentNumber();
			String newdocno = newDocIndexInputStream.getDocumentNumber();
			while (newdocid!=-1 || olddocid !=-1){
				int lexicographicalCompare = olddocno.compareTo(newdocno);
				if (lexicographicalCompare < 0){
					if (olddocid!=-1){
						if ((olddocid = oldDocIndexInputStream.readNextEntry())!=-1)
							olddocno = oldDocIndexInputStream.getDocumentNumber();
						continue;
					}else break;
				}else if (lexicographicalCompare > 0){
					if (newdocid!=-1){
						if ((newdocid = newDocIndexInputStream.readNextEntry())!=-1)
							newdocno = newDocIndexInputStream.getDocumentNumber();
						continue;
					}else break;
				}else{
					docnos.add(newdocno);
					System.err.println("Found updated document "+newdocno);
					if ((olddocid = oldDocIndexInputStream.readNextEntry())!=-1)
						olddocno = oldDocIndexInputStream.getDocumentNumber();
					if ((newdocid = newDocIndexInputStream.readNextEntry())!=-1)
						newdocno = newDocIndexInputStream.getDocumentNumber();
					continue;
				}
			}
			oldDocIndexInputStream.close();
			newDocIndexInputStream.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		return (String[])docnos.toArray(new String[docnos.size()]);
	}

	/*public boolean checkInvertedIndexIntegrity(String oldIndexPath, String oldIndexPrefix,
			String newIndexPath, String newIndexPrefix){
		// for each term in lexicon, check if the corresponding entries in inverted index are
		// consistent
		System.err.print("Checking inverted index integrity between "+
				oldIndexPath+", "+oldIndexPrefix+" and "+newIndexPath+", "+newIndexPrefix+"...");
		boolean OK = true;
		try{
			LexiconInputStream oldLexInputStream = new LexiconInputStream(oldIndexPath, oldIndexPrefix);
			LexiconInputStream newLexInputStream = new LexiconInputStream(oldIndexPath, oldIndexPrefix);
			Lexicon oldLex = new Lexicon(oldIndexPath, oldIndexPrefix);
			Lexicon newLex = new Lexicon(newIndexPath, newIndexPrefix);
			DocumentIndex oldDocIndex = new DocumentIndex(oldIndexPath, oldIndexPrefix);
			DocumentIndex newDocIndex = new DocumentIndex(newIndexPath, newIndexPrefix);
			InvertedIndex oldInvIndex = new InvertedIndex(oldLex, oldIndexPath, oldIndexPrefix);
			InvertedIndex newInvIndex = new InvertedIndex(newLex, newIndexPath, newIndexPrefix);
			int oldReadByte = oldLexInputStream.readNextEntry();
			int newReadByte = newLexInputStream.readNextEntry();
			
			//int numOfTerms=0;
			String newLexFilename = newIndexPath+ApplicationSetup.FILE_SEPARATOR+
					newIndexPrefix+ApplicationSetup.LEXICONSUFFIX;
			if (USE_UTF) {
				java.io.File lex = new java.io.File(newLexFilename);
				//numOfTerms = ((int) lex.length()) / UTFLexicon.lexiconEntryLength;

				//UTFLexicon lex = new UTFLexicon(lexOutputName);
				//numOfTerms = (int) lex.getNumberOfLexiconEntries();
				//lex.close();
			} else {

				java.io.File lex = new java.io.File(newLexFilename);
				//numOfTerms = ((int) lex.length()) / Lexicon.lexiconEntryLength;
				//Lexicon lex = new Lexicon(lexOutputName);
				//numOfTerms = (int) lex.getNumberOfLexiconEntries();
				//lex.close();
			}
			
			int checkedCounter = 0;
			while (oldReadByte!=-1||newReadByte!=-1){
				int lexiGraphicalCompare = oldLexInputStream.getTerm().compareTo(
						newLexInputStream.getTerm());
				if (lexiGraphicalCompare>0){
					if (newReadByte==-1) break;
					else{
						newReadByte = newLexInputStream.readNextEntry();
						continue;
					}
				}else if (lexiGraphicalCompare<0){
					if (oldReadByte==-1) break;
					else{
						oldReadByte = oldLexInputStream.readNextEntry();
						continue;
					}
				}else{
					try{
						if (newLexInputStream.getTermId()<numberOfTerms){
							checkedCounter++;
							int[][] oldPointers = oldInvIndex.getDocuments(oldLexInputStream.getTermId());
							int[][] newPointers = newInvIndex.getDocuments(newLexInputStream.getTermId());
							THashSet<String> oldDocnoSet = new THashSet<String>();
							THashSet<String> newDocnoSet = new THashSet<String>();
							TObjectIntHashMap<String> oldDocnoFreqMap = new TObjectIntHashMap<String>();
							TObjectIntHashMap<String> newDocnoFreqMap = new TObjectIntHashMap<String>();
							for (int i=0; i<oldPointers[0].length; i++){
								String oldDocno = oldDocIndex.getDocumentNumber(oldPointers[0][i]);
								oldDocnoSet.add(oldDocno);
								oldDocnoFreqMap.put(oldDocno, oldPointers[1][i]);
							}
							for (int i=0; i<newPointers[0].length; i++){
								String newDocno = newDocIndex.getDocumentNumber(newPointers[0][i]);
								oldDocnoSet.add(newDocno);
								oldDocnoFreqMap.put(newDocno, newPointers[1][i]);
							}
							String[] newDocnos = (String[])newDocnoSet.toArray(new String[newDocnoSet.size()]);
							for (int i=0; i<newDocnos.length; i++){
								if (!oldDocnoSet.contains(newDocnos[i])||
										oldDocnoFreqMap.get(newDocnos[i])!=newDocnoFreqMap.get(newDocnos[i])
										){
									System.err.println(newLexInputStream.getTerm()+", ("+newDocnos[i]+
											", "+newDocnoFreqMap.get(newDocnos[i])+")");
									OK = false;
								}
							}
						}
										
						if (newReadByte!=-1)
							newReadByte = newLexInputStream.readNextEntry();
						if (oldReadByte!=-1)
							oldReadByte = oldLexInputStream.readNextEntry();
						continue;
					}catch(Exception e){
						System.err.println("newLexInputStream.getTermId(): "+newLexInputStream.getTermId());
						System.err.println("newLexInputStream.getTerm(): "+newLexInputStream.getTerm());
						e.printStackTrace();
						System.exit(1);
					}
				}
			}
			System.err.println("Done. Checked "+checkedCounter+" terms.");
			oldLex.close();
			newLex.close();
			oldDocIndex.close();
			newDocIndex.close();
			oldLexInputStream.close();
			newLexInputStream.close();
			oldInvIndex.close();
			newInvIndex.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		return OK;
	}*/
	
	/**
	public boolean checkDirectIndexIntegrity(String oldIndexPath, String oldIndexPrefix,
			String newIndexPath, String newIndexPrefix){
		boolean checkOK = true;
		System.err.print("Checking direct index integrity between "+
				oldIndexPath+", "+oldIndexPrefix+" and "+newIndexPath+", "+newIndexPrefix+"...");
		// for each document in direct index, check if terms match the entries in the old index
		try{
			DocumentIndexInputStream oldDocIndexInputStream = new DocumentIndexInputStream(
					oldIndexPath, oldIndexPrefix);
			DocumentIndexInputStream newDocIndexInputStream = new DocumentIndexInputStream(
					newIndexPath, newIndexPrefix);
			DirectIndex oldDirectIndex = new DirectIndex(
					new DocumentIndex(oldIndexPath, oldIndexPrefix),
					oldIndexPath, oldIndexPrefix
					);
			DirectIndex newDirectIndex = new DirectIndex(
					new DocumentIndex(newIndexPath, newIndexPrefix),
					newIndexPath, newIndexPrefix
					);
			Lexicon oldLex = new Lexicon(oldIndexPath, oldIndexPrefix);
			Lexicon newLex = new Lexicon(newIndexPath, newIndexPrefix);
			int oldRead = oldDocIndexInputStream.readNextEntry();
			int newRead = newDocIndexInputStream.readNextEntry();
			int checkedDocsCounter = 0;
			while(oldRead!=-1||newRead!=-1){
				String oldDocno = oldDocIndexInputStream.getDocumentNumber();
				String newDocno = newDocIndexInputStream.getDocumentNumber();
				int strCompare = oldDocno.compareTo(newDocno);
				if (strCompare>0){
					if (newRead!=-1)
						newRead = newDocIndexInputStream.readNextEntry();
					continue;
				}else if (strCompare<0){
					if (oldRead!=-1)
						oldRead = oldDocIndexInputStream.readNextEntry();
					continue;
				}else{
					// check
					int[][] oldTerms = oldDirectIndex.getTerms(oldDocIndexInputStream.getDocumentId());
					int[][] newTerms = oldDirectIndex.getTerms(newDocIndexInputStream.getDocumentId());
					if (oldTerms!=null && newTerms!=null){
						THashSet<String> oldTermSet = new THashSet<String>();
						THashSet<String> newTermSet = new THashSet<String>();
						TObjectIntHashMap<String> oldTermFreqMap = new TObjectIntHashMap<String>();
						TObjectIntHashMap<String> newTermFreqMap = new TObjectIntHashMap<String>();
						for (int i=0; i<oldTerms[0].length; i++){
							oldLex.findTerm(oldTerms[0][i]);
							oldTermSet.add(oldLex.getTerm());
							oldTermFreqMap.put(oldLex.getTerm(), oldTerms[1][i]);
						}
						for (int i=0; i<newTerms[0].length; i++){
							newLex.findTerm(newTerms[0][i]);
							newTermSet.add(newLex.getTerm());
							newTermFreqMap.put(newLex.getTerm(), newTerms[1][i]);
						}
						String[] terms = (String[])newTermSet.toArray(new String[newTermSet.size()]);
						for (int i=0; i<terms.length; i++){
							if (!oldTermSet.contains(terms[i])||
									oldTermFreqMap.get(terms[i])!=newTermFreqMap.get(terms[i])
									){
								System.err.println(newDocIndexInputStream.getDocumentId()+
										": ("+terms[i]+", "+oldTermFreqMap.get(terms[i])+
										", "+newTermFreqMap.get(terms[i])+")"
										);
								checkOK = false;
							}
						}
					}
					// read and continue;
					if (newRead!=-1)
						newRead = newDocIndexInputStream.readNextEntry();
					if (oldRead!=-1)
						oldRead = oldDocIndexInputStream.readNextEntry();
					checkedDocsCounter++;
					continue;
				}
			}
			oldLex.close();
			newLex.close();
			oldDocIndexInputStream.close();
			newDocIndexInputStream.close();
			oldDirectIndex.close();
			newDirectIndex.close();
			System.err.println("Done. checked "+checkedDocsCounter+" documents.");
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		return checkOK;
	}
	*/
	/**
	 * Delete the given documents from the specified index.
	 * @param docids The document ids of the documents to be deleted.
	 * @param indexPath The path to the given index.
	 * @param indexPrefix The prefix of the given index.
	 */
	protected void deleteDocuments(int[] docidsToRemove, String indexPath, String indexPrefix){
		int removedDocsCounter = 0;
		int removedPointersCounter = 0;
		int removedTermsCounter = 0;
		long removedTokensCounter = 0;
		TerrierTimer timer = new TerrierTimer();
		timer.start();
		
		// maps from ids of deleted documents to the entries in the deleted documents.
		TIntObjectHashMap<int[][]> docidTermsMap = new TIntObjectHashMap<int[][]>();
		// maps from the original document ids to the newly assigned document ids during
		// the deletion.
		TIntIntHashMap docidMap = new TIntIntHashMap();
		// a hashset containing the ids of the documents to be deleted. 
		TIntHashSet docidsToRemoveSet = new TIntHashSet();
		int numberOfDocsToRemove = docidsToRemove.length;
		for (int i=0; i<numberOfDocsToRemove; i++){
			docidsToRemoveSet.add(docidsToRemove[i]);
		}
		try{
			// scan the direct file
			// store entries of the removed documents in memory
			DirectIndex directIndex = new DirectIndex(new DocumentIndex(indexPath, indexPrefix), indexPath, indexPrefix);
			DocumentIndexInputStream docIndexInputStream = new DocumentIndexInputStream(indexPath, indexPrefix); 
			
			DirectIndexBuilder directIndexBuilder = new DirectIndexBuilder(indexPath, indexPrefix+tmpSuffix);
			int olddocid = -1;
			int newdocid = -1;
			int[][] terms = null;
			while (docIndexInputStream.readNextEntry()!=-1){
				olddocid = docIndexInputStream.getDocumentId();
				terms = directIndex.getTerms(olddocid);
				if (docidsToRemoveSet.contains(olddocid)){
					// TODO: flush the hashmap to disk to reduce memory usage
					removedDocsCounter++;
					if (terms!=null)
						docidTermsMap.put(olddocid, terms);
				}else{
					if (terms!=null){ 
						newdocid++;
						if (olddocid != newdocid){
							docidMap.put(olddocid, newdocid);
						}
					}
				}	
			}
			directIndexBuilder.close();
			directIndex.close();
			docIndexInputStream.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		// compute df and TF of terms in the removed documents
		TIntIntHashMap termNtMap = new TIntIntHashMap();
		TIntIntHashMap termTFMap = new TIntIntHashMap();
		TIntHashSet termidSet = new TIntHashSet();
		for (int i=0; i<numberOfDocsToRemove; i++){
			int[][] terms = docidTermsMap.get(docidsToRemove[i]);
			if (terms==null) continue;
			for (int j=0; j<terms[0].length; j++){
				termNtMap.adjustOrPutValue(terms[0][j], 1, 1);
				termTFMap.adjustOrPutValue(terms[0][j], terms[1][j], terms[1][j]);
				termidSet.add(terms[0][j]);
			}
		}
		
		// substract df and TF of terms in the updated documents from the lexicon. 
		// rebuild inverted index and lexicon
		System.err.print("Rebuilding inverted index and lexicon...");
		int maxTermid = 0;
		int numberOfLexEntries = 0;
		InvertedIndex invIndex = null;
		try{
			LexiconInputStream lexInputStream = null;
			LexiconOutputStream lexOutputStream = null;
				
			if (USE_UTF){
				lexInputStream = new UTFLexiconInputStream(indexPath, indexPrefix);
				lexOutputStream = new UTFLexiconOutputStream(indexPath, indexPrefix+tmpSuffix);
				invIndex = new InvertedIndex(new UTFLexicon(indexPath, indexPrefix), 
						indexPath, indexPrefix);
			}else{
				lexInputStream = new LexiconInputStream(indexPath, indexPrefix);
				lexOutputStream = new LexiconOutputStream(indexPath, indexPrefix+tmpSuffix);
				invIndex = new InvertedIndex(new Lexicon(indexPath, indexPrefix), 
						indexPath, indexPrefix);
			}
				
			BitOutputStream invertedOutput = new BitOutputStream(indexPath+
					ApplicationSetup.FILE_SEPARATOR+
					indexPrefix+tmpSuffix+ApplicationSetup.IFSUFFIX);
			while (lexInputStream.readNextEntry()!=-1){
				// if the term appears only in deleted documents, continue;
				if (lexInputStream.getTF() == termTFMap.get(lexInputStream.getTermId())){
					removedTermsCounter++;
					removedPointersCounter+=lexInputStream.getNt();
					removedTokensCounter+=lexInputStream.getTF();
					continue;
				}
				
				int[][] docs = invIndex.getDocuments(lexInputStream.getTermId());
				// iterate through postings and remove entries of documents to be removed
				TIntHashSet docidsSet = new TIntHashSet();
				TIntIntHashMap docidtfMap = new TIntIntHashMap();
				boolean USE_FIELDS = (docs.length>2);
				TIntIntHashMap docidFSMap = new TIntIntHashMap();
					
				for (int i=0; i<docs[0].length; i++){
					if (!docidsToRemoveSet.contains(docs[0][i])){
						// convert old docids to new docids
						int newdocid = docidMap.get(docs[0][i]);
						if (newdocid == 0){
							docidsSet.add(docs[0][i]);
							docidtfMap.put(docs[0][i], docs[1][i]);
							if (USE_FIELDS)
								docidFSMap.put(docs[0][i], docs[2][i]);
						}else{
							docidsSet.add(newdocid);
							docidtfMap.put(newdocid, docs[1][i]);
							if (USE_FIELDS)
								docidFSMap.put(newdocid, docs[2][i]);
						}
					}
				} 
				docs = (USE_FIELDS)?(new int[2][docidsSet.size()]):(new int[3][docidsSet.size()]);
				int[] docids = docidsSet.toArray();
				Arrays.sort(docids);
				for (int i=0; i<docidsSet.size(); i++){
					docs[0][i] = docids[i];
					docs[1][i] = docidtfMap.get(docids[i]);
					if (USE_FIELDS)
						docs[2][i] = docidFSMap.get(docids[i]);
				}

				writePostings(docs, docs[0][0]+1, invertedOutput, binaryBits=0);
				
				long endOffset = invertedOutput.getByteOffset();
				byte endBitOffset = invertedOutput.getBitOffset();
				endBitOffset--;
				if (endBitOffset < 0 && endOffset > 0) {
					endBitOffset = 7;
					endOffset--;
				}
				if (termidSet.contains(lexInputStream.getTermId())){
					removedPointersCounter+=termNtMap.get(lexInputStream.getTermId());
					removedTokensCounter+=termTFMap.get(lexInputStream.getTermId());
					lexOutputStream.writeNextEntry(lexInputStream.getTerm(), 
							lexInputStream.getTermId(),
							lexInputStream.getNt()-termNtMap.get(lexInputStream.getTermId()), 
							lexInputStream.getTF()-termTFMap.get(lexInputStream.getTermId()), 
							endOffset, 
							endBitOffset);
				}else{
					lexOutputStream.writeNextEntry(lexInputStream.getTerm(), 
							lexInputStream.getTermId(),
							lexInputStream.getNt(), 
							lexInputStream.getTF(), 
							endOffset, 
							endBitOffset);
				}
				numberOfLexEntries++;
				maxTermid = Math.max(maxTermid, lexInputStream.getTermId());
				docs = null;
				docidsSet.clear(); docidsSet = null;
				docidtfMap.clear(); docidtfMap = null;
				docids = null;
			}
			lexInputStream.close();
			lexOutputStream.close();
			invIndex.close();
			invertedOutput.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		
		// rebuild the lexid file
		createLexidFile(indexPath, indexPrefix+tmpSuffix, maxTermid);
		System.err.println("Done.");
		
		// rebuild direct index
		System.err.print("Rebuilding direct file...");
		try{
			// scan the direct file and write entries, apart from the documents to be
			// deleted, to the temporiary index.
			DirectIndex directIndex = new DirectIndex(new DocumentIndex(indexPath, indexPrefix), indexPath, indexPrefix);
			DocumentIndexInputStream docIndexInputStream = new DocumentIndexInputStream(indexPath, indexPrefix);
			DocumentIndexBuilder docIndexBuilder = new DocumentIndexBuilder(indexPath, indexPrefix+tmpSuffix); 
			
			DirectIndexBuilder directIndexBuilder = new DirectIndexBuilder(indexPath, indexPrefix+tmpSuffix);
			int olddocid = -1;
			int[][] terms = null;
			while (docIndexInputStream.readNextEntry()!=-1){
				olddocid = docIndexInputStream.getDocumentId();
				terms = directIndex.getTerms(olddocid);
				if (docidsToRemoveSet.contains(olddocid)){
					System.err.println("delete document ("+olddocid+") "+
							docIndexInputStream.getDocumentNumber()+" from direct file.");
				}else{
					if (terms!=null){ 
						// there is no need to allign term ids
						/**
						int[] termids = new int[terms[0].length];
						TIntIntHashMap termidtfMap = new TIntIntHashMap();
						TIntIntHashMap termidfieldMap = new TIntIntHashMap();
						for (int i=0; i<terms[0].length; i++){
							int newtermid = termidMap.get(terms[0][i]);
							if (newtermid!=0)
								termids[i] = newtermid;
							else
								termids[i] = terms[0][i];
							termidtfMap.put(termids[i], terms[1][i]);
							if (terms.length>2)
								termidfieldMap.put(termids[i], terms[2][i]);
						}
						Arrays.sort(termids);
						for (int i=0; i<terms[0].length; i++){
							terms[0][i] = termids[i];
							terms[1][i] = termidtfMap.get(termids[i]);
							if (terms.length>2)
								terms[2][i] = termidfieldMap.get(termids[i]);
						}*/
						// write to the temp direct index
						FilePosition directIndexPost = directIndexBuilder.addDocument(terms);
						docIndexBuilder.addEntryToBuffer(docIndexInputStream.getDocumentNumber(), 
								docIndexInputStream.getDocumentLength(), 
								directIndexPost);
					}
				}
			}
			docIndexBuilder.close();
			directIndexBuilder.close();
			directIndex.close();
			docIndexInputStream.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		System.err.println("Done.");
		
		// create statistics
		try{
			BufferedReader br = Files.openFileReader(new File(indexPath, indexPrefix+ApplicationSetup.LOG_SUFFIX));
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(new File(indexPath, indexPrefix+tmpSuffix+ApplicationSetup.LOG_SUFFIX));
			String[] stats = br.readLine().trim().split(" ");
			int numberOfDocuments = Integer.parseInt(stats[0])-removedDocsCounter;
			long numberOfTokens = Long.parseLong(stats[1])-removedTokensCounter;
			int numberOfUniqueTerms = Integer.parseInt(stats[2])-removedTermsCounter;
			int numberOfPointers = Integer.parseInt(stats[3])-removedPointersCounter;
			String classes = br.readLine();
			bw.write(numberOfDocuments+" "+numberOfTokens+" "+
					numberOfUniqueTerms+" "+numberOfPointers+ApplicationSetup.EOL);
			bw.write(classes);
			br.close();
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		// create lexicon hash
		String[] args = {"--createlexiconhash", indexPath, indexPrefix+tmpSuffix};
		LexiconBuilder.main(args);
		timer.setBreakPoint();
		System.err.println("Document removal finished in "+timer.toStringMinutesSeconds());
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// binaryBits invfile1 invfile2 outputinvfile
		IncrementalStructureMerger merger = new IncrementalStructureMerger(args[1], args[2]);
		int bits = Integer.parseInt(args[0]);
		merger.setNumberOfBits(bits);
		merger.setOutputFilename(args[3]);
		merger.mergeStructures(merger.oldIndexPath, merger.oldIndexPrefix,
				merger.newIndexPath, merger.newIndexPrefix);
	}

}

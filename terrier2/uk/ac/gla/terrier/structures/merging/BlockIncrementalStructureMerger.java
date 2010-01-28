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
 * The Original Code is BlockIncremetalStructureMerger.java.
 *
 * The Original Code is Copyright (C) 2004-2007 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk> (original author) 
 */
package uk.ac.gla.terrier.structures.merging;

import gnu.trove.THashSet;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import uk.ac.gla.terrier.compression.BitOutputStream;
import uk.ac.gla.terrier.structures.BlockDirectIndex;
import uk.ac.gla.terrier.structures.BlockInvertedIndex;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.DocumentIndexInputStream;
import uk.ac.gla.terrier.structures.FilePosition;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.structures.LexiconInputStream;
import uk.ac.gla.terrier.structures.LexiconOutputStream;
import uk.ac.gla.terrier.structures.UTFLexicon;
import uk.ac.gla.terrier.structures.UTFLexiconInputStream;
import uk.ac.gla.terrier.structures.UTFLexiconOutputStream;
import uk.ac.gla.terrier.structures.indexing.BlockDirectIndexBuilder;
import uk.ac.gla.terrier.structures.indexing.DocumentIndexBuilder;
import uk.ac.gla.terrier.structures.indexing.LexiconBuilder;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.TerrierTimer;
/**
 * This class implements the functionality of merging a new block index into
 * an old block index where the new index may contain updates of documents in
 * the old one.
 * @author Ben He
 * @version $Revision: 1.1 $
 */
public class BlockIncrementalStructureMerger extends BlockStructureMerger {
	
	final protected boolean USE_UTF = Boolean.parseBoolean(
			ApplicationSetup.getProperty("string.use_utf", "false"));
	
	final protected String tmpSuffix = ".tmp";
	
	protected String oldIndexPath;
	
	protected String oldIndexPrefix;
	
	protected String newIndexPrefix;
	
	protected String newIndexPath;
	
	protected String[] updatedDocnos;
	
	protected int reducedDocs;
	
	protected int reducedPointers;
	
	protected long reducedTokens;

	public BlockIncrementalStructureMerger(String _filename1, String _filename2) {
		super(_filename1, _filename2);
		String separator = ApplicationSetup.FILE_SEPARATOR;
		oldIndexPath = invertedFile1.substring(0, invertedFile1.lastIndexOf(separator));
		newIndexPath = invertedFile2.substring(0, invertedFile2.lastIndexOf(separator));
		oldIndexPrefix = invertedFile1.substring(invertedFile1.lastIndexOf(separator)+1, invertedFile1.lastIndexOf("."));
		newIndexPrefix = invertedFile2.substring(invertedFile2.lastIndexOf(separator)+1, invertedFile2.lastIndexOf("."));
	}
	
	public void mergeStructures(String oldIndexPath,
			String oldIndexPrefix,
			String newIndexPath,
			String newIndexPrefix){
		String[] updatedDocnos = findUpdatedDocuments(
				oldIndexPath, 
				oldIndexPrefix, 
				newIndexPath,
				newIndexPrefix
				);
		if (updatedDocnos.length!=0){
			deleteDocuments(updatedDocnos, oldIndexPath, oldIndexPrefix);
			//if (checkInvertedIndexIntegrity(oldIndexPath, oldIndexPrefix, oldIndexPath, oldIndexPrefix+".tmp")&&
					//checkDirectIndexIntegrity(oldIndexPath, oldIndexPrefix, oldIndexPath, oldIndexPrefix+".tmp")){
			//}
			invertedFile1 = oldIndexPath+ApplicationSetup.FILE_SEPARATOR+oldIndexPrefix+tmpSuffix+
					ApplicationSetup.IFSUFFIX;
		}else{
			System.out.println("Found no updated document. Perform a normal index merging.");
		}
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
	
	protected void deleteDocuments(String[] docnos, String indexPath, String indexPrefix){
		DocumentIndex docIndex = new DocumentIndex(indexPath, indexPrefix);
		int n = docnos.length;
		int[] docids = new int[n];
		for (int i=0; i<n; i++)
			docids[i] = docIndex.getDocumentId(docnos[i]);
		docIndex.close();
		deleteDocuments(docids, indexPath, indexPrefix);
	}
	
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
	
	protected void deleteDocuments(int[] docidsToRemove, String indexPath, String indexPrefix){
		int removedDocsCounter = 0;
		int removedPointersCounter = 0;
		int removedTermsCounter = 0;
		long removedTokensCounter = 0;
		
		
		TIntObjectHashMap<int[][]> docidTermsMap = new TIntObjectHashMap<int[][]>();
		TIntIntHashMap docidMap = new TIntIntHashMap();
		TIntHashSet docidsToRemoveSet = new TIntHashSet();
		int numberOfDocsToRemove = docidsToRemove.length;
		for (int i=0; i<numberOfDocsToRemove; i++){
			docidsToRemoveSet.add(docidsToRemove[i]);
			//System.err.println(i+": "+docidsToRemove[i]);
		}
		try{
			// scan the direct file
			// store entries of the documents to be deleted in memory
			BlockDirectIndex directIndex = new BlockDirectIndex(new DocumentIndex(indexPath, indexPrefix), indexPath, indexPrefix);
			DocumentIndexInputStream docIndexInputStream = new DocumentIndexInputStream(indexPath, indexPrefix);
			int olddocid = -1;
			int newdocid = -1;
			int[][] terms = null;
			while (docIndexInputStream.readNextEntry()!=-1){
				olddocid = docIndexInputStream.getDocumentId();
				terms = directIndex.getTerms(olddocid);
				if (docidsToRemoveSet.contains(olddocid)){
					// we may need to flush the hashmap to disk to reduce memory usage
					removedDocsCounter++;
					if (terms!=null){
						int[][] terms_buf = new int[2][];
						terms_buf[0] = terms[0];
						terms_buf[1] = terms[1];
						docidTermsMap.put(olddocid, terms_buf);
					}
				}else{
					if (terms!=null){ 
						// write to the temp direct index
						/*FilePosition directIndexPost = directIndexBuilder.addDocument(terms);
						docIndexBuilder.addEntryToBuffer(docIndexInputStream.getDocumentNumber(), 
								docIndexInputStream.getDocumentLength(), 
								directIndexPost);
								*/
						newdocid++;
						if (olddocid != newdocid){
							docidMap.put(olddocid, newdocid);
						}
					}
				}
			}
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
		try{
			LexiconInputStream lexInputStream = (USE_UTF)?
				(new UTFLexiconInputStream(indexPath, indexPrefix)):
					(new LexiconInputStream(indexPath, indexPrefix));
			LexiconOutputStream lexOutputStream = (USE_UTF)?
				(new UTFLexiconOutputStream(indexPath, indexPrefix+tmpSuffix)):
					(new LexiconOutputStream(indexPath, indexPrefix+tmpSuffix));
			BlockInvertedIndex invIndex = (USE_UTF)?
				(new BlockInvertedIndex(new UTFLexicon(indexPath, indexPrefix), indexPath, indexPrefix)):
					(new BlockInvertedIndex(new Lexicon(indexPath, indexPrefix), indexPath, indexPrefix));
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
				TIntIntHashMap docidFSMap = new TIntIntHashMap();
				TIntIntHashMap docidBlockFreqMap = new TIntIntHashMap();
				TIntObjectHashMap<int[]> docidBlockidMap = new TIntObjectHashMap<int[]>();
				int blockidIndex = 0;
				for (int i=0; i<docs[0].length; i++){
					if (!docidsToRemoveSet.contains(docs[0][i])){
						// convert old docids to new docids
						int newdocid = docidMap.get(docs[0][i]);
						int docid = (newdocid==0)?(docs[0][i]):(newdocid);
						
						docidsSet.add(docid);
						docidtfMap.put(docid, docs[1][i]);
						docidFSMap.put(docid, docs[2][i]);
						docidBlockFreqMap.put(docid, docs[3][i]);
						int[] blockids = new int[docs[3][i]];
						for (int j=0; j<docs[3][i]; j++)
							blockids[j] = docs[4][blockidIndex++];
						docidBlockidMap.put(docid, blockids);
					}
				}
				docs = new int[5][];
				docs[1] = new int[docidsSet.size()];
				docs[2] = new int[docidsSet.size()];
				docs[3] = new int[docidsSet.size()];
				int[] docids = docidsSet.toArray();
				Arrays.sort(docids);
				docs[0] = docids;
				TIntArrayList blockidList = new TIntArrayList();
				for (int i=0; i<docidsSet.size(); i++){
					docs[1][i] = docidtfMap.get(docids[i]);
					docs[2][i] = docidFSMap.get(docids[i]);
					docs[3][i] = docidBlockFreqMap.get(docids[i]);
					blockidList.add(docidBlockidMap.get(docids[i]));
				}
				docs[4] = blockidList.toNativeArray();
				docidsSet.clear(); docidsSet = null;
				docidtfMap.clear(); docidtfMap = null;
				docidFSMap.clear(); docidFSMap = null;
				docidBlockFreqMap.clear(); docidBlockFreqMap = null;
				docidBlockidMap.clear(); docidBlockidMap = null;
				blockidList.clear(); blockidList = null;
				writeBlockPostings(docs, docs[0][0]+1, invertedOutput, binaryBits=0);
				
				long endOffset = invertedOutput.getByteOffset();
				byte endBitOffset = invertedOutput.getBitOffset();
				endBitOffset--;
				if (endBitOffset < 0 && endOffset > 0) {
					endBitOffset = 7;
					endOffset--;
				}
				//System.err.println("endOffset: "+endOffset+", endBitOffset: "+endBitOffset);
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
			}
			lexInputStream.close();
			lexOutputStream.close();
			invIndex.close();
			invertedOutput.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		
		// rebuild the lexid
		createLexidFile(indexPath, indexPrefix+tmpSuffix, maxTermid);
		System.err.println("Done.");
		
		// rebuild direct index
		System.err.print("Rebuilding direct file...");
		try{
			// scan the direct file
			// store entries of the removed documents in memory
			BlockDirectIndex directIndex = new BlockDirectIndex(new DocumentIndex(indexPath, indexPrefix), indexPath, indexPrefix);
			DocumentIndexInputStream docIndexInputStream = new DocumentIndexInputStream(indexPath, indexPrefix);
			DocumentIndexBuilder docIndexBuilder = new DocumentIndexBuilder(indexPath, indexPrefix+tmpSuffix); 
			
			BlockDirectIndexBuilder directIndexBuilder = new BlockDirectIndexBuilder(
					indexPath, 
					indexPrefix+tmpSuffix
					);
			int olddocid = -1;
			int[][] terms = null;
			while (docIndexInputStream.readNextEntry()!=-1){
				olddocid = docIndexInputStream.getDocumentId();
				terms = directIndex.getTerms(olddocid);
				if (docidsToRemoveSet.contains(olddocid)){
					// we may need to flush the hashmap to disk to reduce memory usage
					System.err.println("delete document ("+olddocid+") "+
							docIndexInputStream.getDocumentNumber()+" from direct file.");
				}else{
					if (terms!=null){ 
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
		String[] args = {"--createlexiconhash", indexPath, indexPrefix+tmpSuffix};
		LexiconBuilder.main(args);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		BlockIncrementalStructureMerger merger = new BlockIncrementalStructureMerger(args[1], args[2]);
		int bits = Integer.parseInt(args[0]);
		merger.setNumberOfBits(bits);
		merger.setOutputFilename(args[3]);
		merger.mergeStructures(merger.oldIndexPath, merger.oldIndexPrefix,
				merger.newIndexPath, merger.newIndexPrefix);
	}

}

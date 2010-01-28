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
 * The Original Code is InvertedIndexBuilder.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original author)
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.structures.indexing;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntIntHashMap;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import uk.ac.gla.terrier.compression.BitOut;
import uk.ac.gla.terrier.compression.BitOutputStream;
import uk.ac.gla.terrier.structures.DirectIndexInputStream;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.LexiconInputStream;
import uk.ac.gla.terrier.structures.LexiconOutputStream;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.FieldScore;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.Rounding;
/**
 * Builds an inverted index. It optionally saves term-field information as well. 
 * <p><b>Algorithm:</b>
 * <ol>
 * <li>While there are terms left:
 *  <ol>
 *  <li>Read M term ids from lexicon, in lexicographical order</li>
 *  <li>Read the occurrences of these M terms into memory from the direct file</li>
 *  <li>Write the occurrences of these M terms to the inverted file</li>
 *  </ol>
 * <li>Rewrite the lexicon, removing block frequencies, and adding inverted file offsets</li>
 * <li>Write the collection statistics</li>
 * </ol>
 * <p><b>Lexicon term selection:</b>
 * There are two strategies of selecting the number of terms to read from the lexicon. The trade-off here
 * is to read a small enough number of terms into memory such that the occurrences of all those terms from
 * the direct file can fit in memory. On the other hand, the less terms that are read implies more iterations,
 * which is I/O expensive, as the entire direct file has to be read for every iteration.<br>
 * The two strategies are:
 * <ul>
 * <li>Read a fixed number of terms on each iterations - this corresponds to the property
 *  <tt>invertedfile.processterms</tt></li>
 * <li>Read a fixed number of occurrences (pointers) on each iteration. The number of pointers can be determined
 *  using the sum of frequencies of each term from the lexicon. This corresponds to the property
 *  <tt>invertedfile.processpointers</tt>. 
 * </li></ul>
 * By default, the 2nd strategy is chosen, unless the <tt>invertedfile.processpointers</tt> has a zero
 * value specified.<P>
 * Properties:
 * <ul>
 *  <li><tt>invertedfile.processterms</tt>- the number of terms to process in each iteration. Defaults to 75,000</li>
 *  <li><tt>invertedfile.processpointers</tt> - the number of pointers to process in each iteration. Defaults to 20,000,000</li>
 * </ul>
 * @author Craig Macdonald &amp; Vassilis Plachouras
 * @version $Revision: 1.1 $
 */
public class InvertedIndexBuilder {

	/** class to be used as a lexiconinputstream. set by this and child classes */
	protected Class lexiconInputStream = null;
	/** class to be used as a lexiconoutpustream. set by this and child classes */
	protected Class lexiconOutputStream = null;


	/** The logger used */
	protected static Logger logger = Logger.getRootLogger();
	
	protected static class IntLongTuple
	{
		final int Terms;
		final long Pointers;
		IntLongTuple(int a, long b)
		{
			Terms = a;
			Pointers = b;
		}
	}

	/** the directory in which index files should be created */
	protected String indexPath;
	/** the first part of the filename component of index files */
	protected String indexPrefix;
	
	protected String indexPathPrefix;
	
	/** The number of unique terms in the vocabulary.*/
	public int numberOfUniqueTerms;
	
	/** The number of documents in the collection.*/
	public int numberOfDocuments;
	
	/** The number of tokens in the collection.*/
	public long numberOfTokens = 0;
	
	/** The number of pointers in the inverted file.*/
	public long numberOfPointers = 0;

	/** Indicates whether field information is used. */
	protected final boolean useFieldInformation = FieldScore.USE_FIELD_INFORMATION;
	
	protected Index index = null;
	
	/** The number of pointers to be processed in an interation. This directly corresponds to the
	  * property <tt>invertedfile.processpointers</tt>. If this property is set and > 0, then each
	  * iteration of the inverted index creation will be done to a set number of pointers, not a set
	  * number of terms, overriding <tt>invertedfile.processterms</tt>. Default is 20000000. */
	protected long numberOfPointersPerIteration = Integer.parseInt(
		ApplicationSetup.getProperty("invertedfile.processpointers", "20000000"));
	
	/**
	 * The underlying bit file.
	 */
	protected BitOut file;

	/**
	 * Constructor of the class InvertedIndex.
	 * @deprecated
	 */
	public InvertedIndexBuilder(String Path, String Prefix)
	{
		indexPath = Path; indexPrefix = Prefix;
		indexPathPrefix = indexPath + ApplicationSetup.FILE_SEPARATOR + indexPrefix;
		try{
			file = new BitOutputStream(indexPathPrefix + ApplicationSetup.IFSUFFIX);
		} catch (IOException ioe) {
			logger.error("creating BitOutputStream for writing the inverted file : ", ioe);
		}
		lexiconInputStream = LexiconInputStream.class;
		lexiconOutputStream = LexiconOutputStream.class;
	}
	
	public InvertedIndexBuilder(Index i)
	{
		this.index = i;
		indexPath = index.getPath(); indexPrefix = index.getPrefix();
		indexPathPrefix = indexPath + ApplicationSetup.FILE_SEPARATOR + indexPrefix;
		try{
			file = new BitOutputStream(indexPathPrefix + ApplicationSetup.IFSUFFIX);
		} catch (IOException ioe) {
			logger.error("creating BitOutputStream for writing the inverted file : ", ioe);
		}
		lexiconInputStream = LexiconInputStream.class;
		lexiconOutputStream = LexiconOutputStream.class;
	}


	/**
	 * A default constructor of the class InvertedIndex.
	 * @deprecated
	 */
	public InvertedIndexBuilder() {
		this(ApplicationSetup.TERRIER_INDEX_PATH,
			ApplicationSetup.TERRIER_INDEX_PREFIX);
	}

	/**
	 * Creates an instance of the InvertedIndex
	 * class using the given filename.
	 * @param filename The name of the inverted file
	 * @deprecated Use this() or this(String, String)
	 */
	public InvertedIndexBuilder(String filename) {
		try{
			file = new BitOutputStream(filename);
		} catch (IOException ioe) {
			logger.error("Creating BitOutputStream for writing the direct file : ", ioe);
		}
		lexiconInputStream = LexiconInputStream.class;
		lexiconOutputStream = LexiconOutputStream.class;
	}

	/**
	 * Closes the underlying bit file.
	 */
	public void close() throws IOException {
		file.close();
	}

	/**
	 * Creates the inverted index using the already created direct index,
	 * document index and lexicon.
	 */
	public void createInvertedIndex() {
		try {
			Runtime r = Runtime.getRuntime();
			logger.debug("creating inverted index");
			final String LexiconFilename = indexPathPrefix + ApplicationSetup.LEXICONSUFFIX;
			
			final int numberOfDocuments = index.getCollectionStatistics().getNumberOfDocuments();
		
			long assumedNumberOfPointers = Long.parseLong(index.getIndexProperty("num.Pointers", "0"));				
			long numberOfTokens = 0;
			long numberOfPointers = 0;
		
			LexiconInputStream lexiconStream = getLexInputStream(LexiconFilename);
			numberOfUniqueTerms = lexiconStream.numberOfEntries();
			final int fieldsCount = FieldScore.FIELDS_COUNT;
			//A temporary file for storing the updated lexicon file, after
			// creating the inverted file
			DataOutputStream dos = new DataOutputStream(Files.writeFileStream(LexiconFilename.concat(".tmp2")));

			//if the set number of terms to process is higher than the
			// available,
			if (processTerms > numberOfUniqueTerms)
				processTerms = (int) numberOfUniqueTerms;
			long startProcessingLexicon = 0;
			long startTraversingDirectFile = 0;
			long startWritingInvertedFile = 0;
			long numberOfPointersThisIteration = 0;
			
			int i=0; int iterationCounter = 0;
			// generate a message guessing iteration counts
			String iteration_message_suffix = null;
			if (numberOfPointersPerIteration > 0 || processTerms ==0)
			{
				if (assumedNumberOfPointers > 0)
				{
					iteration_message_suffix = " of "
						+ ((assumedNumberOfPointers % numberOfPointersPerIteration ==0 )
							? (assumedNumberOfPointers/numberOfPointersPerIteration)
							: 1+(assumedNumberOfPointers/numberOfPointersPerIteration))
						+ " iterations";
				}
				else
				{
					iteration_message_suffix = "";
				}
			}
			else
			{
				iteration_message_suffix = " of "
					+ ((numberOfUniqueTerms % processTerms ==0 ) 
						? (numberOfUniqueTerms/processTerms)
						: 1+(numberOfUniqueTerms/processTerms))
					+ " iterations";
			}

			if (numberOfPointersPerIteration == 0)
			{
				logger.warn("Using old-fashioned number of terms strategy. Please consider setting invertedfile.processpointers for forward compatible use");
			}
		
			while(i<numberOfUniqueTerms)
			{
				iterationCounter++;
				TIntIntHashMap codesHashMap = null;
				TIntArrayList[][] tmpStorage = null;
				IntLongTuple results = null;
				
				logger.info("Iteration "+iterationCounter+iteration_message_suffix);
				
				//traverse the lexicon looking to determine the first N() terms
				//this can be done two ways: for the first X terms
				//OR for the first Y pointers
				//ie either N=X, or N=fn(Y)
				
				startProcessingLexicon = System.currentTimeMillis();
				
				if (numberOfPointersPerIteration > 0)
				{//we've been configured to run with a given number of pointers
					if (logger.isDebugEnabled())
						logger.debug("Scanning lexicon for "+ numberOfPointersPerIteration + " pointers");
				
					/* this is less speed efficient, as we have no way to guess how many
					 * terms it will take to fill the given number of pointers. 
					 * The advantage is that memory consumption is more directly correlated
					 * to number of pointers than number of terms, so when indexing tricky
					 * collections, it is easier to find a number of pointers that can fit
					 * in memory */
					 
					codesHashMap = new TIntIntHashMap();
					ArrayList<TIntArrayList[]> tmpStorageStorage = new ArrayList<TIntArrayList[]>();
					results = scanLexiconForPointers(
						numberOfPointersPerIteration, 
						lexiconStream,
						codesHashMap,
						tmpStorageStorage);
					tmpStorage = (TIntArrayList[][]) tmpStorageStorage.toArray(
						new TIntArrayList[0][0]);
					
				}
				else//we're running with a given number of terms
				{
					if (logger.isDebugEnabled())
						logger.debug("Scanning lexicon for " + processTerms+" terms");
					tmpStorage = new TIntArrayList[processTerms][];
					codesHashMap = new TIntIntHashMap(processTerms);
					results = scanLexiconForTerms(
						processTerms,
						lexiconStream,
						codesHashMap,
						tmpStorage);
				}
				
				processTerms = results.Terms;//no of terms to process on this iteration
				numberOfPointersThisIteration = results.Pointers;
				numberOfPointers += results.Pointers;//no of pointers to process on this iteration
				i += processTerms;
				
				if (logger.isDebugEnabled())
					logger.debug("time to process part of lexicon: " + ((System.currentTimeMillis()- startProcessingLexicon) / 1000D));
				
				
				displayMemoryUsage(r);	
				
				//Scan the direct file looking for those terms
				startTraversingDirectFile = System.currentTimeMillis();
				traverseDirectFile(codesHashMap, tmpStorage);
				if (logger.isDebugEnabled())
					logger.debug("time to traverse direct file: " + ((System.currentTimeMillis() - startTraversingDirectFile) / 1000D));
				
				displayMemoryUsage(r);			
	
				//write the inverted file for this part of the lexicon, ie processTerms number of terms
				startWritingInvertedFile = System.currentTimeMillis();
				numberOfTokens += writeInvertedFilePart(dos, tmpStorage, processTerms);
				if (logger.isDebugEnabled())
					logger.debug("time to write inverted file: "
					 + ((System.currentTimeMillis()- startWritingInvertedFile) / 1000D));
				
							
				displayMemoryUsage(r);
	
				if (logger.isDebugEnabled()) {
					logger.debug(
							"time to perform one iteration: "
								+ ((System.currentTimeMillis() - startProcessingLexicon)
									/ 1000D));
					logger.debug(
						"number of pointers processed: "
							+ numberOfPointersThisIteration);	
				}
				
				
				tmpStorage  = null; 
				codesHashMap.clear(); 
				codesHashMap = null;
			}
			
			
			
			file.close();

			this.numberOfDocuments = numberOfDocuments;
			this.numberOfTokens = numberOfTokens;
			this.numberOfUniqueTerms = numberOfUniqueTerms;
			this.numberOfPointers = numberOfPointers;

			lexiconStream.close();
			dos.close();
			//finalising the lexicon file with the updated information
			//on the frequencies and the offsets
			
			//reading the original lexicon
			LexiconInputStream lis = getLexInputStream(LexiconFilename);
			
			//the updated lexicon
			LexiconOutputStream los = getLexOutputStream(LexiconFilename.concat(".tmp3"));
			
			//the temporary data containing the offsets
			DataInputStream dis = new DataInputStream(Files.openFileStream(LexiconFilename.concat(".tmp2")));
			
			while (lis.readNextEntryBytes() != -1) {
				los.writeNextEntry(lis.getTermCharacters(), lis.getTermId(),
						lis.getNt(), 
						dis.readInt(), //the term frequency
						dis.readLong(), //end byte offset
						dis.readByte());//end bit offset
			}
			lis.close();
			los.close();
			dis.close();
            if (! Files.delete(LexiconFilename))
                logger.error("delete file .lex failed!");
            if (! Files.delete(LexiconFilename.concat(".tmp2")))
                logger.error("delete file .lex.tmp2 failed!");
            if (! Files.rename(LexiconFilename.concat(".tmp3"), LexiconFilename))
                logger.error("rename file .lex.tmp3 to .lex failed!");
			
			index.addIndexStructure(
					"inverted", 
					"uk.ac.gla.terrier.structures.InvertedIndex", 
					"uk.ac.gla.terrier.structures.Lexicon,java.lang.String,java.lang.String", 
					"lexicon,path,prefix");
			index.addIndexStructureInputStream(
                    "inverted",
                    "uk.ac.gla.terrier.structures.InvertedIndexInputStream",
                    "java.lang.String,java.lang.String,uk.ac.gla.terrier.structures.LexiconInputStream",
                    "path,prefix,lexicon-inputstream");
			index.setIndexProperty("num.inverted.fields.bits", ""+FieldScore.FIELDS_COUNT );
			//should be already set, but in case their not
			index.setIndexProperty("num.Terms", ""+numberOfUniqueTerms);
			index.setIndexProperty("num.Tokens", ""+numberOfTokens);
			index.setIndexProperty("num.Pointers", ""+numberOfPointers);
			System.gc();
			
		} catch (IOException ioe) {
			logger.error("IOException occured during creating the inverted file. Stack trace follows.", ioe);
		}
	}
	
	/** Iterates through the lexicon, until it has reached the given number of pointers
	  * @param PointersToProcess Number of pointers to stop reading the lexicon after
	  * @param lexiconStream the lexicon input stream to read 
	  * @param codesHashMap
	  * @param tmpStorageStorage
	  * @return
	  */
	protected IntLongTuple scanLexiconForPointers(
		final long PointersToProcess, 
		final LexiconInputStream lexiconStream, 
		final TIntIntHashMap codesHashMap,
		final ArrayList<TIntArrayList[]> tmpStorageStorage)
		throws IOException
	{
		int processTerms = 0;	
		long numberOfPointersThisIteration = 0;
		int j=0; //counter of loop iterations
		while(numberOfPointersThisIteration < PointersToProcess) {
		
			if (lexiconStream.readNextEntry() == -1)
				break;
			
			processTerms++;
			
			TIntArrayList[] tmpArray = new TIntArrayList[3];
			final int tmpNT = lexiconStream.getNt();
			tmpArray[0] = new TIntArrayList(tmpNT);
			tmpArray[1] = new TIntArrayList(tmpNT);
			tmpArray[2] = new TIntArrayList(tmpNT);
			
			numberOfPointersThisIteration += tmpNT;
			
			
			tmpStorageStorage.add(tmpArray);
			
			//the class TIntIntHashMap return zero when you look up for a
			//the value of a key that does not exist in the hash map.
			//For this reason, the values that will be inserted in the 
			//hash map are increased by one. 
			codesHashMap.put(lexiconStream.getTermId(), j + 1);
			
			//increment counter
			j++;
		}
		if (logger.isDebugEnabled())
			logger.debug(
					numberOfPointersThisIteration + " pointers == "+
					processTerms +" terms");
		return new IntLongTuple(processTerms, numberOfPointersThisIteration);
	}
	
	
	/** Iterates through the lexicon, until it has reached the given number of terms
	  * @param processTerms Number of terms to stop reading the lexicon after
	  * @param lexiconStream the lexicon input stream to read 
	  * @param codesHashMap
	  * @param tmpStorageStorage
	  * @return
	  */
	protected IntLongTuple scanLexiconForTerms(
		final int processTerms, 
		final LexiconInputStream lexiconStream, 
		final TIntIntHashMap codesHashMap,
		TIntArrayList[][] tmpStorage)
		throws IOException
	{
		int j = 0; 
		
		long numberOfPointersThisIteration = 0;
		for (; j < processTerms; j++) {
		
			if (lexiconStream.readNextEntry() == -1)
				break;
		
			TIntArrayList[] tmpArray = new TIntArrayList[3];
			final int tmpNT = lexiconStream.getNt();
			tmpArray[0] = new TIntArrayList(tmpNT);
			tmpArray[1] = new TIntArrayList(tmpNT);
			tmpArray[2] = new TIntArrayList(tmpNT);
			
			numberOfPointersThisIteration += tmpNT;
			
			tmpStorage[j] = tmpArray;
			
			
			//the class TIntIntHashMap return zero when you look up for a
			//the value of a key that does not exist in the hash map.
			//For this reason, the values that will be inserted in the 
			//hash map are increased by one. 
			codesHashMap.put(lexiconStream.getTermId(), j + 1);
		}
		if (logger.isDebugEnabled())
			logger.debug(
				numberOfPointersThisIteration + " pointers == "+
				j +" terms");
		return new IntLongTuple(j, numberOfPointersThisIteration);
	}

	/**
	 * Traverses the direct index and creates the inverted index entries 
	 * for the terms specified in the codesHashMap and tmpStorage.
	 * @param tmpStorage TIntArrayList[][] an array of the inverted index entries to store
	 * @param codesHashMap a mapping from the term identifiers to the index 
	 *		in the tmpStorage matrix. 
	 * @throws IOException if there is a problem while traversing the direct index.
	 */
	 
	protected void traverseDirectFile(TIntIntHashMap codesHashMap, TIntArrayList[][] tmpStorage) 
		throws IOException 
	{
		//scan the direct file
		DirectIndexInputStream directInputStream =
			index != null
				? (DirectIndexInputStream)index.getIndexStructureInputStream("direct")
				: new DirectIndexInputStream(indexPath, indexPrefix);
		int[][] documentTerms = null;
		int p = 0; //a document counter;
		final boolean useFieldInformation = this.useFieldInformation;
		while ((documentTerms = directInputStream.getNextTerms())
			!= null) {
			p += directInputStream.getDocumentsSkipped();
			//the two next vectors are used for reducing the number of references
			final int[] documentTerms0 = documentTerms[0];
			final int[] termfreqs = documentTerms[1];
			int[] htmlscores = null;
			if (useFieldInformation)
				htmlscores = documentTerms[2];

			//scan the list of the j-th document's terms
			final int length = documentTerms0.length;
			
			for (int k = 0; k < length; k++) {
				//if the k-th term of the document is to be indexed in this pass
				int codePairIndex = codesHashMap.get(documentTerms0[k]);

				if (codePairIndex > 0) {
					/* need to decrease codePairIndex because it has been already 
					 * increased while storing in codesHashMap */
					codePairIndex--;
					TIntArrayList[] tmpMatrix = tmpStorage[codePairIndex];

					tmpMatrix[0].add(p);
					tmpMatrix[1].add(termfreqs[k]);
					if (useFieldInformation)
						tmpMatrix[2].add(htmlscores[k]);
				}
			}
			p++;
		}
		directInputStream.close();
	}
	 
	protected void traverseDirectFile(int[][][] tmpStorage, int[] indices, TIntIntHashMap codesHashMap) 
		throws IOException
	{
		DirectIndexInputStream directInputStream = new DirectIndexInputStream(
			indexPath, indexPrefix);
		int[][] documentTerms = null;
		int[] documentTerms0 = null;
		int[] documentTerms1 = null;
		int[] documentTerms2 = null;
		int[][] tmpMatrix = null;
		int codePairIndex;
		int p = 0; //a counter;
		int tmp_indices_codePairIndex; //a temporary variable
		while ((documentTerms = directInputStream.getNextTerms()) != null) {
			p += directInputStream.getDocumentsSkipped();
			//the two next vectors are used for reducing the number of
			// references
			documentTerms0 = documentTerms[0];
			documentTerms1 = documentTerms[1];
			//scan the list of the j-th document's terms
			final int length = documentTerms0.length;
			if (useFieldInformation) { //if we are processing tag information
				documentTerms2 = documentTerms[2];
				
				for (int k = 0; k < length; k++) {
					//if the k-th term of the document is to be indexed in
					// this pass
					
					if ((codePairIndex = codesHashMap.get(documentTerms0[k])) > 0) {
						codePairIndex--; //need to decrease it because it has been increased while storing to the codesHashMap
						tmpMatrix = tmpStorage[codePairIndex];
						tmp_indices_codePairIndex = indices[codePairIndex];
						tmpMatrix[0][tmp_indices_codePairIndex] = p;
						tmpMatrix[1][tmp_indices_codePairIndex] = documentTerms1[k];
						tmpMatrix[2][tmp_indices_codePairIndex] = documentTerms2[k];
						indices[codePairIndex]++;
					}
				}						
			} else { //if we are not processing tag information
				for (int k = 0; k < length; k++) {
					//if the k-th term of the document is to be indexed in
					// this pass
					if ((codePairIndex = codesHashMap.get(documentTerms0[k])) > 0) {
						codePairIndex--;
						//tmpMatrix = (int[][]) tmpStorage.elementAt(codePairIndex);
						tmpMatrix = tmpStorage[codePairIndex];
						tmp_indices_codePairIndex = indices[codePairIndex];
						tmpMatrix[0][tmp_indices_codePairIndex] = p;
						tmpMatrix[1][tmp_indices_codePairIndex] = documentTerms1[k];
						indices[codePairIndex]++;
					}
				}
			}
			p++;
		}
		directInputStream.close();
	}
	
	/** Writes the section of the inverted file 
	 * @param dos a temporary data structure that contains the offsets in the inverted
	 *  index for each term.
	 * @param tmpStorage Occurrences information, as described in traverseDirectFile().
	 *  This data is consumed by this method - once this method has been called, all
	 *  the data in tmpStorage will be destroyed.
	 * @param processTerms The number of terms being processed in this iteration.
	 * @returns the number of tokens processed in this iteration */
	protected long writeInvertedFilePart(
		final DataOutputStream dos, 
		TIntArrayList[][] tmpStorage, 
		final int processTerms)
		throws IOException
	{
		//write to the inverted file. We should note that the lexicon 
		//file should be updated as well with the term frequency and
		//the endOffset and endBitOffset.
		
		//remove this, as it now happens at the end of this method
		//the first call is made at the start of createInvertedIndex
		//file.writeReset();
		int frequency; long numTokens = 0;
		for (int j = 0; j < processTerms; j++) {
			frequency = 0; //the term frequency
			TIntArrayList[] tmpMatrix = tmpStorage[j];
			final int[] tmpMatrix0 = tmpMatrix[0].toNativeArray();
			tmpMatrix[0] = null;
			final int[] tmpMatrix1 = tmpMatrix[1].toNativeArray();
			tmpMatrix[1] = null;
			int[] tmpMatrix2 = null;
			if (useFieldInformation)
			{
				tmpMatrix2 = tmpMatrix[2].toNativeArray();
				tmpMatrix[2] = null;
			}
			tmpMatrix = null;
			tmpStorage[j] = null;
			
			//write the first entry
			int docid = tmpMatrix0[0];
			file.writeGamma(docid + 1);
			int termfreq = tmpMatrix1[0];
			frequency += termfreq;
			file.writeUnary(termfreq);
			
			if (useFieldInformation)
			{
				file.writeBinary(FieldScore.FIELDS_COUNT, tmpMatrix2[0]);
				for (int k = 1; k < tmpMatrix0.length; k++) {
					file.writeGamma(tmpMatrix0[k] - docid);
					docid = tmpMatrix0[k];
					termfreq	 = tmpMatrix1[k];
					frequency += termfreq;
					file.writeUnary(termfreq);
					file.writeBinary(FieldScore.FIELDS_COUNT, tmpMatrix2[k]);
				}
			}
			else
			{
				for (int k = 1; k < tmpMatrix0.length; k++) {
					file.writeGamma(tmpMatrix0[k] - docid);
					docid = tmpMatrix0[k];
					termfreq = tmpMatrix1[k];
					frequency += termfreq;
					file.writeUnary(termfreq);
				}
			}
			
			long endOffset = file.getByteOffset();
			byte endBitOffset = file.getBitOffset();
			endBitOffset--;
			if (endBitOffset < 0 && endOffset > 0) {
				endBitOffset = 7;
				endOffset--;
			}
			numTokens += frequency;
			dos.writeInt(frequency);
			dos.writeLong(endOffset);
			dos.writeByte(endBitOffset);
		}
		//file.writeFlush();
		//we have to force a reset here, as otherwise the buffer isn't cleared.
		//file.writeReset();
		return numTokens;
	}
	
	/**
	 * The number of terms for which the inverted file 
	 * is built each time. The corresponding property
	 * is <tt>invertedfile.processterms</tt> and the 
	 * default value is <tt>75000</tt>. The higher the
	 * value, the greater the requirements for memory are, 
	 * but the less time it takes to invert the direct 
	 * file. 
	 */
	protected int processTerms = Integer.parseInt(ApplicationSetup.getProperty("invertedfile.processterms", "75000"));
	
	/*
	for (int i = 0; i < numberOfUniqueTerms; i = i + processTerms) {
		//set the number of terms to process from the lexicon
		if ((i + processTerms) > numberOfUniqueTerms)
			processTerms = (int) numberOfUniqueTerms - i;
		//start processing part of the lexicon
		startProcessingLexicon = System.currentTimeMillis();
		//preparing the data structures to store the data
		int[] indices = new int[processTerms];
		int[][][] tmpStorage = new int[processTerms][][];
		TIntIntHashMap codesHashMap = new TIntIntHashMap(processTerms);
		int numberOfPointersPerIteration = 0;
	
		int numOfFields = 2;
		if (useFieldInformation)
			numOfFields = 3;
	
		for (int j = 0; j < processTerms; j++) {
			lexiconStream.readNextEntry();
			//int[][] tmpArray = new int[numOfFields][lexiconStream.getNt()];
			numberOfPointersPerIteration += lexiconStream.getNt();
			//tmpStorage.add(tmpArray);
			tmpStorage[j] = new int[numOfFields][lexiconStream.getNt()];
			//the class TIntIntHashMap return zero when you look up for
			// a the value of a key that does not exist in the hash map.
			//For this reason, the values that will be inserted in the
			//hash map are increased by one.
			codesHashMap.put(lexiconStream.getTermId(), j + 1);
		}
		numberOfPointers += numberOfPointersPerIteration;
		endProcessingLexicon = System.currentTimeMillis();
		startTraversingDirectFile = System.currentTimeMillis();
		//scan the direct file
		//uses indices, tmpStorage and codesHashMap
		traverseDirectFile(tmpStorage, indices, codesHashMap);
		//end of traversing the
		endTraversingDirectFile = System.currentTimeMillis();
		startWritingInvertedFile = System.currentTimeMillis();
		//write to the inverted file. We should note that the lexicon
		//file should be updated as well with the term frequency and
		//the endOffset and endBitOffset.
		//file.writeReset();
		int frequency;
		int[][] tmpMatrix = null;
		int[] tmpMatrix0 = null;
		int[] tmpMatrix1 = null;
	
		for (int j = 0; j < processTerms; j++) {
			frequency = 0; //the term frequency
			//tmpMatrix = (int[][]) tmpStorage.elementAt(j);
			tmpMatrix = tmpStorage[j];
			tmpMatrix0 = tmpMatrix[0];
			tmpMatrix1 = tmpMatrix[1];
	
			//we do not need to sort because the documents are read in
			//order of docid, and therefore the arrays are already
			// sorted.
			if (useFieldInformation) {
				int[] tmpMatrix2 = tmpMatrix[2];
				//write the first entry
				file.writeGamma(tmpMatrix0[0] + 1);
				frequency += tmpMatrix1[0];
				file.writeUnary(tmpMatrix1[0]);
				file.writeBinary(fieldsCount, tmpMatrix2[0]);
				final int tmpMatrix0Length = tmpMatrix0.length;
				for (int k = 1; k < tmpMatrix0Length; k++) {
					file.writeGamma(tmpMatrix0[k] - tmpMatrix0[k - 1]);
					frequency += tmpMatrix1[k];
					file.writeUnary(tmpMatrix1[k]);
					file.writeBinary(fieldsCount, tmpMatrix2[k]);
				}
			} else {
				//write the first entry
				file.writeGamma(tmpMatrix0[0] + 1);
				frequency += tmpMatrix1[0];
				file.writeUnary(tmpMatrix1[0]);
				final int tmpMatrix0Length = tmpMatrix0.length;
				for (int k = 1; k < tmpMatrix0Length; k++) {
					file.writeGamma(tmpMatrix0[k] - tmpMatrix0[k - 1]);
					frequency += tmpMatrix1[k];
					file.writeUnary(tmpMatrix1[k]);
				}
			}
	
			long endOffset = file.getByteOffset();
			byte endBitOffset = file.getBitOffset();
			endBitOffset--;
			if (endBitOffset < 0 && endOffset > 0) {
				endBitOffset = 7;
				endOffset--;
			}
			numberOfTokens += frequency;
			dos.writeInt(frequency);
			dos.writeLong(endOffset);
			dos.writeByte(endBitOffset);
		}
		//file.writeFlush();
		endWritingInvertedFile = System.currentTimeMillis();
	
		System.err.println("time to process part of lexicon: "
			+ ((endProcessingLexicon - startProcessingLexicon) / 1000D));
		System.err.println("time to traverse direct file: "
			+ ((endTraversingDirectFile - startTraversingDirectFile) / 1000D));
		System.err.println("time to write inverted file: "
			+ ((endWritingInvertedFile - startWritingInvertedFile) / 1000D));
		System.err.println("time to perform one iteration: "
			+ ((endWritingInvertedFile - startProcessingLexicon) / 1000D));
		System.err.println("number of pointers processed: "
			+ numberOfPointersPerIteration);
		
		indices = null;
		tmpStorage  = null; 
		codesHashMap.clear(); 
		codesHashMap = null;
	
	}
	*/
	
	public static void displayMemoryUsage(Runtime r)
	{
		if (logger.isDebugEnabled())
			logger.debug("free: "+ (r.freeMemory() /1024) + "kb; total: "+(r.totalMemory()/1024)
					+"kb; max: "+(r.maxMemory()/1024)+"kb; "+
					Rounding.toString((100*r.freeMemory() / r.totalMemory()),1)+"% free; "+
					Rounding.toString((100*r.totalMemory() / r.maxMemory()),1)+"% allocated; "
		);
	}

	public LexiconInputStream getLexInputStream(String filename)
	{
		LexiconInputStream li = null;
		try{
			li = (LexiconInputStream) lexiconInputStream.getConstructor(String.class).newInstance(filename);
		} catch (Exception e) {
			logger.error("Problem loading a LexiconInputStream", e);
		}
		return li;
	}

	public LexiconOutputStream getLexOutputStream(String filename)
	{
		LexiconOutputStream lo = null;
		try{
			lo = (LexiconOutputStream) lexiconOutputStream.getConstructor(String.class).newInstance(filename);
		} catch (Exception e) {
			logger.error("Problem loading a LexiconOutputStream", e);
		}
		return lo;
	}

}



package uk.ac.gla.terrier.structures.incrementalindex2;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntHashSet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.applications.TRECQuerying;
import uk.ac.gla.terrier.compression.BitOutputStream;
import uk.ac.gla.terrier.indexing.Collection;
import uk.ac.gla.terrier.indexing.Document;
import uk.ac.gla.terrier.indexing.TRECCollection;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.DirectIndex;
import uk.ac.gla.terrier.structures.DirectIndexInputStream;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.DocumentIndexInputStream;
import uk.ac.gla.terrier.structures.DocumentIndexOutputStream;
import uk.ac.gla.terrier.structures.FilePosition;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.InvertedIndex;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.structures.LexiconInputStream;
import uk.ac.gla.terrier.structures.LexiconOutputStream;
import uk.ac.gla.terrier.structures.incrementalindex2.memoryindex.MemoryDirectIndex;
import uk.ac.gla.terrier.structures.incrementalindex2.memoryindex.MemoryDocumentIndex;
import uk.ac.gla.terrier.structures.incrementalindex2.memoryindex.MemoryIndex;
import uk.ac.gla.terrier.structures.incrementalindex2.memoryindex.MemoryInvertedIndex;
import uk.ac.gla.terrier.structures.incrementalindex2.memoryindex.MemoryLexicon;
import uk.ac.gla.terrier.structures.indexing.LexiconBuilder;
import uk.ac.gla.terrier.structures.merging.StructureMerger;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;

/** @author Craig Macdonald. Based on code written by John Kane.
 * 
 */
public class IncrementalIndex extends Index {
	
	/** the logger used */
	private static Logger logger = Logger.getRootLogger();
	
	/* Global properties */ 
	public final static int MAX_SEGMENTS = Integer.parseInt(ApplicationSetup.getProperty("incremental.index.max.segments", "5"));
	protected final static int MEMORY_SEGMENT_DOCUMENT_LIMIT = Integer.parseInt(ApplicationSetup.getProperty("incremental.index.max.memory.segment.docs.size", "1000"));
	
	/* Global properties that can be overridden by index */
	protected boolean USE_DIRECT_FILE = Boolean.parseBoolean(ApplicationSetup.getProperty("incremental.index.use.direct.file", "true"));
	
	
	protected final Properties indexProperties = new Properties();
	protected String propertiesFile;
	
	protected int numSegments = 0;
	protected int indexSerialNum = 0;
	/** incremented after MAX_SEGMENTS merges */
	protected int generationNumber = 0;
	protected int mergeCount = 0;
	protected TIntArrayList segmentIds = new TIntArrayList(MAX_SEGMENTS);
	protected TIntArrayList segmentGenerations = new TIntArrayList(MAX_SEGMENTS);
	protected TIntArrayList segmentMaxDocids = new TIntArrayList(MAX_SEGMENTS);
	protected TIntHashSet deletedDocumentBlacklist = new TIntHashSet();
	
	/** Collection Statistics */
	protected int maxDocumentId = -1;
	protected int numDocuments = 0;
	protected long numTokens = 0;
	protected int numTerms = 0;
	protected long numPointers = 0;
	
	/** Memory index segment */
	protected MemoryIndex MI;
	
	/** virtual index */
	protected SegmentedInvertedIndex SII;
	protected SegmentedLexicon SL;
	protected SegmentedDocumentIndex SDoI;
	protected SegmentedDirectIndex SDiI; 
	
	class IncrementalIndexCollectionStatistics extends CollectionStatistics 
	{
		public IncrementalIndexCollectionStatistics()
		{
			super(numDocuments, numTerms, numTokens, numPointers);
		}

		@Override
		public double getAverageDocumentLength() {
			return (double)(numTokens)/(double)numDocuments;
		}

		@Override
		public int getNumberOfDocuments() {
			System.err.println("NumDocs="+numDocuments);
			return numDocuments;
		}

		@Override
		public long getNumberOfPointers() {
			return numPointers;
		}

		@Override
		public long getNumberOfTokens() {
			return numTokens;
		}

		@Override
		public int getNumberOfUniqueTerms() {
			return numTerms;
		}
		
	}
	
	protected IncrementalIndex() throws IOException  {
		this(ApplicationSetup.TERRIER_INDEX_PATH, ApplicationSetup.TERRIER_INDEX_PREFIX);
	}
	
	

	protected IncrementalIndex(String path, String prefix) throws IOException {
		super(3,3,3);
		this.path = path;
		this.prefix = prefix;
		propertiesFile = path + ApplicationSetup.FILE_SEPARATOR + prefix + ".properties";
		
		if (new File(propertiesFile).exists())
			loadProperties();
		
		CS = new IncrementalIndexCollectionStatistics();
		MI = new MemoryIndex(maxDocumentId+1);
		if (maxDocumentId > -1)
			segmentMaxDocids.add(maxDocumentId);
		SL = new SegmentedLexicon(segmentIds, path, prefix, MI, numTerms);
		((MemoryLexicon)MI.getLexicon()).setSegmentedLexicon(SL);
		SII = new SegmentedInvertedIndex(segmentIds, path, prefix, SL, MI);
		SDoI = new SegmentedDocumentIndex(path, prefix,segmentIds,segmentMaxDocids, MI);
		SDiI = new SegmentedDirectIndex(path, prefix,segmentIds,SDoI, MI);
		loadAllIndexSegments();
	}

	/* Do nothing constuctor. Child class will handle all initialisation. Arguments are meaningless.*/
	public IncrementalIndex(long arg0, long arg1, long arg2) {}
	
	public static String collateIntArray(final int[] ids)
	{
		StringBuilder rtr = new StringBuilder();
		final int l = ids.length;
		if (l == 0)
			return "";
		//final int ids[] = segmentIds.toNativeArray();
		for(int i=0;i<l;i++)
		{
			rtr.append(""+ids[i]);
			rtr.append(",");
		}
		rtr.setLength(rtr.length()-1);
		return rtr.toString();
	}
	
	protected int[] readIntArray(final String in)
	{
		if (in == null || in.length() == 0)
			return new int[0];
		final String[] segmentIds = in.split("\\s*,\\s*");
		final int l = segmentIds.length;
		final int[] segs = new int[l];
		for(int i=0;i<l;i++)
		{
			segs[i] = Integer.parseInt(segmentIds[i]);
		}
		return segs;
	}
	
	protected void saveProperties() throws IOException
	{
		indexProperties.setProperty("segment.ids", collateIntArray(segmentIds.toNativeArray()));
		indexProperties.setProperty("segment.generations", collateIntArray(segmentGenerations.toNativeArray()));
		indexProperties.setProperty("deleted.docids", collateIntArray(deletedDocumentBlacklist.toArray()));
		indexProperties.setProperty("segment.max.docids", collateIntArray(segmentMaxDocids.toNativeArray()));
		indexProperties.setProperty("use.direct.file", ""+USE_DIRECT_FILE);
		indexProperties.setProperty("index.serial.num", ""+indexSerialNum);
		
		//collection statistics
		indexProperties.setProperty("max.docid", ""+maxDocumentId);
		indexProperties.setProperty("num.documents", ""+numDocuments);
		indexProperties.setProperty("num.tokens", ""+numTokens);
		numTerms = SL.numberOfTerms;
		indexProperties.setProperty("num.terms", ""+numTerms);
		indexProperties.setProperty("num.pointers", ""+numPointers);
		
		//these properties are for info only, not for reading
		indexProperties.setProperty("num.segments", ""+numSegments);
		indexProperties.setProperty("max.segments", ""+MAX_SEGMENTS);
		
		//and write it to file
		OutputStream os = Files.writeFileStream(propertiesFile);
		indexProperties.store(os,"");
		os.close();
	}

	/** Should only be called from constructor */
	protected void loadProperties() throws IOException
	{
		//read the properties file in
		InputStream in = Files.openFileStream(propertiesFile);
		indexProperties.load(in);
		in.close();
		
		//load segment ids
		segmentIds.clear(); 
		segmentIds.add(readIntArray(indexProperties.getProperty("segment.ids", "")));
		//load segment generations
		segmentGenerations.clear();
		segmentGenerations.add(readIntArray(indexProperties.getProperty("segment.generations", "")));
		//load documentids pending deletion
		deletedDocumentBlacklist.clear();
		deletedDocumentBlacklist.addAll(readIntArray(indexProperties.getProperty("deleted.docids", "")));
		//load segment docid ranges
		segmentMaxDocids.clear();	
		segmentMaxDocids.add(readIntArray(indexProperties.getProperty("segment.max.docids","")));

	
		USE_DIRECT_FILE = Boolean.parseBoolean(indexProperties.getProperty("use.direct.file", "true"));
		indexSerialNum = Integer.parseInt(indexProperties.getProperty("index.serial.num", nextOverLargestSegmentId()));
		
		//now load the collection statistics
		maxDocumentId = Integer.parseInt(indexProperties.getProperty("max.docid", "0"));
		numDocuments = Integer.parseInt(indexProperties.getProperty("num.documents", "0"));
		numTokens = Long.parseLong(indexProperties.getProperty("num.tokens", "0"));
		numTerms = Integer.parseInt(indexProperties.getProperty("num.terms", "0"));
		numPointers= Long.parseLong(indexProperties.getProperty("num.pointers", "0"));
			
		//the following properties are not examined: num.segments, max.segments
	}
	
	protected String nextOverLargestSegmentId()
	{
		final int[] segIds = segmentIds.toNativeArray();
		final int l = segIds.length;
		if (l == 0)
			return "0";
		int max = segIds[0];
		for(int i=1;i<l;i++)
			max = segIds[i] > max ? segIds[i] : max;
		return ""+(max+1);
	}
	
	protected synchronized void loadAllIndexSegments()
	{
		SL.reOpenSegments();
		
		SII.reOpenSegments();
		SDoI.reOpenSegments();
		if (USE_DIRECT_FILE)
			SDiI.reOpenSegments();
	}
	
	protected void closeAllIndexSegments()
	{
		SL.close();
		SII.close();
		SDoI.close();
		if (USE_DIRECT_FILE)
			SDiI.close();
	}

	public void close() {
		try{
			saveProperties();
			closeAllIndexSegments();
		} catch (IOException ioe) {
			logger.warn("Error closing index",ioe);
		}
	}

	public DirectIndex getDirectIndex() {
		return USE_DIRECT_FILE ? this.SDiI : null;
	}

	@Override
	public DocumentIndex getDocumentIndex() {
		return SDoI;
	}

	@Override
	public InvertedIndex getInvertedIndex() {
		return SII;
	}

	@Override
	public Lexicon getLexicon() {
		return SL;
	}
	
	/** Index all documents in this collection */
	public void indexCollection(final Collection collection)
	{
		final boolean autoFlush = false;
		final long startCollection = System.currentTimeMillis();
		int docCount = 0;
		while(collection.nextDocument())
		{
			/* get the next document from the collection */
			/* get the docno and the document */
			String docno = collection.getDocid();
			Document doc = collection.getDocument();
			
			if (doc == null)
				continue;
 
			docCount++;
			/* add this document to the current memory index */
			final int[] rtr = MI.indexDocument(docno, doc);
			numTokens+= rtr[0];
			numPointers += rtr[1];
			numDocuments++; maxDocumentId++;
			
			/* force a flush of the memory index if it has reached 
			 * its maximum number of documents */
			if (MI.getNumberOfDocuments() >= MEMORY_SEGMENT_DOCUMENT_LIMIT)
				flush();
			
		}
		if (autoFlush && MI.getNumberOfDocuments() > 0)
		{
			//documents left to flush for this collection
			flush();
		}
		
		long endCollection = System.currentTimeMillis();
		System.err.println("Collection took "+((endCollection-startCollection)/1000.0)+"seconds to index "
				+"("+docCount+" documents)\n");
	}
	
	/**
	 * Delete a document and all its information from the index. 
	 * Document will remain in index until next merge of that segment.
	 */
	public void deleteDocument(final int docid)
	{
		deletedDocumentBlacklist.add(docid);
		numDocuments--;
		numTokens -= DoI.getDocumentLength(docid);
	}

	protected synchronized void newMemoryIndex()
	{
		this.MI = new MemoryIndex(maxDocumentId+1);
		((MemoryLexicon)this.MI.getLexicon()).setSegmentedLexicon(SL);
		this.SL.setMemoryIndex(this.MI);
		this.SII.setMemoryIndex(this.MI);
		this.SDoI.setMemoryIndex(this.MI);
		this.SDiI.setMemoryIndex(this.MI);
	}	
	
	/** Flushes the memory index segment to disk */
	public void flush()
	{
		//work out the place the new segment will be stored
		final int segmentId = indexSerialNum++;
		final String segmentPrefix = prefix + "_"+ segmentId;
		final String segmentPath = path + ApplicationSetup.FILE_SEPARATOR + segmentPrefix;
		System.err.println("Flushing memory index of "+MI.getNumberOfDocuments() + " documents to "+segmentPath);
				
		final MemoryIndex oldMI = this.MI;
		int lastDocInSegment;
		synchronized(this)
		{
			lastDocInSegment = maxDocumentId;
			newMemoryIndex();
		}
		
		try{
			BitOutputStream bos = new BitOutputStream(segmentPath + ApplicationSetup.IFSUFFIX);
			
			/* 1. write the inverted index postings to disk */ 
			FilePosition[] fps = ((MemoryInvertedIndex)oldMI.getInvertedIndex()).flush(bos);
			/* (no need to close bos, flush does this for us) */
			
			/* 2. write the lexicon to disk */
			LexiconOutputStream los = new LexiconOutputStream(path, segmentPrefix);
			((MemoryLexicon)oldMI.getLexicon()).flush(los, fps);
			/* 2a. create a lexicon index */
			LexiconBuilder.createLexiconIndex(
					new LexiconInputStream(path, segmentPrefix),
					SL.getNumberOfEntries(), Lexicon.lexiconEntryLength, path, segmentPrefix);
			
			/* 3. write the direct index */
			if (USE_DIRECT_FILE)
			{
				BitOutputStream dbos = new BitOutputStream(segmentPath + ApplicationSetup.DF_SUFFIX);
				fps = ((MemoryDirectIndex)oldMI.getDirectIndex()).flush(dbos);
			}
			else
			{
				fps = null;
			}
			/* 4. write the document index */
			DocumentIndexOutputStream dios = new DocumentIndexOutputStream(segmentPath + ApplicationSetup.DOC_INDEX_SUFFIX);
			((MemoryDocumentIndex)oldMI.getDocumentIndex()).flush(dios, fps);
			
			
			
			/* 5. open new segment */
			synchronized(this)
			{
				segmentIds.add(segmentId); numSegments++;
				segmentGenerations.add(generationNumber);
				segmentMaxDocids.add(lastDocInSegment);	
			}
			if (numSegments == MAX_SEGMENTS)
			{
				int[] segs = selectTwoSegmentsToMerge();
				loadAllIndexSegments();
				mergeSegments(segs, deletedDocumentBlacklist);
			}
			saveProperties();
			loadAllIndexSegments();
			
		} catch (IOException ioe) {
			System.err.println(ioe);
			ioe.printStackTrace();
			
		}
		
		
		
	}
	
	public void mergeAllSegments()
	{
		try{
			while(segmentIds.size() > 1)
			{
				int[] segs = selectTwoSegmentsToMerge();
				loadAllIndexSegments();
				mergeSegments(segs, deletedDocumentBlacklist);
			}			
		}catch (IOException ioe) {
			System.err.println(ioe);
			ioe.printStackTrace();			
		}
	}
	
	protected synchronized int[] selectTwoSegmentsToMerge()
	{
		int index1=-1; int index2=-1;
		int oldest = Integer.MAX_VALUE;
		synchronized(this)
		{
			final int[] generations = segmentGenerations.toNativeArray();
			for(int i=0;i<numSegments;i++)
			{
				if (generations[i] < oldest)
				{
					index1 = i;
					oldest =generations[i]; 
				}
			}
			index2 = index1+1;
			/*int nextOldest = Integer.MAX_VALUE;
			for(int i=0;i<numSegments;i++)
			{
				if (generations[i] <= nextOldest && i != index1)
				{
					index2 = i; nextOldest = generations[i];
				}
			}*/
			if (index1 == -1)
			{
				logger.error("index1 was -1 at index merge determination. Generations was "+ collateIntArray(generations));
			}
		}
		return new int[]{index1,index2};
	}
	
	protected void mergeSegments(final int[] twoSegIndexes, final TIntHashSet documentBlacklist) throws IOException
	{
		mergeSegments(twoSegIndexes[0], twoSegIndexes[1], documentBlacklist);
	}

	/** ONLY EVER MERGE TWO ADJACENT SEGMENTS */	
	protected void mergeSegments(final int segIndex1, final int segIndex2, final TIntHashSet documentBlacklist) throws IOException
	{
		if (segIndex1 +1 != segIndex2)
			logger.error("ERROR: Not merging adjacent segments ("+segIndex1+","+segIndex2+")");
		final int segmentId = indexSerialNum++;
		System.err.println("Merging index "+segmentIds.get(segIndex1)+ " & "+segmentIds.get(segIndex2) + " to "+segmentId);
		final String segmentPrefix = prefix + "_"+ segmentId;
		final String segmentPath = path + ApplicationSetup.FILE_SEPARATOR + segmentPrefix;
		LexiconOutputStream newLexFile = new LexiconOutputStream(segmentPath + ApplicationSetup.LEXICONSUFFIX);
		MultiLexiconInputStream allTerms = (MultiLexiconInputStream) SL.getLexiconInputStream2(segIndex1, segIndex2);
		InvertedIndex ii1 = SII.get(segIndex1);
		InvertedIndex ii2 = SII.get(segIndex2);
		
		BitOutputStream newInvIndexFile = new BitOutputStream(segmentPath + ApplicationSetup.IFSUFFIX);
		int lastIdWritten = -1; final boolean removeAnyDocuments = documentBlacklist.size() > 0;
		while(allTerms.readNextEntry() > 0)
		{
			//System.err.println("Processing term "+ allTerms.getTerm());
			boolean[] indices = allTerms.getSegmentsIndex();
			//FilePosition startOffset = new FilePosition(newInvIndexFile.getByteOffset(), newInvIndexFile.getBitOffset());
			FilePosition endOffset = null;
			if (indices[0])
			{
				//get postings
				//System.err.println("\tIndex0 offsets: ("+allTerms.getStartOffsetBytes(0)+","+allTerms.getStartOffsetBits(0)+") - ("+
				//		allTerms.getEndOffsetBytes(0)+","+allTerms.getEndOffsetBits(0)+")");
				final int[][] postings = 
					removeAnyDocuments
					? grepPostings(ii1.getDocuments(allTerms.getStartOffsetBytes(0),
							allTerms.getStartOffsetBits(0), 
							allTerms.getEndOffsetBytes(0),
							allTerms.getEndOffsetBits(0)), documentBlacklist) 
					: ii1.getDocuments(							
							allTerms.getStartOffsetBytes(0),
							allTerms.getStartOffsetBits(0), 
							allTerms.getEndOffsetBytes(0),
							allTerms.getEndOffsetBits(0));
				if (postings != null && postings[0].length > 0)
				{
					StructureMerger.writeNoFieldPostings(postings,postings[0][0]+1,newInvIndexFile);
					lastIdWritten = postings[0][postings[0].length-1];
					long endByte = newInvIndexFile.getByteOffset();
					byte endBit = newInvIndexFile.getBitOffset();
					endBit--;
	
					if (endBit < 0 && endByte > 0) {
						endBit = 7;
						endByte--;
					}
					endOffset = new FilePosition(endByte,endBit);
				}
				else if (! removeAnyDocuments)
				{
					logger.warn("Postings was null from first index, while no documents were grepped");
				}
			}
			
			if (indices[1])
			{
				//System.err.println("\tIndex1 offsets: ("+allTerms.getStartOffsetBytes(1)+","+allTerms.getStartOffsetBits(1)+") - ("+
				//		allTerms.getEndOffsetBytes(1)+","+allTerms.getEndOffsetBits(1)+")");
				final int[][] postings = 
					removeAnyDocuments
					? grepPostings(ii2.getDocuments(allTerms.getStartOffsetBytes(1),
							allTerms.getStartOffsetBits(1), 
							allTerms.getEndOffsetBytes(1),
							allTerms.getEndOffsetBits(1)), documentBlacklist) 
					: ii2.getDocuments(
							allTerms.getStartOffsetBytes(1),
							allTerms.getStartOffsetBits(1), 
							allTerms.getEndOffsetBytes(1),
							allTerms.getEndOffsetBits(1));
				//int[][] postings = ii2.getDocuments(allTerms.getTermId());
				//postings = grepPostings(postings, documentBlacklist);
				if (postings != null && postings[0].length > 0)
				{
					StructureMerger.writeNoFieldPostings(postings,
						(endOffset == null 
							? postings[0][0] + 1
							: postings[0][0] - lastIdWritten),
						newInvIndexFile);
					//System.err.println("EO="+endOffset+" Startid="+(endOffset == null
					  //	  ? postings[0][0] + 1
						//	: postings[0][0] - lastIdWritten));
					long endByte = newInvIndexFile.getByteOffset();
					byte endBit = newInvIndexFile.getBitOffset();
					endBit--;
	
					if (endBit < 0 && endByte > 0) {
						endBit = 7;
						endByte--;
					}
					//new offset is:
					endOffset = new FilePosition(endByte,endBit);
				}
				else if (! removeAnyDocuments)
				{
					logger.warn("Postings was null from 2nd index, while no documents were grepped");
				}
			}
			if (endOffset != null)
			{
				newLexFile.writeNextEntry(allTerms.getTerm(), allTerms.getTermId(), allTerms.getNt(), allTerms.getTF(), endOffset.Bytes,endOffset.Bits);
			}
		}
		newInvIndexFile.close();
		newLexFile.close();
		LexiconBuilder.createLexiconIndex(
				new LexiconInputStream(path, segmentPrefix),
				SL.getNumberOfEntries(), Lexicon.lexiconEntryLength, path, segmentPrefix);
		
		if (USE_DIRECT_FILE)
		{
			DocumentIndexInputStream dois1 = new DocumentIndexInputStream(path, prefix + "_"+ segmentIds.get(segIndex1));
			DocumentIndexInputStream dois2 = new DocumentIndexInputStream(path, prefix + "_"+ segmentIds.get(segIndex2));
			
			mergeDirectFiles(
				documentBlacklist,
				new DirectIndexInputStream(path, prefix + "_"+ segmentIds.get(segIndex1), dois1),
				new DirectIndexInputStream(path, prefix + "_"+ segmentIds.get(segIndex2), dois2),
				new BitOutputStream(segmentPath + ApplicationSetup.DF_SUFFIX),
				dois1,
				dois2,
				new DocumentIndexOutputStream(segmentPath + ApplicationSetup.DOC_INDEX_SUFFIX)
				);
		}
		else
		{
			mergeDocumentIndexFiles(
				documentBlacklist,
				new DocumentIndexInputStream(path, prefix + "_"+ segmentIds.get(segIndex1)),
				new DocumentIndexInputStream(path, prefix + "_"+ segmentIds.get(segIndex2)),
				new DocumentIndexOutputStream(segmentPath + ApplicationSetup.DOC_INDEX_SUFFIX));
		}
		int oldSegId1; int oldSegId2;
		synchronized(this)
		{
			mergeCount++;
			if (mergeCount % MAX_SEGMENTS == 0)
				generationNumber++;
	
			System.err.println("Size="+segmentIds.size()+","+segmentGenerations.size()+","+ segmentMaxDocids.size());	
			oldSegId1 = segmentIds.get(segIndex1);
			oldSegId2 = segmentIds.get(segIndex2);
			segmentIds.set(segIndex1, segmentId);
			segmentGenerations.set(segIndex1,generationNumber);
			segmentGenerations.remove(segIndex2);
			segmentIds.remove(segIndex2);
			segmentMaxDocids.remove(segIndex1);
			System.err.println("Size="+segmentIds.size()+","+segmentGenerations.size()+","+ segmentMaxDocids.size());
			
			/*	
			oldSegId1 = segmentIds.remove(segIndex1);  
			segmentGenerations.remove(segIndex1);
			oldSegId2 = segmentIds.remove(segIndex2 > segIndex1 ? segIndex2 -1 : segIndex2); 
			segmentGenerations.remove(segIndex2 > segIndex1 ? segIndex2 -1 : segIndex2);
			
			segmentIds.add(segmentId); numSegments--; segmentGenerations.add(generationNumber);

			segmentMaxDocids.remove(segIndex1); 
			segmentMaxDocids.add( segmentMaxDocids.remove( segIndex2 > segIndex1 ? segIndex2 -1 : segIndex2) );
			*/

			saveProperties();
			loadAllIndexSegments();
		}
		//remove old index files
		removeSegmentFiles(oldSegId1);
		removeSegmentFiles(oldSegId2);
		
	}
	
	/** Removes from the postings, any postings with document contained in docidBlacklist */
	protected static int[][] grepPostings(final int[][] inPostings, TIntHashSet docidBlacklist)
	{
		final int parts = inPostings.length;
		final int postingsCount = inPostings[0].length;
		TIntArrayList outPostings[] =  new TIntArrayList[parts];
		for(int i=0;i<parts;i++)
			outPostings[i] = new TIntArrayList(postingsCount);
		for(int j=0;j<postingsCount;j++)
			if (! docidBlacklist.contains(inPostings[0][j]))
				for(int i=0;i<parts;i++)
					outPostings[i].add(inPostings[i][j]);
		
		int rtr[][] = new int[parts][];
		for(int i=0;i<parts;i++)
			rtr[i] = outPostings[i].toNativeArray();
		return rtr;
	}
	
	
	/**
	 * Merges the two direct files and the corresponding document id files.
	 */
	protected void mergeDirectFiles(
			TIntHashSet documentBlackList,
			DirectIndexInputStream diis1, DirectIndexInputStream diis2, 
			BitOutputStream dios,
			DocumentIndexInputStream dois1, DocumentIndexInputStream dois2,
			DocumentIndexOutputStream doos) throws IOException{

		copyDirectFile(documentBlackList, diis1, dios, dois1, doos);
		copyDirectFile(documentBlackList, diis2, dios, dois2, doos);
		diis1.close();diis2.close();
		dios.close();
		dois1.close();dois2.close();
		doos.close();	
			
			
			/* cheaper way of doing DF transfers with cheaper decoding and re-encoding */
			/*currentEndOffset = dfInput1.getByteOffset();
			currentEndBitOffset = dfInput1.getBitOffset();
			while((currentEndOffset < endOffset) || ((currentEndOffset == endOffset) && (currentEndBitOffset < endBitOffset))) {
				dfOutput.writeGamma(dfInput1.readGamma());
				dfOutput.writeUnary(dfInput1.readUnary());
				dfOutput.writeBinary(binaryBits, dfInput1.readBinary(binaryBits));
				currentEndOffset = dfInput1.getByteOffset();
				currentEndBitOffset = dfInput1.getBitOffset();
			*/
			
			//while(diis1.g
		
					/*
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
			
		*/	
		//} catch(IOException ioe) {
		//	logger.error("IOException while merging df and docid files.", ioe);
		//}
	}

	protected void copyDirectFile(TIntHashSet documentBlackList,
			DirectIndexInputStream diis1,
			BitOutputStream dios,
			DocumentIndexInputStream dois1,
			DocumentIndexOutputStream doos) throws IOException
	{
		while(dois1.readNextEntry() > 0)
		{
			final int docid = dois1.getDocumentId();
			int doclen = dois1.getDocumentLength();
			final int olddoclen = doclen;
			/* three conditions: 
			 * (a) document to be kept, document has size: 
			 * (b) document to be kept, document has no size (also document previously deleted) : 
			 * (c) document to be deleted, document has size
			 * (d) document to be deleted, document has no size
			 * three options:
			 * (1) skip direct input if (doclen != -1 && doclen was not previously 0), write docindex only
			 * (2) write direct & docindex
			 */
							
			if (documentBlackList.contains(docid))
			{
				documentBlackList.remove(docid);
				doclen = -1;
			}				
			
			if (doclen > 0)
			{
				//System.err.println("Merging docid "+docid + " endoffset={"+dois1.getEndOffset()+","+dois1.getEndBitOffset()+"}");
				final int[][] terms = diis1.getNextTerms(dois1.getEndOffset(), dois1.getEndBitOffset() );
				final int TermsCount = terms[0].length;
		        final int[] termCodes = terms[0];
		        final int[] termFreqs = terms[1];

		        dios.writeGamma(termCodes[0] + 1);
		        dios.writeUnary(termFreqs[0]);
		        int prevTermCode = termCodes[0];
		        if (TermsCount > 1) {
		            for (int termNo = 1; termNo < TermsCount; termNo++) {
		            	dios.writeGamma(termCodes[termNo] - prevTermCode);
		            	dios.writeUnary(termFreqs[termNo]);
		                prevTermCode = termCodes[termNo];
		            }
		        }
			
				long currentEndOffset =  dios.getByteOffset();
				byte currentEndBitOffset = dios.getBitOffset();
				currentEndBitOffset--;
				if (currentEndBitOffset < 0)
				{
					currentEndOffset--;
					currentEndBitOffset =7;
				}
		        doos.addEntry(dois1.getDocumentNumber(),dois1.getDocumentId(), dois1.getDocumentLength(), currentEndOffset, currentEndBitOffset);
			}
			else
			{
				if (olddoclen != 0)
					diis1.getNextTerms(dois1.getEndOffset(), dois1.getEndBitOffset() );
				doos.addEntry(dois1.getDocumentNumber(), dois1.getDocumentId(), dois1.getDocumentLength(), (long)0,(byte)0);
			}
		}
		
	}
	
	/**
	 * Merges the two document id files. No consideration for direct index files are made.
	 */
	
	protected void mergeDocumentIndexFiles(
		TIntHashSet documentBlackList,
		DocumentIndexInputStream docidInput1, DocumentIndexInputStream docidInput2,
		DocumentIndexOutputStream docidOutput) throws IOException{
	
		while (docidInput1.readNextEntry() >= 0) {
			int doclen = docidInput1.getDocumentLength();
			if (doclen != -1 && documentBlackList.contains(docidInput1.getDocumentId()))
			{
				documentBlackList.remove(docidInput1.getDocumentId());
				doclen = -1;
			}
			docidOutput.addEntry(
				 docidInput1.getDocumentNumber(), 
				 docidInput1.getDocumentId(),
				 doclen,
				 (long)0,(byte)0);
		}
		
		
		while (docidInput2.readNextEntry() >= 0) {
			int doclen = docidInput2.getDocumentLength();
			if (doclen != -1 && documentBlackList.contains(docidInput2.getDocumentId()))
			{
				documentBlackList.remove(docidInput2.getDocumentId());
				doclen = -1;
			}
				
			docidOutput.addEntry(
				docidInput2.getDocumentNumber(),
				docidInput2.getDocumentId(),
				docidInput2.getDocumentLength(),
				(long)0,(byte)0);
		}

		docidInput1.close();
		docidInput2.close();
		docidOutput.close();
	}
	
	protected void removeSegmentFiles(int segmentId)
	{
		final File indexDirectory = new File(path);
		final File[] contents = indexDirectory.listFiles();
		final int fileCount = contents.length;
		final String thisSegmentPrefix = prefix+'_'+segmentId;
		for(int i=0;i<fileCount;i++)
		{
			if (contents[i].getName().startsWith(thisSegmentPrefix))
				contents[i].delete();
		}
	}
	
	public static void main(String args[])
	{
		try{
			IncrementalIndex ii = new IncrementalIndex();
			ii.indexCollection(new TRECCollection());
			//ii.mergeAllSegments();
			new TRECQuerying(ii).processQueries();
			ii.flush();
			new TRECQuerying(ii).processQueries();
			
		} catch (Exception e) {
			System.err.println(e);
			e.printStackTrace();
		}
	}
	

}

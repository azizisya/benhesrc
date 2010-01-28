package uk.ac.gla.terrier.structures.incrementalindex;

import gnu.trove.TIntObjectIterator;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.io.RandomAccessFile;

import uk.ac.gla.terrier.compression.BitOutputStream;
import uk.ac.gla.terrier.indexing.Collection;
import uk.ac.gla.terrier.indexing.Document;
import uk.ac.gla.terrier.indexing.TRECCollection;
import uk.ac.gla.terrier.structures.DirectIndex;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.FilePosition;
import uk.ac.gla.terrier.structures.InvertedIndex;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.structures.incrementalindex.globaldocumentindex.GlobalDocumentIndex;
import uk.ac.gla.terrier.structures.incrementalindex.globallexicon.GlobalLexicon;
import uk.ac.gla.terrier.structures.incrementalindex.globallexicon.GlobalLexiconInputStream;
import uk.ac.gla.terrier.structures.incrementalindex.globallexicon.MemoryLexiconRecord;
import uk.ac.gla.terrier.structures.incrementalindex.memoryindex.MemoryDirectIndex;
import uk.ac.gla.terrier.structures.incrementalindex.memoryindex.MemoryDocumentIndex;
import uk.ac.gla.terrier.structures.incrementalindex.memoryindex.MemoryIndex;
import uk.ac.gla.terrier.structures.incrementalindex.memoryindex.MemoryInvertedIndex;
import uk.ac.gla.terrier.structures.incrementalindex.memoryindex.MemoryLexicon;
import uk.ac.gla.terrier.structures.trees.bplustree.BplusDiskLeafNode;
import uk.ac.gla.terrier.structures.trees.bplustree.BplusDiskNode;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Rounding;
import uk.ac.gla.terrier.utility.TerrierTimer;
import uk.ac.gla.terrier.structures.merging.StructureMerger;

/** @author John Kane &amp; Craig Macdonald */
public class IncrementalIndex extends MemoryIndex {
	
	protected MemoryIndex MI;
	
	protected GlobalLexicon L;
	
	protected ProxyLexicon PL;
	
	protected SegmentedInvertedIndex II;
	
	protected SegmentedDirectIndex DiI;
	
	protected GlobalDocumentIndex DoI;
	
	protected ProxyDocumentIndex PDi;
	
	protected long numberOfTokens = 0;
	
	protected IncrementalIndexProperties properties;
	
	protected int currentNumberOfSegments = 0;
	protected int DocumentLimit = 1000;
	
	protected RandomAccessFile[] deletedDocFiles;
	
	BitOutputStream direct_index;

	
	public IncrementalIndex()
	{
		this(ApplicationSetup.TERRIER_INDEX_PATH, ApplicationSetup.TERRIER_INDEX_PREFIX );
	}
	
	public IncrementalIndex(String path, String prefix)
	{
		this.path = path; this.prefix = prefix;
		properties = new IncrementalIndexProperties(path, prefix);
		
		
		//Open Deleted Document files
		try{
		deletedDocFiles = new RandomAccessFile[properties.Max_number_Of_Segments];
		for(int i = 0; i < currentNumberOfSegments; i++)
		{
			deletedDocFiles[i] = new RandomAccessFile(ApplicationSetup.TERRIER_INDEX_PATH+"deletedDocs"+i, "rw");
		}
		
		File tempfile = new File(path + ApplicationSetup.FILE_SEPARATOR + prefix + "_"+0+ApplicationSetup.DF_SUFFIX);
		if(!tempfile.exists())
			tempfile.createNewFile();
		
		direct_index = new BitOutputStream(path + ApplicationSetup.FILE_SEPARATOR + prefix + "_"+0+ApplicationSetup.DF_SUFFIX);
		}catch(Exception e){e.printStackTrace();}
		//end
		
	
		L = new GlobalLexicon(properties.Max_number_Of_Segments, path, prefix);
		DoI = new GlobalDocumentIndex(path, prefix);
		
		
		II = new SegmentedInvertedIndex(properties,L);
		DiI = new SegmentedDirectIndex(properties,DoI);
		
		MI = new MemoryIndex(properties.nextDocumentId);
		
		PL = new ProxyLexicon(L,this);
		PDi = new ProxyDocumentIndex(DoI, this);
		setMemoryIndex(MI);
		DocumentLimit = properties.Max_number_Of_Document_Memory_Segment;
		
		
	}
	
	protected void segmentsChange()
	{
		II.reOpenSegments();
		DiI.reOpenSegments();
	}
	
	protected void setMemoryIndex(MemoryIndex mi)
	{
		II.setMemoryIndex(mi);
		DiI.setMemoryIndex(mi);

	}

	///Do-ing methods
	
	/** Close this index. <b>Almost imperative for this index to be safely written to disk at present</b> */
	public void close()
	{
		//check we've written all newly indexed documents to disk
		if (MI.getNumberOfDocuments() > 0)
			flush();
		
		//updates the properties file before closing
		if(L.getFirstLeaf() != null)
			properties.GlobalLexiconFirstLeaf =  ((BplusDiskLeafNode)L.getFirstLeaf()).getFilePosition();
		if(DoI.getFirstLeaf() != null)
			properties.GlobalDocIndexFirstLeaf = ((BplusDiskLeafNode)DoI.getFirstLeaf()).getFilePosition();
		properties.GlobalDocIndexRoot = ((BplusDiskNode)DoI.getRoot()).getFilePosition();
		properties.GlobalLexiconRoot = ((BplusDiskNode)L.getRoot()).getFilePosition();
		properties.globalLexiconHeight = L.getHeight();
		properties.globalLexiconNumberOfValues = L.size();
		properties.globalDocIndexHeight = DoI.getHeight();
		properties.globalDocIndNumberOfValues = DoI.size();
		properties.currentNumberOfSegments =currentNumberOfSegments;
		properties.close();
		
		L.close();
		II.close();
		DiI.close();
		DoI.close();
		
		try{
		direct_index.close();
		}catch(Exception e){e.printStackTrace();}

	}
	
	/** Index all documents in this collection */
	public void indexCollection(final Collection collection)
	{
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
			MI.indexDocument(docno, doc);
			
			/* force a flush of the memory index if it has reached 
			 * its maximum number of documents */
			if (MI.getNumberOfDocuments() >= DocumentLimit)
				flush();
			
		}
		if (MI.getNumberOfDocuments() > 0)
		{
			//documents left to flush for this collection
			flush();
		}
		
		long endCollection = System.currentTimeMillis();
		System.err.println("Collection took "+((endCollection-startCollection)/1000.0)+"seconds to index "
				+"("+docCount+" documents)\n");
	}
	
	
	/**
	 * Delete a document and all its information from the index. Updating the statistics so that it is as if it were never indexed.
	 * if its in the memory section then delete it there. Otherwise its in one of the segments.
	 * 
	 * <p>First the Global document index is queried. The start and end position in the DirectIndex of the relevant segment is noted. 
	 * The docno and the start and end offsets are recorded in a deleted document file associated with the segment. The term list
	 * will be stripped out of the Direct Index when that segment is next merged. The doc is then deleted from the global document index.
	 *
	 * <p>The Global Lexicon is then altered. For every term occuring in the document the Global lexicon should update its entry. 
	 * Entries that are reduced to zero occurences should be removed from the tree. The InvertedIndex much like the direct index can be
	 * corrected during a merge. Term's whose number of occurences don't match the length of their posting list should be flagged as 
	 * having deleted documents during a merge. The posting list is then compared with the black list of deleted docs with named docs
	 * being dropped.
	 */
	public void deleteDocument(final int docno)
	{

		
		//If its in the memory section then delete it there.
		//Probaby never used. updates unlikely to occur in the time a memindex exists.
		try{
		if(MI.getDocumentIndex().seek(docno))
		{
		//MI.deleteDocument(docno);
			
		}
		else //It is in one of the segments
		{
			//Check the Global Document Index
			if(DoI.seek(docno))
			{
				//Note the part of the file that are to 
				FilePosition startOffset = DoI.getDirectIndexStartOffset(docno);
				FilePosition endOffset   = DoI.getDirectIndexEndOffset(docno); 
				int segment = DoI.getSegmentNumber(docno);
				
				//Note the deleted documents term list to be ignored at next merge
				deletedDocFiles[segment].writeBytes(docno+" "+startOffset+" "+endOffset+"\n");
				
				//Read the term list
				int[][] terms = DiI.getTerms(docno);
				
				//for each term add a negative entry into the negative lexicon
				MemoryLexicon negLex = (MemoryLexicon)MI.getNegativeLexicon();
				
				for(int i = 0; i < terms[0].length; i++) //the -1 is the minus a single document
					negLex.incrementNegativeEntry(terms[0][i], terms[1][i], -1);
				
				//remove the entry in the DocumentIndex
				DoI.deleteDocEntry(docno);
				
			}
		}
		
		}catch(Exception e){e.printStackTrace();}
	}

	//inherited:
	//public void indexCollections(Collection[] collections)
	
	
	
	/**
	 * Writes the in-memory datastructures, 
	 * MemoryDirectIndex, MemoryLexicon etc 
	 * to disk as a segment, performing any relevant
	 * merges. Should it be querable while this is happening?
	 */
	
	
	public void flush()
	{
		long startTime = System.currentTimeMillis();
		int writingDocuments =0;
		Runtime R = Runtime.getRuntime();
		int newSegmentId = properties.getNextSegmentId();
		try{
		
			
			//work out the place the new segment will be stored
			String segmentPath = path + ApplicationSetup.FILE_SEPARATOR + prefix + "_"+ newSegmentId;
			
			//1. firstly, write the new direct index.
			//TODO: I suspect the termids are incorrect for this DF, but i'm not sure.
			//BitOutputStream new_direct_index = 
			//	new BitOutputStream(segmentPath + ApplicationSetup.DF_SUFFIX);
			FilePosition[] positions = ((MemoryDirectIndex)MI.getDirectIndex()).flush(direct_index);
			writingDocuments = positions.length;
			direct_index.flush();
			//new_direct_index.close();
			
			long gditime =  System.currentTimeMillis();
			System.err.println("Time taken to flush Direct index ("+writingDocuments+"): "+((gditime-startTime)/1000.0d));
			
			//2. add those file positions from the direct file and flush the in-memory document index to the
			//global index
			DoI.merge(currentNumberOfSegments,(MemoryDocumentIndex)MI.getDocumentIndex(),positions);
			long gdoitime = System.currentTimeMillis();
			System.err.println("Time taken to flush document index: "+((gdoitime-gditime)/1000.0d));
	
			
			//3. Flush the Inverted Index to disk while updating the global lexicon
			//for each term in the lexicon,
			
			//get the needed data structures from the current memory index
			MemoryLexicon memLex = (MemoryLexicon)MI.getLexicon();
			MemoryInvertedIndex mii = (MemoryInvertedIndex)MI.getInvertedIndex();
			TIntObjectIterator iter = memLex.iterator_TIntObject();
			MemoryLexiconRecord term;
		
		
			BitOutputStream InvIndexFile = new BitOutputStream(
				segmentPath + ApplicationSetup.IFSUFFIX);
			
			FilePosition startOffset = new FilePosition(0,(byte)0);
			FilePosition endOffset;
			long startIncr, endInc;
			long averInc = 0;
			long startLexFlush = System.currentTimeMillis();
			
			TerrierTimer timer = new TerrierTimer();
			int lexSize = (int)(memLex.getNumberOfLexiconEntries());
			timer.setTotalNumber((double)memLex.getNumberOfLexiconEntries());
			timer.start();
			
			//for each term in the memory index's lexicon
			int termCount =0;
			while(iter.hasNext())
			{
					
				iter.advance();
				term = (MemoryLexiconRecord)iter.value();
				
				//write its posting list to disk 
				MemoryInvertedIndex.postingList postings = mii.getPostings(term.termid);
				
				endOffset = postings.flush(InvIndexFile);
				
				//then add its term to the global lexicon,
				//using the start and end offsets
				startIncr = System.currentTimeMillis();
				L.incrementEntry(term.term,term.TF,term.Nt,currentNumberOfSegments,startOffset,
						endOffset);
				endInc = System.currentTimeMillis();
				averInc+=endInc-startIncr;
				
				startOffset = new FilePosition(endOffset);
				startOffset.Bits +=  1;
				if (startOffset.Bits == 8) {
					startOffset.Bytes = startOffset.Bytes + 1;
					startOffset.Bits = 0;
					
				}
				termCount++;
				timer.setRemainingTime(termCount);
				if (termCount % 1000 == 0 && false)
				{
					timer.setBreakPoint();
					System.out.println(timer.getPercentage() + "% done -"+timer.getMinutes() + ":"+timer.getSeconds() );
					//displayMemoryUsage(R);
					//System.out.println(((100*termCount)/lexSize)+"%");
				}
			}
			
			System.out.println("Average increment time was: "+(averInc/(double)(L.getNumberOfLexiconEntries())));
			long endLexFlush = System.currentTimeMillis();
			System.out.println("Average per term time was: "+((endLexFlush- startLexFlush)/(double)(L.getNumberOfLexiconEntries())));
			System.out.println("Processed "+L.getNumberOfLexiconEntries()+" terms.");
			
			InvIndexFile.close();
		}catch(IOException e){
			System.err.println("Error while flushing lexicon+InvIndex");
			e.printStackTrace();
		}
				
		//*** end of invindex flush + global lexicon update
		System.out.println("Finished merging the Lexicon into global.");
		
		//update the collection statistics of the disk index segments
		properties.numberOfPointers += MI.getNumberOfPointers();
		properties.numberOfTokens += MI.getNumberOfTokens();

		//Create new MemoryIndex but keep the docid counter constant between them		
		properties.nextDocumentId = ((MemoryDocumentIndex)MI.getDocumentIndex()).docidCounter;

		properties.addSegment(newSegmentId);
		
		MI.close();
		//update the index properties and statistics on disk
		properties.save();
		//TODO: at this stage I would like to see the inner nodes in the bplus trees committed to disk
	
		MI = new MemoryIndex(properties.nextDocumentId);
		//put new memory index into use
		setMemoryIndex(MI);
		//and add the new segments
		segmentsChange();
			
		long fin = System.currentTimeMillis();
		System.err.println("Time taken to flush a segment of "+writingDocuments+" documents : "+((fin-startTime)/1000.0d));
		
		currentNumberOfSegments++;
		if (currentNumberOfSegments == properties.Max_number_Of_Segments)
		{
			System.err.println("Reached maximum number of segments, merging...");
			mergeSegments(0,1);	
		}
		
		//DirectIndex tempDi = new DirectIndex(DoI,path + ApplicationSetup.FILE_SEPARATOR + prefix + "_"+0+ApplicationSetup.DF_SUFFIX);
		//System.out.println("Direct index:");
		//tempDi.print();
	}
	
	public void mergeSegments(int segIndex1, int segIndex2)
	{
		
		System.out.println("\n **** Start of Merge of two segments: "+segIndex1+" & "+segIndex2+"" +
				"\n\nThe current state of the GLobal Lexicon:");
		//L.print();
		//System.out.println("\n\n");
		
		
		final long startTime = System.currentTimeMillis();
		try{
			
			//System.err.println("Merging segments at index "+segIndex1+" and "+segIndex2);
			
			int newSegmentId = properties.getNextSegmentId();
			String segmentPath = path + ApplicationSetup.FILE_SEPARATOR + prefix + "_"+ newSegmentId;
			GlobalLexiconInputStream gli = (GlobalLexiconInputStream)L.getLexiconInputStream();
			BitOutputStream newInvIndexFile = new BitOutputStream(segmentPath + ApplicationSetup.IFSUFFIX);
			//BitOutputStream newDirIndexFile = new BitOutputStream(segmentPath + ApplicationSetup.DF_SUFFIX);
			int lastIdWritten = -1;
			while(gli.readNextEntry() > -1)
			{
				FilePosition startOffset = new FilePosition(newInvIndexFile.getByteOffset(), newInvIndexFile.getBitOffset());
				FilePosition endOffset = null;
				//System.err.println("Term "+gli.getTerm() + " in lex node #"+gli.nodeCount);
				if (gli.getSegmentIdsHash().contains(segIndex1))
				{
					//if (gli.getStartOffset(segIndex1).equals(gli.getEndOffset(segIndex1)))
					//	System.err.println("WARNING: Offsets for term "+gli.getTerm()+" are identical"+gli.getStartOffset(segIndex1).toString());
					//System.err.println("INFO: Reading inv index1 for term "+gli.getTerm()+" "+gli.getStartOffset(segIndex1)+" : "+gli.getEndOffset(segIndex1));
					int[][] postings = II.getDocuments(gli.getTermId(), segIndex1, gli.getStartOffset(segIndex1), gli.getEndOffset(segIndex1));
					StructureMerger.writeNoFieldPostings(postings,postings[0][0]+1,newInvIndexFile);
					lastIdWritten = postings[0][postings[0].length-1];
					long endByte = newInvIndexFile.getByteOffset();
					byte endBit = newInvIndexFile.getBitOffset();
					endBit--;

					if (endBit < 0 && endByte > 0) {
						endBit = 7;
						endByte--;
					}
					//new offset is:
					/*store in lexicon: */ 
					endOffset = new FilePosition(endByte,endBit);
					gli.removeSegment(segIndex1);
				}
				if (gli.getSegmentIdsHash().contains(segIndex2))
				{
					//System.err.println("INFO: Reading inv index2 for term "+gli.getTerm()+" "+gli.getStartOffset(segIndex2)+" : "+gli.getEndOffset(segIndex2));	
					int[][] postings = II.getDocuments(gli.getTermId(), segIndex2, gli.getStartOffset(segIndex2), gli.getEndOffset(segIndex2));
					
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
					gli.removeSegment(segIndex2);
				}
				//update lexicon with new startOffset and endOffset for this term in the new merged segment
				if (endOffset != null)
				{	
					//System.err.println("New offset for "+gli.getTerm()+" is "+startOffset+ " : "+ endOffset);
					gli.addSegment(segIndex1, startOffset, endOffset);
					//L.print();
					//updates to gli will be written on next call to readNextEntry() or close()
					//System.err.println(gli.recordToString());
				}
			}
			gli.close();
			newInvIndexFile.close();
			//newDirIndexFile.close();
			final int[] segmentIds = properties.getCurrentSegmentIds();
			final int segmentId1 = segmentIds[segIndex1];
			final int segmentId2 = segmentIds[segIndex2];
			properties.mergedCurrentSegments(segmentId1, segmentId2, newSegmentId);
			properties.save();
			currentNumberOfSegments--;	
			segmentsChange();
			System.err.println("Removing old inverted files");
			new File(this.path + ApplicationSetup.FILE_SEPARATOR + this.prefix + '_' +segmentId1 + ApplicationSetup.IFSUFFIX).delete();
			//new File(this.path + ApplicationSetup.FILE_SEPARATOR + this.prefix + '_' +segmentId1 + ApplicationSetup.DF_SUFFIX).delete();
			new File(this.path + ApplicationSetup.FILE_SEPARATOR + this.prefix + '_' +segmentId2 + ApplicationSetup.IFSUFFIX).delete();
			//new File(this.path + ApplicationSetup.FILE_SEPARATOR + this.prefix + '_' +segmentId2 + ApplicationSetup.DF_SUFFIX).delete();
			final long endTime = System.currentTimeMillis();
			System.err.println("Merge done in "+((endTime-startTime)/1000.0)+" seconds");
		} catch (Exception e) {
			System.err.println("Error merging two indices: "+e);
			e.printStackTrace();
		}
		
		
		
		//System.out.println("Current state of the global lexicon:");
		//L.print();
		//System.out.println("Inverted Indexes");
		//II.print();

		
	}
	
	
	//Getters
	public DirectIndex getDirectIndex() {
		return DiI;
	}

	public DocumentIndex getDocumentIndex() {
		return PDi;//DoI;//PDi;
	}

	public InvertedIndex getInvertedIndex() {
		return II;
	}

	public Lexicon getLexicon() {
		return PL;
	}
	
	public MemoryIndex getMemIndex()
	{
		return MI;
	}
	
	/** Returns the number of indexed documents in all segments */
	public int getNumberOfDocuments()
	{
		return DoI.getNumberOfDocuments() + MI.getNumberOfDocuments();
	}
	
	/** Returns the number of pointers in all indexed segments. */
	public long getNumberOfPointers()
	{
		return properties.numberOfPointers + MI.getNumberOfPointers();
	}
	
	/** Returns the number of terms in the global lexicon (not the Lexicon of the
	  * MemoryIndex) */
	public int getNumberOfTerms()
	{	
		/* TODO: to be correct, this should be size of the union of
		 * the global lexicon and the memory lexicon, but this isnt
		 * really feasible, so we return the number of entries in the
		 * global lexicon */
		//TODO: remove this cast once Lexicon method has been updated to return type int.
		return (int)L.getNumberOfLexiconEntries();
	}
	
	/** Returns the number of tokens in all indexed segments */
	public long getNumberOfTokens()
	{
		return properties.numberOfTokens + MI.getNumberOfTokens();
	}
	
	
	public static void main(String[] args)
	{
		IncrementalIndex IncInd = new IncrementalIndex();
		IncInd.indexCollection(new TRECCollection());
		IncInd.close();
	}
	
	public static void displayMemoryUsage(Runtime r)
	{
		System.err.println("free: "+ (r.freeMemory() /1024) + "kb; total: "+(r.totalMemory()/1024)
			+"kb; max: "+(r.maxMemory()/1024)+"kb; "+
			Rounding.toString((100*r.freeMemory() / r.totalMemory()),1)+"% free; "+
			Rounding.toString((100*r.totalMemory() / r.maxMemory()),1)+"% allocated; "
		);
	}
}

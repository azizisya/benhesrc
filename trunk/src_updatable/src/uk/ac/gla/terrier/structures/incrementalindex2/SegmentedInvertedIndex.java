package uk.ac.gla.terrier.structures.incrementalindex2;

import gnu.trove.TIntArrayList;
import uk.ac.gla.terrier.structures.FilePosition;
import uk.ac.gla.terrier.structures.InvertedIndex;
import uk.ac.gla.terrier.structures.LexiconEntry;
import uk.ac.gla.terrier.structures.incrementalindex2.memoryindex.MemoryIndex;
import uk.ac.gla.terrier.structures.incrementalindex2.memoryindex.MemoryInvertedIndex;
import uk.ac.gla.terrier.utility.ApplicationSetup;

/** @author John Kane &amp; Craig Macdonald */
public class SegmentedInvertedIndex extends InvertedIndex {
	/** The maximum possible segments */
	protected static final int MaxNumberOfSegments = IncrementalIndex.MAX_SEGMENTS;
	/** the number of segments currently in the index */
	protected int numberOfSegments;
	
	protected TIntArrayList segmentIds;
	
	protected String path;
	protected String prefix;
	protected InvertedIndex[] InvIndexes;
	
	
	protected SegmentedLexicon SegLex;
	protected MemoryIndex MI;
	protected MemoryInvertedIndex MII;
	
	SegmentedInvertedIndex(TIntArrayList segmentIds, String path, String prefix, SegmentedLexicon sl, MemoryIndex MI)
	{
		this.segmentIds = segmentIds;
		this.path = path; this.prefix = prefix;
		this.SegLex = sl;
		setMemoryIndex(MI);
		//reOpenSegments();
	}
	
	public InvertedIndex get(int i)
	{
		return InvIndexes[i];
	}
	
	public void setMemoryIndex(MemoryIndex mi)
	{
		MI = mi;
		MII = (MemoryInvertedIndex)MI.getInvertedIndex();
	}
	
	public void reOpenSegments()
	{
		if (InvIndexes != null)
		{
			for(int i=0;i<InvIndexes.length;i++)
				if (InvIndexes[i] != null)
				{
					InvIndexes[i].close();
					InvIndexes[i] = null;
				}
		}
		
		final int[] segmentIds = this.segmentIds.toNativeArray();
		System.err.println("SegmentedInvertedIndex attempting to open "+segmentIds.length+" segments");
		numberOfSegments = segmentIds.length;
		InvIndexes = new InvertedIndex[numberOfSegments];
		for(int i=0;i<numberOfSegments;i++)
		{
			if (segmentIds[i] == -1)
				continue;
			InvIndexes[i] = new InvertedIndex(
				SegLex.get(i),
				path + ApplicationSetup.FILE_SEPARATOR + 
				prefix + "_" + segmentIds[i]+
				ApplicationSetup.IFSUFFIX);
		}
	}
	
	public int[][] getDocuments(LexiconEntry le)
	{
		SegmentedLexiconEntry sle = (SegmentedLexiconEntry)le;
		final int[] segIndexes = sle.getSegmentIndexes();
		final int segCount = segIndexes.length;
		if (segCount ==0)
		{
			//System.err.println("SegmentedInvertedIndex Returning null, cos Term "+ le.term + "("+le.termId+") not in any segments");
			//return null;
			return MII.getDocuments(le.termId);
		}		

		/* first segment */
		FilePosition start = sle.getStartOffset(0);
		FilePosition end = sle.getEndOffset(0);
		System.err.println(start + " -> " + end);
		int[][] segPostings = InvIndexes[segIndexes[0]].getDocuments(start.Bytes,start.Bits, end.Bytes, end.Bits);
		final int postingsParts = segPostings.length;
		TIntArrayList[] postings = new TIntArrayList[postingsParts];
		System.err.println("1st segment had "+ segPostings[0].length + " postings");
		for(int j=0;j<postingsParts;j++)
		{
			postings[j] = new TIntArrayList(sle.n_t);
			postings[j].add(segPostings[j]);
		}
		
		/* subsequent disk segments */
		for(int i=1;i<segCount;i++)
		{
			start = sle.getStartOffset(i);
			end = sle.getEndOffset(i);
			segPostings = InvIndexes[segIndexes[i]].getDocuments(start.Bytes,start.Bits, end.Bytes, end.Bits);
			for(int j=0;j<postingsParts;j++)
			{
				postings[j].add(segPostings[j]);
			}
		}
		
		/* memory segment */
		segPostings = MII.getDocuments(le.termId);
		if (segPostings != null)
		{
			for(int j=0;j<postingsParts;j++)
			{
				postings[j].add(segPostings[j]);
			}
		}
		
		/* collate into int[][] array */
		final int[][] rtr = new int[postingsParts][];
		for(int j=0;j<postingsParts;j++)
			rtr[j] = postings[j].toNativeArray();
	
		System.err.println("Returned "+ rtr[0].length+" postings");	
		return rtr;
	}
	
	public int[][] getDocuments(int termid)
	{
		LexiconEntry le = SegLex.getLexiconEntry(termid); 
		return getDocuments(le);		
	}

	
//	public int[][] getDocuments(int termid)
//	{
//		boolean gFound = lex.findTerm(termid);
//		MemoryLexicon mLex = (MemoryLexicon)MI.getLexicon();
//		
//		boolean mFound;
//		if(gFound)
//			mFound = mLex.findTerm(lex.getTerm());
//		else
//			mFound = mLex.findTerm(termid);
//		
//		//the number of documents trhe term occurs in on the segments
//		//+ the number of documents that it occurs in the memory index
//		int [][] postings = new int[2][lex.getNt()
//		                               		+(mFound ? mLex.getNt() : 0)];
//		
//		FilePosition[][] segmentPositions = lex.getSegments();
//		
//		int marker = 0;
//		if(gFound)
//		{
//		//For each of the possible segments
//		for (int i = 0; i < segmentPositions.length; i++)
//		{
//			//if the segment actually exists
//			if(segmentPositions[i][0] != null)
//			{
//					//retreive the docs for the term
//					
//					//TODO Change the getDocuments method so that it copies
//					//into a given array rather than returning an array then copying over
//				int[][] tempArray =	InvIndexes[i].getDocuments(
//							segmentPositions[i][0].Bytes,
//							segmentPositions[i][0].Bits,
//							segmentPositions[i][1].Bytes,
//							segmentPositions[i][1].Bits
//							);
//				
//				/*
//				for(int p = 0; p < tempArray.length; p++)
//				{
//					System.out.println("");
//					for(int q = 0; q < tempArray[p].length;q++)
//						System.out.print(tempArray[p][q]+" ");
//				}
//				*/
//				
//				//add them to the array of found docs
//				//TODO: check for deleted documents
//				for (int j = 0; j < tempArray[0].length; j++)
//				{
//					postings[0][marker] = tempArray[0][j];
//					postings[1][marker] = tempArray[1][j];
//					marker++;
//					
//				}
//			}
//			
//		}
//		}//if gFound
//		
//		//for the memory inverted index
//		if(mFound)
//		{
//			int MemoryTermid = Mindex.getLexicon().getTermId();
//			int[][] MemoryPostings = Mindex.getInvertedIndex().getDocuments(MemoryTermid);
//		
//			for (int j = 0; j < MemoryPostings[0].length; j++)
//			{
//				postings[0][marker] = MemoryPostings[0][j];
//				postings[1][marker] = MemoryPostings[1][j];
//				marker++;
//			
//			}
//		}//if mFound
//		
//			
//		return postings;
//	}
//	
//	public int[][] getDocuments(int termid, int segmentIndex)
//	{
//		if (! lex.findTerm(termid))
//			return null;
//		FilePosition[][] segmentPositions = lex.getSegments();
//		return InvIndexes[segmentIndex].getDocuments(segmentPositions[segmentIndex][0].Bytes, segmentPositions[segmentIndex][0].Bits,
//			segmentPositions[segmentIndex][1].Bytes, segmentPositions[segmentIndex][1].Bits);
//	}
//	
//	public int[][] getDocuments(int termid, int segmentIndex, FilePosition startOffset, FilePosition endOffset)
//	{
//		return InvIndexes[segmentIndex].getDocuments(startOffset.Bytes, startOffset.Bits, endOffset.Bytes, endOffset.Bits);
//	}
//
//
//	public int[][] getDocuments(int termid, int startDocid, int endDocid) {
//		System.err.println("Used in block version(?), but currently unimplemented.");
//		return null;
//	}
//	
//	public void setMemoryIndex(MemoryIndex mi)
//	{
//		Mindex = mi;
//	}

	/** Close this segmented proxy inverted index. Closes all child segment inverted index segments */
	public void close() {
		for(int i=0;i<numberOfSegments;i++)
		{
			InvIndexes[i].close();
		}
	}
	
	
//	public void print() {
//		//for(int p = 0; p < InvIndexes.length; p++)
//		{
//
//		for (int i = 0; i < lex.getNumberOfLexiconEntries(); i++) {
//			lex.findTerm(i);
//			System.out.println(lex.getTerm()+" :");
//			FilePosition[][] segmentPositions = lex.getSegments();
//			
//			
//			int[][] documents = InvIndexes[0].getDocuments(
//					segmentPositions[0][0].Bytes,
//					segmentPositions[0][0].Bits,
//					segmentPositions[0][1].Bytes,
//					segmentPositions[0][1].Bits
//					);
//			
//				for (int j = 0; j < documents[0].length; j++) {
//					System.out.print("(" + documents[0][j] + ", " 
//										 + documents[1][j] + ") ");
//				}
//				System.out.println();
//			
//		}
//		}
//	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}

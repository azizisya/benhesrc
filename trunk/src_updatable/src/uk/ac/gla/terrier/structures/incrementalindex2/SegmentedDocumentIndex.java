package uk.ac.gla.terrier.structures.incrementalindex2;

import java.io.IOException;

import gnu.trove.TIntArrayList;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.FilePosition;
import uk.ac.gla.terrier.structures.incrementalindex2.memoryindex.MemoryDocumentIndex;
import uk.ac.gla.terrier.structures.incrementalindex2.memoryindex.MemoryIndex;
import uk.ac.gla.terrier.utility.ApplicationSetup;


public class SegmentedDocumentIndex extends DocumentIndex
{
	protected final TIntArrayList segmentIds;
	protected final TIntArrayList segmentMaxDocids;
	protected final String path;
	protected final String prefix;
	
	protected MemoryIndex MI;
	protected MemoryDocumentIndex MDoI;
	
	protected DocumentIndex[] DocIndexes; 
	protected int numberOfSegments;
	
	protected int lastDocid = -10000000;
	protected int lastSegmentIndex;
	
	public SegmentedDocumentIndex(String path, String prefix, TIntArrayList segmentIds, TIntArrayList segmentMaxDocids, MemoryIndex MI)
	{
		super(3,3,3);
		this.segmentIds = segmentIds;
		this.segmentMaxDocids = segmentMaxDocids;
		this.path = path; this.prefix = prefix;
		setMemoryIndex(MI);
	}
	
	public void reOpenSegments()
	{
		if (DocIndexes != null)
		{
			for(int i=0;i<DocIndexes.length;i++)
				if (DocIndexes[i] != null)
				{
					DocIndexes[i].close();
					DocIndexes[i] = null;
				}
		}
		
		final int[] segmentIds = this.segmentIds.toNativeArray();
		System.err.println("SegmentedDocumentIndex attempting to open "+segmentIds.length+" segments");
		numberOfSegments = segmentIds.length;
		DocIndexes = new DocumentIndex[numberOfSegments];
		for(int i=0;i<numberOfSegments;i++)
		{
			if (segmentIds[i] == -1)
				continue;
			DocIndexes[i] = new DocumentIndex(
					path + ApplicationSetup.FILE_SEPARATOR + 
					prefix + "_" + segmentIds[i]+
					ApplicationSetup.DOC_INDEX_SUFFIX);
		}
	}
	
	public void setMemoryIndex(MemoryIndex mi)
	{
		MI = mi;
		MDoI = (MemoryDocumentIndex)MI.getDocumentIndex();
	}

	@Override
	public void close() {
		if (DocIndexes != null)
		{
			for(int i=0;i<numberOfSegments;i++)
				if (DocIndexes[i] != null)
				{
					DocIndexes[i].close();
					DocIndexes[i] = null;
				}
		}
	}
	
	public int findSegment(int docid)
	{
		System.err.println("docid: "+ docid + "; segments["+segmentMaxDocids.size()+"]={"+IncrementalIndex.collateIntArray(segmentMaxDocids.toNativeArray())+"}");
		//TODO: all this requires checking	
		if (docid == lastDocid)
			return lastSegmentIndex;
		if (segmentMaxDocids.size() == 1 && lastDocid <= segmentMaxDocids.get(0) )
			return 0;
		int rtr = segmentMaxDocids.binarySearch(docid);
		System.err.println("BS returned "+rtr);
		/* binary search returns:
		 * index of the search key, if it is contained in the list; otherwise, (-(insertion point) - 1).
		 */
		if (rtr < 0)
		{
			if ((-rtr -1) == segmentMaxDocids.size())
			{
				if (MDoI.seek(docid))
					return -2;
				return -1;
			}
			rtr = -rtr -1;/* (-(insertion point) - 1) */
		}
		lastDocid = docid; lastSegmentIndex = rtr;
		return rtr;
	}
	
	protected int[] findSegment(String docno)
	{
		int[] rtr = new int[2];
		for(int i=0;i<numberOfSegments;i++)
			if ((rtr[1] = DocIndexes[i].getDocumentId(docno)) != -1)
			{
				lastSegmentIndex = rtr[0] = i;
				lastDocid = rtr[1];				
				return rtr;				
			}
		if ((rtr[1] = MDoI.getDocumentId(docno)) != -1)
		{
			lastSegmentIndex = rtr[0] = -2;
			lastDocid = rtr[1];
			return rtr;
		}
		return new int[]{-1,-1};
	}
	
	@Override
	public boolean seek(int docid) throws IOException {
		if (lastDocid == docid)
			return true;
		return findSegment(docid)!= -1;
	}

	@Override
	public boolean seek(String docno) throws IOException {
		final int rtr[] = findSegment(docno);
		return rtr[0] != -1;
	}

	
	@Override
	public FilePosition getDirectIndexEndOffset() {
		return
			lastSegmentIndex == -2 
			? null 
			: DocIndexes[lastSegmentIndex].getDirectIndexEndOffset();
	}

	@Override
	public FilePosition getDirectIndexStartOffset() {
		return lastSegmentIndex == -2 
			? null
			: DocIndexes[lastSegmentIndex].getDirectIndexStartOffset();
	}
	
	public FilePosition getDirectIndexEndOffset(final int docid) {
		final int segIndex = findSegment(docid);		
		return segIndex == -2 
			? null
			: DocIndexes[segIndex].getDirectIndexEndOffset();
	}

	public FilePosition getDirectIndexStartOffset(final int docid) {
		final int segIndex = findSegment(docid);
		return segIndex == -2
			? null
			: DocIndexes[segIndex].getDirectIndexStartOffset();
	}


	@Override
	public int getDocumentId(final String docno) {
		final int rtr[] = findSegment(docno);
		return rtr[1];
	}

	@Override
	public int getDocumentLength(final int docid) {
		final int segIndex = findSegment(docid);
		if (segIndex == -1)
			return -1;
			
		return segIndex == -2
			? MDoI.getDocumentLength(docid)
			: DocIndexes[segIndex].getDocumentLength(docid);
	}

	@Override
	public int getDocumentLength(final String docno) {
		final int rtr[] = findSegment(docno);
		if (rtr[0] == -1)
			return -1;
		if (rtr[0] == -2)
			return MDoI.getDocumentLength(rtr[0]);
		return DocIndexes[rtr[0]].getDocumentLength(rtr[1]);
	}

	@Override
	public String getDocumentNumber(int docid) {
		final int segIndex = findSegment(docid);
		return segIndex == -2
			? MDoI.getDocumentNumber(docid)
			: DocIndexes[segIndex].getDocumentNumber(docid);
	}

	@Override
	public int getNumberOfDocuments() {
		//TODO
		return -1;
	}
	
	
}

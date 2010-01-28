package uk.ac.gla.terrier.structures.incrementalindex2;

import gnu.trove.TIntArrayList;
import uk.ac.gla.terrier.structures.DirectIndex;
import uk.ac.gla.terrier.structures.incrementalindex2.memoryindex.MemoryDirectIndex;
import uk.ac.gla.terrier.structures.incrementalindex2.memoryindex.MemoryIndex;
import uk.ac.gla.terrier.utility.ApplicationSetup;

public class SegmentedDirectIndex extends DirectIndex {
	protected final TIntArrayList segmentIds;
	protected final SegmentedDocumentIndex docIndex;
	protected final String path;
	protected final String prefix;
	
	protected DirectIndex[] DirIndexes;
	protected int numberOfSegments;
	protected MemoryIndex MI;
	protected MemoryDirectIndex MDiI;
	
	public SegmentedDirectIndex(String path, String prefix, TIntArrayList segmentIds, SegmentedDocumentIndex SDoI, MemoryIndex MI)
	{
		super(3,3,3);
		this.path = path;
		this.prefix = prefix;
		this.segmentIds = segmentIds;
		this.docIndex = SDoI;
		this.MI = MI;
	}
	
	public void reOpenSegments()
	{
		if (DirIndexes != null)
		{
			for(int i=0;i<DirIndexes.length;i++)
				if (DirIndexes[i] != null)
				{
					DirIndexes[i].close();
					DirIndexes[i] = null;
				}
		}
		
		final int[] segmentIds = this.segmentIds.toNativeArray();
		System.err.println("SegmentedDirectIndex attempting to open "+segmentIds.length+" segments");
		numberOfSegments = segmentIds.length;
		DirIndexes = new DirectIndex[numberOfSegments];
		for(int i=0;i<numberOfSegments;i++)
		{
			if (segmentIds[i] == -1)
				continue;
			DirIndexes[i] = new DirectIndex(
					docIndex,
					path, 
					prefix + "_" + segmentIds[i]);
		}
	}
	
	public void setMemoryIndex(MemoryIndex mi)
	{
		MI = mi;
		MDiI = (MemoryDirectIndex)MI.getDirectIndex();
	}


	@Override
	public int[][] getTerms(final int docid) {
		int segmentIndex = docIndex.findSegment(docid);
		if (segmentIndex == -1)
			return null;
		if (segmentIndex == -2)
			return MDiI.getTerms(docid);
		return DirIndexes[segmentIndex].getTerms(docid);
	}

	@Override
	public void close() {
		if (DirIndexes != null)
		{
			for(int i=0;i<numberOfSegments;i++)
				if (DirIndexes[i] != null)
				{
					DirIndexes[i].close();
					DirIndexes[i] = null;
				}
		}
		MDiI = null;
	}
	
	
}

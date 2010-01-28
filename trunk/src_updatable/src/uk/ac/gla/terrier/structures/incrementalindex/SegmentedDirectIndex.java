package uk.ac.gla.terrier.structures.incrementalindex;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.structures.DirectIndex;
import uk.ac.gla.terrier.structures.incrementalindex.globaldocumentindex.GlobalDocumentIndex;
import uk.ac.gla.terrier.structures.incrementalindex.memoryindex.MemoryIndex;

/** @author John Kane &amp; Craig Macdonald */
public class SegmentedDirectIndex extends DirectIndex {

	protected final int MaxNumberOfSegments;
	protected int numberOfSegments;
	
	protected GlobalDocumentIndex DocInd;
	
	protected MemoryIndex Mindex;
	
	protected IncrementalIndexProperties properties;
	
	DirectIndex[] DirIndexes;
	
	public SegmentedDirectIndex(IncrementalIndexProperties prop, GlobalDocumentIndex gdi)
	{
		properties = prop;
		DocInd = gdi;
		
		MaxNumberOfSegments = prop.getMaxNumberOfSegments();
		reOpenSegments();
	}
	
	public void reOpenSegments()
	{
		if (DirIndexes != null)
			for(int i=0;i<DirIndexes.length;i++)
				if (DirIndexes[i] != null)
				{
					DirIndexes[i].close();
					DirIndexes[i] = null;
				}
		
		final int[] segmentIds = properties.getCurrentSegmentIds();
		numberOfSegments = 1;//segmentIds.length;
		DirIndexes = new DirectIndex[numberOfSegments];
		for(int i=0;i<numberOfSegments;i++)
		{
			//if (segmentIds[i] == -1)
			//	continue;
			DirIndexes[i] = new DirectIndex(
				DocInd,
				properties.indexPath + ApplicationSetup.FILE_SEPARATOR + 
				properties.indexPrefix + "_" + 0/*segmentIds[i]*/ + 
				ApplicationSetup.DF_SUFFIX);
		}
	}
	
	
	public int[][] getTerms(int docid) {
		//TODO: shouldnt this work with offsets, just like the InvertedIndex segments?
		DirIndexes[DocInd.getSegmentNumber(docid)].getTerms(docid);
		return null;
	}	
	
	public void setMemoryIndex(MemoryIndex mi)
	{
		Mindex = mi;
	}
	
	public void print() {
		// TODO Auto-generated method stub

	}

	public void close() {
		// TODO Auto-generated method stub

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}

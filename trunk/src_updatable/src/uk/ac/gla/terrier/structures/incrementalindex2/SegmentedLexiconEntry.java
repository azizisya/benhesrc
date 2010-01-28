package uk.ac.gla.terrier.structures.incrementalindex2;

import gnu.trove.TIntArrayList;

import java.util.ArrayList;

import uk.ac.gla.terrier.structures.FilePosition;
import uk.ac.gla.terrier.structures.LexiconEntry;

public class SegmentedLexiconEntry extends LexiconEntry
{
	public ArrayList<FilePosition> startSegmentOffsets = new ArrayList<FilePosition>();
	public ArrayList<FilePosition> endSegmentOffsets = new ArrayList<FilePosition>();
	public TIntArrayList indexIndexNos = new TIntArrayList();
	
	public void add(int index, LexiconEntry le)
	{
		super.add(le);
		FilePosition fp = new FilePosition();
		fp.Bytes = le.startOffset; fp.Bits = le.startBitOffset;
		startSegmentOffsets.add(fp);
		fp = new FilePosition();
		fp.Bytes = le.endOffset; fp.Bits = le.endBitOffset;
		endSegmentOffsets.add(fp);
		indexIndexNos.add(index);
	}
	
	public int[] getSegmentIndexes()
	{
		return indexIndexNos.toNativeArray();
	}
	
	public FilePosition getStartOffset(int i)
	{
		return startSegmentOffsets.get(i);
	}
	
	public FilePosition getEndOffset(int i)
	{
		return endSegmentOffsets.get(i);
	}
	
}

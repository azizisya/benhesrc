package uk.ac.gla.terrier.structures.incrementalindex2.memoryindex;

public class MemoryDocIndexRecord implements Comparable<MemoryDocIndexRecord> {

	public int docid;
	public int docLength;
	public String docno;
	
	public MemoryDocIndexRecord() { }
	
	public MemoryDocIndexRecord(int idi, int l, String sdi)
	{
		docid = idi;
		docLength = l;
		docno = sdi;
	}
	
	public String toString()
	{
		return docid+" "+docLength+" "+docno;
	}
	
	public int compareTo(MemoryDocIndexRecord o)
	{
		if (o.docid > this.docid) return 1;
		if (o.docid < this.docid) return -1;
		return 0;
	}

}

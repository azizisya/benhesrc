package uk.ac.gla.terrier.structures.incrementalindex.globaldocumentindex;

public class MemoryDocIndexRecord {

	public int docid;
	public int docLength;
	public String docno;
	
	public MemoryDocIndexRecord()
	{
		
	}
	
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

}

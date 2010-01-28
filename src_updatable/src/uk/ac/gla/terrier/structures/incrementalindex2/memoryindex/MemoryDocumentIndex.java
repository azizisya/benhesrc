package uk.ac.gla.terrier.structures.incrementalindex2.memoryindex;

import gnu.trove.THashMap;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntObjectIterator;
import gnu.trove.TIntObjectProcedure;

import java.io.IOException;
import java.util.Arrays;

import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.DocumentIndexOutputStream;
import uk.ac.gla.terrier.structures.FilePosition;

public class MemoryDocumentIndex extends DocumentIndex {
	
	protected MemoryDocIndexRecord currentDocRecord;
	protected int currentDoc;
	
	protected TIntObjectHashMap IntDocidRecords; 
	protected THashMap docnoRecords;
	
	//TODO write accessor for this, so it doesnt have to be public to the world!
	public int docidCounter = 0;
	
	public MemoryDocumentIndex(long a, long b, long c)
	{
		super(a,b,c);
	}

	public MemoryDocumentIndex(final int firstDocid)
	{
		super((long)3,(long)3,(long)3);
		docidCounter = firstDocid;
		IntDocidRecords = new TIntObjectHashMap();
		docnoRecords = new THashMap();
	}
	
	public void close() {
		IntDocidRecords = null;
		docnoRecords = null;
	}

	public void print()
	{

		System.out.println("Number of Docs:"+IntDocidRecords.size());
		IntDocidRecords.forEachEntry(new printProcedure());
		//TIntObjectIterator iter = new TIntObjectIterator(postingsLists);
		
		/*while(iter.hasNext())
		{
			iter.advance();
			postingList list = (postingList)iter.value();
			System.out.println(list);	
		}*/

	}

	public int getDocumentId(String docno) {
		if(seek(docno))
			return currentDocRecord.docid;
		else
			return -1;
	}

	public int getDocumentLength(int i) {
		if(seek(i))
		return currentDocRecord.docLength;
		else
			return -1;
	}

	public int getDocumentLength(String docno) {
		if(seek(docno))
			return currentDocRecord.docLength;
		else
			return -1;
	}

	public String getDocumentNumber(int i) {
		if(seek(i))
			return currentDocRecord.docno;
		else
			return null;
	}


	public int getNumberOfDocuments() {
		return IntDocidRecords.size();
	}

	/*
	public FilePosition getDirectIndexStartOffset() {
		// TODO Auto-generated method stub
		return null;
	}*/

	public boolean seek(int i) {
		currentDocRecord = (MemoryDocIndexRecord)IntDocidRecords.get(i);
		return currentDocRecord != null;
	}

	public boolean seek(String docno) {
		currentDocRecord = (MemoryDocIndexRecord)docnoRecords.get(docno);
		return currentDocRecord != null;
	}
	
	public int newDocEntry(String docno, int length)
	{
		int docid = docidCounter++;
		MemoryDocIndexRecord entry = new MemoryDocIndexRecord(docid, length, docno);
		IntDocidRecords.put(docid, entry);
		docnoRecords.put(docno, entry);
		return docid;
	}
	
	public boolean deleteDocEntry(String docno)
	{
		if(!seek(docno))
		{
			System.err.println("Tried to delete nonexistent doc from document index.");
			return false;
		}
		
		IntDocidRecords.remove(currentDocRecord.docid);
		docnoRecords.remove(docno);
		
		return true;
	}
	
	public boolean deleteDocEntry(int docid)
	{
		if(!seek(docid))
		{
			System.err.println("Tried to delete nonexistent doc from document index.");
			return false;
		}
		
		IntDocidRecords.remove(docid);
		docnoRecords.remove(currentDocRecord.docno);
		
		return true;
	}
	
	public TIntObjectIterator iterator()
	{
		return new TIntObjectIterator(IntDocidRecords);
	}
	
	class printProcedure implements TIntObjectProcedure{
		
		public boolean execute(int a, Object b)
		{
			System.out.println((MemoryDocIndexRecord)b);
			
			return true;
		}
		
	}
	
	public void flush(DocumentIndexOutputStream Dios, FilePosition[] positions) throws IOException {
		
		final int docids[] = IntDocidRecords.keys();
		Arrays.sort(docids); int count = 0;
		MemoryDocIndexRecord temp;
		final int docCount = docids.length;
		
		if (positions != null)
			for(int i=0;i<docCount;i++)
			{
				temp = (MemoryDocIndexRecord)IntDocidRecords.get(docids[i]);
				Dios.addEntry(temp.docno, temp.docid,temp.docLength,positions[count].Bytes,positions[count].Bits);
				count++;
			}
		else
			for(int i=0;i<docCount;i++)
			{
				temp = (MemoryDocIndexRecord)IntDocidRecords.get(docids[i]);
				Dios.addEntry(temp.docno,temp.docLength,(long)0,(byte)0);
				count++;
			}
		
		Dios.close();
	
		
	}

	 

}

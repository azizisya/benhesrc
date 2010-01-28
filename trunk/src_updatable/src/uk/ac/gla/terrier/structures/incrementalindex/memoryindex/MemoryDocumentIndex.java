package uk.ac.gla.terrier.structures.incrementalindex.memoryindex;

import java.io.IOException;
import java.util.Iterator;
import java.util.TreeMap;

import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.DocumentIndexOutputStream;
import uk.ac.gla.terrier.structures.FilePosition;
import uk.ac.gla.terrier.structures.incrementalindex.globaldocumentindex.*;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import gnu.trove.*;

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
	
	public void flush(FilePosition[] positions) {
		//TObjectObjectHashMapDecorator d = new THashMapDecorator(TermRecords);
		TreeMap tm = new TreeMap(docnoRecords);
		//Set s = tm.entrySet();
		Iterator iter = tm.values().iterator();
		String path =  ApplicationSetup.TERRIER_INDEX_PATH 
						+ ApplicationSetup.FILE_SEPARATOR 
							+ ApplicationSetup.TERRIER_INDEX_PREFIX
								+ ApplicationSetup.DOC_INDEX_SUFFIX;
		try{
		DocumentIndexOutputStream Dios = new DocumentIndexOutputStream(path);
		MemoryDocIndexRecord temp;
		int count = 0;
		//System.out.println("Positions as passed: "+positions[count].Bytes+" "+positions[count].Bits);
		while(iter.hasNext())
		{

			temp = (MemoryDocIndexRecord)iter.next();
			
			Dios.addEntry(temp.docno,temp.docLength,positions[count].Bytes,positions[count].Bits);
			count++;
		}
		Dios.close();
		
		}catch(IOException ioe){
			System.err.println("Indexing failed to write a Document index to disk : "+ioe);
			System.exit(1);}
		
		
	}

	 

}

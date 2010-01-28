package uk.ac.gla.terrier.structures.incrementalindex.globaldocumentindex;


import gnu.trove.TIntObjectIterator;

import java.io.IOException;
import java.io.RandomAccessFile;

import uk.ac.gla.terrier.structures.FilePosition;
import uk.ac.gla.terrier.structures.incrementalindex.memoryindex.MemoryDocumentIndex;
import uk.ac.gla.terrier.structures.trees.bplustree.BplusDiskLeafNode;
import uk.ac.gla.terrier.structures.trees.bplustree.BplusLeafNode;
import uk.ac.gla.terrier.structures.trees.bplustree.BplusNode;
import uk.ac.gla.terrier.structures.trees.bplustree.BplusTree;
import uk.ac.gla.terrier.structures.trees.bplustree.BplusValue;
import uk.ac.gla.terrier.utility.ApplicationSetup;


public class GlobalDocumentIndex extends MemoryDocumentIndex {	


/**
 * The number of entries in the document index.
 */
protected int numberOfDocumentIndexEntries;


protected BplusTree StringTree;

protected DocIndexRecord currentDocIndexRecord;

protected GlobalDocIndexProperties properties;

/** The file containing the mapping from the codes to the offset in the DocumentIndex file.*/
protected RandomAccessFile idToNodeFile;


	public GlobalDocumentIndex()
	{
		this(ApplicationSetup.TERRIER_INDEX_PATH, ApplicationSetup.TERRIER_INDEX_PREFIX);
	}

	public GlobalDocumentIndex(String path, String prefix)
	{
		super((long)3,(long)3,(long)3);
		properties = new GlobalDocIndexProperties(path, prefix);
		StringTree = new BplusTree(properties.getTreeProperties());
		numberOfDocumentIndexEntries = 0;

		idToNodeFile = properties.getIdToNodeFile();
	}

	public void close() {
		StringTree.close();
	}

	public void print() {
		System.out.println(StringTree.toString());
	}

	/**
	 * Returns the document's id for the given docno.
	 * @return int The document's id, or -1 if docno was not found in the index.
	 * @param docno java.lang.String The document's number
	 */
	public int getDocumentId(String docno) {
		if (currentDocIndexRecord == null || !this.currentDocIndexRecord.docno.equals(docno)) 

				if (seek(docno) == false)
					return -1;

		System.out.println("Executed retrun docid");
		return currentDocIndexRecord.docid;
	}

	/**
	 * Reading the length for the i-th document.
	 * @param i the index of the document.
	 * @return the length of the i-th document, or -1 if the docid i wasn't found in the index.
	 */
	public int getDocumentLength(int i) {
		if (currentDocIndexRecord == null || i != currentDocIndexRecord.docid) 
				if (seek(i) == false)
					return -1;

		System.out.println("Executed retrun doclength");
		return currentDocIndexRecord.docLength;
	}

	/**
	 * Return the length of the document with the given docno.
	 * Creation date: (29/05/2003 10:56:49)
	 * @return int The document's length, or -1 if the docno wasn't found in the index.
	 * @param docno java.lang.String The document's number
	 */
	public int getDocumentLength(String docno) {
		if (currentDocIndexRecord == null || !this.currentDocIndexRecord.docno.equals(docno)) {
				if (seek(docno) == false)
					return -1;

		}
		
		System.out.println("Executed retrun doc length");
		return currentDocIndexRecord.docLength;
	}
	
	
	/**
	 * Reading the docno for the i-th document.
	 * @param i the index of the document.
	 * @return the document number of the i-th document, or null if the docid i wasn't found in the index.
	 */
	public String getDocumentNumber(int i) {
		if (currentDocIndexRecord == null || i != currentDocIndexRecord.docid)
				if (seek(i) == false)
					return null;


		return currentDocIndexRecord.docno;
	}
	/** 
	 * Returns the ending offset of the document's entry
	 * in the direct index.
	 * @return FilePosition an offset in the direct index.
	 */
	public FilePosition getDirectIndexEndOffset(int i) {
		if (currentDocIndexRecord == null || i != currentDocIndexRecord.docid)
			if (seek(i) == false)
				return null;


	return currentDocIndexRecord.getEndOffset();
	}
	/**
	 * Returns the number of documents in the collection.
	 * @return the number of documents in the collection.
	 */
	public int getNumberOfDocuments() {
		return numberOfDocumentIndexEntries;
	}
	/**
	 * Returns the starting offset of the document's entry
	 * in the direct index.
	 * @return FilePosition an offset in the direct index.
	 */
	public FilePosition getDirectIndexStartOffset(int i) {
		if (currentDocIndexRecord == null || i != currentDocIndexRecord.docid)
			if (seek(i) == false)
				return null;

	return currentDocIndexRecord.getStartOffset();
	}
	
	public int getSegmentNumber(int i)
	{
		if (currentDocIndexRecord == null || i != currentDocIndexRecord.docid)
			if (seek(i) == false)
				return -1;

		System.out.println("Retrieved segNumber "+currentDocIndexRecord.getSegment());
	return currentDocIndexRecord.getSegment();
	}
	
	public int getHeight()
	{
		return StringTree.getHeight();
	}
	
	public int size()
	{
		return StringTree.size();
	}
	

	public boolean seek(int docid) {
		try{
			System.out.println("Seek on int");
			idToNodeFile.seek((docid/FilePosition.sizeInBytes()));
			byte[] filePositionAsBytes = new byte[FilePosition.sizeInBytes()];
			idToNodeFile.read(filePositionAsBytes);
			
			FilePosition fp = new FilePosition(filePositionAsBytes);
			
			BplusValue[] possibleValues = StringTree.search(fp);
			
			for(int i = 0; i < possibleValues.length; i++)
			{
				if((DocIndexRecord)possibleValues[i] != null &&
						((DocIndexRecord)possibleValues[i]).docid == docid)
				{
					currentDocIndexRecord = (DocIndexRecord)possibleValues[i];
					System.out.println("Looking for "+docid+" found "+currentDocIndexRecord);
					return true;
				}
			}
			}
			catch(IOException ioe){
				ioe.printStackTrace();
			}
			
			return false;
	}

	public boolean seek(String docno) {
		System.out.println("Seek on string");
		currentDocIndexRecord = (DocIndexRecord)StringTree.search(new DocIndexStringKey(docno));
		return currentDocIndexRecord != null; 
	}

	int	DocidCounter = 0;
	
	public int newDocEntry(String docno, int length, 
			int segNumber, FilePosition sOffset, FilePosition eOffset) {
		int docid = DocidCounter++;
		DocIndexRecord entry = new DocIndexRecord(docid, length, docno, segNumber, sOffset, eOffset);
		StringTree.insert(new DocIndexStringKey(docno), entry);
		
		FilePosition fp = ((BplusDiskLeafNode)StringTree.getLastRetrievedLeafNode()).getFilePosition();
		//IntTree.insert(new LexiconIntKey(termid), entry);
		
		writeToIdtoNodeFile(docid,fp);
		currentDocIndexRecord = entry;
		
		numberOfDocumentIndexEntries++;
		return currentDocIndexRecord.docid;
	}

	public boolean deleteDocEntry(String docno) {
		StringTree.deleteValue(new DocIndexStringKey(docno));
		numberOfDocumentIndexEntries--;
		return true;
	}

	public boolean deleteDocEntry(int docid) {
		if(seek(docid))
		{
			StringTree.deleteValue(new DocIndexStringKey(currentDocIndexRecord.docno));
			numberOfDocumentIndexEntries--;
			return true;
		}
		else
			return false;
	}

	public void merge(int segNumber, MemoryDocumentIndex DoI, FilePosition[] positions)
	{
		TIntObjectIterator iter = DoI.iterator();
		MemoryDocIndexRecord rec;
		
		FilePosition current = new FilePosition(0,(byte)0);
		int count = 0;
		
		while(iter.hasNext())
		{
			iter.advance();
			rec = (MemoryDocIndexRecord)iter.value();
			this.newDocEntry(rec.docno,rec.docLength, segNumber,current,positions[count]);
			current = new FilePosition(positions[count]);
			count++;
		}
	}
	
	public void flush(FilePosition[] positions) {
		// TODO Auto-generated method stub

	}
	
	/**
	 * Writes the fileposition of the node corresponding to the docid
	 * to disk. This method assumes that the termids are coming in increasing
	 * order through increments of 1.
	 * @param termid
	 * @param fp
	 */
	protected void writeToIdtoNodeFile(int termid, FilePosition fp)
	{
		try{
			idToNodeFile.seek(idToNodeFile.length());
			fp.toBytes(idToNodeFile);//idToNodeFile.write(fp.toBytes());
		}
		catch(IOException ioe){System.out.println("Error while writing termid to node file: ");
		ioe.printStackTrace();}
	}
	
	public BplusLeafNode getFirstLeaf()
	{
		return StringTree.getFirstLeaf();
	}
	
	public BplusNode getRoot()
	{
		return StringTree.getRoot();
	}
	
	public static void main(String args[])
	{
		GlobalDocumentIndex DoI = new GlobalDocumentIndex();
		
		
		
		MemoryDocumentIndex Mind = new MemoryDocumentIndex(0);
		Mind.newDocEntry("10",255);
		Mind.newDocEntry("245",90);
		Mind.newDocEntry("246",100);
		Mind.newDocEntry("300",120);
		Mind.newDocEntry("24",600);
		
		//MemoryDirectIndex MDii = new MemoryDirectIndex();
		
	//	DoI.merge(Mind);
		DoI.print();
		DoI.seek("10");
		System.out.println(DoI.getDocumentLength("24"));
	}

}

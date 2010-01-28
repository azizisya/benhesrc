package uk.ac.gla.terrier.structures.incrementalindex.globallexicon;


import uk.ac.gla.terrier.structures.incrementalindex.memoryindex.*;
import uk.ac.gla.terrier.structures.indexing.LexiconBuilder;
import uk.ac.gla.terrier.structures.trees.bplustree.*;
import uk.ac.gla.terrier.structures.*;
import uk.ac.gla.terrier.utility.ApplicationSetup;

import java.io.*;
import gnu.trove.*;


public class GlobalLexicon extends MemoryLexicon{
	

	
	protected long numberOfPointers;
	
	protected LexiconRecord currentLexiconRecord;
	
	/** The file containing the mapping from the codes to the offset in the lexicon file.*/
	protected RandomAccessFile idToNodeFile;
	
	protected GlobalLexiconProperties properties;
	protected BplusTree StringTree;
	//protected BplusTree IntTree;
	
	
	protected int maxSegments;

	
	
	/** 
	 * A default constructor.
	 */
	public GlobalLexicon(final int maxSegments){
		this(	maxSegments,
				ApplicationSetup.TERRIER_INDEX_PATH,
				ApplicationSetup.TERRIER_INDEX_PREFIX);
	}
	
	public GlobalLexicon(final int maxSegments, String path, String prefix) {
		//super(3,3,3);
		properties = new GlobalLexiconProperties(path, prefix);
		this.maxSegments = maxSegments;
		StringTree = new BplusTree(properties.getTreeProperties());

		idToNodeFile = properties.getIdToNodeFile();
		
		numberOfPointers = 0;
	}

	public LexiconInputStream getLexiconInputStream()
	{
		return (LexiconInputStream)new GlobalLexiconInputStream(StringTree);
	}

	
	
	/**
	* Closes the lexicon and lexicon index files.
	*/
	public void close() {
		StringTree.close();
	}
	
	/**
	 * Finds the term given its term code. Does a lookup to the idToNOde
	 * file and then retreieves all the values for that node before doing
	 * a linear to find the correct one.
	 *
	 * @return true if the term is found, else return false
	 * @param _termId the term's identifier
	 */
	public boolean findTerm(int _termId) {
		if(currentLexiconRecord == null || !(currentLexiconRecord.termid == _termId))
		{
		try{
		idToNodeFile.seek((_termId*FilePosition.sizeInBytes()));
		byte[] filePositionAsBytes = new byte[FilePosition.sizeInBytes()];
		idToNodeFile.read(filePositionAsBytes);
		
		FilePosition fp = new FilePosition(filePositionAsBytes);
		
		BplusValue[] possibleValues = StringTree.search(fp);
		
		for(int i = 0; i < possibleValues.length; i++)
		{
			if((LexiconRecord)possibleValues[i] != null &&
					((LexiconRecord)possibleValues[i]).getTermid() == _termId)
			{
				currentLexiconRecord = (LexiconRecord)possibleValues[i];
				return true;
			}
		}
		}
		catch(IOException ioe){
			ioe.printStackTrace();
		}
		
		return false;
		}
		else
			return true;

	}
	/** 
	 * Performs a binary search in the lexicon
	 * in order to locate the given term.
	 * If the term is located, the properties
	 * termCharacters, documentFrequency,
	 * termFrequency, startOffset, startBitOffset,
	 * endOffset and endBitOffset contain the
	 * values related to the term.
	 * @param _term The term to search for.
	 * @return true if the term is found, and false otherwise.
	 */
	public boolean findTerm(String _term) {
		
			
		if(currentLexiconRecord == null || !currentLexiconRecord.term.equals(_term))
		{
			currentLexiconRecord = null;
			
			currentLexiconRecord = (LexiconRecord)StringTree.search(new LexiconStringKey(_term));
		}
		
		return currentLexiconRecord != null; 
	}

	/**
	 * Returns the bit offset in the last byte of 
	 * the term's entry in the inverted file.
	 * 
	 * @return byte the bit offset in the last byte of 
	 *         the term's entry in the inverted file
	 */
	public byte getEndBitOffset(int segNumber) {
		if(currentLexiconRecord != null)
			return currentLexiconRecord.getEndOffset(segNumber).Bits;
		else
			return -1;
	}
	
	/**
	 * Returns the ending offset of the term's entry in the inverted file.
	 *
	 * @return long The ending byte of the term's entry in the inverted file.
	 */
	public long getEndOffset(int segNumber) {
		if(currentLexiconRecord != null)
			return currentLexiconRecord.getEndOffset(segNumber).Bytes;
		else
			return -1;
	}
	/**
	 * Return the document frequency for the given term.
	 *
	 * @return int The document frequency for the given term
	 */
	public int getNt() {
		if(currentLexiconRecord != null)
			return currentLexiconRecord.Nt;
		else
			return 0;
	}
	/**
	 * Returns the number of entries in the lexicon.
	 * @return the number of entries in the lexicon.
	 */
	public long getNumberOfLexiconEntries() {
		return StringTree.size();
	}
	
	public int size()
	{
		return StringTree.size();
	}
	
	
	/**
	 * The bit offset in the starting byte of 
	 * the entry in the inverted file.
	 *
	 * @return byte The number of bits in the first 
	 *         byte of the entry in the inverted file
	 */
	public byte getStartBitOffset(int segNumber) {
		if(currentLexiconRecord != null)
			return currentLexiconRecord.getStartOffset(segNumber).Bits;
		else
			return -1;
	}
	/**
	 * Returns the beginning of the term's entry in the inverted file.
	 *
	 * @return long the start offset (in bytes) in the inverted file
	 */
	public long getStartOffset(int segNumber) {
		if(currentLexiconRecord != null)
			return currentLexiconRecord.getStartOffset(segNumber).Bytes;
		else
			return -1;
	}
	
	/**
	 * Returns the term's id.
	 *
	 * @return int the term's id.
	 */
	public int getTermId() {
		if(currentLexiconRecord != null)
			return currentLexiconRecord.termid;
		else return -1;
	}
	/**
	 * Returns the term frequency for the already seeked term.
	 *
	 * @return int The term frequency in the collection.
	 */
	public int getTF() {
		if(currentLexiconRecord != null)
			return currentLexiconRecord.TF;
		else
			return 0;
	}
	
	public String getTerm()
	{
		if(currentLexiconRecord != null)
			return currentLexiconRecord.getTerm();
		else
			return null;
	}
	/**
	 * Seeks the i-th entry of the lexicon.
	 * TODO read a byte array from the file and decode it, 
	 * 		instead of reading the different pieces of 
	 *      information separately.
	 * @param i The index of the entry we are looking for.
	 * @return true if the entry was found, false otherwise.
	 */
	public boolean seekEntry(int i) {
		System.err.println("Tried to read the Global Lexicon sequentially, that was probably a bad idea.");
		
		
		return false;
	}
	
	public FilePosition[][] getSegments()
	{
		if(currentLexiconRecord != null)
			return currentLexiconRecord.getSegments();
		else
			return null;
	}
	
	public int getHeight()
	{
		return StringTree.getHeight();
	}
	
	/**
	 * Makes suitable updates on the addition of a term
	 * Look at params again.
	 * @return the term id of the term updated. -1 if update failed.
	 */
	public int incrementEntry(String term, int TF,int Nt,
			int segNumber, FilePosition sOffset, FilePosition eOffset) {

		if (! findTerm(term))
		{
			//System.err.println("Didn't find "+term+" current lex "+currentLexiconRecord);
			return newEntry(term, TF, Nt,segNumber, sOffset, eOffset);
		}
		currentLexiconRecord.TF += TF;
		currentLexiconRecord.Nt += Nt;
		currentLexiconRecord.addSegment(segNumber, sOffset, eOffset);
		numberOfPointers += Nt;
		
		//System.err.println("Updating "+term);
		StringTree.updateValue(new LexiconStringKey(term),currentLexiconRecord);

		return currentLexiconRecord.termid;
	}
	
	/**
	 * Makes suitable updates on the addition of a term
	 * Look at params again.
	 * @return the term id of the term updated. -1 if update failed.
	 */
	/*public int incrementEntry(String term, int TF,int Nt) {

		if (! findTerm(term))
		{
			return newEntry(term, TF, Nt);
		}
		currentLexiconRecord.TF += TF;
		currentLexiconRecord.Nt += Nt;
		//currentLexiconRecord.addSegment(segNumber, sOffset, eOffset);
		numberOfPointers += Nt;
		
		
		StringTree.updateValue(new LexiconStringKey(term),currentLexiconRecord);

		return currentLexiconRecord.termid;
	}*/
	
	int termidCounter = 0; 
	
	protected int newEntry(String term, int TF, int Nt, 
			int segNumber, FilePosition sOffset, FilePosition eOffset)
	{
		int termid = termidCounter++;//TermCodes.getCode(term);
		LexiconRecord entry = new LexiconRecord(term, termid,TF, Nt, segNumber, sOffset, eOffset);
		StringTree.insert(new LexiconStringKey(term), entry);
		
		FilePosition fp = ((BplusDiskLeafNode)StringTree.getLastRetrievedLeafNode()).getFilePosition();
		//IntTree.insert(new LexiconIntKey(termid), entry);
		
		writeToIdtoNodeFile(termid,fp);
		currentLexiconRecord = entry;
		
		numberOfPointers += Nt;
		return termid;
	}
	/*
	protected int newEntry(String term, int TF, int Nt)
	{
		int termid = termidCounter++;//TermCodes.getCode(term);
		LexiconRecord entry = new LexiconRecord(term, termid,TF, Nt);
		StringTree.insert(new LexiconStringKey(term), entry);
		
		FilePosition fp = ((BplusDiskLeafNode)StringTree.getLastRetrievedLeafNode()).getFilePosition();
		//IntTree.insert(new LexiconIntKey(termid), entry);
		
		writeToIdtoNodeFile(termid,fp);
		currentLexiconRecord = entry;
		
		numberOfPointers += Nt;
		return termid;
	}*/
	
	/**
	 * Writes the fileposition of the node corresponding to the termid
	 * to disk. This method assumes that the termids are coming in increasing
	 * order through increments of 1.
	 * @param termid
	 * @param fp
	 */
	protected void writeToIdtoNodeFile(int termid, FilePosition fp)
	{
		try{
			idToNodeFile.seek(termid*FilePosition.sizeInBytes());
			fp.toBytes(idToNodeFile);//idToNodeFile.write(fp.toBytes());
		}
		catch(IOException ioe){System.out.println("Error while writing termid to node file: ");
		ioe.printStackTrace();}
	}
	
	
	public long getNumberOfPointers()
	{
		return numberOfPointers;
	}

	public int decerementEntry(String term, int TF, int Nt)
	{
		if(!findTerm(term))
			return -1;
		currentLexiconRecord.TF -= TF;
		currentLexiconRecord.Nt -= Nt;
		numberOfPointers -= Nt;
		return currentLexiconRecord.termid;
	}
	
	public boolean deleteEntry(String term)
	{
		//TODO: delete from both trees
		return false;
	}
	
	public boolean deleteEntry(int termid)
	{
		//TODO: delete from both trees
		return false;
	}
	
	public void merge(MemoryLexicon lex/*, FilePosition[] positions*/)
	{
		//Positions is an array of the end offsets of the posting lists
		//hence the start is initially the start of the file then its
		//equivalent to the previous posting lists end offset.
		TIntObjectIterator iter = lex.iterator_TIntObject();
		MemoryLexiconRecord rec;
		//int count = 0;
		//FilePosition start = new FilePosition(0,(byte)0);
		
		MemoryLexiconRecord[] LexRecs = new MemoryLexiconRecord[(int)lex.getNumberOfLexiconEntries()];
		int c = 0;
		while(iter.hasNext())
		{
			iter.advance();
			LexRecs[c] = (MemoryLexiconRecord)iter.value();
			c++;
		}
			
		for(int i = LexRecs.length-1; i >= 0; i--)
		//while(iter.hasNext())
		{
			//iter.advance();
			rec = /*(MemoryLexiconRecord)iter.value();//*/LexRecs[i];
			this.incrementEntry(rec.term,rec.TF,rec.Nt/*,segNumber,start,
					new FilePosition(positions[count])*/);
			/*start = new FilePosition(positions[count]);
			start.Bits +=  1;
			if (start.Bits == 8) {
				start.Bytes = start.Bytes + 1;
				start.Bits = 0;
			}
			
			count++;*/
		}
	}
	
	public void merge(int segNumber, MemoryLexicon lex,FilePosition[] positions)
	{
		TIntObjectIterator iter = lex.iterator_TIntObject();
		MemoryLexiconRecord rec;
		int count = 0;
		FilePosition start = new FilePosition(0,(byte)0);
		
		MemoryLexiconRecord[] LexRecs = new MemoryLexiconRecord[(int)lex.getNumberOfLexiconEntries()];
		int c = 0;
		
		
		while(iter.hasNext())
		{
			iter.advance();
			LexRecs[c] = (MemoryLexiconRecord)iter.value();
			c++;
		}
			
		for(int i = LexRecs.length-1; i >= 0; i--)
		//while(iter.hasNext())
		{
			//iter.advance();
			rec = /*(MemoryLexiconRecord)iter.value();//*/LexRecs[i];
			this.incrementEntry(rec.term,0,0,segNumber,start,
					new FilePosition(positions[count]));
			start = new FilePosition(positions[count]);
			start.Bits +=  1;
			if (start.Bits == 8) {
				start.Bytes = start.Bytes + 1;
				start.Bits = 0;
			}
			
			count++;
		}
	}
	/**
	 * Writes the lexicon to file on disk
	 *
	 */
	public void flush(FilePosition[] positions) {

		String path =  ApplicationSetup.TERRIER_INDEX_PATH 
						+ ApplicationSetup.FILE_SEPARATOR 
							+ ApplicationSetup.TERRIER_INDEX_PREFIX
								+ ApplicationSetup.LEXICONSUFFIX;
		try{
		LexiconOutputStream los = new LexiconOutputStream(path);
		LexiconRecord temp;
		int count = 0;
		BplusDiskLeafNode node = (BplusDiskLeafNode)StringTree.getFirstLeaf();
		
		while(node != null)
		{
			BplusValue[] values = node.getValues();
			
			
			for(int i = 0; i < values.length; i++)
			{
			temp = (LexiconRecord)values[i];
			los.writeNextEntry(temp.term, temp.termid, temp.Nt, temp.TF,
					 positions[count].Bytes,positions[count].Bits);
			count++;
			}
			

			node = (BplusDiskLeafNode)node.getNext();
		}
		los.close();
		
		(new LexiconBuilder()).createLexiconIndex(new LexiconInputStream(path),
				StringTree.size(),
				Lexicon.lexiconEntryLength);
		}catch(IOException ioe){
			System.err.println("Indexing failed to write a lexicon to disk : "+ioe);
			System.exit(1);}
		
		
	}
	
	public BplusLeafNode getFirstLeaf()
	{
		return StringTree.getFirstLeaf();
	}
	
	public BplusNode getRoot()
	{
		return StringTree.getRoot();
	}

	public void print()
	{
		/*BplusLeafNode current = StringTree.getFirstLeaf();
		while(current != null)
		{
			System.out.println(current);
			current = current.getNext();
			System.out.println();
		}*/
		
		System.out.println(StringTree.toString());
	}
	
	public static void main(String args[])
	{
		MemoryLexicon Mlex = new MemoryLexicon();
		MemoryInvertedIndex MInv = new MemoryInvertedIndex();
		
		int termid;
		//int docno = 13;
		
		termid = Mlex.incrementEntry("Mathew",1,2);
		System.out.println(termid +","+"Mathew,1,2");
		/*
		termid =Mlex.incrementEntry("Mark",3,4);
		System.out.println(termid +","+"Mark,3,4");
		
		termid =Mlex.incrementEntry("Luke",5,6);
		System.out.println(termid +","+"Luke,5,6");
		
		termid =Mlex.incrementEntry("John",7,8);
		System.out.println(termid +","+"John,7,8");
		
		
		termid = Mlex.incrementEntry("Thomas",9,10);
		System.out.println(termid +","+"Thomas,9,10");
		*/
		
		
		
		
		
		
		GlobalLexicon L = new GlobalLexicon(1);
		L.merge(Mlex);
		
		
		//L.print();
		
		
		System.out.println(L.findTerm("Mathew"));
		MInv.addDocForTerm(L.getTermId(),2,1);
		//System.out.println(L.findTerm("Mark"));
		MInv.addDocForTerm(L.getTermId(),6,3);
		//System.out.println(L.findTerm("Luke"));
		MInv.addDocForTerm(L.getTermId(),8,5);
		//System.out.println(L.findTerm("John"));
		MInv.addDocForTerm(L.getTermId(),7,7);
		//System.out.println(L.findTerm("Thomas"));
		MInv.addDocForTerm(L.getTermId(),10,9);

		//FilePosition[] pos = MInv.flush(0);

		//L.merge(0,Mlex,pos);
		
		//Mlex.flush(pos);
		
		//L.print();

		//for(int i = 0; i < pos.length; i++ )
		//System.out.println("FilesPosition "+pos[i]);
		
		//LexBuilder.
		//Mlex.flush(pos);
		//L.flush(pos);
		//Lexicon testLex = new Lexicon();
		
		String[] evan = {"Mathew","Mark","Luke","John","Thomas"};
		
		for(int i = 0; i < 1; i++)
		{
			System.out.println("*************************************");
			L.findTerm(evan[i]);
			System.out.println("{"+L.getStartOffset(0)+","+L.getStartBitOffset(0)+"}"
					+"{"+L.getEndOffset(0)+","+L.getEndBitOffset(0)+"}"
					+L.getTermId()+" "+L.getTerm()
					+L.getNt()+" "+L.getTF());
			/*testLex.findTerm(evan[i]);
			System.out.println("{"+testLex.getStartOffset()+","+testLex.getStartBitOffset()+"}"
					+"{"+testLex.getEndOffset()+","+testLex.getEndBitOffset()+"}"
					
					+testLex.getTermId()
					+" "+testLex.getTerm()
					+testLex.getNt()+" "+testLex.getTF());*/
		}

		InvertedIndex testInv = new InvertedIndex(L,
				"/users/students4/level4/kanej/TerrierProject/Terrier/terrier/var/index/data.if0");
		System.out.println("*****************************************");
		testInv.print();
		System.out.println("*****************************************");
		System.out.println("Looking "+L.findTerm("Mathew"));
		int[][] postingList = testInv.getDocuments(L.getStartOffset(0),
													L.getStartBitOffset(0),
													L.getEndOffset(0),
													L.getEndBitOffset(0));
		
		for (int i = 0; i < postingList[0].length; i++)
			System.out.println("["+postingList[0][i]+","+postingList[1][i]+"]");
		
		//testInv.print();
	}
}

package uk.ac.gla.terrier.structures.incrementalindex2.memoryindex;

import gnu.trove.THashMap;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntObjectIterator;

import java.io.IOException;
import java.util.Iterator;
import java.util.TreeMap;

import uk.ac.gla.terrier.structures.FilePosition;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.structures.LexiconEntry;
import uk.ac.gla.terrier.structures.LexiconOutputStream;
import uk.ac.gla.terrier.structures.incrementalindex2.SegmentedLexicon;
import uk.ac.gla.terrier.utility.ApplicationSetup;

public class MemoryLexicon extends Lexicon {

	protected LexiconEntry currentTermInfo = null;
	protected String currentTerm = null;
	
	protected THashMap TermRecords;
	protected TIntObjectHashMap TermidRecords;
	
	protected long numberOfPointers;
	
	protected SegmentedLexicon SL;
	
	
	
	public MemoryLexicon()
	{
		super((long)3,(long)3,(long)3);
		TermRecords = new THashMap();
		TermidRecords = new TIntObjectHashMap();
		numberOfPointers = 0;

	}
	
	public void setSegmentedLexicon(SegmentedLexicon SL)
	{
		this.SL = SL;
	}
	
	
	
	/**
	 * Closes the lexicon and flushes it to disk as a file
	 */
	public void close() {
		TermidRecords = null;
		TermRecords = null;
	}
	
	/**
	 * Writes the lexicon to file on disk
	 *
	 */
	public void flush(LexiconOutputStream los, FilePosition[] positions) {
		//TObjectObjectHashMapDecorator d = new THashMapDecorator(TermRecords);
		TreeMap tm = new TreeMap(TermRecords);
		//Set s = tm.entrySet();
		Iterator iter = tm.values().iterator();
		
		try{
			LexiconEntry temp;
			int count = 0;
			while(iter.hasNext())
			{
				temp = (LexiconEntry)iter.next();
				los.writeNextEntry(temp.term, temp.termId, temp.n_t, temp.TF,
						 positions[count].Bytes,positions[count].Bits);
				count++;
			}
			los.close();
			
			//TODO: consider lexicon index, lexicon hash. Currently done in IncrementalIndex
			
			/*(new LexiconBuilder()).createLexiconIndex(new LexiconInputStream(path),
					TermRecords.size(),
					Lexicon.lexiconEntryLength);*/
		}catch(IOException ioe){
			System.err.println("Indexing failed to write a lexicon to disk : "+ioe);
			System.exit(1);}
		
		
	}

	


	/**
	 * Finds the term given its term code.
	 *
	 * @return true if the term is found, else return false
	 * @param _termId the term's identifier
	 */
	public boolean findTerm(int _termId) {
		currentTermInfo = (LexiconEntry)TermidRecords.get(_termId);
		//System.out.println(currentTermInfo);
		return currentTermInfo != null;
	}

	/** 
	 * Performs a search in the lexicon
	 * in order to locate the given term.
	 * If the term is located, the properties
	 * termCharacters, documentFrequency,
	 * termFrequency contain the
	 * values related to the term.
	 * @param _term The term to search for.
	 * @return true if the term is found, and false otherwise.
	 */
	public boolean findTerm(String _term) {
		currentTermInfo = (LexiconEntry)TermRecords.get(_term);
		//System.out.println(currentTermInfo);
		return currentTermInfo != null;
	}

	/* Getters - all taken over from standard lexicon */ 

	
	/**
	 * Return the document frequency for the given term.
	 *
	 * @return int The document frequency for the given term
	 */
	public int getNt() {
		return currentTermInfo.n_t;
	}

	public long getNumberOfLexiconEntries() {
		return TermRecords.size();
	}


	public String getTerm() {
		return currentTermInfo.term;
	}

	public int getTermId() {
		return currentTermInfo.termId;
	}

	//Check this
	public int getTF() {
		
		return currentTermInfo.TF;
	}
	
	public long getNumberOfPointers()
	{
		return numberOfPointers;
	}
	
	//End of getters
	
	/**
	 * Seeks the i-th entry of the lexicon.
	 * @param i The index of the entry we are looking for.
	 * @return true if the entry was found, false otherwise.
	 */
	/*
	public boolean seekEntry(int i) {
			// TODO
		return false;
	}*/

	
	/**
	 * Makes suitable updates on the addition of a term
	 * Look at params again.
	 * @return the term id of the term updated. -1 if update failed.
	 */
	public int incrementEntry(String term, int TF,int Nt) {
		//System.out.println(term);
		if (! findTerm(term))
		{
			return newEntry(term, TF, Nt);
		}
		currentTermInfo.TF += TF;
		currentTermInfo.n_t += Nt;
		numberOfPointers += Nt;
		return currentTermInfo.termId;
	}
	

	
	int termidCounter = 0; 
	
	protected int newEntry(String term, int TF, int Nt)
	{
		final int termid = SL.findTermIdforTermOnDisk(term); // termidCounter++;//TermCodes.getCode(term);
		LexiconEntry entry = new LexiconEntry()/*(term, termid,TF, Nt)*/;
		entry.n_t = Nt; entry.TF = TF; entry.term = term; entry.termId = termid;
		
		TermRecords.put(term, entry);
		TermidRecords.put(termid, entry);
		currentTermInfo = entry;
		
		//currentTermInfo.collFreq += TF;
		//currentTermInfo.docFreq += Nt;
		numberOfPointers += Nt;
		return termid;
	}
	
	/**
	 * Makes suitable updates on the addition of a term. Used primarily by
	 * the negative lexicon, hence deals with termids rather than terms.
	 * 
	 * @return the term id of the term updated. -1 if update failed.
	 */
	public int incrementNegativeEntry(int termid, int TF,int Nt) {
		//System.out.println(term);
		if (! findTerm(termid))
		{
			return newNegativeEntry(termid, TF, Nt);
		}
		currentTermInfo.TF += TF;
		currentTermInfo.n_t += Nt;
		numberOfPointers += Nt;
		return currentTermInfo.termId;
	}
	
	protected int newNegativeEntry(int termid, int TF, int Nt)
	{
		LexiconEntry entry = new LexiconEntry();/*("<Neg>", termid,TF, Nt);*/
		entry.term="<Neg>"; entry.termId=termid;entry.TF=TF; entry.n_t = Nt;
		TermidRecords.put(termid, entry);
		currentTermInfo = entry;
	
		numberOfPointers += Nt;
		return termid;
	}
	

	/**
	 * Makes alterations to entry based upon deletion of
	 * document. Will delete entry itself if its frequency
	 * drops to zero.
	 * @param term
	 * @param docid
	 * @return term id of the term changed, or -1 if the term is not found
	 * @deprecated
	 */
	public int decerementEntry(String term, int TF, int Nt)
	{
		if (! findTerm(term))
		{
			return -1;
		}
		currentTermInfo.TF -= TF;
		currentTermInfo.n_t -= Nt;
		numberOfPointers -= Nt;
		return currentTermInfo.termId;
	}
	
	/**
	 * Removes an entry from the lexicon
	 * @param term
	 * @return
	 */
	public boolean deleteEntry(String term)
	{
		if(!findTerm(term))
		{
			System.err.println("Tried to delete nonexistent term from lexicon.");
			return false;
		}
		
		TermRecords.remove(term);
		TermidRecords.remove(currentTermInfo.termId);
		
		numberOfPointers -= currentTermInfo.n_t;
		
		return true;
	}
	
	public boolean deleteEntry(int termid)
	{
		if(!findTerm(termid))
		{
			System.err.println("Tried to delete nonexistent term from lexicon.");
			return false;
		}
		
		TermRecords.remove(currentTermInfo.term);
		TermidRecords.remove(termid);
		
		numberOfPointers -= currentTermInfo.n_t;
		
		return true;
	}
	
	public TIntObjectIterator iterator_TIntObject()
	{
		//TreeMap tm = new TreeMap(TermRecords);
		
		return new TIntObjectIterator(TermidRecords);//tm.values().iterator();
	}
	
	public void print()
	{
		//System.out.println("Size of termid:"+TermidRecords.size());
		TIntObjectIterator iter = new TIntObjectIterator(TermidRecords);
		
		while(iter.hasNext())
		{
		iter.advance();
		LexiconEntry temp = (LexiconEntry)iter.value();
		System.out.println(temp.term+ " termid "+temp.termId+" TF "+temp.TF+" Nt "+temp.n_t);	
		}
	}
	
}

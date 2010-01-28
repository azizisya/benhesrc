package uk.ac.gla.terrier.structures.incrementalindex;

import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.structures.incrementalindex.globallexicon.GlobalLexicon;
import uk.ac.gla.terrier.structures.incrementalindex.memoryindex.MemoryIndex;

public class ProxyLexicon extends Lexicon {
	/**This class provides a wrapper around the global lexicon and the in-memory lexicon.
	 * To outside classes it apears as a unified lexicon. It queries both and combines the result.
	 * Some methods don't make sense for a memory lexicon to call and hence only appply to the global
	 * lexicon. 
	 * Termid's are not necessarily the same in the global and in-memory lexicons for the same term.
	 * The Global lexicon takes precedence. Hence in the findTerm(int termId) method the memory lexicon
	 * uses the global lexicon to look up the term for the input termid and searchs for the term instead.
	 */
	
	protected IncrementalIndex incIndex;
	protected GlobalLexicon gLex;
	
	boolean inGlobal;
	boolean inMemory;
	
	
	public ProxyLexicon(GlobalLexicon gl, IncrementalIndex ii)
	{
		super(2,2,2);
		gLex = gl;
		incIndex = ii;
		//mLex = ml;
	}

	public void close() {
		// TODO Auto-generated method stub

	}

	public void print() {
		// TODO Auto-generated method stub

	}

	public boolean findTerm(int _termId) {
		//System.out.println("\nUsed int findTerm\n");
		 inGlobal = gLex.findTerm(_termId);
		 
		 //Termid in gLex and mLex may differ
		 //hence the mLex searches for the string term
		 //once found in the gLex.
		 if(inGlobal)
			 inMemory = incIndex.getMemIndex().getLexicon().findTerm(gLex.getTerm());
		 else
			 inMemory = incIndex.getMemIndex().getLexicon().findTerm(_termId);
		 
		 
		return inGlobal || inMemory;
	}

	public boolean findTerm(String _term) {
		//System.out.println("\nUsed string findTerm\n");
		 inGlobal = gLex.findTerm(_term);
		 inMemory = incIndex.getMemIndex().getLexicon().findTerm(_term);
		return inGlobal || inMemory;
	}

	public byte getEndBitOffset() {
		if(inGlobal)
			return gLex.getEndBitOffset();
		return -1;
	}

	
	public long getEndOffset() {
		if(inGlobal)
			return gLex.getEndOffset();
		return -1;
	}

	public int getNt() {
		int Nt = 0;
		if(inGlobal)
		{
			Nt += gLex.getNt();
		}
		if(inMemory)
		{
			Nt+= incIndex.getMemIndex().getLexicon().getNt();
		}
		
		return Nt;
	}
	
	public int getTF() {
		int TF = 0;
		if(inGlobal)
		{
			TF += gLex.getTF();
		}
		if(inMemory)
		{
			TF+= incIndex.getMemIndex().getLexicon().getTF();
		}
		
		return TF;
	}

	public long getNumberOfLexiconEntries() {
		// TODO Auto-generated method stub
		return -100;
	}

	public byte getStartBitOffset() {
		if(inGlobal)
			return gLex.getStartBitOffset();
		return -1;
	}

	public long getStartOffset() {
		if(inGlobal)
			return gLex.getStartOffset();
		return -1;
	}

	public String getTerm() {
		if(inGlobal)
			return gLex.getTerm();
		if(inMemory)
			return incIndex.getMemIndex().getLexicon().getTerm();
		return null;
	}

	public int getTermId() {
		if(inGlobal)
			return gLex.getTermId();
		if(inMemory)
			return incIndex.getMemIndex().getLexicon().getTermId();
		
		return -1;
	}



	public boolean seekEntry(int i) {
		System.err.println("This shouldn't be used, it is meaningless for this meta class.");
		return false;
	}



	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}

package uk.ac.gla.terrier.structures;

import gnu.trove.TIntObjectHashMap;
import gnu.trove.THashMap;

/** Implements a non-lossy, in-memory cache around a Lexicon */
class CachingLexicon extends Lexicon
{
	class LexiconEntry {
		public int termId;
		public String term;
		public int docFrequency;
		public int termFrequency;
		public long startOffset;
		public byte startBitOffset;
		public long endOffset;
		public byte endBitOffset;
	}

	protected final Lexicon parentLexicon;
	/** Cache for String term lookups */
	protected final THashMap termCache = new THashMap();
	/** Cache for termId lookups */
	protected final TIntObjectHashMap termIdCache = new TIntObjectHashMap();

	
    public CachingLexicon()
    {
        this(new Lexicon());
    }

	public CachingLexicon(String filename)
	{
		this(new Lexicon(filename));
	}


	public CachingLexicon(Lexicon l)
	{
		super(3,3,3);
		parentLexicon = l;
	}

	public void close()
	{
		parentLexicon.close();
		termCache.clear();
		termIdCache.clear();
	}

	public void print()
	{
		parentLexicon.print();
	}

	protected void loadEntry(final LexiconEntry le)
	{
		this.term = le.term;
		this.termId = le.termId;
		this.documentFrequency = le.docFrequency;
		this.termFrequency = le.termFrequency;
		this.startOffset = le.startOffset;
		this.startBitOffset = le.startBitOffset;
		this.endOffset = le.endOffset;
		this.endBitOffset = le.endBitOffset;
	}

	protected void createEntry()
	{
		LexiconEntry le = new LexiconEntry();
		le.term = this.term = parentLexicon.getTerm();
		le.termId = this.termId = parentLexicon.getTermId();
		le.docFrequency = this.documentFrequency = parentLexicon.getNt();
		le.termFrequency = this.termFrequency = parentLexicon.getTF();
		le.startOffset = this.startOffset = parentLexicon.getStartOffset();
		le.startBitOffset = this.startBitOffset = parentLexicon.getStartBitOffset();
		le.endOffset = this.endOffset = parentLexicon.getEndOffset();
		le.endBitOffset = this.endBitOffset = parentLexicon.getEndBitOffset();
		termCache.put(this.term, le);
		termIdCache.put(this.termId, le);
		le = null;
	}


	/** Finds the term given its term code. */
	public boolean findTerm(final int _termId)
	{
		if (termIdCache.contains(_termId))
		{
			Object rtr = termIdCache.get(_termId);
			if (rtr == null)
			{
				return false;
			}
			loadEntry((LexiconEntry)rtr);
			return true;
		}
		if (! parentLexicon.findTerm(_termId))
			return false;
		createEntry();
		return true;
	}

	/** Finds the term given the term string */
	public boolean findTerm(final String _term)
	{
		if (termCache.contains(_term))
		{
			Object rtr = termCache.get(_term);
			if (rtr == null)
			{
				return false;
			}
			loadEntry((LexiconEntry)rtr);
			return true;
		}
		if (! parentLexicon.findTerm(_term))
			return false;
		createEntry();
		return true;
	}

	public boolean seekEntry(final int i)
	{
		if (parentLexicon.seekEntry(i))
		{
			this.findTerm(parentLexicon.getTermId());
			return true;
		}
		return false;
	}

	public long getNumberOfLexiconEntries()
	{
		return parentLexicon.getNumberOfLexiconEntries();
	}

	/* Inherited: 
	public int getNt()
	public long getNumberOfLexiconEntries()
	public byte getStartBitOffset()
	public long getStartOffset()
	public String getTerm()
	public int getTermId()
	public int getTF()
	*/
}



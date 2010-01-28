package uk.ac.gla.terrier.structures;
import it.unimi.dsi.mg4j.util.SignedMinimalPerfectHash;
import it.unimi.dsi.mg4j.util.HashCodeSignedMinimalPerfectHash;
import uk.ac.gla.terrier.utility.*;
import java.io.*;
import java.util.Collection;
import java.util.Iterator;
class PerfectHashedLexicon extends Lexicon
{
	static final String HASH_SUFFIX = ".lexiconhash.oos.gz";
	final SignedMinimalPerfectHash h;
	final Lexicon parent;
	public PerfectHashedLexicon(String path, String prefix, Lexicon parent)
	{
		super(0,0,0);
		this.parent = parent;
		SignedMinimalPerfectHash tmp = null;
		try{
			ObjectInputStream ois = new ObjectInputStream(Files.openFileStream(path + prefix + ApplicationSetup.LEXICONSUFFIX+ HASH_SUFFIX));
			tmp = (SignedMinimalPerfectHash)ois.readObject();
			ois.close();
		} catch (Exception e) { logger.error("Could not load a SignedMinimalPerfectHash for lexicon", e); }
		h = tmp;
	}

	public boolean findTerm(String t)
	{
		if (h == null)
			return parent.findTerm(t);
		final int num = h.getNumber(t);
		if (num == -1)
		{
			return false;
			//or do we do parent.findTerm(t)?
		}
		parent.seekEntry(num);
		if (! t.equals(parent.getTerm()))
			return parent.findTerm(t);
		return true;
	}

	public LexiconEntry getLexiconEntry(java.lang.String _term)
	{
		if (h == null)
			return parent.getLexiconEntry(_term);
		final int num = h.getNumber(_term);
		if (num == -1)
		{
			return null;
			//or do we do parent.getLexiconEntry(t)?
		}
		return parent.getIthLexiconEntry(num);
	}
	
	public LexiconEntry getLexiconEntry(int termid)
	{
		return parent.getLexiconEntry(termid);
	}

	public LexiconEntry getIthLexiconEntry(int num)
	{
		return parent.getIthLexiconEntry(num);
	}

	public void close()
	{
		parent.close();
	}

	public boolean seekEntry(int i)
	{
		return parent.seekEntry(i);
	}
	public void print() {
		parent.print();
	}

	public Iterator<String> iterator() {
		return parent.iterator();
	}

	public int getTF()
	{
		return parent.getTF();
	}

	public int getTermId() {
		return parent.getTermId();
	}

	public String getTerm() {
		return parent.getTerm();
	}

	public int getNt() {
		return parent.getNt();
	}

	public long getNumberOfLexiconEntries(){
		return parent.getNumberOfLexiconEntries();
	}

	public  long 	getStartOffset() {
		return parent.getStartOffset();
	}

	public byte 	getStartBitOffset() {
		return parent.getStartBitOffset();
	}

	public long	getEndOffset() {
		return parent.getEndOffset();
	}

	public  byte 	getEndBitOffset() {
		return parent.getEndBitOffset();
	}


	public static void createPerfectHash(Index index) throws Exception
	{
		final Lexicon l = index.getLexicon();
		Collection c = new Collection<String>()
		{
			public boolean  add(String o)
			{throw new UnsupportedOperationException();}
			public boolean  addAll(Collection<? extends String> c)
			{throw new UnsupportedOperationException();}
			public void	 clear()
			{throw new UnsupportedOperationException();}
			public boolean  contains(Object o)
			{throw new UnsupportedOperationException();}
			public boolean  containsAll(Collection<?> c)
			{throw new UnsupportedOperationException();}
			public boolean  equals(Object o)
			{throw new UnsupportedOperationException();}
			public boolean  isEmpty()
			{return size() == 0;}
			public boolean  remove(Object o)
			{throw new UnsupportedOperationException();}
			public boolean  removeAll(Collection<?> c)
			{throw new UnsupportedOperationException();}
			public boolean  retainAll(Collection<?> c)
			{throw new UnsupportedOperationException();}
			public Iterator<String>	 iterator()
			{
				return l.iterator();
			}
			public int size()
			{
				return (int)l.getNumberOfLexiconEntries();
			}
			public Object[]	 toArray()
			{throw new UnsupportedOperationException();}
			public <String>String[] toArray(String[] a)
			{throw new UnsupportedOperationException();}
		};
		HashCodeSignedMinimalPerfectHash h = new HashCodeSignedMinimalPerfectHash(c);
		String path = index.getPath();
		String prefix = index.getPrefix();
		final ObjectOutputStream oos = new ObjectOutputStream(Files.writeFileStream(path +ApplicationSetup.FILE_SEPARATOR+ prefix +HASH_SUFFIX));
		oos.writeObject(h);
		oos.close();
		index.addIndexStructure("reallexicon",
			index.getIndexProperty("index.lexicon.class", "uk.ac.gla.terrier.structures.Lexicon"),
			index.getIndexProperty("index.lexicon.parameter_types", "java.lang.String,java.lang.String"),
			index.getIndexProperty("index.lexicon.parameter_values", "path,prefix"));
		index.addIndexStructure("lexicon", 
			"uk.ac.gla.terrier.structures.PerfectHashedLexicon",
			"java.lang.String,java.lang.String,uk.ac.gla.terrier.structures.Lexicon", 
			"path,prefix,reallexicon");
		l.close();
	}

	public static void main (String[] args) throws Exception
	{
		if (args[0].equals("-create"))
		{
			Index i = args.length > 1 ? Index.createIndex(args[1], args[2]) : Index.createIndex();
			createPerfectHash(i);
		}
	}
}

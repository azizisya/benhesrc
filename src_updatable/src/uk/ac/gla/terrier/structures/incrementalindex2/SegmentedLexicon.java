/**
 * 
 */
package uk.ac.gla.terrier.structures.incrementalindex2;

import gnu.trove.TIntArrayList;

import java.io.IOException;

import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.structures.LexiconEntry;
import uk.ac.gla.terrier.structures.LexiconInputStream;
import uk.ac.gla.terrier.structures.incrementalindex2.memoryindex.MemoryIndex;
import uk.ac.gla.terrier.utility.ApplicationSetup;

/**
 * @author craigm
 *
 */
public class SegmentedLexicon extends Lexicon {
	
	public int numberOfTerms= 0;

	int numberOfSegments;
	TIntArrayList segmentIds; String path; String prefix; MemoryIndex MI;
	
	Lexicon[] lexicons;
	
	public SegmentedLexicon(TIntArrayList segmentIds, String path, String prefix, MemoryIndex MI, int numTerms)
	{
		super(3,3,3);
		this.segmentIds = segmentIds;
		this.path=path;
		this.prefix=prefix;
		this.MI = MI;
		this.numberOfTerms = numTerms;
		
		//reOpenSegments();
	}
	public Lexicon get(int i)
	{
		return lexicons[i];
	}
	
	public void setMemoryIndex(MemoryIndex mi)
	{
		MI = mi;
	}
	
	public void reOpenSegments()
	{
		if (lexicons != null)
		{
			for(int i=0;i<lexicons.length;i++)
				if (lexicons[i] != null)
				{
					lexicons[i].close();
					lexicons[i] = null;
				}
		}
		
		final int[] segmentIds = this.segmentIds.toNativeArray();
		System.err.println("SegmentedLexicon attempting to open "+segmentIds.length+" segments");
		numberOfSegments = segmentIds.length;
		lexicons = new Lexicon[numberOfSegments];
		for(int i=0;i<numberOfSegments;i++)
		{
			if (segmentIds[i] == -1)
				continue;
			lexicons[i] = new Lexicon(
				path + ApplicationSetup.FILE_SEPARATOR + 
				prefix + "_" + segmentIds[i]+
				ApplicationSetup.LEXICONSUFFIX);
		}
	}
	

	public void close() {
		if (lexicons != null)
		{
			for(int i=0;i<lexicons.length;i++)
				if (lexicons[i] != null)
				{
					lexicons[i].close();
					lexicons[i] = null;
				}
		}
	}


	
	public boolean findTerm(int _termId) {
		// TODO Auto-generated method stub
		return super.findTerm(_termId);
	}
	

	
	public boolean findTerm(String _term) {
		// TODO Auto-generated method stub
		return super.findTerm(_term);
	}
	
	public int findTermIdforTermOnDisk(String _term)
	{
		for(int i=0;i<numberOfSegments;i++)
		{
			LexiconEntry seg_le = lexicons[i].getLexiconEntry(_term);
			if (seg_le != null)
				return seg_le.termId;
		}
		return numberOfTerms++;
	}
	
	public int getNumberOfEntries()
	{
		return numberOfTerms;
	}
	
	public LexiconEntry getLexiconEntry(int termid) {
		
		SegmentedLexiconEntry le = new SegmentedLexiconEntry();
		
		for(int i=0;i<numberOfSegments;i++)
		{
			LexiconEntry seg_le = lexicons[i].getLexiconEntry(termid);
			if (seg_le != null)
				le.add(i, seg_le);/* adds the statistics, appends the offsets */
		}
		
		Lexicon mLex =MI.getLexicon(); 
		if (mLex.findTerm(termid))
		{
			le.TF += mLex.getTF();
			le.n_t += mLex.getNt();
		}
		
		
		if (le.TF == 0)
			return null;
		return le;
	}
	
	public LexiconEntry getLexiconEntry(String _term) {
		
		SegmentedLexiconEntry le = new SegmentedLexiconEntry();
		
		for(int i=0;i<numberOfSegments;i++)
		{
			LexiconEntry seg_le = lexicons[i].getLexiconEntry(_term);
			if (seg_le != null)
				le.add(i, seg_le);/* adds the statistics, appends the offsets */
		}
		
		Lexicon mLex =MI.getLexicon(); 
		if (mLex.findTerm(_term))
		{
			le.TF += mLex.getTF();
			le.n_t += mLex.getNt();
		}
		
		
		if (le.TF == 0)
			return null;
		return le;
	}
	

	
	public long getNumberOfLexiconEntries() {
		// TODO: expensive, involves hashset operation?
		return -1; 
	}
	
	
	
	
	public LexiconInputStream getLexiconInputStream() throws IOException
	{
		final int[] segmentIds = this.segmentIds.toNativeArray();
		LexiconInputStream lexIns[] = new LexiconInputStream[numberOfSegments];
		for (int i=0;i<numberOfSegments;i++)
		{
			lexIns[i] = new LexiconInputStream(path + ApplicationSetup.FILE_SEPARATOR + 
					prefix + "_" + segmentIds[i]+
					ApplicationSetup.LEXICONSUFFIX);
		}
		return new MultiLexiconInputStream(lexIns);
	}
	
	public LexiconInputStream getLexiconInputStream2(int a, int b) throws IOException
	{
		final int[] segmentIds = this.segmentIds.toNativeArray();
		LexiconInputStream lexIns[] = new LexiconInputStream[2];
		lexIns[0] = new LexiconInputStream(path + ApplicationSetup.FILE_SEPARATOR + 
					prefix + "_" + segmentIds[a]+
					ApplicationSetup.LEXICONSUFFIX);
		lexIns[1] = new LexiconInputStream(path + ApplicationSetup.FILE_SEPARATOR + 
				prefix + "_" + segmentIds[b]+
				ApplicationSetup.LEXICONSUFFIX);
		
		return new MultiLexiconInputStream(lexIns);
	}
	
	
}

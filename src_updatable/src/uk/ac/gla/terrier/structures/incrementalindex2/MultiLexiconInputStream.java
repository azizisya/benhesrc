package uk.ac.gla.terrier.structures.incrementalindex2;

import java.io.IOException;
import java.util.Arrays;
import java.util.PriorityQueue;

import uk.ac.gla.terrier.structures.FilePosition;
import uk.ac.gla.terrier.structures.LexiconInputStream;

/** Combines two or more LexiconInputStreams into a single LexiconInputStreams
 * using a priority queue.
 * <p>
 * Notes:
 * <li>This class assumes that the starting offset for the first term in each
 * stream is (0,0), and that also for each stream, the end offset for a term
 * is the start offset of the subsequent term. 
 * @author craigm
 */
public class MultiLexiconInputStream extends LexiconInputStream {

	protected final int lexCount;
	protected final LexiconInputStream[] lexInStream;
	protected final boolean[] hasMore;
	protected final PriorityQueue<String> termQ;
	
	protected boolean[] Indices;
	protected FilePosition[] EndOffSets;
	protected FilePosition[] StartOffSets;
	
	public MultiLexiconInputStream(LexiconInputStream[] lexIns) throws IOException
	{
		super(3,3,3);
		this.lexInStream = lexIns;
		lexCount = lexIns.length;
		hasMore = new boolean[lexCount];
		Indices = new boolean[lexCount];
		EndOffSets = new FilePosition[lexCount];
		StartOffSets = new FilePosition[lexCount];
		termQ = new PriorityQueue<String>(lexCount);
        for(int i=0;i<lexCount;i++)
        {
        	StartOffSets[i] = new FilePosition();
            hasMore[i] = lexInStream[i].readNextEntry() > 0;
            EndOffSets[i] = new FilePosition(
            	lexInStream[i].getEndOffset(),
            	lexInStream[i].getEndBitOffset()
            	);
            termQ.add(lexInStream[i].getTerm());
        }
	}
	
	@Override
	public void close() {
		for(int i=0;i<lexCount;i++)
			lexInStream[i].close();
	}
	
	public boolean[] getSegmentsIndex()
	{
		return Indices;
	}
	
	public int readNextEntry() throws IOException {
		
		if (termQ.size() == 0)
			return 0;
		termFrequency = documentFrequency =0;
		term = termQ.peek(); 
		Arrays.fill(Indices, false);
		for(int i=0;i<lexCount;i++) {
			if (hasMore[i] && lexInStream[i].getTerm().equals(term))
			{
				termQ.poll(); Indices[i] = true;
				termFrequency += lexInStream[i].getTF();
				documentFrequency += lexInStream[i].getNt();
				termId = lexInStream[i].getTermId();
				hasMore[i] = lexInStream[i].readNextEntry() > 0;
				StartOffSets[i] = new FilePosition(
					lexInStream[i].getStartOffset(),
                    lexInStream[i].getStartBitOffset()
				);
				EndOffSets[i] = new FilePosition(
					lexInStream[i].getEndOffset(),
	                lexInStream[i].getEndBitOffset()
                );
				if (hasMore[i])
				{
					termQ.add(lexInStream[i].getTerm());
					//StartOffSets[i] = EndOffSets[i];
					//EndOffSets[i] = new FilePosition(lexInStream[i].getEndOffset(), lexInStream[i].getEndBitOffset());
				}
			}
		}
		if (termFrequency > 0)
			return 1;
		return 0;
	
	}
	
	public String getTerm()
	{
		return term;
	}
	
	public FilePosition getStartOffset(int i)
	{
		return StartOffSets[i];
	}
	
	public long getStartOffsetBytes(int i)
	{
		return StartOffSets[i].Bytes;
	}

	public byte getStartOffsetBits(int i)
	{
		return StartOffSets[i].Bits;
	}
	
	public FilePosition getEndOffset(int i)
	{
		return EndOffSets[i];
	}
	
	public long getEndOffsetBytes(int i)
	{
		return EndOffSets[i].Bytes;
	}

	public byte getEndOffsetBits(int i)
	{
		return EndOffSets[i].Bits;
	}
	
	public int readNextEntryBytes() throws IOException {
		//TODO change to use readNextEntryBytes
		return this.readNextEntry();
	}
	
	
}

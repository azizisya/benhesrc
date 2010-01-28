package uk.ac.gla.terrier.structures.incrementalindex.globallexicon;

import uk.ac.gla.terrier.structures.FilePosition;
import uk.ac.gla.terrier.structures.LexiconInputStream;
import uk.ac.gla.terrier.structures.trees.bplustree.*;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntArrayList;

public class GlobalLexiconInputStream extends LexiconInputStream
{
	protected BplusLeafNode iteratingCurrentLeafNode = null;
	protected BplusValue[] iteratingCurrentLexiconRecords = null;
	protected int currentLexiconRecordIndex = -1;
	protected LexiconRecord iteratingCurrentLexiconRecord = null;
	protected boolean leafNodeChanged = false;
	protected BplusTree tree;
	
	public int nodeCount =0;//TODO remove. this is superfluous

	public GlobalLexiconInputStream(BplusTree tree)
	{
		super(3,3,3);
		this.tree = tree;
	}

	/** Reads the next entry of the lexicon.
	  * @return Returns 1 if the next entry was successfully read, 
	  * or -1 if the end of the lexicon has been reached.
	  */
	public int readNextEntry()
	{
		//if first time readNextEntry is called
		if (iteratingCurrentLeafNode == null)
		{
			iteratingCurrentLeafNode = tree.getFirstLeaf();
			//System.out.println("First leaf is "+iteratingCurrentLeafNode);	
			if (iteratingCurrentLeafNode == null)
				return -1; //empty lexicon

			iteratingCurrentLexiconRecords = iteratingCurrentLeafNode.getValues();
			System.out.println("leaf SIZE is "+iteratingCurrentLexiconRecords.length);
			currentLexiconRecordIndex = 0;
			nodeCount++;
			iteratingCurrentLexiconRecord = (LexiconRecord)iteratingCurrentLexiconRecords[currentLexiconRecordIndex];
		}
		else
		{
			//check for update of lexicon record
			//if (leafNodeChanged)
			//{	//record was changed: update
				//System.err.println("Record changed for "+iteratingCurrentLexiconRecord.getTerm() + " : forcing update");
				//tree.updateValue(new LexiconStringKey(iteratingCurrentLexiconRecord.getTerm()),iteratingCurrentLexiconRecord);
				//tree.updateValue(new LexiconStringKey(iteratingCurrentLexiconRecord.getTerm()),iteratingCurrentLexiconRecord);
				//iteratingCurrentLeafNode.updateValues(); 
				//leafNodeChanged = false;
			//}

			currentLexiconRecordIndex++; 
			if (currentLexiconRecordIndex == iteratingCurrentLexiconRecords.length)
			{	//read all of this record, return another record
				
				if (leafNodeChanged)
				{
					iteratingCurrentLeafNode.updateValues();
					leafNodeChanged = false;
				}
				
				//System.out.println("Has there been an update? "+iteratingCurrentLeafNode);
				//BplusNode temp = iteratingCurrentLeafNode;
				iteratingCurrentLeafNode = iteratingCurrentLeafNode.getNext();
				
				//System.out.println("Has there been an update? "+temp);
				
				if (iteratingCurrentLeafNode == null)
					return -1;//end of lexicon
				nodeCount++;
				iteratingCurrentLexiconRecords = iteratingCurrentLeafNode.getValues();
				//System.out.println("!!! Working on leaf starting with "+((LexiconRecord)iteratingCurrentLexiconRecords[0]).term);
				currentLexiconRecordIndex = 0;
			}
			iteratingCurrentLexiconRecord = (LexiconRecord)iteratingCurrentLexiconRecords[currentLexiconRecordIndex];
		}
		return 1;
	}
	
	/** Close this lexicon input stream. Saves any change made to the most recently
	  * read lexicon record if needed. */
	public void close(){
		if (leafNodeChanged)
		{
			//tree.updateValue(new LexiconStringKey(iteratingCurrentLexiconRecord.getTerm()),iteratingCurrentLexiconRecord);
			//tree.updateValue(new LexiconStringKey(iteratingCurrentLexiconRecord.getTerm()),iteratingCurrentLexiconRecord);
			iteratingCurrentLeafNode.updateValues();
			leafNodeChanged = false;
		}
	}
	
	// Set methods

	public void addSegment(int segNumber, FilePosition sOffset, FilePosition eOffset)
	{
		iteratingCurrentLexiconRecord.addSegment(segNumber, sOffset, eOffset);
		leafNodeChanged = true;
	}
	public void removeSegment(int segNumber)
	{
		iteratingCurrentLexiconRecord.removeSegment(segNumber);
		leafNodeChanged = true;
	}

	public String recordToString()
	{
		return iteratingCurrentLexiconRecord.toString();
	}

	// Get methods

	public int[] getSegmentIds()
	{
		return iteratingCurrentLexiconRecord.getSegmentsIds();
	}

	public TIntHashSet getSegmentIdsHash()
	{
		return iteratingCurrentLexiconRecord.getSegmentIdsHash();
	}

	public int getTermId()
	{
		return iteratingCurrentLexiconRecord.getTermid();
	}
	
	public byte getEndBitOffset(int SegmentNumber) {
		return iteratingCurrentLexiconRecord.getEndOffset(SegmentNumber).Bits;
	}
	public long getEndBytesOffset(int SegmentNumber)
	{
		return iteratingCurrentLexiconRecord.getEndOffset(SegmentNumber).Bytes;
	}

	public FilePosition getEndOffset(int SegmentNumber)
	{
		return iteratingCurrentLexiconRecord.getEndOffset(SegmentNumber);
	}
		
	public byte getStartBitOffset(int SegmentNumber)
	{
		return iteratingCurrentLexiconRecord.getStartOffset(SegmentNumber).Bits;
	}

	public long getStartBytesOffset(int SegmentNumber)
	{
		return iteratingCurrentLexiconRecord.getStartOffset(SegmentNumber).Bytes;
	}

	public FilePosition getStartOffset(int SegmentNumber)
	{
		return iteratingCurrentLexiconRecord.getStartOffset(SegmentNumber);
	}

	public int getTF()
	{
		return iteratingCurrentLexiconRecord.getTF();
	}

	public int getNt()
	{
		return iteratingCurrentLexiconRecord.getNt();
	}

	public String getTerm()
	{
		return iteratingCurrentLexiconRecord.getTerm();
	}

	public static void main(String args[])
	{
		GlobalLexicon lexTree = new GlobalLexicon(2);
		lexTree.newEntry("term1", 1, 1, 0, new FilePosition(0,(byte)0), new FilePosition(1,(byte)1));
		lexTree.newEntry("term2", 1, 1, 0, new FilePosition(0,(byte)0), new FilePosition(1,(byte)1));
		lexTree.findTerm("term1");
        if (lexTree.getTerm().equals("term1"))
        {
            if (lexTree.getStartOffset(0) == 0 && lexTree.getStartBitOffset(0) == 0
                && lexTree.getEndOffset(0) == 1 && lexTree.getEndBitOffset(0) == 1)
            {
                System.out.println("Initial write OK");
            }
            else
            {
                System.out.println("Initial write Failed");
            }
        }
        else
        {
            System.out.println("Initial write: Failed to find term1 again");
        }

		GlobalLexiconInputStream stream = (GlobalLexiconInputStream)lexTree.getLexiconInputStream();
		System.out.println("STREAM OPEN");
		while(stream.readNextEntry() != -1)
		{
			System.out.println("STREAM READ "+stream.getTerm() + " = "+ stream.getStartOffset(0) + " : "+ stream.getEndOffset(0));
			stream.removeSegment(0);
			stream.addSegment(1, new FilePosition(2,(byte)2), new FilePosition(4,(byte)4));
			System.out.println("STREAM WRITTEN TO, expect update");
		}
		System.out.println("STREAM CLOSE");
		stream.close();
		System.out.println("term1 found? "+lexTree.findTerm("term1"));
		if (lexTree.getTerm().equals("term1"))
		{
			if (lexTree.getStartOffset(1) == 2 && lexTree.getStartBitOffset(1) == 2	
				&& lexTree.getEndOffset(1) == 4 && lexTree.getEndBitOffset(1) == 4)
			{
				System.out.println("OK");
			}
			else
			{
				System.out.println("Failed : offsets are {"+lexTree.getStartOffset(0)+","+lexTree.getStartBitOffset(0)+"} : {"+lexTree.getEndOffset(0)+","+lexTree.getEndBitOffset(0)+"}" );
			}
		}
		else
		{
			System.out.println("Failed to find term1 again");
		}
	}
	
}

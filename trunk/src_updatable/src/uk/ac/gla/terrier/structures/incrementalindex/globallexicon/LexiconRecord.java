package uk.ac.gla.terrier.structures.incrementalindex.globallexicon;


import java.io.DataInput;
import java.io.DataOutput;
import java.nio.ByteBuffer;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntHashSet;

import uk.ac.gla.terrier.structures.FilePosition;
import uk.ac.gla.terrier.structures.incrementalindex.IncrementalIndexProperties;
import uk.ac.gla.terrier.structures.trees.bplustree.BplusValue;
import uk.ac.gla.terrier.utility.ApplicationSetup;

public class LexiconRecord extends MemoryLexiconRecord implements BplusValue {

	protected static final int MaxNumberOfSegments = IncrementalIndexProperties.Max_number_Of_Segments;
	
	/** Maximum length of a term in characters */
	protected static final int maxlength = ApplicationSetup.MAX_TERM_LENGTH;
	/*
	protected String term;
	protected int termid;
	protected int Nt;
	protected int TF;
	*/
	
	protected static byte[] BufferArray = new byte[maxlength*2];
	
	protected FilePosition[] startOffset; 
	protected FilePosition[] endOffset; 
	
	public LexiconRecord()
	{
		startOffset = new FilePosition[MaxNumberOfSegments];
		endOffset = new FilePosition[MaxNumberOfSegments];
	}

	public LexiconRecord(String t, int td, int collfreq, int docfreq)
	{
		this();
			
		term = t;
		termid = td;
		Nt = docfreq;
		TF = collfreq;
	}
		
	public LexiconRecord(String t, int td, int collfreq, int docfreq, 
			int segNumber, FilePosition sOffset, FilePosition eOffset)
	{
		this(t,td,collfreq,docfreq);
		
		startOffset[segNumber]= sOffset;
		endOffset[segNumber] = eOffset;
	}
		
	public int getNt() {return Nt;}
	public int getTF() {return TF;}
	public int getTermid() {return termid;}
	public String getTerm() {return term;}

	public int[] getSegmentsIds()
	{
		TIntArrayList segmentIndex = new TIntArrayList(MaxNumberOfSegments);
		for(int i = 0; i < MaxNumberOfSegments; i++)
		{
			if (startOffset[i] != null)
				segmentIndex.add(i);
		}
		return segmentIndex.toNativeArray();
	}

	public TIntHashSet getSegmentIdsHash()
	{
	   TIntHashSet segmentIndex = new TIntHashSet(MaxNumberOfSegments);
		for(int i = 0; i < MaxNumberOfSegments; i++)
		{
			if (startOffset[i] != null)
				segmentIndex.add(i);
		}
		return segmentIndex;
	}

		
	public void addSegment(int segNumber, FilePosition sOffset, FilePosition eOffset)
	{
		startOffset[segNumber]= sOffset;
		endOffset[segNumber] = eOffset;
	}

	public void removeSegment(int segNumber)
	{
		startOffset[segNumber] = endOffset[segNumber] = null;
	}
		
	public FilePosition[][] getSegments()
	{
		FilePosition[][] outArray =  new FilePosition[MaxNumberOfSegments][2];
		for(int i = 0; i < MaxNumberOfSegments; i++)
		{
			outArray[i][0] = startOffset[i];
			outArray[i][1] = endOffset[i];
		}
		return outArray;
	}
	
	public FilePosition getStartOffset(int segNumber)
	{
		return startOffset[segNumber];
	}
		
	public FilePosition getEndOffset(int segNumber)
	{
		return endOffset[segNumber];
	}
		
		
		
	public String toString()
	{
		String out = "["+term+","+termid+","+Nt+","+TF;
		for(int i = 0; i < MaxNumberOfSegments; i++)
			out+="{"+startOffset[i]+","+endOffset[i]+"}";
		
		return out+"]";
	}
		
	public static int sizeInBytes()
	{
		return (2*maxlength)+(3*4)+(2*MaxNumberOfSegments*FilePosition.sizeInBytes());
	}
	
	public void toBytes(DataOutput dao)
	{
		//System.err.println("Writing update to lexicon for term "+term);
		if (term.equals("term1"))
		{
			System.err.println("Writing to lexicon the record for term "+term);
			System.err.println(toString());
		}
		try{
			//ByteBuffer buffer = ByteBuffer.allocate(sizeInBytes());
			/*
			for (int i = 0; i < term.length() && i < maxlength; i++)
				dao.writeChar(term.charAt(i));
		
			for(int i = term.length(); i < maxlength; i++)
				dao.writeChar(' ');*/
			
			dao.writeChars(term);
			//now pad upto 2* maxlength bytes
			dao.write(BufferArray, 0, 2* (maxlength - term.length()));
		
			dao.writeInt(termid);
			dao.writeInt(Nt);
			dao.writeInt(TF);
		
			for(int i = 0; i < MaxNumberOfSegments; i++ )
				if(startOffset[i] == null)
				{
					//This is dependent on the FilePosition implemnetation
					//assumed to be long followed by a byte
					dao.writeLong(Long.MAX_VALUE);
					dao.writeByte(Byte.MAX_VALUE);
					dao.writeLong(Long.MAX_VALUE);
					dao.writeByte(Byte.MAX_VALUE);
				}
				else
				{
					startOffset[i].toBytes(dao);
					endOffset[i].toBytes(dao);
				}
			//return buffer.array();
		}catch(Exception e){e.printStackTrace();}
	}
		
	public void build(DataInput dai)
	{
		try{
			//ByteBuffer buffer = ByteBuffer.allocate(keyAsBytes.length);
			//buffer.put(keyAsBytes);
			//buffer.rewind();
		
			//extract term
			StringBuilder termBuilder = new StringBuilder();
			while(/*buffer.remaining() > 0 &&*/ termBuilder.length() < maxlength)
				termBuilder.append(dai.readChar());
			
			term = termBuilder.toString().trim();
			
			termid = dai.readInt();
			Nt = dai.readInt();
			TF = dai.readInt();
			
			//byte[] offsetAsBytes = new byte[FilePosition.sizeInBytes()];
			//byte[] postingSizeAsBytes = new byte[FilePosition.sizeInBytes()];
			
			FilePosition offset = new FilePosition();
			FilePosition postingSize = new FilePosition();
			
			for(int i = 0; i < MaxNumberOfSegments; i++ )
			{
				//buffer.get(offsetAsBytes);
				//buffer.get(postingSizeAsBytes);
				
				offset.build(dai);
				postingSize.build(dai);
				
				
				if(!(offset.Bytes == Long.MAX_VALUE && offset.Bits == Byte.MAX_VALUE))
				{
					startOffset[i] = new FilePosition(offset.Bytes, offset.Bits);
					endOffset[i] = new FilePosition(postingSize.Bytes,postingSize.Bits);
				}
				else
				{
					startOffset[i] = null;
					endOffset[i] = null;
				}
			}
			
		}catch(Exception e){e.printStackTrace();}
			
	}

		
	public String getClassName()
	{
		return "uk.ac.gla.terrier.structures.globallexicon.incrementalindex.LexiconRecord";
	}
		
	public static void main(String[] args)
	{
		FilePosition pos1 = new FilePosition((long)0,(byte)0);
		FilePosition pos2 = new FilePosition(180,(byte)0);
		
		LexiconRecord rec = new LexiconRecord("John",100,2,2,0,pos1,pos2);
		
		rec.addSegment(3,new FilePosition(240,(byte)0), new FilePosition(340,(byte)0));
		
		System.out.println(rec);
		
		LexiconRecord rec2 = new LexiconRecord();
		//rec2.build(rec.toBytes());
		System.out.println(rec2);
		
		System.out.println(rec.toString().equals(rec2.toString()));
		//System.out.println(LexiconRecord.sizeInBytes() == rec.toBytes().length);
	}
		

}
	
	


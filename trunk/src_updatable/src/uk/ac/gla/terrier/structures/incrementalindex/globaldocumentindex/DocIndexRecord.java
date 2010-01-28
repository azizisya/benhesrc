package uk.ac.gla.terrier.structures.incrementalindex.globaldocumentindex;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import uk.ac.gla.terrier.structures.FilePosition;
import uk.ac.gla.terrier.structures.trees.bplustree.BplusValue;
import uk.ac.gla.terrier.utility.ApplicationSetup;

public class DocIndexRecord extends MemoryDocIndexRecord implements BplusValue {
	
	protected static int maxlength = ApplicationSetup.MAX_TERM_LENGTH;
	
	protected static final int MaxNumberOfSegments = 6;
	/*
	public int docid;
	public int docLength;
	public String docno;
	*/
	protected FilePosition startOffset;
	protected FilePosition endOffset;
	
	protected int segment;
	
	protected static byte[] BufferArray = new byte[maxlength*2];
	
	
	public DocIndexRecord()
	{

	}
	
	public DocIndexRecord(int idi, int l, String sdi, 
			int segNumber, FilePosition sOffset, FilePosition eOffset)
	{
		this();
		
		docid = idi;
		docLength = l;
		docno = sdi;
		
		startOffset= sOffset;
		endOffset = eOffset;
		segment = segNumber;
	}
	
	public String toString()
	{
		return docid+" "+docLength+" "+docno+" "+startOffset+" "+endOffset;
	}
	
	public static int sizeInBytes()
	{
		return (2*maxlength)+(2*4)+(2*FilePosition.sizeInBytes());
	}
	
	public int getSegment()
	{

		return segment;
	}
	
	public FilePosition getStartOffset()
	{
		return startOffset;
	}
	
	public FilePosition getEndOffset()
	{
		return endOffset;
	}
	
	public void toBytes(DataOutput dao)
	{
		try{
		
		dao.writeInt(docid);
		dao.writeInt(docLength);
		
		/*
		for (int i = 0; i < docno.length() && i < maxlength; i++)
			dao.writeChar(docno.charAt(i));
		
		for(int i = docno.length(); i < maxlength; i++)
			dao.writeChar(' ');
		*/
		dao.writeChars(docno);
		//now pad upto 2* maxlength bytes
		dao.write(BufferArray, 0, 2* (maxlength - docno.length()));
		
		
		startOffset.toBytes(dao);
		endOffset.toBytes(dao);
		
		}catch(Exception e){e.printStackTrace();}
	}
	
	public void build(DataInput dai)
	{
		try{
		
		docid = dai.readInt();
		docLength = dai.readInt();
		
		//extract docno
		StringBuilder docnoBuilder = new StringBuilder();
		while(/*buffer.remaining() > 0 && */docnoBuilder.length() < maxlength)
			docnoBuilder.append(dai.readChar());
		
		docno = docnoBuilder.toString().trim();
		
		startOffset = new FilePosition();
		startOffset.build(dai);
		
		
		endOffset = new FilePosition();
		endOffset.build(dai);
		
		}catch(Exception e){e.printStackTrace();}
	}

	public String getClassName() {
		
		return "uk.ac.gla.terrier.structures.incrementalindex.globaldocumentindex.DocIndexRecord";
	}
	
	public static void main(String[] args)
	{
		try{
		RandomAccessFile raf = new RandomAccessFile(ApplicationSetup.TERRIER_INDEX_PATH+"/testFile","rw");
		FilePosition pos1 = new FilePosition((long)0,(byte)0);
		FilePosition pos2 = new FilePosition(180,(byte)0);
		
		DocIndexRecord rec = new DocIndexRecord(5,100,"5",0,pos1,pos2);
		rec.toBytes(raf);
		
		System.out.println(rec);
		
		DocIndexRecord rec2 = new DocIndexRecord();
		raf.seek(0);
		rec2.build(raf);
		//rec2.build(rec.toBytes());
		System.out.println(rec2);
		
		System.out.println(rec.toString().equals(rec2.toString()));
		
		}catch(Exception e){e.printStackTrace();}
		//System.out.println(DocIndexRecord.sizeInBytes() == rec.toBytes().length);
	}

}

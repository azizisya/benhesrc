package uk.ac.gla.terrier.structures.incrementalindex.globaldocumentindex;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.structures.trees.bplustree.*;

public class DocIndexStringKey extends BplusKey {
	
	String key;
	
	static int maxlength = ApplicationSetup.MAX_TERM_LENGTH;
	
	protected static byte[] BufferArray = new byte[maxlength*2];
	
	public DocIndexStringKey()
	{
		
	}
	
	public DocIndexStringKey(String k)
	{
		key = k;
	}
	
	public int compareTo(Object o)
	{
		return key.compareTo(((DocIndexStringKey)o).key);
	}
	
	public String toString()
	{
		return key;
	}
	
	public void toBytes(DataOutput dao)
	{
		try{
		//ByteBuffer buffer = ByteBuffer.allocate(maxlength*2);
		
		int i;
		
		/*
		for (i = 0; i < key.length() && i < maxlength; i++)
			dao.writeChar(key.charAt(i));
		
		for(;i < maxlength; i++)
			dao.writeChar(' ');*/
		
		
		dao.writeChars(key);
		//now pad upto 2* maxlength bytes
		dao.write(BufferArray, 0, 2* (maxlength - key.length()));
	
		}catch(Exception e){e.printStackTrace();}
	}
	
	public void build(DataInput dai)
	{
		try{
		//ByteBuffer buffer = ByteBuffer.allocate(keyAsBytes.length);
		//buffer.put(keyAsBytes);
		//buffer.rewind();
		
		//key = "";
		StringBuilder keyBuilder = new StringBuilder();
		while(/*buffer.remaining() > 0 &&*/ keyBuilder.length() < maxlength)
			keyBuilder.append(dai.readChar());
		
		key = keyBuilder.toString().trim();
		
		}catch(Exception e){e.printStackTrace();}
	}
	
	public String getClassName()
	{
		return "uk.ac.gla.terrier.structures.incrementalindex.globaldocumentindex.DocIndexStringKey";
	}
	
	public static int sizeInBytes()
	{
		return maxlength*2;
	}
	
	public static void main(String[] args)
	{
		try{
		RandomAccessFile raf = new RandomAccessFile(ApplicationSetup.TERRIER_INDEX_PATH+"/testFile","rw");
		DocIndexStringKey k = new DocIndexStringKey("wowwowwow");
		k.toBytes(raf);
		
		raf.seek(0);
		DocIndexStringKey l = new DocIndexStringKey();
		l.build(raf);
		
		System.out.println(k+" "+l);
		System.out.println(k.compareTo(l) == 0);
		
		
		
		}catch(Exception e){e.printStackTrace();}
	
	}
}

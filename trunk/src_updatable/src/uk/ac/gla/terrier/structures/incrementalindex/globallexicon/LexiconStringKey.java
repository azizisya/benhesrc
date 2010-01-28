package uk.ac.gla.terrier.structures.incrementalindex.globallexicon;

import java.io.DataInput;
import java.io.DataOutput;
import java.nio.ByteBuffer;

import uk.ac.gla.terrier.structures.trees.bplustree.*;
import uk.ac.gla.terrier.utility.ApplicationSetup;


public class LexiconStringKey extends BplusKey {
	
	String key;
	
	static int maxlength = ApplicationSetup.MAX_TERM_LENGTH;
	
	protected static byte[] BufferArray = new byte[maxlength*2];
	
	public LexiconStringKey()
	{
		
	}
	
	public LexiconStringKey(String k)
	{
		key = k;
	}
	
	public int compareTo(Object o)
	{
		return key.compareTo(((LexiconStringKey)o).key);
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
		
		while(i < maxlength)
			dao.writeChar(' ');
			*/
		dao.writeChars(key);
		//now pad upto 2* maxlength bytes
		dao.write(BufferArray, 0, 2* (maxlength - key.length()));
		
		//return buffer.array();
		}catch(Exception e){e.printStackTrace();}
	}
	
	public void build(DataInput dai)
	{
		try{
		//ByteBuffer buffer = ByteBuffer.allocate(keyAsBytes.length);
		//buffer.put(keyAsBytes);
		//buffer.rewind();

		StringBuilder keyBuilder = new StringBuilder();
		while(/*buffer.remaining() > 0 &&*/ keyBuilder.length() < maxlength)
			keyBuilder.append(dai.readChar());
		
		key = keyBuilder.toString().trim();
		}catch(Exception e){e.printStackTrace();}
	}
	
	public String getClassName()
	{
		return "uk.ac.gla.terrier.structures.globallexicon.incrementalindex.LexiconStringKey";
	}
	
	public static int sizeInBytes()
	{
		return maxlength*2;
	}

}

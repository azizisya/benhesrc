package uk.ac.gla.terrier.structures.trees.bplustree;

import java.io.DataInput;
import java.io.DataOutput;
import java.nio.ByteBuffer;

public class BplusTestValue implements BplusValue{
	
	String value;
	static int maxlength = 30;
	static byte[] BufferArray = new byte[maxlength*2]; 
	
	public BplusTestValue()
	{
		value = "";
	}
	
	public BplusTestValue(String v)
	{
		value = v;
	}
	
	public void toBytes(DataOutput dao)
	{
		//ByteBuffer buffer = ByteBuffer.allocate(maxlength*2);
		try{
			//for (int i = 0; i < value.length() && i < maxlength; i++)
			//	dao.writeChar(value.charAt(i));
			dao.writeChars(value);
			//now pad upto 2* maxlength bytes
			dao.write(BufferArray, 0, 2* (maxlength - value.length()));
		
		}catch(Exception e){e.printStackTrace();}
		//return buffer.array();
			
	}
	
	public void build(DataInput dai)
	{
		//ByteBuffer buffer = ByteBuffer.allocate(keyAsBytes.length);
		//buffer.put(keyAsBytes);
		//buffer.rewind();
		try{
			StringBuilder s = new StringBuilder();
			while(/*buffer.remaining() > 0 &&*/ s.length() < maxlength)
				s.append(dai.readChar());
			
			value = s.toString().trim(); 
		}catch(Exception e){e.printStackTrace();}
	}
	
	public static int sizeInBytes()
	{
		return maxlength*2;
	}
	
	public String getClassName()
	{
		return "uk.ac.gla.terrier.structures.trees.bplustree.BplusTestValue";
	}

	
	public String toString()
	{
		return value;
	}

}

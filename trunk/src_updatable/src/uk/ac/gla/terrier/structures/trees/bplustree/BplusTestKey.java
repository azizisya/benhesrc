package uk.ac.gla.terrier.structures.trees.bplustree;

import java.io.DataInput;
import java.io.DataOutput;
import java.nio.*;


public class BplusTestKey extends BplusKey{
	
	int key;
	
	public BplusTestKey()
	{
		key = -1;
	}
	
	public BplusTestKey(int k)
	{
		key = k;
	}
	
	
	public int compareTo(Object o)
	{
		return key - ((BplusTestKey)o).key; 
	}
	
	public void toBytes(DataOutput dao)
	{
		//ByteBuffer buffer = ByteBuffer.allocate(4);
		try{
		dao.writeInt(key);
		}catch(Exception e){e.printStackTrace();}
		//return buffer.array();
	}
	
	public static int sizeInBytes()
	{
		return 4;
	}
	
	public void build(DataInput dai)
	{
		//ByteBuffer buffer = ByteBuffer.allocate(4);
		
		//buffer.put(keyAsBytes);
		//buffer.rewind();
		try{
		key = dai.readInt();
		}catch(Exception e){e.printStackTrace();}
	}
	
	public String getClassName()
	{
		return "uk.ac.gla.terrier.structures.trees.bplustree.BplusTestKey";
	}
	
	public String toString()
	{
		return key+"";
	}

}

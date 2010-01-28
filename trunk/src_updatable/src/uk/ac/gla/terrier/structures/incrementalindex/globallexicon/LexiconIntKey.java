package uk.ac.gla.terrier.structures.incrementalindex.globallexicon;

import java.io.DataInput;
import java.io.DataOutput;
import java.nio.ByteBuffer;

import uk.ac.gla.terrier.structures.trees.bplustree.*;


public class LexiconIntKey extends BplusKey {
	
	int key;
	
	public LexiconIntKey(int k)
	{
		key = k;
	}
	
	public int compareTo(Object o)
	{
		return key - ((LexiconIntKey)o).key;
	}
	
	public static int sizeInBytes()
	{
		return 4;
	}
	
	public void toBytes(DataOutput dao)
	{
		//ByteBuffer buffer = ByteBuffer.allocate(sizeInBytes());
		//buffer.putInt(key);
		try{
		dao.writeInt(key);
		}catch(Exception e){e.printStackTrace();}
		//return buffer.array();
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
		return "uk.ac.gla.terrier.structures.globallexicon.LexiconIntKey";
	}

}

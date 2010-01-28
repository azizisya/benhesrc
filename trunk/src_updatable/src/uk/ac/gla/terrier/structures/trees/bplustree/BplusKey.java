package uk.ac.gla.terrier.structures.trees.bplustree;

import java.io.DataInput;
import java.io.DataOutput;

public abstract class BplusKey implements Comparable{
	
	public BplusKey()
	{
		
	}
	
/*	public BplusKey(byte[] keyAsBytes)
	{
		build(keyAsBytes);
	}*/
	
	public abstract void toBytes(DataOutput dao);
	
	public abstract void build(DataInput dai);
	
	public abstract String getClassName();
	
	/*Must be implemented for the Disk nodes to work,
	 * interfaces ban static however, hence can only be
	 * enforced in documentation*/
//	public static abstract int sizeInBytes();
	
}

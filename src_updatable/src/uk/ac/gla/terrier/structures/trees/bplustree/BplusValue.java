package uk.ac.gla.terrier.structures.trees.bplustree;

import java.io.DataInput;
import java.io.DataOutput;

public interface BplusValue{
	
	public void toBytes(DataOutput dao);
	
	public void build(DataInput dai);
	
	public String getClassName();
	
	/*Must implement a the following method, static isn't allowed
	 * in interfaces hence can't be enforced except in code. */
	//public static int sizeInBytes();

}

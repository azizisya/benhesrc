package uk.ac.gla.terrier.structures.trees.bplustree;

public class InsertResult {
	
	public boolean wasSplit;
	public BplusKey centre;
	public BplusNode left;
	public BplusNode right;
	
	public BplusLeafNode leafContainingValue;
	
	
	public String toString()
	{
		return left+" "+centre+" "+right;
	}

}

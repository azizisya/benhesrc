package uk.ac.gla.terrier.structures.trees.bplustree;

public interface BplusLeafNode extends BplusNode{

	public abstract InsertResult insert(BplusKey k, BplusValue v);

	//public abstract BplusLeafNode split(BplusKey k, BplusValue v);

	public abstract BplusValue getValue(BplusKey k);
	
	public abstract BplusValue[] getValues();
	
	public int update(BplusKey k, BplusValue v);
	
	/**Remove the key/value pair from the leaf node. 
	 * For a given input key.
	 */
	public void deleteValue(BplusKey key);
	
	public boolean contains(BplusKey key);

	/**
	 * Retrieves the next leaf node. The leaf
	 * nodes can be accessed sequentially and in
	 * sorted order by key. 
	 * @return a node pointer to the leaf node
	 * containing the next values in the sequence.
	 */
	public abstract BplusLeafNode getNext();

	public abstract boolean isLeaf();

	public abstract String toString();
	
	public void updateValues();

}

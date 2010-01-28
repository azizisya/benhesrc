package uk.ac.gla.terrier.structures.trees.bplustree;

import uk.ac.gla.terrier.structures.FilePosition;
import uk.ac.gla.terrier.utility.ApplicationSetup;

/**
 * An in-memory variant of the B+ node that contains the values.
 * It should be used only when representing nodes on the lowest
 * rung of the tree, hence the leaf.
 * <p>
 * When it maximum key/values is reached, it splits creating another
 * BplusMemoryLeafNode.  
 * @author John Kane
 */
public class BplusMemoryLeafNode implements BplusLeafNode{
	
	/**Determines the number of values allowed in the node
	 * before it splits. 
	 * The max number of keys is twice the branchingfactor
	 * The max number of keys is twice the branchingfactor plus one
	 * The min number of keys is twice the branchingfactor
	 */
	//protected  int BranchingFactor;
	
	/**
	 * Properties object used to set-up this node and any sisters,
	 * it may spawn.
	 */
	protected  BplusTreeProperties properties;
	
	/**
	 * The values the B+tree is indexing, or at least the subset
	 * stored in this leaf node.
	 */
	protected BplusValue[] values;
	
	/**
	 * The keys indexing this nodes values, note there is one less
	 * key than value.
	 */
	protected BplusKey[] keys;
	
	/**
	 * The number of values currently stored in the node. 
	 */
	protected int currentNumberOfValues;
	
	/**
	 * A reference to the next node, that is the one with the
	 * smallest start key larger than this nodes largest key.
	 */
	protected BplusLeafNode next;

	//Constructor
	/**
	 * The standard constructor for the BplusMemoryLeafNode.
	 */
	public BplusMemoryLeafNode(BplusTreeProperties prop)
	{
		properties = prop;
		final int BranchingFactor = properties.getBplusLeafNodeBranchingFactor();
		keys = new BplusKey[2*BranchingFactor];
		values = new BplusValue[2*BranchingFactor];
		currentNumberOfValues = 0;
	}
	
	/**
	 * Performs an insertion of the value into the node, using the
	 * key as index. If the node is full, it is split before the insertion
	 * and the key/value pair then inserted into one of the halves.
	 */
	public InsertResult insert(BplusKey k, BplusValue v)
	{
		InsertResult ir = new InsertResult();
		final int BranchingFactor = properties.getBplusLeafNodeBranchingFactor();
		ir.wasSplit = false;
		ir.leafContainingValue = this;
		
		//if the node is full, split it and add the key/
		//value pair
		if(currentNumberOfValues == (2*BranchingFactor))
		{
			BplusMemoryLeafNode sibling = this.split(k,v);
			ir.left = this;
			ir.right = sibling;
			ir.centre = sibling.keys[0];
			ir.wasSplit = true;
			
			//The node with the most values is the one the value was
			//inserted into.
			if(sibling.currentNumberOfValues > this.currentNumberOfValues)
				ir.leafContainingValue = sibling;
		}
		else //there is enough room so just add it
		{
			int idx = this.getLocation(k);
			for(int i = currentNumberOfValues -1; i >= idx; i--)
			{
				keys[i+1] = keys[i];
				values[i+1] = values[i];
			}
			
			keys[idx] = k;
			values[idx] = v;
			currentNumberOfValues++;
		}
		
		
		return ir;
	}
	
	/**
	 * Splits the node into two equally sized parts. 
	 * The input key/value pair is then inserted into
	 * the correct half.
	 * @param k, the key for the new value that is being inserted
	 * @param v, the value that is being inserted
	 * @return the sibling node that is created, the second half of the initial node
	 */
	protected BplusMemoryLeafNode split(BplusKey k, BplusValue v)
	{
		BplusMemoryLeafNode sibling = new BplusMemoryLeafNode(properties);
		
		//copies the second half of the current node into
		//the first half of sibling.
		final int BranchingFactor = properties.getBplusLeafNodeBranchingFactor();
		for (int i = BranchingFactor; i < (2*BranchingFactor); i++)
		{
			sibling.keys[i-BranchingFactor] = this.keys[i];
			sibling.values[i-BranchingFactor] = this.values[i];
		}
		this.currentNumberOfValues = BranchingFactor;
		sibling.currentNumberOfValues = BranchingFactor;
		
		//set pointer to next node
		sibling.next = this.next;
		this.next = sibling;
		
		if(k.compareTo(sibling.keys[0]) < 0)
			this.insert(k,v);
		else
			sibling.insert(k,v);
		
		return sibling;
	}
	
	/**
	 * Returns the location in the values array
	 * occupied by value associted with the input key.
	 * If there is no matching key in this node it returns
	 * an out of range value.
	 * @param k a key being used to look up one of the nodes values
	 * @return int the position of the value whithin the nodes value array,
	 * if its not found the int will be out of the array size range.
	 */
	protected int getLocation(final BplusKey k)
	{
		int pos = 0;
		while (pos < currentNumberOfValues && 
				keys[pos].compareTo(k) < 0)
			pos++;			

		return pos;
	}
	
	public void deleteValue(BplusKey key)
	{
		//find the location of the value to be deleted
		int idx = this.getLocation(key);
		//if there is such a key
		if(idx < this.currentNumberOfValues)
		{
			//Starting at the value to be deleted shift, every key/value
			//pair one place to the left
			for (int i = idx; i < (this.currentNumberOfValues-1); i++)
			{
				this.keys[i] = this.keys[i+1];
				this.values[i] = this.values[i+1];
			}
			
			//decrement the key count
			this.currentNumberOfValues--;
		}
	}
	
	/**
	 * Update is used when the key and value already exist but
	 * the value must be replaced with another version.
	 * @param k
	 * @param v
	 * @return the index at which the key was found.
	 */
	public int update(BplusKey k, BplusValue v)
	{
		final int idx = this.getLocation(k);
		values[idx] = v;
		return idx;
	}
	
	/**
	 * Returns the value stored under the input key, or null if
	 * nothing was found in this node.
	 */
	public BplusValue getValue(BplusKey k)
	{
		final int pos = this.getLocation(k);
		if(pos >= currentNumberOfValues || k.compareTo(this.keys[pos]) != 0)
			return null;
		return this.values[pos];
	}
	
	
	/**
	 * Retrieves all the stored values of the node as an array.
	 * @return BplusValue[] the values of this node as an array.
	 */
	public BplusValue[] getValues()
	{	
		BplusValue[] out = new BplusValue[this.currentNumberOfValues];
		for(int i = 0; i < currentNumberOfValues; i++)
			out[i] = values[i];
		return out;
	}
	
	public void updateValues() { }
	
	/**
	 * Checks whether the node contains the input key
	 * @param key
	 * @return true if the node contains the key, false otherwise
	 */
	public boolean contains(BplusKey key)
	{
		for(int i = 0; i < this.currentNumberOfValues; i++)
			if(keys[i].compareTo(key) == 0)
				return true;
		return false;
	}
	
	public FilePosition close()
	{
		System.err.println("Not implemented yet. SHoudl create a BplusDiskleafNode " +
				"and copyitslef to it before saving and returning its fileposition," +
				"complexity comes when trying to retrieve the fileposition of next");
		return null;
	}
	
	/**
	 * Retrieves the next leaf node. The leaf
	 * nodes can be accessed sequentially and in
	 * sorted order by key. 
	 * @return a node pointer to the leaf node
	 * containing the next values in the sequence.
	 */
	public BplusLeafNode getNext()
	{
		return next;
	}
	
	/**
	 * Inherited from BplusNode, used to verify that this
	 * is a leaf node. Always return true.
	 */
	public boolean isLeaf()
	{
		return true;
	}
	
	/**
	 * The current number of values stored in the node. Perhaps a misnomer.
	 */
	public int size()
	{
		return this.currentNumberOfValues;
	}
	
	public String toString()
	{

		StringBuilder out = new StringBuilder();
		out.append("Leaf [");
		for(int i = 0; i < this.currentNumberOfValues; i++)
		{
			out.append(keys[i].toString());
			//out.append(": ");
			//out.append(values[i].toString());
			out.append(",");
		}	
		out.append("] //end of node\n");
		return out.toString();		
	}

	public static void main(String[] args)
	{
		BplusTreeProperties properties = new BplusTreeProperties();
		

		// /users/kane/terrier/ // /users/students4/level4/kanej/TerrierProject/Terrier/terrier/var/index/
		properties = new BplusTreeProperties();
		
		properties.setBplusInnerNodeBranchingFactor(4);
		properties.setBplusLeafNodeBranchingFactor(10);
		
		properties.setKeyClass("uk.ac.gla.terrier.structures.trees.bplustree.BplusTestKey");
		properties.setValueClass("uk.ac.gla.terrier.structures.trees.bplustree.BplusTestValue");

		NodeStorageFile a = new NodeStorageFile(ApplicationSetup.TERRIER_INDEX_PATH+ApplicationSetup.FILE_SEPARATOR+"testLeafNodeFile",
				BplusDiskLeafNode.sizeInBytes(properties));
		properties.setLeafNodeFile(a);
		NodeStorageFile b = new NodeStorageFile(ApplicationSetup.TERRIER_INDEX_PATH+ApplicationSetup.FILE_SEPARATOR+"testInnerNodeFile",
				BplusDiskInnerNode.sizeInBytes(properties));
		properties.setInnerNodeFile(b);
		
		
		BplusMemoryLeafNode n = new BplusMemoryLeafNode(properties);
		
		n.insert((new BplusTestKey(8)), (new BplusTestValue("c")));
		n.insert((new BplusTestKey(9)), (new BplusTestValue("d")));
		n.insert((new BplusTestKey(2)), (new BplusTestValue("e")));
		n.insert((new BplusTestKey(16)), (new BplusTestValue("f")));
		n.insert((new BplusTestKey(14)), (new BplusTestValue("g")));
		n.insert((new BplusTestKey(18)), (new BplusTestValue("h")));
		n.insert((new BplusTestKey(11)), (new BplusTestValue("i")));
		n.insert((new BplusTestKey(12)), (new BplusTestValue("j")));
		n.insert((new BplusTestKey(4)), (new BplusTestValue("z")));
		
		System.out.println(n);
		
		n.deleteValue((new BplusTestKey(2)));
		
		System.out.println(n);
		/*
		BplusTree t = new BplusTree(properties);
		BplusTreeProperties prop = new BplusTreeProperties();
		prop.setBplusLeafNodeBranchingFactor(2);
		BplusMemoryLeafNode n = new BplusMemoryLeafNode(prop);
		InsertResult ir = new InsertResult();
		
		t.insert((new BplusTestKey(5)), (new BplusTestValue("a")));
		t.insert((new BplusTestKey(7)), (new BplusTestValue("b")));
		//t.insert((new BplusTestKey(10)), (new BplusTestValue("e")));

		t.insert((new BplusTestKey(8)), (new BplusTestValue("c")));
		t.insert((new BplusTestKey(9)), (new BplusTestValue("d")));
		t.insert((new BplusTestKey(2)), (new BplusTestValue("e")));
		t.insert((new BplusTestKey(16)), (new BplusTestValue("f")));
		t.insert((new BplusTestKey(14)), (new BplusTestValue("g")));
		t.insert((new BplusTestKey(18)), (new BplusTestValue("h")));
		t.insert((new BplusTestKey(11)), (new BplusTestValue("i")));
		t.insert((new BplusTestKey(12)), (new BplusTestValue("j")));
		t.insert((new BplusTestKey(4)), (new BplusTestValue("z")));
		*/
		//System.out.println(t);
		//System.out.println(n.getLocation(new BplusTestKey(11)));
		//if(ir.wasSplit)
		//	System.out.println(ir);
		//else
		//	System.out.println(n+" with "+n.currentNumberOfValues+" values.");
	}
	
}

package uk.ac.gla.terrier.structures.trees.bplustree;

import uk.ac.gla.terrier.structures.FilePosition;

public class BplusMemoryInnerNode implements BplusInnerNode{
	
	protected  BplusTreeProperties properties;
	
	protected int currentNumberOfKeys;
	
	protected boolean isPenultimate;
	
	protected BplusNode[] children;
	protected BplusKey[] keys;
	
	public BplusMemoryInnerNode(BplusTreeProperties p)
	{
		properties = p;
		final int BranchingFactor = properties.getBplusInnerNodeBranchingFactor();
				
		currentNumberOfKeys = 0;
		keys = new BplusKey[(2*BranchingFactor)-1];
		children = new BplusNode[(2*BranchingFactor)];
	}
	
	/*
	public BplusMemoryInnerNode(BplusTreeProperties p, FilePosition fp)
	{
		properties = p;
		BplusDiskInnerNode onDiskNode = new BplusDiskInnerNode(p, fp);
		isPenultimate = onDiskNode.
		keys = onDiskNode.getKeys();
		children = onDiskNode.getChildren();
		currentNumberOfKeys = onDiskNode.getCurrentNumberOfKeys();
	}*/
	
	public InsertResult insert(BplusKey k, BplusValue v)
	{
		int idx = this.getLocation(k);
		final int BranchingFactor = properties.getBplusInnerNodeBranchingFactor();
		InsertResult childSplitResult = children[idx].insert(k,v);
		
		//if the child was split a new value
		//has been passed up to be inserted in this node
		if(childSplitResult.wasSplit)
		{
			
			//if this node is full
			if(currentNumberOfKeys == ((2*BranchingFactor)-1))
			{
				return this.split(childSplitResult);
			}
			else//there is enough room to place the new value
			{
				this.nonfullInsert(childSplitResult);
				childSplitResult.wasSplit = false;
			}
		}
		
		return childSplitResult;
	}
	
	public void nonfullInsert(InsertResult ir)
	{
		int idx = this.getLocation(ir.centre);
		//System.out.println(this+" trying to insert into "+idx);
		if(idx == this.currentNumberOfKeys)
		{
			keys[idx] = ir.centre;
			children[idx] = ir.left;
			children[idx+1] = ir.right;
		}
		else
		{
			//System.out.println(this+" at "+idx);
			for(int i = currentNumberOfKeys-1; i >= idx; i--)
			{
				keys[i+1] = keys[i];
				children[i+2] = children[i+1];
			}
			children[idx+1] = children[idx]; 
			
			keys[idx] = ir.centre;
			//values[idx] = v;
			children[idx] = ir.left;
			children[idx+1] = ir.right;
			//System.out.println("here is the right "+ir.right);
		}
		currentNumberOfKeys++;
	}
	
	protected InsertResult split(InsertResult childResult)
	{
		InsertResult currentResult = new InsertResult();
		//int idx = this.getLocation(childResult.centre);
		final int BranchingFactor = properties.getBplusInnerNodeBranchingFactor();
		currentResult.centre = keys[BranchingFactor-1];//the median key

		//copy the second half of the node into a new node sibling
		//setup so the left child of the median goes to first half,
		//the right child of median starts the second half.
		BplusMemoryInnerNode sibling = new BplusMemoryInnerNode(properties);
		int pos = 0;
		for(int i = BranchingFactor; i < (2*BranchingFactor)-1; i++)
		{
			sibling.keys[pos] = this.keys[i];
			sibling.children[pos] = this.children[i];
			pos++;
		}
		sibling.children[pos] = this.children[(2*BranchingFactor)-1];
		
		this.currentNumberOfKeys = BranchingFactor-1;
		sibling.currentNumberOfKeys = BranchingFactor-1;
		
		//Add the key from the child node to the correct half 
		BplusMemoryInnerNode insertInto = childResult.centre.compareTo(currentResult.centre) > 0 ?
				sibling : this;
		insertInto.nonfullInsert(childResult);
		
	
		currentResult.left = this;
		currentResult.right = sibling;
		
		currentResult.wasSplit = true;
		currentResult.leafContainingValue = childResult.leafContainingValue;
		return currentResult;
	}
	
	protected int getLocation(BplusKey k)
	{
		int pos = 0;
		
		while (pos < currentNumberOfKeys && 
				keys[pos].compareTo(k) <= 0)
		{

			pos++;
		}
		
		
		
		return pos;
	}
	
	
	public BplusNode getChild(BplusKey k)
	{
		return this.children[this.getLocation(k)];
	}
	
	public boolean isLeaf()
	{
		return false;
	}
	
	public BplusNode[] getChildren()
	{
		return children;
	}
	
	public int getCurrentNumberOfKeys()
	{
		return this.currentNumberOfKeys;
	}
	
	public void setPenultimate(boolean b)
	{
		isPenultimate = b;
	}
	
	public String toString()
	{
		String out  = "Inner [";
		for(int i = 0; i < this.currentNumberOfKeys; i++)
			out+= keys[i]+",";
		
		out+="]\n Children";
		for(int i = 0; i <= this.currentNumberOfKeys; i++)
			out+=" "+this.children[i];//*/
		return out+"]";
	}
	
	/**Saves the memory inner node to file. The format on file is equivalent
	 * to a disk node. Before the node can be saved it must know the file position
	 * of its child nodes.  
	 * 
	 * @param file
	 * @return The file position of the node as saved on disk.
	 */
	public FilePosition close()
	{
		//create an equivalent diskinnernode
		BplusDiskInnerNode Dnode = new BplusDiskInnerNode(properties);

		//For each of the children close it and record its position in the file
		for(int i = 0; i <= this.currentNumberOfKeys; i++)
			Dnode.children[i] = this.children[i].close();
		
		
		//copy the other information over.
		Dnode.keys = this.keys;
		Dnode.currentNumberOfKeys = this.currentNumberOfKeys;
		Dnode.isPenultimate = true;
		Dnode.save();
		return Dnode.getFilePosition();
	}
	

	
	/**
	 * shouldn't be used except in test code!
	 * @deprecated
	 */
	public void debug(BplusNode l,BplusTestKey k, BplusNode r)
	{
		this.keys[0] = k;
		this.children[0] = l;
		this.children[1] = r;
		this.currentNumberOfKeys++;
	}
	
	public static void main(String[] args)
	{
		BplusTreeProperties prop = new BplusTreeProperties();
		prop.setBplusInnerNodeBranchingFactor(3);
		
		BplusMemoryLeafNode l = new BplusMemoryLeafNode(prop);
		l.insert((new BplusTestKey(2)), (new BplusTestValue("a")));
		l.insert((new BplusTestKey(5)), (new BplusTestValue("b")));
		l.insert((new BplusTestKey(9)), (new BplusTestValue("c")));

		BplusMemoryLeafNode r = new BplusMemoryLeafNode(prop);
		r.insert((new BplusTestKey(12)), (new BplusTestValue("d")));
		r.insert((new BplusTestKey(14)), (new BplusTestValue("e")));
		r.insert((new BplusTestKey(17)), (new BplusTestValue("f")));
		
		BplusMemoryInnerNode i = new BplusMemoryInnerNode(prop);
		i.keys[0] = new BplusTestKey(12);
		i.children[0] = l;
		i.children[1] = r;
		i.currentNumberOfKeys++;
		
		InsertResult ir = new InsertResult();
		
		ir = i.insert((new BplusTestKey(3)), (new BplusTestValue("e")));
		ir = i.insert((new BplusTestKey(4)), (new BplusTestValue("e")));
		ir = i.insert((new BplusTestKey(15)), (new BplusTestValue("e")));
		ir = i.insert((new BplusTestKey(13)), (new BplusTestValue("e")));
		ir = i.insert((new BplusTestKey(6)), (new BplusTestValue("e")));
		ir = i.insert((new BplusTestKey(7)), (new BplusTestValue("e")));
		ir = i.insert((new BplusTestKey(20)), (new BplusTestValue("e")));
		ir = i.insert((new BplusTestKey(32)), (new BplusTestValue("e")));
		ir = i.insert((new BplusTestKey(19)), (new BplusTestValue("e")));
		ir = i.insert((new BplusTestKey(23)), (new BplusTestValue("e")));
		ir = i.insert((new BplusTestKey(22)), (new BplusTestValue("e")));
		
		if(ir.wasSplit)
			System.out.println(ir);
		else
			System.out.println(i+" with "+i.currentNumberOfKeys+" values.");
	}

}

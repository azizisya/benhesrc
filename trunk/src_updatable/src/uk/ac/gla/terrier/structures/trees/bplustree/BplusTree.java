package uk.ac.gla.terrier.structures.trees.bplustree;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import uk.ac.gla.terrier.structures.FilePosition;
import uk.ac.gla.terrier.utility.ApplicationSetup;



/**
 * The B+ tree is a variant of the binary tree. 
 * @author John Kane
 */
public class BplusTree {
	
	protected static boolean memoryInnerNodes 
		= Boolean.parseBoolean( 
				ApplicationSetup.getProperty("bplustree.innernodes.memory", "true"));
	
	/**
	 * The root node of the tree. On creation it is a leaf node but after the first split operation
	 * it becomes, and remains, an inner node.
	 */
	protected BplusNode root;
	
	/**
	 * Points to the left most leaf node in the tree
	 * so that all the leaves can be processed sequentially.
	 * 
	 * Only gets instantiated after the creation of the
	 * first leaf node when the root first splits.
	 */
	protected BplusLeafNode FirstLeaf;
	
	/**
	 * The number of levels in the tree. A new level can only be added by a split in the root node.
	 */
	protected int height;
	
	
	protected int numberOfValues;
	
	//protected NodeStorageFile LeafNodeFile;
	protected BplusTreeProperties properties;
	
	/**
	 * The last leaf node that was searched for a value
	 */
	protected BplusLeafNode lastRetrievedLeafNode;
	
	
	protected BplusNodeCache cache;
	

	//Constructor
	
	public BplusTree(BplusTreeProperties prop)
	{
		properties = prop;
		
		//LeafNodeFile = prop.getLeafNodeFile();
		root = new BplusDiskLeafNode(prop);//BplusMemoryLeafNode();//
		height = 0;
		numberOfValues = 0;
		
		lastRetrievedLeafNode = null;
		FirstLeaf = (BplusLeafNode)root;
		
		if(prop.isCachingOn())
			cache = new BplusNodeCache(prop.getMaxSizeOfCache());
	}
	
	/**
	 * Constructor for when the Tree already exists on file and is to be recreated by giving it extra information.
	 * @param The properties for the tree
	 * @param The file position of the root node
	 * @param The file position of the first leaf node on the bootom rung of the tree
	 * @param The height of the tree in levels. This is perhaps superflous
	 * @param THe number of values that are storedin the tree.
	 */
	public BplusTree(BplusTreeProperties prop, FilePosition r, FilePosition fl, int h, int numValues)
	{
		properties = prop;
		
		//LeafNodeFile = prop.getLeafNodeFile();
		root = (new BplusDiskInnerNode(prop,r)).toMemory();//BplusMemoryLeafNode();//
		FirstLeaf = new BplusDiskLeafNode(prop,r);
		
		height = h;
		numberOfValues = numValues;
		
		lastRetrievedLeafNode = null;
		
		if(prop.isCachingOn())
			cache = new BplusNodeCache(prop.getMaxSizeOfCache());
	}
	
	//Mutator
	public void insert(BplusKey k, BplusValue v)
	{
		
		InsertResult ir = root.insert(k,v);
		
		//if split in current root, then create
		//new root a level above with the two
		//split nodes as its children
		if(ir.wasSplit)
		{
			boolean pen = root.isLeaf();
			
			if(memoryInnerNodes)
			{
				root = new BplusMemoryInnerNode(properties);
			}
			else
			{
				root = new BplusDiskInnerNode(properties);
				
			}
			
			((BplusInnerNode)root).setPenultimate(pen);
			
			/*
			if(FirstLeaf == null)
			{
				FirstLeaf = (BplusLeafNode)ir.left;
			}*/
			
			((BplusInnerNode)root).nonfullInsert(ir);
			height++;
		}
		
		numberOfValues++;
		

		//LeafCache.add(lastRetrievedLeafNode);
		lastRetrievedLeafNode = ir.leafContainingValue;
		
		if(properties.isCachingOn())
			cache.add((BplusDiskLeafNode)lastRetrievedLeafNode);
		if(!((BplusDiskLeafNode)lastRetrievedLeafNode).hasRead)
			System.err.println("Caching unread node. 1");
		
	}
	
	public BplusValue search(BplusKey key)
	{
		if(lastRetrievedLeafNode != null && lastRetrievedLeafNode.contains(key))
		{
			
			return lastRetrievedLeafNode.getValue(key);
		}
		/*else if(LeafCache.contains(key))
		{
			BplusLeafNode temp = LeafCache.get(key);
			return temp.getValue(key);
		}*/
		else
		{
			BplusNode current = root;
			while(!current.isLeaf())
			{
				current = ((BplusInnerNode)current).getChild(key);
			}
		
			//LeafCache.add(lastRetrievedLeafNode);
			lastRetrievedLeafNode = (BplusLeafNode)current;
			
			
			//Note: this is needed tp avoid errors caused by caching interacting
			//with merging.
			((BplusDiskLeafNode)lastRetrievedLeafNode).read();
			return ((BplusLeafNode)current).getValue(key);
		}
	}
	
	public BplusValue[] search(FilePosition fp)
	{
		BplusDiskLeafNode n = new BplusDiskLeafNode(properties,fp);
		return n.getValues();
	}
	
	public void updateValue(BplusKey key, BplusValue value)
	{
		//System.err.println(lastRetrievedLeafNode);	
		if(lastRetrievedLeafNode.contains(key))
		{
			//System.err.println("LRLN = true");
			lastRetrievedLeafNode.update(key,value);
			
			if(properties.isCachingOn())
				cache.add((BplusDiskLeafNode)lastRetrievedLeafNode);
		}
		else
		{
			//System.err.println("LRLN = false");
			search(key);
			lastRetrievedLeafNode.update(key,value);
			
			if(properties.isCachingOn())
				cache.add((BplusDiskLeafNode)lastRetrievedLeafNode);
		}
	}
	
	public void updateNodeValues(BplusLeafNode node)
	{
		node.updateValues();
	}
	
	
	/**
	 * This method will remove the value associated with hte input key,
	 * if such a key exists within the Bplusree. This implementation
	 * is the simplest variant in which there is no merging of nodes
	 * with less than the minimum number of values.
	 * 
	 * This is standard in databases and should be workable assuming the 
	 * number of deletions does not outway the number of insertions.
	 */
	public void deleteValue(BplusKey key)
	{
		
		if(lastRetrievedLeafNode != null && lastRetrievedLeafNode.contains(key))
		{
			((BplusLeafNode)lastRetrievedLeafNode).deleteValue(key);
		}
		else
		{
			BplusNode current = root;
			while(!current.isLeaf())
			{
				current = ((BplusInnerNode)current).getChild(key);
			}
		
			//LeafCache.add(lastRetrievedLeafNode);
			lastRetrievedLeafNode = (BplusLeafNode)current;
			
			//Note: this is needed tp avoid errors caused by caching interacting
			//with merging.
			((BplusDiskLeafNode)lastRetrievedLeafNode).read();
			
			if(lastRetrievedLeafNode.contains(key))
				((BplusLeafNode)current).deleteValue(key);
		}
	}
	

	
	//Getters
	public BplusLeafNode getFirstLeaf()
	{
		return FirstLeaf;
	}
	
	public BplusLeafNode getLastRetrievedLeafNode()
	{
		return lastRetrievedLeafNode;
	}
	
	/**
	 * Returns the root node of the tree. Note a tree of only one node will pass back a leaf node not an inner node.
	 * @return the root node.
	 */
	public BplusNode getRoot()
	{
		return root;
	}
	
	
	/**
	 * Returns the number of values that have been stored in the tree.
	 * @return
	 */
	public int size()
	{
		return numberOfValues;
	}
	
	
	/**
	 * Returns the height of the tree in levels.
	 */
	public int getHeight()
	{
		return height;
	}
	
	
	/**
	 * Closes the tree structure and returns the location of the root node, in the inner node file.
	 * The close command is
	 * propagated down the tree through the root node.
	 * @return the root node's position in the NodeStorage file.
	 */
	public FilePosition close()
	{
		return root.close();
	}
	
	
	/**
	 * Gives a visual representation of the tree. 
	 * It is really for testing and rather ad hoc.
	 */
	public String toString()
	{
		String out = "Root ";
		LinkedList queue = new LinkedList();
		queue.add(root);
		BplusNode temp;
		
		
		while(!queue.isEmpty())
		{
			
			temp = (BplusNode)queue.removeFirst();
		//	System.out.println(temp);
			if(!temp.isLeaf())
			{
				BplusNode[] tempArray = ((BplusInnerNode)temp).getChildren();
				for(int i = ((BplusInnerNode)temp).getCurrentNumberOfKeys(); i >= 0 ; i--)
					queue.addFirst(tempArray[i]);
			}
			
			out+=temp+"\n";
		}
		return out;
		
	}
	
	/**
	 * This is a variant toString that only gives a print out of 
	 * the bottom rung where the values are stored.
	 */
	public String toStringBottomRung()
	{
		String out = "Bottom Rung: ";
		BplusDiskLeafNode node = (BplusDiskLeafNode)FirstLeaf;
		int averageCapacity = 0;
		int count = 0;
		NodeStorageFile LeafNodeFile = properties.getLeafNodeFile();
		
		while(node != null)
		{
			count++;
			//out += (node+" ");
			LeafNodeFile.read(node);
			averageCapacity +=node.encapsulatedNode.currentNumberOfValues;
			node = (BplusDiskLeafNode)node.getNext();
		}
		
			
		return out+"\n Average Capacity: "+(averageCapacity/count)+" over "+count+" nodes";
	}
	
	public static void main(String[] args) {
		
		BplusTreeProperties properties = new BplusTreeProperties();
		
		properties = new BplusTreeProperties();
		
		properties.setBplusInnerNodeBranchingFactor(10);
		properties.setBplusLeafNodeBranchingFactor(10);
		
		properties.setKeyClass("uk.ac.gla.terrier.structures.trees.bplustree.BplusTestKey");
		properties.setValueClass("uk.ac.gla.terrier.structures.trees.bplustree.BplusTestValue");

		NodeStorageFile a = new NodeStorageFile(ApplicationSetup.TERRIER_INDEX_PATH+ApplicationSetup.FILE_SEPARATOR+"testLeafNodeFile",
				BplusDiskLeafNode.sizeInBytes(properties));
		properties.setLeafNodeFile(a);
		NodeStorageFile b = new NodeStorageFile(ApplicationSetup.TERRIER_INDEX_PATH+ApplicationSetup.FILE_SEPARATOR+"testInnerNodeFile",
				BplusDiskInnerNode.sizeInBytes(properties));
		properties.setInnerNodeFile(b);
		
		

		FilePosition rootPos;
		String before = null; 
		String after;
		
		int[] testValues ={	
				/*1,2,3,4,5,6,7,8,9,10,11,12,13,14,15/*,16,17,18,19,20
				//*/
				
				160, 130, 50, 132, 6, 140, 196, 154, 10, 186, 94, 68, 22, 172, 18, 106, 14, 184, 32, 168, 134, 34, 178, 72, 166, 28, 138, 52, 42, 16, 152, 150, 76, 78, 114, 84, 108, 156, 126, 12, 98, 38, 176, 144, 104, 148, 46, 110
				/*
				45,44,54,7,16,146,8,12,3,53,4,6,105,147,
		106,79,138,112,117,69,82,118,2,18,120,123,126,127,
		100,83,5,132,52,130,131,139,145,55,68,111,101,76,70,149/**/};
		
		
 		BplusTestKey[] testkeys = new BplusTestKey[testValues.length];
 		for(int i = 0; i < testValues.length; i++) 
 			testkeys[i] = new BplusTestKey(testValues[i]*2);
		

	
		for(int z = 0; z < 1; z++)
		{
 		
 		//System.out.println("*** B+ Tree testing ***");

		BplusTree t = new BplusTree(properties);
 		
 		//Sets up values for input by converting them to
 		//testKey objects then randomises their order.
 		

 		
 		List l = Arrays.asList(testkeys);
 		LinkedList c = new LinkedList(); 
 		Collections.shuffle(l);
 		Collections.shuffle(l);
 		
 		ListIterator iter = l.listIterator();

 		int count = testValues.length;
 		while(count > 0)
 		{
 			BplusTestKey k = (BplusTestKey)iter.next();
 			t.insert(k, new BplusTestValue("n/a"));
 			//c.add(k);
 			count--;
 			//System.out.println(t);
 			//System.out.println("*****************************************");
 		}
 		
 		before = t.toString();
 		rootPos = t.close();
 		
 		
		BplusTree s = new BplusTree(properties);
		s.root = (new BplusDiskInnerNode(properties)).toMemory();
		after = s.toString();
		System.out.println("Are they equal? "+before.equals(after));
		}
		
		

		System.out.println("finished.");

	}

}

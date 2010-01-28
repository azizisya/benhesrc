package uk.ac.gla.terrier.structures.trees.bplustree;

import java.io.DataInput;
import java.io.DataOutput;
import java.nio.ByteBuffer;

import uk.ac.gla.terrier.structures.FilePosition;
import uk.ac.gla.terrier.utility.ApplicationSetup;

public class BplusDiskInnerNode extends BplusDiskNode implements BplusInnerNode{
	
	//protected BplusMemoryInnerNode encapsulatedNode;
	protected FilePosition filePosition;
	
	protected  NodeStorageFile InnerFile;
	
	protected  BplusTreeProperties properties;
	
	protected boolean hasRead;

	
	
	protected int currentNumberOfKeys;
	
	protected FilePosition[] children;
	protected BplusKey[] keys;
	
	
	protected boolean isPenultimate = false;
	
	
	
	
	
	public BplusDiskInnerNode(BplusTreeProperties p)
	{
		properties = p;
		
		currentNumberOfKeys = 0;
		final int BranchingFactor = p.getBplusInnerNodeBranchingFactor();
		
		keys = new BplusKey[(2*BranchingFactor)-1];
		children = new FilePosition[(2*BranchingFactor)];
		
		InnerFile = p.getInnerNodeFile();
		filePosition = InnerFile.getFreeBlock(this.sizeInBytes());
		
		hasRead = false;
	}
	
	public BplusDiskInnerNode(BplusTreeProperties p, FilePosition fp)
	{
		properties = p;
		
		currentNumberOfKeys = 0;
		final int BranchingFactor = p.getBplusInnerNodeBranchingFactor();
		
		keys = new BplusKey[(2*BranchingFactor)-1];
		children = new FilePosition[(2*BranchingFactor)];
		
		InnerFile = p.getInnerNodeFile();
		filePosition = fp;
		
		hasRead = false;
	}
	
	public void setPenultimate(boolean b)
	{
		if(!hasRead)
			InnerFile.read(this);
		
		isPenultimate = b;
		InnerFile.write(this);
	}

	public FilePosition getFilePosition() {
		return filePosition;
	}

	public int sizeInBytes() {
		return sizeInBytes(properties);
	}
	
	public static int sizeInBytes(BplusTreeProperties prop) {
		//number of key/value pairs i.e. twice thebranching factor
		//times the size of a key/value pair. The current number of
		//key/value pairs is written as an int at the start of the 
		//node
		final int BranchingFactor =  prop.getBplusInnerNodeBranchingFactor();
		//System.out.println(BranchingFactor+" "+getObjectsSize(prop.getKeyClass())+" "+FilePosition.sizeInBytes());
		return 1+(4/*int number of current pairs*/)
		+(((BranchingFactor*2)-1)*getObjectsSize(prop.getKeyClass()))/*Number of keys times size of key*/
		+ ((BranchingFactor*2)*FilePosition.sizeInBytes())/*Number of children times size of fileposition*/;
	}

	public void toBytes(DataOutput dao) {
		try{
		//ByteBuffer buffer = ByteBuffer.allocate(this.sizeInBytes());
		//buffer.rewind();
		

		//whether its a penultimate node or not
		dao.writeByte(isPenultimate ? (byte)1 : (byte)0);
		
	
		//write the number of keys pairs in the node currently
		dao.writeInt(this.currentNumberOfKeys);
		
		//for each key
		for(int i = 0; i < this.currentNumberOfKeys; i++)
		{
			//write the key
			keys[i].toBytes(dao);
		}
		//for each child i.e. fileposition
		
		for(int i = 0; i <= this.currentNumberOfKeys && this.currentNumberOfKeys != 0; i++)
		{
			//write the child fileposition
			this.children[i].toBytes(dao);
		}//*/
		
		}catch(Exception e){e.printStackTrace();}
	}

	public void load(DataInput dai) {
		
		if(dai == null)
			return;
		try{
		
		//Verify whether the node is in the penultimate rung
		this.isPenultimate = dai.readByte() == (byte)0 ? false : true;
			
		
		//read the current number of keys stored in the node
		this.currentNumberOfKeys = dai.readInt();

		//for each key
		
		BplusKey key;
		for(int i = 0; i < this.currentNumberOfKeys; i++)
		{
			key = (BplusKey)createObject(properties.getKeyClass());
			key.build(dai);
			
			this.keys[i] = key;
		}
		
		//for each child i.e. fileposition, note there is one more child
		//than there are keys
		//byte[] childInBytes = new byte[FilePosition.sizeInBytes()];
		FilePosition fp;
		for(int i = 0; i <= this.currentNumberOfKeys; i++)
		{
			
			fp = new FilePosition();
			fp.build(dai);
			
			this.children[i] = fp;
		}
		
		}catch(Exception e){e.printStackTrace();}
		
	}


	public InsertResult insert(BplusKey k, BplusValue v) {
		final int BranchingFactor = properties.getBplusInnerNodeBranchingFactor();
		
		if(!hasRead)
			InnerFile.read(this);
		//System.out.println(this.isPenultimate);

		int idx = this.getLocation(k);
		BplusNode child = this.getChild(idx);
		
		InsertResult childSplitResult = child.insert(k,v);
		
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
		
		InnerFile.write(this);
		
		return childSplitResult;
	}
	
	public void nonfullInsert(InsertResult ir)
	{
		if(!hasRead)
			InnerFile.read(this);
		//InnerFile.read((BplusDiskNode)ir.left);
		//InnerFile.read((BplusDiskNode)ir.right);
		
		//System.out.println("Inserting "+ir.centre+" "+ir.left);
		
		int idx = this.getLocation(ir.centre);
		
		
		//If its being inserted into the right most value slot
		if(idx == this.currentNumberOfKeys)
		{
			keys[idx] = ir.centre;
			
			children[idx] = ((BplusDiskNode)ir.left).getFilePosition();
			children[idx+1] = ((BplusDiskNode)ir.right).getFilePosition();
		}
		else //otherwise everything has to be shifted up one to make room
		{
			for(int i = currentNumberOfKeys-1; i >= idx; i--)
			{
				keys[i+1] = keys[i];
				children[i+2] = children[i+1];
			}
			children[idx+1] = children[idx]; 
			
			keys[idx] = ir.centre;
			
			children[idx] = ((BplusDiskNode)ir.left).getFilePosition();
			children[idx+1] = ((BplusDiskNode)ir.right).getFilePosition();

		}
		currentNumberOfKeys++;
		
		
		InnerFile.write(this);
		

	}
	
	protected InsertResult split(InsertResult childResult)
	{
		InsertResult currentResult = new InsertResult();
		final int BranchingFactor = properties.getBplusInnerNodeBranchingFactor();
		currentResult.centre = keys[BranchingFactor-1];//the median key

		//copy the second half of the node into a new node sibling
		//setup so the left child of the median goes to first half,
		//the right child of median starts the second half.
		BplusDiskInnerNode sibling = new BplusDiskInnerNode(properties);
		//InnerFile.read(sibling);
		InnerFile.read(this);
		
		//System.out.println("THe siblings file position: "+sibling.filePosition);
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
		
		//set sibling to penultimate if this is, note don't use method
		//as it overwrites the nodes with the version on disk
		sibling.isPenultimate = this.isPenultimate;
	
		//save the changes
		InnerFile.write(this);
		InnerFile.write(sibling);

		
		//Add the key from the child node to the correct half 
		BplusDiskInnerNode insertInto = childResult.centre.compareTo(currentResult.centre) > 0 ?
				sibling : this;
		
		insertInto.nonfullInsert(childResult);
		

		BplusDiskInnerNode debug = new BplusDiskInnerNode(properties, sibling.filePosition);
		InnerFile.read(debug);
		
		
		
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
			pos++;
		
		
		
		return pos;
	}


	public BplusNode getChild(BplusKey k) {
		if(!hasRead)
			InnerFile.read(this);
		int idx = this.getLocation(k);
		return this.getChild(idx);
	}
	
	protected BplusNode getChild(int idx)
	{
		if(isPenultimate) //child is a leaf node
		{
			BplusDiskLeafNode c = new BplusDiskLeafNode(properties,children[idx]);
			return c;
		}
		else //child is an inner node
		{
			BplusDiskInnerNode c = new BplusDiskInnerNode(properties,children[idx]);
			return c;
		}
	}
	

	public BplusNode[] getChildren()
	{
		if(!hasRead)
			InnerFile.read(this);
		
		BplusNode[] tempArray = new  BplusNode[this.currentNumberOfKeys+1];
		
		for(int i = 0; i < tempArray.length; i++)
			tempArray[i] = this.getChild(i);
		
		return tempArray;
	}
	
	public BplusKey[] getKeys()
	{
		if(!hasRead)
			InnerFile.read(this);
		
		BplusKey[] tempArray = new  BplusKey[this.currentNumberOfKeys];
		
		for(int i = 0; i < tempArray.length; i++)
			tempArray[i] = keys[i];
		
		return tempArray;
	}
	
	public int getCurrentNumberOfKeys()
	{
		if(!hasRead)
			InnerFile.read(this);
		return this.currentNumberOfKeys;
	}
	
	
	public boolean isLeaf() {
		return false;
	}
	
	public void save()
	{
		InnerFile.write(this);
	}
	
	public FilePosition close()
	{
		save();
		return this.getFilePosition();
	}
	
	public BplusMemoryInnerNode toMemory()
	{
		InnerFile.read(this);
		
		BplusMemoryInnerNode Mnode = new BplusMemoryInnerNode(properties);
		
		//copy the children by retreiving 
		if(isPenultimate)
		{
			for(int i = 0; i <= this.currentNumberOfKeys; i++)
				Mnode.children[i] = (new BplusDiskLeafNode(properties,children[i]));
		}
		else
		{
			for(int i = 0; i <= this.currentNumberOfKeys; i++)
				Mnode.children[i] = (new BplusDiskInnerNode(properties,children[i])).toMemory();
		}
		
		//copy all other info accross
		Mnode.keys = this.keys;
		Mnode.currentNumberOfKeys = this.currentNumberOfKeys;
		Mnode.isPenultimate = this.isPenultimate;
	    return Mnode;
	}
	
	public String toString()
	{
		if(!hasRead)
			InnerFile.read(this);
		String out = (this.isPenultimate? "Penultimate ":"")+"Inner [";
		for(int i = 0; i < this.currentNumberOfKeys; i++)
			out += keys[i].toString()+",";
		
		
		return out+"]";
	}
	
	public static void main(String[] args) {
		
		BplusTreeProperties properties = new BplusTreeProperties();
		
		properties.setBplusInnerNodeBranchingFactor(2);
		properties.setBplusLeafNodeBranchingFactor(2);
		
		NodeStorageFile a = new NodeStorageFile(ApplicationSetup.TERRIER_INDEX_PATH+"/LeafNodeFile"
				,BplusDiskLeafNode.sizeInBytes(properties));
		properties.setLeafNodeFile(a);
		NodeStorageFile b = new NodeStorageFile(ApplicationSetup.TERRIER_INDEX_PATH+"/InnerNodeFileTemp",
				BplusDiskInnerNode.sizeInBytes(properties));
		properties.setInnerNodeFile(b);
		


		properties.setKeyClass("uk.ac.gla.terrier.structures.trees.bplustree.BplusTestKey");
		properties.setValueClass("uk.ac.gla.terrier.structures.trees.bplustree.BplusTestValue");

		BplusDiskInnerNode[] nodeArray = new BplusDiskInnerNode[10];
		for(int i = 0; i < nodeArray.length; i++)
		{
			nodeArray[i] = new BplusDiskInnerNode(properties);
			nodeArray[i].isPenultimate = i % 2 == 0? true : false;
			b.write(nodeArray[i]);
		}
		
		for(int i = 0; i < nodeArray.length; i++)
		{
			b.read(nodeArray[i]);
			System.out.println(nodeArray[i].isPenultimate);
			b.write(nodeArray[i]);
		}
		
		
		
	}

}

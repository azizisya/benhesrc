package uk.ac.gla.terrier.structures.trees.bplustree;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.RandomAccessFile;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.structures.FilePosition;


public class BplusDiskLeafNode extends BplusDiskNode implements BplusLeafNode  {
	
	//protected static final int sizeInBytes = 100;
	protected BplusTreeProperties properties;
	
	protected BplusMemoryLeafNode encapsulatedNode;
	protected FilePosition filePosition;
	protected NodeStorageFile file;
	
	protected FilePosition next = null;
	
	public boolean hasRead;
	
	
	public BplusDiskLeafNode(BplusTreeProperties prop)
	{
		properties = prop;
		file = prop.getLeafNodeFile();
		filePosition = file.getFreeBlock(this.sizeInBytes());
		hasRead = false;
		
		//next = new FilePosition((long)-1,(byte)-1);
		//encapsulatedNode = new BplusMemoryLeafNode(properties);
		//file.write(this);
	}
	
	public BplusDiskLeafNode(BplusTreeProperties prop, FilePosition fp)
	{
		properties = prop;
		file = prop.getLeafNodeFile();
		filePosition = fp;
		hasRead = false;
		
		//next = new FilePosition((long)-1,(byte)-1);
		//encapsulatedNode = new BplusMemoryLeafNode(properties);
		//file.write(this);
	}

	public InsertResult insert(BplusKey k, BplusValue v) {
		if(!hasRead)
		{
			file.read(this);
			hasRead = true;
		}
		
		InsertResult ir = encapsulatedNode.insert(k,v);
		
		if(ir.wasSplit)
		{
			encapsulatedNode = (BplusMemoryLeafNode)ir.left;
			BplusDiskLeafNode sibling = new BplusDiskLeafNode(properties);
			sibling.encapsulatedNode = (BplusMemoryLeafNode)ir.right;
			
			/* we dont need this because to do an insert in this node, we always
			 * read it first. (See 8 lines above here) */
			//file.readNext(this);
			
			sibling.next = this.next;
			//System.out.println("fp "+next);
			this.next = sibling.filePosition;
			
			//System.out.println("fp before "+next);
			
			if(ir.leafContainingValue.equals(ir.right))
			{
				ir.leafContainingValue = sibling;
				sibling.hasRead = true;
			}
			else
				ir.leafContainingValue = this;
			
			ir.left = this;
			ir.right = sibling;

			file.write(sibling);
		}
		else
			ir.leafContainingValue = this;
		
		if(!properties.isCachingOn())
			file.writeNoKeysOrValue(this);

		
		
		return ir;
	}
	
	/**
	 * Update is used when the key and value already exist but
	 * the value must be replaced with another version.
	 * @param k
	 * @param v
	 */
	public int update(BplusKey k, BplusValue v)
	{
		if(!hasRead)
		{
			file.read(this);
			hasRead = true;
			//System.out.println("Should already have been read!");
		}
		
		final int index = encapsulatedNode.update(k,v);
		
		if(!properties.isCachingOn())
		file.writeOneValueOnly(this, index);
		return index;
	}

	public BplusValue getValue(BplusKey k) {
		if(!hasRead)
		{
			file.read(this);
			hasRead = true;
		}
		
		BplusValue v = encapsulatedNode.getValue(k);

		return v;
	}
	
	public void deleteValue(BplusKey key)
	{
		if(!hasRead)
		{
			file.read(this);
			hasRead = true;
		}
		
		this.encapsulatedNode.deleteValue(key);
	}
	
	public void read()
	{
		save();
		file.read(this);
	}
	
	public BplusValue[] getValues() {
		if(!hasRead)
		{
			file.read(this);
			hasRead = true;
		}
		BplusValue[] v = encapsulatedNode.getValues();

		return v;
	}
	
	public void updateValues() {
		file.writeValuesOnly(this); //TODO: optimise write call here?
	}
	
	/**
	 * Checks whether the node contains the input key
	 * @param key
	 * @return true if the node contains the key, false otherwise
	 */
	public boolean contains(BplusKey key)
	{
		if(!hasRead)
		{
			file.read(this);
			hasRead = true;
		}
		for(int i = 0; i < encapsulatedNode.currentNumberOfValues; i++)
			if(encapsulatedNode.keys[i].compareTo(key) == 0)
			{
				return true;
			}
			

		return false;
	}

	public BplusLeafNode getNext() {
		if(!hasRead)
		{
			file.readNext(this);
		}
		
		
		//The final node will point at {0,0}, the start
		if(next.Bytes != (long)0 )
		{
			//System.out.println("FP "+next);
			BplusDiskLeafNode out = new BplusDiskLeafNode(properties, next);
			return out;
		}
		else
		{
			//System.out.println("FP "+next);
			return null;
		}
	}
	
	public String toString()
	{
		//if(!hasRead)
		{
			file.read(this);
			hasRead = true;
		}
		return "Disk "+encapsulatedNode.toString();
	}
	

	public boolean isLeaf() {
		return true;
	}
	
	public int size()
	{
		if(!hasRead)
		{
			file.read(this);
			hasRead = true;
		}
		return encapsulatedNode.currentNumberOfValues;
	}
	

	public FilePosition getFilePosition() {
		return filePosition;
	}

	public int sizeInBytes() {
		return sizeInBytes(properties);
	}
	
	/**
	 * Reduces the nodes memory footprint by writing its contents back to disk and deallocating
	 *
	 */
	public void deactivate()
	{
		save();
		//hasRead=false;
		//this.encapsulatedNode = new BplusMemoryLeafNode(properties);
	}
	
	public void save()
	{
		file.write(this);
	}
	
	public FilePosition close()
	{
		save();
		return this.getFilePosition();
	}
	
	public static int sizeInBytes(BplusTreeProperties prop) {
		//number of key/value pairs i.e. twice thebranching factor
		//times the size of a key/value pair. The current number of
		//key/value pairs is written as an int at the start of the 
		//node
		
		
		return (FilePosition.sizeInBytes())+(4/*int number of current pairs*/)
		+((prop.getBplusLeafNodeBranchingFactor()*2)/*Number of pairs*/*
		(getObjectsSize(prop.getKeyClass()) /*Size of a pair*/
				+getObjectsSize(prop.getValueClass())));
	}

	public void toBytesNoKeysOrValue(DataOutput dao) {

		this.toBytes(dao);

		/*
		try{
			//write the pointer to next node
			if(next != null)
			{
				next.toBytes(dao);//buffer.put(next.toBytes());
			}
			else
			{
				//if this is the last node then point it back at the start of the file
				dao.writeLong((long)0);
				dao.writeByte((byte)0);
			}
			
			//write the number of key/value pairs in the node currently
			dao.writeInt(encapsulatedNode.currentNumberOfValues);
		}catch(Exception e){e.printStackTrace();}
		*/
	}
	
	/** Updates on-disk copy of this node, only the value */
	public void toBytesValuesOnly(DataOutput dao) {
		//toBytes(dao);
		
		try{
			((RandomAccessFile)dao).skipBytes(
					9+// a long + byte for the FilePosition
					4+// int for the current number of entries 
					(encapsulatedNode.currentNumberOfValues * getObjectsSize(properties.getKeyClass()))
						//for the current number of keys
					);
			
			for(int i = 0; i < encapsulatedNode.currentNumberOfValues; i++)
			{
				//write the value
				encapsulatedNode.values[i].toBytes(dao);
			}
			
		}catch(Exception e){e.printStackTrace();}
		
	}
	
	/* TODO check that index * getObjectsSize(properties.getValueClass()) is the write
	 * calculation */
	/** Updates on-disk only one value in this node, specified by index */
	public void toBytesOneValueOnly(DataOutput dao, int index) {
		toBytes(dao);
		/*
		try{
			((RandomAccessFile)dao).skipBytes(
					9+// a long + byte for the FilePosition
					4+// int for the current number of entries 
					(encapsulatedNode.currentNumberOfValues * getObjectsSize(properties.getKeyClass()))+
						//for the current number of keys
					index * getObjectsSize(properties.getValueClass())
					);
			
			for(int i = 0; i < encapsulatedNode.currentNumberOfValues; i++)
			{
				//write the value
				encapsulatedNode.values[i].toBytes(dao);
			}
			
		}catch(Exception e){e.printStackTrace();}
		*/
	}
	
	public void toBytes(DataOutput dao) {
		try{
		//ByteBuffer buffer = ByteBuffer.allocate(this.sizeInBytes());
		
		//write the pointer to next node
		if(next != null)
		{
			next.toBytes(dao);//buffer.put(next.toBytes());
		}
		else
		{
			//if this is the last node then point it back at the start of the file
			dao.writeLong((long)0);
			dao.writeByte((byte)0);
		}
		
		//write the number of key/value pairs in the node currently
		//System.err.println(encapsulatedNode);
		dao.writeInt(encapsulatedNode.currentNumberOfValues);
		
		//for each pair
		for(int i = 0; i < encapsulatedNode.currentNumberOfValues; i++)
		{
			//write the key
			encapsulatedNode.keys[i].toBytes(dao);
		}
		
		for(int i = 0; i < encapsulatedNode.currentNumberOfValues; i++)
		{
			//write the value
			encapsulatedNode.values[i].toBytes(dao);
		}
		
		}catch(Exception e){e.printStackTrace();}
	}


	/*
	public void load(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
		buffer.put(bytes);
		
		encapsulatedNode = new BplusMemoryLeafNode(properties);
		buffer.rewind();
		
		//read the pointer to the next node at the start
		byte[] nextAsBytes = new byte[FilePosition.sizeInBytes()];
		buffer.get(nextAsBytes);
		next = new FilePosition(nextAsBytes);
		
		//read the current number of key/value pairs stored in the node
		encapsulatedNode.currentNumberOfValues = buffer.getInt();

		//for each key/value pair read it in
		byte[] keyInBytes = new byte[getObjectsSize(properties.getKeyClass())];
		byte[] ValueInBytes = new byte[getObjectsSize(properties.getValueClass())];
		
		
		for(int i = 0; i < encapsulatedNode.currentNumberOfValues; i++)
		{
			buffer.get(keyInBytes);
			buffer.get(ValueInBytes);
			
			BplusKey key = (BplusKey)createObject(properties.getKeyClass());
			key.build(keyInBytes);
			
			BplusValue value = (BplusValue)createObject(properties.getValueClass());
			value.build(ValueInBytes);
			
			encapsulatedNode.keys[i] = key;
			encapsulatedNode.values[i] = value;
		}

	}*/
	
	
	public void load(DataInput di)
	{
		if(di == null)
		{
			encapsulatedNode = new BplusMemoryLeafNode(properties);
			next = new FilePosition();
		}
		else
		try{
		//ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
		//buffer.put(bytes);
		
		
		//buffer.rewind();
		
		encapsulatedNode = new BplusMemoryLeafNode(properties);
		//read the pointer to the next node at the start
		//byte[] nextAsBytes = new byte[FilePosition.sizeInBytes()];
		
		next = new FilePosition();
		next.build(di);
		
		//read the current number of key/value pairs stored in the node
		encapsulatedNode.currentNumberOfValues = di.readInt();

		//for each key/value pair read it in
		//byte[] keyInBytes = new byte[getObjectsSize(properties.getKeyClass())];
		//byte[] ValueInBytes = new byte[getObjectsSize(properties.getValueClass())];
		
		for(int i = 0; i < encapsulatedNode.currentNumberOfValues; i++)
		{
			//buffer.get(keyInBytes);
			//buffer.get(ValueInBytes);
			BplusKey key = (BplusKey)createObject(properties.getKeyClass());
			key.build(di);
			
			encapsulatedNode.keys[i] = key;
		}
		
		for(int i = 0; i < encapsulatedNode.currentNumberOfValues; i++)
		{

			BplusValue value = (BplusValue)createObject(properties.getValueClass());
			value.build(di);
			
			encapsulatedNode.values[i] = value;
		}
		
		}catch(Exception e){e.printStackTrace();}
		
		hasRead = true;
	}
	
	public void setNext(byte[] bytes)
	{
		/*ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
		buffer.put(bytes);
		buffer.rewind();
		
		long l = buffer.getLong();
		byte b = buffer.get();*/
		next = new FilePosition(bytes);
	}
	
	public void setValue(int i, BplusValue v)
	{
		if(!hasRead)
		{
			file.read(this);
			hasRead = true;
		}
		encapsulatedNode.values[i] = v;
		
		if(!properties.isCachingOn())
			file.writeOneValueOnly(this, i);
		//this only works because we have guarenteed spare space
		//at the end of the nodestoragefile (see spareblocks)
	}
	

	
	public static void main(String[] args) {

		
		BplusTreeProperties properties = new BplusTreeProperties();
		
		properties.setBplusInnerNodeBranchingFactor(2);
		properties.setBplusLeafNodeBranchingFactor(20);
		
		properties.setKeyClass(("uk.ac.gla.terrier.structures.trees.bplustree.BplusTestKey"));
		properties.setValueClass(("uk.ac.gla.terrier.structures.trees.bplustree.BplusTestValue"));
		
		
		//NodeStorageFile a = new NodeStorageFile(ApplicationSetup.TERRIER_INDEX_PATH+"LeafNodeFileTemp");
		
		
		NodeStorageFile a = new NodeStorageFile(ApplicationSetup.TERRIER_INDEX_PATH+"/LeafNodeFileTemp",
		// "/users/students4/level4/kanej/TerrierProject/Terrier/terrier/var/index/NodeStorageFile.txt",
				BplusDiskLeafNode.sizeInBytes(properties));
		
		properties.setLeafNodeFile(a);
		BplusDiskLeafNode node1 = new BplusDiskLeafNode(properties);
		System.out.println("Start of first node "+node1.getFilePosition().Bytes);
		BplusDiskLeafNode node2 = new BplusDiskLeafNode(properties);
		System.out.println("Start of second node "+node2.getFilePosition().Bytes);
		
		System.out.println("Node length : "+node1.sizeInBytes());
		
 		BplusTestKey testKey1 = new BplusTestKey(17);
 		BplusTestKey testKey2 = new BplusTestKey(20);
 		
 		
 		/*BplusTestKey testKey2 = new BplusTestKey(95);
 		BplusTestKey testKey3 = new BplusTestKey(46);
 		BplusTestKey testKey4 = new BplusTestKey(125);
 		BplusTestKey testKey5 = new BplusTestKey(126);
 		BplusTestKey testKey6 = new BplusTestKey(130);
 		BplusTestKey testKey7 = new BplusTestKey(133);
 		BplusTestKey testKey8 = new BplusTestKey(146);
 		BplusTestKey testKey9 = new BplusTestKey(149);
 		BplusTestKey testKey10 = new BplusTestKey(151);*/
 		
 		node1.insert(testKey1, new BplusTestValue("Success."));
 		node2.insert(testKey2, new BplusTestValue("."));
 		/*node.insert(testKey2, new BplusTestValue("Victory."));
 		node.insert(testKey3, new BplusTestValue("Triumph."));
 		node.insert(testKey4, new BplusTestValue("Eternal Glory."));
 		node.insert(testKey5, new BplusTestValue("Well done."));
 		node.insert(testKey6, new BplusTestValue("Success."));
 		node.insert(testKey7, new BplusTestValue("Victory."));
 		node.insert(testKey8, new BplusTestValue("Triumph."));
 		node.insert(testKey9, new BplusTestValue("Eternal Glory."));
 		node.insert(testKey10, new BplusTestValue("Well done."));*/
 		
 		//file.write(node);
 		//file.read(node);
 		//System.out.println(node.encapsulatedNode);
 		
 		System.out.println(node1);
 		System.out.println(node2);
 		//System.out.println(node.getNext());
 		//System.out.println(node.getNext().getNext());
 		System.out.println("Start of first node (in bytes) : "+node1.getFilePosition().Bytes);
 		
 		System.out.println("Start of second node (in bytes) : "+node2.getFilePosition().Bytes);
 		
 		System.out.println("finished");
		/*
		BplusDiskLeafNode[] nodeArray = new BplusDiskLeafNode[10];
		for(int i = 0; i < nodeArray.length; i++)
		{
			nodeArray[i] = new BplusDiskLeafNode(properties);
			nodeArray[i].encapsulatedNode = new BplusMemoryLeafNode(properties);
			nodeArray[i].next = new FilePosition((long)123,(byte)0);
			a.write(nodeArray[i]);
		}
		
		
		
		for(int i = 0; i < nodeArray.length; i++)
		{
			a.read(nodeArray[i]);
			System.out.println(nodeArray[i].next);
			a.write(nodeArray[i]);
		}
		
		System.out.println(nodeArray[0].filePosition+" "+nodeArray[0].filePosition+
				" Difference is "+(nodeArray[1].filePosition.Bytes-nodeArray[0].filePosition.Bytes)
				+" where size is "+nodeArray[0].sizeInBytes());
		//*/
		
	}
	
	
}

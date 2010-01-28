package uk.ac.gla.terrier.structures.trees.bplustree;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import uk.ac.gla.terrier.structures.FilePosition;
import uk.ac.gla.terrier.utility.ApplicationSetup;

/**
 * The Node storage file is used by the disk versions of inner and leaf node classes.
 * It deals with reading and writing particular instances, as in nodes, to file as bytes.
 * <p>
 * The class composes around the standard RandomAccessFile. Nodes passed to the NodeStorageFile
 * must implement the BplusDiskNode interface. The key methods the node must provide are sizeInBytes(),
 * toBytes() and load().
 * <p>
 * When created the NodeStorageFile is told the size of a node, hence forth it will allocate
 * and read blocks of that size. Hence a NodeStorageFile cannot be used to store Nodes of different types.
 * <p>
 * When a node is created it can call teh NodeStorageFile to have a free block allocated. When passed into
 * the NodeStorageFile, the node is converted to bytes using its toBytes() method and written in the allocated block.
 * <p>
 * The NodeStorageFile maintains free block list. When a new block must be allocated it searches the list,
 * returning the first block it finds; this is allowed as only one block will ever be needed per node and the size
 * of each block is constant. 
 * @author John Kane
 *
 */


public class NodeStorageFile {

	
	/** Default file mode access for a BitFile object. 
	  * Currently "<tt>rw</tt>". */
	protected static final String DEFAULT_FILE_MODE = "rw";
	
	protected static int spareBlocks = 
		Integer.parseInt(ApplicationSetup.getProperty("bplustree.nodestoragefile.spareblocks",
				"1000"));
	
	/** The underlying random access file.*/
	protected RandomAccessFile file;
	/** 
	 * The current byte offset. This attribute has 
	 * two functionalities. While writing to the file, 
	 * it corresponds to the byte offset from the beginning of 
	 * the file. While reading entries from the file, it 
	 * corresponds to the byte offset from the beginning 
	 * of the buffer.*/
	protected long byteOffset;
	/** The current offset in the file in bits in the last byte.*/
	protected byte bitOffset;
	
	/**
	 * List of free blocks.  
	 */
	protected LinkedList freeBlocks;
	
	/**
	 * The size of a block, it is equal to the size in bytes of the node
	 * type the NodeStorageFile is used to store.
	 */
	protected int LengthOfBlockInBytes;
	
	/**
	 * Position in the file were the free list starts. Stored at the end of the file
	 */
	protected long startOfFreeList;
	
	protected int blockCount;
	
	protected boolean isEmpty;
	
	//Constructor
	/** A constuctor for an instance of the NodeStorageFile class. File access mode is DEFAULT_FILE_MODE */
	public NodeStorageFile(File f, int LengthBlockInBytes) {
		
		LengthOfBlockInBytes = LengthBlockInBytes;
		freeBlocks = new LinkedList();
		
		try {
			if(f.exists() && f.length() > 0)
			{
				isEmpty = false;
				
				file = new RandomAccessFile(f, DEFAULT_FILE_MODE);
				
				//scans to the end of file to read the very last 8 bytes
				//which containts the start point of the free list
				file.seek(file.length()-8);
				
				startOfFreeList = file.readLong();
				file.seek(startOfFreeList);
				
				//reads in the free list
				long pos = startOfFreeList;
				while(pos < file.length() - 8)
				{
					freeBlocks.add(new Long(file.readLong()));
					pos += 8;
				}
				System.err.println("Size of file: "+file.length());
				//chops off the freelist data from the file
				file.setLength(startOfFreeList);
				blockCount = (int)(file.length()/ LengthOfBlockInBytes);
			}
			else
			{
				isEmpty = true;
				file = new RandomAccessFile(f, DEFAULT_FILE_MODE);
				
				file.setLength(LengthOfBlockInBytes * spareBlocks);
				blockCount =0;
				bitOffset = 0;
				byteOffset = 0;
			}
		} catch (IOException ioe) {
			System.err.println("Input/Output exception while creating NodeStorageFile.");
			ioe.printStackTrace();
			System.exit(1);
		}
			
	}
	
	/** A constuctor for an instance of this class. File access mode is DEFAULT_FILE_MODE */
	public NodeStorageFile(String filename, int LengthBlockInBytes) {
		this(new File(filename),LengthBlockInBytes);
	}
	
	/**
	 * Closes the node storage file. Before being closed, the free list is
	 * written at the end of the file, the last 8 bytes is a long indicating
	 * the start of the free list.
	 */
//	TODO Add the functionality to the NodeStorageFile to allow it to close but maintain the free block list, remember to update the documentation

	public void close() {
		try {
		
		startOfFreeList = file.length();
		file.seek(file.length());
		
		//writes the free list at the end of the file
		Iterator iter = freeBlocks.listIterator();
		while(iter.hasNext())
		{
			long t = ((Long)iter.next()).longValue();
			file.writeLong(t);
		}
		
		//writes the start of the free list as the last 8 bytes of the file
		file.writeLong(startOfFreeList);

		file.close();
		} catch(IOException ioe) {
			System.err.println
			("Input/Output exception while closing " +
			"the NodeStorageFile. Stack trace follows");
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Shifts the cursor to the input fileposition. The next read starts
	 * at the position given as input
	 * 
	 * @param fp the position in the file to move to.
	 */
	protected void seek(FilePosition fp)
	{
		try{
			file.seek(fp.Bytes);
		}
		catch(IOException ioe)
		{
			System.err.println("Node strorage file threw exception seeking for offset "+fp.Bytes+", block length="+LengthOfBlockInBytes);
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Reads a the data stored in the file at the block associated with this node.
	 * The node is then loaded with the data, wconverting it to useful information
	 * matching its internal representation.
	 * 
	 * @param node a disk node associated with this storage file
	 */
	public void read(BplusDiskNode node)
	{
		try{
			
		
		//byte[] tempArray = new byte[node.sizeInBytes()];
		//file.read(tempArray);
		if(! isEmpty)
		{
			seek(node.getFilePosition());
			node.load(file);
		}
		else
		{

			node.load(null);
		}
			
		}catch(Exception ioe)
		{System.err.println("Node strorage file: ");ioe.printStackTrace();}
	}
	
	/**
	 * Reads the File Position of the "Next" node, that is, the one
	 * chained sequentially to the current node. The pointer to the next node should be stored
	 * at the start of of the current nodes block.
	 * @param node a disk node associated with this storage file
	 */
	public void readNext(BplusDiskLeafNode node)
	{
		try{
			seek(node.getFilePosition());
			byte[] tempArray = new byte[FilePosition.sizeInBytes()];
			file.read(tempArray);
			
			(node).setNext(tempArray);
			}catch(IOException ioe)
			{System.err.println("Node strorage file: ");ioe.printStackTrace();}
	}
	
	
	/**
	 * Takes a node, converts it into bytes and writes those bytes
	 * into the block on file assocaited with that node.
	 * 
	 * @param node a disk node associated with this storage file
	 */
	public void write(BplusDiskNode node)
	{
		
		try{
			isEmpty = false;
			seek(node.getFilePosition());
			node.toBytes(file);
			//blockCount is incremented in getFreeBlock, as nodes may have space reserved but not yet written
		}catch(Exception ioe)
		{System.err.println("Node strorage file: ");ioe.printStackTrace();}
	}
	
	/**
	 * Like write(), but takes a node, and writes only one value to disk, as
	 * specified by index
	 * 
	 * @param node a disk node associated with this storage file
	 */
	public void writeOneValueOnly(BplusDiskNode node, int index)
	{	
		try{
			isEmpty = false;
			seek(node.getFilePosition());
			((BplusDiskLeafNode)node).toBytesOneValueOnly(file, index);
			//blockCount is incremented in getFreeBlock, as nodes may have space reserved but not yet written
		}catch(Exception ioe)
		{System.err.println("Node storage file: ");ioe.printStackTrace();}
	}
	
	public void writeValuesOnly(BplusDiskNode node)
	{	
		try{
			isEmpty = false;
			seek(node.getFilePosition());
			((BplusDiskLeafNode)node).toBytesValuesOnly(file);
			//blockCount is incremented in getFreeBlock, as nodes may have space reserved but not yet written
		}catch(Exception ioe)
		{System.err.println("Node storage file: ");ioe.printStackTrace();}
	}
	
	/* node has to be an instance of BplusDiskLeafNode */
	public void writeNoKeysOrValue(BplusDiskNode node)
	{
		try{
			isEmpty = false;
			seek(node.getFilePosition());
			((BplusDiskLeafNode)node).toBytesNoKeysOrValue(file);
			/* blockCount is incremented in getFreeBlock, as nodes may have space 
			 * reserved but not yet written. Conversly, this block may already have
			 * been written, and this is just an update */ 
		}catch(Exception ioe)
		{System.err.println("Node strorage file: ");ioe.printStackTrace();}
	}
	
	/**
	 * Returns the file position of the start of a free block that can
	 * be used to store a disk node within. The list of free blocks is searched
	 * for a condidate first, if its empty the free block is appended to the 
	 * end of the file.
	 * @param sizeOfBlocksInBytes
	 * @return File position of the start of the free block
	 */
	public FilePosition getFreeBlock(int sizeOfBlocksInBytes)
	{
	
		LengthOfBlockInBytes = sizeOfBlocksInBytes;
		
		try{
		long pos;
		if(freeBlocks.isEmpty())
		{
			/*Not sure about this, used to be pos = file.length()-1, but had
			 * problems when file.length == 0*/
			//pos = file.length();
			//pos = (l.longValue())*LengthOfBlockInBytes;
			pos = blockCount == 0 ? 0 : (blockCount)*LengthOfBlockInBytes;
			
			/*DISABLED: file is automatically grown to have spareBlocks 
			  blocks left at end of file */
			//file.setLength((blockCount+1)*LengthOfBlockInBytes);
			
			blockCount++;
			
			//OK, if the file is not big enough for this block
			//then add enough space for this block, and another
			//spareBlocks after it.
			if (pos > file.length())
			{
				file.setLength(pos + LengthOfBlockInBytes + (spareBlocks * LengthOfBlockInBytes));
			}
		}
		else
		{
			Long l = (Long)Collections.min(freeBlocks);
			freeBlocks.remove(l);
			pos = (l.longValue())*LengthOfBlockInBytes;
		}
		
		
		
		//bit part not used as blocks are whole bytes in size.
		return new FilePosition((pos), (byte)0);
		}catch(Exception ioe)
		{System.out.println("Exception while reading "
			+	"length of node storage file.\n"+ioe);}
		return null;
	}
	
	/**
	 * Marks a node's block on disk as now free. It does this by adding
	 * the block to the free list.
	 * @param fp the file position of the start of the now free block associated with the deleted disk node
	 */
	public void deleteBlock(FilePosition fp)
	{
		long pos = fp.Bytes / LengthOfBlockInBytes;
		freeBlocks.add(new Long(pos));
		blockCount--;
		//TODO set isEmpty to true is blockCount == 0 ???
		//TODO empty freeBlocks list from memory if no blocks on disk
		//TODO remove trailing empty block space from end of file 
		
	}
	
	/**
	 * Accessor for free blocks list.
	 */
	public LinkedList getFreeBlocks()
	{
		return freeBlocks;
	}
	
}

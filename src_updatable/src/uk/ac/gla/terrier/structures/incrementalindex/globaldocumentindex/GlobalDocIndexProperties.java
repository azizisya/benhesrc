package uk.ac.gla.terrier.structures.incrementalindex.globaldocumentindex;

import java.io.RandomAccessFile;

import uk.ac.gla.terrier.structures.trees.bplustree.BplusDiskInnerNode;
import uk.ac.gla.terrier.structures.trees.bplustree.BplusDiskLeafNode;
import uk.ac.gla.terrier.structures.trees.bplustree.BplusTreeProperties;
import uk.ac.gla.terrier.structures.trees.bplustree.NodeStorageFile;
import uk.ac.gla.terrier.utility.ApplicationSetup;

public class GlobalDocIndexProperties {

	private BplusTreeProperties treeProp;
	protected String prefix;
	protected String path;
	protected RandomAccessFile idToNodeFile;

	public void setIdToNodeFile(RandomAccessFile f) {
		idToNodeFile = f;
	}

	public RandomAccessFile getIdToNodeFile() {
		return idToNodeFile;
	}
	
	protected void setTreeProperties(BplusTreeProperties p) {
		treeProp = p;
	}

	protected  BplusTreeProperties getTreeProperties() {
		return treeProp;
	}
	
	public GlobalDocIndexProperties(String path, String prefix){
		try{
		this.path = path;
		this.prefix = prefix;
		treeProp = new BplusTreeProperties();
		
		treeProp.setBplusInnerNodeBranchingFactor(2);
		treeProp.setBplusLeafNodeBranchingFactor(10);
		treeProp.setKeyClass("uk.ac.gla.terrier.structures.incrementalindex.globaldocumentindex.DocIndexStringKey");
		treeProp.setValueClass("uk.ac.gla.terrier.structures.incrementalindex.globaldocumentindex.DocIndexRecord");
		
		//"/users/students4/level4/kanej/TerrierProject/Terrier/terrier/var/index/DocIndexLeafNodeFile",
		NodeStorageFile a = new NodeStorageFile(
				path+ApplicationSetup.FILE_SEPARATOR+prefix+".docindex.leafnodes",
				BplusDiskLeafNode.sizeInBytes(treeProp));
		getTreeProperties().setLeafNodeFile(a);
		NodeStorageFile b = new NodeStorageFile(
				path+ApplicationSetup.FILE_SEPARATOR+prefix+".docindex.innernodes",
				BplusDiskInnerNode.sizeInBytes(treeProp));
		getTreeProperties().setInnerNodeFile(b);
		
		 
		idToNodeFile = new RandomAccessFile(path+ApplicationSetup.FILE_SEPARATOR+prefix+".docindex.id2nodes", "rw");
		 
		
		
		}catch(Exception ioe){
			System.out.println("Error while loading Global document index properties:");
			ioe.printStackTrace();
		}
	}
	
	
}

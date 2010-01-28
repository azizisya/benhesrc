package uk.ac.gla.terrier.structures.incrementalindex.globallexicon;

import java.io.RandomAccessFile;

import uk.ac.gla.terrier.structures.trees.bplustree.*;
import uk.ac.gla.terrier.utility.ApplicationSetup;


public class GlobalLexiconProperties  {
	
	private BplusTreeProperties treeProp;
	protected String path;
	protected String prefix;
	protected static RandomAccessFile idToNodeFile;

	public void setIdToNodeFile(RandomAccessFile idToNodeFile) {
		GlobalLexiconProperties.idToNodeFile = idToNodeFile;
	}

	public RandomAccessFile getIdToNodeFile() {
		return idToNodeFile;
	}
	
	protected  void setTreeProperties(BplusTreeProperties treeProp) {
		this.treeProp = treeProp;
	}

	protected  BplusTreeProperties getTreeProperties() {
		return treeProp;
	}
	
	public GlobalLexiconProperties(String path, String prefix){
		try{
		this.path = path;
		this.prefix = prefix;
		setTreeProperties(new BplusTreeProperties());
		
		//getTreeProperties().setBplusInnerNodeBranchingFactor(2);
		//getTreeProperties().setBplusLeafNodeBranchingFactor(40);
		treeProp.setKeyClass("uk.ac.gla.terrier.structures.incrementalindex.globallexicon.LexiconStringKey");
		treeProp.setValueClass("uk.ac.gla.terrier.structures.incrementalindex.globallexicon.LexiconRecord");
		
		NodeStorageFile a = new NodeStorageFile(
			path+ApplicationSetup.FILE_SEPARATOR+prefix+".glex.leafnodes",
			BplusDiskLeafNode.sizeInBytes(getTreeProperties()));
		
		getTreeProperties().setLeafNodeFile(a);
		NodeStorageFile b = new NodeStorageFile(
				path+ApplicationSetup.FILE_SEPARATOR+prefix+".glex.innernodes",
				BplusDiskInnerNode.sizeInBytes(getTreeProperties()));
		getTreeProperties().setInnerNodeFile(b);
		
		 
		idToNodeFile = new RandomAccessFile(
				path+ApplicationSetup.FILE_SEPARATOR+prefix+".glex.id2node", "rw");
		 
		
		
		}catch(Exception ioe){
			System.out.println("Error while loading Global lexicon properties:");
			ioe.printStackTrace();
		}
	}



}

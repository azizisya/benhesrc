package uk.ac.gla.terrier.structures.trees.bplustree;

public class BplusTreeProperties {
	
	public int BplusLeafNodeBranchingFactor = 10;
	
	public int BplusInnerNodeBranchingFactor = 10;
	
	//private String locationsofLeafNodeStorageFile;
	
	protected NodeStorageFile LeafNodeFile;
	
	//private String locationsofInnerNodeStorageFile;
	
	protected NodeStorageFile InnerNodeFile;
	
	private boolean cachingOn = true;
	
	private int MaxSizeOfCache = 100;
	
	protected Class KeyClass;
	
	protected Class ValueClass;

	public void setBplusLeafNodeBranchingFactor(int bplusLeafNodeBranchingFactor) {
		BplusLeafNodeBranchingFactor = bplusLeafNodeBranchingFactor;
	}

	public int getBplusLeafNodeBranchingFactor() {
		return BplusLeafNodeBranchingFactor;
	}

	public void setBplusInnerNodeBranchingFactor(int bplusInnerNodeBranchingFactor) {
		BplusInnerNodeBranchingFactor = bplusInnerNodeBranchingFactor;
	}

	public int getBplusInnerNodeBranchingFactor() {
		return BplusInnerNodeBranchingFactor;
	}

	public void setLeafNodeFile(NodeStorageFile locationsofLeafNodeStorageFile) {
		this.LeafNodeFile = locationsofLeafNodeStorageFile;
	}

	public NodeStorageFile getLeafNodeFile() {
		return LeafNodeFile;
	}

	public void setInnerNodeFile(NodeStorageFile locationsofInnerNodeStorageFile) {
		this.InnerNodeFile = locationsofInnerNodeStorageFile;
	}

	public NodeStorageFile getInnerNodeFile() {
		return InnerNodeFile;
	}

	public void setKeyClass(String KC) {
		try{
		KeyClass = Class.forName(KC);
		}catch(ClassNotFoundException e){e.printStackTrace();}
	}

	public Class getKeyClass() {
		return KeyClass;
	}

	public void setValueClass(String VC) {
		try{
			ValueClass = Class.forName(VC);
		}catch(ClassNotFoundException e){e.printStackTrace();}
	}

	public Class getValueClass() {
		return ValueClass;
	}

	public void setCachingOn(boolean cachingOn)
	{
		this.cachingOn = cachingOn;
	}

	public boolean isCachingOn()
	{
		return cachingOn;
	}

	protected void setMaxSizeOfCache(int maxSizeOfCache)
	{
		MaxSizeOfCache = maxSizeOfCache;
	}

	protected int getMaxSizeOfCache()
	{
		return MaxSizeOfCache;
	} 
	
}
